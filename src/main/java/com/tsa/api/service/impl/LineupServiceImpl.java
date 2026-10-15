package com.tsa.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tsa.api.common.BusinessException;
import com.tsa.api.common.ResultCode;
import com.tsa.api.dto.LineupSaveRequest;
import com.tsa.api.dto.LineupVO;
import com.tsa.api.entity.Lineup;
import com.tsa.api.mapper.LineupMapper;
import com.tsa.api.service.LineupService;
import org.springframework.stereotype.Service;

import java.util.List;

/** CS 道具点位业务实现。 */
@Service
public class LineupServiceImpl extends ServiceImpl<LineupMapper, Lineup> implements LineupService {

    /**
     * 列表上限（规范 3.8「查询必须带分页或明确上限」）：指南数据量级为几十条，
     * 走 page(1, MAX) 钳死上限，超限说明数据异常，宁可截断也不裸查全表。
     */
    private static final int MAX_LIST_LIMIT = 200;

    @Override
    public List<LineupVO> listAll() {
        Page<Lineup> page = this.page(new Page<>(1, MAX_LIST_LIMIT),
                new LambdaQueryWrapper<Lineup>()
                        .orderByAsc(Lineup::getMap)
                        .orderByAsc(Lineup::getSite)
                        .orderByAsc(Lineup::getKind)
                        .orderByAsc(Lineup::getId));
        return page.getRecords().stream().map(this::toVO).toList();
    }

    @Override
    public Long create(LineupSaveRequest request) {
        Lineup lineup = new Lineup();
        applyRequest(lineup, request);
        this.save(lineup);
        return lineup.getId();
    }

    @Override
    public void update(Long id, LineupSaveRequest request) {
        Lineup lineup = this.getById(id);
        if (lineup == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "点位不存在：id=" + id);
        }
        applyRequest(lineup, request);
        this.updateById(lineup);
    }

    @Override
    public void delete(Long id) {
        Lineup lineup = this.getById(id);
        if (lineup == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "点位不存在：id=" + id);
        }
        // @TableLogic 逻辑删除：实际 UPDATE ... SET deleted=1
        this.removeById(id);
    }

    /**
     * 请求 → 实体整单覆写（新增/修改共用）：管理表单总是带齐全部字段提交，
     * 语义与公告一致（见 LineupSaveRequest 类注释）。steps 去引用防外部改动；
     * images 缺省落空数组，出参形状更稳（前台无需判 null）。
     */
    private void applyRequest(Lineup lineup, LineupSaveRequest request) {
        lineup.setMap(request.getMap());
        lineup.setSite(request.getSite());
        lineup.setKind(request.getKind());
        lineup.setName(request.getName());
        lineup.setStyle(request.getStyle());
        lineup.setSteps(List.copyOf(request.getSteps()));
        lineup.setNote(request.getNote());
        lineup.setImages(request.getImages() == null ? List.of() : List.copyOf(request.getImages()));
    }

    /** 实体 → VO：无形状转换，字段直抄；历史行 images 可能为 null，出口统一成空数组 */
    private LineupVO toVO(Lineup lineup) {
        LineupVO vo = new LineupVO();
        vo.setId(lineup.getId());
        vo.setMap(lineup.getMap());
        vo.setSite(lineup.getSite());
        vo.setKind(lineup.getKind());
        vo.setName(lineup.getName());
        vo.setStyle(lineup.getStyle());
        vo.setSteps(lineup.getSteps());
        vo.setNote(lineup.getNote());
        vo.setImages(lineup.getImages() == null ? List.of() : lineup.getImages());
        return vo;
    }
}
