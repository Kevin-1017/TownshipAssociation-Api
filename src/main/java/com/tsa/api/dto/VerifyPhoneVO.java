package com.tsa.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 乡会身份核验结果。
 *
 * <p>{@code verified=false} 是<b>正常业务结果</b>（HTTP 200），不是错误——
 * 手机号没进乡会名册而已，前端据此展示「拒绝态」文案。
 */
@Data
@Schema(description = "乡会身份核验结果")
public class VerifyPhoneVO {

    @Schema(description = "是否命中乡会名册：false 是正常业务结果而非报错")
    private boolean verified;

    @Schema(description = "乡会身份令牌（仅 verified=true 时下发），后续详情接口放在 X-Assoc-Token 请求头")
    private String token;

    @Schema(description = "名册姓名", example = "张三")
    private String name;

    @Schema(description = "乡会职务", example = "理事")
    private String role;

    /** 未命中名册 */
    public static VerifyPhoneVO unverified() {
        VerifyPhoneVO vo = new VerifyPhoneVO();
        vo.setVerified(false);
        return vo;
    }

    /** 命中名册：携带新签发的身份令牌 */
    public static VerifyPhoneVO verified(String token, String name, String role) {
        VerifyPhoneVO vo = new VerifyPhoneVO();
        vo.setVerified(true);
        vo.setToken(token);
        vo.setName(name);
        vo.setRole(role);
        return vo;
    }
}