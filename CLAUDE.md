# 影院购票管理系统 · 工程契约

## 目录结构

```
project_02/
├── README.md                          # 对外门面
├── CONTRIBUTING.md                    # 提交规范 · 文档归属表
├── CLAUDE.md                          # 工程契约（本文件）
├── LICENSE                            # MIT
├── Bug.md                             # 规则篇 + 案例篇
├── 标准前端视觉与交互设计规范.md        # 前端规范
├── 前端规范待办.md                     # 规范未落地条目
├── .github/workflows/ci.yml           # CI：后端编译 → 前端构建
└── xm_film/
    ├── springboot/
    │   ├── pom.xml
    │   └── src/main/
    │       ├── java/com/example/springboot/
    │       │   ├── SpringbootApplication.java
    │       │   ├── common/            # BaseMapper · BaseService · BaseController · AuthContext · CorsConfig · FileUtil · JwtUtils · Result
    │       │   ├── common/config/     # AuthInterceptor · WebMvcConfig
    │       │   ├── common/enums/      # RoleEnum · OrderStatus · RecordStatus · PayResult · CinemaStatus · RechargeStatus · FundSource · ErrorCode
    │       │   ├── controller/        # 21
    │       │   ├── entity/            # 16
    │       │   ├── mapper/            # 16（含 MarkLikeMapper）
    │       │   ├── service/           # 17
    │       │   └── exception/
    │       └── resources/
    │           ├── application.yml
    │           ├── application-prod.yml
    │           ├── logback-spring.xml
    │           ├── static/swagger-ui.html
    │           └── mapper/            # 16 XML
    ├── vue/
    │   ├── index.html · vite.config.js · jsconfig.json · package.json
    │   ├── .env.development            # 已入库
    │   ├── .env                        # 不入库
    │   └── src/
    │       ├── main.js · App.vue
    │       ├── router/index.js
    │       ├── components/            # DetailSkeleton · ErrorBoundary · FilmPosterCard · OrderPayDialog
    │       ├── composables/           # useAuth · useCrud · useFormDialog
    │       ├── constants/index.js
    │       ├── types/axios.d.ts · env.d.ts · auto-imports.d.ts · components.d.ts
    │       ├── utils/                 # request · authStorage · format
    │       ├── views/                 # front 15 · back 7 · manage 16 · Login/Register/404
    │       └── assets/css/            # tokens · index · global · admin-layout · auth-layout · admin-pages · front-pages
    └── sql/
        ├── schema.sql                 # 17 表
        ├── data.sql                   # 基础种子
        ├── init.sql                   # 一键初始化
        └── README.md
```

## 架构不变量

### 后端抽象

| 层 | 基类 | 职责 |
|----|------|------|
| Controller | `BaseController<T>` | 7 个标准 CRUD 端点 |
| Service | `BaseService<T>` | CRUD + 事务 |
| Mapper | `BaseMapper<T>` | CRUD 方法定义 |

- Service 17 → 13 继承 `BaseService`；例外 `WalletService` `RechargeService` `FundFlowService` `StatisticsService`
- Controller 21 → 13 继承 `BaseController`；例外 `Auth` `Account` `Recharge` `FundFlow` `Statistics` `Ticket` `FileUpload` `Health`

### 数据模型

| 项 | 事实 |
|----|------|
| 主订票外键 | `room.cinema_id` · `record.film_id` · `ordered.record_id`；`xm_film/sql` 为 schema 与种子唯一出处 |
| 影院上映影片 | 由 `record` 派生（`EXISTS` 子查询）；无关联表；`record.film_id NOT NULL`；`by-cinema` 返回**不分页**的完整列表（传 `pageNum`/`pageSize` 后端不读），且**不填 `typeList`**（`selectByCinema` 漏调 `fillFilmTypes`，见规则 [87](Bug.md#规则篇)） |
| 影片类型 / 地区 | 只读后端字段 `areaName` · `typeList`；`Film` 无 `types` |
| 影厅 | `title` 后端派生；座位随 `room.seat_rows` / `seat_cols` |
| 选座 | 座位来源 `record.roomSeatRows` / `roomSeatCols`；单笔上限 6；USER 无权访问 `/api/v1/rooms` |

### 口径来源

| 口径 | 唯一来源 |
|------|----------|
| 票房 | `FilmMapper.xml` `filmRevenueJoin` 按 `ordered` 实时聚合（`待取票/已取票`），单位元；`film.box_office` 废弃 |
| 今日票房 | `OrderedMapper.selectTodayPaidRevenue` 按 `pay_time` 取日；`GET /api/v1/films/box-office/today` |
| 影片评分 | `mark.score`；`film.score` 纯派生（`MarkService` → `recalculateScore`），无评价为 NULL |
| 账户余额 | `user.balance`；`fund_flow` 只增审计副本 |
| 后台大盘 | `StatisticsController` → `GET /api/v1/statistics/overview`（ADMIN），`GROUP BY` 实时聚合 |

### 状态机

| 实体 | 迁移 |
|------|------|
| 订单 | `待支付 → 待取票 → 已取票`；旁支 `待支付 →（取消/超时）已取消`、`待取票 →（退票）已退票`；仅经 `payOrder` / `cancelOrder` / `pickupOrder` / `redeemByCode` / `refundOrder` |
| 充值单据 | `处理中 → 已完成` / `处理中 → 已失败`；终态不可流转 |
| 影院审核 | `CinemaStatus` 仅 `未审核` / `已审核` |
| 场次 | 可购票性由 `RecordService.isPurchasable` 判定（`start` 晚于当前 且 `status != 停售`）；`未开始`/`放映中`/`已结束` 派生不落库 |

### 鉴权

| 项 | 事实 |
|----|------|
| 公开只读 | 统一走 `AuthInterceptor.PUBLIC_READ_PREFIXES`（含 `/api/v1/films` `/api/v1/records` 等） |
| 令牌失效 | 公开只读资源按匿名放行（`isAnonymousRead`） |
| 匿名写 | 全站唯一 `POST /api/v1/tickets/redeem`（精确路径 + 仅 POST） |
| 账户 / 充值 / 流水 | 均在拦截器覆盖内，未登录 401 |
| 排除表 | `excludePathPatterns` 是角色盲区，不得加入需角色判断的路径 |

### 前端

| 项 | 事实 |
|----|------|
| 认证状态 | 唯一来源 `composables/useAuth.js` 的模块级 `user` ref；`utils/authStorage.js` 只是它的持久化副本。改状态走 `login` / `logout` / `setUser`，不要只动 storage |
| 状态色 | 集中 `constants/index.js`（`*_STATUS_MAP`） |
| 跨端入口 | 各外壳右上角显式按钮；`/front/home` 不用 `router.back()` |
| 热评 | 即 `GET /api/v1/marks/by-film`（`likeCount DESC, id DESC`）前 3 行，不另开查询 |
| 点赞表 | `mark_like` 纯关系表（`PRIMARY KEY (mark_id, user_id)`），赞数 `COUNT(*)`，无计数列 |
| 前台共享骨架 | `assets/css/front-pages.scss`，全部挂在 `.front-content` 下（`.page-card` / `.section-head` / `.poster-grid` / `.filter-chip` / `.service-tag--*` / `.detail-skeleton` / `.detail-skeleton--wide` / `.empty-hint`）。前台页面不再各写一份。`.detail-skeleton` 的标记收在 `components/DetailSkeleton.vue`，`wide` 开关给横版头图的页面（影院详情）；`.empty-hint` 是单行占位，带标题与说明的虚线面板叫 `.empty-panel`（组件内本地写） |
| 管理端共享骨架 | `assets/css/admin-pages.scss`，全部挂在 `.manage-container` 下（`.crud-page` / `.page-head` / `.list-toolbar` / `.table-card` / `.table-foot` / `.selection-count` / `.empty-hint` / `.row-actions` / `.line` / `.section-head`）。manage 的 13 个表格页共用「标题带 + 工具条 + 表格卡」两卡骨架，不再各写一份。`.section-head` 的几何与前台同名类一致 —— 两端各自是所在端的唯一骨架层，刻意不做一份全局层（§11.3 文件职责） |
| 卡片外观分工 | `global.css` 的 `.card` 提供底色 / 圆角 / 阴影（三端共用），`.page-card` 只负责堆叠间距与消费端内边距（前台 `--space-24`，即 §6.2 的密度分端）。manage 页已不用 `.page-card`，改用 `.list-toolbar` / `.table-card` 各自声明内边距；`back/*` 仍用 `.page-card`，其内边距取 `.card` 的 `--space-8` |
| 海报卡 | 唯一 `components/FilmPosterCard.vue`（2:3 海报 + 破图兜底 + 评分角标 + 元信息插槽），消费方 `front/Home.vue` · `front/Movie.vue`。`Search.vue` 横向卡与 `Rank.vue` 榜单行是另两种形状，不并入 |
| 导航高亮 | `Front.vue` 由 `NAV_ITEMS`（各项自带的 `sections` 路由前缀）从 `route.path` 现算，不手工同步 `activePath` 字符串 |
| 表单页跳转 | 登录回跳等站内跳转一律 `router.push`（`window.location.href` 既整页重载，又会把 `//host` 这类路径解析成外站） |
| 影院详情场次 | 日期条是今天起 7 天的固定窗口（客户端时钟，跨零点不自动翻页）；每片场次一次取回（`pageSize=200`，硬上限）后**在客户端按日期过滤**，切日期不发请求；只渲染可购场次（`status != 停售` 且未开场，与 `RecordService.isPurchasable` 同规则），该日无场次的影片整行不渲染；`?filmId=` 深链自动选中该片最近有场次的日期并滚到该行（`.film-row` 的 `scroll-margin-top` 给吸顶日期条让位） |
| 影院服务标签 | 一组三个（退票无忧 / 儿童优惠 / WiFi 覆盖），影院列表页与影院详情横幅两处**同色**，底色走共享层 `.service-tag--*`（功能色基色 + `--color-on-accent`）；两页形状不同故各自写形状，只共享底色 |

## API 接口清单

### 认证与公共

| 路径 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/api/v1/auth/login` | POST | 三端登录 | 否 |
| `/api/v1/auth/register` | POST | 用户 / 影院注册 | 否 |
| `/api/v1/auth/password` | PUT | 改密 | Bearer |
| `/api/v1/auth/years` | GET | 年份列表 | 否 |

### 资源 CRUD

资源（13）：`admins` `users` `cinemas` `films` `actors` `areas` `types` `notices` `rooms` `records` `orders` `marks` `videos`

| 路径 | 方法 | 说明 |
|------|------|------|
| `/{r}` | GET | 列表（支持筛选） |
| `/{r}/{id}` | GET | 按 ID |
| `/{r}/page` | GET | 分页 |
| `/{r}` | POST | 新增 |
| `/{r}` | PUT | 更新 |
| `/{r}/{id}` | DELETE | 删除 |
| `/{r}/batch` | DELETE | 批量删除 |

### 业务接口

| 路径 | 方法 | 说明 |
|------|------|------|
| `/api/v1/films/box-office/top` | GET | 票房榜 Top10 |
| `/api/v1/films/box-office/today` | GET | 今日票房 `{total, updatedAt}`，匿名可读 |
| `/api/v1/films/mark/top` | GET | 评分榜 Top5 |
| `/api/v1/films/search` | GET | 按标题搜索 |
| `/api/v1/films/by-cinema` | GET | 按影院查影片 |
| `/api/v1/marks/by-film` | GET | 某片评价分页（赞数降序 → id 降序）`{total, reviewable, my, list}`，匿名可读 |
| `/api/v1/marks/{id}/like` | PUT | 点赞 / 取消 `{liked}`，仅 USER，幂等 |
| `/api/v1/cinemas/page` | GET | 影院分页；非管理员仅 `已审核` |
| `/api/v1/statistics/overview` | GET | 后台大盘，仅 ADMIN |
| `/api/v1/tickets/redeem` | POST | 取票核销 `{code}`，免登录 |
| `/api/v1/files/upload` | POST | 文件上传 |

### 订单状态机

| 路径 | 方法 | 说明 | 角色 |
|------|------|------|------|
| `/api/v1/orders/create` | POST | 下单 | USER |
| `/api/v1/orders/seats` | GET | 占用座位投影 `{seat, mine}` | 登录 |
| `/api/v1/orders/{id}/pay` | PUT | 支付；超时取消返回 409 | 归属方 |
| `/api/v1/orders/{id}/cancel` | PUT | 取消 | 归属方 |
| `/api/v1/orders/{id}/pickup` | PUT | 柜台取票 | CINEMA |
| `/api/v1/orders/{id}/refund` | PUT | 退票（放映前 60 分钟） | 归属方 |

### 账户与资金

| 路径 | 方法 | 说明 | 角色 |
|------|------|------|------|
| `/api/v1/account/summary` | GET | 本人余额 | USER |
| `/api/v1/recharges` | POST | 提交充值单据 | USER |
| `/api/v1/recharges/page` | GET | 单据分页 | USER / ADMIN |
| `/api/v1/recharges/{id}/callback` | POST | 模拟回调 | 归属方 / ADMIN |
| `/api/v1/fund-flows/page` | GET | 流水分页 | USER / ADMIN |

## 页面清单

| 端 | 前缀 | 页数 | 页面 |
|----|------|------|------|
| 用户前台 | `/front/*` | 15 | home · movie · filmDetail/:id · cinema · cinemaDetail/:id · filmCinema/:id · rank · search · pickup · filmMarks/:id · buyTicket · orders · account · person · password |
| 影院后台 | `/back/*` | 7 | home · film · room · record · ordered · person · password |
| 管理后台 | `/manage/*` | 16 | home · admin · user · cinema · type · area · film · actor · notice · room · record · ordered · mark · video · person · password |

- 公开（免登录）：front 浏览类 + `pickup` + `filmMarks/:id`
- 需登录（USER）：`buyTicket` `orders` `account` `person` `password`
- 未登录访问受保护页 → `/login?redirect=<原路径>` → 登录后回跳
- 根路径 `/` → `/front/home`

## 硬约束索引

| 硬约束 | Bug 规则 |
|--------|----------|
| 金额 / 计数 / 比率用包装类型 | [28](Bug.md#规则篇) |
| 事务方法内不得先写后抛异常 | [29](Bug.md#规则篇) |
| 父数据禁止级联删除（下架用 `status`） | [24](Bug.md#规则篇) |
| `excludePathPatterns` 是角色盲区 | [33](Bug.md#规则篇) |
| 令牌失效公开只读按匿名放行 | [34](Bug.md#规则篇) |
| 余额变更只走 `WalletService` | [39](Bug.md#规则篇) |
| 余额扣减用行锁 + 条件更新 | [41](Bug.md#规则篇) |
| 资金字段 `BigDecimal` 且 > 0 | [42](Bug.md#规则篇) |
| 充值回调幂等，复用 `handleCallback` | [43](Bug.md#规则篇) |
| 订单物理删除仅 `已取消` / `已退票` | [45](Bug.md#规则篇) |
| 同步写库测试用临时库 + 备用端口 | [47](Bug.md#规则篇) |
| 只读视图回投影、不回实体；归属按 JWT 判定 | [48](Bug.md#规则篇) · [49](Bug.md#规则篇) |
| 表格操作列显式写 `width`，多按钮格套 `.row-actions` | [61](Bug.md#规则篇) |
| 柜台取票仅 `CINEMA` 白名单 | [62](Bug.md#规则篇) |
| 点赞写后回读需 `READ_COMMITTED` | [63](Bug.md#规则篇) |
| 派生指标列唯一写者、不收客户端入参 | [65](Bug.md#规则篇) |
| 同一配置项一个读取点、一个兜底 | [66](Bug.md#规则篇) |
| 取票码不加 `used` / `revoked` 列 | [67](Bug.md#规则篇) |
| 并发核销靠条件更新影响行数 | [68](Bug.md#规则篇) |
| 取票码生成收窄、输入放宽 | [69](Bug.md#规则篇) |
| 评价资格服务端校验 | [70](Bug.md#规则篇) |
| 去重插入用 `ON DUPLICATE KEY` | [71](Bug.md#规则篇) |
| 前端空态与异常文案分工 | [72](Bug.md#规则篇) |
| shell 入口匹配 `meta.roles` | [73](Bug.md#规则篇) |
| 改密只认 JWT 角色 | [74](Bug.md#规则篇) |
| "是否已支付 / 占座"集合多消费点同源 | [75](Bug.md#规则篇) |
| 登录态只经 `useAuth` 变更，不直接动 storage | [76](Bug.md#规则篇) |
| 站内跳转用 `router.push`，不用 `window.location.href` | [77](Bug.md#规则篇) |
| 共享 `@keyframes` 放 `global.css` | [78](Bug.md#规则篇) |
| 共享层与组件 scoped 块不同名装不同样式 | [79](Bug.md#规则篇) |
| 压淡文字用令牌，不用 `opacity` | [80](Bug.md#规则篇) |
| 内容页不写 `min-height: 100vh` | [81](Bug.md#规则篇) |
| 导航高亮从路由归属派生 | [82](Bug.md#规则篇) |
| 站内跳转后同页换参不再重挂，取数须 `watch` 参数 | [83](Bug.md#规则篇) |
| `opacity: 0` 叠放层仍可点击可聚焦，用 `visibility` | [84](Bug.md#规则篇) |
| 表单字段必须有数据落点（后端有列或分支承接） | [85](Bug.md#规则篇) |
| 瞬时接口加载反馈要有最短时长；失败保留已展示数据 | [86](Bug.md#规则篇) |
| 实体的派生 / 只读字段须在每条返回该实体的查询路径上填充 | [87](Bug.md#规则篇) |
| 改 `--dark-bg-hero` 必须复测深底文字色；头横幅不许用 `--dark-text-faint` / `--color-brand` 承文字 | [88](Bug.md#规则篇) |

## 开发守则

### 修改流程（防批量修复陷阱）

1. 先通读后修改：跨端改动先读关键文件，不凭记忆
2. 三方校验：对比 文档 / 代码 / 数据库，定位不一致源头
3. 逐块验证：每个逻辑块改完单独验证
4. 主动启动验证：改完主动提议启动验证
5. 文档同步：代码变更后核对三份文档是否需更新

### 文档链

`README` / `CONTRIBUTING` / `CLAUDE.md` → 代码 → 数据库 三者一致；落点见 [CONTRIBUTING · 文档归属](CONTRIBUTING.md#三文档归属一处事实一处归属)。

### 提交规范

见 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 相关文档

- [README.md](README.md) — 功能 · 三端角色 · 技术栈 · 配置 · 部署 · 测试账号
- [CONTRIBUTING.md](CONTRIBUTING.md) — 提交规范 · 分支实践 · 文档归属表
- [Bug.md](Bug.md) — [规则篇](Bug.md#规则篇)（硬约束正文）· [案例篇](Bug.md#案例篇)（根因）
- [数据库说明](xm_film/sql/README.md) — 表设计 · 初始化
- [前端设计规范](标准前端视觉与交互设计规范.md)
- [前端规范待办](前端规范待办.md)
