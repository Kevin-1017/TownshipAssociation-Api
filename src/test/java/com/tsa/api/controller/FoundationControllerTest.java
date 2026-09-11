package com.tsa.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsa.api.common.BusinessException;
import com.tsa.api.common.GlobalExceptionHandler;
import com.tsa.api.common.ResultCode;
import com.tsa.api.dto.DonationItemVO;
import com.tsa.api.dto.DonationRecordVO;
import com.tsa.api.dto.FoundationHomeVO;
import com.tsa.api.dto.RewardCategorySaveRequest;
import com.tsa.api.dto.RewardItemVO;
import com.tsa.api.dto.RewardRecordsVO;
import com.tsa.api.service.FoundationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FoundationController standalone MockMvc 测试（不连数据库）。 */
class FoundationControllerTest {

    private FoundationService foundationService;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        foundationService = Mockito.mock(FoundationService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FoundationController(foundationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /foundation - 首页应返回 rewards/donations 两数组，金额按数字、id 按字符串")
    void homeShouldReturnContractShape() throws Exception {
        RewardItemVO r = new RewardItemVO();
        r.setId(1L);
        r.setLabel("年度奖学金颁发");
        r.setAmount(50000L);
        r.setSponsor("陈某某");
        DonationItemVO d = new DonationItemVO();
        d.setId(2L);
        d.setDonorName("坤坤");
        d.setAmount(2000000L);
        d.setDate(LocalDateTime.of(2026, 8, 10, 0, 0));
        FoundationHomeVO home = new FoundationHomeVO();
        home.setRewards(List.of(r));
        home.setDonations(List.of(d));
        Mockito.when(foundationService.home()).thenReturn(home);

        mockMvc.perform(get("/tsa/foundation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rewards[0].id").value("1"))
                .andExpect(jsonPath("$.data.rewards[0].label").value("年度奖学金颁发"))
                .andExpect(jsonPath("$.data.donations[0].amount").value(2000000));
    }

    @Test
    @DisplayName("GET /foundation/rewards - 应返回 categories + records")
    void rewardsShouldReturnShape() throws Exception {
        RewardRecordsVO vo = new RewardRecordsVO();
        vo.setCategories(List.of("年度奖学金颁发"));
        vo.setRecords(List.of());
        Mockito.when(foundationService.rewards()).thenReturn(vo);

        mockMvc.perform(get("/tsa/foundation/rewards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories[0]").value("年度奖学金颁发"))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("GET /foundation/donations - 数组形状，id 字符串")
    void donationsShouldReturnArray() throws Exception {
        DonationRecordVO d = new DonationRecordVO();
        d.setId(3L);
        d.setDonorName("王宇彬");
        d.setAmount(500000L);
        d.setDate(LocalDateTime.of(2026, 8, 5, 0, 0));
        Mockito.when(foundationService.donations()).thenReturn(List.of(d));

        mockMvc.perform(get("/tsa/foundation/donations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("3"))
                .andExpect(jsonPath("$.data[0].donorName").value("王宇彬"));
    }

    @Test
    @DisplayName("POST /foundation/categories - 名称为空应返回 400 并带字段提示")
    void createCategoryShouldFailWhenNameBlank() throws Exception {
        RewardCategorySaveRequest req = new RewardCategorySaveRequest();
        // name 故意不填
        req.setSponsor("陈某某");

        mockMvc.perform(post("/tsa/foundation/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("奖项类别名称")));
    }

    @Test
    @DisplayName("POST /foundation/donations - 新增成功应返回字符串 id")
    void createDonationShouldReturnStringId() throws Exception {
        Mockito.when(foundationService.createDonation(ArgumentMatchers.any())).thenReturn(55L);
        // 手写 JSON：donationDate 必须带时区偏移（契约要求 + 字段级 @JsonFormat 的 XXX），
        // 裸 ObjectMapper 不认 LocalDateTime，故不走对象序列化
        String json = "{\"donorName\":\"坤坤\",\"amount\":2000000,\"donationDate\":\"2026-08-10T00:00:00+08:00\"}";

        mockMvc.perform(post("/tsa/foundation/donations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("55"));
    }

    @Test
    @DisplayName("DELETE /foundation/categories/{id} - 不存在（Service 抛 1002）应返回业务错误码")
    void deleteCategoryShouldReturn1002WhenNotFound() throws Exception {
        Mockito.doThrow(new BusinessException(ResultCode.DATA_NOT_FOUND))
                .when(foundationService).deleteCategory(999L);

        mockMvc.perform(delete("/tsa/foundation/categories/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.DATA_NOT_FOUND.getCode()));
    }
}
