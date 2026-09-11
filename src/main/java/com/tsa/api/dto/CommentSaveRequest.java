package com.tsa.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 发表评论请求。一期无登录，author 前端固定传「我」或昵称。 */
@Data
@Schema(description = "发表评论请求")
public class CommentSaveRequest {

    @Schema(description = "评论者昵称", example = "我", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写评论者昵称")
    @Size(max = 32, message = "昵称最长 32 个字")
    private String author;

    @Schema(description = "头像 URL", example = "")
    @Size(max = 255, message = "头像地址过长")
    private String avatar;

    @Schema(description = "评论内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容最长 500 个字")
    private String content;
}
