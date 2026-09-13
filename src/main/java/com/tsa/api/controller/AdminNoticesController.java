package com.tsa.api.controller;

import com.tsa.api.common.Result;
import com.tsa.api.config.ApiConstants;
import com.tsa.api.dto.NoticeSaveRequest;
import com.tsa.api.dto.NoticeVO;
import com.tsa.api.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公告管理端接口（v1.3 新能力，需 admin 角色，挂在 /tsa/admin/notices）。
 *
 * <p>公开的只读公告仍走 {@code AnnouncementController}（/tsa/notices/**），本类只做增删改与全量列表。
 * 整个 /tsa/admin/** 前缀由 SaTokenConfig 的 {@code checkRole("admin")} 把门。
 */
@Tag(name = "公告（管理端）", description = "乡会公告的增删改与全量列表（需 admin 角色）")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/admin/notices")
@RequiredArgsConstructor
public class AdminNoticesController {

    private final AnnouncementService announcementService;

    @Operation(summary = "公告全量列表", description = "置顶优先、发布时间倒序；管理端不分页一次给全")
    @GetMapping
    public Result<List<NoticeVO>> list() {
        return Result.ok(announcementService.listAllForAdmin());
    }

    @Operation(summary = "新增公告", description = "返回新公告 id（字符串形式）；标题/正文为空 400")
    @PostMapping
    public Result<String> create(@Valid @RequestBody NoticeSaveRequest request) {
        return Result.ok(String.valueOf(announcementService.create(request)));
    }

    @Operation(summary = "修改公告", description = "id 不存在返回业务码 1002")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody NoticeSaveRequest request) {
        announcementService.update(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除公告", description = "逻辑删除；id 不存在返回业务码 1002")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return Result.ok();
    }
}
