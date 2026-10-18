# 数据库快照

本目录是 `xm-film` 库的**只读结构快照**，入库是为了让「文档 / 代码 / 数据库」三方校验的第三腿
在本仓里可见 —— 在此之前，库只存在于开发机的 MySQL 实例中。

| 文件 | 内容 | 性质 |
| --- | --- | --- |
| `schema.sql` | 18 张表的 `SHOW CREATE TABLE` 原样输出（含列注释、索引、外键） | 只读快照 |
| `inventory.tsv` | 列 / 索引 / 表三合一的逐行清单，便于 grep | 只读快照 |

## 口径

- **这不是迁移脚本**。没有版本号、没有顺序保证、不含数据。`schema.sql` 每条前有
  `DROP TABLE IF EXISTS`，只用于还原一套空结构，不要对着有数据的库执行。
- 不含任何业务数据行，因此可以直接入库（无个人数据）。
- `inventory.tsv` 的 `TBL` 行里 `TABLE_ROWS` 是 InnoDB 的**估算值**，不是精确计数；
  需要精确数时直接 `SELECT COUNT(*)`。

## 重新生成

本机 MySQL 8.0.37，库 `xm-film`，口令取自 `application.yml`：

```bash
python dump-schema.py          # MYSQL_EXE 可用环境变量覆盖 mysql 客户端路径
```

脚本只执行 `SELECT`（`information_schema`）与 `SHOW CREATE TABLE`，不写库。

## 已知的库表侧事实（供审查引用）

- `record` 表**没有任何防止时段重叠的约束**（只有 `idx_record_cinema_id/room_id/film_id/start`），
  这是 `audit/backend-backlog.md` BL-01「先查后插」在数据库侧的证据。
- `ordered` 表有 `uk_ordered_orders`（订单号）与 `uk_ordered_pickup_code`（取票码），
  二者是资金/凭证路径上仅有的硬唯一约束；**座位占用无唯一约束**，只靠 `record` 行锁串行化。
- `user` / `admin` / `cinema` 三表均已有 `username` 唯一索引 —— BL-03 的「唯一索引兜底」
  这一半在库里已具备，缺的是 Java 侧对 `DuplicateKeyException` 的转换。
- `cinema_directory.poi_id` 为 `NOT NULL` 且唯一，BL-04 里「poiId 可能为空」的 UNCERTAIN 不成立。
- `film.box_office` 列注释明确写着「已废弃…此列恒为 0」，与 `FilmMapper.filmRevenueJoin` 的
  实时聚合口径一致（实测 64 行全为 0）。
- `ordered.seat` 为 `varchar(200)`，座位串以逗号分隔存于单列，故占用判定只能走
  `FIND_IN_SET`，无法在库端建座位级唯一索引。
