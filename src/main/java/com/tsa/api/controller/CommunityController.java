package com.tsa.api.controller;

import com.tsa.api.common.Result;
import com.tsa.api.config.ApiConstants;
import com.tsa.api.dto.CommentSaveRequest;
import com.tsa.api.dto.CommentVO;
import com.tsa.api.dto.CommunityPostQuery;
import com.tsa.api.dto.CommunityPostSaveRequest;
import com.tsa.api.dto.CommunityPostVO;
import com.tsa.api.dto.PageVO;
import com.tsa.api.service.CommunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 社区动态接口（美食基地 / 校园广场）。
 *
 * <p>一期无登录：发布与评论的作者为表单自由填写的昵称；点赞只做计数自增。
 *
 * <p><b>2026-09-12 状态</b>：小程序端已将本组功能下线（个人主体不可提供 UGC 发布/浏览，
 * 见运营规范 5.7.1），接口暂不再被客户端调用。服务端<b>刻意保留</b>接口与数据：
 * 主体变更（个人→非个人）并报备【社交-社区/论坛】类目后，社区功能按原契约回归，届时恢复调用即可。
 */
@Tag(name = "社区动态", description = "美食基地 / 校园广场：列表、详情、发布、点赞、评论")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    @Operation(summary = "动态分页列表", description = "支持 type/cuisine/region 筛选与标题/正文关键字搜索，按发布时间倒序")
    @GetMapping("/posts")
    public Result<PageVO<CommunityPostVO>> page(CommunityPostQuery query) {
        return Result.ok(communityService.pageQuery(query));
    }

    @Operation(summary = "动态详情", description = "含评论列表 commentsList；不存在返回 1002")
    @GetMapping("/posts/{id}")
    public Result<CommunityPostVO> detail(@PathVariable Long id) {
        return Result.ok(communityService.getDetail(id));
    }

    @Operation(summary = "发布动态", description = "返回新动态 id（字符串形式）")
    @PostMapping("/posts")
    public Result<String> publish(@Valid @RequestBody CommunityPostSaveRequest request) {
        return Result.ok(String.valueOf(communityService.publish(request)));
    }

    @Operation(summary = "点赞", description = "点赞数 +1，返回点赞后的总数；动态不存在返回 1002")
    @PostMapping("/posts/{id}/like")
    public Result<Integer> like(@PathVariable Long id) {
        return Result.ok(communityService.like(id));
    }

    @Operation(summary = "发表评论", description = "返回新建的评论；动态不存在返回 1002")
    @PostMapping("/posts/{id}/comments")
    public Result<CommentVO> comment(@PathVariable Long id, @Valid @RequestBody CommentSaveRequest request) {
        return Result.ok(communityService.comment(id, request));
    }
}
