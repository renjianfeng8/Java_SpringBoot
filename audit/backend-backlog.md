# 待办技术债清单（backend-backlog）

> 2026-10-18 全盘复核：本文件原为仓库根的 `audit-backlog.md`，已移入 `audit/` 并改名。标题保留 `BL-` 编号以维持历史引用。复核中修正了 BL-03 / BL-04 / BL-42 三处的过时描述（见各自「2026-10-18 复核」段）。索引见根目录 `项目索引.md`。
>
> 2026-10-22 编号修正：原 BL-30（座位串拆表）改为 **BL-53**，原 BL-31（影院名录分页映射）改为 **BL-54**；BL-30 保留给文件上传，BL-31 保留给状态字面量。

本文件是本仓唯一的技术债登记处，承接已删除的 `后端代码规范审查报告.md`（**该文件已不在仓内**，以下为历史引用）的全部"登记备查"项，并汇总 2026-10-13 两轮代码审查（`entity/`、`service/`）、2026-10-14 控制器层审查（`controller/`）、2026-10-14 mapper 层审查（`mapper/`）与 2026-10-14 DTO 层审查（`dto/`）中判定延后或建议的条目。已完成项不进本表；已判定非缺陷的条目集中在末节，避免二次误报。2026-10-15 承 FE-48 契约对齐复查演职人员数据模型，补登 BL-41；同日做纯清理四项（BL-28/33/34/38），另登记 BL-42（同形字符串筛选未做）、BL-43（UserController 读口径分歧）。

维护口径：一处事实一处归属；每条给出 `文件` 与符号，行号随改动可能漂移、以符号为准。状态分三档：待办、有意不整改（与既定政策一致，非债务）、已核验非缺陷；修复完成的条目从表中移出，在该节以「本轮修复并移出本表」一句登记。

更新日期：2026-10-22（B/C 项收尾后）。当前代码基线：`master` @ `2286861e` + 2026-10-22 未提交改动；历史清单基线 `bfdd1c07`、`a2144fc4`，复核基线 `3112f8b7`，Sonar 扫描基线 `0ff8331c`。

**当前实测基线**：`mvn test` = **414/414 通过，48 个测试类**（2026-10-25）。历史记录：2026-10-22 403/403、2026-10-20 398/398、2026-10-19 343/343、2026-10-18 309/309。开发库 `xm-film` 已由 `springboot/demo-data.sql` 补入演示影厅、排片与账号，购票主链已实测跑通。

**状态口径**：SEC-01/02/03 已于 2026-10-25 修复（见 §安全 的修复记录），历史证据保留；不再作为未修漏洞跟踪。

## 架构分层（2026-10-19 专项，三项均已修复）

**背景**：综合评分里「架构与分层 85」的三条扣分项。其中第一条不是纯风格问题 —— 它是**真实的并发窗口**（详见下条），只是后果被外键 RESTRICT 兜住了，表现为提示不可读而非数据损坏。这一点与 BL-02 完全同构。

| 编号 | 问题 | 实测/复核 | 处置 |
| --- | --- | --- | --- |
| **BL-44** | **删除守卫在 Controller，计数与 DELETE 分属两个事务**（`CinemaController.ensureNoReferences`、`FilmController.ensureNoReferences`）。 | 直接 SQL 实验：A 会话「数到 0 → SLEEP 3s → DELETE」，B 在 3s 内插入影厅 → A 的 DELETE 被外键挡住（`ERROR 1451`），**数据未损坏**；但 HTTP 层这条路径拿到的是通用提示「仍被其他数据引用」（`GlobalExceptionHandler.dataIntegrityError`），而非「已有 1 个影厅」。用 MySQL EVENT 卡到 JVM 毫秒级窗口的尝试**未能命中**（4 个时刻点全部走了正常计数路径），所以「接口层能稳定复现」这句不成立 —— 只能证明窗口在事务语义上存在。 | 守卫下沉到 `CinemaService.deleteGuarded` / `FilmService.deleteGuarded`：**先 `selectByIdForUpdate` 锁父行 → 再数子表 → 再删**，全程一个事务；批量按 id **升序**加锁防交叉死锁。`RoomService.addScoped` 改为先锁影院行再插影厅，两端抢同一把锁。守卫 `DeleteGuardLockOrderTest`（6 条，InOrder 断言顺序），已做反证（改回「先数后锁」→ 2 条变红）。 |
| **BL-45** | **`CinemaDirectoryImportService.importAll` 把外部抓取包在 `@Transactional` 里**。 | 抓取实测 13 秒（北京 555 条、上海 536 条），旧实现在这 13 秒内全程持有数据库连接与事务。 | 改成 `TransactionTemplate` 显式包住**写库阶段**，抓取在事务外。守卫 `CinemaDirectoryImportTxBoundaryTest` 直接断言「抓取被调用时无活跃事务」，已做反证（把抓取挪回事务内 → `expected: 0 but was: 1`）。HTTP 实测：抓取期间另开连接对 `cinema_directory` 加表锁耗时 33ms（无阻塞）。 |
| **BL-46** | `@ControllerAdvice("com.example.springboot.controller")` 用字符串包名限定，语义不直观，且包被移动后会**静默失效**。 | 无运行时缺陷；属可读性/可维护性。 | 改为具名属性 `basePackages = "com.example.springboot.controller"` 并注明「为何限定」与「为何依赖字符串」，与该类原有 9 档处理器放在一起解释。 |

**顺带修掉一个真实功能缺陷（不在原评分项内）**：核查 BL-45 时用真实 key 复测名录导入，发现**北京/上海导入恒为 0 条**。根因是 `AmapClient.districts` 假定直辖市「顶层有 province + 一个同级 level=city 且带区县」——**线上实际只返回一个省级节点**，区县在 city 节点的再下一层。旧单测的 fixture 正是按错误假设写的，于是测试绿灯而线上恒空。修复：`subdistrict` 改为 2 并多下钻一层；fixture 改为实测形态。修复后实测北京 16 个区县、上海 16 个、石家庄 22 个；预览北京 total=555、上海 536。另：逐区县抓取时单个区县失败（高德并发超限）改为记 warning 跳过，不再让整次预览/导入失败。

**这一节的教训**：`information_schema.INNODB_TRX` 在**本机恒为 0**（手动开事务后仍查不到），一度让我把「查不到事务」误当成「没有事务」。事务边界的判定改用 `TransactionSynchronizationManager.isActualTransactionActive()` 与「另开连接能否加锁」这类不依赖该表的信号。

## 并发（2026-10-19 专项，补自动化回归 + 修一条映射缺陷）

**背景**：并发维度此前 88 分，但结论**全部来自手工实测**，测试树里 0 条用两条连接验证锁的用例。本轮把最关键的几条变成可回归的测试，并在反证过程中挖出一条真实缺陷。

| 编号 | 问题 | 实测 | 处置 |
| --- | --- | --- | --- |
| **BL-47** | **并发冲突（死锁/等锁超时）漏成 500「系统错误」**。`GlobalExceptionHandler` 没有 `PessimisticLockingFailureException` 分支，全仓只有 `CinemaService.importFromDirectory` 自己 catch 了死锁。 | **反证 BL-01 时意外撞出**：临时去掉 `RecordService.validateSchedule` 的影厅行锁，两个并发建重叠排片的请求撞出 `DeadlockLoserDataAccessException`，原先一路落到兜底分支变成 500。 | 新增 `@ExceptionHandler(PessimisticLockingFailureException.class)` → **409「操作与他人发生冲突，请稍后重试」**。用父类型而非只捕死锁：等锁超时是 `CannotAcquireLockException`，只捕前者会让超时那半漏成 500。守卫 `ExceptionMappingTest` 新增 2 条。 |
| **BL-48** | 并发的**结论不可回归**：`RecordOverlapLockTest` 只断言 SQL 文本含 `FOR UPDATE`，不证明锁真的互斥。 | — | 新增两类测试，见下。 |

**新增的并发测试（均做过反证）**：

| 测试 | 证明什么 | 反证 |
| --- | --- | --- |
| `RowLockMutualExclusionTest`（4 条） | 双物理连接：A 持 `FOR UPDATE` 时 B **300 ms 内拿不到**；A 提交后 B 立刻放行。覆盖 `cinema`/`film`/`room`/`record` 四张表，先用 `performance_schema.data_locks` 确认 A 真的拿到 `RECORD X` 锁。 | 去掉 `FOR UPDATE` → 4 条全红（第一道断言「应出现排他记录锁」即拦截） |
| `RecordConcurrentOverlapTest`（6 轮） | 两个线程各自开事务、调真实 `RecordService.addScoped` 建**重叠时段** → 每轮**只落 1 场**，另一个必须以「已有排片」失败。 | 把锁换成 `selectById` → 立刻红 |

**踩过的坑（值得记）**：`RowLockMutualExclusionTest` 最初在主线程建好 A/B 两条连接、再把 B 的操作丢给线程池执行 —— B 拿到的连接被 Spring 测试上下文按线程绑定，实测**立即返回、锁完全没生效**（日志里 A 确实持有 `RECORD X`，但 B 等锁耗时 0 ms）。修复：**连接在哪个线程执行就在哪个线程创建**。这类"测试假绿"比缺陷本身更危险。

**HTTP 层实测**：4 并发建同一影厅重叠排片 → `HTTP=200 ×4`，业务码 `200 / 409 / 409 / 409`，消息均为「该影厅在此时间段已有排片」。另用独立连接持锁 6 秒，期间提交排片接口挂起至锁释放后返回 200 —— 证明"等锁"在真实链路里会发生。

**未覆盖（诚实记录）**：死锁在 HTTP 层**未能稳定复现**（去掉锁后压 4 并发，4 个请求恰好串行，未撞出死锁）。因此"死锁 → 409"的结论靠单测钉住映射 + SQL 层证明死锁确实会产生，**不是**端到端复现。

## API 契约（2026-10-19，前端收敛 `'200'` 时发现）

| 编号 | 位置 | 问题 | 影响 | 建议 |
| --- | --- | --- | --- | --- |
| **BL-49** | `common/BaseController.getById` | 对**不存在**的 id 返回 `HTTP 200 + code "200" + data null`，而不是 404。实测 `GET /api/v1/films/99999999` → `{"code":"200","msg":"操作成功","data":null}`。 | 前端调用方只要写成 `if (isSuccess(res)) { res.data.xxx }` 就会在"记录刚被删除"时抛错。当前 117 处 `isSuccess()` 调用里大部分已判 `res.data` 或走列表接口，**未发现实际崩溃**，但这是一个易踩的坑。分页/列表接口不受影响。 | 覆写了 `getById` 的 5 个控制器：`Ordered`/`Record`/`Room`/`User` 走 `selectByIdScoped`，**不存在时抛 404**；`Cinema` 是**有意**返回 null（注释写明"未审核影院不对外展示"，返回 404 会泄露该 id 存在）。其余继承基类的 Film/Type/Area/Actor/Notice/Video 返回 200+null。不宜在基类统一改：改基类会让"有意返回 null"的 Cinema 也变 404，且所有实体的前端分支都要跟着改。建议逐实体判断后覆写。 |

## 控制器层测试（2026-10-19 专项，为安全攻坚铺路）
**背景**：测试维度原 62 分，扣分项之一是「23 个 Controller 里 18 个零覆盖」。安全三条（SEC-01/02/03）全在控制器/服务交界处，没有控制器测试就每次都得手工 curl 复现。本轮先覆盖**主链与安全面**三个代表，而不是把所有控制器机械铺一遍。

| 测试类 | 条数 | 钉住什么 |
| --- | --- | --- |
| `controller/OrderedControllerTest` | 9 | ① 支付超时 `TIMEOUT_CANCELLED` 必须翻译成 **409**，不能返回 200 让前端显示"支付成功"；② 选座查询对**非 USER 必须传 `viewerId=null`**（否则同号用户订单被误标 mine、泄露他人订单号与金额）；③ 通用 `POST /orders` 必须拒绝并引导到 `/create`；④ 缺参是 400 不是 500 |
| `controller/RechargeControllerTest` | 6 | 建单/回调的身份**取自 token 而非请求体**（请求体里塞 `userId:999` 无效）；归属越权 403 与重复回调 409 原样透出 |
| `controller/TicketControllerTest` | 7 | 唯一匿名写入口：匿名 POST 放行、**同前缀其它路径仍 401**（精确匹配不是 startsWith）、请求体多余 `orderId` 被忽略、响应不含订单号/金额/用户 id、异常不泄露内部信息 |

**反证**：把 `OrderedController` 的 `viewerId` 改回无条件传 `userId` → 「非 USER 传 null」那条立刻红（`Argument(s) are different! Wanted: (42, isNull())`）。

**关于 SEC-01 的测试策略（2026-10-25 更新）**：修复前本类刻意不把"任意用户给自己回调充值成功"写进断言，避免把缺陷固化成期望行为；修复后新增 `userCallbackSurfacesForbidden`，钉住 USER 即使操作自己的单据也得到 403。当前本类还钉"归属"与"幂等"两条规则。

**一个测不出来的点（诚实记录）**：`TicketController` 的「GET 同路径必须 401」在 `@WebMvcTest` 里验证不了 —— 该控制器只有 `@PostMapping`，GET 在 HandlerMapping 阶段就 405，到不了拦截器。这条改由 `AuthInterceptorAccessTest` 在拦截器层覆盖，没有造一个假通过的用例。

## 安全（2026-10-19 独立审计，三条已实测触发）

**背景**：并发与异常两条线修完之后，另做了一轮独立的安全核查（不看代码注释、只认实测）。三条**均已用真实 HTTP 复现**，优先级高于本文件其余全部条目 —— 它们不是"技术债"，是可用两条请求打穿的功能缺陷。

| 编号 | 位置 | 问题 | 实测证据 | 处置建议 |
| --- | --- | --- | --- | --- |
| **SEC-01** | controller/RechargeController.callback；service/RechargeService.handleCallback（归属判定分支） | **任意 USER 可无限给自己充值**。归属校验允许 USER 操作*自己的*单据，而 `success` 完全取自请求体，没有任何验签。 | `POST /recharges {amount:50000}` → `POST /recharges/{id}/callback {success:true}` → **余额 500 → 50500**。纯 HTTP，无需任何伪造。 | 回调加服务端密钥/签名校验；或干脆下线 USER 可达的 callback，改为 ADMIN 审核入账。**演示环境与生产必须分开**，不能靠"注释里说是演示"约束。 |
| **SEC-02** | resources/application.yml `jwt.signing-key` | **JWT 密钥硬编码在源码，可自签 ADMIN 令牌**。51 字节够长，但密钥本身随仓库公开，长度不构成保护。 | 用 `REDACTED_JWT_SECRET-2024-…` 自签 `{"sub":"1","role":"ADMIN"}`：无令牌 `DELETE /areas/999999` → **401**；同一请求带伪造令牌 → **200 成功**。伪造 USER 令牌读 `/admins/page` → 403（**角色校验本身正确**）。 | 密钥移入环境变量并在启动时校验强度；**当前密钥必须视为已泄露并更换**；考虑加 `jti` 黑名单以支持吊销。 |
| **SEC-03** | service/UserService.login:95-96；AdminService:78-80；CinemaService:201-203；PayPasswordService:136 | **明文回退让哈希可当密码用**。`!matches(pwd, stored) && !pwd.equals(stored)` —— 当 `stored` 是 BCrypt 串时，把该串原样当密码提交即可通过。 | 用库中 `password` 列原文登录 `u1`、`999` 均 **code=200 成功**。叠加 mapper DEBUG 日志（`logback-spring.xml:7` 会打印绑定参数），**日志即凭证**。 | 删除回退分支；存量明文账号一次性迁移为 BCrypt 后删除该分支。演示数据 `demo-data.sql` 不应依赖明文回退。 |

**2026-10-25 修复并移出本表**：

- **SEC-01**：`RechargeService.handleCallback` 改为仅接受 ADMIN；用户端「模拟支付回调」按钮移除，管理员在「充值流水」页通过/拒绝。真实网关接入时再加签名/查单，当前不再存在“任意用户给自己充值”的路径。
- **SEC-02**：`jwt.signing-key` 改为从 `.env` 的 `JWT_SIGNING_KEY` 读取，`JwtUtils` 启动时校验长度 ≥ 32 字节；TMDB token、Amap key、DB 口令也一并移入 `.env`（模板见 `.env.example`，`.env` 不入库）。
- **SEC-03**：删除 `PasswordMatcher` 的 `raw.equals(stored)` 回退；新增 `PlaintextPasswordMigration`，启动时把 user/admin/cinema 的登录口令与 user.pay_password 中的明文统一转成 BCrypt。
- 守卫：新增 `PlaintextPasswordMigrationTest`、`FileUploadControllerTest`；`mvn test` = **414/414 通过，48 个测试类**（2026-10-25）。

**连带发现（中危，未逐条实测）**：

- `FileUploadController` 对**任意登录角色**开放、无配额（`application.yml` 单文件 50MB）—— 可反复上传填满磁盘。
- `FileUtil` 只信客户端 `Content-Type`，不嗅探文件头 —— 可上传伪装成 png 的任意内容（**不构成 XSS**：扩展名由服务端白名单决定，且不含 svg）。
- ~~`CinemaService.importFromDirectory` 批量影院共用默认密码 `cinema123` 且直接 `APPROVED`，用户名规则 `amap_+poiId` 可推导。~~ **2026-10-20 已修复**：每个账号独立随机初始口令（12 位，`SecureRandom`），明文只在导入响应中返回一次；`insertCinema` 不再回退默认口令。见 §SonarQube 静态扫描。
- 登录**无失败锁定**（只有支付密码有 5 次/15 分钟锁定），可暴力撞库。
- 无令牌吊销：删号/改密后旧令牌 24h 内仍有效。
- 匿名核销端点 `POST /tickets/redeem` **无速率限制**（码空间 31⁸ 不可猜，但可被刷接口）。

**这一节的由来**：修完 BL-01…06 之后的自评是 93，但那一轮只验证了"数据库层会不会出问题"，没有验证"**谁能调这些接口**"。SEC-01 从归属校验看完全"正确"（USER 只能动自己的单），从资金流看却是灾难 —— **归属正确不等于授权正确**。

## SonarQube 静态扫描（2026-10-20）

**背景**：用 SonarQube 10.7（`Sonar way`）对 Java 主代码/测试 + Vue 全量扫描，基线 74 条问题，两轮修复后当时降到 **32 条**（漏洞 0、测试代码问题 0；2026-10-22 又修了 8 条 ARIA，未复扫）。完整清单、逐条误报判定与复现命令见 `audit/sonarqube-report.md`；本节只登记三条需要跟踪的缺陷（均已修复），其余为命名/字面量/ARIA/复杂度一类的清理项。

| 编号 | 位置 | 问题 | 处置 |
| --- | --- | --- | --- |
| **BL-50** | service/PayPasswordService | 支付密码校验标注 `@Transactional(REQUIRES_NEW)`，却被本类 `changePayPassword` / `verifyOldPassword` 用 `this` 调用。同类自调用不经过代理，`REQUIRES_NEW` 被静默忽略；当前靠"入口没有外层事务"成立，一旦调用方进事务，失败计数与 15 分钟锁定会随回滚消失。 | **已修复**：改用 `TransactionTemplate` 显式 `REQUIRES_NEW`，不依赖 AOP 代理。守卫 `PayPasswordTransactionBoundaryTest` 用真实库证明"外层回滚后计数仍为 1"；`PayPasswordServiceTest#verifyOldPasswordRunsInRequiresNewTransaction` 钉住传播级别。反证：把传播级别改回 `REQUIRED` → 两条守卫都红（计数 0）。 |
| **BL-51** | service/OrderedService.generatePickupCode | 取票码用 `ThreadLocalRandom`。它是匿名核销接口 `POST /tickets/redeem` 的唯一凭证，随机源可预测等于降低凭证强度；31⁸ 只说明枚举空间，不解决可预测性。 | **已修复**：新增 `common/SecureCodes`（`SecureRandom`），取票码与批量导入的初始口令共用。既有 `payOrderGeneratesPickupCode` 继续钉格式与字母表。 |
| **BL-52** | service/CinemaService.importFromDirectory；test/ExceptionMappingTest、test/service/CinemaServiceTest | 仍 catch / 引用已弃用的 `DeadlockLoserDataAccessException`。BL-47 的结论是必须捕父类型 `PessimisticLockingFailureException`，否则等锁超时那一半仍会漏成 500。另 1 条是 Spring 6.2 弃用的 `HttpMessageNotReadableException` 单参构造器。 | **已修复**：主代码改捕父类型，测试同步；新增 `importFromDirectoryReportsLockWaitTimeoutAsRetryableConflict` 用 `CannotAcquireLockException` 钉住超时那半。异常映射测试改用 `PessimisticLockingFailureException` 与带 `MockHttpInputMessage` 的两参构造器，4 条 `S1874` 清零。 |

**同轮修复（原安全章节的连带发现）**：批量导入影院的硬编码口令 `cinema123` 已移除。每个新账号生成独立的 12 位随机口令，库中只存 BCrypt 哈希，明文只在 `POST /cinemas/from-directory` 的响应 `accounts[]` 里返回一次；`insertCinema` 对空口令返回 400，不再回退默认值。反证：改回共用 `cinema123` → 3 条用例红。

## 异常处理与入参（2026-10-18 专项梳理）

**背景**：BL-01/02/03 三次工单都撞在同一个薄弱点上 —— `GlobalExceptionHandler` 除业务异常外只有一个兜底分支，**任何调用方引起的错误都变成 500「系统错误」**。因此做了一次专项梳理：用真实 HTTP 请求逐类打接口，记录实际返回。

**实测确认 11 条会返回 500 的路径**：

| 类别 | 例 | 修复前 | 修复后 |
| --- | --- | --- | --- |
| 路径参数类型错 | `GET /films/abc`、`/films/999…（超长）` | 500 | 400「参数「id」格式不正确」 |
| 缺必填查询参数 | `/films/search`、`/films/by-cinema`、`/orders/seats`、`/marks/by-film` | 500 | 400「缺少必填参数「title」」 |
| 查询参数类型错 | `/films/page?pageNum=abc` | 500 | 400 |
| 请求体缺失 | `POST /films` 空 body、`PUT /orders/1/pay` 空 body、`POST /auth/register` 空 body | 500 | 400「请求体缺失或格式不正确」 |
| 空对象请求体 | `POST /films {}` | 500（`INSERT INTO film () VALUES ()` 语法错） | 400「请至少填写一项内容」 |

**修复**：

1. `GlobalExceptionHandler` 补 5 个具名处理器：`MethodArgumentTypeMismatchException`、`MissingServletRequestParameterException`、`HttpMessageNotReadableException`、`HttpRequestMethodNotSupportedException`、`HttpMediaTypeNotSupportedException`。提示只回中文，参数名进日志（Spring 默认消息带内部方法签名，不能直接外泄）。
2. `BaseController.add` 在 `toEntity` 之后加 `requireNonEmptyEntity`：实体所有字段为 null 时抛 400。理由：mapper 的 `<trim>` 逐字段拼接，全空会生成非法 SQL。用反射而非逐个补 `@NotBlank`（后者需连带改 `@Valid` 链，属另一轮重构）。
3. 兜底分支的日志补上异常类型，并注明「走到这里的都应是服务端自身的问题；若某类异常明明由调用方引起，正确做法是加具名处理器，而不是放宽兜底」。

**验证**：11/11 路径全部从 500 变 400；新增 `ExceptionMappingTest` 7 条用例（其中两条钉住「真正的服务端异常仍返回 500」「业务异常仍透传自己的 code」，防止这次改动把兜底放宽）。另做了一次正向回归：地区/类型/公告/影片的新增全部正常，未被新守卫误拦。

**附带发现（未修，登记备查）**：`POST /films {}` 的根因是 mapper 的 `<trim>` 允许全空插入。本仓多个 `insert` 都有同样形状（`<if>` 全部不成立即产出空列清单）。`BaseController` 的守卫覆盖了走基类的新增路径，但**子类自行实现的写入端点**（如 `OrderedController.create`、`CinemaController` 的 from-directory）不在保护范围内 —— 那些端点目前都有各自的必填校验，暂无可达缺陷。

---

## 并发与事务

本轮修复并移出本表：**BL-03** —— 并发注册同名。

**先纠正原描述**：该条原写「并发注册同名双双通过落两行」，**实测不成立**。库中 `user` / `admin` / `cinema` 三表都已有 `username` 唯一索引，撞键时第二个 INSERT 会被拒绝，不存在双插。

**也澄清一次失败的复现**：先查后插的窗口在本地单实例下**无法通过 HTTP 稳定复现** —— MySQL 的 INSERT 会阻塞同名并发插入。实测三种角色各 6 轮（2 线程）与各 5 轮（8 线程），全部是「1 个 200 + 其余 409」，没有出现双插、也没有 500。

**真实缺陷**：唯一索引虽然挡住了双插，但 `DuplicateKeyException` 无人处理，会落到 `GlobalExceptionHandler` 的兜底分支变成 **500「系统错误」**。这条路径在当前部署下难以撞到，但在**多实例部署**或**将来把先查后插改成直接插入**时就是唯一的正确性来源 —— 那时它会立刻变成用户可见的 500。

**修复**：`UserService.insertUser` / `AdminService.add` / `CinemaService.insertCinema` 三处 `mapper().insert(...)` 外包 `catch (DuplicateKeyException)`，转成与串行路径**逐字相同**的 `409 + "账号已存在,请更换别的账号"`。

**验证**：新增 `DuplicateUsernameGuardTest` 4 条用例（Mockito 让 mapper 抛 `DuplicateKeyException` 模拟"窗口被撞上"）；实测移除任一 `catch` 后对应用例立即变红。另有一条用例钉住「串行路径与撞键路径的提示必须一致」。

---

本轮修复并移出本表：**BL-02** —— 删排片与下单的竞态。

**先纠正原描述**：该条原写「订单会悬空（record_id 指向已删行）」，**实测不成立**。`ordered.record_id` 有外键 `ordered_ibfk_1 ... ON DELETE RESTRICT`，删除有订单的排片会被数据库直接拒绝，不可能留下悬空订单。

**真实缺陷**（实测 25 轮，21 次撞上竞态）：删除路径是 `selectById`（非锁定读）+ 非锁定 COUNT + DELETE。管理员数到 0 笔后、DELETE 前用户下单并提交，外键异常 `DataIntegrityViolationException` 落到 `GlobalExceptionHandler` 的兜底分支，变成一句 **500「系统错误」**——21 次里 19 次如此。管理员既不知道发生了什么，也收不到「该场次已有订单」这句本可以给出的提示。

**修复**：

1. `RecordService.deleteScoped` / `deleteBatchScoped` 改用 `recordMapper.selectByIdForUpdate(id)` —— 与下单路径抢同一把排片行锁，「数订单 → DELETE」之间不会插进新订单。
2. `GlobalExceptionHandler` 新增 `DataIntegrityViolationException` 处理，外键冲突返回 409 + 可操作提示（「如涉及场次请将状态改为「停售」」），不再漏成 500。

**验证**：修复后 25 轮并发，10 次删除被拒**全部**返回正确业务提示，零「系统错误」，零悬空订单。新增 `RecordDeleteGuardTest` 4 条用例（Mockito 直接盯调用序列）；实测把删除路径改回 `selectById` 后 3 条立即变红。

**踩坑记录**：写该测试时一度改成 `@SpringBootTest` + 真实库，结果**去掉锁后测试依然全绿** —— 因为真实库只能验证「有订单就拒绝」，而串行场景下锁不锁都成立。断言「调用了哪个 mapper 方法」必须用 Mockito。

---

本轮修复并移出本表：**BL-01** —— 排片时段重叠的「先查后插」。实测复现：两个并发请求对同一影厅同一时段提交，双双返回 200、库里落 2 场完全重叠的排片。

**修复分两步，缺一不可**：

1. `RecordService.validateSchedule` 在查重**之前**加 `RoomMapper.selectByIdForUpdate(roomId)`，锁影厅行 —— 让同一影厅的「查 → 插」串行。锁粒度选影厅，因为要保护的是"该影厅内所有排片"这个集合。
2. `RecordMapper.countRoomOverlap` 改为**锁定读**（`SELECT COUNT(*) FROM (SELECT ... FOR UPDATE) locked_rows`）—— 这一步是被实测逼出来的：只加第 1 步时**依然落 2 行**。原因是 MySQL 默认 `REPEATABLE-READ`，普通 `SELECT COUNT(*)` 是一致性非锁定读，读事务开始时的快照；等锁方拿到锁后看到的仍是旧快照，对方刚提交的排片它看不见。改用当前读才能读到最新已提交版本。

**验证**：修复前 1/1 复现（2 行）；修复后 8/8 轮并发，每轮恰好一场落库、另一方 409，胜负交替（不是某一方恒定先到）。新增 `RecordOverlapLockTest` 4 条用例，其中 `countRoomOverlapUsesLockingRead` 断言 SQL 含 `FOR UPDATE` —— 实测把该片段删掉后它立即变红。

**遗留**：`updateScoped`（改期）同样经 `validateSchedule`，自动获得同一保护；但「改期」与「另一场新建」的并发仍靠同一把影厅行锁串行，属预期行为。库端仍无区间约束（MySQL 建不了"任意时段不重叠"的列级约束），故行锁是唯一防线，**不要移除**。

**本节已全部关闭（2026-10-19）**：

- **BL-04（名录导入并发）**：实测**推翻两处工单描述** —— 报的是死锁（非唯一键冲突）；库端 `cinema.directory_id` 原本**没有**唯一索引，同一名录可串行关联两次。修复三层：加 `uk_cinema_directory_id`、逐行捕获 `DuplicateKeyException` 按幂等跳过、`DeadlockLoserDataAccessException` 转 409 提示重试。2 线程 × 7 轮零 500、零重复。详见 `docs/domain.md` §2.4。
- **BL-05（JVM/DB 时钟混用）**：`pending_timeout_at` 改为 SQL `NOW() + INTERVAL 5 MINUTE` 写入（哨兵 `pendingTimeoutFromDb`），支付路径的超时判定改为 `countExpiredById` 并与清理任务共用 `pendingExpiredCondition` 片段。实测两会话 `NOW()` 相差 8 小时；修复后建单超时点距 DB `NOW()` 恰为 300 秒。守卫 `PendingTimeoutClockTest`（4 条，做过反证）。详见 `docs/domain.md` §3.4。

**⚠️ 并发结论的可回归性缺口（2026-10-19 审计）**：上面的并发结论**全部来自 CI 之外的手工实测**，`mvn test` 拦不住回归。实测 grep：test 目录里 `Thread` / `ExecutorService` / `CountDownLatch` / `CompletableFuture` **0 次命中** —— 全仓没有一条真实并发测试。

| 测试类 | 实际证明的 | **不能**证明的 |
| --- | --- | --- |
| `RecordDeleteGuardTest` | 删除路径调的是 `selectByIdForUpdate`（方法名） | 锁真的与 `insertOrder` 争同一把锁；并发下不再 500；(且 `deleteBatchScoped` 未断言 `deleteBatch` 被调用) |
| `PayOrderPurchasableGuardTest` | 拒绝分支没调钱包/密码服务 | **"先复核后扣款"的顺序** —— 注释写着"用 InOrder"，代码里根本没有 `InOrder` |
| `DuplicateUsernameGuardTest` | `catch (DuplicateKeyException)` 存在 | 唯一索引真的存在；两个线程真的会撞键 |
| `RecordOverlapLockTest` | SQL 文本含 `FOR UPDATE`、语句可执行 | 双连接竞争下是否真的串行 |
| `CinemaServiceTest#import…SkipsConcurrentDuplicate` | 逐行 try/catch 存在 | 并发下真的撞键（桩是单线程 `thenThrow→thenReturn`） |

**处置建议**：需要至少两条**双连接**测试（各自开事务、先 `SELECT … FOR UPDATE`、断言第二个阻塞或返回 1）：一条排片重叠（BL-01），一条下单 vs 删排片（BL-02）。没有它们，这一节的所有"已修复"都是**手工证据**，不是**可回归证据**。

## 测试覆盖缺口（2026-10-22 更新）

| 项 | 现状 | 说明 |
| --- | --- | --- |
| Controller | **23 个中 18 个零直接测试** | 已覆盖 `Health`、`Auth`（login/reset）、`Ordered`、`Recharge`、`Ticket`；其余 18 个仍无控制器切片。`MarkController.requireUser`、`UserController/CinemaController.requireAdmin` 等**方法级鉴权第二层**仍无直接覆盖。 |
| Service | **8 个零测试** | `RoomService`、`RecordService`（仅被锁测试间接触及）、`AuthService`、`CinemaDirectoryService`、`AreaService`、`ActorService`、`TypeService`、`VideoService`、`NoticeService`。`RoomService.validateSeatLayout`、`RecordService.validateSchedule` 未测。 |
| 定时任务 | **零覆盖** | `OrderCleanupTask` 及 `OrderedService.cancelExpiredPendingOrders` 无直接测试。 |
| 异常处理器 | 2 个分支未覆盖 | `DataIntegrityViolationException`（BL-02 修复引入的 409 路径）、`RestClientException`。 |
| 空表恒真断言 | 多处 | `CinemaDirectoryMapperLoadTest`、`StatisticsMapperLoadTest` 在空库上 `allSatisfy` / `>= 0` 恒真，只证明 SQL 语法合法。 |
| **mapper XML 参数名漂移** | **2026-10-23 已修并补守卫** | `OrderedMapper.countPickedUpByUserAndFilm` 的 XML 写 `#{pickedUp}`，方法签名只有 `userId/filmId` → 评论区与发评价在真库上恒抛 `BindingException`（500）。Mockito 单测不碰 XML，故 403 条测试全绿却漏掉。**修复**：改走 `@Param("s") OrderStatusCriteria`（与「状态取值唯一来源」同口径）+ `MarkEligibilityMapperLoadTest` 钉住，已做故障注入反证（改回 `#{pickedUp}` 该测试变红）。同类风险：其余「XML 引用了签名里没有的参数」暂无通用守卫，新增语句时请配一条真库执行测试。 |
| **演示脚本重跑的余额漂移** | **2026-10-23 修复** | `demo-data.sql` 里既有账号 `111` 的余额会被 `§8` 按流水末条回写，重跑时若直接拿当前余额当起点，`100 → 174 → 248 → …` 一路漂。**修复**：`§1` 先 `UPDATE user SET balance = 100.00` 还原起点；自检补「流水首尾未接续」「近 7 日有 0 票房的日期」两项，首跑与重跑各 9 项全 0。 |
| 前端 | **零测试** | `package.json` 无 test 脚本。 |

## 状态机与契约

| 编号 | 位置 | 问题 | 处置建议 |
| --- | --- | --- | --- |
| BL-06a | db/record.status | `record.status` 是自由字符串（`"正常"`/`"停售"`），库端无 `CHECK`。2026-10-19 验证脚本误写 `"已停售"` 时 `UPDATE` 不报错、`isPurchasable` 也认不出来，导致「停售场次仍可支付」被误判为通过 —— 同样的错误若发生在手工改库或导入里，会静默变成「永远可售」。 | 加 `CHECK (status IN ('正常','停售'))` 或改为枚举列。 |

本节原登记 **BL-06（支付不复核场次可购性）**，2026-10-19 已修复并移出：`payOrder` 在验密与扣款之前，用 `recordMapper.selectByIdForUpdate` 锁排片行复核 `isPurchasable`；不可购时报 409 且一分钱不动。实测复现与修复细节见 `docs/domain.md` §3.3，守卫 `PayOrderPurchasableGuardTest`（5 条，做过禁用复核的反证）。

本轮修复并移出本表：BL-07 —— update 路径收口。新增 `common/Identifiable`，`BaseService<T extends Identifiable>.update` 前置判存在（主键缺失或查无此行抛 NOT_FOUND），User/Admin/Cinema/Film 的 `update` 覆写改为委派 `super.update`；OrderedService.update 需回读库行回填 pending_timeout_at，就地判存在，口径相同。行为变更：陈旧表单（行已删）由 200 变 404。

## 性能

| 编号 | 位置 | 问题 | 处置建议 |
| --- | --- | --- | --- |
| BL-08 | service/OrderedService.insertOrder | 逐座位调用 countSeatInUse（每单最多 6 次查询）。 | 复用已有的 selectActiveByRecordId(recordId) 一次取回，再在 Java 内求交。 |
| BL-09 | service/RecordService/RoomService/UserService.deleteBatchScoped | 批量删除逐个 id 查存在性与引用计数，N 个选中 = 2~3N 次查询（RoomService 最重）。 | 增加按 IN 批量的 selectByIds 与 countByXxxIds，单批降至 1~2 次查询。 |
| BL-10 | service/StatisticsService.summary | overview 已取回 countGroupByStatus()，summary 又单独发 countByStatus("待取票")，同页对 ordered 多发一次 COUNT，两次读之间还有快照窗口。 | 复用同一份分组结果（countOf 派生）。注意改后 pendingPickupOrders 的 Java 类型由 Integer 变 Long，需同步 3 处测试桩。 |
| BL-11 | service/TmdbBackfillService.backfillVideos | 用 selectAll(new Film())（含票房聚合与 area/actor JOIN）取全表再在 Java 里过滤 video 为空的行。 | 改投影查询 SELECT id,title,... WHERE video IS NULL OR video=''，避开聚合。 |
| BL-12 | service/TmdbService.resolveTypes | 逐类型 selectByTitleExact 并按需 insert（≤4 次）。 | 批量 selectByTitleExact(IN ...) 一次取回。管理员低频，优先级低。 |
| BL-55 | mapper/OrderedMapper.xml:150-181；schema.sql 的 ordered 索引 | `ordered.pay_time` **无索引**；`selectTodayPaidRevenue`、`countTodayPaidOrders`、`selectPaidRevenueByDay` 都按 `pay_time` 过滤，EXPLAIN 为 `type=ALL`（当前 243 行）。今日票房在首页每次加载都查。 | 加 `KEY idx_ordered_pay_time (pay_time)` 或 `(pay_time, status)`；改表后按 `docs/engineering-rules.md` §2 重新生成 `schema.sql` 并同步真实库迁移。 |
| BL-56 | common/config/WebMvcConfig.java:24-27 | `/files/**` 只返回 `Last-Modified`，**没有 `Cache-Control` / `ETag`**。上传文件名是 UUID、内容不变，重复访问仍会条件请求。 | `setCachePeriod(86400)` 或 `CacheControl.maxAge(...)`；文件名唯一，可放心长缓存。 |
| BL-57 | application.yml（server 段） | 未开启 HTTP 压缩；`/films` 的 42,681B JSON 响应没有 `content-encoding`。 | 开 `server.compression.enabled: true`，`mime-types` 含 `application/json`。 |
| BL-58 | resources/logback-spring.xml:7-9 | `com.example.springboot.mapper` 固定 **DEBUG**，所有 SQL 与结果集都写控制台，开发/生产同样生效。 | **2026-10-25 已修复**：默认 INFO；需要看 SQL 时在 `.env` 里设 `logging.level.com.example.springboot.mapper=DEBUG`。 |
| BL-59 | mapper/FilmMapper.xml:12-19、65-84 | 所有影片列表/详情/榜单/搜索都 `LEFT JOIN` 一个**全表 `ordered` 聚合子查询**；首页一次加载触发 3 次（`/films`、票房 Top、评分 Top）。EXPLAIN 当前扫 243 行。 | 数据量上来后考虑 `(status, film_id, total)` 覆盖索引；或把 `film.box_office` 恢复为支付/退票时维护的派生列；或只对需要票房的接口带聚合。 |
| BL-60 | mapper/FilmMapper.xml:77；mapper/RecordMapper.xml:31 | `film.start LIKE '%year%'`、`record.start LIKE '%date%'` 让 `idx_start` 失效，日期/年份筛选退化为全表扫描。 | 前端传完整日期时改范围查询（`>= day AND < day+1`）。 |
| BL-61 | mapper/MarkMapper.xml:55-62 | 影评列表每行两个相关子查询（`likeCount`、`liked`），分页前对整片评价逐行计算。 | 用 `mark_like` 聚合 JOIN / 派生表一次算好，或缓存点赞数。当前 25 条评价无感。 |
| BL-62 | common/BaseController.list；mapper/FilmMapper.selectAll | 集合根无分页上限，且全列返回（含 `content` 长文本）；后台下拉也全量拉。首页的消费方问题见 FE-69。 | 列表改投影/分页；下拉只取 id/title 等轻量字段。 |

**2026-10-23 复核备注**：BL-08 / BL-09 / BL-12 仍成立；BL-10 的现状已与描述不符 —— `StatisticsService.summary` 现在复用 `countGroupByStatus()` 的结果（`countOf`），BL-11 也已在 `TmdbBackfillService` 改用 `selectMissingVideo`。本轮只读、未重跑测试，因此只记备注、未改这两条的状态。

## 重复代码与可维护性（2026-10-20 专项：BL-13/14/15/16 已修复）

| 编号 | 位置 | 问题 | 状态 |
| --- | --- | --- | --- |
| BL-13 | service/UserService、AdminService、CinemaService、PayPasswordService | 凭证比对被抄 4 处（"BCrypt.matches 或明文 equals"），4 个类各 new 一个 BCryptPasswordEncoder | **已修复** → `common/PasswordMatcher`。空值由 NPE 改为不匹配。**明文回退刻意保留**（SEC-03，只收敛不改行为）。测试 `SharedComponentTest` 明确标注 `plaintextFallbackStillPassesForLegacyRows` 记录的是缺陷现状，修 SEC-03 时应改为 false 并会变红 |
| BL-14 | service/OrderedService.parsePrice、RecordService.parsePrice | 逐字重复，仅 ErrorCode 与文案不同 | **已修复** → `common/PriceParser`。抽的过程中发现真实口径分裂：录入侧 400、下单侧 500。另补捕 `ArithmeticException`（`1e999999999` 构造成功但 setScale 溢出，原先漏成 500）。**可达性如实记录**：`record.price` 是 `decimal(10,2)`，库层拒绝非法字符串（实测 SQL 直接报错），下单侧不可达；这是口径统一 + 未来防线，不是正在发生的 500 |
| BL-15 | 时间格式串 4 处 + now() 2 处 | 同上 | **已修复** → `common/DateTimeFormats`（格式）+ `common/SystemClock`（当前时间）。刻意不叫 `Clock`：那会让人以为可替代数据库时钟，而超时判定必须用 DB 时钟（BL-05 根因） |
| BL-16 | generateOrderNo / generateRechargeNo | 日期 + UUID 前 8 位大写，仅前缀不同 | **已修复** → `common/OrderNumber`。订单号无前缀、充值单号 `R` |
| （本轮追加） | service/OrderedService.normalizeSeats | 座位解析（格式/范围/规范化/去重）埋在订单状态机里；规范化是防双卖的唯一防线（库里座位是逗号串，占用检查走 `FIND_IN_SET` 逐字节比对） | **已修复** → `common/SeatParser`。`OrderedService` 622 → 587 行，少一类职责 |
| （本轮追加） | `ErrorCode.X.code()` 16 处 | 每个调用点都把错误码翻成字符串再传给 `Result.error` | **已修复** → `Result.error(ErrorCode, msg)` 重载 + `common/SuccessCode`。服务端 `"200"` 字面量收敛到一处 |

**本轮追加的两条实测边界（既有行为，非本次引入）**：

1. `SeatParser` 的格式正则限定 1–2 位数字，所以 `"01排001座"` 会被判为格式错误而不是归一 ——
   前导零只可能出现在"单数字前补一个零"（`1排01座`），该形态能正确归一。已在测试里钉住这两面。
2. `PasswordMatcher.matches(encoder, "", "")` 返回 **true**：明文回退那一半让空口令通过空密码行。
   这是 SEC-03 同源的既有行为，本轮未改，测试里写了注释。

**反证记录**：`PriceParser` 错误码改回 `SYSTEM_ERROR` → 对应用例红；
`SeatParser` 去掉规范化（改回存 `raw`）→ 防双卖用例红。两次均确认已精确还原。

| 编号 | 位置 | 问题 | 处置建议 |
| --- | --- | --- | --- |
| BL-17 | service/RecordService/RoomService 的 requireAdminOrCinema、applyCinemaScope；OrderedService/RecordService/RoomService 的 ensureXxxAccess | ADMIN 放行、CINEMA 归属 的守卫与 scope 谓词按实体各写一份。 | 在 AuthContext 旁抽统一的角色守卫。**未做**：与 BL-13 不同，这几处的差异是真实的实体语义（谁的 id、查哪张表），强行合并会引入参数化歧义，收益低于风险。 |

## 数据库设计（2026-10-20 专项）

**背景**：DB 维度 82 分的三条扣分是「`seat` 是逗号串」「状态是自由字符串」「金额用 Double」。核实时先纠正了一处事实：**库列本来就是 `decimal(10,2)`**，Double 只存在于 Java 实体/DTO —— 所以第三条的实质与描述不同，见 BL-24。

| 编号 | 问题 | 实测/复核 | 处置 |
| --- | --- | --- | --- |
| **BL-31** | 状态值有**三份副本**：库列（无约束）、Java `*Status` 常量、mapper XML 里的中文字面量 | 状态列是 `varchar(20)`，库层不拒绝任何字符串。改了常量名或取值，编译器和数据库都不报错，SQL 只是从此匹配 0 行 —— 表现为「核销没反应」「超时单清不掉」，日志里一切正常 | **已修复**：新增 `common/OrderStatusCriteria`，mapper 通过 `@Param("s")` 取值，`OrderedMapper.xml` 里的状态字面量全部消除。唯一例外是 `paidOrderStatuses`（被 `FilmMapper` 跨 namespace 静态引用，那里 8 个方法都没有参数入口），由 `StatusLiteralGuardTest` 钉住 |
| **BL-53** | `ordered.seat` 是逗号分隔串，占用检查用 `FIND_IN_SET` | 这不是单纯的设计口味：查重逐字节比对，`1排01座` 与 `1排1座` 在库里是两个不同的串 | **只加固、不改表**：规范化收敛到 `common/SeatParser`（上一轮），并有故障注入测试。拆成 `order_seat` 子表属"改表"量级（建表 + 迁移存量串 + 改写占用检查 + 十余处展示契约），留待与 BL-18/19/20 同轮。原误用 BL-30，2026-10-22 改为 BL-53；BL-30 保留给文件上传 |
| **BL-24** | 订单 `total/payAmount/refundAmount` 用 `Double` | **纠正原描述**：库列早已是 `decimal(10,2)`，Double 只在 Java 侧。实测 `BigDecimal → double → BigDecimal.valueOf` 在 decimal(10,2) 范围内**往返无损**（8 组样本 0 损失），但这个结论依赖 `valueOf` 走 `Double.toString`，是巧合不是保证；累加才会失真，而代码里金额聚合都在 SQL 里 | **已修复**：4 个类的 3 个金额字段全部改 `BigDecimal`，`OrderedService` 里两处 `doubleValue()`/`valueOf()` 转换随之消失。JSON 形态实测仍为数字（`45.5`），前端 `Number(...)` 与插值行为不变。**`Film.boxOffice` 未改**：该列已废弃（恒 0，票房实时聚合），一并改会牵动 FilmMapper 的 `COALESCE(rev.revenue, 0)` 与前端榜单 |

**本轮刻意未做（写明理由，避免被当成遗漏）**：

| 事项 | 未做的原因 |
| --- | --- |
| 给状态列加库级 `CHECK` | MySQL 8 的 CHECK 能拦非法写入，但需要先校验存量数据、并同步测试库与演示数据脚本；本轮改动已能保证**读路径**不吃到非法值（取值来自常量），写入路径由 Service 归一化。加约束属下一轮的迁移工作 |
| `seat` 拆子表 | 见 BL-53，改表 + 迁移 + 十余处契约，风险量级不同 |
| `Film.boxOffice` 改 BigDecimal | 列已废弃、恒为 0；改它只增加 diff 不增加正确性 |
| 重新生成 schema 快照 | 本轮**没有改任何列类型**，快照仍然准确。重跑 `dump-schema.py` 只会产生 AUTO_INCREMENT 漂移与编码转义噪音 |

**故障注入反证**：在 `OrderedMapper.xml` 里把一个参数化的状态改回裸字面量 `'待取票'` → `StatusLiteralGuardTest` 立刻红，报「出现了裸状态字面量，应改为 #{s.xxx}」（已确认还原）。

**HTTP 实测（0 条真实不符预期）**：订单金额在 JSON 里是数字；占座后换写法再占 409；已超时单支付 → 409「支付超时」且库里状态变「已取消」；凭码核销 → 200 且状态变「已取票」，再核销 → 409；今日票房与大盘概览正常。

## 实体层遗留（承接 S10）

| 编号 | 位置 | 问题 | 处置建议 |
| --- | --- | --- | --- |
| BL-18 | entity/ 全 17 类 | 实体混入非持久化展示/派生字段并直接对外：Film.areaName/typeList、Cinema.province/city/district、Ordered/Mark/Record 的 join 字段。 | S10 展示字段剥离，移入对应 View（需改 JOIN 查询 resultType 与 Service 签名）。架构级，用户另立一轮。 |
| BL-19 | entity/Account 及 User/Admin/Cinema | token/newPassword 置于 Account 被三子类继承；三子类 @AllArgsConstructor 只覆盖自身字段、不含父字段（误用会建出 role/password 为 null 的账号，当前无调用者）。 | S10 继承解耦，把 token/newPassword 移出 Account（需改 login/updatePassword 签名）。 |
| BL-20 | entity/Admin 与 entity/User | 两实体字段完全一致；avatar/phone 在 Admin/User/Cinema 三处重复声明。 | 上提至 Account 或抽 Profile 基类（与 BL-19 同轮）。 |
| BL-22 | entity/CinemaDirectory.createTime | 全仓唯一非 String 时间字段（LocalDateTime），序列化为 ISO 带 T 串，与其余 String 时间字段口径不一；前端未读取。 | 统一为 String，或全仓统一改 LocalDateTime（需联动 mapper/DTO/前端）。 |
| BL-23 | entity/Room.title | 存的是所属影院名（RoomService 由 cinema.getName() 派生），而 name 才是影厅自身名。命名与语义不符，是未标注的冗余快照。 | 改名或去除（需联动 mapper/Service/前端）。 |
| BL-24 | entity/Ordered 的 total/payAmount/refundAmount、entity/Film.boxOffice | 金额用 Double，而同实体 unitPrice、RechargeOrder.amount、FundFlow 各额均 BigDecimal。 | **订单三字段已改为 BigDecimal（2026-10-20）**，详见上文 §数据库设计。`Film.boxOffice` 未改（列已废弃、恒为 0）。 |
| BL-41 | 库表 actor 的关联列（`film.actor_id` 与 `actor.film_id`）；views/front/FilmDetail.vue 的四分组 | 演职人员与影片的关联存在两套机制：`film.actor_id` 单值外键（1:1，在用，FilmMapper 多处 JOIN）与 `actor.film_id`（库中已建且带非唯一索引，但全仓无任何读写、`film_id IS NOT NULL` 实测 0 行）。前端按 `grade` 分导演/主演/编剧/二级演员四组，但库中 actor 65 行 `grade` 全为「主演」、film 又只关联单行，故非主演三组无数据可显。 | 要支持多角色展示须先定唯一关联口径（保留 `film.actor_id` 还是改走 `actor.film_id`，二者不可并存），再落 1:N 写路径 + 数据导入；当前无数据、无写路径，属新特性而非缺陷，另立一轮。FE-48 的「读错键名」缺陷已在前端就地修复（见前端审计报告）。 |

## 规范建议遗留（承接 S4/S6/S15）

| 编号 | 位置 | 问题 | 处置建议 |
| --- | --- | --- | --- |
| BL-25 | S4：controller/OrderedController、MarkController、CinemaDirectoryController、TicketController | 路径用动词（/create、/cancel、/pay、/refund、/{id}/like、/redeem）；/import/preview 只读却用 POST；PUT 被用于 cancel/pay 等非幂等语义。 | 改资源式路径与方法（需联动前端与 AuthInterceptor 白名单）。对本地演示无收益，2026-10-12 已决定跳过。 |
| BL-26 | S6：exception/GlobalExceptionHandler | 全局异常一律 HTTP 200、业务码在 body。凭证类错误（约 10 处）用 UNAUTHORIZED，若改 HTTP 状态会令"输错支付密码"→401→前端清会话跳登录；且前端约 5 处依赖 `code !== 200` 做功能分支。 | 先重判凭证类业务码，再连同前端一起改。已决定跳过。 |
| BL-27 | S15 纯风格项：test/ 根包类、*LoadTest 命名、GlobalExceptionHandlerTest 名实不符、JUnit Assertions 与 AssertJ 并存、OrderedServiceTest 用 @SpringBootTest 而非 MockitoExtension | 不改变覆盖与行为，翻动面大、收益低。 | 已决定跳过。 |

## 健壮性（低优先 / 未证实可达）

本轮修复并移出本表：BL-28 —— Record/Room/User 的 `deleteBatchScoped` 各补空 ids 守卫，与同签名的 `OrderedService.deleteBatchScoped` 同款抛 `PARAM_INVALID`（"请选择排片/影厅/用户"）。行为变更：空 ids 由静默 200（经 `BaseService.deleteBatch` 兜底）变 400；该入参现由前端不可达，属口径对齐。

2026-10-15 补充（同层；与前端 FE-34 同形）：common/config/AuthInterceptor 的三个前缀集合原本用裸 `startsWith` 匹配，没有路径分隔符边界 —— 与某个前缀只差字符相邻的新端点会静默继承该档位（放行的被多放行、拦下的被多拦下）。已抽出 `matchesPrefix`（相等，或以 `前缀 + '/'` 开头），`isAnonymousRead` / `hasAccess` 三处调用点全部换用；AuthInterceptorAccessTest 补 3 条用例，其中 2 条在改回裸匹配时各失败一条（已实测）。全部 23 个控制器基路径实测无碰撞，即改动前也无可达缺陷，属防未来误判。

| 编号 | 位置 | 问题 | 处置建议 |
| --- | --- | --- | --- |
| BL-29 | entity/Cinema.province/city/district | /auth/me 与 /login 经 CinemaMapper.selectById/selectByUsername（SELECT *，无 JOIN）取影院，区域字段恒为 null；/cinemas/page 则有值。UNCERTAIN：当前无前端读取该路径的区域。 | 若需在登录态展示区域，给该查询补 JOIN。 |

## 控制器层遗留（2026-10-14 控制器审查）

本轮修复并移出本表：BL-33 —— 删除 RechargeController / FundFlowController / CinemaDirectoryController 三个 `toEntity` 开头的死守卫 `if (request == null) return null;`（query 由 Spring 绑定，恒非 null）。BL-34 —— 删除 OrderedController.getById 的死三元（`selectByIdScoped` 经 `ensureOrderAccess` 在 null 时先抛 NOT_FOUND）。**同形点甄别**：UserController.getById 的 `user == null ? null : toView(user)` 不是死代码 —— `UserService.selectByIdScoped` 直接返回 `BaseService.selectById`（不抛），可为 null，故保留；其 200+null 与 Record/Room/Ordered 的 404 之间的口径分歧另登 BL-43。

| 编号 | 位置 | 问题 | 处置建议 |
| --- | --- | --- | --- |
| BL-30 | controller/FileUploadController.uploadFile（落盘在 common/FileUtil） | `/api/v1/files/upload` 不在 ADMIN_WRITE_PREFIXES / ADMIN_ONLY_PREFIXES，任何已登录角色（含 USER）都能上传。本轮已把落盘扩展名从客户端文件名改为按已校验 MIME 派生（堵掉 `.html` 落盘 + MIME 可伪造那条），但"谁可上传"未收紧。UNCERTAIN：CINEMA 自助改头像/资料是否依赖此端点。 | 确认用途后给前缀加写门禁，或按角色拆端点。 |
| BL-54 | controller/CinemaDirectoryController.page | 本轮已给 BaseController 抽出 `toPageView`，但本类不继承 BaseController，`PageInfo` 拷贝 + toView 映射仍自成一份（与 BL-13 同类的映射重复）。原误用 BL-31，2026-10-22 改为 BL-54。 | 抽静态 `PageMapper.map(PageInfo<S>, Function<S,T>)`，基类与独立控制器共用。 |
| BL-32 | controller/AuthController.updatePassword、me | 手工 `httpRequest.getAttribute("userId"/"role")` 再 `Integer.valueOf`，绕过 AuthContext.role()/userId()；身份存放方式一改此处静默失效（AuthContext 类注释正警告此坑）。 | 改用 AuthContext（`me` 的 userId 参数需由 String 放宽为 Integer）。 |
| BL-35 | controller/StatisticsController.overview、controller/AccountController.requireUser | 角色门禁在方法内逐端点各写一份（`/api/v1/statistics` 未进任何前缀表），新增端点易漏（与 BL-17 同类）。 | 统计前缀加 ADMIN_ONLY_PREFIXES；账户前缀加统一 USER 守卫，或与 BL-17 同轮抽统一守卫。 |
| BL-43 | controller/UserController.getById | 管理员查不存在的 id 返回 200 + `data:null`；Record/Room/Ordered 的同名方法经 ensure 抛 NOT_FOUND（404）。根因是 `UserService.selectByIdScoped` 直接返回 `selectById`（不判存在），与其余 scoped 读不一致。 | 若要统一：`UserService.selectByIdScoped` 补存在性判定抛 NOT_FOUND。属行为变更（该路径现为 200），先定口径。 |

## Mapper 层遗留（2026-10-14 mapper 审查）

本轮修复并移出本表：BL-36 —— 该条所列 13 个 mapper 之外漏登 RechargeOrderMapper，实为 14 个；全部在 `updateById` 的 `<set>` 末尾加 `id = #{id}` 自赋值兜底，SET 恒非空，`UPDATE t WHERE id=?` 触发的 1064 不再出现。实测渲染：`UPDATE area SET id = ? WHERE id = ?`。BL-38 —— 字符串筛选的空串判定，按 CinemaMapper 既有口径（`x != null and x != ''`）补齐 6 个 mapper 共 12 处：CinemaDirectoryMapper（name/province/city/district/brand）、FundFlowMapper（source）、OrderedMapper（status）、RechargeOrderMapper（status）、RecordMapper（title/start/status）、FilmMapper（year）。未补的同形点见 BL-42。

| 编号 | 位置 | 问题 | 处置建议 |
| --- | --- | --- | --- |
| BL-42 | mapper/ 其余字符串筛选：ActorMapper.actorName；AdminMapper.name；AreaMapper.title；NoticeMapper.title；TypeMapper.title；VideoMapper.title；UserMapper.name/username/phone；RoomMapper.name；CinemaMapper.selectAll.name（共 11 处） | 与 BL-38 同形：`<if>` 只判 `!= null`、不判空串。（CinemaMapper 的 selectByFilmId 已带判定，此处为其 selectAll 简易查询。）空串入参落成 `= ''` / `LIKE '%%'` 查出零行。 | 同 BL-38 补 `and x != ''`。**本轮未做（登记备查）**：这些文件为 mixed/CRLF 行尾，改一行即整篇重写 EOL（实测 AdminMapper 原始 diff 16 行 vs `--ignore-cr-at-eol` 1 行），噪声大于收益。**2026-10-18 复核**：空串入参不可达 —— `useCrud.searchForm` 起于 `reactive({})`（不发键，`onReset` 写 `undefined`），文本输入框均无 `clearable`；`manage/CinemaDirectory.vue:141` 的 `|| undefined` 归一属于 `query.city/brand`（clearable 下拉），该页并**没有** `searchForm`，旧文把两处事实混写在一句里。真正 clearable 的输入类控件是 `manage/Admin.vue:12`、`manage/User.vue:12`，清空得 `''`，但只经 `onSearch` 发出，`onReset` 已把键写为 `undefined`。结论不变。 |

## DTO 层遗留（2026-10-14 dto 审查）

本轮修复并移出本表：BL-21（entity/Film.ids 死字段）、BL-37（/cinemas 的 filmId 静默失效，selectAll 已增 filmId 形参透传）。

| 编号 | 位置 | 问题 | 处置建议 |
| --- | --- | --- | --- |
| BL-39 | dto/ 的同名 Request/View 对（Actor、Area、Type、Video、Room、Record、FundFlow、Notice、RechargeOrder、CinemaDirectory、Ordered，以及 MarkRequest 与 MarkEntityView，共 12 对） | 同名字段逐字重复、各对字段完全一致：增删改一个字段要在两个 DTO 加 toEntity、toView 共四处同步，无机制保证两列表不漂移。 | 合并为单一 DTO，或加断言两列表相等的测试；属架构级，另立一轮。 |
| BL-40 | dto/request/RecordRequest 的 cinemaName/roomName/roomSeatRows/roomSeatCols；dto/request/OrderedRequest 的 userName/filmName/cinemaName/roomName | join 派生字段只作 SQL 输出，却落在请求 DTO 上成为可绑定入参；toEntity 逐层拷贝后 mapper 不写这些列，入参被静默丢弃。 | 入参 DTO 剥离这些只读字段（与 BL-18 实体展示字段剥离同轮）。 |

## 有意不整改（仅限本机配置项）

- S14 子项：application.yml 中明文 DB 口令、TMDB token、高德 key；file.upload-dir 绝对路径。**2026-10-25 已把前三项移入 `.env`（与 SEC-02 同批修复）**；`upload-dir` 保留本机默认值，可用 `UPLOAD_DIR` 覆盖。
- **同文件的 `jwt.signing-key` 已于 2026-10-25 移入 `.env` 的 `JWT_SIGNING_KEY`，并在 `JwtUtils` 启动时校验长度**（SEC-02 修复）；当前密钥必须视为已泄露并更换。

## 已核验为非缺陷（防二次误报）

- service/RecordService.selectByIdScoped 忽略 role/userId：排片信息（价格/时间/影片/影厅）本属公开可读，非越权，是按存在性校验的既定口径。
- 订单资金路径（WalletService 行锁 + balance>= 守卫、payOrder/refundOrder 的 selectByIdForUpdate、markPickedUpByCode 条件更新、PayPasswordService 的 REQUIRES_NEW 计数）：审查核验无双重扣款/负余额/幂等缺口。
