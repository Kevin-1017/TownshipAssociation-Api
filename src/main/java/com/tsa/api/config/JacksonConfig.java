package com.tsa.api.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 定制：LocalDateTime 序列化为「ISO 8601 带时区偏移」，如 2026-09-08T08:43:35+08:00。
 *
 * <p>踩坑记录（三步走出来的）：
 * <ul>
 *   <li>application.yml 的 spring.jackson.date-format 只对 java.util.Date 生效，
 *       LocalDateTime 仍走 JSR-310 默认格式（无时区、带纳秒）</li>
 *   <li>给 formatter 挂 withZone 也不行：LocalDateTime 没有偏移量信息，
 *       模式里的 XXX 在 OffsetIdPrinterParser 处直接抛异常</li>
 *   <li>正解：序列化前先 atZone 补上库时区，自定义序列化器输出</li>
 * </ul>
 *
 * <p>为什么要较真这个格式：小程序 iOS 端解析空格分隔格式会失败；
 * 时间必须带时区语义。反序列化同样支持带偏移的入参。
 */
@Configuration
public class JacksonConfig {

    /** 与数据库/服务器时区保持一致 */
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    @Bean
    public JavaTimeModule javaTimeModule() {
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(LocalDateTime.class, new ZoneAwareSerializer());
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(FORMATTER));
        return module;
    }

    /** LocalDateTime 本身不带时区 —— 输出前按库时区补齐偏移再格式化 */
    private static class ZoneAwareSerializer extends JsonSerializer<LocalDateTime> {
        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeString(FORMATTER.format(value.atZone(ZONE)));
        }
    }
}
