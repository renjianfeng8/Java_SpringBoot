# 数据库初始化脚本

## 目录结构

```
sql/
├── schema.sql                              # 数据库表结构（14 张表的 CREATE TABLE 语句）
├── data.sql                                # 初始数据（所有表的 INSERT 语句）
├── init.sql                                # 一键初始化脚本（整合 schema + data）
└── migration-20260927-delete-guard.sql     # 增量迁移（仅已有库需要执行）
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
| 3 | cinema | 影院表 |
| 4 | area | 区域/产地表 |
| 5 | type | 电影类型表 |
| 6 | film | 电影表 |
| 7 | film_type | 电影-类型关联表（多对多） |
| 8 | actor | 演员表 |
| 9 | room | 放映厅表 |
| 10 | record | 放映记录（排片）表，`film_id` 非空 |
| 11 | ordered | 订单表 |
| 12 | mark | 评分表 |
| 13 | notice | 通知公告表 |
| 14 | video | 视频/预告片表 |

> 影院"上映哪些影片"由 `record` 派生（`EXISTS` 子查询），没有独立的影院-影片关联表。
> `record` 与 `ordered` 的外键、`room.cinema_id` 均为 `ON DELETE RESTRICT`。

## 增量迁移（已有数据库）

已初始化过的库不要重跑 `schema.sql` / `data.sql`（会与现有数据冲突），改用迁移脚本：

```bash
mysql -u root -p --default-character-set=utf8mb4 xm-film < migration-20260927-delete-guard.sql
```

`migration-20260927-delete-guard.sql` 内容（幂等，可重复执行）：

1. `room`/`record`/`ordered` 的外键删除动作统一改为 `RESTRICT` 并显式命名
2. `record.film_id` 改为 `NOT NULL`
3. `record.status` 旧词汇（待上映/已上映/停止上映）归一为 `正常/停售`
4. 删除冗余表 `cinema_film`

> 迁移不会修改排片时间。若库中的 `record.start` 停留在过去，场次在前台会显示"已结束"且不可购票，
> 需另行把演示场次时间调整到未来（新库由 `data.sql` 直接写入未来时间）。
