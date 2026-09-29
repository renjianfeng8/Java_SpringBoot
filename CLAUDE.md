# 影院购票管理系统 (Cinema Ticket Management System)

基于 **Spring Boot 3.3 + Vue 3 + MySQL** 构建的在线电影购票管理平台，支持三端角色分离运营（管理员后台、影院端、用户端）。

## 技术栈

### 后端
| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.3.13 | 应用框架 |
| Java | 17 | 运行环境 |
| MyBatis | 3.0.4 | ORM 持久层 |
| MySQL | 8.0 | 数据库 |
| PageHelper | 1.4.6 | 分页插件 |
| JJWT | 0.11.5 | JWT 令牌认证 |
| Spring Security Crypto | - | BCrypt 密码加密 |
| Jackson | 2.17.3 | JSON 序列化（随 starter-web 传递引入） |
| SpringDoc OpenAPI | 2.8.17 | API 文档（OpenAPI 规范 + UI 页面） |
| Lombok | - | 代码简化 |

### 前端
| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5.13 | 前端框架 |
| Vite | 6.2.4 | 构建工具 |
| Element Plus | 2.9.11 | UI 组件库 |
| Vue Router | 4.5.0 | 路由管理 |
| Axios | 1.9.0 | HTTP 请求 |
| ECharts | 6.0.0 | 数据可视化 |
| wangEditor | 5.x | 富文本编辑器 |

## 目录结构

```
project_02/
├── README.md                          # 项目说明
├── CLAUDE.md                          # 项目文档（本文件）
├── LICENSE                            # 许可证
├── Bug.md                             # Bug 修复记录（修复前先查阅）
├── 标准前端视觉与交互设计规范.md        # 前端视觉与交互设计规范（新增页面前先查阅）
├── 前端规范待办.md                     # 规范未落地条目与整改进度（规范正文不记进度）
├── scripts/                           # 通用脚本
│   ├── start-dev.bat                  # 一键启动
│   ├── seed-demo-data.py              # 演示数据生成（走真实接口；默认计划模式，--apply 才写库）
│   └── verify/                        # 隔离环境验证脚本（备用端口 + 临时库，不碰开发库）
│       ├── p4-account-wallet-e2e.py   # 账户-充值-订单闭环端到端验证（70 断言，可反复运行）
│       └── p4-concurrency.py          # 余额扣减并发正确性验证（12 断言，可反复运行）
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
│   │       │   │   └── enums/                  # 词表枚举（RoleEnum / OrderStatus / RecordStatus / PayResult / CinemaStatus / RechargeStatus / FundSource）
│   │       │   ├── common/config/
│   │       │   │   ├── AuthInterceptor.java    # JWT 认证拦截器
│   │       │   │   └── WebMvcConfig.java       # Web MVC 配置
│   │       │   ├── controller/                 # 控制器层（21个，含 TicketController 取票大厅）
│   │       │   ├── entity/                     # 实体类（16个）
│   │       │   ├── mapper/                     # MyBatis Mapper（15个）
│   │       │   ├── service/                    # 业务逻辑层（17个）
│   │       │   └── exception/                  # 异常处理
│   │       └── resources/
│   │           ├── application.yml             # 应用配置
│   │           └── mapper/                     # MyBatis XML 映射（15个）
│   ├── vue/                            # 前端（Vue 3）
│   │   ├── index.html                  # HTML 入口
│   │   ├── vite.config.js              # Vite 配置（含 AutoImport / Components 插件）
│   │   ├── jsconfig.json               # 路径别名与编译选项
│   │   ├── package.json                # 前端依赖
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
│   │   │   │   └── format.js           # 票房格式化（后端聚合的累计售票收入，单位元）
│   │   │   ├── views/                  # 页面视图
│   │   │   │   ├── Login.vue / Register.vue / 404.vue
│   │   │   │   ├── Front.vue           # 用户前台布局
│   │   │   │   ├── Back.vue            # 影院后台布局
│   │   │   │   ├── Manage.vue          # 管理后台布局
│   │   │   │   ├── front/              # 13个用户端页面（含取票大厅 Pickup.vue）
│   │   │   │   ├── back/               # 7个影院端页面
│   │   │   │   └── manage/             # 16个管理端页面
│   │   │   └── assets/                 # 静态资源（css / imgs）
│   │   │       └── css/                # tokens.scss 设计令牌 · index.scss EP 主题覆写
│   │   │                               # global.css 全局重置 · admin-layout.scss 后台外壳
│   │   │                               # auth-layout.scss 认证页外壳（登录/注册共用）
│   │   │                               # admin-pages.scss / front-pages.scss 列表页共用骨架
│   ├── sql/                           # 数据库初始化脚本
│   │   ├── README.md                  # 数据库说明
│   │   ├── schema.sql                 # 16张表建表语句
│   │   ├── data.sql                   # 基础种子（管理员/用户/影院/影厅/影片/词表；
│   │   │                              #   不含场次/订单/评价 —— 这三类由真实接口产生）
│   │   ├── init.sql                   # 一键初始化入口
│   │   └── migration-*.sql            # 增量迁移（已有库执行，幂等）

## 核心模块说明

### 三端角色
| 角色 | 名称 | 职责 |
|------|------|------|
| ADMIN | 系统管理员 | 全局配置、审核影院、管理所有数据 |
| CINEMA | 影院管理员 | 管理本影院影厅、排片、订单 |
| USER | 普通用户 | 浏览影片、购票、评价 |

### 功能模块
- **影片管理** — 影片 CRUD、分类/地区关联、演员关联、预告片上传
- **影院管理** — 影院注册审核（未审核既不可登录也不对外展示，管理端提供「审核通过」入口）、信息维护、影厅管理
- **排片管理** — 创建放映场次（关联影片、影厅、时间、票价）；校验时间晚于当前、票价大于 0、同影厅时段不重叠
- **在线选座** — 座位规模由影厅配置（`room.seat_rows` / `seat_cols`，默认 8×8）驱动的可视化选座图、选定下单；本人未支付锁座可继续支付或释放
- **订单系统** — 购票下单、订单状态流转（待支付 → 待取票 → 已取票；待支付可取消或超时自动取消；待取票可退票 → 已退票）、支付与退款资金凭证留痕；订单留存**单价快照**（`ordered.unit_price`），场次改价不影响历史订单
- **取票与取票大厅** — 支付成功即生成**取票码**（`ordered.pickup_code`，一单一码，`XXXX-XXXX`）；前台「取票大厅」模拟影院自助机，凭码核销出票（待取票 → 已取票）。该核销端点**免登录**（码本身即凭证），有效期到放映结束，用过/退票/取消即失效，均由订单状态派生
- **账户与资金** — 用户账户余额（`user.balance`）、充值单据（处理中 → 已完成/已失败）、资金流水账本（充值/购票/退票三类来源，记录变动前后余额与关联单据ID）。**购票为余额支付**：支付时校验余额并原子扣减，余额不足则订单保持待支付、座位继续锁定；退票时金额退回余额。不接第三方支付渠道，充值由「提交单据 + 模拟支付回调」两步完成
- **评价系统** — **已取票**用户在订单页对影片评分 + 评语（一人一片一条，可修改）；评价均分回写 `film.score` 并驱动评分榜；影片详情页公开展示评价列表
- **排行榜** — 票房榜 Top10（按 `ordered` 实时聚合的累计售票收入）、评分榜 Top5（按 `film.score`，即该片评价均分）；前台首页另展示「今日票房」（今天支付的售票收入合计，匿名可读）
- **搜索筛选** — 按影片名称、类型、年份、地区多维筛选
- **文件上传** — 图片/视频上传，支持本地存储（MIME 白名单校验）

### 后端架构设计
后端采用三层泛型抽象架构消除重复 CRUD 代码：

| 层级 | 基类 | 职责 |
|------|------|------|
| Controller | `BaseController<T>` | 提供 7 个标准 RESTful CRUD 端点 |
| Service | `BaseService<T>` | 提供 CRUD 方法 + 事务管理 |
| Mapper | `BaseMapper<T>` | 提供 MyBatis CRUD 方法定义 |

- 13 个 Service 全部继承 `BaseService<T>`，仅需实现 `mapper()` 方法返回具体 Mapper
- 13 个 Controller 继承 `BaseController<T>`，仅需声明 `@RequestMapping` + 构造函数注入
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

### 用户前台 (`/front/*`) — 14个页面（公开浏览模式）
系统支持公开访问，无需登录即可浏览电影、影院、排行榜等公开内容。根路径 `/` 自动重定向到 `/front/home`。

| 访问模式 | 路由 | 说明 |
|----------|------|------|
| 公开访问（无需登录） | home, movie, filmDetail/:id, cinema, cinemaDetail/:id, filmCinema/:id, rank, search, **pickup** | 浏览类页面 + **取票大厅**（自助机口径，凭取票码核销，所以刻意不要求登录，导航也对游客可见） |
| 需登录（USER） | buyTicket, orders, account, person, password | 操作类页面，未登录时弹框提示跳转登录 |

访问受保护页面时，系统弹出确认框 → 跳转 `/login?redirect=<原路径>` → 登录成功后自动回跳。登录页根据角色（USER/CINEMA/ADMIN）分别跳转 `/front/home`、`/back/home`、`/manage/home`。

购票闭环：选座页支付成功后 `OrderPayDialog` **不关闭**，就地切成「购票成功」凭证态显示取票码（`GET /api/v1/orders/{id}` 回查，含后端 join 的影片/影院/影厅名）并给「去取票大厅」；此后可在 `orders` 页 `待取票` 行的展开区再次查看取票码。

评价闭环：`orders` 页对 `已取票` 的订单提供「去评价 / 修改评价」（弹窗内评分 + 评语）；`filmDetail/:id`（公开页）展示该片的评价列表，匿名可读。

## 快速启动命令

### 环境要求
- JDK 17+、Maven 3.6+、MySQL 8.0+、Node.js 18+、npm 9+

### 1. 初始化数据库
```bash
cd xm_film/sql
mysql --default-character-set=utf8mb4 -u root -p < init.sql
```
或手动执行：
```sql
CREATE DATABASE `xm-film` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `xm-film`;
SOURCE xm_film/sql/schema.sql;
SOURCE xm_film/sql/data.sql;
```

> **已有数据库请勿重跑 `schema.sql`/`data.sql`**，改用增量迁移并按文件名日期顺序执行。
> 账户余额/充值单据/资金流水/订单单价需要 `migration-20260928-p4-account-wallet.sql`，
> 未执行该脚本时账户页与余额支付会报表不存在。
> 废弃 `film.box_office` 静态票房需要 `migration-20260929-deprecate-box-office.sql`；
> 取票码需要 `migration-20260929-pickup-code.sql`（加 `ordered.pickup_code` 唯一列，并给存量
> `待取票` 订单补码 —— 不补的话升级前已支付的订单在取票大厅查不到）；
> 存量库里的演示场次/订单/评价请用 `scripts/seed-demo-data.py` 清理与重建（按演示账号边界删除，不靠猜 id）。

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
| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| ADMIN | 999 | 999 | 系统管理员 |
| CINEMA | asks | cinema123 | 影院管理员 |
| USER | zhangsan | user123 | 普通用户 |

## 配置说明

后端配置位于 `xm_film/springboot/src/main/resources/application.yml`：
- 服务端口：9090
- 数据库：`jdbc:mysql://localhost:3306/xm-film`
- JWT 密钥：`xm-film-secret-key-2024-springboot-vue3-jwt-auth`（支持环境变量 `JWT_SECRET`）
- JWT 过期：24 小时（86400000ms，支持环境变量 `JWT_EXPIRE`）
- 文件上传：`D:/project/picture`（支持环境变量 `FILE_UPLOAD_DIR`）
- 文件大小限制：50MB
- DB 密码：支持环境变量 `DB_PASSWORD`（默认 `123456`）
- DB 库名：支持环境变量 `DB_NAME`（默认 `xm-film`，便于用临时库做验证而不影响开发库）
- MyBatis 日志：SLF4J + Logback，支持环境变量 `MYBATIS_LOG_IMPL`（默认 `Slf4jImpl`）和 `MYBATIS_LOG_LEVEL`（默认 `DEBUG`）

## 项目优化建议（当前状态）

1. **密码安全性**：✅ 已通过环境变量注入解决（`${DB_PASSWORD:123456}`）
2. **JWT 密钥**：✅ 已通过环境变量注入解决（`${JWT_SECRET:...}`）
3. **文件存储**：当前为本地存储，建议生产环境迁移至 OSS（阿里云/S3）
4. **日志配置**：✅ 已切换为 SLF4J + Logback，`logback-spring.xml` 按 mapper 包级别控制 SQL 日志（可通过 `MYBATIS_LOG_LEVEL` 环境变量调整）
5. **API 文档**：✅ 已集成 SpringDoc OpenAPI —— `SwaggerConfig` 定义 OpenAPI Bean 与全局 Bearer 鉴权方案，控制器标注 `@Tag`/`@Operation`，规范端点 `/v3/api-docs`，UI 页面 `static/swagger-ui.html`（swagger-ui 资源走 CDN，离线环境需改用 `springdoc-openapi-starter-webmvc-ui` 本地内嵌）
6. **单元测试**：✅ 已覆盖 13 个测试类 / 173 个用例 —— Service 层 CRUD 与权限（Admin/User/Cinema/Film/Ordered/Mark）、订单状态机（支付超时/退票窗口/座位边界与单笔上限）、评价规则（评分区间/一人一片去重/均分回写/归属不可转移/**未取票不得评价**）、影院审核与可见性下推、订单座位并发冲突、AuthInterceptor 访问边界（含令牌失效与匿名放行、**匿名写白名单的三处收窄**）、全局异常处理；**账户资金**（余额足额/不足扣减、退款入账、金额非正拒绝、流水前后余额与关联单据）、**充值单据状态机**（提交不改余额、回调成功/失败、重复回调被拒、金额上限、跨用户回调被拒）、**订单删除守卫**按状态拒绝、**取票码核销**（一次性/退票作废/过场作废/未支付拒绝/并发抢核销/输入归一化）；`mvn test` 可复现。**今日票房的 SQL 谓词与取票码/核销的全链路 Mockito 测不到**（打桩后测的是桩，不是谓词、不是唯一索引、不是状态条件更新），这两块必须另在「备用端口 + 临时库」上打真实库验证，单测覆盖不到它们
7. **前端构建**：生产构建后建议接入 CDN 分发静态资源
8. **CI/CD**：✅ 已配置 GitHub Actions 流水线（后端编译 → 前端构建）
9. **错误边界**：前端可引入 Vue ErrorBoundary 机制处理渲染异常
10. **权限校验**：✅ 已实现前端路由守卫 + 后端 AuthInterceptor 双重角色校验

## 开发守则

### 文档链完整性
所有架构级变更必须维护完整的文档链：**CLAUDE.md/README.md → 代码 → 数据库** 三者一致。当修改代码时，同步检查并更新所有链上文档。

### 修改流程（防批量修复陷阱）

1. **先通读后修改** — 跨域切换（后端→前端→数据库）时，先读关键文件再改，不凭记忆
2. **三方校验** — 看到"错误"时不急于修复，对比 文档/代码/数据库 三方，找出真正的不一致源头（防确认偏差）
3. **逐块验证** — 批量修改时降低警戒线是危险的，每个逻辑块改完后需单独验证（编译/测试/启动）
4. **主动启动验证** — 改完后主动提议启动项目验证效果，不等人问
5. **文档同步** — 代码变更完成后检查 CLAUDE.md / README.md / Bug.md 是否需要同步更新

### 提交规范
本仓库遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范：
`<type>: <description>`

| 类型 | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | 修复 |
| `docs` | 文档 |
| `test` | 测试 |
| `refactor` | 重构 |
| `chore` | 构建/工具 |

## 相关文档

- [README.md](README.md) — 项目说明、快速启动、部署方式
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
- 票房口径只有一个来源：后端按 `ordered` 实时聚合的「本系统累计售票收入」（`FilmMapper.xml` 的 `filmRevenueJoin`，只统计 `待取票/已取票`），单位是**元**，前端 `utils/format.js` 只做格式化。`film.box_office` 静态列已废弃、恒为 0（`migration-20260929-deprecate-box-office.sql` 清零存量值并改列注释）。**`filmRevenueJoin` 的状态集合与 `OrderedMapper` 的占座判定同源**，新增改变"是否已支付"的状态时两处必须同步。
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
- 取票码的**生成字母表与输入校验刻意不同**：生成用 `ABCDEFGHJKMNPQRSTUVWXYZ23456789`（剔除 I/L/O/0/1，人工从手机抄到自助机上看不错），校验放宽到 `[A-Z0-9]` 并把输入归一成 `XXXX-XXXX` 再等值查（`OrderedService.normalizePickupCode`）—— 因为 `migration-20260929-pickup-code.sql` 给存量订单补的是含 0/1 的十六进制码，收窄校验会把它们挡在门外。归一后必须保持**等值**查询，写成 `WHERE REPLACE(pickup_code,'-','') = ?` 会让唯一索引失效。
- **柜台取票（`pickupOrder`）只放行 `CINEMA`，且必须是白名单写法**（`if (!"CINEMA".equals(role))`）。取票记录的是"影院把票交到顾客手里"这一物理事实，能如实断言的只有放映该场次的影院；`ADMIN` 在 `ensureOrderAccess` 里不受 `cinemaId` 约束，放行它等于"一键把任意用户的票记为已取"，而该方法**不记录操作人**、事后无法追溯，又因 `已取票` 是终态而毫无纠错用途（Bug.md BUG-050）。写成 denylist（"拒 USER"）会让新增角色静默继承取票能力 —— 与 `MarkService` 那轮"只有前端按钮在守"是同一种漏。`manage/Ordered.vue` 因此也没有取票按钮，权限落点在服务端而不是按钮可见性。用户通路只有免登录的 `redeemByCode`。
- 评价资格在服务端校验：`MarkService.add` 要求 `OrderedMapper.countPickedUpByUserAndFilm(userId, filmId) > 0`。**修改评价不重复校验**，因为 `已取票` 是终态（退票与删除都进不来），资格一旦成立不会被推翻。
- 占用座位的判定只有一个出处：`OrderedMapper.countSeatInUse` / `selectActiveByRecordId`，状态集合为 `NOT IN ('已取消','已退票')`，且待支付订单仅在 `pending_timeout_at > NOW()` 时锁座。**新增任何"释放座位"的状态时，两处查询必须同步**，否则座位永远锁死。
- `/api/v1/orders/seats` 对外返回的是投影 `SeatOccupancy`（`seat` + 后端按 JWT 算出的 `mine`），只有本人订单才带 `orderId`/`orders`/`total`/`pendingTimeoutAt`。**不要把 `Ordered` 实体直接回给这个端点** —— 那等于把该场次所有订单的订单号、`user_id`、金额发给任意登录用户（见 Bug.md BUG-040）。归属判定必须在后端做，前端只读 `mine`，不得再拿 `userId` 自己比对。
- 支付超时不用异常表达：`OrderedService.payOrder` 返回 `PayResult.TIMEOUT_CANCELLED`，由控制器翻译为 409。原因是该方法带 `rollbackFor = Exception.class`，"先取消再抛异常"会把取消一起回滚，订单停在待支付（另见 Bug.md BUG-035）。
- 金额字段必须是包装类型：`ordered.total` 为 `Double` 而非 `double`。`updateById` 用 `<if test="total != null">` 守卫，原始类型永远非 null，会让支付/取票/取消等局部更新把金额写成 0.00（另见 Bug.md BUG-034）。
- 选座图的座位来源是 `record.roomSeatRows` / `roomSeatCols`（`RecordMapper` 从 `room` 表 JOIN 出来），而不是写死的 8×8，也不是让用户端去读 `/api/v1/rooms`（USER 无权访问影厅接口）。后端座位合法性校验同样按影厅边界，单笔订单座位数上限 6（`OrderedService.MAX_SEATS_PER_ORDER`）。
- 影厅的 `title`（影院名称）由后端按所属影院记录派生，前端不再手填；`back/Room.vue` 的影院名是只读展示。影厅 `seat_rows`/`seat_cols` 合法区间为 1~50，由 `RoomController.validateSeatLayout` 兜底。
- 影片评分只有一个数值来源：`mark.score`（`DECIMAL(3,1)`，0~10）。`film.score` 是派生缓存，由 `MarkService` 在评价增删改后经 `FilmMapper.recalculateScore` 回写；无评价时保留基线分、**不归零**（`data.sql` 已不再预置评价，种子影片的 `score` 就是它的基线分本身；老库由 `migration-20260928-p3` 的同一条 `EXISTS` 守卫规则收敛）。评价唯一性由 `MarkMapper.countByUserAndFilm` 保证（一人一片一条），评价人只认 JWT 里的 `userId`。前端「去评价」入口在 `front/Orders.vue`（仅 `已取票` 订单），`front/FilmDetail.vue` 展示评价列表。
- 影院审核状态词表 `CinemaStatus` 只有 `未审核`/`已审核`（与 `schema.sql` 默认值、`data.sql` 种子一致）。`CinemaService.login` 拒绝未审核影院；公开列表经 `CinemaMapper.selectByFilmId` 的 `approvedOnly` 过滤，该标记由 `CinemaController` 按 `!isAdmin()` 传入 —— 管理员必须看得到未审核的，否则无法审核（见 Bug.md BUG-036）。
- `WebMvcConfig.excludePathPatterns` 是**角色盲区**：被排除的路径不执行 `AuthInterceptor`，request 上没有 `role`/`userId`，控制器里的角色判断会静默失效（BUG-036 即由此而来）。公开访问统一交给 `PUBLIC_READ_PREFIXES`，**不要往排除表里加路径**。
- 令牌失效时公开只读资源仍按匿名放行（`AuthInterceptor.isAnonymousRead`）。否则游客带着过期令牌浏览公开页会被判 401，而前端 `request.js` 的 401 处理会跳登录页 —— 公开内容就变成了事实上的必须登录。
- `Film.boxOffice` 已由原始 `double` 改为 `Double`，与 `ordered.total` 同因同治（`film.box_office` 有 `DEFAULT 0.0`，新增影片不受影响）。凡是被 `<if test="X != null">` 守卫的字段一律用包装类型。查询时该字段承载上面聚合出来的票房（元），**不再来自 `film.box_office` 列**。
- 账户余额的唯一可信来源是 `user.balance`；`fund_flow` 是只增的审计副本。**任何改余额的代码只能走 `WalletService`**（`creditRecharge` / `debitPurchase` / `creditRefund`），它统一做「行锁读余额 → 校验/变更 → 写一条流水」，因此不会出现"改了余额没记账"或"扣款成功但订单没出票"。
- 余额扣减是 `SELECT balance ... FOR UPDATE` 行锁 + `UPDATE ... WHERE balance >= ?` 条件更新双保险。**不要绕过 `WalletService` 直接用 `UserMapper.addBalance` 写业务代码** —— 那样会跳过流水与校验，余额与账本必然对不上（并发验证见 `scripts/verify/p4-concurrency.py`）。
- 金额字段一律 `BigDecimal`（`user.balance` / `recharge_order.amount` / `fund_flow.change_amount` 等），前端展示经 `Number(...).toFixed(2)`。`ordered.total`/`unit_price` 仍是 `Double`/`BigDecimal`，历史原因不同，新增资金字段不要再用 `double`。
- 充值单据状态机只有三个状态、两条边：`处理中 → 已完成`（回调成功，入账）、`处理中 → 已失败`（回调失败，余额不变）。**终态不可再流转**，重复回调返回业务冲突——这是幂等的唯一实现，新增任何充值入口都必须复用 `RechargeService.handleCallback`。
- 订单物理删除只允许终态废单（`已取消` / `已退票`），白名单在 `OrderedService.DELETABLE_STATUSES`，前端三端按钮由 `constants.isOrderDeletable` 同构渲染。**这是"删订单当免费退票用"的后门**：退票能回款而删除不能，一旦放开已支付订单的删除，资金账就永远对不平。
- 演示账号 `zhangsan` 预置 100 元余额（`data.sql` 与 `migration-20260928-p4-account-wallet.sql` 保持一致）。余额不足的演示路径由"连买几张高价票"自然触发，不需要额外的穷账号。
- **`data.sql` 不预置交易类数据**：`record`（场次）/ `ordered`（订单）/ `mark`（评价）一律由真实业务接口产生。手写的订单必须同时伪造订单号、单价快照、支付凭证、余额扣减与资金流水 —— 老种子正是如此（`unit_price` 全为 NULL、`fund_flow` 里没有对应购票记录、`zhangsan` 余额未因那条 42 元订单扣减、单号是 12 位纯数字而真实单号是 `yyyyMMdd` + 8 位十六进制），一查就露。**要演示数据请跑 `scripts/seed-demo-data.py`**：默认计划模式（`--apply` 才写库），走真实接口生成 3 条已支付订单 + 3 条真实评价 + 真实充值与流水；幂等可重跑，按"同影厅同影片已有可购票场次就复用"避免排片膨胀，并且只在检测到 v1 种子订单（单号 12 位纯数字）时才清理旧种子排片。
- 后台大盘统计走 `StatisticsController` / `StatisticsService` → `GET /api/v1/statistics/overview`（仅 ADMIN），由 `CinemaMapper.countGroupByStatus` / `FilmMapper.countGroupByType` 用 `GROUP BY` 实时聚合。**前端不再拉全表自己算**（`manage/Home.vue` 原先为此拉取 films + cinemas + types 三张全表再在 JS 里聚合）。无数据时按规范 §11.2 渲染「暂无数据」占位，不塞编造默认值。
- 前端空态与异常的文案分工（规范 §11.2）：接口成功但无数据 → 「暂无数据」；请求失败（网络异常 / 超时 / 5xx）→ 「数据加载失败，请稍后重试」。失败提示由 `utils/request.js` 的响应拦截器统一给出，**页面内的 `catch` 只落错误态、不再重复弹提示**，否则同一次失败会弹两次。区分两者是必需的：请求失败时显示「暂无数据」会让用户以为系统里真的没有数据。
- `excludePathPatterns` 是角色盲区（BUG-036），账户/充值/流水端点**都在拦截器覆盖范围内**：`/api/v1/recharges`、`/api/v1/fund-flows`、`/api/v1/account` 均未加入 `PUBLIC_READ_PREFIXES`，因此未登录一律 401 而不是匿名放行。
- 登录 / 注册页共用一套外壳 `assets/css/auth-layout.scss`（两页各自 `@use` 进 `scoped` 块，与 `admin-layout.scss` 同构）。**卡片宽度只有 `.auth-card` 的 `max-width` 一个来源，卡片内部一律 `width: 100%`** —— 曾因内层写死 `380px` 而父级内容宽仅 192px，导致标题折行、表单溢出 188px（BUG-042）。容器用 `min-height: 100vh` + flex 居中，**不要**改回 `height: 100vh` + `overflow: hidden` + 绝对定位（矮视口会裁掉卡片且无法滚动）。口径见规范 §6.4，回归守卫在 `tests/design-tokens.test.mjs`。
- 认证页表单的固定形态：`label-position="top"` + `status-icon` + 可见 `label` + 文本输入框 `@keyup.enter`（`el-form` 上 `@submit.prevent` 兜底）+ 图标一律组件绑定 `:prefix-icon="User"`（字符串写法不会解析，`main.js` 未全局注册图标集，BUG-043）。登录角色默认 `USER`，**不要**改回 `ADMIN`（BUG-044）。

## Git 提交历史

### 约定式提交规范
本仓库遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范：
`<type>: <description>`

| 类型 | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | 修复 |
| `docs` | 文档 |
| `test` | 测试 |
| `refactor` | 重构 |
| `chore` | 构建/工具 |

### 最近提交
```
89d94a93 docs: 同步更新 .md 文档中的数据库路径引用 (BUG-002/006)

9525efa4 refactor: 数据库脚本重构 — 目录规范化 + schema/data 分离 (BUG-002)

- 移除 数据库/ 中文目录，新建 xm_film/sql/（schema.sql + data.sql + init.sql）
- 新增完整 CREATE TABLE 定义（14 张表，含字段类型/注释/默认值）
- 配置 spring.sql.init 自动初始化支持
- 更新 CLAUDE.md 目录树和初始化指引

28646785 feat: 全栈自动化工程化构建 — CI/CLAUDE.md/E2E测试/启动脚本

- 新增 CLAUDE.md 完整项目文档
- 新增 GitHub Actions CI 配置
- 新增 Playwright 全量 E2E 测试（53 用例，100% 通过）
- 新增 start-dev.bat / run-e2e-tests.bat 一键启动脚本
- 新增 scripts/scan-project.sh 全栈项目扫描脚本（2026-09-27 清理时移除）
