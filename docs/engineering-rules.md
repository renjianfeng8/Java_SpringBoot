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

前置：MySQL 8 在 `localhost:3306`，库 `xm-film`。先复制 `xm_film/springboot/.env.example` 为 `.env` 并填写 `JWT_SIGNING_KEY`、`DB_PASSWORD`（以及需要导入数据时的 `TMDB_TOKEN` / `AMAP_KEY`）；`application.yml` 不再保存任何密钥或口令，缺少 `JWT_SIGNING_KEY` 时后端会拒绝启动。

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
mvn test                                      # 72 个测试文件 / 548 条用例（2026-10-27）
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

- 后端：`mvn test` 全绿（72 文件 / 548 条）。
- 前端：`npm test` 全绿（Vitest，5 文件 / 33 条）且 `npm run build` 成功（无 TS/模板报错）。
- 涉及页面：`npm run dev` 起 5173 目视；涉及数据链路时先确保库里有 `room` + `record`。当前开发库已由 `demo-data.sql` 建好演示影厅/排片；若刚重置或换库，按 `项目索引.md` §3.3 重新导入。

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
| `docs/architecture.md` | 技术栈版本、分层文件数、配置项、统一响应/鉴权结构变化 |
| `docs/domain.md` | 业务规则变化（状态机、金额口径、时限） |
| `docs/backend.md` / `docs/frontend.md` | 新增控制器前缀、新增页面、新增 composable |
| `docs/demo-script.md` | 演示账号、演示数据、操作步骤变化 |
| `audit/backend-backlog.md` / `audit/frontend-audit.md` | 新增或关闭债务条目 |
| `audit/frontend-progress.md` | 前端收尾/功能项进度与验证结果变化 |
| `audit/sonarqube-report.md` | 重新扫描后更新快照；不重扫时明确标注历史快照 |
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
| 调用方引起的错误不得落到兜底 500；在 `GlobalExceptionHandler` 加具名处理器 | `architecture.md` §4 |
| 时间比较只能用 DB `NOW()`；超时判断必须 `include` `pendingExpiredCondition`，不得另写谓词 | `domain.md` §3.4 |
| **回调/入账类端点必须有服务端可验证的凭证**；「归属校验通过」不等于「授权通过」 | `backend-backlog.md` §安全 SEC-01 |
| **管理员重置密码只走 `PUT /auth/password/reset`**：控制器限 ADMIN，目标按 role 路由，6–64 位 BCrypt，不校验旧密码；日志不打印密码 | `AuthController` / `AuthService` |
| **管理端取消/退票复用订单状态机**：取消只限待支付、退票只限待取票且不绕过映前 60 分钟截止 | `domain.md` §3 |
| **删除守卫必须在 Service 且在同一事务内**：先锁父行 → 再数子表 → 再删；批量按 id 升序加锁 | `backend-backlog.md` §架构分层 BL-44 |
| **外部 HTTP/文件 IO 不得出现在 `@Transactional` 方法体内**；用 `TransactionTemplate` 只包写库段 | `backend-backlog.md` §架构分层 BL-45 |
| **并发冲突（死锁/等锁超时）必须映射为 409 可重试**，不得落到 500；用父类型 `PessimisticLockingFailureException` 一并覆盖 | `backend-backlog.md` §并发 BL-47 |
| **业务上"只能有一份"的规则必须只有一个实现**：口令比对、票价解析、单号生成、时间格式、座位解析分别在 `common/` 下各一处 | `backend-backlog.md` §重复代码 (2026-10-20) |
| **错误码传枚举，不传 `code()`**：`throw new CustomException(ErrorCode.X, msg)` / `Result.error(ErrorCode.X, msg)` | 同上 |
| **SQL 里不写状态中文字面量**，一律 `#{s.xxx}` 由 `OrderStatusCriteria` 传入；新增例外必须配守卫测试 | `backend-backlog.md` §数据库设计 BL-31 |
| **金额字段一律 `BigDecimal`**，不用 `Double`；库列用 `decimal(10,2)` | `backend-backlog.md` §数据库设计 BL-24 |
| **并发测试必须用两条物理连接**，且连接在"执行它的那个线程"上创建；只断言 SQL 文本含 `FOR UPDATE` 不算数 | `backend-backlog.md` §并发 BL-48 |
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

## 6 当前已知边界（2026-10-26）

- 开发库已由 `demo-data.sql` 建好演示影厅、排片与账号，购票主链已用真实数据跑通；重置数据后需重新导入。
- 前端已接入 Vitest（`npm test`），覆盖 `utils/`、`constants/` 与 `composables/` 的纯逻辑；`npm run build` 与无头 Chrome 冒烟脚本仍是组件与端到端的验证手段。
- 集成测试验证"XML 能被解析并执行"；业务语义由 Mockito 单测覆盖，二者合并为 72 个文件 / 548 条。
- SEC-01 / SEC-02 / SEC-03 已于 2026-10-25 修复：充值回调改为仅 ADMIN 可审核（用户在账户页只能提交申请，管理员在「充值流水」页通过/拒绝）；JWT 密钥移入 `.env` 的 `JWT_SIGNING_KEY` 并在启动时校验长度；删除明文密码回退，新增 `PlaintextPasswordMigration` 在启动时把存量明文转成 BCrypt。
