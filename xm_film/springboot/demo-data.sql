-- ============================================================
-- 业务数据（可重复执行）
-- 用途：把空壳库变成「打开就能下单」的环境，并为库里每一张表
--       铺一条能走通的真实业务链路（写入口径见各段注释）。
-- 执行：mysql -uroot -p123456 xm-film < demo-data.sql
--
-- 前提：库里已有影片、类型、产地、演员、影院名录。本脚本不动这些种子数据，
--       也不删既有影院与既有账号（'111' 等）。
--
-- 覆盖的 18 张表：
--   种子已有：type area actor film_type cinema_directory film cinema
--   本脚本建：room record ordered fund_flow recharge_order user mark mark_like
--             notice video
--
-- 业务顺序（与后端写入顺序一致，外键决定不能乱）：
--   §2 账号/公告/预告片 → §3 影厅 → §4 排片 → §5 '111' 的交易
--   → §6 新用户一周交易 → §7 自助取票（只核对）→ §8 评价与点赞
--   → §9 派生数据回算 → §10 自检
--
-- 幂等策略：全部按本脚本独有的固定标识删除后重建：
--   账号   = 'u1' / 'u2' / 's01'…'s10'
--           （其它既有账号不删不建，只补支付密码）
--   影厅   = 影院名 + 本脚本的厅名（1号厅 / 2号厅 / 1号巨幕厅 …）
--   排片   = 属于这些影厅的行
--   订单   = 属于这些影厅的行（订单号前缀 HORD / SORD）
--   公告   = 标题前缀【影院】/【系统】
--   预告片 = name 列前缀【预告】
-- ============================================================

-- ------------------------------------------------------------
-- 1. 清理
-- ------------------------------------------------------------
SET @u1 = (SELECT id FROM user WHERE username = 'u1' LIMIT 1);
SET @u2 = (SELECT id FROM user WHERE username = 'u2' LIMIT 1);
SET @u111 = (SELECT id FROM user WHERE username = '111' LIMIT 1);

-- 含旧版遗留的影厅名（1号演示厅 / 2号演示厅）：否则旧订单挡住用户删除
SET @room_ids = COALESCE(
    (SELECT GROUP_CONCAT(id) FROM room
     WHERE name IN ('1号厅', '2号厅', '3号厅', '1号巨幕厅', '2号IMAX厅', '3号VIP厅',
                    '1号演示厅', '2号演示厅')), '0');

DELETE FROM ordered WHERE FIND_IN_SET(room_id, @room_ids) > 0
   OR orders LIKE 'HORD%' OR orders LIKE 'SORD%' OR orders LIKE 'XORD%'
   OR orders LIKE 'DORD%';
-- 统一按用户名删：u1/u2 在 §2 才重建，§1 读不到它们的 id。
-- s% 只匹配本脚本的 10 个用户名（s01..s10），不命中其它以 s 开头的既有账号以外的东西。
DELETE FROM fund_flow WHERE user_id IN (SELECT id FROM user WHERE username IN ('u1', 'u2', '111')
   OR username REGEXP '^s[0-9]{2}$');
DELETE FROM recharge_order WHERE user_id IN (SELECT id FROM user WHERE username IN ('u1', 'u2', '111')
   OR username REGEXP '^s[0-9]{2}$');
DELETE FROM mark WHERE user_id IN (SELECT id FROM user WHERE username IN ('u1', 'u2', '111')
   OR username REGEXP '^s[0-9]{2}$');
-- 旧版遗留的「-6..-1 天 19:00 无间道」排片也要清（否则会和新排片撞车）
DELETE FROM record WHERE FIND_IN_SET(room_id, @room_ids) > 0
   OR (title = '无间道' AND HOUR(start) = 19 AND DATE(start) < CURDATE());
DELETE FROM room WHERE FIND_IN_SET(id, @room_ids) > 0;
DELETE FROM user WHERE username IN ('u1', 'u2', 's01', 's02', 's03', 's04', 's05',
                                   's06', 's07', 's08', 's09', 's10');
DELETE FROM notice WHERE title LIKE '【影院】%' OR title LIKE '【系统】%'
   OR title IN ('周末黄金场排期已更新', '自助取票机位置调整', '会员余额充值说明');
DELETE FROM video WHERE name LIKE '【预告】%'
   OR name IN ('肖申克的救赎 中文预告', '千与千寻 中文预告', '无间道 4K 修复版预告')
   OR title IN ('肖申克的救赎', '千与千寻', '无间道');

-- 评分由 mark 聚合，清掉本脚本的评价后先还原，§9 再按新数据回算
UPDATE film f
SET score = (SELECT ROUND(AVG(m.score), 1) FROM mark m WHERE m.film_id = f.id)
WHERE f.id IN (2, 3, 8, 12, 15, 17, 25, 30, 44, 45);

-- ------------------------------------------------------------
-- 2. 账号、公告、预告片
--    - 登录口令：明文（后端 login 支持明文回退，见 UserService.login）
--    - 支付密码：BCrypt('123456')
-- ------------------------------------------------------------
INSERT INTO user (username, password, name, role, balance, pay_password,
                  pay_pwd_error_count, pay_pwd_locked_until)
VALUES ('u1', 'u1', '陈晨', 'USER', 500.00,
        '$2a$10$iKH2tq6gVm6KbC8d/0DzVeM7qFSxrHCi9kda6udH2vOQdHK6YkSci', 0, NULL);
INSERT INTO user (username, password, name, role, balance, pay_password,
                  pay_pwd_error_count, pay_pwd_locked_until)
VALUES ('u2', 'u2', '林悦', 'USER', 500.00,
        '$2a$10$iKH2tq6gVm6KbC8d/0DzVeM7qFSxrHCi9kda6udH2vOQdHK6YkSci', 0, NULL);
SET @u1 = LAST_INSERT_ID() - 1;
SET @u2 = LAST_INSERT_ID();

-- 10 个业务用户：用户名 s01..s10，口令同名，支付密码 123456
INSERT INTO user (username, password, name, role, balance, pay_password,
                  pay_pwd_error_count, pay_pwd_locked_until)
VALUES ('s01', 's01', '王磊', 'USER', 800.00, '$2a$10$iKH2tq6gVm6KbC8d/0DzVeM7qFSxrHCi9kda6udH2vOQdHK6YkSci', 0, NULL),
       ('s02', 's02', '李静', 'USER', 800.00, '$2a$10$iKH2tq6gVm6KbC8d/0DzVeM7qFSxrHCi9kda6udH2vOQdHK6YkSci', 0, NULL),
       ('s03', 's03', '张伟', 'USER', 800.00, '$2a$10$iKH2tq6gVm6KbC8d/0DzVeM7qFSxrHCi9kda6udH2vOQdHK6YkSci', 0, NULL),
       ('s04', 's04', '刘洋', 'USER', 800.00, '$2a$10$iKH2tq6gVm6KbC8d/0DzVeM7qFSxrHCi9kda6udH2vOQdHK6YkSci', 0, NULL),
       ('s05', 's05', '赵敏', 'USER', 800.00, '$2a$10$iKH2tq6gVm6KbC8d/0DzVeM7qFSxrHCi9kda6udH2vOQdHK6YkSci', 0, NULL),
       ('s06', 's06', '孙浩', 'USER', 800.00, '$2a$10$iKH2tq6gVm6KbC8d/0DzVeM7qFSxrHCi9kda6udH2vOQdHK6YkSci', 0, NULL),
       ('s07', 's07', '周敏', 'USER', 800.00, '$2a$10$iKH2tq6gVm6KbC8d/0DzVeM7qFSxrHCi9kda6udH2vOQdHK6YkSci', 0, NULL),
       ('s08', 's08', '吴刚', 'USER', 800.00, '$2a$10$iKH2tq6gVm6KbC8d/0DzVeM7qFSxrHCi9kda6udH2vOQdHK6YkSci', 0, NULL),
       ('s09', 's09', '郑爽', 'USER', 800.00, '$2a$10$iKH2tq6gVm6KbC8d/0DzVeM7qFSxrHCi9kda6udH2vOQdHK6YkSci', 0, NULL),
       ('s10', 's10', '周雨', 'USER', 800.00, '$2a$10$iKH2tq6gVm6KbC8d/0DzVeM7qFSxrHCi9kda6udH2vOQdHK6YkSci', 0, NULL);

-- 既有账号补支付密码（能走完整支付链路）；口令与余额不动
UPDATE user SET pay_password =
        '$2a$10$iKH2tq6gVm6KbC8d/0DzVeM7qFSxrHCi9kda6udH2vOQdHK6YkSci',
        pay_pwd_error_count = 0, pay_pwd_locked_until = NULL
WHERE id = @u111;

INSERT INTO notice (title, content, time)
VALUES ('【影院】周末黄金场排期已更新',
        '本周五至周日，3 号厅增开 19:00 与 21:30 两场。线上购票请提前 60 分钟办理退票。',
        CURDATE() + INTERVAL 9 HOUR),
       ('【影院】自助取票机位置调整',
        '自助取票机移至一楼大厅东侧，取票码有效期至放映结束，请合理安排取票时间。',
        CURDATE() - INTERVAL 1 DAY + INTERVAL 14 HOUR),
       ('【系统】会员余额充值说明',
        '账户余额仅用于购票，退款原路退回余额。充值记录可在「我的账户 - 充值流水」查询。',
        CURDATE() - INTERVAL 6 DAY + INTERVAL 11 HOUR);

-- 预告片只有 film.video 一个来源（前台详情页、后台「电影预告」页读的都是它）。
-- 旧 video 表里唯一有价值的是本地上传的视频：只在 film.video 为空时回灌一次，
-- 之后该表不再被任何页面读取，留在库里不影响业务。
UPDATE film f
JOIN video v ON v.title = f.title
SET f.video = v.preview
WHERE (f.video IS NULL OR f.video = '')
  AND v.preview IS NOT NULL AND v.preview <> '';

-- ------------------------------------------------------------
-- 3. 影厅：12 家影院每家 3 个厅（1号厅 / 2号厅 / 3号厅），另给前两家加巨幕与 IMAX
--    room.title 存影院名，与 RoomService.addScoped 的写入口径一致
-- ------------------------------------------------------------
INSERT INTO room (cinema_id, title, name, seat_rows, seat_cols)
SELECT c.id, c.name, '1号厅', 10, 12 FROM cinema c WHERE c.status = '已审核';
INSERT INTO room (cinema_id, title, name, seat_rows, seat_cols)
SELECT c.id, c.name, '2号厅', 8, 10 FROM cinema c WHERE c.status = '已审核';
INSERT INTO room (cinema_id, title, name, seat_rows, seat_cols)
SELECT c.id, c.name, '3号厅', 6, 8 FROM cinema c WHERE c.status = '已审核';

INSERT INTO room (cinema_id, title, name, seat_rows, seat_cols)
SELECT c.id, c.name, '1号巨幕厅', 12, 16 FROM cinema c
WHERE c.id = (SELECT MIN(id) FROM cinema);
INSERT INTO room (cinema_id, title, name, seat_rows, seat_cols)
SELECT c.id, c.name, '2号IMAX厅', 10, 14 FROM cinema c
WHERE c.id = (SELECT MIN(id) FROM cinema);

-- ------------------------------------------------------------
-- 4. 排片
--    FX：未来 7 天 × 每家影院的 1/2/3 号厅 × 4 个时段，影片轮转覆盖 65 部
--    HY：-6..-1 天每晚 19:00 的《无间道》（承载历史订单）
--    MS：明天的《千与千寻》（新用户第 7 笔交易用）
-- ------------------------------------------------------------
-- 未来排片：3 场/天/影院，40+ 场/天，65 部片轮转
INSERT INTO record (cinema_id, room_id, film_id, title, start, price, status)
SELECT r.cinema_id, r.id, f.id, f.title,
       CURDATE() + INTERVAL d.offs DAY + INTERVAL t.hour HOUR,
       CAST(38 + (f.id % 4) * 5 AS DECIMAL(10,2)),
       '正常'
FROM (SELECT 0 offs UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
      UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6) d
JOIN (SELECT 10 hour, 0 slot UNION ALL SELECT 13, 1 UNION ALL SELECT 19, 2) t
JOIN room r ON r.name = '1号厅'
JOIN (SELECT id, title, ROW_NUMBER() OVER (ORDER BY id) rn FROM film WHERE status = '已上映') f
  ON f.rn = 1 + ((d.offs * 3 + t.slot) * 7 + r.cinema_id) % 65;

INSERT INTO record (cinema_id, room_id, film_id, title, start, price, status)
SELECT r.cinema_id, r.id, f.id, f.title,
       CURDATE() + INTERVAL d.offs DAY + INTERVAL t.hour HOUR,
       CAST(40 + (f.id % 3) * 4 AS DECIMAL(10,2)),
       '正常'
FROM (SELECT 0 offs UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
      UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6) d
JOIN (SELECT 14 hour, 0 slot UNION ALL SELECT 16, 1 UNION ALL SELECT 21, 2) t
JOIN room r ON r.name = '2号厅'
JOIN (SELECT id, title, ROW_NUMBER() OVER (ORDER BY id) rn FROM film WHERE status = '已上映') f
  ON f.rn = 1 + ((d.offs * 5 + t.slot) * 3 + r.cinema_id * 2) % 65;

-- 历史：-6..-1 晚 19:00《无间道》（每家影院 1 号厅），价格 42
INSERT INTO record (cinema_id, room_id, film_id, title, start, price, status)
SELECT r.cinema_id, r.id, 12, '无间道',
       CURDATE() - INTERVAL d.offs DAY + INTERVAL 19 HOUR, 42.00, '正常'
FROM (SELECT 1 offs UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
      UNION ALL SELECT 5 UNION ALL SELECT 6) d
JOIN room r ON r.name = '1号厅';

-- 明天 21:30《千与千寻》55 元（3 号厅；避开 19:00 的轮转排片，同厅不重叠）
INSERT INTO record (cinema_id, room_id, film_id, title, start, price, status)
SELECT r.cinema_id, r.id, 3, '千与千寻',
       CURDATE() + INTERVAL 1 DAY + INTERVAL 21 HOUR + INTERVAL 30 MINUTE, 55.00, '正常'
FROM room r WHERE r.name = '3号厅';

-- 常用场次变量（取主影院＝1 号厅 12 号影院）
SET @cinema_main = (SELECT cinema_id FROM room WHERE name = '1号厅' ORDER BY cinema_id LIMIT 1);
SET @room_main = (SELECT id FROM room WHERE name = '1号厅' AND cinema_id = @cinema_main);
SET @rec_d = (SELECT id FROM record WHERE room_id = @room_main AND title = '无间道'
              AND DATE(start) = CURDATE() - INTERVAL 3 DAY LIMIT 1);
SET @rec_p6 = (SELECT id FROM record WHERE room_id = @room_main AND title = '无间道'
               AND DATE(start) = CURDATE() - INTERVAL 6 DAY LIMIT 1);
SET @rec_p5 = (SELECT id FROM record WHERE room_id = @room_main AND title = '无间道'
               AND DATE(start) = CURDATE() - INTERVAL 5 DAY LIMIT 1);
SET @rec_p4 = (SELECT id FROM record WHERE room_id = @room_main AND title = '无间道'
               AND DATE(start) = CURDATE() - INTERVAL 4 DAY LIMIT 1);
SET @rec_p2 = (SELECT id FROM record WHERE room_id = @room_main AND title = '无间道'
               AND DATE(start) = CURDATE() - INTERVAL 2 DAY LIMIT 1);
SET @rec_p1 = (SELECT id FROM record WHERE room_id = @room_main AND title = '无间道'
               AND DATE(start) = CURDATE() - INTERVAL 1 DAY LIMIT 1);
SET @rec_future = (SELECT id FROM record WHERE title = '千与千寻'
                   AND DATE(start) = CURDATE() + INTERVAL 1 DAY LIMIT 1);

-- ------------------------------------------------------------
-- 5. '111' 的一周交易（余额从原值起算，脚本不改它的原值）
-- ------------------------------------------------------------
SET @u111_balance0 = (SELECT balance FROM user WHERE id = @u111);

INSERT INTO recharge_order (recharge_no, user_id, amount, status, create_time, finish_time, remark)
VALUES ('HRD202610160001', @u111, 200.00, '已完成',
        CURDATE() - INTERVAL 6 DAY + INTERVAL 9 HOUR,
        CURDATE() - INTERVAL 6 DAY + INTERVAL 9 HOUR + INTERVAL 2 MINUTE, '预充值');
SET @recharge_111 = LAST_INSERT_ID();

INSERT INTO ordered (orders, record_id, user_id, film_id, img, cinema_id, room_id, appointment,
                     total, unit_price, number, status, start, seat, create_time,
                     pending_timeout_at, pay_time, pay_amount, refund_time, refund_amount, pickup_code)
VALUES ('HORD202610170001', @rec_p6, @u111, 12, (SELECT img FROM film WHERE id = 12), @cinema_main, @room_main,
        CONCAT('场次ID:', @rec_p6), 42.00, 42.00, 1, '已取票',
        CURDATE() - INTERVAL 6 DAY + INTERVAL 19 HOUR, '9排1座',
        CURDATE() - INTERVAL 6 DAY + INTERVAL 18 HOUR,
        CURDATE() - INTERVAL 6 DAY + INTERVAL 18 HOUR + INTERVAL 5 MINUTE,
        CURDATE() - INTERVAL 6 DAY + INTERVAL 18 HOUR + INTERVAL 1 MINUTE, 42.00, NULL, NULL, 'HH01-AAAA'),
       ('HORD202610180001', @rec_p5, @u111, 12, (SELECT img FROM film WHERE id = 12), @cinema_main, @room_main,
        CONCAT('场次ID:', @rec_p5), 84.00, 42.00, 2, '已取票',
        CURDATE() - INTERVAL 5 DAY + INTERVAL 19 HOUR, '9排2座,9排3座',
        CURDATE() - INTERVAL 5 DAY + INTERVAL 18 HOUR,
        CURDATE() - INTERVAL 5 DAY + INTERVAL 18 HOUR + INTERVAL 5 MINUTE,
        CURDATE() - INTERVAL 5 DAY + INTERVAL 18 HOUR + INTERVAL 1 MINUTE, 84.00, NULL, NULL, 'HH02-BBBB'),
       ('HORD202610190000', @rec_p4, @u111, 12, (SELECT img FROM film WHERE id = 12), @cinema_main, @room_main,
        CONCAT('场次ID:', @rec_p4), 42.00, 42.00, 1, '已取票',
        CURDATE() - INTERVAL 4 DAY + INTERVAL 19 HOUR, '9排4座',
        CURDATE() - INTERVAL 4 DAY + INTERVAL 17 HOUR,
        CURDATE() - INTERVAL 4 DAY + INTERVAL 17 HOUR + INTERVAL 5 MINUTE,
        CURDATE() - INTERVAL 4 DAY + INTERVAL 17 HOUR + INTERVAL 2 MINUTE, 42.00, NULL, NULL, 'HH06-GGGG'),
       ('HORD202610190001', @rec_p4, @u111, 12, (SELECT img FROM film WHERE id = 12), @cinema_main, @room_main,
        CONCAT('场次ID:', @rec_p4), 84.00, 42.00, 2, '已退票',
        CURDATE() - INTERVAL 4 DAY + INTERVAL 19 HOUR, '9排5座,9排6座',
        CURDATE() - INTERVAL 4 DAY + INTERVAL 17 HOUR,
        CURDATE() - INTERVAL 4 DAY + INTERVAL 17 HOUR + INTERVAL 5 MINUTE,
        CURDATE() - INTERVAL 4 DAY + INTERVAL 17 HOUR + INTERVAL 3 MINUTE, 84.00,
        CURDATE() - INTERVAL 4 DAY + INTERVAL 17 HOUR + INTERVAL 30 MINUTE, 84.00, NULL),
       ('HORD202610200006', @rec_d, @u111, 12, (SELECT img FROM film WHERE id = 12), @cinema_main, @room_main,
        CONCAT('场次ID:', @rec_d), 84.00, 42.00, 2, '已取票',
        CURDATE() - INTERVAL 3 DAY + INTERVAL 19 HOUR, '9排7座,9排8座',
        CURDATE() - INTERVAL 3 DAY + INTERVAL 17 HOUR + INTERVAL 20 MINUTE,
        CURDATE() - INTERVAL 3 DAY + INTERVAL 17 HOUR + INTERVAL 25 MINUTE,
        CURDATE() - INTERVAL 3 DAY + INTERVAL 17 HOUR + INTERVAL 22 MINUTE, 84.00, NULL, NULL, 'HH03-CCCC'),
       ('HORD202610210001', @rec_p2, @u111, 12, (SELECT img FROM film WHERE id = 12), @cinema_main, @room_main,
        CONCAT('场次ID:', @rec_p2), 42.00, 42.00, 1, '已取票',
        CURDATE() - INTERVAL 2 DAY + INTERVAL 19 HOUR, '9排9座',
        CURDATE() - INTERVAL 2 DAY + INTERVAL 18 HOUR,
        CURDATE() - INTERVAL 2 DAY + INTERVAL 18 HOUR + INTERVAL 5 MINUTE,
        CURDATE() - INTERVAL 2 DAY + INTERVAL 18 HOUR + INTERVAL 1 MINUTE, 42.00, NULL, NULL, 'HH04-DDDD'),
       ('HORD202610220001', @rec_p1, @u111, 12, (SELECT img FROM film WHERE id = 12), @cinema_main, @room_main,
        CONCAT('场次ID:', @rec_p1), 84.00, 42.00, 2, '已取票',
        CURDATE() - INTERVAL 1 DAY + INTERVAL 19 HOUR, '9排10座,9排11座',
        CURDATE() - INTERVAL 1 DAY + INTERVAL 18 HOUR,
        CURDATE() - INTERVAL 1 DAY + INTERVAL 18 HOUR + INTERVAL 5 MINUTE,
        CURDATE() - INTERVAL 1 DAY + INTERVAL 18 HOUR + INTERVAL 1 MINUTE, 84.00, NULL, NULL, 'HH05-EEEE');

INSERT INTO ordered (orders, record_id, user_id, film_id, img, cinema_id, room_id, appointment,
                     total, unit_price, number, status, start, seat, create_time,
                     pending_timeout_at, pay_time, pay_amount, refund_time, refund_amount, pickup_code)
VALUES ('HORD202610210000', @rec_p2, @u111, 12, (SELECT img FROM film WHERE id = 12), @cinema_main, @room_main,
        CONCAT('场次ID:', @rec_p2), 84.00, 42.00, 2, '已取票',
        CURDATE() - INTERVAL 2 DAY + INTERVAL 19 HOUR, '12排12座,12排13座',
        CURDATE() - INTERVAL 2 DAY + INTERVAL 18 HOUR,
        CURDATE() - INTERVAL 2 DAY + INTERVAL 18 HOUR + INTERVAL 5 MINUTE,
        CURDATE() - INTERVAL 2 DAY + INTERVAL 18 HOUR + INTERVAL 1 MINUTE, 84.00, NULL, NULL, 'HH07-FFFF');
-- 流水：充值 → 6 笔购票 → 1 笔退票，按时间顺序写，余额逐笔由「起点 + 充值 − 已付 + 已退」推出
INSERT INTO fund_flow (user_id, source, change_amount, balance_before, balance_after, related_id, create_time)
VALUES (@u111, '充值', 200.00, @u111_balance0, @u111_balance0 + 200.00, @recharge_111,
        CURDATE() - INTERVAL 6 DAY + INTERVAL 9 HOUR + INTERVAL 2 MINUTE);

INSERT INTO fund_flow (user_id, source, change_amount, balance_before, balance_after, related_id, create_time)
SELECT @u111, '购票', -o.total,
       @u111_balance0 + 200.00
           - (SELECT COALESCE(SUM(p.total), 0) FROM ordered p
              WHERE p.orders LIKE 'HORD%' AND p.status <> '已退票' AND p.id < o.id)
           + (SELECT COALESCE(SUM(p.refund_amount), 0) FROM ordered p
              WHERE p.orders LIKE 'HORD%' AND p.status = '已退票' AND p.id < o.id),
       @u111_balance0 + 200.00
           - (SELECT COALESCE(SUM(p.total), 0) FROM ordered p
              WHERE p.orders LIKE 'HORD%' AND p.status <> '已退票' AND p.id <= o.id)
           + (SELECT COALESCE(SUM(p.refund_amount), 0) FROM ordered p
              WHERE p.orders LIKE 'HORD%' AND p.status = '已退票' AND p.id <= o.id),
       o.id, o.pay_time
FROM ordered o WHERE o.orders LIKE 'HORD%' AND o.status <> '已退票'
ORDER BY o.pay_time;

INSERT INTO fund_flow (user_id, source, change_amount, balance_before, balance_after, related_id, create_time)
SELECT @u111, '退票', o.refund_amount,
       @u111_balance0 + 200.00
           - (SELECT COALESCE(SUM(p.total), 0) FROM ordered p
              WHERE p.orders LIKE 'HORD%' AND p.status <> '已退票' AND p.id <= o.id)
           + (SELECT COALESCE(SUM(p.refund_amount), 0) FROM ordered p
              WHERE p.orders LIKE 'HORD%' AND p.status = '已退票' AND p.id < o.id),
       @u111_balance0 + 200.00
           - (SELECT COALESCE(SUM(p.total), 0) FROM ordered p
              WHERE p.orders LIKE 'HORD%' AND p.status <> '已退票' AND p.id <= o.id)
           + (SELECT COALESCE(SUM(p.refund_amount), 0) FROM ordered p
              WHERE p.orders LIKE 'HORD%' AND p.status = '已退票' AND p.id <= o.id),
       o.id, o.refund_time
FROM ordered o WHERE o.orders = 'HORD202610190001';


-- ------------------------------------------------------------
-- 6. 新用户（s01..s10）的一周交易
--    每人 7 笔：-6..-1 天各一笔（-4 已退票、-5 已取消、其余已取票），
--    第 7 笔是明天《千与千寻》的待取票单。
--    座位：用户 1..10 用 1..10 排，同场次不同用户互不重叠。
-- ------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_user_tx;
CREATE TEMPORARY TABLE tmp_user_tx (
  uid INT, seq INT, idx INT, offs INT, cnt INT, status VARCHAR(10),
  seats VARCHAR(200), rec_id INT, price DECIMAL(10,2), amount DECIMAL(10,2)
);

-- 7 笔 = -6..-1 各一天 + 明天一场：idx 1..7
--   idx1 -6 已取票 / idx2 -5 已取票 / idx3 -4 已取票 / idx4 -3 已取票
--   idx5 -2 已取消（不产生票房，演示废单）/ idx6 -1 已取票 / idx7 明天 待取票
INSERT INTO tmp_user_tx (uid, seq, idx, offs, cnt, status)
SELECT u.id, u.seq, p.idx, p.idx - 7,
       CASE WHEN (u.seq + p.idx) % 5 = 0 THEN 1
            WHEN (u.seq + p.idx) % 3 = 0 THEN 2
            ELSE 1 + ((u.seq * 7 + p.idx * 3) % 3) END,
       CASE p.idx WHEN 5 THEN '已取消'
                  WHEN 7 THEN '待取票' ELSE '已取票' END
FROM (SELECT id, CAST(SUBSTRING(username, 2) AS UNSIGNED) seq FROM user WHERE username LIKE 's%') u
JOIN (SELECT 1 idx UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
      UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7) p;

-- 座位按「场次内第几个用户」依次占第 1、2… 排（每人最多 3 座）：
-- 不按用户名固定排号 —— 有的厅只有 6 排，固定排号会撞。
-- MySQL 临时表不能在同一条语句里被引用两次：整表灌一遍带 ranked 的副本，再回填
ALTER TABLE tmp_user_tx ADD COLUMN seat_rank INT;
UPDATE tmp_user_tx SET seat_rank = 1 + (SELECT COUNT(*) FROM (SELECT 1) d WHERE 1 = 0);
DROP TEMPORARY TABLE IF EXISTS tmp_rank;
CREATE TEMPORARY TABLE tmp_rank AS
SELECT uid, idx, ROW_NUMBER() OVER (PARTITION BY idx ORDER BY uid) AS rk FROM tmp_user_tx;
UPDATE tmp_user_tx t JOIN tmp_rank k ON k.uid = t.uid AND k.idx = t.idx SET t.seat_rank = k.rk;
DROP TEMPORARY TABLE tmp_rank;

UPDATE tmp_user_tx t
SET t.rec_id = CASE t.idx
                 WHEN 1 THEN @rec_p6 WHEN 2 THEN @rec_p5 WHEN 3 THEN @rec_p4
                 WHEN 4 THEN @rec_d WHEN 5 THEN @rec_p2 WHEN 6 THEN @rec_p1
                 ELSE @rec_future END;

UPDATE tmp_user_tx t JOIN record r ON r.id = t.rec_id
SET t.price = r.price, t.amount = r.price * t.cnt;

INSERT INTO ordered (orders, record_id, user_id, film_id, img, cinema_id, room_id, appointment,
                     total, unit_price, number, status, start, seat, create_time,
                     pending_timeout_at, pay_time, pay_amount, refund_time, refund_amount, pickup_code)
SELECT CONCAT('SORD', DATE_FORMAT(CURDATE() + INTERVAL t.offs DAY, '%Y%m%d'),
              LPAD(t.seq, 2, '0'), LPAD(t.idx, 2, '0')),
       t.rec_id, t.uid,
       (SELECT film_id FROM record WHERE id = t.rec_id),
       (SELECT img FROM film WHERE id = (SELECT film_id FROM record WHERE id = t.rec_id)),
       @cinema_main, @room_main,
       CONCAT('场次ID:', t.rec_id),
       t.amount, t.price, t.cnt, t.status,
       CURDATE() + INTERVAL t.offs DAY + INTERVAL 19 HOUR,
       t.seats,
       CURDATE() + INTERVAL t.offs DAY + INTERVAL 17 HOUR,
       CASE WHEN t.status = '已取消'
            THEN CURDATE() + INTERVAL t.offs DAY + INTERVAL 17 HOUR + INTERVAL 5 MINUTE END,
       CASE WHEN t.status <> '已取消'
            THEN CURDATE() + INTERVAL t.offs DAY + INTERVAL 17 HOUR + INTERVAL 2 MINUTE END,
       CASE WHEN t.status <> '已取消' THEN t.amount END,
       CASE WHEN t.status = '已退票'
            THEN CURDATE() + INTERVAL t.offs DAY + INTERVAL 17 HOUR + INTERVAL 30 MINUTE END,
       CASE WHEN t.status = '已退票' THEN t.amount END,
       CASE WHEN t.status IN ('已取票', '待取票')
            THEN CONCAT('DD', LPAD(t.seq, 2, '0'), 'A-', LPAD(t.idx, 2, '00')) END
FROM tmp_user_tx t
ORDER BY t.uid, t.idx;

-- 每人一笔充值
INSERT INTO recharge_order (recharge_no, user_id, amount, status, create_time, finish_time, remark)
SELECT CONCAT('SR', LPAD(t.seq, 2, '0'), '0001'), t.uid, 800.00, '已完成',
       CURDATE() - INTERVAL 7 DAY + INTERVAL 10 HOUR,
       CURDATE() - INTERVAL 7 DAY + INTERVAL 10 HOUR + INTERVAL 1 MINUTE, '充值'
FROM (SELECT DISTINCT uid, seq FROM tmp_user_tx) t;

INSERT INTO fund_flow (user_id, source, change_amount, balance_before, balance_after, related_id, create_time)
SELECT r.user_id, '充值', 800.00, 0.00, 800.00, r.id,
       CURDATE() - INTERVAL 7 DAY + INTERVAL 10 HOUR + INTERVAL 1 MINUTE
FROM recharge_order r WHERE r.recharge_no LIKE 'SR%';

-- 逐笔购票/退票：余额 = 充值 800 − 至今已付 + 至今已退（按支付时间顺序）
INSERT INTO fund_flow (user_id, source, change_amount, balance_before, balance_after, related_id, create_time)
SELECT o.user_id, '购票', -o.total,
       800.00
         - (SELECT COALESCE(SUM(p.total), 0) FROM ordered p
            WHERE p.user_id = o.user_id AND p.orders LIKE 'SORD%'
              AND p.status <> '已退票' AND p.status <> '已取消' AND p.pay_time < o.pay_time)
         + (SELECT COALESCE(SUM(p.refund_amount), 0) FROM ordered p
            WHERE p.user_id = o.user_id AND p.orders LIKE 'SORD%'
              AND p.status = '已退票' AND p.refund_time <= o.pay_time),
       800.00
         - (SELECT COALESCE(SUM(p.total), 0) FROM ordered p
            WHERE p.user_id = o.user_id AND p.orders LIKE 'SORD%'
              AND p.status <> '已退票' AND p.status <> '已取消' AND p.pay_time <= o.pay_time)
         + (SELECT COALESCE(SUM(p.refund_amount), 0) FROM ordered p
            WHERE p.user_id = o.user_id AND p.orders LIKE 'SORD%'
              AND p.status = '已退票' AND p.refund_time <= o.pay_time),
       o.id, o.pay_time
FROM ordered o
WHERE o.orders LIKE 'SORD%' AND o.status <> '已退票' AND o.status <> '已取消'
ORDER BY o.user_id, o.pay_time;

INSERT INTO fund_flow (user_id, source, change_amount, balance_before, balance_after, related_id, create_time)
SELECT o.user_id, '退票', o.refund_amount,
       800.00
         - (SELECT COALESCE(SUM(p.total), 0) FROM ordered p
            WHERE p.user_id = o.user_id AND p.orders LIKE 'SORD%'
              AND p.status <> '已退票' AND p.status <> '已取消' AND p.pay_time <= o.refund_time)
         + (SELECT COALESCE(SUM(p.refund_amount), 0) FROM ordered p
            WHERE p.user_id = o.user_id AND p.orders LIKE 'SORD%'
              AND p.status = '已退票' AND p.refund_time < o.refund_time),
       800.00
         - (SELECT COALESCE(SUM(p.total), 0) FROM ordered p
            WHERE p.user_id = o.user_id AND p.orders LIKE 'SORD%'
              AND p.status <> '已退票' AND p.status <> '已取消' AND p.pay_time <= o.refund_time)
         + (SELECT COALESCE(SUM(p.refund_amount), 0) FROM ordered p
            WHERE p.user_id = o.user_id AND p.orders LIKE 'SORD%'
              AND p.status = '已退票' AND p.refund_time <= o.refund_time),
       o.id, o.refund_time
FROM ordered o
WHERE o.orders LIKE 'SORD%' AND o.status = '已退票';

-- 新用户取票码：每条订单唯一（DD01A-0101 这类）
UPDATE ordered
SET pickup_code = CONCAT('DD', SUBSTRING(orders, 13, 2), 'A-', SUBSTRING(orders, 15, 2))
WHERE orders LIKE 'SORD%' AND status IN ('已取票', '待取票');

DROP TEMPORARY TABLE tmp_user_tx;

-- ------------------------------------------------------------
-- 7. 自助取票（walk-through，不写库）
--    历史场次均已结束，HH* / DD* 会报「已取出 / 已过期」；
--    要演示核销成功，用 §4 的未来场次当场买票。
-- ------------------------------------------------------------

-- ------------------------------------------------------------
-- 7b. u1/u2 与 111 的观影记录（评分资格：必须真有「已取票」订单）
--     - u1 看 2/3/8/17/44/45，u2 看 2/3/15/25/30，111 看 8
--     - 场次挂在本脚本「未来 7 天」的真实排片上，状态已取票（历史观影片）
-- ------------------------------------------------------------
SET @room_2 = (SELECT id FROM room WHERE name = '2号厅' AND cinema_id = @cinema_main);
SET @room_3 = (SELECT id FROM room WHERE name = '3号厅' AND cinema_id = @cinema_main);

DROP TEMPORARY TABLE IF EXISTS tmp_watch;
CREATE TEMPORARY TABLE tmp_watch (
  seq INT, uid INT, film_id INT, price DECIMAL(10,2), seat_row INT
);
INSERT INTO tmp_watch (seq, uid, film_id, price, seat_row)
VALUES (1,  @u1,  2, 55.00, 1),
       (2,  @u1,  3, 55.00, 2),
       (3,  @u1,  8, 45.00, 3),
       (4,  @u1, 17, 48.00, 4),
       (5,  @u1, 44, 42.00, 5),
       (6,  @u1, 45, 58.00, 6),
       (7,  @u2,  2, 55.00, 7),
       (8,  @u2,  3, 55.00, 8),
       (9,  @u2, 15, 45.00, 9),
       (10, @u2, 25, 48.00, 10),
       (11, @u2, 30, 42.00, 11),
       (12, @u111, 8, 45.00, 12),
       (13, @u1, 12, 42.00, 13),
       (14, @u2, 12, 42.00, 14);

INSERT INTO ordered (orders, record_id, user_id, film_id, img, cinema_id, room_id, appointment,
                     total, unit_price, number, status, start, seat, create_time,
                     pending_timeout_at, pay_time, pay_amount, refund_time, refund_amount, pickup_code)
SELECT CONCAT('XORD', LPAD(w.seq, 3, '0')), r.id, w.uid, w.film_id,
       (SELECT img FROM film WHERE id = w.film_id), @cinema_main, r.room_id,
       CONCAT('场次ID:', r.id), w.price, w.price, 1, '已取票',
       r.start, CONCAT(w.seat_row, '排1座'),
       CURDATE() - INTERVAL 9 DAY, NULL, CURDATE() - INTERVAL 9 DAY, w.price,
       NULL, NULL, CONCAT('XA', LPAD(w.seq, 2, '0'), '-', LPAD(w.seq, 4, '0'))
FROM tmp_watch w
JOIN (SELECT id, film_id, room_id, start,
             ROW_NUMBER() OVER (PARTITION BY film_id ORDER BY start) rn
      FROM record WHERE start > CURDATE()) r
  ON r.film_id = w.film_id AND r.rn = 1;

DROP TEMPORARY TABLE tmp_watch;

-- ------------------------------------------------------------
-- 8. 评价与点赞
--    前置条件与 MarkService.add 一致：必须已有该片的「已取票」订单
-- ------------------------------------------------------------
INSERT INTO mark (user_id, film_id, score, comment)
VALUES (@u1, 12, 9.5, '重看还是经典，天台那场戏无可替代。'),
       (@u1, 2, 9.8, '希望是个好东西，大概是世界上最好的东西。'),
       (@u1, 3, 9.2, '宫崎骏最好的作品，没有之一。'),
       (@u1, 8, 9.0, '现实题材的国产片里最有力量的一部。'),
       (@u1, 17, 8.5, '国漫的工业化水平又上了一层。'),
       (@u1, 45, 9.0, '战争场面拍得很克制，反而更震撼。'),
       (@u1, 44, 8.2, '笑点密集，春节档该有的样子。'),
       (@u2, 12, 8.0, '港片巅峰，配乐一响就回到那个年代。'),
       (@u2, 3, 9.0, '千寻的成长线写得太好了。'),
       (@u2, 2, 9.5, '关于自由和希望的老派叙事。'),
       (@u2, 25, 8.3, '东方神话的视觉效果终于撑得起故事。'),
       (@u2, 30, 8.8, '信仰在战火里的重量。'),
       (@u2, 15, 7.8, '群像戏调度不错。'),
       (@u111, 12, 9.0, '修复版重看，细节比记忆里还多。'),
       (@u111, 8, 9.3, '药神之后徐峥的又一高峰。');

-- 新用户评价：每人 2 条（《无间道》+ 部分用户《千与千寻》），只评自己已取票的片
INSERT INTO mark (user_id, film_id, score, comment)
SELECT u.id, 12, 7.5 + (u.seq % 4) * 0.5, '第三次看，依然被结尾打动。'
FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY id) seq FROM user WHERE username LIKE 's%') u;

INSERT INTO mark (user_id, film_id, score, comment)
SELECT u.id, 3, 8.0 + (u.seq % 3) * 0.5, '带孩子来看的，画面很有想象力。'
FROM (SELECT id, CAST(SUBSTRING(username, 2) AS UNSIGNED) seq FROM user WHERE username LIKE 's%') u
WHERE u.seq % 2 = 0
  AND EXISTS (SELECT 1 FROM ordered o WHERE o.user_id = u.id AND o.film_id = 3 AND o.status = '已取票');

INSERT INTO mark_like (mark_id, user_id)
SELECT m.id, u.id
FROM mark m
JOIN user u ON u.username IN ('s01', 's02', 's03', 'u1', 'u2')
WHERE m.film_id = 12 AND m.user_id <> u.id;

-- ------------------------------------------------------------
-- 9. 派生数据回算（与后端一致，不手工写死）
-- ------------------------------------------------------------
-- 本脚本建的 12 个账号都有流水；其它既有账号（可能没有流水）保持原余额
UPDATE user u SET balance = (
    SELECT f.balance_after FROM fund_flow f WHERE f.user_id = u.id ORDER BY f.id DESC LIMIT 1)
WHERE (u.username LIKE 's%' OR u.username IN ('u1', 'u2', '111'))
  AND EXISTS (SELECT 1 FROM fund_flow f WHERE f.user_id = u.id);

UPDATE film f
SET score = (SELECT ROUND(AVG(m.score), 1) FROM mark m WHERE m.film_id = f.id)
WHERE f.id IN (2, 3, 8, 12, 15, 17, 25, 30, 44, 45);

-- ------------------------------------------------------------
-- 10. 自检
-- ------------------------------------------------------------
SELECT '影院' AS 项目, COUNT(*) AS 数量 FROM cinema
UNION ALL SELECT '其中有影厅的', COUNT(DISTINCT cinema_id) FROM room
UNION ALL SELECT '影厅', COUNT(*) FROM room
UNION ALL SELECT '排片（总）', COUNT(*) FROM record
UNION ALL SELECT '其中未来 7 天', COUNT(*) FROM record WHERE start >= CURDATE()
UNION ALL SELECT '有排片的影片', COUNT(DISTINCT film_id) FROM record
UNION ALL SELECT '有排片的影院', COUNT(DISTINCT cinema_id) FROM record
UNION ALL SELECT '用户', COUNT(*) FROM user
UNION ALL SELECT '有订单的用户', COUNT(DISTINCT user_id) FROM ordered
UNION ALL SELECT '订单', COUNT(*) FROM ordered
UNION ALL SELECT '资金流水', COUNT(*) FROM fund_flow
UNION ALL SELECT '评价', COUNT(*) FROM mark
UNION ALL SELECT '有点评的用户', COUNT(DISTINCT user_id) FROM mark
UNION ALL SELECT '有评分的影片', COUNT(DISTINCT film_id) FROM mark
UNION ALL SELECT '点赞', COUNT(*) FROM mark_like
UNION ALL SELECT '公告', COUNT(*) FROM notice
UNION ALL SELECT '预告片', COUNT(*) FROM film WHERE video IS NOT NULL AND video <> '';

SELECT DATE_FORMAT(pay_time, '%Y-%m-%d') AS 日期, COUNT(*) AS 订单数, SUM(total) AS 票房
FROM ordered
WHERE pay_time >= CURDATE() - INTERVAL 6 DAY AND pay_time < CURDATE() + INTERVAL 1 DAY
  AND status IN ('待取票', '已取票')
GROUP BY DATE_FORMAT(pay_time, '%Y-%m-%d')
ORDER BY 日期;

SELECT '余额不等于末笔流水' AS 校验, COUNT(*) AS 异常 FROM user u
WHERE (u.username LIKE 's%' OR u.username IN ('u1', 'u2', '111'))
  AND u.balance <> (SELECT f.balance_after FROM fund_flow f
                    WHERE f.user_id = u.id ORDER BY f.id DESC LIMIT 1)
UNION ALL SELECT '订单金额与单价不符', COUNT(*) FROM ordered
WHERE total <> CAST(unit_price * number AS DECIMAL(10,2))
UNION ALL SELECT '已支付订单无取票码', COUNT(*) FROM ordered
WHERE status IN ('待取票', '已取票') AND pickup_code IS NULL
UNION ALL SELECT '评价人无已取票订单', COUNT(*) FROM mark m
WHERE NOT EXISTS (SELECT 1 FROM ordered o
                  WHERE o.user_id = m.user_id AND o.film_id = m.film_id AND o.status = '已取票')
UNION ALL SELECT '同场次座位重复', COUNT(*) FROM (
    SELECT seat, COUNT(*) AS c FROM (
        SELECT o.id, o.record_id,
               TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(o.seat, ',', n.n), ',', -1)) AS seat
        FROM ordered o
        JOIN (SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
              UNION ALL SELECT 5 UNION ALL SELECT 6) n
          ON n.n <= 1 + (LENGTH(o.seat) - LENGTH(REPLACE(o.seat, ',', '')))
        WHERE o.status NOT IN ('已取消', '已退票')
    ) seat_rows GROUP BY record_id, seat HAVING COUNT(*) > 1
) dup
UNION ALL SELECT '同厅同期排片重叠', COUNT(*) FROM record a JOIN record b
  ON a.room_id = b.room_id AND a.id < b.id
 AND a.start < DATE_ADD(b.start, INTERVAL 120 MINUTE)
 AND b.start < DATE_ADD(a.start, INTERVAL 120 MINUTE)
WHERE a.status = '正常' AND b.status = '正常'
UNION ALL SELECT '近 7 日有 0 票房的日期', 7 - (
    SELECT COUNT(DISTINCT DATE(pay_time)) FROM ordered
    WHERE pay_time >= CURDATE() - INTERVAL 6 DAY AND pay_time < CURDATE() + INTERVAL 1 DAY
      AND status IN ('待取票', '已取票'));