-- ============================================================
-- 影院购票管理系统 — 数据库初始化脚本
-- 数据库: xm-film
-- 说明: 一键执行建表 + 初始数据导入
-- ============================================================
-- 使用方式:
--   方式一（推荐）: 先 cd 到 sql 目录, 再执行
--     cd xm_film/sql
--     mysql --default-character-set=utf8mb4 -u root -p < init.sql
--
--   方式二: 在 MySQL 客户端中指定全路径
--     SOURCE /absolute/path/to/xm_film/sql/init.sql;
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `xm-film` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `xm-film`;

-- 建表（SOURCE 路径相对于当前工作目录）
SOURCE schema.sql;

-- 导入数据
SOURCE data.sql;
