-- ============================================================
-- 迁移脚本: 评价点赞 (2026-09-29)
--
-- 背景:
--   评价此前只有"发表 / 修改"一条通路（mark 表，一人一片一条），无法表达"这条评价有用"。
--   本脚本新增关系表 mark_like，为"点赞评价"提供存储：
--     点赞   → 写入一行 (mark_id, user_id)
--     取消赞 → 删除该行
--     赞数   → COUNT(*) 实时聚合
--
--   赞数**刻意不落冗余计数列**：主键 (mark_id, user_id) 同时承担"一人一赞"与
--   "可取消"两件事，是赞数的唯一权威来源。冗余计数列会引入第二处真相 —— 取消赞、
--   评价被删、并发点赞任意一处漏同步，计数就永久偏离，且无法自证对错。
--   这与 fund_flow 只增行、user.balance 为唯一余额来源是同一思路。
--
--   纯关系表用复合主键、不留代理 id（与 film_type 同构）：既然没有任何代码按 id 取
--   点赞行，代理键只会多出一个没人用的 PRIMARY 索引。
--
--   两个外键都是 ON DELETE CASCADE：
--     * 评价被删 → 其点赞关系随之消失（点赞依附于评价，无评价即无意义）
--     * 用户被删 → 其点过的赞随之消失（不留归属已不存在的孤儿行）
--   注意这与 ordered 的 ON DELETE RESTRICT（交易凭证禁删）方向相反，是刻意区分：
--   点赞是"轻关系"，订单是"资金凭证"。
--
-- 幂等: 使用 CREATE TABLE IF NOT EXISTS —— 重复执行不会报错，且**不含任何 DROP TABLE**
--       （本脚本跑在活库上，DROP 会抹掉用户真实的点赞数据）。
-- 执行顺序: 先部署新代码，再执行本脚本。
--          全新安装以 schema.sql 为唯一来源（其中的 mark_like 与下表定义一致），
--          本脚本只服务"已存在的库"。
-- ============================================================

CREATE TABLE IF NOT EXISTS `mark_like` (
    `mark_id` INT NOT NULL COMMENT '被点赞的评价ID',
    `user_id` INT NOT NULL COMMENT '点赞人ID',
    PRIMARY KEY (mark_id, user_id),
    INDEX idx_mark_like_user_id (user_id),
    FOREIGN KEY (mark_id) REFERENCES mark(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价点赞关系';
