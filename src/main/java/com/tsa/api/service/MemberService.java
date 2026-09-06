package com.tsa.api.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.tsa.api.dto.MapMarkerVO;
import com.tsa.api.dto.MemberQuery;
import com.tsa.api.dto.MemberSaveRequest;
import com.tsa.api.entity.Member;

import java.util.List;

/**
 * 成员业务接口。
 *
 * <p>教学要点：Controller 只依赖接口不依赖实现类（面向接口编程），
 * 这也是后续 L5 任务（重构/替换实现）不动调用方的前提。
 */
public interface MemberService extends IService<Member> {

    /** 分页 + 条件查询（仅返回审核通过的成员） */
    IPage<Member> pageQuery(MemberQuery query);

    /** 地图打点数据（仅审核通过且有坐标的成员，轻量字段） */
    List<MapMarkerVO> listMapMarkers();

    /** 注册/更新成员资料；openid 已存在时抛 BusinessException */
    Member register(MemberSaveRequest request);
}
