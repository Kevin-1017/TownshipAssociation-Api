package com.tsa.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成员公开视图对象 —— 列表接口的出参契约。
 *
 * <p>教学要点：实体不外泄给不匹配的消费方。Member 里的 {@code openid} 是微信登录凭证、
 * {@code status} 在公开查询里恒为 1 —— 都不是前端该看到的，在这里裁掉；
 * phone 则在实体上直接 @JsonIgnore。裁剪发生在 VO 层，数据库和实体保持原样。
 */
@Data
@Schema(description = "成员公开信息（列表用）")
public class MemberVO {

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

    @Schema(description = "纬度")
    private BigDecimal lat;

    @Schema(description = "经度")
    private BigDecimal lng;

    @Schema(description = "个人简介")
    private String intro;

    @Schema(description = "入会时间（即注册时间）")
    private LocalDateTime createdAt;
}
