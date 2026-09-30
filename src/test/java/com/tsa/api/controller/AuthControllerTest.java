package com.tsa.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsa.api.common.BusinessException;
import com.tsa.api.common.GlobalExceptionHandler;
import com.tsa.api.common.ResultCode;
import com.tsa.api.dto.LoginVO;
import com.tsa.api.dto.WechatLoginRequest;
import com.tsa.api.service.AdminAuthService;
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
 * AuthController 测试（standalone MockMvc）。
 *
 * <p>mock 的是 AuthService —— StpUtil 只存在于 ServiceImpl 内部，
 * Controller 层测试完全不受 Sa-Token 环境问题影响。
 */
class AuthControllerTest {

    private AuthService authService;
    private AdminAuthService adminAuthService;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        authService = Mockito.mock(AuthService.class);
        adminAuthService = Mockito.mock(AdminAuthService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService, adminAuthService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---------- wechat-login ----------

    @Test
    @DisplayName("POST /auth/wechat-login - code 为空时应返回 400 且 message 含「登录凭证」")
    void wechatLoginShouldFailWhenCodeBlank() throws Exception {
        WechatLoginRequest request = new WechatLoginRequest();
        // code 故意不填

        mockMvc.perform(post("/tsa/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("登录凭证")));
    }

    @Test
    @DisplayName("POST /auth/wechat-login - 桩成功应 200 + data.token 非空，且 data.user 键必须在场（值可为 null=未建档）")
    void wechatLoginShouldReturnTokenWithUserKeyPresent() throws Exception {
        // 「已登录未建档」是本期所有新用户的常态：响应必须包含 data.user 这个键（值为 null），
        // 用原文子串断言而不是 jsonPath —— 键整个消失时 jsonPath 的报错信息分不出「缺键」还是「值为 null」，
        // 而 containsString("\"user\":null") 两态都能钉死（LoginVO 类上禁加 @JsonInclude 就是为它）
        Mockito.when(authService.wechatLogin(ArgumentMatchers.anyString()))
                .thenReturn(LoginVO.of("token-abc", null));

        WechatLoginRequest request = new WechatLoginRequest();
        request.setCode("the-wx-login-code");

        mockMvc.perform(post("/tsa/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("token-abc"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().string(org.hamcrest.Matchers.containsString("\"user\":null")));
    }

    @Test
    @DisplayName("POST /auth/wechat-login - 微信侧失败（Service 抛 1303）时应返回业务码 1303")
    void wechatLoginShouldReturn1303WhenWechatFails() throws Exception {
        Mockito.when(authService.wechatLogin(ArgumentMatchers.anyString()))
                .thenThrow(new BusinessException(ResultCode.WECHAT_LOGIN_FAILED));

        WechatLoginRequest request = new WechatLoginRequest();
        request.setCode("expired-or-forged-code");

        mockMvc.perform(post("/tsa/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.WECHAT_LOGIN_FAILED.getCode()))
                .andExpect(jsonPath("$.message").value("微信登录失败，请重试"));
    }

    // ---------- logout ----------

    @Test
    @DisplayName("POST /auth/logout - 无守卫幂等：Service 不抛异常即 200 且 data 为 null")
    void logoutShouldReturnIdempotent200() throws Exception {
        // void 方法 Mockito 默认不抛异常 = 「未登录幂等静默返回」的桩化身；
        // verify 防 Controller 将来被改成"忘了调 service 也返回 200"的假绿
        mockMvc.perform(post("/tsa/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));

        Mockito.verify(authService).logout();
    }
}