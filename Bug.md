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
- **提交记录**: `dc4fb9e7`
- **状态**: 已修复（E2E 54/54 100% 通过）

---

### BUG-012: CI 数据库初始化 init.sql SOURCE 路径使用反斜杠，Linux 不识别

- **日期**: 2026-05-29
- **Bug 描述**: CI 中 `mysql < init.sql` 执行失败，SOURCE 命令找不到 schema.sql/data.sql
- **根因分析**: init.sql 中 SOURCE 路径使用 Windows 风格反斜杠 `.\schema.sql`，Linux runner 不识别，应为 `./schema.sql`
- **解决方案**: init.sql 中将所有 `.\` 替换为 `./`（跨平台兼容写法）
- **相关文件**: `xm_film/sql/init.sql`
- **提交记录**: `389b0bec`
- **状态**: 已修复

---

### BUG-013: data.sql film 表第 26 行数据 VALUES 语法错误

- **日期**: 2026-05-29
- **Bug 描述**: 执行 data.sql 时 film 表第 26 行插入失败，导致初始化不完整
- **根因分析**: film(id=26) 的 VALUES 结尾额外逗号导致语法截断；且 SQL 脚本被多次 SOURCE 执行时主键冲突
- **解决方案**: 修复 VALUES 语法；data.sql 开头加 `TRUNCATE` 清理旧数据（避免重复执行冲突）
- **相关文件**: `xm_film/sql/data.sql`
- **提交记录**: `0bf666fd`、`b199e106`
- **状态**: 已修复

---

### BUG-014: CI backend JAR 路径与 Maven 输出不匹配

- **日期**: 2026-05-29
- **Bug 描述**: CI 中 `java -jar` 指定的路径找不到 JAR 文件，后端启动失败
- **根因分析**: `--spring.profiles.active=ci` 参数后的 JAR 路径使用相对路径，与 Maven 实际输出目录不匹配；`actions/download-artifact` 下载到 `$GITHUB_WORKSPACE` 但路径拼接错误
- **解决方案**: 使用 `$GITHUB_WORKSPACE` 绝对路径引用 JAR 文件
- **相关文件**: `.github/workflows/ci.yml`
- **提交记录**: `fe05f0ae`
- **状态**: 已修复

---

### BUG-015: CI 前端启动方式 — npm run preview 路径不匹配

- **日期**: 2026-05-29
- **Bug 描述**: CI 中前端启动后无法访问，Playwright 无法连接
- **根因分析**: 最初使用 `npm run preview`（读取 dist 目录），但 dist 目录未正确构建或路径不匹配；改为 `npm run dev` 后 Vite 直接启动开发服务器，无需构建产物
- **解决方案**: CI 前端启动从 `npm run preview` 改为 `npm run dev`
- **相关文件**: `.github/workflows/ci.yml`
- **提交记录**: `95d75cd8`
- **状态**: 已修复

---

### BUG-016: springdoc-openapi WebJars 与 Spring Framework 6.1 不兼容

- **日期**: 2026-05-29
- **Bug 描述**: 后端启动时抛出 `NoClassDefFoundError: LiteWebJarsResourceResolver`，Spring Boot 无法启动；CI 后端健康检查失败
- **根因分析**: `springdoc-openapi-starter-webmvc-ui:2.8.x` 传递依赖 `webjars-locator-lite`，该库引用了 `LiteWebJarsResourceResolver` 类，但 Spring Framework 6.1.x 已移除该类。Spring Boot 3.3.13 内置 Spring Framework 6.1.x，运行时触发 `NoClassDefFoundError`
- **解决方案**: 移除 `springdoc-openapi-starter-webmvc-ui`，改用 `springdoc-openapi-starter-webmvc-api`（不包含 Swagger UI 依赖）；Swagger UI 通过 `static/swagger-ui.html` 静态页面从 CDN 加载
- **相关文件**:
  - `xm_film/springboot/pom.xml`（依赖切换）
  - `xm_film/springboot/src/main/resources/static/swagger-ui.html`（新文件，CDN 加载 Swagger UI）
- **提交记录**: `d48de68e`、`1ee0e2bb`
- **状态**: 已修复

---

### BUG-017: FilmMapper.xml 列名 boxOffice 与 schema.sql 定义的 box_office 不匹配

- **日期**: 2026-05-29
- **Bug 描述**: `GET /api/v1/films/box-office/top?topNum=10` 返回 500 错误，E2E 测试失败
- **根因分析**: FilmMapper.xml 中 ORDER BY/INSERT/UPDATE 使用了 camelCase 列名 `boxOffice`，但 schema.sql 定义的是 snake_case 列名 `box_office`。MyBatis `map-underscore-to-camel-case` 仅对 SELECT `film.*` 的自动映射有效，不影响 ORDER BY、INSERT、UPDATE 中的显式列名。本地 MySQL 是旧 schema（列名为 `boxOffice`），CI MySQL 从 schema.sql 创建（列名为 `box_office`），导致 CI 中 3 处显式引用报错
- **解决方案**: FilmMapper.xml 中 3 处 `boxOffice` → `box_office`：
  - 第 66 行: `ORDER BY film.boxOffice DESC` → `ORDER BY film.box_office DESC`
  - 第 98 行: INSERT 列名 `boxOffice,` → `box_office,`
  - 第 137 行: UPDATE SET `boxOffice = #{boxOffice},` → `box_office = #{boxOffice},`
- **相关文件**: `xm_film/springboot/src/main/resources/mapper/FilmMapper.xml`
- **提交记录**: `6f03c737`
- **状态**: 已修复

---

### BUG-018: 登录页 setTimeout router.push 在 Playwright E2E 中不生效

- **日期**: 2026-05-29
- **Bug 描述**: 管理员/用户登录后页面未跳转，URL 停留在 `/login`，但 localStorage 中用户信息（含 token/role）已正确写入
- **根因分析**: Login.vue 在登录成功后使用 `setTimeout(() => router.push(homePath), 500)` 执行路由跳转。在 Playwright headless Chromium 环境下，`router.push` 在 `setTimeout` 回调中未能触发 Vue Router 导航（`setTimeout` 回调中的 Vue Router navigation 在 E2E 上下文中被跳过）。而 `window.location.href` 是浏览器原生 API，在任何环境下都能可靠触发导航
- **解决方案**: Login.vue 第 62 行 `setTimeout(() => router.push(homePath), 500)` 改为 `window.location.href = homePath`
- **相关文件**: `xm_film/vue/src/views/Login.vue`
- **提交记录**: `d1cace41`
- **状态**: 已修复（E2E 59/59 100% 通过）

---

### BUG-019: Front.vue 搜索框 handleSearch 使用 router.push 在 E2E 中不生效

- **日期**: 2026-05-30
- **Bug 描述**: 前台首页搜索框输入"哈利"后点击搜索按钮，URL 未跳转到 `/front/search`，仍停留在 `/front/home`；CI 中 E2E 搜索用例失败（Run #27，59 用例 58 通过 1 失败）
- **根因分析**: `Front.vue` 的 `handleSearch()` 使用 `router.push({ path: '/front/search', query: { title } })` 导航。在 Playwright headless Chromium 下与 BUG-018 登录跳转是同一类问题——`router.push` 在某些调用上下文（非用户直接交互触发）中被跳过，而 `window.location.href` 是浏览器原生 API，在任何环境下都能可靠触发导航
- **解决方案**: `handleSearch()` 中的 `router.push({ path, query })` 替换为 `window.location.href = '/front/search?title=' + encodeURIComponent(keyword)`
- **相关文件**: `xm_film/vue/src/views/Front.vue`（第 153~162 行）
- **提交记录**: `0de65567`
- **状态**: 已修复（后续 CI 59/59 100% 通过）

---

## 预防清单

1. **数据库初始化**: 新环境部署时务必执行 `xm_film/sql/init.sql`（或依次执行 `schema.sql` + `data.sql`）
2. **代理环境变量**: 本地开发测试时注意 `http_proxy`/`https_proxy` 是否会影响 `localhost` 请求
3. **Playwright 变量类型**: `isVisible()` 返回 `boolean`，`locator()` 返回 `Locator`，不可混用
4. **异常日志**: `RuntimeException` 子类构造函数需调用 `super(message)` 以确保 `getMessage()` 可用
5. **JWT Token**: 所有需认证的后端 API 测试务必先获取 token 并传入请求头
6. **密码明文兼容**: `data.sql` 中使用明文密码时，`login()` / `updatePassword()` 需保留 BCrypt 明文回退逻辑
7. **JS .toFixed() 类型**: `.toFixed()` 返回 `string` 而非 `number`，数值运算需用 `parseFloat()` 包裹
8. **API 路径前导斜杠**: axios GET 请求路径必须以 `/` 开头（如 `'/film/selectAll'`），否则拼接 baseURL 后路径错误
9. **SQL 列名一致**: MyBatis XML 中 ORDER BY/INSERT/UPDATE 的列名必须与数据库实际列名一致（snake_case），不能依赖 `map-underscore-to-camel-case` 自动映射（该配置仅对 SELECT 结果映射生效）
10. **E2E 路由跳转**: 页面跳转（登录/搜索等）使用 `window.location.href` 而非 `router.push`，确保在 Playwright headless 模式下可靠触发导航
11. **依赖兼容性**: Spring Boot 3.3.x (Spring 6.1.x) 项目引入依赖时需确认其不引用已移除的 Spring 类（如 `LiteWebJarsResourceResolver`）
