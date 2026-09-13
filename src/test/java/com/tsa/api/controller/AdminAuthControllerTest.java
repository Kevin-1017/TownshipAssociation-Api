package com.tsa.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsa.api.common.BusinessException;
import com.tsa.api.common.GlobalExceptionHandler;
import com.tsa.api.common.ResultCode;
import com.tsa.api.dto.AdminCurrentUserVO;
import com.tsa.api.dto.AdminLoginRequest;
import com.tsa.api.dto.AdminLoginVO;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理后台身份接口测试（standalone MockMvc，模板照抄 AuthControllerTest）。
 *
 * <p>admin-login/logout 在 AuthController（公开路径），/me 在 AdminAuthController（admin 墙内），
 * 故本类挂两套 standalone：登录/注销验 admin-login 契约（成功/1307/400），/me 验只读会话回显。
 * StpUtil 只存在于 AdminAuthServiceImpl 内部，standalone 层 mock 掉 service 即可，不受 Sa-Token 环境影响；
 * checkRole("admin") 的闸门本身在 SaTokenConfig 拦截器，非 standalone 覆盖范围（属集成/手工回归）。
 */
class AdminAuthControllerTest {

    private AdminAuthService adminAuthService;
    private MockMvc authMvc;
    private MockMvc adminMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        adminAuthService = Mockito.mock(AdminAuthService.class);
        AuthService authService = Mockito.mock(AuthService.class);
        authMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService, adminAuthService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        adminMvc = MockMvcBuilders
                .standaloneSetup(new AdminAuthController(adminAuthService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /auth/admin-login - 成功应 200 且 data={token,username,role}")
    void adminLoginShouldReturnTokenShape() throws Exception {
        Mockito.when(adminAuthService.login(ArgumentMatchers.any()))
                .thenReturn(AdminLoginVO.of("admin-token-xyz", "admin", 1));

        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("admin");
        request.setPassword("whatever");

        authMvc.perform(post("/tsa/auth/admin-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("admin-token-xyz"))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.role").value(1));
    }

    @Test
    @DisplayName("POST /auth/admin-login - 账号不存在/密码错（Service 抛 1307）应回 1307 且不泄露区分线索")
    void adminLoginShouldReturn1307OnFailure() throws Exception {
        Mockito.when(adminAuthService.login(ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ResultCode.ADMIN_LOGIN_FAILED));

        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("admin");
        request.setPassword("wrong-password");

        authMvc.perform(post("/tsa/auth/admin-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.ADMIN_LOGIN_FAILED.getCode()))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("POST /auth/admin-login - 用户名为空应 400 且 message 含「用户名」")
    void adminLoginShouldFailWhenUsernameBlank() throws Exception {
        AdminLoginRequest request = new AdminLoginRequest();
        // username 故意不填
        request.setPassword("whatever");

        authMvc.perform(post("/tsa/auth/admin-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("用户名")));
    }

    @Test
    @DisplayName("POST /auth/admin-logout - 无守卫幂等：Service 不抛异常即 200 data=null")
    void adminLogoutShouldBeIdempotent() throws Exception {
        authMvc.perform(post("/tsa/auth/admin-logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));

        Mockito.verify(adminAuthService).logout();
    }

    @Test
    @DisplayName("GET /admin/auth/me - 应从会话回显 {username,role}")
    void meShouldReturnPrincipal() throws Exception {
        Mockito.when(adminAuthService.currentUser()).thenReturn(AdminCurrentUserVO.of("admin", 1));

        adminMvc.perform(get("/tsa/admin/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.role").value(1));
    }
}
