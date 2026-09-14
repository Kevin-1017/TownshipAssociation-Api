package com.tsa.api.controller;

import com.tsa.api.common.Result;
import com.tsa.api.config.ApiConstants;
import com.tsa.api.dto.EventDetailVO;
import com.tsa.api.dto.EventListVO;
import com.tsa.api.dto.EventQuery;
import com.tsa.api.dto.PageVO;
import com.tsa.api.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 活动接口（v1.2 D2「跳公众号文章」形态）：公开只读两个端点。
 *
 * <p>Controller 零逻辑：分页/年份筛选/展示态派生全在 EventService。
 * 公开理由：活动是拉新门面，未登录用户必须能逛列表与详情——
 * SaTokenConfig 的锁只圈 /tsa/user/**、POST /tsa/members、POST /tsa/files，
 * 本前缀天然免登录（「计划 B16/A7 一次配齐」口径，勿在此另立闸门）。
 */
@Tag(name = "活动", description = "活动列表与详情（正文在公众号，小程序只给链接）")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @Operation(summary = "活动分页列表", description = "按开始时间降序；query：page(默认1)/pageSize(默认10,钳1..50)/"
            + "year(可选,按 start_time 年份)/yearFrom+yearTo(可选,年份区间含端点,任一侧可缺省,"
            + "与 year 同时给出取交集,yearFrom>yearTo 判 400)；status 为派生的 upcoming/past；"
            + "含 articleUrl(可为 null,web 列表直跳公众号)；不含正文")
    @GetMapping
    public Result<PageVO<EventListVO>> page(EventQuery query) {
        return Result.ok(eventService.pageQuery(query));
    }

    @Operation(summary = "活动详情", description = "返回 articleUrl（公众号永久链接，可为 null）；"
            + "前端「阅读公众号全文」按钮用它调 wx.openOfficialAccountArticle；查无返回业务码 1002")
    @GetMapping("/{id}")
    public Result<EventDetailVO> detail(@PathVariable Long id) {
        return Result.ok(eventService.detail(id));
    }
}
