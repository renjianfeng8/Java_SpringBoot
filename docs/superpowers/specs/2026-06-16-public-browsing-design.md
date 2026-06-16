# 公开浏览 + 操作鉴权 设计文档

## 概述

将系统从「强制登录才能进入」改造为「公开浏览 + 关键操作鉴权」模式。用户访问域名直接进入首页，可自由浏览电影、影院、排行榜等公开内容，仅在购票、订单等操作时需要登录。登录后根据角色（USER/CINEMA/ADMIN）在顶栏展示对应的后台入口。

## 相关文档

- [CLAUDE.md](../../../CLAUDE.md) — 项目主文档
- [路由配置](../../../xm_film/vue/src/router/index.js) — 当前路由定义
- [AuthInterceptor](../../../xm_film/springboot/src/main/java/com/example/springboot/common/config/AuthInterceptor.java) — 后端认证拦截器

## 设计

### 1. 前端路由鉴权重构

**文件**: `src/router/index.js`

#### 1.1 父路由改为公开

```js
// 改前
{ path: '/front', component: FrontLayout, meta: { requiresAuth: true, roles: ['USER'] } }

// 改后
{ path: '/front', component: FrontLayout, meta: { guest: true } }
```

#### 1.2 受保护子路由加 meta

为 `buyTicket`、`orders`、`person`、`password` 四个子路由添加：

```js
{ path: 'buyTicket', meta: { requiresAuth: true, roles: ['USER'] }, ... }
{ path: 'orders', meta: { requiresAuth: true, roles: ['USER'] }, ... }
{ path: 'person', meta: { requiresAuth: true, roles: ['USER'] }, ... }
{ path: 'password', meta: { requiresAuth: true, roles: ['USER'] }, ... }
```

#### 1.3 beforeEach 守卫调整

- 保留 `guest` 路由的无条件放行
- 改为检查**当前路由**的 `meta.requiresAuth`（不继承父路由）
- 未登录访问受保护路由时：弹出 `ElMessageBox.confirm` 提示「请先登录后再进行此操作」，点击确定跳转 `/login?redirect=<当前路径>`
- `redirect` 参数用于登录后回跳原页面

#### 1.4 角色默认首页映射

| 角色 | 登录后默认首页 |
|------|--------------|
| USER | `/front/home` |
| CINEMA | `/back/home` |
| ADMIN | `/manage/home` |

Login.vue 登录成功后检查 `route.query.redirect`，有则回跳，无则按角色跳到默认首页。

#### 1.5 受影响文件清单

| 文件 | 改动 |
|------|------|
| `src/router/index.js` | 路由表 + beforeEach 鉴权逻辑 |
| `src/views/Login.vue` | 登录成功回跳逻辑 |

---

### 2. 前端顶栏自适应

**文件**: `src/views/Front.vue`

#### 2.1 渲染逻辑

引入 `useAuth` composable，用 `isLoggedIn`、`isUser`、`isCinema`、`isAdmin` 控制顶栏右侧区域：

```
if (isLoggedIn) {
  if (isCinema || isAdmin) {
    显示「管理后台」按钮（跳转 /back/home 或 /manage/home）
  }
  显示用户头像 + 用户名 + 下拉菜单（个人中心/修改密码/退出）
} else {
  显示「登录」·「注册」链接
}
```

#### 2.2 管理后台入口按钮

CINEMA/ADMIN 登录后，顶栏导航栏右侧、用户头像左方新增一个 `<el-button type="primary" size="small">`，文案为「管理后台」，点击跳转对应后台首页。

#### 2.3 受影响文件清单

| 文件 | 改动 |
|------|------|
| `src/views/Front.vue` | 顶栏右侧渲染改造 |

---

### 3. 前端操作鉴权流程

#### 3.1 路由守卫弹窗

在 `router/beforeEach` 中，未登录访问受保护路由时：

```js
if (to.meta.requiresAuth) {
  const user = getStoredUser()
  if (!user?.token) {
    ElMessageBox.confirm('请先登录后再进行此操作', '提示', {
      confirmButtonText: '去登录',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      next(`/login?redirect=${to.path}`)
    }).catch(() => {
      next(false)  // 取消导航，留在当前页
    })
    return
  }
  // 角色校验...
}
```

#### 3.2 401 过期处理

`request.js` 的 401 拦截保持原行为（清除用户 + 提示「登录已过期，请重新登录」）。用户看到提示后被路由守卫拦截（下次操作触发弹窗 → 跳登录），或手动刷新后被路由守卫拦住。

#### 3.3 登录成功回跳

`Login.vue` 登录成功后：

```js
const redirect = route.query.redirect || getDefaultPath(user.role)
window.location.href = redirect
```

`getDefaultPath` 映射：USER → `/front/home`，CINEMA → `/back/home`，ADMIN → `/manage/home`。

---

### 4. 后端公开读免登录

**文件**: `AuthInterceptor.java`

#### 4.1 PUBLIC_READ_PREFIXES

新增公开读资源集合，这些资源的 GET 请求免登录：

```java
private static final Set<String> PUBLIC_READ_PREFIXES = Set.of(
    "/api/v1/films",
    "/api/v1/cinemas",
    "/api/v1/types",
    "/api/v1/areas",
    "/api/v1/notices",
    "/api/v1/actors"
);
```

#### 4.2 preHandle 改造

当前逻辑：
```java
// 有 token → 解析并校验
// 无 token → 直接 401
```

改造后：
```java
// 有 token → 解析并校验（不变）
// 无 token → 检查是否公开读路径 + GET 方法 → 放行
//           否则 401
```

```java
if (token == null) {
    if ("GET".equalsIgnoreCase(request.getMethod()) &&
        PUBLIC_READ_PREFIXES.stream().anyMatch(p -> request.getRequestURI().startsWith(p))) {
        return true;
    }
    response.setStatus(401);
    // 返回 JSON 错误
    return false;
}
```

> 注意：`/api/v1/auth/login` 等路径已在 `WebMvcConfig.excludePathPatterns` 中排除，不受拦截器影响。

#### 4.3 受影响文件清单

| 文件 | 改动 |
|------|------|
| `AuthInterceptor.java` | 新增 PUBLIC_READ_PREFIXES + 匿名 GET 放行逻辑 |

## 边界情况

| 场景 | 行为 |
|------|------|
| 未登录访问 `/front/buyTicket` | 弹确认框 → 跳登录 → 登录后回跳购票页 |
| 登录过期停留在页面 | 点击购票等操作时 401 → 清除用户 → 提示 → 跳登录 |
| USER 直接输入 `/back/home` | 路由守卫拦截 + 后端 403 |
| 匿名用户 POST 给 films 接口 | 后端 401（需登录） |
| CINEMA 登录后访问前台 | 顶栏有「管理后台」按钮，下拉菜单保留 |

## 未涉及范围

- 本设计不修改后端业务逻辑、不修改数据库
- 本设计不涉及 `/back/*` 和 `/manage/*` 的路由守卫（已有且不变）
- 本设计不涉及文件上传、订单创建等已有业务接口的改
