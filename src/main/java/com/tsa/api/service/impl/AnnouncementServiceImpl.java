package com.tsa.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tsa.api.entity.Announcement;
import com.tsa.api.mapper.AnnouncementMapper;
import com.tsa.api.service.AnnouncementService;
import org.springframework.stereotype.Service;

import java.util.List;

/** 公告业务实现。 */
@Service
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement>
        implements AnnouncementService {

    @Override
    public List<Announcement> listForHome() {
        return this.list(new LambdaQueryWrapper<Announcement>()
                .orderByDesc(Announcement::getIsTop)
                .orderByDesc(Announcement::getPublishedAt));
    }
}
