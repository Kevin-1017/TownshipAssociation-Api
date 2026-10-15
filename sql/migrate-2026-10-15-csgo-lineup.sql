-- =========================================================
-- CS 道具点位可配置化（2026-10-15）
-- 背景：官网 kevin/csgo 道具指南原来是前端 lineups.ts 打样硬编码，
-- 改为后台（/admin/lineups）可配置：新增/修改/删除点位 + 图片上传。
-- 口径（用户拍板）：多图（images JSON 数组，抄 community_post 写法）；
-- 前台不兜底示例数据，接口失败显示重试；现有 4 条打样作为种子灌入，
-- 避免线上切 API 后空窗（种子带（示例）后缀，admin 里改完即真数据）。
-- 幂等：建表 IF NOT EXISTS；种子按 name 判重，删过不复活（含 deleted=1）。
-- =========================================================
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS csgo_lineup (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    map        VARCHAR(32)     NOT NULL COMMENT '地图名（Mirage/Inferno…，前台页签按本列去重生成）',
    site       VARCHAR(32)     NOT NULL COMMENT '归类点位名（如「警家」「香蕉道」）',
    kind       VARCHAR(16)     NOT NULL COMMENT '道具类型：smoke烟/flash闪/molotov火/decoy诱饵',
    name       VARCHAR(64)     NOT NULL COMMENT '点位一句话名字',
    style      VARCHAR(32)     NOT NULL COMMENT '投掷方式标签：站位/跳投/Timing 等',
    steps      JSON            NOT NULL COMMENT '分步操作（JSON 字符串数组）',
    note       VARCHAR(500)    NULL COMMENT '用途或备注',
    images     JSON            NULL COMMENT '截图 URL 数组（JSON，相对路径 /tsa/files/…）',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted    TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_map_site (map, site)
) ENGINE = InnoDB COMMENT 'CS 道具点位（kevin/csgo 指南）';

-- 种子：前端 lineups.ts 的 4 条打样原样入库（无截图）
INSERT INTO csgo_lineup (map, site, kind, name, style, steps, note)
SELECT 'Mirage', '警家', 'smoke', 'T 萌新一号位警家烟（示例）', '站位',
       JSON_ARRAY('站 T 二楼口栏杆最左侧，背贴墙', '平视对准对面电线杆与屋檐交界点', '大跳松 W 落定后原地投，抛物线过高说明参照点偏了'),
       '示例占位：请替换成你实测稳定落进警家的站位。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM csgo_lineup WHERE name = 'T 萌新一号位警家烟（示例）');

INSERT INTO csgo_lineup (map, site, kind, name, style, steps, note)
SELECT 'Mirage', 'Connector', 'molotov', '哨位近道火（示例）', '跳投',
       JSON_ARRAY('从 T 出口贴右墙助跑', '瞄二楼窗下沿', '出跳瞬间投，落点近道拐角'), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM csgo_lineup WHERE name = '哨位近道火（示例）');

INSERT INTO csgo_lineup (map, site, kind, name, style, steps, note)
SELECT 'Inferno', '香蕉道', 'smoke', 'CT 位香蕉道烟（示例）', '站位',
       JSON_ARRAY('香蕉道 CT 侧沙堆后站位', '瞄墓园砖缝交叉点', '高抛落 CT 车头'),
       '示例占位，换成你自己的双封烟第一颗。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM csgo_lineup WHERE name = 'CT 位香蕉道烟（示例）');

INSERT INTO csgo_lineup (map, site, kind, name, style, steps, note)
SELECT 'Inferno', 'Arch', 'flash', '拱门反清闪（示例）', '反弹',
       JSON_ARRAY('退到图书馆门框', '瞄右侧门柱上沿打反弹', '爆点刚好盖住拱门缺口'), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM csgo_lineup WHERE name = '拱门反清闪（示例）');
