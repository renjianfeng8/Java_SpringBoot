-- ============================================================
-- 迁移脚本: 影厅座位容量 + 订单资金凭证 (2026-09-27, P2)
--
-- 背景:
--   1. 选座图原为前端硬编码 8×8、后端座位校验正则 [1-8]排[1-8]座 也写死，
--      所有影厅被假设成同样大小。改为按影厅存量行列数渲染与校验。
--   2. 订单只有 total 一个金额字段，没有支付/退款的时间与金额留痕，
--      退票功能缺少资金凭证。
--   3. ordered.status 词表新增「已退票」。
--
-- 幂等: 重复执行不会报错（列已存在则跳过；回填只在字段为空时进行）。
-- 执行顺序: 先部署新代码，再执行本脚本。
--
-- 注意: 已有影厅一律按 8×8 初始化 —— 这是旧规则下唯一合法的座位范围，
--       因此存量订单的座位号必然落在新边界内；如需放大影厅请到后台改该厅行列数。
-- ============================================================

-- ---------------------------
-- 1. 按需加列（MySQL 8.0 无 ADD COLUMN IF NOT EXISTS，故用 information_schema 判断）
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

CALL add_column_if_missing('room', 'seat_rows',
    'INT NOT NULL DEFAULT 8 COMMENT ''座位行数（选座图纵向格数）''');
CALL add_column_if_missing('room', 'seat_cols',
    'INT NOT NULL DEFAULT 8 COMMENT ''座位列数（选座图横向格数）''');

CALL add_column_if_missing('ordered', 'pay_time',
    'DATETIME NULL DEFAULT NULL COMMENT ''支付时间（资金凭证）''');
CALL add_column_if_missing('ordered', 'pay_amount',
    'DECIMAL(10,2) NULL DEFAULT NULL COMMENT ''实付金额（元）''');
CALL add_column_if_missing('ordered', 'refund_time',
    'DATETIME NULL DEFAULT NULL COMMENT ''退票时间（资金凭证）''');
CALL add_column_if_missing('ordered', 'refund_amount',
    'DECIMAL(10,2) NULL DEFAULT NULL COMMENT ''退款金额（元）''');

DROP PROCEDURE IF EXISTS add_column_if_missing;

-- ---------------------------
-- 2. ordered.status 词表补「已退票」
-- ---------------------------
ALTER TABLE ordered MODIFY COLUMN status VARCHAR(20) DEFAULT '待取票'
    COMMENT '订单状态（待支付/待取票/已取票/已取消/已退票）';

-- ---------------------------
-- 3. 回填存量已支付订单的支付凭证
--    待取票/已取票必然已支付，故以下单时间作为支付时间、订单总额作为实付金额；
--    仅在字段为空时回填，不覆盖新代码写入的真实凭证。
-- ---------------------------
UPDATE ordered
SET pay_time = create_time,
    pay_amount = total
WHERE status IN ('待取票', '已取票')
  AND pay_time IS NULL
  AND create_time IS NOT NULL;
