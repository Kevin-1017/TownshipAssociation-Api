package com.tsa.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tsa.api.entity.Member;

/**
 * 成员表数据访问层：继承 BaseMapper 获得单表增删改查与分页，
 * 复杂统计（如地图聚合）如需再在 XML 或 @Select 中补充。
 */
public interface MemberMapper extends BaseMapper<Member> {
}
