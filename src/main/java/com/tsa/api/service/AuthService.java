package com.tsa.api.service;

import com.tsa.api.dto.LoginVO;
import com.tsa.api.dto.MemberDetailVO;
import com.tsa.api.dto.ProfileUpdateRequest;

/**
 * 身份服务：微信静默登录（openid）与本人档案。
 *
 * <p>本系统与 Sa-Token 的 loginId 命名空间约定：
 * <ul>
 *   <li>微信登录：{@code loginId = 裸 openid}，令牌走 {@code Authorization: Bearer <token>}，
 *       受保护路由由 SaTokenConfig 拦截器校验</li>
 *   <li>管理后台：{@code loginId = "admin-" + 管理员id}，前缀只由后台登录接口铸造（见 AdminAuthServiceImpl）</li>
 * </ul>
 * 两套令牌共存于同一个 Sa-Token，{@code checkLogin()} 对 admin 会话同样放行 ——
 * {@code /tsa/user/**} 侧的隔离靠 {@link #currentOpenid()} 把 loginId 直接当 openid 使用：
 * admin 前缀查不到 member 行，本人接口拿不到任何他人档案。
 */
public interface AuthService {

    /**
     * 微信静默登录：wx.login 的 code 换 openid → 签发登录会话。
     *
     * <p>时序：exchangeOpenid → 查 member（可为 null，首登不建档）→ StpUtil.login(openid)
     * → wechat_user 埋点 upsert（失败仅 log.warn 不阻断）。
     * 微信侧失败统一 1303；本方法不做限频（由 AuthRateLimitInterceptor 在入口拦 1306）。
     */
    LoginVO wechatLogin(String jsCode);

    /**
     * 本人视角的登录用户档案（{@code GET /tsa/user/me} 数据源）。
     *
     * <p>loginId 缺失 → 抛 {@code NotLoginException}（→401 壳）；
     * 已登录但未建档 → 返回 null（前端据此走「点击完善资料」引导）。
     */
    MemberDetailVO currentUser();

    /**
     * 从 Bearer 登录会话解析裸 openid（与 currentUser 同一拒绝规则）。
     *
     * <p>供 /user/me 与本人写接口复用：openid 一律服务端推导，严禁信任客户端自报（防钓鱼建档）。
     */
    String currentOpenid();

    /**
     * 注销当前 Bearer 会话。
     *
     * <p>未登录时幂等静默返回（端点无守卫，前端本地清态流程不该被 401 卡住）。
     */
    void logout();

    /**
     * 本人资料保存（{@code PUT /tsa/user/profile}）。
     *
     * <p>openid 从 Bearer loginId 推导（同 currentUser 的拒绝规则），严禁信任请求体；
     * 无 member 行 → INSERT status=0(待审) + source=1(本人提交)，province/city 留 NULL
     * （待秘书处审核时补录）；有行 → 只更白名单（status/province/city/openid 结构上不动）。
     * 返回更新后的最新 MemberDetailVO（本人视角，不裁联系方式）。
     */
    MemberDetailVO updateOwnProfile(ProfileUpdateRequest request);
}
