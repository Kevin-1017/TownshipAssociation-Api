package com.tsa.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文件上传结果（契约 C8，{@code POST /tsa/files} 出参）。
 *
 * <p>只回相对路径不回绝对 URL：API base 是前端编译期常量（VITE_API_BASE_URL），
 * 后端拼它反而把部署域名写进了数据；落库存的也是这个相对路径
 * （头像 → avatar_url、封面 → cover_url），前端展示时再拼绝对地址。
 */
@Data
@Schema(description = "文件上传结果")
public class FileUploadVO {

    @Schema(description = "相对访问路径（uuid 文件名由服务端生成，客户端原名一律丢弃）",
            example = "/tsa/files/0b1e5c2f-9a3d-4e7c-8f21-6d0a4b3c5e77.jpg")
    private String path;

    public FileUploadVO() {
    }

    public FileUploadVO(String path) {
        this.path = path;
    }
}
