package com.tsa.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI3 文档元信息。
 *
 * <p>启动后访问 http://localhost:8080/doc.html ，
 * 所有 @Operation 注解的接口都会出现在左侧目录里，可直接在线调试。
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI tsaOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("乡会项目后端 API")
                .description("服务 uni-app 小程序与 React 官网的统一后端。所有业务接口以 /tsa 开头。")
                .version("v0.0.1"));
    }
}
