package com.tsa.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 获奖记录（对应前端 RewardRecord，详情页 tab0 用）。amount 单位：元。 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "获奖记录")
public class RewardRecordVO {

    @Schema(description = "记录 id（字符串形式）", example = "1")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "所属类别 id（字符串形式）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long categoryId;

    @Schema(description = "所属类别名称")
    private String categoryName;

    @Schema(description = "获奖人姓名")
    private String recipient;

    @Schema(description = "奖金金额（元）")
    private Long amount;
}
