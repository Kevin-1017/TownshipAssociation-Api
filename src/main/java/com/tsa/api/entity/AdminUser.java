package com.tsa.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 管理后台账号实体（表已建好，登录接口在第二期开放）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_user")
@Schema(description = "管理员账号")
public class AdminUser extends BaseEntity {

    @Schema(description = "登录用户名")
    private String username;

    /** 密码哈希绝不能返回给前端 */
    @JsonIgnore
    @Schema(description = "密码（BCrypt 哈希，不对外返回）", hidden = true)
    private String passwordHash;

    @Schema(description = "角色：1 超级管理员 / 2 普通管理员")
    private Integer role;

    @Schema(description = "最近登录时间")
    private LocalDateTime lastLoginAt;
}
