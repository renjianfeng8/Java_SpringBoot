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
├── CLAUDE.md                          # 项目文档（本文件）
├── README.md                          # 项目说明
├── CHANGELOG.md                       # 变更日志
├── Bug.md                             # Bug 修复记录（修复前先查阅）
├── xm_film/                           # 项目主目录
│   ├── springboot/                    # 后端（Spring Boot）
│   │   ├── pom.xml                    # Maven 依赖配置
│   │   └── src/main/
│   │       ├── java/com/example/springboot/
│   │       │   ├── SpringbootApplication.java  # 启动入口
│   │       │   ├── common/                     # 公共组件
│   │       │   │   ├── CorsConfig.java         # CORS 跨域
│   │       │   │   ├── FileUtil.java           # 文件上传工具
│   │       │   │   ├── JwtUtils.java           # JWT 令牌工具
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
│   ├── package.json
│   └── package-lock.json
├── 数据库/                              # 数据库初始化脚本（14个表）
└── start_claude.bat                    # Claude 启动脚本
```

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
- **文件上传** — 图片/视频上传，支持本地存储

## API 接口清单

### 公共接口
| 路径 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/login` | POST | 用户登录 | 否 |
| `/register` | POST | 用户注册 | 否 |
| `/updatePassword` | PUT | 修改密码 | Bearer |
| `/getYear` | GET | 获取年份列表 | 否 |

### 管理端接口 (`/admin/**`, `/user/**`, `/cinema/**`, `/film/**` 等)
每个资源模块统一提供以下 RESTful 接口：
| 路径 | 方法 | 说明 |
|------|------|------|
| `/selectAll` | GET | 查询全部（支持筛选） |
| `/selectById/{id}` | GET | 按 ID 查询 |
| `/selectList` | GET | 查询列表（精简版） |
| `/selectPage` | GET | 分页查询 |
| `/add` | POST | 新增 |
| `/update` | PUT | 更新 |
| `/delete/{id}` | DELETE | 删除 |
| `/deleteBatch` | DELETE | 批量删除 |
| `/upload` | POST | 文件上传 |

### 业务接口
| 路径 | 方法 | 说明 |
|------|------|------|
| `/film/getAllBoxOfficeTop` | GET | 票房排行榜 |
| `/film/getAllMarkTop` | GET | 评分排行榜 |
| `/film/selectByTitle` | GET | 按标题搜索 |
| `/film/selectByCinema` | GET | 按影院查询电影 |
| `/cinema/selectPage` | GET | 影院分页（支持按电影筛选） |

## 页面清单

### 管理后台 (`/manage/*`) — 16个页面
home, admin, user, cinema, type, area, film, actor, notice, room, record, ordered, mark, video, person, password

### 影院后台 (`/back/*`) — 7个页面
home, film, room, record, ordered, person, password

### 用户前台 (`/front/*`) — 12个页面
home, movie, filmDetail/:id, cinema, cinemaDetail/:id, filmCinema/:id, buyTicket, orders, rank, search, person, password

## Playwright E2E 验证说明

### 安装与运行
```bash
cd xm_film/vue
npm install
npx playwright install chromium
npx playwright test e2e-tests/e2e-scan.spec.mjs --config=e2e-tests/playwright.config.mjs
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

### 生成 HTML 报告
```bash
npx playwright show-report xm_film/vue/e2e-tests/playwright-report
```

## 快速启动命令

### 环境要求
- JDK 17+、Maven 3.6+、MySQL 8.0+、Node.js 18+、npm 9+

### 1. 初始化数据库
```sql
CREATE DATABASE `xm-film` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
按顺序执行 `数据库/` 目录下的 SQL 脚本。

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
npx playwright test e2e-tests/e2e-scan.spec.mjs --config=e2e-tests/playwright.config.mjs
```

### 默认账号
| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| ADMIN | 999 | 999 | 系统管理员 |
| USER | zhangsan | user123 | 普通用户 |

## 配置说明

后端配置位于 `xm_film/springboot/src/main/resources/application.yml`：
- 服务端口：9090
- 数据库：`jdbc:mysql://localhost:3306/xm-film`
- JWT 密钥：`xm-film-secret-key-2024-springboot-vue3-jwt-auth`
- JWT 过期：24 小时（86400000ms）
- 文件上传：`D:/project/picture`
- 文件大小限制：50MB

## 项目优化建议

1. **密码安全性**：`application.yml` 中的数据库密码硬编码，建议通过环境变量注入
2. **JWT 密钥**：生产环境应替换为更复杂的随机密钥
3. **文件存储**：当前为本地存储，建议生产环境迁移至 OSS（阿里云/S3）
4. **日志配置**：MyBatis 日志当前使用 `StdOutImpl`，生产环境应改为 SLF4J
5. **API 文档**：建议集成 Swagger/SpringDoc OpenAPI 自动生成接口文档
6. **单元测试**：后端仅依赖测试（spring-boot-starter-test），缺少业务单元测试
7. **前端构建**：生产构建后建议接入 CDN 分发静态资源
8. **CI/CD**：已配置 GitHub Actions，可按需集成自动化部署
9. **错误边界**：前端可引入 Vue ErrorBoundary 机制处理渲染异常
10. **权限校验**：前端路由守卫当前仅设置页面标题，建议添加强角色权限控制

## 相关文档

- [Bug 修复记录](Bug.md) — 已修复 Bug 的根因与解决方案，遇到相似问题优先查阅

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
28646785 feat: 全栈自动化工程化构建 — CI/CLAUDE.md/E2E测试/启动脚本

- 新增 CLAUDE.md 完整项目文档
- 新增 GitHub Actions CI 配置
- 新增 Playwright 全量 E2E 测试（53 用例，100% 通过）
- 新增 start-dev.bat / run-e2e-tests.bat 一键启动脚本
- 新增 scan-project.sh 全栈项目扫描脚本
