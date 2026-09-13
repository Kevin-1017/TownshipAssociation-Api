package com.tsa.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/** 新增/修改捐赠鸣谢请求（管理端 CRUD 用）。amount 单位：元。 */
@Data
@Schema(description = "捐赠记录保存请求")
public class DonationSaveRequest {

    @Schema(description = "捐赠人姓名", example = "坤坤", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写捐赠人姓名")
    @Size(max = 64, message = "捐赠人名称过长")
    private String donorName;

    @Schema(description = "捐赠金额（元）", example = "2000000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请填写捐赠金额")
    @PositiveOrZero(message = "捐赠金额不能为负")
    private Long amount;

    /** 缺省不传 = 保密（只有显式 true 才对外展示金额） */
    @Schema(description = "金额是否公开显示（缺省 false）", example = "false")
    private Boolean amountVisible;

    @Schema(description = "捐赠日期（ISO 8601 带时区）", example = "2026-08-10T00:00:00+08:00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请填写捐赠日期")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private LocalDateTime donationDate;
}
