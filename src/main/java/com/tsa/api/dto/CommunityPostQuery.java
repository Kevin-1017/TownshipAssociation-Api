package com.tsa.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 社区动态列表查询条件。
 *
 * <p>type 为美食/校园分栏主筛选；cuisine/region 为美食基地可选筛选；keyword 命中标题/正文。
 */
@Data
@Schema(description = "社区动态列表查询条件")
public class CommunityPostQuery {

    @Schema(description = "页码，从 1 开始", example = "1", defaultValue = "1")
    private Integer page = 1;

    @Schema(description = "每页条数，最大 100", example = "20", defaultValue = "20")
    private Integer pageSize = 20;

    @Schema(description = "动态类型：food / campus", example = "food")
    private String type;

    @Schema(description = "菜系筛选（仅美食）", example = "潮汕菜")
    private String cuisine;

    @Schema(description = "地区筛选（仅美食）", example = "longdong")
    private String region;

    @Schema(description = "关键字：模糊匹配标题/正文", example = "牛肉丸")
    private String keyword;
}
