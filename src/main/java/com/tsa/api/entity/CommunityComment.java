package com.tsa.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 社区动态评论实体。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("community_comment")
@Schema(description = "社区动态评论")
public class CommunityComment extends BaseEntity {

    @Schema(description = "所属动态 id")
    private Long postId;

    @Schema(description = "评论者昵称")
    private String author;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "评论点赞数")
    private Integer likes;

    @Schema(description = "评论时间")
    private LocalDateTime createTime;
}
