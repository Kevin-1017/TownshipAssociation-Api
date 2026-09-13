package com.tsa.api.service;

import com.tsa.api.dto.AdminCurrentUserVO;
import com.tsa.api.dto.AdminLoginRequest;
import com.tsa.api.dto.AdminLoginVO;

/**
 * 管理后台身份服务：账号口令登录（loginId={@code admin-<id>}）+ 当前管理员读取 + 注销。
 *
 * <p>与 {@link AuthService}（微信 openid / assoc 乡会两套身份）并列，共用同一个 Sa-Token，
 * 靠 loginId 前缀划命名空间。管理会话额外把 role/username 写进 SaSession，
 * 角色判定（供 /tsa/admin/** 的 checkRole("admin") 消费）在 {@code StpInterfaceImpl}。
 */
public interface AdminAuthService {

    /**
     * 账号口令登录：查 admin_user → BCrypt 比对 → 签发 {@code admin-<id>} 会话。
     *
     * <p>账号不存在 / 密码错误 / 已软删除一律抛 {@code BusinessException(ADMIN_LOGIN_FAILED)}（1307），
     * message 不带区分线索（防账号枚举）；本方法不做限频（由 AuthRateLimitInterceptor 在入口拦 1306）。
     */
    AdminLoginVO login(AdminLoginRequest request);

    /** 注销当前 admin 会话；无 token / 非 admin 会话幂等静默返回（端点无守卫，前端清态流程不该被 401 卡住）。 */
    void logout();

    /** 当前登录管理员身份（username + role，从会话读）；非 admin 会话抛 NotLoginException（→401）。 */
    AdminCurrentUserVO currentUser();
}
