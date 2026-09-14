package com.tsa.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 发布社区动态请求。一期无登录，author 为表单自由填写的昵称。
 *
 * <p>校验口径与小程序发布表单一致：标题 5~30 字、内容 ≥10 字。
 */
@Data
@Schema(description = "发布社区动态请求")
public class CommunityPostSaveRequest {

    @Schema(description = "动态类型：food / campus", example = "food", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请选择动态类型")
    private String type;

    @Schema(description = "发布者昵称", example = "陈阿姨", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写发布者昵称")
    @Size(max = 32, message = "昵称最长 32 个字")
    private String author;

    @Schema(description = "头像 URL", example = "")
    @Size(max = 255, message = "头像地址过长")
    private String avatar;

    @Schema(description = "标题", example = "潮汕牛肉丸哪家最正宗?", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写标题")
    @Size(min = 5, max = 30, message = "标题需 5~30 个字")
    private String title;

    @Schema(description = "正文", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写内容")
    @Size(min = 10, max = 1000, message = "内容至少 10 个字")
    private String content;

    @Schema(description = "图片相对路径数组（最多 1 张，先传 POST /tsa/community/uploads 拿路径）", example = "[]")
    @Size(max = 1, message = "最多上传 1 张图片")
    private List<String> images;

    @Schema(description = "菜系（仅美食动态）", example = "潮汕菜")
    @Size(max = 32, message = "菜系名称过长")
    private String cuisine;

    @Schema(description = "所在地区（仅美食动态）", example = "longdong")
    @Size(max = 32, message = "地区名称过长")
    private String region;
}
