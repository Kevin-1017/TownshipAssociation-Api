-- ============================================================
-- 增量迁移：社区动态 + 基金会（本地已建库执行；新库直接跑 schema.sql 全文即可，无需本文件）
-- 用法（Windows）：
--   & 'D:\MySQL\MySQL Server 8.0\bin\mysql.exe' -uroot -proot < sql/migrate-2026-09-community-foundation.sql
-- ============================================================
SET NAMES utf8mb4;
USE tsa;

CREATE TABLE IF NOT EXISTS community_post (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    type         VARCHAR(16)     NOT NULL COMMENT '动态类型：food 美食 / campus 校园',
    author       VARCHAR(32)     NOT NULL COMMENT '发布者昵称（一期无登录，自由填写）',
    avatar       VARCHAR(255)    NULL COMMENT '头像 URL',
    title        VARCHAR(64)     NOT NULL COMMENT '标题',
    content      VARCHAR(1000)   NOT NULL COMMENT '正文',
    images       JSON            NULL COMMENT '图片 URL 数组（JSON）',
    cuisine      VARCHAR(32)     NULL COMMENT '菜系（仅美食动态）',
    region       VARCHAR(32)     NULL COMMENT '所在地区（仅美食动态）：longdong/daxuecheng 或自定义',
    likes        INT             NOT NULL DEFAULT 0 COMMENT '点赞数',
    publish_time DATETIME        NOT NULL COMMENT '发布时间',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_type_publish (type, publish_time),
    KEY idx_cuisine (cuisine),
    KEY idx_region (region)
) ENGINE = InnoDB COMMENT '社区动态（美食基地/校园广场）';

CREATE TABLE IF NOT EXISTS community_comment (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    post_id     BIGINT UNSIGNED NOT NULL COMMENT '所属动态 id',
    author      VARCHAR(32)     NOT NULL COMMENT '评论者昵称',
    avatar      VARCHAR(255)    NULL COMMENT '头像 URL',
    content     VARCHAR(500)    NOT NULL COMMENT '评论内容',
    likes       INT             NOT NULL DEFAULT 0 COMMENT '评论点赞数',
    create_time DATETIME        NOT NULL COMMENT '评论时间',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_post (post_id)
) ENGINE = InnoDB COMMENT '社区动态评论';

CREATE TABLE IF NOT EXISTS foundation_reward_category (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    name       VARCHAR(64)     NOT NULL COMMENT '奖项类别名（如「年度奖学金颁发」）',
    sponsor    VARCHAR(64)     NULL COMMENT '赞助人/捐赠方',
    amount     BIGINT          NULL COMMENT '类别奖金总额（元）',
    sort       INT             NOT NULL DEFAULT 0 COMMENT '展示顺序，越小越前',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted    TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_sort (sort)
) ENGINE = InnoDB COMMENT '基金会奖项类别';

CREATE TABLE IF NOT EXISTS foundation_reward_record (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    category_id BIGINT UNSIGNED NOT NULL COMMENT '所属奖项类别 id',
    recipient   VARCHAR(32)     NOT NULL COMMENT '获奖人姓名',
    amount      BIGINT          NULL COMMENT '奖金金额（元）',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_category (category_id)
) ENGINE = InnoDB COMMENT '基金会获奖记录';

CREATE TABLE IF NOT EXISTS foundation_donation (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    donor_name    VARCHAR(64)     NOT NULL COMMENT '捐赠人姓名',
    amount        BIGINT          NOT NULL COMMENT '捐赠金额（元）',
    donation_date DATETIME        NOT NULL COMMENT '捐赠日期',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_donation_date (donation_date)
) ENGINE = InnoDB COMMENT '基金会捐赠鸣谢';

-- 种子数据（仅在空表时灌入，避免重复执行本迁移造成翻倍）----------------
INSERT INTO community_post (type, author, title, content, images, cuisine, region, likes, publish_time)
SELECT * FROM (
    SELECT 'food' AS a,'陈阿姨' AS b,'潮汕牛肉丸哪家最正宗?求推荐' AS c,'周末想带外地朋友去尝尝地道的潮汕牛肉丸,目前看了几家,拿不定主意。大家有没有私藏好店?' AS d, JSON_ARRAY() AS e,'潮汕菜' AS f,'longdong' AS g,128 AS h,'2026-09-08 10:30:00' AS i
    UNION ALL SELECT 'food','小林','家乡粿条的做法分享','老妈远程教学的粿条做法,终于还原出七成味道。把步骤整理了一下,分享给想家的老乡。',JSON_ARRAY(),'潮汕菜','daxuecheng',85,'2026-09-07 18:20:00'
    UNION ALL SELECT 'campus','老郑','龙洞校区篮球赛组队','周六下午有三对三,还差两人,想来的老乡评论区扣一下。',JSON_ARRAY(),NULL,NULL,40,'2026-09-06 15:00:00'
    UNION ALL SELECT 'campus','敏姐','毕业十周年同学会预告','暂定下月在上游举办,详情后续公告,欢迎潮阳潮南的老乡参加。',JSON_ARRAY(),NULL,NULL,66,'2026-09-05 09:10:00'
) t
WHERE NOT EXISTS (SELECT 1 FROM community_post);

INSERT INTO community_comment (post_id, author, content, likes, create_time)
SELECT id, '老郑', '龙洞那家潮香居的牛肉丸绝了!手打的真材实料。', 5, '2026-09-08 11:00:00'
FROM community_post WHERE title = '潮汕牛肉丸哪家最正宗?求推荐' AND deleted = 0;
INSERT INTO community_comment (post_id, author, content, likes, create_time)
SELECT id, '小林', '大学城后门有家开了十几年的老店,老板是汕头人,味道很正', 2, '2026-09-08 14:20:00'
FROM community_post WHERE title = '潮汕牛肉丸哪家最正宗?求推荐' AND deleted = 0;
INSERT INTO community_comment (post_id, author, content, likes, create_time)
SELECT id, '敏姐', '这个做法我试了,米浆比例很关键,学到了!', 3, '2026-09-07 20:00:00'
FROM community_post WHERE title = '家乡粿条的做法分享' AND deleted = 0;

INSERT INTO foundation_reward_category (name, sponsor, amount, sort)
SELECT * FROM (
    SELECT '年度奖学金颁发' AS a,'陈某某' AS b,50000 AS c,1 AS d
    UNION ALL SELECT '优秀负责人表彰','林某某',30000,2
    UNION ALL SELECT '校园活动支持','黄某某',10000,3
) t
WHERE NOT EXISTS (SELECT 1 FROM foundation_reward_category);

INSERT INTO foundation_reward_record (category_id, recipient, amount)
SELECT id, '李思远', 8000 FROM foundation_reward_category WHERE name = '年度奖学金颁发';
INSERT INTO foundation_reward_record (category_id, recipient, amount)
SELECT id, '王梓涵', 8000 FROM foundation_reward_category WHERE name = '年度奖学金颁发';
INSERT INTO foundation_reward_record (category_id, recipient, amount)
SELECT id, '张明浩', 6000 FROM foundation_reward_category WHERE name = '优秀负责人表彰';
INSERT INTO foundation_reward_record (category_id, recipient, amount)
SELECT id, '刘雨欣', 5000 FROM foundation_reward_category WHERE name = '校园活动支持';

INSERT INTO foundation_donation (donor_name, amount, donation_date)
SELECT * FROM (
    SELECT '坤坤' AS a,2000000 AS b,'2026-08-10 00:00:00' AS c
    UNION ALL SELECT '王宇彬',500000,'2026-08-05 00:00:00'
    UNION ALL SELECT '黄某某',5000,'2026-07-15 00:00:00'
    UNION ALL SELECT '蔡某某',3000,'2026-08-01 00:00:00'
    UNION ALL SELECT '林某某',10000,'2026-08-10 00:00:00'
) t
WHERE NOT EXISTS (SELECT 1 FROM foundation_donation);
