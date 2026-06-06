# 用例图、业务流程图与数据流图

本文档补充影院购票管理系统需求阶段常用的三类建模图：用例图、业务流程图、数据流图。图中只表达当前 V1.0 已实现或已在 PRD 中明确的能力，后续扩展能力放在[PRD 优化路线图](./04-prd-optimization-roadmap.md)中说明。

## 1. 用例图

### 1.1 系统总用例图

```mermaid
flowchart LR
    USER["普通用户<br/>USER"]
    CINEMA["影院管理员<br/>CINEMA"]
    ADMIN["系统管理员<br/>ADMIN"]

    subgraph SYS["影院购票管理系统"]
        UC_LOGIN(("登录/注册"))
        UC_PROFILE(("维护个人资料"))
        UC_PASSWORD(("修改密码"))

        UC_BROWSE_FILM(("浏览电影"))
        UC_SEARCH(("搜索影片"))
        UC_RANK(("查看排行榜"))
        UC_FILM_DETAIL(("查看电影详情"))
        UC_BROWSE_CINEMA(("浏览影院"))
        UC_SELECT_RECORD(("选择影院与场次"))
        UC_SELECT_SEAT(("在线选座"))
        UC_CREATE_ORDER(("提交订单"))
        UC_VIEW_ORDER(("查看订单"))
        UC_MARK(("评分评价"))

        UC_CINEMA_PROFILE(("维护影院资料"))
        UC_ROOM(("管理影厅"))
        UC_RECORD(("管理排片"))
        UC_CINEMA_ORDER(("查看影院订单"))
        UC_CINEMA_FILM(("查看可排影片"))

        UC_ADMIN_ACCOUNT(("管理账号"))
        UC_AUDIT_CINEMA(("审核影院"))
        UC_FILM_MGMT(("管理影片"))
        UC_BASE_DATA(("管理分类/地区"))
        UC_ACTOR_VIDEO(("管理演员/预告片"))
        UC_NOTICE(("管理公告"))
        UC_ADMIN_ORDER(("管理订单"))
        UC_ADMIN_MARK(("管理评价"))
    end

    USER --> UC_LOGIN
    USER --> UC_PROFILE
    USER --> UC_PASSWORD
    USER --> UC_BROWSE_FILM
    USER --> UC_SEARCH
    USER --> UC_RANK
    USER --> UC_FILM_DETAIL
    USER --> UC_BROWSE_CINEMA
    USER --> UC_SELECT_RECORD
    USER --> UC_SELECT_SEAT
    USER --> UC_CREATE_ORDER
    USER --> UC_VIEW_ORDER
    USER --> UC_MARK

    CINEMA --> UC_LOGIN
    CINEMA --> UC_PASSWORD
    CINEMA --> UC_CINEMA_PROFILE
    CINEMA --> UC_ROOM
    CINEMA --> UC_RECORD
    CINEMA --> UC_CINEMA_ORDER
    CINEMA --> UC_CINEMA_FILM

    ADMIN --> UC_LOGIN
    ADMIN --> UC_PASSWORD
    ADMIN --> UC_ADMIN_ACCOUNT
    ADMIN --> UC_AUDIT_CINEMA
    ADMIN --> UC_FILM_MGMT
    ADMIN --> UC_BASE_DATA
    ADMIN --> UC_ACTOR_VIDEO
    ADMIN --> UC_NOTICE
    ADMIN --> UC_ADMIN_ORDER
    ADMIN --> UC_ADMIN_MARK
```

### 1.2 用户购票用例分解

```mermaid
flowchart TD
    USER["普通用户"]

    subgraph BUY["用户购票子系统"]
        LOGIN(("登录"))
        BROWSE(("浏览/搜索电影"))
        DETAIL(("查看电影详情"))
        CINEMA(("选择影院"))
        RECORD(("选择场次"))
        SEAT(("选择座位"))
        SUBMIT(("提交订单"))
        ORDER(("查看订单"))
        MARK(("评分评价"))
    end

    USER --> LOGIN
    LOGIN --> BROWSE
    BROWSE --> DETAIL
    DETAIL --> CINEMA
    CINEMA --> RECORD
    RECORD --> SEAT
    SEAT --> SUBMIT
    SUBMIT --> ORDER
    ORDER --> MARK
```

### 1.3 后台管理用例分解

```mermaid
flowchart LR
    ADMIN["系统管理员"]
    CINEMA["影院管理员"]

    subgraph ADMIN_SYS["平台管理后台"]
        MANAGE_USER(("管理用户"))
        MANAGE_CINEMA(("管理/审核影院"))
        MANAGE_FILM(("管理影片"))
        MANAGE_BASE(("管理分类地区"))
        MANAGE_ACTOR(("管理演员"))
        MANAGE_VIDEO(("管理预告片"))
        MANAGE_NOTICE(("管理公告"))
        MANAGE_ORDER(("管理订单"))
        MANAGE_MARK(("管理评价"))
    end

    subgraph CINEMA_SYS["影院后台"]
        CINEMA_PROFILE(("维护影院资料"))
        CINEMA_ROOM(("管理影厅"))
        CINEMA_RECORD(("管理排片"))
        CINEMA_ORDER(("查看订单"))
    end

    ADMIN --> MANAGE_USER
    ADMIN --> MANAGE_CINEMA
    ADMIN --> MANAGE_FILM
    ADMIN --> MANAGE_BASE
    ADMIN --> MANAGE_ACTOR
    ADMIN --> MANAGE_VIDEO
    ADMIN --> MANAGE_NOTICE
    ADMIN --> MANAGE_ORDER
    ADMIN --> MANAGE_MARK

    CINEMA --> CINEMA_PROFILE
    CINEMA --> CINEMA_ROOM
    CINEMA --> CINEMA_RECORD
    CINEMA --> CINEMA_ORDER
```

## 2. 业务流程图

### 2.1 用户购票主流程

```mermaid
flowchart TD
    START([开始])
    LOGIN{是否已登录}
    TO_LOGIN[跳转登录页]
    HOME[进入用户首页]
    SEARCH[浏览/搜索电影]
    FILM_DETAIL[查看电影详情]
    SELECT_CINEMA[选择影院]
    SELECT_RECORD[选择放映场次]
    CHECK_RECORD{场次是否有效}
    SELECT_SEAT[进入 8x8 座位图选座]
    CHECK_SEAT{是否选择座位}
    SUBMIT_ORDER[提交订单]
    SAVE_ORDER[写入订单 ordered]
    SHOW_ORDER[查看我的订单]
    END([结束])

    START --> LOGIN
    LOGIN -- 否 --> TO_LOGIN --> HOME
    LOGIN -- 是 --> HOME
    HOME --> SEARCH --> FILM_DETAIL --> SELECT_CINEMA --> SELECT_RECORD --> CHECK_RECORD
    CHECK_RECORD -- 否 --> SELECT_CINEMA
    CHECK_RECORD -- 是 --> SELECT_SEAT --> CHECK_SEAT
    CHECK_SEAT -- 否 --> SELECT_SEAT
    CHECK_SEAT -- 是 --> SUBMIT_ORDER --> SAVE_ORDER --> SHOW_ORDER --> END
```

业务规则：

- 用户必须登录且角色为 USER 才能进入购票流程。
- 当前版本订单提交后默认进入“待取票”状态。
- 当前座位图为固定 8x8，座位信息以字符串写入 `ordered.seat`。
- 当前版本不包含真实支付和取票码。

### 2.2 影院排片流程

```mermaid
flowchart TD
    START([开始])
    LOGIN[影院管理员登录]
    AUTH{角色是否为 CINEMA}
    DENY[拒绝访问/跳转登录]
    PROFILE[维护影院资料]
    ROOM[维护影厅 room]
    FILM[查看可排影片 film/cinema_film]
    RECORD_FORM[填写排片信息]
    CHECK_RECORD{电影/影院/影厅/时间/价格是否完整}
    SAVE_RECORD[保存排片 record]
    ORDER[查看影院订单 ordered]
    END([结束])

    START --> LOGIN --> AUTH
    AUTH -- 否 --> DENY
    AUTH -- 是 --> PROFILE --> ROOM --> FILM --> RECORD_FORM --> CHECK_RECORD
    CHECK_RECORD -- 否 --> RECORD_FORM
    CHECK_RECORD -- 是 --> SAVE_RECORD --> ORDER --> END
```

业务规则：

- 排片必须关联影院、影厅和电影。
- `record.cinema_id`、`record.room_id`、`record.film_id` 是排片主链路字段。
- 影院端只能进入 `/back/*` 页面，不能进入管理端 `/manage/*`。

### 2.3 管理员内容运营流程

```mermaid
flowchart TD
    START([开始])
    LOGIN[管理员登录]
    AUTH{角色是否为 ADMIN}
    DENY[拒绝访问/跳转登录]
    BASE[维护分类/地区]
    FILM[维护影片信息]
    ACTOR[维护演员信息]
    VIDEO[维护预告片]
    CINEMA_AUDIT[审核影院]
    NOTICE[发布公告]
    MONITOR[查看订单/评价]
    END([结束])

    START --> LOGIN --> AUTH
    AUTH -- 否 --> DENY
    AUTH -- 是 --> BASE --> FILM --> ACTOR --> VIDEO --> CINEMA_AUDIT --> NOTICE --> MONITOR --> END
```

业务规则：

- 管理员拥有平台全局基础数据管理权限。
- 管理员可以访问 `/manage/*` 全部管理页面。
- 后端 `AuthInterceptor` 对管理端资源进行角色校验。

### 2.4 影院注册与审核流程

```mermaid
flowchart TD
    START([开始])
    REGISTER[影院提交注册资料]
    SAVE_CINEMA[写入 cinema 表]
    STATUS[状态为未审核]
    ADMIN_REVIEW[管理员查看影院资料]
    PASS{审核是否通过}
    APPROVE[更新为已审核]
    REJECT[保持/更新为未审核]
    LOGIN[影院账号登录后台]
    END([结束])

    START --> REGISTER --> SAVE_CINEMA --> STATUS --> ADMIN_REVIEW --> PASS
    PASS -- 是 --> APPROVE --> LOGIN --> END
    PASS -- 否 --> REJECT --> ADMIN_REVIEW
```

## 3. 数据流图

### 3.1 上下文数据流图 DFD Level 0

```mermaid
flowchart LR
    USER["普通用户"]
    CINEMA["影院管理员"]
    ADMIN["系统管理员"]

    SYS(("影院购票管理系统"))

    DB[(MySQL 数据库)]
    FILES[(本地文件存储)]

    USER -- 登录信息/浏览条件/选座订单/评价 --> SYS
    SYS -- 电影/影院/排片/订单/评价结果 --> USER

    CINEMA -- 影院资料/影厅/排片/订单查询 --> SYS
    SYS -- 影院资料/排片列表/订单列表 --> CINEMA

    ADMIN -- 基础数据/审核/公告/全局管理操作 --> SYS
    SYS -- 管理列表/统计数据/操作结果 --> ADMIN

    SYS -- 读写业务数据 --> DB
    SYS -- 上传/访问图片视频 --> FILES
```

### 3.2 一层数据流图 DFD Level 1

```mermaid
flowchart TD
    USER["普通用户"]
    CINEMA["影院管理员"]
    ADMIN["系统管理员"]

    P1(("P1 认证与权限"))
    P2(("P2 影片与影院浏览"))
    P3(("P3 排片管理"))
    P4(("P4 选座下单"))
    P5(("P5 订单管理"))
    P6(("P6 评价与排行榜"))
    P7(("P7 平台基础数据管理"))
    P8(("P8 文件上传"))

    D_ACCOUNT[(admin/user/cinema)]
    D_CONTENT[(film/type/area/actor/video/notice)]
    D_SCHEDULE[(cinema_film/room/record)]
    D_ORDER[(ordered)]
    D_MARK[(mark)]
    D_FILE[(本地文件目录)]

    USER -- 账号密码 --> P1
    CINEMA -- 账号密码 --> P1
    ADMIN -- 账号密码 --> P1
    P1 -- JWT/角色信息 --> USER
    P1 -- JWT/角色信息 --> CINEMA
    P1 -- JWT/角色信息 --> ADMIN
    P1 <--> D_ACCOUNT

    USER -- 搜索条件/浏览请求 --> P2
    P2 -- 电影/影院/详情数据 --> USER
    P2 <--> D_CONTENT
    P2 <--> D_SCHEDULE

    CINEMA -- 影厅/排片维护 --> P3
    ADMIN -- 排片维护/查看 --> P3
    P3 <--> D_SCHEDULE
    P3 <--> D_CONTENT

    USER -- 场次/座位/购票数量 --> P4
    P4 -- 订单提交结果 --> USER
    P4 <--> D_SCHEDULE
    P4 --> D_ORDER

    USER -- 我的订单查询 --> P5
    CINEMA -- 影院订单查询 --> P5
    ADMIN -- 全局订单管理 --> P5
    P5 <--> D_ORDER

    USER -- 评分评价 --> P6
    ADMIN -- 评价管理 --> P6
    P6 -- 排行榜/评价列表 --> USER
    P6 <--> D_MARK
    P6 <--> D_CONTENT

    ADMIN -- 影片/分类/地区/公告/账号管理 --> P7
    P7 <--> D_ACCOUNT
    P7 <--> D_CONTENT

    ADMIN -- 图片/视频上传 --> P8
    CINEMA -- 资质/头像上传 --> P8
    P8 --> D_FILE
    P8 -- 文件 URL --> D_CONTENT
    P8 -- 文件 URL --> D_ACCOUNT
```

### 3.3 购票订单数据流图

```mermaid
flowchart LR
    USER["普通用户"]
    FE["Vue 用户端"]
    API["Spring Boot API"]
    AUTH["JWT 认证/角色校验"]
    DB_FILM[(film)]
    DB_CINEMA[(cinema)]
    DB_RECORD[(record)]
    DB_ROOM[(room)]
    DB_ORDER[(ordered)]

    USER -- 选择电影/影院/场次/座位 --> FE
    FE -- 携带 Token 提交订单 --> API
    API --> AUTH
    AUTH -- 校验通过 --> API
    API -- 查询电影 --> DB_FILM
    API -- 查询影院 --> DB_CINEMA
    API -- 查询场次 --> DB_RECORD
    API -- 查询影厅 --> DB_ROOM
    API -- 写入订单 --> DB_ORDER
    DB_ORDER -- 订单结果 --> API
    API -- Result(code/data/msg) --> FE
    FE -- 展示订单状态 --> USER
```

## 4. 图与当前实现的对应关系

| 图 | 对应代码/数据 | 说明 |
|----|---------------|------|
| 用例图 | `router/index.js`、各 Controller、三端页面 | 按 USER/CINEMA/ADMIN 三角色划分 |
| 用户购票流程 | `/front/*` 页面、`OrderedController`、`ordered` 表 | 当前不含真实支付 |
| 影院排片流程 | `/back/room`、`/back/record`、`room`、`record` 表 | 排片通过 ID 关联影院、影厅、电影 |
| 管理员运营流程 | `/manage/*` 页面、基础数据 Controller | 覆盖影片、影院、订单、评价、公告等后台能力 |
| DFD Level 0 | 前端、后端、MySQL、本地文件存储 | 展示系统外部参与者和数据边界 |
| DFD Level 1 | 认证、浏览、排片、下单、订单、评价、文件上传 | 展示主要数据处理过程和数据存储 |

## 5. 后续可扩展图

如果进入二期迭代，可以继续补充：

- 支付状态机图。
- 退票/改签流程图。
- 影院对象级权限校验流程图。
- 动态座位模板数据流图。
- 管理端运营看板数据流图。
