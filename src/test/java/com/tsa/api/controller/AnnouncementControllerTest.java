package com.tsa.api.controller;

import com.tsa.api.common.BusinessException;
import com.tsa.api.common.GlobalExceptionHandler;
import com.tsa.api.common.ResultCode;
import com.tsa.api.dto.NoticeVO;
import com.tsa.api.service.AnnouncementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AnnouncementController 测试（对外路径 /tsa/notices）。
 *
 * <p>模板照抄 MemberControllerTest：standalone MockMvc 只测 Web 层，
 * Service 用 Mockito 打桩，不连数据库、不起 Spring 容器。
 */
class AnnouncementControllerTest {

    private AnnouncementService announcementService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        announcementService = Mockito.mock(AnnouncementService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AnnouncementController(announcementService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private NoticeVO sample() {
        NoticeVO vo = new NoticeVO();
        vo.setId(1L);
        vo.setTitle("乡会 2026 年度恳亲大会筹备启动");
        vo.setSummary("筹备组已成立，志愿者报名通道开启。");
        vo.setContent("各位老乡……");
        vo.setPinned(true);
        vo.setPublishedAt(LocalDateTime.of(2026, 8, 20, 10, 0, 0));
        return vo;
    }

    @Test
    @DisplayName("GET /notices - 列表应返回 data 数组，字段为契约形状")
    void listShouldReturnContractShape() throws Exception {
        Mockito.when(announcementService.listForHome()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/tsa/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value("1"))
                .andExpect(jsonPath("$.data[0].pinned").value(true))
                .andExpect(jsonPath("$.data[0].summary").isString());
    }

    @Test
    @DisplayName("GET /notices/{id} - 详情正常返回")
    void detailShouldReturnOk() throws Exception {
        Mockito.when(announcementService.getDetail(1L)).thenReturn(sample());

        mockMvc.perform(get("/tsa/notices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").isString());
    }

    @Test
    @DisplayName("GET /notices/{id} - 不存在时应返回业务错误码 1002")
    void detailShouldReturnBizCodeWhenNotFound() throws Exception {
        Mockito.when(announcementService.getDetail(999L))
                .thenThrow(new BusinessException(ResultCode.DATA_NOT_FOUND));

        mockMvc.perform(get("/tsa/notices/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.DATA_NOT_FOUND.getCode()));
    }
}
