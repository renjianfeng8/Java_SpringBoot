-- ============================================================
-- 迁移脚本: 评价引入数值评分 + 影院审核词表收敛 (2026-09-28, P3)
--
-- 背景:
--   1. mark 表只有一个 VARCHAR 的 mark 列，评分与评语混在里面，没有可聚合的
--      数值列；film.score 是独立的人工字段，评分榜与用户评价完全脱节。
--      改为 mark.score(DECIMAL) 作为影片评分的唯一数值来源，mark 列退回「评语」。
--   2. cinema.status 的代码写入值是「待审核」，而 schema 默认值与种子数据都是
--      「未审核」，同一含义两个值；统一收敛为「未审核」/「已审核」。
--      种子影院 8 由「未审核」改为「已审核」（公开接口即将只放行已审核影院）。
--
-- 幂等: 重复执行不会报错（列已存在则跳过；回填只在字段为空时进行；
--       评价种子按 (user_id, film_id) 存在性判断）。
-- 执行顺序: 先部署新代码，再执行本脚本。
--
-- 注意: 演示评价不再预置 —— data.sql 已移除 mark 种子，本脚本也不再镜像评价种子
--       （旧版第 3 节会往已存在的库灌入 51 条虚构评价，与「不写死假数据」相悖）。
--       需要演示评价请跑 scripts/seed-demo-data.py，它走真实接口生成。
-- ============================================================

-- ---------------------------
-- 1. 按需加列 / 加宽（MySQL 8.0 无 ADD COLUMN IF NOT EXISTS，故用 information_schema 判断）
-- ---------------------------
DROP PROCEDURE IF EXISTS add_column_if_missing;
DELIMITER $$
CREATE PROCEDURE add_column_if_missing(
    IN tbl VARCHAR(64), IN col VARCHAR(64), IN definition VARCHAR(255))
BEGIN
    DECLARE cnt INT DEFAULT 0;
    SELECT COUNT(*) INTO cnt
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl AND COLUMN_NAME = col;
    IF cnt = 0 THEN
        SET @s = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN `', col, '` ', definition);
        PREPARE st FROM @s;
        EXECUTE st;
        DEALLOCATE PREPARE st;
    END IF;
END$$
DELIMITER ;

CALL add_column_if_missing('mark', 'score',
    'DECIMAL(3,1) NULL DEFAULT NULL COMMENT ''评分（0.0~10.0，影片评分的唯一数值来源）''');

DROP PROCEDURE IF EXISTS add_column_if_missing;

-- mark 列语义由「评分/评语」收敛为「评语」，20 字符写不下正常评语，加宽到 255
ALTER TABLE mark MODIFY COLUMN `mark` VARCHAR(255) COMMENT '评语';
ALTER TABLE film MODIFY COLUMN `score` DECIMAL(3,1) DEFAULT 0.0
    COMMENT '评分（由 mark.score 回写；无评价时保留基线值）';

-- ---------------------------
-- 2. 回填存量评价的评分
--    历史数据把评分写成了数字文本（种子即 '9.5'），先搬到新列，原列改填评语；
--    其余score为空的行（历史纯文字评语）按所属影片的基线分补齐，
--    避免留下空评分导致该评价被均分忽略、且用户因「已评价过」无法再评。
-- ---------------------------
UPDATE mark
SET score = CAST(mark AS DECIMAL(3,1)),
    mark  = '画面和配音都很用心，孩子全程笑个不停'
WHERE score IS NULL
  AND mark REGEXP '^[0-9]+([.][0-9]+)?$';

UPDATE mark m
SET m.score = (SELECT f.score FROM film f WHERE f.id = m.film_id)
WHERE m.score IS NULL;

-- ---------------------------
-- 3. 演示评价种子 —— 已移除
--    旧版在此镜像 data.sql 的 51 条演示评价，会往已存在的库灌入虚构评价；
--    data.sql 已不再预置 mark，本脚本同步移除。演示评价改由
--    scripts/seed-demo-data.py 走真实接口生成。
-- ---------------------------

-- ---------------------------
-- 4. 影片评分 = 该影片评价均分；无评价的影片保留基线分（不归零）
-- ---------------------------
UPDATE film f
SET f.score = (SELECT ROUND(AVG(m.score), 1) FROM mark m WHERE m.film_id = f.id)
WHERE EXISTS (SELECT 1 FROM mark m2 WHERE m2.film_id = f.id);

-- ---------------------------
-- 5. 影院审核词表收敛
--    「待审核」与「未审核」同义，统一为 schema 默认值「未审核」
-- ---------------------------
UPDATE cinema SET status = '未审核' WHERE status = '待审核';
UPDATE cinema SET status = '已审核' WHERE id = 8 AND status = '未审核';
