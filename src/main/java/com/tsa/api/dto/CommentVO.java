package com.tsa.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/** 评论出参视图对象（对应前端 CommentItem）。 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "社区评论")
public class CommentVO {

    @Schema(description = "评论 id（字符串形式）", example = "1")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "评论者昵称")
    private String author;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "评论时间")
    private LocalDateTime createTime;

    @Schema(description = "评论点赞数")
    private Integer likes;

    @Schema(description = "所属动态 id（字符串形式；2026-09-15 评论审核制随管理端列表下发）", example = "1")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long postId;

    @Schema(description = "审核状态：0 待审核 / 1 已通过 / 2 已驳回（公开详情只会下发 1，前端可无视）",
            example = "1", allowableValues = {"0", "1", "2"})
    private Integer status;
}
