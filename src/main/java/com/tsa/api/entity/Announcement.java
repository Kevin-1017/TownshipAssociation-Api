package com.tsa.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 公告表实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("announcement")
@Schema(description = "乡会公告")
public class Announcement extends BaseEntity {

    @Schema(description = "标题")
    private String title;

    @Schema(description = "正文内容")
    private String content;

    @Schema(description = "发布人（admin_user.id）")
    private Long publisherId;

    @Schema(description = "是否置顶：0 否 / 1 是")
    private Integer isTop;

    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;
}
