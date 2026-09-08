---
name: dev-standard
description: tsa-api 后端项目的标准开发流程与强制规范。任何后端编码任务——新增/修改接口、修 bug、重构、建表/改表、改配置/依赖——都用它，确保产出遵从项目分层、命名、测试、文档同步规范。
---

# tsa-api 开发规范流程

本 skill 是项目的强制执行流程，适用于**所有**后端编码任务。规范的权威来源是 `docs/CONTRIBUTING.md`
（分层 §3.2、命名 §3.3、注入 §3.4、类内顺序 §3.5、错误码 §3.6、数据库 §3.8、闸门 §4、清单 §5），
本 skill 只固化**流程和踩坑**，细则以文档为准；两者冲突时以文档为准，并提醒我更新 skill。

## 阶段 0：定类型、复述边界

先把任务归到一类（决定后面走哪些步骤），再复述需求边界：服务谁（小程序/官网/管理后台）、读还是写、涉及哪些表。

| 类型 | 特征 | 特殊要求 |
|------|------|---------|
| A 新增/修改接口 | 最常见迭代 | 走完整五阶段 |
| B 修 bug | 有明确的错误现象 | **先定位根因再动手**，不许"看起来该改的地方"顺手改；修复必须配一个能复现该 bug 的回归测试（先红后绿） |
| C 重构 | 不改行为改结构 | 动手前确认 `clean test` 全绿，完成后行为不变、依然全绿；接口出入参要变时先跟用户确认 |
| D 基建级 | 新表 / 新依赖 / 新中间件 / 改统一响应或鉴权规则 | 最高风险：**先给方案让用户拍板**，讲清选型理由和影响面，通过后才动代码 |

若涉及**新错误码**：按模块分段（成员 10xx / 活动 11xx / 公告 12xx），先加进 `ResultCode` 枚举再写业务。

## 阶段 1：写代码（固定顺序，按依赖方向从下往上）

类型 A/C/D 新增代码按此顺序，包路径 `com.tsa.api.*`：

1. **表变更**（如需）：改 `sql/schema.sql`（utf8mb4、逻辑删除三件套 created_at/updated_at/deleted、禁物理外键）
2. **entity**：继承 `BaseEntity`，`@TableName`、`@Schema` 齐全；敏感字段（phone/passwordHash）加 `@JsonIgnore`
3. **mapper**：`interface XxxMapper extends BaseMapper<Xxx>`，复杂 SQL 才用 XML（放 `resources/mapper/`）
4. **dto**：入参 `XxxRequest`（写）/ `XxxQuery`（读筛选），出参裁剪用 `XxxVO`，全放 `dto/` 包；校验注解 + message 全部中文 + `@Schema` example；entity 不外泄给不匹配的消费方
5. **service + impl**：接口 `extends IService<T>`（注意 import 是 `com.baomidou.mybatisplus.spring.service.IService`），实现 `extends ServiceImpl`；业务失败抛 `BusinessException`；状态用 `private static final int` 常量；分页 size 钳制上限；对外查询记得 `status=1` 过滤
6. **controller**：只做接参（`@Valid`）→ 调 service → 包 `Result`；`@Tag`/`@Operation` 注解齐全；路径 `/tsa/` 统一前缀、名词复数、多词 kebab-case

每个类内部顺序：常量 → 注入字段（`@RequiredArgsConstructor` + final，禁 `@Autowired` 字段注入）→ public 方法 → private 辅助方法。

**不合格红线**：Controller 出现业务逻辑、Service 出现 HttpRequest/Response、Mapper 出现业务判断、跨层调用（Controller 直调 Mapper）、代码里出现魔法值。

## 阶段 2：测试

- 类型 A：每个新 Controller 写 standalone MockMvc 测试（模板照抄 `src/test/java/.../MemberControllerTest.java`），至少覆盖：正常路径、参数校验失败、一个业务错误码
- 类型 B：回归测试先行——没有复现 bug 的测试就不要声称修好了
- 运行 `JAVA_HOME="D:/JDK/jdk-25" ./mvnw.cmd clean test`，必须 BUILD SUCCESS 全绿才算完成
- 改了数据库的话：用管理员方式导入 schema 增量，再起 `spring-boot:run` 实测接口（Windows 下 POST 中文测试体用 `--data-binary @utf8文件`，curl -d 内联中文会被转 GBK）

## 阶段 3：文档同步（该改的一处不落）

- `docs/API.md`：接口清单表 + 若有新错误码更新状态码表（类型 A/B/C 常涉及）
- `docs/DATABASE.md`：若建表/改表（类型 D 常涉及）
- `sql/schema.sql` 与开发库保持一致
- 若发现 CONTRIBUTING.md 与实际做法脱节：同一轮里把文档改对，不许将错就错

## 阶段 4：提交

- 分支：从当前分支拉 `feat/<中文任务名>` / `fix/<问题>` / `refactor/<范围>`（小任务经用户同意可直接提交当前分支）
- 提交信息：Conventional Commits，中文描述，一次提交只做一件事
- **绝不 `git add` `application-dev.yml`**（含本机密码）；提交前跑 `git status` 确认无密钥、无本机路径、无 target/
- 把 CONTRIBUTING.md §5 的 PR 自查清单逐项过一遍，结果汇报给用户（测试是否全绿、文档是否同步、分层/命名/注入是否干净、P3C Error 是否清零）

## 已埋好的坑提醒（不要再踩）

- JDK 23+ 编译找不到 getter → pom 已配 annotationProcessorPaths，新增注解处理器依赖时要同步加
- MyBatis-Plus 3.5.17：IService/ServiceImpl 在 `mybatis.mybatisplus.spring.service` 包；分页要 `mybatis-plus-jsqlparser` 依赖
- Knife4j 增强组件被 exclude（见 application.yml 注释），doc.html 可正常用
- schema.sql 开头必须有 `SET NAMES utf8mb4`（Windows mysql 客户端默认 GBK）
