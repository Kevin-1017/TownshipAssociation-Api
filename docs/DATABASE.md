# 数据库设计说明（DATABASE）

库名 `tsa`，字符集 `utf8mb4`，引擎 InnoDB。建表脚本见 [`../sql/schema.sql`](../sql/schema.sql)。

## 表关系（ER 概览）

```
member 1 ──── N activity_registration N ──── 1 activity
announcement.publisher_id ────▶ admin_user.id（弱关联，二期启用）
association_member.member_ref_id ────▶ member.id（弱关联，二期回填）
wechat_user ──────（独立埋点表，与以上所有表无外键/弱关联，仅逻辑上同用 openid）
```

除 `wechat_user` 外所有表共有字段：`id`（自增主键）、`created_at` / `updated_at`（代码自动填充）、
`deleted`（逻辑删除 0/1）。wechat_user 刻意不带三件套（埋点只进不删，见其小节）。

## 表清单

### member 成员
| 字段 | 类型 | 说明 |
|------|------|------|
| openid | varchar(64) 唯一 | 微信登录凭证，业务主键 |
| name | varchar(32) | 姓名 |
| avatar_url | varchar(255) | 头像 |
| gender | tinyint | 0未知/1男/2女 |
| graduation_year | smallint | 届别 |
| industry | varchar(32) | 行业（列表筛选维度）。**存字典 code 不存中文**（如 `internet`），映射表以小程序 `src/constants/industry.ts` 为准 |
| province / city | varchar | 地区（地图与筛选维度）。**可空**（v1.2 D3 放宽）：本人先建档、地区留 NULL 待秘书处审核时补录 |
| lat / lng | decimal(10,6) | 坐标，地图打点用；精度 6 位≈0.1m |
| phone | varchar(16) | 仅服务端存储，接口经 `@JsonIgnore` 不返回 |
| district / company / title | varchar | 区县 / 工作单位 / 职务（详情接口字段） |
| school / major | varchar | 毕业院校 / 专业（详情接口字段） |
| wechat_id | varchar(32) | 微信号（敏感，同 phone 仅服务端存储） |
| contact_visible | tinyint | **联系方式可见性 0否/1是**：详情接口据此**剔除** phone/wechatId 字段（剔除而非置 null） |
| status | tinyint | **0待审核/1通过/2拒绝**——所有对外查询强制 `status=1`；`PUT /tsa/user/profile` 首次建档即置 0（待秘书处审核放行） |
| source | tinyint | **来源：0 后台种子 / 1 本人提交**（v1.2 D3；`PUT /tsa/user/profile` 建档时置 1，本人接口结构上不可回改） |
| intro | varchar(200) | 简介 |

索引：`uk_openid`、`idx_city`、`idx_industry`、`idx_status_deleted`。

### announcement 公告
title / summary（列表摘要）/ content / publisher_id / is_top（置顶排序键，出参转 `pinned` 布尔）/ published_at。

### activity 活动（v1.2 D2：只读接口已上线 `GET /tsa/events` / `/tsa/events/{id}`，报名仍二期）
标题、**summary（一句话简介，小程序列表卡片用）**、详情、**article_url（公众号文章永久链接 /s/xxx，
正文不进小程序，前端点击调 wx.openOfficialAccountArticle）**、封面（cover_url）、
地点（描述+经纬度）、时间三组（开始/结束/报名截止）、capacity、status。
注意：**对外契约的 status（upcoming/past）由 start_time 与当前时间派生，不落库**——库里那套
0/1/2/3 流转状态本期不过滤（秘书处 SQL 直插即视为已发布），二期管理系统上下架时收口。

### activity_registration 活动报名（二期）
`uk_activity_member(activity_id, member_id)` 保证**一人一活动只能有一条报名**；status：0报名/1签到/2取消。

### admin_user 管理员
username 唯一，password_hash 存 BCrypt(cost=10) 哈希（绝不明文入库/回前端），role：1超管/2普通。
v1.3 起 `POST /tsa/auth/admin-login` 消费本表（loginId=`admin-<id>`，连字符前缀与 `assoc-` 惯例一致）；
账号不存在/密码错/软删除统一回 1307（防枚举）。
**初始种子账号**见 [`../sql/migrate-2026-09-14-admin-login.sql`](../sql/migrate-2026-09-14-admin-login.sql)
（`INSERT IGNORE` 幂等植入超级管理员 `admin`；明文口令走交付说明下发、不入库，上线后立即改密）。

### association_member 乡会用户（内部名册）
**内部手动维护，不对外提供 CRUD 接口** —— 数据由乡会秘书处手动 SQL 增删改。
乡会身份的唯一区分依据是 `phone`（uk_phone 唯一）：`verify-phone` 核验时比对，
命中才算乡会用户，才有资格访问成员详情（1301 闸门）。
字段：phone / name / role（会长/理事/会员等）/ remark（仅内部可见）/
member_ref_id（二期映射 member.id，实现登录态自动识别乡会身份，一期恒 NULL）。

### wechat_user 小程序登录用户埋点（v1.2 D1「来过」层）
| 字段 | 类型 | 说明 |
|------|------|------|
| openid | varchar(64) 唯一 | 微信下发的小程序内用户唯一 ID（uk_openid） |
| unionid | varchar(64) NULL | 开放平台同账号下跨应用识别预留（本期无公众号，恒 NULL） |
| first_login_at | datetime | 首次登录时间（**新增用户**统计口径；upsert 的 UPDATE 分支故意不碰它） |
| last_login_at | datetime | 最近登录时间（**活跃用户**统计口径） |
| login_count | int | 累计登录次数（每次登录 +1） |

`POST /tsa/auth/wechat-login` 成功后旁路写入
（`INSERT ... ON DUPLICATE KEY UPDATE last_login_at=NOW(), login_count=login_count+1`），
失败仅 log.warn、**不阻断登录**。三层身份各归各位：wechat_user（来过）→ member（登记为乡贤）
→ association_member（乡会身份核验），本表与后两张**彻底分离**，不做任何授权依据。
统计口径：总用户=COUNT(*)、新增=first_login_at 区间、活跃=last_login_at 区间。
**刻意不用 deleted/created_at/updated_at 三件套**（埋点只进不删，逻辑删除对统计口径毫无意义），
`WechatUser` 实体因此是全项目唯一不继承 BaseEntity 的实体——继承会让 @TableLogic 在查询上
拼 `deleted=0` 直接 SQL 报错，属有意破例而非疏漏。

## 社区与基金会（本轮新增）

### community_post 社区动态（美食基地 / 校园广场共用）
type（`food`/`campus`）/ author（一期无登录，自由填写昵称）/ avatar / title / content /
images（**JSON 列**，URL 数组，实体用 `JacksonTypeHandler` + `autoResultMap` 映射 `List<String>`）/
cuisine / region（仅美食动态）/ likes / publish_time。**评论数不落库**，由 `community_comment` 派生，避免计数与子表不一致。

### community_comment 社区评论
post_id / author / avatar / content / likes / create_time。逻辑外键关联 `community_post`（无物理外键）。

### foundation_reward_category 基金会奖项类别
name / sponsor / amount（**单位：元**）/ sort。首页「奖励」卡片与详情页分组的依据。

### foundation_reward_record 获奖记录
category_id / recipient / amount（元）。删除类别时连带软删其下记录（`@Transactional`）。

### foundation_donation 捐赠鸣谢
donor_name / amount（元）/ donation_date。首页与详情页 tab1 共用，按日期倒序。

> 说明：基金会/社区写接口（增删改）系秘书处管理端预留、小程序零调用——保持**仅服务端可达**现状；
> 管理系统接入时统一加鉴权（v1.2 D7 口径，非「无鉴权规划」）。社区前端已下线但接口与数据刻意保留，
> 见 API.md「社区模块下线」节。

## 约定（学生必读）

1. **逻辑删除**：禁止物理 DELETE，删除即 `UPDATE ... SET deleted=1`（MyBatis-Plus `@TableLogic` 自动处理）。
2. **改表流程**：先改 `sql/schema.sql` → 在开发库执行 → 同步更新本文档 → PR 说明变更原因。
3. 金额类字段禁用 float/double，用 `DECIMAL`；坐标已示范。
4. 时间统一 `DATETIME` + 应用层 `LocalDateTime`，时区 Asia/Shanghai。
