package com.tsa.api.controller;

import com.tsa.api.common.Result;
import com.tsa.api.config.ApiConstants;
import com.tsa.api.dto.VerifyPhoneRequest;
import com.tsa.api.dto.VerifyPhoneVO;
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
 * 乡会身份接口。
 *
 * <p>注意路径在 {@code /tsa/auth} 下但不挂 SaRouter 鉴权 —— 核验接口本就要给
 * 未登录的人用；受限资源（成员详情）的闸门在 Service 层按业务码 1301 裁决。
 */
@Tag(name = "乡会身份", description = "手机号核验与乡会用户识别")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "乡会身份核验",
            description = "前端 getPhoneNumber 按钮拿动态 code 传进来，后端换真实手机号后与内部乡会名册比对。"
                    + "命中返回 verified=true 与 X-Assoc-Token 用的令牌；未命中返回 verified=false（HTTP 200，业务结果非错误）")
    @PostMapping("/verify-phone")
    public Result<VerifyPhoneVO> verifyPhone(@Valid @RequestBody VerifyPhoneRequest request) {
        return Result.ok(authService.verifyPhone(request.getCode()));
    }
}