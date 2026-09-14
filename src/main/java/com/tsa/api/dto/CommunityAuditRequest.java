package com.tsa.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 社区动态审核动作请求（PUT /tsa/admin/community/posts/{id}/audit）。
 *
 * <p>只允许 通过(1)/驳回(2)：待审核(0) 不是管理员会主动设置的状态——发新帖天然回 0；
 * 「驳回可恢复」口径下，把已驳回帖审回 1 也是走本接口传 status=1。
 */
@Data
@Schema(description = "社区动态审核请求")
public class CommunityAuditRequest {

    @Schema(description = "目标状态：1 通过 / 2 驳回", example = "1",
            allowableValues = {"1", "2"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请选择审核结果")
    @Min(value = 1, message = "审核结果只能是 1 通过或 2 驳回")
    @Max(value = 2, message = "审核结果只能是 1 通过或 2 驳回")
    private Integer status;
}
