package com.tsa.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 社区动态出参视图对象（对应前端 CommunityPost）。
 *
 * <p>列表与详情共用：列表 commentsList 为 null 由 NON_NULL 省略；
 * cuisine/region 仅美食动态有值，校园动态为 null 同样省略。
 * comments 为派生的评论条数。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "社区动态（列表与详情共用）")
public class CommunityPostVO {

    @Schema(description = "动态 id（字符串形式）", example = "1")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "动态类型：food / campus")
    private String type;

    @Schema(description = "发布者昵称")
    private String author;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "正文")
    private String content;

    @Schema(description = "图片 URL 数组")
    private List<String> images;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "点赞数")
    private Integer likes;

    @Schema(description = "评论条数")
    private Integer comments;

    @Schema(description = "审核状态：0 待审核 / 1 已通过 / 2 已驳回（管理端列表消费；"
            + "公开列表/详情只会下发 1，前端可无视）", example = "1", allowableValues = {"0", "1", "2"})
    private Integer status;

    @Schema(description = "评论列表（仅详情返回）")
    private List<CommentVO> commentsList;

    @Schema(description = "菜系（仅美食动态）")
    private String cuisine;

    @Schema(description = "所在地区（仅美食动态）")
    private String region;
}
