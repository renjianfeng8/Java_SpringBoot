# 前端未修项攻坚：进度记录（2026-10-20）

> 本轮目标：集中处理 `audit/frontend-audit.md` 里「本轮登记未做 / 2026-10-18 全盘复核新增」
> 与 Sonar 前端剩余项。一次只做一类，每类都过 `npm run build`，提交信息里写验证方式。

## 一、已提交（master）

| 提交 | 内容 | 对应条目 | 验证 |
| --- | --- | --- | --- |
| `8708df41` | 新增 `utils/navigation.js`（角色主页 / loginPath / 路径边界匹配 / 站名）、`useLoginPrompt`、`useLogout`；useAuth 订阅 storage 变化、`isLoggedIn` 只看令牌；useCrud 写入后清空选中；useFormDialog `openEdit` 深拷贝并补齐默认键 | FE-04/06/07、FE-20/21/22、FE-50 | build 通过 |
| `20761e88` | back/Film、back/Ordered、back/Room、back/Record、back/Home、front/Movie、front/FilmCinema 补 `loadSeq`；FilmMarks 翻页补序号 | FE-65 | build 通过 |
| `cf25d061` | 三个资料页表单跟随 `useAuth` 权威会话，且有未保存修改时不覆盖；manage/Person 不再整份提交 token/角色 | FE-51 | build 通过 |
| `db2c8342` | manage/CinemaDirectory 补 `loadSeq` | FE-65 | build 通过 |
| `5795e04b` | `front-pages.scss` 提供 `.measure*` 共享测度；9 处手写 `60% + 1200px` 与两个页头改为引用 | FE-58 | build 通过 + 产物核对 |
| `506b89fb` | 新增 `AdminShell.vue` + `constants/adminMenu.js`，Back/Manage 只剩传参薄壳 | FE-64 | build 通过 |
| `4312b0fd` | 前台导航改由路由 `meta.nav` 派生（`constants/nav.js`） | FE-59 | build + 无头浏览器实测 6 个路由导航/高亮 |
| `0e746db0` | 守卫加登录确认框在途标志；站名统一 `SITE_NAME`，标题写入前判等 | FE-08、FE-10 | build + 冒烟 |
| `049f98f3` | `constants/cinemaServices.js`，三个页面的影院服务改为 v-for | FE-57 | build + 冒烟 |

## 二、进行中（**未提交**）

工作区现有三处改动，是「只读分页收敛」这一项的中间状态：

```
 M xm_film/vue/src/views/front/Cinema.vue
 M xm_film/vue/src/views/front/Orders.vue
?? xm_film/vue/src/composables/usePagedList.js
```

- **`usePagedList.js`（新增，已完成）**：只读分页的唯一实现，负责分页、请求序号、
  越界页回退第 1 页、失败清空 + 错误文案。对外是 `{ items, state, load, reload, setPage, setPageSize }`。
- **front/Orders.vue（已改完，build 通过）**：表格数据改绑 `rows = list.items`，
  分页器绑 `state.pageNum/pageSize/total`，`load()` 走 `pagination.load()`，`search()` 走
  `pagination.reload({ orders, status })`。
- **front/Cinema.vue（已改完，build 通过，但冒烟被中断、未复核）**：
  `data` 里保留 `total: null`（「尚未取到结果」）语义，`load()` 把 `list.state` 同步回 `data`。
  **注意**：这个文件第一次用脚本改时按行号截断，误删了 `<script setup>` 头部，已 `git checkout` 还原后
  用精确文本替换重做；当前版本 diff 只有预期的两处。

### 已验证
- `npm run build` 通过。
- `usePagedList` 的 4 条语义断言（stale 响应、失败清空、越界回退、reload 复位）在
  `%TEMP%\smoke\pagedlist.test.mjs` 里用 esbuild 打包真实源码跑通，全绿。

### 未验证（下次继续的第一步）
- 无头浏览器冒烟 `/front/orders`、`/front/cinema` 被中断，**还没拿到最后一轮结果**。
  脚本在 `%TEMP%\smoke\smoke.cjs`（用法：先 `http-server dist -p <port>`，再
  `node smoke.cjs http://127.0.0.1:<port> /front/orders /front/cinema`）。
  注意：`/front/orders` 需要登录态，否则守卫会弹确认框、页面停在原地，这是预期行为。

## 三、待做（按建议顺序）

1. **收尾工作区**：复跑冒烟 → 提交「只读分页收敛（FE-49/65）」。
2. **FE-19**：把 `isSuccess` 的判断继续下沉（拦截器统一失败语义）属行为变更，需先定口径；
   可先只做「调用方一律判 `res.data`」的清单核对。
3. **FE-03 残留**：角色不符已改回本角色主页（已做），登录页「已登录访问 /login」的落地策略仍未定。
4. **FE-05**：路由 meta 两套范式已在前台/后台统一为「门禁标在各自层级」，
   建议补一条路由表断言（受保护前缀下每个子路由必须带门禁标记）。
5. **FE-09**：`/front` 与 login/register/404 上无消费者的 `meta.name` 仍在，删除属纯清理。
6. **FE-60**：裸前缀匹配已收敛到 `isPathWithin`，剩余检查点只有 `Login.vue` 的 `startsWith('/')`
   （属开放重定向防护，不是路径段匹配，保留）。
7. **Sonar 前端 32 条里的前端部分**：8 条 ARIA（`Web:S6819`）+ 2 条 `S3776` 复杂度，
   建议单独一轮，先做 `manage/Film.vue` 的 `div role="button"` 与两个超阈函数拆分。

## 四、临时工具（不在仓库内）

- `%TEMP%\smoke\`：puppeteer-core、`smoke.cjs`（页面冒烟）、`pagedlist.test.mjs`（usePagedList 断言）。
- 两者都不进仓库，需要时可重建。
