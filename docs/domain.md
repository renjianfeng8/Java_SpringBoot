# 业务域

本文件是「系统怎么运转」的单一出处。实现细节以代码为准；此处只写规则与它们的理由。

## 1 角色

三个账号表各自独立、互不 JOIN：`admin`（角色 ADMIN）、`cinema`（CINEMA）、`user`（USER，有余额与支付密码）。
`entity/Account` 是三者的父类（**无对应表**），`token` / `newPassword` 挂在它上面（既定取舍，不打算拆）。

管理员重置他人登录密码只走 `PUT /auth/password/reset`，只允许 ADMIN，不校验旧密码，新密码 6–64 位并一律 BCrypt 入库；重置会吊销该账号已签发的 JWT（`TokenRevocationService` 按 `iat` 截断，本人改密同样生效），旧令牌在下一次请求即被视为失效。管理端订单的取消/退票复用同一状态机，管理员也不能绕过映前 60 分钟退款截止。

关键：**影院的 `id` 同时也是它的「用户 id」**。`ordered.cinema_id` 与 token 里的 `userId` 在 CINEMA 语境下是同一 id 空间，所以 `ensureOrderAccess` 能直接比。

### 1.1 登录失败锁定

- 同一 `role` + 账号连续失败 **5 次锁 15 分钟**；成功登录清零。
- 计数主体是**库中解析出的账号 id**，不是请求里的用户名。MySQL `utf8mb4_unicode_ci` 会把大小写、重音、全角、尾随空格视为同一账号；按原文本计数会让人用拼写变体各拿一份 5 次，锁定形同虚设。
- 锁定检查在验密之前；已锁定账号再试不延长锁定期，也不增加计数。
- 计数在单进程内存（`LoginAttemptService`），重启清空、多实例不共享；换 Redis/DB 前不要假设多节点口径一致。
- 支付密码有独立的 5 次/15 分钟计数（§5），两者互不影响。

## 2 排片（record）

一场 = `cinema_id + room_id + film_id + start + price + status`。

| 规则 | 实现 | 位置 |
| --- | --- | --- |
| 可购票性 | `start` 晚于当前时间且 status ≠ 停售 | `RecordService.isPurchasable`（静态，全场唯一权威） |
| 展示状态 | 未开始 / 放映中 / 已结束**由 start 派生**，不落库 | 前端 + `RECORD_STATUS` |
| 排片冲突 | `countRoomOverlap(room, id, start, start+片长)`；**先锁影厅行再查（当前读）** | `validateSchedule`；见下 |
| 片长兜底 | 影片 `time` ≤ 0 时用 120 分钟 | `DEFAULT_DURATION_MINUTES` |
| 删除守卫 | 已产生订单的场次禁止物理删除；`deleteScoped` 先锁行再数订单 | `ensureNoOrders`（BL-02，见 §2.2） |
| `room.title` | 存的是**影院名**，`room.name` 才是影厅名；无展示点，不打算改 | — |

### 2.1 排片并发（BL-01，2026-10-18 修复）

「同一影厅不能有重叠场次」在**数据库层无法表达**（那是区间约束，不是列约束），只能靠应用层。两道防线缺一不可：

1. **锁影厅行**：`validateSchedule` 在查重之前 `SELECT * FROM room WHERE id=? FOR UPDATE`，同一影厅的「查 → 插」因此串行，不同影厅互不影响。
2. **查重必须是当前读**：`countRoomOverlap` 写成 `SELECT COUNT(*) FROM (SELECT ... FOR UPDATE) locked_rows`。

**为什么第 2 条不能省**：MySQL 默认 `REPEATABLE-READ`，普通 `SELECT COUNT(*)` 是一致性非锁定读，读的是事务开始时的快照。实测只做第 1 条时仍然落 2 行 —— 等锁方拿到锁后，看到的还是旧快照，对方刚提交的排片它看不见。

**回归守卫**：`RecordOverlapLockTest.countRoomOverlapUsesLockingRead` 断言 SQL 含 `FOR UPDATE`；删掉该片段测试立即变红。改这条语句前先看它的注释。

### 2.2 删排片与下单（BL-02，2026-10-18 修复）

**原描述需要纠正**：该条原写「订单会悬空（record_id 指向已删行）」。实测不成立 —— `ordered.record_id` 有外键 `ON DELETE RESTRICT`，删除有订单的排片会被数据库直接拒绝。

**真实缺陷**：删除路径原是非锁定读 + 非锁定 COUNT + DELETE。管理员数到 0 笔后、DELETE 前用户下单并提交，外键异常落到兜底分支变成 **500「系统错误」**（实测 25 轮 21 次竞态，19 次如此），管理员收不到「该场次已有订单」这句本可给出的提示。

**修复**：`deleteScoped` / `deleteBatchScoped` 先 `selectByIdForUpdate` 锁行，再数订单；`GlobalExceptionHandler` 补 `DataIntegrityViolationException` → 409 + 可操作提示。

**教训**：写守卫测试时先用了 `@SpringBootTest` + 真实库，**去掉锁后依然全绿** —— 串行场景下锁不锁都成立。断言「调用了哪个 mapper」必须用 Mockito 盯调用序列。

### 2.3 并发注册同名（BL-03，2026-10-18 修复）

**原描述需要纠正**：该条原写「并发注册同名双双通过落两行」。实测不成立 —— 三张账号表都有 `username` 唯一索引，第二个 INSERT 会被拒绝。

**也记一次失败的复现**：先查后插的窗口在本地单实例下**无法通过 HTTP 稳定撞出**。MySQL 的 INSERT 会阻塞同名并发插入，实测 2 线程 × 6 轮、8 线程 × 5 轮、三种角色，全部是「1 成功 + 其余 409」。

**真实缺陷**：唯一索引挡住了双插，但 `DuplicateKeyException` 无人处理，会变成 500「系统错误」。当前部署难以撞到，但多实例部署或改成直接插入时它立刻暴露。

**修复**：三处 `insert` 外包 `catch (DuplicateKeyException)`，转成与串行路径同一句提示。

**守卫测试的写法**：Mockito 让 mapper 直接抛 `DuplicateKeyException`，模拟"窗口被撞上"。这是唯一能稳定测到该分支的方式 —— 走 HTTP 永远撞不到。

### 2.4 名录导入并发（BL-04，2026-10-19 修复）

**工单描述的偏差**：原写「并发导入同一名录 id，各读 linked=false 双插同名影院；命中唯一键则整批回滚报 500」。实测有两处不同：

1. **报的不是唯一键冲突，而是死锁**。`DeadlockLoserDataAccessException` —— 两个事务各插不同名录行，却争同一把 `username` 索引锁。
2. **库端原本没有防重复的约束**。`cinema` 表只有 `username` 唯一索引，`directory_id` **没有任何唯一索引**，因此"同一名录关联两家影院"在串行下也能直接插进去（实测用不同 username 插入同一 `directory_id`，成功）。

**实测复现（修复前）**：两个线程同时导入同一批 3 条未关联名录，一个 `created=3`，另一个 **500「系统错误」**。

**修复三件事**：

| 层 | 改动 | 理由 |
| --- | --- | --- |
| 库 | `ALTER TABLE cinema ADD UNIQUE KEY uk_cinema_directory_id (directory_id)` | 根本防线。`directory_id` 可为 NULL，MySQL 唯一索引允许多行 NULL，**不关联名录的影院不受影响** |
| 代码 | `importFromDirectory` 逐行 `catch (DuplicateKeyException)` → 跳过、不计入 `created` | 并发败者的正常结局是「别人已导入」，属幂等语义，不该报错 |
| 代码 | `catch (DeadlockLoserDataAccessException)` → 409「有其他人正在导入同一批名录，请稍后重试」 | 死锁是「可重试」，不是用户操作错误，不能变 500 |

**为什么不做 `INSERT IGNORE`**：需要区分「名录已关联」（跳过）与「username 撞了别的影院」（真冲突）。整批降级成 IGNORE 会把后者也吞掉。

**修复后**：2 线程并发 × 7 轮，全部「一人建 3 家、另一人 0 家或 409」，**零 500、零重复行**。两种分支都实际出现（`created=0` 是撞唯一键，`409` 是撞死锁），说明两条路径都覆盖到了。

**顺带纠正一处错误注释**：`insertCinema` 原写「名录导入生成的 amap_+poiId 也走这里」，实际 `importFromDirectory` 直接调 `cinemaMapper.insert`、**不经过** `insertCinema`。正是这句注释掩盖了「导入路径没有 DuplicateKey 处理」这个事实。

**初始口令（2026-10-20，Sonar `java:S6437`）**：不再让导入影院共用硬编码的 `cinema123`。每个新账号生成独立的 12 位随机口令（`SecureRandom`，剔除易混字符），库中只存 BCrypt 哈希；明文只在 `POST /api/v1/cinemas/from-directory` 的响应 `accounts[]` 中返回一次，后台名录页用弹窗展示并支持「复制全部」。`insertCinema` 对空口令返回 400，不再回退默认值。**这意味着导入后必须当场保存口令**：关掉弹窗后无法再次取回，只能走改密流程。

**库结构与快照同步**：新增索引已写入 `src/test/resources/db/schema.sql` 与 `inventory.tsv`，并在 `xm_film_test` 上实际添加。

## 3 订单状态机

> **2026-10-18 端到端实测**：在演示数据（`demo-data.sql`）上用真实 HTTP 接口走通了
> 登录 → 查场次 → 选座 → 下单 → 错误密码被拒 → 正确密码支付 → 拿到取票码 →
> 匿名核销 → 重复核销被拒 → 重复下单同座位被拒。余额 500 → 390（2 张 × 55 元）。
> 并发争夺同一座位 5 轮，3 个有效轮次全部「恰好一人成功」，后到者 26 ms 收到
> 「座位已售」。演示剧本见 `docs/demo-script.md`。

```
        ┌──────── cancelOrder / 超时清理 ────────┐
        ↓                                        │
   已取消                                   待支付 ──payOrder──→ 待取票 ──pickupOrder/redeemByCode──→ 已取票
        ↑                                        │                    │
        └────────────── 已退票 ←────refundOrder──┘◄───────────────────┘

可物理删除：仅 已取消 / 已退票
```

| 动作 | 方法 | 前置 | 资金 |
| --- | --- | --- | --- |
| 建单 | `insertOrder` | USER；存在场次；未停售；未开场；座位合法 | 不动钱包 |
| 支付 | `payOrder` | 待支付；未超时；**场次仍可购**（锁排片行复核）；**订单归属者的支付密码** | 扣余额 + 写流水 + 生成取票码 |
| 取消 | `cancelOrder` | 待支付 | 无 |
| 超时 | `cancelExpiredPendingOrders`（每 15s）+ `payOrder` 内分支 | 待支付且 `pending_timeout_at < NOW()` | 无 |
| 退票 | `refundOrder` | 待取票；距开场 > 60 分钟 | 退余额 + 写流水 |
| 柜台取票 | `pickupOrder` | 待取票；**仅 CINEMA** | 无 |
| 自助取票 | `redeemByCode` | 待取票；未过放映结束 | 无 |

### 3.1 座位占用

座位串存于一列：`ordered.seat = "1排1座,1排2座"`（`varchar(200)`）。占用判定：

```sql
status NOT IN ('已取消','已退票')
AND (status <> '待支付' OR pending_timeout_at IS NULL OR pending_timeout_at > NOW())
AND FIND_IN_SET(#{seat}, REPLACE(seat, '，', ',')) > 0
```

- `selectActiveByRecordId`（选座图）与 `countSeatInUse`（下单查重）**共用 `seatActiveCondition` 片段**，两者分叉即双卖。
- 并发：靠 `record` 行 `SELECT ... FOR UPDATE` 串行化同场次下单（这是全系统唯一的座位级保护）。
- 座位规范化：`(\d{1,2})排(\d{1,2})座`，去前导零（`1排01座` → `1排1座`），去重，校验影厅行列（缺省 8×8），上限 **6 座/单**。
- 库端**无法**建座位唯一索引（座位是逗号串），故没有硬约束兜底。

### 3.3 支付前复核场次（BL-06，2026-10-19 修复）

**缺陷**：`insertOrder` 判过 `isPurchasable`，但 `payOrder` 完全不看场次。

**为什么这是缺陷而不是理论风险**：建单到支付之间最长 5 分钟，而**场次不需要任何人退票就会自己开演**。开演前最后一秒锁下的座位，可以在开演后付款出票 —— 此时退票截止（映前 60 分钟）早已过去，用户**看不了、退不了**，钱却真的扣了。

**实测复现**（修复前）：

```
建单 record=83（start 19:00）        → code=200
UPDATE record SET start = NOW()-1min → 场次已开演，订单仍待支付
PUT /orders/{id}/pay 密码正确         → code=200 ✅ 出票成功
订单最终状态                           → 待取票，取票码 MXPZ-6VWZ
```

**修复**：`payOrder` 在**验密与扣款之前**插入复核：

```java
Record recordItem = recordMapper.selectByIdForUpdate(ordered.getRecordId());
if (recordItem == null)                       → 409「放映场次已不存在，无法支付」
if (!RecordService.isPurchasable(recordItem)) → 409「该场次已开场或已停售，无法支付」
```

三个设计点：

1. **用锁定读而不是普通读**。与 `addScoped` / `updateScoped`（改时间、停售）和 `deleteScoped`（BL-02）抢同一把排片行锁，避免「刚判完可购、管理员同时把场次改到过去」。
2. **排在验密之前**。场次不可购时不该白白消耗用户一次密码尝试次数（有失败计数与锁定）。
3. **排在超时分支之后**。已超时的订单优先按超时取消，不再走复核 —— 否则用户看到的是「场次已开场」，而实际原因是订单早过期。

**验证**：
- 修复后同一脚本：`code=409`，订单保持「待支付」、取票码为 NULL、未扣款。
- 正向回归：未开演场次正常支付成功；建单后停售 → 409；余额扣退一致。
- 守卫测试 `PayOrderPurchasableGuardTest`（5 条）。**做过反证**：把复核整块禁用后 4 条失败、只剩正向用例通过，确认测试能抓住缺陷。

**库约束（2026-10-27 已补）**：`record.status` 等状态列已加 `CHECK`，非法值由数据库直接拒绝（`StatusConstraintLoadTest` 钉住）。此前是自由字符串 —— 第一版验证脚本误写成 `"已停售"` 时 `UPDATE` 不报错、`isPurchasable` 也认不出来，于是「停售场次仍可支付」被误判为通过。

### 3.4 待支付超时（BL-05，2026-10-19 修复）

**修复前的缺陷**：写入用 JVM 时钟、判定用 DB `NOW()`，两个时钟混用。

```
建单：pending_timeout_at = LocalDateTime.now(JVM) + 5min
判定：pending_timeout_at < NOW()        ← DB 时钟（占座判定、清理任务）
      LocalDateTime.now().isAfter(...)  ← JVM 时钟（payOrder 的 isPendingTimedOut）
```

**实测时区错位有多大**：同一时刻，`SET time_zone='+00:00'` 与 `'+08:00'` 两个 MySQL 会话的 `NOW()` 相差 **8 小时**。典型触发场景是部署到 UTC 容器、连接串又写死 `serverTimezone=Asia/Shanghai`。后果分两种：

| 偏移方向 | 后果 |
| --- | --- |
| JVM 比 DB 快 | 清理任务永不命中 → **废单永久占座** |
| JVM 比 DB 慢 | 有效订单被提前取消（前端倒计时还在走） |

**当前配置是一致的**（`serverTimezone=Asia/Shanghai`、MySQL `time_zone=SYSTEM`、系统为北京时间），所以这不是一个已经发生的故障，而是**部署即触发**的隐患。

**修复原则：让写入与比较落在同一个时钟上。**

| 位置 | 修复前 | 修复后 |
| --- | --- | --- |
| 写入 | JVM `now() + 5min` | SQL `NOW() + INTERVAL 5 MINUTE`（哨兵 `pendingTimeoutFromDb`） |
| 支付路径判超时 | JVM 时钟解析字符串 | `countExpiredById`，与清理任务**共用** `pendingExpiredCondition` |
| 占座判定 / 清理任务 | DB `NOW()` | 不变 |

**为什么用哨兵字段而不是直接改 SQL**：`pending_timeout_at` 在 `insert` 里是"可选列"（`<if>` 包裹），直接写死 `NOW()` 会破坏其他写入路径。哨兵与已有的 `clearPendingTimeout` 对称：只表达"这次写入用库时钟"，不改列的可选性。超时分钟数由 `OrderedService.PENDING_TIMEOUT_MINUTES` 与 mapper 中的 INTERVAL 共同表达，**测试断言两者一致**。

**验证**：
- 建单后 `TIMESTAMPDIFF(SECOND, NOW(), pending_timeout_at)` = **300 秒**（由库算）
- 手工把超时点改成 `NOW() - 1 SECOND` → 支付返回「支付超时，订单已自动取消」并落库为已取消，**不是**「场次已开场」
- 支付路径取消后，「已超时待支付」行数 = 0，与清理任务结论一致
- 守卫 `PendingTimeoutClockTest`（4 条，读 mapper XML 原文）。**做过反证**：把 `NOW() + INTERVAL` 改回 `#{pendingTimeoutAt}`，测试立即红

**约束**：`pendingExpiredCondition` 是全仓唯一的超时谓词片段，`selectExpiredPendingOrders`（清理）与 `countExpiredById`（支付）都 `include` 它。新加任何超时判断都必须引用本片段 —— 测试会数"出现几处 `pending_timeout_at < NOW()`"，多写一处即红。

- `updateById` 对这一列用 `<choose>`：只有「显式清空（`clearPendingTimeout=true`）」或「写新值」两种写法，其余更新一律不碰 —— 否则任何部分更新都会顺手清掉倒计时。

### 3.5 金额

- 下单时 `unit_price` 取场次票价并**快照**（`updateById` 不含该列），`total = unitPrice × number`。
- 订单金额 `total/payAmount/refundAmount` 均为 `BigDecimal`（BL-24 已完成）；支付/退票直接使用订单金额，不再经过 `double` 往返。

### 3.6 取票码

- 8 位 + 中间连字符，字母表剔除 I/L/O/0/1（31^8 ≈ 8.5e11）。
- **在 `payOrder` 内与扣款同事务生成**，不重试（重试须拆事务）；`uk_ordered_pickup_code` 唯一索引兜底。
- 归一：输入放宽到 `[A-Z0-9]`，去除非字母数字、转大写、长度必须 8 —— 兼容存量含 0/1 的十六进制码。
- 核销：`markPickedUpByCode` 是**条件更新**（`WHERE pickup_code=? AND status='待取票'`），受影响行数即"是否真出票"；不加悲观锁。
- 限流：匿名核销按来源 IP 计失败（`RedeemRateLimiter`），10 分钟 30 次后 429；成功清零，窗口到期自动放行。

## 4 资金账本

`WalletService` 是余额唯一写入口，三条路径共用同一套：

```
requirePositive → SELECT balance FOR UPDATE → 校验/变更
→ UPDATE user SET balance = balance ± ?（扣款额外带 AND balance >= ?）
→ INSERT fund_flow(change_amount, balance_before, balance_after, related_id)
```

| 方法 | 来源 `FundSource` | relatedId |
| --- | --- | --- |
| `debitPurchase` | 购票（负数） | 订单 id |
| `creditRefund` | 退票（正数） | 订单 id |
| `creditRecharge` | 充值（正数） | 充值单 id |

- 流水**只增不改不删**；`balance_before` 与上一行 `balance_after` 恒等（账本连续性的可证明前提，见 `audit/frontend-audit.md` 关于删列的那段）。
- 事务传播一律 `REQUIRED`：`payOrder` 的扣款、出码、状态更新是**同一事务**。
- 管理端 `/manage/finance` 的「充值单据」页有「通过 / 拒绝」按钮；只有 ADMIN 能调用回调端点，用户端只能提交申请。

### 4.1 充值

```
createRecharge（仅 USER，>0 且 ≤50000，建「处理中」单据，不动余额）
   → handleCallback（仅 ADMIN；行锁；只接受「处理中」）
        ├ 成功：置「已完成」+ creditRecharge
        └ 失败：置「已失败」+ 备注，余额不变
```

- 这是**模拟网关**：用户端只提交申请，管理员在「充值流水」页通过/拒绝；接入真实支付平台时再把回调改成签名/查单。SEC-01 的「任意用户给自己充值」路径已关闭。
- 幂等：`selectByIdForUpdate` + 状态判定，没有幂等键。

## 5 支付密码

- 存 `user.pay_password`（BCrypt），只有 `user` 表有这三列。
- 限次：连续错 5 次锁 15 分钟；**支付与改密共用同一计数**（否则改密成为不限次试错通道）。
- **关键设计**：`checkForPayment` / `checkOldPassword` 标注 `REQUIRES_NEW` 且**返回异常而不是抛出**。原因：失败计数必须先由独立事务提交，若在同一事务内抛出，计数会随回滚一起消失，限次形同虚设。调用方拿到非 null 自行 throw。**改签名前先读这段注释。**
- `verifyOldPassword`（只验不写）是同类**自调用**，`REQUIRES_NEW` 被代理绕过 —— 它只在"入口无外层事务"（`AccountController` 直接调用）时成立。**不要把它挪进事务方法里。**
- 登录密码比对：只认 BCrypt，四个服务类共用 `common/PasswordMatcher`（BL-13）；存量明文由 `PlaintextPasswordMigration` 在启动时统一迁移，SEC-03 的明文回退已删除。

## 6 评价（mark）

- 一个用户对一部影片一条：`uk_user_film` 唯一索引；先查后插 + `DuplicateKeyException` 兜底。
- 写评前提：**对该片取过票**（`countPickedUpByUserAndFilm`，`已取票` 是终态故无需重复校验）。
- 评分：`film.score = AVG(mark.score)`，由 `MarkService.recalculateFilmScore` 写（`FilmMapper.recalculateScore` 是唯一写者）。
- 点赞：`mark_like(mark_id, user_id)` 复合主键，`insertIfAbsent` 不用 `INSERT IGNORE`（那会把外键错误静默成 0 行）。

## 7 票房与大盘

| 指标 | 口径 | 位置 |
| --- | --- | --- |
| 影片累计票房 | `SUM(ordered.total)`，状态 `IN ('待取票','已取票')`，**实时 JOIN** | `FilmMapper.filmRevenueJoin` |
| 今日票房 | 同上状态，按 **`pay_time`** 落在今天（不是放映日） | `OrderedMapper.selectTodayPaidRevenue` |
| 逐日趋势 | 同上区间，`GROUP BY DATE_FORMAT(pay_time,'%Y-%m-%d')` | `selectPaidRevenueByDay` |
| 统计时刻 | DB `NOW()` | 与今日票房同一次查询 |

- `paidOrderStatuses` 片段**全仓唯一一处定义**，被 4 条聚合语句 `<include>`。新增/修改状态必须同步这一处。
- 趋势窗口 = 截至昨天的 7 个完整日（不含今天，否则每天上午都是"断崖"）。
- `film.box_office` 列已废弃，恒为 0（实测 64 行全 0），列注释写明了这一点。

## 8 演职人员（actor）

- 现状是 **1:1**：`film.actor_id` 指向 `actor.id`；`actor.film_id` 是另一套未使用的 1:N 关联（65 行全 NULL → 实际 0 行使用）。
- 前端详情页按 `grade` 分四组（导演/主演/编剧/二级演员），但库中 65 行 `grade` 全为「主演」，故只有一组有数据。属正确空态，不是死 UI。
- `figure` = 饰演角色名（如 `Andy Dufresne`），`grade` = 所属角色。FE-48 曾误读 `roleType`/`role` 两个不存在的键，已修。
- 多角色 1:N 属新特性，已按「不做」关闭，不再跟踪。

## 9 影院名录（cinema_directory）

- 高德 POI 导入的**只读资料表**，`poi_id` 唯一且非空；`directories.linked` 由 `EXISTS` 实时派生，不落库。
- 前台已下线名录页，整条 `/api/v1/cinema-directory` 前缀仅 ADMIN。
- 导入为影院：生成 `amap_+poiId` 账号，`linked` 判定为无锁读（BL-04，但 poiId 非空这一 UNCERTAIN 已排除）。

### 9.1 抓取策略与直辖市的坑（2026-10-19 实测修正）

- 高德 v5 单组参数硬上限 **200 条**（25/页 × 8 页，第 9 页起恒空）。城市命中满 200 时改用**区县细分**再抓。
- **直辖市必须多下钻一层**：`/v3/config/district` 对「北京市」只返回**一个**省级节点，其下只有一个 `level=city` 的「北京城区」，区县在**再下一层**；`subdistrict=1` 拿不到区县。此前代码与单测都假设「省级 + 同级 city 带区县」，导致北京/上海导入恒为 0 条、测试却全绿。现改为 `subdistrict=2` 并下钻，实测北京 16 个区县、上海 16 个、石家庄 22 个。
- 逐页连续请求会撞高德 QPS 限制（`CUQPS_HAS_EXCEEDED_THE_LIMIT`）。`AmapConfig` 的节流拦截器按 300 ms 间隔；若某个区县重试后仍失败，**记 warning 跳过**，不让整次预览/导入失败。
- 抓取是外部慢操作（北京实测约 13 秒），**必须在数据库事务之外**：写库阶段才开事务（BL-45）。
