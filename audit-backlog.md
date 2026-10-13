# 待办技术债清单（audit-backlog）

本文件是本仓唯一的技术债登记处，承接已删除的 `后端代码规范审查报告.md` 的全部"登记备查"项，并汇总 2026-10-13 两轮代码审查（`entity/`、`service/`）中判定延后或建议的条目。已完成项不进本表；已判定非缺陷的条目集中在末节，避免二次误报。

维护口径：一处事实一处归属；每条给出 `文件` 与符号，行号随改动可能漂移、以符号为准。状态分三档：待办、有意不整改（与既定政策一致，非债务）、已核验非缺陷。

更新日期：2026-10-13。基线：master `e724ab20`。

## 并发与事务

| 编号 | 位置 | 问题 | 处置建议 |
| --- | --- | --- | --- |
| BL-01 | service/RecordService.validateSchedule（countRoomOverlap 调用处） | 影厅时段重叠是"先查后插"的无锁判定：countRoomOverlap 与随后的 INSERT 间无行锁，库端也建不了时间段唯一约束。两个管理员并发为同一影厅提交重叠时段，各读 overlap=0，双双落库成两场重叠排片；用户在两场上各订同一物理座位（countSeatInUse 以 record_id 为界）即同座双卖。 | 建库时对 (room_id, start) 加唯一约束，或 add/update 路径对 room 行加锁串行化。schema 级改动，另立一轮。 |
| BL-02 | service/RecordService.deleteScoped | 删除前置校验 ensureNoOrders 是非锁定 COUNT，随后才 DELETE；下单路径对 record 行取 FOR UPDATE，两者互不排斥。管理员删排片读到 0 订单后、DELETE 前，用户下单并提交，排片被删而订单悬空（record_id 指向已删行，无座位/凭证来源）。 | 删除路径同样对 record 行取锁，或改软删除/下架状态。 |
| BL-03 | service/UserService.insertUser、service/AdminService.add、service/CinemaService.insertCinema | 注册用户名为"先 selectByUsername 判空、后 insert"，无锁、无 DuplicateKeyException 兜底（MarkService 有先例）。并发注册同名双双通过落两行；selectByUsername 无 LIMIT 1，之后登录 TooManyResultsException。 | 捕获 DuplicateKeyException 转同一句"账号已存在"，并给 username 加唯一索引兜底。 |
| BL-04 | service/CinemaService.importFromDirectory | 名录导入的"已收录"判定为无锁读，生成用户名 amap_+poiId 也无唯一兜底。两个管理员并发导入同一名录 id，各读 linked=false 双插同名影院；命中唯一键则整批 @Transactional 回滚报 500。poiId 为 null 时两行亦撞同名。UNCERTAIN：名录行 poi_id 是否可能为空。 | 导入路径加锁或捕获唯一键冲突；补 poi_id 非空校验。 |
| BL-05 | service/OrderedService.insertOrder（写 pending_timeout_at 处） | pending_timeout_at 用 JVM 时钟写入，却在 SQL 里与 DB 的 NOW() 比较（OrderedMapper.seatActiveCondition、selectExpiredPendingOrders）。JVM 与 DB 时区不一致时清理任务永不命中（废单永久占座），或提前取消有效订单（前端倒计时仍在走）。 | 超时值与判定统一由 DB 时钟产生（INSERT ... NOW() + INTERVAL 或存时间戳），另立一轮。 |

## 状态机与契约

| 编号 | 位置 | 问题 | 处置建议 |
| --- | --- | --- | --- |
| BL-06 | service/OrderedService.payOrder | 支付时不复议场次是否仍可购；isPurchasable 只在建单时判。建单后、到期前付款即可为已开演场次出票（退票已过截止，只能取票或作废）。 | 支付前复核 RecordService.isPurchasable，或在开场时强制取消。 |
| BL-07 | service/UserService.updateScoped、service/CinemaService.update | updateById 前不判存在性，影响行数为 0 也返回成功。管理员对已删行提交（陈旧表单）得 200，实际未改；RecordService/RoomService.updateScoped 反而先 selectById 再抛 NOT_FOUND，口径不一。 | 更新前校验存在性并抛 NOT_FOUND（行为变更，需确认前端可接受）。 |

## 性能

| 编号 | 位置 | 问题 | 处置建议 |
| --- | --- | --- | --- |
| BL-08 | service/OrderedService.insertOrder | 逐座位调用 countSeatInUse（每单最多 6 次查询）。 | 复用已有的 selectActiveByRecordId(recordId) 一次取回，再在 Java 内求交。 |
| BL-09 | service/RecordService/RoomService/UserService.deleteBatchScoped | 批量删除逐个 id 查存在性与引用计数，N 个选中 = 2~3N 次查询（RoomService 最重）。 | 增加按 IN 批量的 selectByIds 与 countByXxxIds，单批降至 1~2 次查询。 |
| BL-10 | service/StatisticsService.summary | overview 已取回 countGroupByStatus()，summary 又单独发 countByStatus("待取票")，同页对 ordered 多发一次 COUNT，两次读之间还有快照窗口。 | 复用同一份分组结果（countOf 派生）。注意改后 pendingPickupOrders 的 Java 类型由 Integer 变 Long，需同步 3 处测试桩。 |
| BL-11 | service/TmdbBackfillService.backfillVideos | 用 selectAll(new Film())（含票房聚合与 area/actor JOIN）取全表再在 Java 里过滤 video 为空的行。 | 改投影查询 SELECT id,title,... WHERE video IS NULL OR video=''，避开聚合。 |
| BL-12 | service/TmdbService.resolveTypes | 逐类型 selectByTitleExact 并按需 insert（≤4 次）。 | 批量 selectByTitleExact(IN ...) 一次取回。管理员低频，优先级低。 |

## 重复代码与可维护性

| 编号 | 位置 | 问题 | 处置建议 |
| --- | --- | --- | --- |
| BL-13 | service/UserService、AdminService、CinemaService 的 login/updatePassword | 三者登录与改密体逐字重复；"BCrypt.matches 或明文 equals"的凭证比对谓词在三类 + PayPasswordService 共 7 处，属安全相关规则。改密策略或明文回退规则变更需同步 4 个类。 | 抽 AbstractAccountService<A extends Account> 或 PasswordSupport.verify(raw, stored)，login/updatePassword 共用。 |
| BL-14 | service/OrderedService.parsePrice、service/RecordService.parsePrice | 逐字重复，仅 ErrorCode 与文案不同。 | 上提到 common（如与 FileUtil 同层）。 |
| BL-15 | service/OrderedService、RecordService、RechargeService、PayPasswordService | 时间格式串 "yyyy-MM-dd HH:mm:ss" 在 4 处各自定义；now() 在 OrderedService 与 RechargeService 各写一份。 | 抽 DateTimeUtil 统一格式与 now()。 |
| BL-16 | service/OrderedService.generateOrderNo、service/RechargeService.generateRechargeNo | 日期 + UUID 前 8 位大写的后缀生成重复，仅前缀不同。 | 抽公共生成器。 |
| BL-17 | service/RecordService/RoomService 的 requireAdminOrCinema、applyCinemaScope；OrderedService/RecordService/RoomService 的 ensureXxxAccess | ADMIN 放行、CINEMA 归属 的守卫与 scope 谓词按实体各写一份。 | 在 AuthContext 旁抽统一的角色守卫。 |

## 实体层遗留（承接 S10）

| 编号 | 位置 | 问题 | 处置建议 |
| --- | --- | --- | --- |
| BL-18 | entity/ 全 17 类 | 实体混入非持久化展示/派生字段并直接对外：Film.areaName/typeList/ids、Cinema.province/city/district、Ordered/Mark/Record 的 join 字段。 | S10 展示字段剥离，移入对应 View（需改 JOIN 查询 resultType 与 Service 签名）。架构级，用户另立一轮。 |
| BL-19 | entity/Account 及 User/Admin/Cinema | token/newPassword 置于 Account 被三子类继承；三子类 @AllArgsConstructor 只覆盖自身字段、不含父字段（误用会建出 role/password 为 null 的账号，当前无调用者）。 | S10 继承解耦，把 token/newPassword 移出 Account（需改 login/updatePassword 签名）。 |
| BL-20 | entity/Admin 与 entity/User | 两实体字段完全一致；avatar/phone/email 在 Admin/User/Cinema 三处重复声明。 | 上提至 Account 或抽 Profile 基类（与 BL-19 同轮）。 |
| BL-21 | entity/Film.ids | 死字段：在 Film/FilmRequest/FilmView 三处声明，仅 Controller 逐层拷贝，无 Service/Mapper/前端读取。 | 连同 DTO 一并删除。 |
| BL-22 | entity/CinemaDirectory.createTime | 全仓唯一非 String 时间字段（LocalDateTime），序列化为 ISO 带 T 串，与其余 String 时间字段口径不一；前端未读取。 | 统一为 String，或全仓统一改 LocalDateTime（需联动 mapper/DTO/前端）。 |
| BL-23 | entity/Room.title | 存的是所属影院名（RoomService 由 cinema.getName() 派生），而 name 才是影厅自身名。命名与语义不符，是未标注的冗余快照。 | 改名或去除（需联动 mapper/Service/前端）。 |
| BL-24 | entity/Ordered 的 total/payAmount/refundAmount、entity/Film.boxOffice | 金额用 Double，而同实体 unitPrice、RechargeOrder.amount、FundFlow 各额均 BigDecimal。当前经 BigDecimal.valueOf(double) 往返未见精度损失，但与"金额用 BigDecimal"的既定口径不一致。 | 统一为 BigDecimal（需改库列类型与 mapper/DTO）。 |

## 规范建议遗留（承接 S4/S6/S15）

| 编号 | 位置 | 问题 | 处置建议 |
| --- | --- | --- | --- |
| BL-25 | S4：controller/OrderedController、MarkController、CinemaDirectoryController、TicketController | 路径用动词（/create、/cancel、/pay、/refund、/{id}/like、/redeem）；/import/preview 只读却用 POST；PUT 被用于 cancel/pay 等非幂等语义。 | 改资源式路径与方法（需联动前端与 AuthInterceptor 白名单）。对本地演示无收益，2026-10-12 已决定跳过。 |
| BL-26 | S6：exception/GlobalExceptionHandler | 全局异常一律 HTTP 200、业务码在 body。凭证类错误（约 10 处）用 UNAUTHORIZED，若改 HTTP 状态会令"输错支付密码"→401→前端清会话跳登录；且前端约 5 处依赖 `code !== 200` 做功能分支。 | 先重判凭证类业务码，再连同前端一起改。已决定跳过。 |
| BL-27 | S15 纯风格项：test/ 根包类、*LoadTest 命名、GlobalExceptionHandlerTest 名实不符、JUnit Assertions 与 AssertJ 并存、OrderedServiceTest 用 @SpringBootTest 而非 MockitoExtension | 不改变覆盖与行为，翻动面大、收益低。 | 已决定跳过。 |

## 健壮性（低优先 / 未证实可达）

| 编号 | 位置 | 问题 | 处置建议 |
| --- | --- | --- | --- |
| BL-28 | service/RecordService/RoomService/UserService.deleteBatchScoped | 缺 ids 为空/null 的守卫（OrderedService、MarkService、BaseService 均有）。经 Spring 拒绝 null @RequestBody，HTTP 路径不可达，属同形不一致。 | 与兄弟方法对齐补守卫。 |
| BL-29 | entity/Cinema.province/city/district | /auth/me 与 /login 经 CinemaMapper.selectById/selectByUsername（SELECT *，无 JOIN）取影院，区域字段恒为 null；/cinemas/page 则有值。UNCERTAIN：当前无前端读取该路径的区域。 | 若需在登录态展示区域，给该查询补 JOIN。 |

## 有意不整改（与既定政策一致，非债务）

- S14 子项：application.yml 中明文 DB 口令、jwt.signing-key、TMDB token、高德 key；file.upload-dir 绝对路径。依据本仓"仅本机运行、配置直写字面量、不引入环境变量注入"的既定政策，属有意偏离。

## 已核验为非缺陷（防二次误报）

- service/RecordService.selectByIdScoped 忽略 role/userId：排片信息（价格/时间/影片/影厅）本属公开可读，非越权，是按存在性校验的既定口径。
- service/RechargeService.handleCallback 允许订单归属用户自助"模拟支付"回调：与前端演示按钮一致的既定设计，非缺陷。
- 订单资金路径（WalletService 行锁 + balance>= 守卫、payOrder/refundOrder 的 selectByIdForUpdate、markPickedUpByCode 条件更新、PayPasswordService 的 REQUIRES_NEW 计数）：审查核验无双重扣款/负余额/幂等缺口。
