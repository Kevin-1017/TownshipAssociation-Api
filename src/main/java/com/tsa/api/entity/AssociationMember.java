package com.tsa.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 乡会用户表实体。
 *
 * <p>这张表由乡会秘书处**手动 SQL 维护**，不对外提供 CRUD 接口，
 * 因此实体上不留任何序列化出口：phone 加 @JsonIgnore 属于防御性措施，
 * 即使将来有人误把实体直接返回，也不会泄露名册手机号。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("association_member")
@Schema(description = "乡会用户（内部名册，不对外）")
public class AssociationMember extends BaseEntity {

    @JsonIgnore
    @Schema(description = "手机号（唯一区分依据，绝不下发）", hidden = true)
    private String phone;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "乡会职务（会长/理事/会员等）")
    private String role;

    @Schema(description = "备注（仅内部可见）")
    private String remark;

    /** 二期：映射 member.id，登录态自动识别乡会身份时回填，一期保持 NULL */
    @Schema(description = "关联成员 id（二期启用）")
    private Long memberRefId;
}