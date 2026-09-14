package com.tsa.api.common;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 图片上传参数级校验规则（契约 C8 的单一来源）。
 *
 * <p>为什么抽出来：C8 的「≤2MB + content-type 白名单」原本写死在 FileController，
 * 2026-09-14 社区审核制新增公开上传端点 POST /tsa/community/uploads 需要同一套闸口，
 * 复制两份必然出现「改一处漏一处」的口径漂移 —— 规则收在这里，两个入口只做编排。
 */
public final class UploadRules {

    /**
     * 业务大小上限（契约 C8）：2MB。
     * 与 spring.servlet.multipart 的容器闸（3MB）刻意分层：容器先拦超大 body 的 DoS，
     * 业务闸负责把「超限」翻译回契约钉死的 400 提示。
     */
    public static final long MAX_SIZE_BYTES = 2L * 1024 * 1024;

    /**
     * 类型白名单按 <b>content-type</b> 判定（契约 C8），扩展名由服务端生成 ——
     * 客户端自报的文件名/后缀一律不可信（改名 .html 就绕过黑名单是老式事故）。
     * content-type 同样可伪造，本期接受：上传入口有各自的防线（登录态或 IP 限频）
     * + uuid 落盘名 + GET 只按白名单回 image/* 三层兜着，伪装的字节流最多是张坏图，成不了脚本。
     */
    public static final Map<String, String> EXT_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    /** 命中白名单返回落盘扩展名，否则 null —— 调用方按契约统一回 400，不区分具体原因 */
    public static String extOfAllowedType(MultipartFile file) {
        if (file == null) {
            return null;
        }
        return EXT_BY_CONTENT_TYPE.get(file.getContentType());
    }

    /** 缺字段/空文件/超限/类型不符的合并判定（true=可受理） */
    public static boolean accepted(MultipartFile file) {
        return extOfAllowedType(file) != null && !file.isEmpty() && file.getSize() <= MAX_SIZE_BYTES;
    }

    private UploadRules() {
    }
}
