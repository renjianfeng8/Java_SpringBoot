# 数据库初始化脚本

## 目录结构

```
sql/
├── schema.sql                               # 数据库表结构（17 张表的 CREATE TABLE 语句）
├── data.sql                                 # 基础种子（不含场次/订单/评价，见「数据约定」）
├── init.sql                                 # 一键初始化脚本（整合 schema + data）
├── migration-20260927-delete-guard.sql      # 增量迁移（删除守卫 + 上映关系派生）
├── migration-20260927-p2-seat-payment.sql   # 增量迁移（座位容量 + 订单资金凭证）
├── migration-20260928-p3-review-score-cinema-audit.sql  # 增量迁移（评价数值评分 + 影院审核词表）
├── migration-20260928-p4-account-wallet.sql # 增量迁移（账户余额 + 充值单据 + 资金流水 + 订单单价）
├── migration-20260929-deprecate-box-office.sql  # 增量迁移（废弃 film.box_office 静态票房列）
└── migration-20260929-mark-like.sql         # 增量迁移（评价点赞关系表 mark_like）
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
- **种子范围**：`data.sql` 只写基础数据（`admin` / `user` / `area` / `type` / `cinema` / `room` / `film` / `film_type` / `actor` / `notice` / `video`）。`record`（场次）、`ordered`（订单）、`mark`（评价）**不预置** —— 手写的订单必须同时伪造订单号、单价快照、支付凭证、余额扣减与资金流水，任意一处对不上就是能被查出的假数据（老种子正是如此）。需要演示数据只能经真实业务接口生成（前台下单 → 支付 → 取票 → 评价），不要手工 `INSERT`。

## 表清单（17 张）

| # | 表名 | 说明 |
|---|------|------|
| 1 | admin | 管理员表 |
| 2 | user | 用户表（`balance` 是账户余额，资金唯一可信来源；演示账号 zhangsan 预置 100 元） |
| 3 | cinema | 影院表（`status` 只有 `未审核`/`已审核`，未审核不可登录且不对外展示） |
| 4 | area | 区域/产地表 |
| 5 | type | 电影类型表 |
| 6 | film | 电影表 |
| 7 | film_type | 电影-类型关联表（多对多） |
| 8 | actor | 演员表 |
| 9 | room | 放映厅表 |
| 10 | record | 放映记录（排片）表，`film_id` 非空 |
| 11 | ordered | 订单表（`unit_price` 是下单时的单价快照） |
| 12 | mark | 评价表（`score` 是影片评分的唯一数值来源，`mark` 只存评语；一人一片一条） |
| 13 | notice | 通知公告表 |
| 14 | video | 视频/预告片表 |
| 15 | recharge_order | 充值单据表（处理中/已完成/已失败；提交单据不改余额，仅回调成功入账） |
| 16 | fund_flow | 资金流水表（充值/购票/退票三类来源，记录变动前后余额与关联单据ID；只增不改不删） |
| 17 | mark_like | 评价点赞关系表（复合主键 `(mark_id, user_id)` 即"一人一赞"的唯一权威，赞数由 `COUNT(*)` 聚合，**无计数列**） |

> 影院"上映哪些影片"由 `record` 派生（`EXISTS` 子查询），没有独立的影院-影片关联表。
> `mark_like` 是纯关系表：复合主键不留代理 `id`（与 `film_type` 同构），一行即一个赞。
> 两个外键均为 `ON DELETE CASCADE`（评价或用户被删，其点赞关系随之消失）—— 与 `ordered` 等
> 资金凭证的 `RESTRICT` 方向相反，是刻意的区分：点赞是"轻关系"，订单是"资金凭证"。
> `record` 与 `ordered` 的外键、`room.cinema_id`、`recharge_order.user_id`、`fund_flow.user_id` 均为 `ON DELETE RESTRICT`。
> `fund_flow.related_id` 指向 `recharge_order.id` 或 `ordered.id`（跨表二选一），故不建外键。
> `record` / `ordered` / `mark` 三张表不预置种子数据，行一律由真实业务接口产生。

## 增量迁移（已有数据库）

已初始化过的库不要重跑 `schema.sql` / `data.sql`（会与现有数据冲突），改用迁移脚本，按文件名日期顺序执行：

```bash
mysql -u root -p --default-character-set=utf8mb4 xm-film < migration-20260927-delete-guard.sql
mysql -u root -p --default-character-set=utf8mb4 xm-film < migration-20260927-p2-seat-payment.sql
mysql -u root -p --default-character-set=utf8mb4 xm-film < migration-20260928-p3-review-score-cinema-audit.sql
mysql -u root -p --default-character-set=utf8mb4 xm-film < migration-20260928-p4-account-wallet.sql
mysql -u root -p --default-character-set=utf8mb4 xm-film < migration-20260929-deprecate-box-office.sql
mysql -u root -p --default-character-set=utf8mb4 xm-film < migration-20260929-mark-like.sql
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
4. `film.score` 按 `mark.score` 均分回写（`EXISTS` 守卫：无评价的影片保留基线分，不归零）
5. 影院审核词表收敛：`待审核` → `未审核`，种子影院 8 改为 `已审核`

> 旧版第 4 步会往已有库灌入 51 条演示评价（镜像 `data.sql` 的评价种子）。`data.sql` 已移除评价种子，
> 该步一并删除 —— 不删的话，这个迁移就成了唯一还会造出假评价的地方。演示评价只能由真实业务接口产生。

> 迁移不会修改排片时间。若库中的 `record.start` 停留在过去，场次在前台会显示"已结束"且不可购票，
> 需另行把场次时间调整到未来 —— `data.sql` 已不再预置场次，新库建完后经排片接口创建场次即可。
>
> 已有影厅一律按 8×8 初始化 —— 这是旧规则下唯一合法的座位范围，因此存量订单的座位号必然落在新边界内。
> 需要更大的厅，请到影院后台修改该厅的座位行列数（1~50）。

`migration-20260929-deprecate-box-office.sql` 内容（幂等，可重复执行）：

1. `film.box_office` 的残留值清零
2. 更新该列注释，标明已废弃

> 票房已改为按 `ordered` 实时聚合的累计售票收入（`FilmMapper.xml` 的 `filmRevenueJoin`，只统计 `待取票/已取票`），
> 前端展示口径由「万元」改为**元**；`film.box_office` 不再被任何查询读取。
> 本脚本刻意不删除 `record` / `ordered` / `mark` 的任何行 —— 存量库的演示数据请按演示账号边界手工清理（不靠猜 id），再经真实业务接口重建。

`migration-20260929-mark-like.sql` 内容（幂等，可重复执行）：

1. 新增评价点赞关系表 `mark_like`（`PRIMARY KEY (mark_id, user_id)`，两个外键均 `ON DELETE CASCADE`）

> 用 `CREATE TABLE IF NOT EXISTS`，**不含任何 `DROP TABLE`** —— 脚本跑在活库上，DROP 会抹掉用户
> 真实的点赞数据。全新安装以 `schema.sql` 为唯一来源（其中的 `mark_like` 与本脚本定义一致），
> 本脚本只服务"已存在的库"；未执行时点赞接口报错 `mark_like` 表不存在。
> 部署顺序：先上新代码，再执行本脚本。
> 赞数**不落冗余计数列**：主键同时承担"一人一赞"与"可取消"，是赞数的唯一权威来源，
> 冗余列会引入需要人工同步的第二处真相（与 `user.balance` 为唯一余额来源同一思路）。
