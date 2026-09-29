-- ============================================================
-- 影院购票管理系统 (Cinema Ticket Management System)
-- 数据库表结构 — xm-film
-- ============================================================
-- 使用说明:
--   1. 创建数据库: CREATE DATABASE `xm-film` DEFAULT CHARACTER SET utf8mb4;
--   2. 选择数据库: USE `xm-film`;
--   3. 执行本文件: SOURCE schema.sql;
--   4. 导入数据:   SOURCE data.sql;
-- ============================================================

-- ---------------------------
-- 1. 管理员表 (admin)
-- ---------------------------
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin` (
    `id`       INT          AUTO_INCREMENT PRIMARY KEY COMMENT '管理员ID',
    `username` VARCHAR(50)  NOT NULL UNIQUE           COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL                    COMMENT '密码',
    `role`     VARCHAR(20)  DEFAULT 'ADMIN'             COMMENT '角色',
    `name`     VARCHAR(50)                              COMMENT '姓名',
    `avatar`   VARCHAR(500)                             COMMENT '头像URL',
    `phone`    VARCHAR(20)                              COMMENT '手机号',
    `email`    VARCHAR(100)                             COMMENT '邮箱'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

-- ---------------------------
-- 2. 用户表 (user)
-- ---------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`       INT          AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    `username` VARCHAR(50)  NOT NULL UNIQUE           COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL                    COMMENT '密码',
    `name`     VARCHAR(50)                              COMMENT '姓名',
    `role`     VARCHAR(20)  DEFAULT 'USER'              COMMENT '角色',
    `avatar`   VARCHAR(500)                             COMMENT '头像URL',
    `phone`    VARCHAR(20)                              COMMENT '手机号',
    `email`    VARCHAR(100)                             COMMENT '邮箱',
    `balance`  DECIMAL(10,2) NOT NULL DEFAULT 0.00       COMMENT '账户余额（元，资金唯一可信来源）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ---------------------------
-- 3. 影院表 (cinema)
-- ---------------------------
DROP TABLE IF EXISTS `cinema`;
CREATE TABLE `cinema` (
    `id`           INT          AUTO_INCREMENT PRIMARY KEY COMMENT '影院ID',
    `username`     VARCHAR(50)  NOT NULL UNIQUE           COMMENT '影院账号',
    `password`     VARCHAR(100) NOT NULL                    COMMENT '密码',
    `avatar`       VARCHAR(500)                             COMMENT '影院头像',
    `role`         VARCHAR(20)  DEFAULT 'CINEMA'            COMMENT '角色',
    `name`         VARCHAR(100)                             COMMENT '影院名称',
    `phone`        VARCHAR(20)                              COMMENT '联系电话',
    `email`        VARCHAR(100)                             COMMENT '邮箱',
    `address`      VARCHAR(200)                             COMMENT '影院地址',
    `leader`       VARCHAR(50)                              COMMENT '负责人',
    `code`         VARCHAR(50)                              COMMENT '统一社会信用代码',
    `status`       VARCHAR(20)  DEFAULT '未审核'            COMMENT '审核状态',
    `certificate`  VARCHAR(500)                             COMMENT '资质证书URL',
    `description`  TEXT                                     COMMENT '影院简介'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='影院表';

-- ---------------------------
-- 4. 区域/产地表 (area)
-- ---------------------------
DROP TABLE IF EXISTS `area`;
CREATE TABLE `area` (
    `id`    INT         AUTO_INCREMENT PRIMARY KEY COMMENT '区域ID',
    `title` VARCHAR(50) NOT NULL                   COMMENT '区域名称'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='区域/产地表（如中国大陆、美国、日本）';

-- ---------------------------
-- 5. 电影类型表 (type)
-- ---------------------------
DROP TABLE IF EXISTS `type`;
CREATE TABLE `type` (
    `id`    INT         AUTO_INCREMENT PRIMARY KEY COMMENT '类型ID',
    `title` VARCHAR(50) NOT NULL                   COMMENT '类型名称'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电影类型表（如剧情、喜剧、动作）';

-- ---------------------------
-- 6. 电影表 (film)
-- ---------------------------
DROP TABLE IF EXISTS `film`;
CREATE TABLE `film` (
    `id`        INT           AUTO_INCREMENT PRIMARY KEY COMMENT '电影ID',
    `title`     VARCHAR(100)  NOT NULL                    COMMENT '电影中文标题',
    `english`   VARCHAR(200)                              COMMENT '电影英文标题',
    `start`     DATE                                      COMMENT '上映日期',
    `time`      SMALLINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '片长（分钟）',
    `language`  VARCHAR(50)                               COMMENT '语言',
    `resolution` VARCHAR(50)                              COMMENT '版本/分辨率',
    `content`   TEXT                                      COMMENT '剧情简介',
    `img`       VARCHAR(500)                              COMMENT '海报URL',
    `employee`  VARCHAR(100)                              COMMENT '维护人员',
    `area_id`   INT                                       COMMENT '产地ID（关联area表）',
    `status`    VARCHAR(20)   DEFAULT '待上映'             COMMENT '状态（已上映/待上映）',
    `score`     DECIMAL(3,1)  DEFAULT 0.0                 COMMENT '评分（由 mark.score 回写；无评价时保留基线值）',
    `box_office` DECIMAL(10,1) DEFAULT 0.0                COMMENT '已废弃：票房改由 ordered 实时聚合（见 FilmMapper.filmRevenueJoin），此列恒为 0',
    `actor_id`   INT                                      COMMENT '关联演员ID',
    `video`     VARCHAR(500)                              COMMENT '预告片URL',
    INDEX idx_area_id (area_id),
    INDEX idx_status (status),
    INDEX idx_start (start),
    FULLTEXT INDEX idx_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电影表';

-- ---------------------------
-- 7. 演员表 (actor)
-- ---------------------------
DROP TABLE IF EXISTS `actor`;
CREATE TABLE `actor` (
    `id`      INT          AUTO_INCREMENT PRIMARY KEY COMMENT '演员ID',
    `film_id` INT                                       COMMENT '关联电影ID',
    `title`   VARCHAR(100) NOT NULL                   COMMENT '所属电影标题',
    `img`     VARCHAR(500)                            COMMENT '电影海报URL',
    `actor`   VARCHAR(50)                             COMMENT '演员姓名',
    `figure`  VARCHAR(50)                             COMMENT '饰演角色名',
    `picture` VARCHAR(500)                            COMMENT '演员照片URL',
    `grade`   VARCHAR(20)                             COMMENT '演员级别（如主演、二级演员）',
    `video`   VARCHAR(500)                            COMMENT '相关视频URL',
    FOREIGN KEY (film_id) REFERENCES film(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='演员表';

-- ---------------------------
-- 8. 电影类型关联表 (film_type)
-- ---------------------------
DROP TABLE IF EXISTS `film_type`;
CREATE TABLE `film_type` (
    `film_id` INT NOT NULL COMMENT '电影ID',
    `type_id` INT NOT NULL COMMENT '类型ID',
    PRIMARY KEY (film_id, type_id),
    INDEX idx_type_id (type_id),
    FOREIGN KEY (film_id) REFERENCES film(id) ON DELETE CASCADE,
    FOREIGN KEY (type_id) REFERENCES type(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电影-类型关联表';

-- ---------------------------
-- 9. 放映厅表 (room)
-- ---------------------------
DROP TABLE IF EXISTS `room`;
CREATE TABLE `room` (
    `id`    INT          AUTO_INCREMENT PRIMARY KEY COMMENT '放映厅ID',
    `cinema_id` INT                                    COMMENT '所属影院ID',
    `title` VARCHAR(100) NOT NULL                   COMMENT '所属影院名称',
    `name`  VARCHAR(50)  NOT NULL                   COMMENT '放映厅名称（如一号厅）',
    `seat_rows` INT      NOT NULL DEFAULT 8         COMMENT '座位行数（选座图纵向格数）',
    `seat_cols` INT      NOT NULL DEFAULT 8         COMMENT '座位列数（选座图横向格数）',
    INDEX idx_room_cinema_id (cinema_id),
    FOREIGN KEY (cinema_id) REFERENCES cinema(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='放映厅表';

-- ---------------------------
-- 10. 放映记录表 (record)
-- ---------------------------
DROP TABLE IF EXISTS `record`;
CREATE TABLE `record` (
    `id`        INT           AUTO_INCREMENT PRIMARY KEY COMMENT '放映记录ID',
    `cinema_id` INT           NOT NULL                    COMMENT '影院ID',
    `room_id`   INT           NOT NULL                    COMMENT '放映厅ID',
    `film_id`   INT           NOT NULL                    COMMENT '电影ID（排片必须关联影片，否则前台不可见）',
    `title`     VARCHAR(100)  NOT NULL                    COMMENT '电影名称',
    `start`     DATETIME                                 COMMENT '放映时间（可购票性由该时间派生）',
    `price`     DECIMAL(10,2) DEFAULT 0.00                COMMENT '票价（元）',
    `status`    VARCHAR(20)   DEFAULT '正常'              COMMENT '售卖状态（正常/停售），展示状态由 start 派生',
    INDEX idx_record_cinema_id (cinema_id),
    INDEX idx_record_room_id (room_id),
    INDEX idx_record_film_id (film_id),
    INDEX idx_record_start (start),
    -- 排片是订单的父数据，禁止级联删除，避免删影片/影院/影厅时静默抹掉订单
    FOREIGN KEY (cinema_id) REFERENCES cinema(id) ON DELETE RESTRICT,
    FOREIGN KEY (room_id) REFERENCES room(id) ON DELETE RESTRICT,
    FOREIGN KEY (film_id) REFERENCES film(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='放映记录表（排片/场次）';

-- ---------------------------
-- 11. 订单表 (ordered)
-- ---------------------------
DROP TABLE IF EXISTS `ordered`;
CREATE TABLE `ordered` (
    `id`          INT           AUTO_INCREMENT PRIMARY KEY COMMENT '订单ID',
    `orders`      VARCHAR(50)   NOT NULL                   COMMENT '订单编号',
    `record_id`   INT                                      COMMENT '放映场次ID',
    `user_id`     INT           NOT NULL                   COMMENT '用户ID',
    `film_id`     INT           NOT NULL                   COMMENT '电影ID',
    `img`         VARCHAR(500)                             COMMENT '电影海报URL',
    `cinema_id`   INT           NOT NULL                   COMMENT '影院ID',
    `room_id`     INT           NOT NULL                   COMMENT '放映厅ID',
    `appointment` VARCHAR(100)                             COMMENT '预约场次信息',
    `total`       DECIMAL(10,2) DEFAULT 0.00               COMMENT '订单总金额（元）',
    `unit_price`  DECIMAL(10,2) NULL DEFAULT NULL          COMMENT '单价快照（元，下单时取自场次票价）',
    `number`      INT           DEFAULT 1                  COMMENT '购票数量',
    `status`      VARCHAR(20)   DEFAULT '待取票'           COMMENT '订单状态（待支付/待取票/已取票/已取消/已退票）',
    `start`       DATETIME                                 COMMENT '放映时间',
    `seat`        VARCHAR(200)                             COMMENT '座位信息',
    `create_time`       DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '订单创建时间',
    `pending_timeout_at` DATETIME NULL DEFAULT NULL COMMENT '待支付超时时间',
    `pay_time`      DATETIME      NULL DEFAULT NULL COMMENT '支付时间（资金凭证）',
    `pay_amount`    DECIMAL(10,2) NULL DEFAULT NULL COMMENT '实付金额（元）',
    `refund_time`   DATETIME      NULL DEFAULT NULL COMMENT '退票时间（资金凭证）',
    `refund_amount` DECIMAL(10,2) NULL DEFAULT NULL COMMENT '退款金额（元）',
    UNIQUE KEY uk_ordered_orders (orders),
    INDEX idx_ordered_record_id (record_id),
    INDEX idx_ordered_user_id (user_id),
    INDEX idx_ordered_film_id (film_id),
    INDEX idx_ordered_cinema_id (cinema_id),
    INDEX idx_ordered_room_id (room_id),
    INDEX idx_ordered_status (status),
    INDEX idx_ordered_status_timeout (status, pending_timeout_at),
    -- 订单是交易凭证，禁止级联删除：任何被订单引用的影片/影院/影厅/场次/用户都不能物理删除
    FOREIGN KEY (record_id) REFERENCES record(id) ON DELETE RESTRICT,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE RESTRICT,
    FOREIGN KEY (film_id) REFERENCES film(id) ON DELETE RESTRICT,
    FOREIGN KEY (cinema_id) REFERENCES cinema(id) ON DELETE RESTRICT,
    FOREIGN KEY (room_id) REFERENCES room(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- ---------------------------
-- 12. 评分表 (mark)
-- ---------------------------
DROP TABLE IF EXISTS `mark`;
CREATE TABLE `mark` (
    `id`      INT          AUTO_INCREMENT PRIMARY KEY COMMENT '评分ID',
    `user_id` INT          NOT NULL                   COMMENT '用户ID',
    `film_id` INT          NOT NULL                   COMMENT '电影ID',
    `img`     VARCHAR(500)                            COMMENT '相关图片URL',
    `score`   DECIMAL(3,1)                            COMMENT '评分（0.0~10.0，影片评分的唯一数值来源）',
    `mark`    VARCHAR(255)                            COMMENT '评语',
    INDEX idx_mark_user_id (user_id),
    INDEX idx_mark_film_id (film_id),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (film_id) REFERENCES film(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评分表';

-- ---------------------------
-- 13. 通知公告表 (notice)
-- ---------------------------
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice` (
    `id`      INT          AUTO_INCREMENT PRIMARY KEY COMMENT '通知ID',
    `title`   VARCHAR(100) NOT NULL                   COMMENT '通知标题',
    `content` TEXT                                    COMMENT '通知内容',
    `time`    DATETIME     DEFAULT CURRENT_TIMESTAMP   COMMENT '发布时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知公告表';

-- ---------------------------
-- 14. 视频/预告片表 (video)
-- ---------------------------
DROP TABLE IF EXISTS `video`;
CREATE TABLE `video` (
    `id`      INT          AUTO_INCREMENT PRIMARY KEY COMMENT '视频ID',
    `title`   VARCHAR(100) NOT NULL                   COMMENT '关联电影标题',
    `img`     VARCHAR(500)                            COMMENT '封面图URL',
    `name`    VARCHAR(200)                            COMMENT '视频名称',
    `preview` VARCHAR(500)                            COMMENT '视频预览URL',
    `start`   DATE                                    COMMENT '上映日期'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频/预告片表';

-- ---------------------------
-- 15. 充值单据表 (recharge_order)
-- ---------------------------
-- 充值走「先建单据（处理中）→ 支付网关异步回调 → 已完成/已失败」，
-- 提交单据不改变余额，只有回调成功才入账。演示环境由前端按钮触发回调，
-- 正式环境为第三方支付平台的回调入口，接口形态一致。
DROP TABLE IF EXISTS `recharge_order`;
CREATE TABLE `recharge_order` (
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

-- ---------------------------
-- 16. 资金流水表 (fund_flow)
-- ---------------------------
-- 每一笔余额变动（充值入账/购票扣款/退票退款）都留一条流水，记录变动前余额、
-- 变动后余额、业务来源与关联业务单据ID。只增不改不删，不提供更新/删除入口。
-- related_id 指向 recharge_order.id 或 ordered.id，跨表二选一故不建外键。
DROP TABLE IF EXISTS `fund_flow`;
CREATE TABLE `fund_flow` (
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

SET FOREIGN_KEY_CHECKS = 1;
