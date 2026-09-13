package com.tsa.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动详情（契约 C6，{@code GET /tsa/events/{id}} 出参）。
 *
 * <p>与 {@link EventListVO} 的字段差异即本期形态的全部信息量：
 * detail 页只多给一个 {@code articleUrl}（前端「阅读公众号全文」按钮的入参，
 * wx.openOfficialAccountArticle 打不开就复制链接兜底），<b>没有 content</b> ——
 * 正文永远留在公众号，小程序不重抄（v1.2 D2 拍板）。
 * articleUrl 允许为 null（键必须在场，秘书处还没补链接的过渡态）。
 */
@Data
@Schema(description = "活动详情（正文在公众号，本接口只给文章链接）")
public class EventDetailVO {

    @Schema(description = "活动 id（字符串形式）", example = "1")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "活动标题", example = "成都老乡下午茶")
    private String title;

    @Schema(description = "封面图相对路径/URL，可为 null")
    private String cover;

    @Schema(description = "一句话简介，可为 null")
    private String summary;

    @Schema(description = "开始时间（ISO 8601 带时区）", example = "2026-10-01T14:00:00+08:00")
    private LocalDateTime startTime;

    @Schema(description = "公众号文章永久链接（/s/xxx），秘书处 SQL 直插维护，可为 null",
            example = "https://mp.weixin.qq.com/s/xxxxxxxxxxxx")
    private String articleUrl;
}
