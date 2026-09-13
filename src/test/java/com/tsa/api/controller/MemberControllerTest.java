package com.tsa.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsa.api.common.BusinessException;
import com.tsa.api.common.GlobalExceptionHandler;
import com.tsa.api.common.ResultCode;
import com.tsa.api.dto.MemberSaveRequest;
import com.tsa.api.service.AuthService;
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
    private AuthService authService;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        memberService = Mockito.mock(MemberService.class);
        authService = Mockito.mock(AuthService.class);
        // B16：注册所需 openid 由服务端从 Bearer 推导（Controller 注入 AuthService），
        // standalone 范式下 StpUtil 不存在，桩一个固定 loginId 即可
        Mockito.when(authService.currentOpenid()).thenReturn("wx-openid-test");
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MemberController(memberService, authService))
                .setControllerAdvice(new GlobalExceptionHandler())   // 挂上全局异常处理器
                .build();
    }

    @Test
    @DisplayName("POST /members - 姓名为空时应返回 400 和字段提示")
    void registerShouldFailWhenNameBlank() throws Exception {
        MemberSaveRequest request = new MemberSaveRequest();
        // name 故意不填
        request.setGender(1);
        request.setProvince("四川省");
        request.setCity("成都市");
        request.setLat(new java.math.BigDecimal("30.57"));
        request.setLng(new java.math.BigDecimal("104.06"));

        mockMvc.perform(post("/tsa/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("姓名")));
    }

    @Test
    @DisplayName("POST /members - openid 重复时应返回业务错误码 1001")
    void registerShouldReturnBizCodeWhenDuplicated() throws Exception {
        Mockito.when(memberService.register(ArgumentMatchers.any(), ArgumentMatchers.anyString()))
                .thenThrow(new BusinessException(ResultCode.MEMBER_ALREADY_EXISTS));

        MemberSaveRequest request = new MemberSaveRequest();
        request.setName("张三");
        request.setGender(1);
        request.setProvince("四川省");
        request.setCity("成都市");
        request.setLat(new java.math.BigDecimal("30.57"));
        request.setLng(new java.math.BigDecimal("104.06"));

        mockMvc.perform(post("/tsa/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.MEMBER_ALREADY_EXISTS.getCode()));
    }

    @Test
    @DisplayName("GET /members - 分页响应应为契约形状 data.list/total/page/pageSize")
    void pageShouldReturnContractShape() throws Exception {
        com.tsa.api.dto.MemberVO vo = new com.tsa.api.dto.MemberVO();
        vo.setId(1L);
        vo.setName("张三");
        com.tsa.api.dto.PageVO<com.tsa.api.dto.MemberVO> pageVO =
                new com.tsa.api.dto.PageVO<>(java.util.List.of(vo), 1L, 1L, 20L);
        Mockito.when(memberService.pageQuery(ArgumentMatchers.any())).thenReturn(pageVO);

        mockMvc.perform(get("/tsa/members").param("page", "1").param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.list[0].id").value("1"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                // 公开 VO 不允许出现登录凭证字段
                .andExpect(jsonPath("$.data.list[0].openid").doesNotExist());
    }

    @Test
    @DisplayName("POST /members - 注册成功应返回字符串形式的新成员 id")
    void registerShouldReturnStringId() throws Exception {
        com.tsa.api.entity.Member saved = new com.tsa.api.entity.Member();
        saved.setId(123L);
        Mockito.when(memberService.register(ArgumentMatchers.any(), ArgumentMatchers.anyString()))
                .thenReturn(saved);

        MemberSaveRequest request = new MemberSaveRequest();
        request.setName("张三");
        request.setGender(1);
        request.setProvince("四川省");
        request.setCity("成都市");
        request.setLat(new java.math.BigDecimal("30.57"));
        request.setLng(new java.math.BigDecimal("104.06"));

        mockMvc.perform(post("/tsa/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("123"));
    }

    @Test
    @DisplayName("GET /members/map-data - 正常返回时应带 data 数组")
    void mapDataShouldReturnOk() throws Exception {
        Mockito.when(memberService.listMapMarkers()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/tsa/members/map-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /members/stats/province - 应原样透传 [{province,count}] 列表（契约 C4）")
    void statsProvinceShouldReturnContractList() throws Exception {
        // 降序、仅 status=1 的口径是 Service/SQL 的事（listMaps+groupBy），standalone 层只钉
        // 「Controller 不改形状、按序透传」——顺序断言同时防住将来有人在 Controller 里画蛇添足地重排
        Mockito.when(memberService.listProvinceStats()).thenReturn(java.util.List.of(
                new com.tsa.api.dto.ProvinceStatVO("广东省", 12L),
                new com.tsa.api.dto.ProvinceStatVO("四川省", 5L)));

        mockMvc.perform(get("/tsa/members/stats/province"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].province").value("广东省"))
                .andExpect(jsonPath("$.data[0].count").value(12))
                .andExpect(jsonPath("$.data[1].province").value("四川省"))
                .andExpect(jsonPath("$.data[1].count").value(5));
    }

    @Test
    @DisplayName("GET /members/{id} - 带 X-Assoc-Token 时应返回详情数据")
    void detailShouldReturnOkWithAssocToken() throws Exception {
        com.tsa.api.dto.MemberDetailVO vo = new com.tsa.api.dto.MemberDetailVO();
        vo.setId(1L);
        vo.setName("张三");
        vo.setContactVisible(true);
        Mockito.when(memberService.getDetail(ArgumentMatchers.eq(1L), ArgumentMatchers.anyString()))
                .thenReturn(vo);

        mockMvc.perform(get("/tsa/members/1").header("X-Assoc-Token", "mock-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("张三"))
                .andExpect(jsonPath("$.data.id").value("1"));
    }

    @Test
    @DisplayName("GET /members/{id} - 未核验（Service 抛 1301）时应返回业务错误码 1301")
    void detailShouldReturn1301WhenNotAssoc() throws Exception {
        // 用 any() 而非 anyString()：不带 header 时 assocToken 是 null，anyString 匹配不上
        Mockito.when(memberService.getDetail(ArgumentMatchers.eq(1L), ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ResultCode.ASSOC_MEMBER_ONLY));

        mockMvc.perform(get("/tsa/members/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.ASSOC_MEMBER_ONLY.getCode()));
    }

    @Test
    @DisplayName("GET /members/{id} - 成员不存在（Service 抛 1002）时应返回业务错误码 1002")
    void detailShouldReturn1002WhenNotFound() throws Exception {
        Mockito.when(memberService.getDetail(ArgumentMatchers.eq(999L), ArgumentMatchers.anyString()))
                .thenThrow(new BusinessException(ResultCode.DATA_NOT_FOUND));

        mockMvc.perform(get("/tsa/members/999").header("X-Assoc-Token", "mock-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.DATA_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("GET /members/{id} - contactVisible=false 时响应不得出现 phone/wechatId 字段（剔除而非 null）")
    void detailShouldOmitContactFieldsWhenInvisible() throws Exception {
        com.tsa.api.dto.MemberDetailVO vo = new com.tsa.api.dto.MemberDetailVO();
        vo.setId(2L);
        vo.setName("李四");
        vo.setContactVisible(false);
        vo.setPhone(null);
        vo.setWechatId(null);
        Mockito.when(memberService.getDetail(ArgumentMatchers.eq(2L), ArgumentMatchers.anyString()))
                .thenReturn(vo);

        mockMvc.perform(get("/tsa/members/2").header("X-Assoc-Token", "mock-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contactVisible").value(false))
                .andExpect(jsonPath("$.data.phone").doesNotExist())
                .andExpect(jsonPath("$.data.wechatId").doesNotExist());
    }
}
