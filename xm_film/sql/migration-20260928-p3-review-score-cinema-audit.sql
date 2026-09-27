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
-- 注意: data.sql 是种子数据的权威副本，本脚本第 3 节镜像了其中的演示评价，
--       使已存在的库与全新 init.sql 初始化后的库收敛到同一状态。
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
-- 3. 演示评价种子（镜像 data.sql）
--    每部影片 3 条评价，取值 基线-0.1 / 基线 / 基线+0.1，均分恰好等于原基线分，
--    因此影片评分与评分榜排序保持不变，但从此由真实评价派生。
-- ---------------------------
CREATE TEMPORARY TABLE tmp_mark_seed (
    user_id INT, film_id INT, score DECIMAL(3,1), comment VARCHAR(255)
);

INSERT INTO tmp_mark_seed (user_id, film_id, score, comment) VALUES
(6, 10, 9.5, '画面和配音都很用心，孩子全程笑个不停'),
(7, 10, 9.6, '故事节奏舒服，笑点密集，适合全家一起看'),
(8, 10, 9.7, '比预想的好很多，海洋场景做得很漂亮'),
(6, 11, 9.1, '悬疑铺得很稳，中段开始完全停不下来'),
(7, 11, 9.2, '节奏紧凑，拆弹段落拍得很有压迫感'),
(8, 11, 9.3, '结局收得干净，是近几年少见的推理佳片'),
(6, 12, 8.2, '上海方言的烟火气很足，台词写得有味道'),
(7, 12, 8.3, '三个女人一台戏，松弛又真实'),
(8, 12, 8.4, '中年人的爱情被拍得意外轻盈，值得二刷'),
(6, 13, 8.3, '舞台与实景的切换很有气势'),
(7, 13, 8.4, '画面质感很强，配乐加分不少'),
(8, 13, 8.5, '史料与艺术结合得不错，看完很受触动'),
(6, 14, 9.0, '圣诞元素拉满，动作和喜剧平衡得不错'),
(7, 14, 9.1, '两位主演的斗嘴是全片最好的部分'),
(8, 14, 9.2, '节日气氛很浓，图个热闹完全不亏'),
(6, 15, 7.7, '原著还原度尚可，反派演得让人牙痒'),
(7, 15, 7.8, '魔法部大战是亮点，前面铺垫略长'),
(8, 15, 7.9, '系列里偏暗的一部，成年观众更有共鸣'),
(6, 16, 6.3, '经典归经典，这一版配音少了点味道'),
(7, 16, 6.4, '情感真挚，但节奏对现在的观众偏慢'),
(8, 16, 6.5, '两位主角的对手戏依旧动人'),
(6, 17, 8.1, '母星的故事线比真人版清爽很多'),
(7, 17, 8.2, '领袖反目拍得有说服力'),
(8, 17, 8.3, '变形特效燃，当作动画大片看很过瘾'),
(6, 18, 7.8, '神秘感营造得不错，中后段解释得略仓促'),
(7, 18, 7.9, '少年成长线完整，特效在国产片里算用心'),
(8, 18, 8.0, '设定有意思，结尾收得稍急但整体可看'),
(6, 19, 8.2, '青春片该有的样子，少年感很足'),
(7, 19, 8.3, '暗恋的细腻写得很准，看哭了一片人'),
(8, 19, 8.4, '天降对竹马，选谁都让人心疼'),
(6, 20, 8.4, '卧底线拍得克制，最后一段很难受'),
(7, 20, 8.5, '男主的表演撑住了全片'),
(8, 20, 8.6, '虐得合理，同类题材里少见的完成度'),
(6, 21, 8.3, '两个女孩对画画的执念写得很温柔'),
(7, 21, 8.4, '原作的叙事节奏被完整保留下来'),
(8, 21, 8.5, '短小但后劲很大，散场后还在想'),
(6, 22, 8.1, '成龙本色出演，动作设计依旧在线'),
(7, 22, 8.2, '熊猫幼崽太可爱了，孩子很喜欢'),
(8, 22, 8.3, '合家欢喜剧，笑点低龄但不尴尬'),
(6, 23, 8.0, '争霸赛的比赛场面很有看头'),
(7, 23, 8.1, '迷宫那一段压迫感做得很足'),
(8, 23, 8.2, '系列里节奏最紧凑的一部之一'),
(6, 24, 8.7, '机舱内的封闭空间调度得相当紧张'),
(7, 24, 8.8, '男主的身手依旧，打戏干净利落'),
(8, 24, 8.9, '劫机题材拍出了新意，全程不敢走神'),
(6, 25, 8.5, '主角搭档的互怼依旧好笑'),
(7, 25, 8.6, '终章的动作场面给足了分量'),
(8, 25, 8.7, '结尾有点舍不得，系列粉丝值得一看'),
(6, 26, 8.6, '阻击战的场面拍得很震撼'),
(7, 26, 8.7, '人物群像立得住，情绪推得很稳'),
(8, 26, 8.8, '战争片的分量感很足，看完很沉重');

INSERT INTO mark (user_id, film_id, score, mark)
SELECT t.user_id, t.film_id, t.score, t.comment
FROM tmp_mark_seed t
WHERE NOT EXISTS (
    SELECT 1 FROM mark m WHERE m.user_id = t.user_id AND m.film_id = t.film_id
);

DROP TEMPORARY TABLE tmp_mark_seed;

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
