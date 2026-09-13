package com.tsa.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新增/修改公告请求（管理端 CRUD 用，POST/PUT /tsa/admin/notices）。
 *
 * <p>字段对齐 announcement 表 + NoticeVO 契约：用 {@code pinned} 布尔表达置顶（Service 层转 0/1 落 isTop），
 * 与出参 NoticeVO 同一命名口径。公告无上下架状态列（见 Announcement 实体），故本请求不造 status 字段。
 */
@Data
@Schema(description = "公告保存请求")
public class NoticeSaveRequest {

    @Schema(description = "标题", example = "2026 年度会员大会通知", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写公告标题")
    @Size(max = 64, message = "标题最长 64 个字")
    private String title;

    @Schema(description = "列表摘要（正文摘录，可空）", example = "定于下月召开年度大会")
    @Size(max = 200, message = "摘要最长 200 个字")
    private String summary;

    @Schema(description = "正文内容（纯文本含换行）", example = "各位会员：……", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写公告正文")
    private String content;

    /** 缺省不传 = 不置顶（只有显式 true 才置顶） */
    @Schema(description = "是否置顶（缺省 false）", example = "false")
    private Boolean pinned;

    /** 不传 = 新增时取当前时间；修改时保留原发布时间 */
    @Schema(description = "发布时间（ISO 8601 带时区，新增不传默认当前时间）",
            example = "2026-09-14T09:00:00+08:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private LocalDateTime publishedAt;
}
