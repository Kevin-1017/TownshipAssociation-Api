package com.tsa.api.controller;

import com.tsa.api.common.Result;
import com.tsa.api.config.ApiConstants;
import com.tsa.api.dto.DonationRecordVO;
import com.tsa.api.dto.FoundationHomeVO;
import com.tsa.api.dto.RewardRecordsVO;
import com.tsa.api.service.FoundationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 校友基金会接口：小程序/官网<b>只读</b>展示（首页聚合 + 明细）。
 *
 * <p>写接口（类别/记录/捐赠的增删改）已于 v1.3 收口迁至 {@link AdminFoundationController}
 * （{@code /tsa/admin/foundation/**}，需 admin 角色），本类只保留公开读接口，供小程序免登录浏览。
 */
@Tag(name = "基金会", description = "奖励与捐赠：首页聚合与明细（只读）")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/foundation")
@RequiredArgsConstructor
public class FoundationController {

    private final FoundationService foundationService;

    @Operation(summary = "首页数据", description = "奖励类别 + 捐赠鸣谢聚合，金额单位为元")
    @GetMapping
    public Result<FoundationHomeVO> home() {
        return Result.ok(foundationService.home());
    }

    @Operation(summary = "奖励明细", description = "类别名列表 + 获奖记录（含类别名）")
    @GetMapping("/rewards")
    public Result<RewardRecordsVO> rewards() {
        return Result.ok(foundationService.rewards());
    }

    @Operation(summary = "捐赠明细", description = "按捐赠日期倒序")
    @GetMapping("/donations")
    public Result<List<DonationRecordVO>> donations() {
        return Result.ok(foundationService.donations());
    }
}
