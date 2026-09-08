package com.tsa.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 成员注册/资料提交的请求体。
 *
 * <p>教学要点：请求参数用 DTO 接收并加校验注解，永远不要把校验写在 Service 里手写 if；
 * Controller 方法参数前加 @Valid 即触发校验，失败自动抛 MethodArgumentNotValidException。
 */
@Data
@Schema(description = "成员注册请求")
public class MemberSaveRequest {

    @NotBlank(message = "openid 不能为空")
    @Schema(description = "微信 openid（小程序登录获取）")
    private String openid;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 32, message = "姓名最长 32 个字符")
    @Schema(description = "姓名", example = "张三")
    private String name;

    @NotNull(message = "性别不能为空")
    @Min(value = 0, message = "性别取值为 0/1/2")
    @Max(value = 2, message = "性别取值为 0/1/2")
    @Schema(description = "性别：0 未知 / 1 男 / 2 女")
    private Integer gender;

    @Min(value = 1950, message = "届别不合理")
    @Max(value = 2100, message = "届别不合理")
    @Schema(description = "届别（入学/毕业年份）", example = "2018")
    private Integer graduationYear;

    @Size(max = 32, message = "行业最长 32 个字符")
    @Schema(description = "所属行业（字典 code，见前端 constants/industry.ts）", example = "internet")
    private String industry;

    @NotBlank(message = "省份不能为空")
    @Schema(description = "所在省份", example = "四川省")
    private String province;

    @NotBlank(message = "城市不能为空")
    @Schema(description = "所在城市", example = "成都市")
    private String city;

    @NotNull(message = "纬度不能为空")
    @DecimalMin(value = "-90.0", message = "纬度范围 -90 ~ 90")
    @DecimalMax(value = "90.0", message = "纬度范围 -90 ~ 90")
    @Schema(description = "纬度", example = "30.5728")
    private BigDecimal lat;

    @NotNull(message = "经度不能为空")
    @DecimalMin(value = "-180.0", message = "经度范围 -180 ~ 180")
    @DecimalMax(value = "180.0", message = "经度范围 -180 ~ 180")
    @Schema(description = "经度", example = "104.0668")
    private BigDecimal lng;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号（可留空）", example = "13800138000")
    private String phone;

    @Size(max = 200, message = "简介最长 200 个字符")
    @Schema(description = "个人简介")
    private String intro;

    @Schema(description = "头像 URL（二期接入文件上传后由前端先传后填）")
    private String avatarUrl;
}
