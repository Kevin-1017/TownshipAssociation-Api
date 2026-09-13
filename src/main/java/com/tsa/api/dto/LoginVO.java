package com.tsa.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 微信登录结果（契约 C1）。
 *
 * <p><b>类上禁加 {@code @JsonInclude(NON_NULL)}</b>：Result 本身无 NON_NULL 注解，
 * 而契约要求「已登录未建档」时 {@code data.user} 这个键必须出现在 JSON 里
 * （值为 null）——前端以「键在而值为 null」判定未建档引导态，键整个消失会被
 * 误判成响应结构异常。这是本类与 MemberDetailVO（字段级 NON_NULL）策略不同的原因。
 */
@Data
@Schema(description = "微信登录结果")
public class LoginVO {

    @Schema(description = "Sa-Token 令牌，后续请求放 Authorization: Bearer <token> 请求头")
    private String token;

    @Schema(description = "本人档案（本人视角，联系方式不裁剪）；已登录未建档时为 null")
    private MemberDetailVO user;

    /** 登录成功统一出口；user 允许为 null（首登无档案，不建幽灵 member 行） */
    public static LoginVO of(String token, MemberDetailVO user) {
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUser(user);
        return vo;
    }
}
