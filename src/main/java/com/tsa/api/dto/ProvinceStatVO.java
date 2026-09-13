package com.tsa.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 省份分布统计行（契约 C4，{@code GET /tsa/members/stats/province} 出参元素）。
 *
 * <p>count 用 Long 而不是 Integer：SQL 是 COUNT(*)（BIGINT），类型跟着数据源走，
 * 不在 DTO 层做有损收窄；JSON 序列化后前端拿到的仍是 number。
 */
@Data
@Schema(description = "省份分布统计（仅审核通过成员）")
public class ProvinceStatVO {

    @Schema(description = "省份", example = "广东省")
    private String province;

    @Schema(description = "该省审核通过成员数", example = "12")
    private Long count;

    public ProvinceStatVO() {
    }

    public ProvinceStatVO(String province, Long count) {
        this.province = province;
        this.count = count;
    }
}
