package com.tsa.api.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.tsa.api.dto.MapMarkerVO;
import com.tsa.api.dto.MemberDetailVO;
import com.tsa.api.dto.MemberQuery;
import com.tsa.api.dto.MemberSaveRequest;
import com.tsa.api.dto.MemberVO;
import com.tsa.api.dto.PageVO;
import com.tsa.api.dto.ProvinceStatVO;
import com.tsa.api.entity.Member;

import java.util.List;

/**
 * 成员业务接口。
 *
 * <p>教学要点：Controller 只依赖接口不依赖实现类（面向接口编程），
 * 这也是后续 L5 任务（重构/替换实现）不动调用方的前提。
 */
public interface MemberService extends IService<Member> {

    /** 分页 + 条件查询（仅返回审核通过的成员），出参为契约分页结构 + 公开 VO */
    PageVO<MemberVO> pageQuery(MemberQuery query);

    /** 地图打点数据（仅审核通过且有坐标的成员，轻量字段） */
    List<MapMarkerVO> listMapMarkers();

    /**
     * 注册建档（openid 已存在时抛 BusinessException）。
     *
     * <p>openid 由 Controller 从 Bearer loginId 推导传入（契约 C9，修订 A7/B16 防钓鱼：
     * 请求体已删除 openid 字段，身份只认服务端会话）。
     */
    Member register(MemberSaveRequest request, String openid);

    /** 省份分布统计（契约 C4）：仅 status=1 且未逻辑删除的成员，按人数降序 */
    List<ProvinceStatVO> listProvinceStats();

    /**
     * 成员详情（乡会用户专享）。
     *
     * <p>assocToken 由 verify-phone 核验成功后签发（前端放 X-Assoc-Token 请求头）。
     * 未核验/失效抛 1301；contactVisible=false 时响应剔除联系方式字段。
     */
    MemberDetailVO getDetail(Long id, String assocToken);
}
