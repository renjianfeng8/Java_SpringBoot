# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.5.0] - 2026-05-16

### Fixed

- **全栈批量 Bug 修复** (BUG-003): 修复 35 个运行时/逻辑 Bug
  - 后端 11 个 Service 的 `selectList()` 返回 `null` → 改为调用 `mapper.selectAll(entity)`
  - Admin/User/Cinema 实体所有子类添加 `@JsonProperty(WRITE_ONLY)` 防止密码序列化泄露
  - Admin/User/Cinema Service 添加 `@Transactional` 事务支持
  - CinemaController 添加 `@RequestParam(required = false)` 注解
  - Vue 页面修复 ElMessage 导入、JSON.parse 安全包装、emit 参数、路由路径等

- **后端安全加固** (BUG-004): RBAC/密码保护/批量赋值防护
  - AuthInterceptor 添加 `/admin/**` 路径的 ADMIN 角色校验（403 拒绝非管理员）
  - Service 层 `update()` 方法置空 password/role，防止批量赋值修改敏感字段
  - `WebController.updatePassword()` 改为从 JWT 请求属性读取 userId

- **前端 14 处 Bug 修复** (BUG-005)
  - `front/Home.vue` — `.toFixed()` 返回 string 导致类型混淆，`parseFloat()` 包裹
  - `Front.vue` — router-link 相对路径 → 绝对路径 `/front/home`
  - `manage/Cinema.vue` — 影院状态映射反向逻辑修复
  - `back/Room.vue` — `el-form-item prop` 与 `v-model` 不匹配修复
  - `Front/Back/Manage.vue` — `https://your-domain.com` 占位域名替换
  - `front/Movie.vue`(4处)、`front/Home.vue` — API 路径缺少前导 `/` 修复
  - `front/BuyTicket.vue` — 移除无依赖的死 `watchEffect`
  - `back/Ordered.vue` — initLoad 竞态条件，`await Promise.all()` 修复
  - `front/FilmDetail.vue`、`front/FilmCinema.vue` — `JSON.parse` try-catch 保护

### Changed

- **代码质量优化** (BUG-006)
  - 命名规范：13 个 Controller 中 `selectByID` → `selectById`
  - Javadoc：修复 11 个 Controller 中拷贝粘贴的类级/方法级文档（"管理员"→ 正确实体名）
  - 事务：为 10 个 Service 添加 `@Transactional(rollbackFor = Exception.class)`
  - 日志：AuthInterceptor 空 catch → SLF4J `log.warn`
  - 日志：9 个 Controller 的 `System.out.println` / `e.printStackTrace` → SLF4J `log.info` / `log.error`
  - 删除 2 个 Service 中未使用的 `@Resource TypeService` 注入
  - 删除 17 个 Vue 文件中 30+ 条 `console.log()` 调试语句

### Added

- **环境配置**: 创建 `vue/.env`，定义 `VITE_API_BASE_URL` 环境变量
- **预防清单**: Bug.md 补充 3 条预防项（密码明文兼容、.toFixed 类型、API 路径前导斜杠）

### Removed

- 删除 11 处硬编码 `http://localhost:9090`（统一通过 `VITE_API_BASE_URL` 配置）
- 删除 FilmMapper.xml、CinemaMapper.xml 中过时/误导性注释

## [0.4.0] - 2026-05-15

### Security

- AuthInterceptor 添加角色访问控制（`/admin/**` → ADMIN）
- `@JsonProperty(WRITE_ONLY)` 添加到所有子类实体，防止密码泄露
- Service 层 `update()` 置空 password/role，防止批量赋值篡改

### Fixed

- 11 个 Service 的 `selectList()` 返回 `null` → 改为 `mapper.selectAll(entity)`
- CinemaController 添加 `@RequestParam(required = false)` 参数注解
- 前端 Vue 页面修复：ElMessage 导入缺失、JSON.parse 未捕获、emit 参数丢失等
- 登录密码兼容：保持 BCrypt 明文回退路径（兼容 data.sql 明文密码）

## [0.3.0] - 2026-05-14

### Changed

- **数据库脚本重构**: 移除 `数据库/` 中文目录（14 个 INSERT-only 文件），新建 `xm_film/sql/` 英文目录
  - `schema.sql` — 新增完整 CREATE TABLE 定义（字段类型、注释、默认值）
  - `data.sql` — 所有初始数据合并为单文件，按表分区管理
  - `init.sql` — 一键初始化入口（建库 → 建表 → 导数据）
  - `application.yml` — 新增 `spring.sql.init` 配置，支持自动初始化
  - `CLAUDE.md`、`README.md`、`Bug.md` — 同步更新文档引用

## [0.2.0] - 2026-05-09

### Added

- **JWT 认证系统**
  - `JwtUtils.java` — 基于 JJWT 的令牌生成与解析工具
  - `AuthInterceptor.java` — Bearer Token 验证拦截器，排除 `/login`、`/register`、`/files/**`、`/getYear`
  - `WebController.login()` 返回 JWT Token，前端 `request.js` 自动从 localStorage 读取并注入 `Authorization` 头
- **BCrypt 密码加密**
  - 新增 `spring-security-crypto` 依赖
  - `AdminService`、`UserService`、`CinemaService` 的 `add()` 方法自动 BCrypt 编码密码
  - `login()` 和 `updatePassword()` 使用 `passwordEncoder.matches()` 验证，兼容旧版明文密码迁移
- **FileUtil 工具类**
  - 抽取 `FileUtil.uploadFile()` 静态方法，消除 9 个 Service 中的重复代码（~360 行）
- **Playwright E2E 自动化测试**
  - 新增 `e2e-tests/test.mjs` 测试套件（32 项用例，93.8% 通过率）
  - 覆盖：后端健康检查、页面渲染、登录流程、16 个页面导航、分类 CRUD、管理员 CRUD、搜索功能
  - 自动截图（19 张）和 HTML 测试报告生成
- **README.md** — 完整项目文档（技术栈、项目结构、快速启动、API、部署指南）
- **CHANGELOG.md** — 项目变更日志

### Fixed

- **ActorMapper.xml** — `updateById` 中 `grade = #{video}` 修正为 `video = #{video}`，防止更新视频时破坏 grade 字段
- **NoticeMapper.xml** — `updateById` 中 `title = #{content}` 和 `title = #{time}` 分别修正为 `content = #{content}`、`time = #{time}`
- **TypeService.java** — `selectByIds()` 方法补充缺失的 `import java.util.ArrayList`，修复编译错误
- **WebController.login()** — 增加 `result == null` 空值检查，返回明确错误信息而非 `Result.success(null)`
- **CorsConfig.java** — 增加 `setMaxAge(3600L)` 预检缓存配置

### Changed

- **安全增强**
  - CORS 配置添加 `setMaxAge(3600L)`
  - 数据库密码配置增加生产环境安全注释
  - 上传文件大小限制从 1000MB 降低至 50MB
- **性能优化**
  - `FilmService.selectPage()` — N+1 类型查询优化为单次 `WHERE id IN (...)` 批量查询
  - `FilmService.getBoxOfficeTop()` / `getMarkTop()` — 应用层全量排序替换为 SQL 级 `ORDER BY ... DESC LIMIT`，消除全表加载
  - 新增 `FilmMapper.xml` 专用排名查询：`selectBoxOfficeTop`、`selectMarkTop`
  - 新增 `TypeMapper.selectByIds()` 批量查询接口
- **代码重构**
  - `AdminService`、`CinemaService` — 移除未使用的 `com.fasterxml.jackson.databind.util.BeanUtil` 导入
  - `UserService` — 移除重复的 `com.example.springboot.entity.User` 导入
  - 全部 9 个 Service 的 `uploadFile()` / `getFileExtension()` 委托至 `FileUtil` 工具类
- **配置文件**
  - `application.yml` 新增 `jwt.secret` 和 `jwt.expire` 配置项
  - `Account.java` 新增 `token` 字段

### Removed

- 移除 `UserService.java` 中重复的实体类导入
- 移除 `CinemaService.java` 中未使用的 `BeanUtil` 导入

## [0.1.0] - 2026-05-09

### Added

- 项目初始化：Spring Boot 3.3.13 + Vue 3 + MySQL 影院购票管理系统
- 三端角色：系统管理员（ADMIN）、影院管理员（CINEMA）、普通用户（USER）
- 14 张数据表：admin、user、cinema、film、actor、area、type、notice、room、record、ordered、mark、video、cinema_film
- 后端模块：14 个 Controller、13 个 Service、14 个 Mapper、14 个实体
- 前端模块：42 个 Vue 页面、Element Plus UI 组件、Vue Router 路由
- 核心功能：影片管理、影院管理、影厅排片、在线选座购票、订单评价、排行榜
