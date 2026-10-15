package com.tsa.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 道具点位出参视图对象 —— 前台 kevin/csgo 浏览器与管理端共用一份形状。
 *
 * <p>无发布时间等业务时间要下发，BaseEntity 三件套不外泄；images 给相对路径原样，
 * 展示端用 buildFileUrl 拼绝对址（口径同公告/事件封面）。
 */
@Data
@Schema(description = "CS 道具点位（列表一次给全）")
public class LineupVO {

    @Schema(description = "点位 id（字符串形式）", example = "1")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "地图名", example = "Mirage")
    private String map;

    @Schema(description = "归类点位名", example = "警家")
    private String site;

    @Schema(description = "道具类型：smoke/flash/molotov/decoy", example = "smoke")
    private String kind;

    @Schema(description = "点位一句话名字")
    private String name;

    @Schema(description = "投掷方式标签", example = "站位")
    private String style;

    @Schema(description = "分步操作（按顺序）")
    private List<String> steps;

    @Schema(description = "用途或备注")
    private String note;

    @Schema(description = "截图相对路径数组（无图为空数组）")
    private List<String> images;
}
