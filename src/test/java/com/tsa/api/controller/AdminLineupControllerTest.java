package com.tsa.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsa.api.common.BusinessException;
import com.tsa.api.common.GlobalExceptionHandler;
import com.tsa.api.common.ResultCode;
import com.tsa.api.dto.LineupSaveRequest;
import com.tsa.api.dto.LineupVO;
import com.tsa.api.service.FileStorageService;
import com.tsa.api.service.LineupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.InputStream;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AdminLineupController 测试（standalone MockMvc，不连库）：CRUD 委托 + Result 形状 + 1002 + 上传闸口。 */
class AdminLineupControllerTest {

    private LineupService lineupService;
    private FileStorageService fileStorageService;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        lineupService = Mockito.mock(LineupService.class);
        fileStorageService = Mockito.mock(FileStorageService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminLineupController(lineupService, fileStorageService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private LineupSaveRequest validRequest() {
        LineupSaveRequest req = new LineupSaveRequest();
        req.setMap("Mirage");
        req.setSite("警家");
        req.setKind("smoke");
        req.setName("T 一号位警家烟");
        req.setStyle("站位");
        req.setSteps(List.of("站位贴墙", "瞄参照点", "原地投"));
        req.setNote("双封烟第一颗");
        req.setImages(List.of("/tsa/files/0b1e.jpg"));
        return req;
    }

    @Test
    @DisplayName("GET /admin/lineups - 应返回数组形状，id 序列化为字符串")
    void listShouldReturnArrayShape() throws Exception {
        LineupVO vo = new LineupVO();
        vo.setId(1L);
        vo.setName("T 一号位警家烟");
        vo.setSteps(List.of("站位贴墙"));
        vo.setImages(List.of());
        Mockito.when(lineupService.listAll()).thenReturn(List.of(vo));

        mockMvc.perform(get("/tsa/admin/lineups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value("1"))
                .andExpect(jsonPath("$.data[0].name").value("T 一号位警家烟"));
    }

    @Test
    @DisplayName("POST /admin/lineups - 新增成功应返回字符串 id")
    void createShouldReturnStringId() throws Exception {
        Mockito.when(lineupService.create(ArgumentMatchers.any())).thenReturn(9L);

        mockMvc.perform(post("/tsa/admin/lineups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("9"));
    }

    @Test
    @DisplayName("POST /admin/lineups - 点位名为空应 400 且 message 含「点位名称」")
    void createShouldFailWhenNameBlank() throws Exception {
        LineupSaveRequest req = validRequest();
        req.setName("");

        mockMvc.perform(post("/tsa/admin/lineups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("点位名称")));
    }

    @Test
    @DisplayName("POST /admin/lineups - 道具类型不在枚举应 400 且 message 含「道具类型」")
    void createShouldFailWhenKindInvalid() throws Exception {
        LineupSaveRequest req = validRequest();
        req.setKind("grenade");

        mockMvc.perform(post("/tsa/admin/lineups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("道具类型")));
    }

    @Test
    @DisplayName("POST /admin/lineups - 步骤为空应 400 且 message 含「至少」")
    void createShouldFailWhenStepsEmpty() throws Exception {
        LineupSaveRequest req = validRequest();
        req.setSteps(List.of());

        mockMvc.perform(post("/tsa/admin/lineups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("至少")));
    }

    @Test
    @DisplayName("PUT /admin/lineups/{id} - id 不存在（Service 抛 1002）应返回业务错误码")
    void updateShouldReturn1002WhenNotFound() throws Exception {
        Mockito.doThrow(new BusinessException(ResultCode.DATA_NOT_FOUND))
                .when(lineupService).update(ArgumentMatchers.eq(999L), ArgumentMatchers.any());

        mockMvc.perform(put("/tsa/admin/lineups/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.DATA_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("DELETE /admin/lineups/{id} - id 不存在（Service 抛 1002）应返回业务错误码")
    void deleteShouldReturn1002WhenNotFound() throws Exception {
        Mockito.doThrow(new BusinessException(ResultCode.DATA_NOT_FOUND))
                .when(lineupService).delete(999L);

        mockMvc.perform(delete("/tsa/admin/lineups/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.DATA_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("POST /admin/lineups/image - 白名单类型应受理并回相对路径")
    void uploadShouldAcceptPngAndReturnPath() throws Exception {
        Mockito.when(fileStorageService.upload(ArgumentMatchers.anyString(),
                        ArgumentMatchers.anyString(), ArgumentMatchers.any(InputStream.class)))
                .thenReturn("/tsa/files/0b1e.png");

        mockMvc.perform(multipart("/tsa/admin/lineups/image")
                        .file(new MockMultipartFile("file", "lineup.png", "image/png", new byte[]{1, 2, 3})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.path").value("/tsa/files/0b1e.png"));
    }

    @Test
    @DisplayName("POST /admin/lineups/image - 非白名单类型应 400 同一句文案")
    void uploadShouldRejectDisallowedType() throws Exception {
        mockMvc.perform(multipart("/tsa/admin/lineups/image")
                        .file(new MockMultipartFile("file", "evil.html", "text/plain", new byte[]{1})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value("文件超限或类型不支持"));
    }
}
