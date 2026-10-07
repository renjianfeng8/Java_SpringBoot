# Bug 修复记录

记录项目中已修复的 Bug，避免重复踩坑。
遇到相似问题时，优先查阅本文档。

注：项目中的 Playwright E2E 测试已于 2026-09-27 整体移除。
以下历史条目中凡提及 E2E / Playwright 的根因分析与结论均按当时情况原样保留，
仅供追溯；其中与框架无关的经验（如 BUG-018/019 的导航时序结论）仍然适用于前端开发。

注：项目中的本地工具脚本已于 2026-09-29 整体移除 —— 一键启动脚本 `scripts/start-dev.bat`、
演示数据生成脚本 `scripts/seed-demo-data.py`、`scripts/verify/` 下的真库验证脚本，
以及 `xm_film/vue/tests/` 下的前端守卫测试（`scripts/` 与 `xm_film/vue/tests/` 两个目录已随之删除）。
以下历史条目中凡提及这些脚本的验证记录与结论均按当时情况原样保留，仅供追溯；
其中与工具无关的经验（如"手写业务数据迟早露馅""Mockito 打桩测不到 SQL 谓词"等）仍然适用。

注：项目中的增量迁移脚本已于 2026-09-29 整体移除 —— `xm_film/sql/migration-*.sql` 共 8 个文件
（自动建列、给存量订单回填取票码、收敛影院审核词表等）已从工作区删除，`xm_film/sql` 只保留
`schema.sql` / `data.sql` / `init.sql` 一条全新安装口径；已存在的库改为 `DROP DATABASE` 后重建
（见 `xm_film/sql/README.md`）。以下历史条目中凡提及这些迁移脚本的「相关文件」与验证记录均按
当时情况原样保留，仅供追溯；需要脚本本身时用 `git log --all -- xm_film/sql/migration-*.sql` 取回。

## 记录格式

每条 Bug 记录包含：
- Bug 描述: 问题现象
- 根因分析: 为什么会发生
- 解决方案: 如何修复
- 相关文件: 涉及的文件路径
- 提交记录: 对应的 Git commit

提交信息的写法规范见 [CONTRIBUTING.md](CONTRIBUTING.md#一提交信息规范)；本节的「提交记录」只是指向该 Bug 的 commit 编号。

## 规则篇

本清单是"改动时必须遵守的硬约束"的唯一落点。反引号内为代码标识，`（见 BUG-0XX）` 指向下方案例篇的对应条目。
[CLAUDE.md · 硬约束索引](CLAUDE.md#硬约束索引) 只保留一行摘要并链到这里，不重复正文 —— 新增教训只改本段。

1. 数据库初始化: 新环境部署时务必执行 `xm_film/sql/init.sql`（或依次执行 `schema.sql` + `data.sql`）
2. 代理环境变量: 本地开发测试时注意 `http_proxy`/`https_proxy` 是否会影响 `localhost` 请求
3. Playwright 变量类型: `isVisible()` 返回 `boolean`，`locator()` 返回 `Locator`，不可混用
4. 异常日志: `RuntimeException` 子类构造函数需调用 `super(message)` 以确保 `getMessage()` 可用
5. JWT Token: 所有需认证的后端 API 测试务必先获取 token 并传入请求头
6. 密码明文兼容: `data.sql` 中使用明文密码时，`login()` / `updatePassword()` 需保留 BCrypt 明文回退逻辑
7. JS .toFixed() 类型: `.toFixed()` 返回 `string` 而非 `number`，数值运算需用 `parseFloat()` 包裹
8. API 路径前导斜杠: axios GET 请求路径必须以 `/` 开头（如 `'/film/selectAll'`），否则拼接 baseURL 后路径错误
9. SQL 列名一致: MyBatis XML 中 ORDER BY/INSERT/UPDATE 的列名必须与数据库实际列名一致（snake_case），不能依赖 `map-underscore-to-camel-case` 自动映射（该配置仅对 SELECT 结果映射生效）
10. E2E 路由跳转: 页面跳转（登录/搜索等）使用 `window.location.href` 而非 `router.push`，确保在 Playwright headless 模式下可靠触发导航
11. 依赖兼容性: Spring Boot 3.3.x (Spring 6.1.x) 项目引入依赖时需确认其不引用已移除的 Spring 类（如 `LiteWebJarsResourceResolver`）
12. MyBatis `<if>` null 语义: UPDATE 语句中用 `<if test="field != null">` 包裹字段时，Java 显式设为 `null` 会导致该字段被跳过不更新。若需要允许将字段设为 `null`，应移除 `<if>` 包装
13. CORS 生产安全: 生产环境 CORS 禁止使用 `*` 通配符，应使用环境变量白名单精确控制允许的域名
14. JDBC 编码: MySQL JDBC 连接 URL 必须显式指定 `useUnicode=true&characterEncoding=utf-8`，防止生产环境中文乱码
15. 业务异常不是 HTTP 错误: 本仓 `GlobalExceptionHandler` 返回的业务异常是 HTTP 200 + body 里的 `code`（400/404/409…），只有 `AuthInterceptor` 才直写 401/403。写接口测试或前端判断时必须读响应体的 `code`，拿 HTTP 状态码当业务结果会让"预期失败"的用例全部误判为失败（BUG-048 的验证脚本就踩了这个）
16. Docker 卷初始化: Docker 部署中首次挂载的命名卷为空，需要 entrypoint 脚本检测并自动填充种子数据（Docker 层已于 2026-09-27 移除，本项目改为纯本地运行，该项不再适用）
17. 静态资源缓存: 替换静态资源后需设置 `Cache-Control: no-cache` 防止浏览器缓存旧版本。原先配在 `nginx.conf`（已于 2026-09-27 移除）；本地开发由 Spring 静态资源处理器服务 `/files/`，如需防缓存可设 `spring.web.resources.cache.period=0`
18. 映射结构选择: 文件映射关系使用 `Object` 存储时同名 key 会覆盖，应使用 `Array<[源, 目标]>` 支持一源多目标
19. 角色权限校验范围: 资源控制器的角色校验应区分读写操作——读操作放行 USER，写操作保持 CINEMA/ADMIN 权限保护
20. 字段单位以数据库列注释为准: `film.box_office` 单位是万元而非元（见 `schema.sql` 列注释）。前端做数值格式化前先查列注释，否则整站数值可能差 10000 倍
21. 关联字段以后端返回为准: 影片的 `areaName` 与 `typeList` 已由 SQL `JOIN` 和 `fillFilmTypes` 解析好。前端不得再维护同名硬编码字典，也不得猜测字段形状（`Film` 实体没有 `types` 字段，`typeIds` 是数组不是 JSON 字符串）
22. 状态映射与格式化函数集中维护: 影片状态色、订单状态色、票房格式化统一放 `constants/index.js` 与 `utils/format.js`，视图内不再复制实现（本次清理了 5 处状态 switch、3 处透传包装、4 处票房格式化副本）
23. 关联关系只留一个数据源: "影院上映哪些影片"由排片 `record` 派生，不要再维护第二张关联表（原 `cinema_film` 无写入入口，必然与排片漂移，造成前台看不到新建场次）
24. 父数据禁止级联删除: 交易凭证（`ordered`）引用的影片/影院/影厅/场次/用户一律用 `ON DELETE RESTRICT` 兜底，删除接口再做引用计数校验给出可读提示；下架语义用 `status` 而非物理删除
25. 派生状态不落库: 凡是能由时间/其他字段算出的状态（如场次的未开始/放映中/已结束）一律运行时计算，表字段只保留无法推导的人工开关（`status` = 正常/停售）
26. 公开页面依赖的接口必须在白名单内: 新增公开页面时，先确认其调用的所有 GET 接口都在 `AuthInterceptor.PUBLIC_READ_PREFIXES` 中，否则匿名访问会 401
27. 必填外键要给到数据库约束: 关键关联字段（如 `record.film_id`）应声明 `NOT NULL`，并在服务层校验后回填冗余字段（如影片名），避免只有应用层约定导致的脏数据
28. 实体字段可空性必须与 `<if test="X != null">` 守卫一致: 原始类型（`double`/`int`）经 OGNL 取值恒非 null，"只更新非空字段"会退化成"用 0 覆盖"。金额/计数/比率类字段一律用包装类型（`Double`/`Integer`）。同类地雷 `Film.boxOffice` 已于 P3 一并改为 `Double` 清除（`film.box_office` 有 `DEFAULT 0.0`，新增影片不受影响）
29. 事务方法内不得"先写入再抛异常"表达失败: `rollbackFor = Exception.class` 会把刚写入的状态一起回滚（见 BUG-035）。失败用返回值（枚举/结果对象）传出，由控制器翻译成错误码；这类缺陷会被定时任务掩盖，只能靠单元测试或代码审查发现
30. 资源占用状态集合只留一处: 占用座位的状态集合定义在 `OrderedMapper.countSeatInUse` / `selectActiveByRecordId`（`NOT IN ('已取消','已退票')`）。新增任何"释放资源"的状态时必须同步这两处，否则座位永远锁死
31. 容量/尺寸限制必须数据驱动: 写死的 8×8 选座图与 `[1-8]排[1-8]座` 正则会让他厅配置直接不可用。容量随实体列走（`room.seat_rows`/`seat_cols`），后端按实体校验、前端只负责渲染
32. 派生字段不接受前端输入: 影厅的影院名、排片的影片名等冗余字段一律由后端按外键回填。前端可提供输入框会造成同一事实的两份数据长期漂移（`room.title` 与 `cinema.name` 在种子数据里就已经不一致）
33. 拦截器排除表就是"角色盲区": `excludePathPatterns` 里的路径不执行 `AuthInterceptor`，request 上没有 `role`/`userId`。凡是要在控制器里做角色判断的端点，绝不能被排除；公开访问统一交给 `PUBLIC_READ_PREFIXES`（见 BUG-036）
34. 令牌失效不得把公开内容变成"必须登录": 携带无法解析的令牌访问公开只读资源时按匿名放行（`AuthInterceptor.isAnonymousRead`）。否则前端 401 处理会把游客从公开页踢去登录页
35. 评分只有一个数值来源: `mark.score` 是影片评分的唯一数值来源，`film.score` 由该片评价均分回写（没有评价时保留基线分，不归零）。写评价的唯一入口是 `MarkService`，增删改后统一重算；种子数据用同一条 SQL 规则（`EXISTS` 守卫）保证新库与增量库结果一致
36. "同一主体对同一目标"要显式去重: 一个用户对一部影片只能有一条评价（`MarkMapper.countByUserAndFilm` 拦截），否则单人反复评分即可带偏均分。评价人只认 JWT 里的 `userId`，请求体里的同名字段一律忽略
37. 审核状态要同时落到"能否登录"和"是否公开"两条路径: 影院未审核时既不可登录（`CinemaService.login`）也不出现在公开列表（`CinemaMapper.selectByFilmId` 的 `approvedOnly`，管理员豁免）。只做其一就会出现"审核前就能用"或"审核后仍看不见"
38. 词表以数据库真实取值为准: 影院审核状态只有 `未审核`/`已审核`（后端 `CinemaStatus`）。不要引入 `待审核`/`审核通过`/`审核拒绝` 等同义值 —— 每多一个同义值，过滤条件就多一处漏网
39. 余额变更只留一个入口: 任何改余额的代码都必须走 `WalletService`（`creditRecharge`/`debitPurchase`/`creditRefund`），它统一做"行锁读余额 → 校验/变更 → 写流水"。绕过它直接用 `UserMapper.addBalance` 一定漏掉流水或校验，余额与账本必然对不上（见 BUG-037）
40. 扣款必须与业务结果同事务: 余额扣减和订单出票写在同一个 `@Transactional` 方法里，余额不足时整体回滚 —— 订单停在待支付、`pending_timeout_at` 与座位占用都不动，用户充值后能继续支付。分两个事务做就会出现"扣了钱没出票"或"出了票没扣钱"
41. 扣余额要"行锁 + 条件更新"双保险: `SELECT balance ... FOR UPDATE` 串行化并发，`UPDATE ... WHERE balance >= ?` 保证扣不动时影响行数为 0。只靠先查后改在并发下会把余额扣成负数（并发用例见 `scripts/verify/p4-concurrency.py`）
42. 金额一律正数校验: 负数入账等于凭空造钱，负数扣款等于把扣款变成加钱。金额必须 > 0 且为 `BigDecimal`，不要用 `double`（见 BUG-037）
43. 充值回调端点必须幂等: 支付网关会重试。只有「处理中」单据可流转，终态（已完成/已失败）再次回调一律返回业务冲突。中途失败只允许置终态、不得改余额（见 BUG-037）
44. 子表是资金凭证时禁止级联删除: `recharge_order.user_id` 与 `fund_flow.user_id` 用 `ON DELETE RESTRICT`；`fund_flow` 不提供任何 update/delete 端点，账本只增不改
45. "删除"可能是资金后门: 任何能删掉已成交业务数据的入口，都要先问"它能不能替代某个会回滚资金的流程"。订单物理删除只允许「已取消/已退票」，否则删除就是免费退票（见 BUG-039）
46. 敏感字段不进通用查询结果: `/api/v1/users` 是 `SELECT *` + `resultType=User`，往 `User` 实体上挂什么字段就等于公开什么字段。余额这类只应对本人可见的数据必须走独立端点按 JWT 返回，不要挂实体
47. 同步写库面测试的库名与端口: 隔离验证一律用临时库（`DB_NAME`）+ 备用端口，不要指向开发库 `xm-film` 与本机 9090/5173；验证脚本要能反复运行（自带状态重置），否则第二次跑就会被上一次的残留数据判失败
48. 只读视图不要回传实体: 面向"占用/状态"这类共享视图的接口，不能直接把 `SELECT *` 的实体列表发给客户端 —— 会把他人订单号、用户ID、金额一并带出去。用专门的投影 DTO，只给渲染必需字段（见 BUG-040）
49. 归属判定必须在后端按 JWT 做: "是不是我的"不能靠下发 `userId` 让前端自己比对，那等于先泄露再要求前端自觉。归属依据只认令牌里的 `userId`，且取不到令牌用户时一律判定为非本人（见 BUG-040）
50. 提交类按钮要有在途标记: 下单/支付/取票这类会改变状态的按钮必须带 `submitting` 在途标记并禁用，否则双击会连发两次请求，第二次被"座位已售"之类的并发拒绝，弹出与成功提示并存的错误提示（见 BUG-041）
51. 前端文案不得断言后端未发生的事: 倒计时归零只是把弹窗收掉，真正的取消由后端定时任务完成，此时不能提示"订单已自动取消"。前端只能提示"将自动取消"，或改为中性表述（见 BUG-041）
52. 定宽容器里只留一个宽度来源: 卡片内部一律 `width: 100%` 跟随父级。父级内容宽 = 卡片宽 − 2×padding，内层再写死一个尺寸必然溢出（认证页因此溢出 188px，且标题被挤到折行，见 BUG-042）。抽出共用外壳时同步加终态断言，避免下次再分叉
53. 居中容器不要用 `height: 100vh` + `overflow: hidden` + 绝对定位: 视口一变矮，内容就被裁掉且无法滚动。用 `min-height: 100vh` + flex 居中，内容超高时整页滚动（见 BUG-042）
54. 字符串形式的图标 prop 不会被解析: `@element-plus/icons-vue` 未全局注册（`main.js` 无 `app.component`），`prefix-icon="User"` 只会渲染空白。一律用组件绑定 `:prefix-icon="User"`（见 BUG-043）
55. 表单控件的可访问名称不能只靠 placeholder: placeholder 一输入即消失，既不构成可访问名称也不是持久提示。用可见 `label`（`label-position="top"`）或 `aria-label`（见 BUG-043）
56. 回车提交只在输入框上接一次，并挂 `@submit.prevent` 兜底: 不要指望浏览器隐式提交（多字段表单会放弃它）；也不要同时在输入框与 `<form>` 上各绑一处，否则一次回车发两次请求（见 BUG-043）
57. 不要把最少人用的角色设为登录页默认值: 默认 `ADMIN` 会让普通用户忘记切换时鉴权失败甚至误登管理员。默认值取最常见角色（`USER`），让失败模式是"明确报角色不匹配"而不是"进错后台"（见 BUG-044）
58. 往背景图上放文字必须先解决底衬: 同一段文字在浅色插画上深浅两头都不到 4.5:1（`#ccc` 1.41:1、`#606266` 2.71:1）。要么给半透明面板兜底，要么不放文字 —— 换颜色解决不了（见 BUG-045）
59. 手写业务数据迟早露馅，种子只放基础配置: 订单/评价/场次这类"一整套互相印证"的数据不要用 `INSERT` 预置 —— 单号格式、单价快照、支付凭证、余额扣减、资金流水任意一处对不上就能被查出来。演示数据一律走真实接口生成（见 BUG-046 的 `scripts/seed-demo-data.py`）
60. 派生指标不要留成静态列: `film.box_office` 这种"人工填、没人重算"的列，迟早变成没有来源的数字并被当成真实数据展示。要么按业务表实时聚合，要么就让它是空的。改这类指标时先 grep 一遍有没有任何代码在重算它（见 BUG-046）
61. 表格操作列必须显式定宽，多按钮格用 flex + gap 排: `el-table` 给未指定 `width` 的列按 `minWidth || 80` 起算、再均分富余空间，列多的表操作列只会分到 ~80px；两个文字按钮（`继续支付 + 取消` 需 104px）必然折行，而 EP 的按钮间距是 `.el-button + .el-button{margin-left:12px}` —— 折行不改变它，第二个按钮被右推 12px，两行就左右错开。操作列一律写 `width`，多按钮格套 `.row-actions`（`front-pages.scss`：flex + gap，已把该 margin 中和为 0）。加宽所需的像素尽量从"内容本就不需要 80px"的列上让出（展开列、2 字表头的列），别让表格最小总宽上涨 —— 否则窄视口会凭空多出横向滚动条（见 BUG-049）
62. "能访问这一行"不等于"能做这个动作": `ADMIN` 靠 `ensureOrderAccess` 的早退拿到任意订单的访问权（管理数据本该如此），但 `pickupOrder` 把它顺带翻译成了操作权，于是变成"一键把任意用户的票记为已取"。判断这类权限先问一句"这个动作记录的是谁的物理事实、谁能如实断言" —— 取票只有放映该场次的影院能断言，所以只放行 `CINEMA`。再叠加"不记录操作人"和"目标状态是终态无出口"，这种能力连纠错价值都没有，只剩伪造。权限判断一律写白名单（`if (!"CINEMA".equals(role))`），denylist 会在新增角色时静默扩权（见 BUG-050）
63. "回读权威状态"只在读是"当前读"时才权威: REPEATABLE READ 下同一事务的一致读共享一条在第一条读时固定的快照；若中间有一次写入撞上并发事务的提交而阻塞（如 `INSERT ... ON DUPLICATE KEY` 撞同键的未提交事务），阻塞结束后的一致读看不到那条刚提交的写 —— 于是"库里有、回读说没有"。凡"写后回读"的语义要求读到最新状态，必须显式把该方法降到 `READ_COMMITTED`，或改用锁定读（后者以串行化为代价）。兄弟先例：`WalletService` 的 `SELECT ... FOR UPDATE` 是当前读，天然免疫（见 BUG-051）
64. 本仓库的路由没有 `name`，导航一律走 `path`: `router/index.js` 里每条的 `meta.name` 是标题文案（'影评' / '电影详情'），不是路由名 —— 全仓没有一条路由声明过 `name`。所以 `router.push({ name: 'xxx' })` 必然在运行时抛 `No match for {"name":"xxx",...}`，冒到渲染层就是「页面渲染异常」。这个错编译、构建、三个守卫测试、dev server 的模块编译全都查不出来，只有真的点一下才炸 —— 验证"路由能解析"属于必须跑起来的那一类。新增页面时照抄邻座写法（`Movie.vue` 的 `` `/front/filmDetail/${id}` `` 或 `{ path }`）；真要用具名路由，先把 `name` 加进路由表并全仓统一（见 BUG-052）
65. 派生指标列必须只有唯一写者，且不接受客户端入参: `film.score` 与 `film.box_office` 是同一族踩了两次 —— 前者有个"只在有评价时回写"的写者、初值却由种子填死，于是 `mark` 0 行时评分榜照样有数据（见 BUG-053）。凡被判为"派生"的列：① 种子里一律写 `NULL`，别给 `DEFAULT`（`DEFAULT 0.0` 会把"没有"渲染成「0 分」，而 0 是合法的真实值）；② 通用 CRUD 的 insert/updateById 里删掉它的 `<if>` 分支 —— `@RequestBody` 整实体 + `BaseController` 的组合会让任何调用方手写这个值；③ 消费端（榜单 / 聚合查询）显式再声明一次前置条件（如 `EXISTS (mark)`），别让"某列恰好为 NULL"这条不变量独自承担正确性；④ 前端区分"无"与"0"，判定用 `== null` 而不是 falsy
66. 同一配置项只留一个读取点、一个兜底值: `VITE_API_BASE_URL` 曾有三个读取点、两套兜底（`'/'` 与 `'http://localhost:9090'`），BUG-023 修一处漏两处，直到"无 `.env` 构建"才暴露（见 BUG-054）。凡读环境变量：① 收敛到一个模块导出，其余 import；② 兜底值全仓只有一个，改的时候先 `grep` 旧值；③ 兜底值的正确性必须在变量缺失的条件下验证 —— 本机通常有 `.env`，Vite 会把 `"/" || '字面量'` 常量折叠掉，`grep dist` 查不到那个字面量，只能显式跑一次 `VITE_API_BASE_URL= npm run build`。另：把相对路径与 base 直接字符串相加时，同源 base `/` 会拼出 `//host/path`（协议相对 URL，指向名为 `host` 的主机）—— 拼之前先想 base 会不会是 `/`
67. 取票码不要自己的有效/失效状态: `ordered.pickup_code` 没有 `used`/`revoked` 之类标记列 —— 可用性完全派生自订单状态：核销只接受 `status = '待取票'`（`OrderedMapper.markPickedUpByCode` 的状态条件更新）。这一个谓词同时实现了"一单一码 / 用过即废 / 退票取消作废 / 没付款不出发"；新增标记列等于引入需要人工同步的第二处真相。有效期到放映结束（`ordered.start` + 片长，缺失按 `RecordService.DEFAULT_DURATION_MINUTES` 兜底），同样不落库。码在 `payOrder` 内与扣款同一事务生成，故未支付订单永远没有码
68. 并发去重靠"条件更新影响行数"，不靠悲观锁: 读到的状态可能已过期，但写库走 `UPDATE ... WHERE status = '待取票'`，受影响 0 行即判定被人抢先（`redeemByCode` 抛 409）。与余额扣减的 `UPDATE ... WHERE balance >= ?` 同一手法 —— 不要退回"先读后无条件写"
69. 生成收窄、输入放宽是刻意的: 取票码生成用剔除易混字符的字母表（`ABCDEFGHJKMNPQRSTUVWXYZ23456789`，去掉 I/L/O/0/1），校验却放宽到 `[A-Z0-9]` 并归一成 `XXXX-XXXX` 再等值查（`OrderedService.normalizePickupCode`）—— 历史上给存量订单补过含 0/1 的码，收窄校验会把那批码挡在门外。归一后必须保持等值查询，写成 `WHERE REPLACE(pickup_code,'-','') = ?` 会让唯一索引失效
70. 评价资格必须在服务端校验: `MarkService.add` 要求 `OrderedMapper.countPickedUpByUserAndFilm(userId, filmId) > 0`。只靠前端按钮在守，等于任何登录用户能给没买过票的影片打分。修改评价不重复校验 —— `已取票` 是终态，资格一旦成立不会被推翻
71. 去重插入用 `ON DUPLICATE KEY`，不用 `INSERT IGNORE`: `INSERT IGNORE` 把所有错误降级为警告，外键违规（如指向已被删的评价）同样只返回 `ROW_COUNT()=0`，与"已赞过"字节级相同、二者不可区分；`ON DUPLICATE KEY UPDATE mark_id = mark_id` 只吸收重复键冲突，真实外键错误照常抛 1452。端点的幂等应由"显式意图 + 主键去重"构造出来，而不是靠吞错误；响应回读权威状态而非信任受影响行数（重复插入同样报 0 行）
72. 前端空态与异常文案分工: 接口成功但无数据 → 「暂无数据」；请求失败（网络 / 超时 / 5xx）→ 「数据加载失败，请稍后重试」。失败提示由 `utils/request.js` 响应拦截器统一给出，页面内 `catch` 只落错误态、不再重复弹提示（否则同一次失败弹两次）。请求失败时显示「暂无数据」会让用户误以为系统里真没有数据
73. shell 渲染的入口必须匹配该路由的 `meta.roles`: 能通向某角色的入口就要保证该角色真能进；后端确实不服务的角色，做法是藏入口而不是放开权限（`Front.vue` 的 `showUserEntries` 只对 `!isAdmin && !isCinema` 显示 USER 专属入口）。反向的例外也要显式：免登录的自助通路（取票大厅）刻意不挂在 `showUserEntries` 之下，加上 `v-if` 就等于把唯一通路藏起来
74. 改密只认 JWT 里的角色，不认请求体: 后端改密信任 JWT 派生的请求角色，忽略 body 里的同名 role 字段，防止 `@RequestBody` 篡改越权
75. "是否已支付/占座"的状态集合有多个消费点，改动必须同源: 票房聚合（`FilmMapper.xml` 的 `filmRevenueJoin`，只统计 `待取票/已取票`）、占座判定（`OrderedMapper.countSeatInUse` / `selectActiveByRecordId`）与「今日票房」（`selectTodayPaidRevenue`，按 `pay_time` 取日）共享同一状态集合。新增或改变任一状态时这几处必须同步，否则某个口径会漏算或把座位永久锁死
76. 登录态只能经 `useAuth` 变更，不要直接动 storage: `useAuth` 用模块级 `ref` 持有当前用户（`globalUser`），storage 只是它的持久化副本。只清 storage 而不同步那个 `ref`，内存里仍留着 token，`isLoggedIn` 继续为真、外壳继续渲染成已登录 —— 幽灵登录态。凡是"模块级单例状态 + 持久化副本"的组合都有这个形状：改状态走单例的 API（`login` / `logout` / `setUser`），不要绕过去写副本（见 BUG-055）
77. `window.location.href = <来自 URL 的路径>` 是开放重定向: `'//evil.com'` 也以 `/` 开头，`startsWith('/')` 判不出来，赋值后浏览器按协议相对 URL 跳到外站。站内跳转一律走 `router.push` —— 它只解析站内路径，顺带免掉整页重载。确实要拼绝对地址时，显式排掉 `//` 前缀（见 BUG-055）
78. 组件 `scoped` 里的 `@keyframes` 会被改名，跨组件引用不到: `@vitejs/plugin-vue` 把 scoped 块里的 `@keyframes rotating` 改写成 `rotating-<组件哈希>`，只有同一个块里的 `animation` 引用能命中。另一个组件写 `animation: rotating ...` 引用它，编译与构建都不报错，动画静默失效。共享关键帧放 `global.css`；引用它的 scoped 块因为块内没有同名定义，名字不会被改写，正好命中全局那一条（见 BUG-055）
79. 共享层与组件 `scoped` 块不要用同一个类名装不同样式: `.front-content .empty-hint`（0-2-0）与 `.empty-hint[data-v-xxxx]`（0-2-0）特异性完全相同，胜负交给产物里的先后顺序 —— 换个构建顺序就换一套观感，且不报错。名字相同就该样式相同；形状不同就换名（带标题与说明的虚线面板叫 `.empty-panel`，单行占位才叫 `.empty-hint`）（见 BUG-055）
80. 压淡文字用令牌，不用 `opacity`: `opacity: .7` / `.8` 是对比度的隐性扣减，既不可核对也不达 §10.1。深底次级文字用 `--dark-text-secondary`（10.84:1），浅底次级文字用 `--el-text-color-regular`（6.11:1）。`opacity` 只用于纯装饰块（见 BUG-055）
81. 页面底色与最小高度归外壳，内容页不要再写 `min-height: 100vh`: 内容页挂在外壳内容区之内，自己再声明一次 `100vh` 会把页脚顶到视口之外（外壳头部的高度是凭空多出来的）。前台底色 `#ffffff` 由 `Front.vue` 的外壳铺（规范 §6.2），`body` 默认取的是 `--el-bg-color-page` 灰，所以外壳不铺就整个前台是灰的（见 BUG-055）
82. 导航高亮由路由归属派生，不要手工同步: 用一个 `activePath` 字符串逐段 `if-else` 的写法，漏掉一段就点错项 —— `/front/pickup` 曾落到兜底分支点亮「首页」，而它自己的高亮条件（`activePath === '/front/pickup'`）永远不成立。改成"每个导航段拥有哪些路由前缀"的映射，从 `route.path` 现算；不属于任何段的页面（只在头像下拉里的个人中心 / 修改密码）保持无高亮，而不是把「首页」点亮
83. `window.location.href` 换成 `router.push` 会改变组件复用语义，同页换 query 不再重新挂载: 改之前是整页重载，目标组件必然重建；改之后同一路由记录只换 query 时组件被复用，只写在 `onMounted` 里的取数**不会**再触发。搜索页因此会一直显示上一次的关键词与结果（`Front.vue` 的顶栏搜索正是同路由换 `?title=`）。凡"同路径不同参数"的取数入口都要 `watch` 参数本身，不能只靠 `onMounted` —— 把"整页重载"改成"站内跳转"时，必须回头检查目标页是不是靠重载来刷新的（见 BUG-055）
84. `opacity: 0` 的叠放层仍然接收点击、也仍然可聚焦: 轮播把多张 slide 都设成 `position: absolute; inset: 0; opacity: 0` 时，后置兄弟节点绘制在上层并照常命中测试 —— 点可见那张的按钮，实际命中的是最后一张透明层的链接，跳到错的条目；同时它们仍在 Tab 顺序与无障碍树里，配上 `aria-hidden` 就成了"可聚焦但不可见不可读"的焦点陷阱。用 `visibility: hidden` 一次解决两件事（既不接收指针事件、也不进 Tab 顺序），过渡写成 `opacity …, visibility …` 即可保留淡入淡出（见 BUG-055）
85. 表单字段必须有数据落点: 绑定到 `v-model` 并随整表单提交的字段，后端必须有对应列（或对应的处理分支）承接它。`manage/Person.vue` 的「个人介绍」曾绑 `data.form.description` 并 PUT 给 `/api/v1/admins`，而 `admin` 表没有 `description` 列（`cinema` 表有 —— 该页是从影院端复制改写的，字段跟着文本一起搬了过来）。用户填了、点保存、看到「更新成功」，内容静默丢弃：比字段不存在更有害，因为它让人以为填过了。该字段已按本条删除（见 BUG-056）。判断方法同规则 32 的反向 —— 规则 32 问"这个字段该不该由前端给"，本条问"前端给了之后谁接"。加字段前先 `grep` 一次 schema，跨端复制表单时要逐个字段核对目标表
86. 瞬时接口的加载反馈要有最短时长，失败不要擦掉已展示的数据: 「今日票房」这类本地聚合接口几十毫秒就返回，`loading` 直接跟随请求生命周期会让转圈一闪而过、数字无声替换，用户无从确认「点过了」。给手动刷新一个最短展示时长（约 450ms），并把 loading 的结束与结果高亮压到同一刻 —— 读起来是「转圈停 → 数字亮一下」。配套三条：① `el-button` 的 `loading` 会自己渲染一个转圈图标，默认插槽里的自定义图标不会随之消失，两者叠加就是一个按钮两个图标，要在 loading 时 `v-if` 掉自定义图标；② 刷新失败保留上次的数字（状态里的值不回退），只在次要行提示，瞬时故障不该把已知数据抹成错误文案；③ 宽字号数据（前台 `--fs-5xl` 36px 的大字）不要与"两态宽度会变"的按钮同排 —— 按钮一宽就把数字挤到折行，「元」掉到第二行再弹回，看着就是数字上下跳。数据要 `white-space: nowrap`，并让它独占一行，按钮让到次要行（见 BUG-057）
87. 实体的派生 / 只读字段必须在**每一条**返回该实体的查询路径上填充: `film.typeList` 由 `FilmService.fillFilmTypes` 后置填充，`selectAll` / `selectById` / `selectPage` / `selectByTitle` 都调了，唯独 `selectByCinema` 漏了 —— 于是影院详情页的影片永远没有类型，而影片详情页（同一实体的另一条查询路径）一切正常。后置填充不是"顺手加的加工"，它和 SQL 一样是这条路径输出契约的一部分：新增或修改任何返回该实体的查询方法时，逐个核对后置填充有没有跟着走（见 BUG-058）
88. 深色表面上的文字色受底色亮度约束，改 `--dark-bg-hero` 必须复测: `--dark-bg-hero` 与 `--dark-bg` 亮度相近是有意维持的 —— §3.5 那张对比度表同时担保两者。曾经 `--dark-bg-hero` 取 `#41036a`，亮度远高于 `#1a1a1a`，于是 `--dark-text-faint`（实测 4.21:1）与 `--color-brand`（3.81:1）在头横幅上都不达 AA，而表格看上去是达标的。头横幅上承载文字只许用 `--dark-text` / `--dark-text-secondary`；要用 `--dark-text-faint` 或 `--color-brand`，先把底色换回近黑档并复测（见 BUG-059）
89. 驾驶舱的每个数字都要有对比基准，且对比基准不得另取一次数: KPI 卡片只给一个孤立的数字等于没有信息 —— 必须带环比 / 目标 / 上期。基准取「近 7 日趋势的末点」（= 昨天），不另发请求：同一份趋势数据既画折线又算环比，两者永远对得上，也不会出现「卡片说涨、图上是跌」。除数为 0 或没有基准时渲染「较昨日 —」，不给没有意义的百分比。涨跌按**有利性**而非方向着色（门票收入涨是成功色，待处理数涨是危险色），并同时给箭头与 +/- 文字，不单靠颜色（§3.7）。趋势窗口不含今天：今天只过了一半，画进折线会让曲线在每个上午都呈现"断崖下跌"（见 BUG-060）

## 案例篇

每个 Bug 的根因、处置与相关文件，按编号排列。本段只增不改 —— 从案例中提炼出的规则写进上方规则篇。

### BUG-001: CustomException.getMessage() 返回 null

- 日期: 2026-05-14
- Bug 描述: `GlobalExceptionHandler` 捕获 `CustomException` 后，调用 `e.getMessage()` 始终返回 `null`，导致前端收到的错误消息为空
- 根因分析: `CustomException` 继承 `RuntimeException`，构造函数只设置了自定义字段 `code` 和 `msg`，但未调用 `super(msg)`，导致父类的 `getMessage()` 返回 `null`；全局异常处理使用的正是 `e.getMessage()` 而非 `e.getMsg()`
- 解决方案: `CustomException` 构造函数中追加 `super(msg)`；`GlobalExceptionHandler` 中改用 `e.getMsg()` 替代 `e.getMessage()`
- 相关文件: `xm_film/springboot/src/main/java/com/example/springboot/exception/CustomException.java`、`GlobalExceptionHandler.java`
- 提交记录: `045545e4`
- 状态: 已修复

### BUG-002: 默认管理员账号 '999' 未初始化到数据库

- 日期: 2026-05-14
- Bug 描述: `POST /login` 返回 500，提示"账号不存在"；Playwright 测试中管理员登录流程失败
- 根因分析: `admin.sql` 脚本未执行，`admin` 表中没有 username='999' 的记录；`AdminService.login()` 查询返回 `null` 后抛出异常
- 解决方案: 手动执行 `INSERT INTO admin (username, password, role, name) VALUES ('999', '999', 'ADMIN', '任建峰')`；长期方案在项目初始化文档中强调 SQL 导入步骤
- 相关文件: `xm_film/sql/data.sql`、`xm_film/springboot/src/main/java/com/example/springboot/service/AdminService.java`
- 状态: 已修复

### BUG-003: Playwright E2E 测试中变量类型错误导致搜索失败

- 日期: 2026-05-14
- Bug 描述: 前台首页搜索测试报错 `searchInput.fill is not a function`
- 根因分析: 测试代码中 `const searchInput = await page.isVisible(...)` 返回的是 `boolean` 类型，后续对该布尔值调用了 `.fill()` 方法，而 `.fill()` 是 Playwright Locator 的方法
- 解决方案: 将 `isVisible` 检查与真实 Locator 变量分离：`const searchInputLocator = page.locator(...)`，再用单独的变量存储 `isVisible` 检查结果
- 相关文件: `xm_film/vue/e2e-tests/e2e-scan.spec.mjs`
- 提交记录: `28646785`
- 状态: 已修复

### BUG-004: HTTP 代理环境变量干扰本地 API 请求

- 日期: 2026-05-14
- Bug 描述: 从 Bash 调用 `curl http://localhost:9090/getYear` 返回 502 Bad Gateway，但后端实际运行正常
- 根因分析: 系统设置了 `http_proxy=http://127.0.0.1:7890` 环境变量，curl 将本地请求也发往代理服务器，代理无法连接本地后端
- 解决方案: 使用 `curl --noproxy '*'` 绕过代理，或在测试脚本开头执行 `unset http_proxy && unset https_proxy`
- 状态: 已修复（运行测试时前置 `unset http_proxy`）

### BUG-005: 未认证请求返回 401 而非明确错误信息

- 日期: 2026-05-14
- Bug 描述: 调用需要 JWT 认证的 API（如 `/film/selectAll`）时直接返回 401 且无错误信息，排查问题时不易定位
- 根因分析: `AuthInterceptor` 在校验失败后设置 401 状态码并返回固定 JSON，但缺乏具体的角色/权限提示；`GlobalExceptionHandler` 不处理拦截器层的异常
- 解决方案: 仅在 `AuthInterceptor` 的响应 JSON 中添加文字提示即可（当前已有）：`{"code":"401","msg":"登录已过期，请重新登录"}`
- 相关文件: `xm_film/springboot/src/main/java/com/example/springboot/common/config/AuthInterceptor.java`
- 状态: 设计如此，无需修改

### BUG-006: 数据库脚本目录结构不规范（schema 与数据混放）

- 日期: 2026-05-14
- Bug 描述: `数据库/` 目录下的 14 个 SQL 文件仅含 INSERT 语句，无 CREATE TABLE 建表语句；目录名为中文，与项目其他英文命名不统一；新环境部署需逐个手动执行，缺少一键初始化入口
- 根因分析: 项目初期从数据库工具导出时仅导出 INSERT 语句，未包含表结构定义；中文目录名在跨平台/CI 中存在路径编码风险
- 解决方案:
  - 移除 `数据库/` 目录，新建 `xm_film/sql/` 英文目录
  - 新增 `schema.sql`：14 张表的完整 CREATE TABLE（含字段类型、注释、默认值）
  - 合并数据为 `data.sql`：所有初始数据按表分区、统一管理
  - 新增 `init.sql`：一键初始化入口（建库 → 建表 → 导数据）
  - 在 `src/main/resources/db/` 下放置副本，支持 `spring.sql.init` 自动初始化 —— 已于 2026-09-27 移除（`spring.sql.init.mode` 恒为 `never`，该副本从未被加载，且 `data.sql` 已与 `xm_film/sql/` 漂移）
- 相关文件:
  - `xm_film/sql/schema.sql`、`xm_film/sql/data.sql`、`xm_film/sql/init.sql`
  - `xm_film/springboot/src/main/resources/application.yml`
  - `CLAUDE.md`
- 提交记录: `9525efa4`
- 状态: 已修复

### BUG-007: 全栈批量修复 — NPE/崩溃/数据丢失/竞态条件 (BUG-003)

- 日期: 2026-05-15
- Bug 描述: 全栈扫描发现约 35 个 Bug，涵盖 NPE、崩溃、数据丢失、竞态条件等严重问题
- 根因分析: 后端 Service 中 `selectList()` 返回 `null`（前端调用无数据）；`AdminService/UserService/CinemaService` 缺少 `@Transactional`；`OrderedService.update()` 未置空 `status`；`CinemaController.selectPage` 未标注 `@RequestParam` 导致参数必填；`Account.java` 缺少 `@JsonProperty(WRITE_ONLY)` 导致密码序列化泄露；前端口令修改/个人资料页面缺少 `ElMessage` 导入、`localStorage` 解析未做 try-catch、路由路径错误等
- 解决方案:
  - 后端 11 个 Service 的 `selectList()` 改为调用 `mapper.selectAll(entity)`
  - Admin/User/Cinema Service 添加 `@Transactional(rollbackFor = Exception.class)`
  - Account/Admin/User/Cinema 实体添加 `@JsonProperty(access = WRITE_ONLY)`
  - CinemaController 添加 `@RequestParam(required = false)` 注解
  - OrderedService.update() 置空 status 防止意外更新
  - 前端口令修改/个人资料/404 页面修复 ElMessage 导入、JSON.parse 安全包装、emit 修复
  - Login.vue 补充 ElMessageBox 导入
- 相关文件: 涉及 30+ 文件（后端 11 个 Service、4 个 Entity、3 个 Controller；前端 7 个 Vue 页面）
- 提交记录: `3ba277f4`
- 状态: 已修复

### BUG-008: 后端安全加固 — RBAC/密码保护/批量赋值/事务 (BUG-004)

- 日期: 2026-05-15
- Bug 描述: 后端 API 缺少角色访问控制、密码通过 API 响应泄露、缺少批量赋值防护、部分操作无事务保护
- 根因分析: AuthInterceptor 仅验证 JWT 有效性，未做基于路径的角色校验；`@JsonProperty(WRITE_ONLY)` 仅在 Account 基类有效，子类（Admin/User/Cinema）重新声明 password 字段，覆盖了注解；`AdminService.update()` 允许通过 `@RequestBody` 更新 password；10 个 Service 无 `@Transactional`
- 解决方案:
  - AuthInterceptor 添加 `/admin/` 路径的 ADMIN 角色校验（403 拒绝非管理员）
  - 为 Admin/User/Cinema 实体类所有子类的 password 字段添加 `@JsonProperty(WRITE_ONLY)`
  - Service 层 `update()` 方法中置空 password/role，防止通过更新接口修改
  - WebController.updatePassword() 改为从 JWT 请求属性读取 userId，而非请求体传入
  - 为 10 个 Service 添加 `@Transactional(rollbackFor = Exception.class)`
- 相关文件:
  - `AuthInterceptor.java`、`WebMvcConfig.java`
  - `Account.java`、`Admin.java`、`User.java`、`Cinema.java`
  - `AdminService.java`、`UserService.java`、`CinemaService.java`、`OrderedService.java`
  - `WebController.java`
- 提交记录: `abeedd04`
- 状态: 已修复

### BUG-009: 前端 14 处 Bug — 类型转换/路由/路径/竞态条件/JSON 解析 (BUG-005)

- 日期: 2026-05-16
- Bug 描述: 前端代码扫描发现 14 个运行时/逻辑 Bug
- 根因分析:
  - `front/Home.vue:294` — `.toFixed()` 返回 string 而非 number，导致后续数值运算类型混淆
  - `Front.vue:13` — `<router-link to="home">` 使用相对路径，路由匹配失败
  - `manage/Cinema.vue:339` — 影院状态映射反向：`已审批 → 未审核`
  - `back/Room.vue:50` — `el-form-item prop="title"` 与 `v-model="data.form.name"` 不匹配，表单验证失效
  - `Front/Back/Manage.vue` — 头像地址使用 `https://your-domain.com` 占位域名
  - `front/Movie.vue:124,143,153,163` — API 路径缺少前导 `/`
  - `front/BuyTicket.vue:253` — `watchEffect` 无响应式依赖，等价于普通函数调用
  - `back/Ordered.vue:285` — initLoad 未 await load* 函数，产生竞态条件
  - `front/FilmDetail.vue:326`、`FilmCinema.vue:259` — `JSON.parse()` 无 try-catch 保护
- 解决方案: 逐一修复上述 14 个问题（parseFloat 包裹、绝对路由、修复映射、修正 prop、替换域名、补前导斜杠、移除死代码、Promise.all 等待、JSON.parse try-catch）
- 相关文件: 13 个 Vue 文件
- 提交记录: `4c5e916c`
- 状态: 已修复

### BUG-010: 代码质量优化 — 命名/Javadoc/事务/日志/环境配置 (BUG-006)

- 日期: 2026-05-16
- Bug 描述: 代码审计发现大量拷贝粘贴 Javadoc、命名不一致、调试输出残留、硬编码地址、空 catch 块等可维护性问题
- 根因分析:
  - 13 个 Controller 中 `selectByID()` 违反 Java camelCase 规范（应为 `selectById`）
  - 11 个 Controller 类级 Javadoc 拷贝自 AdminController："管理员管理API控制器" — 即使管理的是电影/类型/演员
  - AuthInterceptor 中 `catch (Exception ignored) {}` 静默吞掉 JWT 解析异常
  - 9 个 Controller 的 upload 方法使用 `System.out.println` / `e.printStackTrace()`（无结构化日志）
  - 17 个 Vue 文件残留 30+ 条 `console.log()` 调试语句
  - 11 处硬编码 `http://localhost:9090`（切换后端地址需修改多处）
  - 2 个 Service 注入未使用的 `TypeService`
- 解决方案:
  - 所有 Controller 方法重命名 `selectByID` → `selectById`
  - 修复 11 个 Controller 的 Javadoc（"管理员"→ 正确实体名）
  - AuthInterceptor 空 catch 改为 `log.warn`
  - 9 个 Controller 添加 SLF4J Logger，替换 `System.out` / `e.printStackTrace`
  - 创建 `.env` + `VITE_API_BASE_URL`，更新 11 处引用
  - 删除 30+ 条 `console.log()` 和 2 个未使用的 `@Resource`
  - 为 10 个 Service 补充 `@Transactional`
- 相关文件:
  - 13 个 Controller、10 个 Service、AuthInterceptor
  - `vue/.env`、`request.js`、`Front/Back/Manage.vue` + 6 个 manage 视图
  - 17 个 Vue 视图文件（console.log 删除）
  - `FilmMapper.xml`、`CinemaMapper.xml`
- 提交记录: `0df50934`
- 状态: 已修复

### BUG-011: API 路径前后端不匹配 — box-office/mark-top dash/slash 不一致 & Type.vue crud 引用失效

- 日期: 2026-05-29
- Bug 描述: E2E 全栈扫描 54 用例中 10 项失败：(a) 票房/评分排行榜 API 返回 404 — 后端 `/box-office-top` 与前端调用 `/box-office/top` 路径不匹配；(b) 分类管理表格显示 0 行 — `Type.vue` 中 `useFormDialog(crud)` 的 `crud` 为 `undefined`；(c) 影院后台 7 页面访问被拒 — ADMIN 角色无法访问 CINEMA 路由
- 根因分析:
  - (a) 重构 Phase 3 中后端 FilmController 路径为 `/box-office-top`（dash），但前端 Home.vue/Rank.vue 和 E2E 测试调用 `/box-office/top`（slash）
  - (b) Type.vue 将 `useCrud()` 返回值直接解构（`const { dataList, ... } = useCrud()`），未保存为变量，导致 `useFormDialog(crud, ...)` 传入 `undefined`
  - (c) 路由守卫 `meta: { roles: ['CINEMA'] }` 正确拦截 ADMIN，但 E2E 测试未切换影院用户
- 解决方案:
  - (a) FilmController: `@GetMapping("/box-office-top")` → `@GetMapping("/box-office/top")`；`@GetMapping("/mark-top")` → `@GetMapping("/mark/top")`
  - (b) Type.vue: 改为 `const crud = useCrud(API_PATHS.TYPES)` → 解构 `crud` → `useFormDialog(crud, ...)`
  - (c) E2E 测试: 新增 CINEMA 登录（`asks`/`cinema123`），登录后再测试影院后台页面
- 相关文件:
  - `xm_film/springboot/src/main/java/com/example/springboot/controller/FilmController.java`
  - `xm_film/vue/src/views/manage/Type.vue`
  - `xm_film/vue/e2e-tests/e2e-scan.spec.mjs`
- 提交记录: `dc4fb9e7`
- 状态: 已修复（E2E 54/54 100% 通过）

### BUG-012: CI 数据库初始化 init.sql SOURCE 路径使用反斜杠，Linux 不识别

- 日期: 2026-05-29
- Bug 描述: CI 中 `mysql < init.sql` 执行失败，SOURCE 命令找不到 schema.sql/data.sql
- 根因分析: init.sql 中 SOURCE 路径使用 Windows 风格反斜杠 `.\schema.sql`，Linux runner 不识别，应为 `./schema.sql`
- 解决方案: init.sql 中将所有 `.\` 替换为 `./`（跨平台兼容写法）
- 相关文件: `xm_film/sql/init.sql`
- 提交记录: `389b0bec`
- 状态: 已修复

### BUG-013: data.sql film 表第 26 行数据 VALUES 语法错误

- 日期: 2026-05-29
- Bug 描述: 执行 data.sql 时 film 表第 26 行插入失败，导致初始化不完整
- 根因分析: film(id=26) 的 VALUES 结尾额外逗号导致语法截断；且 SQL 脚本被多次 SOURCE 执行时主键冲突
- 解决方案: 修复 VALUES 语法；data.sql 开头加 `TRUNCATE` 清理旧数据（避免重复执行冲突）
- 相关文件: `xm_film/sql/data.sql`
- 提交记录: `0bf666fd`、`b199e106`
- 状态: 已修复

### BUG-014: CI backend JAR 路径与 Maven 输出不匹配

- 日期: 2026-05-29
- Bug 描述: CI 中 `java -jar` 指定的路径找不到 JAR 文件，后端启动失败
- 根因分析: `--spring.profiles.active=ci` 参数后的 JAR 路径使用相对路径，与 Maven 实际输出目录不匹配；`actions/download-artifact` 下载到 `$GITHUB_WORKSPACE` 但路径拼接错误
- 解决方案: 使用 `$GITHUB_WORKSPACE` 绝对路径引用 JAR 文件
- 相关文件: `.github/workflows/ci.yml`
- 提交记录: `fe05f0ae`
- 状态: 已修复

### BUG-015: CI 前端启动方式 — npm run preview 路径不匹配

- 日期: 2026-05-29
- Bug 描述: CI 中前端启动后无法访问，Playwright 无法连接
- 根因分析: 最初使用 `npm run preview`（读取 dist 目录），但 dist 目录未正确构建或路径不匹配；改为 `npm run dev` 后 Vite 直接启动开发服务器，无需构建产物
- 解决方案: CI 前端启动从 `npm run preview` 改为 `npm run dev`
- 相关文件: `.github/workflows/ci.yml`
- 提交记录: `95d75cd8`
- 状态: 已修复

### BUG-016: springdoc-openapi WebJars 与 Spring Framework 6.1 不兼容

- 日期: 2026-05-29
- Bug 描述: 后端启动时抛出 `NoClassDefFoundError: LiteWebJarsResourceResolver`，Spring Boot 无法启动；CI 后端健康检查失败
- 根因分析: `springdoc-openapi-starter-webmvc-ui:2.8.x` 传递依赖 `webjars-locator-lite`，该库引用了 `LiteWebJarsResourceResolver` 类，但 Spring Framework 6.1.x 已移除该类。Spring Boot 3.3.13 内置 Spring Framework 6.1.x，运行时触发 `NoClassDefFoundError`
- 解决方案: 移除 `springdoc-openapi-starter-webmvc-ui`，改用 `springdoc-openapi-starter-webmvc-api`（不包含 Swagger UI 依赖）；Swagger UI 通过 `static/swagger-ui.html` 静态页面从 CDN 加载
- 相关文件:
  - `xm_film/springboot/pom.xml`（依赖切换）
  - `xm_film/springboot/src/main/resources/static/swagger-ui.html`（新文件，CDN 加载 Swagger UI）
- 提交记录: `d48de68e`、`1ee0e2bb`
- 状态: 已修复

### BUG-017: FilmMapper.xml 列名 boxOffice 与 schema.sql 定义的 box_office 不匹配

- 日期: 2026-05-29
- Bug 描述: `GET /api/v1/films/box-office/top?topNum=10` 返回 500 错误，E2E 测试失败
- 根因分析: FilmMapper.xml 中 ORDER BY/INSERT/UPDATE 使用了 camelCase 列名 `boxOffice`，但 schema.sql 定义的是 snake_case 列名 `box_office`。MyBatis `map-underscore-to-camel-case` 仅对 SELECT `film.*` 的自动映射有效，不影响 ORDER BY、INSERT、UPDATE 中的显式列名。本地 MySQL 是旧 schema（列名为 `boxOffice`），CI MySQL 从 schema.sql 创建（列名为 `box_office`），导致 CI 中 3 处显式引用报错
- 解决方案: FilmMapper.xml 中 3 处 `boxOffice` → `box_office`：
  - 第 66 行: `ORDER BY film.boxOffice DESC` → `ORDER BY film.box_office DESC`
  - 第 98 行: INSERT 列名 `boxOffice,` → `box_office,`
  - 第 137 行: UPDATE SET `boxOffice = #{boxOffice},` → `box_office = #{boxOffice},`
- 相关文件: `xm_film/springboot/src/main/resources/mapper/FilmMapper.xml`
- 提交记录: `6f03c737`
- 状态: 已修复

### BUG-018: 登录页 setTimeout router.push 在 Playwright E2E 中不生效

- 日期: 2026-05-29
- Bug 描述: 管理员/用户登录后页面未跳转，URL 停留在 `/login`，但 localStorage 中用户信息（含 token/role）已正确写入
- 根因分析: Login.vue 在登录成功后使用 `setTimeout(() => router.push(homePath), 500)` 执行路由跳转。在 Playwright headless Chromium 环境下，`router.push` 在 `setTimeout` 回调中未能触发 Vue Router 导航（`setTimeout` 回调中的 Vue Router navigation 在 E2E 上下文中被跳过）。而 `window.location.href` 是浏览器原生 API，在任何环境下都能可靠触发导航
- 解决方案: Login.vue 第 62 行 `setTimeout(() => router.push(homePath), 500)` 改为 `window.location.href = homePath`
- 相关文件: `xm_film/vue/src/views/Login.vue`
- 提交记录: `d1cace41`
- 状态: 已修复（E2E 59/59 100% 通过）

### BUG-019: Front.vue 搜索框 handleSearch 使用 router.push 在 E2E 中不生效

- 日期: 2026-05-30
- Bug 描述: 前台首页搜索框输入"哈利"后点击搜索按钮，URL 未跳转到 `/front/search`，仍停留在 `/front/home`；CI 中 E2E 搜索用例失败（Run #27，59 用例 58 通过 1 失败）
- 根因分析: `Front.vue` 的 `handleSearch()` 使用 `router.push({ path: '/front/search', query: { title } })` 导航。在 Playwright headless Chromium 下与 BUG-018 登录跳转是同一类问题——`router.push` 在某些调用上下文（非用户直接交互触发）中被跳过，而 `window.location.href` 是浏览器原生 API，在任何环境下都能可靠触发导航
- 解决方案: `handleSearch()` 中的 `router.push({ path, query })` 替换为 `window.location.href = '/front/search?title=' + encodeURIComponent(keyword)`
- 相关文件: `xm_film/vue/src/views/Front.vue`（第 153~162 行）
- 提交记录: `0de65567`
- 状态: 已修复（后续 CI 59/59 100% 通过）

### BUG-020: 选座环节 USER 读取排片被误拦截

- 日期: 2026-06-19
- Bug 描述: USER 角色用户在选座页面调用 `GET /api/v1/records/{id}` 时报"无权操作该排片"，无法正常选座购票
- 根因分析: `RecordController.ensureRecordAccess()` 仅允许 ADMIN 和 CINEMA 角色访问，未放行 USER 角色。选座页作为读操作不需要角色校验，被误拦截
- 解决方案: `getById()` 中移除非必要的角色校验（仅保留空值检查），写操作（PUT/DELETE）保持原有权限保护不变
- 相关文件: `xm_film/springboot/src/main/java/com/example/springboot/controller/RecordController.java`
- 提交记录: `b0578696`
- 状态: 已修复

### BUG-021: OrderedServiceTest 取消用例状态不匹配 P1 变更

- 日期: 2026-06-19
- Bug 描述: P1 支付流程上线后，4 个 `OrderedServiceTest` 单元测试因状态不匹配而失败
- 根因分析: P1 将 `cancelOrder` 方法接受的订单状态从"待取票"收窄为仅"待支付"，但测试 mock 数据仍使用旧状态
- 解决方案: 更新测试 mock 数据中的订单状态为"待支付"
- 相关文件: `xm_film/springboot/src/test/java/com/example/springboot/OrderedServiceTest.java`
- 提交记录: `fcf6e256`
- 状态: 已修复

### BUG-022: CORS 通配符 + pending_timeout_at 设置 null 不写库

- 日期: 2026-06-20
- Bug 描述: (a) CORS 配置使用 `*` 通配符，生产环境存在安全隐患；(b) 订单取消后 `pending_timeout_at` 字段未清除，MyBatis UPDATE 跳过了该字段；(c) 前端 token 过期无法自动检测登出
- 根因分析:
  - (a) `CorsConfig.java` 中 `allowedOrigins` 设为 `*`，允许任意域跨域访问
  - (b) `OrderedMapper.xml` 中 UPDATE 语句用 `<if test="pendingTimeoutAt != null">` 包装该字段，Java 显式设为 `null` 后 `<if>` 判断为 `false`，跳过了该字段的更新
  - (c) 缺少 token 有效性校验端点和前端自动检测逻辑
- 解决方案:
  - (a) CORS 从 `*` 改为 `CORS_ALLOWED_ORIGINS` 环境变量白名单
  - (b) 移除 `<if>` 包装，允许显式 `null` 写入数据库
  - (c) 新增 `/api/v1/auth/me` 接口；前端 `useAuth.js` 初始化自动校验 token，过期自动登出
- 相关文件: `CorsConfig.java`、`OrderedMapper.xml`、`OrderedService.java`、`AuthController.java`、`useAuth.js`、`application-prod.yml`
- 提交记录: `0cbb6664`
- 状态: 已修复

### BUG-023: Docker HTTPS 部署 + Vite SPA 路由 403

- 日期: 2026-06-25
- Bug 描述: (a) 生产环境 Docker 部署前端缺少 HTTPS 支持；(b) Vite `fs.allow` 配置导致 SPA 路由刷新时返回 403；(c) `VITE_API_BASE_URL` 硬编码为 `http://localhost:9090` 无法适配同源部署
- 根因分析:
  - (a) Nginx 配置缺少 SSL 证书挂载和 HTTPS server block
  - (b) `vite.config.js` 中 `fs.allow` 限制过严，SPA 路由刷新时 Vite 开发服务器拒绝服务
  - (c) `vue/.env` 中 `VITE_API_BASE_URL=http://localhost:9090` 被 git 跟踪，生产环境无法覆盖
- 解决方案:
  - (a) 前端容器加 443 端口 + SSL 证书挂载；Nginx HTTPS server block + HTTP→HTTPS 301 重定向
  - (b) `vite.config.js` 中 `fs.allow` 改为允许项目根目录
  - (c) `vue/.env` 取消 git 跟踪，默认值改为 `/`，新增 `.env.development` 本地开发配置；`request.js` 回退值从 `http://localhost:9090` 改为 `/`
  - (d) `npm audit fix` 修复 8 个前端安全漏洞（1 critical, 4 high, 3 moderate）
- 相关文件: `nginx.conf`、`docker-compose.yml`、`vite.config.js`、`request.js`、`.env` → `.env.development`（其中 `nginx.conf`、`docker-compose.yml` 已于 2026-09-27 随 Docker 层移除；(b)(c) 两处在 `vite.config.js` 与 `.env.development` 的修复仍然生效）
- 提交记录: `1bbb6571`
- 状态: 已修复

### BUG-024: 生产环境 MySQL 乱码

- 日期: 2026-06-25
- Bug 描述: 生产环境 MySQL 中文数据出现乱码，页面显示问号或乱码字符
- 根因分析: JDBC 连接 URL 缺少 `characterEncoding=utf-8` 和 `useUnicode=true` 参数，MySQL 连接使用默认编码（非 UTF-8）
- 解决方案: `application.yml` 中 JDBC URL 追加 `?useUnicode=true&characterEncoding=utf-8`
- 相关文件: `xm_film/springboot/src/main/resources/application.yml`
- 提交记录: `71927b4d`
- 状态: 已修复

### BUG-025: 生产环境 /files/* 图片全部 404

- 日期: 2026-06-25
- Bug 描述: Docker 部署后所有电影海报、用户头像、预告片返回 404，页面图片全部缺失
- 根因分析: SQL seed 数据引用了 61 个 `/files/*` 资源（47 JPG、4 PNG、10 MP4），但仓库中不存在这些文件。Docker 部署时 `uploads` 命名卷为空，无种子文件填充机制
- 解决方案:
  - 新增 `xm_film/sql/seed-uploads/` 目录，容纳 61 个自动生成的占位文件
  - 新增 `scripts/generate-seed-uploads.ps1` 种子文件生成脚本
  - 新增 `scripts/docker-entrypoint.sh` Docker 入口包装脚本
  - Dockerfile 在构建时将种子文件拷入镜像，entrypoint 在首次启动时自动填充空卷
  - 用户后续上传不受影响（仅首次部署时填充空卷）
- 相关文件: `Dockerfile`、`scripts/docker-entrypoint.sh`、`scripts/generate-seed-uploads.ps1`、`xm_film/sql/seed-uploads/`（61 个文件）—— 以上文件已于 2026-09-27 随 Docker 层一并移除，本项目改为纯本地运行，本 Bug 的修复机制不再适用
- 提交记录: `6adac709`
- 状态: 已修复

### BUG-026: 同名素材文件覆盖导致部分占位图未替换

- 日期: 2026-06-25
- Bug 描述: 替换 seed-uploads 为真实素材后，部分占位图未被替换，仍显示占位内容
- 根因分析: 映射脚本使用合并对象（`{源文件: UUID}`）存储映射关系，当多个不同 UUID 文件名映射到同名源文件时，后一个覆盖前一个，导致"毒液：最后一舞"海报被视频封面覆盖、演员张梓宸头像被其他映射覆盖
- 解决方案: 映射结构改为数组存储 `[源文件, UUID]` 对，支持一源多目标映射
- 相关文件: `scripts/replace-with-real-images.mjs`（该脚本与 `xm_film/sql/seed-uploads/` 目录均已于 2026-09-27 随 Docker 层移除）
- 提交记录: `2e6f2856`
- 状态: 已修复

### BUG-027: /files/ 未设置 Cache-Control 导致浏览器缓存旧占位图

- 日期: 2026-06-25
- Bug 描述: 替换占位图为真实素材后，用户浏览器仍显示旧占位图，需手动刷新或清除缓存
- 根因分析: Nginx 代理 `/files/` 静态资源时未设置 `Cache-Control` 头，浏览器默认强缓存旧占位图
- 解决方案: Nginx location `/files/` 添加 `add_header Cache-Control 'no-cache'`，每次请求回源验证
- 相关文件: `xm_film/vue/nginx.conf`（已于 2026-09-27 随 Docker 层移除；若将来改用裸 jar + Nginx 反代部署，需在新配置的 `/files/` location 重新加上 `add_header Cache-Control`）
- 提交记录: `22c6b60b`
- 状态: 已修复

### BUG-028: 票房显示比真实值小 10000 倍

- 日期: 2026-09-27
- Bug 描述: 所有展示票房的页面（`front/FilmDetail.vue`、`front/FilmCinema.vue`、`front/Home.vue`、`front/Rank.vue`）把影片票房显示成 `8868.5元` / `8,868.5元`，而真实值是 8868.5 万元——相差 10000 倍。同一字段在 4 个页面上还有两种互不相同的格式（万级 vs 千分位）
- 根因分析: `film.box_office` 在数据库中的单位是万元，`schema.sql` 的列注释已写明 `DECIMAL(10,1) ... COMMENT '票房（万元）'`；但前端 5 处独立实现（含已作为死代码删除的旧 `utils/format.js`）一律按"元"处理——`toFixed`/`toLocaleString`/万级除法各写一套，结果全部差 10000 倍
- 解决方案: 统一为 `xm_film/vue/src/utils/format.js` 的单一实现 `formatBoxOffice`，按行业惯例（猫眼/灯塔）输出：`0` 或空 → `暂无数据`；`< 10000` 万 → `8868.5万`；`>= 10000` 万（即 ≥ 1 亿）→ `1.23亿`。4 个视图删除本地副本改为导入
- 相关文件: `xm_film/vue/src/utils/format.js`、`xm_film/vue/src/views/front/{FilmDetail,FilmCinema,Home,Rank}.vue`、`xm_film/sql/schema.sql`
- 提交记录: 待提交
- 状态: 已修复

### BUG-029: 电影"类型"字段前端取错，长期显示空白或"未知类型"

- 日期: 2026-09-27
- Bug 描述: 5 个页面的电影类型展示失效——`back/Film.vue` 的类型列与展开面板空白；`front/Home.vue`、`front/Rank.vue`、`front/FilmDetail.vue`、`front/FilmCinema.vue` 恒显示"未知类型"；且硬编码字典把 id=1 写成"记录"，而数据库实为"纪录"
- 根因分析: 后端 `Film` 实体没有 `types` 字段，只有 `typeIds`(`List<Integer>`) 与 `typeList`(`List<Type>{id,title}`)，类型名由 `FilmService.fillFilmTypes` 从 `film_type` 关联表填充；地区名也早已由 `FilmMapper.xml` 的 `LEFT JOIN area` 解析为 `areaName`。但前端用 3 种方式猜字段形状：`props.row.types` / `movie.types`（字段不存在 → `undefined` → 渲染空白）、`JSON.parse(data.typeIds)`（`typeIds` 是数组，`JSON.parse([5,22])` 抛错被 `try` 吞掉 → 恒为空数组），同时另行维护两份硬编码类型/地区字典
- 解决方案: 删除全部前端硬编码 `typeMap`/`areaMap` 以及零引用的死代码 `roleTypeMap`，统一改用后端已解析字段：类型用 `typeList.map(t => t.title)`，地区用 `areaName`
- 相关文件: `xm_film/vue/src/views/back/Film.vue`、`xm_film/vue/src/views/front/{Home,Rank,FilmDetail,FilmCinema}.vue`、`xm_film/springboot/src/main/java/com/example/springboot/entity/Film.java`、`service/FilmService.java`、`src/main/resources/mapper/FilmMapper.xml`
- 提交记录: 待提交
- 状态: 已修复

### BUG-030: 影院上映影片与排片脱节，新建场次在前台不可见

- 日期: 2026-09-27
- Bug 描述: 影院后台新建一条排片后，前台该影院的影片/场次列表里看不到它，用户无法购票。实测影院 11（丁丁影城）的排片 18、19（影片 26、22）在前台完全不可见；同时后台排片表单的"电影名称"是手工输入的文本框，与 `film` 表无任何关联
- 根因分析: 前台"影院上映哪些影片"由 `FilmMapper.selectByCinema` / `CinemaMapper.selectByFilmId` 通过 `INNER JOIN cinema_film` 决定，而 `cinema_film` 没有任何写入入口（无 Controller、无前端页面），只在 `data.sql` 里手工维护了 16 行。新建排片只写 `record` 表，不会写 `cinema_film`，于是排片与"上映关系"两张表长期漂移：`(11,22)`、`(11,26)` 两行关联缺失，对应场次成为前台不可达的死数据。`record.film_id` 当时还可空，排片本身也可能不指向任何影片
- 解决方案:
  - 影院上映影片改为由排片派生：`FilmMapper.selectByCinema` 用 `EXISTS (SELECT 1 FROM record ...)` 取代 `cinema_film` 关联，`showCount` 改为真实场次数量；`CinemaMapper.selectByFilmId` 同样改为按 `record` 判断
  - 删除冗余表 `cinema_film`（schema.sql / data.sql / README 表清单同步移除），使 `record` 成为"影院是否上映某片"的唯一数据源
  - `record.film_id` 改为 `NOT NULL`，后端 `RecordService.validateSchedule` 强制校验影片存在并回填 `title`，后台表单的"电影名称"改为只读、"影片"改为下拉（`GET /api/v1/films`）
- 相关文件: `mapper/FilmMapper.xml`、`mapper/CinemaMapper.xml`、`service/RecordService.java`、`sql/schema.sql`、`sql/data.sql`、`vue/src/views/back/Record.vue`
- 提交记录: `96324f28`
- 状态: 已修复

### BUG-031: 删除影片/影院/影厅会级联删除订单（交易凭证丢失）

- 日期: 2026-09-27
- Bug 描述: 管理端删除一部影片、一个影院或一个影厅时，SQL 不会报错，但其历史订单会被数据库静默删除。演示时"删掉一个影院看看效果"会直接抹掉该影院的所有交易记录
- 根因分析: `schema.sql` 中 `ordered` 表的 5 个外键全部是 `ON DELETE CASCADE`（`record_id` 为 `SET NULL`），`record` 的 `cinema_id`/`room_id` 是 `CASCADE`、`film_id` 是 `SET NULL`，`room.cinema_id` 是 `SET NULL`。级联动作完全由数据库执行，ORM 层不感知，因此删除接口返回成功而数据已丢失
- 解决方案:
  - `ordered` 的 5 个外键（record/user/film/cinema/room）、`record` 的 3 个外键、`room.cinema_id` 全部改为 `ON DELETE RESTRICT`，并把约束显式命名（`fk_ordered_film` 等）便于后续迁移
  - 五个删除入口（Film/Cinema/Room/Record/User Controller）增加引用计数前置校验，返回可读提示（如"该影片已有 4 个排片、4 笔订单，无法删除；如需下架请将状态改为「停止上映」"）
  - 确立业务语义：影片/影院/场次的下架走 `status`，不做物理删除
  - 批量删除在循环校验通过后才执行，保证整批原子（不会删一半）
  - 提供幂等迁移脚本 `sql/migration-20260927-delete-guard.sql`
- 相关文件: `sql/schema.sql`、`sql/migration-20260927-delete-guard.sql`、`controller/{Film,Cinema,Room,Record,User}Controller.java`、`service/{Record,Room,Ordered}Service.java`、`mapper/{Record,Ordered,Room}Mapper.{java,xml}`
- 提交记录: `96324f28`
- 状态: 已修复

### BUG-032: 公开的影院详情页排片列表返回 401

- 日期: 2026-09-27
- Bug 描述: 未登录用户浏览影院详情页时，该影院的场次列表加载失败（401 "登录已过期"），而页面本身是公开可访问的
- 根因分析: `front/CinemaDetail.vue` 通过 `GET /api/v1/records/page` 拉取场次，但 `AuthInterceptor.PUBLIC_READ_PREFIXES` 白名单里没有 `/api/v1/records`，匿名 GET 被直接拒绝
- 解决方案: 将 `/api/v1/records` 加入匿名 GET 白名单（该资源不含用户隐私字段）；写操作仍受保护，由 `RecordController.requireAdminOrCinema()` 做业务层校验。补充 `AuthInterceptorAccessTest` 匿名读放行/写拒绝用例
- 相关文件: `common/config/AuthInterceptor.java`、`src/test/java/com/example/springboot/AuthInterceptorAccessTest.java`
- 提交记录: `96324f28`
- 状态: 已修复

### BUG-033: 场次可购票判断与放映时间无关，过去场次仍可下单

- 日期: 2026-09-27
- Bug 描述: 可以给已经放映结束的场次下单并生成"待支付"订单；后台也无法区分"未开始/放映中/已结束"，`record.status` 手工填的"待上映/已上映/停止上映"与前台判断用的"未开始/放映中/已结束"是两套互不匹配的取值
- 根因分析: `record.status` 被当成人工维护的派生状态字段（放映状态本应是 `start` 的函数），而 `OrderedService.insertOrder` 校验场次存在后直接售票，从不比较 `start` 与当前时间；前端 `CinemaDetail.vue` 用 `status === '已上映' || '放映中' || '未开始'` 判断可购票，其中"已上映"与后台表单的取值重合、另两个永远不会被写入
- 解决方案:
  - 派生状态不落库：`未开始/放映中/已结束` 一律由 `start` 计算（前端 `recordState()`），`status` 收敛为 `正常/停售` 单一人工开关
  - 新增 `common/enums/RecordStatus.java`；`RecordService.isPurchasable()` 作为唯一权威判定
  - `OrderedService.insertOrder` 增加"已停售""已开场"拒绝分支，作为下单的最后一道关
  - 新增/编辑校验：`start` 必须晚于当前（编辑时未改动时间则不重复校验，保证存量过期场次仍可停售）、`price > 0`、同影厅时段不重叠（按影片片长计算区间，默认 120 分钟兜底）
  - 种子数据 `record.start` 整体平移到未来（原来停留在 2024~2025，新规则上线后所有场次都会显示不可购票）
- 相关文件: `service/{RecordService,OrderedService}.java`、`controller/RecordController.java`、`common/enums/RecordStatus.java`、`mapper/RecordMapper.{java,xml}`、`vue/src/views/front/CinemaDetail.vue`、`vue/src/views/back/Record.vue`、`vue/src/constants/index.js`、`sql/{data.sql,schema.sql}`
- 提交记录: `96324f28`
- 状态: 已修复

### BUG-034: 订单状态流转清空订单金额（total 被写成 0.00）

- 日期: 2026-09-27
- Bug 描述: 订单一旦发生状态流转（支付/取票/取消），其 `total` 就变成 0.00，`pay_amount`/`refund_amount` 也随之失真。开发库中 18 笔订单（7 笔待取票、11 笔已取消）全部中招，用户看到的是"总费用 0 元"的订单；退票时记录的退款金额也是 0
- 根因分析: `Ordered.total` 声明为原始类型 `double`，而 `OrderedMapper.xml` 的 `updateById` 用 `<if test="total != null">total = #{total},</if>` 守卫。原始类型经 getter 取值时永远非 null，MyBatis 的 OGNL 判断恒为真，于是任何不带 total 的局部更新对象都会被补写 `total = 0.0`。状态流转只设置 `status`，恰好命中该路径。`Film.boxOffice`（同为原始 `double`）存在同样写法，但 `manage/Film.vue` 用 `Object.assign(form, row)` 提交整个对象，带上了 `boxOffice`，因此当前不可达 —— 属于同类地雷，未在本次改动
- 解决方案:
  - `entity/Ordered.java` 的 `total` 改为包装类型 `Double`，使 `!= null` 守卫真正生效；调用点（`setPayAmount`/`setRefundAmount`）签名本为 `Double`，无需改动
  - 提供一次性数据修复脚本，按 `total = record.price × ordered.number` 重建被归零的存量订单金额（18 笔全部可确定性重建，0 笔不可恢复）
  - 端到端验证新增断言：支付、取票、退票、超时取消四条路径后 `total` 必须保持原值
- 相关文件: `entity/Ordered.java`、`mapper/OrderedMapper.xml`、`service/OrderedService.java`
- 提交记录: `60fa75f8`
- 状态: 已修复

### BUG-035: 支付超时取消被事务回滚，订单停在待支付

- 日期: 2026-09-27
- Bug 描述: 用户对已超时的待支付订单发起支付时，接口返回"支付超时，订单已自动取消"，但数据库里该订单仍是待支付——提示与实际状态不符
- 根因分析: `OrderedService.payOrder` 在超时分支里先用 `updateById` 把订单置为已取消，紧接着 `throw new CustomException(...)`。该方法标注了 `@Transactional(rollbackFor = Exception.class)`，异常触发事务回滚，把刚刚写入的取消一并撤销。整体表现被 `OrderCleanupTask`（每 15 秒扫描一次）掩盖，所以端到端难复现，只能通过单元测试或代码审查发现
- 解决方案:
  - 超时分支不再抛异常：`payOrder` 返回 `PayResult.TIMEOUT_CANCELLED`，由 `OrderedController` 翻译成 409 业务错误返回客户端，取消得以正常提交
  - 新增 `common/enums/PayResult.java`，并在类注释中写明"为何不能用异常表达"
  - 补单元测试 `payOrderAfterTimeoutCancelsOrderInsteadOfThrowing`：断言返回超时结果且调用了状态更新，实现若改回抛异常该用例即失败
- 相关文件: `service/OrderedService.java`、`controller/OrderedController.java`、`common/enums/PayResult.java`、`src/test/java/com/example/springboot/OrderedServiceTest.java`
- 提交记录: `60fa75f8`
- 状态: 已修复

### BUG-036: 影院分页接口被排除在拦截器外，角色信息缺失导致审核列表查不到待审核影院

- 日期: 2026-09-28
- Bug 描述: 为「未审核影院不对外展示」加上按角色过滤后，管理员打开影院管理页也只看到 4 家已审核影院，新注册的（未审核）影院在前后台都查不到 —— 管理员因此根本无法审核它，「影院注册审核」这条业务线整体不可用
- 根因分析: `WebMvcConfig.addInterceptors` 的 `excludePathPatterns` 里列着 `/api/v1/cinemas/page`。被排除的路径根本不进 AuthInterceptor，拦截器自然不会往 request 写 `role`/`userId` 属性，控制器里的 `isAdmin()` 于是恒为 false，"管理员看全部、其余人只看已审核"退化成"所有人都只看已审核"。该排除项本意是放开公开访问，但 `PUBLIC_READ_PREFIXES` 已包含 `/api/v1/cinemas`，排除是冗余的 —— 它唯一的实际效果是让这个端点变成"角色盲"
- 解决方案:
  - 从 `excludePathPatterns` 移除 `/api/v1/cinemas/page`，并在代码里留注释说明"公开访问交给 `PUBLIC_READ_PREFIXES`，不要往排除表里加"
  - 端到端验证补断言：管理员的 `/cinemas/page` 必须能看到未审核影院，且审核通过后该影院出现在前台列表并可以登录
- 相关文件: `common/config/WebMvcConfig.java`、`common/config/AuthInterceptor.java`、`controller/CinemaController.java`、`mapper/CinemaMapper.xml`
- 提交记录: `e3a250f1`
- 状态: 已修复

### BUG-037: 支付不校验余额、不扣减、不记账 —— "点一下按钮就出票"

- 日期: 2026-09-28
- Bug 描述: 订单进入「待支付」后点「模拟支付」，无论用户账户有没有钱都直接出票（`待取票`）。系统既没有用户余额字段，也没有充值单据与资金流水模型 —— 所谓"模拟支付"只是一次无条件的状态流转，"余额不足则支付失败、订单保持待支付"这条分支根本不存在
- 根因分析: `OrderedService.payOrder` 的校验只有两条：订单存在、状态为「待支付」，随后直接 `status = 待取票` + `pay_amount = total`。整条链路没有任何一处读 `user` 表，`user` 表也确实没有余额列；充值单据表与资金流水表在 `schema.sql` 中不存在，因此"提交充值单不改余额""回调成功才入账""重复回调幂等"这些约束没有任何载体
- 解决方案:
  - 数据层：`user.balance`、`recharge_order`（处理中/已完成/已失败）、`fund_flow`（来源 + 变动前后余额 + 关联单据ID）、`ordered.unit_price` 单价快照
  - 新增 `WalletService` 作为余额读写的唯一出处：`SELECT balance ... FOR UPDATE` 行锁 + `UPDATE ... WHERE balance >= ?` 条件更新，并在同一事务内写一条 `fund_flow`。充值入账/购票扣减/退票入账三条路径全部复用它，消除"同一套金额逻辑多处各写一遍"
  - `payOrder` 在超时判定之后调用 `walletService.debitPurchase`，余额不足抛业务冲突。因为扣款与出票在同一事务，失败时整笔回滚 —— 订单停在待支付、`pending_timeout_at` 不变、座位继续锁定，用户充值后可回到订单页继续支付
  - 新增 `RechargeService` / `RechargeController`：提交申请只生成「处理中」单据（余额不变）；`POST /api/v1/recharges/{id}/callback` 仅允许「处理中」单据流转，成功入账、失败置「已失败」且余额不变，重复回调一律拒绝
  - 余额不挂 `User` 实体，避免 `/api/v1/users` 的 `SELECT *` 把他人余额带出去；余额只经 `/api/v1/account/summary` 按 JWT 返回本人
  - 前端新增 `/front/account`（余额 + 档位/自由输入充值 + 单据列表含模拟回调双按钮 + 资金流水），`OrderPayDialog` 改为余额支付并展示余额不足差额与「去充值」入口
- 验证: 单测 25 例（`WalletServiceTest` / `RechargeServiceTest` / `FundFlowServiceTest`）+ 端到端 59 断言（`scripts/verify/p4-account-wallet-e2e.py`）+ 并发 11 断言（`scripts/verify/p4-concurrency.py`：余额只够一单时并发支付恰好一单成功、余额为 0.50 且不为负）
- 相关文件: `sql/schema.sql`、`sql/migration-20260928-p4-account-wallet.sql`、`service/WalletService.java`、`service/RechargeService.java`、`service/FundFlowService.java`、`controller/RechargeController.java`、`controller/FundFlowController.java`、`controller/AccountController.java`、`service/OrderedService.java`、`mapper/UserMapper.java(+xml)`、`views/front/Account.vue`、`components/OrderPayDialog.vue`
- 提交记录: `f97eb3ab`
- 状态: 已修复

### BUG-038: 退票只改状态与凭证字段，款项没有回到用户账户

- 日期: 2026-09-28
- Bug 描述: 用户退票后订单变成「已退票」、`refund_amount` 也写了金额，但用户账户上什么都没发生 —— 没有余额增加，也没有任何资金流水。前台退票确认框却写着"退票后座位释放、款项退回"，属有文案无实现
- 根因分析: `OrderedService.refundOrder` 只做 `updateById(status=已退票, refundTime, refundAmount)`，没有任何余额入账动作；且当时项目里根本没有"用户余额"这个概念，`refund_amount` 只是一份写给自己看的凭证
- 解决方案: 在退票窗口与状态校验全部通过之后调用 `walletService.creditRefund(userId, total, orderId)`，与状态更新同事务 —— 入账成功但状态没改回去（或反之）的情况不会出现；取消未扣款的待支付订单依旧不触碰余额
- 验证: `OrderedServiceTest.refundOrderCreditsBalanceWithOrderAmount` / `refundOrderDoesNotCreditWhenDeadlinePassed`（校验必须先于资金动作）；端到端断言余额 `181.50 → 300.00`、座位释放、新增一条 `+118.50` 且关联订单ID 的退票流水
- 相关文件: `service/OrderedService.java`、`service/WalletService.java`、`mapper/OrderedMapper.xml`
- 提交记录: `f97eb3ab`
- 状态: 已修复

### BUG-039: 订单可被物理删除，删单成为绕过退票的免费后门

- 日期: 2026-09-28
- Bug 描述: 用户端、影院端、管理端三处订单列表的删除按钮都不判状态，后端 `deleteScoped` 也只校验归属。用户对一张「待取票」（已付款）订单点删除，订单行直接消失、座位被静默释放、既没有退票记录也没有退款流水 —— 等于用删除当免费退票用，资金凭证链彻底断裂
- 根因分析: 删除接口是从通用 CRUD 继承下来的，只做了"这条订单是不是你的"的归属校验，没有做"这条订单允不允许被删"的状态校验。资金模型落地后这个缺口更严重：退票会回款而删除不会，只要后门开着，用户必然走后门
- 解决方案:
  - 后端新增 `OrderedService.DELETABLE_STATUSES = {已取消, 已退票}` 与 `ensureDeletable` 守卫，单删与批删都先校验（批删任一不合格则整批拒绝）
  - 前端三端按钮由 `constants.isOrderDeletable` 同构条件渲染；列表勾选列加 `:selectable`，不可删除的订单连勾选都不允许，批量删除自然带不上它们
- 验证: `OrderedServiceTest` 六个用例（待支付/待取票/已取票拒绝，已取消/已退票放行，批删整批拒绝）；端到端断言删除待取票订单被拒且订单仍存在、已退票与已取消订单可删除
- 相关文件: `service/OrderedService.java`、`views/front/Orders.vue`、`views/back/Ordered.vue`、`views/manage/Ordered.vue`、`constants/index.js`
- 提交记录: `f97eb3ab`
- 状态: 已修复

### BUG-040: 选座接口把全场次订单明细发给任意登录用户（越权读）

- 日期: 2026-09-28
- Bug 描述: `GET /api/v1/orders/seats?recordId=X` 直接返回 `Ordered` 实体列表，任何登录用户只要换一个 `recordId`，就能拿到该场次所有订单的订单编号、购票用户ID、订单金额、支付/退款凭证字段。选座图渲染只需要"哪个座位被占"，其余字段全是越权可见的他人交易信息；前端也确实是拿响应里的 `userId` 和自己本地存的 ID 比对来判断"是不是我的锁座"
- 根因分析: 该端点当初直接复用了面向后台列表的 `selectActiveByRecordId`（`SELECT *`），把"内部查询"当成了"对外响应"。归属信息（`user_id`）被一并下发，判定"是不是我的"这件事被推给了前端 —— 等于先泄露再让前端自觉忽略
- 解决方案:
  - 新增 `dto/response/SeatOccupancy`：只含 `seat` 与 `mine`；仅当 `mine` 为真时才带 `orderId` / `orders` / `status` / `total` / `pendingTimeoutAt`（继续支付与取消锁座所需）
  - `OrderedService.selectSeatOccupancy(recordId, tokenUserId)` 在后端按 JWT 里的 `userId` 完成归属判定与字段裁剪，他人订单只回 `{seat, mine:false}`；`tokenUserId` 为空时一律判定为非本人，避免误把他人订单当成自己的
  - `OrderedController.seats` 改用该方法；原先只做透传的 `OrderedService.selectActiveByRecordId` 随之删除
  - `BuyTicket.vue` 改读 `order.mine`（不再比对 `userId`），并把选座视角的字段映射成支付弹窗需要的订单形态；本地不再保存 `userId`
- 验证: `OrderedServiceTest` 三例（他人订单只出 `seat`+`mine:false`、本人订单带齐字段、无令牌用户不得被判成本人）；端到端新增 10 条断言（他人座位可见但 `orderId`/`orders`/`total` 为 null、响应中不含 `userId` 键、本人座位 `mine:true` 且带 `orderId`）—— E2E 共 69 断言全通过
- 相关文件: `dto/response/SeatOccupancy.java`、`service/OrderedService.java`、`controller/OrderedController.java`、`views/front/BuyTicket.vue`、`scripts/verify/p4-account-wallet-e2e.py`
- 提交记录: `f97eb3ab`
- 状态: 已修复

### BUG-041: 购票可双击重复提交、支付超时提示与真实行为不符

- 日期: 2026-09-28
- Bug 描述: 两个前端交互缺陷合在一起：① 选座页「确认购票」在网络往返期间不禁用，双击会连发两次下单请求，第二次必然被"座位已售"拒绝并在成功弹窗旁边弹出一条错误提示；② 支付弹窗倒计时归零时提示"支付超时，订单已自动取消"，但前端归零只关闭弹窗，订单其实仍停在待支付，要等后端定时任务（约 15 秒后）才真正取消 —— 文案断言了一件当时还没发生的事
- 根因分析: ① 提交类按钮缺少在途状态，只挡了"未选座/未登录/加载中"，没挡"上一次请求还在飞"；② 文案把"前端倒计时结束"等同于"订单已取消"，混淆了前端计时器与后端 `OrderCleanupTask` 两条独立路径
- 解决方案:
  - `BuyTicket.vue` 新增 `submitting` 在途标记与 `canSubmit` 计算属性，提交期间按钮禁用并显示"提交中…"，`finally` 中复位
  - `OrderPayDialog.vue` 倒计时归零改提示"支付时间已到，未支付的订单将自动取消"，并注释说明真正的取消由后端定时任务完成
- 验证: 前端 `npm run build` 通过；后端全量单测 154 例全绿（改动未触及后端逻辑）。两条均为纯前端交互改动，没有浏览器点击验证，仅验证到构建通过与后端接口未回归
- 相关文件: `views/front/BuyTicket.vue`、`components/OrderPayDialog.vue`
- 提交记录: `f97eb3ab`
- 状态: 已修复

### BUG-042: 登录页卡片宽度有两个来源 —— 标题折行、表单溢出 188px

- 日期: 2026-09-28
- Bug 描述: `/login` 页面上「欢迎登录电影购票系统」被拆成两行，账号 / 密码 / 角色三个输入框横向冲出白色半透明卡片约 188px，卡片看上去只有内容的一半宽。`/register` 逐字同病
- 根因分析: 卡片宽度有两个互不相干的来源，且都对不上。外层 `.login-box` 是 `width: 40%` + `max-width: 400px` + `padding: 64px`，内层 `.login-form-wrapper` 又是 `max-width: 380px` + `padding: 40px`（全局 `box-sizing: border-box`）：两级 padding 叠加后，卡片内容宽只剩 192px；而 `.login-form` 又写死 `width: 380px` —— 这是从旧内联样式 `style="width: 380px"` 原样搬过来的（`cc3379b0` 消解内联样式时保留了它），容器宽度却在那之前就已经收窄了。溢出 380 − 192 = 188px。标题折行是同一根因：10 个汉字 × `--fs-xl`(20px) = 200px > 192px，必然断行
- 解决方案:
  - 抽出 `src/assets/css/auth-layout.scss`，与 `Register.vue` 共用（同 `admin-layout.scss` 的处理方式）：卡片宽度只由 `.auth-card` 的 `max-width: 380px` 决定，删除页面里写死的 `width: 380px`
  - 容器改为 `min-height: 100vh` + flex 居中，去掉 `height: 100vh` + `overflow: hidden` + 绝对居中的组合 —— 顺带修掉"视口变矮时卡片被裁掉且无法滚动"
  - 两页共用同一张背景图（按产品要求把注册页换成登录页的图），背景声明也收进共用外壳
  - 规范新增 §6.4 固化这条口径；`tests/design-tokens.test.mjs` 加终态断言（认证页不得出现写死的 px 宽度、必须 `@use` 共用外壳、共用外壳不得出现 `overflow: hidden`）
- 验证: `npm run test:tokens` 13/13、`test:inline` 2/2、`npm run build` 通过；构建产物核对 `Login-*.css` 与 `Register-*.css` 均引用同一份 `bg_login-*.jpg`，注册页旧图 `registerbg.jpg` 已不再打包（无引用）。页面渲染由用户在本机目视确认通过
- 相关文件: `src/assets/css/auth-layout.scss`（新增）、`src/views/Login.vue`、`src/views/Register.vue`、`tests/design-tokens.test.mjs`
- 提交记录: 未提交
- 状态: 已修复

### BUG-043: 认证页表单无可访问名称、无错误图标、不支持回车提交，前缀图标不渲染

- 日期: 2026-09-28
- Bug 描述: 四个同源的表单缺陷：① 所有输入框只有 `placeholder`，没有 `label`，读屏软件读不出字段用途，且一开始输入提示就消失；② 校验只出红框与文案，规范 §9.3 要求的"错误图标"缺失；③ 回车键不能提交，只能鼠标点按钮；④ 账号 / 密码输入框左侧的 `User` / `Lock` 图标根本不显示，只有一块空白
- 根因分析: ①③ 从未实现；② 缺 `status-icon` —— 错误图标由该属性驱动，EP 不会自己加；④ `Login.vue:8,11` 与 `Register.vue:11,19,27` 用的是字符串写法 `prefix-icon="User"`，而 `prefix-icon` 接受的是组件，字符串要靠全局注册才能解析 —— 全项目从未 `app.component()` 注册图标集（`main.js` 无该调用），因此解析失败、图标为空。全项目其余 20 多个页面都用绑定写法 `:prefix-icon="Search"`，只有认证两页是字符串，属孤例
- 解决方案:
  - `import { User, Lock } from '@element-plus/icons-vue'` + `:prefix-icon="User"`，与其余页面统一
  - `el-form` 加 `label-position="top"` 与 `status-icon`，每个 `el-form-item` 补可见 `label`
  - 文本输入框加 `@keyup.enter`，`el-form` 挂 `@submit.prevent` 兜住原生提交；只在一处绑定，避免一次回车发两次请求
  - `role` 的校验触发由 `blur` 改 `change`（下拉框不会触发 blur 那一刻的语义）
  - 规范 §10.2 新增「表单控件必须有可访问名称」、§9.3 补 `status-icon` 与回车提交的施工口径；其余表单页的同类整改列入《前端规范待办》T-1
- 验证: `npm run test:tokens` 13/13（其中新增断言校验认证页有可见 label、`status-icon`、`@keyup.enter`，且不再出现字符串 `prefix-icon`）；`npm run build` 通过。回车提交与图标显示由用户在本机目视确认通过
- 相关文件: `src/views/Login.vue`、`src/views/Register.vue`、`标准前端视觉与交互设计规范.md`（§9.3 / §10.2 / 附录 A）
- 提交记录: 未提交
- 状态: 已修复

### BUG-044: 登录页角色下拉默认选中"管理员"，把普通用户导向鉴权失败

- 日期: 2026-09-28
- Bug 描述: `/login` 的角色下拉初始值写死 `ADMIN`。普通用户登录时若忘记切换身份，会带着 `role=ADMIN` 去鉴权；后端按 role 路由到不同表（`AuthController` → `adminService` / `cinemaService` / `userService`），于是要么报角色不匹配，要么在管理员表恰好存在同名账号时登进管理员 —— 默认值把一个"每次都存在的选择"变成了默认错误答案
- 根因分析: 默认值取的是三端里最少人用的角色。选择器本身不能删（后端按 role 路由到三张表，必须显式指定），所以问题只在默认值的取值方向
- 解决方案: `data.form.role` 由 `"ADMIN"` 改为 `"USER"`（最常见的登录身份），并把下拉项按 用户 / 电影院 / 管理员 排列。这样失败模式更安全：忘记切换只会得到明确的角色不匹配提示，而不会误登
- 验证: 构建通过；登录流程本身未改动（仍走 `useAuth.login`），由用户在 `/login` 目视确认默认选中"用户"
- 相关文件: `src/views/Login.vue`
- 提交记录: 未提交
- 状态: 已修复

### BUG-045: 登录 / 注册页页脚声明文字压在浅色插画上，对比度 1.41:1

- 日期: 2026-09-28
- Bug 描述: 页面底部的免责声明「本系统为个人学习项目…」几乎看不见 —— 它是浅灰字压在浅蓝插画上
- 根因分析: 该段文字用了 `--dark-text-secondary`(#cccccc)，而这是 §3.5 的深色表面令牌（设计用于 `#1a1a1a` 一类深底，那里是 10.84:1）。登录页背景是浅色插画（实测约 `#7FB2DC`），#ccc 压其上只有 1.41:1。这不是"换个颜色就能修"的问题：同一位置改压深色文字（`--el-text-color-regular` #606266）也只有 2.71:1 —— 直接往图片上放文字，深浅两头都到不了 4.5:1
- 解决方案: 经确认该段声明与前台页脚（`Front.vue` 第 133 行有更完整的同名声明）重复，按产品决定删除认证两页的该段文字及对应样式，不再往背景图上压文字。共用外壳里的相关样式一并移除，未留下无用类名
- 验证: `grep` 确认 `src/` 与 `tests/` 内已无残留引用；`npm run build` 通过
- 相关文件: `src/views/Login.vue`、`src/views/Register.vue`、`src/assets/css/auth-layout.scss`
- 提交记录: 未提交
- 状态: 已修复（按产品决定移除，而非配色补偿）

### BUG-046: 种子业务数据账实不符，票房与"今日票房"由虚构数值驱动

- 日期: 2026-09-29
- Bug 描述: 三处假数据同时存在。① `data.sql` 预置的 12 条订单/51 条评价与资金账本对不上；② 前台首页「今日票房」写死 `1.28亿`，刷新按钮用随机数改数字；③ 票房榜由 `film.box_office` 这个运行时从不重算的静态列驱动
- 根因分析: 三条互相独立的来源：
  1. 手写订单必然要伪造一整条链。`data.sql:172` 的 `ordered` 列清单不含 `unit_price`（单价快照全为 NULL）；`fund_flow` 一条种子都没有 —— 8 条带 `pay_time/pay_amount` 的"已支付"订单在账本里没有任何对应购票流水；`zhangsan` 余额 100.00 未因他那条 42 元订单扣减；订单号是 `202603058485`（12 位纯数字），而真实单号由 `OrderedService.generateOrderNo` 生成，是 `yyyyMMdd` + 8 位大写十六进制。三项互相印证即可判定为编造
  2. `front/Home.vue` 的 `totalPrice` 写死 `{total: 1.28, change: 5.3}`；`refreshTodayBoxOffice` 用 `Math.random()` 改这个数字并弹「已更新最新今日票房数据」—— 刷新按钮不请求任何接口，界面谎报数据新鲜度
  3. `film.box_office` 只由 `data.sql` 写入，`FilmMapper` 之外无任何代码重算它，前端也只读展示、无编辑入口 → 票房榜 100% 由虚构数值驱动。对比：`film.score` 是真的 —— 由 `MarkService` 按 `mark.score` 求均分回写
- 解决方案:
  1. `data.sql` 删除 `record`（15 行）/`ordered`（12 行）/`mark`（51 行）三块种子与恒为空操作的均分回写 UPDATE，并把 `film.box_office` 种子值清零；种子只留基础数据（管理员/用户/影院/影厅/影片/词表）
  2. `migration-20260928-p3` 删除镜像那 51 条评价的第 3 节 —— 否则它成了唯一还会造出假评价的地方
  3. 票房改为按 `ordered` 实时聚合（`FilmMapper.xml` 的 `filmRevenueJoin`，只统计 `待取票/已取票`），单位由「万元」改为元（本系统内的售票收入是几十到几百元量级，按万元渲染会恒显示 0.00万）；`film.box_office` 废弃并由 `migration-20260929-deprecate-box-office.sql` 清零存量值
  4. 新增只读统计接口 `GET /api/v1/statistics/overview`（仅 ADMIN），后台大盘不再由前端拉 films+cinemas+types 三张全表自己算
  5. 删除「今日票房」组件与其随机数刷新；补齐「暂无数据 / 数据加载失败，请稍后重试」的空态与异常态
  6. 新增 `scripts/seed-demo-data.py`：走真实接口生成 3 条已支付并取票的订单 + 3 条真实评价（lisi/wangwu 先经真实充值流程补足余额），并清理演示账号的旧数据
  7. 顺带修掉 `front/CinemaDetail.vue` 的「免费停车」硬编码卡（含具体地址，且该地址实为丁丁影城的信息，却对每家影院都展示）与 `roomId` 缺失时静默兜底成"一号厅"的跳转
- 验证: `mvn test` 154 例全绿；前端 `npm run build` + `test:tokens`/`test:inline`/`test:bundle` 全绿；E2E 70/70、并发 12/12（临时库 `xm_film_verify` + 备用端口 9191）；开发库 `xm-film` 实测落库 3 条真实订单（单号 `20260929B588E9EC` 格式、`unit_price`=场次票价、余额 100→55/60.5/48、购票流水 3 条 + 充值流水 2 条、`film.score` 回写 8.8/9.2/9.0）；`/statistics/overview` 返回 4 影院 + 45 条类型计数，USER 调用被 403 拒绝；票房榜返回 52.0/45.0/39.5 元三行。未做浏览器渲染验证（UI 目视由用户自查）
- 相关文件: `xm_film/sql/data.sql`、`xm_film/sql/schema.sql`、`xm_film/sql/migration-20260928-p3-review-score-cinema-audit.sql`、`xm_film/sql/migration-20260929-deprecate-box-office.sql`、`FilmMapper.xml`/`FilmMapper.java`、`CinemaMapper.xml`/`CinemaMapper.java`、`StatisticsController.java`/`StatisticsService.java`、`Film.java`、`front/Home.vue`、`front/Movie.vue`、`front/Rank.vue`、`front/CinemaDetail.vue`、`front/Cinema.vue`、`front/FilmCinema.vue`、`manage/Home.vue`、`utils/format.js`、`constants/index.js`、`scripts/seed-demo-data.py`、`scripts/verify/p4-*.py`
- 提交记录: 未提交
- 状态: 已修复

### BUG-047: 前台首页「今日票房」以真实数据源重新引入（修订 BUG-046 第 5 条的处置）

- 日期: 2026-09-29
- 问题描述: BUG-046 第 5 条把「今日票房」组件连同它的随机数刷新一起删掉了。删除是对当时那份假实现的正确处置，但组件本身是首页要有的功能 —— 于是需求重新提出：首页要有今日票房，且数据必须真实。
- 根因分析: 重新引入的难点不在组件，而在这个数没有任何现成来源：
  1. 唯一的聚合票房口径 `FilmMapper.xml` 的 `filmRevenueJoin` 只按影片累计，没有日期维度，取不出「今天」
  2. `/api/v1/statistics/overview` 是 ADMIN 专属，首页是公开页（游客可访问），用了就是把游客踹去登录页
  3. 订单相关端点全在 `AuthInterceptor` 覆盖范围内、未登录一律 401（`/api/v1/orders/` 不在 `PUBLIC_READ_PREFIXES` 里）
  4. 所以「从用户接口获取」必然意味着新增一个匿名可读端点
- 口径决策（两处都是有实际后果的分歧，不是形式选择）:
  1. 按 `pay_time`（收款日）取日，不按 `ordered.start`（放映日）。行业里「今日票房」通常指当日场次的票房，但本系统是提前购票：种子脚本把三个演示场次排在今天 +5/+6/+7 天（`scripts/seed-demo-data.py` 的 `SLOT_OFFSET_DAYS`），按放映日聚合会让这个指标长期恒为 0，失去展示价值。按收款日则它天然是累计票房的一个日期切片，必然 ≤ 累计票房，与首页已有的「总票房Top 10」同源可比。
  2. 空集上的 0 渲染成 `0.00元`，不是「暂无数据」。「今天还没卖出票」本身就是数据，空集 SUM 是 0 而非缺失。为此新增 `formatYuan` 而没有复用 `formatBoxOffice` —— 后者把 0 当缺失值返回「暂无数据」（票房榜靠 `rev.revenue > 0` 过滤，所以它从来看不到 0，不能改它）。
- 解决方案:
  1. `OrderedMapper.selectTodayPaidRevenue()`：单条 SQL 同时算出金额与统计时刻，日期边界与时间戳同出一个库时钟（`CURDATE()` + `NOW()`），不会出现"边界按 23:59 切、时间戳按另一台钟写"的分叉；`status IN ('待取票','已取票')` 与 `filmRevenueJoin` 同源并注释互相指认
  2. 端点挂在 `GET /api/v1/films/box-office/today` —— `/api/v1/films` 本来就在 `PUBLIC_READ_PREFIXES` 内，因此没有为它新增任何放行规则，也没有碰 `excludePathPatterns`（那是角色盲区，见 BUG-036）；与同类的 `/box-office/top` 毗邻
  3. 返回 `Map` 的别名刻意写 camelCase：`map-underscore-to-camel-case` 只转换 bean 属性、不转换 Map 的 key，写 `updated_at` 出去就是 snake_case，与全站 camelCase 不一致
  4. 前端 `utils/format.js` 新增 `formatYuan`；`front/Home.vue` 卡片挂在 `.home-aside` 最上方，刷新按钮这次真的重新请求（BUG-046 的病灶正是刷新按钮不请求任何接口却弹「已更新最新今日票房数据」）；失败只落错误态不弹提示（网络类提示由 `request.js` 统一给，规范 §11.2）
  5. 色条用 `--el-color-primary`（#BF352D）而非 `--color-brand`（#ef4238）：色条上压的是白字，`#ef4238` + 白字只有 3.81:1 不达 AA。规范 §3.2 正是为此把品牌色拆成"承文字 / 不承文字"两枚令牌，BUG-046 删掉的那版用的是 `#ef4238` + 白字，属违规
- 验证: `mvn test` 155/155（新增 1 例，钉住服务层只转发、不改写金额）；前端 `npm run build` + `test:tokens`/`test:inline`/`test:bundle` 全绿，编译产物实测色条是 `var(--el-color-primary)`、无任何硬编码 hex；口径打真实库验证（临时库 + 备用端口 9191，28 项断言全过、连跑两次一致）：今天支付+待取票 ✅计入、今天支付+已取票 ✅计入、今天支付后退票 ✗排除、`pay_time` 挪到昨天 ✗排除（证明取的是收款日而非场次日）、待支付 ✗排除、匿名 GET 返回 200。未做浏览器渲染验证（UI 目视由用户自查）
- 验证脚本: 本次验证用的是临时脚本，按项目要求用完即删，仓库内不再保留 —— 上面这些口径结论无法从仓库里重跑，要复现请按同样的 5 类订单（今天支付待取票/今天支付已取票/今天支付后退票/`pay_time` 挪到昨天/今天下单未支付）在临时库上重建
- 相关文件: `OrderedMapper.java`/`OrderedMapper.xml`、`OrderedService.java`、`FilmController.java`、`OrderedServiceTest.java`、`vue/src/constants/index.js`、`vue/src/utils/format.js`、`vue/src/views/front/Home.vue`
- 提交记录: 未提交
- 状态: 已修复

### BUG-048: 用户端没有取票通路，评价闭环从未对用户打开

- 日期: 2026-09-29
- Bug 描述: 用户在真实走完购票流程后发现，余额支付成功后跳到「购票记录」，看到的只是 `待取票` 状态，界面里没有任何"接下来去哪取票"的路径。初看是跳转体验问题，实际是功能缺失。
- 根因分析: `OrderedService.pickupOrder` 第 3 行就对 USER 硬拦截 ——
  ```java
  if ("USER".equals(role)) { throw new CustomException(ErrorCode.FORBIDDEN, "用户无权执行取票操作"); }
  ```
  取票只有 ADMIN / CINEMA 能做，入口在 `back/Ordered.vue` / `manage/Ordered.vue` 的订单列表里。而 `front/Orders.vue` 的「去评价」按钮只对 `已取票` 渲染。串起来就是：普通用户买完票 → 状态永远停在 `待取票` → 永远看不到「去评价」→ 评价闭环对用户端从未打开过。所有人都只在后台点「取票」时才会走通，前台缺半条链路。
- 另一个同源缺陷: `MarkService`（120 行）全文没有任何一处引用订单 —— 零命中 `ordered` / `OrderStatus` / `PICKED_UP`。"已取票才能评价"此前只是 `front/Orders.vue` 的按钮可见性，服务端不校验，直接 `POST /api/v1/marks` 能给任何没买过票的影片打分。
- 解决方案:
  1. `ordered` 加 `pickup_code`（唯一列），取票码在 `payOrder` 内与扣款同一事务生成 —— 不存在"扣了钱没码"或"有码没扣钱"，且未支付的订单永远没有码。码形 `XXXX-XXXX`，生成字母表剔除 `I/L/O/0/1`（人工从手机抄到自助机上看不错）。
  2. 新增 `POST /api/v1/tickets/redeem`（免登录）+ 前台「取票大厅」`front/Pickup.vue`（`meta.guest`，导航对游客可见）模拟影院自助机。入参只有 code、没有 orderId —— 码本身即凭证，允许传 orderId 就等于谁都能核销别人的单。
  3. 码不带任何有效/失效标记：核销只接受 `status = '待取票'`（`markPickedUpByCode` 的状态条件更新）。这一个谓词同时实现「一单一码」「用过即废」「退票/取消作废」「没付款不出发」。有效期到放映结束，由 `ordered.start` + `film.time` 派生。用户曾提出"为期一天"，但直译成支付后 24 小时会让当前种子数据（场次在 +5~+7 天）的码在开映前就过期 —— 比不做更糟，故改为"到放映结束"。
  4. 并发重复核销不靠悲观锁：读到的状态可能是 `待取票`，写库走 `UPDATE ... WHERE status = '待取票'`，受影响 0 行即判定被人抢先（与余额扣减的 `WHERE balance >= ?` 同一手法）。
  5. `AuthInterceptor` 新增 `ANONYMOUS_WRITE_EXACT`（精确路径 + 仅 POST）—— 本仓库第一个匿名写入口。三处刻意收窄：精确匹配而非前缀、只放行 POST、不与 `PUBLIC_READ_PREFIXES` 合并（读放行的依据是"内容本来公开"，写放行的依据是"动作由凭证授权"，混在一起会让人以为写操作只要前缀命中即可放行）。为何安全逐条论证写在 `TicketController` 的类注释里，并有 4 个单测钉住三处收窄。
  6. 支付成功后的落地从"跳订单列表"改为 `OrderPayDialog` 就地切成取票凭证态（取票码 + 场次座位 + 「去取票大厅」），码由 `GET /api/v1/orders/{id}` 回查（该查询已 join 出影片/影院/影厅名）。
  7. `MarkService.add` 补上服务端门禁：`countPickedUpByUserAndFilm(userId, filmId) > 0`。修改评价不重复校验 —— `已取票` 是终态，退票与删除都进不来，资格一旦成立不会被推翻。
- 踩到的坑（留给后来者）: 本仓的业务异常经 `GlobalExceptionHandler` 返回的是 HTTP 200 + body 里的 code，只有 `AuthInterceptor` 才直写 401/403。本次的验证脚本第一版按 HTTP 状态码断言，6 条用例全红而产品行为其实全对【详见 规则 15】。
- 验证: `mvn test` 173/173（OrderedServiceTest 42→53、MarkServiceTest 13→15、AuthInterceptorAccessTest 30→35）；前端 `npm run build` + `test:tokens`/`test:inline`/`test:bundle` 全绿，编译产物实测 `Pickup` 分块里是真 `post(REDEEM, {code})`、卡片无硬编码色值；全链路打真实库验证（临时库 + 备用端口 9191，39 项断言全过、连跑两次一致；改动后又各跑一遍仍是 39/39，并回归跑了今日票房那 28 项确认本次 schema/实体/XML 改动没影响它）：未支付无码 ✅、码形态与字母表 ✅、全程不带 Authorization 核销成功 ✅、凭条不含 orderId/订单号/金额/userId（逐字段断言）✅、小写去横杠也能核销 ✅、同码再核销 409 ✅、退票后 409 ✅、放映结束后 409 ✅、无效码 404 / 长度不符 400 / 空码 400 ✅、未取票评价被拒 → 取票后评价成功 ✅；另单独造了一个"升级前的库"（无 `pickup_code` 列 + 待取票与已退票各一条）验证迁移：待取票订单被补上 `XXXX-XXXX` 形态的码、已退票保持 NULL、重跑既不报错也不改写已有码（换出的码不能用固定值断言 —— 取值含 `RAND()`/`UUID()`，每次补出来的都不同）；补出来的码走状态条件更新仍是 1 行/0 行。迁移还给存量 `待取票` 订单补码，跑完后在真实开发库 `xm-film` 上实测：2 条待取票订单各得一枚码，4 条已取票 / 2 条已取消保持 NULL。未做浏览器渲染验证（UI 目视由用户自查）
- 验证脚本: 同 BUG-047，本次用的是临时脚本，按项目要求用完即删，仓库内不再保留
- 相关文件: `xm_film/sql/schema.sql`、`xm_film/sql/migration-20260929-pickup-code.sql`、`Ordered.java`、`OrderedMapper.java`/`.xml`、`OrderedService.java`、`MarkService.java`、`RecordService.java`（`DEFAULT_DURATION_MINUTES` 提为 public 复用）、`AuthInterceptor.java`、`TicketController.java`、`dto/request/TicketRedeemRequest.java`、`dto/response/TicketVoucher.java`、`OrderedServiceTest.java`、`MarkServiceTest.java`、`AuthInterceptorAccessTest.java`、`vue/src/constants/index.js`、`vue/src/router/index.js`、`vue/src/views/Front.vue`、`vue/src/views/front/Pickup.vue`、`vue/src/views/front/Orders.vue`、`vue/src/views/front/BuyTicket.vue`、`vue/src/components/OrderPayDialog.vue`
- 提交记录: 未提交
- 状态: 已修复

### BUG-049: 前台购票记录的取票 / 退票按钮折行后左右错开

- 日期: 2026-09-29
- Bug 描述: `front/Orders.vue` 购票记录表里，订单处于 `待取票` 时操作列同时有「取票」与「退票」两个文字按钮，用户反馈"因为空间有限…上下或左右无法对齐，极其影响视觉观感"。同表的 `待支付` 行「继续支付 / 取消」是同一个缺陷（更宽、错位更明显），一并修掉。
- 根因分析: 三层原因叠加，缺一不可。
  1. 操作列分不到宽度 —— `el-table` 给未显式指定 `width` 的列按 `minWidth || 80` 起算（`element-plus/es/components/table/src/table-layout.mjs:102`），并把富余空间按各列 `minWidth` 权重分摊（`:96` 把无 `width` 的列全归入 `flexColumns`，`:106` 起分摊）。本表 13 列全部未定宽，最小总宽 13×80 = 1040px，而容器是 `.page-wide`（`min(85vw, 1200px)`）再减去 `.card` 的 8px 内边距 —— 1920 视口下也只有 1184px，富余 144px 摊完操作列约 89px，去掉 `.cell` 的 24px 内边距只剩 65px。
  2. 两个文字按钮需要 ~100px —— `.el-button.is-link` 是 `padding: 2px` 的内联元素：4 字按钮（继续支付 / 修改评价）= 56px + 4px，2 字按钮（取票 / 退票 / 取消）= 28px + 4px；按钮间距来自 EP 的 `.el-button+.el-button{margin-left:12px}`。于是 `继续支付 + 取消` 需 104px、`取票 + 退票` 需 76px，都超过 65px，必然折行。
  3. 错位的直接原因：那 12px 是 `margin-left`，折行并不改变它。第二个按钮落到第二行时仍带 12px 左边距，两行右错开 12px —— 用户看到的"上下无法对齐"就是这个。
- 解决方案:
  1. 操作列显式定宽 `width="140"`（最宽组合 104px + 24px 内边距 = 128px，留 12px 富余）。
  2. 按钮组套上 `front-pages.scss` 新增的 `.row-actions`：`display: flex; gap: var(--space-8)`，并把 `.el-button + .el-button` 的 `margin-left` 中和为 0。间距从"相邻选择器的边距"变成"容器 gap"，于是间距与"是否折行"解耦 —— 一行时是 8px 等距，真折行时第二行也从同一左边缘起排。
  3. 定宽多出来的 60px 由两列让出：展开列 `min-width="60"`、单价列 `min-width="70"`（两列的内容本来就不需要 EP 的 80px 下限）。表格最小总宽只从 1040px 涨到 1070px，横向滚动条的触发阈值（约 1242px 视口）几乎不动 —— 若单纯把操作列加宽 60px 而不让宽，1280px 这类常见视口会凭空多出一条横向滚动条。
- 哪些列能让宽、哪些不能: 展开列没有表头文字、单价列表头只有 2 个字，压到 60 / 70 仍有余量。其余列的下限被内容或表头顶住 —— `电影图片` 表头 4 字就要 56px + 24px 内边距，压到 80px 以下表头即折行；`总费用` 可能出 `5994.00` 这类 7 位金额、`订单号` 是 16 位单号（本就在折叠行里被截断），压到 70px 会新造出"金额换行 / 单号截得更狠"。宁可让最小总宽多 30px，也不制造新的折行。
- 验证: 前端 `npm run test:tokens`(13) / `test:inline`(2) / `test:bundle`(3) 全绿，`npm run build` 通过。构建产物客观核对：`dist/assets/index-*.css` 内含 `.front-content .row-actions{display:flex;flex-wrap:wrap;align-items:center;gap:var(--space-8)}` 与 `.front-content .row-actions .el-button+.el-button{margin-left:0}`（特异性 0-4-0，压得住 EP 的 0-2-0）；`dist/assets/Orders-*.js` 内含 `label:"操作",width:"140"`、`label:"单价"…"min-width":"70"`、`type:"expand","min-width":"60"`。未做浏览器渲染验证（UI 目视由用户自查），两列让宽后各列的实际分配是按 el-table 布局算法推算的，未经浏览器实测。
- 相关文件: `vue/src/views/front/Orders.vue`、`vue/src/assets/css/front-pages.scss`
- 提交记录: 未提交
- 状态: 已修复

### BUG-050: 管理员可一键把任意用户的票标记为已取

- 日期: 2026-09-29
- Bug 描述: `manage/Ordered.vue`（管理员订单列表）对 `待取票` 行渲染「取票」按钮，点一下就把别人的订单置为 `已取票`。用户提出：票是用户的、取票也是用户的事，管理员界面上不该有这颗按钮。
- 根因分析: 权限判定把"能不能访问这一行"和"能不能执行取票这个动作"混成了一件事。
  1. `ensureOrderAccess`（`OrderedService.java:519-521`）对 `ADMIN` 直接 `return`，不做任何归属校验 —— 管理员对任意订单都有访问权，这是"管理所有数据"该有的。
  2. `pickupOrder`（`:469-484`）却只做了一条 `if ("USER".equals(role)) throw` 的反向判断，于是 `ADMIN` 与 `CINEMA` 一起被放行。ADMIN 的访问权因此被顺带翻译成了"替任意用户确认取票"的操作权。
  3. 三层放大：① 该方法不记录操作人（只写 `status`），伪造后查不出是谁点的；② `已取票` 是终态、没有任何出口（`redeemByCode` 对已取票直接拒、`cancelOrder` 的前置是 `待支付`），所以这个能力连纠错用途都没有，只能提前截胡真实柜台的交付；③ 它会顺带伪造评价资格 —— `MarkService.add` 的门槛正是"该用户对该影片有已取票订单"（`countPickedUpByUserAndFilm`），管理员一点，那个用户就凭空获得给这部片打分的资格。
  4. 旁证（说明它并非有意设计）：管理端订单页只接了 `PICKUP` 一个状态操作，金额更大的 退款 / 取消 都没接（`REFUND`/`CANCEL` 只出现在 `front/` 两页）—— 钱不动的取票给了管理员、钱动的退款不给，本身就不自洽。
- 解决方案:
  1. `pickupOrder` 改为只放行 `CINEMA` 的白名单：`if (!"CINEMA".equals(role)) throw FORBIDDEN`。不要改成"再补一条拒 ADMIN" —— denylist 在新增角色时会静默把取票能力一并授予新角色，正是 `MarkService` 那轮"只有前端按钮在守"的同一种漏。`CINEMA` 保留，因为它本来就被 `ensureOrderAccess` 限制在本影院的订单上，是真实的柜台员工。
  2. `manage/Ordered.vue` 删掉按钮、`pickupOrder()` 处理器与随之失效的 `ORDER_API` 导入。只删按钮是错解（会退化成"只有前端在守"）—— 权限落点始终是服务端的这一个方法：`AuthInterceptor` 的 `ADMIN_ONLY_PREFIXES`/`ADMIN_WRITE_PREFIXES` 都不含 `/orders`（只要求登录），`OrderedController.pickup` 只做转发。
  3. `back/Ordered.vue`（影院端）不动 —— 影院端本来就该有，且受 `cinemaId` 约束。
  4. 错误文案改成有指向性的"取票为影院柜台操作，请到取票大厅凭取票码自助取票"，把用户导向免登录的自助通路。
- 代价（明确接受）: 管理员从此不能代客取票。本系统里不算损失 —— "用户到店取票"由取票大厅覆盖（免登录、凭码），"柜台取票"由影院端覆盖；唯一受影响的是演示时想用 admin 账号把某条订单推到 `已取票`，改用取票大厅的码即可，反而把真实自助通路演到位。未新增"admin 只读看取票码"之类的补救功能（admin 要查码需先有正当场景，真有需求再单独提）。
- 验证: `mvn test` 173/173 全绿（13 个测试类，Failures 0 / Errors 0）—— `adminPickupOrderSuccessfully` 反转为 `adminCannotPickupOrder`（断言 FORBIDDEN 且 `verify(..., never()).updateById(any())` 守住"拒绝时不写库"）；`userCannotPickupOrder` 的断言随文案从匹配"无权"改为匹配"影院柜台"；`cinemaPickupOrderSuccessfully` 继续守住影院端不受影响。前端 `npm run build` 通过 + `test:tokens`/`test:inline`/`test:bundle` 全绿，构建产物核对 `manage` 分块里已无 `PICKUP` 调用。未做浏览器渲染验证（UI 目视由用户自查）。
- 相关文件: `OrderedService.java`、`OrderedServiceTest.java`、`vue/src/views/manage/Ordered.vue`、`CLAUDE.md`
- 提交记录: 未提交
- 状态: 已修复

### BUG-051: 点赞并发时回读到旧快照，返回了与事实相反的 liked

- 日期: 2026-09-29
- Bug 描述: 多个请求几乎同时点赞同一条评价时，库里正确只留下 1 行（主键去重生效），但其中一部分响应报 `liked=false` / `likeCount=0`。前端把响应当权威值写回该行，于是"刚点上的赞"显示成没点上，刷新才恢复。真库复现（`scripts/verify/p5-mark-like.py` 并发段）：5 个并发响应里 4 个报 `liked=false`。
- 根因分析: `MarkService.setLike` 承诺"回读写库后的权威状态"，但它用的是一致读（`SELECT ... COUNT(*)`），而 MySQL 默认隔离级别 REPEATABLE READ 让一个事务的所有一致读共享同一条快照，该快照在事务的第一条一致读时就已经固定。`setLike` 的第一条一致读是 `requireExisting(markId)`（`selectById`）；随后 `insertIfAbsent` 撞上另一个尚未提交的同键事务时会阻塞到对方提交之后才返回 —— 快照却仍是那条早于对方提交的旧快照。此后再 `COUNT`，读到的还是"没有这一行"。库里有行、回读说没有，与方法自己的承诺正好相反。这不是主键去重的问题（去重是对的），是"回读"这一步读到的不是当前状态。
- 解决方案: `setLike` 显式声明 `@Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)`（`MarkService.java`）。READ_COMMITTED 下每条语句取最新已提交快照，回读才是本方法真正需要的"权威状态"。刻意不用锁定读（`SELECT ... FOR UPDATE`）来纠正：那会锁住该评价行，把同一部片子上所有人的点赞串行化 —— 用一个写热点换一次回读，代价不成比例。
- 验证: `mvn test` 193/193 全绿（14 个测试类，Failures 0 / Errors 0）—— 但隔离级别 Mockito 验不了（打桩后测的是桩，不是事务边界），真正守住这条的是真库脚本 `scripts/verify/p5-mark-like.py`：74/74 断言（连跑两次），并发段 5 个响应一致报 `liked=true` / `likeCount=1`。修复前该段稳定复现 4/5 报 `liked=false`。
- 相关文件: `xm_film/springboot/src/main/java/com/example/springboot/service/MarkService.java`、`scripts/verify/p5-mark-like.py`
- 提交记录: `cade086d`
- 状态: 已修复

### BUG-052: 影评页入口写成具名路由，点击即「页面渲染异常」

- 日期: 2026-09-29
- Bug 描述: 影片详情页「查看全部 N 条评价」与购票记录「去评价 / 修改评价」两处入口，点击后落到 ErrorBoundary 的「页面渲染异常 / 组件加载时发生了意外错误，请尝试刷新」，控制台报 `No match for {"name":"filmMarks","params":{"id":"22"}}`。影评页本身是好的（直接访问 `/front/filmMarks/26` 正常渲染），坏的是跳转。由用户在浏览器目视时发现。
- 根因分析: 两处写的是 `router.push({ name: 'filmMarks', params: { id } })`，而本仓库的路由一律没有 `name` —— `router/index.js` 里只有 `meta.name`，那是页面标题文案（'影评' / '电影详情'），不是路由名；全仓 30 余条路由没有一条声明过 `name`。具名路由解析不到时 Vue Router 4 抛 `No match for ...`，冒到渲染层就成了兜底错误页。仓库既有导航全部走 path（`Movie.vue` 的 `` `/front/filmDetail/${filmId}` ``、`FilmCinema.vue` / `Home.vue` 的 `{ path }`），本次是唯一一处具名写法。源头在设计方案里就写了 `{ name: 'filmMarks', params: { id } }`，实现与复核都照着抄，没有人核对过这个 name 是否存在 —— 这是"照着规格实现"在规格本身出错时的失效模式。
- 解决方案: 两处改为按 path 跳转 `` router.push(`/front/filmMarks/${filmId}`) ``，与仓库既有写法一致；并在该函数上留一行注释点明"本仓库路由没有 name"，避免下一个人再写具名。
- 验证: 改后 `FilmDetail.vue` / `Orders.vue` 在 dev server 下仍正常编译（HTTP 200）；跳转本身由用户在浏览器点验 —— 渲染层只有真的点一下才算验过。
- 为什么四道自动化都没拦住: `npm run build`、`test:tokens` / `test:inline` / `test:bundle`、dev server 的模块编译，验证的都只是"能编译"，没有一道验证"路由名能解析"；193 个后端用例与 74 个真库断言又都在服务端，看不见前端路由表。编译类检查替代不了"真的点一下" —— 这与 BUG-049 同属"构建产物核对过了、但没人用眼睛看"的一类。
- 相关文件: `xm_film/vue/src/views/front/FilmDetail.vue`、`xm_film/vue/src/views/front/Orders.vue`
- 提交记录: `72988a6c`
- 状态: 已修复

### BUG-053: 评分榜读的是种子里的编造分数，与有没有真实评价无关

- 日期: 2026-09-29
- Bug 描述: 开发库重建后 `mark`（评价）0 行、`ordered`（订单）0 行，前台评分榜仍列出 9.6 / 9.2 / 9.1 / 8.8 / 8.7 五部影片，影片列表、搜索结果、详情页、影评页也都带着分数。用户指出「数据已经清零，评分榜还有数据」。数据库现场：`mark`=0 行、`film.score > 0`=17 行，榜单值与该列逐一对得上。
- 根因分析: 评分榜查询 `selectMarkTop` 只做 `WHERE film.score IS NOT NULL ORDER BY film.score DESC`，整条 SQL 不读 `mark` 表；而 `film.score` 的初值是 `data.sql` 里人工填死的静态值（17 部影片每部带一个 9.x/8.x）。`MarkService` 只在评价增删改时经 `FilmMapper.recalculateScore` 回写均分，且带 `AND EXISTS (SELECT 1 FROM mark ...)` 守卫（无评价时不写），于是那个编造初值永远留存、不需要任何评价就能上榜。与 BUG-046 废掉的 `film.box_office` 是同一族：一个"人工填、没人重算"的列被当成真实业务数据展示。`film.score` 比它更隐蔽 —— 它确实有个写者，只是那个写者从不处理"没有评价"的情形。
- 解决方案: 让 `film.score` 成为纯派生列，四个入口一起堵：
  1. `data.sql` 的 17 部影片 `score` 改显式 `NULL`；`schema.sql` 该列去掉 `DEFAULT 0.0`（否则"新增影片不带评分"会落成「0 分」，而 `0.0` 是合法的真实评分，与 NULL 是两回事）
  2. `recalculateScore` 去掉 `AND EXISTS` 守卫 —— 无评价时写 NULL。该守卫是"保留基线分"设计的产物，而基线分本身就是编造值；不删它则"删光某片评价后旧均分残留"
  3. `selectMarkTop` 加 `EXISTS (SELECT 1 FROM mark ...)` 谓词，与票房榜 `WHERE rev.revenue > 0`（无售票不上榜）同构 —— 这是第二层保险，不靠"score 恰好为 NULL"这条不变量独担
  4. 删掉 `FilmMapper.xml` 里 insert/updateById 的 score 分支 —— `FilmController` 继承 `BaseController` 的通用 `POST/PUT /api/v1/films` 直收整实体，留着那个 `<if>` 分支等于任何人能手写一个评分
  前端同时把 9 处渲染改走新增的 `utils/format.js#formatScore`，并删掉 `{{ film.score || 0 }} 分` 与 `score: data.score || 0` 这类兜底 —— 它们把"无评分"渲染成「0 分」，凭空造出第二个假分数。
- 验证: `mvn test` 193/193；`npm run build` 通过、产物含 `暂无评分`。真实闭环全部在临时库 `xm_score_verify` + 备用端口 9191 上跑（用户的 9090 / 5173 未碰）：影院建排片 → 用户下单 35.00 → 支付（余额 100→65）→ 匿名核销取票（码 `A5UF-N9RS`）→ 支付后尚无评价时 `film10.score` 仍为 NULL、榜单仍 `[]` → 发表 8.5 → `film10.score`=8.5、榜单只出现这一部 → 第二个用户（先经充值 200 入账）评 9.5 → 均分 9.0、榜单 9.0 → 删掉一条 → 9.5 → 删光两条 → `film.score` 回 NULL、该片离开榜单；`PUT /api/v1/films` 带 `score:9.9`、`POST` 新增影片带 `score:9.9` 均不落库；整实体 PUT 编辑影片仍正常（200，字段照改、score 不被改写）；匿名可读评分榜 200（未新增任何放行规则）。未做浏览器渲染验证（UI 目视由用户自查）。
- 顺带发现（未修，非本次引入）: 通用 `PUT /api/v1/{resource}` 只传 `{id}`（无任何可更新字段）时 `<set>` 为空，生成 `UPDATE film SET WHERE id=?` 直接 500。既有行为、任何资源都如此；本次改动只是让 `score` 也进入"不可更新字段"集合（`{id,score}` 的 PUT 因此也落到这条路径）。修法应收敛在 `BaseController`/`BaseService`（无字段可更新时返回 400），属另一件事。
- 相关文件: `xm_film/sql/data.sql`、`xm_film/sql/schema.sql`、`xm_film/springboot/src/main/resources/mapper/FilmMapper.xml`、`xm_film/springboot/src/main/java/com/example/springboot/mapper/FilmMapper.java`、`xm_film/vue/src/utils/format.js`、`xm_film/vue/src/views/front/{Movie,Search,Home,Rank,FilmDetail,FilmCinema,CinemaDetail,FilmMarks}.vue`、`xm_film/vue/src/views/{manage,back}/Film.vue`
- 提交记录: 未提交
- 状态: 已修复

### BUG-054: 无 `.env` 的构建把 http://localhost:9090 烘进上传端点与头像地址

- 日期: 2026-09-29
- Bug 描述: 全新克隆或 CI（没有 `.env`）执行 `npm run build`，产物里文件上传端点是 `http://localhost:9090/api/v1/files/upload`，头像也被拼成 `http://localhost:9090/files/...`。前者把上传请求发到访问者本机，后者在生产直接取不到图。开发机上完全看不出来 —— 本机有 `.env`（`VITE_API_BASE_URL=/`），Vite 把 `"/" || '…'` 常量折叠，那个 localhost 字面量成了死代码，`grep dist` 是 0 命中。
- 根因分析: `VITE_API_BASE_URL` 有三个读取点、两套兜底值 —— `utils/request.js` 用 `'/'`，`constants/index.js` 与 `views/Front.vue` 用 `'http://localhost:9090'`。BUG-023 取消 `.env` 跟踪时只改了 `request.js` 的回退值，另外两处沿用了跨域开发时代的老兜底。第二个独立缺陷：`Front.vue` 把头像与 base 直接字符串相加，同源（`/`）时会拼出 `//files/...` —— 浏览器按协议相对 URL 解析，指向 host `files`，即便兜底值正确也会坏。`data.sql` 里 41 处文件引用全是 `/files/...` 相对路径、零 `http://`，且 `Back.vue`/`Manage.vue` 及其余 15+ 处都直接绑定，可见"存相对路径、直接绑定"本就是全站约定，`Front.vue` 的拼接是异类。
- 解决方案: 收敛为单一来源，两个缺陷一起堵：
  1. `constants/index.js` 兜底改为 `'/'`，成为全仓唯一读取点
  2. `utils/request.js` 改为 `import { API_BASE_URL } from '@/constants'`，不再自己读环境变量（`@/constants` 不含任何 import，不构成循环依赖）
  3. `views/Front.vue` 的 `userAvatar` 去掉拼接，直接 `user.value?.avatar || null`，与其余视图一致；注释写明不要在这里拼 `API_BASE_URL`
  4. `env.d.ts` 类型改为 `string | undefined` —— 原本声明成 `string` 会让下一个读者以为它一定有值
- 验证: 空变量构建（`VITE_API_BASE_URL= npm run build`，等价于无 `.env` 的全新克隆/CI）：修复前产物含 `http://localhost:9090` 2 处（`Front-*.js` 的头像、`index-*.js` 的 `As = ${Yr}${FILES}`），修复后 0 处，且上传端点折叠为 `As = T.FILES`（相对路径）、axios `baseURL` 取自同一个 `API_BASE_URL`。跨域开发分支回归：`npm run build -- --mode development`（加载 `.env.development`）产物仍为绝对地址，未被破坏。正常 `npm run build` 通过。未做浏览器验证（UI 目视由用户自查）。
- 相关文件: `xm_film/vue/src/constants/index.js`、`xm_film/vue/src/utils/request.js`、`xm_film/vue/src/views/Front.vue`、`xm_film/vue/src/env.d.ts`、`README.md`
- 提交记录: 未提交
- 状态: 已修复
- 同族未修（记录备查）: 开发环境的 `file.access-prefix` 是绝对值（`fileBaseUrl = http://localhost:${server.port}`），所以在开发环境上传的文件会在库里存成绝对 URL；把这样一个开发库直接部署到生产，图片会指向 localhost。生产 profile 的 `FILE_ACCESS_PREFIX` 默认 `/files/`，故生产环境产生的数据不受影响。要不要让开发环境也返回相对路径，属另一件事。

### BUG-055: 第二轮前台视觉重构 — 顺带查出的一批前端缺陷（导航高亮错项 / 开放重定向 / 动画静默失效 / 幽灵登录态）

- 日期: 2026-10-06
- Bug 描述: 本轮以「前台 15 页 + 三端外壳 + 认证两页」为范围做视觉重构（明亮猫眼方向）。重构过程中逐文件核对，除观感问题外还查出 6 个功能/安全缺陷与 3 类规范偏差，全部一并处置。逐条如下。
- 缺陷与根因:

  1. **导航高亮点错项**（`Front.vue`）。`updateActivePath` 用一串 `path.startsWith('/front/xxx')` 逐段赋值给 `activePath` 字符串，兜底分支是 `path.startsWith('/front')` → `'/home'`。`/front/pickup` 不匹配前面任何一段、落进兜底，于是「取票大厅」页把「首页」点亮；而导航里「取票大厅」项的高亮条件 `activePath === '/front/pickup'` 永远不会成立。根因是"手写同步一份路由→高亮映射"这种写法漏一段就静默出错，且没有任何检查能发现。改为数据驱动的 `NAV_ITEMS` + `NAV_SECTIONS`（每个导航段声明自己拥有哪些路由前缀），高亮从 `route.path` 现算，`activePath` / `updateActivePath` / 对应的 `watch` 一并删除。影片详情 / 选择影院 / 影评归「电影」，影院详情归「影院」；只在头像下拉里的个人中心与修改密码不属于任何段，保持无高亮。

  2. **登录回跳是开放重定向**（`Login.vue`）。`const redirect = redirectParam.startsWith('/') ? redirectParam : getDefaultPath(role)` 之后 `window.location.href = redirect`。`'//evil.com'` 同样以 `/` 开头，通过 `startsWith('/')` 后按协议相对 URL 解析，直接跳到外站。改为 `router.push`（Vue Router 只解析站内路径）并显式排除 `//` 前缀；顺带免掉登录后的整页重载。`Register.vue` 的注册成功跳登录同样由 `location.href` 改为 `router.push`。

  3. **转圈动画静默失效**（`CinemaDetail.vue`）。`.record-placeholder__icon { animation: rotating 2s linear infinite }` 引用的 `@keyframes rotating` 只在 `BuyTicket.vue` 的 `<style scoped>` 里定义过。scoped 块的关键帧会被改写成 `rotating-<组件哈希>`，CinemaDetail 引用不到，动画不生效 —— 编译、构建、产物核对全都不报错。修法：关键帧移到 `global.css`（引用它的 scoped 块因块内无同名定义而保留原名，正好命中全局那条），并删掉 BuyTicket 的本地副本，全站一处定义。

  4. **幽灵登录态被整页重载掩盖**（`front/Password.vue`）。改密成功后是 `clearStoredUser()` + `location.href = '/login'`。`clearStoredUser` 只清 localStorage，而登录态的唯一依据是 `useAuth` 里那个模块级 `globalUser` ref —— 内存仍持 token，`isLoggedIn` 继续为真、外壳继续渲染成已登录。整页重载正是为了盖住这一点。改为 `useAuth().logout()`（同时清单例与副本）+ `router.push('/login')`。**这一条是"用重载绕过状态同步缺陷"的典型**，同一形状还出现在 `Front.vue` 的搜索跳转上（`window.location.href` 做站内跳转，纯属多余）。

  5. **共享类名与组件 scoped 同名冲突**。`.empty-hint` 在 `front-pages.scss`（`.front-content .empty-hint`，0-2-0，单行「暂无数据」占位）与 `CinemaDetail.vue` / `FilmCinema.vue` 的 scoped 块（`.empty-hint[data-v-xxx]`，同为 0-2-0，带标题与说明的虚线面板）里各有一套样式。特异性相同，胜负只取决于产物里的先后顺序。两条虚线面板改名为 `.empty-panel`（含 `__title` / `__desc`）。

  6. **`min-height: 100vh` 把页脚顶出视口**（`front/Person.vue`）。`.front-person-container` 挂在外壳 `.front-content` 之内，再声明一次 `100vh` 就凭空多出外壳头部（60px）的高度，页脚被推到视口之外。页面底色（`--el-fill-color-light`）也一并去掉 —— 前台底色由外壳统一定，见下条。
- 规范偏差与处置:

  1. **前台底色不符 §6.2**。`body` 取的是 `--el-bg-color-page`（`#f2f3f5` 灰），而 `.front-container` / `.front-content` 此前没有任何样式，所以整个前台其实是灰的，与「前台页面背景 `#ffffff`」相悖。改在 `Front.vue` 的外壳上铺白，并加 flex 纵向布局 + `min-height: 100vh`，内容区 `flex: 1`，短页面时页脚贴底。
  2. **两处硬编码色值**（§3.7）。`CinemaDetail.vue` 影院服务卡的 `rgba(255,255,255,.1)` —— §3.5 的深色表面族里没有"深底上的浅色面层"这一类令牌（`--surface-glass` 是 80% 白，专供压在照片上，用这里会过亮），改为不设底色，分组感由彩色标题片与间距提供；`Search.vue` 评分上的 `text-shadow: 0 1px 2px rgba(0,0,0,.6)` —— 改为把评分放进 `--overlay-mask` 衬底的小胶囊（与海报卡同一语言），不再需要给文字描边，也就没有手写阴影（§7.2）。
  3. **`opacity` 当文字对比度调节器**（§10.1）。服务卡说明 `opacity: .9`、面板说明 `opacity: .7`、统计标签 `opacity: .8` 一律换成令牌：深底次级文字用 `--dark-text-secondary`（10.84:1），说明文字用 `--el-text-color-regular`（6.11:1）。`opacity` 只保留给纯装饰块。
- 本轮重构的落地口径:

  - **新增共享单元 `components/FilmPosterCard.vue`**：2:3 `aspect-ratio` 海报 + 片名 + 破图兜底（取片名首字，与 `.mark-item__avatar` 同手法）+ 评分角标 + hover 品牌色购票提示 + 默认插槽给各页放元信息。消费方 `Home.vue`（正在热映 / 即将上映）与 `Movie.vue`。**为什么立组件而不是共享 SCSS**：共享部分含 CSS 表达不了的行为（破图要 `@error` 改状态、点击进详情、骨架），且三处的海报高度已经漂移到 260px / 240px 两档。`Search.vue` 的横向卡与 `Rank.vue` 的榜单行是另外两种形状，刻意不并入，避免给组件堆开关。
  - **`front-pages.scss` 扩为前台唯一骨架层**：新增 `.section-head`（标题 + 品牌短线 + 「全部 ›」）、`.poster-grid`（`auto-fill minmax(180px,1fr)` 自适应列数，取代写死的 `el-row :span="6"`）、`.filter-chip`（筛选项改 `<button>`）、`.service-tag--*`（影院服务配色三处共用一份）、`.detail-skeleton`（两个详情页共用的首屏骨架）、`.empty-hint`。前台 4 处重复的 `.empty-hint` 收敛到这里。
  - **卡片分工落定**：`global.css` 的 `.card` 提供外观（底色 / 圆角 / 阴影），`front-pages.scss` 的 `.page-card` 只覆写消费端的内边距（`--space-24`）与堆叠间距。不删全局 `.card` —— 它被 9 个范围外的 manage/back 文件引用，且它本就是"外观层"而非重复实现。同名 `.page-card` 在 `admin-pages.scss` 里另有一份（管理端内边距更紧），这是规范 §6.2「密度分端是有意决策」的落地。
  - **键盘可达性**：`BuyTicket.vue` 的 64 个座位格由 `div @click` 改为 `<button>`（不可选座位用 `disabled`，`aria-label` 报出「3排5座，已售」这类编号与状态，`focus-visible` 显式描边，悬停放大只给 `:not(:disabled)`）；座位胶囊的 `×` 由 `span @click` 改为 `<button aria-label="移除 X排X座">`；`Home.vue` / `Rank.vue` 的榜单行改 `<router-link>`；`Movie.vue` 的筛选片改 `<button>`；搜索框与下拉补 `aria-label`（placeholder 不构成可访问名称）；纯图标按钮补 `aria-label`；`h1` / `h2` 语义化（购买页标题、账户板块标题、认证页标题、个人中心标题）。
  - **空态与失败态分离（§11.2）**：`Rank.vue` 此前失败时渲染「暂无票房数据」，与"库里确实没有"不可区分，且完全没有错误态 —— 两份榜单各加 `error` 标志并渲染「数据加载失败，请稍后重试」；`Search.vue` 同样补上（此前 catch 后落回"没有找到匹配的电影"）。`Cinema.vue` 的 `catch` 里重复弹的 `ElMessage` 删除（拦截器已弹过），非 200 业务码仍保留提示（拦截器只管网络 / 超时 / HTTP 状态，不管 `code`）。
  - **死代码**：删 `constants/index.js` 的 `AUTH_API.YEARS`（与 `API_PATHS.YEARS` 同值且零引用）；删 `Movie.vue` 的 `data.cinemaData` / `data.status` 与永远为 `null` 的 `cinemaId` 查询参数；`Cinema.vue` 补上影院卡到 `CinemaDetail` 的跳转（此前整页没有任何入口，影院详情只能从「选择影院」页进）。`front/Person.vue` / `front/Password.vue` 按规范 §9.3 补 `status-icon` 与回车提交（含输入框的表单在输入框上挂 `@keyup.enter`，`el-form` 上挂 `@submit.prevent` 兜原生提交，只挂一处），`front/Person.vue` 的「电话」补 `prop`。
- 验证: `npm run build` 通过（每完成一个工作流各跑一次，共 8 次）。产物核对：`aspect-ratio` 已按 2/3、16/7、5/4、10/7 四档进各页 CSS；`FilmPosterCard` 已自动注册进 `components.d.ts` 并产出独立 CSS 块；共享层的 `section-head__title` / `filter-chip--active` / `detail-skeleton__poster` / `service-tag--*` / `poster-grid` / `empty-hint` 均在 `index-*.css` 中；已删除的 `.empty-hint__title` / `.empty-hint__desc` / `.seat-item--clickable` / `.seat-item--locked` 在全部产物中 0 命中；`@keyframes rotating` 全局只有 1 处定义，而 `BuyTicket` 与 `CinemaDetail` 两个分块都以未改写名 `animation:rotating` 引用它（改动前 CinemaDetail 引用的是不存在的名字）。**未做浏览器渲染验证** —— UI 目视由用户自查，本轮不声称验过界面。
- 评审后追加修复（同一轮，`code-review` 三角度并行复审查出）:
  1. **搜索页不再刷新——本轮的回归**。把顶栏搜索由 `window.location.href` 改成 `router.push` 之后，"已在搜索页再搜一次"变成同路由换 `?title=`，`Search.vue` 组件被复用、只写在 `onMounted` 里的取数不再触发，页面一直显示上一次的关键词与结果。旧写法靠整页重载掩盖了这一点。修法：`Search.vue` 加 `watch(() => route.query.title, fetchSearchResults)`（规则 83）。
  2. **轮播点错片**。五张 slide 都是 `position: absolute; inset: 0; opacity: 0`，后置兄弟节点绘制在上层，点可见那张的「购票」命中的是最后一张透明层的链接 —— 跳到第 5 部而不是当前展示的那部；同时五张都还在 Tab 顺序里，配 `aria-hidden` 构成"可聚焦但不可见不可读"的焦点陷阱。修法：非激活页 `visibility: hidden`（规则 84），一处改动同时解决命中测试与焦点两件事。
  3. **登录回跳丢参数**。`router/index.js` 的路由守卫用 `to.path` 拼 `redirect`，把 query 丢了；购票页的场次由 `cinemaId/filmId/recordId/roomId` 决定，登录后落在一个无参数的 `/front/buyTicket` 上只能报"参数无效"。改为 `encodeURIComponent(to.fullPath)` —— 这正是 `utils/request.js:24` 与 `FilmMarks.vue:232` 已有的写法，守卫是三者里唯一的例外，改完三处口径一致。（此条非本轮引入，但本轮重写了该守卫的下游消费方，顺带补齐。）
  4. **暂停中翻页又被自动播放**。`goHero` 无条件调 `startHero()`，用户鼠标悬停在 hero 上（已 `pauseHero`）点圆点换一张后，计时器被重启，5 秒后画面在他眼皮底下自己跳走 —— 与"悬停即暂停"的约定相矛盾。修法：加 `heroPaused` 标志，`startHero` 在暂停时直接返回。
  5. **深底次级文字的令牌不一致**。同一语义槽位（深色 hero 上的说明文字）在首页用 `--dark-text-muted`、在两个详情页用 `--dark-text-secondary`。统一为 `--dark-text-secondary`；这也让此前零消费者的 `--dark-text-secondary` 真正落到了它被定义时的用途上。
  6. **首页「全部」带了一个没人消费的 `?type=`**。影片列表页只支持 类型/年代/区域 三个筛选，从不读 query；`/films/page` 是否支持按上映状态筛无法从前端确认（后端本轮不在范围内）。与其留一个"看起来筛过其实没筛"的参数，不如去掉它并在原处注释说明缺口 —— 需要真正按上映状态筛时，先确认后端有无该口径。
  7. **复核出的两处重复**（`code-review` 的 reuse/simplification 角度）：`typeText`（类型文案）与 `scoreText`（角标评分）各在 2 个文件里逐字重复，且后者承载着"只有 null 表示无评分、0 是真实评分"这条关键判定 —— 收进 `utils/format.js` 的 `formatFilmTypes` / `formatScoreBadge`；三份逐字相同的 `.detail-skeleton` 标记收进 `components/DetailSkeleton.vue`（样式本就在共享层，标记却各写一份）；`Front.vue` 里与 `NAV_ITEMS` 一一对应、改一处必须同步改另一处的 `NAV_SECTIONS` 折进 `NAV_ITEMS[].sections`，消除"加了导航项忘了加段就永远不高亮"的隐患。
  8. **未做但记录备查**：`.film-hero__backdrop` + `__scrim` + `__inner` 这一组深色头图样式在 `FilmDetail.vue` / `FilmCinema.vue` / `CinemaDetail.vue` 三个 scoped 块里各写一份（约 55 行 ×3），且已经出现漂移（`__inner` 的 `align-items: flex-start` 只有两个文件有）。它们是纯视觉原语、无 per-page 行为，理应提升到 `front-pages.scss`；本轮未做，因为这需要把 `CinemaDetail` 的 `.cinema-hero` 前缀一并改名，属跨 3 个文件的版式改动，而浏览器目视验证不在本轮自证范围内 —— 留作下一轮的条目。
- 侦察报告的更正（子代理结论不当作事实，逐条核对后推翻的）:
  - `Orders.vue` 的 `lang="ts"` **不是**漂移：该文件真的含 TS 语法（`interface MarkRow`）。按"统一为 js"改掉后构建当场失败（`Unexpected reserved word 'interface'`），已回退。据此，把 manage/back 那 6 个同类文件也一并断言为"JS 却写了 ts"是不成立的，该条整条作废。
  - `FilmDetail.vue` 的 `film.types` / `film.area` **不是**字段名错误：CLAUDE.md 说 `Film` 无 `types` 是对的，但该页在第 301-302 行把 `typeList` / `areaName` 本地归一成了 `types` / `area`，两者一致。`FilmCinema.vue` 同样处理。差点误改。
  - `front/FilmMarks.vue` 写评价弹窗里 `el-form-item label="影片"` **不需要** `prop`：那是一行只读展示（`<span>{{ film.title }}</span>`），没有输入、没有校验规则，`prop` 也无处生效。规范 §9.3 的"每个 `el-form-item` 有 `prop`"只对参与校验的字段成立，待办里把只读展示行计入违规数是口径错误。
  - `CinemaDetail.vue` 的 `.status-tag--upcoming/playing/ended` 三态**未**收进 `constants/index.js`：它们是"未开始 / 放映中 / 已结束"这类**派生**状态（由 `start` 现算，不落库），与 `constants` 里那些落库状态词表不同源；且三个配色（primary / success / info 的基色压各自 `light-9` 底）实测均在 4.5:1 以上，合规。单消费点的派生状态放本地是合理的，强行搬进 `constants` 只会造出一个只有一个读者的映射。
- 相关文件: `xm_film/vue/src/components/{FilmPosterCard.vue,DetailSkeleton.vue}`（均新增）、`xm_film/vue/src/utils/format.js`、`xm_film/vue/src/router/index.js`、`xm_film/vue/src/assets/css/{front-pages.scss,global.css,auth-layout.scss}`、`xm_film/vue/src/views/{Front.vue,Login.vue,Register.vue}`、`xm_film/vue/src/constants/index.js`、`xm_film/vue/src/views/front/{Home,Movie,Search,FilmDetail,CinemaDetail,BuyTicket,Rank,Cinema,FilmCinema,Orders,Account,Person,Password}.vue`
- 提交记录: `e09701dc`
- 状态: 已修复

### BUG-056: 第二轮视觉重构 · 管理后台（16 页）—— 两代写法收敛，与同形缺陷的最后一处清理

- 日期: 2026-10-06
- Bug 描述: 三端里前台 15 页（BUG-055）与影院后台 7 页（`9275859e`）都已整理过，`views/manage` 的 16 页是最后一块 —— 它同时背着版式债（两代写法并存）与语义债（与 BUG-055 同形、但从未被清理的缺陷）。本轮以「严格令牌内：不新增色相、字号档、字体族、阴影档」为约束，把它收敛为与前台对称的单一共享骨架，并逐文件核对，查出 5 处实际缺陷、5 类规范偏差，另有一处文档错位。逐条如下。
- 缺陷与根因:

  1. **改密后是幽灵登录态**（`manage/Password.vue:69-72`、`back/Password.vue:68-71` 同形）。成功分支走 `clearStoredUser()` + `location.href = '/login'`。`clearStoredUser` 只清 localStorage 副本，而登录态的唯一依据是 `useAuth` 的模块级 ref —— 内存仍持 token，`isLoggedIn` 继续为真、外壳继续渲染成已登录；那一句整页重载正是用来盖住这一点的。同时违反规则 76（登录态只经 `useAuth` 变更）与规则 77（站内跳转用 `router.push`）。BUG-055 修掉了前台的同一实例（`front/Password.vue`），两处后台实例都漏了。改为 `useAuth().logout()` + `router.push('/login')`。

  2. **改完资料顶栏不刷新**（`manage/Person.vue:80,92`，`back/Person.vue` 同形）。直接 `setStoredUser(...)` 而不经 `useAuth.setUser`，只更新存储副本、不动那个 ref，顶栏的用户名与头像要等整页刷新才变（规则 76）。改走 `setUser({ ...user.value, ...data.form })`。

  3. **一次失败弹两次提示**（`manage/Record.vue:76`）。`catch` 里再弹一次 `ElMessage.error('加载数据失败，请重试')`，而 `utils/request.js:86-92` 的拦截器已经弹过（§11.2 明令页面内 `catch` 只落错误态、不再重复弹提示）。根因是该页把 `useCrud` 只当状态容器用，手写了一套 `load` / `onSearch` / `onReset` / `onPageChange` / `onSizeChange` / `handleDel` / `handleDelBatch` —— 逐字等价于 `useCrud` 自己的实现（`apiPage(RECORDS)` 就是 `${apiBase}/page`），只有错误处理多了一句。整段删掉改回 `useCrud` 自带方法：消掉双重弹错、顺带获得此前拿不到的 `error` 态、减约 35 行。

  4. **内容页把页脚顶出视口**（`manage/Home.vue:302-305`）。`.home-container` 声明 `min-height: 100vh` + `padding: 20px`，而它挂在外壳 `.manage-content` 之内 —— 凭空多出外壳头部与页脚的高度（规则 81，BUG-055 在 `front/Person.vue` 修过同一形状）。`100vh` 与那层 padding 一并删除（外壳内容区已有 16px 内边距）。

  5. **表单字段没有数据落点**（`manage/Person.vue` 的「个人介绍」）。模板绑 `data.form.description` 并随整表单 PUT 给 `/api/v1/admins`，但 `sql/schema.sql` 的 `admin` 表**没有** `description` 列（`cinema` 表有，所以 `back/Person.vue` 的「影院介绍」是有效的）—— 管理端这页是从影院端复制改写的，字段跟着文案一起搬了过来。用户填了介绍、点保存、看到「更新成功」，内容是丢的。修法二选一：补 `admin.description` 列，或删掉该字段。**处置：删字段**（用户决断）—— 补列属后端 + schema 变更，而该属性 `admin` 从未有过、`data.sql` 也没有它的种子值，字段本身没有存在理由。删掉整块 `<el-form-item>` 后 `description` 与该文案在本文件内不再出现。

- 规范偏差与处置:

  1. **内容区底色不符 §6.2**（`admin-layout.scss:99`）。`.manage-content` 取的是 `--el-bg-color`（白），而 §6.2 规定管理端页面背景是 `--el-bg-color-page`（`#f2f3f5`）。后果是卡片与页面底同色，表格卡与图表卡失去"面"的层次、只靠阴影分隔 —— 这是本轮单点视觉收益最大的一处。改为 `--el-bg-color-page`。**外溢**：`.manage-container` 是 manage 与 back 共用外壳，back 的 7 页随之变灰；两者同属管理端，§6.2 对两端同等成立，故一并修正（已事先向用户声明并获准）。

  2. **行操作图标超档**（`admin-pages.scss` 的 `.row-action`）。原值 `font-size: var(--fs-lg)`（18px），不在 §五 的 12 / 16 / 20 / 24 四档内。已核实 EP 的 `.el-icon { font-size: inherit }`（`theme-chalk/src/icon.scss:31`）且内嵌 svg 是 `1em`，故 18px 确实落在图标上。改为 `--fs-md`（16px，§五「常规功能」档）；原注释「便于点击」的意图由 16px + EP link 按钮自带的内边距承担。**外溢**：back 的 4 个表格页同步变小。**同形未修**：`front-pages.scss:164` 的 `.front-content .row-action` 仍是 18px（消费方 `front/Orders.vue`），那是 BUG-055 已交付的范围，见下「未做但记录备查」。

  3. **表格操作列缺 `width`**（规则 61）。13 个表格页的操作列**全部**没写 `width` —— `el-table` 对未指定宽度的列按「`minWidth || 80`、再均分富余空间」起算，列多的表操作列只会分到约 80px；多按钮格必然折行，而 EP 的 `.el-button + .el-button { margin-left: 12px }` 会把折行后的第二个按钮右推、两行左右错开（BUG-049 的原形）。13 页逐页定宽（单图标 80 / 双图标 110 / Cinema 的「审核通过 + 双图标」160），并把 `Cinema.vue` 的三按钮格套上 `.row-actions`（此前只有前台有这个类，本轮给管理端补了一份）。

  4. **选择列宽三种取值**。50（Cinema）/ 55（多数）/ 70（Video）→ 统一 55。

  5. **T-1 §9.3 收口（manage 部分）**。11 页（9 个弹窗表单 + `Person` + `Password`）补 `status-icon`（错误反馈三件套的第三件）、输入框 `@keyup.enter` 与 `el-form` 的 `@submit.prevent`（只挂一处，避免一次回车发两次请求）；`manage/Person.vue` 的「电话」「邮箱」补 `prop`（「个人介绍」见缺陷 5 —— 那个字段已删除，故不在补 `prop` 之列）。**行为变更**：「邮箱」补 `prop="email"` 后首次激活 `rules.email` 的 `type: 'email'`（原先无 `prop` 故永不校验），且该页新增了提交前的 `validate()` 闸门 —— 存量邮箱格式非法的管理员改资料时会被挡住。已记入《前端规范待办》。

  6. **原始值工具类与重复声明**。`Area` / `Type` / `Notice` 三页带着 `.mb-2 { margin-bottom: 8px }` / `.mr-2 { margin-right: 8px }` / `.w-72 { width: 18rem }` / `.pt-5 { padding-top: 1.25rem }` / `.pr-12 { padding-right: 3rem }` 五个直写数值的工具类（§11.2 要求一切经类名 + 令牌表达；18rem = 288px 也不在 4px 网格上，同一个「搜索框」在另外 10 页是 300px）。另有 8 个文件的 scoped 块各自重声明一遍 `.card` 的底色 / 圆角 / 阴影（`global.css` 已提供），以及 `Cinema.vue` / `Film.vue` / `back/Film.vue` 三处各写一份逐字相同的 `.line` 截断基类。全部收敛进共享层。

- 本轮重构的落地口径:

  - **`admin-pages.scss` 扩为管理端唯一骨架层**，与 `front-pages.scss` 对称：新增 `.crud-page`（页面根堆叠，模块间距的唯一出处，取代每页各写的 `margin-bottom`）、`.page-head`（标题带 + 右侧主操作 + 品牌短线）、`.list-toolbar`（筛选区与操作区合并成一张卡）、`.selection-count`、`.table-card` / `.table-foot`（表格与其卡内分页条）、`.empty-hint`（空态 / 失败态占位）、`.row-actions`、`.line`、`.section-head`。`.page-head` 与 `.section-head` 的几何合成一条选择器（两者只差标题的字号与颜色）。

  - **13 个表格页从 4 张卡收敛为 2 张**：搜索卡与操作卡合并为 `.list-toolbar`，分页从独立卡折进 `.table-foot`。「新增」从 `type="info"`（灰）升为 `type="primary"` 并移进标题带（§9.2：页面主操作应是主按钮）；「批量删除」在未选中时 `disabled`，旁边显示「已选 N 项」（带 `aria-live="polite"`），随之删掉各页那句已不可达的「请选择数据」守卫。表格加 `size="small"`（§6.2 管理端密集档）。筛选控件与纯图标按钮补 `aria-label`（placeholder 不构成可访问名称，§9.3）。行操作按钮统一 16px（§五）。

  - **`el-table` 的 `#empty` 槽区分空态与失败态**（§11.2）。此前没有任何一页读 `crud.error`，所以「请求失败」与「库里确实没有」渲染成同一句 EP 默认文案。

  - **`Home.vue` 大盘重写**。两个区块标题统一走 `.section-head`（此前一处是光杆标题、一处是品牌色竖条，两种语言）；`el-row :span` 换成 `.chart-grid` / `.entry-grid` 两个 CSS grid —— `.chart-grid` 用 `repeat(2, minmax(0, 1fr))`，`minmax` 的 0 下限是承重件（裸 `1fr` 的 `min-width: auto` 会被 ECharts 画布撑破），顺带消掉「卡里套卡」；四个功能入口由 `<el-card @click>`（div + 点击处理器，不可聚焦、键盘不可达）改为 `<router-link>`（真 `<a href>`，天然进 Tab 序、回车可激活，与前台榜单行同手法）；hover 用边框与标题色而非叠阴影（全局 `.card` 已常驻 `--el-box-shadow-lighter`，再叠一层阴影得先把全部 `.card` 降档，会外溢到其他端）。ECharts 的 `cssVar()` / `getComputedStyle(documentElement)` / `ref` / resize / `v-if` 数据门控全部保留。

  - **`useCrud` 收拢删除确认**。13 页各写一份的「确认框 + `del`」与「确认框 + `delBatch`」（文案逐页有出入：「删除后无法恢复」半数缺、两处用的是半角逗号）收进 `confirmDel(id)` / `confirmDelBatch()`。同轮删掉零消费者的 `loadAll`（`front/Account.vue` 有自己的同名本地函数，全仓无人解构它）。

  - **`ROLE_TAG_MAP` / `getRoleType` 收进 `constants/index.js`**。`Admin` 与 `User` 两页各写一份的 `role === 'ADMIN' ? 'warning' : ...` 嵌套三元式收敛为查表，符合规则 22 与 §9.5「状态列必须走 constants 的映射」；缺省分支保持 `'success'`，与原三元式的兜底一致。

  - **不新建共享组件**。13 页的骨架标记仍各写一份。判据同 BUG-055 —— 立组件需要「CSS 表达不了的行为」加上「已经漂移的取值」两样都占，这里两样都不具备：行为已在 `useCrud` / `useFormDialog` 里，页面之间的差异（列、筛选项、弹窗字段）本身就是内容。包成一个槽位比内容还多的组件壳没有收益。

  - **`el-row` / `el-col` 至此零消费者**。BUG-055 把前台的 `:span="6"` 换成 `.poster-grid`，本轮把大盘最后两处换成 CSS grid；全仓已无 `<el-row>` / `<el-col>`，产物中也不再包含这两个组件。附注：`components.d.ts` 在多次构建之间反复增删 `ElRow` / `ElCol`（一次构建去掉了、之后又回到含它们的版本），最终与提交前一致，故该文件不进本提交 —— 可核对的事实是「零消费者 + 不打包」，不是那个生成文件的当下内容。

- 验证: `npm run build` 通过（每个工作包各跑一次）。产物核对：新骨架类全部进了 `index-*.css`；`.manage-content[data-v-*]` 为 `background-color:var(--el-bg-color-page)`、`.manage-container .row-action` 为 `font-size:var(--fs-md)`；`ADMIN:"warning",CINEMA:"danger",USER:"success"` 进了产物（`getRoleType` 的标识符被压缩，故按映射字面量核对）；已删的 `.mb-2` / `.mr-2` / `.w-72` / `.pt-5` / `.pr-12` / `.chart-empty` / `.title-tag` / `.video-wrapper` 在产物中 0 命中，`.section-title--spaced` 仅剩 `front/FilmDetail.vue` 自己那个带 `[data-v]` 的 scoped 定义。back 外溢审计：本轮新增的类名在 `back/*` 模板中 0 引用，外溢仅限上述两处已声明的取值修正。行尾：本轮改动的 27 个文件逐文件比对 `git diff --stat` 与 `git diff --ignore-cr-at-eol --stat`，完全一致，无幻影行尾变更。**未做浏览器渲染验证** —— UI 目视由用户自查，本轮不声称验过界面。

- 评审后追加修复（同一轮，`code-review` 复用 / 简化 / 正确性三角度 + `code-simplifier` 并行复审查出）:

  1. **`.line` 的共享化差点踩规则 79**。把 `.line` 加进共享层时，`back/Film.vue` 的 scoped 块里还留着一份同名定义 —— 两份特异性都是 0-2-0，胜负交给产物里的先后顺序。两份声明当前逐字等价，故无可见差异，但仍按规则 79 删掉 back 那份（宽度档 `.line--*` 本就在共享层）。
  2. **`.page-head` 与 `.section-head` 同文件内重复**。两者几何逐字相同（9 条声明，`::after` 完全一样），只有标题档不同。合成一条选择器，免得同一组声明在一个文件里写两遍。
  3. **大盘图表实例泄漏**。`onUnmounted` 只摘了 resize 监听、没有 `dispose()` —— ECharts 内部注册表仍持有画布 DOM 与 canvas。补上卸载时销毁两个实例。
  4. **`manage/Record.vue` 留了个没人用的 `crud` 绑定**（该页无弹窗表单，与 `Room` / `Mark` 同形却没照那样直接解构）。改为直接解构。
  5. **分页标记两种写法**。`Room` / `Mark` / `Video` 的 `el-pagination` 挤在一行，其余 10 页分 7 行（属性完全相同）。统一为多行式。

- 侦察报告的更正（子代理结论不当作事实，逐条核对后推翻或修正的）:

  - **子代理提出的「`.section-head__title` 在两套骨架层之间是漂移」不成立**。前台那份没有 `margin: 0`、管理端有，是因为前台用在 `<div>` / `<span>` 上（无默认外边距）、管理端用在 `<h2>` 上（有默认外边距）—— 差异由承载元素决定，不是漂移。
  - **子代理建议把两套骨架层里字节相同的规则（`.section-head` / `.empty-hint` / `.row-actions` / `.search-input` / `.field-full`）上提到 `global.css`** —— 本轮不采纳。上提会丢掉 `.manage-container` / `.front-content` 这层作用域，特异性从 0-2-0 降到 0-1-0；而后台蓝与前台红是靠 `html.theme-front` 换令牌值实现的，作用域一打开，任何页面都能盖掉骨架。本仓库刻意维持「一端的骨架层一个文件」（§11.3），重复是这套结构的已知代价，已记入《前端规范待办》备查。
  - **本轮的 EOL 检测脚本一度是空转的**。首版用 `grep -c $'\r$'` 计数，该模式在本环境下退化成 `$`（匹配每一行），于是「CR 行数 == 总行数」恒成立，把 LF 文件全报成 CRLF。改用 `tr -cd '\r' | wc -c` 逐字节计数才看到真相：本轮改动的 27 个文件里，22 个是纯 LF，5 个是「CRLF 为主、夹几行 LF」的混合行尾。据此把那 5 个文件按 HEAD 的逐行行尾还原，最终 `--ignore-cr-at-eol` 前后的 numstat 完全一致。

- 未做但记录备查:
  - **`back/Person.vue` 只做了同源的两行修复，未跟随 manage 侧一起清理**：`role === 'USER'` 分支在 `/back`（`meta.roles: ['CINEMA']`）下不可达，`defineEmits(['updateUser'])` 无消费方，`apiById` 仅为那个分支而导入，`data.user` 仍读 `getStoredUser()`。刻意不扩大本轮范围。
  - **规则 76 的清理在前台仍未完成**：`front/Person.vue:84` 直接 `setStoredUser(...)`（与 ①② 同形，顶栏不刷新）、`front/BuyTicket.vue:198,206` 直接 `clearStoredUser()`、`router/index.js:88` 直接 `clearStoredUser()`。BUG-055 只修了 `front/Password.vue` 一处。
  - **前台的行操作图标仍是 18px**（`front-pages.scss:164`，消费方 `front/Orders.vue`），与 §五 不符；本轮改了管理端的同名类，前台那半属于 BUG-055 已交付的范围，不静默改动。
  - **`confirmDel` / `confirmDelBatch` 只覆盖 manage**：`back/Ordered.vue` / `back/Record.vue` / `back/Room.vue` 仍各自内联确认框（其中两处是半角逗号），但它们不经过 `useCrud`（用裸 `request`），迁移是另一件事。
  - **`.manage-content` 的 `min-height: calc(100vh - 160px)` 与 `.manage-main` 的 `overflow: hidden` 是联动魔法数**，本轮不动（Home 删掉自己的 `100vh` 已足够）。
  - **`manage/Home.vue` 的图表每次加载初始化两次**（`initData()` 内显式初始化一次，`watch` 又初始化一次；因为每次都先 `dispose` 旧实例，所以只是冗余不是错误）。该 `watch` 服务于后续数据更新，未动。
  - **「重置」按钮沿用 `type="warning"`**：按 §3.7 功能色不得用于装饰，橙色「重置」站不住；但这是全站既有约定（前台 1 页 + back 4 页 + manage 13 页共 18 处，含 BUG-055 已审的前台页），不静默分叉，留待一次性统一。
- 相关文件: `xm_film/vue/src/assets/css/{admin-pages.scss,admin-layout.scss}`、`xm_film/vue/src/views/manage/`（16 页）、`xm_film/vue/src/views/back/{Film.vue,Person.vue,Password.vue}`、`xm_film/vue/src/composables/useCrud.js`、`xm_film/vue/src/constants/index.js`、`CLAUDE.md`、`Bug.md`、`前端规范待办.md`
- 提交记录: `7d60ca01`
- 状态: 已修复

### BUG-057: 首页「今日票房」的刷新反馈不可感知（转圈一闪而过、数字无声替换）

- 日期: 2026-10-06
- 问题描述: `/front/home` 右侧栏「今日票房」点「刷新」几乎看不到任何反馈：按钮的转圈几十毫秒就消失，数字直接换掉，用户无从判断是否点中、是否刷新成功。失败时更糟 —— 大数字被整块换成错误文案，一次瞬时故障就把已知的票房数抹掉了
- 根因分析: 三个独立的原因叠加。① `box-office/today`（BUG-047 引入的匿名聚合端点）在本机几十毫秒返回，`loading.today` 直接挂在请求生命周期上（`finally` 里清），转圈时长等于请求时长 —— 这是典型的 spinner flash：操作快过感知阈值时反而像没响应。② 数字与错误文案是 `v-if` / `v-else` 二选一渲染的，错误态一到就把数字换掉，没有"保留旧值"的概念。③ 数字与刷新按钮同处一行，而这一行只有 208px 内容宽（`aside 280` − 色条 40 − 内边距 32）；按钮 idle 态「刷新」与 loading 态「刷新中」宽度不同，EP 又把转圈图标渲染成标签 `<span>` 的兄弟节点（`.el-button [class*=el-icon]+span` 再加 6px 间距），刷新时按钮宽约 20px。临界长度的金额（如 `135.00元`，36px 粗体约 146px）在 idle 时刚好放下、刷新时被挤到折行 ——「元」掉到第二行、刷新完又弹回，就是肉眼看到的"元字上下跳"
- 解决方案:
  1. `loadTodayBoxOffice` 引入 `MIN_REFRESH_MS = 450`：请求结果先拿到，再在结算点统一提交 —— 请求耗时不足 450ms 就等满，够了就立即提交，loading 的结束与结果高亮压在同一刻
  2. 数字挂一次性高亮 class `today-box__amount--fresh`（~500ms 主色 → 默认文字色的颜色脉冲），`animationend` 自清；只在值真的变化时触发。关键帧只在本组件 scoped 块引用，非共享故不进 `global.css`（规则 78）
  3. 按钮 loading 时 `v-if` 掉自定义的 `Refresh` 图标并改文案「刷新中」—— EP 的 `loading` 自身会渲染一个转圈图标且默认插槽照常渲染，不 `v-if` 就是一个按钮两个图标
  4. 失败不再覆盖数字：`error` 只驱动时间戳那行的提示（有旧值 → 「刷新失败 · 数据更新于 HH:mm:ss」；无旧值 → 「数据加载失败，请稍后重试」），数字始终渲染 `total`（首屏未拿到时是 `—`）
  5. 时间戳由「北京时间：<完整日期时间>」改为「更新于 HH:mm:ss」（widget 名叫「今日」，日期冗余）；数字配 `aria-live="polite"`、刷新行配 `:aria-busy`
  6. 用户复查时发现「元」上下跳，定位到 ③：`.today-box__row` 拆成 `.today-box__amount`（独占整行）+ `.today-box__foot`（时间戳 `flex: 1` / 按钮 `flex-shrink: 0`），数字补 `white-space: nowrap`。数字不再与按钮争同一行的 208px，按钮宽度怎么变都挤不到它
- 验证: `npm run build` 通过；构建产物核对 `Home-*.css` 含 `@keyframes today-amount-flash` 与 `white-space:nowrap`、`Home-*.js` 含 `MIN_REFRESH_MS` / 「刷新中」 / 失败文案。未做浏览器渲染验证（UI 目视由用户自查）
- 相关文件: `xm_film/vue/src/views/front/Home.vue`
- 提交记录: `ce596c0f`
- 状态: 已修复

### BUG-058: 影院详情页排版与交互重构（场次表格 + 分页器嵌套 + 竖版影院图 + 重复弹提示）

- 日期: 2026-10-07
- 问题描述: `/front/cinemaDetail/:id` 多处失当，其中三处是功能性缺陷而不只是观感问题。① 头横幅底色是紫（`#41036a`），与前台红品牌（`--color-brand` `#ef4238`）冲突。② 影院图被裁成 5:6 竖条（220×264）——影院没有「海报」，一张门脸照裁成竖版是错的比例尺，也与影院列表页的 10:7 不一致。③ 「营业时间」这行恒显示「暂无营业时间信息」，永远拿不到值。④ 场次以表格呈现：「操作」列占 40% 宽却只放一个 `--fs-xs` 的小按钮；表格体锁 `max-height: 200px` 内滚动，而分页器（`.record-table__pagination`）又塞在这个滚动容器**里面**；每部影片各带一套分页器，10 部片就是 10 个。⑤ 不同日期的场次混在同一个分页列表里，日期只能靠单元格内 `<br>` 换行看出，用户无法「看明天的场次」。⑥ 某片场次请求失败时被置成 `list = []`，与「该片无排片」渲染成同一副样子
- 根因分析: 五条独立的原因。
  1. **紫色头图不是页面自己定的**，是 `--dark-bg-hero` 的令牌值，规范 §3.5 把它指定给「影片详情 / 影院详情页头横幅」。所以它是三处详情页共有的问题，只改这一个页面无效
  2. **「营业时间」绑的字段不存在**。`Cinema` 继承 `Account`，全字段是 `id / username / password / role / name / newPassword / token / avatar / email / address / leader / code / certificate / status / phone / description` —— 没有 `businessHours`。模板却渲染它，于是兜底文案成了唯一可能的结果。同一页的 `cinema.rating` / `hallCount` / `todaySchedule` 也是死状态（声明了但后端无此字段，且模板从未渲染）
  3. **表格是后台思维的产物**。「操作」列 40% 宽是照抄后台 CRUD 表格的列宽分配（那里一格放多个按钮），搬到前台后，一格只有一个「选座购票」，剩下全是空白。内滚动 + 分页器的嵌套则来自「每部影片独立分页」这个设计：为了让表格不无限长而锁高度，锁了高度又得分页，分页器只能塞进容器里
  4. **日期不是一等公民**。场次是按「影院 + 影片」查的（`records/page?cinemaId&filmId`），日期只是 `start` 字符串的前 10 位。没有日期维度，就没有日期选择，只能把所有日期堆在一起
  5. **`catch` 里重复弹提示**。`fetchCinemaInfo` / `loadFilmList` / `fetchRecordList` 的 `catch` 都调了 `ElMessage.error`，而 `utils/request.js` 的响应拦截器已经弹过一次 —— 同一次失败弹两遍，违反规则 72
- 解决方案:
  1. `--dark-bg-hero` 由 `#41036a` 改为 `#2A1214`（近黑红），三处详情页同步受益。改之前先用对比度公式核过候选值：`#2A1214` 上白 17.57 / `#cccccc` 10.94 / `#aaaaaa` 7.56 / `#8a8a8a` 5.09 / `#ef4238` 4.61，五项全达 AA。选它而非更红的 `#3D0F12`，是因为后者会把 `--color-brand` 压到 4.32（不达 AA），且 `#2A1214` 的数值与规范 §3.5 现值几乎重合，表格不必改数字（见 BUG-059）
  2. 影院图改 `240px` / `aspect-ratio: 10 / 7`，与影院列表页同比例
  3. 删掉营业时间行；`cinema` 状态收敛为 `id / name / avatar / address / phone`，用显式挑字段的 `Object.assign` 代替原来整包 `Object.assign(cinema, cinemaInfo)`（后者会把响应的 `role` / `token` / `newPassword` 一起灌进前端状态）
  4. 场次表格整体删除，改为「日期条 + 影片行 + 场次块」：日期条是今天起 7 天的固定窗口，sticky 吸顶；影片行左侧 96×134 海报，右侧场次块（`14:30` + 影厅名）；点场次块直接进选座。视觉选中态走三通道（颜色 + 字重 + 3px 下划线），另挂 `aria-pressed`，满足 §3.7「禁止仅靠颜色传递状态」
  5. **日期切换做到 0 请求**：加载时对每部影片发一次 `records/page?cinemaId&filmId&pageSize=200` 取回该片全部场次，前端按 `start` 的前 10 位归并成 `sessionsByFilm[filmId]`，切日期只是客户端过滤。若改成「按日期查」，每切一天就要对每部片各发一次请求（10 部片 = 10 次），切日期变成有延迟的操作。代价是 `SESSION_PAGE_SIZE = 200` 这个硬上限，写进代码注释
  6. **只渲染可购场次**（`status != '停售'` 且 `start` 晚于当前，与后端 `RecordService.isPurchasable` 同规则），于是不需要「已结束 / 放映中 / 停售」的置灰态，也就免掉了整套禁用样式。该日无场次的影片整行不渲染 —— 一屏十行「暂无排片」比不显示更吵
  7. **失败态与空态分开**（规则 72）：影片列表失败 → 「数据加载失败，请稍后重试」；某片场次失败 → 保留该行并写「场次加载失败」（不当作「无场次」静默隐藏，那会让用户以为系统里真没有）；`catch` 里不再弹提示，只落错误态
  8. 首屏 1 + N 次请求（N = 影片数，与改前相同），但**等齐再渲染**（逐行落位会让行数反复跳），并给骨架一个 `MIN_SKELETON_MS = 400` 的最短时长（本机三组请求百毫秒内就回，骨架会一闪而过，规则 86）
  9. `DetailSkeleton` 加 `wide` 开关：影院页头图是横版，骨架必须同形，否则数据到位时跳高。原骨架海报位是 2:3，与真实头图 5:6 本就不同形
  10. 深度链接 `?filmId=`（来自「选择影院」页）保留：自动选中该片最近一个有场次的日期，再滚动到该行。`.film-row` 补 `scroll-margin-top: var(--space-64)` 给吸顶日期条让出高度，否则片名被压在日期条底下
  11. 不再传 `pageNum` / `pageSize` 给 `films/by-cinema` —— 该接口返回完整列表，后端根本不读这两个参数
- 验证: `npm run build` 通过；产物核对 `index-*.css` 含 `2A1214` 且全仓已无 `41036a`、含共享层的 `detail-skeleton--wide`；`CinemaDetail-*.css` 含 `date-tab--active` / `showtime__room` / `film-row__meta` / `scroll-margin-top`；`CinemaDetail-*.js` 含 `aria-pressed` 与「今天」「明天」「今日可购」「场次加载失败」「所选日期暂无场次」文案。另核实 `--el-index-normal: 1` 确由 EP 产出（否则 sticky 的 `z-index` 会静默失效），以及 `Search.vue` 的同名 `.film-row__meta` 与本页哈希不同（`data-v-9d8a95ab` / `data-v-d6b24c36`，不触发规则 79）。未做浏览器渲染验证（UI 目视由用户自查）
- 相关文件: `xm_film/vue/src/views/front/CinemaDetail.vue`、`xm_film/vue/src/components/DetailSkeleton.vue`、`xm_film/vue/src/assets/css/{tokens.scss,front-pages.scss}`、`标准前端视觉与交互设计规范.md`、`CLAUDE.md`
- 提交记录: `55b93b6b`
- 状态: 已修复
- 未做但记录备查:
  - **影院详情接口会把 `token` / `newPassword` 一起序列化**。`Cinema extends Account`，而 `Account` 只给 `password` 加了 `@JsonProperty(access = WRITE_ONLY)`，`token` 与 `newPassword` 没有 —— `GET /api/v1/cinemas/{id}` 的响应因此多出两个与前台无关的键（当前值为 null）。规则 48 管的是「共享视图泄露他人数据」，这里是另一条路径（继承带出的自身字段），故未并入该条。稳妥修法是给 `BaseController` 的只读端点建投影 DTO，或至少给这两个字段补写保护；本轮按「接口不动」的约定只记录不动
  - **`FilmService.selectByCinema` 漏调 `fillFilmTypes`**。`selectAll` / `selectById` / `selectPage` / `selectByTitle` 都调了，唯独它没有，于是影院详情页的影片永远拿不到 `typeList`，影片行只能显示「时长 · 语言 · 格式」而不能显示类型。修法是补一行调用，但这会让响应多出一个字段（属于接口变更），同样按约定只记录不动。已立规则 87

### BUG-059: 规范 §3.5 深色表面的对比度担保只在页脚底色上成立

- 日期: 2026-10-07
- 问题描述: 规范 §3.5 的表格给 `--dark-text-secondary` 标 10.84:1、`--dark-text-faint` 标 5.04:1、`--color-brand` 标 4.56:1，读起来像是「深色表面这一族令牌都达 AA」。但 `--dark-bg-hero` 取的是 `#41036a`（紫），亮度远高于 `#1a1a1a`
- 根因分析: 那一列数值实际是拿 `--dark-bg`（`#1a1a1a`，相对亮度 0.0103）算出来的，却被当成整组深色令牌的担保。在 `#41036a` 上实测：`--dark-text-faint` 只有 4.21:1、`--color-brand` 只有 3.81:1，**两者都不达 AA**。当时没暴露，纯属运气 —— 三处详情页恰好只用了 `--dark-text`（14.52:1）与 `--dark-text-secondary`（9.04:1），没用到那两个穷色
- 解决方案: 把 `--dark-bg-hero` 收到与 `--dark-bg` 亮度相近的近黑红 `#2A1214`，两个深底共用同一份达标口径；规范 §3.5 补一段说明「深底对比度」一列同时适用于两个令牌，并把这次实测的六个比值列出来。另在规范里写明：改 `--dark-bg-hero` 时必须复测 `--dark-text-faint` 与 `--color-brand` —— 底色每变亮一档，最先跌破 AA 的就是它们（`#3D0F12` 已经开始掉 `--color-brand`）
- 验证: 候选值与前景色的对比度用亮度公式逐个算过（`#2A1214`：白 17.57 / `#cccccc` 10.94 / `#aaaaaa` 7.56 / `#8a8a8a` 5.09 / `#ef4238` 4.61）；产物核对 `index-*.css` 含 `2A1214`
- 相关文件: `xm_film/vue/src/assets/css/tokens.scss`、`标准前端视觉与交互设计规范.md`（§3.5）
- 提交记录: `55b93b6b`
- 状态: 已修复

### BUG-060: 管理后台首页没有任何数字，且两个图表都是弱图表

- 日期: 2026-10-07
- Bug 描述: `/manage/home` 整页只有「影院状态分布」饼图、「电影类型占比」柱图与 4 个路由入口卡，**页面上没有一个数字**。管理员打开首屏看不到"今天卖了多少票""有多少影院等着审核"
- 根因分析: 三条独立的原因。
  1. **大盘接口只服务了「分布」这一种问题**。`StatisticsController.overview` 从建立起就只返回 `cinemaStatus` 与 `filmType` 两组分组数据，没有任何计数与时间序列。首屏想做 KPI 行与趋势图，接口给不出数据
  2. **影院状态只有两个类别，饼图在这里本就选错了图表类型**。行业共识是饼图 / 环图不用于量的比较 —— 两个数字画成饼没有任何增量信息，而同一份信息用一条进度条 + 一行文字更清楚，还省掉 ECharts 的 PieChart 依赖
  3. **柱图把中文类目名旋转了 30°**。影片类型名是 2~4 个汉字，竖柱图放不下才要旋转；横向条不需要
- 解决方案:
  1. `overview` 扩出 `summary`（今日票房 / 今日订单 / 用户总数 / 影院总数 / 待审核影院 / 待取票订单）与 `revenueTrend`（近 7 个完整日）。**影院总数与待审核数由 `cinemaStatus` 派生**，不发第二次 COUNT —— 同一份响应里重复取数会在两次数之间留下窗口，让两个数字对不上
  2. `OrderedMapper.xml` 抽出 `<sql id="paidOrderStatuses">`，由今日票房、今日订单数、逐日趋势三处 `<include>`：让"状态集合同源"（规则 75）由构建保证，而不是靠注释提醒
  3. 趋势**不含今天**。今天只过了一半，画进折线会让曲线在每个上午都呈现"断崖下跌"，读者会把半天数据误读成经营恶化。副作用是趋势末点正好是昨天，KPI 环比可直接从它推出来，后端不必另算
  4. 无销售的日期由 `StatisticsService` 补 0：折线图缺一天会把它连成一条跨越两天的直线，读起来像那天也有销售额
  5. 饼图换成 `el-progress`（审核进度条 + 百分比 + 「共 N 家 · 已审核 X · 未审核 Y」），柱图转横向条并按数量升序排（ECharts 类目轴从下往上画）
  6. 待办条只放**有落地页**的两项（待审核影院 / 待取票订单），无待办时整条不渲染。「处理中充值单据」刻意不放：管理后台 16 个页面里没有充值单据页，一个点不动的数字比不显示更糟
  7. 待办卡的链接带筛选参数，故 `manage/Cinema.vue` 新增「审核状态」筛选项、两页都按规则 83 `watch` `route.query.status`（站内跳转同页换参不重挂组件，只写在顶层只有第一次生效）
  8. 失败态遵守规则 72 与 86：接口成功但空 → 「暂无数据」；请求失败 → 「数据加载失败，请稍后重试」；**刷新失败保留已展示的数据**，只在次要行提示。刷新沿用前台 `front/Home.vue` 的最短展示时长口径（450ms）
  9. 单一初始化路径：图表只由 `watch(数据)` 触发建图，页面挂载时不再单独调一次 —— 旧实现每次加载初始化两遍，正是《前端规范待办》T-11 记的那处冗余
- 规范修订: §3.7 补「指标状态色是状态通道，不是装饰」（按有利性着色 + 必须搭配箭头/文字）；§4.2 的 `--fs-3xl` 用途补上「管理端 KPI 主指标」
- 已知取舍: 刷新交互在前台 `front/Home.vue` 与本次的 `manage/Home.vue` 各有一份实现（最短时长、失败保留旧值、数字脉冲三段行为同形不同源）。本仓没有 `useRefresh` 之类的共享抽象，两端骨架层也刻意不合并（见《前端规范待办》T-7），故不静默处理，在此登记
- 验证: 后端 `StatisticsServiceTest` 4/4 通过（纯 Mockito，不起 Spring 上下文、不连库）；绑定层守卫 `StatisticsMapperLoadTest` 4/4 通过，且它**首次运行就抓到一处真实 SQL 缺陷** —— `selectPaidRevenueByDay` 原写 `GROUP BY DATE(pay_time)` 而 SELECT 是 `DATE_FORMAT(pay_time, …)`，违反 MySQL 8 默认的 `only_full_group_by`，接口一上线必崩，已改为两处表达式逐字一致。备用端口 9091 起实例 curl 核对契约：`totalCinemas`(4) == `cinemaStatus` 各项之和、匿名访问返回 401、`revenueTrend` 恰好 7 条且覆盖 2026-09-30…10-06（末点是昨天、不含今天）、无销售的日期补 0（09-30…10-05 全 0，10-06 为 684.00 元 / 8 笔）、`summary.todayRevenue`(116.00) 与公开端点 `GET /api/v1/films/box-office/today` 完全一致。前端 `npm run build` 通过；产物命中「较昨日」/ `kpi-card__delta--up` / `todo-card` / `tabular-nums` / `audit__percent`。另记一处**判据更正**：原计划用 `grep "pie"` 验证 PieChart 已摇掉，该判据无效（ECharts 的 lang 字典与事件分发里始终有 "pie" 字样，与注册了哪些图表无关），改用 pie 专有实现符号 `padAngle` / `avoidLabelOverlap` 判定，两者均不在产物中。未做浏览器渲染验证（UI 目视由用户自查）
- 相关文件: `xm_film/springboot/src/main/java/com/example/springboot/service/StatisticsService.java`、`xm_film/springboot/src/main/java/com/example/springboot/controller/StatisticsController.java`、`xm_film/springboot/src/main/resources/mapper/{OrderedMapper,UserMapper}.xml`、`xm_film/vue/src/views/manage/{Home,Cinema,Ordered}.vue`、`标准前端视觉与交互设计规范.md`、`CLAUDE.md`
- 提交记录: `d480215d`（后端口径）· `04984256`（首页重构）· `5cb634ae`（本条目）
- 状态: 已修复
