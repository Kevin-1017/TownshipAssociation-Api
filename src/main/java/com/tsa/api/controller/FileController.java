package com.tsa.api.controller;

import com.tsa.api.common.BusinessException;
import com.tsa.api.common.Result;
import com.tsa.api.common.ResultCode;
import com.tsa.api.config.ApiConstants;
import com.tsa.api.dto.FileUploadVO;
import com.tsa.api.service.AuthService;
import com.tsa.api.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 文件接口（契约 C8）：头像 / 活动封面共用的最小上传与公开回读。
 *
 * <p>闸门口径：POST 需<b>微信登录态</b>——路由层 checkLogin 圈住裸 token（SaTokenConfig
 * 按方法圈定 POST /tsa/files），方法内再过一道 {@code authService.currentOpenid()} 显式拒绝
 * assoc 前缀（修订 A9：assoc 令牌在 Sa-Token 眼里是合法会话，checkLogin 拦不住它，
 * /tsa/user/**、PUT /profile、POST /members 同口径，写门类端点一个都不能漏）。
 * GET 公开（&lt;image&gt; 组件发不了带令牌的请求，读图必须免登录）。
 *
 * <p>multipart 的接参与 JSON 不同：Bean Validation 的声明式校验覆盖不到文件字段，
 * 大小/类型这道「参数级」检查只能显式写在入口 —— 存储怎么落盘在
 * LocalFileStorageServiceImpl，这里零触碰文件系统。
 */
@Tag(name = "文件", description = "图片上传（登录）与读取（公开）")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/files")
@RequiredArgsConstructor
public class FileController {

    /**
     * 业务大小上限（契约 C8）：2MB。
     * 与 spring.servlet.multipart 的容器闸（3MB）刻意分层：容器先拦超大 body 的 DoS，
     * 业务闸负责把「超限」翻译回契约钉死的 400 提示。
     */
    private static final long MAX_SIZE_BYTES = 2L * 1024 * 1024;

    /**
     * 类型白名单按 <b>content-type</b> 判定（契约 C8），扩展名由服务端生成 ——
     * 客户端自报的文件名/后缀一律不可信（改名 .html 就绕过黑名单是老式事故）。
     * content-type 同样可伪造，本期接受：有 Bearer 门 + uuid 落盘名 + GET 只按白名单
     * 回 image/* 类型三层兜着，伪装的字节流最多是张坏图，成不了脚本。
     */
    private static final Map<String, String> EXT_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    /** 回读 Content-Type 按扩展名给（存储层没存 MIME，本期不为此加 sidecar 元数据） */
    private static final Map<String, MediaType> MEDIA_BY_EXT = Map.of(
            "jpg", MediaType.IMAGE_JPEG,
            "jpeg", MediaType.IMAGE_JPEG,
            "png", MediaType.IMAGE_PNG,
            "webp", MediaType.parseMediaType("image/webp"));

    /**
     * 对象名格式（契约 C8 原文钉死）：uuid（36 位带横杠 / 32 位去横杠）+ 3~4 位小写扩展名。
     * GET 第一道闸：不匹配直接 404，连存储层都不进 —— "../"、绝对路径、双扩展名全死在这行；
     * LocalFileStorageServiceImpl 里的白名单+越界检查是第二道闸（防这道将来被人"优化"掉）。
     */
    private static final Pattern SAFE_NAME = Pattern.compile("^[0-9a-f-]{32,36}\\.[a-z]{3,4}$");

    private final FileStorageService fileStorageService;
    private final AuthService authService;

    @Operation(summary = "上传图片", description = "multipart 字段名 file；≤2MB、类型限 jpg/png/webp（按 content-type 判定）；"
            + "需 Bearer 登录态。返回相对访问路径 /tsa/files/<uuid>.<ext>（存库用，不落库由调用方决定）")
    @PostMapping
    public Result<FileUploadVO> upload(@RequestPart(value = "file", required = false) MultipartFile file)
            throws IOException {
        // 身份裁决只验返回值丢弃：本期不做「文件归属表」(D3 定案不落库),但 assoc- 前缀必须拒
        // ——否则乡会令牌可无限写盘(路由层的 checkLogin 对它是放行的,见类注释 A9)
        authService.currentOpenid();
        // 缺字段/空文件/超限/类型不符一律同一句 400 提示（契约 C8 钉死 message），
        // 不区分原因既省对客户端的信息泄露面，也省得前端为四种失败摆四张 toast
        String ext = file == null ? null : EXT_BY_CONTENT_TYPE.get(file.getContentType());
        if (ext == null || file.isEmpty() || file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件超限或类型不支持");
        }
        String name = UUID.randomUUID() + "." + ext;
        // 存储层负责关流；真抛 IO/磁盘异常归 GlobalExceptionHandler 兜底成 500 壳（服务端问题不伪装成参数问题）
        String path = fileStorageService.upload(name, file.getContentType(), file.getInputStream());
        return Result.ok(new FileUploadVO(path));
    }

    @Operation(summary = "读取图片", description = "公开；name 必须是上传返回的 uuid 文件名；"
            + "非法名或文件不存在返回统一壳 code=404")
    @GetMapping("/{name}")
    public ResponseEntity<Resource> view(@PathVariable String name) {
        Resource resource = (name != null && SAFE_NAME.matcher(name).matches())
                ? fileStorageService.load(name)
                : null;
        if (resource == null) {
            // 统一壳 404（HTTP 200 + code=404）而非裸 HTTP 404 的取舍：小程序 request 层统一按
            // 壳里的 code 分流，裸 404 会走 uni 的 fail 回调伪装成网络异常，错误语义反而失真；
            // 而 <image> 组件加载本来就不看响应体，两种消费方在此口径下都无感 —— 一致性优先
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        int dot = name.lastIndexOf('.');
        MediaType mediaType = MEDIA_BY_EXT.get(name.substring(dot + 1).toLowerCase());
        return ResponseEntity.ok()
                .contentType(mediaType == null ? MediaType.APPLICATION_OCTET_STREAM : mediaType)
                .body(resource);
    }
}
