package com.tsa.api.controller;

import com.tsa.api.common.Result;
import com.tsa.api.config.ApiConstants;
import com.tsa.api.dto.AdminLoginRequest;
import com.tsa.api.dto.AdminLoginVO;
import com.tsa.api.dto.LoginVO;
import com.tsa.api.dto.VerifyPhoneRequest;
import com.tsa.api.dto.VerifyPhoneVO;
import com.tsa.api.dto.WechatLoginRequest;
import com.tsa.api.service.AdminAuthService;
import com.tsa.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：微信静默登录 + 当前会话注销 + 乡会身份核验 + 管理后台账号口令登录/注销。
 *
 * <p>注意路径在 {@code /tsa/auth} 下但不挂 SaRouter 鉴权 —— 登录本身就是给未登录的人用的，
 * logout 亦刻意无守卫（幂等 200，见契约 C3）；受限资源（成员详情）的闸门在 Service 层按
 * 业务码 1301 裁决，受保护资源（/tsa/user/**）的闸门在 SaTokenConfig。
 * 本前缀另挂 IP 限频拦截器 AuthRateLimitInterceptor（wechat-login 10/分、verify-phone 5/分、
 * admin-login 5/分，超限 1306）。管理后台<b>只读会话</b>的接口 {@code GET /tsa/admin/auth/me}
 * 不在本类（它在 admin 墙内，另见 AdminAuthController），本类只管登录/注销的<b>铸造与销毁</b>。
 */
@Tag(name = "认证", description = "微信登录、注销、乡会身份核验与管理后台登录")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AdminAuthService adminAuthService;

    @Operation(summary = "微信静默登录",
            description = "前端用 wx.login 回调拿到的 code 换 openid 并签发登录令牌（后续请求放 Authorization: Bearer 头）。"
                    + "user 为 null 表示已登录未建档（前端走完善资料引导）；code 为空 400，"
                    + "微信侧失败 1303，IP 超限 1306")
    @PostMapping("/wechat-login")
    public Result<LoginVO> wechatLogin(@Valid @RequestBody WechatLoginRequest request) {
        return Result.ok(authService.wechatLogin(request.getCode()));
    }

    @Operation(summary = "乡会身份核验",
            description = "前端 getPhoneNumber 按钮拿动态 code 传进来，后端换真实手机号后与内部乡会名册比对。"
                    + "命中返回 verified=true 与 X-Assoc-Token 用的令牌；未命中返回 verified=false（HTTP 200，业务结果非错误）")
    @PostMapping("/verify-phone")
    public Result<VerifyPhoneVO> verifyPhone(@Valid @RequestBody VerifyPhoneRequest request) {
        return Result.ok(authService.verifyPhone(request.getCode()));
    }

    @Operation(summary = "注销当前微信登录会话",
            description = "只注销当前 Bearer 会话（乡会身份 X-Assoc-Token 不受影响）；无 token 幂等返回 200。"
                    + "会话存内存（Redis 二期），多端旧 token 滞留本期可接受")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok(null);
    }

    @Operation(summary = "管理后台登录",
            description = "账号口令换 admin 会话令牌（后续请求放 Authorization: Bearer 头，访问 /tsa/admin/** 用）。"
                    + "成功 data={token,username,role}；账号不存在/密码错/已删除统一回 1307（防账号枚举）；"
                    + "用户名或密码为空 400；IP 超限 1306")
    @PostMapping("/admin-login")
    public Result<AdminLoginVO> adminLogin(@Valid @RequestBody AdminLoginRequest request) {
        return Result.ok(adminAuthService.login(request));
    }

    @Operation(summary = "注销当前管理后台会话",
            description = "仅注销 admin 前缀会话；无 token 或非 admin 会话幂等返回 200（前端清本地态不该被 401 卡住）")
    @PostMapping("/admin-logout")
    public Result<Void> adminLogout() {
        adminAuthService.logout();
        return Result.ok(null);
    }
}