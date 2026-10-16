package com.tsa.api.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * CS 道具点位实体（官网 kevin/csgo 指南，管理后台可配置）。
 *
 * <p>images 是 JSON 列，用 {@link JacksonTypeHandler} 在 List&lt;String&gt; 与 JSON 间
 * 自动转换，开启 {@code autoResultMap} 后查询才会走该 handler（同 community_post.images）。
 * images 只存服务端签发的相对路径（/tsa/files/&lt;uuid&gt;.&lt;ext&gt;），拼绝对地址是展示端的事。
 * kind 枚举校验收口在 LineupSaveRequest，实体不设常量——本表无状态列，不涉 status 过滤。
 * 2026-10-16 精简：删 site（归类点位）与 steps（分步操作），见 migrate-2026-10-16-csgo-lineup-simplify.sql。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "csgo_lineup", autoResultMap = true)
@Schema(description = "CS 道具点位")
public class Lineup extends BaseEntity {

    @Schema(description = "地图名", example = "Mirage")
    private String map;

    @Schema(description = "道具类型：烟/闪/火/雷/诱饵", example = "烟")
    private String kind;

    @Schema(description = "点位一句话名字")
    private String name;

    @Schema(description = "投掷方式标签：站位/跳投/Timing 等", example = "站位")
    private String style;

    @Schema(description = "用途或备注")
    private String note;

    @Schema(description = "截图 URL 数组（相对路径）")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;
}
