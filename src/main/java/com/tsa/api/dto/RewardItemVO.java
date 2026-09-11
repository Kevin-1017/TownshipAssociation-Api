package com.tsa.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 基金会首页奖励条目（对应前端 FoundationRewardItem）。amount 单位：元。 */
@Data
@Schema(description = "基金会奖励条目（首页）")
public class RewardItemVO {

    @Schema(description = "id（字符串形式）", example = "1")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "奖项名称标签")
    private String label;

    @Schema(description = "金额（元）")
    private Long amount;

    @Schema(description = "赞助人/捐赠方")
    private String sponsor;
}
