# 影院购票管理系统 (Cinema Ticket Management System)

基于 **Spring Boot 3.3 + Vue 3 + MySQL** 构建的在线电影购票管理平台，支持三端角色分离运营（管理员后台、影院端、用户端）。

## 技术栈

技术栈与版本号的唯一落点是 [README.md · 技术栈](README.md#技术栈) —— 本文件不再重复一份，避免两处版本号各自漂移。

## 目录结构

```
project_02/
├── README.md                          # 项目说明（对外门面：功能、技术栈、快速启动、部署）
├── CONTRIBUTING.md                    # 提交规范 · 分支实践 · 文档归属表
├── CLAUDE.md                          # 工程契约（本文件）：架构 · 目录 · 接口/页面清单 · 不变量 · 守则
├── LICENSE                            # 许可证（MIT）
├── Bug.md                             # Bug 修复记录（修复前先查阅）
├── 标准前端视觉与交互设计规范.md        # 前端视觉与交互设计规范（新增页面前先查阅）
├── 前端规范待办.md                     # 规范未落地条目与整改进度（规范正文不记进度）
├── .github/workflows/ci.yml           # CI：后端编译 → 前端构建
├── xm_film/                           # 项目主目录
│   ├── springboot/                    # 后端（Spring Boot）
│   │   ├── pom.xml                    # Maven 依赖配置
│   │   └── src/main/
│   │       ├── java/com/example/springboot/
│   │       │   ├── SpringbootApplication.java  # 启动入口
│   │       │   ├── common/                     # 公共组件
│   │       │   │   ├── BaseMapper.java         # MyBatis 通用 Mapper 接口
│   │       │   │   ├── BaseService.java        # 通用 CRUD Service 基类
│   │       │   │   ├── BaseController.java     # 通用 CRUD Controller 基类
│   │       │   │   ├── AuthContext.java        # 取当前请求的 role / userId（拦截器写入的属性）
│   │       │   │   ├── CorsConfig.java         # CORS 跨域
│   │       │   │   ├── FileUtil.java           # 文件上传工具（含 MIME 白名单）
│   │       │   │   ├── JwtUtils.java           # JWT 令牌工具（JJWT 新版 API）
│   │       │   │   ├── Result.java             # 统一响应封装
│   │       │   │   └── enums/                  # 词表枚举（RoleEnum / OrderStatus / RecordStatus / PayResult / CinemaStatus / RechargeStatus / FundSource / ErrorCode）
│   │       │   ├── common/config/
│   │       │   │   ├── AuthInterceptor.java    # JWT 认证拦截器
│   │       │   │   └── WebMvcConfig.java       # Web MVC 配置
│   │       │   ├── controller/                 # 控制器层（21个，含 TicketController 取票大厅）
│   │       │   ├── entity/                     # 实体类（16个）
│   │       │   ├── mapper/                     # MyBatis Mapper（16个，含 MarkLikeMapper 点赞关系）
│   │       │   ├── service/                    # 业务逻辑层（17个）
│   │       │   └── exception/                  # 异常处理
│   │       └── resources/
│   │           ├── application.yml             # 开发环境配置
│   │           ├── application-prod.yml        # 生产环境配置（禁用 Swagger、密钥必填）
│   │           ├── logback-spring.xml          # 日志配置
│   │           ├── static/swagger-ui.html      # Swagger UI 页面（资源走 CDN）
│   │           └── mapper/                     # MyBatis XML 映射（16个）
│   ├── vue/                            # 前端（Vue 3）
│   │   ├── index.html                  # HTML 入口
│   │   ├── vite.config.js              # Vite 配置（含 AutoImport / Components 插件）
│   │   ├── jsconfig.json               # 路径别名与编译选项
│   │   ├── package.json                # 前端依赖
│   │   ├── .env.development            # 开发环境默认值（已入库）
│   │   ├── .env                        # 生产构建默认值（不入库，本机文件）
│   │   ├── src/
│   │   │   ├── main.js                 # Vue 入口（含全局 errorHandler）
│   │   │   ├── App.vue                 # 根组件（ElConfigProvider + ErrorBoundary）
│   │   │   ├── router/index.js         # 路由配置 + 角色守卫
│   │   │   ├── components/             # 通用组件
│   │   │   │   ├── ErrorBoundary.vue   # 渲染异常兜底
│   │   │   │   └── OrderPayDialog.vue  # 支付弹窗（选座页与订单页共用；支付成功后就地切成取票凭证态）
│   │   │   ├── composables/            # 组合式函数
│   │   │   │   ├── useAuth.js          # 登录态 / 角色判断
│   │   │   │   ├── useCrud.js          # 分页 CRUD 通用逻辑
│   │   │   │   └── useFormDialog.js    # 表单弹窗通用逻辑
│   │   │   ├── constants/index.js      # API 路径 / 状态映射（订单 · 影片 · 场次）
│   │   │   ├── types/axios.d.ts        # Axios 响应类型增强
│   │   │   ├── env.d.ts                # 环境变量类型声明
│   │   │   ├── auto-imports.d.ts       # 自动导入声明（unplugin-auto-import 生成）
│   │   │   ├── components.d.ts         # 自动注册组件声明（unplugin-vue-components 生成）
│   │   │   ├── utils/                  # 工具层
│   │   │   │   ├── request.js          # Axios 封装（拦截器 + 统一错误提示）
│   │   │   │   ├── authStorage.js      # 登录态本地存储
│   │   │   │   └── format.js           # 格式化（formatBoxOffice / formatYuan 票房 · formatScore 评分）
│   │   │   ├── views/                  # 页面视图
│   │   │   │   ├── Login.vue / Register.vue / 404.vue
│   │   │   │   ├── Front.vue           # 用户前台布局
│   │   │   │   ├── Back.vue            # 影院后台布局
│   │   │   │   ├── Manage.vue          # 管理后台布局
│   │   │   │   ├── front/              # 15个用户端页面（含取票大厅 Pickup.vue、影评页 FilmMarks.vue）
│   │   │   │   ├── back/               # 7个影院端页面
│   │   │   │   └── manage/             # 16个管理端页面
│   │   │   └── assets/                 # 静态资源（css / imgs）
│   │   │       └── css/                # tokens.scss 设计令牌 · index.scss EP 主题覆写
│   │   │                               # global.css 全局重置 · admin-layout.scss 后台外壳
│   │   │                               # auth-layout.scss 认证页外壳（登录/注册共用）
│   │   │                               # admin-pages.scss / front-pages.scss 列表页共用骨架
│   ├── sql/                           # 数据库初始化（全新安装的唯一路径）
│   │   ├── README.md                  # 数据库说明
│   │   ├── schema.sql                 # 17张表建表语句（含全部列，无补充脚本）
│   │   ├── data.sql                   # 基础种子（管理员/用户/影院/影厅/影片/词表；
│   │   │                              #   不含场次/订单/评价 —— 这三类由真实接口产生）
│   │   └── init.sql                   # 一键初始化入口（建库 + schema + data）

## 核心模块说明

> 三端角色职责与功能模块摘要的对外口径唯一落点是 [README.md · 功能模块](README.md#功能模块)，本文件不再重复一份。

### 后端架构设计
后端采用三层泛型抽象架构消除重复 CRUD 代码：

| 层级 | 基类 | 职责 |
|------|------|------|
| Controller | `BaseController<T>` | 提供 7 个标准 RESTful CRUD 端点 |
| Service | `BaseService<T>` | 提供 CRUD 方法 + 事务管理 |
| Mapper | `BaseMapper<T>` | 提供 MyBatis CRUD 方法定义 |

- 17 个 Service 中 13 个继承 `BaseService<T>`，仅需实现 `mapper()` 方法返回具体 Mapper；例外四个不构成通用 CRUD 资源：`WalletService`、`RechargeService`、`FundFlowService`、`StatisticsService`
- 21 个 Controller 中 13 个继承 `BaseController<T>`，仅需声明 `@RequestMapping` + 构造函数注入；例外八个：`Auth`/`Account`/`Recharge`/`FundFlow`/`Statistics`/`Ticket`/`FileUpload`/`Health`（认证、状态机端点、免登录核销、文件上传、健康检查）
- 复杂业务（如 Film 的排行榜、Cinema 的按电影筛选分页）通过方法覆写实现

## API 接口清单

### 认证与公共接口 (`/api/v1/auth/**`)
| 路径 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/api/v1/auth/login` | POST | 用户登录（三端共用） | 否 |
| `/api/v1/auth/register` | POST | 用户/影院注册 | 否 |
| `/api/v1/auth/password` | PUT | 修改密码 | Bearer |
| `/api/v1/auth/years` | GET | 获取年份列表（搜索筛选用） | 否 |

> 匿名 GET 访问 `/api/v1/films`、`/api/v1/cinemas`、`/api/v1/types`、`/api/v1/areas`、`/api/v1/notices`、`/api/v1/actors`、`/api/v1/records`、`/api/v1/marks` 等公开资源无需认证，由 AuthInterceptor 自动放行。写操作（POST/PUT/DELETE）仍需登录；**令牌失效时公开只读资源仍按匿名放行**，否则前端 401 处理会把游客从公开页踢去登录页。

> **唯一的匿名写入口**是取票大厅核销 `POST /api/v1/tickets/redeem`（`AuthInterceptor.ANONYMOUS_WRITE_EXACT`，**精确路径 + 仅 POST**，不是前缀）。自助机不认识用户、只认取票码，所以它必须免登录；安全性由"码本身即凭证"保证，逐条论证见 `TicketController` 的类注释 —— 新增任何匿名写端点都必须同样能回答那五个问题。

> `marks` 的写操作有额外规则（在 `MarkController` 内校验，拦截器只做前缀级判断）：发表评价仅限 USER 且评价人取自 JWT；修改/删除仅限本人，ADMIN 可管理全部。**且发表评价要求该用户对该影片有 `已取票` 订单**（`OrderedMapper.countPickedUpByUserAndFilm`，在 `MarkService.add` 内校验）—— 这条此前只有前端按钮在守，服务端不校验，等于任何登录用户能给没买过票的影片打分。
> `cinemas` 的公开列表只返回 `已审核` 影院，管理员（含后台审核列表）返回全部；新增影院仅管理员可用，初始状态固定为 `未审核`。

### 资源管理接口 (`/api/v1/{resources}`)
13 个资源（`admins`、`users`、`cinemas`、`films`、`actors`、`areas`、`types`、`notices`、`rooms`、`records`、`orders`、`marks`、`videos`）统一提供以下 RESTful 接口：

| 路径 | 方法 | 说明 |
|------|------|------|
| `/{resources}` | GET | 查询全部（支持筛选） |
| `/{resources}/{id}` | GET | 按 ID 查询 |
| `/{resources}/page` | GET | 分页查询 |
| `/{resources}` | POST | 新增 |
| `/{resources}` | PUT | 更新 |
| `/{resources}/{id}` | DELETE | 删除 |
| `/{resources}/batch` | DELETE | 批量删除 |

### 业务接口
| 路径 | 方法 | 说明 |
|------|------|------|
| `/api/v1/films/box-office/top` | GET | 票房排行榜 Top10（按 `ordered` 实时聚合，只统计已支付；无售票的影片不上榜） |
| `/api/v1/films/box-office/today` | GET | 今日票房：今天支付的售票收入合计 + 统计时刻（`{total, updatedAt}`）；**匿名可读**，前台首页展示 |
| `/api/v1/films/mark/top` | GET | 评分排行榜 Top5 |
| `/api/v1/films/search` | GET | 按标题搜索电影 |
| `/api/v1/films/by-cinema` | GET | 按影院查询电影 |
| `/api/v1/marks/by-film` | GET | 某片评价分页（按**赞数降序 → id 降序**），返回 `{total, reviewable, my, list}`；每行 `MarkView` 的 `liked`/`mine` 由后端按 JWT 计算、**投影不含 `userId`**；**匿名可读**，热评即该排序的头部 |
| `/api/v1/marks/{id}/like` | PUT | 点赞 / 取消点赞：入参 `{liked:true\|false}`（**显式意图**，缺失即拒，不是服务端 toggle），响应回读写库后的 `{liked, likeCount}`；**仅 USER**，重复调用幂等 |
| `/api/v1/cinemas/page` | GET | 影院分页（支持按电影筛选）；匿名/非管理员只返回 `已审核` 影院，管理员返回全部（否则后台审核列表查不到待审核影院） |
| `/api/v1/statistics/overview` | GET | 后台可视化大盘（影院状态分布 + 影片类型分布，数据库实时聚合；仅 ADMIN） |
| `/api/v1/tickets/redeem` | POST | **取票大厅核销**：入参只有 `{code}`（没有 orderId），订单 待取票 → 已取票；返回出票凭条（不含 orderId/订单号/金额/userId）。**全站唯一免登录写接口**，见上方说明 |
| `/api/v1/files/upload` | POST | 文件上传（图片/视频） |

### 订单状态机接口（`/api/v1/orders/**`）
订单不支持通用 PUT 更新（`OrderedService.updateScoped` 直接拒绝），状态流转全部走显式端点：

| 路径 | 方法 | 说明 | 允许角色 |
|------|------|------|----------|
| `/api/v1/orders/create` | POST | 下单（校验场次可售、座位在影厅范围内且未被占用） | USER |
| `/api/v1/orders/seats` | GET | 查询某场次占用中的座位（`{seat, mine}` 投影；仅本人订单附带 orderId/金额/倒计时） | 登录用户 |
| `/api/v1/orders/{id}/pay` | PUT | 支付待支付订单；超时则取消并返回 409 | 订单归属方 |
| `/api/v1/orders/{id}/cancel` | PUT | 取消待支付订单 | 订单归属方 |
| `/api/v1/orders/{id}/pickup` | PUT | 取票（待取票 → 已取票）；这是**影院柜台的员工通路**，只对 `CINEMA` 开放（受 `cinemaId` 范围限制）；用户的自助通路是 `/api/v1/tickets/redeem` | CINEMA |
| `/api/v1/orders/{id}/refund` | PUT | 退票（待取票 → 已退票，需放映前 60 分钟以上） | 订单归属方 |

> 上面两条是同一个状态迁移的两个入口：柜台的 `pickup` 与自助机的 `redeem`，共用「待取票 → 已取票」这一条边，前端 `OrderPayDialog` 支付成功后展示取票码并给「去取票大厅」入口。

### 账户与资金接口（`/api/v1/account/**` · `/api/v1/recharges/**` · `/api/v1/fund-flows/**`）

| 路径 | 方法 | 说明 | 允许角色 |
|------|------|------|----------|
| `/api/v1/account/summary` | GET | 当前登录用户的账户余额（`userId` 取自 JWT） | USER |
| `/api/v1/recharges` | POST | 提交充值申请：生成「处理中」单据，**余额不变** | USER |
| `/api/v1/recharges/page` | GET | 充值单据分页（USER 只看自己的，ADMIN 看全部） | USER / ADMIN |
| `/api/v1/recharges/{id}/callback` | POST | 模拟支付网关回调（仅「处理中」可流转，重复回调被拒） | 单据归属方 / ADMIN |
| `/api/v1/fund-flows/page` | GET | 资金流水分页（USER 只看自己的，ADMIN 看全部） | USER / ADMIN |

> 充值单据与资金流水都**不继承** `BaseController` / `BaseService`：单据是资金凭证、账本只增不改，不存在通用更新与删除，因此不暴露 PUT/DELETE 端点。
> 余额不挂在 `User` 实体上，`/api/v1/users` 是 `SELECT *` 的通用查询，挂上去等于把任何人的余额公开；余额只经 `/account/summary` 按 JWT 返回本人。

## 页面清单

### 管理后台 (`/manage/*`) — 16个页面
home, admin, user, cinema, type, area, film, actor, notice, room, record, ordered, mark, video, person, password

### 影院后台 (`/back/*`) — 7个页面
home, film, room, record, ordered, person, password

### 用户前台 (`/front/*`) — 15个页面（公开浏览模式）
系统支持公开访问，无需登录即可浏览电影、影院、排行榜等公开内容。根路径 `/` 自动重定向到 `/front/home`。

| 访问模式 | 路由 | 说明 |
|----------|------|------|
| 公开访问（无需登录） | home, movie, filmDetail/:id, cinema, cinemaDetail/:id, filmCinema/:id, rank, search, **pickup**, **filmMarks/:id** | 浏览类页面 + **取票大厅**（自助机口径，凭取票码核销，所以刻意不要求登录，导航也对游客可见）+ **影评页**（只从影片详情页与购票记录两处进入，**刻意不进顶部导航**；游客也能看别人的评价） |
| 需登录（USER） | buyTicket, orders, account, person, password | 操作类页面，未登录时弹框提示跳转登录 |

访问受保护页面时，系统弹出确认框 → 跳转 `/login?redirect=<原路径>` → 登录成功后自动回跳。登录页根据角色（USER/CINEMA/ADMIN）分别跳转 `/front/home`、`/back/home`、`/manage/home`。

购票闭环：选座页支付成功后 `OrderPayDialog` **不关闭**，就地切成「购票成功」凭证态显示取票码（`GET /api/v1/orders/{id}` 回查，含后端 join 的影片/影院/影厅名）并给「去取票大厅」；此后可在 `orders` 页 `待取票` 行的展开区再次查看取票码。

评价闭环：`orders` 页对 `已取票` 的订单提供「去评价 / 修改评价」，点击跳转影评页 `/front/filmMarks/:id`（发表/修改与点赞在同一条赞序列表上，故表单从订单页搬到了该页）；`filmDetail/:id`（公开页）展示该片「用户热评」（即赞序的前 3 条）并给「查看全部 N 条评价」入口，匿名可读。

## 快速启动命令

> 环境要求、完整步骤与环境变量清单的唯一落点是 [README.md · 快速启动](README.md#快速启动)。此处只保留最小可运行命令。

### 1. 初始化数据库
```bash
cd xm_film/sql
mysql --default-character-set=utf8mb4 -u root -p < init.sql
```

> `init.sql` 是唯一的初始化路径（建库 + `schema.sql` + `data.sql`），**已存在的库请重建而非增量升级**；
> `record` / `ordered` / `mark` / `mark_like` 的行只能经真实业务接口产生，不要手工 `INSERT` 补。
> 完整口径见 [数据库说明](xm_film/sql/README.md)。

### 2. 启动后端
```bash
cd xm_film/springboot
mvn clean package -DskipTests
java -jar target/springboot-0.0.1-SNAPSHOT.jar
```
后端默认运行在 `http://localhost:9090`

### 3. 启动前端
```bash
cd xm_film/vue
npm install
npm run dev
```
前端默认运行在 `http://localhost:5173`

### 默认账号

三个演示账号由 `data.sql` 初始化，见 [README.md · 测试账号](README.md#测试账号)。

## 配置说明

配置项、默认值与全部环境变量（`DB_*` / `JWT_*` / `FILE_*` / `CORS_ALLOWED_ORIGINS` / `MYBATIS_LOG_*`）见 [README.md · 配置说明](README.md#配置说明) —— 本文件不再重复一份，避免两处默认值各自漂移。

## 开发守则

### 文档链完整性
所有架构级变更必须维护完整的文档链：**README.md/CLAUDE.md → 代码 → 数据库** 三者一致。改代码时按 [CONTRIBUTING.md · 文档归属](CONTRIBUTING.md#三文档归属一处事实一处归属) 找到对应落点同步更新，**不要**在第二个文档里再抄一份。

### 修改流程（防批量修复陷阱）

1. **先通读后修改** — 跨域切换（后端→前端→数据库）时，先读关键文件再改，不凭记忆
2. **三方校验** — 看到"错误"时不急于修复，对比 文档/代码/数据库 三方，找出真正的不一致源头（防确认偏差）
3. **逐块验证** — 批量修改时降低警戒线是危险的，每个逻辑块改完后需单独验证（编译/测试/启动）
4. **主动启动验证** — 改完后主动提议启动项目验证效果，不等人问
5. **文档同步** — 代码变更完成后检查 CLAUDE.md / README.md / Bug.md 是否需要同步更新

### 提交规范
提交信息规范、分支实践与**文档归属表**统一见 [CONTRIBUTING.md](CONTRIBUTING.md) —— 本文件不再重复。

## 相关文档

- [README.md](README.md) — 功能、技术栈、快速启动、配置、部署（对外门面）
- [CONTRIBUTING.md](CONTRIBUTING.md) — 提交信息规范、分支实践、**文档归属表**
- [Bug 修复记录](Bug.md) — 已修复 Bug 的根因与解决方案，遇到相似问题优先查阅
- [数据库说明](xm_film/sql/README.md) — 数据库表设计与初始化指引
- [前端设计规范](标准前端视觉与交互设计规范.md) — 三端视觉与交互标准（设计原则、令牌表、色板分端机制、组件与可访问性条款）
- [前端规范待办](前端规范待办.md) — 尚未落地的规范条目与整改进度（规范正文不记录进度）

## Current Architecture Notes

- Authentication state is centralized in `xm_film/vue/src/utils/authStorage.js`; router guards, Axios token injection, password pages, profile pages, and ticket purchase use the same storage helpers.
- Cross-shell entry lives in the top-right user area of each shell, as an explicit button: the front header renders 「管理后台」 for ADMIN/CINEMA (→ `/manage/home` or `/back/home`), and both back shells render 「前台首页」 (→ `/front/home`, never `router.back()` — an admin lands straight on their own backend home). Any entry rendered by a shell must match the route's `meta.roles`, because `Front.vue` gates 「购票记录」/「我的账户」/「个人中心」/「修改密码」 on `showUserEntries` (`!isAdmin && !isCinema`, so guests still see them and get the login prompt) — those routes are `roles: ['USER']`, and the backend side genuinely does not serve other roles (`/account/summary` is USER-only). Hiding is the fix; granting access is not. Standard: 《标准前端视觉与交互设计规范》§6.5. 同理，`取票大厅`（`/front/pickup`，`meta.guest`）**刻意不挂在 `showUserEntries` 下** —— 它是自助机口径、免登录，游客也必须能进，加上 `v-if` 就等于把唯一的自助取票通路藏起来。

- Backend password changes trust the JWT-derived request role instead of the request body role.
- `AuthInterceptor` enforces role boundaries for admin-only resources and write operations on protected resources.
- Database relations now use explicit keys for the main booking path: `room.cinema_id`, `record.film_id`, and `ordered.record_id`; `xm_film/sql` is the single source of truth for both schema and seed data.
- Film type/area display reads backend-resolved fields only: `areaName` (SQL `LEFT JOIN area`) and `typeList` (filled by `FilmService.fillFilmTypes` from `film_type`). `Film` has no `types` field — do not reintroduce frontend type/area dictionaries.
- 票房口径只有一个来源：后端按 `ordered` 实时聚合的「本系统累计售票收入」（`FilmMapper.xml` 的 `filmRevenueJoin`，只统计 `待取票/已取票`），单位是**元**，前端 `utils/format.js` 只做格式化。`film.box_office` 静态列已废弃、恒为 0（列注释已标明废弃，取值不被任何查询读取）。**`filmRevenueJoin` 的状态集合与 `OrderedMapper` 的占座判定同源**，新增改变"是否已支付"的状态时两处必须同步。
- 「今日票房」是同一口径的日期切片：`OrderedMapper.selectTodayPaidRevenue` 按 **`pay_time` 取日**（收款日，不是 `ordered.start` 放映日 —— 本系统是提前购票，按放映日聚合会让「今日」长期恒为 0），状态集合与前一条**同源**，因此它必然 ≤ 累计票房。它挂在 `GET /api/v1/films/box-office/today`（`/api/v1/films` 已在 `PUBLIC_READ_PREFIXES` 内，**没有为它新增任何放行规则**），返回 `{total, updatedAt}`，日期边界与统计时刻都由库时钟在同一条 SQL 里给出。**空集上 SUM 为 0 是真实值，前端因此用 `formatYuan` 渲染 `0.00元` 而不是「暂无数据」** —— `formatBoxOffice` 把 0 当缺失值，两者不可混用。
- Status tag colors are centralized in `xm_film/vue/src/constants/index.js` (`FILM_STATUS_MAP`/`getFilmStatusType`, `ORDER_STATUS_MAP`/`getOrderStatusType`, `RECORD_STATUS_MAP`/`getRecordStatusType`, `CINEMA_STATUS_MAP`/`getCinemaStatusType`); views import them instead of re-declaring the switch.
- 表格的**操作列必须显式写 `width`**：`el-table` 给未指定宽度的列按 `minWidth || 80` 起算再均分富余空间，列多的表里操作列只会拿到 ~80px，两个文字按钮（`继续支付 + 取消` 需 104px）必然折行；而 EP 的按钮间距是 `.el-button + .el-button { margin-left: 12px }`，**折行不改变它**，第二个按钮被右推 12px，两行就左右错开（BUG-049）。多按钮格套 `front-pages.scss` 的 `.row-actions`（flex + gap，并把该 margin 中和为 0）。加宽所需像素从"内容本就不需要 80px"的列上让出（展开列、2 字表头的列），**不要让表格最小总宽上涨** —— 否则窄视口凭空多出横向滚动条。
- 影院"上映哪些影片"由排片 `record` 派生（`FilmMapper.selectByCinema` / `CinemaMapper.selectByFilmId` 用 `EXISTS` 子查询）。**不存在影院-影片关联表**（原 `cinema_film` 已删除）——新建排片后前台立即可见，不要再引入第二张关联表。`record.film_id` 为 `NOT NULL`。
- 场次可购票性由 `start` 与 `status` 共同决定，唯一权威实现在 `RecordService.isPurchasable`（`start` 晚于当前 且 `status != 停售`）；`OrderedService.insertOrder` 复用该规则做下单拦截，前端 `CinemaDetail.vue` 的 `recordState()`/`canBuy()` 与之同构。`未开始/放映中/已结束` 是派生状态，不落库；`record.status` 只保留 `正常/停售` 一个人工开关。
- 排片的创建/编辑统一走 `RecordController` → `RecordService.validateSchedule(record, previousStart)`：校验影厅与影片归属、`start` 晚于当前（编辑时时间未改动则不重复校验，保证存量过期场次仍可停售）、`price > 0`、同影厅时段不重叠（按影片片长计算区间，无片长时按 120 分钟兜底），并按 `filmId` 回填 `title`。
- 父数据禁止物理删除：`ordered` 的 5 个外键、`record` 的 3 个外键、`room.cinema_id` 均为 `ON DELETE RESTRICT`；Film/Cinema/Room/Record/User 五个删除入口先做引用计数校验并返回可读提示。下架影片/场次请改 `status`，不要删除。
- `/api/v1/records` 在 `AuthInterceptor.PUBLIC_READ_PREFIXES` 内（匿名 GET 放行），因为公开的影院详情页需要拉取场次列表。
- 订单状态机只有一条合法路径：`待支付 → 待取票 → 已取票`，旁支为 `待支付 →（取消/超时）已取消` 与 `待取票 →（退票）已退票`。`OrderedService.updateScoped` 拒绝通用 PUT，状态只能经 `payOrder`/`cancelOrder`/`pickupOrder`/`redeemByCode`/`refundOrder` 迁移。前端 `ORDER_STATUS_MAP` 是状态色的唯一来源，筛选下拉由 `ORDER_STATUS_OPTIONS` 从同一 map 派生，避免筛选项与状态脱节。
- **取票码（`ordered.pickup_code`）没有自己的有效/失效状态**，可用性完全派生自订单状态：核销只接受 `status = '待取票'`（`OrderedMapper.markPickedUpByCode` 的状态条件更新）。这一个谓词同时实现了「一单一码」「用过即废」「退票/取消作废」「没付款不出发」，因此**不要为它新增 `used` / `revoked` 之类的标记列** —— 那会引入需要人工同步的第二处真相。有效期到放映结束（`ordered.start` + `film.time`，片长缺失时按 `RecordService.DEFAULT_DURATION_MINUTES` 兜底），同样不落库。码在 `payOrder` 内与扣款同一事务生成，故未支付订单永远没有码。
- 并发重复核销不靠悲观锁：读到的状态可能是 `待取票`，但写库走 `UPDATE ... WHERE status = '待取票'`，**受影响 0 行即判定为被人抢先**（`redeemByCode` 抛 409）。与余额扣减的 `UPDATE ... WHERE balance >= ?` 同一手法，改动时不要退回"先读后无条件写"。
- 取票码的**生成字母表与输入校验刻意不同**：生成用 `ABCDEFGHJKMNPQRSTUVWXYZ23456789`（剔除 I/L/O/0/1，人工从手机抄到自助机上看不错），校验放宽到 `[A-Z0-9]` 并把输入归一成 `XXXX-XXXX` 再等值查（`OrderedService.normalizePickupCode`）—— 生成收窄、输入放宽是刻意的：历史上给存量订单补过含 0/1 的十六进制码，收窄校验会把那批码挡在门外。归一后必须保持**等值**查询，写成 `WHERE REPLACE(pickup_code,'-','') = ?` 会让唯一索引失效。
- **柜台取票（`pickupOrder`）只放行 `CINEMA`，且必须是白名单写法**（`if (!"CINEMA".equals(role))`）。取票记录的是"影院把票交到顾客手里"这一物理事实，能如实断言的只有放映该场次的影院；`ADMIN` 在 `ensureOrderAccess` 里不受 `cinemaId` 约束，放行它等于"一键把任意用户的票记为已取"，而该方法**不记录操作人**、事后无法追溯，又因 `已取票` 是终态而毫无纠错用途（Bug.md BUG-050）。写成 denylist（"拒 USER"）会让新增角色静默继承取票能力 —— 与 `MarkService` 那轮"只有前端按钮在守"是同一种漏。`manage/Ordered.vue` 因此也没有取票按钮，权限落点在服务端而不是按钮可见性。用户通路只有免登录的 `redeemByCode`。
- 评价资格在服务端校验：`MarkService.add` 要求 `OrderedMapper.countPickedUpByUserAndFilm(userId, filmId) > 0`。**修改评价不重复校验**，因为 `已取票` 是终态（退票与删除都进不来），资格一旦成立不会被推翻。
- 占用座位的判定只有一个出处：`OrderedMapper.countSeatInUse` / `selectActiveByRecordId`，状态集合为 `NOT IN ('已取消','已退票')`，且待支付订单仅在 `pending_timeout_at > NOW()` 时锁座。**新增任何"释放座位"的状态时，两处查询必须同步**，否则座位永远锁死。
- `/api/v1/orders/seats` 对外返回的是投影 `SeatOccupancy`（`seat` + 后端按 JWT 算出的 `mine`），只有本人订单才带 `orderId`/`orders`/`total`/`pendingTimeoutAt`。**不要把 `Ordered` 实体直接回给这个端点** —— 那等于把该场次所有订单的订单号、`user_id`、金额发给任意登录用户（见 Bug.md BUG-040）。归属判定必须在后端做，前端只读 `mine`，不得再拿 `userId` 自己比对。
- 支付超时不用异常表达：`OrderedService.payOrder` 返回 `PayResult.TIMEOUT_CANCELLED`，由控制器翻译为 409。原因是该方法带 `rollbackFor = Exception.class`，"先取消再抛异常"会把取消一起回滚，订单停在待支付（另见 Bug.md BUG-035）。
- 金额字段必须是包装类型：`ordered.total` 为 `Double` 而非 `double`。`updateById` 用 `<if test="total != null">` 守卫，原始类型永远非 null，会让支付/取票/取消等局部更新把金额写成 0.00（另见 Bug.md BUG-034）。
- 选座图的座位来源是 `record.roomSeatRows` / `roomSeatCols`（`RecordMapper` 从 `room` 表 JOIN 出来），而不是写死的 8×8，也不是让用户端去读 `/api/v1/rooms`（USER 无权访问影厅接口）。后端座位合法性校验同样按影厅边界，单笔订单座位数上限 6（`OrderedService.MAX_SEATS_PER_ORDER`）。
- 影厅的 `title`（影院名称）由后端按所属影院记录派生，前端不再手填；`back/Room.vue` 的影院名是只读展示。影厅 `seat_rows`/`seat_cols` 合法区间为 1~50，由 `RoomController.validateSeatLayout` 兜底。
- 影片评分只有一个数值来源：`mark.score`（`DECIMAL(3,1)`，0~10）。`film.score` 是**纯派生列**，唯一写者是 `MarkService` → `FilmMapper.recalculateScore`（评价增删改后回写该片均分）；**无评价时为 NULL**（AVG 空集即 NULL，`schema.sql` 里该列也刻意不给 DEFAULT），前端一律渲染「暂无评分」。因此它**不接受客户端入参**：`FilmMapper.xml` 的 insert/updateById 刻意不写 score —— `FilmController` 继承 `BaseController` 的通用 `POST/PUT /api/v1/films` 直收整实体，留着那个 `<if>` 分支等于任何人能手写一个评分。`data.sql` 里 17 部影片的 `score` 全是 NULL（原先是人工填死的编造值，见下一条）。评价唯一性由 `MarkMapper.countByUserAndFilm` 保证（一人一片一条），评价人只认 JWT 里的 `userId`。前端「去评价」入口在 `front/Orders.vue`（仅 `已取票` 订单，点击跳影评页），`front/FilmDetail.vue` 展示热评（该赞序前 3 条）、完整列表在 `front/FilmMarks.vue`。
- 评分榜（`selectMarkTop`）**只含真正有评价的影片**：SQL 里带 `EXISTS (SELECT 1 FROM mark ...)` 谓词，与票房榜的 `WHERE rev.revenue > 0`（无售票不上榜）同构 —— 这是刻意的第二层保险，不能靠"无评价时 score 恰好为 NULL"这条不变量独自承担（种子曾预置编造分数，于是 `mark` 0 行时榜单照样有数据）。`film.score` 的"0.0 分"与"暂无评分"是两回事：`0.0` 是合法的真实评分，只有 NULL 才代表没人评过，前端 `utils/format.js` 的 `formatScore` 因此**只判 null/undefined**（与把 0 当缺失值的 `formatBoxOffice` 正好相反，两者不可混用）。
- 影院审核状态词表 `CinemaStatus` 只有 `未审核`/`已审核`（与 `schema.sql` 默认值、`data.sql` 种子一致）。`CinemaService.login` 拒绝未审核影院；公开列表经 `CinemaMapper.selectByFilmId` 的 `approvedOnly` 过滤，该标记由 `CinemaController` 按 `!isAdmin()` 传入 —— 管理员必须看得到未审核的，否则无法审核（见 Bug.md BUG-036）。
- `WebMvcConfig.excludePathPatterns` 是**角色盲区**：被排除的路径不执行 `AuthInterceptor`，request 上没有 `role`/`userId`，控制器里的角色判断会静默失效（BUG-036 即由此而来）。公开访问统一交给 `PUBLIC_READ_PREFIXES`，**不要往排除表里加路径**。
- 令牌失效时公开只读资源仍按匿名放行（`AuthInterceptor.isAnonymousRead`）。否则游客带着过期令牌浏览公开页会被判 401，而前端 `request.js` 的 401 处理会跳登录页 —— 公开内容就变成了事实上的必须登录。
- `Film.boxOffice` 已由原始 `double` 改为 `Double`，与 `ordered.total` 同因同治（`film.box_office` 有 `DEFAULT 0.0`，新增影片不受影响）。凡是被 `<if test="X != null">` 守卫的字段一律用包装类型。查询时该字段承载上面聚合出来的票房（元），**不再来自 `film.box_office` 列**。
- 账户余额的唯一可信来源是 `user.balance`；`fund_flow` 是只增的审计副本。**任何改余额的代码只能走 `WalletService`**（`creditRecharge` / `debitPurchase` / `creditRefund`），它统一做「行锁读余额 → 校验/变更 → 写一条流水」，因此不会出现"改了余额没记账"或"扣款成功但订单没出票"。
- 余额扣减是 `SELECT balance ... FOR UPDATE` 行锁 + `UPDATE ... WHERE balance >= ?` 条件更新双保险。**不要绕过 `WalletService` 直接用 `UserMapper.addBalance` 写业务代码** —— 那样会跳过流水与校验，余额与账本必然对不上。
- 金额字段一律 `BigDecimal`（`user.balance` / `recharge_order.amount` / `fund_flow.change_amount` 等），前端展示经 `Number(...).toFixed(2)`。`ordered.total`/`unit_price` 仍是 `Double`/`BigDecimal`，历史原因不同，新增资金字段不要再用 `double`。
- 充值单据状态机只有三个状态、两条边：`处理中 → 已完成`（回调成功，入账）、`处理中 → 已失败`（回调失败，余额不变）。**终态不可再流转**，重复回调返回业务冲突——这是幂等的唯一实现，新增任何充值入口都必须复用 `RechargeService.handleCallback`。
- 订单物理删除只允许终态废单（`已取消` / `已退票`），白名单在 `OrderedService.DELETABLE_STATUSES`，前端三端按钮由 `constants.isOrderDeletable` 同构渲染。**这是"删订单当免费退票用"的后门**：退票能回款而删除不能，一旦放开已支付订单的删除，资金账就永远对不平。
- 演示账号 `zhangsan` 预置 100 元余额（`data.sql` 是唯一出处）。余额不足的演示路径由"连买几张高价票"自然触发，不需要额外的穷账号。
- **`data.sql` 不预置交易类数据**：`record`（场次）/ `ordered`（订单）/ `mark`（评价）一律由真实业务接口产生。手写的订单必须同时伪造订单号、单价快照、支付凭证、余额扣减与资金流水 —— 老种子正是如此（`unit_price` 全为 NULL、`fund_flow` 里没有对应购票记录、`zhangsan` 余额未因那条 42 元订单扣减、单号是 12 位纯数字而真实单号是 `yyyyMMdd` + 8 位十六进制），一查就露。**要演示数据只能经真实业务接口生成**（前台下单 → 支付 → 取票 → 评价），不要手工 `INSERT` 补单。
- 后台大盘统计走 `StatisticsController` / `StatisticsService` → `GET /api/v1/statistics/overview`（仅 ADMIN），由 `CinemaMapper.countGroupByStatus` / `FilmMapper.countGroupByType` 用 `GROUP BY` 实时聚合。**前端不再拉全表自己算**（`manage/Home.vue` 原先为此拉取 films + cinemas + types 三张全表再在 JS 里聚合）。无数据时按规范 §11.2 渲染「暂无数据」占位，不塞编造默认值。
- 前端空态与异常的文案分工（规范 §11.2）：接口成功但无数据 → 「暂无数据」；请求失败（网络异常 / 超时 / 5xx）→ 「数据加载失败，请稍后重试」。失败提示由 `utils/request.js` 的响应拦截器统一给出，**页面内的 `catch` 只落错误态、不再重复弹提示**，否则同一次失败会弹两次。区分两者是必需的：请求失败时显示「暂无数据」会让用户以为系统里真的没有数据。
- `excludePathPatterns` 是角色盲区（BUG-036），账户/充值/流水端点**都在拦截器覆盖范围内**：`/api/v1/recharges`、`/api/v1/fund-flows`、`/api/v1/account` 均未加入 `PUBLIC_READ_PREFIXES`，因此未登录一律 401 而不是匿名放行。
- 登录 / 注册页共用一套外壳 `assets/css/auth-layout.scss`（两页各自 `@use` 进 `scoped` 块，与 `admin-layout.scss` 同构）。**卡片宽度只有 `.auth-card` 的 `max-width` 一个来源，卡片内部一律 `width: 100%`** —— 曾因内层写死 `380px` 而父级内容宽仅 192px，导致标题折行、表单溢出 188px（BUG-042）。容器用 `min-height: 100vh` + flex 居中，**不要**改回 `height: 100vh` + `overflow: hidden` + 绝对定位（矮视口会裁掉卡片且无法滚动）。口径见规范 §6.4；改动共用外壳时须同时核对登录/注册两页的上述布局不变量。
- 认证页表单的固定形态：`label-position="top"` + `status-icon` + 可见 `label` + 文本输入框 `@keyup.enter`（`el-form` 上 `@submit.prevent` 兜底）+ 图标一律组件绑定 `:prefix-icon="User"`（字符串写法不会解析，`main.js` 未全局注册图标集，BUG-043）。登录角色默认 `USER`，**不要**改回 `ADMIN`（BUG-044）。
- `mark_like` 是评价点赞的**纯关系表**（`PRIMARY KEY (mark_id, user_id)`，两个外键均 `ON DELETE CASCADE`）：一行即一个赞，"一人一赞"与"可取消"都由主键承担，赞数由 `COUNT(*)` 实时聚合，**刻意不落计数列** —— 冗余计数列会把真相分到两处（取消赞 / 评价被删 / 并发点赞任一处漏同步，计数就永久偏离且无法自证对错），与 `user.balance` 为唯一余额来源同一思路。`MarkLikeMapper` 因此**不继承 `BaseMapper`**（没有 CRUD 资源，继承来的 7 个方法只会是死代码），也没有实体类（只有关系、没有身份），四条语句只收/还 int。
- **「热评」不是第二条查询**：`filmDetail/:id` 的头部 3 条就是 `GET /api/v1/marks/by-film` 那条「`likeCount DESC, mark.id DESC`」排序（`MarkMapper.xml` 的 `selectFilmMarks`）的**前 3 行**（`pageSize=3`），影评页 `/front/filmMarks/:id` 是同一排序的完整分页。**不要再为热评另写一条 SQL 或另开一个端点** —— 两个排序一旦分叉，同一部片会在两个页面给出不同的"热门"。
- `MarkView` 的 `liked` / `mine` 由后端按 JWT 在 SQL 里算好（`viewer` 为 null 时一并为 false），**投影不含 `userId`** —— 下发作者 id 等于把"这条是不是我写的"下放给前端自己比对，正是 BUG-040 的漏（`/api/v1/orders/seats` 曾直接下发他人订单号与 `userId`）。归属只认 `mine` 布尔量，前端不得再拿 `userId` 自己比对。
- `MarkService.setLike` 必须 `@Transactional(isolation = Isolation.READ_COMMITTED)`。MySQL 默认的 REPEATABLE READ 下，本事务的读快照在第一条一致读（`requireExisting`）就已固定；`insertIfAbsent` 撞上另一个**尚未提交**的同键事务会阻塞到对方提交之后才返回，此后再用一致读 `COUNT`，读到的仍是那个早于对方提交的旧快照 —— 库里已有该行，回读却报 `liked=false` / `likeCount=0`，与"返回写库后的权威状态"正好相反（真库复现：5 个并发响应里 4 个报 `liked=false`，见 Bug.md BUG-051）。**不要改用锁定读 `FOR UPDATE`**：那会锁住该评价行，把同一部片子上所有人的点赞串行化。
- `insertIfAbsent` 用 `ON DUPLICATE KEY UPDATE mark_id = mark_id`，**不用 `INSERT IGNORE`**：`INSERT IGNORE` 把**所有**错误一并降级为警告，外键违规（`mark_id` 指向的评价已被删）同样只返回 `ROW_COUNT()=0`，与"已赞过"字节级相同，使二者不可区分；`ON DUPLICATE KEY` 只吸收重复键冲突，真实的外键错误照常抛 1452。端点是**构造上幂等**的：`liked` 是显式意图（非服务端 toggle）叠加主键去重；响应回读权威状态而非信任受影响行数，因为重复插入同样报 0 行。
- 点赞**没有新增任何拦截器放行规则**：`/api/v1/marks` 早已在 `PUBLIC_READ_PREFIXES` 内（评价列表本就匿名可读），`PUT /{id}/like` 的 `USER` 限制落在 `MarkController.requireUser`，不是 `excludePathPatterns` 那类角色盲区（BUG-036）。
