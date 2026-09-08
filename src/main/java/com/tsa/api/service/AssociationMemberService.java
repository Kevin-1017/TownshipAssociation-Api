package com.tsa.api.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.tsa.api.entity.AssociationMember;

/**
 * 乡会用户（名册）服务。
 *
 * <p>只服务内部身份核验，**不提供对外 CRUD** —— 名册数据由秘书处手动 SQL 维护。
 */
public interface AssociationMemberService extends IService<AssociationMember> {

    /**
     * 按手机号查名册成员。
     *
     * @return 命中返回实体，未命中（或已逻辑删除）返回 null
     */
    AssociationMember findByPhone(String phone);
}