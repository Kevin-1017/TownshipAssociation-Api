package com.tsa.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成员详情视图对象 —— 本人档案出参（/user/me 与登录结果的 user 键），与小程序 MemberDetail 类型 1:1。
 *
 * <p>与 {@link MemberVO}（列表用）的差别：字段更全，本人视角含联系方式（不裁剪）；
 * {@code contactVisible} 表达本人的联系方式可见性偏好（只约束他人能否查看）。
 */
@Data
@Schema(description = "成员详情（本人档案）")
public class MemberDetailVO {

    @Schema(description = "成员 id（字符串形式）", example = "1")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "头像 URL")
    private String avatarUrl;

    @Schema(description = "性别：0 未知 / 1 男 / 2 女")
    private Integer gender;

    @Schema(description = "届别（入学/毕业年份）", example = "2018")
    private Integer graduationYear;

    @Schema(description = "行业（字典 code，如 internet）")
    private String industry;

    @Schema(description = "省份")
    private String province;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "区/县")
    private String district;

    @Schema(description = "工作单位")
    private String company;

    @Schema(description = "职务/头衔")
    private String title;

    @Schema(description = "毕业院校")
    private String school;

    @Schema(description = "专业")
    private String major;

    @Schema(description = "会龄（年，由届别推算）", example = "8")
    private Integer seniority;

    @Schema(description = "个人简介")
    private String intro;

    @Schema(description = "联系方式可见性（本人偏好开关）")
    private Boolean contactVisible;

    @Schema(description = "微信联系方式")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String wechatId;

    @Schema(description = "手机号")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String phone;

    @Schema(description = "纬度")
    private BigDecimal lat;

    @Schema(description = "经度")
    private BigDecimal lng;

    @Schema(description = "入会时间")
    private LocalDateTime createdAt;

    @Schema(description = "国家（恒为中国，保留字段）", example = "中国")
    private String country;
}