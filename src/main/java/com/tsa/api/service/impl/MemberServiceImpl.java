package com.tsa.api.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tsa.api.common.BusinessException;
import com.tsa.api.common.ResultCode;
import com.tsa.api.dto.MapMarkerVO;
import com.tsa.api.dto.MemberDetailVO;
import com.tsa.api.dto.MemberQuery;
import com.tsa.api.dto.MemberSaveRequest;
import com.tsa.api.dto.MemberVO;
import com.tsa.api.dto.PageVO;
import com.tsa.api.entity.Member;
import com.tsa.api.mapper.MemberMapper;
import com.tsa.api.service.AuthService;
import com.tsa.api.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Year;
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
@RequiredArgsConstructor
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberService {

    /** 审核状态：通过 */
    private static final int STATUS_APPROVED = 1;
    /** 审核状态：待审核 */
    private static final int STATUS_PENDING = 0;
    /** 联系方式可见：是 */
    private static final int CONTACT_VISIBLE = 1;
    /** 国家常量：一期只有国内成员，字段保留是为了二期海外潮籍乡亲不返工 */
    private static final String COUNTRY_DEFAULT = "中国";

    private final AuthService authService;

    @Override
    public PageVO<MemberVO> pageQuery(MemberQuery query) {
        // 每页条数钳制在 1~50，防止有人传 pageSize=10000 拖垮数据库
        long size = Math.min(Math.max(query.getPageSize() == null ? 10 : query.getPageSize(), 1), 50);
        long page = Math.max(query.getPage() == null ? 1 : query.getPage(), 1);

        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<Member>()
                .eq(Member::getStatus, STATUS_APPROVED)
                .eq(StringUtils.hasText(query.getProvince()), Member::getProvince, query.getProvince())
                .eq(StringUtils.hasText(query.getCity()), Member::getCity, query.getCity())
                .eq(StringUtils.hasText(query.getIndustry()), Member::getIndustry, query.getIndustry())
                .and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(Member::getName, query.getKeyword())
                        .or().like(Member::getIntro, query.getKeyword()))
                .orderByDesc(Member::getId);

        // convert 把实体投影成公开 VO：裁掉 openid 等内部字段
        return PageVO.of(this.page(new Page<>(page, size), wrapper)
                .convert(m -> BeanUtil.copyProperties(m, MemberVO.class)));
    }

    @Override
    public List<MapMarkerVO> listMapMarkers() {
        // select 限定列：只查地图需要的字段，二期会在这里加 Redis 缓存
        List<Member> members = this.list(new LambdaQueryWrapper<Member>()
                .select(Member::getId, Member::getName, Member::getProvince, Member::getCity,
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

    @Override
    public MemberDetailVO getDetail(Long id, String assocToken) {
        // 顺序即安全策略：先鉴权、后查存在性。反过来会让未鉴权的任何人
        // 拿数字 id 探测「某成员是否存在」（1301 与 1002 是两个不同的响应）
        authService.ensureAssoc(assocToken);

        Member member = this.getById(id);
        if (member == null || !Integer.valueOf(STATUS_APPROVED).equals(member.getStatus())) {
            // 不存在与待审核/已拒绝同口径 1002：不给外部任何状态线索，与分页列表口径一致
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        // phone/wechatId/contactVisible 不在拷贝列表里 —— 它们要经过隐私分支显式处理，
        // 防止 copyProperties 把联系方式直接带过去绕开裁剪
        MemberDetailVO vo = BeanUtil.copyProperties(member, MemberDetailVO.class,
                "phone", "wechatId", "contactVisible");
        vo.setContactVisible(Integer.valueOf(CONTACT_VISIBLE).equals(member.getContactVisible()));
        vo.setCountry(COUNTRY_DEFAULT);
        if (member.getGraduationYear() != null) {
            vo.setSeniority(Year.now().getValue() - member.getGraduationYear());
        }

        if (Integer.valueOf(CONTACT_VISIBLE).equals(member.getContactVisible())) {
            vo.setPhone(member.getPhone());
            vo.setWechatId(member.getWechatId());
        } else {
            // 显式置 null 双保险：配合 @JsonInclude(NON_NULL) 让字段整个不出现在响应里（剔除，而非 null）
            vo.setPhone(null);
            vo.setWechatId(null);
        }
        return vo;
    }
}
