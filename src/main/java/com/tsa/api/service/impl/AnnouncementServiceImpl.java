package com.tsa.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tsa.api.common.BusinessException;
import com.tsa.api.common.ResultCode;
import com.tsa.api.dto.NoticeSaveRequest;
import com.tsa.api.dto.NoticeVO;
import com.tsa.api.entity.Announcement;
import com.tsa.api.mapper.AnnouncementMapper;
import com.tsa.api.service.AnnouncementService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/** 公告业务实现。 */
@Service
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement>
        implements AnnouncementService {

    /** 置顶标记：1 置顶 / 0 普通（announcement.is_top 列口径） */
    private static final int TOP_YES = 1;
    private static final int TOP_NO = 0;

    @Override
    public List<NoticeVO> listForHome() {
        return this.list(new LambdaQueryWrapper<Announcement>()
                        .orderByDesc(Announcement::getIsTop)
                        .orderByDesc(Announcement::getPublishedAt))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public NoticeVO getDetail(Long id) {
        Announcement announcement = this.getById(id);
        if (announcement == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "公告不存在：id=" + id);
        }
        return toVO(announcement);
    }

    @Override
    public List<NoticeVO> listAllForAdmin() {
        // 管理端全量：无上下架状态列，故与首页同源全给（@TableLogic 已滤软删除），排序口径一致
        return this.list(new LambdaQueryWrapper<Announcement>()
                        .orderByDesc(Announcement::getIsTop)
                        .orderByDesc(Announcement::getPublishedAt))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public Long create(NoticeSaveRequest request) {
        Announcement announcement = new Announcement();
        applyRequest(announcement, request);
        // 新增缺省发布时间 = 当前（列 NOT NULL，不能留空）
        if (announcement.getPublishedAt() == null) {
            announcement.setPublishedAt(LocalDateTime.now());
        }
        this.save(announcement);
        return announcement.getId();
    }

    @Override
    public void update(Long id, NoticeSaveRequest request) {
        Announcement announcement = this.getById(id);
        if (announcement == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "公告不存在：id=" + id);
        }
        applyRequest(announcement, request);
        // 修改时未显式给发布时间则保留原值（applyRequest 里已按 request 是否为 null 决定覆写）
        this.updateById(announcement);
    }

    @Override
    public void delete(Long id) {
        Announcement announcement = this.getById(id);
        if (announcement == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "公告不存在：id=" + id);
        }
        // @TableLogic 逻辑删除：实际 UPDATE ... SET deleted=1
        this.removeById(id);
    }

    /**
     * 请求 → 实体覆写（新增/修改共用）：只搬请求里「有值」的字段。
     *
     * <p>pinned 布尔 → isTop 整型；publishedAt 仅在请求显式给值时覆写（改公告不传即不动原发布时间）。
     */
    private void applyRequest(Announcement announcement, NoticeSaveRequest request) {
        announcement.setTitle(request.getTitle());
        announcement.setSummary(request.getSummary());
        announcement.setContent(request.getContent());
        announcement.setIsTop(Boolean.TRUE.equals(request.getPinned()) ? TOP_YES : TOP_NO);
        if (request.getPublishedAt() != null) {
            announcement.setPublishedAt(request.getPublishedAt());
        }
    }

    /** 实体 → 契约 VO：isTop 整型转 pinned 布尔，id 由 VO 注解序列化为字符串 */
    private NoticeVO toVO(Announcement announcement) {
        NoticeVO vo = new NoticeVO();
        vo.setId(announcement.getId());
        vo.setTitle(announcement.getTitle());
        vo.setSummary(announcement.getSummary());
        vo.setContent(announcement.getContent());
        vo.setPinned(Integer.valueOf(TOP_YES).equals(announcement.getIsTop()));
        vo.setPublishedAt(announcement.getPublishedAt());
        return vo;
    }
}
