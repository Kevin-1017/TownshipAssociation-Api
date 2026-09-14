package com.tsa.api.controller;

import com.tsa.api.common.BusinessException;
import com.tsa.api.common.GlobalExceptionHandler;
import com.tsa.api.common.ResultCode;
import com.tsa.api.dto.EventAdminQuery;
import com.tsa.api.dto.EventListVO;
import com.tsa.api.dto.EventSaveRequest;
import com.tsa.api.dto.PageVO;
import com.tsa.api.service.EventService;
import com.tsa.api.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminEventController standalone MockMvc 测试（不连数据库，Service/存储全打桩）。
 *
 * <p>钉的是 Web 层契约形状：query 绑定透传、@Valid 校验（标题必填、startTime 必填）、
 * 1002 业务码壳、封面上传三态（成功回路径 / 非法类型 400 / 超限 400）。
 * 「删除即下架」的落库语义在 EventServiceImpl，standalone 测不到，靠服务层单测兜（本期从简，与 notices 同口径）。
 */
class AdminEventControllerTest {

    private EventService eventService;
    private FileStorageService fileStorageService;
    private MockMvc mockMvc;

    /**
     * 合法事件请求体：startTime 用带时区 ISO 串（与 EventSaveRequest 的 @JsonFormat pattern 对齐）。
     * 不走 ObjectMapper 序列化——LocalDateTime 没有 offset 字段，Jackson 能按 XXX 解析、却不能按 XXX 输出，
     * 手写串既贴真实前端契约又绕开这个不对称。
     */
    private static final String SAVE_JSON = "{\"title\":\"乡会 2026 年度恳亲大会\","
            + "\"cover\":\"/tsa/files/0b1e5c2f-9a3d-4e7c-8f21-6d0a4b3c5e77.jpg\","
            + "\"summary\":\"定档下月\",\"articleUrl\":\"https://mp.weixin.qq.com/s/abc\","
            + "\"startTime\":\"2026-10-01T14:00:00+08:00\"}";

    @BeforeEach
    void setUp() {
        eventService = Mockito.mock(EventService.class);
        fileStorageService = Mockito.mock(FileStorageService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminEventController(eventService, fileStorageService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private EventListVO sampleVO() {
        EventListVO vo = new EventListVO();
        vo.setId(1L);
        vo.setTitle("乡会 2026 年度恳亲大会");
        vo.setCover(null);
        vo.setSummary("定档下月");
        vo.setArticleUrl("https://mp.weixin.qq.com/s/abc");
        vo.setStartTime(LocalDateTime.of(2026, 10, 1, 14, 0));
        vo.setStatus("upcoming");
        return vo;
    }

    @Test
    @DisplayName("GET /admin/events - keyword 应绑定进 EventAdminQuery 透传，出参为 PageVO 形状")
    void pageShouldBindAndReturnShape() throws Exception {
        Mockito.when(eventService.adminPageQuery(ArgumentMatchers.any()))
                .thenReturn(new PageVO<>(List.of(sampleVO()), 1L, 1L, 10L));

        mockMvc.perform(get("/tsa/admin/events").param("keyword", "恳亲").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list[0].id").value("1"))
                .andExpect(jsonPath("$.data.list[0].articleUrl").value("https://mp.weixin.qq.com/s/abc"));

        ArgumentCaptor<EventAdminQuery> captor = ArgumentCaptor.forClass(EventAdminQuery.class);
        Mockito.verify(eventService).adminPageQuery(captor.capture());
        assertEquals("恳亲", captor.getValue().getKeyword());
        assertEquals(2, captor.getValue().getPage());
    }

    @Test
    @DisplayName("POST /admin/events - 合法请求应透传并回字符串 id")
    void createShouldReturnStringId() throws Exception {
        Mockito.when(eventService.create(ArgumentMatchers.any())).thenReturn(88L);

        mockMvc.perform(post("/tsa/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SAVE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("88"));

        ArgumentCaptor<EventSaveRequest> captor = ArgumentCaptor.forClass(EventSaveRequest.class);
        Mockito.verify(eventService).create(captor.capture());
        assertEquals(LocalDateTime.of(2026, 10, 1, 14, 0), captor.getValue().getStartTime());
    }

    @Test
    @DisplayName("POST /admin/events - 标题为空应 400 并带字段提示")
    void createShouldFailWhenTitleBlank() throws Exception {
        mockMvc.perform(post("/tsa/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SAVE_JSON.replace("乡会 2026 年度恳亲大会", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("标题")));
        Mockito.verify(eventService, Mockito.never()).create(ArgumentMatchers.any());
    }

    @Test
    @DisplayName("POST /tsa/admin/events - startTime 缺失应 400（排序/区间依赖它，必填钉死）")
    void createShouldFailWithoutStartTime() throws Exception {
        mockMvc.perform(post("/tsa/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SAVE_JSON.replace(",\"startTime\":\"2026-10-01T14:00:00+08:00\"", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("开始时间")));
    }

    @Test
    @DisplayName("PUT /admin/events/{id} - 应把路径 id 与请求体一并透传 Service")
    void updateShouldPassIdAndBody() throws Exception {
        mockMvc.perform(put("/tsa/admin/events/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SAVE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<EventSaveRequest> captor = ArgumentCaptor.forClass(EventSaveRequest.class);
        Mockito.verify(eventService).update(ArgumentMatchers.eq(7L), captor.capture());
        assertEquals("乡会 2026 年度恳亲大会", captor.getValue().getTitle());
    }

    @Test
    @DisplayName("DELETE /admin/events/{id} - 不存在（Service 抛 1002）应回业务码壳")
    void deleteShouldReturn1002WhenMissing() throws Exception {
        Mockito.doThrow(new BusinessException(ResultCode.DATA_NOT_FOUND))
                .when(eventService).delete(999L);

        mockMvc.perform(delete("/tsa/admin/events/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.DATA_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("POST /admin/events/cover - 白名单类型应存盘并回 /tsa/files/<uuid>.<ext>")
    void coverUploadShouldReturnPath() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "c.png", "image/png", new byte[]{1, 2, 3});
        Mockito.when(fileStorageService.upload(ArgumentMatchers.anyString(),
                        ArgumentMatchers.eq("image/png"), ArgumentMatchers.any()))
                .thenAnswer(inv -> "/tsa/files/" + inv.getArgument(0, String.class));

        mockMvc.perform(multipart("/tsa/admin/events/cover").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.path").value(
                        org.hamcrest.Matchers.matchesPattern("^/tsa/files/[0-9a-f-]{36}\\.png$")));
    }

    @Test
    @DisplayName("POST /admin/events/cover - 类型不在白名单应回统一 400 文案且不落盘")
    void coverUploadShouldRejectBadType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "x.gif", "image/gif", new byte[]{1});

        mockMvc.perform(multipart("/tsa/admin/events/cover").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value("文件超限或类型不支持"));
        Mockito.verifyNoInteractions(fileStorageService);
    }
}
