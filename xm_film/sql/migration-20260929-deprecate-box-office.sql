-- ============================================================
-- 迁移脚本: 废弃 film.box_office 静态票房列 (2026-09-29)
--
-- 背景:
--   票房原先读 film.box_office 这个人工维护的静态列，种子写的是编造的数值，
--   运行时没有任何代码重算它 —— 榜单上那串数字没有任何来源。
--   现已改为按 ordered 实时聚合累计售票收入（见 FilmMapper.xml 的 filmRevenueJoin，
--   只统计 待取票/已取票），前端展示口径同步由「万元」改为「元」（utils/format.js）。
--
-- 本脚本只做两件安全的事，不删除任何业务行：
--   1. 把残留的编造票房清零（该列已不再被任何查询读取）
--   2. 更新列注释，标明已废弃
--
-- 幂等: 重复执行结果一致。
-- 注意: 存量库里的演示订单 / 评价 / 排片请用 scripts/seed-demo-data.py 清理与重建
--       —— 它按演示账号边界删除，不靠猜 id；本脚本刻意不碰这三张表。
-- ============================================================

UPDATE film SET box_office = 0.0 WHERE box_office <> 0.0;

ALTER TABLE film MODIFY COLUMN `box_office` DECIMAL(10,1) DEFAULT 0.0
    COMMENT '已废弃：票房改由 ordered 实时聚合（见 FilmMapper.filmRevenueJoin），此列恒为 0';
