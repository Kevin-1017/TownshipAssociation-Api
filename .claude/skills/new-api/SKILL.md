---
name: new-api
description: 在 tsa-api 后端项目中新增或修改接口/功能的标准开发流程。当用户要求"新增接口/加个功能/新建表/实现某需求"时使用，确保产出遵从项目分层、命名、测试、文档同步规范。
---

# tsa-api 新增接口标准流程

本 skill 是项目的强制执行流程。任何新增/修改接口的需求，按以下阶段走完，不得跳步。
规范依据（有冲突时以文档为准并提醒我更新 skill）：`docs/CONTRIBUTING.md`、`docs/API.md`、`docs/DATABASE.md`。

## 阶段 0：动手前确认

1. 先复述需求边界：这个接口服务谁（小程序/官网/管理后台）、读还是写、涉及哪些表
2. 若涉及**新表、新依赖、新中间件、改统一响应/鉴权规则**：这是基建级改动，先给出方案让用户拍板，再继续
3. 若涉及**新错误码**：按模块分段（成员 10xx / 活动 11xx / 公告 12xx），先加进 `ResultCode` 枚举

## 阶段 1：写代码（固定顺序）

按依赖方向从下往上写，包路径 `com.tsa.api.*`：

1. **表变更**（如需）：改 `sql/schema.sql`（utf8mb4、逻辑删除三件套 created_at/updated_at/deleted、禁物理外键）
2. **entity**：继承 `BaseEntity`，`@TableName`、`@Schema` 齐全；敏感字段（phone/passwordHash）加 `@JsonIgnore`
3. **mapper**：`interface XxxMapper extends BaseMapper<Xxx>`，复杂 SQL 才用 XML（放 `resources/mapper/`）
4. **dto**：入参 `XxxRequest`（校验注解 + message 全部中文 + `@Schema` example）、出参裁剪用 `XxxVO`；entity 不外泄给不匹配的消费方
5. **service + impl**：接口 `extends IService<T>`（注意 import 是 `com.baomidou.mybatisplus.spring.service.IService`），实现 `extends ServiceImpl`；业务失败抛 `BusinessException`；状态用 `private static final int` 常量；分页 size 钳制上限；对外查询记得 `status=1` 过滤
6. **controller**：只做接参（`@Valid`）→ 调 service → 包 `Result`；`@Tag`/`@Operation` 注解齐全；路径 `/api/v1/` 版本化

分层红线：Controller 出现业务逻辑、Service 出现 HttpRequest/Response、代码里出现魔法值 = 不合格。

## 阶段 2：测试

- 每个新 Controller 写 standalone MockMvc 测试（模板照抄 `src/test/java/.../MemberControllerTest.java`），至少覆盖：正常路径、参数校验失败、一个业务错误码
- 运行 `JAVA_HOME="D:/JDK/jdk-25" ./mvnw.cmd clean test`，必须 BUILD SUCCESS 全绿才算完成
- 改了数据库的话：用管理员方式导入 schema 增量，再起 `spring-boot:run` 实测接口（Windows 下 POST 中文测试体用 `--data-binary @utf8文件`，curl -d 内联中文会被转 GBK）

## 阶段 3：文档同步（三处，缺一即未完成）

- `docs/API.md`：接口清单表 + 若有新错误码更新状态码表
- `docs/DATABASE.md`：若建表/改表
- `sql/schema.sql` 与开发库保持一致

## 阶段 4：提交

- 分支：从当前分支拉 `feat/<中文任务名>` 或 `fix/<问题>`（小任务经用户同意可直接提交当前分支）
- 提交信息：Conventional Commits，中文描述，如 `feat: 新增活动报名接口`
- **绝不 `git add` `application-dev.yml`**（含本机密码）；提交前跑 `git status` 确认无密钥、无本机路径、无 target/
- 把 CONTRIBUTING.md §5 的 PR 自查清单结果汇报给用户（测试是否全绿、文档是否同步、分层是否干净）

## 已埋好的坑提醒（不要再踩）

- JDK 23+ 编译找不到 getter → pom 已配 annotationProcessorPaths，新增注解处理器依赖时要同步加
- MyBatis-Plus 3.5.17：IService/ServiceImpl 在 `mybatis.mybatisplus.spring.service` 包；分页要 `mybatis-plus-jsqlparser` 依赖
- Knife4j 增强组件被 exclude（见 application.yml 注释），doc.html 可正常用
- schema.sql 开头必须有 `SET NAMES utf8mb4`（Windows mysql 客户端默认 GBK）
