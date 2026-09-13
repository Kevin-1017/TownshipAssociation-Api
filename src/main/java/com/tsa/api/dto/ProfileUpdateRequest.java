package com.tsa.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 本人资料更新请求（契约 C7，PUT /tsa/user/profile）。
 *
 * <p>与 {@link MemberSaveRequest}（后台注册建档）的区别是本请求<b>全部字段可选</b>：
 * 已登录用户在资料页改哪项就传哪项，后端按白名单逐列 update，绝不整行覆盖。
 * openid 不在请求体里 —— 由 Bearer 登录态推导（防钓鱼建档，修订 A7/B16 同源逻辑）。
 * 字段长度上限与 member 表列宽一一对应，校验放这里而不是 Service 手写 if。
 */
@Data
@Schema(description = "本人资料更新请求（全字段可选）")
public class ProfileUpdateRequest {

    @Size(max = 32, message = "姓名最长 32 个字符")
    @Schema(description = "姓名", example = "张三")
    private String name;

    @Min(value = 0, message = "性别取值为 0/1/2")
    @Max(value = 2, message = "性别取值为 0/1/2")
    @Schema(description = "性别：0 未知 / 1 男 / 2 女")
    private Integer gender;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号（可留空）", example = "13800138000")
    private String phone;

    @Size(max = 32, message = "微信号最长 32 个字符")
    @Schema(description = "微信号", example = "wx_zhangsan")
    private String wechatId;

    @Min(value = 1950, message = "届别不合理")
    @Max(value = 2100, message = "届别不合理")
    @Schema(description = "届别（入学/毕业年份）", example = "2018")
    private Integer graduationYear;

    @Size(max = 64, message = "专业最长 64 个字符")
    @Schema(description = "专业", example = "计算机科学与技术")
    private String major;

    @Size(max = 200, message = "简介最长 200 个字符")
    @Schema(description = "个人简介")
    private String intro;

    @Size(max = 255, message = "头像地址最长 255 个字符")
    @Schema(description = "头像 URL（先经 POST /tsa/files 上传，回填相对路径）")
    private String avatarUrl;
}
