# 前端 Vue3 代码安全与质量审计报告

本文件登记 2026-10-14 前端代码安全与质量审查的结论。审查范围为本轮已审的 `xm_film/vue/src/router/`（导航守卫与路由表，vue-router 4.5.1），并登记该轮牵连核实的跨文件前端问题（前端鉴权与跳转口径：`utils/request.js`、`composables/useAuth.js`、`views/Login.vue`、`views/404.vue`、`views/Front.vue`、`views/back/Back.vue`、`views/manage/Manage.vue`）。后端条目另见 audit-backlog.md，本文件不重复。

维护口径：一处事实一处归属；每条给出 `文件` 与符号，行号随改动可能漂移、以符号为准。状态分三档：已修复、本轮登记未做、已核验非缺陷。编号 FE-xx 与后端 BL-xx 区隔。

更新日期：2026-10-14。基线：master `cf83e680` 及本轮工作区改动。

## 已修复（2026-10-14）

| 编号 | 位置 | 问题 | 处置 |
| --- | --- | --- | --- |
| FE-01 | router/index.js beforeEach 的 ElMessageBox 确认框 `.catch` | 匿名打开受保护路由（地址栏直入 / 刷新 / 书签）触发的是首次导航，from 为 START_LOCATION。点「取消」或关闭确认框走 `next(false)`，中止唯一一次导航。vue-router 4.5.1 仅在 `info.delta` 为真或 pop 导航时回滚 history（dist/vue-router.mjs 的 pushWithRedirect），首次导航 delta=0 不回滚，currentRoute 停在 START_LOCATION、matched 为空 —— App.vue 仅剩空 RouterView 与 ErrorBoundary，页面白屏且无可点元素，只能改地址。 | `.catch` 改为 `from === START_LOCATION ? next('/front/home') : next(false)`，非首次导航仍按「取消即留在当前页」。新增 `import { START_LOCATION } from 'vue-router'`。 |
| FE-02 | router/index.js beforeEach 的鉴权判定 | 原判定先 `if (to.meta.guest) return next()`、后 `if (to.meta.requiresAuth)`，且 `roles` 只在 requiresAuth 分支内读取。vue-router 把父路由 meta 合并进子路由，任何「只认其中一个标记」的写法在标记共存时 fail-open：guest 压过 requiresAuth（e3089365 修的正是这一类，守卫层未根除），或 roles-only 路由整块被跳过。 | 判定改为 `if (to.meta.requiresAuth \|\| to.meta.roles)`，不再有 guest 早退。`guest` 标记保留为文档；公开页既无 requiresAuth 也无 roles，行为不变。当前路由表下逐条等价（/manage、/back 父级 requiresAuth 继承不变；/front 各 guest 子路由落到末尾 next()）。 |

## 本轮登记未做

| 编号 | 位置 | 问题 | 处置建议 |
| --- | --- | --- | --- |
| FE-03 | router/index.js beforeEach 的角色不符分支；views/Login.vue `login` | 角色不符时 `ElMessage.error` 后 `next('/login')`：丢弃来路（无 redirect），将已登录用户留在登录页。Login.vue 无「已登录即转主页」逻辑，故不致死循环，但来路不可恢复，重登只能按角色主页落地。 | 与 FE-07 同轮：抽出共享 `getDefaultPath(role)`，该分支改为回本角色主页，或带 redirect 的登录页。属行为变更。 |
| FE-04 | router/index.js beforeEach 的 `clearStoredUser()`；composables/useAuth.js `globalUser` | 守卫直读 / 直清 localStorage，绕过 useAuth 的模块级 globalUser。存储缺 token/role 时守卫清存储，但 globalUser 仍持旧值；同一 SPA 会话内头部 isLoggedIn 仍为真、显示已登录态，而守卫再判要求登录，UI 与守卫状态不一致，直到整页刷新。 | 守卫改走 useAuth 的会话口径。注意 useAuth() 函数体内含 `init()`→`verifyToken()`（每次调用触发一次 /me），需先重构 useAuth 再改，否则守卫每次判定多打一次 /me。 |
| FE-05 | router/index.js 路由表 | meta 两种放置范式并存且默认方向相反：/manage、/back 把 requiresAuth+roles 标在父路由（子路由只带 name），/front 父无 meta、逐子标 requiresAuth/roles。新增 /front 子路由漏写 meta → 合并为 `{}` → 落末尾 next() 静默公开（前台含资金类页面）；新增 /manage 子路由则静默变 ADMIN-only。无类型或测试可拦。 | 统一为一套范式（建议门禁标记落父路由，子路由只声明例外），或加路由表单测断言：受保护前缀下的每个子路由必须带门禁标记。 |
| FE-06 | router/index.js beforeEach；views/front/FilmMarks.vue `promptLogin`；utils/request.js `redirectToLogin` | 登录引导确认框与 `?redirect=${encodeURIComponent(...)}` 拼接在三处重复（FilmMarks 注释自称「与路由守卫同一套」）。改文案、参数名或编码方式需同步三处，漏一处即静默漂移。 | 抽共享 `promptLogin(fullPath)` 与 `loginUrl(fromPath)`（置于 composables/useAuth.js 或 utils/），三处共用。 |
| FE-07 | views/Login.vue `getDefaultPath`；views/404.vue `goHome`；views/Front.vue `goAdmin`；views/back/Back.vue 退出跳转 | role→主页映射以 if 链复现于四处。新增角色或改主页路径需改四处；守卫的角色不符分支因此无法复用，只能裸跳 /login。 | 上提 `getDefaultPath(role)` 到 utils/ 或 composables/，四处共用；与 FE-03 同轮。 |
| FE-08 | router/index.js beforeEach 的 ElMessageBox | ElMessageBox 为单例。受保护页上双击链接或后续程序化导航会重入 beforeEach 再弹确认框，先前导航的 Promise 与闭包里的 next 悬空，用户应答后可能对已过期的导航调用 next，落到旧目标或触发多余中止。需并发导航才可达。 | 守卫内加模块级「确认框在途」标志，在途时复用同一次判定，避免重复弹框与陈旧 next。 |
| FE-09 | router/index.js 路由表 meta.name | meta.name 仅被 Manage.vue、Back.vue 的面包屑读取；/front 全树与 login/register/404 的 20 余处 name 渲染不到任何地方，属纯维护负担，且看起来仍权威。 | 删除 /front 树与独立路由上无消费者的 meta.name，或补上消费者。 |
| FE-10 | router/index.js 路由表 meta.title 与 beforeEach 首行 `document.title` | 站名 `'电影购票网站'` 作为标题在 index.html、routes 的两处 title、守卫兜底 `to.meta.title \|\| '电影购票网站'` 共四处重复；且 document.title 在每次导航无条件写入，多数路由无 meta.title、写入的都是同一默认值。 | 站名收敛为一处常量；写入前判等（`if (t !== document.title)`）避免无谓的 document 写入。 |

## 已核验为非缺陷（防二次误报）

- views/404.vue 的 `goHome`：`getStoredUser()?.role || 'USER'` 有兜底，未知 / 空角色落 USER 并跳 /front/home，非「无分支则静默不动」。
- router/index.js beforeEach 的角色不符分支不会死循环：Login.vue 无「已登录即转主页」逻辑，且 redirect query 已在 `next('/login')` 时丢弃，重申登录按本角色主页落地。
- views/Front.vue 的 `goAdmin`、views/404.vue、Manage.vue 与 Back.vue 的退出跳转：目标路由均存在且角色匹配，未发现失效调用点。
- router/index.js 的 `/:pathMatch(.*)*` 兜底与 `/` 重定向：均落在 guest 路由，行为正确。
