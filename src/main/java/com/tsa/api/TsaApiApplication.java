package com.tsa.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 乡会项目后端启动类。
 *
 * <p>启动方式：
 * <ul>
 *   <li>命令行：{@code ./mvnw spring-boot:run}</li>
 *   <li>IDEA：直接右键 Run 本类</li>
 * </ul>
 *
 * <p>启动后访问 http://localhost:8080/doc.html 查看接口文档（Knife4j）。
 */
@SpringBootApplication
@MapperScan("com.tsa.api.mapper")
public class TsaApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TsaApiApplication.class, args);
    }
}
