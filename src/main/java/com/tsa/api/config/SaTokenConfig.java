package com.tsa.api.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 路由鉴权配置。
 *
 * <p>第一期策略：面向小程序/官网的公开接口全部放行；
 * 只有 /tsa/admin/** （二期管理后台）要求登录态。
 *
 * <p>教学要点：登录校验用"拦截器 + 路由规则"集中声明，
 * 而不是在每个 Controller 方法里写 if ——这就是"切面思维"的入门实践。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle ->
                        // 管理后台前缀：必须登录（第二期实现登录接口后生效）
                        SaRouter.match("/tsa/admin/**").check(r -> StpUtil.checkLogin())
                ))
                .addPathPatterns("/tsa/**");
    }
}
