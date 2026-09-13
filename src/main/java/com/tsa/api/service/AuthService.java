package com.tsa.api.service;

import com.tsa.api.dto.LoginVO;
import com.tsa.api.dto.MemberDetailVO;
import com.tsa.api.dto.ProfileUpdateRequest;
import com.tsa.api.dto.VerifyPhoneVO;

/**
 * 身份服务：微信静默登录（openid）+ 乡会身份核验（手机号名册）。
 *
 * <p>两套身份体系共用同一个 Sa-Token，靠 <b>loginId 前缀</b>划命名空间（v1.1 修订 A9 定稿）：
 * <ul>
 *   <li>微信登录：{@code loginId = 裸 openid}（本期已落地），令牌走 {@code Authorization: Bearer <token>}，
 *       受保护路由由 SaTokenConfig 拦截器校验</li>
 *   <li>乡会核验：{@code loginId = "assoc-" + association_member.id}，令牌走 X-Assoc-Token，
 *       身份类失败一律 1301（绝不出 401）</li>
 * </ul>
 * 两套令牌<b>互不通用</b>不是框架自动保证的 —— assoc 令牌在 Sa-Token 眼里是合法登录态，
 * 防线是 {@link #currentUser()} / {@link #currentOpenid()} 里对 "assoc-" 前缀的显式拒绝。
 */
public interface AuthService {

    /**
     * 用 getPhoneNumber 动态 code 换手机号并比对乡会名册。
     *
     * <p>命中：签发乡会身份 Sa-Token 会话并返回令牌；未命中：返回
     * <code>verified=false</code>（HTTP 200 业务结果，不是错误）。
     */
    VerifyPhoneVO verifyPhone(String getPhoneNumberCode);

    /**
     * 乡会身份闸门：详情等受限接口先过这道校验。
     *
     * <p>token 缺失/失效/非法一律抛
     * {@code BusinessException(ASSOC_MEMBER_ONLY)}。
     */
    void ensureAssoc(String assocToken);

    /**
     * 微信静默登录（第一期主干）：wx.login 的 code 换 openid → 签发登录会话。
     *
     * <p>时序：exchangeOpenid → 查 member（可为 null，首登不建档）→ StpUtil.login(openid)
     * → wechat_user 埋点 upsert（失败仅 log.warn 不阻断）。
     * 微信侧失败统一 1303；本方法不做限频（由 AuthRateLimitInterceptor 在入口拦 1306）。
     */
    LoginVO wechatLogin(String jsCode);

    /**
     * 本人视角的登录用户档案（{@code GET /tsa/user/me} 数据源）。
     *
     * <p>loginId 缺失或以 "assoc-" 开头 → 抛 {@code NotLoginException}（→401 壳）；
     * 已登录但未建档 → 返回 null（契约 C2，前端据此走「点击完善资料」引导）。
     */
    MemberDetailVO currentUser();

    /**
     * 从 Bearer 登录会话解析裸 openid（与 currentUser 同一拒绝规则）。
     *
     * <p>供 /user/me 及后续本人写接口（资料保存等，第 2 棒）复用：
     * openid 一律服务端推导，严禁信任客户端自报（修订 A7/B16 的钓鱼防线同源）。
     */
    String currentOpenid();

    /**
     * 注销当前 Bearer 会话（契约 C3）。
     *
     * <p>未登录时幂等静默返回（端点无守卫，前端本地清态流程不该被 401 卡住）；
     * 只注销当前 token，assoc 会话（loginId 不同）不受影响。
     */
    void logout();

    /**
     * 本人资料真保存（契约 C7，PUT /tsa/user/profile）。
     *
     * <p>openid 从 Bearer loginId 推导（assoc- 拒绝规则同 currentUser），严禁信任请求体；
     * 无 member 行 → INSERT status=0(待审) + source=1(本人提交)，province/city 留 NULL
     * （待秘书处审核时补录）；有行 → 只更白名单（status/province/city/openid 结构上不动）。
     * 返回更新后的最新 MemberDetailVO（本人视角，不裁联系方式）。
     */
    MemberDetailVO updateOwnProfile(ProfileUpdateRequest request);
}
