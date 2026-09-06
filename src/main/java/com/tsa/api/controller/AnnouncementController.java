package com.tsa.api.controller;

import com.tsa.api.common.Result;
import com.tsa.api.entity.Announcement;
import com.tsa.api.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公告接口：小程序首页公告栏与官网新闻共用。
 */
@Tag(name = "公告", description = "乡会公告查询")
@RestController
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @Operation(summary = "公告列表", description = "置顶优先，其次按发布时间倒序")
    @GetMapping
    public Result<List<Announcement>> list() {
        return Result.ok(announcementService.listForHome());
    }
}
