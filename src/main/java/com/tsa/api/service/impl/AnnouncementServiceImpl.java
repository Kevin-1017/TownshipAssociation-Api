package com.tsa.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tsa.api.common.BusinessException;
import com.tsa.api.common.ResultCode;
import com.tsa.api.dto.NoticeVO;
import com.tsa.api.entity.Announcement;
import com.tsa.api.mapper.AnnouncementMapper;
import com.tsa.api.service.AnnouncementService;
import org.springframework.stereotype.Service;

import java.util.List;

/** 公告业务实现。 */
@Service
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement>
        implements AnnouncementService {

    /** 置顶标记：1 置顶 */
    private static final int TOP_YES = 1;

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
