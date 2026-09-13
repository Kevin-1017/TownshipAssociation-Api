package com.tsa.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;

/** 管理后台登录请求（POST /tsa/auth/admin-login）。 */
@Data
// 安全审查 minor#2：toString 排除 password，防日志/异常上下文泄露明文口令
@ToString(exclude = "password")
@Schema(description = "管理后台登录请求")
public class AdminLoginRequest {

    @Schema(description = "登录用户名", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写用户名")
    private String username;

    @Schema(description = "登录密码（明文，仅走 HTTPS 传输；服务端只比对 BCrypt 哈希，绝不明文落库）",
            example = "your-password", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写密码")
    private String password;
}
