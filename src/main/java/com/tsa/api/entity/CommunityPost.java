package com.tsa.api.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 社区动态实体（美食基地 / 校园广场共用一张表，靠 type 区分）。
 *
 * <p>images 是 JSON 列，用 MyBatis-Plus 的 {@link JacksonTypeHandler} 在 List&lt;String&gt; 与
 * JSON 之间自动转换；开启 {@code autoResultMap} 后查询才会走这个 handler。
 * 评论条数不入库，由 community_comment 派生，避免计数与子表长期不一致。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "community_post", autoResultMap = true)
@Schema(description = "社区动态")
public class CommunityPost extends BaseEntity {

    @Schema(description = "动态类型：food 美食 / campus 校园")
    private String type;

    @Schema(description = "发布者昵称（一期无登录，自由填写）")
    private String author;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "正文")
    private String content;

    @Schema(description = "图片 URL 数组")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    @Schema(description = "菜系（仅美食动态）")
    private String cuisine;

    @Schema(description = "所在地区（仅美食动态）")
    private String region;

    @Schema(description = "点赞数")
    private Integer likes;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;
}
