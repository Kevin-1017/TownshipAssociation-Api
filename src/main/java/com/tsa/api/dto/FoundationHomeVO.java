package com.tsa.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/** 基金会首页聚合出参（对应前端 foundationApi.getData 的返回形状）。 */
@Data
@Schema(description = "基金会首页数据（奖励 + 捐赠）")
public class FoundationHomeVO {

    @Schema(description = "奖励条目列表")
    private List<RewardItemVO> rewards;

    @Schema(description = "捐赠条目列表")
    private List<DonationItemVO> donations;
}
