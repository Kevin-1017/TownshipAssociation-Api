# 数据库设计说明（DATABASE）

库名 `tsa`，字符集 `utf8mb4`，引擎 InnoDB。建表脚本见 [`../sql/schema.sql`](../sql/schema.sql)。

## 表关系（ER 概览）

```
member 1 ──── N activity_registration N ──── 1 activity
announcement.publisher_id ────▶ admin_user.id（弱关联，二期启用）
```

所有表共有字段：`id`（自增主键）、`created_at` / `updated_at`（代码自动填充）、`deleted`（逻辑删除 0/1）。

## 表清单

### member 成员
| 字段 | 类型 | 说明 |
|------|------|------|
| openid | varchar(64) 唯一 | 微信登录凭证，业务主键 |
| name | varchar(32) | 姓名 |
| avatar_url | varchar(255) | 头像 |
| gender | tinyint | 0未知/1男/2女 |
| graduation_year | smallint | 届别 |
| industry | varchar(32) | 行业（列表筛选维度） |
| province / city | varchar | 地区（地图与筛选维度） |
| lat / lng | decimal(10,6) | 坐标，地图打点用；精度 6 位≈0.1m |
| phone | varchar(16) | 仅服务端存储，接口经 `@JsonIgnore` 不返回 |
| status | tinyint | **0待审核/1通过/2拒绝**——所有对外查询强制 `status=1` |
| intro | varchar(200) | 简介 |

索引：`uk_openid`、`idx_city`、`idx_industry`、`idx_status_deleted`。

### announcement 公告
title / content / publisher_id / is_top（置顶排序键）/ published_at。

### activity 活动（表已建，接口二期开放）
标题、详情、地点（描述+经纬度）、时间三组（开始/结束/报名截止）、capacity、status。

### activity_registration 活动报名（二期）
`uk_activity_member(activity_id, member_id)` 保证**一人一活动只能有一条报名**；status：0报名/1签到/2取消。

### admin_user 管理员（二期）
username 唯一，password_hash 存 BCrypt 哈希（绝不明文），role：1超管/2普通。

## 约定（学生必读）

1. **逻辑删除**：禁止物理 DELETE，删除即 `UPDATE ... SET deleted=1`（MyBatis-Plus `@TableLogic` 自动处理）。
2. **改表流程**：先改 `sql/schema.sql` → 在开发库执行 → 同步更新本文档 → PR 说明变更原因。
3. 金额类字段禁用 float/double，用 `DECIMAL`；坐标已示范。
4. 时间统一 `DATETIME` + 应用层 `LocalDateTime`，时区 Asia/Shanghai。
