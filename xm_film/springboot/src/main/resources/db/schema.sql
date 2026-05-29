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
    `email`    VARCHAR(100)                             COMMENT '邮箱'
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
    `score`     DECIMAL(3,1)  DEFAULT 0.0                 COMMENT '评分',
    `box_office` DECIMAL(10,1) DEFAULT 0.0                COMMENT '票房（万元）',
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
-- 9. 影院-电影关联表 (cinema_film)
-- ---------------------------
DROP TABLE IF EXISTS `cinema_film`;
CREATE TABLE `cinema_film` (
    `id`        INT AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
    `cinema_id` INT NOT NULL                   COMMENT '影院ID',
    `film_id`   INT NOT NULL                   COMMENT '电影ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='影院-电影关联表';

-- ---------------------------
-- 10. 放映厅表 (room)
-- ---------------------------
DROP TABLE IF EXISTS `room`;
CREATE TABLE `room` (
    `id`    INT          AUTO_INCREMENT PRIMARY KEY COMMENT '放映厅ID',
    `title` VARCHAR(100) NOT NULL                   COMMENT '所属影院名称',
    `name`  VARCHAR(50)  NOT NULL                   COMMENT '放映厅名称（如一号厅）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='放映厅表';

-- ---------------------------
-- 11. 放映记录表 (record)
-- ---------------------------
DROP TABLE IF EXISTS `record`;
CREATE TABLE `record` (
    `id`        INT           AUTO_INCREMENT PRIMARY KEY COMMENT '放映记录ID',
    `cinema_id` INT           NOT NULL                    COMMENT '影院ID',
    `room_id`   INT           NOT NULL                    COMMENT '放映厅ID',
    `title`     VARCHAR(100)  NOT NULL                    COMMENT '电影名称',
    `start`     DATETIME                                 COMMENT '放映时间',
    `price`     DECIMAL(10,2) DEFAULT 0.00                COMMENT '票价（元）',
    `status`    VARCHAR(20)   DEFAULT '未开始'            COMMENT '放映状态'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='放映记录表（排片/场次）';

-- ---------------------------
-- 12. 订单表 (ordered)
-- ---------------------------
DROP TABLE IF EXISTS `ordered`;
CREATE TABLE `ordered` (
    `id`          INT           AUTO_INCREMENT PRIMARY KEY COMMENT '订单ID',
    `orders`      VARCHAR(50)   NOT NULL                   COMMENT '订单编号',
    `user_id`     INT           NOT NULL                   COMMENT '用户ID',
    `film_id`     INT           NOT NULL                   COMMENT '电影ID',
    `img`         VARCHAR(500)                             COMMENT '电影海报URL',
    `cinema_id`   INT           NOT NULL                   COMMENT '影院ID',
    `room_id`     INT           NOT NULL                   COMMENT '放映厅ID',
    `appointment` VARCHAR(100)                             COMMENT '预约场次信息',
    `total`       DECIMAL(10,2) DEFAULT 0.00               COMMENT '订单总金额（元）',
    `number`      INT           DEFAULT 1                  COMMENT '购票数量',
    `status`      VARCHAR(20)   DEFAULT '待取票'           COMMENT '订单状态（待取票/已取票/已取消）',
    `start`       DATETIME                                 COMMENT '放映时间',
    `seat`        VARCHAR(200)                             COMMENT '座位信息'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- ---------------------------
-- 13. 评分表 (mark)
-- ---------------------------
DROP TABLE IF EXISTS `mark`;
CREATE TABLE `mark` (
    `id`      INT          AUTO_INCREMENT PRIMARY KEY COMMENT '评分ID',
    `user_id` INT          NOT NULL                   COMMENT '用户ID',
    `film_id` INT          NOT NULL                   COMMENT '电影ID',
    `img`     VARCHAR(500)                            COMMENT '相关图片URL',
    `mark`    VARCHAR(20)                             COMMENT '评分/评语'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评分表';

-- ---------------------------
-- 14. 通知公告表 (notice)
-- ---------------------------
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice` (
    `id`      INT          AUTO_INCREMENT PRIMARY KEY COMMENT '通知ID',
    `title`   VARCHAR(100) NOT NULL                   COMMENT '通知标题',
    `content` TEXT                                    COMMENT '通知内容',
    `time`    DATETIME     DEFAULT CURRENT_TIMESTAMP   COMMENT '发布时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知公告表';

-- ---------------------------
-- 15. 视频/预告片表 (video)
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
