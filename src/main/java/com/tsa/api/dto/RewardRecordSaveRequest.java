package com.tsa.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新增/修改获奖记录请求（管理端 CRUD 用）。amount 单位：元。 */
@Data
@Schema(description = "获奖记录保存请求")
public class RewardRecordSaveRequest {

    @Schema(description = "所属奖项类别 id", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请选择所属奖项类别")
    private Long categoryId;

    @Schema(description = "获奖人姓名", example = "李思远", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写获奖人姓名")
    @Size(max = 32, message = "姓名最长 32 个字")
    private String recipient;

    @Schema(description = "奖金金额（元）", example = "8000")
    private Long amount;
}
