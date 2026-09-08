package com.tsa.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 成员列表查询条件（GET 请求的 query 参数自动绑定到本对象）。
 *
 * <p>参数名与前端契约一致：page / pageSize / province / city / industry / keyword。
 */
@Data
@Schema(description = "成员列表查询条件")
public class MemberQuery {

    @Schema(description = "页码，从 1 开始", example = "1", defaultValue = "1")
    private Integer page = 1;

    @Schema(description = "每页条数，最大 50", example = "20", defaultValue = "10")
    private Integer pageSize = 10;

    @Schema(description = "按省份筛选", example = "广东省")
    private String province;

    @Schema(description = "按城市筛选", example = "汕头市")
    private String city;

    @Schema(description = "按行业筛选（字典 code）", example = "internet")
    private String industry;

    @Schema(description = "关键字：模糊匹配姓名/简介", example = "张")
    private String keyword;
}
