-- ============================================================
-- 乡会项目数据库初始化脚本
-- 执行方式（任选其一）：
--   1) 命令行：  mysql -uroot -p < sql/schema.sql
--   2) IDEA 的 Database 工具：右键连接 -> Run SQL Script
-- MySQL 版本要求：8.0+（本机为 8.0.36）
-- ============================================================

-- 重要：Windows 下 mysql 客户端默认用 GBK 连接，会把本文件（UTF-8）里的中文转码搞乱，
-- 表现为 "Data too long for column" 或注释乱码。此行强制连接字符集为 utf8mb4。
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS tsa
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE tsa;

-- ------------------------------------------------------------
-- 1. 成员表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS member (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    openid          VARCHAR(64)     NOT NULL COMMENT '微信 openid，唯一',
    name            VARCHAR(32)     NOT NULL COMMENT '姓名',
    avatar_url      VARCHAR(255)    NULL COMMENT '头像 URL',
    gender          TINYINT         NOT NULL DEFAULT 0 COMMENT '0未知/1男/2女',
    graduation_year SMALLINT        NULL COMMENT '届别（入学/毕业年份）',
    industry        VARCHAR(32)     NULL COMMENT '所属行业',
    province        VARCHAR(16)     NULL COMMENT '省份（本人自报资料可空，待秘书处审核时补录）',
    city            VARCHAR(16)     NULL COMMENT '城市（本人自报资料可空，待秘书处审核时补录）',
    lat             DECIMAL(10, 6)  NULL COMMENT '纬度',
    lng             DECIMAL(10, 6)  NULL COMMENT '经度',
    phone           VARCHAR(16)     NULL COMMENT '手机号（仅服务端可见）',
    district        VARCHAR(32)     NULL COMMENT '区/县',
    company         VARCHAR(64)     NULL COMMENT '工作单位',
    title           VARCHAR(32)     NULL COMMENT '职务/头衔',
    school          VARCHAR(64)     NULL COMMENT '毕业院校',
    major           VARCHAR(64)     NULL COMMENT '专业',
    wechat_id       VARCHAR(32)     NULL COMMENT '微信号（仅服务端可见）',
    contact_visible TINYINT         NOT NULL DEFAULT 0 COMMENT '联系方式可见 0否/1是',
    status          TINYINT         NOT NULL DEFAULT 0 COMMENT '0待审核/1已通过/2已拒绝',
    source          TINYINT         NOT NULL DEFAULT 0 COMMENT '来源:0后台种子/1本人提交',
    intro           VARCHAR(200)    NULL COMMENT '个人简介',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除 0正常/1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_openid (openid),
    KEY idx_city (city),
    KEY idx_industry (industry),
    KEY idx_status_deleted (status, deleted)
) ENGINE = InnoDB COMMENT '乡会成员';

-- ------------------------------------------------------------
-- 2. 公告表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS announcement (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    title        VARCHAR(64)     NOT NULL COMMENT '标题',
    summary      VARCHAR(200)    NULL COMMENT '列表摘要（正文摘录，列表页展示）',
    content      TEXT            NOT NULL COMMENT '正文',
    publisher_id BIGINT UNSIGNED NULL COMMENT '发布人 admin_user.id',
    is_top       TINYINT         NOT NULL DEFAULT 0 COMMENT '0普通/1置顶',
    published_at DATETIME        NOT NULL COMMENT '发布时间',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_top_published (is_top, published_at)
) ENGINE = InnoDB COMMENT '乡会公告';

-- ------------------------------------------------------------
-- 3. 活动表（v1.2 D2「跳公众号文章」轻量形态：只读两接口读本表；
--    正文不进小程序，article_url 放公众号永久链接，封面/摘要在列表卡片展示）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS activity (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    title           VARCHAR(64)     NOT NULL COMMENT '活动标题',
    cover_url       VARCHAR(255)    NULL COMMENT '封面图',
    summary         VARCHAR(500)    NULL COMMENT '一句话简介（小程序列表卡片用，正文在公众号）',
    content         TEXT            NULL COMMENT '活动详情',
    article_url     VARCHAR(500)    NULL COMMENT '公众号文章永久链接（/s/xxx，点击调 wx.openOfficialAccountArticle）',
    location        VARCHAR(128)    NULL COMMENT '地点描述',
    lat             DECIMAL(10, 6)  NULL,
    lng             DECIMAL(10, 6)  NULL,
    start_time      DATETIME        NOT NULL COMMENT '开始时间',
    end_time        DATETIME        NULL COMMENT '结束时间',
    signup_deadline DATETIME        NULL COMMENT '报名截止',
    capacity        INT             NULL COMMENT '人数上限',
    status          TINYINT         NOT NULL DEFAULT 0 COMMENT '0未开始/1报名中/2进行中/3已结束',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_start_time (start_time)
) ENGINE = InnoDB COMMENT '乡会活动';

-- ------------------------------------------------------------
-- 4. 活动报名表（二期开放接口）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS activity_registration (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    activity_id BIGINT UNSIGNED NOT NULL COMMENT '活动 id',
    member_id   BIGINT UNSIGNED NOT NULL COMMENT '成员 id',
    status      TINYINT         NOT NULL DEFAULT 0 COMMENT '0已报名/1已签到/2已取消',
    signup_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_activity_member (activity_id, member_id) COMMENT '一人一活动只能报名一次',
    KEY idx_member (member_id)
) ENGINE = InnoDB COMMENT '活动报名';

-- ------------------------------------------------------------
-- 5. 管理员表（二期管理后台使用）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS admin_user (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    username      VARCHAR(32)     NOT NULL COMMENT '登录名',
    password_hash VARCHAR(100)    NOT NULL COMMENT 'BCrypt 哈希',
    role          TINYINT         NOT NULL DEFAULT 2 COMMENT '1超管/2普通管理员',
    last_login_at DATETIME        NULL COMMENT '最近登录',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB COMMENT '管理后台账号';

-- ------------------------------------------------------------
-- 6. 乡会用户表（内部手动维护，不对外提供 CRUD 接口）
--    乡会身份的唯一区分依据是手机号：verify-phone 核验时
--    用本表比对,命中才算乡会用户。数据由乡会秘书处手动 SQL 维护。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS association_member (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    phone         VARCHAR(16)     NOT NULL COMMENT '手机号（唯一区分依据）',
    name          VARCHAR(32)     NOT NULL COMMENT '姓名',
    role          VARCHAR(32)     NULL COMMENT '乡会职务（会长/理事/会员等，内部维护）',
    remark        VARCHAR(200)    NULL COMMENT '备注（仅内部可见）',
    member_ref_id BIGINT UNSIGNED NULL COMMENT '二期：映射 member.id，登录态自动识别乡会身份',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除 0正常/1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_phone (phone)
) ENGINE = InnoDB COMMENT '乡会用户（内部手动维护，不对外提供 CRUD）';

-- ============================================================
-- 演示种子数据：12 位带真实城市坐标的成员（地图接口一跑就有数据）
-- 正式数据接入微信登录后由用户注册产生，这些种子仅用于开发/演示
-- ============================================================
INSERT INTO member (openid, name, gender, graduation_year, industry, province, city, lat, lng, status, intro)
VALUES
    ('demo_openid_01', '张三',   1, 2015, 'internet',    '四川省', '成都市', 30.572800, 104.066800, 1, '在成都做后端，欢迎老乡交流'),
    ('demo_openid_02', '李四',   1, 2016, 'finance',     '北京市', '北京市', 39.904200, 116.407400, 1, '金融街搬砖人'),
    ('demo_openid_03', '王五',   2, 2018, 'education',   '广东省', '深圳市', 22.543100, 114.057900, 1, '在深中做老师'),
    ('demo_openid_04', '赵六',   1, 2014, 'manufacture', '上海市', '上海市', 31.230400, 121.473700, 1, '汽车产业链'),
    ('demo_openid_05', '钱七',   2, 2019, 'internet',    '浙江省', '杭州市', 30.274100, 120.155100, 1, '电商运营'),
    ('demo_openid_06', '孙八',   1, 2017, 'medical',     '湖北省', '武汉市', 30.593100, 114.305400, 1, '协和住院医'),
    ('demo_openid_07', '周九',   2, 2020, 'other',       '广东省', '广州市', 23.129100, 113.264400, 1, '执业律师'),
    ('demo_openid_08', '吴十',   1, 2013, 'construction','重庆市', '重庆市', 29.563000, 106.551600, 1, '结构设计'),
    ('demo_openid_09', '郑一',   2, 2021, 'other',       '江苏省', '南京市', 32.060300, 118.796900, 1, '电视台编导'),
    ('demo_openid_10', '王二',   1, 2012, 'internet',    '陕西省', '西安市', 34.341600, 108.939800, 1, '芯片验证'),
    ('demo_openid_11', '李三',   2, 2022, 'other',       '四川省', '宜宾市', 28.751300, 104.641700, 0, '待审核示例账号'),
    ('demo_openid_12', '张四',   1, 2019, 'civil',       '四川省', '泸州市', 28.871700, 105.442600, 2, '审核拒绝示例账号');

INSERT INTO announcement (title, summary, content, publisher_id, is_top, published_at)
VALUES
    ('乡会 2026 年度恳亲大会筹备启动', '筹备组已成立，志愿者报名通道开启，详情关注后续公告。', '各位老乡，年度恳亲大会筹备组已成立，欢迎报名志愿者……', NULL, 1, '2026-08-20 10:00:00'),
    ('新成员审核规则说明', '注册资料需管理员审核通过后，才会显示在成员列表与地图中。', '为保护成员隐私，注册资料需管理员审核通过后才会出现在成员列表与地图中。', NULL, 0, '2026-08-10 09:00:00'),
    ('小程序内测招募', '招募 20 位老乡体验小程序并反馈问题。', '官网小程序进入内测阶段，招募 20 位老乡体验并反馈问题。', NULL, 0, '2026-07-28 15:30:00');

INSERT INTO activity (title, content, location, lat, lng, start_time, status, capacity)
VALUES
    ('成都老乡下午茶（示例）', '第一期活动功能演示数据', '成都市武侯区某茶馆', 30.572800, 104.066800, '2026-10-01 14:00:00', 1, 30);

-- ------------------------------------------------------------
-- 乡会用户种子数据：测试号码（非真实手机号），本地联调用
-- 真实乡会名册由秘书处手动 SQL 维护，见 association_member 表注释
-- ------------------------------------------------------------
INSERT INTO association_member (phone, name, role, remark)
VALUES
    ('13800000001', '张三', '理事', '测试号码：与 member 种子张三同一手机号，演示乡会会员看联系方式'),
    ('13900000001', '测试会员A', '会员', '测试号码（非真实手机号），本地联调用'),
    ('13700000002', '测试会员B', '会员', '测试号码（非真实手机号）');

-- 让种子张三的详情有联系方式可展示（演示隐私开关：contact_visible=1）
UPDATE member
SET contact_visible = 1, wechat_id = 'demo_zhangsan', phone = '13800000001'
WHERE openid = 'demo_openid_01';

-- ============================================================
-- 已建库升级（增量执行。CREATE TABLE IF NOT EXISTS 不会给已有表加列，
-- 已有开发库需手动跑这一段；新库直接执行全文即可）
-- ============================================================
-- ALTER TABLE member ADD COLUMN district VARCHAR(32) NULL COMMENT '区/县';
-- ALTER TABLE member ADD COLUMN company VARCHAR(64) NULL COMMENT '工作单位';
-- ALTER TABLE member ADD COLUMN title VARCHAR(32) NULL COMMENT '职务/头衔';
-- ALTER TABLE member ADD COLUMN school VARCHAR(64) NULL COMMENT '毕业院校';
-- ALTER TABLE member ADD COLUMN major VARCHAR(64) NULL COMMENT '专业';
-- ALTER TABLE member ADD COLUMN wechat_id VARCHAR(32) NULL COMMENT '微信号（仅服务端可见）';
-- ALTER TABLE member ADD COLUMN contact_visible TINYINT NOT NULL DEFAULT 0 COMMENT '联系方式可见 0否/1是';
--
-- v1.2 登录一期增量（D2 活动两列 + D3/C7 来源列与省市值字段）：
-- 新库无需执行（上方 CREATE 段已含）；已有开发库放开逐条执行。
-- province/city 放宽可空是「本人先建档、秘书处审核时补录地区」的前提，
-- 已有行的存量值不受影响（NOT NULL→NULL 是宽松化，安全）。
-- ALTER TABLE activity ADD COLUMN summary VARCHAR(500) NULL COMMENT '一句话简介（小程序列表卡片用，正文在公众号）' AFTER cover_url;
-- ALTER TABLE activity ADD COLUMN article_url VARCHAR(500) NULL COMMENT '公众号文章永久链接（/s/xxx，点击调 wx.openOfficialAccountArticle）' AFTER content;
-- ALTER TABLE member ADD COLUMN source TINYINT NOT NULL DEFAULT 0 COMMENT '来源:0后台种子/1本人提交' AFTER status;
-- ALTER TABLE member MODIFY COLUMN province VARCHAR(16) NULL COMMENT '省份（本人自报资料可空，待秘书处审核时补录）';
-- ALTER TABLE member MODIFY COLUMN city VARCHAR(16) NULL COMMENT '城市（本人自报资料可空，待秘书处审核时补录）';

-- ============================================================
-- 7. 社区动态表（美食基地 / 校园广场；一期无登录，发布者为自由填写昵称）
--    images 用 JSON 列存 URL 数组；评论数不落库，由 community_comment 派生，
--    避免计数与子表长期不一致。
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- 8. 社区动态评论表
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- 9. 基金会奖项类别表（首页「奖励」卡片 + 详情页分组依据）
--    amount 为类别奖金总额，单位：元。
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- 10. 基金会获奖记录表（详情页 tab0：类别下的获奖人）
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- 11. 基金会捐赠鸣谢表（首页 + 详情页 tab1）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS foundation_donation (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    donor_name     VARCHAR(64)     NOT NULL COMMENT '捐赠人姓名',
    amount         BIGINT          NOT NULL COMMENT '捐赠金额（元）',
    amount_visible TINYINT         NOT NULL DEFAULT 0 COMMENT '金额是否对外展示（秘书处口径:默认保密,显式公开的才下发）',
    donation_date  DATETIME        NOT NULL COMMENT '捐赠日期',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_donation_date (donation_date)
) ENGINE = InnoDB COMMENT '基金会捐赠鸣谢';

-- ============================================================
-- 社区 / 基金会演示种子数据（本地联调用；amount 单位为元）
-- ============================================================
INSERT INTO community_post (type, author, title, content, images, cuisine, region, likes, publish_time)
VALUES
    ('food',   '陈阿姨', '潮汕牛肉丸哪家最正宗?求推荐', '周末想带外地朋友去尝尝地道的潮汕牛肉丸,目前看了几家,拿不定主意。大家有没有私藏好店?', JSON_ARRAY(), '潮汕菜', 'longdong',    128, '2026-09-08 10:30:00'),
    ('food',   '小林',   '家乡粿条的做法分享',           '老妈远程教学的粿条做法,终于还原出七成味道。把步骤整理了一下,分享给想家的老乡。',       JSON_ARRAY(), '潮汕菜', 'daxuecheng',   85, '2026-09-07 18:20:00'),
    ('campus', '老郑',   '龙洞校区篮球赛组队',           '周六下午有三对三,还差两人,想来的老乡评论区扣一下。',                                   JSON_ARRAY(), NULL,     NULL,           40, '2026-09-06 15:00:00'),
    ('campus', '敏姐',   '毕业十周年同学会预告',         '暂定下月在上游举办,详情后续公告,欢迎潮阳潮南的老乡参加。',                             JSON_ARRAY(), NULL,     NULL,           66, '2026-09-05 09:10:00');

INSERT INTO community_comment (post_id, author, content, likes, create_time)
SELECT id, '老郑', '龙洞那家潮香居的牛肉丸绝了!手打的真材实料。', 5, '2026-09-08 11:00:00' FROM community_post WHERE title = '潮汕牛肉丸哪家最正宗?求推荐';
INSERT INTO community_comment (post_id, author, content, likes, create_time)
SELECT id, '小林', '大学城后门有家开了十几年的老店,老板是汕头人,味道很正', 2, '2026-09-08 14:20:00' FROM community_post WHERE title = '潮汕牛肉丸哪家最正宗?求推荐';
INSERT INTO community_comment (post_id, author, content, likes, create_time)
SELECT id, '敏姐', '这个做法我试了,米浆比例很关键,学到了!', 3, '2026-09-07 20:00:00' FROM community_post WHERE title = '家乡粿条的做法分享';

INSERT INTO foundation_reward_category (name, sponsor, amount, sort)
VALUES
    ('年度奖学金颁发', '陈某某', 50000, 1),
    ('优秀负责人表彰', '林某某', 30000, 2),
    ('校园活动支持',   '黄某某', 10000, 3);

INSERT INTO foundation_reward_record (category_id, recipient, amount)
SELECT id, '李思远', 8000 FROM foundation_reward_category WHERE name = '年度奖学金颁发';
INSERT INTO foundation_reward_record (category_id, recipient, amount)
SELECT id, '王梓涵', 8000 FROM foundation_reward_category WHERE name = '年度奖学金颁发';
INSERT INTO foundation_reward_record (category_id, recipient, amount)
SELECT id, '张明浩', 6000 FROM foundation_reward_category WHERE name = '优秀负责人表彰';
INSERT INTO foundation_reward_record (category_id, recipient, amount)
SELECT id, '刘雨欣', 5000 FROM foundation_reward_category WHERE name = '校园活动支持';

-- 金额展示口径（2026-09-13 秘书处）：只有坤坤这条公开金额，其余保密（amount 照常入库，仅不下发）
INSERT INTO foundation_donation (donor_name, amount, amount_visible, donation_date)
VALUES
    ('坤坤',     2000000, 1, '2026-08-10 00:00:00'),
    ('王宇彬',   500000,  0, '2026-08-05 00:00:00'),
    ('黄某某',   5000,    0, '2026-07-15 00:00:00'),
    ('蔡某某',   3000,    0, '2026-08-01 00:00:00'),
    ('林某某',   10000,   0, '2026-08-10 00:00:00');

-- ------------------------------------------------------------
-- 12. 小程序登录用户埋点表（计划 v1.2 D1：「来过」层）
--     三层身份各归各位：wechat_user(登录过的访客) → member(乡贤名册)
--     → association_member(乡会核验)。统计口径：总用户=COUNT(*)、
--     新增=first_login_at 区间、活跃=last_login_at 区间。
--     注意：本表刻意不带 deleted 三件套（埋点表只进不删，逻辑删除对
--     统计口径毫无意义），WechatUser 实体因此不继承 BaseEntity —— 继承会
--     让 @TableLogic 在查询上拼 deleted=0 直接报 Unknown column。
--     已有库重跑全文若此行报「table already exists」属正常，跳过即可
--     （此处按计划 D1 原文逐字落库，未套 IF NOT EXISTS）。
-- ------------------------------------------------------------
CREATE TABLE wechat_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  openid VARCHAR(64) NOT NULL,
  unionid VARCHAR(64) NULL,
  first_login_at DATETIME NOT NULL,
  last_login_at DATETIME NOT NULL,
  login_count INT NOT NULL DEFAULT 1,
  UNIQUE KEY uk_openid (openid)
) COMMENT '小程序登录用户埋点(访客),区别于 association_member 名册与 member 乡贤档案';
