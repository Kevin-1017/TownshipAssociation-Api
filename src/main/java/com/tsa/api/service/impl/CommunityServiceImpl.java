package com.tsa.api.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tsa.api.common.BusinessException;
import com.tsa.api.common.ResultCode;
import com.tsa.api.dto.CommentSaveRequest;
import com.tsa.api.dto.CommentVO;
import com.tsa.api.dto.CommunityCommentAdminQuery;
import com.tsa.api.dto.CommunityPostAdminQuery;
import com.tsa.api.dto.CommunityPostQuery;
import com.tsa.api.dto.CommunityPostSaveRequest;
import com.tsa.api.dto.CommunityPostVO;
import com.tsa.api.dto.PageVO;
import com.tsa.api.entity.CommunityComment;
import com.tsa.api.entity.CommunityPost;
import com.tsa.api.mapper.CommunityCommentMapper;
import com.tsa.api.mapper.CommunityPostMapper;
import com.tsa.api.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 社区动态业务实现。
 *
 * <p>评论条数不入库：列表页一次查询把本页动态的评论按 post_id 分组计数，避免逐条 count 的 N+1，
 * 也不会出现「计数与子表不一致」。详情页直接加载整棵评论树。
 *
 * <p>审核制（2026-09-14 帖子 / 2026-09-15 评论）：publish 与 comment 一律落待审（status=0），
 * 公开读路径（列表/详情/点赞/评论入口）只放行 status=1；驳回（2）保留记录可审回
 * （用户拍板「驳回可恢复」），存量行由迁移脚本的 DEFAULT 1 视为已过审，无需人工处理。
 */
@Service
@RequiredArgsConstructor
public class CommunityServiceImpl extends ServiceImpl<CommunityPostMapper, CommunityPost>
        implements CommunityService {

    /** 分页每页条数上限。 */
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 审核状态（status 列字面量，语义与 CommunityPost#status 注释同源） */
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;

    private final CommunityCommentMapper commentMapper;

    @Override
    public PageVO<CommunityPostVO> pageQuery(CommunityPostQuery query) {
        long size = Math.min(Math.max(query.getPageSize() == null ? DEFAULT_PAGE_SIZE : query.getPageSize(), 1), MAX_PAGE_SIZE);
        long page = Math.max(query.getPage() == null ? 1 : query.getPage(), 1);

        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getStatus, STATUS_APPROVED)
                .eq(StringUtils.hasText(query.getType()), CommunityPost::getType, query.getType())
                .eq(StringUtils.hasText(query.getCuisine()), CommunityPost::getCuisine, query.getCuisine())
                .eq(StringUtils.hasText(query.getRegion()), CommunityPost::getRegion, query.getRegion())
                .and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(CommunityPost::getTitle, query.getKeyword())
                        .or().like(CommunityPost::getContent, query.getKeyword()))
                .orderByDesc(CommunityPost::getPublishTime);

        IPage<CommunityPost> ipage = this.page(new Page<>(page, size), wrapper);
        Map<Long, Integer> counts = commentCounts(ipage.getRecords().stream().map(CommunityPost::getId).toList());
        List<CommunityPostVO> vos = ipage.getRecords().stream()
                .map(p -> toVO(p, counts.getOrDefault(p.getId(), 0)))
                .toList();
        return new PageVO<>(vos, ipage.getTotal(), ipage.getCurrent(), ipage.getSize());
    }

    @Override
    public CommunityPostVO getDetail(Long id) {
        // 出口审核闸门收在 requireApproved：未过审内容即使被直链也按「不存在」处理
        CommunityPost post = requireApproved(id);
        // 评论树同样只放已过审（2026-09-15 评论审核制）；待审评论对所有人不可见，只在管理端队列里
        List<CommunityComment> comments = commentMapper.selectList(new LambdaQueryWrapper<CommunityComment>()
                .eq(CommunityComment::getPostId, id)
                .eq(CommunityComment::getStatus, STATUS_APPROVED)
                .orderByDesc(CommunityComment::getCreateTime));
        CommunityPostVO vo = toVO(post, comments.size());
        vo.setCommentsList(comments.stream().map(this::toCommentVO).toList());
        return vo;
    }

    @Override
    public Long publish(CommunityPostSaveRequest request) {
        CommunityPost post = BeanUtil.copyProperties(request, CommunityPost.class);
        post.setImages(request.getImages() == null ? Collections.emptyList() : request.getImages());
        post.setLikes(0);
        // 一律待审：status 不接受客户端自证（SaveRequest 无该字段，copyProperties 拷不进来，这里显式给值）
        post.setStatus(STATUS_PENDING);
        post.setPublishTime(LocalDateTime.now());
        this.save(post);
        return post.getId();
    }

    @Override
    public PageVO<CommunityPostVO> adminPageQuery(CommunityPostAdminQuery query) {
        long size = Math.min(Math.max(query.getPageSize() == null ? DEFAULT_PAGE_SIZE : query.getPageSize(), 1), MAX_PAGE_SIZE);
        long page = Math.max(query.getPage() == null ? 1 : query.getPage(), 1);

        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<CommunityPost>()
                .eq(query.getStatus() != null, CommunityPost::getStatus, query.getStatus())
                .eq(StringUtils.hasText(query.getType()), CommunityPost::getType, query.getType())
                .orderByDesc(CommunityPost::getPublishTime);

        IPage<CommunityPost> ipage = this.page(new Page<>(page, size), wrapper);
        Map<Long, Integer> counts = commentCounts(ipage.getRecords().stream().map(CommunityPost::getId).toList());
        List<CommunityPostVO> vos = ipage.getRecords().stream()
                .map(p -> toVO(p, counts.getOrDefault(p.getId(), 0)))
                .toList();
        return new PageVO<>(vos, ipage.getTotal(), ipage.getCurrent(), ipage.getSize());
    }

    @Override
    public void audit(Long id, int status) {
        if (status != STATUS_APPROVED && status != STATUS_REJECTED) {
            // 双保险：CommunityAuditRequest 已用 @Min/@Max 挡过一道，Service 不信任任何调用方
            throw new BusinessException(ResultCode.BAD_REQUEST, "审核结果只能是 1 通过或 2 驳回");
        }
        CommunityPost post = this.getById(id);
        if (post == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "动态不存在：id=" + id);
        }
        post.setStatus(status);
        this.updateById(post);
    }

    @Override
    public Integer like(Long id) {
        CommunityPost post = requireApproved(id);
        post.setLikes(post.getLikes() + 1);
        this.updateById(post);
        return post.getLikes();
    }

    @Override
    public CommentVO comment(Long postId, CommentSaveRequest request) {
        requireApproved(postId);
        CommunityComment comment = new CommunityComment();
        comment.setPostId(postId);
        comment.setAuthor(request.getAuthor());
        comment.setAvatar(request.getAvatar());
        comment.setContent(request.getContent());
        comment.setLikes(0);
        // 一律待审：审核通过前不进详情评论树、不计数（用户定稿「评论应该过审」）
        comment.setStatus(STATUS_PENDING);
        comment.setCreateTime(LocalDateTime.now());
        commentMapper.insert(comment);
        return toCommentVO(comment);
    }

    @Override
    public PageVO<CommentVO> adminCommentPage(CommunityCommentAdminQuery query) {
        long size = Math.min(Math.max(query.getPageSize() == null ? DEFAULT_PAGE_SIZE : query.getPageSize(), 1), MAX_PAGE_SIZE);
        long page = Math.max(query.getPage() == null ? 1 : query.getPage(), 1);
        LambdaQueryWrapper<CommunityComment> wrapper = new LambdaQueryWrapper<CommunityComment>()
                .eq(query.getStatus() != null, CommunityComment::getStatus, query.getStatus())
                .eq(query.getPostId() != null, CommunityComment::getPostId, query.getPostId())
                .orderByDesc(CommunityComment::getCreateTime);
        IPage<CommunityComment> ipage = commentMapper.selectPage(new Page<>(page, size), wrapper);
        List<CommentVO> vos = ipage.getRecords().stream().map(this::toCommentVO).toList();
        return new PageVO<>(vos, ipage.getTotal(), ipage.getCurrent(), ipage.getSize());
    }

    @Override
    public void auditComment(Long id, int status) {
        if (status != STATUS_APPROVED && status != STATUS_REJECTED) {
            // 双保险：请求 DTO 已 @Min/@Max 挡过，Service 不信任任何调用方（与帖子审核同款）
            throw new BusinessException(ResultCode.BAD_REQUEST, "审核结果只能是 1 通过或 2 驳回");
        }
        CommunityComment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "评论不存在：id=" + id);
        }
        comment.setStatus(status);
        commentMapper.updateById(comment);
    }

    // ---------- private 辅助 ----------

    /** 公开读写路径的审核闸门：不存在或未过审一律 1002，不给「直链窥探待审内容」留缝 */
    private CommunityPost requireApproved(Long id) {
        CommunityPost post = this.getById(id);
        if (post == null || post.getStatus() == null || post.getStatus() != STATUS_APPROVED) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "动态不存在：id=" + id);
        }
        return post;
    }

    /** 一次查询取回本页动态的已过审评论 id 列表，在内存按 post_id 计数，规避 N+1（待审评论不计数）。 */
    private Map<Long, Integer> commentCounts(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<CommunityComment> rows = commentMapper.selectList(new LambdaQueryWrapper<CommunityComment>()
                .select(CommunityComment::getPostId)
                .eq(CommunityComment::getStatus, STATUS_APPROVED)
                .in(CommunityComment::getPostId, postIds));
        return rows.stream().collect(Collectors.groupingBy(
                CommunityComment::getPostId, Collectors.reducing(0, c -> 1, Integer::sum)));
    }

    private CommunityPostVO toVO(CommunityPost post, int comments) {
        CommunityPostVO vo = BeanUtil.copyProperties(post, CommunityPostVO.class);
        vo.setImages(post.getImages() == null ? Collections.emptyList() : post.getImages());
        vo.setComments(comments);
        return vo;
    }

    private CommentVO toCommentVO(CommunityComment comment) {
        return BeanUtil.copyProperties(comment, CommentVO.class);
    }
}
