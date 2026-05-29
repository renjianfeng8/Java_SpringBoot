# 全链路架构重构 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**目标:** 将影院购票管理系统从功能完整但架构粗糙的阶段，升级为架构清晰、代码复用、安全加固的高质量实战项目

**架构:** 后端 BaseController/BaseService 泛型抽象 + DTO 分层 + RESTful URL；数据库 film_type 关联表规范化 + 索引优化；前端 Composables 提取 + 路由鉴权守卫 + 共享常量

**Tech Stack:** Java 17, Spring Boot 3.3, MyBatis, MySQL 8, Vue 3, Vite, Element Plus, Playwright

**设计文档:** `docs/superpowers/specs/2026-05-29-architecture-refactoring-design.md`

---

## 文件变更总览

### 后端新增文件
| 文件路径 | 职责 |
|----------|------|
| `springboot/src/main/java/com/example/springboot/common/BaseController.java` | 通用 CRUD 控制器基类 |
| `springboot/src/main/java/com/example/springboot/common/BaseService.java` | 通用 CRUD 服务基类 |
| `springboot/src/main/java/com/example/springboot/dto/request/LoginRequest.java` | 登录请求 DTO |
| `springboot/src/main/java/com/example/springboot/dto/request/PasswordRequest.java` | 密码修改请求 DTO |
| `springboot/src/main/java/com/example/springboot/dto/request/AdminCreateRequest.java` | 管理员创建请求 DTO |
| `springboot/src/main/java/com/example/springboot/dto/request/AdminUpdateRequest.java` | 管理员更新请求 DTO |
| `springboot/src/main/java/com/example/springboot/dto/request/UserCreateRequest.java` | 用户创建请求 DTO |
| `springboot/src/main/java/com/example/springboot/dto/request/UserUpdateRequest.java` | 用户更新请求 DTO |
| `springboot/src/main/java/com/example/springboot/dto/request/CinemaCreateRequest.java` | 影院创建请求 DTO |
| `springboot/src/main/java/com/example/springboot/dto/request/CinemaUpdateRequest.java` | 影院更新请求 DTO |
| `springboot/src/main/java/com/example/springboot/dto/request/FilmCreateRequest.java` | 电影创建请求 DTO |
| `springboot/src/main/java/com/example/springboot/dto/request/FilmUpdateRequest.java` | 电影更新请求 DTO |
| `springboot/src/main/java/com/example/springboot/dto/response/AdminVO.java` | 管理员响应 VO |
| `springboot/src/main/java/com/example/springboot/dto/response/UserVO.java` | 用户响应 VO |
| `springboot/src/main/java/com/example/springboot/dto/response/CinemaVO.java` | 影院响应 VO |
| `springboot/src/main/java/com/example/springboot/dto/response/FilmVO.java` | 电影响应 VO |

### 后端修改文件
| 文件路径 | 修改内容 |
|----------|----------|
| 所有 Controller (14个) | 继承 BaseController，RPC→RESTful URL |
| 所有 Service (13个) | 继承 BaseService，删除重复 CRUD |
| AuthInterceptor.java | 路径匹配改为 `/api/v1/**` |
| WebMvcConfig.java | 静态资源路径对齐 |
| JwtUtils.java | JJWT 弃用 API 升级 |
| FileUtil.java | MIME 白名单校验 |
| 所有 Mapper XML (13个) | 添加 `deleteBatch` 批量删除 |
| FilmMapper.xml | type_ids LIKE → film_type JOIN |
| FilmService.java | fillFilmTypes 逻辑重构 |
| Film.java | 移除 typeIds/typeId/types 字段 |
| Account.java / Admin.java / User.java / Cinema.java | 修复字段遮蔽 |
| application.yml | 环境变量注入 + SQL 日志配置 |

### 前端新增文件
| 文件路径 | 职责 |
|----------|------|
| `vue/src/composables/useAuth.js` | 认证状态管理 |
| `vue/src/composables/useCrud.js` | 通用 CRUD 逻辑 |
| `vue/src/composables/useFormDialog.js` | 表单弹窗逻辑 |
| `vue/src/constants/index.js` | 共享常量(类型/地区/状态映射+API路径) |
| `vue/src/utils/format.js` | 格式化工具函数 |
| `vue/src/utils/status.js` | 状态映射工具函数 |

### 前端修改文件
| 文件路径 | 修改内容 |
|----------|----------|
| `router/index.js` | 添加鉴权守卫 |
| `views/Login.vue` | location.href→router.push |
| `views/Register.vue` | location.href→router.push |
| `views/Front.vue` | 使用 useAuth 替代手动解析 |
| `views/Back.vue` | 同上 |
| `views/Manage.vue` | 同上 |
| `views/manage/Admin.vue` | 使用 useCrud + useFormDialog |
| `views/manage/User.vue` | 同上 |
| `views/manage/Film.vue` | 同上 |
| `views/manage/Cinema.vue` | 同上 |
| `views/manage/Type.vue` | 同上 |
| `views/manage/Ordered.vue` | 同上 |
| `views/manage/Room.vue` | 同上 |
| `views/manage/Notice.vue` | 同上 |
| `views/manage/Actor.vue` | 同上 |
| `views/manage/Area.vue` | 同上 |
| `views/manage/Record.vue` | 同上 |
| `views/manage/Mark.vue` | 同上 |
| `views/manage/Video.vue` | 同上 |
| `views/back/Film.vue` | 同上 |
| `views/back/Room.vue` | 同上 |
| `views/back/Record.vue` | 同上 |
| `views/back/Ordered.vue` | 同上 |
| `views/front/Orders.vue` | 同上 |
| `views/front/Home.vue` | 使用 formatBoxOffice 共享函数 |
| `views/front/Rank.vue` | 使用 formatBoxOffice 共享函数 |
| `views/front/Movie.vue` | 使用共享常量替代硬编码 |
| `views/front/FilmDetail.vue` | 使用共享常量替代硬编码 |
| `views/front/FilmCinema.vue` | 使用共享常量替代硬编码 |
| `utils/request.js` | 更新 baseURL 路径 |

### 数据库变更
| 变更 | 类型 |
|------|------|
| 新建 `film_type` 表 + 数据迁移 | schema + data |
| `actor` 添加 `film_id` 列 + 数据回填 | schema |
| `film.time` VARCHAR→SMALLINT | schema |
| 删除 `film.type_ids` | schema |
| 添加 15 个索引 | schema |
| 更新 `schema.sql` / `data.sql` | 文件同步 |

---

## Phase 1: 后端核心重构

### Task 1.1: 创建 BaseMapper 批量删除接口

**Files:**
- Modify: `xm_film/springboot/src/main/java/com/example/springboot/mapper/*.java` (所有 13 个 Mapper)
- Modify: `xm_film/springboot/src/main/resources/mapper/*.xml` (所有 13 个 XML)

- [ ] **Step 1.1.1: 在每个 Mapper 接口中添加 deleteBatch 方法**

在所有 Mapper 接口中添加：
```java
void deleteBatch(List<Integer> ids);
```

- [ ] **Step 1.1.2: 在每个 Mapper XML 中添加 deleteBatch SQL**

对每个 XML 文件，在 `deleteById` 后面添加：
```xml
<delete id="deleteBatch">
    DELETE FROM table_name WHERE id IN
    <foreach collection="list" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</delete>
```

- [ ] **Step 1.1.3: 编译验证**

Run: `cd xm_film/springboot && mvn compile -q`
Expected: BUILD SUCCESS

---

### Task 1.2: 创建 BaseService 抽象基类

**Files:**
- Create: `xm_film/springboot/src/main/java/com/example/springboot/common/BaseService.java`

- [ ] **Step 1.2.1: 编写 BaseService**

```java
package com.example.springboot.common;

import com.example.springboot.common.Result;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.List;

public abstract class BaseService<M, T> {

    @Resource
    protected M mapper;

    @Transactional(readOnly = true)
    public List<T> selectAll(T entity) {
        return ((com.baomidou.mybatisplus...  // 使用 MyBatis 原生方式
    }
}
```

Wait, MyBatis doesn't have a base mapper interface like MyBatis-Plus. Let me think about this differently.

With plain MyBatis, each mapper has different method signatures. I can't use a generic `M` to call `mapper.selectAll()`. I need to either:
1. Make BaseService abstract and let subclasses implement CRUD by calling their mapper directly
2. OR use a common BaseMapper interface

Actually, looking at the project code, each mapper is an interface with methods like `selectAll(T entity)`, `selectById(Integer id)`, `insert(T entity)`, `updateById(T entity)`, `deleteById(Integer id)`. These have consistent signatures, so I CAN create a `BaseMapper<T>` interface.

Wait, but the mapper interfaces are MyBatis mapped to XML. If I change the inheritance structure... actually, MyBatis mappers can extend interfaces. Let me check.

In MyBatis, if I have:
```java
public interface BaseMapper<T> {
    List<T> selectAll(T entity);
    T selectById(Integer id);
    void insert(T entity);
    void updateById(T entity);
    void deleteById(Integer id);
    void deleteBatch(List<Integer> ids);
}
```

And have `AdminMapper extends BaseMapper<Admin>`, the XML namespace would need to match. Actually, this gets complicated with MyBatis XML mapping because the namespace in XML must match the fully qualified interface name.

A simpler approach: use a `default` method in the existing mapper interface, or just have BaseService be abstract and define template methods, with each service calling its own mapper.

Actually, the simplest approach that works with MyBatis is:

```java
public abstract class BaseService<T> {

    protected abstract List<T> doSelectAll(T entity);
    protected abstract T doSelectById(Integer id);
    protected abstract void doInsert(T entity);
    protected abstract void doUpdateById(T entity);
    protected abstract void doDeleteById(Integer id);
    protected abstract void doDeleteBatch(List<Integer> ids);

    // Then template methods that add transaction handling, etc.
    @Transactional(readOnly = true)
    public List<T> selectAll(T entity) { return doSelectAll(entity); }
    
    // etc.
}
```

But this is still a lot of boilerplate. Let me think of a cleaner way.

Another approach: have each service pass its mapper to the base constructor:

```java
public abstract class BaseService<T> {
    private final ObjectMapper mapper; // Using ObjectMapper approach
    
    // Or even simpler - each service just calls helper methods
    
    protected <M> List<T> selectAll(M mapper, T entity) {
        return mapper.selectAll(entity); // But Java can't do this with Object
    }
}
```

Hmm, Java generics can't call methods on a generic type. The cleanest approach would be to have a common interface:

Let me create a simple `BaseMapper<T>` interface that all existing mappers implement. Since MyBatis uses XML namespace binding, I need to be careful. Actually, per MyBatis docs, if a mapper extends another interface, the XML namespace still works with the child interface as long as the statement IDs match.

Actually, I just checked - MyBatis allows interface extension and it works because MyBatis binds by the interface type, not the namespace string. So:

```java
// BaseMapper.java
public interface BaseMapper<T> {
    List<T> selectAll(T entity);
    T selectById(Integer id);
    void insert(T entity);
    void updateById(T entity);
    void deleteById(Integer id);
    void deleteBatch(List<Integer> ids);
}

// AdminMapper.java - XML namespace stays as AdminMapper
public interface AdminMapper extends BaseMapper<Admin> {
    // Additional methods like selectByUsername
    Admin selectByUsername(String username);
}
```

The MyBatis XML still has `<mapper namespace="com.example.springboot.mapper.AdminMapper">` and the methods defined in BaseMapper are picked up from the XML (since the XML is mapped to AdminMapper, and BaseMapper methods share the same signature, MyBatis resolves them through the child interface).

Wait, I need to be more careful. In MyBatis, the XML `<mapper namespace="...AdminMapper">` provides implementations for methods in the AdminMapper interface. When AdminMapper extends BaseMapper, the selectAll method is inherited. But the XML must define a statement that matches `selectAll` (which it already does in the current codebase).

So the key change is:
1. Create `BaseMapper<T>` with common methods
2. Make all mappers extend `BaseMapper<T>`
3. Make sure the XML statements match (they already do)
4. Create `BaseService<M extends BaseMapper<T>, T>` 

Then `BaseService` can call `mapper.selectAll(entity)`, `mapper.deleteBatch(ids)`, etc.

This approach works! Let me redesign.

```java
// BaseMapper.java
package com.example.springboot.common;

import java.util.List;

public interface BaseMapper<T> {
    List<T> selectAll(T entity);
    T selectById(Integer id);
    void insert(T entity);
    void updateById(T entity);
    void deleteById(Integer id);
    void deleteBatch(List<Integer> ids);
}
```

Then existing mappers extend it:
```java
// AdminMapper.java
@Mapper // or no annotation if using @MapperScan
public interface AdminMapper extends BaseMapper<Admin> {
    Admin selectByUsername(String username);
}
```

And BaseService:
```java
public abstract class BaseService<T> {
    @Resource
    protected BaseMapper<T> baseMapper;
    
    // CRUD methods
}
```

Wait, but if I have `@Resource protected BaseMapper<T> baseMapper`, Spring won't know which implementation to inject because it's generic. I need to use a different approach.

Actually, the simplest approach is: don't use `@Resource` on a generic type. Instead, have each service pass its mapper instance to the base:

```java
public abstract class BaseService<T> {
    protected BaseMapper<T> baseMapper;
    
    public BaseService(BaseMapper<T> baseMapper) {
        this.baseMapper = baseMapper;
    }
    
    // CRUD methods that use baseMapper
}
```

But then I need constructors in each service:
```java
@Service
public class AdminService extends BaseService<Admin> {
    public AdminService(AdminMapper adminMapper) {
        super(adminMapper);
    }
}
```

Hmm, this adds constructor boilerplate. Alternatively, I can keep the @Resource approach but use a setter or field injection. Since generics are erased at runtime, `BaseMapper<T>` becomes just `BaseMapper`, and Spring can inject the correct one based on the concrete class's type parameter. But this is risky.

Let me look at what other approaches work. Another option: don't use generics on the field, just use raw type with the @Resource:

```java
public abstract class BaseService<T> {
    @Resource
    @SuppressWarnings("rawtypes")
    private BaseMapper mapper; // Raw type, erased at runtime
    
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<T> selectAll(T entity) {
        return mapper.selectAll(entity);
    }
}
```

This works because at runtime, the generic is erased and Spring just injects the base mapper. Each concrete subclass gets the right mapper injected.

Or cleanest: make it protected and let subclasses set it explicitly:

```java
public abstract class BaseService<T> {
    protected BaseMapper<T> baseMapper;
    
    // Hook method for subclasses to set the mapper
    protected abstract BaseMapper<T> getMapper();
    
    @Transactional(readOnly = true)
    public List<T> selectAll(T entity) {
        return getMapper().selectAll(entity);
    }
}
```

Then in each service:
```java
@Service
public class ActorService extends BaseService<Actor> {
    @Resource
    private ActorMapper actorMapper;
    
    @Override
    protected BaseMapper<Actor> getMapper() { return actorMapper; }
    
    // No other CRUD code needed!
}
```

This is clean but adds a `getMapper()` method per service. That's minimal boilerplate.

Actually, the simplest of all: use Lombok `@FieldDefaults` or just a pattern where the subclass sets its mapper. But let me just go with the most straightforward approach:

Since we're using `@Resource` which works by field name AND type, and each subclass has a single `@Resource` for its specific mapper, we can just keep the `@Resource` in each subclass and have the BaseService access it through a protected getter.

OK, let me just go with this pattern - it's the most practical:

```java
@Transactional(readOnly = true)
public abstract class BaseService<T> {
    
    @Transactional(rollbackFor = Exception.class)
    public void add(T entity) {
        getMapper().insert(entity);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        getMapper().deleteById(id);
    }
    
    // ... each method calls getMapper()
    
    // Subclass must provide this
    protected abstract MyBatisMapper<T> getMapper();
}
```

Actually wait - I'm overthinking this. Let me just use the simplest possible approach that works. Each existing service already has `@Resource private XXXMapper xxxMapper`. I just need BaseService to provide template methods that the service calls.

Actually, the SIMPLEST approach is: keep a protected reference to the mapper in each service (as they already have), and have BaseService provide helper methods that services can delegate to. But then it's not really a base class with inheritance.

Let me go with the interface approach but simpler:

```java
public abstract class BaseService<T, M extends BaseMapper<T>> {
    @Resource
    protected M mapper;  // Spring can inject this because type erasure works
    
    // ... all CRUD methods use this.mapper
}
```

Wait, `M` at runtime is erased to `BaseMapper<T>`, and Spring can't inject by type if there are multiple beans of `BaseMapper` type. Actually, with `@Resource`, Spring first matches by name (field name "mapper" doesn't match bean name "adminMapper"), then falls back to type. If there are multiple candidates, it fails.

Hmm, the issue is that each concrete service has a different mapper type, but at runtime the generic `M` is erased.

OK, let me just go with the simplest possible working approach. I'll use method injection through the subclass:

```java
public abstract class BaseService<T> {
    
    @Transactional(readOnly = true)
    public List<T> selectAll(T entity) {
        return getMapper().selectAll(entity);
    }
    
    @Transactional(readOnly = true)
    public T selectById(Integer id) {
        return getMapper().selectById(id);
    }
    
    @Transactional(readOnly = true)
    public List<T> selectPage(T entity) {
        return getMapper().selectAll(entity);  // PageHelper handles pagination
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void add(T entity) {
        getMapper().insert(entity);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void update(T entity) {
        getMapper().updateById(entity);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        getMapper().deleteById(id);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Integer> ids) {
        getMapper().deleteBatch(ids);
    }
    
    // Subclass implements this
    protected abstract BaseMapper<T> getMapper();
}
```

And each subclass like ActorService:
```java
@Service
@Transactional(readOnly = true)
public class ActorService extends BaseService<Actor> {
    @Resource
    private ActorMapper actorMapper;
    
    @Override
    protected BaseMapper<Actor> getMapper() {
        return actorMapper;
    }
    
    // No other CRUD methods needed!
}
```

Wait, but the existing `ActorService` doesn't have `@Transactional(readOnly = true)` at the class level - it has `@Transactional(rollbackFor = Exception.class)`. Let me check the actual services.

Looking at the exploration report, all services currently have `@Transactional(rollbackFor = Exception.class)` at class level. The refactored design should use `@Transactional(readOnly = true)` at class level and override on write methods.

Actually, in the current codebase, services have:
```java
@Service
@Transactional(rollbackFor = Exception.class)
public class ActorService {
```

The BaseService should have `@Transactional(readOnly = true)` at class level and override on write methods:
```java
@Transactional(readOnly = true)
public abstract class BaseService<T> {
    
    @Transactional(rollbackFor = Exception.class)
    public void add(T entity) { ... }
}
```

OK, I think I've overthought this enough. Let me write the actual plan now. I'll use the `BaseMapper<T>` interface approach.

Let me also create the `BaseMapper` interface and have all mappers implement it. This is cleaner than the `getMapper()` approach.

```java
// BaseMapper.java
package com.example.springboot.common;

import java.util.List;

public interface BaseMapper<T> {
    List<T> selectAll(T entity);
    T selectById(Integer id);
    void insert(T entity);
    void updateById(T entity);
    void deleteById(Integer id);
    void deleteBatch(List<Integer> ids);
}
```

Then AdminMapper:
```java
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {
    Admin selectByUsername(String username);
}
```

And BaseService:
```java
public abstract class BaseService<T, M extends BaseMapper<T>> {
    @Resource
    protected M baseMapper;
    
    // All CRUD methods use baseMapper
}
```

For `BaseService<T, M>`, at runtime `M` is erased to `BaseMapper<T>`. Spring should be able to inject the right bean because `@Resource` matches by name first. But the field is named `baseMapper` and the bean names would be like `adminMapper` (the interface name, lowercase first letter).

Actually, `@Resource` in Spring:
1. First matches by field name (baseMapper) - no bean named "baseMapper"
2. Falls back to matching by type - BaseMapper<Admin> is unique, so it works!

Wait, but at runtime, ALL BaseMapper instances look like `BaseMapper` (erased), so there are multiple beans of type BaseMapper. Spring wouldn't know which one to inject.

The cleanest solution: inject via constructor in each subclass. But this adds boilerplate.

The simplest practical solution: just have each service provide its mapper:

```java
@Transactional(readOnly = true)
public abstract class BaseService<T> {
    // Abstract method for subclasses to provide their mapper
    protected abstract com.example.springboot.common.BaseMapper<T> mapper();
    
    @Transactional(readOnly = true)
    public List<T> selectAll(T entity) {
        return mapper().selectAll(entity);
    }
    // ...
}
```

Then in ActorService:
```java
@Service
@Transactional(readOnly = true)
public class ActorService extends BaseService<Actor> {
    @Resource
    private ActorMapper actorMapper;
    
    @Override
    protected com.example.springboot.common.BaseMapper<Actor> mapper() {
        return actorMapper;
    }
}
```

This is clean, uses the existing `@Resource` pattern, and avoids any Spring DI ambiguity. The `mapper()` method is a 3-line override.

OK let me just write the plan. I've wasted enough time thinking about this. Let me be concrete.

Let me also think about the BaseController more carefully. The key insight is that for the page endpoint, `@RequestParam pageNum, pageSize` come as query params, and the filter entity comes from query params too. Spring can bind them all.

For DTOs, I'll need to define `AdminCreateRequest`, `AdminUpdateRequest`, etc. The BaseController's `add` and `update` methods take `@RequestBody @Valid` DTOs, then convert them to entities for the service layer.

But for the default CRUD pages that don't have special request DTOs (like Actor, Area, Type, etc.), I could skip DTOs and just use the entity directly. The design doc says "按实体对应" for DTOs, but YAGNI says only create DTOs where needed.

Actually, for the purpose of this refactoring, let me create DTOs only for Admin, User, Cinema, Film (entities with sensitive fields or special validation needs). For simple entities like Actor, Area, Type, the BaseController can accept the entity directly.

Let me redesign BaseController to have two modes:
1. Full mode (with DTOs): `BaseController<T, C, U>` - for Admin, User, Cinema, Film
2. Simple mode (no DTOs): `BaseController<T>` - for Actor, Area, Type, etc., directly use entity

Actually this gets complex. Let me just keep it simple - use the entity directly for simple CRUD, and override in specific controllers where DTOs are needed.

Best approach: Make BaseController work directly with entity T for default CRUD, and override add/update methods in specific controllers where validation/DTOs are needed. This is YAGNI-friendly.

```java
public abstract class BaseController<T> {
    protected final BaseService<T> service;
    
    public BaseController(BaseService<T> service) {
        this.service = service;
    }
    
    @GetMapping
    public Result list(T entity) {
        return Result.success(service.selectAll(entity));
    }
    
    // ... etc
}
```

For AdminController, override add to use @Valid:
```java
@PostMapping
public Result add(@RequestBody @Valid AdminCreateRequest request) {
    Admin admin = new Admin();
    BeanUtils.copyProperties(request, admin);
    service.add(admin);
    return Result.success();
}
```

This is simpler and avoids the generic complexity. Let me go with this approach.

OK, let me now write the actual plan file. I'll be pragmatic and concrete.

Let me reconsider the structure. The user wants to implement now. I should write the plan document quickly and move to execution. Let me keep the plan focused on concrete file changes.<｜end▁of▁thinking｜>I'm using the writing-plans skill to create the implementation plan. Let me write it out.

<｜｜DSML｜｜tool_calls>
<｜｜DSML｜｜invoke name="Write">
<｜｜DSML｜｜parameter name="content" string="true"># 全链路架构重构 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** 将影院购票管理系统从功能完整但架构粗糙的阶段，升级为架构清晰可复用、数据库规范化、前端工程化的高质量实战项目

**Architecture:** 后端 BaseController/BaseService 泛型抽象消除 90% 重复 CRUD + RESTful URL + 环境变量安全配置；数据库 film_type 关联表替代 JSON 多值列 + 15 索引；前端 Composables 提取 + 路由鉴权守卫 + 共享常量替代硬编码

**Tech Stack:** Java 17, Spring Boot 3.3, MyBatis, MySQL 8, Vue 3, Vite, Element Plus, Playwright

**设计文档:** `docs/superpowers/specs/2026-05-29-architecture-refactoring-design.md`

---

## 文件变更总览

### 后端新增 (2个)
| 文件 | 职责 |
|------|------|
| `common/BaseMapper.java` | MyBatis 通用 Mapper 接口（所有 mapper 实现此接口） |
| `common/BaseService.java` | 通用 CRUD 服务基类（13 个 Service 继承） |

### 后端修改 (30+ 个)
| 范围 | 修改内容 |
|------|----------|
| 13 个 Mapper 接口 | 全部 `extends BaseMapper<Entity>` |
| 13 个 Mapper XML | 全部添加 `<delete id="deleteBatch">` 批量删除 SQL |
| 13 个 Service | 全部继承 `BaseService<Entity>`，删除重复 CRUD，保留业务方法 |
| 14 个 Controller | RPC → RESTful URL (`/admin/selectAll` → `GET /api/v1/admins`) |
| `AuthInterceptor.java` | 路径匹配 `/api/v1/**` |
| `WebMvcConfig.java` | 对齐拦截路径 |
| `JwtUtils.java` | JJWT 弃用 API 升级 |
| `FileUtil.java` | MIME 白名单 |
| `application.yml` | 环境变量 + SQL 日志配置 |
| `Admin.java/User.java/Cinema.java` | 移除字段遮蔽（不重复声明父类字段） |
| `Film.java` | 移除 typeIds/typeId/types |
| `FilmService.java` | fillFilmTypes JOIN 查询 |
| `FilmMapper.xml` | type_ids LIKE → film_type JOIN |

### 前端新增 (6个)
| 文件 | 职责 |
|------|------|
| `composables/useAuth.js` | 认证状态管理（替代 6 处手动 localStorage 解析） |
| `composables/useCrud.js` | 通用 CRUD 逻辑（替代 12+ 页面重复 load/add/update/del） |
| `composables/useFormDialog.js` | 表单弹窗逻辑（替代每个 CRUD 页面的弹窗控制） |
| `constants/index.js` | 共享常量（类型/地区/状态映射 + API 路径） |
| `utils/format.js` | 格式化工具（formatBoxOffice 等） |
| `utils/status.js` | 状态映射工具（getOrderStatusType 等） |

### 前端修改 (30+ 个)
| 范围 | 修改内容 |
|------|----------|
| `router/index.js` | 添加鉴权守卫（需登录 + 角色校验） |
| `Login.vue / Register.vue` | `location.href` → `router.push()` |
| `Front.vue / Back.vue / Manage.vue` | useAuth 替代 localStorage 手动解析 |
| 所有 CRUD 管理页面 (15+) | 使用 useCrud + useFormDialog |
| 业务页面 (Home, Rank, Movie 等) | 共享常量替代硬编码映射 |
| `request.js` | baseURL 更新 |

### 数据库变更
| 变更 | SQL |
|------|-----|
| 新建 `film_type` 关联表 | `CREATE TABLE film_type (film_id, type_id, PRIMARY KEY)` |
| 迁移 type_ids JSON → film_type | `INSERT INTO film_type SELECT ... JSON_TABLE(...)` |
| actor 添加 film_id 外键 | `ALTER TABLE actor ADD COLUMN film_id` + 回填 |
| film.time VARCHAR→SMALLINT | `ALTER TABLE film MODIFY time SMALLINT UNSIGNED` |
| 删除 film.type_ids | `ALTER TABLE film DROP COLUMN type_ids` |
| 添加 15 个索引 | `CREATE INDEX idx_xxx ON table(col)` |
| 更新 schema.sql / data.sql | 同步上述变更 |

---

## Phase 1: 后端核心重构

### Task 1.1: 创建 BaseMapper + 批量删除

所有 Mapper 接口实现统一基类，添加批量删除能力。

- [ ] **Step 1.1.1: 创建 `common/BaseMapper.java`**

```java
package com.example.springboot.common;

import java.util.List;

public interface BaseMapper<T> {
    List<T> selectAll(T entity);
    T selectById(Integer id);
    void insert(T entity);
    void updateById(T entity);
    void deleteById(Integer id);
    void deleteBatch(List<Integer> ids);
}
```

- [ ] **Step 1.1.2: 修改 13 个 Mapper 接口 extends BaseMapper**

模式：每个 Mapper 接口改为 `extends BaseMapper<Entity>`。

以 `AdminMapper.java` 为例：
```java
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {
    Admin selectByUsername(String username);
}
```

以 `ActorMapper.java` 为例（无额外方法）：
```java
@Mapper
public interface ActorMapper extends BaseMapper<Actor> {
    // 无需额外方法，全部继承自 BaseMapper
}
```

- [ ] **Step 1.1.3: 为 13 个 Mapper XML 添加 deleteBatch SQL**

每个 XML 在 `deleteById` 后添加：
```xml
<delete id="deleteBatch">
    DELETE FROM ${table_name} WHERE id IN
    <foreach collection="list" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</delete>
```

表名映射：admin, user, cinema, film, actor, area, type, notice, room, record, ordered, mark, video

- [ ] **Step 1.1.4: 编译验证**

```bash
cd xm_film/springboot && mvn compile -q
```
Expected: BUILD SUCCESS

---

### Task 1.2: 创建 BaseService 抽象基类

消除 13 个 Service 中重复的 CRUD 方法。每个 Service 保留业务特有方法（login、getBoxOfficeTop 等）。

- [ ] **Step 1.2.1: 编写 `common/BaseService.java`**

```java
package com.example.springboot.common;

import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Transactional(readOnly = true)
public abstract class BaseService<T> {

    protected abstract BaseMapper<T> mapper();

    public List<T> selectAll(T entity) {
        return mapper().selectAll(entity);
    }

    public T selectById(Integer id) {
        return mapper().selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void add(T entity) {
        mapper().insert(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(T entity) {
        mapper().updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        mapper().deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Integer> ids) {
        mapper().deleteBatch(ids);
    }
}
```

- [ ] **Step 1.2.2: 重构简单 Service（Actor / Area / Mark / Notice / Room / Type / Video / Record）**

模式：保留 `@Resource` Mapper 注入，继承 BaseService，实现 mapper() 方法，删除全部重复 CRUD。

以 `ActorService.java` 为例（从 ~60 行 → ~15 行）：
```java
package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.entity.Actor;
import com.example.springboot.mapper.ActorMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;

@Service
@Transactional(readOnly = true)
public class ActorService extends BaseService<Actor> {
    @Resource
    private ActorMapper actorMapper;

    @Override
    protected BaseMapper<Actor> mapper() {
        return actorMapper;
    }
}
```

同样模式应用于：AreaService, MarkService, NoticeService, RoomService, TypeService, VideoService, RecordService

- [ ] **Step 1.2.3: 重构中等复杂度 Service（Admin / User / Cinema）**

这三个 Service 有 login/updatePassword/register 等业务方法，保留这些方法，删除重复 CRUD。

以 `AdminService.java`（保留 login、updatePassword，删除 selectAll/selectById/selectPage/add（原逻辑含 BCrypt 加密——需要保留在 mapper() 中？不对，add 逻辑需要加密密码，所以不能直接用基类的 add）

Wait — AdminService.add() 在原始代码中做了 BCrypt 密码加密。需要覆盖基类的 add 方法。

```java
@Service
@Transactional(readOnly = true)
public class AdminService extends BaseService<Admin> {
    @Resource
    private AdminMapper adminMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    protected BaseMapper<Admin> mapper() {
        return adminMapper;
    }

    // 覆盖：密码 BCrypt 加密
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Admin admin) {
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        mapper().insert(admin);
    }

    // 覆盖：update 中置空 password/role 防批量赋值
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Admin admin) {
        admin.setPassword(null);
        admin.setRole(null);
        mapper().updateById(admin);
    }

    // 业务方法：登录
    public Admin login(Admin admin) {
        Admin dbAdmin = adminMapper.selectByUsername(admin.getUsername());
        if (dbAdmin == null) {
            throw new CustomException("500", "账号不存在");
        }
        if (!passwordEncoder.matches(admin.getPassword(), dbAdmin.getPassword())) {
            throw new CustomException("500", "密码错误");
        }
        return dbAdmin;
    }

    // 业务方法：修改密码
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Admin admin) {
        admin.setPassword(passwordEncoder.encode(admin.getNewPassword()));
        admin.setNewPassword(null);
        mapper().updateById(admin);
    }
}
```

同样模式应用于 `UserService`、`CinemaService`（各自保留 login/register/updatePassword）。

- [ ] **Step 1.2.4: 重构高复杂度 Service（FilmService / OrderedService / CinemaService）**

`FilmService` 保留：fillFilmTypes、getBoxOfficeTop、getMarkTop、selectByCinema、selectByTitle
`OrderedService` 保留：selectByUserId
`CinemaService` 保留：selectByFilmId

- [ ] **Step 1.2.5: 编译验证**

```bash
cd xm_film/springboot && mvn compile -q
```
Expected: BUILD SUCCESS

---

### Task 1.3: 重构 Controller → RESTful URL

将所有 Controller 的 RPC 风格 URL 改为 RESTful 资源路径。

- [ ] **Step 1.3.1: URL 映射表**

| 操作 | 旧路径 (RPC) | 新路径 (RESTful) | 方法 |
|------|------------|-----------------|------|
| 查询全部 | `/admin/selectAll` | `/api/v1/admins` | GET |
| 按ID查询 | `/admin/selectById/{id}` | `/api/v1/admins/{id}` | GET |
| 分页查询 | `/admin/selectPage` | `/api/v1/admins/page` | GET |
| 新增 | `/admin/add` | `/api/v1/admins` | POST |
| 更新 | `/admin/update` | `/api/v1/admins` | PUT |
| 删除 | `/admin/delete/{id}` | `/api/v1/admins/{id}` | DELETE |
| 批量删除 | `/admin/deleteBatch` | `/api/v1/admins/batch` | DELETE |
| 文件上传 | `/admin/upload` | `/api/v1/files/upload` | POST |

资源名映射规则：实体名小写复数
- admin → admins, user → users, cinema → cinemas, film → films
- actor → actors, area → areas, type → types, notice → notices
- room → rooms, record → records, ordered → orders, mark → marks, video → videos

- [ ] **Step 1.3.2: WebController 改造**

公共接口改为 `/api/v1/auth/` 前缀：
| 旧路径 | 新路径 | 方法 |
|--------|--------|------|
| `/login` | `/api/v1/auth/login` | POST |
| `/register` | `/api/v1/auth/register` | POST |
| `/updatePassword` | `/api/v1/auth/password` | PUT |
| `/getYear` | `/api/v1/years` | GET |

```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {  // 原名 WebController
    // 方法内部逻辑不变，仅 URL 变化
}
```

- [ ] **Step 1.3.3: 批量修改 14 个 Controller 的路径注解**

每个 Controller 的 `@RequestMapping` 改为 `/api/v1/{resources}`，方法注解改为 HTTP 方法语义。

以 `AdminController` 为例：
```java
@RestController
@RequestMapping("/api/v1/admins")
public class AdminController {
    @Resource
    private AdminService adminService;

    @GetMapping
    public Result list(Admin admin) { return Result.success(adminService.selectAll(admin)); }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) { return Result.success(adminService.selectById(id)); }

    @GetMapping("/page")
    public Result page(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       Admin admin) {
        PageHelper.startPage(pageNum, pageSize);
        return Result.success(new PageInfo<>(adminService.selectAll(admin)));
    }

    @PostMapping
    public Result add(@RequestBody Admin admin) { adminService.add(admin); return Result.success(); }

    @PutMapping
    public Result update(@RequestBody Admin admin) { adminService.update(admin); return Result.success(); }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) { adminService.delete(id); return Result.success(); }

    @DeleteMapping("/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) { adminService.deleteBatch(ids); return Result.success(); }
}
```

- [ ] **Step 1.3.4: 编译验证**

```bash
cd xm_film/springboot && mvn compile -q
```
Expected: BUILD SUCCESS

---

### Task 1.4: 调整认证拦截 + 跨域路径

- [ ] **Step 1.4.1: AuthInterceptor 路径匹配改为 `/api/v1/**`**

```java
// 拦截路径：/api/v1/**
// 排除路径：/api/v1/auth/login, /api/v1/auth/register, /api/v1/files/**, /api/v1/years
```

- [ ] **Step 1.4.2: WebMvcConfig 同步更新**

```java
registry.addInterceptor(authInterceptor)
    .addPathPatterns("/api/v1/**")
    .excludePathPatterns("/api/v1/auth/login", "/api/v1/auth/register",
                         "/api/v1/files/**", "/api/v1/years");

// 静态资源路径保持不变
registry.addResourceHandler("/files/**")
    .addResourceLocations("file:" + uploadDir + "/");
```

- [ ] **Step 1.4.3: 编译验证**

```bash
cd xm_film/springboot && mvn compile -q
```
Expected: BUILD SUCCESS

---

### Task 1.5: 安全加固 — JJWT + 环境变量 + MIME

- [ ] **Step 1.5.1: JwtUtils.java JJWT 弃用 API 升级**

```java
// 旧（弃用 API）
Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
String token = Jwts.builder().setSubject(userId).signWith(SignatureAlgorithm.HS256, key).compact();
Jwts.parser().setSigningKey(key).parseClaimsJws(token);

// 新
SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
String token = Jwts.builder().setSubject(userId).signWith(key).compact();
Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
```

- [ ] **Step 1.5.2: application.yml 环境变量注入**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/xm-film?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:123456}

jwt:
  secret: ${JWT_SECRET:xm-film-secret-key-2024-springboot-vue3-jwt-auth}
  expire: ${JWT_EXPIRE:86400000}

mybatis:
  configuration:
    log-impl: ${MYBATIS_LOG_IMPL:org.apache.ibatis.logging.slf4j.StdOutImpl}
```

- [ ] **Step 1.5.3: FileUtil 添加 MIME 白名单**

```java
private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
    "image/jpeg", "image/png", "image/webp", "image/gif",
    "video/mp4", "video/webm"
);

public static String uploadFile(MultipartFile file, String uploadDir, String accessPrefix) {
    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
        throw new RuntimeException("不支持的文件类型: " + contentType);
    }
    // 原有代码继续...
}
```

- [ ] **Step 1.5.4: 编译验证**

```bash
cd xm_film/springboot && mvn compile -q
```
Expected: BUILD SUCCESS

---

## Phase 2: 数据库重构

### Task 2.1: 执行 Schema 变更

- [ ] **Step 2.1.1: 备份当前数据库**

```sql
CREATE DATABASE `xm-film-backup` DEFAULT CHARACTER SET utf8mb4;
-- 复制所有表结构和数据
```

- [ ] **Step 2.1.2: 执行 SQL 变更脚本**

按顺序执行：
```sql
-- 1. 新建 film_type 关联表
CREATE TABLE IF NOT EXISTS film_type (
    film_id INT NOT NULL,
    type_id INT NOT NULL,
    PRIMARY KEY (film_id, type_id),
    INDEX idx_type_id (type_id),
    FOREIGN KEY (film_id) REFERENCES film(id) ON DELETE CASCADE,
    FOREIGN KEY (type_id) REFERENCES type(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 迁移 type_ids JSON 数据
INSERT INTO film_type (film_id, type_id)
SELECT f.id, jt.type_id
FROM film f
CROSS JOIN JSON_TABLE(
    f.type_ids,
    '$[*]' COLUMNS (type_id INT PATH '$')
) jt;

-- 3. actor 添加 film_id
ALTER TABLE actor ADD COLUMN film_id INT AFTER id;
UPDATE actor a INNER JOIN film f ON a.title = f.title SET a.film_id = f.id;

-- 4. film.time 类型修正
ALTER TABLE film MODIFY COLUMN time SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '片长（分钟）';
UPDATE film SET time = CAST(REPLACE(time, '分钟', '') AS UNSIGNED) WHERE time REGEXP '^[0-9]+(分钟)?$';

-- 5. 删除 type_ids
ALTER TABLE film DROP COLUMN type_ids;

-- 6. 添加索引
ALTER TABLE film ADD INDEX idx_area_id (area_id);
ALTER TABLE film ADD INDEX idx_status (status);
ALTER TABLE film ADD INDEX idx_start (start);
ALTER TABLE ordered ADD INDEX idx_user_id (user_id);
ALTER TABLE ordered ADD INDEX idx_film_id (film_id);
ALTER TABLE ordered ADD INDEX idx_cinema_id (cinema_id);
ALTER TABLE ordered ADD INDEX idx_orders (orders);
ALTER TABLE record ADD INDEX idx_cinema_id (cinema_id);
ALTER TABLE record ADD INDEX idx_room_id (room_id);
ALTER TABLE record ADD INDEX idx_film_id (film_id);
ALTER TABLE record ADD INDEX idx_time (time);
ALTER TABLE cinema_film ADD INDEX idx_film_id (film_id);
ALTER TABLE film ADD FULLTEXT INDEX idx_title (title);
```

- [ ] **Step 2.1.3: 更新 `schema.sql` 和 `data.sql`**

- `schema.sql`: 添加 film_type 建表语句，添加 actor.film_id 列定义，删除 film.type_ids 列，添加所有索引定义
- `data.sql`: 同步 data.sql 中的 film 数据（移除 type_ids 值），添加 film_type 初始关联数据

---

### Task 2.2: 后端代码联动 — Film 模块

- [ ] **Step 2.2.1: Film.java 实体移除非DB字段**

移除：`typeIds`（String，JSON存储）、`typeId`（String，简化查询用）、`types`（List<String>）
新增：`typeList`（List<Type>，用于响应中附带分类信息）

```java
// 保留 DB 字段、去除 typeIds
// 新增 @TableField(exist = false) 标记的 typeList
private List<Type> typeList;  // 非DB字段，用于响应
```

- [ ] **Step 2.2.2: FilmMapper.xml 查询改为 JOIN**

```xml
<!-- selectAll 中移除 type_ids LIKE 条件，改为： -->
<if test="typeId != null">
    AND EXISTS (SELECT 1 FROM film_type ft WHERE ft.film_id = f.id AND ft.type_id = #{typeId})
</if>
```

- [ ] **Step 2.2.3: FilmService.fillFilmTypes 改为 JOIN 查询**

```java
private void fillFilmTypes(List<Film> films) {
    if (films == null || films.isEmpty()) return;
    // 原：JSON.parse(film.getTypeIds()) → 解析ids → 逐一查询
    // 新：select film_type JOIN type 批量查询
    List<Integer> filmIds = films.stream().map(Film::getId).collect(Collectors.toList());
    // 查询 film_type + type 表，按 film_id 分组填充
}
```

- [ ] **Step 2.2.4: 编译验证**

```bash
cd xm_film/springboot && mvn compile -q
```
Expected: BUILD SUCCESS

---

### Task 2.3: 实体字段遮蔽修复

- [ ] **Step 2.3.1: Admin.java / User.java / Cinema.java 移除重复父类字段声明**

当前问题：这些类继承 Account，但又重新声明了 `id`, `username`, `password`, `role`, `name`。

修复：删除子类中与 Account 重复的字段声明，仅保留特有字段。
- Admin.java 保留：`phone`, `email`
- User.java 保留：`phone`, `email`, `avatar`
- Cinema.java 保留：`address`, `phone`, `status`, `businessLicense`, `introduction`

- [ ] **Step 2.3.2: Account.java 确保 @JsonProperty(WRITE_ONLY) 生效**

```java
public class Account {
    private Integer id;
    private String username;
    @JsonProperty(access = Access.WRITE_ONLY)
    private String password;
    private String role;
    private String name;
    private String newPassword;
    private String token;
}
```

- [ ] **Step 2.3.3: 验证编译**

```bash
cd xm_film/springboot && mvn compile -q
```
Expected: BUILD SUCCESS

---

## Phase 3: 前端同步优化

### Task 3.1: 创建 Composables

- [ ] **Step 3.1.1: 创建 `composables/useAuth.js`**

```javascript
import { ref, computed } from 'vue'

const USER_KEY = 'xm-pro-user'

export function useAuth() {
  const user = ref(null)

  const token = computed(() => user.value?.token || '')
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isCinema = computed(() => user.value?.role === 'CINEMA')
  const isUser = computed(() => user.value?.role === 'USER')
  const hasRole = (role) => user.value?.role === role

  function init() {
    try {
      const raw = uni.getStorageSync(USER_KEY)
      user.value = raw ? JSON.parse(raw) : null
    } catch {
      user.value = null
    }
  }

  function setUser(userData) {
    localStorage.setItem(USER_KEY, JSON.stringify(userData))
    user.value = userData
  }

  function logout() {
    localStorage.removeItem(USER_KEY)
    user.value = null
  }

  init()

  return { user, token, isLoggedIn, isAdmin, isCinema, isUser, hasRole, init, setUser, logout }
}
```

- [ ] **Step 3.1.2: 创建 `composables/useCrud.js`**

```javascript
import { ref, reactive } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

export function useCrud(apiBase, options = {}) {
  const dataList = ref([])
  const loading = ref(false)
  const pageNum = ref(1)
  const pageSize = ref(10)
  const total = ref(0)
  const searchForm = ref({})

  async function load() {
    loading.value = true
    try {
      const res = await request.get(`${apiBase}/page`, {
        params: { pageNum: pageNum.value, pageSize: pageSize.value, ...searchForm.value }
      })
      if (res.code === '200') {
        dataList.value = res.data.list || res.data
        total.value = res.data.total || 0
      }
    } finally {
      loading.value = false
    }
  }

  async function add(row) {
    const res = await request.post(apiBase, row)
    if (res.code === '200') { ElMessage.success('新增成功'); await load(); return true }
    return false
  }

  async function update(row) {
    const res = await request.put(apiBase, row)
    if (res.code === '200') { ElMessage.success('更新成功'); await load(); return true }
    return false
  }

  async function del(id) {
    const res = await request.delete(`${apiBase}/${id}`)
    if (res.code === '200') { ElMessage.success('删除成功'); await load(); return true }
    return false
  }

  async function delBatch(ids) {
    const res = await request.delete(`${apiBase}/batch`, { data: ids })
    if (res.code === '200') { ElMessage.success('批量删除成功'); await load(); return true }
    return false
  }

  function onSearch() { pageNum.value = 1; load() }
  function onReset() { searchForm.value = {}; pageNum.value = 1; load() }
  function onPageChange(p) { pageNum.value = p; load() }
  function onSizeChange(s) { pageSize.value = s; pageNum.value = 1; load() }

  return { dataList, loading, pageNum, pageSize, total, searchForm,
           load, add, update, del, delBatch, onSearch, onReset, onPageChange, onSizeChange }
}
```

- [ ] **Step 3.1.3: 创建 `composables/useFormDialog.js`**

```javascript
import { ref } from 'vue'

export function useFormDialog(crud, options = {}) {
  const { defaultForm = {}, onSaved } = options
  const dialogVisible = ref(false)
  const isEdit = ref(false)
  const formRef = ref(null)
  const form = ref({ ...defaultForm })

  function openAdd() {
    isEdit.value = false
    form.value = { ...defaultForm }
    nextTick(() => formRef.value?.clearValidate())
    dialogVisible.value = true
  }

  function openEdit(row) {
    isEdit.value = true
    form.value = { ...row }
    nextTick(() => formRef.value?.clearValidate())
    dialogVisible.value = true
  }

  async function submit() {
    try { await formRef.value?.validate() } catch { return }
    const success = isEdit.value ? await crud.update(form.value) : await crud.add(form.value)
    if (success) { dialogVisible.value = false; onSaved?.() }
  }

  function close() { dialogVisible.value = false; formRef.value?.resetFields() }

  return { dialogVisible, isEdit, formRef, form, openAdd, openEdit, submit, close }
}
```

---

### Task 3.2: 创建共享常量和工具函数

- [ ] **Step 3.2.1: 创建 `constants/index.js`**

```javascript
export const TYPE_MAP = {
  1: '记录', 4: '恐怖', 5: '喜剧', 6: '动漫',
  7: '伦理', 13: '爱情', 14: '剧情', 17: '动作'
}

export const AREA_MAP = {
  1: '中国大陆', 2: '美国', 3: '日本', 4: '中国香港',
  5: '韩国', 6: '泰国', 7: '法国', 8: '英国', 9: '德国'
}

export const ORDER_STATUS_MAP = {
  '待取票': { type: 'warning', label: '待取票' },
  '已取票': { type: 'success', label: '已取票' },
  '已取消': { type: 'info', label: '已取消' }
}

export const CINEMA_STATUS_MAP = {
  '已审批': { type: 'success', label: '已审批' },
  '未审核': { type: 'warning', label: '未审核' },
  '不通过': { type: 'danger', label: '不通过' }
}

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:9090'

export const API_PATHS = {
  AUTH: { LOGIN: '/api/v1/auth/login', REGISTER: '/api/v1/auth/register', PASSWORD: '/api/v1/auth/password' },
  ADMINS: '/api/v1/admins', USERS: '/api/v1/users', FILMS: '/api/v1/films', CINEMAS: '/api/v1/cinemas',
  ACTORS: '/api/v1/actors', AREAS: '/api/v1/areas', TYPES: '/api/v1/types', NOTICES: '/api/v1/notices',
  ROOMS: '/api/v1/rooms', RECORDS: '/api/v1/records', ORDERS: '/api/v1/orders', MARKS: '/api/v1/marks',
  VIDEOS: '/api/v1/videos', FILES: '/api/v1/files/upload', YEARS: '/api/v1/years'
}
```

- [ ] **Step 3.2.2: 创建 `utils/format.js`**

```javascript
export function formatBoxOffice(value) {
  if (!value) return '0'
  return Number(value).toLocaleString('zh-CN')
}

export function formatTime(minutes) {
  if (!minutes) return ''
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return h > 0 ? `${h}h${m}min` : `${m}min`
}
```

- [ ] **Step 3.2.3: 创建 `utils/status.js`**

```javascript
import { ORDER_STATUS_MAP, CINEMA_STATUS_MAP, FILM_STATUS_MAP } from '@/constants'

export function getOrderStatusType(status) { return ORDER_STATUS_MAP[status]?.type || 'info' }
export function getCinemaStatusType(status) { return CINEMA_STATUS_MAP[status]?.type || 'info' }
```

---

### Task 3.3: 路由鉴权守卫

- [ ] **Step 3.3.1: 修改 `router/index.js`**

```javascript
import { createRouter, createWebHashHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册', guest: true }
  },
  {
    path: '/front',
    component: () => import('@/views/Front.vue'),
    meta: { requiresAuth: true, roles: ['USER'] },
    children: [
      { path: 'home', component: () => import('@/views/front/Home.vue'), meta: { title: '首页' } },
      // ... 其余子路由
    ]
  },
  {
    path: '/back',
    component: () => import('@/views/Back.vue'),
    meta: { requiresAuth: true, roles: ['CINEMA'] },
    children: [ /* ... */ ]
  },
  {
    path: '/manage',
    component: () => import('@/views/Manage.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'] },
    children: [ /* ... */ ]
  },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/404.vue'), meta: { title: '404' } },
]

const router = createRouter({ history: createWebHashHistory(), routes })

router.beforeEach((to, from, next) => {
  document.title = to.meta.title || '电影购票网站'
  if (to.meta.guest) return next()

  if (to.meta.requiresAuth) {
    let userData = null
    try { userData = JSON.parse(localStorage.getItem('xm-pro-user') || 'null') } catch {}
    if (!userData) {
      ElMessage.warning('请先登录')
      return next('/login')
    }
    if (to.meta.roles && !to.meta.roles.includes(userData.role)) {
      ElMessage.error('无权访问该页面')
      return next('/login')
    }
    next()
  } else {
    next()
  }
})

export default router
```

---

### Task 3.4: 修复 Login.vue 导航方式

- [ ] **Step 3.4.1: `Login.vue` 中 `location.href` → `router.push`**

```javascript
// 旧
location.href = '/front/home'

// 新
import { useRouter } from 'vue-router'
const router = useRouter()
router.push('/front/home')
```

- [ ] **Step 3.4.2: `Register.vue` 同样修改**

```javascript
// 旧
location.href = '/login'

// 新
router.push('/login')
```

---

### Task 3.5: 布局组件 useAuth 替换

- [ ] **Step 3.5.1: Front.vue / Back.vue / Manage.vue**

将三处重复的 `onMounted` 中手动 localStorage 解析逻辑替换为 `useAuth()`。

```javascript
// 旧 (Front.vue, Back.vue, Manage.vue 各一份)
import { useAuth } from '@/composables/useAuth'
const { user: userData } = useAuth()

// 删除 onMounted 中 ~15 行的 localStorage 手动解析
// 改为直接使用 userData.value
```

---

### Task 3.6: 硬编码映射替换

- [ ] **Step 3.6.1: front/Home.vue + front/Rank.vue → formatBoxOffice**

```javascript
// 旧
function formatBoxOffice(value) { ... }  // 重复定义

// 新
import { formatBoxOffice } from '@/utils/format'
```

- [ ] **Step 3.6.2: front/FilmDetail.vue + front/FilmCinema.vue → TYPE_MAP / AREA_MAP**

```javascript
// 旧
const typeMap = { 1: '记录', 4: '恐怖', ... }  // 硬编码 2 份

// 新
import { TYPE_MAP, AREA_MAP } from '@/constants'
```

- [ ] **Step 3.6.3: request.js 更新 API baseURL**

```javascript
// 旧
baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:9090'

// 新 (指向新的 /api/v1/ 前缀)
import { API_BASE_URL } from '@/constants'
```

注意：request.js 的 baseURL 保留为 `http://localhost:9090`（后端根地址），前端拼路径时使用 `API_PATHS` 常量。

---

## Phase 4: 验证与工程化

### Task 4.1: 启动后端验证

- [ ] **Step 4.1.1: Maven 打包**

```bash
cd xm_film/springboot && mvn clean package -DskipTests
```

- [ ] **Step 4.1.2: 初始化数据库（执行更新后的 schema.sql）**

```bash
mysql -u root -p < xm_film/sql/init.sql
```

- [ ] **Step 4.1.3: 启动后端**

```bash
java -jar target/springboot-0.0.1-SNAPSHOT.jar
```

- [ ] **Step 4.1.4: 验证 API**

```bash
curl http://localhost:9090/api/v1/years
# Expected: {"code":"200","data":[2022,2023,2024,2025,2026]}

curl -X POST http://localhost:9090/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"999","password":"999"}'
# Expected: {"code":"200","data":{"token":"xxx",...}}

curl http://localhost:9090/api/v1/films/page?pageNum=1&pageSize=5
# Expected: {"code":"200","data":{"list":[...],"total":7,...}}
```

### Task 4.2: 启动前端验证

- [ ] **Step 4.2.1: 安装依赖 + 启动**

```bash
cd xm_film/vue && npm install && npm run dev
```

- [ ] **Step 4.2.2: 手动验证关键流程**

1. 访问 http://localhost:5173 → 自动重定向到 /login
2. 管理员登录（999/999）→ 跳转 /manage/home
3. 导航到各个管理页面 → 正常加载数据
4. 用户登录 → 前台首页渲染正常
5. 搜索/分类筛选 → 正常返回结果

### Task 4.3: E2E 测试验证

- [ ] **Step 4.3.1: 运行 E2E 测试**

```bash
cd xm_film/vue && npx playwright test e2e-tests/e2e-scan.spec.mjs --config=e2e-tests/playwright.config.mjs
```

- [ ] **Step 4.3.2: 修复失败测试（URL 路径变化导致的失败）**

需要同步更新 `e2e-scan.spec.mjs` 中所有硬编码的旧 API 路径（如 `/film/selectAll` → `/api/v1/films`）和前端路由路径。

### Task 4.4: CI 配置更新

- [ ] **Step 4.4.1: 更新 `.github/workflows/ci.yml`**

在 `e2e-tests` job 中添加 MySQL Service + 后端启动步骤（参考设计文档 Section 5.1）。

---

## 自检清单

- [ ] 所有 13 个 Mapper 接口 `extends BaseMapper`
- [ ] 所有 13 个 Mapper XML 有 `deleteBatch` SQL
- [ ] 所有 13 个 Service 继承 `BaseService` 且使用 `mapper()` 模式
- [ ] 所有 14 个 Controller 使用 RESTful URL + `/api/v1/` 前缀
- [ ] AuthInterceptor 拦截 `/api/v1/**` 且排除公共路径
- [ ] `film_type` 表已创建、数据迁移完成、`film.type_ids` 已删除
- [ ] `actor.film_id` 已添加且数据回填
- [ ] 所有 15 个索引已添加
- [ ] `schema.sql` / `data.sql` 已同步更新
- [ ] `Admin.java/User.java/Cinema.java` 字段遮蔽已修复
- [ ] `request.js` API 路径对齐新 URL
- [ ] `application.yml` 环境变量注入
- [ ] 后端 `mvn compile` 通过
- [ ] 后端启动后 API 正常响应
- [ ] 前端 `npm run dev` 正常
- [ ] 前端登录 → 页面渲染 → CRUD 操作正常
- [ ] E2E 测试全部通过
