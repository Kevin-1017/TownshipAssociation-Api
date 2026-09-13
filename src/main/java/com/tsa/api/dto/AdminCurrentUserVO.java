package com.tsa.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 当前登录管理员身份（GET /tsa/admin/auth/me 的 data，从会话读，不含令牌）。 */
@Data
@Schema(description = "当前登录管理员身份")
public class AdminCurrentUserVO {

    @Schema(description = "登录用户名", example = "admin")
    private String username;

    @Schema(description = "角色：1 超级管理员 / 2 普通管理员", example = "1")
    private Integer role;

    public static AdminCurrentUserVO of(String username, Integer role) {
        AdminCurrentUserVO vo = new AdminCurrentUserVO();
        vo.setUsername(username);
        vo.setRole(role);
        return vo;
    }
}
