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
| 1002 | 数据不存在 |
| 1303 | 微信登录失败，请重试（jscode2session 的 code 无效/已消费/被风控/凭据未配/上游异常统一此码；真实微信 errcode 只进服务端日志不外泄） |
| 1306 | 操作过于频繁，请稍后再试（`/tsa/auth/**` 按 IP 限频超限；刻意不并入 401——限频是「稍后再试」不是「登录失效」，前端据此只 toast 不清登录态） |
| 1307 | 管理后台登录失败（用户名或密码错误）：账号不存在 / 密码错误 / 已删除统一此码，message 不带区分线索（防账号枚举） |

> 1301/1302/1304/1305 为预留号段（1301/1302 已随乡会身份核验链下线腾出），勿复用。

新增业务码规则：1xxx 起，按模块分段（成员 10xx、活动 11xx、公告 12xx、
鉴权/限频 13xx），加码先更新本表（两份 API.md + ResultCode 三处同步）。

## 认证约定

- 请求头 `Authorization: Bearer <token>`（Sa-Token：`token-name: Authorization` +
  `token-prefix: Bearer`，配置在 `application.yml`）
- 一期公开查询接口免登录；`/tsa/admin/**` 需 **admin 角色会话**（v1.3 起由 `checkLogin` 升级为 `checkRole("admin")`，
  管理后台登录接口已上线，见下）
- **微信登录态（一期已上线）**：`POST /tsa/auth/wechat-login` 用 wx.login 的 code 换 openid 并签发
  Bearer 令牌（loginId=裸 openid）。受保护面：
  - `/tsa/user/**`（`GET /me`、`PUT /profile`）需 Bearer 登录态；
  - `POST /tsa/files`（上传）**需 Bearer** ——
    SaRouter 按 HTTP 方法圈定，同路径的 GET 读接口（图片回读）维持公开。
- **401 语义 = 微信会话失效**（token 7 天过期 / 会话存 JVM 内存后端重启即全员失效）：
  前端的职责是**静默重登一次并重放原请求**（自愈），失败才清态跳「我的」页；
  别把 401 当成需要用户看懂的错误。
- **限频（1306）**：`/tsa/auth/wechat-login` 10 次/分/IP、`/tsa/auth/admin-login` 5 次/分/IP，
  固定 60s 窗口（`AuthRateLimitInterceptor`），两端点各自独立计数。**内存实现 = 单实例假设**：
  多实例部署各数各的（实际阈值 ≈ N×配置值），Redis 化随二期。IP 取 `getRemoteAddr()`：
  直连/dev 可信；**套 nginx 反代必须配 `server.forward-headers-strategy`**（application.yml 留了注释开关），
  否则全体用户共享一个桶（10/分 ≈ 全球限流）；且仅在确认上游代理受信后启用（X-Forwarded-For 可伪造=可绕过）。
  配置键 `tsa.rate-limit.login-per-minute` / `tsa.rate-limit.admin-login-per-minute`。
- **mock 逃生通道**：`tsa.wechat.login-mock-mode=true` 时 wechat-login
  跳过 jscode2session、openid 恒为 `mock-openid-local`（**严禁由 code 派生**，开发期所有 mock 登录共用同一建档属已知后果）。
  非 dev/test 环境开关为 true 直接拒绝启动（`WechatClientImpl` 守卫）。
  **该守卫以部署环境显式激活非 dev/test profile 为前提**：application.yml 写死 `spring.profiles.active: dev`
  （本机开发默认），部署侧漏配 profile = dev 裸跑，守卫形同虚设——Dockerfile 已烧入 `ENV SPRING_PROFILES_ACTIVE=prod`；
  **绕开镜像直接 `java -jar` 上线时必须自行注入 `SPRING_PROFILES_ACTIVE=prod`**。
  prod 在仓库中刻意无 application-prod.yml（凭据不入库），数据源全靠 `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` 环境变量注入。
- **管理后台登录（已上线）**：`POST /tsa/auth/admin-login`（公开、限频 5/分）用账号口令换 admin 会话令牌
  （`loginId = "admin-" + admin_user.id`，连字符前缀不触发 Sa-Token 的 loginId 冒号禁令），
  成功 `data={token,username,role}`，失败一律 **1307**（账号不存在/密码错/已删除同一码，防枚举）。
  令牌同样放 `Authorization: Bearer`。**冒充闭环**：`/tsa/admin/**` 走 `checkRole("admin")`，角色由
  `StpInterfaceImpl` 仅对 `admin-` 前缀会话发放 —— openid 裸登录态虽过 `checkLogin` 但无 "admin" 角色，
  访问 admin 面一律 **403**（未登录 401）。`GET /tsa/admin/auth/me` 从会话回显 `{username,role}`（role：1 超管/2 普通，本期仅回显）。
  `POST /tsa/auth/admin-logout` 幂等注销当前 admin 会话。初始账号见 `sql/migrate-2026-09-14-admin-login.sql`。

## 一期接口清单

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/tsa/health` | 健康检查 |
| GET | `/tsa/members` | 成员分页列表（参数：page,pageSize,province,city,industry,keyword） |
| GET | `/tsa/members/map-data` | 地图打点轻量数据（id,name,avatarUrl,lat,lng,province,city,industry） |
| GET | `/tsa/members/stats/province` | 省份分布统计（公开；仅 status=1 且未删除，`[{province,count}]` 按 count 降序；首页/我的页「乡友分布」数据源） |
| POST | `/tsa/auth/wechat-login` | 微信静默登录（body `{"code":…}` → `data={token,user}`；user=null=已登录未建档，**键必须在场**；400 空 code / 1303 微信侧失败 / 1306 超频；登录成功旁路 upsert wechat_user 埋点，失败不阻断） |
| POST | `/tsa/auth/logout` | 注销当前 Bearer 会话（无守卫、无 token 幂等 200 data=null；同 openid 其他端旧 token 滞留本期可接受） |
| GET | `/tsa/user/me` | 本人档案（**需 Bearer**；data=MemberDetailVO｜null（null=未建档，前端走「点击完善资料」引导）；本人视角**不裁** phone/wechatId；未登录 → 401） |
| PUT | `/tsa/user/profile` | 保存本人资料（**需 Bearer**；全字段可选、只更白名单，status/province/city/openid/source 结构上不可改；无档案首次提交即建档 status=0 待审 + source=1；返回保存后最新档案；401 同上） |
| POST | `/tsa/files` | 上传图片（**需 Bearer**；multipart 字段名 `file`，≤2MB、类型限 jpg/png/webp 按 content-type 判定；data=`{path:"/tsa/files/<uuid>.<ext>"}` 相对路径、不落库；缺字段/空/超限/类型不符一律 400「文件超限或类型不支持」） |
| GET | `/tsa/files/{name}` | 读取图片（公开——`<image>` 组件发不了带令牌请求；name 须为上传返回的 uuid 文件名，非法名/文件不存在一律 code=404 壳；按扩展名给 Content-Type） |
| GET | `/tsa/events` | 活动分页列表（公开；query：page 默认 1、pageSize 默认 10 钳 1..50、year 可选 4 位数字按 start_time 年份、yearFrom/yearTo 可选年份区间含端点（任一侧可缺省=开区间，与 year 同时给出取交集，yearFrom>yearTo 或越界 400）；start_time 倒序；status 为派生 upcoming/past 不落库；列表项含 **articleUrl**（可为 null，web 端已删详情页、列表卡片直跳公众号）；**无正文**） |
| GET | `/tsa/events/{id}` | 活动详情（公开；含 articleUrl=公众号永久链接可为 null，前端「阅读公众号全文」按钮用它调 wx.openOfficialAccountArticle；查无 1002；**无 content 字段**——正文留在公众号） |
| GET | `/tsa/notices` | 公告列表（置顶优先；字段含 summary 摘要与 pinned 布尔） |
| GET | `/tsa/notices/{id}` | 公告详情（不存在返回 1002） |
| GET | `/tsa/community/posts` | ⚠️已下线 社区动态分页列表（参数：page,pageSize,type,cuisine,region,keyword；按发布时间倒序；不下发评论树；**2026-09-14 审核制：只下发已过审 status=1**） |
| GET | `/tsa/community/posts/{id}` | ⚠️已下线 动态详情（含 commentsList；不存在**或未过审**返回 1002） |
| POST | `/tsa/community/posts` | ⚠️已下线 发布动态（author 为自由填写昵称；**落库一律待审 status=0，管理端审核通过才进列表**；返回新动态 id 字符串；images 最多 1 张；**IP 限频 5 次/时**，超限 1306） |
| POST | `/tsa/community/uploads` | 发布配图上传（**公开端点**（web 无登录态），multipart 字段名 `file`；≤2MB、类型限 jpg/png/webp，校验与 `/tsa/files` 同源 UploadRules；**IP 限频 5 次/时**，超限 1306；data=`{path}` 同 `/tsa/files` 形状，随发布请求放进 images） |
| POST | `/tsa/community/posts/{id}/like` | ⚠️已下线 点赞 +1（返回点赞后总数；动态不存在**或未过审** 1002；**IP 限频 5 次/时·跨帖子共享同桶**） |
| POST | `/tsa/community/posts/{id}/comments` | ⚠️已下线 发表评论（**2026-09-15 落库一律待审 status=0，审核通过后才随详情 commentsList 下发、才计入 comments 条数**；返回新评论；动态不存在**或未过审** 1002；**IP 限频 5 次/时·跨帖子共享同桶**） |
| GET | `/tsa/foundation` | 基金会首页聚合（rewards 奖励类别 + donations 捐赠；amount 单位元） |
| GET | `/tsa/foundation/rewards` | 奖励明细（categories 类别名 + records 获奖记录，含 categoryName） |
| GET | `/tsa/foundation/donations` | 捐赠明细（按日期倒序） |
| POST | `/tsa/auth/admin-login` | 管理后台登录（公开、**限频 5/分/IP**；body `{username,password}` → `data={token,username,role}`；账号不存在/密码错/已删除统一 **1307**；空字段 400；超频 1306） |
| POST | `/tsa/auth/admin-logout` | 注销当前 admin 会话（无守卫、非 admin/无 token 幂等 200 data=null） |
| GET | `/tsa/admin/auth/me` | 当前登录管理员（**需 admin 角色**；`data={username,role}`，role 1超管/2普通；未登录 401、非 admin 403） |
| GET | `/tsa/admin/notices` | 公告全量列表（**需 admin 角色**；置顶优先、发布时间倒序，不分页） |
| POST | `/tsa/admin/notices` | 新增公告（**需 admin 角色**；body `NoticeSaveRequest{title,summary?,content,pinned?,publishedAt?}`；返回新 id 字符串；标题/正文空 400） |
| PUT | `/tsa/admin/notices/{id}` | 修改公告（**需 admin 角色**；id 不存在 1002） |
| DELETE | `/tsa/admin/notices/{id}` | 删除公告（**需 admin 角色**；逻辑删除；id 不存在 1002） |
| POST/PUT/DELETE | `/tsa/admin/foundation/categories[/{id}]` | 奖项类别增删改（**v1.3 从 `/tsa/foundation/**` 迁入并收口到 admin 墙，需 admin 角色**；返回形状不变：POST 回字符串 id、PUT/DELETE 回 Void；Service 抛不存在回 1002） |
| POST/PUT/DELETE | `/tsa/admin/foundation/records[/{id}]` | 获奖记录增删改（同上迁移说明） |
| POST/PUT/DELETE | `/tsa/admin/foundation/donations[/{id}]` | 捐赠鸣谢增删改（同上迁移说明） |
| GET | `/tsa/admin/community/posts` | 动态分页列表·管理端（**需 admin 角色**；2026-09-14 审核制；query：page/pageSize 钳 1..100、status 可选 0 待审/1 已过/2 已驳（缺省全部）、type 可选 food/campus；发布时间倒序；VO 含 status 与评论数） |
| PUT | `/tsa/admin/community/posts/{id}/audit` | 审核动态（**需 admin 角色**；body `{status: 1通过|2驳回}`（越界 400）；驳回可恢复：status=2 的记录可再审回 1；id 不存在 1002；成功回 Void） |
| GET | `/tsa/admin/community/comments` | 评论分页列表·管理端（**需 admin 角色**；2026-09-15 评论审核制；query：page/pageSize 钳 1..100、status 可选 0/1/2（缺省全部）、postId 可选只看某动态下；评论时间倒序；CommentVO 含 postId/status） |
| PUT | `/tsa/admin/community/comments/{id}/audit` | 审核评论（**需 admin 角色**；body `{status: 1通过|2驳回}` 同动态审核口径；通过后随详情下发并计入条数；id 不存在 1002） |
| GET | `/tsa/admin/events` | 事件分页列表·管理端（**需 admin 角色**；query：page/pageSize 钳 1..50、keyword 标题模糊；start_time 倒序；出参复用公开列表同款 VO） |
| POST | `/tsa/admin/events` | 新建事件（**需 admin 角色**；EventSaveRequest{title≤64 必填, cover?, summary?, articleUrl?, startTime 必填带时区 ISO}；创建即公开；返回字符串 id） |
| PUT | `/tsa/admin/events/{id}` | 修改事件（**需 admin 角色**；null=不改、cover/summary/articleUrl 传空串=清除；id 不存在 1002） |
| DELETE | `/tsa/admin/events/{id}` | 删除事件=下架（**需 admin 角色**；逻辑删除，公开列表/详情立即查无；id 不存在 1002） |
| POST | `/tsa/admin/events/cover` | 事件封面上传（**需 admin 角色**；multipart 字段名 file，校验与 `/tsa/files` 同源 UploadRules ≤2MB/jpg·png·webp；不加 IP 限频（角色墙已收口）；回 `{path}` 随保存请求放进 cover） |

### 分页响应 `data` 结构（`dto/PageVO`，与前端契约一致）

```json
{ "list": [ ... ], "total": 100, "page": 1, "pageSize": 20 }
```

注意不是 MyBatis-Plus IPage 的原始形状（records/current/size/pages）——
Service 层用 `PageVO.of(ipage)` 做一次转换。

### 客户端功能状态（社区模块在小程序下线）

个人主体小程序不可提供「用户发布且他人可浏览」的 UGC 功能（运营规范 5.7.1 主体未开放类目），
小程序端已将社区功能代码整体删除（api/页面），上表 ⚠️已下线 的接口不再被小程序调用，
但**官网（TownshipAssociation-Web）仍在使用社区浏览/发布/管理与 `/tsa/notices` 等接口**。
**服务端刻意保留接口与数据不删**：主体变更（个人→非个人主体）并报备【社交-社区/论坛】类目后，
按原契约重建小程序前端即回归（契约与回归说明见小程序仓库 docs/API.md「社区动态」一节）。

### 推迟实现（口径）

登录/身份主干与统计、活动只读**均已上线**（见上表）。本节只列**真推迟项**：

- 活动报名/签到（`activity_registration` 表继续闲置，代码占位已删，二期按 docs/DATABASE.md 重建）
  与富文本正文进小程序（正文由公众号文章承载）
- Redis 会话与限频存储：均维持 JVM 内存（**单实例假设**）——后端重启微信登录态全员失效，由前端
  401 静默重登自愈吸收；多实例部署前须 Redis 化
- community 写接口：小程序端随社区功能下线（见上），接口与数据保留供官网使用，未鉴权现状不变
- 管理后台（已落地）：登录/注销在 `/tsa/auth/admin-*`，只读会话在 `/tsa/admin/auth/me`，
  基金会写接口收口在 `/tsa/admin/foundation/**`、公告管理端 CRUD 在 `/tsa/admin/notices`。
  loginId 用 **`admin-` 连字符前缀**（避开 Sa-Token 对 loginId 含冒号的默认禁令，
  二期 Redis 化 key 解析亦无隐患）

## 新增接口 Checklist

1. `dto/` 定义请求对象 + 校验注解（参考 `ProfileUpdateRequest`）；出参与实体形状不一致时用 `XxxVO`（参考 `NoticeVO`）
2. `entity/`、`mapper/` 已有一张表则继承 `BaseMapper`
3. `service/` 接口 + `impl/` 实现（业务规则、事务在这里；抛错用 `BusinessException`）
4. `controller/` 接参（`@Valid`）→ 调 service → 包 `Result`（不写逻辑）
5. `@Tag/@Operation` 注解补全文档，启动后在 doc.html 自测
6. 写一个 `MemberControllerTest` 式的 standalone MockMvc 测试
7. 更新本文档接口清单 + 小程序侧 `docs/API.md` + 提交 PR
