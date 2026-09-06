package com.tsa.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsa.api.common.BusinessException;
import com.tsa.api.common.GlobalExceptionHandler;
import com.tsa.api.common.ResultCode;
import com.tsa.api.dto.MemberSaveRequest;
import com.tsa.api.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MemberController 示例测试。
 *
 * <p>教学要点：用 standalone MockMvc 只测 Web 层（不连数据库、不启动 Spring 容器），
 * 跑得快且无环境依赖 —— CI 里跑这类测试不需要装 MySQL。
 * Service 用 Mockito 打桩，Controller 的行为即可独立验证。
 */
class MemberControllerTest {

    private MemberService memberService;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        memberService = Mockito.mock(MemberService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MemberController(memberService))
                .setControllerAdvice(new GlobalExceptionHandler())   // 挂上全局异常处理器
                .build();
    }

    @Test
    @DisplayName("POST /members - 姓名为空时应返回 400 和字段提示")
    void registerShouldFailWhenNameBlank() throws Exception {
        MemberSaveRequest request = new MemberSaveRequest();
        request.setOpenid("wx-openid-test");
        // name 故意不填
        request.setGender(1);
        request.setProvince("四川省");
        request.setCity("成都市");
        request.setLat(new java.math.BigDecimal("30.57"));
        request.setLng(new java.math.BigDecimal("104.06"));

        mockMvc.perform(post("/api/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("姓名")));
    }

    @Test
    @DisplayName("POST /members - openid 重复时应返回业务错误码 1001")
    void registerShouldReturnBizCodeWhenDuplicated() throws Exception {
        Mockito.when(memberService.register(ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ResultCode.MEMBER_ALREADY_EXISTS));

        MemberSaveRequest request = new MemberSaveRequest();
        request.setOpenid("wx-openid-test");
        request.setName("张三");
        request.setGender(1);
        request.setProvince("四川省");
        request.setCity("成都市");
        request.setLat(new java.math.BigDecimal("30.57"));
        request.setLng(new java.math.BigDecimal("104.06"));

        mockMvc.perform(post("/api/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.MEMBER_ALREADY_EXISTS.getCode()));
    }

    @Test
    @DisplayName("GET /members/map-data - 正常返回时应带 data 数组")
    void mapDataShouldReturnOk() throws Exception {
        Mockito.when(memberService.listMapMarkers()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/members/map-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }
}
