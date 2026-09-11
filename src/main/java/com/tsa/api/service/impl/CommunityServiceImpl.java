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
 */
@Service
@RequiredArgsConstructor
public class CommunityServiceImpl extends ServiceImpl<CommunityPostMapper, CommunityPost>
        implements CommunityService {

    /** 分页每页条数上限。 */
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final CommunityCommentMapper commentMapper;

    @Override
    public PageVO<CommunityPostVO> pageQuery(CommunityPostQuery query) {
        long size = Math.min(Math.max(query.getPageSize() == null ? DEFAULT_PAGE_SIZE : query.getPageSize(), 1), MAX_PAGE_SIZE);
        long page = Math.max(query.getPage() == null ? 1 : query.getPage(), 1);

        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<CommunityPost>()
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
        CommunityPost post = this.getById(id);
        if (post == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "动态不存在：id=" + id);
        }
        List<CommunityComment> comments = commentMapper.selectList(new LambdaQueryWrapper<CommunityComment>()
                .eq(CommunityComment::getPostId, id)
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
        post.setPublishTime(LocalDateTime.now());
        this.save(post);
        return post.getId();
    }

    @Override
    public Integer like(Long id) {
        CommunityPost post = this.getById(id);
        if (post == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "动态不存在：id=" + id);
        }
        post.setLikes(post.getLikes() + 1);
        this.updateById(post);
        return post.getLikes();
    }

    @Override
    public CommentVO comment(Long postId, CommentSaveRequest request) {
        CommunityPost post = this.getById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "动态不存在：id=" + postId);
        }
        CommunityComment comment = new CommunityComment();
        comment.setPostId(postId);
        comment.setAuthor(request.getAuthor());
        comment.setAvatar(request.getAvatar());
        comment.setContent(request.getContent());
        comment.setLikes(0);
        comment.setCreateTime(LocalDateTime.now());
        commentMapper.insert(comment);
        return toCommentVO(comment);
    }

    // ---------- private 辅助 ----------

    /** 一次查询取回本页动态的评论 id 列表，在内存按 post_id 计数，规避 N+1。 */
    private Map<Long, Integer> commentCounts(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<CommunityComment> rows = commentMapper.selectList(new LambdaQueryWrapper<CommunityComment>()
                .select(CommunityComment::getPostId)
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
