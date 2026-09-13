package com.tsa.api.service;

import org.springframework.core.io.Resource;

import java.io.InputStream;

/**
 * 文件存储服务抽象（v1.2 D3 落地本地磁盘实现：头像 / 活动封面共用）。
 *
 * <p>教学要点：先定义接口、再延迟实现——业务代码只认本接口，将来换存储介质
 * （OSS / MinIO）时新增实现类即可，Controller 与 DTO 零改动。
 * 本期对象一律为<b>扁平名</b>（{@code <uuid>.<ext>}，契约 C8 钉死的命名），
 * 不引入目录层级——头像没有按日期分片的检索需求，多一层目录只多一处穿越面。
 */
public interface FileStorageService {

    /**
     * 上传文件并返回可访问路径。
     *
     * <p>本地实现返回 {@code /tsa/files/<objectKey>}（GET 端点回读，契约 C8 的相对路径）；
     * 二期 OSS 实现可返回 CDN 绝对 URL——存库/下发统一只用本返回值，
     * 调用方禁止自己拼前缀，两种实现才能无痛切换。
     *
     * @param objectKey   对象名（{@code <uuid>.<ext>}；实现必须防目录穿越，见 load 同款说明）
     * @param contentType MIME 类型（本地实现暂不消费——GET 侧按扩展名回推；
     *                    OSS 实现写对象元数据用。参数形状先行钉死）
     * @param content     文件输入流（实现读完负责关）
     * @return 对外访问路径（相对或绝对由实现决定，见上）
     */
    String upload(String objectKey, String contentType, InputStream content);

    /**
     * 读取文件。
     *
     * <p>objectKey 一律视为<b>不可信输入</b>（来自 URL）：实现必须先钉扩展名白名单、
     * 再判 normalize 后仍在根目录内——两道防任意一道失守，都到不了越权读写。
     *
     * @return 可读资源；文件不存在返回 null（由调用方决定回什么错误，接口层不预设 Result 形状）
     */
    Resource load(String objectKey);
}
