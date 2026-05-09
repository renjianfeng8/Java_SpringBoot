# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
