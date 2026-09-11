# API 规范（API）

在线文档（启动后）：**http://localhost:8080/doc.html**（Knife4j，可直接调试）

本文件与 `tsa-miniprogram/docs/API.md` 是同一份契约的两半 —— 改任何一边都要同步另一边。

## 通用约定

- 路径前缀 `/tsa`，统一在 `ApiConstants.BASE_PATH` 中维护
- Method 语义：GET 查询 / POST 新增 / PUT 修改 / DELETE 删除
- 所有响应统一包装（HTTP 状态码恒为 200，成败看 `code`）：

```json
{ "code": 200, "message": "操作成功", "data": { } }
```

- **id 一律序列化为字符串**（数据库仍是 BIGINT）：前端 JS number 超过 2^53 会丢精度，
  统一在 `BaseEntity.id` / VO 上以 `@JsonSerialize(ToStringSerializer)` 输出
- **时间一律 ISO 8601 带时区**（`2026-09-01T09:00:00+08:00`），见 `JacksonConfig`；
  小程序 iOS 端解析不了空格分隔格式，别改回 `yyyy-MM-dd HH:mm:ss`
- 字典类字段（如 `industry`）存 code 不存中文，映射表以小程序 `src/constants/industry.ts` 为准

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
| 1301 | 查看资料仅限乡会会员（乡会身份未核验/token 失效/非名册手机号） |
| 1302 | 手机号核验失败，请重试（微信换号 code 无效/已消费/微信侧限频） |

新增业务码规则：1xxx 起，按模块分段（成员 10xx、活动 11xx、公告 12xx、
乡会身份 13xx），加码先更新本表（两份 API.md + ResultCode 三处同步）。

## 认证约定

- 请求头 `Authorization: Bearer <token>`（Sa-Token：`token-name: Authorization` +
  `token-prefix: Bearer`，配置在 `application.yml`）
- 一期所有公开查询接口免登录；`/tsa/admin/**` 一律要求登录（二期生效）
- **乡会身份**：`POST /tsa/auth/verify-phone` 核验成功后签发乡会会话令牌，前端放
  `X-Assoc-Token` 请求头访问受限资源（如成员详情）。令牌过期/伪造一律返回 **1301**
  （不是 401 —— 401 会触发前端「清登录态跳我的页」，语义错乱）；
  Sa-Token 会话暂存内存（Redis 二期接入），后端重启后旧令牌失效，
  前端按 1301 引导重新核验即自愈。

## 一期接口清单

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/tsa/health` | 健康检查 |
| GET | `/tsa/members` | 成员分页列表（参数：page,pageSize,province,city,industry,keyword） |
| GET | `/tsa/members/map-data` | 地图打点轻量数据（id,name,avatarUrl,lat,lng,province,city,industry） |
| GET | `/tsa/members/{id}` | 成员详情（**乡会用户专享**：需 X-Assoc-Token，未核验 1301；contactVisible=false 时剔除 phone/wechatId 字段） |
| POST | `/tsa/members` | 成员注册（status 置 0 待审核），data 返回新成员 id（字符串） |
| POST | `/tsa/auth/verify-phone` | 乡会身份核验（getPhoneNumber 动态 code 换手机号 → 比对内部名册 association_member；命中返 verified=true+token，未命中返 verified=false 仍是 HTTP 200） |
| GET | `/tsa/notices` | 公告列表（置顶优先；字段含 summary 摘要与 pinned 布尔） |
| GET | `/tsa/notices/{id}` | 公告详情（不存在返回 1002） |
| GET | `/tsa/community/posts` | 社区动态分页列表（参数：page,pageSize,type,cuisine,region,keyword；按发布时间倒序；不下发评论树） |
| GET | `/tsa/community/posts/{id}` | 动态详情（含 commentsList；不存在返回 1002） |
| POST | `/tsa/community/posts` | 发布动态（一期无登录，author 为自由填写昵称；返回新动态 id 字符串） |
| POST | `/tsa/community/posts/{id}/like` | 点赞 +1（返回点赞后总数；动态不存在 1002） |
| POST | `/tsa/community/posts/{id}/comments` | 发表评论（返回新建评论对象；动态不存在 1002） |
| GET | `/tsa/foundation` | 基金会首页聚合（rewards 奖励类别 + donations 捐赠；amount 单位元） |
| GET | `/tsa/foundation/rewards` | 奖励明细（categories 类别名 + records 获奖记录，含 categoryName） |
| GET | `/tsa/foundation/donations` | 捐赠明细（按日期倒序） |
| POST/PUT/DELETE | `/tsa/foundation/categories[/{id}]` | 奖项类别增删改（一期不鉴权，管理后台二期收口到 `/tsa/admin/**`） |
| POST/PUT/DELETE | `/tsa/foundation/records[/{id}]` | 获奖记录增删改 |
| POST/PUT/DELETE | `/tsa/foundation/donations[/{id}]` | 捐赠鸣谢增删改 |

### 分页响应 `data` 结构（`dto/PageVO`，与前端契约一致）

```json
{ "list": [ ... ], "total": 100, "page": 1, "pageSize": 20 }
```

注意不是 MyBatis-Plus IPage 的原始形状（records/current/size/pages）——
Service 层用 `PageVO.of(ipage)` 做一次转换。

### 推迟实现（本轮「登录/身份/地图先不做」，社区与基金会已上线）

- 登录/身份：`/tsa/auth/wechat-login`、`/tsa/user/me`；成员详情 `/tsa/members/{id}` 的
  X-Assoc-Token 身份闸门（接口已实现，联调依赖 verify-phone，暂按推迟处理）
- 地图：`/tsa/members/map-data`、`/tsa/members/stats/province`（供地图区域着色/排行榜）
- 活动：`/tsa/events` 系列**不再做后端**——改由跳转公众号文章承载，小程序 `pages/event/detail`
  为早期废弃原型，后续替换（`activity` 表暂留）

## 新增接口 Checklist（学生模板）

1. `dto/` 定义请求对象 + 校验注解（参考 `MemberSaveRequest`）；出参与实体形状不一致时用 `XxxVO`（参考 `NoticeVO`）
2. `entity/`、`mapper/` 已有一张表则继承 `BaseMapper`
3. `service/` 接口 + `impl/` 实现（业务规则、事务在这里；抛错用 `BusinessException`）
4. `controller/` 接参（`@Valid`）→ 调 service → 包 `Result`（不写逻辑）
5. `@Tag/@Operation` 注解补全文档，启动后在 doc.html 自测
6. 写一个 `MemberControllerTest` 式的 standalone MockMvc 测试
7. 更新本文档接口清单 + 小程序侧 `docs/API.md` + 提交 PR
