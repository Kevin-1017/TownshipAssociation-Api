package com.tsa.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tsa.api.entity.AssociationMember;
import com.tsa.api.mapper.AssociationMemberMapper;
import com.tsa.api.service.AssociationMemberService;
import org.springframework.stereotype.Service;

/**
 * 乡会用户（名册）服务实现。
 *
 * <p>@TableLogic 会自动给查询拼上 deleted=0，已移除的名册成员
 * 不再具备乡会身份，无需手写过滤。
 */
@Service
public class AssociationMemberServiceImpl
        extends ServiceImpl<AssociationMemberMapper, AssociationMember>
        implements AssociationMemberService {

    @Override
    public AssociationMember findByPhone(String phone) {
        LambdaQueryWrapper<AssociationMember> wrapper = new LambdaQueryWrapper<AssociationMember>()
                .eq(AssociationMember::getPhone, phone);
        return this.getOne(wrapper, false);
    }
}