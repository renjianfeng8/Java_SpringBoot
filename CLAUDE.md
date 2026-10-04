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
- [Bug 修复记录](Bug.md) — 规则篇（硬约束）+ 案例篇（根因）；遇到相似问题优先查阅
- [数据库说明](xm_film/sql/README.md) — 数据库表设计与初始化指引
- [前端设计规范](标准前端视觉与交互设计规范.md) — 三端视觉与交互标准（设计原则、令牌表、色板分端机制、组件与可访问性条款）
- [前端规范待办](前端规范待办.md) — 尚未落地的规范条目与整改进度（规范正文不记录进度）

## 架构不变量

> 本节是**结构事实**（能画成图/表、即使没出过 bug 也成立）的唯一落点。"改动时不要做 X / 必须做 Y"的硬约束见下方「硬约束索引」与 [Bug.md · 规则篇](Bug.md#规则篇)。

### 数据模型与关系
- 主订票路径用显式外键：`room.cinema_id`、`record.film_id`、`ordered.record_id`；`xm_film/sql` 是 schema 与种子数据的唯一出处。
- 影院"上映哪些影片"由排片 `record` 派生（`FilmMapper.selectByCinema` / `CinemaMapper.selectByFilmId` 用 `EXISTS` 子查询）。**不存在影院-影片关联表**；`record.film_id` 为 `NOT NULL`。（→ 规则 22）
- 影片的类型/地区只读后端解析字段：`areaName`（`LEFT JOIN area`）与 `typeList`（`FilmService.fillFilmTypes` 从 `film_type` 填）。`Film` 无 `types` 字段。（→ 规则 20）
- 影厅 `title`（影院名称）由后端按所属影院派生；座位规模随 `room.seat_rows` / `seat_cols`。（→ 规则 31）
- **选座**：座位来源 `record.roomSeatRows` / `roomSeatCols`（`RecordMapper` 从 `room` JOIN 出来），非写死 8×8；后端座位校验按影厅边界，单笔订单上限 6（`OrderedService.MAX_SEATS_PER_ORDER`）；USER 无权访问 `/api/v1/rooms`。（→ 规则 30）

### 口径来源（唯一聚合点）
- **票房**：`FilmMapper.xml` 的 `filmRevenueJoin` 按 `ordered` 实时聚合（只统计 `待取票/已取票`），单位**元**；前端 `utils/format.js` 只做格式化。`film.box_office` 静态列已废弃、恒为 0。
- **今日票房**：同一口径按 `pay_time` 取日的切片（`OrderedMapper.selectTodayPaidRevenue`），挂 `GET /api/v1/films/box-office/today`，返回 `{total, updatedAt}`。空集 SUM 为 0 是真实值（前端 `formatYuan` 渲染 `0.00元`，非「暂无数据」）。（→ 规则 75）
- **影片评分**：`mark.score` 是唯一数值来源；`film.score` 是纯派生列，唯一写者 `MarkService` → `FilmMapper.recalculateScore`，**无评价时为 NULL**。
- **账户余额**：`user.balance` 是唯一可信来源，`fund_flow` 是只增的审计副本。（→ 规则 38）
- **后台大盘**：`StatisticsController` → `GET /api/v1/statistics/overview`（仅 ADMIN），`GROUP BY` 实时聚合。

### 状态机
- **订单**：`待支付 → 待取票 → 已取票`，旁支 `待支付 →（取消/超时）已取消` 与 `待取票 →（退票）已退票`。`OrderedService.updateScoped` 拒绝通用 PUT，状态只能经 `payOrder` / `cancelOrder` / `pickupOrder` / `redeemByCode` / `refundOrder` 迁移。
- **充值单据**：`处理中 → 已完成` / `处理中 → 已失败`，终态不可再流转。（→ 规则 42）
- **影院审核词表** `CinemaStatus` 只有 `未审核`/`已审核`。（→ 规则 37）
- **场次可购票性**：唯一权威在 `RecordService.isPurchasable`（`start` 晚于当前 且 `status != 停售`）；`未开始/放映中/已结束` 派生不落库，`record.status` 只保留 `正常/停售`。（→ 规则 24）
- **排片创建/编辑**统一走 `RecordController` → `RecordService.validateSchedule`：校验影厅与影片归属、`start` 晚于当前、`price > 0`、同影厅不重叠（按片长算区间，无片长按 120 分钟兜底），并按 `filmId` 回填 `title`。

### 鉴权与放行
- `AuthInterceptor` 对 admin-only 资源与受保护资源的写操作做角色边界；公开只读统一走 `PUBLIC_READ_PREFIXES`。
- `/api/v1/records`、`/api/v1/films` 等在 `PUBLIC_READ_PREFIXES` 内，供公开页拉取。（→ 规则 25）
- 令牌失效时公开只读资源仍按匿名放行（`AuthInterceptor.isAnonymousRead`）。（→ 规则 33）
- 账户/充值/流水端点都在拦截器覆盖范围内，未登录一律 401。（→ 规则 32）

### 并发与一致性
- **座位占用判定**唯一出处 `OrderedMapper.countSeatInUse` / `selectActiveByRecordId`（`NOT IN ('已取消','已退票')`；待支付仅在 `pending_timeout_at > NOW()` 锁座）。（→ 规则 29）
- **余额扣减**：`SELECT ... FOR UPDATE` 行锁 + `UPDATE ... WHERE balance >= ?` 条件更新双保险。（→ 规则 40）
- 点赞 `MarkService.setLike` 用 `READ_COMMITTED` 隔离级别。（→ 规则 63）

### 前端
- 认证状态集中在 `xm_film/vue/src/utils/authStorage.js`；路由守卫、Axios 注令牌、改密/资料页共用同一组 helper。
- 状态色集中在 `constants/index.js`（`FILM/ORDER/RECORD/CINEMA_STATUS_MAP`），视图 import 而不自行实现 switch。（→ 规则 21）
- 跨端入口在每个外壳右上角，是显式按钮（前台给 ADMIN/CINEMA 「管理后台」，两后台给「前台首页」→ `/front/home`，不用 `router.back()`）。（→ 规则 73）
- **「热评」不是第二条查询**：`filmDetail/:id` 头部 3 条就是 `GET /api/v1/marks/by-film`（`likeCount DESC, mark.id DESC`）的前 3 行（`pageSize=3`）；影评页 `filmMarks/:id` 是同一排序的完整分页。
- `mark_like` 是纯关系表（`PRIMARY KEY (mark_id, user_id)`，两外键 `ON DELETE CASCADE`），赞数按 `COUNT(*)` 实时聚合，**无计数列**。

## 硬约束索引

> 一行一条，正文在 [Bug.md · 规则篇](Bug.md#规则篇)。新增教训只改规则篇，此处补一行。

- 金额 / 计数 / 比率字段用包装类型，不用原始类型（规则 27）
- 事务方法内不得"先写入再抛异常"表达失败（规则 28）
- 父数据禁止物理删除，下架改 `status`（规则 23）
- `excludePathPatterns` 是角色盲区，公开访问只走 `PUBLIC_READ_PREFIXES`（规则 32）
- 令牌失效时公开只读资源按匿名放行（规则 33）
- 余额变更只走 `WalletService`（规则 38）；扣减用行锁 + 条件更新（规则 40）
- 资金字段一律 `BigDecimal` 且 > 0（规则 41）
- 充值回调必须幂等，复用 `RechargeService.handleCallback`（规则 42）
- 订单物理删除只允许「已取消 / 已退票」（规则 44）
- 同步写库面测试用临时库 + 备用端口（规则 46）
- 只读视图回投影、不回实体；归属只认 `mine`（规则 47 / 48）
- 取票码不加 `used`/`revoked` 标记列（规则 67）；并发核销靠条件更新影响行数（规则 68）
- 取票码生成收窄、输入放宽，归一后等值查（规则 69）
- 柜台取票只放行 `CINEMA`，白名单写法（规则 62）
- 评价资格在服务端校验（规则 70）
- 表格操作列必须显式写 `width`（规则 60）
- 影厅容量随实体列走，不写死（规则 30）
- 派生字段不接受前端输入，后端按外键回填（规则 31）
- 派生指标列只留唯一写者、不接受客户端入参（规则 65）
- 去重插入用 `ON DUPLICATE KEY`，不用 `INSERT IGNORE`（规则 71）
- 点赞"写后回读"需 `READ_COMMITTED`（规则 63）
- 前端空态与异常文案分工，失败提示只在 `request.js` 统一给（规则 72）
- shell 渲染的入口必须匹配 `meta.roles`（规则 73）
- 改密只认 JWT 里的角色（规则 74）
- 同一配置项只留一个读取点、一个兜底值（规则 66）
- 种子只放基础配置，交易类数据走真实接口生成（规则 58）
- 同一主体对同一目标显式去重，如一人一片一条（规则 35）
- "是否已支付 / 占座"状态集合多消费点必须同源（规则 75）
