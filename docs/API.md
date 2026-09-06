# API 规范（API）

在线文档（启动后）：**http://localhost:8080/doc.html**（Knife4j，可直接调试）

## 通用约定

- 路径前缀 `/api/v1`，版本升级（不兼容变更）时开 `/api/v2`，旧版本保留一个迭代周期
- Method 语义：GET 查询 / POST 新增 / PUT 修改 / DELETE 删除
- 所有响应统一包装（HTTP 状态码恒为 200，成败看 `code`）：

```json
{ "code": 200, "message": "操作成功", "data": { } }
```

## 状态码表（对应 `common/ResultCode.java`）

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 参数校验失败（message 内含具体字段提示） |
| 401 | 未登录 / token 过期（Sa-Token） |
| 403 | 无权限 |
| 404 | 路径不存在 |
| 500 | 服务器内部错误（日志里有完整堆栈，对外不泄露） |
| 1001 | 该微信已注册过成员 |
| 1002 | 数据不存在 |

新增业务码规则：1xxx 起，按模块分段（成员 10xx、活动 11xx、公告 12xx），加码先更新本表。

## 认证约定

- 请求头 `tsa-token: <token值>`（字段名在 `application.yml > sa-token.token-name`）
- 一期所有公开查询接口免登录；`/api/v1/admin/**` 一律要求登录（二期生效）

## 一期接口清单

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/health` | 健康检查 |
| GET | `/api/v1/members` | 成员分页列表（参数：page,size,city,industry,keyword） |
| GET | `/api/v1/members/map-data` | 地图打点轻量数据 |
| POST | `/api/v1/members` | 成员注册（status 置 0 待审核） |
| GET | `/api/v1/announcements` | 公告列表（置顶优先） |

分页响应 `data` 结构（MyBatis-Plus IPage 序列化）：

```json
{ "records": [ ... ], "total": 100, "current": 1, "size": 10, "pages": 10 }
```

## 新增接口 Checklist（学生模板）

1. `dto/` 定义请求对象 + 校验注解（参考 `MemberSaveRequest`）
2. `entity/`、`mapper/` 已有一张表则继承 `BaseMapper`
3. `service/` 接口 + `impl/` 实现（业务规则、事务在这里；抛错用 `BusinessException`）
4. `controller/` 接参（`@Valid`）→ 调 service → 包 `Result`（不写逻辑）
5. `@Tag/@Operation` 注解补全文档，启动后在 doc.html 自测
6. 写一个 `MemberControllerTest` 式的 standalone MockMvc 测试
7. 更新本文档接口清单 + 提交 PR
