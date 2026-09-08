package com.tsa.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tsa.api.entity.AssociationMember;

/**
 * 乡会用户表数据访问层。仅服务端内部查询（verify-phone 比对手机号）使用，
 * 不挂任何对外接口。
 */
public interface AssociationMemberMapper extends BaseMapper<AssociationMember> {
}