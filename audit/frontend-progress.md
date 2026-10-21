# 前端未修项攻坚：进度记录（2026-10-21 收尾）

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

- **FE-03 残留**：角色不符已回本角色主页；「已登录访问 `/login`」策略未定。
- **FE-05**：缺一条路由表断言（受保护前缀下每个子路由必须带门禁标记）。
- **FE-09**：`/front` 与 login/register/404 上无消费者的 `meta.name` 仍在。
- **FE-19**：失败语义下沉到拦截器属行为变更，未动。
- **Sonar 前端**：8 条 ARIA（`Web:S6819`）+ 2 条 `S3776` 复杂度。

## 五、临时工具（不在仓库内）

- `%TEMP%\smoke\`：puppeteer-core、`smoke.cjs`（页面渲染冒烟）、`paging.cjs`（翻页行为）、
  `pagedlist.test.mjs`（usePagedList 断言）。需要时可用 `http-server dist -p <port>` 起静态服务再跑。
