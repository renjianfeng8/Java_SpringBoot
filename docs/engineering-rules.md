# 工程约定

本文件回答"在本地怎么跑、怎么验、怎么改、怎么记"。业务规则见 `domain.md`，架构见 `architecture.md`。

---

## 1 启动与验证

### 1.1 后端

```bash
cd xm_film/springboot
mvn -o spring-boot:run                      # 缺省 9090
mvn -o spring-boot:run -Dspring-boot.run.arguments=--server.port=9091   # 备用端口，勿动 9090
```

前置：MySQL 8 在 `localhost:3306`，库 `xm-film`，账号口令见 `application.yml`（`root` / `123456`）。

### 1.2 前端

```bash
cd xm_film/vue
npm run dev        # 5173
npm run build      # 产物到 dist/
```

`vite.config.js` **只代理 `/files`**；`/api` 走 `.env.development` 的 `VITE_API_BASE_URL=http://localhost:9090`。

### 1.3 测试

```bash
cd xm_film/springboot
mvn -o test                                   # 313 个用例
mvn -o test -Dtest=OrderedServiceTest         # 单类
mvn -o clean test -Dtest=XxxTest              # 改了 mapper XML / 改了 main 代码后，用这个
```

> ⚠️ **务必先 `clean` 的情况**：改过 `src/main/resources/mapper/*.xml`，或用编辑工具整体替换过 main 下的 `.java`（`target/classes` 里的旧副本不会自动失效）。本仓已两次因此出现「改了没生效」：BL-01 修完重跑仍落 2 行、BL-02 的测试在旧 class 上假失败。判断方法：测试结果与代码明显不符时，先 `clean` 再判。

**测试库是 `xm_film_test`，不是 `xm-film`。** 两库分离靠三样东西，缺一不可：

1. `src/test/resources/application-ci.yml` —— 数据源指向 `xm_film_test`，端口 0。
2. 每个 `@SpringBootTest` 上的 `@ActiveProfiles("ci")`。
3. 会写库的测试类上的 `@Transactional`。

首次准备（本机已建好，新机器照做）：

```bash
mysql -uroot -p123456 -e "CREATE DATABASE xm_film_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
python dump-schema.py --target xm_film_test   # 见 §2
```

> 为什么必须隔离：`TmdbLookupMapperLoadTest` 会真插 `type/area/actor` 再删。没有 profile 时它落在开发库上，`finally` 之前的断言失败或 JVM 被杀都会留下脏行。2026-10-18 之前一直如此。

### 1.4 验证清单（改完必须主动提议跑）

- 后端：`mvn -o test` 全绿。
- 前端：`npm run build` 成功（无 TS/模板报错）。
- 涉及页面：`npm run dev` 起 5173 目视；涉及数据链路时先确保库里有 `room` + `record`（**当前库这两张表是空的**，见 `项目索引.md` §3.3）。

---

## 2 数据库快照

`xm_film/springboot/src/test/resources/db/` 存的是 `xm-film` 的**只读结构快照**：

| 文件 | 内容 |
| --- | --- |
| `schema.sql` | 18 张表的 `SHOW CREATE TABLE`（含列注释、索引、外键） |
| `inventory.tsv` | 列 / 索引 / 表三合一的逐行清单 |
| `README.md` | 口径与库表侧已确认的事实 |

重新生成：

```bash
python dump-schema.py                      # 默认写 dev 库的结构到仓库
python dump-schema.py --target xm_film_test   # 用来初始化测试库
```

脚本只执行 `SELECT`（`information_schema`）与 `SHOW CREATE TABLE`，**不写库、不含数据行**。

三条使用约束：

- 不是迁移脚本：每条前有 `DROP TABLE IF EXISTS`，**不要对着有数据的库执行**。
- `inventory.tsv` 的 `TABLE_ROWS` 是 InnoDB 估算值。
- 改表结构后必须重新生成并提交，否则"三方校验"的第三腿又失真。

---

## 3 文档维护

| 文件 | 何时改 |
| --- | --- |
| `项目索引.md` | 文档文件增删、基线提交号、验证状态、数据现状变化 |
| `docs/domain.md` | 业务规则变化（状态机、金额口径、时限） |
| `docs/backend.md` / `docs/frontend.md` | 新增控制器前缀、新增页面、新增 composable |
| `audit/backend-backlog.md` / `audit/frontend-audit.md` | 新增或关闭债务条目 |
| `audit/table-spec.md` | 表格规范变化 |
| `xm_film/.../db/README.md` | 库表事实变化 |

**一处事实一处归属**：同一条结论只写在最相关的那份文档里，其余位置只放链接。发现问题先改事实源，再顺手修引用。

**已完成条目**：不进待办表，在对应章节写一句「本轮修复并移出本表」并说明行为变更。

**「已核验为非缺陷」章节不要删**：它的用途是防止下一轮审查把同一件事再报一遍。

---

## 4 编码约定

### 4.1 后端

| 规则 | 依据 |
| --- | --- |
| 新增纯 CRUD 资源复用 `BaseController/BaseService/BaseMapper` | `backend.md` §3 |
| 覆写 `update` 必须委派 `super.update` | BL-07：存在性判定唯一出处 |
| XML `updateById` 的 `<set>` 结尾必须有 `id = #{id}` | 否则空 SET 触发 1064 |
| 字符串筛选判 `x != null and x != ''` | BL-38 口径 |
| 返回 `Map` 的语句别名写 camelCase | 驼峰映射不作用于 Map key |
| `GROUP BY` 表达式与 `SELECT` 逐字相同 | `only_full_group_by` |
| 新增 `@SpringBootTest` 必带 `@ActiveProfiles("ci")`，写库再加 `@Transactional` | §1.3 |
| 状态是中文字面量，改动须跨 Java/XML/JS 同步 | `domain.md` §1 |

### 4.2 前端

| 规则 | 依据 |
| --- | --- |
| 列表页优先用 `useCrud` + `useFormDialog` | `frontend.md` §3 |
| 任何异步取数要带请求序号 | FE-11 / FE-25 类缺陷的共同根因 |
| `.then` 必须有 `.catch`（提示交给拦截器） | FE-45 |
| 提交按钮必须有 in-flight 守卫 + loading | FE-46 |
| 前缀匹配一律「相等或 `prefix + '/'`」 | FE-34 / FE-60 |
| 颜色/字号/间距只用令牌；字重只有 400/500/700 | `frontend.md` §6 |

### 4.3 表格（动表格前必读 `audit/table-spec.md`）

三条最容易踩的：

1. 每列必须同时给 `align` 与 `header-align`，取值相同。
2. `:last-child` 不能用来定位末列 —— 行结构由 EP 决定。
3. 表头与表体是两个独立 `<table>`，行高必须显式钉成同一值。

---

## 5 提交与分支

- 主分支 `master`；提交信息用 `type(scope): 中文摘要`，如 `fix(web): 账户页复查修复 —— …`。
- 文档变更单独提交（`docs: …`）。
- 不把 `.claude/`、`picture/`、`video/`、`*.log`、`*.sql`（含 backup）提交；`src/test/resources/db/schema.sql` 是例外（以 `db/` 目录下、非 `*backup*` 命名，不受忽略规则影响）。

---

## 6 本轮（2026-10-18）复核后的已知边界

- 库里 `room`、`record`、`ordered` 均为 **0 行**，购票主链未经真实数据验证。
- 前端无任何自动化测试；`npm run build` 是唯一自动验证手段。
- 集成测试只验证"XML 能被解析并执行"，不验证业务语义（语义由 Mockito 单测覆盖）。
