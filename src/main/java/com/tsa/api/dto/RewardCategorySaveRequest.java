package com.tsa.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新增/修改奖项类别请求（管理端 CRUD 用）。amount 单位：元。 */
@Data
@Schema(description = "奖项类别保存请求")
public class RewardCategorySaveRequest {

    @Schema(description = "奖项类别名", example = "年度奖学金颁发", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写奖项类别名称")
    @Size(max = 64, message = "类别名称最长 64 个字")
    private String name;

    @Schema(description = "赞助人/捐赠方", example = "陈某某")
    @Size(max = 64, message = "赞助人名称过长")
    private String sponsor;

    @Schema(description = "类别奖金总额（元）", example = "50000")
    private Long amount;

    @Schema(description = "展示顺序，越小越前", example = "1")
    private Integer sort;
}
