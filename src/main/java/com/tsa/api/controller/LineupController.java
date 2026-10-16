package com.tsa.api.controller;

import com.tsa.api.common.Result;
import com.tsa.api.config.ApiConstants;
import com.tsa.api.dto.LineupVO;
import com.tsa.api.service.LineupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CS 道具点位公开只读接口：官网 kevin/csgo 页在浏览器运行时拉取（静态导出，无服务端渲染时机）。
 *
 * <p>与管理端同源全量下发（本表无状态/草稿列，逻辑删除行天然被 @TableLogic 滤掉），
 * 故不给公开侧另造 VO；写操作在 {@code AdminLineupController}。
 */
@Tag(name = "道具点位", description = "CS 道具指南的公开查询")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/lineups")
@RequiredArgsConstructor
public class LineupController {

    private final LineupService lineupService;

    @Operation(summary = "点位全量列表", description = "按 地图→道具→id 升序；不分页一次给全（服务端钳 200 上限）")
    @GetMapping
    public Result<List<LineupVO>> list() {
        return Result.ok(lineupService.listAll());
    }
}
