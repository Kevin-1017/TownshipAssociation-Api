package com.tsa.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsa.api.common.BusinessException;
import com.tsa.api.common.GlobalExceptionHandler;
import com.tsa.api.common.ResultCode;
import com.tsa.api.dto.RewardCategorySaveRequest;
import com.tsa.api.service.FoundationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminFoundationController 测试（standalone MockMvc）：验写接口迁移后仍委托 FoundationService、Result 形状不变。
 *
 * <p>这些用例是从旧 FoundationControllerTest 的写接口用例随迁移一并搬过来的（路径改到 /tsa/admin/foundation/**）。
 */
class AdminFoundationControllerTest {

    private FoundationService foundationService;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        foundationService = Mockito.mock(FoundationService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminFoundationController(foundationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /admin/foundation/categories - 新增成功应返回字符串 id（形状与迁移前一致）")
    void createCategoryShouldReturnStringId() throws Exception {
        Mockito.when(foundationService.createCategory(ArgumentMatchers.any())).thenReturn(7L);
        RewardCategorySaveRequest req = new RewardCategorySaveRequest();
        req.setName("年度奖学金颁发");

        mockMvc.perform(post("/tsa/admin/foundation/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("7"));
    }

    @Test
    @DisplayName("POST /admin/foundation/categories - 名称为空应 400 且 message 含「奖项类别名称」")
    void createCategoryShouldFailWhenNameBlank() throws Exception {
        RewardCategorySaveRequest req = new RewardCategorySaveRequest();
        // name 故意不填
        req.setSponsor("陈某某");

        mockMvc.perform(post("/tsa/admin/foundation/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("奖项类别名称")));
    }

    @Test
    @DisplayName("POST /admin/foundation/donations - 新增成功应返回字符串 id（带时区日期入参）")
    void createDonationShouldReturnStringId() throws Exception {
        Mockito.when(foundationService.createDonation(ArgumentMatchers.any())).thenReturn(55L);
        String json = "{\"donorName\":\"坤坤\",\"amount\":2000000,\"donationDate\":\"2026-08-10T00:00:00+08:00\"}";

        mockMvc.perform(post("/tsa/admin/foundation/donations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("55"));
    }

    @Test
    @DisplayName("PUT /admin/foundation/records/{id} - 修改成功应 200 data=null 且委托 Service")
    void updateRecordShouldDelegate() throws Exception {
        String json = "{\"categoryId\":1,\"recipient\":\"张三\",\"amount\":5000}";

        mockMvc.perform(put("/tsa/admin/foundation/records/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Mockito.verify(foundationService).updateRecord(ArgumentMatchers.eq(3L), ArgumentMatchers.any());
    }

    @Test
    @DisplayName("DELETE /admin/foundation/categories/{id} - Service 抛 1002 应返回业务错误码")
    void deleteCategoryShouldReturn1002WhenNotFound() throws Exception {
        Mockito.doThrow(new BusinessException(ResultCode.DATA_NOT_FOUND))
                .when(foundationService).deleteCategory(999L);

        mockMvc.perform(delete("/tsa/admin/foundation/categories/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.DATA_NOT_FOUND.getCode()));
    }
}
