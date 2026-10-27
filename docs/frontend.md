# 前端

## 1 目录与规模

```
src/                       80 文件 / 17,387 行（vue+js+scss）
├── views/                 48 个 .vue / 13,559 行
│   ├── 根目录 7 篇：Front / Back / Manage / AdminShell / Login / Register / 404
│   ├── front/  16 篇 / 7,585 行
│   ├── back/    7 篇 / 1,337 行
│   └── manage/ 18 篇 / 3,648 行
├── components/  6 个 / 1,199 行
├── composables/ 6 个（useAuth / useCrud / useFormDialog / usePagedList / useLoginPrompt / useLogout）
├── utils/       6 个（含 navigation）
└── assets/css/  tokens.scss + global.css + index.scss + 4 个布局 scss
```

三个最大的单文件：`manage/Home.vue` 974、`front/BuyTicket.vue` 945、`front/Home.vue` 910 行。

## 2 路由表（`router/index.js`，单文件 142 行）

| 前缀 | 壳 | meta |
| --- | --- | --- |
| `/manage/*` | `Manage.vue`（11 行，内部复用 `AdminShell.vue`） | 父级 `requiresAuth + roles:['ADMIN']`，子级只带 `name` |
| `/back/*` | `Back.vue`（11 行，内部复用 `AdminShell.vue`） | 父级 `requiresAuth + roles:['CINEMA']` |
| `/front/*` | `Front.vue` | 父级无 meta，**逐子标记** `guest` 或 `requiresAuth+roles:['USER']` |
| `/login` `/register` `/404` | 无壳 | `guest: true` |

守卫逻辑（唯一一个 `beforeEach`）：

1. `document.title = to.meta.title || '电影购票网站'`。
2. `if (to.meta.requiresAuth || to.meta.roles)` —— **roles 本身蕴含需登录**，不再有 guest 早退（FE-02）。
3. 无 token/role → 清存储 → 确认框 →「去登录」带 `redirect`；「取消」时若 `from === START_LOCATION` 落首页（否则白屏，FE-01），否则 `next(false)`。
4. 角色不匹配 → 提示 + 回**该角色自己的主页**（不再踢到 `/login`；FE-03 已修）。

两种 meta 范式并存是已知债务（FE-05）：`/front` 下新增子路由漏写 meta 会**静默公开**。

## 3 composables（六个复用件）

| 文件 | 导出 | 关键约定 |
| --- | --- | --- |
| `useAuth.js` | `user` / `isLoggedIn` / `isAdmin` / `isCinema` / `isUser` / `hasRole` / `login` / `logout` / `init` | 模块级单例 `globalUser`；`verified` 闸门保证 `/me` **全生命周期只验一次**（FE-14）；验签期间比对 token 防旧响应复活会话（FE-13） |
| `useCrud.js` | `dataList/loading/error/pageNum/pageSize/total/searchForm/selectedIds/tableRef` + `load/add/update/del/delBatch/onSearch/onReset/onPageChange/onSizeChange/onSelectionChange` | `load` 带**请求序号**（FE-11）与**删空末页回退第 1 页**（FE-12）；写入/查询/重置后同时清表格内部选中与 `selectedIds`（B2） |
| `useFormDialog.js` | `dialogVisible/isEdit/submitting/form/formRef/openAdd/openEdit/submit/close` | 默认值**深拷贝**（FE-16）；`submit` 在校验前取 in-flight 守卫（FE-15）；`openAdd` 删除 defaultForm 之外的键（防带上一行 id 撞主键） |
| `usePagedList.js` | `items/state` + `load/reload/setPage/setPageSize` | 只读分页：末次请求落地、失败清空、越界回退第 1 页；`front/Orders`、`front/Cinema` 已采用 |
| `useLoginPrompt.js` | 游客登录引导 | 与路由守卫共用同一确认框文案 |
| `useLogout.js` | 退出登录 | 前台/后台外壳共用，带在途与取消处理 |

**采用率**：`front/Orders`、`front/Cinema` 用 `usePagedList`；`front/Movie` 与 `back/*` 仍手写分页，但都已带 `loadSeq`（FE-11）。

## 4 请求与存储

`utils/request.js`：

- `axios.create({ baseURL: API_BASE_URL, timeout: 30000 })`，`API_BASE_URL` 来自 `constants/index.js`（唯一读取点）。
- 响应拦截器**直接返回 `res.data`**；业务码 `code !== '200'` **不在这里处理**，由各页面自行 `ElMessage.error(res.msg)`（FE-19 仍未改）。当前业务码判断已收敛到 `isSuccess()`：**116 处调用 / 39 个文件，业务码字面量 0 处**。
- HTTP 401 → 清存储 + 跳登录（防重入）；网络/超时单独中文提示。

`utils/authStorage.js`：localStorage 键 `xm-film-user`；解析失败自动清空。

## 5 常量与格式

`constants/index.js`：

- `API_PATHS`（23 条）+ 各组业务路径（`ORDER_API` / `FILM_API` / `MARK_API` / `ACCOUNT_API` / `RECHARGE_API` / `FUND_FLOW_API` / `STATISTICS_API` / `TMDB_API` / `CINEMA_DIRECTORY_API` / `CINEMA_API` / `AUTH_API` / `TICKET_API`）。
- `apiById` / `apiPage` / `apiBatch` 三个路径助手。
- 状态映射：`ORDER_STATUS_MAP`、`CINEMA_STATUS(_MAP)`、`FILM_STATUS_MAP`、`RECORD_STATUS_MAP`、`RECHARGE_STATUS_MAP`、`ROLE_TAG_MAP`、`ACTOR_ROLE_TYPE_MAP` + 各自的 `get*Type` 兜底。
- `ORDER_DELETABLE_STATUSES` 与后端 `OrderedService.DELETABLE_STATUSES` 靠注释同步。
- `DIRECTORY_IMPORT_CITIES`（45 城）。

`utils/format.js`：`formatBoxOffice`（0 → 「暂无数据」）、`formatYuan`（0 是真实值）、`formatScore`、`formatScoreBadge`、`formatFilmTypes`。

## 6 样式体系

| 文件 | 作用 |
| --- | --- |
| `tokens.scss` | **唯一数值权威**：字号 11 档（`--fs-xs` 12px → `--fs-5xl` 36px）、间距 4px 网格（`--space-4…64`）、深色表面、扩展色、`--color-brand` |
| `global.css` | reset、`.card`、page 灰底、`prefers-reduced-motion`、el-table 表头（全站） |
| `index.scss` | 注入 EP 主色 `#165DFF` |
| `front-pages.scss` | `.page-narrow`(55%)、`.page-wide`(85%)、`.page-card`、`.section-head`、`.poster-grid`、`.filter-*` |
| `admin-pages.scss` | `.crud-page`、`.list-toolbar`、`.table-card`、`.field-*`、`.profile-*` |
| `admin-layout.scss` | 后台外壳（50px 头 / 175px 侧栏） |
| `auth-layout.scss` | 登录/注册共用卡片 |

四条硬规则：

1. **字重只有 400 / 500 / 700**，可能含中文的文本不得用 500（雅黑只有 400/700，500 触发伪粗体）。
2. 颜色/字号/间距一律引用令牌，禁止硬编码。
3. 前台主题色由 `html.theme-front` 覆写（`App.vue` 按路由切换）—— EP 弹层 teleport 到 body，只能挂在 html 上。
4. **覆盖 EP 自带样式必须提高特异性**（如 `.el-table.el-table`）。`global.css` 在 EP 样式**之前**加载，同优先级的选择器会被 EP 覆盖 —— 规则明明在样式表里、计算值却没变，这一坑在 FE-63 上实际发生过。

## 7 页面清单

**front/（16）**

| 页 | 作用 | 主要接口 |
| --- | --- | --- |
| `Home` | 轮播 hero、今日票房、热播/待映、票房 Top10、评分 Top5 | `films`、`films/box-office/today`、`box-office/top`、`mark/top` |
| `Movie` | 影片列表 + 类型/年代/地区/状态筛选 + 分页 | `films/page`、`types`、`areas`、`auth/years` |
| `Search` | 搜索结果 | `films/search` |
| `Rank` | 票房榜 + 口碑榜 | `box-office/top`、`mark/top` |
| `FilmDetail` | 详情 + 演职人员 + 热评前 3 | `films/{id}`、`actors/{id}`、`marks/by-film` |
| `FilmCinema` | 选该片的影院 | `films/{id}`、`cinemas/page?filmId` |
| `Cinema` | 影院列表（深色头图 + 城市/区域 chip 筛选） | `cinemas/regions`、`cinemas/page` |
| `CinemaDetail` | 日期条 + 按片分组场次 | `cinemas/{id}`、`films/by-cinema`、`records/page` |
| `FilmMarks` | 影评列表（写/改/赞） | `films/{id}`、`marks/by-film`、`PUT marks/{id}/like`、`POST/PUT marks` |
| `BuyTicket` | 选座购票 | `orders/seats`、`orders/create`、`orders/{id}/cancel` |
| `Orders` | 我的订单（续付/取消/退票/取票/评价/删除） | `orders/page`、`marks` |
| `Account` | 余额 + 充值 + 单据表 + 流水 + 余额走势 | `account/summary`、`recharges/*`、`fund-flows/page` |
| `PayPassword` | 支付密码 设置/修改/忘记重设（三步） | `pay-password/*` |
| `Person` | 资料 + 头像 | `PUT users` |
| `Password` | 改登录密码 | `PUT auth/password` |
| `Pickup` | 取票大厅（免登录） | `POST tickets/redeem` |

**back/（7）**：`Home`（公告）、`Film`（只读）、`Room`、`Record`、`Ordered`（出票）、`Person`、`Password`。

**manage/（18）**：`Home`（驾驶舱）、`Admin`、`User`、`Cinema`（含审核）、`CinemaDirectory`、`Film`（TMDB 导入）、`Video`、`Actor`、`Type`、`Area`、`Notice`、`Room`、`Record`、`Mark`、`Ordered`（含取消/退票）、`Finance`（充值/资金流水）、`Person`、`Password`。

## 8 组件

| 组件 | 作用 |
| --- | --- |
| `FilmPosterCard` | 2:3 海报卡 + 评分角标 + 破图首字兜底，整卡跳详情 |
| `OrderPayDialog` | 三态支付弹窗（支付 → 6 位密码 → 取票凭证），含倒计时与超时 |
| `PayPasswordInput` | 6 格密码输入（自动跳格、粘贴分发、退格） |
| `DetailSkeleton` | 详情页首屏骨架（`wide` 切横版） |
| `ResetPasswordDialog` | 管理员重置 user/admin/cinema 登录密码；6–64 位校验，成功后关闭 |
| `ErrorBoundary` | `onErrorCaptured` 兜底，包住整个 App |

## 9 已知债务（详见 `audit/frontend-audit.md`）

- **FE-05**：后台父路由已统一门禁，前台仍逐子路由标 `guest` / `requiresAuth`，缺一条路由表断言防未来漏写。
- **FE-09**：`/front` 与 login/register/404 上无消费者的 `meta.name` 仍在。
- **FE-18**：网络类失败保持登录（fail-open）的口径未定，未改。
- **FE-19**：`isSuccess()` 只收敛字面量，失败语义下沉到拦截器仍未做。
- **FE-49**：`front/Orders`、`front/Cinema` 已用 `usePagedList`；`front/Movie`、`back/*` 仍手写分页（已补 `loadSeq`）。
- **FE-60**：`App.vue` / `request.js` 的裸前缀匹配残余两处，当前路由不可达，未做。
- **Sonar 前端**：2 条 `S3776` 复杂度（`front/FilmDetail.vue`、`manage/Film.vue`）未处理；8 条 `S6819` ARIA 已于 2026-10-22 修复。
- 前端自动化测试：**已接入 Vitest**（`npm test`），当前 5 个测试文件覆盖 `utils/`（navigation / format / avatar）、`constants/` 与 `composables/`（usePagedList / useCrud）的纯逻辑；组件渲染与端到端仍靠 `npm run build` + 无头 Chrome 脚本。
