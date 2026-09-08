package com.tsa.api.controller;

import com.tsa.api.common.Result;
import com.tsa.api.config.ApiConstants;
import com.tsa.api.dto.NoticeVO;
import com.tsa.api.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公告接口：小程序首页公告栏与官网新闻共用。
 *
 * <p>路径用前端契约的 notices（资源名面向消费方，内部实体仍叫 Announcement）。
 */
@Tag(name = "公告", description = "乡会公告查询")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/notices")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @Operation(summary = "公告列表", description = "置顶优先，其次按发布时间倒序；不分页，数量少一次给全")
    @GetMapping
    public Result<List<NoticeVO>> list() {
        return Result.ok(announcementService.listForHome());
    }

    @Operation(summary = "公告详情", description = "按 id 查询单条公告，不存在返回业务码 1002")
    @GetMapping("/{id}")
    public Result<NoticeVO> detail(@PathVariable Long id) {
        return Result.ok(announcementService.getDetail(id));
    }
}
