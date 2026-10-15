package com.tsa.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 新增/修改道具点位请求（管理端 CRUD 用，POST/PUT /tsa/admin/lineups）。
 *
 * <p>字段对齐 csgo_lineup 表与出参 LineupVO。POST/PUT 共用：管理表单总是整单提交，
 * 因此修改按「请求有什么就覆写什么」的全量覆写语义（与公告一致，不造 events 那套
 * null=不改/空串=清除的局部更新口径）；images 不传 = 无图。
 */
@Data
@Schema(description = "道具点位保存请求")
public class LineupSaveRequest {

    @Schema(description = "地图名", example = "Mirage", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写地图名")
    @Size(max = 32, message = "地图名最长 32 个字")
    private String map;

    @Schema(description = "归类点位名", example = "警家", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写点位名")
    @Size(max = 32, message = "点位名最长 32 个字")
    private String site;

    @Schema(description = "道具类型（枚举）", example = "smoke", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请选择道具类型")
    @Pattern(regexp = "smoke|flash|molotov|decoy", message = "道具类型仅限 smoke/flash/molotov/decoy")
    private String kind;

    @Schema(description = "点位一句话名字", example = "T 一号位警家烟", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写点位名称")
    @Size(max = 64, message = "点位名称最长 64 个字")
    private String name;

    @Schema(description = "投掷方式标签", example = "站位", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写投掷方式标签")
    @Size(max = 32, message = "方式标签最长 32 个字")
    private String style;

    @Schema(description = "分步操作（按顺序）", example = "[\"站位贴墙\", \"瞄参照点\", \"原地投\"]",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "请至少填写一步操作")
    @Size(max = 20, message = "步骤最多 20 条")
    private List<@Size(max = 200, message = "单步描述最长 200 个字") String> steps;

    @Schema(description = "用途或备注（可空）", example = "双封烟的第一颗")
    @Size(max = 500, message = "备注最长 500 个字")
    private String note;

    @Schema(description = "截图相对路径数组（先传 /tsa/admin/lineups/image 拿路径；不传/空数组=无图）",
            example = "[\"/tsa/files/0b1e5c2f-9a3d-4e7c-8f21-6d0a4b3c5e77.jpg\"]")
    @Size(max = 9, message = "一条点位最多 9 张图")
    private List<@Size(max = 255, message = "图片路径过长") String> images;
}
