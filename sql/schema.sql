-- ============================================================
-- 乡会项目数据库初始化脚本
-- 执行方式（任选其一）：
--   1) 命令行：  mysql -uroot -p < sql/schema.sql
--   2) IDEA 的 Database 工具：右键连接 -> Run SQL Script
-- MySQL 版本要求：8.0+（本机为 8.0.36）
-- ============================================================

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

-- ============================================================
-- 演示种子数据：12 位带真实城市坐标的成员（地图接口一跑就有数据）
-- 正式数据接入微信登录后由用户注册产生，这些种子仅用于开发/演示
-- ============================================================
INSERT INTO member (openid, name, gender, graduation_year, industry, province, city, lat, lng, status, intro)
VALUES
    ('demo_openid_01', '张三',   1, 2015, '互联网',  '四川省', '成都市', 30.572800, 104.066800, 1, '在成都做后端，欢迎老乡交流'),
    ('demo_openid_02', '李四',   1, 2016, '金融',    '北京市', '北京市', 39.904200, 116.407400, 1, '金融街搬砖人'),
    ('demo_openid_03', '王五',   2, 2018, '教育',    '广东省', '深圳市', 22.543100, 114.057900, 1, '在深中做老师'),
    ('demo_openid_04', '赵六',   1, 2014, '制造',    '上海市', '上海市', 31.230400, 121.473700, 1, '汽车产业链'),
    ('demo_openid_05', '钱七',   2, 2019, '互联网',  '浙江省', '杭州市', 30.274100, 120.155100, 1, '电商运营'),
    ('demo_openid_06', '孙八',   1, 2017, '医疗',    '湖北省', '武汉市', 30.593100, 114.305400, 1, '协和住院医'),
    ('demo_openid_07', '周九',   2, 2020, '法律',    '广东省', '广州市', 23.129100, 113.264400, 1, '执业律师'),
    ('demo_openid_08', '吴十',   1, 2013, '建筑',    '重庆市', '重庆市', 29.563000, 106.551600, 1, '结构设计'),
    ('demo_openid_09', '郑一',   2, 2021, '媒体',    '江苏省', '南京市', 32.060300, 118.796900, 1, '电视台编导'),
    ('demo_openid_10', '王二',   1, 2012, '互联网',  '陕西省', '西安市', 34.341600, 108.939800, 1, '芯片验证'),
    ('demo_openid_11', '李三',   2, 2022, '自由职业', '四川省', '宜宾市', 28.751300, 104.641700, 0, '待审核示例账号'),
    ('demo_openid_12', '张四',   1, 2019, '公务员',  '四川省', '泸州市', 28.871700, 105.442600, 2, '审核拒绝示例账号');

INSERT INTO announcement (title, content, publisher_id, is_top, published_at)
VALUES
    ('乡会 2026 年度恳亲大会筹备启动', '各位老乡，年度恳亲大会筹备组已成立，欢迎报名志愿者……', NULL, 1, '2026-08-20 10:00:00'),
    ('新成员审核规则说明', '为保护成员隐私，注册资料需管理员审核通过后才会出现在成员列表与地图中。', NULL, 0, '2026-08-10 09:00:00'),
    ('小程序内测招募', '官网小程序进入内测阶段，招募 20 位老乡体验并反馈问题。', NULL, 0, '2026-07-28 15:30:00');

INSERT INTO activity (title, content, location, lat, lng, start_time, status, capacity)
VALUES
    ('成都老乡下午茶（示例）', '第一期活动功能演示数据', '成都市武侯区某茶馆', 30.572800, 104.066800, '2026-10-01 14:00:00', 1, 30);
