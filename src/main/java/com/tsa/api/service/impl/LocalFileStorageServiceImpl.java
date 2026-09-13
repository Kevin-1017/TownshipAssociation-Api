package com.tsa.api.service.impl;

import com.tsa.api.common.BusinessException;
import com.tsa.api.common.ResultCode;
import com.tsa.api.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 文件存储·本地磁盘实现（契约 C8）。
 *
 * <p>二期换 OSS 的机制是新增实现类 + 两边各标 {@code @Profile}（本类 "!prod"、
 * OSS 类 "prod"）做切换；本棒<b>刻意不加</b>任何 @Profile —— 仓库里 prod profile
 * 尚不存在（application.yml 写死 active: dev，全仓无 application-prod.yml），
 * 现在标注解只会制造「将来可能不生效」的困惑，等真出现 prod 部署形态再补。
 *
 * <p>防目录穿越双保险（与接口注释呼应）：第一道是 FileController 的 name 正则
 * （{@code ^[0-9a-f-]{32,36}\.[a-z]{3,4}$}，正常流量根本到不了第二道就已经被拒），
 * 这里是第二道：扩展名白名单 + normalize 后必须仍在根目录内。任何一道失守，
 * 另一道都拦得住 "../" 与绝对路径注入。
 */
@Slf4j
@Service
public class LocalFileStorageServiceImpl implements FileStorageService {

    /** GET/落库路径前缀，与 FileController 的映射一致 —— 两处对齐即可，勿散落成三处 */
    private static final String URL_PREFIX = "/tsa/files/";

    /** 落盘扩展名白名单（契约 C8）：只可能生成这三种名，白名单外的对象名直接判非法 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    /** 合法对象名：uuid（带横杠 36 位 / 去横杠 32 位）. 小写扩展名 —— 与 controller 正则同源，双保险 */
    private static final Pattern SAFE_NAME = Pattern.compile("^[0-9a-f-]{32,36}\\.[a-z]{3,4}$");

    /** 上传根目录（tsa.files.dir，默认 ./upload-data）：部署时经环境变量指到持久卷 */
    @Value("${tsa.files.dir}")
    private String dir;

    /** 启动时定稿的绝对根路径：后续所有 resolve 以它为锚做越界判定 */
    private Path root;

    /** 启动即建目录（mkdirs 语义）：配置写错路径要在启动时暴露，而不是第一张头像上传时才炸 500 */
    @PostConstruct
    void prepareRoot() {
        root = Path.of(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
            log.info("文件存储根目录就绪: {}", root);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建文件存储根目录: " + root
                    + "（检查 tsa.files.dir 配置与磁盘权限）", e);
        }
    }

    @Override
    public String upload(String objectKey, String contentType, InputStream content) {
        Path target = resolveInside(objectKey);
        try (content) {
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // 写盘失败是服务端问题（磁盘满/权限），对外收敛到通用 ERROR，细节只进日志
            log.error("文件写入失败: {}", target, e);
            throw new BusinessException(ResultCode.ERROR, "文件写入失败");
        }
        return URL_PREFIX + objectKey;
    }

    @Override
    public Resource load(String objectKey) {
        // load 比 upload 多一步形态校验：objectKey 来自 URL，不匹配安全名直接当「不存在」
        if (objectKey == null || !SAFE_NAME.matcher(objectKey).matches()) {
            return null;
        }
        Path target = resolveInside(objectKey);
        return Files.isRegularFile(target) ? new FileSystemResource(target) : null;
    }

    /**
     * 把对象名解析为根目录内的绝对路径；越界（"../"、绝对路径注入）一律 404 壳拒掉。
     *
     * <p>不用 FILE_NAME_PATTERN 单独再写一套：扩展名白名单在这里兜底，
     * 防的是「controller 规则改了、这里忘了同步」的维护事故 —— 两道独立防线才叫防线。
     */
    private Path resolveInside(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        int dot = objectKey.lastIndexOf('.');
        String ext = dot < 0 ? "" : objectKey.substring(dot + 1).toLowerCase();
        Path target = root.resolve(objectKey).normalize();
        if (!ALLOWED_EXTENSIONS.contains(ext) || !target.startsWith(root)) {
            log.warn("非法文件名被存储层拦截（controller 正则失守的信号）: {}", objectKey);
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return target;
    }
}
