# Bug 修复记录

> 记录项目中已修复的 Bug，避免重复踩坑。
> 遇到相似问题时，优先查阅本文档。
>
> **注**：项目中的 Playwright E2E 测试已于 2026-09-27 整体移除。
> 以下历史条目中凡提及 E2E / Playwright 的根因分析与结论均按当时情况原样保留，
> 仅供追溯；其中与框架无关的经验（如 BUG-018/019 的导航时序结论）仍然适用于前端开发。

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
  - 在 `src/main/resources/db/` 下放置副本，支持 `spring.sql.init` 自动初始化 —— **已于 2026-09-27 移除**（`spring.sql.init.mode` 恒为 `never`，该副本从未被加载，且 `data.sql` 已与 `xm_film/sql/` 漂移）
- **相关文件**:
  - `xm_film/sql/schema.sql`、`xm_film/sql/data.sql`、`xm_film/sql/init.sql`
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

### BUG-020: 选座环节 USER 读取排片被误拦截

- **日期**: 2026-06-19
- **Bug 描述**: USER 角色用户在选座页面调用 `GET /api/v1/records/{id}` 时报"无权操作该排片"，无法正常选座购票
- **根因分析**: `RecordController.ensureRecordAccess()` 仅允许 ADMIN 和 CINEMA 角色访问，未放行 USER 角色。选座页作为读操作不需要角色校验，被误拦截
- **解决方案**: `getById()` 中移除非必要的角色校验（仅保留空值检查），写操作（PUT/DELETE）保持原有权限保护不变
- **相关文件**: `xm_film/springboot/src/main/java/com/example/springboot/controller/RecordController.java`
- **提交记录**: `b0578696`
- **状态**: 已修复

---

### BUG-021: OrderedServiceTest 取消用例状态不匹配 P1 变更

- **日期**: 2026-06-19
- **Bug 描述**: P1 支付流程上线后，4 个 `OrderedServiceTest` 单元测试因状态不匹配而失败
- **根因分析**: P1 将 `cancelOrder` 方法接受的订单状态从"待取票"收窄为仅"待支付"，但测试 mock 数据仍使用旧状态
- **解决方案**: 更新测试 mock 数据中的订单状态为"待支付"
- **相关文件**: `xm_film/springboot/src/test/java/com/example/springboot/OrderedServiceTest.java`
- **提交记录**: `fcf6e256`
- **状态**: 已修复

---

### BUG-022: CORS 通配符 + pending_timeout_at 设置 null 不写库

- **日期**: 2026-06-20
- **Bug 描述**: (a) CORS 配置使用 `*` 通配符，生产环境存在安全隐患；(b) 订单取消后 `pending_timeout_at` 字段未清除，MyBatis UPDATE 跳过了该字段；(c) 前端 token 过期无法自动检测登出
- **根因分析**:
  - (a) `CorsConfig.java` 中 `allowedOrigins` 设为 `*`，允许任意域跨域访问
  - (b) `OrderedMapper.xml` 中 UPDATE 语句用 `<if test="pendingTimeoutAt != null">` 包装该字段，Java 显式设为 `null` 后 `<if>` 判断为 `false`，跳过了该字段的更新
  - (c) 缺少 token 有效性校验端点和前端自动检测逻辑
- **解决方案**:
  - (a) CORS 从 `*` 改为 `CORS_ALLOWED_ORIGINS` 环境变量白名单
  - (b) 移除 `<if>` 包装，允许显式 `null` 写入数据库
  - (c) 新增 `/api/v1/auth/me` 接口；前端 `useAuth.js` 初始化自动校验 token，过期自动登出
- **相关文件**: `CorsConfig.java`、`OrderedMapper.xml`、`OrderedService.java`、`AuthController.java`、`useAuth.js`、`application-prod.yml`
- **提交记录**: `0cbb6664`
- **状态**: 已修复

---

### BUG-023: Docker HTTPS 部署 + Vite SPA 路由 403

- **日期**: 2026-06-25
- **Bug 描述**: (a) 生产环境 Docker 部署前端缺少 HTTPS 支持；(b) Vite `fs.allow` 配置导致 SPA 路由刷新时返回 403；(c) `VITE_API_BASE_URL` 硬编码为 `http://localhost:9090` 无法适配同源部署
- **根因分析**:
  - (a) Nginx 配置缺少 SSL 证书挂载和 HTTPS server block
  - (b) `vite.config.js` 中 `fs.allow` 限制过严，SPA 路由刷新时 Vite 开发服务器拒绝服务
  - (c) `vue/.env` 中 `VITE_API_BASE_URL=http://localhost:9090` 被 git 跟踪，生产环境无法覆盖
- **解决方案**:
  - (a) 前端容器加 443 端口 + SSL 证书挂载；Nginx HTTPS server block + HTTP→HTTPS 301 重定向
  - (b) `vite.config.js` 中 `fs.allow` 改为允许项目根目录
  - (c) `vue/.env` 取消 git 跟踪，默认值改为 `/`，新增 `.env.development` 本地开发配置；`request.js` 回退值从 `http://localhost:9090` 改为 `/`
  - (d) `npm audit fix` 修复 8 个前端安全漏洞（1 critical, 4 high, 3 moderate）
- **相关文件**: `nginx.conf`、`docker-compose.yml`、`vite.config.js`、`request.js`、`.env` → `.env.development`（其中 `nginx.conf`、`docker-compose.yml` 已于 2026-09-27 随 Docker 层移除；(b)(c) 两处在 `vite.config.js` 与 `.env.development` 的修复仍然生效）
- **提交记录**: `1bbb6571`
- **状态**: 已修复

---

### BUG-024: 生产环境 MySQL 乱码

- **日期**: 2026-06-25
- **Bug 描述**: 生产环境 MySQL 中文数据出现乱码，页面显示问号或乱码字符
- **根因分析**: JDBC 连接 URL 缺少 `characterEncoding=utf-8` 和 `useUnicode=true` 参数，MySQL 连接使用默认编码（非 UTF-8）
- **解决方案**: `application.yml` 中 JDBC URL 追加 `?useUnicode=true&characterEncoding=utf-8`
- **相关文件**: `xm_film/springboot/src/main/resources/application.yml`
- **提交记录**: `71927b4d`
- **状态**: 已修复

---

### BUG-025: 生产环境 /files/* 图片全部 404

- **日期**: 2026-06-25
- **Bug 描述**: Docker 部署后所有电影海报、用户头像、预告片返回 404，页面图片全部缺失
- **根因分析**: SQL seed 数据引用了 61 个 `/files/*` 资源（47 JPG、4 PNG、10 MP4），但仓库中不存在这些文件。Docker 部署时 `uploads` 命名卷为空，无种子文件填充机制
- **解决方案**:
  - 新增 `xm_film/sql/seed-uploads/` 目录，容纳 61 个自动生成的占位文件
  - 新增 `scripts/generate-seed-uploads.ps1` 种子文件生成脚本
  - 新增 `scripts/docker-entrypoint.sh` Docker 入口包装脚本
  - Dockerfile 在构建时将种子文件拷入镜像，entrypoint 在首次启动时自动填充空卷
  - 用户后续上传不受影响（仅首次部署时填充空卷）
- **相关文件**: `Dockerfile`、`scripts/docker-entrypoint.sh`、`scripts/generate-seed-uploads.ps1`、`xm_film/sql/seed-uploads/`（61 个文件）—— **以上文件已于 2026-09-27 随 Docker 层一并移除**，本项目改为纯本地运行，本 Bug 的修复机制不再适用
- **提交记录**: `6adac709`
- **状态**: 已修复

---

### BUG-026: 同名素材文件覆盖导致部分占位图未替换

- **日期**: 2026-06-25
- **Bug 描述**: 替换 seed-uploads 为真实素材后，部分占位图未被替换，仍显示占位内容
- **根因分析**: 映射脚本使用合并对象（`{源文件: UUID}`）存储映射关系，当多个不同 UUID 文件名映射到同名源文件时，后一个覆盖前一个，导致"毒液：最后一舞"海报被视频封面覆盖、演员张梓宸头像被其他映射覆盖
- **解决方案**: 映射结构改为数组存储 `[源文件, UUID]` 对，支持一源多目标映射
- **相关文件**: `scripts/replace-with-real-images.mjs`（**该脚本与 `xm_film/sql/seed-uploads/` 目录均已于 2026-09-27 随 Docker 层移除**）
- **提交记录**: `2e6f2856`
- **状态**: 已修复

---

### BUG-027: /files/ 未设置 Cache-Control 导致浏览器缓存旧占位图

- **日期**: 2026-06-25
- **Bug 描述**: 替换占位图为真实素材后，用户浏览器仍显示旧占位图，需手动刷新或清除缓存
- **根因分析**: Nginx 代理 `/files/` 静态资源时未设置 `Cache-Control` 头，浏览器默认强缓存旧占位图
- **解决方案**: Nginx location `/files/` 添加 `add_header Cache-Control 'no-cache'`，每次请求回源验证
- **相关文件**: `xm_film/vue/nginx.conf`（**已于 2026-09-27 随 Docker 层移除**；若将来改用裸 jar + Nginx 反代部署，需在新配置的 `/files/` location 重新加上 `add_header Cache-Control`）
- **提交记录**: `22c6b60b`
- **状态**: 已修复

---

### BUG-028: 票房显示比真实值小 10000 倍

- **日期**: 2026-09-27
- **Bug 描述**: 所有展示票房的页面（`front/FilmDetail.vue`、`front/FilmCinema.vue`、`front/Home.vue`、`front/Rank.vue`）把影片票房显示成 `8868.5元` / `8,868.5元`，而真实值是 8868.5 **万元**——相差 10000 倍。同一字段在 4 个页面上还有两种互不相同的格式（万级 vs 千分位）
- **根因分析**: `film.box_office` 在数据库中的单位是万元，`schema.sql` 的列注释已写明 `DECIMAL(10,1) ... COMMENT '票房（万元）'`；但前端 5 处独立实现（含已作为死代码删除的旧 `utils/format.js`）一律按"元"处理——`toFixed`/`toLocaleString`/万级除法各写一套，结果全部差 10000 倍
- **解决方案**: 统一为 `xm_film/vue/src/utils/format.js` 的单一实现 `formatBoxOffice`，按行业惯例（猫眼/灯塔）输出：`0` 或空 → `暂无数据`；`< 10000` 万 → `8868.5万`；`>= 10000` 万（即 ≥ 1 亿）→ `1.23亿`。4 个视图删除本地副本改为导入
- **相关文件**: `xm_film/vue/src/utils/format.js`、`xm_film/vue/src/views/front/{FilmDetail,FilmCinema,Home,Rank}.vue`、`xm_film/sql/schema.sql`
- **提交记录**: 待提交
- **状态**: 已修复

---

### BUG-029: 电影"类型"字段前端取错，长期显示空白或"未知类型"

- **日期**: 2026-09-27
- **Bug 描述**: 5 个页面的电影类型展示失效——`back/Film.vue` 的类型列与展开面板空白；`front/Home.vue`、`front/Rank.vue`、`front/FilmDetail.vue`、`front/FilmCinema.vue` 恒显示"未知类型"；且硬编码字典把 id=1 写成"记录"，而数据库实为"纪录"
- **根因分析**: 后端 `Film` 实体没有 `types` 字段，只有 `typeIds`(`List<Integer>`) 与 `typeList`(`List<Type>{id,title}`)，类型名由 `FilmService.fillFilmTypes` 从 `film_type` 关联表填充；地区名也早已由 `FilmMapper.xml` 的 `LEFT JOIN area` 解析为 `areaName`。但前端用 3 种方式猜字段形状：`props.row.types` / `movie.types`（字段不存在 → `undefined` → 渲染空白）、`JSON.parse(data.typeIds)`（`typeIds` 是数组，`JSON.parse([5,22])` 抛错被 `try` 吞掉 → 恒为空数组），同时另行维护两份硬编码类型/地区字典
- **解决方案**: 删除全部前端硬编码 `typeMap`/`areaMap` 以及零引用的死代码 `roleTypeMap`，统一改用后端已解析字段：类型用 `typeList.map(t => t.title)`，地区用 `areaName`
- **相关文件**: `xm_film/vue/src/views/back/Film.vue`、`xm_film/vue/src/views/front/{Home,Rank,FilmDetail,FilmCinema}.vue`、`xm_film/springboot/src/main/java/com/example/springboot/entity/Film.java`、`service/FilmService.java`、`src/main/resources/mapper/FilmMapper.xml`
- **提交记录**: 待提交
- **状态**: 已修复

---

### BUG-030: 影院上映影片与排片脱节，新建场次在前台不可见

- **日期**: 2026-09-27
- **Bug 描述**: 影院后台新建一条排片后，前台该影院的影片/场次列表里看不到它，用户无法购票。实测影院 11（丁丁影城）的排片 18、19（影片 26、22）在前台完全不可见；同时后台排片表单的"电影名称"是手工输入的文本框，与 `film` 表无任何关联
- **根因分析**: 前台"影院上映哪些影片"由 `FilmMapper.selectByCinema` / `CinemaMapper.selectByFilmId` 通过 `INNER JOIN cinema_film` 决定，而 `cinema_film` 没有任何写入入口（无 Controller、无前端页面），只在 `data.sql` 里手工维护了 16 行。新建排片只写 `record` 表，不会写 `cinema_film`，于是排片与"上映关系"两张表长期漂移：`(11,22)`、`(11,26)` 两行关联缺失，对应场次成为前台不可达的死数据。`record.film_id` 当时还可空，排片本身也可能不指向任何影片
- **解决方案**:
  - 影院上映影片改为由排片派生：`FilmMapper.selectByCinema` 用 `EXISTS (SELECT 1 FROM record ...)` 取代 `cinema_film` 关联，`showCount` 改为真实场次数量；`CinemaMapper.selectByFilmId` 同样改为按 `record` 判断
  - 删除冗余表 `cinema_film`（schema.sql / data.sql / README 表清单同步移除），使 `record` 成为"影院是否上映某片"的唯一数据源
  - `record.film_id` 改为 `NOT NULL`，后端 `RecordService.validateSchedule` 强制校验影片存在并回填 `title`，后台表单的"电影名称"改为只读、"影片"改为下拉（`GET /api/v1/films`）
- **相关文件**: `mapper/FilmMapper.xml`、`mapper/CinemaMapper.xml`、`service/RecordService.java`、`sql/schema.sql`、`sql/data.sql`、`vue/src/views/back/Record.vue`
- **提交记录**: `96324f28`
- **状态**: 已修复

---

### BUG-031: 删除影片/影院/影厅会级联删除订单（交易凭证丢失）

- **日期**: 2026-09-27
- **Bug 描述**: 管理端删除一部影片、一个影院或一个影厅时，SQL 不会报错，但其历史订单会被数据库静默删除。演示时"删掉一个影院看看效果"会直接抹掉该影院的所有交易记录
- **根因分析**: `schema.sql` 中 `ordered` 表的 5 个外键全部是 `ON DELETE CASCADE`（`record_id` 为 `SET NULL`），`record` 的 `cinema_id`/`room_id` 是 `CASCADE`、`film_id` 是 `SET NULL`，`room.cinema_id` 是 `SET NULL`。级联动作完全由数据库执行，ORM 层不感知，因此删除接口返回成功而数据已丢失
- **解决方案**:
  - `ordered` 的 5 个外键（record/user/film/cinema/room）、`record` 的 3 个外键、`room.cinema_id` 全部改为 `ON DELETE RESTRICT`，并把约束显式命名（`fk_ordered_film` 等）便于后续迁移
  - 五个删除入口（Film/Cinema/Room/Record/User Controller）增加引用计数前置校验，返回可读提示（如"该影片已有 4 个排片、4 笔订单，无法删除；如需下架请将状态改为「停止上映」"）
  - 确立业务语义：**影片/影院/场次的下架走 `status`，不做物理删除**
  - 批量删除在循环校验通过后才执行，保证整批原子（不会删一半）
  - 提供幂等迁移脚本 `sql/migration-20260927-delete-guard.sql`
- **相关文件**: `sql/schema.sql`、`sql/migration-20260927-delete-guard.sql`、`controller/{Film,Cinema,Room,Record,User}Controller.java`、`service/{Record,Room,Ordered}Service.java`、`mapper/{Record,Ordered,Room}Mapper.{java,xml}`
- **提交记录**: `96324f28`
- **状态**: 已修复

---

### BUG-032: 公开的影院详情页排片列表返回 401

- **日期**: 2026-09-27
- **Bug 描述**: 未登录用户浏览影院详情页时，该影院的场次列表加载失败（401 "登录已过期"），而页面本身是公开可访问的
- **根因分析**: `front/CinemaDetail.vue` 通过 `GET /api/v1/records/page` 拉取场次，但 `AuthInterceptor.PUBLIC_READ_PREFIXES` 白名单里没有 `/api/v1/records`，匿名 GET 被直接拒绝
- **解决方案**: 将 `/api/v1/records` 加入匿名 GET 白名单（该资源不含用户隐私字段）；写操作仍受保护，由 `RecordController.requireAdminOrCinema()` 做业务层校验。补充 `AuthInterceptorAccessTest` 匿名读放行/写拒绝用例
- **相关文件**: `common/config/AuthInterceptor.java`、`src/test/java/com/example/springboot/AuthInterceptorAccessTest.java`
- **提交记录**: `96324f28`
- **状态**: 已修复

---

### BUG-033: 场次可购票判断与放映时间无关，过去场次仍可下单

- **日期**: 2026-09-27
- **Bug 描述**: 可以给已经放映结束的场次下单并生成"待支付"订单；后台也无法区分"未开始/放映中/已结束"，`record.status` 手工填的"待上映/已上映/停止上映"与前台判断用的"未开始/放映中/已结束"是两套互不匹配的取值
- **根因分析**: `record.status` 被当成人工维护的派生状态字段（放映状态本应是 `start` 的函数），而 `OrderedService.insertOrder` 校验场次存在后直接售票，从不比较 `start` 与当前时间；前端 `CinemaDetail.vue` 用 `status === '已上映' || '放映中' || '未开始'` 判断可购票，其中"已上映"与后台表单的取值重合、另两个永远不会被写入
- **解决方案**:
  - 派生状态不落库：`未开始/放映中/已结束` 一律由 `start` 计算（前端 `recordState()`），`status` 收敛为 `正常/停售` 单一人工开关
  - 新增 `common/enums/RecordStatus.java`；`RecordService.isPurchasable()` 作为唯一权威判定
  - `OrderedService.insertOrder` 增加"已停售""已开场"拒绝分支，作为下单的最后一道关
  - 新增/编辑校验：`start` 必须晚于当前（编辑时未改动时间则不重复校验，保证存量过期场次仍可停售）、`price > 0`、同影厅时段不重叠（按影片片长计算区间，默认 120 分钟兜底）
  - 种子数据 `record.start` 整体平移到未来（原来停留在 2024~2025，新规则上线后所有场次都会显示不可购票）
- **相关文件**: `service/{RecordService,OrderedService}.java`、`controller/RecordController.java`、`common/enums/RecordStatus.java`、`mapper/RecordMapper.{java,xml}`、`vue/src/views/front/CinemaDetail.vue`、`vue/src/views/back/Record.vue`、`vue/src/constants/index.js`、`sql/{data.sql,schema.sql}`
- **提交记录**: `96324f28`
- **状态**: 已修复

---

### BUG-034: 订单状态流转清空订单金额（total 被写成 0.00）

- **日期**: 2026-09-27
- **Bug 描述**: 订单一旦发生状态流转（支付/取票/取消），其 `total` 就变成 0.00，`pay_amount`/`refund_amount` 也随之失真。开发库中 18 笔订单（7 笔待取票、11 笔已取消）全部中招，用户看到的是"总费用 0 元"的订单；退票时记录的退款金额也是 0
- **根因分析**: `Ordered.total` 声明为原始类型 `double`，而 `OrderedMapper.xml` 的 `updateById` 用 `<if test="total != null">total = #{total},</if>` 守卫。原始类型经 getter 取值时永远非 null，MyBatis 的 OGNL 判断恒为真，于是任何**不带** total 的局部更新对象都会被补写 `total = 0.0`。状态流转只设置 `status`，恰好命中该路径。`Film.boxOffice`（同为原始 `double`）存在同样写法，但 `manage/Film.vue` 用 `Object.assign(form, row)` 提交整个对象，带上了 `boxOffice`，因此当前不可达 —— 属于同类地雷，未在本次改动
- **解决方案**:
  - `entity/Ordered.java` 的 `total` 改为包装类型 `Double`，使 `!= null` 守卫真正生效；调用点（`setPayAmount`/`setRefundAmount`）签名本为 `Double`，无需改动
  - 提供一次性数据修复脚本，按 `total = record.price × ordered.number` 重建被归零的存量订单金额（18 笔全部可确定性重建，0 笔不可恢复）
  - 端到端验证新增断言：支付、取票、退票、超时取消四条路径后 `total` 必须保持原值
- **相关文件**: `entity/Ordered.java`、`mapper/OrderedMapper.xml`、`service/OrderedService.java`
- **提交记录**: `60fa75f8`
- **状态**: 已修复

---

### BUG-035: 支付超时取消被事务回滚，订单停在待支付

- **日期**: 2026-09-27
- **Bug 描述**: 用户对已超时的待支付订单发起支付时，接口返回"支付超时，订单已自动取消"，但数据库里该订单仍是**待支付**——提示与实际状态不符
- **根因分析**: `OrderedService.payOrder` 在超时分支里先用 `updateById` 把订单置为已取消，紧接着 `throw new CustomException(...)`。该方法标注了 `@Transactional(rollbackFor = Exception.class)`，异常触发事务回滚，把刚刚写入的取消一并撤销。整体表现被 `OrderCleanupTask`（每 15 秒扫描一次）掩盖，所以端到端难复现，只能通过单元测试或代码审查发现
- **解决方案**:
  - 超时分支不再抛异常：`payOrder` 返回 `PayResult.TIMEOUT_CANCELLED`，由 `OrderedController` 翻译成 409 业务错误返回客户端，取消得以正常提交
  - 新增 `common/enums/PayResult.java`，并在类注释中写明"为何不能用异常表达"
  - 补单元测试 `payOrderAfterTimeoutCancelsOrderInsteadOfThrowing`：断言返回超时结果**且**调用了状态更新，实现若改回抛异常该用例即失败
- **相关文件**: `service/OrderedService.java`、`controller/OrderedController.java`、`common/enums/PayResult.java`、`src/test/java/com/example/springboot/OrderedServiceTest.java`
- **提交记录**: `60fa75f8`
- **状态**: 已修复

---

### BUG-036: 影院分页接口被排除在拦截器外，角色信息缺失导致审核列表查不到待审核影院

- **日期**: 2026-09-28
- **Bug 描述**: 为「未审核影院不对外展示」加上按角色过滤后，管理员打开影院管理页也只看到 4 家已审核影院，新注册的（未审核）影院在前后台都查不到 —— 管理员因此根本无法审核它，「影院注册审核」这条业务线整体不可用
- **根因分析**: `WebMvcConfig.addInterceptors` 的 `excludePathPatterns` 里列着 `/api/v1/cinemas/page`。被排除的路径**根本不进 AuthInterceptor**，拦截器自然不会往 request 写 `role`/`userId` 属性，控制器里的 `isAdmin()` 于是恒为 false，"管理员看全部、其余人只看已审核"退化成"所有人都只看已审核"。该排除项本意是放开公开访问，但 `PUBLIC_READ_PREFIXES` 已包含 `/api/v1/cinemas`，排除是冗余的 —— 它唯一的实际效果是让这个端点变成"角色盲"
- **解决方案**:
  - 从 `excludePathPatterns` 移除 `/api/v1/cinemas/page`，并在代码里留注释说明"公开访问交给 `PUBLIC_READ_PREFIXES`，不要往排除表里加"
  - 端到端验证补断言：管理员的 `/cinemas/page` 必须能看到未审核影院，且审核通过后该影院出现在前台列表并可以登录
- **相关文件**: `common/config/WebMvcConfig.java`、`common/config/AuthInterceptor.java`、`controller/CinemaController.java`、`mapper/CinemaMapper.xml`
- **提交记录**: `e3a250f1`
- **状态**: 已修复

---

### BUG-037: 支付不校验余额、不扣减、不记账 —— "点一下按钮就出票"

- **日期**: 2026-09-28
- **Bug 描述**: 订单进入「待支付」后点「模拟支付」，无论用户账户有没有钱都直接出票（`待取票`）。系统既没有用户余额字段，也没有充值单据与资金流水模型 —— 所谓"模拟支付"只是一次无条件的状态流转，"余额不足则支付失败、订单保持待支付"这条分支根本不存在
- **根因分析**: `OrderedService.payOrder` 的校验只有两条：订单存在、状态为「待支付」，随后直接 `status = 待取票` + `pay_amount = total`。整条链路没有任何一处读 `user` 表，`user` 表也确实没有余额列；充值单据表与资金流水表在 `schema.sql` 中不存在，因此"提交充值单不改余额""回调成功才入账""重复回调幂等"这些约束没有任何载体
- **解决方案**:
  - 数据层：`user.balance`、`recharge_order`（处理中/已完成/已失败）、`fund_flow`（来源 + 变动前后余额 + 关联单据ID）、`ordered.unit_price` 单价快照
  - 新增 `WalletService` 作为余额读写的**唯一出处**：`SELECT balance ... FOR UPDATE` 行锁 + `UPDATE ... WHERE balance >= ?` 条件更新，并在同一事务内写一条 `fund_flow`。充值入账/购票扣减/退票入账三条路径全部复用它，消除"同一套金额逻辑多处各写一遍"
  - `payOrder` 在超时判定之后调用 `walletService.debitPurchase`，余额不足抛业务冲突。因为扣款与出票在同一事务，失败时整笔回滚 —— 订单停在待支付、`pending_timeout_at` 不变、座位继续锁定，用户充值后可回到订单页继续支付
  - 新增 `RechargeService` / `RechargeController`：提交申请只生成「处理中」单据（余额不变）；`POST /api/v1/recharges/{id}/callback` 仅允许「处理中」单据流转，成功入账、失败置「已失败」且余额不变，重复回调一律拒绝
  - 余额不挂 `User` 实体，避免 `/api/v1/users` 的 `SELECT *` 把他人余额带出去；余额只经 `/api/v1/account/summary` 按 JWT 返回本人
  - 前端新增 `/front/account`（余额 + 档位/自由输入充值 + 单据列表含模拟回调双按钮 + 资金流水），`OrderPayDialog` 改为余额支付并展示余额不足差额与「去充值」入口
- **验证**: 单测 25 例（`WalletServiceTest` / `RechargeServiceTest` / `FundFlowServiceTest`）+ 端到端 59 断言（`scripts/verify/p4-account-wallet-e2e.py`）+ 并发 11 断言（`scripts/verify/p4-concurrency.py`：余额只够一单时并发支付恰好一单成功、余额为 0.50 且不为负）
- **相关文件**: `sql/schema.sql`、`sql/migration-20260928-p4-account-wallet.sql`、`service/WalletService.java`、`service/RechargeService.java`、`service/FundFlowService.java`、`controller/RechargeController.java`、`controller/FundFlowController.java`、`controller/AccountController.java`、`service/OrderedService.java`、`mapper/UserMapper.java(+xml)`、`views/front/Account.vue`、`components/OrderPayDialog.vue`
- **提交记录**: `f97eb3ab`
- **状态**: 已修复

---

### BUG-038: 退票只改状态与凭证字段，款项没有回到用户账户

- **日期**: 2026-09-28
- **Bug 描述**: 用户退票后订单变成「已退票」、`refund_amount` 也写了金额，但用户账户上什么都没发生 —— 没有余额增加，也没有任何资金流水。前台退票确认框却写着"退票后座位释放、款项退回"，属有文案无实现
- **根因分析**: `OrderedService.refundOrder` 只做 `updateById(status=已退票, refundTime, refundAmount)`，没有任何余额入账动作；且当时项目里根本没有"用户余额"这个概念，`refund_amount` 只是一份写给自己看的凭证
- **解决方案**: 在退票窗口与状态校验全部通过之后调用 `walletService.creditRefund(userId, total, orderId)`，与状态更新同事务 —— 入账成功但状态没改回去（或反之）的情况不会出现；取消未扣款的待支付订单依旧不触碰余额
- **验证**: `OrderedServiceTest.refundOrderCreditsBalanceWithOrderAmount` / `refundOrderDoesNotCreditWhenDeadlinePassed`（校验必须先于资金动作）；端到端断言余额 `181.50 → 300.00`、座位释放、新增一条 `+118.50` 且关联订单ID 的退票流水
- **相关文件**: `service/OrderedService.java`、`service/WalletService.java`、`mapper/OrderedMapper.xml`
- **提交记录**: `f97eb3ab`
- **状态**: 已修复

---

### BUG-039: 订单可被物理删除，删单成为绕过退票的免费后门

- **日期**: 2026-09-28
- **Bug 描述**: 用户端、影院端、管理端三处订单列表的删除按钮都不判状态，后端 `deleteScoped` 也只校验归属。用户对一张「待取票」（已付款）订单点删除，订单行直接消失、座位被静默释放、既没有退票记录也没有退款流水 —— 等于用删除当免费退票用，资金凭证链彻底断裂
- **根因分析**: 删除接口是从通用 CRUD 继承下来的，只做了"这条订单是不是你的"的归属校验，没有做"这条订单允不允许被删"的状态校验。资金模型落地后这个缺口更严重：退票会回款而删除不会，只要后门开着，用户必然走后门
- **解决方案**:
  - 后端新增 `OrderedService.DELETABLE_STATUSES = {已取消, 已退票}` 与 `ensureDeletable` 守卫，单删与批删都先校验（批删任一不合格则整批拒绝）
  - 前端三端按钮由 `constants.isOrderDeletable` 同构条件渲染；列表勾选列加 `:selectable`，不可删除的订单连勾选都不允许，批量删除自然带不上它们
- **验证**: `OrderedServiceTest` 六个用例（待支付/待取票/已取票拒绝，已取消/已退票放行，批删整批拒绝）；端到端断言删除待取票订单被拒且订单仍存在、已退票与已取消订单可删除
- **相关文件**: `service/OrderedService.java`、`views/front/Orders.vue`、`views/back/Ordered.vue`、`views/manage/Ordered.vue`、`constants/index.js`
- **提交记录**: `f97eb3ab`
- **状态**: 已修复

---

### BUG-040: 选座接口把全场次订单明细发给任意登录用户（越权读）

- **日期**: 2026-09-28
- **Bug 描述**: `GET /api/v1/orders/seats?recordId=X` 直接返回 `Ordered` 实体列表，任何登录用户只要换一个 `recordId`，就能拿到该场次**所有**订单的订单编号、购票用户ID、订单金额、支付/退款凭证字段。选座图渲染只需要"哪个座位被占"，其余字段全是越权可见的他人交易信息；前端也确实是拿响应里的 `userId` 和自己本地存的 ID 比对来判断"是不是我的锁座"
- **根因分析**: 该端点当初直接复用了面向后台列表的 `selectActiveByRecordId`（`SELECT *`），把"内部查询"当成了"对外响应"。归属信息（`user_id`）被一并下发，判定"是不是我的"这件事被推给了前端 —— 等于先泄露再让前端自觉忽略
- **解决方案**:
  - 新增 `dto/response/SeatOccupancy`：只含 `seat` 与 `mine`；仅当 `mine` 为真时才带 `orderId` / `orders` / `status` / `total` / `pendingTimeoutAt`（继续支付与取消锁座所需）
  - `OrderedService.selectSeatOccupancy(recordId, tokenUserId)` 在后端按 JWT 里的 `userId` 完成归属判定与字段裁剪，他人订单只回 `{seat, mine:false}`；`tokenUserId` 为空时一律判定为非本人，避免误把他人订单当成自己的
  - `OrderedController.seats` 改用该方法；原先只做透传的 `OrderedService.selectActiveByRecordId` 随之删除
  - `BuyTicket.vue` 改读 `order.mine`（不再比对 `userId`），并把选座视角的字段映射成支付弹窗需要的订单形态；本地不再保存 `userId`
- **验证**: `OrderedServiceTest` 三例（他人订单只出 `seat`+`mine:false`、本人订单带齐字段、无令牌用户不得被判成本人）；端到端新增 10 条断言（他人座位可见但 `orderId`/`orders`/`total` 为 null、响应中不含 `userId` 键、本人座位 `mine:true` 且带 `orderId`）—— E2E 共 69 断言全通过
- **相关文件**: `dto/response/SeatOccupancy.java`、`service/OrderedService.java`、`controller/OrderedController.java`、`views/front/BuyTicket.vue`、`scripts/verify/p4-account-wallet-e2e.py`
- **提交记录**: `f97eb3ab`
- **状态**: 已修复

---

### BUG-041: 购票可双击重复提交、支付超时提示与真实行为不符

- **日期**: 2026-09-28
- **Bug 描述**: 两个前端交互缺陷合在一起：① 选座页「确认购票」在网络往返期间不禁用，双击会连发两次下单请求，第二次必然被"座位已售"拒绝并在成功弹窗旁边弹出一条错误提示；② 支付弹窗倒计时归零时提示"支付超时，订单已自动取消"，但前端归零只关闭弹窗，订单其实仍停在待支付，要等后端定时任务（约 15 秒后）才真正取消 —— 文案断言了一件当时还没发生的事
- **根因分析**: ① 提交类按钮缺少在途状态，只挡了"未选座/未登录/加载中"，没挡"上一次请求还在飞"；② 文案把"前端倒计时结束"等同于"订单已取消"，混淆了前端计时器与后端 `OrderCleanupTask` 两条独立路径
- **解决方案**:
  - `BuyTicket.vue` 新增 `submitting` 在途标记与 `canSubmit` 计算属性，提交期间按钮禁用并显示"提交中…"，`finally` 中复位
  - `OrderPayDialog.vue` 倒计时归零改提示"支付时间已到，未支付的订单将自动取消"，并注释说明真正的取消由后端定时任务完成
- **验证**: 前端 `npm run build` 通过；后端全量单测 154 例全绿（改动未触及后端逻辑）。**两条均为纯前端交互改动，没有浏览器点击验证，仅验证到构建通过与后端接口未回归**
- **相关文件**: `views/front/BuyTicket.vue`、`components/OrderPayDialog.vue`
- **提交记录**: `f97eb3ab`
- **状态**: 已修复

---

### BUG-042: 登录页卡片宽度有两个来源 —— 标题折行、表单溢出 188px

- **日期**: 2026-09-28
- **Bug 描述**: `/login` 页面上「欢迎登录电影购票系统」被拆成两行，账号 / 密码 / 角色三个输入框横向冲出白色半透明卡片约 188px，卡片看上去只有内容的一半宽。`/register` 逐字同病
- **根因分析**: 卡片宽度有**两个互不相干的来源**，且都对不上。外层 `.login-box` 是 `width: 40%` + `max-width: 400px` + `padding: 64px`，内层 `.login-form-wrapper` 又是 `max-width: 380px` + `padding: 40px`（全局 `box-sizing: border-box`）：两级 padding 叠加后，卡片**内容宽只剩 192px**；而 `.login-form` 又写死 `width: 380px` —— 这是从旧内联样式 `style="width: 380px"` 原样搬过来的（`cc3379b0` 消解内联样式时保留了它），容器宽度却在那之前就已经收窄了。溢出 380 − 192 = 188px。标题折行是同一根因：10 个汉字 × `--fs-xl`(20px) = 200px > 192px，必然断行
- **解决方案**:
  - 抽出 `src/assets/css/auth-layout.scss`，与 `Register.vue` 共用（同 `admin-layout.scss` 的处理方式）：卡片宽度只由 `.auth-card` 的 `max-width: 380px` 决定，**删除**页面里写死的 `width: 380px`
  - 容器改为 `min-height: 100vh` + flex 居中，去掉 `height: 100vh` + `overflow: hidden` + 绝对居中的组合 —— 顺带修掉"视口变矮时卡片被裁掉且无法滚动"
  - 两页共用同一张背景图（按产品要求把注册页换成登录页的图），背景声明也收进共用外壳
  - 规范新增 §6.4 固化这条口径；`tests/design-tokens.test.mjs` 加终态断言（认证页不得出现写死的 px 宽度、必须 `@use` 共用外壳、共用外壳不得出现 `overflow: hidden`）
- **验证**: `npm run test:tokens` 13/13、`test:inline` 2/2、`npm run build` 通过；构建产物核对 `Login-*.css` 与 `Register-*.css` 均引用同一份 `bg_login-*.jpg`，注册页旧图 `registerbg.jpg` 已不再打包（无引用）。**页面渲染由用户在本机目视确认通过**
- **相关文件**: `src/assets/css/auth-layout.scss`（新增）、`src/views/Login.vue`、`src/views/Register.vue`、`tests/design-tokens.test.mjs`
- **提交记录**: 未提交
- **状态**: 已修复

---

### BUG-043: 认证页表单无可访问名称、无错误图标、不支持回车提交，前缀图标不渲染

- **日期**: 2026-09-28
- **Bug 描述**: 四个同源的表单缺陷：① 所有输入框只有 `placeholder`，没有 `label`，读屏软件读不出字段用途，且一开始输入提示就消失；② 校验只出红框与文案，规范 §9.3 要求的"错误图标"缺失；③ 回车键不能提交，只能鼠标点按钮；④ 账号 / 密码输入框左侧的 `User` / `Lock` 图标根本不显示，只有一块空白
- **根因分析**: ①③ 从未实现；② 缺 `status-icon` —— 错误图标由该属性驱动，EP 不会自己加；④ `Login.vue:8,11` 与 `Register.vue:11,19,27` 用的是**字符串**写法 `prefix-icon="User"`，而 `prefix-icon` 接受的是组件，字符串要靠全局注册才能解析 —— 全项目从未 `app.component()` 注册图标集（`main.js` 无该调用），因此解析失败、图标为空。全项目其余 20 多个页面都用绑定写法 `:prefix-icon="Search"`，只有认证两页是字符串，属孤例
- **解决方案**:
  - `import { User, Lock } from '@element-plus/icons-vue'` + `:prefix-icon="User"`，与其余页面统一
  - `el-form` 加 `label-position="top"` 与 `status-icon`，每个 `el-form-item` 补可见 `label`
  - 文本输入框加 `@keyup.enter`，`el-form` 挂 `@submit.prevent` 兜住原生提交；**只在一处绑定**，避免一次回车发两次请求
  - `role` 的校验触发由 `blur` 改 `change`（下拉框不会触发 blur 那一刻的语义）
  - 规范 §10.2 新增「表单控件必须有可访问名称」、§9.3 补 `status-icon` 与回车提交的施工口径；其余表单页的同类整改列入《前端规范待办》T-1
- **验证**: `npm run test:tokens` 13/13（其中新增断言校验认证页有可见 label、`status-icon`、`@keyup.enter`，且不再出现字符串 `prefix-icon`）；`npm run build` 通过。**回车提交与图标显示由用户在本机目视确认通过**
- **相关文件**: `src/views/Login.vue`、`src/views/Register.vue`、`标准前端视觉与交互设计规范.md`（§9.3 / §10.2 / 附录 A）
- **提交记录**: 未提交
- **状态**: 已修复

---

### BUG-044: 登录页角色下拉默认选中"管理员"，把普通用户导向鉴权失败

- **日期**: 2026-09-28
- **Bug 描述**: `/login` 的角色下拉初始值写死 `ADMIN`。普通用户登录时若忘记切换身份，会带着 `role=ADMIN` 去鉴权；后端按 role 路由到不同表（`AuthController` → `adminService` / `cinemaService` / `userService`），于是要么报角色不匹配，要么在管理员表恰好存在同名账号时登进管理员 —— 默认值把一个"每次都存在的选择"变成了默认错误答案
- **根因分析**: 默认值取的是三端里**最少人用的角色**。选择器本身不能删（后端按 role 路由到三张表，必须显式指定），所以问题只在默认值的取值方向
- **解决方案**: `data.form.role` 由 `"ADMIN"` 改为 `"USER"`（最常见的登录身份），并把下拉项按 用户 / 电影院 / 管理员 排列。这样失败模式更安全：忘记切换只会得到明确的角色不匹配提示，而不会误登
- **验证**: 构建通过；登录流程本身未改动（仍走 `useAuth.login`），由用户在 `/login` 目视确认默认选中"用户"
- **相关文件**: `src/views/Login.vue`
- **提交记录**: 未提交
- **状态**: 已修复

---

### BUG-045: 登录 / 注册页页脚声明文字压在浅色插画上，对比度 1.41:1

- **日期**: 2026-09-28
- **Bug 描述**: 页面底部的免责声明「本系统为个人学习项目…」几乎看不见 —— 它是浅灰字压在浅蓝插画上
- **根因分析**: 该段文字用了 `--dark-text-secondary`(#cccccc)，而这是 §3.5 的**深色表面**令牌（设计用于 `#1a1a1a` 一类深底，那里是 10.84:1）。登录页背景是浅色插画（实测约 `#7FB2DC`），#ccc 压其上只有 **1.41:1**。这不是"换个颜色就能修"的问题：同一位置改压深色文字（`--el-text-color-regular` #606266）也只有 **2.71:1** —— 直接往图片上放文字，深浅两头都到不了 4.5:1
- **解决方案**: 经确认该段声明与前台页脚（`Front.vue` 第 133 行有更完整的同名声明）重复，**按产品决定删除**认证两页的该段文字及对应样式，不再往背景图上压文字。共用外壳里的相关样式一并移除，未留下无用类名
- **验证**: `grep` 确认 `src/` 与 `tests/` 内已无残留引用；`npm run build` 通过
- **相关文件**: `src/views/Login.vue`、`src/views/Register.vue`、`src/assets/css/auth-layout.scss`
- **提交记录**: 未提交
- **状态**: 已修复（按产品决定移除，而非配色补偿）

---

### BUG-046: 种子业务数据账实不符，票房与"今日票房"由虚构数值驱动

- **日期**: 2026-09-29
- **Bug 描述**: 三处假数据同时存在。① `data.sql` 预置的 12 条订单/51 条评价与资金账本对不上；② 前台首页「今日票房」写死 `1.28亿`，刷新按钮用随机数改数字；③ 票房榜由 `film.box_office` 这个运行时从不重算的静态列驱动
- **根因分析**: 三条互相独立的来源：
  1. **手写订单必然要伪造一整条链**。`data.sql:172` 的 `ordered` 列清单**不含 `unit_price`**（单价快照全为 NULL）；`fund_flow` 一条种子都没有 —— 8 条带 `pay_time/pay_amount` 的"已支付"订单在账本里**没有任何对应购票流水**；`zhangsan` 余额 100.00 **未因**他那条 42 元订单扣减；订单号是 `202603058485`（12 位纯数字），而真实单号由 `OrderedService.generateOrderNo` 生成，是 `yyyyMMdd` + 8 位大写十六进制。三项互相印证即可判定为编造
  2. `front/Home.vue` 的 `totalPrice` 写死 `{total: 1.28, change: 5.3}`；`refreshTodayBoxOffice` 用 `Math.random()` 改这个数字并弹「已更新最新今日票房数据」—— 刷新按钮不请求任何接口，界面谎报数据新鲜度
  3. `film.box_office` 只由 `data.sql` 写入，`FilmMapper` 之外无任何代码重算它，前端也只读展示、无编辑入口 → 票房榜 100% 由虚构数值驱动。**对比**：`film.score` 是真的 —— 由 `MarkService` 按 `mark.score` 求均分回写
- **解决方案**:
  1. `data.sql` 删除 `record`（15 行）/`ordered`（12 行）/`mark`（51 行）三块种子与恒为空操作的均分回写 UPDATE，并把 `film.box_office` 种子值清零；种子只留基础数据（管理员/用户/影院/影厅/影片/词表）
  2. `migration-20260928-p3` 删除镜像那 51 条评价的第 3 节 —— 否则它成了唯一还会造出假评价的地方
  3. 票房改为按 `ordered` 实时聚合（`FilmMapper.xml` 的 `filmRevenueJoin`，只统计 `待取票/已取票`），单位由「万元」改为**元**（本系统内的售票收入是几十到几百元量级，按万元渲染会恒显示 0.00万）；`film.box_office` 废弃并由 `migration-20260929-deprecate-box-office.sql` 清零存量值
  4. 新增只读统计接口 `GET /api/v1/statistics/overview`（仅 ADMIN），后台大盘不再由前端拉 films+cinemas+types 三张全表自己算
  5. 删除「今日票房」组件与其随机数刷新；补齐「暂无数据 / 数据加载失败，请稍后重试」的空态与异常态
  6. 新增 `scripts/seed-demo-data.py`：走真实接口生成 3 条已支付并取票的订单 + 3 条真实评价（lisi/wangwu 先经真实充值流程补足余额），并清理演示账号的旧数据
  7. 顺带修掉 `front/CinemaDetail.vue` 的「免费停车」硬编码卡（含具体地址，且该地址实为丁丁影城的信息，却对每家影院都展示）与 `roomId` 缺失时静默兜底成"一号厅"的跳转
- **验证**: `mvn test` 154 例全绿；前端 `npm run build` + `test:tokens`/`test:inline`/`test:bundle` 全绿；E2E 70/70、并发 12/12（临时库 `xm_film_verify` + 备用端口 9191）；开发库 `xm-film` 实测落库 3 条真实订单（单号 `20260929B588E9EC` 格式、`unit_price`=场次票价、余额 100→55/60.5/48、购票流水 3 条 + 充值流水 2 条、`film.score` 回写 8.8/9.2/9.0）；`/statistics/overview` 返回 4 影院 + 45 条类型计数，USER 调用被 403 拒绝；票房榜返回 52.0/45.0/39.5 元三行。**未做浏览器渲染验证**（UI 目视由用户自查）
- **相关文件**: `xm_film/sql/data.sql`、`xm_film/sql/schema.sql`、`xm_film/sql/migration-20260928-p3-review-score-cinema-audit.sql`、`xm_film/sql/migration-20260929-deprecate-box-office.sql`、`FilmMapper.xml`/`FilmMapper.java`、`CinemaMapper.xml`/`CinemaMapper.java`、`StatisticsController.java`/`StatisticsService.java`、`Film.java`、`front/Home.vue`、`front/Movie.vue`、`front/Rank.vue`、`front/CinemaDetail.vue`、`front/Cinema.vue`、`front/FilmCinema.vue`、`manage/Home.vue`、`utils/format.js`、`constants/index.js`、`scripts/seed-demo-data.py`、`scripts/verify/p4-*.py`
- **提交记录**: 未提交
- **状态**: 已修复

---

### BUG-047: 前台首页「今日票房」以真实数据源重新引入（修订 BUG-046 第 5 条的处置）

- **日期**: 2026-09-29
- **问题描述**: BUG-046 第 5 条把「今日票房」组件连同它的随机数刷新一起删掉了。删除是对当时那份假实现的正确处置，但组件本身是首页要有的功能 —— 于是需求重新提出：首页要有今日票房，且数据必须真实。
- **根因分析**: 重新引入的难点不在组件，而在**这个数没有任何现成来源**：
  1. 唯一的聚合票房口径 `FilmMapper.xml` 的 `filmRevenueJoin` 只按影片累计，**没有日期维度**，取不出「今天」
  2. `/api/v1/statistics/overview` 是 ADMIN 专属，首页是公开页（游客可访问），用了就是把游客踹去登录页
  3. 订单相关端点全在 `AuthInterceptor` 覆盖范围内、未登录一律 401（`/api/v1/orders/**` 不在 `PUBLIC_READ_PREFIXES` 里）
  4. 所以「从用户接口获取」必然意味着新增一个匿名可读端点
- **口径决策（两处都是有实际后果的分歧，不是形式选择）**:
  1. **按 `pay_time`（收款日）取日，不按 `ordered.start`（放映日）**。行业里「今日票房」通常指当日场次的票房，但本系统是**提前购票**：种子脚本把三个演示场次排在今天 +5/+6/+7 天（`scripts/seed-demo-data.py` 的 `SLOT_OFFSET_DAYS`），按放映日聚合会让这个指标长期恒为 0，失去展示价值。按收款日则它天然是累计票房的一个日期切片，必然 ≤ 累计票房，与首页已有的「总票房Top 10」同源可比。
  2. **空集上的 0 渲染成 `0.00元`，不是「暂无数据」**。「今天还没卖出票」本身就是数据，空集 SUM 是 0 而非缺失。为此新增 `formatYuan` 而没有复用 `formatBoxOffice` —— 后者把 0 当缺失值返回「暂无数据」（票房榜靠 `rev.revenue > 0` 过滤，所以它从来看不到 0，不能改它）。
- **解决方案**:
  1. `OrderedMapper.selectTodayPaidRevenue()`：单条 SQL 同时算出金额与统计时刻，**日期边界与时间戳同出一个库时钟**（`CURDATE()` + `NOW()`），不会出现"边界按 23:59 切、时间戳按另一台钟写"的分叉；`status IN ('待取票','已取票')` 与 `filmRevenueJoin` 同源并注释互相指认
  2. 端点挂在 `GET /api/v1/films/box-office/today` —— `/api/v1/films` 本来就在 `PUBLIC_READ_PREFIXES` 内，**因此没有为它新增任何放行规则**，也没有碰 `excludePathPatterns`（那是角色盲区，见 BUG-036）；与同类的 `/box-office/top` 毗邻
  3. 返回 `Map` 的别名刻意写 camelCase：`map-underscore-to-camel-case` **只转换 bean 属性、不转换 Map 的 key**，写 `updated_at` 出去就是 snake_case，与全站 camelCase 不一致
  4. 前端 `utils/format.js` 新增 `formatYuan`；`front/Home.vue` 卡片挂在 `.home-aside` 最上方，刷新按钮这次**真的重新请求**（BUG-046 的病灶正是刷新按钮不请求任何接口却弹「已更新最新今日票房数据」）；失败只落错误态不弹提示（网络类提示由 `request.js` 统一给，规范 §11.2）
  5. **色条用 `--el-color-primary`（#BF352D）而非 `--color-brand`（#ef4238）**：色条上压的是白字，`#ef4238` + 白字只有 3.81:1 不达 AA。规范 §3.2 正是为此把品牌色拆成"承文字 / 不承文字"两枚令牌，BUG-046 删掉的那版用的是 `#ef4238` + 白字，属违规
- **验证**: `mvn test` 155/155（新增 1 例，钉住服务层只转发、不改写金额）；前端 `npm run build` + `test:tokens`/`test:inline`/`test:bundle` 全绿，编译产物实测色条是 `var(--el-color-primary)`、无任何硬编码 hex；**口径打真实库验证**（临时库 + 备用端口 9191，28 项断言全过、连跑两次一致）：今天支付+待取票 ✅计入、今天支付+已取票 ✅计入、今天支付后退票 ✗排除、`pay_time` 挪到昨天 ✗排除（证明取的是收款日而非场次日）、待支付 ✗排除、匿名 GET 返回 200。**未做浏览器渲染验证**（UI 目视由用户自查）
- **验证脚本**: 本次验证用的是临时脚本，按项目要求用完即删，仓库内不再保留 —— 上面这些口径结论无法从仓库里重跑，要复现请按同样的 5 类订单（今天支付待取票/今天支付已取票/今天支付后退票/`pay_time` 挪到昨天/今天下单未支付）在临时库上重建
- **相关文件**: `OrderedMapper.java`/`OrderedMapper.xml`、`OrderedService.java`、`FilmController.java`、`OrderedServiceTest.java`、`vue/src/constants/index.js`、`vue/src/utils/format.js`、`vue/src/views/front/Home.vue`
- **提交记录**: 未提交
- **状态**: 已修复

---

### BUG-048: 用户端没有取票通路，评价闭环从未对用户打开

- **日期**: 2026-09-29
- **Bug 描述**: 用户在真实走完购票流程后发现，余额支付成功后跳到「购票记录」，看到的只是 `待取票` 状态，界面里没有任何"接下来去哪取票"的路径。初看是跳转体验问题，实际是**功能缺失**。
- **根因分析**: `OrderedService.pickupOrder` 第 3 行就对 USER 硬拦截 ——
  ```java
  if ("USER".equals(role)) { throw new CustomException(ErrorCode.FORBIDDEN, "用户无权执行取票操作"); }
  ```
  取票只有 ADMIN / CINEMA 能做，入口在 `back/Ordered.vue` / `manage/Ordered.vue` 的订单列表里。而 `front/Orders.vue` 的「去评价」按钮只对 `已取票` 渲染。串起来就是：**普通用户买完票 → 状态永远停在 `待取票` → 永远看不到「去评价」→ 评价闭环对用户端从未打开过**。所有人都只在后台点「取票」时才会走通，前台缺半条链路。
- **另一个同源缺陷**: `MarkService`（120 行）**全文没有任何一处引用订单** —— 零命中 `ordered` / `OrderStatus` / `PICKED_UP`。"已取票才能评价"此前只是 `front/Orders.vue` 的按钮可见性，服务端不校验，直接 `POST /api/v1/marks` 能给任何没买过票的影片打分。
- **解决方案**:
  1. `ordered` 加 `pickup_code`（唯一列），**取票码在 `payOrder` 内与扣款同一事务生成** —— 不存在"扣了钱没码"或"有码没扣钱"，且未支付的订单永远没有码。码形 `XXXX-XXXX`，生成字母表剔除 `I/L/O/0/1`（人工从手机抄到自助机上看不错）。
  2. 新增 `POST /api/v1/tickets/redeem`（**免登录**）+ 前台「取票大厅」`front/Pickup.vue`（`meta.guest`，导航对游客可见）模拟影院自助机。入参**只有 code、没有 orderId** —— 码本身即凭证，允许传 orderId 就等于谁都能核销别人的单。
  3. **码不带任何有效/失效标记**：核销只接受 `status = '待取票'`（`markPickedUpByCode` 的状态条件更新）。这一个谓词同时实现「一单一码」「用过即废」「退票/取消作废」「没付款不出发」。有效期到放映结束，由 `ordered.start` + `film.time` 派生。用户曾提出"为期一天"，但直译成支付后 24 小时会让当前种子数据（场次在 +5~+7 天）的码在开映前就过期 —— 比不做更糟，故改为"到放映结束"。
  4. 并发重复核销**不靠悲观锁**：读到的状态可能是 `待取票`，写库走 `UPDATE ... WHERE status = '待取票'`，受影响 0 行即判定被人抢先（与余额扣减的 `WHERE balance >= ?` 同一手法）。
  5. `AuthInterceptor` 新增 `ANONYMOUS_WRITE_EXACT`（**精确路径 + 仅 POST**）—— 本仓库第一个匿名写入口。三处刻意收窄：精确匹配而非前缀、只放行 POST、不与 `PUBLIC_READ_PREFIXES` 合并（读放行的依据是"内容本来公开"，写放行的依据是"动作由凭证授权"，混在一起会让人以为写操作只要前缀命中即可放行）。为何安全逐条论证写在 `TicketController` 的类注释里，并有 4 个单测钉住三处收窄。
  6. 支付成功后的落地从"跳订单列表"改为 `OrderPayDialog` 就地切成**取票凭证态**（取票码 + 场次座位 + 「去取票大厅」），码由 `GET /api/v1/orders/{id}` 回查（该查询已 join 出影片/影院/影厅名）。
  7. `MarkService.add` 补上服务端门禁：`countPickedUpByUserAndFilm(userId, filmId) > 0`。**修改评价不重复校验** —— `已取票` 是终态，退票与删除都进不来，资格一旦成立不会被推翻。
- **踩到的坑（留给后来者）**: 本仓的业务异常经 `GlobalExceptionHandler` 返回的是 **HTTP 200 + body 里的 code**，只有 `AuthInterceptor` 才直写 401/403。本次的验证脚本第一版按 HTTP 状态码断言，6 条用例全红而产品行为其实全对（见预防清单第 15 条）。
- **验证**: `mvn test` 173/173（OrderedServiceTest 42→53、MarkServiceTest 13→15、AuthInterceptorAccessTest 30→35）；前端 `npm run build` + `test:tokens`/`test:inline`/`test:bundle` 全绿，编译产物实测 `Pickup` 分块里是真 `post(REDEEM, {code})`、卡片无硬编码色值；**全链路打真实库验证**（临时库 + 备用端口 9191，39 项断言全过、连跑两次一致；改动后又各跑一遍仍是 39/39，并回归跑了今日票房那 28 项确认本次 schema/实体/XML 改动没影响它）：未支付无码 ✅、码形态与字母表 ✅、**全程不带 Authorization 核销成功** ✅、凭条不含 orderId/订单号/金额/userId（逐字段断言）✅、小写去横杠也能核销 ✅、同码再核销 409 ✅、退票后 409 ✅、放映结束后 409 ✅、无效码 404 / 长度不符 400 / 空码 400 ✅、**未取票评价被拒 → 取票后评价成功** ✅；另单独造了一个"升级前的库"（无 `pickup_code` 列 + 待取票与已退票各一条）验证迁移：待取票订单被补上 `XXXX-XXXX` 形态的码、已退票保持 NULL、重跑既不报错也不改写已有码（换出的码**不能**用固定值断言 —— 取值含 `RAND()`/`UUID()`，每次补出来的都不同）；补出来的码走状态条件更新仍是 1 行/0 行。迁移还给存量 `待取票` 订单补码，跑完后在真实开发库 `xm-film` 上实测：2 条待取票订单各得一枚码，4 条已取票 / 2 条已取消保持 NULL。**未做浏览器渲染验证**（UI 目视由用户自查）
- **验证脚本**: 同 BUG-047，本次用的是临时脚本，按项目要求用完即删，仓库内不再保留
- **相关文件**: `xm_film/sql/schema.sql`、`xm_film/sql/migration-20260929-pickup-code.sql`、`Ordered.java`、`OrderedMapper.java`/`.xml`、`OrderedService.java`、`MarkService.java`、`RecordService.java`（`DEFAULT_DURATION_MINUTES` 提为 public 复用）、`AuthInterceptor.java`、`TicketController.java`、`dto/request/TicketRedeemRequest.java`、`dto/response/TicketVoucher.java`、`OrderedServiceTest.java`、`MarkServiceTest.java`、`AuthInterceptorAccessTest.java`、`vue/src/constants/index.js`、`vue/src/router/index.js`、`vue/src/views/Front.vue`、`vue/src/views/front/Pickup.vue`、`vue/src/views/front/Orders.vue`、`vue/src/views/front/BuyTicket.vue`、`vue/src/components/OrderPayDialog.vue`
- **提交记录**: 未提交
- **状态**: 已修复

---

### BUG-049: 前台购票记录的取票 / 退票按钮折行后左右错开

- **日期**: 2026-09-29
- **Bug 描述**: `front/Orders.vue` 购票记录表里，订单处于 `待取票` 时操作列同时有「取票」与「退票」两个文字按钮，用户反馈"因为空间有限…上下或左右无法对齐，极其影响视觉观感"。同表的 `待支付` 行「继续支付 / 取消」是同一个缺陷（更宽、错位更明显），一并修掉。
- **根因分析**: 三层原因叠加，缺一不可。
  1. **操作列分不到宽度** —— `el-table` 给未显式指定 `width` 的列按 `minWidth || 80` 起算（`element-plus/es/components/table/src/table-layout.mjs:102`），并把富余空间按各列 `minWidth` 权重分摊（`:96` 把无 `width` 的列全归入 `flexColumns`，`:106` 起分摊）。本表 13 列**全部未定宽**，最小总宽 13×80 = 1040px，而容器是 `.page-wide`（`min(85vw, 1200px)`）再减去 `.card` 的 8px 内边距 —— 1920 视口下也只有 1184px，富余 144px 摊完操作列约 **89px**，去掉 `.cell` 的 24px 内边距只剩 65px。
  2. **两个文字按钮需要 ~100px** —— `.el-button.is-link` 是 `padding: 2px` 的内联元素：4 字按钮（继续支付 / 修改评价）= 56px + 4px，2 字按钮（取票 / 退票 / 取消）= 28px + 4px；按钮间距来自 EP 的 `.el-button+.el-button{margin-left:12px}`。于是 `继续支付 + 取消` 需 104px、`取票 + 退票` 需 76px，**都超过 65px**，必然折行。
  3. **错位的直接原因**：那 12px 是 `margin-left`，**折行并不改变它**。第二个按钮落到第二行时仍带 12px 左边距，两行右错开 12px —— 用户看到的"上下无法对齐"就是这个。
- **解决方案**:
  1. 操作列显式定宽 `width="140"`（最宽组合 104px + 24px 内边距 = 128px，留 12px 富余）。
  2. 按钮组套上 `front-pages.scss` 新增的 `.row-actions`：`display: flex; gap: var(--space-8)`，并把 `.el-button + .el-button` 的 `margin-left` 中和为 0。**间距从"相邻选择器的边距"变成"容器 gap"**，于是间距与"是否折行"解耦 —— 一行时是 8px 等距，真折行时第二行也从同一左边缘起排。
  3. 定宽多出来的 60px 由两列让出：展开列 `min-width="60"`、单价列 `min-width="70"`（两列的内容本来就不需要 EP 的 80px 下限）。表格最小总宽只从 1040px 涨到 **1070px**，横向滚动条的触发阈值（约 1242px 视口）几乎不动 —— 若单纯把操作列加宽 60px 而不让宽，1280px 这类常见视口会凭空多出一条横向滚动条。
- **哪些列能让宽、哪些不能**: 展开列没有表头文字、单价列表头只有 2 个字，压到 60 / 70 仍有余量。其余列的下限被内容或表头顶住 —— `电影图片` 表头 4 字就要 56px + 24px 内边距，压到 80px 以下表头即折行；`总费用` 可能出 `5994.00` 这类 7 位金额、`订单号` 是 16 位单号（本就在折叠行里被截断），压到 70px 会新造出"金额换行 / 单号截得更狠"。宁可让最小总宽多 30px，也不制造新的折行。
- **验证**: 前端 `npm run test:tokens`(13) / `test:inline`(2) / `test:bundle`(3) 全绿，`npm run build` 通过。**构建产物客观核对**：`dist/assets/index-*.css` 内含 `.front-content .row-actions{display:flex;flex-wrap:wrap;align-items:center;gap:var(--space-8)}` 与 `.front-content .row-actions .el-button+.el-button{margin-left:0}`（特异性 0-4-0，压得住 EP 的 0-2-0）；`dist/assets/Orders-*.js` 内含 `label:"操作",width:"140"`、`label:"单价"…"min-width":"70"`、`type:"expand","min-width":"60"`。**未做浏览器渲染验证**（UI 目视由用户自查），两列让宽后各列的实际分配是按 el-table 布局算法推算的，未经浏览器实测。
- **相关文件**: `vue/src/views/front/Orders.vue`、`vue/src/assets/css/front-pages.scss`
- **提交记录**: 未提交
- **状态**: 已修复

---

### BUG-050: 管理员可一键把任意用户的票标记为已取

- **日期**: 2026-09-29
- **Bug 描述**: `manage/Ordered.vue`（管理员订单列表）对 `待取票` 行渲染「取票」按钮，点一下就把**别人的**订单置为 `已取票`。用户提出：票是用户的、取票也是用户的事，管理员界面上不该有这颗按钮。
- **根因分析**: 权限判定把"能不能访问这一行"和"能不能执行取票这个动作"混成了一件事。
  1. `ensureOrderAccess`（`OrderedService.java:519-521`）对 `ADMIN` **直接 `return`**，不做任何归属校验 —— 管理员对任意订单都有访问权，这是"管理所有数据"该有的。
  2. `pickupOrder`（`:469-484`）却只做了一条 `if ("USER".equals(role)) throw` 的反向判断，于是 `ADMIN` 与 `CINEMA` 一起被放行。ADMIN 的**访问权**因此被顺带翻译成了"替任意用户确认取票"的**操作权**。
  3. 三层放大：① 该方法**不记录操作人**（只写 `status`），伪造后查不出是谁点的；② `已取票` 是终态、没有任何出口（`redeemByCode` 对已取票直接拒、`cancelOrder` 的前置是 `待支付`），所以这个能力**连纠错用途都没有**，只能提前截胡真实柜台的交付；③ 它会**顺带伪造评价资格** —— `MarkService.add` 的门槛正是"该用户对该影片有已取票订单"（`countPickedUpByUserAndFilm`），管理员一点，那个用户就凭空获得给这部片打分的资格。
  4. 旁证（说明它并非有意设计）：管理端订单页**只**接了 `PICKUP` 一个状态操作，金额更大的 退款 / 取消 都没接（`REFUND`/`CANCEL` 只出现在 `front/` 两页）—— 钱不动的取票给了管理员、钱动的退款不给，本身就不自洽。
- **解决方案**:
  1. `pickupOrder` 改为**只放行 `CINEMA` 的白名单**：`if (!"CINEMA".equals(role)) throw FORBIDDEN`。**不要**改成"再补一条拒 ADMIN" —— denylist 在新增角色时会静默把取票能力一并授予新角色，正是 `MarkService` 那轮"只有前端按钮在守"的同一种漏。`CINEMA` 保留，因为它本来就被 `ensureOrderAccess` 限制在**本影院的**订单上，是真实的柜台员工。
  2. `manage/Ordered.vue` 删掉按钮、`pickupOrder()` 处理器与随之失效的 `ORDER_API` 导入。**只删按钮是错解**（会退化成"只有前端在守"）—— 权限落点始终是服务端的这一个方法：`AuthInterceptor` 的 `ADMIN_ONLY_PREFIXES`/`ADMIN_WRITE_PREFIXES` 都不含 `/orders`（只要求登录），`OrderedController.pickup` 只做转发。
  3. `back/Ordered.vue`（影院端）不动 —— 影院端本来就该有，且受 `cinemaId` 约束。
  4. 错误文案改成有指向性的"取票为影院柜台操作，请到取票大厅凭取票码自助取票"，把用户导向免登录的自助通路。
- **代价（明确接受）**: 管理员从此不能代客取票。本系统里不算损失 —— "用户到店取票"由取票大厅覆盖（免登录、凭码），"柜台取票"由影院端覆盖；唯一受影响的是演示时想用 admin 账号把某条订单推到 `已取票`，改用取票大厅的码即可，反而把真实自助通路演到位。**未新增**"admin 只读看取票码"之类的补救功能（admin 要查码需先有正当场景，真有需求再单独提）。
- **验证**: `mvn test` **173/173 全绿**（13 个测试类，Failures 0 / Errors 0）—— `adminPickupOrderSuccessfully` 反转为 `adminCannotPickupOrder`（断言 FORBIDDEN 且 `verify(..., never()).updateById(any())` 守住"拒绝时不写库"）；`userCannotPickupOrder` 的断言随文案从匹配"无权"改为匹配"影院柜台"；`cinemaPickupOrderSuccessfully` 继续守住影院端不受影响。前端 `npm run build` 通过 + `test:tokens`/`test:inline`/`test:bundle` 全绿，构建产物核对 `manage` 分块里已无 `PICKUP` 调用。**未做浏览器渲染验证**（UI 目视由用户自查）。
- **相关文件**: `OrderedService.java`、`OrderedServiceTest.java`、`vue/src/views/manage/Ordered.vue`、`CLAUDE.md`
- **提交记录**: 未提交
- **状态**: 已修复

---

### BUG-051: 点赞并发时回读到旧快照，返回了与事实相反的 liked

- **日期**: 2026-09-29
- **Bug 描述**: 多个请求几乎同时点赞同一条评价时，库里正确只留下 1 行（主键去重生效），但其中一部分响应报 `liked=false` / `likeCount=0`。前端把响应当权威值写回该行，于是"刚点上的赞"显示成没点上，刷新才恢复。真库复现（`scripts/verify/p5-mark-like.py` 并发段）：5 个并发响应里 **4 个**报 `liked=false`。
- **根因分析**: `MarkService.setLike` 承诺"回读写库后的权威状态"，但它用的是**一致读**（`SELECT ... COUNT(*)`），而 MySQL 默认隔离级别 REPEATABLE READ 让一个事务的所有一致读共享**同一条快照**，该快照在事务的**第一条一致读**时就已经固定。`setLike` 的第一条一致读是 `requireExisting(markId)`（`selectById`）；随后 `insertIfAbsent` 撞上另一个**尚未提交**的同键事务时会**阻塞到对方提交之后**才返回 —— 快照却仍是那条早于对方提交的旧快照。此后再 `COUNT`，读到的还是"没有这一行"。**库里有行、回读说没有**，与方法自己的承诺正好相反。这不是主键去重的问题（去重是对的），是"回读"这一步读到的不是当前状态。
- **解决方案**: `setLike` 显式声明 `@Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)`（`MarkService.java`）。READ_COMMITTED 下每条语句取**最新已提交**快照，回读才是本方法真正需要的"权威状态"。**刻意不用锁定读（`SELECT ... FOR UPDATE`）来纠正**：那会锁住该评价行，把同一部片子上所有人的点赞串行化 —— 用一个写热点换一次回读，代价不成比例。
- **验证**: `mvn test` **193/193 全绿**（14 个测试类，Failures 0 / Errors 0）—— 但**隔离级别 Mockito 验不了**（打桩后测的是桩，不是事务边界），真正守住这条的是真库脚本 `scripts/verify/p5-mark-like.py`：**74/74 断言**（连跑两次），并发段 5 个响应一致报 `liked=true` / `likeCount=1`。修复前该段稳定复现 4/5 报 `liked=false`。
- **相关文件**: `xm_film/springboot/src/main/java/com/example/springboot/service/MarkService.java`、`scripts/verify/p5-mark-like.py`
- **提交记录**: `cade086d`
- **状态**: 已修复

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
12. **MyBatis `<if>` null 语义**: UPDATE 语句中用 `<if test="field != null">` 包裹字段时，Java 显式设为 `null` 会导致该字段被跳过不更新。若需要允许将字段设为 `null`，应移除 `<if>` 包装
13. **CORS 生产安全**: 生产环境 CORS 禁止使用 `*` 通配符，应使用环境变量白名单精确控制允许的域名
14. **JDBC 编码**: MySQL JDBC 连接 URL 必须显式指定 `useUnicode=true&characterEncoding=utf-8`，防止生产环境中文乱码
15. **业务异常不是 HTTP 错误**: 本仓 `GlobalExceptionHandler` 返回的业务异常是 **HTTP 200 + body 里的 `code`**（400/404/409…），只有 `AuthInterceptor` 才直写 401/403。写接口测试或前端判断时**必须读响应体的 `code`**，拿 HTTP 状态码当业务结果会让"预期失败"的用例全部误判为失败（BUG-048 的验证脚本就踩了这个）
15. **Docker 卷初始化**: Docker 部署中首次挂载的命名卷为空，需要 entrypoint 脚本检测并自动填充种子数据（Docker 层已于 2026-09-27 移除，本项目改为纯本地运行，该项不再适用）
16. **静态资源缓存**: 替换静态资源后需设置 `Cache-Control: no-cache` 防止浏览器缓存旧版本。原先配在 `nginx.conf`（已于 2026-09-27 移除）；本地开发由 Spring 静态资源处理器服务 `/files/**`，如需防缓存可设 `spring.web.resources.cache.period=0`
17. **映射结构选择**: 文件映射关系使用 `Object` 存储时同名 key 会覆盖，应使用 `Array<[源, 目标]>` 支持一源多目标
18. **角色权限校验范围**: 资源控制器的角色校验应区分读写操作——读操作放行 USER，写操作保持 CINEMA/ADMIN 权限保护
19. **字段单位以数据库列注释为准**: `film.box_office` 单位是**万元**而非元（见 `schema.sql` 列注释）。前端做数值格式化前先查列注释，否则整站数值可能差 10000 倍
20. **关联字段以后端返回为准**: 影片的 `areaName` 与 `typeList` 已由 SQL `JOIN` 和 `fillFilmTypes` 解析好。前端不得再维护同名硬编码字典，也不得猜测字段形状（`Film` 实体没有 `types` 字段，`typeIds` 是数组不是 JSON 字符串）
21. **状态映射与格式化函数集中维护**: 影片状态色、订单状态色、票房格式化统一放 `constants/index.js` 与 `utils/format.js`，视图内不再复制实现（本次清理了 5 处状态 switch、3 处透传包装、4 处票房格式化副本）
22. **关联关系只留一个数据源**: "影院上映哪些影片"由排片 `record` 派生，不要再维护第二张关联表（原 `cinema_film` 无写入入口，必然与排片漂移，造成前台看不到新建场次）
23. **父数据禁止级联删除**: 交易凭证（`ordered`）引用的影片/影院/影厅/场次/用户一律用 `ON DELETE RESTRICT` 兜底，删除接口再做引用计数校验给出可读提示；下架语义用 `status` 而非物理删除
24. **派生状态不落库**: 凡是能由时间/其他字段算出的状态（如场次的未开始/放映中/已结束）一律运行时计算，表字段只保留无法推导的人工开关（`status` = 正常/停售）
25. **公开页面依赖的接口必须在白名单内**: 新增公开页面时，先确认其调用的所有 GET 接口都在 `AuthInterceptor.PUBLIC_READ_PREFIXES` 中，否则匿名访问会 401
26. **必填外键要给到数据库约束**: 关键关联字段（如 `record.film_id`）应声明 `NOT NULL`，并在服务层校验后回填冗余字段（如影片名），避免只有应用层约定导致的脏数据
27. **实体字段可空性必须与 `<if test="X != null">` 守卫一致**: 原始类型（`double`/`int`）经 OGNL 取值恒非 null，"只更新非空字段"会退化成"用 0 覆盖"。金额/计数/比率类字段一律用包装类型（`Double`/`Integer`）。同类地雷 `Film.boxOffice` 已于 P3 一并改为 `Double` 清除（`film.box_office` 有 `DEFAULT 0.0`，新增影片不受影响）
28. **事务方法内不得"先写入再抛异常"表达失败**: `rollbackFor = Exception.class` 会把刚写入的状态一起回滚（见 BUG-035）。失败用返回值（枚举/结果对象）传出，由控制器翻译成错误码；这类缺陷会被定时任务掩盖，只能靠单元测试或代码审查发现
29. **资源占用状态集合只留一处**: 占用座位的状态集合定义在 `OrderedMapper.countSeatInUse` / `selectActiveByRecordId`（`NOT IN ('已取消','已退票')`）。新增任何"释放资源"的状态时必须同步这两处，否则座位永远锁死
30. **容量/尺寸限制必须数据驱动**: 写死的 8×8 选座图与 `[1-8]排[1-8]座` 正则会让他厅配置直接不可用。容量随实体列走（`room.seat_rows`/`seat_cols`），后端按实体校验、前端只负责渲染
31. **派生字段不接受前端输入**: 影厅的影院名、排片的影片名等冗余字段一律由后端按外键回填。前端可提供输入框会造成同一事实的两份数据长期漂移（`room.title` 与 `cinema.name` 在种子数据里就已经不一致）
32. **拦截器排除表就是"角色盲区"**: `excludePathPatterns` 里的路径不执行 `AuthInterceptor`，request 上没有 `role`/`userId`。凡是要在控制器里做角色判断的端点，绝不能被排除；公开访问统一交给 `PUBLIC_READ_PREFIXES`（见 BUG-036）
33. **令牌失效不得把公开内容变成"必须登录"**: 携带无法解析的令牌访问公开只读资源时按匿名放行（`AuthInterceptor.isAnonymousRead`）。否则前端 401 处理会把游客从公开页踢去登录页
34. **评分只有一个数值来源**: `mark.score` 是影片评分的唯一数值来源，`film.score` 由该片评价均分回写（没有评价时保留基线分，不归零）。写评价的唯一入口是 `MarkService`，增删改后统一重算；种子数据用同一条 SQL 规则（`EXISTS` 守卫）保证新库与增量库结果一致
35. **"同一主体对同一目标"要显式去重**: 一个用户对一部影片只能有一条评价（`MarkMapper.countByUserAndFilm` 拦截），否则单人反复评分即可带偏均分。评价人只认 JWT 里的 `userId`，请求体里的同名字段一律忽略
36. **审核状态要同时落到"能否登录"和"是否公开"两条路径**: 影院未审核时既不可登录（`CinemaService.login`）也不出现在公开列表（`CinemaMapper.selectByFilmId` 的 `approvedOnly`，管理员豁免）。只做其一就会出现"审核前就能用"或"审核后仍看不见"
37. **词表以数据库真实取值为准**: 影院审核状态只有 `未审核`/`已审核`（后端 `CinemaStatus`）。不要引入 `待审核`/`审核通过`/`审核拒绝` 等同义值 —— 每多一个同义值，过滤条件就多一处漏网
38. **余额变更只留一个入口**: 任何改余额的代码都必须走 `WalletService`（`creditRecharge`/`debitPurchase`/`creditRefund`），它统一做"行锁读余额 → 校验/变更 → 写流水"。绕过它直接用 `UserMapper.addBalance` 一定漏掉流水或校验，余额与账本必然对不上（见 BUG-037）
39. **扣款必须与业务结果同事务**: 余额扣减和订单出票写在同一个 `@Transactional` 方法里，余额不足时整体回滚 —— 订单停在待支付、`pending_timeout_at` 与座位占用都不动，用户充值后能继续支付。分两个事务做就会出现"扣了钱没出票"或"出了票没扣钱"
40. **扣余额要"行锁 + 条件更新"双保险**: `SELECT balance ... FOR UPDATE` 串行化并发，`UPDATE ... WHERE balance >= ?` 保证扣不动时影响行数为 0。只靠先查后改在并发下会把余额扣成负数（并发用例见 `scripts/verify/p4-concurrency.py`）
41. **金额一律正数校验**: 负数入账等于凭空造钱，负数扣款等于把扣款变成加钱。金额必须 > 0 且为 `BigDecimal`，不要用 `double`（见 BUG-037）
42. **充值回调端点必须幂等**: 支付网关会重试。只有「处理中」单据可流转，终态（已完成/已失败）再次回调一律返回业务冲突。中途失败只允许置终态、不得改余额（见 BUG-037）
43. **子表是资金凭证时禁止级联删除**: `recharge_order.user_id` 与 `fund_flow.user_id` 用 `ON DELETE RESTRICT`；`fund_flow` 不提供任何 update/delete 端点，账本只增不改
44. **"删除"可能是资金后门**: 任何能删掉已成交业务数据的入口，都要先问"它能不能替代某个会回滚资金的流程"。订单物理删除只允许「已取消/已退票」，否则删除就是免费退票（见 BUG-039）
45. **敏感字段不进通用查询结果**: `/api/v1/users` 是 `SELECT *` + `resultType=User`，往 `User` 实体上挂什么字段就等于公开什么字段。余额这类只应对本人可见的数据必须走独立端点按 JWT 返回，不要挂实体
46. **同步写库面测试的库名与端口**: 隔离验证一律用临时库（`DB_NAME`）+ 备用端口，不要指向开发库 `xm-film` 与本机 9090/5173；验证脚本要能反复运行（自带状态重置），否则第二次跑就会被上一次的残留数据判失败
47. **只读视图不要回传实体**: 面向"占用/状态"这类共享视图的接口，不能直接把 `SELECT *` 的实体列表发给客户端 —— 会把他人订单号、用户ID、金额一并带出去。用专门的投影 DTO，只给渲染必需字段（见 BUG-040）
48. **归属判定必须在后端按 JWT 做**: "是不是我的"不能靠下发 `userId` 让前端自己比对，那等于先泄露再要求前端自觉。归属依据只认令牌里的 `userId`，且取不到令牌用户时一律判定为非本人（见 BUG-040）
49. **提交类按钮要有在途标记**: 下单/支付/取票这类会改变状态的按钮必须带 `submitting` 在途标记并禁用，否则双击会连发两次请求，第二次被"座位已售"之类的并发拒绝，弹出与成功提示并存的错误提示（见 BUG-041）
50. **前端文案不得断言后端未发生的事**: 倒计时归零只是把弹窗收掉，真正的取消由后端定时任务完成，此时不能提示"订单已自动取消"。前端只能提示"将自动取消"，或改为中性表述（见 BUG-041）
51. **定宽容器里只留一个宽度来源**: 卡片内部一律 `width: 100%` 跟随父级。父级内容宽 = 卡片宽 − 2×padding，内层再写死一个尺寸必然溢出（认证页因此溢出 188px，且标题被挤到折行，见 BUG-042）。抽出共用外壳时同步加终态断言，避免下次再分叉
52. **居中容器不要用 `height: 100vh` + `overflow: hidden` + 绝对定位**: 视口一变矮，内容就被裁掉且无法滚动。用 `min-height: 100vh` + flex 居中，内容超高时整页滚动（见 BUG-042）
53. **字符串形式的图标 prop 不会被解析**: `@element-plus/icons-vue` 未全局注册（`main.js` 无 `app.component`），`prefix-icon="User"` 只会渲染空白。一律用组件绑定 `:prefix-icon="User"`（见 BUG-043）
54. **表单控件的可访问名称不能只靠 placeholder**: placeholder 一输入即消失，既不构成可访问名称也不是持久提示。用可见 `label`（`label-position="top"`）或 `aria-label`（见 BUG-043）
55. **回车提交只在输入框上接一次，并挂 `@submit.prevent` 兜底**: 不要指望浏览器隐式提交（多字段表单会放弃它）；也不要同时在输入框与 `<form>` 上各绑一处，否则一次回车发两次请求（见 BUG-043）
56. **不要把最少人用的角色设为登录页默认值**: 默认 `ADMIN` 会让普通用户忘记切换时鉴权失败甚至误登管理员。默认值取最常见角色（`USER`），让失败模式是"明确报角色不匹配"而不是"进错后台"（见 BUG-044）
57. **往背景图上放文字必须先解决底衬**: 同一段文字在浅色插画上深浅两头都不到 4.5:1（`#ccc` 1.41:1、`#606266` 2.71:1）。要么给半透明面板兜底，要么不放文字 —— 换颜色解决不了（见 BUG-045）
58. **手写业务数据迟早露馅，种子只放基础配置**: 订单/评价/场次这类"一整套互相印证"的数据不要用 `INSERT` 预置 —— 单号格式、单价快照、支付凭证、余额扣减、资金流水任意一处对不上就能被查出来。演示数据一律走真实接口生成（见 BUG-046 的 `scripts/seed-demo-data.py`）
59. **派生指标不要留成静态列**: `film.box_office` 这种"人工填、没人重算"的列，迟早变成没有来源的数字并被当成真实数据展示。要么按业务表实时聚合，要么就让它是空的。**改这类指标时先 grep 一遍有没有任何代码在重算它**（见 BUG-046）
60. **表格操作列必须显式定宽，多按钮格用 flex + gap 排**: `el-table` 给未指定 `width` 的列按 `minWidth || 80` 起算、再均分富余空间，列多的表操作列只会分到 ~80px；两个文字按钮（`继续支付 + 取消` 需 104px）必然折行，而 EP 的按钮间距是 `.el-button + .el-button{margin-left:12px}` —— **折行不改变它**，第二个按钮被右推 12px，两行就左右错开。操作列一律写 `width`，多按钮格套 `.row-actions`（`front-pages.scss`：flex + gap，已把该 margin 中和为 0）。加宽所需的像素尽量从"内容本就不需要 80px"的列上让出（展开列、2 字表头的列），别让表格最小总宽上涨 —— 否则窄视口会凭空多出横向滚动条（见 BUG-049）
61. **"能访问这一行"不等于"能做这个动作"**: `ADMIN` 靠 `ensureOrderAccess` 的早退拿到**任意订单**的访问权（管理数据本该如此），但 `pickupOrder` 把它顺带翻译成了操作权，于是变成"一键把任意用户的票记为已取"。判断这类权限先问一句"这个动作记录的是谁的物理事实、谁能如实断言" —— 取票只有放映该场次的影院能断言，所以只放行 `CINEMA`。再叠加"不记录操作人"和"目标状态是终态无出口"，这种能力连纠错价值都没有，只剩伪造。**权限判断一律写白名单**（`if (!"CINEMA".equals(role))`），denylist 会在新增角色时静默扩权（见 BUG-050）
62. **"回读权威状态"只在读是"当前读"时才权威**: REPEATABLE READ 下同一事务的一致读共享一条**在第一条读时固定**的快照；若中间有一次写入撞上并发事务的提交而阻塞（如 `INSERT ... ON DUPLICATE KEY` 撞同键的未提交事务），阻塞结束后的一致读**看不到**那条刚提交的写 —— 于是"库里有、回读说没有"。凡"写后回读"的语义要求读到最新状态，必须显式把该方法降到 `READ_COMMITTED`，或改用锁定读（后者以串行化为代价）。兄弟先例：`WalletService` 的 `SELECT ... FOR UPDATE` 是**当前读**，天然免疫（见 BUG-051）
