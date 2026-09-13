package com.tsa.api.controller;

import com.tsa.api.common.Result;
import com.tsa.api.config.ApiConstants;
import com.tsa.api.dto.MemberDetailVO;
import com.tsa.api.dto.ProfileUpdateRequest;
import com.tsa.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 本人接口：微信登录用户自己的档案（小程序「我的」页/资料页数据源与保存出口）。
 *
 * <p>路由 {@code /tsa/user/**} 是 SaTokenConfig 声明的受保护面（checkLogin）；
 * Controller 保持零逻辑 —— 鉴权拒绝规则（assoc- 前缀等）与 openid 推导
 * 单点收敛在 AuthService.currentOpenid()，GET /me 与 PUT /profile 共用同一入口，
 * 本人写路径不再从请求体接受 openid（契约 C7，防钓鱼建档同源逻辑）。
 */
@Tag(name = "本人", description = "微信登录用户的本人档案")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/user")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @Operation(summary = "本人档案",
            description = "Bearer 登录视角读取；已登录未建档返回 data=null（前端显示「点击完善资料」）；"
                    + "本人视角不裁剪 phone/wechatId；token 失效或乡会令牌（assoc-）冒充一律 401")
    @GetMapping("/me")
    public Result<MemberDetailVO> me() {
        return Result.ok(authService.currentUser());
    }

    @Operation(summary = "保存本人资料",
            description = "全字段可选、只更白名单（status/province/city/openid 结构上不可改）；"
                    + "首次提交即建档（status=0 待秘书处审核 + source=1 本人提交）；"
                    + "返回保存后的最新档案（本人视角）；未登录/乡会令牌冒充一律 401")
    @PutMapping("/profile")
    public Result<MemberDetailVO> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return Result.ok(authService.updateOwnProfile(request));
    }
}
