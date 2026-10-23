# 前端未修项攻坚：进度记录（2026-10-22 收尾）

> 当前代码基线：`master` @ `2286861e` + 2026-10-22 未提交的 A/B/C 项改动。下列「已提交」表只到 `93adba6e`，A/B/C 项均尚未提交。

> 目标：集中处理 `audit/frontend-audit.md` 里「本轮登记未做 / 2026-10-18 全盘复核新增」
> 与 Sonar 前端剩余项。一次只做一类，每类过 `npm run build`，能起浏览器的额外跑无头 Chrome。

## 一、已提交

| 提交 | 内容 | 条目 |
| --- | --- | --- |
| `8708df41` | `utils/navigation.js`（角色主页 / loginPath / 路径边界 / 站名）、`useLoginPrompt`、`useLogout`；useAuth 订阅 storage 变化；`isLoggedIn` 只看令牌；useCrud 写后清选中；useFormDialog 深拷贝 + 补齐缺键 | FE-04/06/07/20/21/22/50 |
| `20761e88` | 后台 5 个列表 + front/Movie、front/FilmCinema 补 `loadSeq`；FilmMarks 翻页补序号 | FE-65 |
| `cf25d061` | 三个资料页表单跟随权威会话，未保存时不覆盖；manage/Person 不再整份提交 token/角色 | FE-51 |
| `db2c8342` | manage/CinemaDirectory 补 `loadSeq` | FE-65 |
| `5795e04b` | `front-pages.scss` 的 `.measure*`；9 处手写 `60% + 1200px` 与两个页头改为引用 | FE-58 |
| `506b89fb` | `AdminShell.vue` + `adminMenu.js`，Back/Manage 只剩薄壳 | FE-64 |
| `4312b0fd` | 前台导航由路由 `meta.nav` 派生 | FE-59 |
| `0e746db0` | 登录确认框在途守卫；站名与标题统一 | FE-08/10 |
| `049f98f3` | `cinemaServices.js`，三个页面影院服务 v-for | FE-57 |
| `120df194` | `usePagedList.js`；front/Orders、front/Cinema 改用它 | FE-49/65 |
| `93adba6e` | 本进度文档（第一版） | — |

## 二、验证方式

- 每类改动都过 `npm run build`。
- 页面级：无头 Chrome 加载 `dist` 并拦截 `/api/v1/**` 返回固定数据，
  断言导航/高亮、影院卡片、订单行、分页文案与控制台无异常。
- `usePagedList`：esbuild 打包真实源码 + 桩掉 vue/request，在 node 里断言四条语义
  （旧响应不覆盖新结果 / 失败清空 / 越界回退 / 换筛选回第 1 页）。

## 三、本轮踩到的坑（防回归）

1. **`el-pagination` 页码不要「`v-model` + 回调里再 setPage」**：
   `v-model` 已写入新页码，回调再按旧值写回会把它覆盖，表现为点了页码不动。
2. **前台分页器被压缩**：`el-pagination` 在 flex 容器里会被压到只显示当前页，
   需要 `flex-shrink: 0`；`front/Cinema` 的 `layout` 原先缺 `sizes`，`:page-sizes` 是死配置。
3. **脚本按行号截断會误删内容**：本轮改 `Cinema.vue` 时一次按行号拼接把 `<script setup>` 头删掉了，
   已还原并用精确文本替换重做。以后改大文件只做精确替换。

## 四、仍未做

- ~~**FE-03 残留**：角色不符已回本角色主页；「已登录访问 `/login`」策略未定。~~ 2026-10-22 已修，见 §六。
- **FE-05**：缺一条路由表断言（受保护前缀下每个子路由必须带门禁标记）。
- **FE-09**：`/front` 与 login/register/404 上无消费者的 `meta.name` 仍在。
- **FE-18**：网络类失败保持登录（fail-open）的口径未定，未改。
- **FE-19**：失败语义下沉到拦截器属行为变更，未动。
- **FE-49**：`front/Orders`、`front/Cinema` 已用 `usePagedList`；`front/Movie`、`back/*` 仍手写分页（已补 `loadSeq`）。
- **FE-60**：`App.vue` / `request.js` 裸前缀匹配残余两处，当前路由不可达，未做。
- **Sonar 前端**：8 条 ARIA（`Web:S6819`）已处理（2026-10-22），2 条 `S3776` 复杂度仍未动。

## 五、临时工具（不在仓库内）

- `%TEMP%\smoke\`：puppeteer-core、`smoke.cjs`（页面渲染冒烟）、`paging.cjs`（翻页行为）、
  `pagedlist.test.mjs`（usePagedList 断言）。需要时可用 `http-server dist -p <port>` 起静态服务再跑。

## 六、2026-10-22 追加（用户感知四项）

| 条目 | 做了什么 | 验证 |
| --- | --- | --- |
| FE-03 收尾 | `Login.vue` 已登录时回 `?redirect=`（站内、非 `/login`、角色允许）或角色主页；登录成功复用同一 `landingPath()` | 冒烟：游客留登录页、已登录回主页/目标页、`redirect=/login` 不循环 |
| BL-49 前端兜底 | 详情请求成功但 `data` 为空时显示「电影/影院/场次不存在或已下架」，不再出现「加载失败：操作成功」。覆盖 FilmDetail（影片+演员）、CinemaDetail、FilmCinema、BuyTicket | 冒烟：四个失效详情页文案均正确 |
| 管理端筛选 | 影片列表加类型/地区/年份/状态；用户列表加电话/角色；订单列表加电影/影院。三者均复用后端已有查询字段，未改接口 | 冒烟：三页筛选参数正确 |
| 分页条数 | 当时 18 个列表页的 `:page-sizes` 增加 50/100（原最大 20）；新增 `manage/Finance` 后当前为 23 个页面 / 24 处分页配置 | 冒烟：切到 100 条/页后请求 `pageSize=100` |

四项均过 `npm run build`；无头 Chrome 拦截 `/api/v1` 共 13 条断言全绿（8 + 5）。

## 七、2026-10-22 追加（B 项：管理端与一致性）

| 条目 | 做了什么 | 验证 |
| --- | --- | --- |
| B1 批量删除点名 | 排片/影厅/用户/订单的批量删除仍保持「全有或全无」，但不再遇到第一条被引用就中断；`BatchDeleteGuard` 汇总被挡行（类型 + id + 名称 + 原因），最多列 5 条，其余报数量 | `RecordDeleteGuardTest` 新增断言：文案含「整批未删除」「排片 #7」且不调用 `deleteBatch` |
| B2 跨页勾选 | 13 个 manage 列表 + 影院名录 + 3 个 back 列表加 `row-key` + `reserve-selection`；`useCrud` 暴露 `tableRef`，写入成功、查询、重置时同时清表格内部选中，避免带已删除/已隐藏 id 去批量删 | 无头 Chrome 5/5：第 1 页选 1 条、翻到第 2 页再选 1 条，界面显示「已选 2 项」，批量删除请求体为 `[1,11]`；查询后选中清零 |
| B3 大盘同源 | `StatisticsService` 的待取票数改为从同一份 `orderStatus` 分组派生，不再对订单表发第二次 COUNT，避免同一屏 KPI 与图表对不上 | `StatisticsServiceTest` 5/5；`pendingPickupOrders` 断言改为 `12L` |
| B4 补预告片查询 | 新增 `FilmMapper.selectMissingVideo()`，只查 `video` 为空的行；不再用带票房聚合与 area/actor JOIN 的 `selectAll` 扫全表 | `TmdbBackfillServiceTest` 6/6；MySQL mapper 加载测试通过 |
| B5 ARIA | `div role=button` 改原生 `<button>`（内部 div 改 span，补按钮样式重置）；4 处 `role=group` 改 `<fieldset aria-label>` 并清默认边框/内边距；`role=status` 改 `<output>`；走势图改 `aria-hidden`，结论由可见 caption 承担 | `Web:S6819` 的 8 个位置已无 ARIA role；build + 原有前台冒烟 8/8 |

验证汇总：后端 `mvn test` 43 个测试类 / 403 条用例，0 失败；前端 `npm run build` 通过；无头 Chrome 8/8 + 5/5 + B2 5/5。

## 八、2026-10-22 追加（C 项：管理端能力）

| 条目 | 做了什么 | 验证 |
| --- | --- | --- |
| C1 管理员重置密码 | `PUT /auth/password/reset` 只允许 ADMIN；按目标 `role` 路由到 user/admin/cinema 三张账号表，新密码 BCrypt 后写库，统一 6–64 位校验；三个管理列表共用 `ResetPasswordDialog`，操作日志只记操作人/目标，不打印密码 | `GlobalExceptionHandlerTest` 非 ADMIN 返回 403、ADMIN 正确路由；`UserServiceTest` 断言入库为 BCrypt 且不校验旧密码；前端冒烟正确发送 `{id,role,newPassword}` |
| C2 管理端取消/退票 | 订单页在「待支付」显示取消、在「待取票」显示退票，复用既有 `PUT /orders/{id}/cancel`、`/refund`；确认框带订单号/金额，成功后重载并清选中。沿用后端状态机，管理员不绕过开场前 60 分钟退款截止 | 无头 Chrome：取消/退票分别命中 `PUT /orders/11/cancel`、`/orders/12/refund` |
| C3 充值 / 资金流水 | 新增 `/manage/finance` 两个只读页签：充值单据（单号/用户/金额/状态/时间）与资金流水（用户/来源/关联单/变动额/余额），支持单号、用户 ID、状态、来源筛选及分页；后端查询 LEFT JOIN `user.name` 带出姓名。未加人工入账，避免在 SEC-01 未处理时新开资金写入面 | 无头 Chrome：两页签数据、姓名、金额正确；前端 build 通过 |

C 项验证：后端全量 `mvn test` 通过（0 失败）；前端 `npm run build` 通过；新增无头 Chrome 5/5 通过。C1 暂未做「强制下次登录改密/DB 审计流水」，C2 未做管理员超时退款绕过，C3 未做人工补单，均需先定策略。
