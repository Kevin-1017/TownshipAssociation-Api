-- =========================================================
-- CS 道具点位精简（2026-10-16，用户拍板）
-- 背景：kevin/csgo 指南字段瘦身——
--   1) kind 枚举从英文码改中文值（smoke→烟 / flash→闪 / molotov→火 / decoy→诱饵），
--      并新增「雷」（高爆），历史行无雷、无需补数据；
--   2) 删 site（归类点位名）——前端只留点位名称一个名字；
--   3) 删 steps（分步操作）——投掷方式（style）顶替其表单位置，步骤内容整体弃用。
-- 顺序：先洗 kind（列还在旧口径时 UPDATE 最直观），再删列换索引。
-- ⚠️ 非幂等：ALTER 只可执行一次，重复跑会报「列/索引不存在」；
--    kind 的 UPDATE 重跑无害（已无英文值命中）。
-- =========================================================
SET NAMES utf8mb4;

UPDATE csgo_lineup
SET kind = CASE kind
    WHEN 'smoke'   THEN '烟'
    WHEN 'flash'   THEN '闪'
    WHEN 'molotov' THEN '火'
    WHEN 'decoy'   THEN '诱饵'
    ELSE kind
  END
WHERE kind IN ('smoke', 'flash', 'molotov', 'decoy');

ALTER TABLE csgo_lineup
    DROP INDEX idx_map_site,
    ADD INDEX idx_map (map),
    DROP COLUMN site,
    DROP COLUMN steps,
    MODIFY kind VARCHAR(16) NOT NULL COMMENT '道具类型（中文值）：烟/闪/火/雷/诱饵',
    COMMENT 'CS 道具点位（kevin/csgo 指南）；2026-10-16 起无 site/steps';
