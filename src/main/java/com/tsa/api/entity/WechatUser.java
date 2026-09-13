package com.tsa.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小程序登录用户埋点实体（计划 D1：「来过」层）。
 *
 * <p>三层身份各归各位：wechat_user（登录过的访客）→ member（登记为乡贤的名册档案）
 * → association_member（乡会身份核验）。本表与后两张表<b>彻底分离</b>，
 * 只承担「有多少用户 / 新增 / 活跃」的运营统计，不做任何业务授权依据。
 *
 * <p><b>刻意不继承 {@link BaseEntity}</b>：本表 DDL（见 sql/schema.sql 第 12 节）
 * 没有 deleted / created_at / updated_at 三件套 —— 继承基类会让 {@code @TableLogic}
 * 在 MP 自动生成的查询上拼 {@code deleted = 0}，直接 SQL 报错（Unknown column）。
 * 这也是全项目第一个不继承 BaseEntity 的实体，属有意破例而非疏漏。
 */
@Data
@TableName("wechat_user")
@Schema(description = "小程序登录用户埋点（访客，非名册）")
public class WechatUser {

    /** 与其他表同样规则：主键序列化为字符串，防 JS number 精度丢失 */
    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "微信 openid（本小程序下用户唯一 ID）")
    private String openid;

    @Schema(description = "unionid（同开放平台账号下跨应用识别同一用户；本期无公众号，恒 NULL）")
    private String unionid;

    @Schema(description = "首次登录时间（新增用户统计口径）")
    private LocalDateTime firstLoginAt;

    @Schema(description = "最近登录时间（活跃用户统计口径）")
    private LocalDateTime lastLoginAt;

    @Schema(description = "累计登录次数")
    private Integer loginCount;
}
