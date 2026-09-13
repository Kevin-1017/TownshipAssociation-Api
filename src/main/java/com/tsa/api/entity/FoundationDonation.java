package com.tsa.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 基金会捐赠鸣谢实体。amount 单位：元。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("foundation_donation")
@Schema(description = "基金会捐赠鸣谢")
public class FoundationDonation extends BaseEntity {

    @Schema(description = "捐赠人姓名")
    private String donorName;

    @Schema(description = "捐赠金额（元）")
    private Long amount;

    /**
     * 金额是否对外展示。秘书处口径（2026-09-13）：除明确点名公开的记录外一律保密，
     * 故默认 0（库层 DEFAULT 0 + 新增请求缺省 false 双保险）；true 的才会把 amount 带给前端。
     */
    @Schema(description = "金额是否公开显示")
    private Boolean amountVisible;

    @Schema(description = "捐赠日期")
    private LocalDateTime donationDate;
}
