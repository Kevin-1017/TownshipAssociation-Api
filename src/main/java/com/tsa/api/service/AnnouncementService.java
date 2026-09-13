package com.tsa.api.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.tsa.api.dto.NoticeSaveRequest;
import com.tsa.api.dto.NoticeVO;
import com.tsa.api.entity.Announcement;

import java.util.List;

/** 公告业务接口。 */
public interface AnnouncementService extends IService<Announcement> {

    /** 公告列表：置顶优先，其次按发布时间倒序 */
    List<NoticeVO> listForHome();

    /** 公告详情；不存在时抛 BusinessException(DATA_NOT_FOUND) */
    NoticeVO getDetail(Long id);

    // ---------- 管理端（v1.3，需 admin 角色，供 AdminNoticesController 消费） ----------

    /** 管理端全量列表：置顶优先、发布时间倒序（无上下架状态列，故与首页同源全给，含未过期的全部公告） */
    List<NoticeVO> listAllForAdmin();

    /** 新增公告：返回新公告 id；publishedAt 缺省取当前时间 */
    Long create(NoticeSaveRequest request);

    /** 修改公告：id 不存在抛 BusinessException(DATA_NOT_FOUND) */
    void update(Long id, NoticeSaveRequest request);

    /** 删除公告（逻辑删除）：id 不存在抛 BusinessException(DATA_NOT_FOUND) */
    void delete(Long id);
}
