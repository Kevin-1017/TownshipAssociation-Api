package com.tsa.api.controller;

import com.tsa.api.common.Result;
import com.tsa.api.config.ApiConstants;
import com.tsa.api.dto.AdminCurrentUserVO;
import com.tsa.api.service.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台「当前身份」接口：挂在 admin 墙内，供前端拉取登录用户名/角色做界面渲染与守卫。
 *
 * <p>登录/注销的<b>铸造与销毁</b>在 AuthController（公开路径 /tsa/auth/admin-*），
 * 本类只做鉴权后的<b>只读会话</b>。整个 /tsa/admin/** 前缀由 SaTokenConfig 的
 * {@code checkRole("admin")} 把门：非 admin 会话到不了这里（401 未登录 / 403 无角色）。
 */
@Tag(name = "管理后台-身份", description = "当前登录管理员信息")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @Operation(summary = "当前登录管理员",
            description = "从会话读 {username,role}（role：1 超管 / 2 普通）；未登录 401、非 admin 会话 403")
    @GetMapping("/me")
    public Result<AdminCurrentUserVO> me() {
        return Result.ok(adminAuthService.currentUser());
    }
}
