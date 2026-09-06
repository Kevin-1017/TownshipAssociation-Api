package com.tsa.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 活动报名表实体（表已建好，接口在第二期开放）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("activity_registration")
@Schema(description = "活动报名记录")
public class ActivityRegistration extends BaseEntity {

    @Schema(description = "活动 id")
    private Long activityId;

    @Schema(description = "报名成员 id")
    private Long memberId;

    @Schema(description = "状态：0 已报名 / 1 已签到 / 2 已取消")
    private Integer status;

    @Schema(description = "报名时间")
    private LocalDateTime signupTime;
}
