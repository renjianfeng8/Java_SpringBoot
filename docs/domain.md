# 业务域

本文件是「系统怎么运转」的单一出处。实现细节以代码为准；此处只写规则与它们的理由。

## 1 角色

三个账号表各自独立、互不 JOIN：`admin`（角色 ADMIN）、`cinema`（CINEMA）、`user`（USER，有余额与支付密码）。
`entity/Account` 是三者的父类（**无对应表**），`token` / `newPassword` 挂在它上面（BL-19）。

关键：**影院的 `id` 同时也是它的「用户 id」**。`ordered.cinema_id` 与 token 里的 `userId` 在 CINEMA 语境下是同一 id 空间，所以 `ensureOrderAccess` 能直接比。

## 2 排片（record）

一场 = `cinema_id + room_id + film_id + start + price + status`。

| 规则 | 实现 | 位置 |
| --- | --- | --- |
| 可购票性 | `start` 晚于当前时间且 status ≠ 停售 | `RecordService.isPurchasable`（静态，全场唯一权威） |
| 展示状态 | 未开始 / 放映中 / 已结束**由 start 派生**，不落库 | 前端 + `RECORD_STATUS` |
| 排片冲突 | `countRoomOverlap(room, id, start, start+片长)`；**先锁影厅行再查（当前读）** | `validateSchedule`；见下 |
| 片长兜底 | 影片 `time` ≤ 0 时用 120 分钟 | `DEFAULT_DURATION_MINUTES` |
| 删除守卫 | 已产生订单的场次禁止物理删除；`deleteScoped` 先锁行再数订单 | `ensureNoOrders`（BL-02，见 §2.2） |
| `room.title` | 存的是**影院名**，`room.name` 才是影厅名 | BL-23 |

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
| 支付 | `payOrder` | 待支付；未超时；**订单归属者的支付密码** | 扣余额 + 写流水 + 生成取票码 |
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

### 3.2 待支付超时

- 建单时写 `pending_timeout_at = JVM now + 5min`（**JVM 时钟**）。
- 判定用 **DB `NOW()`**（SQL 谓词与清理任务）。
- 两者不一致时：清理永不命中（废单永久占座）或提前取消。见 BL-05。
- `updateById` 对这一列用 `<choose>`：只有「显式清空（`clearPendingTimeout=true`）」或「写新值」两种写法，其余更新一律不碰 —— 否则任何部分更新都会顺手清掉倒计时。

### 3.3 金额

- 下单时 `unit_price` 取场次票价并**快照**（`updateById` 不含该列），`total = unitPrice × number`。
- 支付/退票的资金额 = `BigDecimal.valueOf(ordered.total)`。
- 订单总价为 `Double`（BL-24），当前经 `BigDecimal.valueOf(double)` 往返未见精度损失。

### 3.4 取票码

- 8 位 + 中间连字符，字母表剔除 I/L/O/0/1（31^8 ≈ 8.5e11）。
- **在 `payOrder` 内与扣款同事务生成**，不重试（重试须拆事务）；`uk_ordered_pickup_code` 唯一索引兜底。
- 归一：输入放宽到 `[A-Z0-9]`，去除非字母数字、转大写、长度必须 8 —— 兼容存量含 0/1 的十六进制码。
- 核销：`markPickedUpByCode` 是**条件更新**（`WHERE pickup_code=? AND status='待取票'`），受影响行数即"是否真出票"；不加悲观锁。

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

### 4.1 充值

```
createRecharge（仅 USER，>0 且 ≤50000，建「处理中」单据，不动余额）
   → handleCallback（ADMIN 或单据本人可触发；行锁；只接受「处理中」）
        ├ 成功：置「已完成」+ creditRecharge
        └ 失败：置「已失败」+ 备注，余额不变
```

- 这是**模拟网关**：前端按钮直接回调。属既定演示设计，非缺陷（见 `audit/backend-backlog.md`「已核验非缺陷」）。
- 幂等：`selectByIdForUpdate` + 状态判定，没有幂等键。

## 5 支付密码

- 存 `user.pay_password`（BCrypt），只有 `user` 表有这三列。
- 限次：连续错 5 次锁 15 分钟；**支付与改密共用同一计数**（否则改密成为不限次试错通道）。
- **关键设计**：`checkForPayment` / `checkOldPassword` 标注 `REQUIRES_NEW` 且**返回异常而不是抛出**。原因：失败计数必须先由独立事务提交，若在同一事务内抛出，计数会随回滚一起消失，限次形同虚设。调用方拿到非 null 自行 throw。**改签名前先读这段注释。**
- `verifyOldPassword`（只验不写）是同类**自调用**，`REQUIRES_NEW` 被代理绕过 —— 它只在"入口无外层事务"（`AccountController` 直接调用）时成立。**不要把它挪进事务方法里。**
- 登录密码比对：先 BCrypt，再退到明文相等（兼容存量种子账号），共 7 处同口径（BL-13）。

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
- 多角色 1:N 属新特性，见 BL-41。

## 9 影院名录（cinema_directory）

- 高德 POI 导入的**只读资料表**，`poi_id` 唯一且非空；`directories.linked` 由 `EXISTS` 实时派生，不落库。
- 前台已下线名录页，整条 `/api/v1/cinema-directory` 前缀仅 ADMIN。
- 导入为影院：生成 `amap_+poiId` 账号，`linked` 判定为无锁读（BL-04，但 poiId 非空这一 UNCERTAIN 已排除）。
