# 数据库初始化脚本

## 目录结构

```
sql/
├── schema.sql     # 17 张表全量建表语句（含所有列 —— 表结构的唯一权威）
├── data.sql       # 基础种子（不含场次/订单/评价，见「数据约定」）
├── init.sql       # 一键初始化（建库 + schema + data）—— 唯一受支持的入口
└── README.md      # 本文件
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
- **派生指标不预置**：`film.score`（影片评分）是派生列 —— 来自 `mark.score` 的均分，由 `MarkService` 在评价增删改后回写。种子里 17 部影片的 `score` **一律写 `NULL`**，表示"还没有人评过"（前端渲染「暂无评分」）；该列也刻意**不给 `DEFAULT`**，否则"新增影片不带评分"会落成「0 分」——`0.0` 是合法的真实评分，与 `NULL` 是两回事。已废弃的 `film.box_office` 同理（由 `ordered` 实时聚合）。

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

## 已存在的数据库：重建，不做增量升级

本项目**不提供增量迁移脚本**，只维护「全新安装」一条口径：`schema.sql` 是 17 张表全量、含所有列的
最终结构（包括 `ordered.pickup_code`、`mark_like`、`room.seat_rows/seat_cols`、钱包三表），
`init.sql` 是唯一受支持的入口。对已经初始化过的库，正确做法是重建：

```bash
mysql -u root -p -e "DROP DATABASE \`xm-film\`"
cd xm_film/sql && mysql --default-character-set=utf8mb4 -u root -p < init.sql
```

不必担心丢数据：`data.sql` 只播基础种子，`record`（场次）/ `ordered`（订单）/ `mark`（评价）/
`mark_like`（点赞）的行一律由真实业务接口产生（前台下单 → 支付 → 取票 → 评价 → 点赞），
本就不该有需要手工保住的内容。

2026-09-29 之前，仓库为「已有库」另维护过 8 个 `migration-*.sql`（自动建列、给存量订单回填取票码、
收敛影院审核词表等）。它们已被整体移除 —— 同时维护「全新安装」与「增量升级」两套口径，正是文档与
脚本漂移的来源（当时本文件的脚本清单就已经漏掉了其中 2 个）。历史脚本仍可取回：

```bash
git log --all --oneline -- xm_film/sql/migration-*.sql
git show <commit>:xm_film/sql/migration-20260929-pickup-code.sql
```

> 注意：这些历史脚本引用的 `scripts/seed-demo-data.py` 等本地工具脚本同样已删除，
> 取回迁移脚本时不要照抄其中的命令。
