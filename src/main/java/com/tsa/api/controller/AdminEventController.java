package com.tsa.api.controller;

import com.tsa.api.common.BusinessException;
import com.tsa.api.common.Result;
import com.tsa.api.common.ResultCode;
import com.tsa.api.common.UploadRules;
import com.tsa.api.config.ApiConstants;
import com.tsa.api.dto.EventAdminQuery;
import com.tsa.api.dto.EventListVO;
import com.tsa.api.dto.EventSaveRequest;
import com.tsa.api.dto.FileUploadVO;
import com.tsa.api.dto.PageVO;
import com.tsa.api.service.EventService;
import com.tsa.api.service.FileStorageService;
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
import java.util.UUID;

/**
 * 乡会事件管理接口（2026-09-14 管理端配置入口）：列表 + 增删改 + 封面上传。
 *
 * <p>鉴权同其余 /tsa/admin/**（SaTokenConfig 的 checkRole("admin")），本类不自建闸门；
 * Controller 零逻辑，字段落库与「删除即下架」语义收口在 EventService。
 * 公开的只读列表/详情仍走 {@code EventController}（/tsa/events/**，小程序共用）。
 */
@Tag(name = "事件（管理端）", description = "乡会事件的配置入口：列表、新建、修改、删除（即下架）、封面上传")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/admin")
@RequiredArgsConstructor
public class AdminEventController {

    private final EventService eventService;
    private final FileStorageService fileStorageService;

    @Operation(summary = "事件分页列表（管理端）", description = "query：page/pageSize(钳1..50)/keyword(标题模糊)；"
            + "start_time 倒序；出参复用契约 C5 列表 VO（含 articleUrl，可为 null）")
    @GetMapping("/events")
    public Result<PageVO<EventListVO>> page(EventAdminQuery query) {
        return Result.ok(eventService.adminPageQuery(query));
    }

    @Operation(summary = "新建事件", description = "startTime 必填（排序/年份筛选依据）；cover 先传 /events/cover 拿相对路径；"
            + "创建即公开（无发布工作流）；返回新 id 字符串")
    @PostMapping("/events")
    public Result<String> create(@Valid @RequestBody EventSaveRequest request) {
        return Result.ok(String.valueOf(eventService.create(request)));
    }

    @Operation(summary = "修改事件", description = "null 字段不改、cover/summary/articleUrl 传空串=清除；id 不存在 1002")
    @PutMapping("/events/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody EventSaveRequest request) {
        eventService.update(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除事件", description = "逻辑删除=下架：公开列表/详情立即查无；id 不存在 1002")
    @DeleteMapping("/events/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        eventService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "事件封面上传", description = "multipart 字段名 file；≤2MB、限 jpg/png/webp（校验与 C8 同源 UploadRules）；"
            + "admin 角色已由路由墙把关，不再叠 IP 限频；返回相对路径 /tsa/files/<uuid>.<ext>，随保存请求放进 cover")
    @PostMapping("/events/cover")
    public Result<FileUploadVO> uploadCover(@RequestPart(value = "file", required = false) MultipartFile file)
            throws IOException {
        // 与 FileController/社区上传同一句 400 文案（契约 C8 收口口径），原因不细分
        if (!UploadRules.accepted(file)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件超限或类型不支持");
        }
        String ext = UploadRules.extOfAllowedType(file);
        String name = UUID.randomUUID() + "." + ext;
        String path = fileStorageService.upload(name, file.getContentType(), file.getInputStream());
        return Result.ok(new FileUploadVO(path));
    }
}
