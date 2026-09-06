# 乡会项目后端技术选型说明（TECH_STACK）

> 版本：v1.0（2026-09）  
> 适用读者：参与维护本项目的学生、教师  
> 结论先行：**Spring Boot 3.5.x + JDK 25 + MyBatis-Plus + MySQL 8 + Sa-Token + Knife4j，单体项目、Maven 构建**。

---

## 1. 项目背景与选型原则

本后端是乡会平台的**统一数据服务**，同时服务两类消费方：

- 现在：uni-app 微信小程序（成员地图、列表、公告）
- 未来：React（Next.js）官网 + 管理后台

项目定位是**教学载体**——要能交给一届届学生维护、学习、写进简历。因此选型按以下权重打分：

| 原则 | 含义 |
|------|------|
| 贴合招聘 | 学生在项目里学的 = JD 上考的，简历关键词直接可用 |
| 上手平滑 | 学生有 Java/数据库前置课程，学习曲线不能太陡 |
| 资料丰富 | 踩坑后中文社区必须搜得到答案 |
| 架构清晰 | 分层明确，适合按难度拆分任务给学生 |
| 规模匹配 | 乡会数据量（千级成员、百级活动）不需要任何分布式组件 |

## 2. 最终技术栈全景

| 层次 | 技术 | 版本 | 选型理由 |
|------|------|------|---------|
| 语言/运行时 | Java (Temurin JDK) | **25 LTS** | 最新 LTS；虚拟线程等新特性有教学价值。若遇生态兼容问题回退 JDK 21（仅改配置，见 §5） |
| Web 框架 | **Spring Boot**（内含 Spring MVC） | 3.5.16 | 国内 Java Web 事实标准，JD 提及率 >90%；B站/掘金中文教程数量碾压其他框架；报错信息自带修复提示，适合学生自学排障。**Controller/HTTP 层就是它提供的，不需要别的框架** |
| 参数校验 | Spring Validation | Boot 内置 | `@Valid` + 注解式校验，DTO 设计是后端基本功 |
| ORM | **MyBatis-Plus** | 3.5.17 | 国内企业主流；SQL 可见（不像 JPA 黑盒），学生能理解每条查询；自带分页插件、逻辑删除、字段自动填充，省掉 80% 样板代码 |
| 数据库 | **MySQL** | 8.0（本机 8.0.36） | 数据量绰绰有余；招聘覆盖率第一的关系库。8.0.x 与国内生态兼容性最稳，**无需升级 8.4/9.x** |
| 连接池 | HikariCP | Boot 内置 | Boot 默认，性能足够；不引 Druid 降低复杂度 |
| 认证授权 | **Sa-Token** | 1.46.0 | 3 行代码实现登录鉴权；Spring Security 配置复杂易劝退初学者。国内资料丰富 |
| API 文档 | **Knife4j**（OpenAPI3） | 4.5.0 + springdoc 2.8.17 | 自动生成 `/doc.html` 在线调试页，前后端联调、学生自测必备 |
| 工具库 | Lombok / Hutool | 1.18.48 / 5.8.47 | Lombok 减少样板代码；Hutool 中文工具库，教程常见 |
| 构建 | **Maven + Wrapper** | 3.9.16 | 国内企业绝对主流；Wrapper（`mvnw`）让新学生免装 Maven，克隆即构建 |
| 测试 | JUnit5 + Mockito + MockMvc | Boot 内置 | 已提供不依赖数据库的 Web 层测试范例 |
| 缓存 | Redis | **二期接入**（届时用 Redis 7） | 本机旧版 3.0 暂不使用；二期通过 docker-compose 起官方 redis:7（见根目录 compose 文件注释） |
| 文件存储 | MinIO / 阿里云 OSS | **二期接入** | 头像上传；已预留 `FileStorageService` 接口 |
| 地图服务 | 腾讯位置服务 API | 二期接入 | 地理编码/逆编码；配置位已留（`tsa.map.qq-key`） |

## 3. 明确不用的技术，以及为什么（面试被问就照这个答）

| 技术 | 为什么不用 |
|------|-----------|
| **微信云开发** | 强绑定微信生态、无标准 REST API，React 官网无法复用；不算真正的后端能力 |
| **Node.js (NestJS)** | 教学场景下中文资料与高校衔接不如 Java；且本项目目标是长期学生维护而非个人快速上线 |
| **Dubbo** | 它是**微服务之间 RPC 内部通信**框架，不处理 HTTP，和"写 Controller"根本不是一个层面的东西。单体项目用它 = 杀鸡用牛刀 |
| **Spring Cloud / 网关 / 注册中心** | 微服务全家桶留给日活百万级场景；几十个学生的项目上只会增加部署与认知负担 |
| **JPA / Hibernate** | 生成 SQL 黑盒太多，学生难理解底层；国内企业也主流 MyBatis 系 |
| **Gradle** | 构建更快但国内 Java 教学与招聘占比低，学生还要多学一套 DSL |
| **Quarkus / Micronaut / Solon** | 理念先进但生态小、中文踩坑资料少，出问题学生搜不到答案 |
| **消息队列（Kafka/RabbitMQ）** | 乡会场景暂无异步解耦需求，作为 L5 进阶任务预留 |
| **Spring Boot 4.x** | 2025.11 已发布，但 MyBatis-Plus/Sa-Token/Knife4j 对 Spring Framework 7 的适配仍在成熟期，3.5.x 是当前"新而稳"的最优点 |

## 4. 架构与分层 = 教学大纲

```
小程序 uni-app ──┐
                 ├──HTTPS──▶ Nginx(部署期) ──▶ Spring Boot 单体 ──▶ MySQL 8
React 官网 ──────┘                              │
                                                ├─ controller/  接参、校验、路由
                                                ├─ service/     业务逻辑、事务
                                                ├─ mapper/      数据访问（SQL）
                                                ├─ entity/dto/  数据模型与传输对象
                                                ├─ config/      鉴权、文档、分页等横切配置
                                                └─ common/      统一响应、全局异常
```

**一个项目统一管理后端是正确的**：接口按 `/api/v1/` 版本化 + RESTful 设计，未来无论接入多少前端（小程序/H5/官网/后台）都共用这一套 API，不需要拆第二个后端项目。

学生任务分级（与分层架构对应）：

| 级别 | 任务示例 | 涉及层 |
|------|---------|--------|
| L1 | 新增"荣誉墙"列表接口 | controller + mapper |
| L2 | 成员注册 + 校验 + 头像上传 | service + DTO + FileStorage |
| L3 | 地图聚合接口 + Redis 缓存 | 性能优化、缓存策略 |
| L4 | 微信登录 + Sa-Token 鉴权 | 第三方集成、安全 |
| L5 | CI/CD、消息队列、模块重构 | 工程化与架构 |

## 5. JDK 25 兼容性说明与回退方案

Spring Boot 3.5.6+ 官方支持 JDK 25；本项目已按支持版本锁定依赖（Lombok ≥1.18.48、MyBatis-Plus ≥3.5.17 等）。

若构建或运行遇到第三方库字节码报错：

1. 优先升级对应依赖到最新 patch；
2. 仍不行则回退 JDK 21：修改 `pom.xml` 中 `<java.version>` 为 21，并将 `JAVA_HOME` 指向 JDK 21 —— **业务代码零改动**。

### 已实测踩坑记录（本仓库已全部修复，勿回退）

| 现象 | 原因 | 修复位置 |
|------|------|---------|
| 编译报"找不到符号 getXxx()"，Lombok 好像没生效 | JDK 23 起 javac 不再自动执行 classpath 上的注解处理器 | `pom.xml` 显式声明 `annotationProcessorPaths` |
| `IService`/`ServiceImpl` 包路径找不到 | MyBatis-Plus 3.5.17 重构，挪到 `com.baomidou.mybatisplus.spring.service` | 各 service import |
| `PaginationInnerInterceptor` 找不到 | 3.5.9 起分页插件拆到独立模块 | `pom.xml` 补 `mybatis-plus-jsqlparser` |
| 访问 /v3/api-docs 报 `NoSuchMethodError: getGroupConfigs` | Knife4j 4.5.0（最后版本）按旧版 springdoc 编译，与 2.8.x 不兼容 | `application.yml` 排除 `Knife4jAutoConfiguration`，doc.html 保留可用 |

## 6. 环境与部署规划

| 阶段 | 内容 |
|------|------|
| 开发期（现在） | 本机 JDK 25 + 本机 MySQL80 服务 + `mvnw` 构建；Redis 不用 |
| 二期 | Redis 7（docker-compose）、文件存储、微信登录 |
| 部署期 | 服务器 Docker Compose 编排（应用 + MySQL + Redis + Nginx），仓库已留 Dockerfile 模板 |

## 7. 代码规范约定

- 遵循《阿里巴巴 Java 开发手册》（IDEA 安装 **Alibaba Java Coding Guidelines** 插件）
- 提交信息遵循 Conventional Commits：`feat: 新增公告置顶` / `fix: 修复分页越界`
- 分支模型：`main`（可发布）← `dev`（集成）← `feat/xxx`（每人每任务一个分支 + PR Review）
- 统一响应结构见 `docs/API.md`；数据库变更必须同步更新 `sql/schema.sql` 与 `docs/DATABASE.md`
