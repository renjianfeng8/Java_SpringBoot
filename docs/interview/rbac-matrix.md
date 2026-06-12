# RBAC 权限矩阵

| 资源 | ADMIN | CINEMA | USER |
|---|---|---|---|
| 管理员 | 全部管理 | 禁止 | 禁止 |
| 用户 | 全部管理 | 禁止 | 仅本人资料 |
| 影院 | 全部管理/审核 | 仅本影院资料 | 只读 |
| 影厅 | 全部管理 | 仅本影院影厅 | 禁止 |
| 排片 | 全部管理 | 仅本影院排片 | 只读 |
| 订单 | 全部管理 | 仅本影院订单 | 仅本人订单 |
| 影片/分类/地区/演员 | 全部管理 | 只读影片 | 只读 |
| 公告/视频 | 全部管理 | 只读 | 只读 |

## 后端执行点

- `AuthInterceptor` 处理通用资源和写操作边界。
- `OrderedService.ensureOrderAccess` 处理订单所属权边界。
- 影院端资源查询在 service/controller 层注入 `cinemaId` scope。
- 用户订单查询在 service/controller 层注入 `userId` scope。
