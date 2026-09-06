package com.tsa.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动表实体（表已建好，接口在第二期开放）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("activity")
@Schema(description = "乡会活动")
public class Activity extends BaseEntity {

    @Schema(description = "活动标题")
    private String title;

    @Schema(description = "封面图 URL")
    private String coverUrl;

    @Schema(description = "活动详情")
    private String content;

    @Schema(description = "活动地点描述")
    private String location;

    @Schema(description = "地点纬度")
    private BigDecimal lat;

    @Schema(description = "地点经度")
    private BigDecimal lng;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "报名截止时间")
    private LocalDateTime signupDeadline;

    @Schema(description = "人数上限")
    private Integer capacity;

    @Schema(description = "状态：0 未开始 / 1 报名中 / 2 进行中 / 3 已结束")
    private Integer status;
}
