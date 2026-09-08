package com.tsa.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 乡会身份核验请求。
 */
@Data
@Schema(description = "乡会身份核验请求")
public class VerifyPhoneRequest {

    @NotBlank(message = "缺少手机号授权码")
    @Schema(description = "getPhoneNumber 按钮返回的动态 code（注意：与 wx.login 的 code 不是同一个，5 分钟有效且一次性）",
            example = "the-phone-code")
    private String code;
}