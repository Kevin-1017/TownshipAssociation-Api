-- 一次性增量脚本：对齐前后端契约（执行后即弃，权威结构以 sql/schema.sql 为准）
-- 1) announcement 增加 summary 列并回填种子摘要
-- 2) member.industry 由中文改为字典 code（与前端 constants/industry.ts 对齐）
USE tsa;

ALTER TABLE announcement ADD COLUMN summary VARCHAR(200) NULL COMMENT '列表摘要（正文摘录，列表页展示）' AFTER title;

UPDATE announcement SET summary='筹备组已成立，志愿者报名通道开启，详情关注后续公告。' WHERE title='乡会 2026 年度恳亲大会筹备启动';
UPDATE announcement SET summary='注册资料需管理员审核通过后，才会显示在成员列表与地图中。' WHERE title='新成员审核规则说明';
UPDATE announcement SET summary='招募 20 位老乡体验小程序并反馈问题。' WHERE title='小程序内测招募';

UPDATE member SET industry='internet'     WHERE industry='互联网';
UPDATE member SET industry='finance'      WHERE industry='金融';
UPDATE member SET industry='education'    WHERE industry='教育';
UPDATE member SET industry='manufacture'  WHERE industry='制造';
UPDATE member SET industry='medical'      WHERE industry='医疗';
UPDATE member SET industry='construction' WHERE industry='建筑';
UPDATE member SET industry='civil'        WHERE industry='公务员';
UPDATE member SET industry='other'        WHERE industry IN ('法律','媒体','自由职业');
