-- =========================================================
-- 社区评论审核制（2026-09-15，随「评论应该过审」决策）
-- 口径与帖子审核制完全一致：新评论落 0 待审，管理端审后仅 status=1 随详情下发；
-- 存量 4 条示例评论视为已过审（DEFAULT 1 即完成回填）。驳回可恢复（status=2 保留记录）。
-- status 语义：0 待审核 / 1 已通过 / 2 已驳回。
-- =========================================================
SET NAMES utf8mb4;

ALTER TABLE community_comment
    ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '审核状态：0待审核/1已通过/2已驳回' AFTER likes,
    ADD KEY idx_status_create (status, create_time);
