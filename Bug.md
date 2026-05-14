# Bug 修复记录

> 记录项目中已修复的 Bug，避免重复踩坑。
> 遇到相似问题时，优先查阅本文档。

## 提交规范

每条 Bug 记录包含：
- **Bug 描述**: 问题现象
- **根因分析**: 为什么会发生
- **解决方案**: 如何修复
- **相关文件**: 涉及的文件路径
- **提交记录**: 对应的 Git commit

---

## 已修复 Bug

### BUG-001: CustomException.getMessage() 返回 null

- **日期**: 2026-05-14
- **Bug 描述**: `GlobalExceptionHandler` 捕获 `CustomException` 后，调用 `e.getMessage()` 始终返回 `null`，导致前端收到的错误消息为空
- **根因分析**: `CustomException` 继承 `RuntimeException`，构造函数只设置了自定义字段 `code` 和 `msg`，但未调用 `super(msg)`，导致父类的 `getMessage()` 返回 `null`；全局异常处理使用的正是 `e.getMessage()` 而非 `e.getMsg()`
- **解决方案**: 构造函数中追加 `super(msg)`，或将全局异常处理器改为调用 `e.getMsg()`
- **相关文件**: `xm_film/springboot/src/main/java/com/example/springboot/exception/CustomException.java`、`GlobalExceptionHandler.java`
- **解决方案**: `CustomException` 构造函数中追加 `super(msg)`；`GlobalExceptionHandler` 中改用 `e.getMsg()` 替代 `e.getMessage()`
- **相关文件**: `xm_film/springboot/src/main/java/com/example/springboot/exception/CustomException.java`、`GlobalExceptionHandler.java`
- **提交记录**: `（待提交）`
- **状态**: 已修复

---

### BUG-002: 默认管理员账号 '999' 未初始化到数据库

- **日期**: 2026-05-14
- **Bug 描述**: `POST /login` 返回 500，提示"账号不存在"；Playwright 测试中管理员登录流程失败
- **根因分析**: `admin.sql` 脚本未执行，`admin` 表中没有 username='999' 的记录；`AdminService.login()` 查询返回 `null` 后抛出异常
- **解决方案**: 手动执行 `INSERT INTO admin (username, password, role, name) VALUES ('999', '999', 'ADMIN', '任建峰')`；长期方案在项目初始化文档中强调 SQL 导入步骤
- **相关文件**: `数据库/admin.sql`、`xm_film/springboot/src/main/java/com/example/springboot/service/AdminService.java`
- **状态**: 已修复

---

### BUG-003: Playwright E2E 测试中变量类型错误导致搜索失败

- **日期**: 2026-05-14
- **Bug 描述**: 前台首页搜索测试报错 `searchInput.fill is not a function`
- **根因分析**: 测试代码中 `const searchInput = await page.isVisible(...)` 返回的是 `boolean` 类型，后续对该布尔值调用了 `.fill()` 方法，而 `.fill()` 是 Playwright Locator 的方法
- **解决方案**: 将 `isVisible` 检查与真实 Locator 变量分离：`const searchInputLocator = page.locator(...)`，再用单独的变量存储 `isVisible` 检查结果
- **相关文件**: `xm_film/vue/e2e-tests/e2e-scan.spec.mjs`
- **提交记录**: `28646785`
- **状态**: 已修复

---

### BUG-004: HTTP 代理环境变量干扰本地 API 请求

- **日期**: 2026-05-14
- **Bug 描述**: 从 Bash 调用 `curl http://localhost:9090/getYear` 返回 502 Bad Gateway，但后端实际运行正常
- **根因分析**: 系统设置了 `http_proxy=http://127.0.0.1:7890` 环境变量，curl 将本地请求也发往代理服务器，代理无法连接本地后端
- **解决方案**: 使用 `curl --noproxy '*'` 绕过代理，或在测试脚本开头执行 `unset http_proxy && unset https_proxy`
- **相关文件**: `start_claude.bat`
- **状态**: 已修复（运行测试时前置 `unset http_proxy`）

---

### BUG-005: 未认证请求返回 401 而非明确错误信息

- **日期**: 2026-05-14
- **Bug 描述**: 调用需要 JWT 认证的 API（如 `/film/selectAll`）时直接返回 401 且无错误信息，排查问题时不易定位
- **根因分析**: `AuthInterceptor` 在校验失败后设置 401 状态码并返回固定 JSON，但缺乏具体的角色/权限提示；`GlobalExceptionHandler` 不处理拦截器层的异常
- **解决方案**: 仅在 `AuthInterceptor` 的响应 JSON 中添加文字提示即可（当前已有）：`{"code":"401","msg":"登录已过期，请重新登录"}`
- **相关文件**: `xm_film/springboot/src/main/java/com/example/springboot/common/config/AuthInterceptor.java`
- **状态**: 设计如此，无需修改

---

## Bug 预防清单

1. **数据库初始化**: 新环境部署时务必执行 `数据库/` 目录下的所有 SQL 脚本
2. **代理环境变量**: 本地开发测试时注意 `http_proxy`/`https_proxy` 是否会影响 `localhost` 请求
3. **Playwright 变量类型**: `isVisible()` 返回 `boolean`，`locator()` 返回 `Locator`，不可混用
4. **异常日志**: `RuntimeException` 子类构造函数需调用 `super(message)` 以确保 `getMessage()` 可用
5. **JWT Token**: 所有需认证的后端 API 测试务必先获取 token 并传入请求头
