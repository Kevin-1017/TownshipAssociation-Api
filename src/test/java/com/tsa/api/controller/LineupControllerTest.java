package com.tsa.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsa.api.common.GlobalExceptionHandler;
import com.tsa.api.dto.LineupVO;
import com.tsa.api.service.LineupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** LineupController 测试（standalone MockMvc，不连库）：公开列表的形状与 id 字符串化。 */
class LineupControllerTest {

    private LineupService lineupService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lineupService = Mockito.mock(LineupService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new LineupController(lineupService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /lineups - 应返回数组形状，id 字符串化，steps/images 为数组")
    void listShouldReturnArrayShape() throws Exception {
        LineupVO vo = new LineupVO();
        vo.setId(1L);
        vo.setMap("Mirage");
        vo.setSite("警家");
        vo.setKind("smoke");
        vo.setName("T 一号位警家烟");
        vo.setStyle("站位");
        vo.setSteps(List.of("站位贴墙", "瞄参照点"));
        vo.setImages(List.of("/tsa/files/a.jpg"));
        Mockito.when(lineupService.listAll()).thenReturn(List.of(vo));

        mockMvc.perform(get("/tsa/lineups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value("1"))
                .andExpect(jsonPath("$.data[0].map").value("Mirage"))
                .andExpect(jsonPath("$.data[0].kind").value("smoke"))
                .andExpect(jsonPath("$.data[0].steps[0]").value("站位贴墙"))
                .andExpect(jsonPath("$.data[0].images[0]").value("/tsa/files/a.jpg"));
    }
}
