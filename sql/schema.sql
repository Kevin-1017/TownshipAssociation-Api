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
    province        VARCHAR(16)     NOT NULL COMMENT '省份',
    city            VARCHAR(16)     NOT NULL COMMENT '城市',
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
-- 3. 活动表（二期开放接口，表先建好）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS activity (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    title           VARCHAR(64)     NOT NULL COMMENT '活动标题',
    cover_url       VARCHAR(255)    NULL COMMENT '封面图',
    content         TEXT            NULL COMMENT '活动详情',
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
