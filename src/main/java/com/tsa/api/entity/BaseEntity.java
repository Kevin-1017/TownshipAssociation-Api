package com.tsa.api.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实体基类：所有表的公共字段。
 *
 * <p>教学要点：
 * <ul>
 *   <li>id 用数据库自增（IdType.AUTO），简单直观</li>
 *   <li>createdAt / updatedAt 由 MybatisPlusConfig 中的 MetaObjectHandler 自动填充，业务代码不用管</li>
 *   <li>deleted 是"逻辑删除"标记：delete 语句实际执行 UPDATE ... SET deleted=1，数据可追溯</li>
 * </ul>
 */
@Data
public abstract class BaseEntity {

    /**
     * 主键序列化为字符串输出：与前端契约一致（id 一律字符串），
     * 同时避免 Long 超过 2^53 时前端 JS number 精度丢失。数据库里仍是 BIGINT。
     */
    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 创建时间：插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间：插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除标记：0 正常 / 1 已删除；@TableLogic 让 MP 自动在查询上拼 deleted=0 */
    @JsonIgnore
    @TableLogic
    private Integer deleted;
}
