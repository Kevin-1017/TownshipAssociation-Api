package com.tsa.api.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.tsa.api.dto.EventDetailVO;
import com.tsa.api.dto.EventListVO;
import com.tsa.api.dto.EventQuery;
import com.tsa.api.dto.PageVO;
import com.tsa.api.entity.Activity;

/**
 * 活动业务接口（v1.2 D2「跳公众号文章」形态：只读两接口，不做报名/富文本）。
 *
 * <p>列表与详情出参都是裁剪后的 VO（契约 C5/C6）：{@code content}（活动详情正文）
 * <b>永不外给</b> —— 正文留在公众号，小程序只展示封面/标题/摘要 + 一个文章链接。
 */
public interface EventService extends IService<Activity> {

    /** 分页 + 可选年份筛选（按 start_time 降序）；status 为派生的 upcoming/past */
    PageVO<EventListVO> pageQuery(EventQuery query);

    /** 活动详情；id 不存在抛 BusinessException(DATA_NOT_FOUND) */
    EventDetailVO detail(Long id);
}
