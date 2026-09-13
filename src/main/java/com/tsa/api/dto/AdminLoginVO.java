package com.tsa.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 管理后台登录结果（POST /tsa/auth/admin-login 成功 data）。 */
@Data
@Schema(description = "管理后台登录结果")
public class AdminLoginVO {

    @Schema(description = "Sa-Token 令牌，后续请求放 Authorization: Bearer <token> 请求头")
    private String token;

    @Schema(description = "登录用户名", example = "admin")
    private String username;

    @Schema(description = "角色：1 超级管理员 / 2 普通管理员", example = "1")
    private Integer role;

    public static AdminLoginVO of(String token, String username, Integer role) {
        AdminLoginVO vo = new AdminLoginVO();
        vo.setToken(token);
        vo.setUsername(username);
        vo.setRole(role);
        return vo;
    }
}
