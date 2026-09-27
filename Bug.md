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
