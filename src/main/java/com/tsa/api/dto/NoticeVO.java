package com.tsa.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告出参视图对象 —— 按前端 NoticeItem 契约裁剪。
 *
 * <p>教学要点：实体里的 {@code isTop} 是数据库的 0/1 整型，
 * 契约要求 {@code pinned} 布尔 —— 形状转换发生在 VO 这一层，
 * 不让实体直接对着前端输出（也顺手避开了 boolean isXxx 的序列化命名坑）。
 * 注意列表与详情都带 content：公告数量少、正文短，契约约定不分页一次给全。
 */
@Data
@Schema(description = "公告（列表与详情共用）")
public class NoticeVO {

    @Schema(description = "公告 id（字符串形式）", example = "1")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "列表摘要")
    private String summary;

    @Schema(description = "正文（纯文本含换行）")
    private String content;

    @Schema(description = "是否置顶")
    private Boolean pinned;

    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;
}
