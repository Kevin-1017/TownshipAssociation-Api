package com.tsa.api.controller;

import com.tsa.api.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 健康检查接口：部署后运维/监控探活用，也作为学生认识项目的第一步。
 */
@Tag(name = "系统", description = "健康检查等系统级接口")
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @Operation(summary = "健康检查", description = "返回 ok 表示服务存活")
    @GetMapping("/health")
    public Result<Map<String, String>> health() {
        return Result.ok(Map.of("status", "ok", "time", LocalDateTime.now().toString()));
    }
}
