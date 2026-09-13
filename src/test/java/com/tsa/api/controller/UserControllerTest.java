package com.tsa.api.controller;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsa.api.common.GlobalExceptionHandler;
import com.tsa.api.common.ResultCode;
import com.tsa.api.dto.MemberDetailVO;
import com.tsa.api.dto.ProfileUpdateRequest;
import com.tsa.api.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserController 测试（契约 C2/C7，standalone MockMvc + mock AuthService）。
 *
 * <p><b>覆盖盲区声明（计划 v1.1 修订 gaps#17 / C17，本段是"写明"而非疏漏，删注释不会补上覆盖）</b>：
 * standalone 范式有意绕开 Sa-Token 拦截器与真实 StpUtil，以下三项依赖容器/运行时行为，
 * <b>在本类与整个 CI 均零覆盖</b>，仅靠计划 §9.B 的手工 curl 兜底：
 * <ul>
 *   <li>{@code /tsa/user/**} 的 SaRouter checkLogin → 401 壳（拦截器链根本不在本测试的 MVC 装配里，
 *       SaTokenConfig 是 WebMvcConfigurer，standaloneSetup 不加载）——§9.B 第 5 步裸 curl /tsa/user/me 验；</li>
 *   <li>assoc- 前缀令牌的显式拒绝（判定逻辑在 AuthServiceImpl.currentOpenid()，这里被 mock 掉了，
 *       只能桩"抛 NotLoginException"这一出口形态；真令牌冒充真会话走真逻辑）——§9.B 第 8 步双向验；</li>
 *   <li>1306 限频（AuthRateLimitInterceptor 未注册进 standalone MockMvc，且它挂在 /tsa/auth/**
 *       与本类无关，列在此处是提醒整条 auth 拦截链同理零覆盖）——§9.B 第 4 步连发验。</li>
 * </ul>
 * 本类能证明的：Controller 零逻辑透传 + Result 包装形状 + GlobalExceptionHandler 把
 * NotLoginException 翻译成 401 壳（Advice 层是真挂的）。
 */
class UserControllerTest {

    private AuthService authService;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        authService = Mockito.mock(AuthService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /** 构造与 AuthServiceImpl.currentOpenid() 同款异常（type 不同仅语义标注差异，401 壳结果一致） */
    private static NotLoginException notLogin(String type) {
        return new NotLoginException(NotLoginException.DEFAULT_MESSAGE, StpUtil.TYPE, type);
    }

    @Test
    @DisplayName("GET /user/me - 未登录（Service 抛 NotLoginException）应翻译成 401 壳")
    void meShouldReturn401ShellWhenNotLogin() throws Exception {
        Mockito.when(authService.currentUser())
                .thenThrow(notLogin(NotLoginException.NOT_TOKEN));

        mockMvc.perform(get("/tsa/user/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.UNAUTHORIZED.getCode()))
                .andExpect(jsonPath("$.message").value("未登录或登录已过期"));
    }

    @Test
    @DisplayName("GET /user/me - 已登录未建档（Service 返回 null）应 200 且 data 为 null（契约 C2）")
    void meShouldReturnNullDataWhenNoProfile() throws Exception {
        Mockito.when(authService.currentUser()).thenReturn(null);

        mockMvc.perform(get("/tsa/user/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @DisplayName("GET /user/me - 有档案应 200 全量 MemberDetailVO，本人视角不裁 phone/wechatId")
    void meShouldReturnFullProfileWithoutContactTrim() throws Exception {
        MemberDetailVO vo = new MemberDetailVO();
        vo.setId(7L);
        vo.setName("张三");
        vo.setPhone("13800138000");
        vo.setWechatId("wx_zhangsan");
        vo.setCountry("中国");
        Mockito.when(authService.currentUser()).thenReturn(vo);

        // 与 GET /tsa/members/{id}（第三方视角 contactVisible=false 剔除联系方式）刻意相反：
        // 本人看自己不泄露，phone/wechatId 必须原样在场
        mockMvc.perform(get("/tsa/user/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("7"))
                .andExpect(jsonPath("$.data.name").value("张三"))
                .andExpect(jsonPath("$.data.phone").value("13800138000"))
                .andExpect(jsonPath("$.data.wechatId").value("wx_zhangsan"))
                .andExpect(jsonPath("$.data.country").value("中国"));
    }

    @Test
    @DisplayName("PUT /user/profile - 请求体应原样透传给 Service，响应为更新后的档案（契约 C7）")
    void updateProfileShouldPassRequestThroughAndReturnLatest() throws Exception {
        MemberDetailVO updated = new MemberDetailVO();
        updated.setId(8L);
        updated.setName("李四");
        updated.setMajor("计算机科学与技术");
        Mockito.when(authService.updateOwnProfile(ArgumentMatchers.any())).thenReturn(updated);

        mockMvc.perform(put("/tsa/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"李四\",\"major\":\"计算机科学与技术\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("8"))
                .andExpect(jsonPath("$.data.name").value("李四"))
                .andExpect(jsonPath("$.data.major").value("计算机科学与技术"));

        // 透传核验：Controller 不许偷改/丢字段（openid 不在请求体形状里，身份推导在 Service）
        ArgumentCaptor<ProfileUpdateRequest> captor = ArgumentCaptor.forClass(ProfileUpdateRequest.class);
        Mockito.verify(authService).updateOwnProfile(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("李四", captor.getValue().getName());
        org.junit.jupiter.api.Assertions.assertEquals("计算机科学与技术", captor.getValue().getMajor());
    }

    @Test
    @DisplayName("PUT /user/profile - 乡会令牌冒充/未登录（Service 抛 NotLoginException）应 401 壳")
    void updateProfileShouldReturn401WhenRejected() throws Exception {
        // INVALID_TOKEN 对应 assoc- 前缀拒绝那一支（真实判定在 currentOpenid，见类头盲区声明）
        Mockito.when(authService.updateOwnProfile(ArgumentMatchers.any()))
                .thenThrow(notLogin(NotLoginException.INVALID_TOKEN));

        mockMvc.perform(put("/tsa/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"李四\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @DisplayName("PUT /user/profile - 届别越界应 400（@Valid 在 Controller 层就拦住，不打到 Service）")
    void updateProfileShouldFailWhenGraduationYearOutOfRange() throws Exception {
        mockMvc.perform(put("/tsa/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"graduationYear\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("届别")));

        Mockito.verifyNoInteractions(authService);
    }
}
