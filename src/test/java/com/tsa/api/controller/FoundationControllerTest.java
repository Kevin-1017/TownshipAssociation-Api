package com.tsa.api.controller;

import com.tsa.api.dto.DonationItemVO;
import com.tsa.api.dto.DonationRecordVO;
import com.tsa.api.dto.FoundationHomeVO;
import com.tsa.api.dto.RewardItemVO;
import com.tsa.api.dto.RewardRecordsVO;
import com.tsa.api.common.GlobalExceptionHandler;
import com.tsa.api.service.FoundationService;
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
 * FoundationController standalone MockMvc 测试（不连数据库）：只读接口。
 *
 * <p>v1.3 起写接口（categories/records/donations 的 POST/PUT/DELETE）已从本 Controller 迁至
 * AdminFoundationController（{@code /tsa/admin/foundation/**}），对应写用例随之搬至
 * {@link AdminFoundationControllerTest}；本类仅保留三个公开读接口用例。
 */
class FoundationControllerTest {

    private FoundationService foundationService;
    private MockMvc mockMvc;

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
}
