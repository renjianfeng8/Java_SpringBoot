-- ============================================================
-- 迁移脚本: 支付流程 + 座位超时释放 (2026-06-19)
--
-- 为已有数据库增加 ordered 表的新列和索引。
-- 幂等: 列/索引已存在时跳过，不会报错。
-- ============================================================

-- 1. 添加 create_time 列
SET @db = (SELECT DATABASE());
SET @exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'ordered' AND COLUMN_NAME = 'create_time');
SET @sql = IF(@exists = 0,
    'ALTER TABLE ordered ADD COLUMN create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''订单创建时间'' AFTER seat',
    'SELECT ''create_time already exists, skipping''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 添加 pending_timeout_at 列
SET @exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'ordered' AND COLUMN_NAME = 'pending_timeout_at');
SET @sql = IF(@exists = 0,
    'ALTER TABLE ordered ADD COLUMN pending_timeout_at DATETIME NULL DEFAULT NULL COMMENT ''待支付超时时间'' AFTER create_time',
    'SELECT ''pending_timeout_at already exists, skipping''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 添加复合索引 (status, pending_timeout_at)
SET @exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'ordered' AND INDEX_NAME = 'idx_ordered_status_timeout');
SET @sql = IF(@exists = 0,
    'ALTER TABLE ordered ADD INDEX idx_ordered_status_timeout (status, pending_timeout_at)',
    'SELECT ''idx_ordered_status_timeout already exists, skipping''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
