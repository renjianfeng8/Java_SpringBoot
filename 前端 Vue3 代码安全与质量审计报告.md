# 前端 Vue3 代码安全与质量审计报告

本文件登记 2026-10-14 前端代码安全与质量审查的结论。第一轮审查 `xm_film/vue/src/router/`（导航守卫与路由表，vue-router 4.5.1），并登记该轮牵连核实的跨文件前端问题（前端鉴权与跳转口径：`utils/request.js`、`composables/useAuth.js`、`views/Login.vue`、`views/404.vue`、`views/Front.vue`、`views/back/Back.vue`、`views/manage/Manage.vue`）。同日第二轮审查 `xm_film/vue/src/composables/` 三个复用件（`useAuth.js`、`useCrud.js`、`useFormDialog.js`）及其调用方（`views/manage/*`、`views/back/*`、`views/front/*`），结论见「已修复（composables 复用件）」一节。后端条目另见 audit-backlog.md，本文件不重复。

维护口径：一处事实一处归属；每条给出 `文件` 与符号，行号随改动可能漂移、以符号为准。状态分三档：已修复、本轮登记未做、已核验非缺陷。编号 FE-xx 与后端 BL-xx 区隔。

更新日期：2026-10-14。基线：master `cf83e680`（第一轮）、`7e0dcc1a`（第二轮 composables 修复）。

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

## 已核验为非缺陷（防二次误报）

- views/404.vue 的 `goHome`：`getStoredUser()?.role || 'USER'` 有兜底，未知 / 空角色落 USER 并跳 /front/home，非「无分支则静默不动」。
- router/index.js beforeEach 的角色不符分支不会死循环：Login.vue 无「已登录即转主页」逻辑，且 redirect query 已在 `next('/login')` 时丢弃，重申登录按本角色主页落地。
- views/Front.vue 的 `goAdmin`、views/404.vue、Manage.vue 与 Back.vue 的退出跳转：目标路由均存在且角色匹配，未发现失效调用点。
- router/index.js 的 `/:pathMatch(.*)*` 兜底与 `/` 重定向：均落在 guest 路由，行为正确。
- composables/useCrud.js 的 add/update/del/delBatch 失败后不清空 `dataList`：写失败时列表既有数据仍然有效，只有 `load`（读）需要清空兜底，非缺陷。
- composables/useFormDialog.js `openAdd` 删除 defaultForm 之外键的清理逻辑：是 load-bearing 而非冗余——`openEdit` 把整行拷进 form，五张显式写 id 的表会在「编辑→新增」时撞主键，注释已述。
