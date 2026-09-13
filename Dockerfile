# ============================================================
# 部署期 Dockerfile（第一期本机运行不需要；二期上服务器时启用）
# 构建：docker build -t tsa-api:0.0.1 .
# 运行：docker run -d -p 8080:8080 \
#          -e SPRING_DATASOURCE_URL='jdbc:mysql://<db_host>:3306/tsa?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true' \
#          -e SPRING_DATASOURCE_USERNAME=<账号> -e SPRING_DATASOURCE_PASSWORD=xxx \
#          tsa-api:0.0.1
#       （数据源只能靠上面 -e 注入：prod 在仓库里没有 application-prod.yml，凭据一律不入库不进镜像；
#         如需临时覆盖 profile 可再加 -e SPRING_PROFILES_ACTIVE=...）
# 前置：先执行 ./mvnw.cmd clean package -DskipTests 生成 target/tsa-api-0.0.1-SNAPSHOT.jar
# ============================================================
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# 容器内时区设为上海，与数据库时间一致
ENV TZ=Asia/Shanghai

# 显式激活 prod profile（计划修订 A6 的部署侧半边）：application.yml 写死 active: dev，
# 镜像若不带这个 ENV，容器起来仍是 dev「裸跑」——WechatClientImpl 的 mock 启动守卫
# （白名单 {dev,test}，之外的环境任一 mock 开关为 true 即拒绝启动）永不触发，
# login-mock-mode 可带着 true 上线，任何人拿 mock-openid-local 冒充任意用户。
# ENV 可被 docker run -e 覆盖，本行只是把「忘记设 profile」从默认失败方向翻成默认安全。
ENV SPRING_PROFILES_ACTIVE=prod

COPY target/tsa-api-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
