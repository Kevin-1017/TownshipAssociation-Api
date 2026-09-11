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
}
