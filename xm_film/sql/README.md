# 数据库初始化脚本

## 目录结构

```
sql/
├── schema.sql                               # 数据库表结构（14 张表的 CREATE TABLE 语句）
├── data.sql                                 # 初始数据（所有表的 INSERT 语句）
├── init.sql                                 # 一键初始化脚本（整合 schema + data）
├── migration-20260927-delete-guard.sql      # 增量迁移（删除守卫 + 上映关系派生）
├── migration-20260927-p2-seat-payment.sql   # 增量迁移（座位容量 + 订单资金凭证）
└── migration-20260928-p3-review-score-cinema-audit.sql  # 增量迁移（评价数值评分 + 影院审核词表）
```

## 使用方式

### 方式一：一键初始化

```bash
mysql -u root -p < init.sql
```

### 方式二：分步执行

```sql
CREATE DATABASE `xm-film` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `xm-film`;
SOURCE schema.sql;
SOURCE data.sql;
```

### 方式三：手动导入（MySQL 客户端）

1. 创建数据库：`CREATE DATABASE \`xm-film\` DEFAULT CHARACTER SET utf8mb4;`
2. 选择数据库：`USE \`xm-film\``
3. 执行 schema.sql 建表
4. 执行 data.sql 导入数据

## 数据约定

- 数据库名：`xm-film`（与 `application.yml` 配置一致）
- 字符集：`utf8mb4` + `utf8mb4_unicode_ci`
- 引擎：`InnoDB`

## 表清单（14 张）

| # | 表名 | 说明 |
|---|------|------|
| 1 | admin | 管理员表 |
| 2 | user | 用户表 |
| 3 | cinema | 影院表（`status` 只有 `未审核`/`已审核`，未审核不可登录且不对外展示） |
| 4 | area | 区域/产地表 |
| 5 | type | 电影类型表 |
| 6 | film | 电影表 |
| 7 | film_type | 电影-类型关联表（多对多） |
| 8 | actor | 演员表 |
| 9 | room | 放映厅表 |
| 10 | record | 放映记录（排片）表，`film_id` 非空 |
| 11 | ordered | 订单表 |
| 12 | mark | 评价表（`score` 是影片评分的唯一数值来源，`mark` 只存评语；一人一片一条） |
| 13 | notice | 通知公告表 |
| 14 | video | 视频/预告片表 |

> 影院"上映哪些影片"由 `record` 派生（`EXISTS` 子查询），没有独立的影院-影片关联表。
> `record` 与 `ordered` 的外键、`room.cinema_id` 均为 `ON DELETE RESTRICT`。

## 增量迁移（已有数据库）

已初始化过的库不要重跑 `schema.sql` / `data.sql`（会与现有数据冲突），改用迁移脚本，按文件名日期顺序执行：

```bash
mysql -u root -p --default-character-set=utf8mb4 xm-film < migration-20260927-delete-guard.sql
mysql -u root -p --default-character-set=utf8mb4 xm-film < migration-20260927-p2-seat-payment.sql
mysql -u root -p --default-character-set=utf8mb4 xm-film < migration-20260928-p3-review-score-cinema-audit.sql
```

`migration-20260927-delete-guard.sql` 内容（幂等，可重复执行）：

1. `room`/`record`/`ordered` 的外键删除动作统一改为 `RESTRICT` 并显式命名
2. `record.film_id` 改为 `NOT NULL`
3. `record.status` 旧词汇（待上映/已上映/停止上映）归一为 `正常/停售`
4. 删除冗余表 `cinema_film`

`migration-20260927-p2-seat-payment.sql` 内容（幂等，可重复执行）：

1. `room` 新增 `seat_rows` / `seat_cols`（默认 8×8）
2. `ordered` 新增 `pay_time` / `pay_amount` / `refund_time` / `refund_amount`
3. `ordered.status` 词表补 `已退票`
4. 为存量已支付订单（待取票/已取票）回填支付凭证，仅填充空值

`migration-20260928-p3-review-score-cinema-audit.sql` 内容（幂等，可重复执行）：

1. `mark` 新增 `score`（DECIMAL(3,1)，影片评分的唯一数值来源）
2. `mark.mark` 语义由「评分/评语」收敛为「评语」并加宽到 `VARCHAR(255)`
3. 回填存量评价：数字文本（如 `'9.5'`）搬进 `score`，原列改填评语；其余空评分按所属影片基线分补齐
4. 写入演示评价（每部影片 3 条，均分恰好等于原基线分，评分榜排序不变但从此由评价派生）
5. `film.score` 按 `mark.score` 均分回写（`EXISTS` 守卫：无评价的影片保留基线分，不归零）
6. 影院审核词表收敛：`待审核` → `未审核`，种子影院 8 改为 `已审核`

> 第 3、4 步的演示评价与 `data.sql` 完全一致，目的是让"已有库执行迁移"与"新库执行 init.sql"收敛到同一状态；
> `data.sql` 才是种子数据的权威副本。

> 迁移不会修改排片时间。若库中的 `record.start` 停留在过去，场次在前台会显示"已结束"且不可购票，
> 需另行把演示场次时间调整到未来（新库由 `data.sql` 直接写入未来时间）。
>
> 已有影厅一律按 8×8 初始化 —— 这是旧规则下唯一合法的座位范围，因此存量订单的座位号必然落在新边界内。
> 需要更大的厅，请到影院后台修改该厅的座位行列数（1~50）。
