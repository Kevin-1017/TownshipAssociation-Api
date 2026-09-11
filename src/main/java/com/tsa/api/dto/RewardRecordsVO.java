package com.tsa.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/** 奖励明细出参（对应前端 foundationApi.getRewards 返回形状）。 */
@Data
@Schema(description = "基金会奖励明细（类别 + 获奖记录）")
public class RewardRecordsVO {

    @Schema(description = "类别名称列表（去重，供前端分组/筛选）")
    private List<String> categories;

    @Schema(description = "获奖记录列表")
    private List<RewardRecordVO> records;
}
