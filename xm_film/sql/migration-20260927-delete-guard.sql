-- ============================================================
-- 迁移脚本: 删除守卫 + 排片关联影片 (2026-09-27)
--
-- 背景:
--   1. ordered/record/room 的外键原为 ON DELETE CASCADE / SET NULL，
--      删除影片/影院/影厅/场次会静默级联删除订单（交易凭证丢失）。
--   2. record.film_id 可空，导致排片不关联影片时前台不可见。
--   3. cinema_film 表由代码以外的渠道手工维护，与 record 重复表达
--      "影院上映哪些影片"，实际由 record 派生即可。
--
-- 幂等: 重复执行不会报错。
-- 执行顺序: 先部署新代码，再执行本脚本（脚本会删除 cinema_film 表）。
-- ============================================================

-- ---------------------------
-- 1. 外键删除动作统一改为 RESTRICT
-- ---------------------------
DROP PROCEDURE IF EXISTS refresh_fk_delete_action;
DELIMITER $$
CREATE PROCEDURE refresh_fk_delete_action(
    IN tbl VARCHAR(64), IN col VARCHAR(64), IN ref_tbl VARCHAR(64), IN new_name VARCHAR(64))
BEGIN
    DECLARE fk VARCHAR(64) DEFAULT NULL;
    SELECT CONSTRAINT_NAME INTO fk
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = tbl
      AND COLUMN_NAME = col
      AND REFERENCED_TABLE_NAME = ref_tbl
    LIMIT 1;

    IF fk IS NOT NULL THEN
        SET @s = CONCAT('ALTER TABLE `', tbl, '` DROP FOREIGN KEY `', fk, '`');
        PREPARE st FROM @s;
        EXECUTE st;
        DEALLOCATE PREPARE st;
    END IF;

    SET @s = CONCAT('ALTER TABLE `', tbl, '` ADD CONSTRAINT `', new_name,
                    '` FOREIGN KEY (`', col, '`) REFERENCES `', ref_tbl,
                    '`(id) ON DELETE RESTRICT');
    PREPARE st FROM @s;
    EXECUTE st;
    DEALLOCATE PREPARE st;
END$$
DELIMITER ;

CALL refresh_fk_delete_action('room',    'cinema_id', 'cinema', 'fk_room_cinema');
CALL refresh_fk_delete_action('record',  'cinema_id', 'cinema', 'fk_record_cinema');
CALL refresh_fk_delete_action('record',  'room_id',   'room',   'fk_record_room');
CALL refresh_fk_delete_action('record',  'film_id',   'film',   'fk_record_film');
CALL refresh_fk_delete_action('ordered', 'record_id', 'record', 'fk_ordered_record');
CALL refresh_fk_delete_action('ordered', 'user_id',   'user',   'fk_ordered_user');
CALL refresh_fk_delete_action('ordered', 'film_id',   'film',   'fk_ordered_film');
CALL refresh_fk_delete_action('ordered', 'cinema_id', 'cinema', 'fk_ordered_cinema');
CALL refresh_fk_delete_action('ordered', 'room_id',   'room',   'fk_ordered_room');

DROP PROCEDURE IF EXISTS refresh_fk_delete_action;

-- ---------------------------
-- 2. record.film_id 改为 NOT NULL
--    排片必须关联影片，否则该场次在前台影院详情中不可见
-- ---------------------------
SET @nullable = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'record'
      AND COLUMN_NAME = 'film_id' AND IS_NULLABLE = 'YES');
SET @sql = IF(@nullable = 1,
    'ALTER TABLE record MODIFY COLUMN film_id INT NOT NULL COMMENT ''电影ID''',
    'SELECT ''record.film_id already NOT NULL, skipping''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------
-- 3. record.status 语义务实化: 待上映/已上映/停止上映 → 正常/停售
--    可购票性由 start 派生，status 只保留一个人工开关
-- ---------------------------
UPDATE record SET status = '正常' WHERE status IN ('待上映', '已上映', '放映中');
UPDATE record SET status = '停售' WHERE status IN ('停止上映', '已结束');
ALTER TABLE record MODIFY COLUMN status VARCHAR(20) DEFAULT '正常'
    COMMENT '售卖状态（正常/停售），展示状态由 start 派生';

-- ---------------------------
-- 4. 移除冗余表 cinema_film
--    影院上映影片改为由 record 派生（见 FilmMapper.selectByCinema / CinemaMapper.selectByFilmId）
-- ---------------------------
DROP TABLE IF EXISTS `cinema_film`;
