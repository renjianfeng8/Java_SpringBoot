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
- **提交记录**: `045545e4`
- **状态**: 已修复

---

### BUG-002: 默认管理员账号 '999' 未初始化到数据库

- **日期**: 2026-05-14
- **Bug 描述**: `POST /login` 返回 500，提示"账号不存在"；Playwright 测试中管理员登录流程失败
- **根因分析**: `admin.sql` 脚本未执行，`admin` 表中没有 username='999' 的记录；`AdminService.login()` 查询返回 `null` 后抛出异常
- **解决方案**: 手动执行 `INSERT INTO admin (username, password, role, name) VALUES ('999', '999', 'ADMIN', '任建峰')`；长期方案在项目初始化文档中强调 SQL 导入步骤
- **相关文件**: `xm_film/sql/data.sql`、`xm_film/springboot/src/main/java/com/example/springboot/service/AdminService.java`
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

### BUG-006: 数据库脚本目录结构不规范（schema 与数据混放）

- **日期**: 2026-05-14
- **Bug 描述**: `数据库/` 目录下的 14 个 SQL 文件仅含 INSERT 语句，无 CREATE TABLE 建表语句；目录名为中文，与项目其他英文命名不统一；新环境部署需逐个手动执行，缺少一键初始化入口
- **根因分析**: 项目初期从数据库工具导出时仅导出 INSERT 语句，未包含表结构定义；中文目录名在跨平台/CI 中存在路径编码风险
- **解决方案**:
  - 移除 `数据库/` 目录，新建 `xm_film/sql/` 英文目录
  - 新增 `schema.sql`：14 张表的完整 CREATE TABLE（含字段类型、注释、默认值）
  - 合并数据为 `data.sql`：所有初始数据按表分区、统一管理
  - 新增 `init.sql`：一键初始化入口（建库 → 建表 → 导数据）
  - 在 `src/main/resources/db/` 下放置副本，支持 `spring.sql.init` 自动初始化
- **相关文件**:
  - `xm_film/sql/schema.sql`、`xm_film/sql/data.sql`、`xm_film/sql/init.sql`
  - `xm_film/springboot/src/main/resources/db/schema.sql`、`db/data.sql`
  - `xm_film/springboot/src/main/resources/application.yml`
  - `CLAUDE.md`
- **提交记录**: `9525efa4`
- **状态**: 已修复

---

### BUG-007: 全栈批量修复 — NPE/崩溃/数据丢失/竞态条件 (BUG-003)

- **日期**: 2026-05-15
- **Bug 描述**: 全栈扫描发现约 35 个 Bug，涵盖 NPE、崩溃、数据丢失、竞态条件等严重问题
- **根因分析**: 后端 Service 中 `selectList()` 返回 `null`（前端调用无数据）；`AdminService/UserService/CinemaService` 缺少 `@Transactional`；`OrderedService.update()` 未置空 `status`；`CinemaController.selectPage` 未标注 `@RequestParam` 导致参数必填；`Account.java` 缺少 `@JsonProperty(WRITE_ONLY)` 导致密码序列化泄露；前端口令修改/个人资料页面缺少 `ElMessage` 导入、`localStorage` 解析未做 try-catch、路由路径错误等
- **解决方案**:
  - 后端 11 个 Service 的 `selectList()` 改为调用 `mapper.selectAll(entity)`
  - Admin/User/Cinema Service 添加 `@Transactional(rollbackFor = Exception.class)`
  - Account/Admin/User/Cinema 实体添加 `@JsonProperty(access = WRITE_ONLY)`
  - CinemaController 添加 `@RequestParam(required = false)` 注解
  - OrderedService.update() 置空 status 防止意外更新
  - 前端口令修改/个人资料/404 页面修复 ElMessage 导入、JSON.parse 安全包装、emit 修复
  - Login.vue 补充 ElMessageBox 导入
- **相关文件**: 涉及 30+ 文件（后端 11 个 Service、4 个 Entity、3 个 Controller；前端 7 个 Vue 页面）
- **提交记录**: `3ba277f4`
- **状态**: 已修复

---

### BUG-008: 后端安全加固 — RBAC/密码保护/批量赋值/事务 (BUG-004)

- **日期**: 2026-05-15
- **Bug 描述**: 后端 API 缺少角色访问控制、密码通过 API 响应泄露、缺少批量赋值防护、部分操作无事务保护
- **根因分析**: AuthInterceptor 仅验证 JWT 有效性，未做基于路径的角色校验；`@JsonProperty(WRITE_ONLY)` 仅在 Account 基类有效，子类（Admin/User/Cinema）重新声明 password 字段，覆盖了注解；`AdminService.update()` 允许通过 `@RequestBody` 更新 password；10 个 Service 无 `@Transactional`
- **解决方案**:
  - AuthInterceptor 添加 `/admin/**` 路径的 ADMIN 角色校验（403 拒绝非管理员）
  - 为 Admin/User/Cinema 实体类所有子类的 password 字段添加 `@JsonProperty(WRITE_ONLY)`
  - Service 层 `update()` 方法中置空 password/role，防止通过更新接口修改
  - WebController.updatePassword() 改为从 JWT 请求属性读取 userId，而非请求体传入
  - 为 10 个 Service 添加 `@Transactional(rollbackFor = Exception.class)`
- **相关文件**:
  - `AuthInterceptor.java`、`WebMvcConfig.java`
  - `Account.java`、`Admin.java`、`User.java`、`Cinema.java`
  - `AdminService.java`、`UserService.java`、`CinemaService.java`、`OrderedService.java`
  - `WebController.java`
- **提交记录**: `abeedd04`
- **状态**: 已修复

---

### BUG-009: 前端 14 处 Bug — 类型转换/路由/路径/竞态条件/JSON 解析 (BUG-005)

- **日期**: 2026-05-16
- **Bug 描述**: 前端代码扫描发现 14 个运行时/逻辑 Bug
- **根因分析**:
  - `front/Home.vue:294` — `.toFixed()` 返回 string 而非 number，导致后续数值运算类型混淆
  - `Front.vue:13` — `<router-link to="home">` 使用相对路径，路由匹配失败
  - `manage/Cinema.vue:339` — 影院状态映射反向：`已审批 → 未审核`
  - `back/Room.vue:50` — `el-form-item prop="title"` 与 `v-model="data.form.name"` 不匹配，表单验证失效
  - `Front/Back/Manage.vue` — 头像地址使用 `https://your-domain.com` 占位域名
  - `front/Movie.vue:124,143,153,163` — API 路径缺少前导 `/`
  - `front/BuyTicket.vue:253` — `watchEffect` 无响应式依赖，等价于普通函数调用
  - `back/Ordered.vue:285` — initLoad 未 await load* 函数，产生竞态条件
  - `front/FilmDetail.vue:326`、`FilmCinema.vue:259` — `JSON.parse()` 无 try-catch 保护
- **解决方案**: 逐一修复上述 14 个问题（parseFloat 包裹、绝对路由、修复映射、修正 prop、替换域名、补前导斜杠、移除死代码、Promise.all 等待、JSON.parse try-catch）
- **相关文件**: 13 个 Vue 文件
- **提交记录**: `4c5e916c`
- **状态**: 已修复

---

### BUG-010: 代码质量优化 — 命名/Javadoc/事务/日志/环境配置 (BUG-006)

- **日期**: 2026-05-16
- **Bug 描述**: 代码审计发现大量拷贝粘贴 Javadoc、命名不一致、调试输出残留、硬编码地址、空 catch 块等可维护性问题
- **根因分析**:
  - 13 个 Controller 中 `selectByID()` 违反 Java camelCase 规范（应为 `selectById`）
  - 11 个 Controller 类级 Javadoc 拷贝自 AdminController："管理员管理API控制器" — 即使管理的是电影/类型/演员
  - AuthInterceptor 中 `catch (Exception ignored) {}` 静默吞掉 JWT 解析异常
  - 9 个 Controller 的 upload 方法使用 `System.out.println` / `e.printStackTrace()`（无结构化日志）
  - 17 个 Vue 文件残留 30+ 条 `console.log()` 调试语句
  - 11 处硬编码 `http://localhost:9090`（切换后端地址需修改多处）
  - 2 个 Service 注入未使用的 `TypeService`
- **解决方案**:
  - 所有 Controller 方法重命名 `selectByID` → `selectById`
  - 修复 11 个 Controller 的 Javadoc（"管理员"→ 正确实体名）
  - AuthInterceptor 空 catch 改为 `log.warn`
  - 9 个 Controller 添加 SLF4J Logger，替换 `System.out` / `e.printStackTrace`
  - 创建 `.env` + `VITE_API_BASE_URL`，更新 11 处引用
  - 删除 30+ 条 `console.log()` 和 2 个未使用的 `@Resource`
  - 为 10 个 Service 补充 `@Transactional`
- **相关文件**:
  - 13 个 Controller、10 个 Service、AuthInterceptor
  - `vue/.env`、`request.js`、`Front/Back/Manage.vue` + 6 个 manage 视图
  - 17 个 Vue 视图文件（console.log 删除）
  - `FilmMapper.xml`、`CinemaMapper.xml`
- **提交记录**: `0df50934`
- **状态**: 已修复

---

---

### BUG-011: API 路径前后端不匹配 — box-office/mark-top dash/slash 不一致 & Type.vue crud 引用失效

- **日期**: 2026-05-29
- **Bug 描述**: E2E 全栈扫描 54 用例中 10 项失败：(a) 票房/评分排行榜 API 返回 404 — 后端 `/box-office-top` 与前端调用 `/box-office/top` 路径不匹配；(b) 分类管理表格显示 0 行 — `Type.vue` 中 `useFormDialog(crud)` 的 `crud` 为 `undefined`；(c) 影院后台 7 页面访问被拒 — ADMIN 角色无法访问 CINEMA 路由
- **根因分析**:
  - (a) 重构 Phase 3 中后端 FilmController 路径为 `/box-office-top`（dash），但前端 Home.vue/Rank.vue 和 E2E 测试调用 `/box-office/top`（slash）
  - (b) Type.vue 将 `useCrud()` 返回值直接解构（`const { dataList, ... } = useCrud()`），未保存为变量，导致 `useFormDialog(crud, ...)` 传入 `undefined`
  - (c) 路由守卫 `meta: { roles: ['CINEMA'] }` 正确拦截 ADMIN，但 E2E 测试未切换影院用户
- **解决方案**:
  - (a) FilmController: `@GetMapping("/box-office-top")` → `@GetMapping("/box-office/top")`；`@GetMapping("/mark-top")` → `@GetMapping("/mark/top")`
  - (b) Type.vue: 改为 `const crud = useCrud(API_PATHS.TYPES)` → 解构 `crud` → `useFormDialog(crud, ...)`
  - (c) E2E 测试: 新增 CINEMA 登录（`asks`/`cinema123`），登录后再测试影院后台页面
- **相关文件**:
  - `xm_film/springboot/src/main/java/com/example/springboot/controller/FilmController.java`
  - `xm_film/vue/src/views/manage/Type.vue`
  - `xm_film/vue/e2e-tests/e2e-scan.spec.mjs`
- **提交记录**: 待提交
- **状态**: 已修复（E2E 54/54 100% 通过）

1. **数据库初始化**: 新环境部署时务必执行 `xm_film/sql/init.sql`（或依次执行 `schema.sql` + `data.sql`）
2. **代理环境变量**: 本地开发测试时注意 `http_proxy`/`https_proxy` 是否会影响 `localhost` 请求
3. **Playwright 变量类型**: `isVisible()` 返回 `boolean`，`locator()` 返回 `Locator`，不可混用
4. **异常日志**: `RuntimeException` 子类构造函数需调用 `super(message)` 以确保 `getMessage()` 可用
5. **JWT Token**: 所有需认证的后端 API 测试务必先获取 token 并传入请求头
6. **密码明文兼容**: `data.sql` 中使用明文密码时，`login()` / `updatePassword()` 需保留 BCrypt 明文回退逻辑
7. **JS .toFixed() 类型**: `.toFixed()` 返回 `string` 而非 `number`，数值运算需用 `parseFloat()` 包裹
8. **API 路径前导斜杠**: axios GET 请求路径必须以 `/` 开头（如 `'/film/selectAll'`），否则拼接 baseURL 后路径错误
