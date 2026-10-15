package com.tsa.api.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.tsa.api.dto.LineupSaveRequest;
import com.tsa.api.dto.LineupVO;
import com.tsa.api.entity.Lineup;

import java.util.List;

/** CS 道具点位业务接口（官网 kevin/csgo 指南数据源）。 */
public interface LineupService extends IService<Lineup> {

    /** 全量列表：按 地图 → 点位 → 道具 → id 升序；前台页签/分组由本列去重生成（服务端钳上限，不分页） */
    List<LineupVO> listAll();

    // ---------- 管理端（/tsa/admin/lineups，Sa-Token 路由墙把关 admin 角色） ----------

    /** 新增点位：返回新 id */
    Long create(LineupSaveRequest request);

    /** 修改点位：整单覆写；id 不存在抛 BusinessException(DATA_NOT_FOUND) */
    void update(Long id, LineupSaveRequest request);

    /** 删除点位（逻辑删除）：id 不存在抛 BusinessException(DATA_NOT_FOUND) */
    void delete(Long id);
}
