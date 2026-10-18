-- ============================================================
-- 演示数据（可重复执行）
-- 用途：把空壳库变成「打开就能下单」的演示环境。
-- 执行：mysql -uroot -p123456 xm-film < demo-data.sql
--
-- 前提：库里已有影院与影片（当前开发库：10 家影院 / 64 部影片）。
-- 本脚本只补「影厅 + 排片 + 演示账号」，不动影片与影院。
--
-- 幂等策略：按本脚本独有的固定标识删除后重建。
--   影厅名 = '1号演示厅'
--   账号   = 'u1' / 'u2'
--   排片   = 属于该影厅的行
-- ============================================================

-- ------------------------------------------------------------
-- 1. 清理（只删本脚本建的行）
--    顺序：订单 → 资金/充值/评价 → 排片 → 影厅 → 账号（受外键约束）
-- ------------------------------------------------------------
SET @room_id = (SELECT id FROM room WHERE name = '1号演示厅' LIMIT 1);
SET @u1 = (SELECT id FROM user WHERE username = 'u1' LIMIT 1);
SET @u2 = (SELECT id FROM user WHERE username = 'u2' LIMIT 1);

DELETE FROM ordered WHERE room_id = @room_id;
DELETE FROM fund_flow WHERE user_id IN (@u1, @u2);
DELETE FROM recharge_order WHERE user_id IN (@u1, @u2);
DELETE FROM mark WHERE user_id IN (@u1, @u2);
DELETE FROM record WHERE room_id = @room_id;
DELETE FROM room WHERE id = @room_id;
DELETE FROM user WHERE username IN ('u1', 'u2');

-- ------------------------------------------------------------
-- 2. 影厅：建在 id 最小的那家影院名下，8 排 x 8 座
-- ------------------------------------------------------------
SET @cinema_id = (SELECT MIN(id) FROM cinema);

INSERT INTO room (cinema_id, title, name, seat_rows, seat_cols)
VALUES (@cinema_id,
        (SELECT name FROM cinema WHERE id = @cinema_id),
        '1号演示厅', 8, 8);

SET @room_id = LAST_INSERT_ID();

-- ------------------------------------------------------------
-- 3. 排片：三场，各覆盖一个演示场景
--    A. 明天 19:00  —— 正常购票 / 抢座 / 退票（距开演 > 60 分钟）
--    B. 1 小时后    —— 演示「临近开演退票被拒」（< 60 分钟）
--    C. 3 天后      —— 演示「超时废单」（下单不付款，5 分钟自动释放座位）
-- ------------------------------------------------------------
SET @film_a = (SELECT id FROM film WHERE status = '已上映' ORDER BY id LIMIT 1);
SET @film_b = (SELECT id FROM film WHERE status = '已上映' ORDER BY id LIMIT 1 OFFSET 1);
SET @film_c = (SELECT id FROM film WHERE status = '已上映' ORDER BY id LIMIT 1 OFFSET 2);

INSERT INTO record (cinema_id, room_id, film_id, title, start, price, status)
VALUES (@cinema_id, @room_id, @film_a,
        (SELECT title FROM film WHERE id = @film_a),
        DATE_ADD(CURDATE(), INTERVAL 1 DAY) + INTERVAL 19 HOUR,
        '45.00', '正常');

INSERT INTO record (cinema_id, room_id, film_id, title, start, price, status)
VALUES (@cinema_id, @room_id, @film_b,
        (SELECT title FROM film WHERE id = @film_b),
        DATE_ADD(NOW(), INTERVAL 1 HOUR),
        '55.00', '正常');

INSERT INTO record (cinema_id, room_id, film_id, title, start, price, status)
VALUES (@cinema_id, @room_id, @film_c,
        (SELECT title FROM film WHERE id = @film_c),
        DATE_ADD(CURDATE(), INTERVAL 3 DAY) + INTERVAL 20 HOUR,
        '38.00', '正常');

-- ------------------------------------------------------------
-- 4. 演示账号
--    登录密码：明文（后端 login 支持明文回退，见 UserService.login）
--    支付密码：BCrypt('123456')，由 spring-security-crypto 生成并 verify 通过
--    余额：500.00（够买 6 座 x 55 元 = 330）
-- ------------------------------------------------------------
INSERT INTO user (username, password, name, role, balance, pay_password,
                  pay_pwd_error_count, pay_pwd_locked_until)
VALUES ('u1', 'u1', '演示用户甲', 'USER', 500.00,
        '$2a$10$iKH2tq6gVm6KbC8d/0DzVeM7qFSxrHCi9kda6udH2vOQdHK6YkSci', 0, NULL);

INSERT INTO user (username, password, name, role, balance, pay_password,
                  pay_pwd_error_count, pay_pwd_locked_until)
VALUES ('u2', 'u2', '演示用户乙', 'USER', 500.00,
        '$2a$10$iKH2tq6gVm6KbC8d/0DzVeM7qFSxrHCi9kda6udH2vOQdHK6YkSci', 0, NULL);

-- ------------------------------------------------------------
-- 5. 自检
-- ------------------------------------------------------------
SELECT '影厅' AS 项目, COUNT(*) AS 数量 FROM room WHERE id = @room_id
UNION ALL SELECT '排片', COUNT(*) FROM record WHERE room_id = @room_id
UNION ALL SELECT '演示账号', COUNT(*) FROM user WHERE username IN ('u1', 'u2')
UNION ALL SELECT '其中已设支付密码', COUNT(*) FROM user
         WHERE username IN ('u1', 'u2') AND pay_password IS NOT NULL
UNION ALL SELECT '排片时间', COUNT(*) FROM record
         WHERE room_id = @room_id AND start > NOW();
