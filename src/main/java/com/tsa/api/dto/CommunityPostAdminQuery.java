package com.tsa.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 社区动态管理端查询条件（GET /tsa/admin/community/posts 的 query 参数自动绑定）。
 *
 * <p>与公开列表的 {@link CommunityPostQuery} 分开：公开侧看不到也搜不到未过审内容，
 * 管理侧的 status 是筛选维度而不是恒定过滤值。
 */
@Data
@Schema(description = "社区动态管理端查询条件")
public class CommunityPostAdminQuery {

    @Schema(description = "页码，从 1 开始", example = "1", defaultValue = "1")
    private Integer page = 1;

    @Schema(description = "每页条数，最大 100", example = "20", defaultValue = "20")
    private Integer pageSize = 20;

    @Schema(description = "按审核状态筛选（可选：0 待审核/1 已通过/2 已驳回；缺省全部）", example = "0")
    private Integer status;

    @Schema(description = "按栏目筛选（可选：food/campus；缺省全部）", example = "food")
    private String type;
}
