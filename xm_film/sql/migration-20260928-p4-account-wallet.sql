-- ============================================================
-- 迁移脚本: 账户余额 + 充值单据 + 资金流水 (2026-09-28, P4)
--
-- 背景:
--   1. 原「模拟支付」是一次无条件的状态流转：payOrder 不读余额、不扣减、
--      不留资金凭证，余额不足的用户也能出票。
--   2. 退票只改订单状态与 refund_amount，没有任何款项回到用户账户。
--   3. 订单没有单价快照，场次改价后历史订单单价不可还原。
--
-- 本脚本为已有数据库补齐三张资金链路所需的表/列：
--   user.balance          账户余额（资金唯一可信来源）
--   recharge_order        充值单据（处理中/已完成/已失败）
--   fund_flow             资金流水（只增不改不删）
--   ordered.unit_price    订单单价快照
--
-- 幂等: 重复执行不会报错（列/表已存在则跳过；回填只在字段为空时进行）。
-- 执行顺序: 先部署新代码，再执行本脚本。
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

CALL add_column_if_missing('user', 'balance',
    'DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT ''账户余额（元，资金唯一可信来源）''');
CALL add_column_if_missing('ordered', 'unit_price',
    'DECIMAL(10,2) NULL DEFAULT NULL COMMENT ''单价快照（元，下单时取自场次票价）''');

DROP PROCEDURE IF EXISTS add_column_if_missing;

-- ---------------------------
-- 2. 新建充值单据表与资金流水表
-- ---------------------------
CREATE TABLE IF NOT EXISTS `recharge_order` (
    `id`          INT           AUTO_INCREMENT PRIMARY KEY COMMENT '充值单据ID',
    `recharge_no` VARCHAR(50)   NOT NULL                   COMMENT '充值单号',
    `user_id`     INT           NOT NULL                   COMMENT '充值用户ID',
    `amount`      DECIMAL(10,2) NOT NULL                   COMMENT '充值金额（元）',
    `status`      VARCHAR(20)   NOT NULL DEFAULT '处理中'   COMMENT '单据状态（处理中/已完成/已失败）',
    `create_time` DATETIME      DEFAULT CURRENT_TIMESTAMP  COMMENT '申请时间',
    `finish_time` DATETIME      NULL DEFAULT NULL          COMMENT '完成时间（回调处理时间）',
    `remark`      VARCHAR(255)                             COMMENT '备注（失败原因等）',
    UNIQUE KEY uk_recharge_no (recharge_no),
    INDEX idx_recharge_user_id (user_id),
    INDEX idx_recharge_status (status),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值单据表';

CREATE TABLE IF NOT EXISTS `fund_flow` (
    `id`             INT           AUTO_INCREMENT PRIMARY KEY COMMENT '流水ID',
    `user_id`        INT           NOT NULL                   COMMENT '用户ID',
    `source`         VARCHAR(20)   NOT NULL                   COMMENT '业务来源（充值/购票/退票）',
    `change_amount`  DECIMAL(10,2) NOT NULL                   COMMENT '变动金额（正为入账，负为出账）',
    `balance_before` DECIMAL(10,2) NOT NULL                   COMMENT '变动前余额（元）',
    `balance_after`  DECIMAL(10,2) NOT NULL                   COMMENT '变动后余额（元）',
    `related_id`     INT                                      COMMENT '关联业务单据ID（充值单ID 或 订单ID）',
    `create_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP  COMMENT '发生时间',
    INDEX idx_flow_user_id (user_id),
    INDEX idx_flow_source (source),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资金流水表（只增不改不删）';

-- ---------------------------
-- 3. 回填存量订单的单价快照
--    单价 = 总金额 ÷ 票数；仅在字段为空时回填，不覆盖新代码写入的快照。
-- ---------------------------
UPDATE ordered
SET unit_price = ROUND(total / number, 2)
WHERE unit_price IS NULL
  AND total IS NOT NULL
  AND number IS NOT NULL
  AND number > 0;

-- ---------------------------
-- 4. 与 data.sql 的种子余额收敛
--    演示账号 zhangsan 预置 100 元；只在余额为 0 时回填，
--    避免把用户真实充值后的余额覆盖回种子值。
-- ---------------------------
UPDATE `user`
SET balance = 100.00
WHERE username = 'zhangsan'
  AND balance = 0.00;
