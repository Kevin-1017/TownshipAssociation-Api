package com.tsa.api.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 路由鉴权 + MVC 拦截器注册。
 *
 * <p>路由规则一次配齐（v1.3：管理后台从 checkLogin 升级为 checkRole("admin")，防冒充已闭环；
 * 第 2 棒的 profile/files 端点到时无需再改本文件 ——
 * 「规则先到、端点后到」无副作用，端点不存在时该条规则永远匹配不上）：
 * <ul>
 *   <li>{@code /tsa/admin/**}：管理后台，<b>必须是 admin 角色会话</b>（{@code loginId} 以
 *       {@code admin-} 前缀签发）。由 checkLogin 改为 {@code checkRole("admin")}：openid 裸登录态
 *       与 {@code assoc-} 乡会令牌虽能过 checkLogin，但拿不到 "admin" 角色（角色判定见
 *       {@link StpInterfaceImpl}），故一律 NotRoleException → 403 —— 冒充面就此收口</li>
 *   <li>{@code /tsa/user/**}：微信登录受保护面（GET /me 与后续 PUT /profile 共用前缀）</li>
 *   <li>{@code POST /tsa/members}：注册建档需登录态（修订 A7/B16 的方向：openid 由服务端
 *       从 loginId 推导，防客户端自报钓鱼建档；请求体删 openid 字段随第 2 棒落地）</li>
 *   <li>{@code POST /tsa/files}：文件上传需登录态；同路径的 GET 公开读<b>不在</b>此列，
 *       故必须按方法匹配，不能只 match 路径</li>
 *   <li>其余 {@code /tsa/**}（含 {@code /tsa/auth/**} 全部端点）：公开 —— 核验/登录本就要给未登录的人用</li>
 * </ul>
 *
 * <p>注意：loginId=裸 openid 的会话与 {@code assoc-} 乡会会话同体系，{@code checkLogin()}
 * <b>挡不住</b> assoc 令牌冒充（修订 A9）—— /tsa/user/** 的反向防御在 AuthServiceImpl.currentOpenid()。
 * /tsa/admin/** 因已改用 {@code checkRole("admin")}（前缀隔离的角色判定）而不受此问题影响：
 * 无论 openid 还是 assoc 会话，无 "admin" 角色即 403。
 *
 * <p>教学要点：登录校验用"拦截器 + 路由规则"集中声明，
 * 而不是在每个 Controller 方法里写 if ——这就是"切面思维"的入门实践。
 */
@Configuration
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {

    private final AuthRateLimitInterceptor authRateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
                    // 管理后台前缀：必须是 admin 角色会话（v1.3 由 checkLogin 升级，防 openid/assoc 冒充）
                    SaRouter.match("/tsa/admin/**").check(r -> StpUtil.checkRole("admin"));
                    // 本人接口：微信登录态（loginId=裸 openid）
                    SaRouter.match("/tsa/user/**").check(r -> StpUtil.checkLogin());
                    // 写端点按 HTTP 方法精确圈定：同路径的读接口（列表/公开取文件）维持免登录
                    SaRouter.match("/tsa/members").match(SaHttpMethod.POST).check(r -> StpUtil.checkLogin());
                    SaRouter.match("/tsa/files").match(SaHttpMethod.POST).check(r -> StpUtil.checkLogin());
                }))
                .addPathPatterns("/tsa/**");

        // 认证端点防刷（wechat-login 10/分、verify-phone 5/分、admin-login 5/分，超限 1306）：
        // 排在鉴权拦截器之后 —— /tsa/auth/** 本就全部免登录，两者互不干扰
        registry.addInterceptor(authRateLimitInterceptor)
                .addPathPatterns(ApiConstants.BASE_PATH + "/auth/**");
    }
}
