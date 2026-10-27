# e2e（Playwright 冒烟）

只覆盖单测照不到的接缝：表单提交、跨页流程、SQL 条件、库约束。5 条用例对应 2026-10-27
那一轮实测出的 4 个缺陷 + 未登录拦截，改动这些路径时先跑这里。

## 前置条件

| 需要 | 说明 |
| --- | --- |
| 后端 | `9090` 已启动，且连的是**开发库** `xm-film`（含 `demo-data.sql` 的演示数据） |
| 前端 | 不用手动起：`webServer` 会自动 `npm run dev`（已在跑则复用） |
| 浏览器 | 用系统 Chrome（`channel: 'chrome'`），不下载 Playwright 自带 chromium |
| 账号 | `u1/u1`（用户）、`999/999`（管理员） |

```bash
# 终端 A
cd xm_film/springboot && mvn -o spring-boot:run
# 终端 B
cd xm_film/vue && npm run test:e2e
```

## 用例与数据卫生

| 文件 | 覆盖 | 是否写库 |
| --- | --- | --- |
| `auth.spec.js` | 未登录拦截、注册提交、新账号登录 | 建一个 `e2e_*` 账号，`finally` 里由管理员删掉 |
| `order.spec.js` | 选座 → 下单 → 取消 → 座位重新可选 | 下单后取消，`finally` 里删除废单 |
| `admin.spec.js` | 影院「未审核」筛选、重复分类 409 | 否（重名请求被库拒绝） |

用例共用同一个库，所以 `workers: 1`。别把这里写成并发压测：座位与账号会互相踩。

## CI 里的差别

e2e **不在 CI 里跑**：它依赖 `demo-data.sql` 之外的基础数据（影片、影院名录、影厅、排片），
而本仓不含业务数据行。CI 只跑 `mvn test` 与前端 lint / 单测 / 构建；冒烟由本机或带开发库的
runner 负责。CI 若以后要加这一层，需要先补一份不含个人数据的最小种子脚本。
