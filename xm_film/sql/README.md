# 数据库初始化脚本

## 目录结构

```
sql/
├── schema.sql   # 数据库表结构（14张表的 CREATE TABLE 语句）
├── data.sql     # 初始数据（所有表的 INSERT 语句）
└── init.sql     # 一键初始化脚本（整合 schema + data）
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

## 表清单（15张）

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
| 9 | cinema_film | 影院-电影关联表 |
| 10 | room | 放映厅表 |
| 11 | record | 放映记录（排片）表 |
| 12 | ordered | 订单表 |
| 13 | mark | 评分表 |
| 14 | notice | 通知公告表 |
| 15 | video | 视频/预告片表 |
