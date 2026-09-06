# ============================================================
# 部署期 Dockerfile（第一期本机运行不需要；二期上服务器时启用）
# 构建：docker build -t tsa-api:0.0.1 .
# 运行：docker run -d -p 8080:8080 -e SPRING_DATASOURCE_PASSWORD=xxx tsa-api:0.0.1
# 前置：先执行 ./mvnw.cmd clean package -DskipTests 生成 target/tsa-api-0.0.1-SNAPSHOT.jar
# ============================================================
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# 容器内时区设为上海，与数据库时间一致
ENV TZ=Asia/Shanghai

COPY target/tsa-api-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
