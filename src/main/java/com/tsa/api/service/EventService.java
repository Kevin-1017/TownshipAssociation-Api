package com.tsa.api.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.tsa.api.dto.EventAdminQuery;
import com.tsa.api.dto.EventDetailVO;
import com.tsa.api.dto.EventListVO;
import com.tsa.api.dto.EventQuery;
import com.tsa.api.dto.EventSaveRequest;
import com.tsa.api.dto.PageVO;
import com.tsa.api.entity.Activity;

/**
 * 活动业务接口（v1.2 D2「跳公众号文章」形态：不做报名/富文本；2026-09-14 起管理端可配置）。
 *
 * <p>列表与详情出参都是裁剪后的 VO（契约 C5/C6）：{@code content}（活动详情正文）
 * <b>永不外给</b> —— 正文留在公众号，小程序只展示封面/标题/摘要 + 一个文章链接。
 * 管理端「删除即下架」（用户定稿，不做发布/下架工作流开关）。
 */
public interface EventService extends IService<Activity> {

    /** 公开分页 + 可选年份筛选（按 start_time 降序）；status 为派生的 upcoming/past */
    PageVO<EventListVO> pageQuery(EventQuery query);

    /** 活动详情；id 不存在抛 BusinessException(DATA_NOT_FOUND) */
    EventDetailVO detail(Long id);

    /** 管理端分页（标题关键字可筛，start_time 降序），出参复用 C5 列表 VO */
    PageVO<EventListVO> adminPageQuery(EventAdminQuery query);

    /** 新建活动，返回新 id；activity 表其余列（报名/经纬度等）保持库默认 */
    Long create(EventSaveRequest request);

    /** 修改活动；id 不存在抛 1002。null=不改，空串=清除（cover/summary/articleUrl） */
    void update(Long id, EventSaveRequest request);

    /** 逻辑删除（删除即下架）；id 不存在抛 1002 */
    void delete(Long id);
}
