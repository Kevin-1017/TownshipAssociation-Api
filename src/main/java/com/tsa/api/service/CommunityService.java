package com.tsa.api.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.tsa.api.dto.CommentSaveRequest;
import com.tsa.api.dto.CommentVO;
import com.tsa.api.dto.CommunityPostQuery;
import com.tsa.api.dto.CommunityPostSaveRequest;
import com.tsa.api.dto.CommunityPostVO;
import com.tsa.api.dto.PageVO;
import com.tsa.api.entity.CommunityPost;

/** 社区动态业务接口（美食基地 / 校园广场）。 */
public interface CommunityService extends IService<CommunityPost> {

    /** 分页 + 条件查询，按发布时间倒序。 */
    PageVO<CommunityPostVO> pageQuery(CommunityPostQuery query);

    /** 详情（含评论列表）；不存在抛 BusinessException(DATA_NOT_FOUND)。 */
    CommunityPostVO getDetail(Long id);

    /** 发布动态，返回新动态 id。 */
    Long publish(CommunityPostSaveRequest request);

    /** 点赞 +1，返回点赞后的总数；动态不存在抛 1002。 */
    Integer like(Long id);

    /** 发表评论，返回新建的评论；动态不存在抛 1002。 */
    CommentVO comment(Long postId, CommentSaveRequest request);
}
