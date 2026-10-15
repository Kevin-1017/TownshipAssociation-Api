package com.tsa.api.controller;

import com.tsa.api.common.BusinessException;
import com.tsa.api.common.Result;
import com.tsa.api.common.ResultCode;
import com.tsa.api.common.UploadRules;
import com.tsa.api.config.ApiConstants;
import com.tsa.api.dto.FileUploadVO;
import com.tsa.api.dto.LineupSaveRequest;
import com.tsa.api.dto.LineupVO;
import com.tsa.api.service.FileStorageService;
import com.tsa.api.service.LineupService;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * 道具点位管理接口（kevin/csgo 指南的配置入口）：列表 + 增删改 + 截图上传。
 *
 * <p>鉴权同其余 /tsa/admin/**（SaTokenConfig 的 checkRole("admin") 路由墙，本类不自建闸门）；
 * Controller 零逻辑，整单覆写与 1002 语义收口在 LineupService。
 * 公开只读走 {@code LineupController}（/tsa/lineups，前台 kevin 页消费）。
 */
@Tag(name = "道具点位（管理端）", description = "CS 道具点位的配置入口：列表、新建、修改、删除、截图上传")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/admin/lineups")
@RequiredArgsConstructor
public class AdminLineupController {

    private final LineupService lineupService;
    private final FileStorageService fileStorageService;

    @Operation(summary = "点位全量列表（管理端）", description = "与公开侧同源同形（无状态列），排序口径一致")
    @GetMapping
    public Result<List<LineupVO>> list() {
        return Result.ok(lineupService.listAll());
    }

    @Operation(summary = "新建点位", description = "images 先传 /lineups/image 拿相对路径；返回新 id 字符串")
    @PostMapping
    public Result<String> create(@Valid @RequestBody LineupSaveRequest request) {
        return Result.ok(String.valueOf(lineupService.create(request)));
    }

    @Operation(summary = "修改点位", description = "整单覆写（管理表单带齐全部字段）；id 不存在 1002")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody LineupSaveRequest request) {
        lineupService.update(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除点位", description = "逻辑删除；前台列表立即查无；id 不存在 1002")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        lineupService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "点位截图上传", description = "multipart 字段名 file；≤2MB、限 jpg/png/webp（校验与 /tsa/files 同源 UploadRules）；"
            + "一条点位最多 9 张在保存请求里把关，这里单次单张；返回相对路径 /tsa/files/<uuid>.<ext>，随保存请求放进 images")
    @PostMapping("/image")
    public Result<FileUploadVO> uploadImage(@RequestPart(value = "file", required = false) MultipartFile file)
            throws IOException {
        // 与事件封面/社区上传同一句 400 文案（统一收口口径），原因不细分
        if (!UploadRules.accepted(file)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件超限或类型不支持");
        }
        String ext = UploadRules.extOfAllowedType(file);
        String name = UUID.randomUUID() + "." + ext;
        String path = fileStorageService.upload(name, file.getContentType(), file.getInputStream());
        return Result.ok(new FileUploadVO(path));
    }
}
