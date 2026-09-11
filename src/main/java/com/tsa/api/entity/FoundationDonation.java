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

    @Schema(description = "捐赠日期")
    private LocalDateTime donationDate;
}
