# 影院购票管理系统 🎬

基于 **Spring Boot 3.3 + Vue 3 + MySQL** 构建的在线电影购票管理平台，支持三端角色分离运营（管理员后台、影院端、用户端），提供完整的影片管理、影厅排片、在线选座购票、订单评价等功能闭环。

---

## 目录

- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [快速启动](#快速启动)
- [配置说明](#配置说明)
- [功能模块](#功能模块)
- [API 概览](#api-概览)
- [数据库设计](#数据库设计)
- [安全机制](#安全机制)
- [部署指南](#部署指南)

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
│       │   ├── config/
│       │   │   ├── AuthInterceptor.java   # JWT 认证拦截器
│       │   │   └── WebMvcConfig.java      # Web MVC 配置
│       │   ├── CorsConfig.java        # CORS 跨域配置
│       │   ├── FileUtil.java          # 文件上传工具类
│       │   ├── JwtUtils.java          # JWT 令牌工具
│       │   └── Result.java            # 统一响应封装
│       ├── controller/                # 控制器层
│       │   ├── WebController.java     # 登录/注册/密码修改
│       │   ├── AdminController.java   # 管理员管理
│       │   ├── UserController.java    # 用户管理
│       │   ├── FilmController.java    # 电影管理
│       │   ├── CinemaController.java  # 影院管理
│       │   ├── ActorController.java   # 演职人员管理
│       │   ├── AreaController.java    # 区域管理
│       │   ├── TypeController.java    # 分类管理
│       │   ├── NoticeController.java  # 公告管理
│       │   ├── OrderedController.java # 订单管理
│       │   ├── RecordController.java  # 放映记录管理
│       │   ├── RoomController.java    # 影厅管理
│       │   ├── MarkController.java    # 评价管理
│       │   └── VideoController.java   # 预告片管理
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
│       ├── router/index.js            # 路由配置
│       ├── utils/request.js           # Axios 封装
│       └── assets/                    # 静态资源
```

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

执行项目提供的 `xm_film/sql/init.sql` 一键初始化脚本（或依次执行 `schema.sql` + `data.sql`）。

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

前端默认启动在 `http://localhost:5173`。

### 4. 默认账号

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| ADMIN | admin | 123456 | 系统管理员 |
| CINEMA | (注册) | cinema123 | 影院管理员 |
| USER | (注册) | user123 | 普通用户 |

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

> 生产环境建议通过环境变量注入数据库密码与 JWT Secret。

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

| 路径 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/login` | POST | 用户登录 | - |
| `/register` | POST | 用户注册 | - |
| `/updatePassword` | PUT | 修改密码 | Bearer |
| `/getYear` | GET | 获取年份列表 | - |
| `/api/admin/**` | * | 管理员 CRUD | Bearer |
| `/api/user/**` | * | 用户 CRUD | Bearer |
| `/api/film/**` | * | 影片 CRUD | Bearer |
| `/api/cinema/**` | * | 影院 CRUD | Bearer |
| `/api/ordered/**` | * | 订单 CRUD | Bearer |
| `/api/record/**` | * | 排片 CRUD | Bearer |
| `/api/mark/**` | * | 评价 CRUD | Bearer |

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

系统共 14 张核心表：

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `admin` | 系统管理员 | username, password, name, role |
| `user` | 普通用户 | username, password, name, phone |
| `cinema` | 影院 | name, address, phone, status |
| `film` | 电影 | title, content, score, boxOffice, type_ids |
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

- **密码加密** — BCrypt 哈希存储，`add()` 自动加密，`login()` 通过 `matches()` 验证
- **JWT 令牌** — 登录成功返回 Bearer Token，前端存储在 localStorage 并在每次请求时携带
- **认证拦截** — `AuthInterceptor` 拦截除登录/注册/文件外的所有接口，校验 Token 有效性
- **XSS 防护** — 前端使用 Element Plus 内置转义
- **上传限制** — 文件大小限制 50MB，防恶意大文件上传
- **CORS 配置** — 统一跨域处理，预检缓存 1 小时

---

## 部署指南

### 生产构建

```bash
# 后端
cd xm_film/springboot
mvn clean package -DskipTests -Pprod

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

---

## 开发说明

```bash
# 后端热部署（DevTools 已集成）
# 修改代码后自动重启

# 前端开发模式
cd xm_film/vue
npm run dev     # HMR 热更新
```

> 前端请求已配置代理到 `http://localhost:9090`，开发时需确保后端服务运行。

---

## License

MIT License
