# 前端 Vue3 代码安全与质量审计（FE-01…FE-63）

> 2026-10-18 全盘复核：本文件原为仓库根的 `前端 Vue3 代码安全与质量审计报告.md`，已移入 `audit/` 并改名，`git mv` 保留了历史。复核只改动了确有出入的描述（见下），条目编号与结论未动。索引见根目录 `项目索引.md`。
>
> 复核更正：原第 3 行写「47 个视图文件」，实测 `views/` 下共 **46 个 `.vue`**（另有 3 个壳 `Front.vue` / `Back.vue` / `Manage.vue` 与 `Login` / `Register` / `404` 也在此目录，均已计入）。原写「约 12.8k 行」，实测 13,399 行。

本文件登记 2026-10-14 起前端代码安全与质量审查的结论。第一轮审查 `xm_film/vue/src/router/`（导航守卫与路由表，vue-router 4.5.1），并登记该轮牵连核实的跨文件前端问题（前端鉴权与跳转口径：`utils/request.js`、`composables/useAuth.js`、`views/Login.vue`、`views/404.vue`、`views/Front.vue`、`views/back/Back.vue`、`views/manage/Manage.vue`）。同日第二轮审查 `xm_film/vue/src/composables/` 三个复用件（`useAuth.js`、`useCrud.js`、`useFormDialog.js`）及其调用方（`views/manage/*`、`views/back/*`、`views/front/*`），结论见「已修复（composables 复用件）」一节。2026-10-15 第三轮审查 `xm_film/vue/src/views/` 全目录（**46 个视图文件，13,399 行**；原文误作 47 / 12.8k），结论见「已修复（views/ 目录）」及「本轮登记未做」新增条目；同日就 FE-48 做契约对齐（前端读错键名，非后端缺字段），见「已修复（契约对齐）」，多角色 1:N 需求转登 `audit/backend-backlog.md`。后端条目另见 `audit/backend-backlog.md`，本文件不重复。2026-10-15 另对重做后的前台影院列表页做了复查（并核到与 FE-34 同形的后端前缀匹配），结论见「已修复（影院列表页复查）」及「本轮登记未做」新增条目。

维护口径：一处事实一处归属；每条给出 `文件` 与符号，行号随改动可能漂移、以符号为准。状态分三档：已修复、本轮登记未做、已核验非缺陷。编号 FE-xx 与后端 BL-xx 区隔。

更新日期：2026-10-15。基线：master `cf83e680`（第一轮）、`7e0dcc1a`（第二轮 composables 修复）、`02527cf4`（第三轮 views/ 起点）、`1f813fa5`（契约对齐起点）、`f8b10705`（影院列表页复查起点）。

## 已修复（2026-10-14）

| 编号 | 位置 | 问题 | 处置 |
| --- | --- | --- | --- |
| FE-01 | router/index.js beforeEach 的 ElMessageBox 确认框 `.catch` | 匿名打开受保护路由（地址栏直入 / 刷新 / 书签）触发的是首次导航，from 为 START_LOCATION。点「取消」或关闭确认框走 `next(false)`，中止唯一一次导航。vue-router 4.5.1 仅在 `info.delta` 为真或 pop 导航时回滚 history（dist/vue-router.mjs 的 pushWithRedirect），首次导航 delta=0 不回滚，currentRoute 停在 START_LOCATION、matched 为空 —— App.vue 仅剩空 RouterView 与 ErrorBoundary，页面白屏且无可点元素，只能改地址。 | `.catch` 改为 `from === START_LOCATION ? next('/front/home') : next(false)`，非首次导航仍按「取消即留在当前页」。新增 `import { START_LOCATION } from 'vue-router'`。 |
| FE-02 | router/index.js beforeEach 的鉴权判定 | 原判定先 `if (to.meta.guest) return next()`、后 `if (to.meta.requiresAuth)`，且 `roles` 只在 requiresAuth 分支内读取。vue-router 把父路由 meta 合并进子路由，任何「只认其中一个标记」的写法在标记共存时 fail-open：guest 压过 requiresAuth（e3089365 修的正是这一类，守卫层未根除），或 roles-only 路由整块被跳过。 | 判定改为 `if (to.meta.requiresAuth \|\| to.meta.roles)`，不再有 guest 早退。`guest` 标记保留为文档；公开页既无 requiresAuth 也无 roles，行为不变。当前路由表下逐条等价（/manage、/back 父级 requiresAuth 继承不变；/front 各 guest 子路由落到末尾 next()）。 |

## 已修复（composables 复用件，2026-10-14 第二轮）

| 编号 | 位置 | 问题 | 处置 |
| --- | --- | --- | --- |
| FE-11 | composables/useCrud.js `load` | 无请求序号。搜索/翻页/改页大小并发触发多次 `load` 时，慢的旧响应后到会覆盖新结果；且任一请求的 `finally` 先到即把 `loading` 置 false，新请求仍在途却已收起转圈。本机后端 + 30000ms 超时下概率低但可达（连点页码）。 | `load` 前置递增 `loadSeq`，仅序号最新的响应写入 `dataList`/`total`/`error`，`loading` 也只由最新请求复位。 |
| FE-12 | composables/useCrud.js `load`（`del`/`delBatch` 均经此重载） | 删除后按原 `pageNum` 重新加载，不收敛页码。后端 PageMethod.startPage 未开 reasonable，删空末页后该页只返回空列表而 `total` 仍 >0，表格渲染「暂无数据」——前面明明还有数据。 | `load` 成功且 `list` 空、`total>0`、`pageNum>1` 时回退第 1 页并重载。 |
| FE-13 | composables/useAuth.js `verifyToken` | 令牌在 `/me` 解析之后才从 storage 读回（`getStoredUser()?.token`）。验签期间登出/换号/他标签页清空 storage，则解析时 `stored` 为 null，`setUser({ ...res.data, token: undefined })` 把「有角色、无令牌」的用户写回 storage —— isLoggedIn 为假、后续请求丢 Authorization、守卫再拦，需整页刷新才自愈。 | 发请求前捕获 `currentToken`；解析时若 `user.value.token !== currentToken` 即丢弃结果（登录态已变），否则用捕获的令牌回填。 |
| FE-14 | composables/useAuth.js `init` | `init()` 写在 `useAuth()` 工厂体内，每个调用 `useAuth()` 的组件挂载都触发一次 `GET /auth/me`（约 15 个组件；壳与子页同时调用者每次导航打两发）。`globalVerifying` 只降不升，`isLoggedIn` 的门只护得住首次请求。 | 加模块级 `verified` 一次性闸门：`init` 仍每次从 storage 同步单例，但 `/me` 全生命周期只验一次。此改动同时解除了 FE-04 的前置阻塞（见下）。 |
| FE-15 | composables/useFormDialog.js `submit` | 无 in-flight 守卫，保存按钮无 loading/disabled。双击（或输入框 `@keyup.enter`）触发两次写入：无 id 的表插两条，显式写 id 的五张表（admin/area/type/notice/video）第二次撞主键、成功之后反报错。 | `submit` 进入即置 `submitting` 守卫、`finally` 复位。守卫必须在 `validate()` **之前**置位——置于校验之后时两次调用都能先过校验，拦不住。`submitting` 一并返回供按钮绑 loading。 |
| FE-16 | composables/useFormDialog.js `defaultForm` 合入 | `reactive({ ...defaultForm })` 与 `Object.assign(form, defaultForm)` 都是浅拷贝，默认值里的数组/对象（如 Film 的 `typeIds: []`）按下标共享，一旦就地改动会渗进后续每次新增。当前各处均为重新赋值，故未外显；拷贝方式本身是隐患。 | 新增递归 `cloneDefaults`，初始与 `openAdd` 均深拷贝默认值（不用 `structuredClone`/`JSON`：前者有浏览器支持面，后者丢 `undefined` 键，而 `time: undefined` 这类默认值依赖它）。 |
| FE-17 | composables/useCrud.js；useAuth.js `logout`/`forceLogout`；useFormDialog.js `rules` 别名 | 同类一致性清理：useCrud 手拼 `${apiBase}/page`、`/${id}`、`/batch` 而未复用 constants 既有的 apiPage/apiById/apiBatch（其他视图已在用，两套路径口径并存）；useAuth 的 `logout` 与 `forceLogout` 函数体完全相同、仅名称分叉；useFormDialog 的 `const rules = formRules` 是空别名。 | useCrud 改用 apiPage/apiById/apiBatch；`forceLogout` 并入 `logout`；去掉 `rules` 别名（对外返回键名仍为 `rules`）。 |

## 已修复（views/ 目录，2026-10-15 第三轮）

| 编号 | 位置 | 问题 | 处置 |
| --- | --- | --- | --- |
| FE-23 | views/back/Person.vue `data.form = data.user`；GET/PUT | 编辑态表单直接别名存储用户对象（`getStoredUser()` 内含 `token`），PUT body 携带裸会话令牌，泄漏进请求体与服务端日志；两处请求均无 `.catch`。 | 非 USER 分支剥离 `token` 后再作表单；GET/PUT 各补 `.catch`。 |
| FE-24 | views/front/BuyTicket.vue `initSeats` / `selectSeat` | `initSeats()` 重建座位图却不清 `selectedSeats`：支付/取消/超时后 chips、计数、总价、`canSubmit` 全部残留，可对刚释放或已售座位重复下单。另无单笔座位上限，后端 `MAX_SEATS_PER_ORDER=6` 只在提交时 400 拒绝。 | `initSeats` 置空 `selectedSeats`；`selectSeat` 达上限告警拒绝。 |
| FE-25 | views/front/Orders.vue `search`/`reset`/`load` | 查询/重置调用 `load()` 不复位 `pageNumber`：在第 3 页发起筛选、过滤后该页为空，明明匹配的订单显示「暂无数据」。`load` 亦无请求序号。 | 新增 `search()` 复位页码并改绑查询按钮；`reset` 复位页码；`load` 加 `loadSeq`。 |
| FE-26 | views/front/Cinema.vue `el-pagination` / `load` | 分页只绑 `:total`，未绑 `:page-size`/`:current-page`：`pageSize=12` 而 pager 按默认 10 计，21 家影院渲染 3 页、点第 3 页 offset 越界拉到空列表。错误态不清旧结果。 | 补 `v-model:current-page`/`:page-size`；错误分支清 `filmData`/`total`；`load` 加请求序号。 |
| FE-27 | views/front/PayPassword.vue `switchMode` / `verifyIdentity` | 验证请求在途时第一步的「忘记支付密码？」仍可点：切模式清空字段后响应回来无条件 `step=2`，未经登录密码验证即进入「设置新密码」。 | `switchMode` 在途早退；两处链接加 `:disabled="submitting"`。 |
| FE-28 | views/manage/Notice.vue `defaultForm.time` | 默认发布时间用 `new Date().toISOString()`（UTC），el-date-picker 按本地渲染，东八区差 8 小时；且默认值在 setup 求值一次后冻结。 | 改本地时钟 `nowLocal()`。 |
| FE-29 | views/back/Home.vue `load` | 公告 `apiPage` 未传分页参数，固定后端默认 `pageSize=10` 且页面无分页控件，表头「共 N 条」与实际行数矛盾，第 11 条起不可达。 | 传 `pageNum`/`pageSize` 并补 `el-pagination`。 |
| FE-30 | views/front/CinemaDirectory.vue `load` | 错误态保留上一个筛选的旧行、丢弃后端 `res.msg`、无 loading、无请求序号。 | 补请求序号、`v-loading`、`res.msg` 提示、错误清行。（2026-10-15 该页整体删除 —— 前台不再暴露影院名录，本条随之失效） |
| FE-31 | views/front/Movie.vue `el-pagination` / `load` / 三个下拉 | 分页未绑 `v-model`（`applyFilter` 复位页码不生效，pager 与内容错位）、`layout` 缺 `sizes` 致 `:page-sizes`/`@size-change` 为死配置；错误态不清旧列表；`loadType/Area/Year` 无 `.catch`。 | 补 `v-model` 与 `sizes`；错误清 `filmData`/`total`；三个下拉补 `.catch`。 |
| FE-32 | views/front/FilmDetail.vue `img` / `types` | 缺海报兜底写死为字面中文串「默认海报地址（可选）」并当 `<img src>` 用，触发不存在路径的请求、头图裂图；`types` 本地 `map+join` 未复用共享 `formatFilmTypes`。 | 兜底改 `''`；改存 `typeList` 并复用 `formatFilmTypes`。 |
| FE-33 | views/front/FilmCinema.vue `loading` / `types` | `loading` 初值 `false`，电影信息到位前渲染 `v-else` 空态（「未知电影」「暂无该电影的上映影院信息」）再切骨架，每次进入闪一屏假空态；`types` 未复用 `formatFilmTypes`。 | 初值改 `true`；改存 `typeList` 并复用 `formatFilmTypes`。 |
| FE-34 | views/Front.vue `isNavActive` | `route.path.startsWith(prefix)` 无路径边界：`/front/cinema` 吞掉同前缀的 `/front/cinemaDirectory`，访问影院名录时「影院」与「影院名录」两项同时点亮。 | 匹配改为「相等或 `prefix + '/'`」。（2026-10-15 前台名录页与导航项删除，所举这对碰撞源消失；边界匹配本身保留） |
| FE-35 | views/Login.vue `login` catch | 传输类失败（网络/5xx）时 `e.message` 是 axios 英文串，且响应拦截器已弹过中文提示，此处再弹即二次提示 + 未翻译文案。 | 命中 `e.isAxiosError` 时不再弹框（业务失败仍弹后端中文 msg）。 |
| FE-36 | views/front/Search.vue `fetchSearchResults` | 空关键字早退发生在清空 `filmList`/`error` 之前：同路由只换 query（顶栏搜索）时残留上一次搜索结果。 | 早退前清空 `filmList`/`error`/`loading`。 |
| FE-37 | views/front/FilmMarks.vue `filmId` | `route.params.id` 一次性 `const` 捕获且只在 `onMounted` 取数：同路由记录换 `:id` 时组件复用、`onMounted` 不重触发，页面停留在上一部片的数据。 | 改 `let` + `watch(() => route.params.id)` 复位页码并重取。 |
| FE-38 | views/front/CinemaDetail.vue `applyFilmDeepLink` | 深链影片的场次请求失败时 `sessionsByFilm` 为空，报「该影片近 7 天暂无场次」，把加载错误说成事实（同行却不显示「场次加载失败」）。 | 命中 `sessionError[target]` 时改提示「场次加载失败，请稍后重试」。 |
| FE-39 | views/manage/Cinema.vue `searchDirectory` | 远程搜索无请求序号，连打关键字时慢的旧响应覆盖新候选；`res.data.list` 无守卫，`data` 为 null 时抛错被空 `.catch` 吞掉。 | 加 `dirSeq` 序号；改 `res.data?.list`。 |
| FE-40 | views/manage/Home.vue 票房趋势卡 | 仅判 `!overview.revenueTrend.length`，无 `loading.overview` 判别，200-but-empty 时永久停在骨架屏（同页 filmType 卡已有该门）。 | 补 `loading.overview` 门与「暂无数据」分支。 |
| FE-41 | views/manage/Film.vue `getTypeTagType` / el-tag / 三个下拉 | `String('').charCodeAt(0)` 返回 `NaN`，而 `?? 0` 只兜 null/undefined → `tagTypes[NaN]` 为 undefined，`type` 失效；两处 `v-for` 的 `<el-tag>` 无 `:key`；三个下拉无 `.catch`。 | 改 `Number.isFinite(raw) ? raw : 0`；补 `:key`；补 `.catch`。 |
| FE-42 | views/manage/Video.vue `rules` | `start` 的校验文案是「请选择上映日期」，字段标签却是「发布时间」（复制粘贴残留）；「视频名称」`name` 无规则，可存出空名行。 | 文案改「请选择发布时间」；补 `name` 必填规则。 |
| FE-43 | views/back/Film.vue `el-rate` | `v-model="props.row.score"` 对 props 派生的行对象双向绑定（仅因 `disabled` 而不外显），一旦去掉 disabled 即向 props 写入。 | 改 `:model-value`。 |
| FE-44 | views/manage/Ordered.vue；views/back/Ordered.vue | 影厅名兜底整套（`roomData`/`loadRoom`/`getRoomName`）为死代码：后端 `OrderedMapper.xml` 用 `IFNULL(room.name,'暂未关联影厅')` 保证 `roomName` 恒非空；模板 `\|\|` 右支不可达，且每次进页多发一次 `/rooms` 请求，`roomData` 还是非响应式数组。 | 删除兜底与 `loadRoom`，模板直接用 `roomName`，去掉冗余请求与死导入。 |
| FE-45 | 多处 `.then` 链缺 `.catch`（类） | `Register.vue`、`manage/Password.vue`、`manage/Person.vue`、`back/Password.vue`、`front/Person.vue`、`manage/Film.vue`（下拉）、`front/Movie.vue`（下拉）、`back/Person.vue` 的 `.then` 无 `.catch`，传输失败逃逸为未处理 rejection。 | 逐处补 `.catch`（提示仍由响应拦截器统一给出）。 |
| FE-46 | 独立表单无 in-flight 守卫（类） | `Register.vue`、`manage/Password.vue`、`back/Password.vue`、`front/Password.vue`、`front/Person.vue` 的提交按钮无 loading/disabled，双击或回车并发提交；改密类第二次会因原密码已变反报「原密码错误」。 | 各加 `submitting` 守卫并在 `finally` 复位，按钮绑 `:loading`。 |
| FE-47 | el-upload 编辑态无回填（类） | `manage/Admin.vue`（头像）、`User.vue`（头像）、`Actor.vue`（电影图片/演员照片）、`Video.vue`（电影图片/预告视频）的 `<el-upload>` 未绑 `file-list`，弹窗 `destroy-on-close` 每次从空列表起步，编辑时看不到已保存文件。 | 各加 computed `file-list` 绑定。 |

## 已修复（2026-10-15 契约对齐）

| 编号 | 位置 | 问题 | 处置 |
| --- | --- | --- | --- |
| FE-48 | views/front/FilmDetail.vue `fetchCastBySingleId` | 读 `actorData.roleType`/`role` 两键，后端响应里均不存在：`roleType` 恒落兜底 2、`role` 恒为空。原记「后端缺字段、越出前端范围」经复核不成立——后端 `ActorView.java` 的 `figure`/`grade`、`ActorController.toView` 的回填、`ActorMapper.xml` 的 `SELECT *` 三处俱在，数据在库、接口在返，只是前端读错了键名（`figure`=饰演角色名、`grade`=所属角色，取值域见 `manage/Actor.vue:102-105`）。 | 前端改读真字段：`roleType` 由 `constants.getActorRoleType(grade)` 派生（`ACTOR_ROLE_TYPE_MAP`，缺省 2=主演），`role` 改读 `actorData.figure`。后端零改动，无需加列落库。导演/编剧/二级演员三组仍空——库中 `grade` 65 行全为「主演」、无该类数据，属正确空态而非死 UI；多角色 1:N 需求另登记 `audit/backend-backlog.md` BL-41。 |

## 已修复（2026-10-15 影院列表页复查）

前台影院列表页重做（e88490ec）与前台下线影院名录（f8b10705）之后，复查该页及相邻改动。

| 编号 | 位置 | 问题 | 处置 |
| --- | --- | --- | --- |
| FE-53 | views/front/Cinema.vue 两行筛选项 | 筛选由两个 el-select 改为 .filter-chip 按钮后，控制件自身的可访问名丢失：.filter-label（「城市 :」/「区域 :」）是并列的 `<div>`，与按钮之间没有 `for`/`aria-labelledby` 关联，读屏只念「北京 按钮」，两行的「全部」按钮无法区分。旧版 el-select 是带 `aria-label` 的。 | 两个 `.filter-options` 加 `role="group"` 与 `aria-label`（按城市筛选 / 按区域筛选），与 BuyTicket.vue、CinemaDetail.vue 的既有写法同款；不加 aria-pressed，选中态已由底色 + 加粗双通道表达。 |
| FE-54 | views/front/Cinema.vue `el-pagination` | 分页器不在 loading 的 v-if 链内、也无 disabled，而 `load()` 不清 `total`：筛选重载期间旧分页器仍可点，点中的页码与刚切换的筛选叠加，答一次越界空请求并闪出「没有符合筛选条件的影院」。EP 的 pageCount 监听会把页码夹回并重发，故后果是一次多余请求加一次误报空态，不是持久损坏。 | 分页器加 `:disabled="data.loading"`。 |
| FE-55 | views/front/Cinema.vue 影院卡片 `img` | 卡片图缺 `loading="lazy"`。前身 Cinema.vue 同样缺，重做时被原样带过来：一屏之外的十余张仍立即加载，且每次翻页/换筛选重来一遍。同仓列表组件 FilmPosterCard.vue 已设该属性，是既定口径。 | 补 `loading="lazy"`。 |
| FE-56 | views/front/Cinema.vue `filters.cities` | 该字段恒等于 `Object.keys(filters.districtsByCity)` —— 同一份 `res.data` 派生、又各自赋值，是一份永远不该不一致、却必须手工同步的重复状态。 | 删除 `filters.cities`，改 `computed cityOptions = Object.keys(filters.districtsByCity)`。后端 `regions()` 用 LinkedHashMap 且保证 city 非空，派生值与展示顺序一致。 |

## 已修复（2026-10-16 账户页视觉重做）

前台账户页重做，排版取控制台口径：四个同款 el-card 平铺改为一行页头（左侧页面标题、右侧品牌红余额大字，下压一条与内容同宽的发丝线收口）+ 四段 `.card page-card` 分区（充值 / 充值单据 / 余额走势 / 资金流水）；卡片由阴影改 1px 淡边框，表头改灰底弱化小字，去斑马纹改行 hover。新增余额走势折线图，由资金流水逐行自带的 `balanceBefore` / `balanceAfter` / `createTime` 重建（手写 SVG，未引入 echarts —— 产物核对：账户 chunk 8.43 kB，其中 echarts/zrender 命中 0 次），故图上只标注「最近 N 笔」的窗口，不做需要全量求和的累计值。另有三处顺带修正：回调按钮用状态色表达动作；两表空态与失败态同文案；两表表头的文字色与字重（见 FE-63）。原余额卡上的「刷新」按钮随重做删除，页头只留标题与余额。

对齐统一到一条竖线：卡片内容自「1px 边框 + `--space-24` 内边距」处起，该值即 `--page-inset`（25px），同时用作页头文字与发丝线的内缩 —— 于是页头标题、三条 `.section-head` 通栏线、走势图两端全部落在同一条线上（发丝线因此不能用 `border-bottom`：border 连着左右内边距一起铺，会比分区线各宽 25px，改为伪元素）。表格不参与这条竖线 —— 两张表全列居中，居中内容没有左缘。

对齐口径：两表**每一列**一律 `align="center"` + `header-align="center"`，同一列的表头与内容必然同侧（EP 是把对齐打成单元格上的 `is-center` / `is-right` 类，`align` 管内容、`header-align` 管表头，两者不设其一就会各走各的）。两条随之而来的约束：其一，单元格那 12px 水平内边距（`.el-table .cell` 的 `padding: 0 12px`）必须左右对称地留着 —— 早先为让表首列与分区标题共竖线而只消掉左侧内边距，那在居中列上会让内容偏离列心 6px；其二，操作列里的按钮组是 flex 容器，列的 `align="center"` 只管 `.cell` 内的行内内容，管不到 flex 主轴分布，须另补 `justify-content: center`，否则该列表头居中、按钮却贴左。

该表列序也理过一遍，并删掉一列：原序「变动金额 / 变动前余额 / 变动后余额」把差额排在它所关联的两个余额之前，一行读出来是 300 → 128 → 428，从左到右验不了算；现为「发生时间 / 业务来源 / 关联单据ID / 变动金额 / 变动后余额」，标识在前、数值在后。**变动前余额整列不再展示** —— 它与「往下数一行」的变动后余额恒为同值（`FundSource` 要求每笔余额变动都归入一个来源，故流水连续完整），两列是同一串数字错开一行、属重复展示，银行流水亦只留「发生额 + 余额」。数据层字段保留，走势图起点仍在用它，删的只是展示列。

两条同位 EP 陷阱，对全仓任何动表格的地方都成立：其一，末列**不能**用 `:last-child` 定位 —— EP 在每行行末恒定渲染一个 gutter 单元格（补纵向滚动条宽度，`table/src/layout-observer.mjs` 只把它的宽度置 0、并不移除），命中的是它而非末列，要挂钩只能走列自身的 `class-name` / `label-class-name`；其二，表头与表体是两个**独立的 `<table>`**（`.el-table__header` / `.el-table__body`，均 `border-collapse: separate`），两边行高各算各的 —— 不显式钉成同一值，就会出现表头偏上、单元格偏下这类「表头与内容对不齐」的观感。现统一 `--table-row-height: 44px` + `vertical-align: middle`，顺带把两表行高也统一了（充值单据表因 `el-tag` 原本比资金流水表高 1px）。

列宽还有一条，同样出自 EP 且同样不显然：`updateColumnsWidth` 把「没有显式 `width` 的列」**一律**当 flex 列，富余宽度按各列 `min-width` 的比例分摊（`parseWidth` 会把 `width="110"` 这类字符串属性 `parseInt` 成数字，故定宽仍定宽）；而当 flex 列**只剩一个**时，它走 `realWidth = (minWidth || 80) + totalFlexWidth` —— 独吞全部富余。资金流水表原先六列里只有「发生时间」没给 `width`，于是它涨到 550px，其余五列被挤到表格右侧、左边空出一大片。现两表一律用 `width` 定死金额 / 状态 / 操作列，用 `min-width` 表达配比（含首列），并保证各表 `min-width` 之和不超过最窄情形的内容宽（视口 1120px 时约 902px）——超了 EP 会置 `scrollX` 并让每个 flex 列退回其 `minWidth`，表内出现横向滚动条。

| 编号 | 位置 | 问题 | 处置 |
| --- | --- | --- | --- |
| FE-52 | views/front/Account.vue 充值单据/资金流水两列表 | 两处均硬编码 `pageNum:1`/`pageSize:50` 且无分页控件，累计超 50 条后更早的记录在页面上无任何入口（对账场景是资金凭证不可见）。 | 两表各加 `reactive({ pageNum, pageSize: 10, total })` 与 `el-pagination`（`background`，layout 含 `total` 即「共 N 条」，`:disabled` 绑各自 loading —— 与 FE-54 同口径）。后端两处 `/page` 本就按 id DESC 返回且已按当前用户归属过滤（RechargeService.java 的 `selectScoped`），故提交充值 / 触发回调后一并回到第 1 页，新记录必然落在视野内。 |
| FE-61 | views/front/Account.vue 回调列 `el-button link type="success"` / `type="danger"` | 「模拟成功 / 模拟失败」是动作，却用 success / danger 承载 —— 这对色在本页已是状态语义（`el-tag` 用它表达单据状态），同一行里动作与状态同色；且使演示控件的视觉权重与业务列持平。 | 改自绘 `.callback-action`（常规文字色 6.11:1 → hover 主色 5.58:1，两态达 AA）并压到 `--fs-sm`，降为行内次要操作；保留图标以维持可扫读性。仅「处理中」行可见这一约束不变。 |
| FE-62 | views/front/Account.vue 两表空态 | 两表均无 `#empty`，复用 EP 默认「暂无数据」：请求失败与「确实没有充值记录 / 资金流水」同文案，把一次失败说成「没有资金凭证」。Cinema.vue 已确立「失败与空态文案必须区分」的口径，本页未跟上。 | 两表各补 `#empty`、各加一个 error 标志：失败显示「数据加载失败，请稍后重试」，确实为空显示「暂无充值记录 / 暂无资金流水」。失败时清空列表与 `total`，避免失败文案下挂着旧页数据；业务码非 200 补一次 `ElMessage.error`（不经过响应拦截器），网络类失败仍只落错误态以免弹两次。 |
| FE-63 | 全站 el-table 表头（EP 默认；本页 views/front/Account.vue 已就地修） | EP 表头读两个变量，默认值两头都不合适：`--el-table-header-text-color` 取 `--el-text-color-secondary`（实测 #909399，白底 3.08:1，不达 AA——表头是 14px 常规字，需 4.5:1），`--el-table-header-bg-color` 取 `--el-fill-color-blank`（实测 #ffffff，故表头与表体之间只有一条 1px 线，无底色分界）；且 `.el-table thead th` 写死 `font-weight: 600`，与 tokens.scss「禁用 600」相悖（中文字体栈 Microsoft YaHei 只有 400 / 700，600 会触发伪粗体，渲染比 700 更脏）。项目此前对表头零覆盖，故这是全站每一张表格的现状。 | **2026-10-19 已修复并移出**：覆写提到 `assets/css/global.css` 一处生效全站，`Account.vue` 的局部覆写已删除（只留本页特有的表格间距）。关键坑：EP 自己的 `.el-table { --el-table-header-* }` 同为单类选择器，且 EP 样式**后加载** —— 只写 `.el-table` 会被原样覆盖回去（实测规则确在样式表里、计算值仍是 `#ffffff`）。改用 `.el-table.el-table` 提高特异性后生效。实测计算值：背景 `#f5f7fa`、文字 `#606266`（约 6.1:1，达 AA）、字重 `700`。已逐页截图核验 `/manage/film`、`/manage/ordered`、`/front/orders`、`/front/account`。 |

## 本轮登记未做

| 编号 | 位置 | 问题 | 处置建议 |
| --- | --- | --- | --- |
| FE-03 | router/index.js beforeEach 的角色不符分支；views/Login.vue `login` | 角色不符时 `ElMessage.error` 后 `next('/login')`：丢弃来路（无 redirect），将已登录用户留在登录页。Login.vue 无「已登录即转主页」逻辑，故不致死循环，但来路不可恢复，重登只能按角色主页落地。 | 与 FE-07 同轮：抽出共享 `getDefaultPath(role)`，该分支改为回本角色主页，或带 redirect 的登录页。属行为变更。 |
| FE-04 | router/index.js beforeEach 的 `clearStoredUser()`；composables/useAuth.js `globalUser` | 守卫直读 / 直清 localStorage，绕过 useAuth 的模块级 globalUser。存储缺 token/role 时守卫清存储，但 globalUser 仍持旧值；同一 SPA 会话内头部 isLoggedIn 仍为真、显示已登录态，而守卫再判要求登录，UI 与守卫状态不一致，直到整页刷新。 | 守卫改走 useAuth 的会话口径；utils/request.js 的 401 处理同样只 `clearStoredUser()`、不动单例，需一并改。前置已解除：FE-14 已把 `init()` 的 `/me` 收敛为全生命周期一次，守卫改走 useAuth 不再每次判定多打一发。 |
| FE-05 | router/index.js 路由表 | meta 两种放置范式并存且默认方向相反：/manage、/back 把 requiresAuth+roles 标在父路由（子路由只带 name），/front 父无 meta、逐子标 requiresAuth/roles。新增 /front 子路由漏写 meta → 合并为 `{}` → 落末尾 next() 静默公开（前台含资金类页面）；新增 /manage 子路由则静默变 ADMIN-only。无类型或测试可拦。 | 统一为一套范式（建议门禁标记落父路由，子路由只声明例外），或加路由表单测断言：受保护前缀下的每个子路由必须带门禁标记。 |
| FE-06 | router/index.js beforeEach；views/front/FilmMarks.vue `promptLogin`；utils/request.js `redirectToLogin` | 登录引导确认框与 `?redirect=${encodeURIComponent(...)}` 拼接在三处重复（FilmMarks 注释自称「与路由守卫同一套」）。改文案、参数名或编码方式需同步三处，漏一处即静默漂移。 | 抽共享 `promptLogin(fullPath)` 与 `loginUrl(fromPath)`（置于 composables/useAuth.js 或 utils/），三处共用。 |
| FE-07 | views/Login.vue `getDefaultPath`；views/404.vue `goHome`；views/Front.vue `goAdmin`；views/back/Back.vue 退出跳转 | role→主页映射以 if 链复现于四处。新增角色或改主页路径需改四处；守卫的角色不符分支因此无法复用，只能裸跳 /login。 | 上提 `getDefaultPath(role)` 到 utils/ 或 composables/，四处共用；与 FE-03 同轮。 |
| FE-08 | router/index.js beforeEach 的 ElMessageBox | ElMessageBox 为单例。受保护页上双击链接或后续程序化导航会重入 beforeEach 再弹确认框，先前导航的 Promise 与闭包里的 next 悬空，用户应答后可能对已过期的导航调用 next，落到旧目标或触发多余中止。需并发导航才可达。 | 守卫内加模块级「确认框在途」标志，在途时复用同一次判定，避免重复弹框与陈旧 next。 |
| FE-09 | router/index.js 路由表 meta.name | meta.name 仅被 Manage.vue、Back.vue 的面包屑读取；/front 全树与 login/register/404 的 20 余处 name 渲染不到任何地方，属纯维护负担，且看起来仍权威。 | 删除 /front 树与独立路由上无消费者的 meta.name，或补上消费者。 |
| FE-10 | router/index.js 路由表 meta.title 与 beforeEach 首行 `document.title` | 站名 `'电影购票网站'` 作为标题在 index.html、routes 的两处 title、守卫兜底 `to.meta.title \|\| '电影购票网站'` 共四处重复；且 document.title 在每次导航无条件写入，多数路由无 meta.title、写入的都是同一默认值。 | 站名收敛为一处常量；写入前判等（`if (t !== document.title)`）避免无谓的 document 写入。 |
| FE-18 | composables/useAuth.js `verifyToken` 的 catch | 仅 401/403 触发登出；网络中断、500、超时被吞，用户保持「已登录」而令牌可能已失效，直到下一次真实请求 401 才清。刻意 fail-open（避免网络抖动误登出）与非刻意漏网在此实现里难分。 | 若判定为缺陷：对「非网络类失败」（如 5xx）复用 `logout`，仅在 `ERR_NETWORK`/`ECONNABORTED` 时保持登录。属行为变更，先定口径再动。 |
| FE-19 | utils/request.js 响应拦截器；全仓 `res.code === '200'` | 拦截器已解包 `response.data`，却把业务码判断留给每个调用方，`'200'` 字面量分散约 101 处 / 37 文件、无单一出处；协议变更（码型、新增业务码）需全量手改，无编译期兜底。 | 在拦截器内归一（`code !== '200'` 直接 reject 并给统一提示），或至少收敛 `SUCCESS_CODE` 常量。跨文件重构，属独立一轮。 |
| FE-20 | composables/useCrud.js `selectedIds` | add/update/del/delBatch 经 `load()` 换掉 `dataList`，却不重置 `selectedIds`；重置完全依赖 el-table 在 `:data` 替换时发 `selection-change([])`。当前无 `row-key`/`reserve-selection`，通常自愈；一旦加 row-key 或 EP 行为变更，删除后工具条仍显示「已选 N 项」且批量删除可用，会重发已删 id。 | 写操作成功后由 composable 显式清 `selectedIds`，不再依赖组件契约。 |
| FE-21 | composables/useFormDialog.js `openEdit` | openEdit 只 `Object.assign(form, row)`：既不清理 row 缺失的 defaultForm 独有键（如 Admin 的 `password`），也不隔离 row 的对象字段（form 与列表行按下标共享）。当前后端 update 显式置空 password，故不外显；契约「form 恰好等于当前记录」不成立。 | 若需修：openEdit 也按默认值重置缺键，并深拷贝 row 的对象字段（row 可能是 el-table 的响应式代理，需先 `toRaw` 再拷）。 |
| FE-22 | composables/useAuth.js `isLoggedIn` 的 `!globalVerifying` 门 | isAdmin/isCinema/isUser 直取 `user`、不设门，`isLoggedIn` 却被 `!globalVerifying` 挡住。冷加载已登录页时 `globalVerifying` 初始为真，首次渲染 isLoggedIn 为假 —— 顶栏先渲染游客「登录/注册」再切已登录菜单（一次 `/me` 往返的闪烁，慢网/后端停顿时可见）。 | 若要消除：与路由守卫口径一致地信任 storage（isLoggedIn 只看 token），或让 role 标记同样受门（更差）。属行为变更，先定口径。 |
| FE-49 | views/front/Cinema.vue；views/front/Orders.vue；views/front/Movie.vue；views/manage/CinemaDirectory.vue | 分页列表各自手搓（`pageNum`/`pageSize`/`total`/`load`/`handlePage`），未复用 `useCrud`，因而不带它已有的请求序号与越界回退。第三轮已就地补齐由此派生的具体缺陷（FE-25/FE-26/FE-30/FE-31），但两套口径并存本身仍在。 | 收敛到 `useCrud`（同端点 `manage/Cinema.vue` 已在用）；跨文件重构，属独立一轮。另：`front/Cinema.vue` 的 `:page-sizes`/`@size-change` 因 layout 无 `sizes` 仍为死配置。 |
| FE-50 | views/Front.vue；views/back/Back.vue；views/manage/Manage.vue | 退出登录确认块（`ElMessageBox.confirm` → `logout()` → `push('/login')` → 成功/取消提示）在三处逐字重复，改文案或跳转目标需同步三处。 | 抽共享 `confirmLogout`（置于 `composables/useAuth.js` 或 `utils/`），三处共用。与 FE-06/FE-07 同族。 |
| FE-51 | views/manage/Person.vue `form: { ...user.value }` | setup 时对 `user.value` 一次性浅拷贝快照，`verifyToken` 异步回填的 `/me` 权威资料不落进表单；存储与后端不一致时表单显示陈旧值，直接保存会用旧值覆盖。 | 需 dirty-check 设计（异步回填不得覆盖用户已改字段），属行为变更，先定口径。 |
| FE-57 | views/front/Cinema.vue；views/front/CinemaDetail.vue；views/front/FilmCinema.vue | 影院服务三项（退票无忧 / 儿童优惠 / WiFi覆盖）的标签与图标在三处硬编码，只有底色 `.service-tag--*` 是共享的：改一个服务名要动三个模板，而底色共享、文案不共享，同一底色可以挂在两个不同名字下。 | 上提 `SERVICES` 数据常量到 constants/index.js（key 映射既有底色类），三处按各自形状 v-for。三处渲染形状不同（纯片 / 片+说明 / 无图标），共享单元应是常量而非组件。**本轮未做（登记备查）**：CinemaDetail.vue、FilmCinema.vue 不在本次 diff 内，属跨文件重构。 |
| FE-58 | views/front/Cinema.vue 页头内层；views/front/CinemaDetail.vue、FilmDetail.vue、FilmCinema.vue、FilmMarks.vue 的 hero 与内容层 | 「1200px 居中内容测度」（`width: 55%`/`60%` + `max-width: 1200px`）手写在约 10 处，`.page-narrow`/`.page-wide` 只是其中两份：改一次内容宽度要动 10 个文件，新增页头一旦漂移就与其下方列表错位。 | 上提为共享 measure（front-pages.scss 的变量/mixin，或一个类加「无纵向外边距」变体），各页引用。**本轮未做（登记备查）**：系统性缺口，跨 5 个文件，另起一轮。2026-10-16 追加：Account.vue 重做时其内容区已改用 `.page-wide`（消掉一份手写），但同页页头内层仍按该口径手写一份（与 Cinema.vue 页头同形），故手写份数并未净减 —— 页头这一层正是该项最难收敛的部分。 |
| FE-59 | views/Front.vue `NAV_ITEMS`；router/index.js 路由表 | 顶部导航由数组手工维护、与路由表并列，两者之间没有任何机制关联：删路由忘删导航项则会落 `/:pathMatch` 兜底 404，加路由忘加导航项则页面无入口。本次下线影院名录就需同时改两处，正是该漂移的实例。 | 导航改由路由 meta（如 `meta.nav`）派生，或加断言：/front 下需出现在导航的路由必须各有对应项。属结构变更。 |
| FE-60 | App.vue `path.startsWith('/front')`；utils/request.js `pathname.startsWith('/login')` | 与 FE-34 同形的裸前缀匹配残余两处：只差字符前缀相邻的路径也会命中（`/frontx` 会被套上前台主题、`/loginx` 会走登录页的跳过分支）。当前路由表下均无可达碰撞，属同形未做点。 | 与 FE-34 同口径改为「相等或 `prefix + '/'`」。两条均非当前可达缺陷。 |

## 2026-10-18 全盘复核新增

本轮按「读代码 + 实测」复核了下列此前未记录的点。前三条是**已修复**，第四条登记备查。

| 编号 | 位置 | 问题 | 处置 |
| --- | --- | --- | --- |
| FE-64 | views/Back.vue 与 views/Manage.vue | 两个后台外壳 **91% 行完全相同**（`Back.vue` 150 行非空行中有 137 行也出现在 `Manage.vue` 196 行里）：logout 块、header、用户下拉、面包屑、`el-menu` 结构逐字重复，差异只在菜单数据。与 FE-50 同族但范围更大——改一处 header 行为要动两个文件。 | 合并为一个外壳 + 菜单配置（两块 `el-menu` 的差异可收成 `menuGroups` 数据）。属结构变更，另起一轮。**未做（登记备查）**。 |
| FE-65 | views/front/Cinema.vue、Orders.vue、Movie.vue；views/back/* | FE-49 已登记"两套分页口径并存"，但未量化：这 4 个页面的 `pageNum/pageSize/total/load/handlePage` 各自手写，**请求序号守卫只在 `useCrud` 与 FE-25/26 修过的那几处**，其余仍可能旧响应覆盖新结果。 | 收敛到 `useCrud`，或至少抽出带 `loadSeq` 的 `usePagedList`。与 FE-49 同一轮处理。**未做（登记备查）**，本条只是把基数补全。 |
| FE-66 | 全站 `res.code === '200'` | FE-19 估的是"约 101 处 / 37 文件"。实测（2026-10-18，正则 `code\s*===?\s*['"]200['"]`，`.vue` + `.js`）为 **98 处 / 35 文件**：`views/` 87、`composables/` 7（其中 `useCrud.js` 5）、`components/` 4。最多的单文件各 6 处（`back/Record.vue`、`front/Account.vue`、`manage/Film.vue`）。 | 数量下调不影响结论（仍无单一出处）。改法同 FE-19：拦截器内归一，或至少收敛 `SUCCESS_CODE` 常量。**未做（登记备查）**。 |
| FE-63 复核 | 全站 el-table 表头 | 2026-10-18 复核确认「全站未做」成立；**2026-10-19 已修复**，见上表。 | 已关闭。 |

## 已核验为非缺陷（防二次误报）

- views/404.vue 的 `goHome`：`getStoredUser()?.role || 'USER'` 有兜底，未知 / 空角色落 USER 并跳 /front/home，非「无分支则静默不动」。
- router/index.js beforeEach 的角色不符分支不会死循环：Login.vue 无「已登录即转主页」逻辑，且 redirect query 已在 `next('/login')` 时丢弃，重申登录按本角色主页落地。
- views/Front.vue 的 `goAdmin`、views/404.vue、Manage.vue 与 Back.vue 的退出跳转：目标路由均存在且角色匹配，未发现失效调用点。
- router/index.js 的 `/:pathMatch(.*)*` 兜底与 `/` 重定向：均落在 guest 路由，行为正确。
- composables/useCrud.js 的 add/update/del/delBatch 失败后不清空 `dataList`：写失败时列表既有数据仍然有效，只有 `load`（读）需要清空兜底，非缺陷。
- composables/useFormDialog.js `openAdd` 删除 defaultForm 之外键的清理逻辑：是 load-bearing 而非冗余——`openEdit` 把整行拷进 form，五张显式写 id 的表会在「编辑→新增」时撞主键，注释已述。
- views/front/Account.vue 充值单据表的「备注」列不是冗余（尽管资金流水表的「变动前余额」列是，已删）：后者与被删列之间是**可证明的恒等关系**（账本连续完整 → 某行变动前余额 == 下一行变动后余额），属同一串数据错开一行；而备注是独立自由文本字段，失败单据里存的是失败原因（本页演示控件写入「模拟支付回调失败」）、其余由后台填写，与「状态」不是同一份数据的两种写法。勿照搬删列。
