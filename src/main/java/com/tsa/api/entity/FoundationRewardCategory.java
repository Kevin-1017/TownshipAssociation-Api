package com.tsa.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 基金会奖项类别实体（首页「奖励」卡片 + 详情页分组依据）。
 * amount 为该类别奖金总额，单位：分。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("foundation_reward_category")
@Schema(description = "基金会奖项类别")
public class FoundationRewardCategory extends BaseEntity {

    @Schema(description = "奖项类别名")
    private String name;

    @Schema(description = "赞助人/捐赠方")
    private String sponsor;

    @Schema(description = "类别奖金总额（元）")
    private Long amount;

    @Schema(description = "展示顺序，越小越前")
    private Integer sort;
}
