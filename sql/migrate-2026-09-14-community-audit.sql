-- =========================================================
-- 社区动态审核制（2026-09-14）
-- 背景：官网社区发布改为「提交→后台审核→通过才上列表」。
-- 口径（用户拍板）：存量帖子一律视为已通过（DEFAULT 1 即完成回填），
-- 驳回可恢复（status=2 保留记录，可再审回 1）。
-- status 语义：0 待审核 / 1 已通过 / 2 已驳回。
-- =========================================================
SET NAMES utf8mb4;

ALTER TABLE community_post
    ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '审核状态：0待审核/1已通过/2已驳回' AFTER likes,
    ADD KEY idx_status_publish (status, publish_time);
