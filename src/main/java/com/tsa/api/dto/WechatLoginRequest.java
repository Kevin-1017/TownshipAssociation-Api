package com.tsa.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 微信静默登录请求。
 */
@Data
@Schema(description = "微信静默登录请求")
public class WechatLoginRequest {

    @NotBlank(message = "登录凭证不能为空")
    @Size(max = 128, message = "登录凭证过长")
    @Schema(description = "wx.login success 回调返回的 code（注意：与 getPhoneNumber 的 code 不是同一个，5 分钟有效且一次性）",
            example = "the-wx-login-code")
    private String code;
}
