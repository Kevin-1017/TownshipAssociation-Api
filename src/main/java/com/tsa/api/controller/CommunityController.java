package com.tsa.api.controller;

import com.tsa.api.common.BusinessException;
import com.tsa.api.common.Result;
import com.tsa.api.common.ResultCode;
import com.tsa.api.common.UploadRules;
import com.tsa.api.config.ApiConstants;
import com.tsa.api.dto.CommentSaveRequest;
import com.tsa.api.dto.CommentVO;
import com.tsa.api.dto.CommunityPostQuery;
import com.tsa.api.dto.CommunityPostSaveRequest;
import com.tsa.api.dto.CommunityPostVO;
import com.tsa.api.dto.FileUploadVO;
import com.tsa.api.dto.PageVO;
import com.tsa.api.service.CommunityService;
import com.tsa.api.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * 社区动态接口（美食基地 / 校园广场）。
 *
 * <p>一期无登录：发布与评论的作者为表单自由填写的昵称；点赞只做计数自增。
 *
 * <p><b>2026-09-14 审核制</b>：发布一律落待审（管理端 /tsa/admin/community/** 审后才公开），
 * 读路径只放行已过审内容；配图走公开上传端点 /uploads（web 无登录态，Bearer 版 /tsa/files 不适用），
 * 参数级校验与 C8 同源（UploadRules），IP 限频在 AuthRateLimitInterceptor 圈定。
 *
 * <p><b>2026-09-12 状态</b>：小程序端已将本组功能下线（个人主体不可提供 UGC 发布/浏览，
 * 见运营规范 5.7.1），服务端<b>刻意保留</b>接口与数据：主体变更（个人→非个人）并报备
 * 【社交-社区/论坛】类目后，社区功能按原契约回归，届时恢复调用即可。
 */
@Tag(name = "社区动态", description = "美食基地 / 校园广场：列表、详情、发布（待审）、配图上传、点赞、评论")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;
    private final FileStorageService fileStorageService;

    @Operation(summary = "动态分页列表", description = "只下发已过审内容；支持 type/cuisine/region 筛选与标题/正文关键字搜索，按发布时间倒序")
    @GetMapping("/posts")
    public Result<PageVO<CommunityPostVO>> page(CommunityPostQuery query) {
        return Result.ok(communityService.pageQuery(query));
    }

    @Operation(summary = "动态详情", description = "含已过审评论列表 commentsList；不存在或未过审返回 1002")
    @GetMapping("/posts/{id}")
    public Result<CommunityPostVO> detail(@PathVariable Long id) {
        return Result.ok(communityService.getDetail(id));
    }

    @Operation(summary = "发布动态", description = "落库为待审核，审核通过后才出现在列表；返回新动态 id（字符串形式）。"
            + "images 最多 1 张，路径先经 /uploads 取得")
    @PostMapping("/posts")
    public Result<String> publish(@Valid @RequestBody CommunityPostSaveRequest request) {
        return Result.ok(String.valueOf(communityService.publish(request)));
    }

    @Operation(summary = "发布配图上传", description = "公开端点（web 社区发布无登录态）；multipart 字段名 file，"
            + "≤2MB、类型限 jpg/png/webp（校验与 /tsa/files 同源）；按 IP 限频（默认 5 次/分，超限 1306）。"
            + "返回相对路径 /tsa/files/<uuid>.<ext>，随发布请求放进 images")
    @PostMapping("/uploads")
    public Result<FileUploadVO> upload(@RequestPart(value = "file", required = false) MultipartFile file)
            throws IOException {
        // 与 FileController.upload 同一句 400 文案：原因不细分的收口口径也一并沿用（契约 C8）
        String ext = UploadRules.extOfAllowedType(file);
        if (!UploadRules.accepted(file)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件超限或类型不支持");
        }
        String name = UUID.randomUUID() + "." + ext;
        // 落盘/读回复用文件服务：/uploads 只是把「谁能上传」从登录态换成 IP 限频，存储口径不变
        String path = fileStorageService.upload(name, file.getContentType(), file.getInputStream());
        return Result.ok(new FileUploadVO(path));
    }

    @Operation(summary = "点赞", description = "点赞数 +1，返回点赞后的总数；动态不存在或未过审返回 1002")
    @PostMapping("/posts/{id}/like")
    public Result<Integer> like(@PathVariable Long id) {
        return Result.ok(communityService.like(id));
    }

    @Operation(summary = "发表评论", description = "落库为待审核，通过后随详情下发并计入条数；返回新评论；"
            + "动态不存在或未过审返回 1002；IP 限频 5 次/时（跨帖子共享同桶）")
    @PostMapping("/posts/{id}/comments")
    public Result<CommentVO> comment(@PathVariable Long id, @Valid @RequestBody CommentSaveRequest request) {
        return Result.ok(communityService.comment(id, request));
    }
}
