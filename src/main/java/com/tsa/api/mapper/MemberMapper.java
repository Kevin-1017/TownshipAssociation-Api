package com.tsa.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tsa.api.entity.Member;

/**
 * 成员表数据访问层。
 *
 * <p>继承 BaseMapper 后即拥有单表的增删改查 + 分页能力，
 * 复杂 SQL（如地图聚合统计）再在 XML 或 @Select 中补充。
 */
public interface MemberMapper extends BaseMapper<Member> {
}
