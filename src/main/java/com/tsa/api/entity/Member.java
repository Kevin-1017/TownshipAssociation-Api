package com.tsa.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 乡会成员表实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("member")
@Schema(description = "乡会成员")
public class Member extends BaseEntity {

    @Schema(description = "微信 openid（小程序登录凭证，唯一）")
    private String openid;

    @Schema(description = "姓名", example = "张三")
    private String name;

    @Schema(description = "头像 URL")
    private String avatarUrl;

    @Schema(description = "性别：0 未知 / 1 男 / 2 女")
    private Integer gender;

    @Schema(description = "届别（入学/毕业年份）", example = "2018")
    private Integer graduationYear;

    @Schema(description = "所属行业", example = "互联网")
    private String industry;

    @Schema(description = "所在省份", example = "四川省")
    private String province;

    @Schema(description = "所在城市", example = "成都市")
    private String city;

    @Schema(description = "纬度（地图打点用）")
    private BigDecimal lat;

    @Schema(description = "经度（地图打点用）")
    private BigDecimal lng;

    /** 手机号仅服务端与管理员可见，普通接口不返回 —— @JsonIgnore 屏蔽序列化 */
    @JsonIgnore
    @Schema(description = "手机号（不对外返回）", hidden = true)
    private String phone;

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

    /** 微信号与手机号同类敏感：实体序列化出口统一屏蔽，详情接口在 Service 层按 contractVisible 显式裁剪 */
    @JsonIgnore
    @Schema(description = "微信号（不对外返回）", hidden = true)
    private String wechatId;

    @Schema(description = "联系方式可见：0 否 / 1 是（后端据此裁掉联系方式字段）")
    private Integer contactVisible;

    @Schema(description = "审核状态：0 待审核 / 1 已通过 / 2 已拒绝")
    private Integer status;

    @Schema(description = "来源：0 后台种子 / 1 本人提交（v1.2 D3 profile 真保存建档时置 1）")
    private Integer source;

    @Schema(description = "个人简介")
    private String intro;
}
