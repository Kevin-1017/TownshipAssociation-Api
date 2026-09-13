-- =========================================================
-- 初始管理后台账号种子（2026-09-14 管理后台鉴权补洞）
-- 用途：给 admin_user 表植入第一个超级管理员，供 POST /tsa/auth/admin-login 登录。
-- 幂等：INSERT IGNORE + uk_username，重复执行不会插入第二条、也不会覆盖已改的口令。
-- 口令：BCrypt(cost=10) 哈希，明文口令不在本仓库内（走交付说明/环境变量下发）；
--       上线后请第一时间登录并改密（改密接口本期未做，见 API.md「推迟实现」）。
-- 角色：1 = 超级管理员（本期 role 仅存/仅回显，超管与普通管理员行为无差异）。
-- 执行：以管理员身份对 tsa 库导入本增量（SET NAMES 防 Windows 客户端 GBK 乱码）。
-- =========================================================
SET NAMES utf8mb4;

INSERT IGNORE INTO admin_user (username, password_hash, role)
VALUES ('admin', '$2a$10$h4nntPTJdCSfRm9NNsKjb.IIMENCI4kp4ucxWC6PaimstO7NAe5Le', 1);
