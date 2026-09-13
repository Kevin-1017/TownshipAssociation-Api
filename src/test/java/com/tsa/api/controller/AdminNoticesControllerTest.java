package com.tsa.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsa.api.common.BusinessException;
import com.tsa.api.common.GlobalExceptionHandler;
import com.tsa.api.common.ResultCode;
import com.tsa.api.dto.NoticeSaveRequest;
import com.tsa.api.dto.NoticeVO;
import com.tsa.api.service.AnnouncementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AdminNoticesController 测试（standalone MockMvc，不连库）：CRUD 委托 + Result 形状 + 1002。 */
class AdminNoticesControllerTest {

    private AnnouncementService announcementService;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        announcementService = Mockito.mock(AnnouncementService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminNoticesController(announcementService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private NoticeSaveRequest validRequest() {
        NoticeSaveRequest req = new NoticeSaveRequest();
        req.setTitle("年度会员大会通知");
        req.setSummary("定于下月召开");
        req.setContent("各位会员：……");
        req.setPinned(Boolean.TRUE);
        // publishedAt 留空：新增由 Service 兜底当前时间；裸 ObjectMapper 也不涉 LocalDateTime 序列化
        return req;
    }

    @Test
    @DisplayName("GET /admin/notices - 应返回数组形状，id 序列化为字符串")
    void listShouldReturnArrayShape() throws Exception {
        NoticeVO vo = new NoticeVO();
        vo.setId(1L);
        vo.setTitle("年度会员大会通知");
        vo.setPinned(true);
        Mockito.when(announcementService.listAllForAdmin()).thenReturn(List.of(vo));

        mockMvc.perform(get("/tsa/admin/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value("1"))
                .andExpect(jsonPath("$.data[0].title").value("年度会员大会通知"))
                .andExpect(jsonPath("$.data[0].pinned").value(true));
    }

    @Test
    @DisplayName("POST /admin/notices - 新增成功应返回字符串 id")
    void createShouldReturnStringId() throws Exception {
        Mockito.when(announcementService.create(ArgumentMatchers.any())).thenReturn(9L);

        mockMvc.perform(post("/tsa/admin/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("9"));
    }

    @Test
    @DisplayName("POST /admin/notices - 标题为空应 400 且 message 含「标题」")
    void createShouldFailWhenTitleBlank() throws Exception {
        NoticeSaveRequest req = validRequest();
        req.setTitle("");

        mockMvc.perform(post("/tsa/admin/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("标题")));
    }

    @Test
    @DisplayName("PUT /admin/notices/{id} - id 不存在（Service 抛 1002）应返回业务错误码")
    void updateShouldReturn1002WhenNotFound() throws Exception {
        Mockito.doThrow(new BusinessException(ResultCode.DATA_NOT_FOUND))
                .when(announcementService).update(ArgumentMatchers.eq(999L), ArgumentMatchers.any());

        mockMvc.perform(put("/tsa/admin/notices/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.DATA_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("DELETE /admin/notices/{id} - id 不存在（Service 抛 1002）应返回业务错误码")
    void deleteShouldReturn1002WhenNotFound() throws Exception {
        Mockito.doThrow(new BusinessException(ResultCode.DATA_NOT_FOUND))
                .when(announcementService).delete(999L);

        mockMvc.perform(delete("/tsa/admin/notices/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.DATA_NOT_FOUND.getCode()));
    }
}
