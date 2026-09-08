package com.tsa.api.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.tsa.api.dto.NoticeVO;
import com.tsa.api.entity.Announcement;

import java.util.List;

/** 公告业务接口。 */
public interface AnnouncementService extends IService<Announcement> {

    /** 公告列表：置顶优先，其次按发布时间倒序 */
    List<NoticeVO> listForHome();

    /** 公告详情；不存在时抛 BusinessException(DATA_NOT_FOUND) */
    NoticeVO getDetail(Long id);
}
