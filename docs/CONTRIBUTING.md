# 开发规范（CONTRIBUTING）

> 新同学必读。这份文档规定"在这个仓库里怎么写代码才算对"。
> 配套的详细文档：[技术选型](TECH_STACK.md) · [接口规范](API.md) · [数据库设计](DATABASE.md)

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
| 分支 | 用途 | 谁能合入 |
|------|------|---------|
| `main` | 可随时部署的稳定版本 | 仅管理员，从 dev 合并 |
| `dev` | 集成测试分支 | 经 Review 的 PR |
| `feat/成员审核`、`fix/分页越界` | 每人每任务一个短生命周期分支 | 不提 PR 就不进公共仓库 |

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
- ❌ 直接向 `dev`/`main` push，必须走 PR
- ❌ `git push -f` 公共分支

## 3. 代码规范

### 分层纪律（最重要的一条）
| 层 | 允许做 | 禁止做 |
|----|--------|--------|
| Controller | 接参、`@Valid`、调 service、包 `Result` | ❌ 写任何业务逻辑 ❌ 直接调 Mapper |
| Service | 业务规则、事务 `@Transactional`、抛 `BusinessException` | ❌ 出现 `HttpServletRequest/Response` |
| Mapper | 数据访问 | ❌ 业务判断 |

### 命名与结构
- 请求对象 `XxxRequest`、响应视图 `XxxVO`、表实体进不过出（entity 不外泄时用 VO 裁剪）
- 常量不写魔法值：状态一律定义 `private static final int STATUS_APPROVED = 1;`（参考 `MemberServiceImpl`）
- 新错误码进 `ResultCode` 枚举按模块分段（成员 10xx、活动 11xx、公告 12xx），并同步更新 API.md 状态码表
- 类与公共方法写中文 Javadoc，说明"为什么"而不只是"是什么"（参考现有文件）

### 数据库
- 禁止物理删除，统一逻辑删除（`@TableLogic` 已配好）
- 查询必须带分页或明确上限；列表接口强制过滤 `status=1` 的规则别遗漏
- 改表四步走：改 `sql/schema.sql` → 开发库执行 → 更新 `docs/DATABASE.md` → PR 里说明（见 DATABASE.md 约定）

### 测试
- 每个新 Controller 至少一个 standalone MockMvc 测试（模板：`MemberControllerTest`），不连数据库
- 提交前 `./mvnw.cmd clean test` 必须全绿

## 4. 代码风格工具

- IDEA 安装插件 **Alibaba Java Coding Guidelines**（P3C），提交前跑一次右键 → 规约扫描，Error 级必须清零
- 仓库根的 `.editorconfig` 统一了缩进/编码，IDEA 原生支持，无需配置
- 缩进 4 空格、类成员顺序：常量 → 字段 → 构造 → 公共方法 → 私有方法

## 5. PR 自查清单（贴进 PR 描述里逐项勾选）

```
- [ ] ./mvnw.cmd clean test 通过
- [ ] 本地启动，doc.html 里自测过新增/修改的接口
- [ ] 分层纪律：Controller 无逻辑、Service 无 Web 对象、无魔法值
- [ ] 新接口已写 MockMvc 测试
- [ ] 文档已同步：API.md 接口清单 / DATABASE.md（若改表）/ ResultCode 表（若加码）
- [ ] 无密钥、密码、本机路径等不应入库的内容
```

PR 标题同样用 Conventional Commits 格式；至少一人 Review 通过后由管理员合入。

## 6. 环境要求（新成员入职配置）

JDK 25（配到 Project Structure，见 README）· MySQL 8 服务 · IDEA 装 Lombok 插件并开启 Annotation Processing。Redis/MinIO 二期才需要。
