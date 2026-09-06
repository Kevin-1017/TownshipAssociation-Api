package com.tsa.api.service;

import java.io.InputStream;

/**
 * 文件存储服务抽象（占位接口，第二期实现）。
 *
 * <p>教学要点：先定义接口、再延迟实现——头像上传落地时，
 * 只需新增 LocalFileStorageImpl / MinioFileStorageImpl / OssFileStorageImpl 之一，
 * 业务代码通过接口调用，零改动。
 */
public interface FileStorageService {

    /**
     * 上传文件并返回可访问 URL。
     *
     * @param objectKey   存储路径，如 "avatar/2026/0001.jpg"
     * @param contentType MIME 类型
     * @param content     文件输入流
     * @return 公网可访问的 URL
     */
    String upload(String objectKey, String contentType, InputStream content);
}
