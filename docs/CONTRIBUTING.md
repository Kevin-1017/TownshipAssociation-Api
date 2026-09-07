# 开发规范（CONTRIBUTING）

> 新同学必读。这份文档规定"在这个仓库里怎么写代码才算对"。
> 配套的详细文档：[技术选型](TECH_STACK.md) · [接口规范](API.md) · [数据库设计](DATABASE.md)
>
> 核心理念只有四句：**声明先于使用、职责单一、依赖方向单向、约定优于配置**。
> 下面所有细则都是这四句话在具体场景下的展开，遇到文档没覆盖的情况，回到理念做判断。

## 1. 日常开发的主循环

后端的绝大部分迭代就是**新增/修改接口**，标准流程（详细版见 API.md 末尾 Checklist）：

```
领任务 → 从 dev 拉 feat/xxx 分支 → 写代码（顺序：entity/mapper → service → controller → 测试）
→ 本地跑 ./mvnw.cmd clean test + 启动自测 doc.html → 更新文档 → 提 PR → 别人 Review → 合入 dev
```

不是"只新增接口"的情况（每期开头会有一次性基建，做完又回到加接口）：
引入新依赖/中间件（如 Redis）、新增数据表、改统一响应或鉴权规则——这类改动需要先在组内对齐方案再动手。

## 2. Git 规范

### 分支模型
项目起步阶段采用**单分支开发**：`master` 是唯一共享分支，所有产出最终都汇入它。

| 分支 | 用途 | 规则 |
|------|------|------|
| `master` | 开发与可部署版本一体 | 短任务分支合入，或经约定直接推送 |
| `feat/成员审核`、`fix/分页越界` | 每任务一个短生命周期分支，合入即删 | 随意开，但不经约定不进共享仓库 |

团队扩大、开始并行开发时再升级：`master` 只留可部署的稳定版本，新增 `dev` 集成测试分支，一切经 PR + Review 合入。

### 提交信息：Conventional Commits
格式 `<类型>: <一句话说清做了什么>`，类型只用这五个：

```
feat:     新功能（feat: 新增活动报名接口）
fix:      修 bug（fix: 修复成员列表 size 越界）
docs:     只改文档（docs: 补充地图接口参数说明）
refactor: 重构不改行为（refactor: 抽取坐标校验为工具方法）
chore:    构建/依赖/脚本（chore: 升级 mybatis-plus 到 3.5.17）
```

一次提交只做一件事；禁止 `update`、`111`、`改好了` 这类提交信息。

### 硬性禁令
- ❌ 把真实密码、appid/secret、token 提交进仓库（配置用占位符或环境变量，如 `application-dev.yml` 的本地改动不要提交）
- ❌ `git push -f` 公共分支
- （升级为多分支模型后追加：直接向 `dev`/`master` push 必须走 PR）

## 3. 代码规范

### 3.1 目录结构与依赖方向

所有代码在包路径 `com.tsa.api.*` 下，**新类必须按职责落进对应的包**：

```
src/main/java/com/tsa/api/
├── TsaApiApplication.java   # 启动类，只放这一个
├── common/                  # 横切公共件：Result / ResultCode / BusinessException / GlobalExceptionHandler
├── config/                  # @Configuration 配置类：CORS、Knife4j、MyBatis-Plus、Sa-Token
├── controller/              # 接口层：接参校验 → 调 service → 包 Result
├── service/                 # 业务层接口（extends IService<T>）
│   └── impl/                # 业务实现（extends ServiceImpl）
├── mapper/                  # 数据访问层（extends BaseMapper<T>）
├── entity/                  # 数据库实体，与表一一对应，继承 BaseEntity
└── dto/                     # 所有跨接口边界的对象：XxxRequest / XxxQuery / XxxVO
```

> 说明：本项目 VO 不单独建 `vo` 包——目前对象量级还小，全放 `dto/` 一个包里反而好找；
> 等哪天 dto 超过 20 个类，再拆 `dto/` + `vo/`，并同步更新本文档。

**依赖方向单向，禁止反向或跨层调用**：

```
Controller ──→ Service ──→ Mapper ──→ 数据库
     ✗ 不允许 Controller 直接调 Mapper（跳层）
     ✗ 不允许 Service 引用 Controller 的类（反向）
     ✗ 不允许 Mapper 里出现业务判断
```

### 3.2 分层纪律（最重要的一条）
| 层 | 允许做 | 禁止做 |
|----|--------|--------|
| Controller | 接参、`@Valid`、调 service、包 `Result` | ❌ 写任何业务逻辑 ❌ 直接调 Mapper |
| Service | 业务规则、事务 `@Transactional`、抛 `BusinessException` | ❌ 出现 `HttpServletRequest/Response` |
| Mapper | 数据访问 | ❌ 业务判断 |

### 3.3 命名规范

| 类型 | 规则 | 项目里的真实示例 |
|------|------|-----------------|
| 类名 | PascalCase + 后缀标明所在层 | `MemberController` / `MemberService` / `MemberServiceImpl` / `MemberMapper` |
| 写接口入参 | `XxxRequest` | `MemberSaveRequest` |
| 读接口筛选参数 | `XxxQuery` | `MemberQuery` |
| 出参裁剪对象 | `XxxVO` | `MapMarkerVO` |
| 方法 | camelCase、动词开头；查询用 `get/list/page`，写操作用 `save/update/remove` | `pageQuery` / `listMapMarkers` / `register` |
| 常量 | UPPER_SNAKE_CASE | `STATUS_APPROVED` |
| REST 路径 | `/api/v1/` 版本化 + 名词复数，多词用 kebab-case | `/api/v1/members/map-data` |
| 数据库表/字段 | snake_case，表名单数 | `member` / `activity_registration` / `created_at` |
| 包名 | 全小写、无下划线 | `com.tsa.api.service.impl` |

DTO 三件套的共同原则：**接口契约先于实现**。看一个接口的 Request/VO，
就知道它能接收什么、返回什么，不用点进 Controller 源码。entity 不外泄给不匹配的消费方
（小程序要的字段和管理后台要的不一样时，各给各的 VO）。

### 3.4 依赖注入：只有一种写法

```java
@RestController
@RequiredArgsConstructor          // Lombok 生成构造器
public class MemberController {
    private final MemberService memberService;   // 必须 final
}
```

- ✅ 构造器注入（`@RequiredArgsConstructor` + `private final`），不可变、可直接 new 出来写单测、循环依赖在启动时就暴露
- ❌ 禁止 `@Autowired` 字段注入——它把依赖关系藏进框架魔法里，测试要起 Spring 容器才能注入

### 3.5 类内部成员顺序（从上到下）

```java
public class XxxServiceImpl ... {
    // 1. 静态常量（CACHE_KEY_PREFIX、STATUS_APPROVED...）
    // 2. 注入的依赖字段（全部 private final）
    // 3. 构造器（用了 @RequiredArgsConstructor 就没有这一段）
    // 4. public 方法——按业务流程排，CRUD 统一 create → read → update → delete
    // 5. private 辅助方法（toVO、validate...），紧跟调用它的 public 方法之后
}
```

原则：自上而下阅读时先看"这个类能做什么"，再看"怎么做的"。
public 在前、private 在后；辅助方法不要甩到文件末尾让人来回翻。

### 3.6 常量、魔法值与错误码

- 零容忍魔法值：状态一律定义 `private static final int STATUS_APPROVED = 1;`（参考 `MemberServiceImpl`）
- 新错误码进 `ResultCode` 枚举按模块分段（成员 10xx、活动 11xx、公告 12xx），并同步更新 API.md 状态码表

### 3.7 注释

- 类与公共方法写中文 Javadoc，说明"**为什么**"而不只是"是什么"（参考现有文件里的"教学要点"写法）
- import 顺序：JDK → 第三方框架 → 本项目，IDEA 默认的 optimize imports 即可，不要手动打乱

### 3.8 数据库
- 禁止物理删除，统一逻辑删除（`@TableLogic` 已配好）；每张表三件套 `created_at` / `updated_at` / `deleted` 由 `BaseEntity` 提供
- 查询必须带分页或明确上限；列表接口强制过滤 `status=1` 的规则别遗漏
- 改表四步走：改 `sql/schema.sql` → 开发库执行 → 更新 `docs/DATABASE.md` → PR 里说明（见 DATABASE.md 约定）

### 3.9 测试
- 每个新 Controller 至少一个 standalone MockMvc 测试（模板：`MemberControllerTest`），不连数据库
- 提交前 `./mvnw.cmd clean test` 必须全绿

## 4. 规范靠什么保证：三道闸门 + 演进路线

规范靠人记不住，也劝不住，要靠流程和工具强制执行。本项目按成本从低到高设了三道闸门：

| 闸门 | 工具 | 什么时候用 |
|------|------|-----------|
| ① 编辑器层 | `.editorconfig`（缩进/编码统一）+ IDEA 插件 **Alibaba Java Coding Guidelines (P3C)** | 随时生效；提交前右键 → 规约扫描，Error 级必须清零 |
| ② 测试层 | `./mvnw.cmd clean test`（MockMvc 覆盖正常/校验失败/业务错误码） | 每次提交前 |
| ③ 评审层 | 第 5 节 PR 自查清单 + 至少一人 Review | 每次合入 dev 前 |

**当前有意不引入的东西**（知道边界在哪，比堆工具更重要）：

- *Checkstyle / Spotless*：P3C + editorconfig 目前够用；等团队超过 5 人、代码风格争议变多时再上，配到 `mvn validate` 里强制执行
- *ArchUnit*：能把"Controller 不许调 Mapper""ServiceImpl 必须在 impl 包"写成单元测试挂进 CI。3.1/3.2 两条红线现在靠 Review 人肉把关，**这是二期最值得先加的**——加一个 `ArchitectureTest.java` 即可，成本低、一劳永逸
- *MapStruct*：DTO ↔ entity 转换目前用 Hutool `BeanUtil.copyProperties`，简单场景够用；当出现字段名不一致、多表拼装、嵌套映射时换 MapStruct（编译期生成代码、零反射、改字段立刻报错）
- *Lombok*：只允许 `@RequiredArgsConstructor`、`@Data`（entity/dto）、`@Slf4j`；不允许 `@SneakyThrows`、`@Builder` 满天飞——以本文件为准，逐步收紧

## 5. PR 自查清单（贴进 PR 描述里逐项勾选）

```
- [ ] ./mvnw.cmd clean test 通过
- [ ] 本地启动，doc.html 里自测过新增/修改的接口
- [ ] 分层纪律：Controller 无逻辑、Service 无 Web 对象、Mapper 无业务判断、无跨层调用
- [ ] 注入方式是 @RequiredArgsConstructor + final，无 @Autowired 字段注入
- [ ] 命名符合 3.3 表格；类内顺序符合 3.5（常量 → 字段 → public → private）
- [ ] 无魔法值；新错误码已进 ResultCode 分段
- [ ] 新接口已写 MockMvc 测试
- [ ] 文档已同步：API.md 接口清单 / DATABASE.md（若改表）/ ResultCode 表（若加码）
- [ ] 无密钥、密码、本机路径等不应入库的内容
- [ ] P3C 规约扫描 Error 级清零
```

PR 标题同样用 Conventional Commits 格式；至少一人 Review 通过后由管理员合入。

## 6. 环境要求（新成员入职配置）

JDK 25（配到 Project Structure，见 README）· MySQL 8 服务 · IDEA 装 Lombok 插件并开启 Annotation Processing。Redis/MinIO 二期才需要。
