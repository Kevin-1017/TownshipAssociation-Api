package com.tsa.api.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tsa.api.dto.MapMarkerVO;
import com.tsa.api.dto.MemberQuery;
import com.tsa.api.dto.MemberVO;
import com.tsa.api.dto.PageVO;
import com.tsa.api.dto.ProvinceStatVO;
import com.tsa.api.entity.Member;
import com.tsa.api.mapper.MemberMapper;
import com.tsa.api.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 成员业务实现：全部读端点统一限定 status=1（审核通过），待审核数据不对外可见。
 */
@Service
@RequiredArgsConstructor
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberService {

    /** 审核状态：通过 */
    private static final int STATUS_APPROVED = 1;

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
        // select 限定列：只查地图需要的字段，字段裁剪到最小是这份端点的对外口径
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
    public List<ProvinceStatVO> listProvinceStats() {
        // 聚合查询没有实体可映射，listMaps 是唯一形状；QueryWrapper（非 Lambda 版）因为
        // select 里是 SQL 片段（COUNT(*) AS cnt）不是方法引用能表达的。
        // @TableLogic 自动拼 deleted=0，与分页列表同一可见面口径。
        List<Map<String, Object>> rows = this.listMaps(new QueryWrapper<Member>()
                .select("province", "COUNT(*) AS cnt")
                .eq("status", STATUS_APPROVED)
                .groupBy("province")
                .orderByDesc("cnt"));
        return rows.stream()
                .map(row -> new ProvinceStatVO(
                        (String) row.get("province"),
                        // COUNT(*) 经 JDBC 可能是 Long/BigInteger，统一走 Number 收口
                        row.get("cnt") == null ? 0L : ((Number) row.get("cnt")).longValue()))
                .toList();
    }
}
