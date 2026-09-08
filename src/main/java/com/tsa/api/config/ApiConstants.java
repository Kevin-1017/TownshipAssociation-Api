package com.tsa.api.config;

/**
 * API URL 常量汇总。所有 Controller 的 {@code @RequestMapping} 前缀统一从这里引用。
 */
public final class ApiConstants {

    /** 全局前缀，例：{@code /tsa/members} */
    public static final String BASE_PATH = "/tsa";

    private ApiConstants() {
        // prevent instantiation
    }
}
