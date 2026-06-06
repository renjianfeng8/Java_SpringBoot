# 架构与模块设计图

本文档补充系统设计阶段常用的架构图和模块图，用于说明影院购票管理系统的静态结构、运行关系和核心调用链路。

## 1. 系统上下文图

```mermaid
flowchart LR
    USER["普通用户"]
    CINEMA["影院管理员"]
    ADMIN["系统管理员"]
    SYS["影院购票管理系统"]
    MYSQL[(MySQL)]
    FILES[(本地文件存储)]

    USER -- 浏览/购票/评价 --> SYS
    CINEMA -- 影厅/排片/订单管理 --> SYS
    ADMIN -- 平台运营/审核/基础数据 --> SYS
    SYS -- 读写业务数据 --> MYSQL
    SYS -- 上传/访问图片视频 --> FILES
```

## 2. 前后端分离架构图

```mermaid
flowchart TB
    Browser["浏览器"]
    Vue["Vue 3 + Vite"]
    Router["Vue Router"]
    Axios["Axios request"]
    Spring["Spring Boot 3.3"]
    MVC["Controller / Service / Mapper"]
    MyBatis["MyBatis XML"]
    DB[(MySQL 8.0)]

    Browser --> Vue
    Vue --> Router
    Vue --> Axios
    Axios -- HTTP JSON + JWT --> Spring
    Spring --> MVC
    MVC --> MyBatis
    MyBatis --> DB
```

## 3. 后端模块结构图

```mermaid
flowchart TD
    App["SpringbootApplication"]

    subgraph Common["common 公共层"]
        BaseController["BaseController"]
        BaseService["BaseService"]
        BaseMapper["BaseMapper"]
        Result["Result"]
        JwtUtils["JwtUtils"]
        FileUtil["FileUtil"]
    end

    subgraph Config["common/config 配置层"]
        AuthInterceptor["AuthInterceptor"]
        WebMvcConfig["WebMvcConfig"]
        CorsConfig["CorsConfig"]
    end

    subgraph Business["业务层"]
        Controller["controller"]
        Service["service"]
        Mapper["mapper"]
        Entity["entity"]
        XML["resources/mapper XML"]
    end

    subgraph Exception["异常层"]
        CustomException["CustomException"]
        GlobalHandler["GlobalExceptionHandler"]
    end

    App --> Config
    Controller --> BaseController
    Service --> BaseService
    Mapper --> BaseMapper
    Controller --> Service
    Service --> Mapper
    Mapper --> XML
    Controller --> Result
    Config --> JwtUtils
    Controller --> FileUtil
    Exception --> Result
```

## 4. 前端模块结构图

```mermaid
flowchart TD
    Main["main.js"]
    App["App.vue"]
    Router["router/index.js"]

    subgraph Views["views 页面"]
        Front["front 用户端"]
        Back["back 影院端"]
        Manage["manage 管理端"]
        Login["Login/Register/404"]
    end

    subgraph Shared["共享能力"]
        Request["utils/request.js"]
        AuthStorage["utils/authStorage.js"]
        Constants["constants/index.js"]
        Composables["composables"]
        Assets["assets"]
    end

    Main --> App
    Main --> Router
    Router --> Front
    Router --> Back
    Router --> Manage
    Router --> Login
    Front --> Request
    Back --> Request
    Manage --> Request
    Request --> AuthStorage
    Request --> Constants
    Front --> Composables
    Back --> Composables
    Manage --> Composables
```

## 5. 三端角色模块图

```mermaid
flowchart LR
    subgraph UserSide["用户端 /front"]
        UHome["首页"]
        UMovie["电影列表/详情"]
        UCinema["影院列表/详情"]
        UBuy["选座购票"]
        UOrder["我的订单"]
        URank["排行榜/搜索"]
        UProfile["个人资料/密码"]
    end

    subgraph CinemaSide["影院端 /back"]
        CHome["影院首页"]
        CFilm["电影信息"]
        CRoom["影厅管理"]
        CRecord["排片管理"]
        COrder["订单管理"]
        CProfile["影院资料/密码"]
    end

    subgraph AdminSide["管理端 /manage"]
        AHome["管理首页"]
        AAccount["管理员/用户/影院"]
        AFilm["电影/演员/视频"]
        ABase["分类/地区"]
        ANotice["公告"]
        ARoomRecord["影厅/排片"]
        AOrderMark["订单/评价"]
        AProfile["个人资料/密码"]
    end
```

## 6. 标准 CRUD 调用链路

```mermaid
sequenceDiagram
    participant Page as Vue 页面
    participant Request as Axios request
    participant Interceptor as AuthInterceptor
    participant Controller as BaseController
    participant Service as BaseService
    participant Mapper as BaseMapper/XML
    participant DB as MySQL

    Page->>Request: 发起 GET/POST/PUT/DELETE
    Request->>Interceptor: 携带 Authorization Token
    Interceptor->>Interceptor: 解析 JWT 和校验角色
    Interceptor->>Controller: 放行请求
    Controller->>Service: 调用业务方法
    Service->>Mapper: 调用 Mapper
    Mapper->>DB: 执行 SQL
    DB-->>Mapper: 返回结果
    Mapper-->>Service: 返回实体/列表
    Service-->>Controller: 返回业务结果
    Controller-->>Request: Result(code,msg,data)
    Request-->>Page: 渲染页面
```

## 7. 登录认证调用链路

```mermaid
sequenceDiagram
    participant User as 用户/影院/管理员
    participant Vue as Vue Login
    participant API as AuthController
    participant Service as Account Services
    participant JWT as JwtUtils
    participant Storage as authStorage

    User->>Vue: 输入账号密码和角色
    Vue->>API: POST /api/v1/auth/login
    API->>Service: 按角色查询账号并校验密码
    Service-->>API: 账号信息
    API->>JWT: 生成 Token
    API-->>Vue: 返回用户信息 + token
    Vue->>Storage: 保存登录态
    Vue->>Vue: 按角色跳转 /front /back /manage
```

## 8. 文件上传链路

```mermaid
flowchart LR
    Page["前端上传控件"]
    API["/api/v1/files/upload"]
    FileUtil["FileUtil MIME 白名单"]
    Disk[(本地上传目录)]
    URL["文件访问 URL"]

    Page -- multipart/form-data --> API
    API --> FileUtil
    FileUtil -- 校验通过 --> Disk
    Disk --> URL
    URL --> Page
```

## 9. 设计图使用说明

- 答辩汇报优先使用：系统上下文图、前后端分离架构图、三端角色模块图。
- 技术评审优先使用：后端模块结构图、标准 CRUD 调用链路、登录认证调用链路。
- 安全说明优先使用：登录认证调用链路、文件上传链路。
