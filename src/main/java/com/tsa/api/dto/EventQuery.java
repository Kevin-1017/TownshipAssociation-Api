package com.tsa.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 活动列表查询条件（GET /tsa/events 的 query 参数自动绑定，契约 C5）。
 *
 * <p>参数名与前端契约一致：page / pageSize / year / yearFrom / yearTo。year 与
 * 年份区间（yearFrom..yearTo，含端点）是本项目列表页（event/list 的年份筛选）
 * 唯一的服务端过滤维度，不做关键字搜索；同时给出时按交集生效。
 */
@Data
@Schema(description = "活动列表查询条件")
public class EventQuery {

    @Schema(description = "页码，从 1 开始", example = "1", defaultValue = "1")
    private Integer page = 1;

    @Schema(description = "每页条数，最大 50", example = "10", defaultValue = "10")
    private Integer pageSize = 10;

    @Schema(description = "按活动开始时间的年份筛选（可选，4 位数字）", example = "2026")
    private Integer year;

    @Schema(description = "年份区间起（含，可选，4 位数字）；与 year 同时给出取交集", example = "2024")
    private Integer yearFrom;

    @Schema(description = "年份区间止（含，可选，4 位数字）；小于 yearFrom 返回 400", example = "2026")
    private Integer yearTo;
}
