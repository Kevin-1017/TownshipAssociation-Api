package com.tsa.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 活动管理端查询条件（GET /tsa/admin/events 的 query 参数自动绑定）。
 *
 * <p>与公开列表（{@link EventQuery}）的差异：管理端没有年份区间维度，
 * 换成了标题关键字——后台翻找靠搜不靠筛；且这里能看到的全部行都是已发布的
 * （删除即下架的口径下不存在待审态）。
 */
@Data
@Schema(description = "活动管理端查询条件")
public class EventAdminQuery {

    @Schema(description = "页码，从 1 开始", example = "1", defaultValue = "1")
    private Integer page = 1;

    @Schema(description = "每页条数，最大 50", example = "10", defaultValue = "10")
    private Integer pageSize = 10;

    @Schema(description = "标题关键字（可选，模糊匹配）", example = "恳亲大会")
    private String keyword;
}
