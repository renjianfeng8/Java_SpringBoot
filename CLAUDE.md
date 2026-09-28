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
├── scripts/                           # 通用脚本
│   ├── start-dev.bat                  # 一键启动
│   └── verify/                        # 隔离环境验证脚本（备用端口 + 临时库，不碰开发库）
│       ├── p4-account-wallet-e2e.py   # 账户-充值-订单闭环端到端验证（69 断言，可反复运行）
│       └── p4-concurrency.py          # 余额扣减并发正确性验证（11 断言，可反复运行）
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
│   │       │   ├── controller/                 # 控制器层（19个）
│   │       │   ├── entity/                     # 实体类（16个）
│   │       │   ├── mapper/                     # MyBatis Mapper（15个）
│   │       │   ├── service/                    # 业务逻辑层（16个）
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
│   │   │   │   └── OrderPayDialog.vue  # 支付弹窗（选座页与订单页共用）
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
│   │   │   │   └── format.js           # 票房格式化（DB 万元 → 万/亿）
│   │   │   ├── views/                  # 页面视图
│   │   │   │   ├── Login.vue / Register.vue / 404.vue
│   │   │   │   ├── Front.vue           # 用户前台布局
│   │   │   │   ├── Back.vue            # 影院后台布局
│   │   │   │   ├── Manage.vue          # 管理后台布局
│   │   │   │   ├── front/              # 12个用户端页面
│   │   │   │   ├── back/               # 7个影院端页面
│   │   │   │   └── manage/             # 16个管理端页面
│   │   │   └── assets/                 # 静态资源（css / imgs）
│   │   │       └── css/                # tokens.scss 设计令牌 · index.scss EP 主题覆写
│   │   │                               # global.css 全局重置 · admin-layout.scss 后台外壳
│   │   │                               # admin-pages.scss / front-pages.scss 列表页共用骨架
│   ├── sql/                           # 数据库初始化脚本
│   │   ├── README.md                  # 数据库说明
│   │   ├── schema.sql                 # 16张表建表语句
│   │   ├── data.sql                   # 初始数据
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
- **账户与资金** — 用户账户余额（`user.balance`）、充值单据（处理中 → 已完成/已失败）、资金流水账本（充值/购票/退票三类来源，记录变动前后余额与关联单据ID）。**购票为余额支付**：支付时校验余额并原子扣减，余额不足则订单保持待支付、座位继续锁定；退票时金额退回余额。不接第三方支付渠道，充值由「提交单据 + 模拟支付回调」两步完成
- **评价系统** — 已取票用户在订单页对影片评分 + 评语（一人一片一条，可修改）；评价均分回写 `film.score` 并驱动评分榜；影片详情页公开展示评价列表
- **排行榜** — 票房榜 Top10、评分榜 Top5（按 `film.score`，即该片评价均分）
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

> `marks` 的写操作有额外规则（在 `MarkController` 内校验，拦截器只做前缀级判断）：发表评价仅限 USER 且评价人取自 JWT；修改/删除仅限本人，ADMIN 可管理全部。
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
| `/api/v1/films/box-office/top` | GET | 票房排行榜 Top10 |
| `/api/v1/films/mark/top` | GET | 评分排行榜 Top5 |
| `/api/v1/films/search` | GET | 按标题搜索电影 |
| `/api/v1/films/by-cinema` | GET | 按影院查询电影 |
| `/api/v1/cinemas/page` | GET | 影院分页（支持按电影筛选）；匿名/非管理员只返回 `已审核` 影院，管理员返回全部（否则后台审核列表查不到待审核影院） |
| `/api/v1/files/upload` | POST | 文件上传（图片/视频） |

### 订单状态机接口（`/api/v1/orders/**`）
订单不支持通用 PUT 更新（`OrderedService.updateScoped` 直接拒绝），状态流转全部走显式端点：

| 路径 | 方法 | 说明 | 允许角色 |
|------|------|------|----------|
| `/api/v1/orders/create` | POST | 下单（校验场次可售、座位在影厅范围内且未被占用） | USER |
| `/api/v1/orders/seats` | GET | 查询某场次占用中的座位（`{seat, mine}` 投影；仅本人订单附带 orderId/金额/倒计时） | 登录用户 |
| `/api/v1/orders/{id}/pay` | PUT | 支付待支付订单；超时则取消并返回 409 | 订单归属方 |
| `/api/v1/orders/{id}/cancel` | PUT | 取消待支付订单 | 订单归属方 |
| `/api/v1/orders/{id}/pickup` | PUT | 取票（待取票 → 已取票） | ADMIN / CINEMA |
| `/api/v1/orders/{id}/refund` | PUT | 退票（待取票 → 已退票，需放映前 60 分钟以上） | 订单归属方 |

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

### 用户前台 (`/front/*`) — 13个页面（公开浏览模式）
系统支持公开访问，无需登录即可浏览电影、影院、排行榜等公开内容。根路径 `/` 自动重定向到 `/front/home`。

| 访问模式 | 路由 | 说明 |
|----------|------|------|
| 公开访问（无需登录） | home, movie, filmDetail/:id, cinema, cinemaDetail/:id, filmCinema/:id, rank, search | 浏览类页面，无需认证 |
| 需登录（USER） | buyTicket, orders, account, person, password | 操作类页面，未登录时弹框提示跳转登录 |

访问受保护页面时，系统弹出确认框 → 跳转 `/login?redirect=<原路径>` → 登录成功后自动回跳。登录页根据角色（USER/CINEMA/ADMIN）分别跳转 `/front/home`、`/back/home`、`/manage/home`。

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
6. **单元测试**：✅ 已覆盖 13 个测试类 / 154 个用例 —— Service 层 CRUD 与权限（Admin/User/Cinema/Film/Ordered/Mark）、订单状态机（支付超时/退票窗口/座位边界与单笔上限）、评价规则（评分区间/一人一片去重/均分回写/归属不可转移）、影院审核与可见性下推、订单座位并发冲突、AuthInterceptor 访问边界（含令牌失效与匿名放行）、全局异常处理；**账户资金**（余额足额/不足扣减、退款入账、金额非正拒绝、流水前后余额与关联单据）、**充值单据状态机**（提交不改余额、回调成功/失败、重复回调被拒、金额上限、跨用户回调被拒）、**订单删除守卫**按状态拒绝；`mvn test` 可复现
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
- [前端设计规范](标准前端视觉与交互设计规范.md) — 三端视觉与交互标准（令牌表、色板分端机制、附录 B 现状偏差清单）

## Current Architecture Notes

- Authentication state is centralized in `xm_film/vue/src/utils/authStorage.js`; router guards, Axios token injection, password pages, profile pages, and ticket purchase use the same storage helpers.
- Backend password changes trust the JWT-derived request role instead of the request body role.
- `AuthInterceptor` enforces role boundaries for admin-only resources and write operations on protected resources.
- Database relations now use explicit keys for the main booking path: `room.cinema_id`, `record.film_id`, and `ordered.record_id`; `xm_film/sql` is the single source of truth for both schema and seed data.
- Film type/area display reads backend-resolved fields only: `areaName` (SQL `LEFT JOIN area`) and `typeList` (filled by `FilmService.fillFilmTypes` from `film_type`). `Film` has no `types` field — do not reintroduce frontend type/area dictionaries.
- Box office formatting is centralized in `xm_film/vue/src/utils/format.js`; `film.box_office` is stored in **万元** (see `xm_film/sql/schema.sql`), so it renders 万 below 1 亿 and 亿 at or above it.
- Status tag colors are centralized in `xm_film/vue/src/constants/index.js` (`FILM_STATUS_MAP`/`getFilmStatusType`, `ORDER_STATUS_MAP`/`getOrderStatusType`, `RECORD_STATUS_MAP`/`getRecordStatusType`, `CINEMA_STATUS_MAP`/`getCinemaStatusType`); views import them instead of re-declaring the switch.
- 影院"上映哪些影片"由排片 `record` 派生（`FilmMapper.selectByCinema` / `CinemaMapper.selectByFilmId` 用 `EXISTS` 子查询）。**不存在影院-影片关联表**（原 `cinema_film` 已删除）——新建排片后前台立即可见，不要再引入第二张关联表。`record.film_id` 为 `NOT NULL`。
- 场次可购票性由 `start` 与 `status` 共同决定，唯一权威实现在 `RecordService.isPurchasable`（`start` 晚于当前 且 `status != 停售`）；`OrderedService.insertOrder` 复用该规则做下单拦截，前端 `CinemaDetail.vue` 的 `recordState()`/`canBuy()` 与之同构。`未开始/放映中/已结束` 是派生状态，不落库；`record.status` 只保留 `正常/停售` 一个人工开关。
- 排片的创建/编辑统一走 `RecordController` → `RecordService.validateSchedule(record, previousStart)`：校验影厅与影片归属、`start` 晚于当前（编辑时时间未改动则不重复校验，保证存量过期场次仍可停售）、`price > 0`、同影厅时段不重叠（按影片片长计算区间，无片长时按 120 分钟兜底），并按 `filmId` 回填 `title`。
- 父数据禁止物理删除：`ordered` 的 5 个外键、`record` 的 3 个外键、`room.cinema_id` 均为 `ON DELETE RESTRICT`；Film/Cinema/Room/Record/User 五个删除入口先做引用计数校验并返回可读提示。下架影片/场次请改 `status`，不要删除。
- `/api/v1/records` 在 `AuthInterceptor.PUBLIC_READ_PREFIXES` 内（匿名 GET 放行），因为公开的影院详情页需要拉取场次列表。
- 订单状态机只有一条合法路径：`待支付 → 待取票 → 已取票`，旁支为 `待支付 →（取消/超时）已取消` 与 `待取票 →（退票）已退票`。`OrderedService.updateScoped` 拒绝通用 PUT，状态只能经 `payOrder`/`cancelOrder`/`pickupOrder`/`refundOrder` 迁移。前端 `ORDER_STATUS_MAP` 是状态色的唯一来源，筛选下拉由 `ORDER_STATUS_OPTIONS` 从同一 map 派生，避免筛选项与状态脱节。
- 占用座位的判定只有一个出处：`OrderedMapper.countSeatInUse` / `selectActiveByRecordId`，状态集合为 `NOT IN ('已取消','已退票')`，且待支付订单仅在 `pending_timeout_at > NOW()` 时锁座。**新增任何"释放座位"的状态时，两处查询必须同步**，否则座位永远锁死。
- `/api/v1/orders/seats` 对外返回的是投影 `SeatOccupancy`（`seat` + 后端按 JWT 算出的 `mine`），只有本人订单才带 `orderId`/`orders`/`total`/`pendingTimeoutAt`。**不要把 `Ordered` 实体直接回给这个端点** —— 那等于把该场次所有订单的订单号、`user_id`、金额发给任意登录用户（见 Bug.md BUG-040）。归属判定必须在后端做，前端只读 `mine`，不得再拿 `userId` 自己比对。
- 支付超时不用异常表达：`OrderedService.payOrder` 返回 `PayResult.TIMEOUT_CANCELLED`，由控制器翻译为 409。原因是该方法带 `rollbackFor = Exception.class`，"先取消再抛异常"会把取消一起回滚，订单停在待支付（另见 Bug.md BUG-035）。
- 金额字段必须是包装类型：`ordered.total` 为 `Double` 而非 `double`。`updateById` 用 `<if test="total != null">` 守卫，原始类型永远非 null，会让支付/取票/取消等局部更新把金额写成 0.00（另见 Bug.md BUG-034）。
- 选座图的座位来源是 `record.roomSeatRows` / `roomSeatCols`（`RecordMapper` 从 `room` 表 JOIN 出来），而不是写死的 8×8，也不是让用户端去读 `/api/v1/rooms`（USER 无权访问影厅接口）。后端座位合法性校验同样按影厅边界，单笔订单座位数上限 6（`OrderedService.MAX_SEATS_PER_ORDER`）。
- 影厅的 `title`（影院名称）由后端按所属影院记录派生，前端不再手填；`back/Room.vue` 的影院名是只读展示。影厅 `seat_rows`/`seat_cols` 合法区间为 1~50，由 `RoomController.validateSeatLayout` 兜底。
- 影片评分只有一个数值来源：`mark.score`（`DECIMAL(3,1)`，0~10）。`film.score` 是派生缓存，由 `MarkService` 在评价增删改后经 `FilmMapper.recalculateScore` 回写；无评价时保留基线分、**不归零**（种子数据用同一条 `EXISTS` 守卫的 SQL 规则，保证新库与增量库一致）。评价唯一性由 `MarkMapper.countByUserAndFilm` 保证（一人一片一条），评价人只认 JWT 里的 `userId`。前端「去评价」入口在 `front/Orders.vue`（仅 `已取票` 订单），`front/FilmDetail.vue` 展示评价列表。
- 影院审核状态词表 `CinemaStatus` 只有 `未审核`/`已审核`（与 `schema.sql` 默认值、`data.sql` 种子一致）。`CinemaService.login` 拒绝未审核影院；公开列表经 `CinemaMapper.selectByFilmId` 的 `approvedOnly` 过滤，该标记由 `CinemaController` 按 `!isAdmin()` 传入 —— 管理员必须看得到未审核的，否则无法审核（见 Bug.md BUG-036）。
- `WebMvcConfig.excludePathPatterns` 是**角色盲区**：被排除的路径不执行 `AuthInterceptor`，request 上没有 `role`/`userId`，控制器里的角色判断会静默失效（BUG-036 即由此而来）。公开访问统一交给 `PUBLIC_READ_PREFIXES`，**不要往排除表里加路径**。
- 令牌失效时公开只读资源仍按匿名放行（`AuthInterceptor.isAnonymousRead`）。否则游客带着过期令牌浏览公开页会被判 401，而前端 `request.js` 的 401 处理会跳登录页 —— 公开内容就变成了事实上的必须登录。
- `Film.boxOffice` 已由原始 `double` 改为 `Double`，与 `ordered.total` 同因同治（`film.box_office` 有 `DEFAULT 0.0`，新增影片不受影响）。凡是被 `<if test="X != null">` 守卫的字段一律用包装类型。
- 账户余额的唯一可信来源是 `user.balance`；`fund_flow` 是只增的审计副本。**任何改余额的代码只能走 `WalletService`**（`creditRecharge` / `debitPurchase` / `creditRefund`），它统一做「行锁读余额 → 校验/变更 → 写一条流水」，因此不会出现"改了余额没记账"或"扣款成功但订单没出票"。
- 余额扣减是 `SELECT balance ... FOR UPDATE` 行锁 + `UPDATE ... WHERE balance >= ?` 条件更新双保险。**不要绕过 `WalletService` 直接用 `UserMapper.addBalance` 写业务代码** —— 那样会跳过流水与校验，余额与账本必然对不上（并发验证见 `scripts/verify/p4-concurrency.py`）。
- 金额字段一律 `BigDecimal`（`user.balance` / `recharge_order.amount` / `fund_flow.change_amount` 等），前端展示经 `Number(...).toFixed(2)`。`ordered.total`/`unit_price` 仍是 `Double`/`BigDecimal`，历史原因不同，新增资金字段不要再用 `double`。
- 充值单据状态机只有三个状态、两条边：`处理中 → 已完成`（回调成功，入账）、`处理中 → 已失败`（回调失败，余额不变）。**终态不可再流转**，重复回调返回业务冲突——这是幂等的唯一实现，新增任何充值入口都必须复用 `RechargeService.handleCallback`。
- 订单物理删除只允许终态废单（`已取消` / `已退票`），白名单在 `OrderedService.DELETABLE_STATUSES`，前端三端按钮由 `constants.isOrderDeletable` 同构渲染。**这是"删订单当免费退票用"的后门**：退票能回款而删除不能，一旦放开已支付订单的删除，资金账就永远对不平。
- 演示账号 `zhangsan` 预置 100 元余额（`data.sql` 与 `migration-20260928-p4-account-wallet.sql` 保持一致）。余额不足的演示路径由"连买几张高价票"自然触发，不需要额外的穷账号。
- `excludePathPatterns` 是角色盲区（BUG-036），账户/充值/流水端点**都在拦截器覆盖范围内**：`/api/v1/recharges`、`/api/v1/fund-flows`、`/api/v1/account` 均未加入 `PUBLIC_READ_PREFIXES`，因此未登录一律 401 而不是匿名放行。

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
