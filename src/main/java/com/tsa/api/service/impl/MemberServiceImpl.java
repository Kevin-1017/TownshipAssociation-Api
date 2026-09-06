package com.tsa.api.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tsa.api.common.BusinessException;
import com.tsa.api.common.ResultCode;
import com.tsa.api.dto.MapMarkerVO;
import com.tsa.api.dto.MemberQuery;
import com.tsa.api.dto.MemberSaveRequest;
import com.tsa.api.entity.Member;
import com.tsa.api.mapper.MemberMapper;
import com.tsa.api.service.MemberService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 成员业务实现。
 *
 * <p>教学要点：
 * <ul>
 *   <li>继承 ServiceImpl 获得 save/updateById/getById 等通用方法</li>
 *   <li>LambdaQueryWrapper 用方法引用（Member::getCity）写条件，避免字符串字段名拼错</li>
 *   <li>查询统一限定 status=1（审核通过），待审核数据不对外可见</li>
 * </ul>
 */
@Service
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberService {

    /** 审核状态：通过 */
    private static final int STATUS_APPROVED = 1;
    /** 审核状态：待审核 */
    private static final int STATUS_PENDING = 0;

    @Override
    public IPage<Member> pageQuery(MemberQuery query) {
        // 每页条数钳制在 1~50，防止有人传 size=10000 拖垮数据库
        long size = Math.min(Math.max(query.getSize() == null ? 10 : query.getSize(), 1), 50);
        long page = Math.max(query.getPage() == null ? 1 : query.getPage(), 1);

        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<Member>()
                .eq(Member::getStatus, STATUS_APPROVED)
                .eq(StringUtils.hasText(query.getCity()), Member::getCity, query.getCity())
                .eq(StringUtils.hasText(query.getIndustry()), Member::getIndustry, query.getIndustry())
                .and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(Member::getName, query.getKeyword())
                        .or().like(Member::getIntro, query.getKeyword()))
                .orderByDesc(Member::getId);

        return this.page(new Page<>(page, size), wrapper);
    }

    @Override
    public List<MapMarkerVO> listMapMarkers() {
        // select 限定列：只查地图需要的字段，二期会在这里加 Redis 缓存
        List<Member> members = this.list(new LambdaQueryWrapper<Member>()
                .select(Member::getId, Member::getName, Member::getCity,
                        Member::getIndustry, Member::getAvatarUrl, Member::getLat, Member::getLng)
                .eq(Member::getStatus, STATUS_APPROVED)
                .isNotNull(Member::getLat)
                .isNotNull(Member::getLng));

        return members.stream()
                .map(m -> BeanUtil.copyProperties(m, MapMarkerVO.class))
                .toList();
    }

    @Override
    public Member register(MemberSaveRequest request) {
        Member existing = this.getOne(new LambdaQueryWrapper<Member>()
                .eq(Member::getOpenid, request.getOpenid()));
        if (existing != null) {
            throw new BusinessException(ResultCode.MEMBER_ALREADY_EXISTS);
        }

        Member member = BeanUtil.copyProperties(request, Member.class);
        // 新注册一律待审核，由管理后台通过后才对外可见（审核接口二期做）
        member.setStatus(STATUS_PENDING);
        this.save(member);
        return member;
    }
}
