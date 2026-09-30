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
 * Sa-Token 路由鉴权 + MVC 拦截器注册：规则集中声明，不散落进各 Controller。
 *
 * <p>路由规则：
 * <ul>
 *   <li>{@code /tsa/admin/**}：管理后台，<b>必须是 admin 角色会话</b>（{@code loginId} 以
 *       {@code admin-} 前缀签发）。用 {@code checkRole("admin")} 而非 checkLogin：openid 裸登录态
 *       虽能过 checkLogin，但拿不到 "admin" 角色（角色判定见 {@link StpInterfaceImpl}），
 *       一律 NotRoleException → 403 —— 冒充面就此收口</li>
 *   <li>{@code /tsa/user/**}：微信登录受保护面（GET /me 与 PUT /profile 共用前缀）</li>
 *   <li>{@code POST /tsa/files}：文件上传需登录态；同路径的 GET 公开读<b>不在</b>此列，
 *       故必须按方法匹配，不能只 match 路径</li>
 *   <li>其余 {@code /tsa/**}（含 {@code /tsa/auth/**} 全部端点）：公开 —— 登录本就要给未登录的人用</li>
 * </ul>
 */
@Configuration
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {

    private final AuthRateLimitInterceptor authRateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
                    // 管理后台前缀：必须是 admin 角色会话（checkRole 防 openid 登录态冒充）
                    SaRouter.match("/tsa/admin/**").check(r -> StpUtil.checkRole("admin"));
                    // 本人接口：微信登录态（loginId=裸 openid）
                    SaRouter.match("/tsa/user/**").check(r -> StpUtil.checkLogin());
                    // 写端点按 HTTP 方法精确圈定：同路径的读接口（公开取文件）维持免登录
                    SaRouter.match("/tsa/files").match(SaHttpMethod.POST).check(r -> StpUtil.checkLogin());
                }))
                .addPathPatterns("/tsa/**");

        // 防刷闸门（认证组 5~10/分；社区写组——发布/点赞/评论/上传——5 次/时/IP，超限 1306）：
        // 排在鉴权拦截器之后 —— 这些端点本就全部免登录，两者互不干扰。
        // 社区整前缀挂上即可：拦截器只对 POST + 命中桶名的路径计数，GET 读端点一律放行；
        // 点赞/评论带帖子 id 的写口靠归一桶收口（口径见 AuthRateLimitInterceptor 类注释）
        registry.addInterceptor(authRateLimitInterceptor)
                .addPathPatterns(ApiConstants.BASE_PATH + "/auth/**",
                        ApiConstants.BASE_PATH + "/community/**");
    }
}
