package com.tsa.api.controller;

import com.tsa.api.common.BusinessException;
import com.tsa.api.common.GlobalExceptionHandler;
import com.tsa.api.common.ResultCode;
import com.tsa.api.dto.EventDetailVO;
import com.tsa.api.dto.EventListVO;
import com.tsa.api.dto.EventQuery;
import com.tsa.api.dto.PageVO;
import com.tsa.api.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * EventController 测试（契约 C5/C6，v1.2 D2「跳公众号文章」形态）。
 *
 * <p>模板照抄 MemberControllerTest：standalone MockMvc 只测 Web 层，Service 打桩。
 * 分页钳制（pageSize 1..50）、year 校验、status 派生等业务口径在 EventServiceImpl，
 * 本类钉的是<b>响应形状</b>：PageVO 契约键、id 字符串化、可空键在场（C5/C6 的
 * cover/summary/articleUrl 为 null 时键不能消失——两个 VO 类上都没有 @JsonInclude，
 * 用原文子串断言锁死）、content 字段永不出现（正文留在公众号）。
 * startTime 只断言键在场：带时区的 ISO 8601 格式由应用装配的 JacksonConfig 决定，
 * standalone 不加载该 Bean，断死格式反而是测了个假环境。
 */
class EventControllerTest {

    private EventService eventService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        eventService = Mockito.mock(EventService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new EventController(eventService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private EventListVO sampleListVO() {
        EventListVO vo = new EventListVO();
        vo.setId(5L);
        vo.setTitle("成都老乡下午茶");
        vo.setCover(null);          // 秘书处还没传封面的常态：键必须在场
        vo.setSummary("茶馆小聚");
        vo.setStartTime(LocalDateTime.of(2026, 10, 1, 14, 0, 0));
        vo.setStatus("upcoming");
        return vo;
    }

    @Test
    @DisplayName("GET /events - 应返回 PageVO 契约形状 list/total/page/pageSize 与列表项字段")
    void pageShouldReturnContractShape() throws Exception {
        Mockito.when(eventService.pageQuery(ArgumentMatchers.any()))
                .thenReturn(new PageVO<>(List.of(sampleListVO()), 30L, 1L, 10L));

        mockMvc.perform(get("/tsa/events").param("page", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(30))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.list[0].id").value("5"))
                .andExpect(jsonPath("$.data.list[0].title").value("成都老乡下午茶"))
                .andExpect(jsonPath("$.data.list[0].status").value("upcoming"))
                .andExpect(jsonPath("$.data.list[0].startTime").exists())
                // 正文永不外给：字段在 VO 形状上根本不存在（D2 拍板），键出现即为回归
                .andExpect(jsonPath("$.data.list[0].content").doesNotExist())
                // cover=null：键必须在场（前端据「键在值 null」走占位图，缺键=结构异常）
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"cover\":null")));
    }

    @Test
    @DisplayName("GET /events - page/pageSize/year 应原样绑定进 EventQuery 透传给 Service（C5）")
    void pageShouldBindQueryParamsAndPassThrough() throws Exception {
        Mockito.when(eventService.pageQuery(ArgumentMatchers.any()))
                .thenReturn(new PageVO<>(List.<EventListVO>of(), 0L, 2L, 5L));

        mockMvc.perform(get("/tsa/events").param("page", "2").param("pageSize", "5").param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<EventQuery> captor = ArgumentCaptor.forClass(EventQuery.class);
        Mockito.verify(eventService).pageQuery(captor.capture());
        assertEquals(2, captor.getValue().getPage());
        assertEquals(5, captor.getValue().getPageSize());
        assertEquals(2026, captor.getValue().getYear());
    }

    @Test
    @DisplayName("GET /events - 不带 year 时应为 null（服务端过滤是可选维度，缺省不过滤）")
    void pageShouldPassNullYearWhenAbsent() throws Exception {
        Mockito.when(eventService.pageQuery(ArgumentMatchers.any()))
                .thenReturn(new PageVO<>(List.<EventListVO>of(), 0L, 1L, 10L));

        mockMvc.perform(get("/tsa/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list").isArray());

        ArgumentCaptor<EventQuery> captor = ArgumentCaptor.forClass(EventQuery.class);
        Mockito.verify(eventService).pageQuery(captor.capture());
        // 无参请求走 DTO 的字段默认值（page=1/pageSize=10），year 缺省 null
        assertEquals(1, captor.getValue().getPage());
        assertEquals(10, captor.getValue().getPageSize());
        org.junit.jupiter.api.Assertions.assertNull(captor.getValue().getYear());
    }

    @Test
    @DisplayName("GET /events/{id} - 详情应含 articleUrl 键（值可为 null=秘书处未补链接的过渡态）")
    void detailShouldReturnArticleUrlKey() throws Exception {
        EventDetailVO vo = new EventDetailVO();
        vo.setId(5L);
        vo.setTitle("成都老乡下午茶");
        vo.setCover("/tsa/files/0b1e5c2f-9a3d-4e7c-8f21-6d0a4b3c5e77.jpg");
        vo.setSummary("茶馆小聚");
        vo.setStartTime(LocalDateTime.of(2026, 10, 1, 14, 0, 0));
        vo.setArticleUrl(null);
        Mockito.when(eventService.detail(5L)).thenReturn(vo);

        mockMvc.perform(get("/tsa/events/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("5"))
                .andExpect(jsonPath("$.data.title").value("成都老乡下午茶"))
                .andExpect(jsonPath("$.data.cover").value("/tsa/files/0b1e5c2f-9a3d-4e7c-8f21-6d0a4b3c5e77.jpg"))
                .andExpect(jsonPath("$.data.startTime").exists())
                .andExpect(jsonPath("$.data.content").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"articleUrl\":null")));
    }

    @Test
    @DisplayName("GET /events/{id} - 查无（Service 抛 1002）时应返回业务错误码 1002")
    void detailShouldReturn1002WhenNotFound() throws Exception {
        Mockito.when(eventService.detail(999L))
                .thenThrow(new BusinessException(ResultCode.DATA_NOT_FOUND));

        mockMvc.perform(get("/tsa/events/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.DATA_NOT_FOUND.getCode()));
    }
}
