package com.tsa.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 捐赠记录（管理端）。保密只是官网展示口径：amount 原样返回不抹码，
 * amountVisible 仅表示官网是否显示数字。对应前端 DonationAdminRecord。
 */
@Data
@Schema(description = "捐赠记录（管理端，金额原值）")
public class DonationAdminVO {

    @Schema(description = "记录 id（字符串形式）", example = "1")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "捐赠人姓名")
    private String donorName;

    @Schema(description = "捐赠金额（元），原值不抹码")
    private Long amount;

    @Schema(description = "金额是否在官网公开显示（false = 官网只鸣谢不出数字，管理端仍可见原值）")
    private Boolean amountVisible;

    @Schema(description = "捐赠日期")
    private LocalDateTime date;
}
