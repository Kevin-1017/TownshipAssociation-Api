package com.tsa.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 基金会获奖记录实体（类别下的获奖人）。amount 单位：元。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("foundation_reward_record")
@Schema(description = "基金会获奖记录")
public class FoundationRewardRecord extends BaseEntity {

    @Schema(description = "所属奖项类别 id")
    private Long categoryId;

    @Schema(description = "获奖人姓名")
    private String recipient;

    @Schema(description = "奖金金额（元）")
    private Long amount;
}
