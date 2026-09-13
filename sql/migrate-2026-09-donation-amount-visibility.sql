-- ============================================================
-- 迁移：捐赠金额保密口径（2026-09-13 秘书处决定）
-- 除「坤坤」外所有捐赠记录不再对外展示金额；金额仍留库，仅接口不下发。
-- 幂等写法：可安全重复执行。已有库与新建库（schema.sql 已同步）都跑这个即可对齐。
-- ============================================================

ALTER TABLE foundation_donation
    ADD COLUMN amount_visible TINYINT NOT NULL DEFAULT 0
        COMMENT '金额是否对外展示（秘书处口径:默认保密,显式公开的才下发）'
        AFTER amount;

UPDATE foundation_donation
SET amount_visible = 1
WHERE donor_name = '坤坤';
