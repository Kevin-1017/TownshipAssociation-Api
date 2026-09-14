package com.tsa.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新增/修改乡会事件请求（POST/PUT /tsa/admin/events）。
 *
 * <p>字段是「秘书处配置一条事件」的最小闭环（用户定稿：图片、标题、公众号 url）：
 * activity 表的报名/人数/经纬度等列随功能冻结不进契约，服务端不写、保持库默认值。
 * <b>startTime 必填</b>——列表排序与年份区间筛选全押在 start_time 列上，缺了它一条事件
 * 既排不进序也筛不进区间。cover 传 POST /tsa/admin/events/cover 返回的相对路径；
 * 更新时传空串 = 清除封面（null = 不改，MyBatis-Plus NOT_NULL 更新策略）。
 */
@Data
@Schema(description = "活动保存请求（管理端）")
public class EventSaveRequest {

    @Schema(description = "活动标题", example = "乡会 2026 年度恳亲大会", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写活动标题")
    @Size(max = 64, message = "标题最长 64 个字")
    private String title;

    @Schema(description = "封面图相对路径（先传 /tsa/admin/events/cover 拿路径），可空",
            example = "/tsa/files/0b1e5c2f-9a3d-4e7c-8f21-6d0a4b3c5e77.jpg")
    @Size(max = 255, message = "封面地址过长")
    private String cover;

    @Schema(description = "一句话简介（列表卡片展示），可空", example = "恳亲大会定档下月，欢迎老乡报名。")
    @Size(max = 500, message = "简介最长 500 个字")
    private String summary;

    @Schema(description = "公众号文章链接（列表直跳/阅读按钮用），可空=「整理中」置灰态",
            example = "https://mp.weixin.qq.com/s/sXNMWVwmhmIspCxfAFmJDQ")
    @Size(max = 500, message = "公众号链接过长")
    private String articleUrl;

    @Schema(description = "开始时间（ISO 8601 带时区；排序与年份筛选依据，必填）",
            example = "2026-10-01T14:00:00+08:00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请选择活动开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private LocalDateTime startTime;
}
