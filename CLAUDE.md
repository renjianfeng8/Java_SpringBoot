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
| Fastjson | 2.0.33 | JSON 处理 |
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
| Playwright | 1.59.1 | E2E 测试 |

## 目录结构

```
project_02/
├── README.md                          # 项目说明
├── CLAUDE.md                          # 项目文档（本文件）
├── CHANGELOG.md                       # 变更日志
├── LICENSE                            # 许可证
├── Bug.md                             # Bug 修复记录（修复前先查阅）
├── .env.example                       # 环境变量模板
├── docker-compose.yml                 # Docker 编排
├── docs/                              # 文档
│   └── README.md                      # 文档总索引
├── scripts/                           # 通用脚本
│   ├── start-dev.bat                  # 一键启动
│   ├── run-e2e-tests.bat              # E2E 测试运行
│   ├── start-claude.bat               # Claude 启动
│   ├── scan-project.sh                # 项目扫描
│   ├── generate-seed-uploads.ps1     # 种子占位文件生成
│   └── docker-entrypoint.sh           # Docker 入口脚本（自动填充 uploads 卷）
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
│   │       │   │   ├── CorsConfig.java         # CORS 跨域
│   │       │   │   ├── FileUtil.java           # 文件上传工具（含 MIME 白名单）
│   │       │   │   ├── JwtUtils.java           # JWT 令牌工具（JJWT 新版 API）
│   │       │   │   ├── Result.java             # 统一响应封装
│   │       │   │   └── enums/RoleEnum.java     # 角色枚举
│   │       │   ├── common/config/
│   │       │   │   ├── AuthInterceptor.java    # JWT 认证拦截器
│   │       │   │   └── WebMvcConfig.java       # Web MVC 配置
│   │       │   ├── controller/                 # 控制器层（14个）
│   │       │   ├── entity/                     # 实体类（14个）
│   │       │   ├── mapper/                     # MyBatis Mapper（14个）
│   │       │   ├── service/                    # 业务逻辑层（14个）
│   │       │   └── exception/                  # 异常处理
│   │       └── resources/
│   │           ├── application.yml             # 应用配置
│   │           └── mapper/                     # MyBatis XML 映射（14个）
│   ├── vue/                            # 前端（Vue 3）
│   │   ├── index.html                  # HTML 入口
│   │   ├── vite.config.js              # Vite 配置
│   │   ├── package.json                # 前端依赖
│   │   ├── src/
│   │   │   ├── main.js                 # Vue 入口
│   │   │   ├── App.vue                 # 根组件
│   │   │   ├── router/index.js         # 路由配置
│   │   │   ├── utils/request.js        # Axios 封装
│   │   │   ├── views/                  # 页面视图
│   │   │   │   ├── Login.vue / Register.vue / 404.vue
│   │   │   │   ├── Front.vue           # 用户前台布局
│   │   │   │   ├── Back.vue            # 影院后台布局
│   │   │   │   ├── Manage.vue          # 管理后台布局
│   │   │   │   ├── front/              # 12个用户端页面
│   │   │   │   ├── back/               # 7个影院端页面
│   │   │   │   └── manage/             # 16个管理端页面
│   │   │   └── assets/                 # 静态资源
│   │   └── e2e-tests/                  # E2E 测试
│   │       ├── playwright.config.mjs   # Playwright 配置
│   │       ├── e2e-scan.spec.mjs       # 全量测试脚本
│   │       ├── test.mjs                # 旧版测试脚本
│   │       └── screenshots/            # 截图目录
│   ├── sql/                           # 数据库初始化脚本
│   │   ├── README.md                  # 数据库说明
│   │   ├── schema.sql                 # 14张表建表语句
│   │   ├── data.sql                   # 初始数据
│   │   ├── init.sql                   # 一键初始化入口
│   │   └── seed-uploads/              # data.sql 引用的 61 个占位文件
│   ├── package.json
│   ├── package-lock.json

## 核心模块说明

### 三端角色
| 角色 | 名称 | 职责 |
|------|------|------|
| ADMIN | 系统管理员 | 全局配置、审核影院、管理所有数据 |
| CINEMA | 影院管理员 | 管理本影院影厅、排片、订单 |
| USER | 普通用户 | 浏览影片、购票、评价 |

### 功能模块
- **影片管理** — 影片 CRUD、分类/地区关联、演员关联、预告片上传
- **影院管理** — 影院注册审核、信息维护、影厅管理
- **排片管理** — 创建放映场次（关联影厅、时间、价格）
- **在线选座** — 8×8 可视化座位图、选定下单
- **订单系统** — 购票下单、订单状态流转（待取票/已取票/已取消）
- **评价系统** — 用户对影片评分评价
- **排行榜** — 票房榜 Top10、评分榜 Top5
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

> 匿名 GET 访问 `/api/v1/films`、`/api/v1/cinemas`、`/api/v1/types`、`/api/v1/areas`、`/api/v1/notices`、`/api/v1/actors` 等公开资源无需认证，由 AuthInterceptor 自动放行。写操作（POST/PUT/DELETE）仍需登录。

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
| `/api/v1/cinemas/page` | GET | 影院分页（支持按电影筛选） |
| `/api/v1/files/upload` | POST | 文件上传（图片/视频） |

## 页面清单

### 管理后台 (`/manage/*`) — 16个页面
home, admin, user, cinema, type, area, film, actor, notice, room, record, ordered, mark, video, person, password

### 影院后台 (`/back/*`) — 7个页面
home, film, room, record, ordered, person, password

### 用户前台 (`/front/*`) — 12个页面（公开浏览模式）
系统支持公开访问，无需登录即可浏览电影、影院、排行榜等公开内容。根路径 `/` 自动重定向到 `/front/home`。

| 访问模式 | 路由 | 说明 |
|----------|------|------|
| 公开访问（无需登录） | home, movie, filmDetail/:id, cinema, cinemaDetail/:id, filmCinema/:id, rank, search | 浏览类页面，无需认证 |
| 需登录（USER） | buyTicket, orders, person, password | 操作类页面，未登录时弹框提示跳转登录 |

访问受保护页面时，系统弹出确认框 → 跳转 `/login?redirect=<原路径>` → 登录成功后自动回跳。登录页根据角色（USER/CINEMA/ADMIN）分别跳转 `/front/home`、`/back/home`、`/manage/home`。

## Playwright E2E 验证说明

### 安装与运行
```bash
cd xm_film/vue
npm install
npx playwright install chromium
node e2e-tests/e2e-scan.spec.mjs
```

### 测试覆盖范围
- 后端 API 健康检查（10 个接口）
- 前端页面渲染与路由跳转（5 个场景）
- 管理员登录与完整页面导航（16 个管理页面）
- 电影分类 CRUD 流程闭环
- 分页功能验证
- 搜索功能验证
- 用户前台页面（7 个场景）
- 影院后台页面（7 个页面）
- 负面测试（5 个场景：错误密码、无 token 访问、路由越权、空选批量删除、未登录重定向）

### 生成 HTML 报告
```bash
npx playwright show-report xm_film/vue/e2e-tests/playwright-report
```

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

### 4. 运行 E2E 测试
```bash
cd xm_film/vue
node e2e-tests/e2e-scan.spec.mjs
```

### 默认账号
| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| ADMIN | 999 | 999 | 系统管理员 |
| CINEMA | asks | cinema123 | 影院管理员 |
| USER | zhangsan | user123 | 普通用户 |

### 种子文件（Docker 部署）
`xm_film/sql/seed-uploads/` 存储 data.sql 引用的 61 个 `/files/*` 占位文件（47 JPG、4 PNG、10 MP4），解决 Docker 部署时 `uploads` 命名卷为空导致的 404 问题。

- 生成脚本：`scripts/generate-seed-uploads.ps1`（解析 data.sql → 自动生成占位图）
- 更新种子：data.sql 中新加文件引用后，重新运行该脚本即可
- 部署机制：Dockerfile 将 seed-uploads 拷入镜像 → entrypoint 脚本检测 if uploads 卷为空 → 自动拷贝种子文件 → 启动 Java 应用
- 用户上传文件写入 uploads 卷，不受种子文件影响（仅首次部署时填充空卷）

## 配置说明

后端配置位于 `xm_film/springboot/src/main/resources/application.yml`：
- 服务端口：9090
- 数据库：`jdbc:mysql://localhost:3306/xm-film`
- JWT 密钥：`xm-film-secret-key-2024-springboot-vue3-jwt-auth`（支持环境变量 `JWT_SECRET`）
- JWT 过期：24 小时（86400000ms，支持环境变量 `JWT_EXPIRE`）
- 文件上传：`D:/project/picture`（支持环境变量 `FILE_UPLOAD_DIR`）
- 文件大小限制：50MB
- DB 密码：支持环境变量 `DB_PASSWORD`（默认 `123456`）
- MyBatis 日志：SLF4J + Logback，支持环境变量 `MYBATIS_LOG_IMPL`（默认 `Slf4jImpl`）和 `MYBATIS_LOG_LEVEL`（默认 `DEBUG`）

## 项目优化建议（当前状态）

1. **密码安全性**：✅ 已通过环境变量注入解决（`${DB_PASSWORD:123456}`）
2. **JWT 密钥**：✅ 已通过环境变量注入解决（`${JWT_SECRET:...}`）
3. **文件存储**：当前为本地存储，建议生产环境迁移至 OSS（阿里云/S3）
4. **日志配置**：✅ 已切换为 SLF4J + Logback，`logback-spring.xml` 按 mapper 包级别控制 SQL 日志（可通过 `MYBATIS_LOG_LEVEL` 环境变量调整）
5. **API 文档**：建议集成 Swagger/SpringDoc OpenAPI 自动生成接口文档
6. **单元测试**：后端仅依赖测试（spring-boot-starter-test），缺少业务单元测试
7. **前端构建**：生产构建后建议接入 CDN 分发静态资源
8. **CI/CD**：✅ 已配置 GitHub Actions 完整流水线（后端编译 → 前端构建 → MySQL 初始化 → 后端启动 → 59 用例 E2E 验证），支持 `application-ci.yml` CI 专属配置
9. **错误边界**：前端可引入 Vue ErrorBoundary 机制处理渲染异常
10. **权限校验**：✅ 已实现前端路由守卫 + 后端 AuthInterceptor 双重角色校验

## 开发守则

### 文档链完整性
所有架构级变更必须维护完整的文档链：**设计文档 → CLAUDE.md/README.md → 代码 → 数据库** 四者一致。当修改代码时，同步检查并更新所有链上文档。

### 修改流程（防批量修复陷阱）

1. **先通读后修改** — 跨域切换（后端→前端→数据库）时，先读关键文件再改，不凭记忆
2. **三方校验** — 看到"错误"时不急于修复，对比 文档/代码/数据库 三方，找出真正的不一致源头（防确认偏差）
3. **逐块验证** — 批量修改时降低警戒线是危险的，每个逻辑块改完后需单独验证（编译/测试/启动）
4. **主动启动验证** — 改完后主动提议启动项目验证效果，不等人问
5. **文档同步** — 代码变更完成后检查设计文档是否需要同步更新

> 详细说明参见 [重构设计文档](docs/superpowers/specs/2026-05-29-architecture-refactoring-design.md) 附录A：开发注意事项

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

- [文档总索引](docs/README.md) — 文档体系入口
- [Bug 修复记录](Bug.md) — 已修复 Bug 的根因与解决方案，遇到相似问题优先查阅
- [重构设计文档](docs/superpowers/specs/2026-05-29-architecture-refactoring-design.md) — 全链路架构重构设计（BaseCRUD/DTO/RESTful/数据库规范化/前端Composable）
- [产品需求文档](docs/product/README.md) — 用户需求、业务需求、竞品分析、PRD 与优化路线图
- [系统设计文档](docs/design/README.md) — 系统设计说明、架构图、数据库设计、接口安全与部署设计

## Current Architecture Notes

- Authentication state is centralized in `xm_film/vue/src/utils/authStorage.js`; router guards, Axios token injection, password pages, profile pages, and ticket purchase use the same storage helpers.
- Backend password changes trust the JWT-derived request role instead of the request body role.
- `AuthInterceptor` enforces role boundaries for admin-only resources and write operations on protected resources.
- Database relations now use explicit keys for the main booking path: `room.cinema_id`, `record.film_id`, and `ordered.record_id`; schema/data SQL under `xm_film/sql` and `xm_film/springboot/src/main/resources/db` are kept in sync.

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
- 新增 scripts/scan-project.sh 全栈项目扫描脚本
