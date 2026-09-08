package com.tsa.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 地图打点专用的轻量视图对象（VO）。
 *
 * <p>教学要点：地图接口可能被高频调用，只返回渲染所需的最小字段集，
 * 不要把整个实体序列化出去——这就是"接口按消费方裁剪响应"的思路。
 * 字段集与前端契约一致：id name avatarUrl lat lng province city industry。
 */
@Data
@Schema(description = "地图标记点（轻量）")
public class MapMarkerVO {

    @Schema(description = "成员 id（字符串形式）", example = "1")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "省份")
    private String province;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "行业（字典 code，如 internet）")
    private String industry;

    @Schema(description = "头像 URL")
    private String avatarUrl;

    @Schema(description = "纬度")
    private BigDecimal lat;

    @Schema(description = "经度")
    private BigDecimal lng;
}
