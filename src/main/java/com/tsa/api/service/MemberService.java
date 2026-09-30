package com.tsa.api.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.tsa.api.dto.MapMarkerVO;
import com.tsa.api.dto.MemberQuery;
import com.tsa.api.dto.MemberVO;
import com.tsa.api.dto.PageVO;
import com.tsa.api.dto.ProvinceStatVO;
import com.tsa.api.entity.Member;

import java.util.List;

/**
 * 成员业务接口（公开读侧）。Controller 只依赖本接口不依赖实现类。
 */
public interface MemberService extends IService<Member> {

    /** 分页 + 条件查询（仅返回审核通过的成员），出参为分页结构 + 公开 VO */
    PageVO<MemberVO> pageQuery(MemberQuery query);

    /** 地图打点数据（仅审核通过且有坐标的成员，轻量字段） */
    List<MapMarkerVO> listMapMarkers();

    /** 省份分布统计：仅 status=1 且未逻辑删除的成员，按人数降序 */
    List<ProvinceStatVO> listProvinceStats();
}
