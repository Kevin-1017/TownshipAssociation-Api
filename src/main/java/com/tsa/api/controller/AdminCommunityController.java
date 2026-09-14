package com.tsa.api.controller;

import com.tsa.api.common.Result;
import com.tsa.api.config.ApiConstants;
import com.tsa.api.dto.CommunityAuditRequest;
import com.tsa.api.dto.CommunityCommentAdminQuery;
import com.tsa.api.dto.CommunityPostAdminQuery;
import com.tsa.api.dto.CommunityPostVO;
import com.tsa.api.dto.CommentVO;
import com.tsa.api.dto.PageVO;
import com.tsa.api.service.CommunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 社区动态管理接口（2026-09-14 审核制）：待审列表 + 通过/驳回。
 *
 * <p>鉴权在路由层统一收口（SaTokenConfig 的 /tsa/admin/** → checkRole("admin")），
 * 本类不再自建闸门；Controller 零逻辑，审核规则与状态校验全在 CommunityService。
 */
@Tag(name = "社区管理", description = "动态审核：列表（含待审/驳回）与审核动作")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/admin/community")
@RequiredArgsConstructor
public class AdminCommunityController {

    private final CommunityService communityService;

    @Operation(summary = "动态分页列表（管理端）", description = "含待审/驳倒在内容；query：page/pageSize(钳1..100)/"
            + "status(0待审/1已过/2已驳回，缺省全部)/type(food/campus，缺省全部)，按发布时间倒序")
    @GetMapping("/posts")
    public Result<PageVO<CommunityPostVO>> page(CommunityPostAdminQuery query) {
        return Result.ok(communityService.adminPageQuery(query));
    }

    @Operation(summary = "审核动态", description = "body {status: 1通过|2驳回}；驳回可恢复（再传 1 即回通过）；"
            + "id 不存在 1002；成功返回 Void")
    @PutMapping("/posts/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody CommunityAuditRequest request) {
        communityService.audit(id, request.getStatus());
        return Result.ok(null);
    }

    @Operation(summary = "评论分页列表（管理端）", description = "含待审/驳回；query：page/pageSize(钳1..100)/"
            + "status(0待审/1已过/2已驳，缺省全部)/postId(可选，只看某动态下评论)，按评论时间倒序")
    @GetMapping("/comments")
    public Result<PageVO<CommentVO>> comments(CommunityCommentAdminQuery query) {
        return Result.ok(communityService.adminCommentPage(query));
    }

    @Operation(summary = "审核评论", description = "body {status: 1通过|2驳回}；驳回可恢复；id 不存在 1002；成功返回 Void")
    @PutMapping("/comments/{id}/audit")
    public Result<Void> auditComment(@PathVariable Long id, @Valid @RequestBody CommunityAuditRequest request) {
        communityService.auditComment(id, request.getStatus());
        return Result.ok(null);
    }
}
