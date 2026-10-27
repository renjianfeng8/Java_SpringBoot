# 后端

## 1 控制器清单（23 个，基路径）

| 控制器 | 基路径 | 鉴权档 | 备注 |
| --- | --- | --- | --- |
| `AuthController` | `/api/v1/auth` | 公开（login/register/years）+ 登录态（me/password）+ ADMIN（password/reset） | 重置他人登录密码只允许 ADMIN |
| `HealthController` | `/api/v1/health` | 公开 | |
| `AdminController` | `/api/v1/admins` | ADMIN_ONLY | |
| `UserController` | `/api/v1/users` | 读登录态 / 写 ADMIN | 管理端用户 CRUD |
| `CinemaController` | `/api/v1/cinemas` | 读公开 / 写 ADMIN | 含 `regions`、`from-directory`、`covers/backfill`（返回一次性初始口令） |
| `CinemaDirectoryController` | `/api/v1/cinema-directory` | ADMIN_ONLY（不限方法） | |
| `FilmController` | `/api/v1/films` | 读公开 / 写 ADMIN | 含 box-office、mark-top、search、by-cinema、options（下拉只回 id/title） |
| `ActorController` | `/api/v1/actors` | 读公开 / 写 ADMIN | |
| `AreaController` | `/api/v1/areas` | 读公开 / 写 ADMIN | |
| `TypeController` | `/api/v1/types` | 读公开 / 写 ADMIN | |
| `NoticeController` | `/api/v1/notices` | 读公开 / 写 ADMIN | |
| `VideoController` | `/api/v1/videos` | 读公开 / 写 ADMIN | |
| `RoomController` | `/api/v1/rooms` | ADMIN / CINEMA（按归属） | `*Scoped` |
| `RecordController` | `/api/v1/records` | 读公开 / 写 ADMIN+CINEMA | `*Scoped` |
| `OrderedController` | `/api/v1/orders` | 登录态，按归属 | create/cancel/pay/pickup/refund/seats |
| `MarkController` | `/api/v1/marks` | 读公开 / 写登录态 | by-film、{id}/like |
| `TicketController` | `/api/v1/tickets` | **匿名 POST**（唯一） | `/redeem` 精确白名单 |
| `RechargeController` | `/api/v1/recharges` | 登录态；审核回调仅 ADMIN | page、{id}/callback |
| `FundFlowController` | `/api/v1/fund-flows` | 登录态 | 只读 page |
| `AccountController` | `/api/v1/account` | 登录态（**方法内 `requireUser` 守卫**） | 余额、支付密码 |
| `StatisticsController` | `/api/v1/statistics` | 方法内判 ADMIN | **未进任何前缀表**（BL-35） |
| `FileUploadController` | `/api/v1/files` | **未进任何前缀表**，任何登录角色可上传（BL-30） | |
| `TmdbController` | `/api/v1/tmdb` | ADMIN_ONLY | search / movie/{id} / backfill-videos |

> 新增端点自检：路径是否落在上面某个前缀下？该前缀的档位是不是你想要的？**没有"默认安全"** —— 未进前缀表即"登录即可访问"或"匿名可读（GET）"。

## 2 鉴权前缀表（`AuthInterceptor`）

```java
ADMIN_ONLY_PREFIXES   = { admins, cinema-directory, tmdb }
ADMIN_WRITE_PREFIXES  = { films, actors, areas, types, notices, videos }
PUBLIC_READ_PREFIXES  = { films, cinemas, types, areas, notices, actors, records, marks }
ANONYMOUS_WRITE_EXACT = { /api/v1/tickets/redeem }
```

判定顺序：

1. OPTIONS 直接放行。
2. 无 `Authorization` 头 → 匿名判定（GET+公开前缀，或 POST+精确白名单），否则 401。
3. 有 `Bearer`：解析 JWT（签名/过期/吊销点）→ 写 `userId`/`role` 到 request attribute → `hasAccess()`：ADMIN 全通；非 ADMIN 撞 `ADMIN_ONLY` 即 403；写方法撞 `ADMIN_WRITE` 即 403。
4. 令牌解析失败 → 降级为匿名判定（过期令牌仍可匿名读公开内容、仍可核销取票码）。

`matchesPrefix(path, p)` = `path.equals(p) || path.startsWith(p + "/")`。**禁止**改回裸 `startsWith`（历史缺陷：FE-34 / FE-60）。

## 3 Base 三件套与扩展点

| 想做的事 | 做法 |
| --- | --- |
| 加一个纯 CRUD 资源 | 实体实现 `Identifiable`；mapper 继承 `BaseMapper` 并写 XML（复制 `AreaMapper.xml` 为模板）；service 只实现 `mapper()`；controller `extends BaseController<T,R,V>` 实现两个映射 |
| 加业务校验 | 覆写 `add/update/delete` 或在 controller 调 `*Scoped` 方法；**`update` 必须委派 `super.update`**（存在性判定的唯一出处） |
| 局部更新 | 实体字段为 null 即"不更新"；XML 用 `<if test="x != null">`；`<set>` 结尾必须有 `id = #{id}` 兜底 |
| 字符串筛选 | 判空必须写 `x != null and x != ''`（BL-38 口径；BL-42 是尚未补齐的同形点） |

## 4 事务

- `BaseService` 类级 `@Transactional(readOnly = true)`；写方法逐个覆盖 `@Transactional(rollbackFor = Exception.class)`。
- 资金路径（`OrderedService.payOrder/refundOrder`、`RechargeService.handleCallback`）与 `WalletService` 同事务。
- `PayPasswordService` 的两个 check 是 `REQUIRES_NEW` 且返回异常（见 `domain.md` §5）。
- 清理任务 `task/OrderCleanupTask`：`@Scheduled(fixedDelay = 15s)`，入口 `@EnableScheduling`（在 `SpringbootApplication` 上）。

## 5 数据访问约定

- 分页：controller 里 `PageMethod.startPage(pageNum, pageSize)` 紧跟 `service.selectAll(...)`；**PageHelper 是线程绑定的一次性拦截**，两者之间不能插入其它查询。
- 列名映射：`map-underscore-to-camel-case: true` 只作用于 bean 属性，**不作用于 Map 的 key** —— 返回 `Map` 的语句别名必须写 camelCase。
- `SELECT *` 是常态；join 派生列用别名（`user.name as userName`）。
- `ONLY_FULL_GROUP_BY` 开启：`GROUP BY` 必须与 `SELECT` 里的表达式**逐字相同**（`DATE_FORMAT(...)` 而非 `DATE(...)`）。
- `GET /list` 硬上限 500 条（`BaseController.MAX_LIST_SIZE`）；要全量或翻页走 `/page`，不要把 `/list` 当全量导出用。
- 日期/年份筛选用半开区间（`>= day AND < day+1`、`>= year-01-01 AND < 下一年`），不要写 `LIKE '%...%'`，否则 `idx_start` 失效。

## 6 测试

74 个测试文件 / 551 条执行（2026-10-27），按启动方式分四类：

| 类型 | 数量 | 依赖数据库 | 说明 |
| --- | --- | --- | --- |
| Mockito 单测 | 21 个类 | 否 | `@ExtendWith(MockitoExtension.class)` + `@InjectMocks` |
| `@SpringBootTest` 绑定层守卫 | 19 个类（含 `*LoadTest`、`OrderedSeatConflictTest`、`OrderedServiceTest`、并发/事务边界测试） | **是，走 `xm_film_test`** | 证明 mapper XML 能被 MyBatis 解析并执行 |
| `@WebMvcTest` 控制器切片 | 13 个类（覆盖全部 23 个 Controller） | 否 | 鉴权前缀、角色范围、空请求体、业务码透传 |
| 纯单元 / 静态守卫 | 22 个类 | 否 | 无上述注解，如 `FileUtilTest`、`TmdbClientTest`、`StatusLiteralGuardTest`、`MapperXmlParameterGuardTest` |

> `RecordDeleteGuardTest` 同时带 `MockitoExtension` 与 `@SpringBootTest`，前两行各计一次；四类去重后共 74 个文件。

**硬规则**：任何新增 `@SpringBootTest` 必须同时加 `@ActiveProfiles("ci")`；会写库的再加 `@Transactional`。缺前者 = 写开发库，缺后者 = 失败即留脏行。

`OrderedServiceTest` 是唯一一个 `@SpringBootTest(classes = OrderedService.class)`（只装一个 bean 的切片）的单测，53 个用例覆盖了状态机、权限、金额、取票码全路径（mock 掉全部 mapper）。

一个测不出来的点：`TicketController` 的「GET 同路径必须 401」在 `@WebMvcTest` 里验不了 —— 该控制器只有 `@PostMapping`，GET 在 HandlerMapping 阶段就 405，到不了拦截器。这条由 `AuthInterceptorAccessTest` 在拦截器层覆盖，不要造一个假通过的用例。

## 7 值得记住的三个"反直觉"实现

1. **`payOrder` 超时不抛异常而返回 `PayResult.TIMEOUT_CANCELLED`**：该方法是 `rollbackFor = Exception.class`，抛异常会把"置为已取消"这次写入一起回滚，订单又变回待支付。
2. **`BaseService.update` 先 `selectById` 再 `updateById`**：陈旧表单由 200 变 404，这是 BL-07 的有意行为变更；所有覆写 `update` 的子类必须委派它（`OrderedService` 因需回填 `pending_timeout_at` 而就地复制了同样的判定）。
3. **`ActorMapper.insert` 的 `actor.title` 是 NOT NULL 且无默认值**：导入路径必须把影片名一并写入（`TmdbLookupMapperLoadTest.actorInsertBackfillsGeneratedId` 钉住了这条）。
