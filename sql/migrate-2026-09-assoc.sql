-- ============================================================
-- 地图权限分流增量脚本（2026-09，执行后即弃，权威结构以 sql/schema.sql 为准）
--
-- 内容：
--   1. member 表补详情契约字段（7 列）
--   2. 新增 association_member 乡会用户表
--   3. 乡会用户种子数据 + 演示用隐私开关
--
-- 执行方式：
--   mysql -uroot -p --default-character-set=utf8mb4 < sql/migrate-2026-09-assoc.sql
-- ============================================================

SET NAMES utf8mb4;
USE tsa;

-- 1. member 表补列（新库直接执行 schema.sql 全文即可，无需本段）
ALTER TABLE member
    ADD COLUMN district        VARCHAR(32) NULL COMMENT '区/县',
    ADD COLUMN company         VARCHAR(64) NULL COMMENT '工作单位',
    ADD COLUMN title           VARCHAR(32) NULL COMMENT '职务/头衔',
    ADD COLUMN school          VARCHAR(64) NULL COMMENT '毕业院校',
    ADD COLUMN major           VARCHAR(64) NULL COMMENT '专业',
    ADD COLUMN wechat_id       VARCHAR(32) NULL COMMENT '微信号（仅服务端可见）',
    ADD COLUMN contact_visible TINYINT     NOT NULL DEFAULT 0 COMMENT '联系方式可见 0否/1是';

-- 2. 乡会用户表（内部手动维护，不对外提供 CRUD）
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

-- 3. 种子：测试号码（非真实手机号），IGNORE 保证重复执行不报错
INSERT IGNORE INTO association_member (phone, name, role, remark)
VALUES
    ('13800000001', '张三', '理事', '测试号码：与 member 种子张三同一手机号，演示乡会会员看联系方式'),
    ('13900000001', '测试会员A', '会员', '测试号码（非真实手机号），本地联调用'),
    ('13700000002', '测试会员B', '会员', '测试号码（非真实手机号）');

-- 让种子张三的详情有联系方式可展示（演示隐私开关：contact_visible=1）
UPDATE member
SET contact_visible = 1, wechat_id = 'demo_zhangsan', phone = '13800000001'
WHERE openid = 'demo_openid_01';