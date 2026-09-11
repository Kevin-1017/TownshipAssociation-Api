package com.tsa.api.controller;

import com.tsa.api.common.Result;
import com.tsa.api.config.ApiConstants;
import com.tsa.api.dto.DonationRecordVO;
import com.tsa.api.dto.DonationSaveRequest;
import com.tsa.api.dto.FoundationHomeVO;
import com.tsa.api.dto.RewardCategorySaveRequest;
import com.tsa.api.dto.RewardRecordSaveRequest;
import com.tsa.api.dto.RewardRecordsVO;
import com.tsa.api.service.FoundationService;
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
 * 校友基金会接口：小程序只读展示 + 管理端 CRUD。
 *
 * <p>写接口一期不鉴权（放在非 admin 路径下，管理后台二期上线后再收口到 /tsa/admin/**）。
 */
@Tag(name = "基金会", description = "奖励与捐赠：首页聚合、明细，及类别/记录/捐赠的增删改")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/foundation")
@RequiredArgsConstructor
public class FoundationController {

    private final FoundationService foundationService;

    // ---------- 读 ----------

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

    // ---------- 写：奖项类别 ----------

    @Operation(summary = "新增奖项类别", description = "返回新类别 id（字符串形式）")
    @PostMapping("/categories")
    public Result<String> createCategory(@Valid @RequestBody RewardCategorySaveRequest request) {
        return Result.ok(String.valueOf(foundationService.createCategory(request)));
    }

    @Operation(summary = "修改奖项类别")
    @PutMapping("/categories/{id}")
    public Result<Void> updateCategory(@PathVariable Long id, @Valid @RequestBody RewardCategorySaveRequest request) {
        foundationService.updateCategory(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除奖项类别", description = "连带删除其下的获奖记录")
    @DeleteMapping("/categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        foundationService.deleteCategory(id);
        return Result.ok();
    }

    // ---------- 写：获奖记录 ----------

    @Operation(summary = "新增获奖记录", description = "返回新记录 id（字符串形式）")
    @PostMapping("/records")
    public Result<String> createRecord(@Valid @RequestBody RewardRecordSaveRequest request) {
        return Result.ok(String.valueOf(foundationService.createRecord(request)));
    }

    @Operation(summary = "修改获奖记录")
    @PutMapping("/records/{id}")
    public Result<Void> updateRecord(@PathVariable Long id, @Valid @RequestBody RewardRecordSaveRequest request) {
        foundationService.updateRecord(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除获奖记录")
    @DeleteMapping("/records/{id}")
    public Result<Void> deleteRecord(@PathVariable Long id) {
        foundationService.deleteRecord(id);
        return Result.ok();
    }

    // ---------- 写：捐赠鸣谢 ----------

    @Operation(summary = "新增捐赠鸣谢", description = "返回新记录 id（字符串形式）")
    @PostMapping("/donations")
    public Result<String> createDonation(@Valid @RequestBody DonationSaveRequest request) {
        return Result.ok(String.valueOf(foundationService.createDonation(request)));
    }

    @Operation(summary = "修改捐赠鸣谢")
    @PutMapping("/donations/{id}")
    public Result<Void> updateDonation(@PathVariable Long id, @Valid @RequestBody DonationSaveRequest request) {
        foundationService.updateDonation(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除捐赠鸣谢")
    @DeleteMapping("/donations/{id}")
    public Result<Void> deleteDonation(@PathVariable Long id) {
        foundationService.deleteDonation(id);
        return Result.ok();
    }
}
