# 多角色影院票务运营平台

基于 **Spring Boot 3.3 + Vue 3 + MySQL** 构建的多角色影院票务运营平台，覆盖用户购票、影院排片、平台审核、订单流转、权限隔离、CI/E2E 自动化验证与 Docker 化部署。

![CI](https://github.com/renjianfeng8/Java_SpringBoot/actions/workflows/ci.yml/badge.svg)

## 在线演示

| 项目 | 地址 |
|------|------|
| 前端演示 | http://localhost:5173 |
| 后端健康检查 | http://localhost:9090/api/v1/health |

## 测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | 999 | 999 |
| 影院管理员 | asks | cinema123 |
| 普通用户 | zhangsan | user123 |

**代码质量**: 全栈 E2E 自动化测试覆盖（59 用例，含负面测试，100% 通过率），BCrypt 密码加密 + JWT 认证 + RBAC 权限控制，GitHub Actions CI 流水线。

---

## 项目亮点

- **多角色 RBAC**：管理员、影院端、用户端分离，后端拦截器和业务层共同保证权限边界。
- **订单一致性**：购票链路校验排片、座位、订单状态，防止重复购票和越权操作。
- **工程化验证**：GitHub Actions 自动执行后端构建、前端构建、MySQL 初始化和 Playwright E2E。
- **部署交付**：支持 Docker Compose 轻量化部署，生产配置通过环境变量注入。
- **文档闭环**：产品、设计、数据库、接口、安全、Bug 复盘和面试材料同步维护。

---

## 目录

- [项目亮点](#项目亮点)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [架构概览](#架构概览)
- [快速启动](#快速启动)
- [配置说明](#配置说明)
- [功能模块](#功能模块)
- [API 概览](#api-概览)
- [数据库设计](#数据库设计)
- [安全机制](#安全机制)
- [E2E 测试](#e2e-测试)
- [部署指南](#部署指南)
- [相关文档](#相关文档)

---

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
| Commons Lang3 | 3.14.0 | 工具类库 |

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
| Sass | 1.89.0 | CSS 预处理器 |

---

## 项目结构

```
xm_film/
├── springboot/                        # 后端（Spring Boot）
│   └── src/main/java/com/example/springboot/
│       ├── common/                    # 公共组件
│       │   ├── BaseController.java    # 泛型 CRUD 控制器基类（7 个标准接口）
│       │   ├── BaseService.java       # 泛型 CRUD Service 基类
│       │   ├── BaseMapper.java        # MyBatis 通用 Mapper 接口
│       │   ├── config/
│       │   │   ├── AuthInterceptor.java   # JWT 认证拦截器
│       │   │   └── WebMvcConfig.java      # Web MVC 配置
│       │   ├── CorsConfig.java        # CORS 跨域配置
│       │   ├── FileUtil.java          # 文件上传工具类
│       │   ├── JwtUtils.java          # JWT 令牌工具
│       │   └── Result.java            # 统一响应封装
│       ├── controller/                # 控制器层（16个）
│       │   ├── AuthController.java    # 登录/注册/密码修改（原 WebController）
│       │   ├── FileUploadController.java # 文件上传
│       │   └── 14 个资源 Controller    # 继承 BaseController，3-15 行代码
│       ├── entity/                    # 实体类
│       ├── mapper/                    # MyBatis Mapper 接口
│       ├── service/                   # 业务逻辑层
│       └── exception/                 # 异常处理
│   └── src/main/resources/
│       ├── application.yml            # 应用配置
│       └── mapper/                    # MyBatis XML 映射
│           ├── FilmMapper.xml
│           ├── AdminMapper.xml
│           ├── UserMapper.xml
│           ├── CinemaMapper.xml
│           ├── ActorMapper.xml
│           ├── AreaMapper.xml
│           ├── TypeMapper.xml
│           ├── NoticeMapper.xml
│           ├── OrderedMapper.xml
│           ├── RecordMapper.xml
│           ├── RoomMapper.xml
│           ├── MarkMapper.xml
│           └── VideoMapper.xml
│
├── vue/                               # 前端（Vue 3）
│   └── src/
│       ├── views/                     # 页面视图
│       │   ├── Login.vue              # 登录页
│       │   ├── Register.vue           # 注册页
│       │   ├── Front.vue              # 用户前台布局
│       │   ├── Back.vue               # 影院后台布局
│       │   ├── Manage.vue             # 管理后台布局
│       │   ├── front/                 # 用户端页面
│       │   │   ├── Home.vue           # 首页
│       │   │   ├── Movie.vue          # 影片列表
│       │   │   ├── FilmDetail.vue     # 影片详情
│       │   │   ├── Cinema.vue         # 影院列表
│       │   │   ├── CinemaDetail.vue   # 影院详情
│       │   │   ├── FilmCinema.vue     # 影片排片
│       │   │   ├── BuyTicket.vue      # 购票选座
│       │   │   ├── Orders.vue         # 我的订单
│       │   │   ├── Rank.vue           # 排行榜
│       │   │   ├── Search.vue         # 搜索
│       │   │   ├── Person.vue         # 个人资料
│       │   │   └── Password.vue       # 修改密码
│       │   ├── back/                  # 影院端页面
│       │   │   ├── Home.vue           # 影院首页
│       │   │   ├── Film.vue           # 影片管理
│       │   │   ├── Room.vue           # 影厅管理
│       │   │   ├── Record.vue         # 排片管理
│       │   │   ├── Ordered.vue        # 订单管理
│       │   │   ├── Person.vue         # 个人资料
│       │   │   └── Password.vue       # 修改密码
│       │   └── manage/                # 管理后台页面
│       │       ├── Home.vue           # 首页仪表盘
│       │       ├── Admin.vue          # 管理员管理
│       │       ├── User.vue           # 用户管理
│       │       ├── Cinema.vue         # 影院审核
│       │       ├── Film.vue           # 影片管理
│       │       ├── Actor.vue          # 演职人员
│       │       ├── Type.vue           # 电影分类
│       │       ├── Area.vue           # 地区管理
│       │       ├── Notice.vue         # 公告管理
│       │       ├── Room.vue           # 影厅管理
│       │       ├── Record.vue         # 放映记录
│       │       ├── Ordered.vue        # 订单管理
│       │       ├── Video.vue          # 预告片管理
│       │       ├── Mark.vue           # 评价管理
│       │       ├── Person.vue         # 个人资料
│       │       └── Password.vue       # 修改密码
│       ├── composables/               # 组合式 API
│       │   ├── useAuth.js             # 认证状态管理
│       │   ├── useCrud.js             # 通用 CRUD 操作
│       │   └── useFormDialog.js       # 表单弹窗控制
│       ├── constants/index.js         # 常量（角色/状态映射）
│       ├── utils/
│       │   ├── request.js             # Axios 封装
│       │   ├── format.js              # 格式化工具
│       │   └── status.js              # 状态工具函数
│       └── assets/                    # 静态资源
```

---

## 架构概览

```mermaid
flowchart LR
  User[用户端 Vue 3] --> API[Spring Boot API]
  Cinema[影院端 Vue 3] --> API
  Admin[管理端 Vue 3] --> API
  API --> Auth[JWT + RBAC]
  API --> Service[业务服务层]
  Service --> MyBatis[MyBatis Mapper]
  MyBatis --> MySQL[(MySQL)]
  API --> Upload[本地文件存储]
```

## 架构设计

### 后端泛型三层架构

系统采用泛型基类抽象消除 90% 重复 CRUD 代码：

| 层级 | 基类 | 职责 |
|------|------|------|
| Controller | `BaseController<T>` | 提供 7 个标准 RESTful 端点（list/getById/page/add/update/delete/deleteBatch） |
| Service | `BaseService<T>` | 提供 CRUD 方法 + `@Transactional` 事务管理 |
| Mapper | `BaseMapper<T>` | 提供 MyBatis CRUD 方法定义 |

- 13 个 Service 全部继承 `BaseService<T>`，仅需实现 `mapper()` 方法
- 13 个 Controller 继承 `BaseController<T>`，仅需声明 `@RequestMapping` + 构造函数注入（3-15 行代码）
- 复杂业务（Film 排行榜、Cinema 按电影筛选）通过方法覆写实现

### 前端 Composable 架构

| Composable | 职责 |
|------------|------|
| `useAuth` | 认证状态管理、登录/登出、角色判断 |
| `useCrud` | 通用 CRUD 操作（增删改查/分页/批量删除） |
| `useFormDialog` | 表单弹窗状态控制（打开/关闭/提交） |

---

## 快速启动

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Node.js 18+
- npm 9+

### 1. 初始化数据库

```sql
CREATE DATABASE `xm-film` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

执行项目提供的 `xm_film/sql/init.sql` 一键初始化脚本（或依次执行 `schema.sql` + `data.sql`）。使用 MySQL 客户端导入时请指定 `--default-character-set=utf8mb4`，避免中文默认值和初始数据在不同终端编码下被错误解析。

### 2. 启动后端

```bash
cd xm_film/springboot
mvn clean package -DskipTests
java -jar target/springboot-0.0.1-SNAPSHOT.jar
```

服务默认启动在 `http://localhost:9090`。

### 3. 启动前端

```bash
cd xm_film/vue
npm install
npm run dev
```

前端默认启动在 `http://localhost:5173`（端口可能因占用自增为 5174/5175）。

### 4. 默认账号

> 管理员账号 `999`、影院账号 `asks` 和用户 `zhangsan` 均通过 `data.sql` 初始化，默认账号见顶部表格。

### 5. API 文档（Swagger）

启动后端后访问：

```
http://localhost:9090/swagger-ui.html
```

所有 API 接口自动生成文档，支持在线调试（需先获取 JWT Token 登录）。

> 注意：Swagger UI 从 CDN 加载，服务端无需额外依赖，首次加载需联网。

### 6. Docker 部署

```bash
docker compose up -d --build
```

构建并启动 MySQL、Spring Boot 后端和 Nginx 前端。访问 `http://localhost/` 打开系统，访问 `http://localhost:9090/api/v1/health` 验证后端健康状态。

---

## 配置说明

核心配置位于 `application.yml`：

```yaml
# 服务端口
server:
  port: 9090

# 数据库连接
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/xm-film
    username: root
    password: 123456

# JWT 认证
jwt:
  secret: xm-film-secret-key-...      # 生产环境请修改
  expire: 86400000                    # 24小时过期

# 文件上传
file:
  upload-dir: D:/project/picture      # 文件存储路径
  max-file-size: 50MB                 # 单文件大小限制
```

> 生产环境建议通过环境变量注入敏感配置：
> - `DB_PASSWORD` — 数据库密码（默认 `123456`）
> - `JWT_SECRET` — JWT 签名密钥
> - `FILE_UPLOAD_DIR` — 文件上传存储路径
> - `MYBATIS_LOG_LEVEL` — SQL 日志级别（默认 `DEBUG`）
>
> 前端后端地址在 `vue/.env` 中通过 `VITE_API_BASE_URL` 配置，修改一处即可切换环境。
>
> CI 环境使用 `application-ci.yml` 独立配置（MySQL host、日志级别、上传目录）。

---

## 功能模块

### 三端角色

| 角色 | 名称 | 职责 |
|------|------|------|
| ADMIN | 系统管理员 | 全局配置、审核影院、管理所有数据 |
| CINEMA | 影院管理员 | 管理本影院影厅、排片、订单 |
| USER | 普通用户 | 浏览影片、购票、评价 |

### 核心功能

- **影片管理** — 影片 CRUD、分类/地区关联、演员关联、预告片上传
- **影院管理** — 影院注册审核、信息维护、影厅管理
- **排片管理** — 创建放映场次（关联影厅、时间、价格）
- **在线选座** — 可视化座位图、选定下单
- **订单系统** — 购票下单、订单状态流转
- **评价系统** — 用户对影片评分评价
- **排行榜** — 票房榜、评分榜（SQL 级排序）
- **搜索筛选** — 按影片名称、类型、年份、地区多维筛选
- **文件上传** — 图片/视频上传，支持本地存储

---

## API 概览

### 通用 CRUD 接口（每个资源模块，继承 `BaseController<T>`）

| 路径 | 方法 | 说明 |
|------|------|------|
| `/api/v1/{resources}` | GET | 查询全部（支持筛选） |
| `/api/v1/{resources}/{id}` | GET | 按 ID 查询 |
| `/api/v1/{resources}/page` | GET | 分页查询 |
| `/api/v1/{resources}` | POST | 新增 |
| `/api/v1/{resources}` | PUT | 更新 |
| `/api/v1/{resources}/{id}` | DELETE | 删除 |
| `/api/v1/{resources}/batch` | DELETE | 批量删除 |

> `resources` 取值：`admins`、`users`、`cinemas`、`films`、`actors`、`areas`、`types`、`notices`、`rooms`、`records`、`orders`、`marks`、`videos`

### 认证与公共接口

| 路径 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/api/v1/auth/login` | POST | 用户登录（三端共用） | 否 |
| `/api/v1/auth/register` | POST | 用户/影院注册 | 否 |
| `/api/v1/auth/password` | PUT | 修改密码 | Bearer |
| `/api/v1/auth/years` | GET | 获取年份列表 | 否 |

### 业务接口

| 路径 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/api/v1/films/box-office/top` | GET | 票房排行榜 Top10 | 否 |
| `/api/v1/films/mark/top` | GET | 评分排行榜 Top5 | 否 |
| `/api/v1/films/search` | GET | 按标题搜索 | 否 |
| `/api/v1/films/by-cinema` | GET | 按影院查询电影 | Bearer |
| `/api/v1/cinemas/page` | GET | 影院分页（支持按电影筛选） | 否 |
| `/api/v1/files/upload` | POST | 文件上传 | Bearer |

统一响应格式：

```json
{
  "code": "200",
  "msg": "请求成功",
  "data": { ... }
}
```

---

## 数据库设计

系统共 15 张核心表：

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `admin` | 系统管理员 | username, password, name, role |
| `user` | 普通用户 | username, password, name, phone |
| `cinema` | 影院 | name, address, phone, status |
| `film` | 电影 | title, content, score, boxOffice（多对多关联 type） |
| `film_type` | 电影-类型关联（多对多） | film_id, type_id |
| `actor` | 演职人员 | actor, title, figure, grade |
| `area` | 地区 | title |
| `type` | 电影分类 | title |
| `notice` | 系统公告 | title, content, time |
| `room` | 影厅 | name, cinema_id, seat_data |
| `record` | 放映记录 | film_id, cinema_id, room_id, time, price |
| `ordered` | 购票订单 | record_id, user_id, seats, total |
| `mark` | 用户评价 | film_id, user_id, content, score |
| `video` | 预告片 | film_id, url, title |
| `cinema_film` | 影院-影片关联 | cinema_id, film_id |

> 密码字段统一使用 BCrypt 加密存储（兼容旧版明文密码迁移）。

---

## 安全机制

- **密码加密** — BCrypt 哈希存储，`add()` 自动加密，`login()` 通过 `matches()` 验证（兼容 `data.sql` 明文密码迁移）
- **JWT 令牌** — 基于 JJWT 的 Bearer Token 认证，24 小时过期，密钥可环境变量配置
- **认证拦截** — `AuthInterceptor` 拦截除公开路径外的所有接口，校验 Token 有效性
- **角色访问控制** — `AuthInterceptor` 对 ADMIN/CINEMA/USER 路径校验对应角色，不匹配返回 403
- **批量赋值防护** — Service 层 `update()` 方法置空 `password`/`role`，防止通过 `@RequestBody` 篡改敏感字段
- **密码序列化防护** — `@JsonProperty(WRITE_ONLY)` 注解阻止密码字段在 API 响应中泄露
- **事务保护** — 所有 Service 类均标注 `@Transactional(rollbackFor = Exception.class)`，确保数据一致性
- **XSS 防护** — 前端使用 Element Plus 内置转义
- **上传限制** — 文件大小限制 50MB，防恶意大文件上传
- **CORS 配置** — 统一跨域处理，预检缓存 1 小时

---

## 部署指南

### 生产构建

```bash
# 后端
cd xm_film/springboot
mvn clean package -DskipTests
java -jar target/springboot-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

# 前端
cd xm_film/vue
npm run build    # 输出到 dist/
```

### 环境变量注入

```bash
# Linux / macOS
export DB_PASSWORD=your_secure_password
export JWT_SECRET=your_long_random_secret

# Windows PowerShell
$env:DB_PASSWORD = "your_secure_password"
$env:JWT_SECRET = "your_long_random_secret"
```

### Nginx 反向代理示例

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    root /path/to/vue/dist;
    index index.html;

    # API 代理
    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:9090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /files/ {
        proxy_pass http://127.0.0.1:9090;
    }
}
```

生产部署完整步骤见 [生产部署说明](docs/deployment/production-deploy.md)。

---

## 测试

### 单元测试（33 用例）

```bash
cd xm_film/springboot
mvn test
```

覆盖 4 个核心 Service（AdminService、UserService、CinemaService、FilmService），包括登录认证、密码加密、注册去重、密码修改、批量赋值防护、排行榜查询、类型关联维护等业务逻辑。

### E2E 测试（59 用例）

项目使用 Playwright 进行全栈自动化扫描测试，覆盖后端 API、前端页面渲染、CRUD 流程、分页、搜索、三端导航、负面场景等。

```bash
cd xm_film/vue
npm install
npx playwright install chromium
node e2e-tests/e2e-scan.spec.mjs
```

> 运行前需确保后端服务运行在 `http://localhost:9090`，前端运行在 `http://localhost:5173`。
> 测试报告输出为 `e2e-tests/e2e-scan-report.html`，截图保存至 `e2e-tests/screenshots/`。

### 本地复现 CI

```bash
cd xm_film/springboot
mvn clean package

cd ../vue
npm install
npm run build
node e2e-tests/e2e-scan.spec.mjs
```

---

## 相关文档

- [变更日志](CHANGELOG.md) — 版本历史与功能变更
- [Bug 修复记录](Bug.md) — 已修复 Bug 的根因分析与解决方案
- [数据库说明](xm_film/sql/README.md) — 数据库表设计与初始化指引
- [产品需求文档](docs/product/README.md) — 用户需求、业务需求、竞品分析、PRD 与优化路线图
- [系统设计文档](docs/design/README.md) — 系统设计说明、架构图、数据库设计、接口安全与部署设计

---

## License

MIT License

## Current Architecture Notes

- Authentication state is centralized in `xm_film/vue/src/utils/authStorage.js`; router guards, Axios token injection, password pages, profile pages, and ticket purchase use the same storage helpers.
- Backend password changes trust the JWT-derived request role instead of the request body role.
- `AuthInterceptor` enforces role boundaries for admin-only resources and write operations on protected resources.
- Database relations now use explicit keys for the main booking path: `room.cinema_id`, `record.film_id`, and `ordered.record_id`; schema/data SQL under `xm_film/sql` and `xm_film/springboot/src/main/resources/db` are kept in sync.
