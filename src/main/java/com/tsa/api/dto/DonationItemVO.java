package com.tsa.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/** 基金会首页捐赠条目（对应前端 FoundationDonationItem）。amount 单位：元。 */
@Data
@Schema(description = "基金会捐赠条目（首页）")
public class DonationItemVO {

    @Schema(description = "id（字符串形式）", example = "1")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "捐赠人姓名")
    private String donorName;

    @Schema(description = "捐赠金额（元）")
    private Long amount;

    @Schema(description = "捐赠日期")
    private LocalDateTime date;
}
