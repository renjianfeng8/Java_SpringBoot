ALTER TABLE `admin` ADD COLUMN IF NOT EXISTS `token_revoked_before` bigint DEFAULT NULL COMMENT '令牌吊销点（epoch 秒）：iat 早于此值的令牌失效';
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `token_revoked_before` bigint DEFAULT NULL COMMENT '令牌吊销点（epoch 秒）：iat 早于此值的令牌失效';
ALTER TABLE `cinema` ADD COLUMN IF NOT EXISTS `token_revoked_before` bigint DEFAULT NULL COMMENT '令牌吊销点（epoch 秒）：iat 早于此值的令牌失效';
