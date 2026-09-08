package com.tsa.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsa.api.common.BusinessException;
import com.tsa.api.common.GlobalExceptionHandler;
import com.tsa.api.common.ResultCode;
import com.tsa.api.dto.VerifyPhoneRequest;
import com.tsa.api.dto.VerifyPhoneVO;
import com.tsa.api.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 测试。模板照抄 MemberControllerTest（standalone MockMvc）。
 *
 * <p>注意：这里 mock 的是 AuthService —— StpUtil 只存在于 ServiceImpl 内部，
 * Controller 层测试完全不受 Sa-Token 环境问题影响。
 */
class AuthControllerTest {

    private AuthService authService;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        authService = Mockito.mock(AuthService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /auth/verify-phone - code 为空时应返回 400 和字段提示")
    void verifyPhoneShouldFailWhenCodeBlank() throws Exception {
        VerifyPhoneRequest request = new VerifyPhoneRequest();
        // code 故意不填

        mockMvc.perform(post("/tsa/auth/verify-phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("授权码")));
    }

    @Test
    @DisplayName("POST /auth/verify-phone - 命中名册时应返回 verified=true 与令牌")
    void verifyPhoneShouldReturnVerifiedWithToken() throws Exception {
        Mockito.when(authService.verifyPhone(ArgumentMatchers.anyString()))
                .thenReturn(VerifyPhoneVO.verified("mock-token-abc", "张三", "理事"));

        VerifyPhoneRequest request = new VerifyPhoneRequest();
        request.setCode("some-phone-code");

        mockMvc.perform(post("/tsa/auth/verify-phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.verified").value(true))
                .andExpect(jsonPath("$.data.token").value("mock-token-abc"))
                .andExpect(jsonPath("$.data.name").value("张三"));
    }

    @Test
    @DisplayName("POST /auth/verify-phone - 未命中名册时是正常业务结果：HTTP 200 + verified=false")
    void verifyPhoneShouldReturnUnverifiedAsNormalResult() throws Exception {
        Mockito.when(authService.verifyPhone(ArgumentMatchers.anyString()))
                .thenReturn(VerifyPhoneVO.unverified());

        VerifyPhoneRequest request = new VerifyPhoneRequest();
        request.setCode("external-user-code");

        mockMvc.perform(post("/tsa/auth/verify-phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.verified").value(false));
    }

    @Test
    @DisplayName("POST /auth/verify-phone - 微信换号失败时应返回业务错误码 1302")
    void verifyPhoneShouldReturn1302WhenWechatFails() throws Exception {
        Mockito.when(authService.verifyPhone(ArgumentMatchers.anyString()))
                .thenThrow(new BusinessException(ResultCode.WECHAT_PHONE_VERIFY_FAILED));

        VerifyPhoneRequest request = new VerifyPhoneRequest();
        request.setCode("expired-code");

        mockMvc.perform(post("/tsa/auth/verify-phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.WECHAT_PHONE_VERIFY_FAILED.getCode()));
    }
}