-- ============================================================
-- 迁移脚本: 取票码 (2026-09-29)
--
-- 背景:
--   取票此前只有一条人工通路 —— OrderedService.pickupOrder 对 USER 直接抛
--   FORBIDDEN（"用户无权执行取票操作"），只有 ADMIN / CINEMA 能在后台订单列表里
--   点「取票」。而前台的「去评价」按钮只对 已取票 订单渲染 ——
--   于是普通用户买完票后状态永远停在 待取票，评价闭环对用户端从未打开过。
--
--   本脚本为 ordered 加一列取票码，配合新增的取票大厅（前台自助核销）打通：
--     支付成功 → 生成取票码（一单一码） → 取票大厅凭码核销 → 已取票 → 可评价
--
-- 取票码不需要独立的有效/失效标记：它的可用性完全派生自订单状态 ——
--   核销 = 凭码取单 → 必须 status = '待取票' → 置为 '已取票'
-- 这一个谓词同时实现了「一单一码」「用过即废」「退票/取消作废」「未付款不出发」。
-- 有效期到放映结束（放映开始 + 片长），由 ordered.start 与 film.time 派生，同样不落库。
--
-- 幂等: 重复执行不会报错（列/索引已存在则跳过；回填只在字段为空时进行）。
--       索引名会收敛到 schema.sql 的命名，见第 2 节的说明。
-- 执行顺序: 先部署新代码，再执行本脚本。
-- ============================================================

-- ---------------------------
-- 1. 加列（MySQL 8.0 无 ADD COLUMN IF NOT EXISTS，故用 information_schema 判断）
--    刻意**不写内联 UNIQUE**：内联写法建出来的索引名是列名本身，与 schema.sql 里
--    显式的 `uk_ordered_pickup_code` 对不上，会让新装库与迁移库长出不同的库形。
--    唯一索引改由第 2 节按名字建。
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

CALL add_column_if_missing('ordered', 'pickup_code',
    'VARCHAR(20) NULL DEFAULT NULL COMMENT ''取票码（支付成功时生成，一单一码；核销即作废，有效期到放映结束）''');

DROP PROCEDURE IF EXISTS add_column_if_missing;

-- ---------------------------
-- 2. 唯一索引：保证"两单不会撞码"，且索引名与 schema.sql 一致
--    三种进入状态都要收敛到同一个终态：
--      a. 全新迁移（列刚由第 1 节加上，无索引）        → 建 uk_ordered_pickup_code
--      b. 本脚本的旧版本已跑过（内联 UNIQUE → 索引名=列名）→ 改名，而不是再加一条
--         —— 同一列上留两条唯一索引是纯浪费，且会让 DROP/比对按名字操作时行为不一致
--      c. 已收敛过                                  → 什么都不做
-- ---------------------------
DROP PROCEDURE IF EXISTS ensure_pickup_code_index;
DELIMITER $$
CREATE PROCEDURE ensure_pickup_code_index()
BEGIN
    DECLARE named_cnt INT DEFAULT 0;
    DECLARE implicit_cnt INT DEFAULT 0;

    SELECT COUNT(*) INTO named_cnt
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ordered'
      AND INDEX_NAME = 'uk_ordered_pickup_code';

    IF named_cnt = 0 THEN
        SELECT COUNT(*) INTO implicit_cnt
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ordered'
          AND INDEX_NAME = 'pickup_code';

        IF implicit_cnt > 0 THEN
            ALTER TABLE ordered RENAME INDEX pickup_code TO uk_ordered_pickup_code;
        ELSE
            ALTER TABLE ordered ADD UNIQUE KEY uk_ordered_pickup_code (pickup_code);
        END IF;
    END IF;
END$$
DELIMITER ;

CALL ensure_pickup_code_index();

DROP PROCEDURE IF EXISTS ensure_pickup_code_index;

-- ---------------------------
-- 3. 给存量「待取票」订单补码
--    不补的话，升级前就已支付的订单在取票大厅里查不到 —— 用户会以为订单丢了。
--    已取票 / 已取消 / 已退票 都是终态，码对它们没有意义，刻意不补。
--
--    取值为 4-4 分组的 8 位十六进制，由 id + 随机数 + UUID 共同派生：
--      * 掺入 id 保证同一次 UPDATE 里每行取值不同（否则整表会被写成同一个码，撞唯一索引）
--      * 空间为 16^8 ≈ 4.3e9，本量级撞码概率可忽略；万一真撞了 UPDATE 会以
--        重复键报错整体回滚 —— 是响亮的失败，不是静默写坏数据，重跑即可
--    新代码生成的码用的是去混淆字母表（剔除 I/L/O/0/1），存量这批是十六进制，
--    因此核销端点的入参校验取二者并集 [A-Z0-9]，只对"生成"收窄、对"输入"放宽。
-- ---------------------------
UPDATE ordered
SET pickup_code = UPPER(CONCAT(
        SUBSTRING(MD5(CONCAT(id, '-', RAND(), '-', UUID())), 1, 4),
        '-',
        SUBSTRING(MD5(CONCAT(UUID(), '-', RAND(), '-', id)), 1, 4)))
WHERE status = '待取票'
  AND pickup_code IS NULL;
