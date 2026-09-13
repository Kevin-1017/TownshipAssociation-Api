package com.tsa.api.controller;

import com.tsa.api.common.Result;
import com.tsa.api.config.ApiConstants;
import com.tsa.api.dto.DonationSaveRequest;
import com.tsa.api.dto.RewardCategorySaveRequest;
import com.tsa.api.dto.RewardRecordSaveRequest;
import com.tsa.api.service.FoundationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 基金会管理端写接口（v1.3 从 FoundationController 收口迁入）。
 *
 * <p>原 9 个写方法（categories/records/donations 的 POST/PUT/DELETE）本挂在公开的
 * {@code /tsa/foundation/**} 下、一期不鉴权；现整体迁到 admin 墙内的 {@code /tsa/admin/foundation/**}，
 * 由 SaTokenConfig 的 {@code checkRole("admin")} 把门。返回形状与原实现逐一保持一致
 * （POST 回字符串 id、PUT/DELETE 回 Void），业务全在 FoundationService，本类零逻辑。
 * 读接口仍留在 {@code FoundationController}（/tsa/foundation/** 公开，供小程序只读）。
 */
@Tag(name = "基金会（管理端）", description = "奖励类别/获奖记录/捐赠鸣谢的增删改（需 admin 角色）")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/admin/foundation")
@RequiredArgsConstructor
public class AdminFoundationController {

    private final FoundationService foundationService;

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
