package com.tsa.api.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.tsa.api.dto.CommentSaveRequest;
import com.tsa.api.dto.CommentVO;
import com.tsa.api.dto.CommunityCommentAdminQuery;
import com.tsa.api.dto.CommunityPostAdminQuery;
import com.tsa.api.dto.CommunityPostQuery;
import com.tsa.api.dto.CommunityPostSaveRequest;
import com.tsa.api.dto.CommunityPostVO;
import com.tsa.api.dto.PageVO;
import com.tsa.api.entity.CommunityPost;

/** 社区动态业务接口（美食基地 / 校园广场）。2026-09-14 起发布走审核制：新帖待审，管理端审后公开。 */
public interface CommunityService extends IService<CommunityPost> {

    /** 公开分页 + 条件查询，只下发已通过（status=1），按发布时间倒序。 */
    PageVO<CommunityPostVO> pageQuery(CommunityPostQuery query);

    /** 公开详情（含评论列表）；不存在<b>或未通过审核</b>抛 BusinessException(DATA_NOT_FOUND)。 */
    CommunityPostVO getDetail(Long id);

    /** 发布动态：一律落为待审核（status=0），返回新动态 id。 */
    Long publish(CommunityPostSaveRequest request);

    /** 管理端分页（含待审/驳回，status/type 可筛），按发布时间倒序。 */
    PageVO<CommunityPostVO> adminPageQuery(CommunityPostAdminQuery query);

    /** 审核：置为通过(1)/驳回(2)；动态不存在抛 1002。驳回可恢复（再传 1 即回通过）。 */
    void audit(Long id, int status);

    /** 评论管理端分页（含待审/驳回，status/postId 可筛），按评论时间倒序。 */
    PageVO<CommentVO> adminCommentPage(CommunityCommentAdminQuery query);

    /** 评论审核：置为通过(1)/驳回(2)；评论不存在抛 1002。与帖子审核同构（驳回可恢复）。 */
    void auditComment(Long id, int status);

    /** 点赞 +1，返回点赞后的总数；动态不存在抛 1002。 */
    Integer like(Long id);

    /** 发表评论（一律落待审，审核通过后才随详情下发、才计入条数），返回新建的评论；动态不存在抛 1002。 */
    CommentVO comment(Long postId, CommentSaveRequest request);
}
