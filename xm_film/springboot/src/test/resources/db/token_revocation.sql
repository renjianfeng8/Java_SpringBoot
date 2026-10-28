-- 令牌吊销点：改密/重置后，iat 早于该值的旧令牌一律拒绝（TokenRevocationService）。
-- NULL＝从未吊销过。用 BIGINT 存 epoch 秒，与 JWT 的 NumericDate 同单位。
-- MySQL 8 的 ADD COLUMN 不支持 IF NOT EXISTS（那是 MariaDB 语法）：用 information_schema 判一下，
-- 让本脚本可以对着已有该列的开发库重复执行。
SET @ddl = (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE `admin` ADD COLUMN `token_revoked_before` bigint DEFAULT NULL COMMENT ''令牌吊销点（epoch 秒）：iat 早于此值的令牌失效''',
            'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'admin' AND column_name = 'token_revoked_before');
PREPARE stmt FROM @ddl;
EXECUTE stmt;

SET @ddl = (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE `user` ADD COLUMN `token_revoked_before` bigint DEFAULT NULL COMMENT ''令牌吊销点（epoch 秒）：iat 早于此值的令牌失效''',
            'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'user' AND column_name = 'token_revoked_before');
PREPARE stmt FROM @ddl;
EXECUTE stmt;

SET @ddl = (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE `cinema` ADD COLUMN `token_revoked_before` bigint DEFAULT NULL COMMENT ''令牌吊销点（epoch 秒）：iat 早于此值的令牌失效''',
            'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'cinema' AND column_name = 'token_revoked_before');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
