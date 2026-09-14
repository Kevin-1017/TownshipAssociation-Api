package com.tsa.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 社区评论管理端查询条件（GET /tsa/admin/community/comments 的 query 参数自动绑定）。
 *
 * <p>与帖子侧 {@link CommunityPostAdminQuery} 同构；postId 可选——审核人从某条动态
 * 的评论区点进来时能只看该帖的待审评论。
 */
@Data
@Schema(description = "社区评论管理端查询条件")
public class CommunityCommentAdminQuery {

    @Schema(description = "页码，从 1 开始", example = "1", defaultValue = "1")
    private Integer page = 1;

    @Schema(description = "每页条数，最大 100", example = "20", defaultValue = "20")
    private Integer pageSize = 20;

    @Schema(description = "按审核状态筛选（可选：0 待审核/1 已通过/2 已驳回；缺省全部）", example = "0")
    private Integer status;

    @Schema(description = "只看某条动态下的评论（可选）", example = "1")
    private Long postId;
}
