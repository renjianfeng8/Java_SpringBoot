package com.example.springboot.common;

import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.exception.CustomException;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * 通用 CRUD 控制器骨架。三层泛型隔离领域模型与出入参：
 * {@code T}=实体（Service/DAO 只认它），{@code R}=请求 DTO，{@code V}=响应 DTO。
 * 控制器出入参一律用 R/V，实体不下沉到 HTTP 边界。
 */
public abstract class BaseController<T extends Identifiable, R, V> {

    /** 下拉/列表接口的硬上限：需要全量数据走 /page，不允许 GET /list 拖全表。 */
    private static final int MAX_LIST_SIZE = 500;

    protected final BaseService<T> service;

    protected BaseController(BaseService<T> service) {
        this.service = service;
    }

    /** 请求 DTO → 实体；Service/DAO 只认实体 */
    protected abstract T toEntity(R request);

    /** 实体 → 响应 DTO；对外只暴露视图字段 */
    protected abstract V toView(T entity);

    /**
     * 给 /list 加硬上限。子类覆盖 list() 时必须先调一次，否则绕过 BaseController 的默认实现。
     * 第三个参数 false：只要 LIMIT，不要额外的 COUNT(*)；列表接口本来就不返回总数。
     */
    protected void limitList() {
        PageMethod.startPage(1, MAX_LIST_SIZE, false);
    }

    @GetMapping
    public Result<List<V>> list(R query) {
        limitList();
        return Result.success(service.selectAll(toEntity(query)).stream().map(this::toView).toList());
    }

    @GetMapping("/{id}")
    public Result<V> getById(@PathVariable Integer id) {
        T entity = service.selectById(id);
        return Result.success(entity == null ? null : toView(entity));
    }

    @GetMapping("/page")
    public Result<PageInfo<V>> page(R query,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageMethod.startPage(pageNum, pageSize);
        return Result.success(toPageView(new PageInfo<>(service.selectAll(toEntity(query)))));
    }

    /** 分页映射：PageInfo<T> → PageInfo<V>（拷贝分页元数据，list 换成视图）。子类的分页覆写共用此段 */
    protected PageInfo<V> toPageView(PageInfo<T> src) {
        PageInfo<V> dst = new PageInfo<>();
        BeanUtils.copyProperties(src, dst);
        dst.setList(src.getList().stream().map(this::toView).toList());
        return dst;
    }

    /**
     * 新增。请求体必须是对象：多个 mapper 的 insert 用 {@code <trim>} 逐字段拼接，
     * 所有字段为 null 时会生成 {@code INSERT INTO t () VALUES ()}，数据库直接报语法错
     * （实测 {@code POST /films} 传 {@code {}} 即 500）。
     * 在入口拦住比在二十几个 mapper 里各加兜底更可靠，也给出了可读提示。
     */
    @PostMapping
    public Result<Void> add(@RequestBody R request) {
        requireBody(request);
        T entity = toEntity(request);
        requireNonEmptyEntity(entity);
        service.add(entity);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody R request) {
        requireBody(request);
        service.update(toEntity(request));
        return Result.success();
    }

    /** 批量删除的请求体是 id 数组，空数组与 null 都视为"没选中任何行" */
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody List<Integer> ids) {
        service.deleteBatch(ids);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return Result.success();
    }

    /**
     * 请求体非空守卫。Spring 对 {@code @RequestBody} 只校验"有没有"，不校验"是不是空对象"，
     * 而空对象在部分 mapper 上会生成非法 SQL。
     */
    protected void requireBody(R request) {
        if (request == null) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "请求体不能为空");
        }
    }

    /**
     * 实体全字段为空守卫。
     *
     * <p>为什么要有这一层：{@code R} 是个空对象时 {@code toEntity} 会产出一个所有字段
     * 都为 null 的实体，而 mapper 的 insert 用 {@code <trim>} 逐字段拼接，
     * 会生成 {@code INSERT INTO film () VALUES ()} —— 数据库报的是 SQL 语法错，
     * 用户看到「系统错误」。实测 {@code POST /films} 传 {@code {}} 即如此。
     *
     * <p>用反射判断而不是要求每个子类声明必填字段：本仓没有统一的必填元数据，
     * 逐个补 {@code @NotBlank} 需要连带改 BaseController 的 {@code @Valid} 链，
     * 属另一轮重构。这里只挡"一个字都没传"这一种，已覆盖实测暴露的形态。
     */
    protected void requireNonEmptyEntity(T entity) {
        if (entity == null) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "请求体不能为空");
        }
        for (Field field : entity.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }
            ReflectionUtils.makeAccessible(field);
            try {
                if (field.get(entity) != null) {
                    return;
                }
            } catch (IllegalAccessException ignored) {
                // 取不到就当该字段为空继续；这里是防御性代码，不因单个字段失败而误拒
            }
        }
        throw new CustomException(ErrorCode.PARAM_INVALID, "请至少填写一项内容");
    }
}
