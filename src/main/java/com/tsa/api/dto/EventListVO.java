package com.tsa.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动列表项（契约 C5，{@code GET /tsa/events} 分页元素）。
 *
 * <p>字段名钉死三方契约（后端/mock/前端 types 必须逐字一致）：
 * <ul>
 *   <li>{@code cover} 对应库列 cover_url —— 契约用前端惯用名，不用列名直译</li>
 *   <li>{@code status} 是<b>派生展示态</b> upcoming/past（start_time 与当前时间比较，
 *       不落库），不是 activity.status 那套 0/1/2/3 库内流转状态</li>
 *   <li>没有 content 字段 —— 正文留在公众号文章里（v1.2 D2 形态），接口永不外给</li>
 * </ul>
 */
@Data
@Schema(description = "活动列表项")
public class EventListVO {

    @Schema(description = "活动 id（字符串形式）", example = "1")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "活动标题", example = "成都老乡下午茶")
    private String title;

    @Schema(description = "封面图相对路径/URL，可为 null")
    private String cover;

    @Schema(description = "一句话简介，可为 null")
    private String summary;

    @Schema(description = "开始时间（ISO 8601 带时区，JacksonConfig 统一格式化）",
            example = "2026-10-01T14:00:00+08:00")
    private LocalDateTime startTime;

    @Schema(description = "展示态：upcoming 未开始 / past 已过期（由 startTime 派生）",
            example = "upcoming", allowableValues = {"upcoming", "past"})
    private String status;
}
