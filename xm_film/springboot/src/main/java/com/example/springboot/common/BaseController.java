package com.example.springboot.common;

import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 通用 CRUD 控制器骨架。三层泛型隔离领域模型与出入参：
 * {@code T}=实体（Service/DAO 只认它），{@code R}=请求 DTO，{@code V}=响应 DTO。
 * 控制器出入参一律用 R/V，实体不下沉到 HTTP 边界。
 */
public abstract class BaseController<T extends Identifiable, R, V> {

    protected final BaseService<T> service;

    protected BaseController(BaseService<T> service) {
        this.service = service;
    }

    /** 请求 DTO → 实体；Service/DAO 只认实体 */
    protected abstract T toEntity(R request);

    /** 实体 → 响应 DTO；对外只暴露视图字段 */
    protected abstract V toView(T entity);

    @GetMapping
    public Result<List<V>> list(R query) {
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

    @PostMapping
    public Result<Void> add(@RequestBody R request) {
        service.add(toEntity(request));
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody R request) {
        service.update(toEntity(request));
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return Result.success();
    }

    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody List<Integer> ids) {
        service.deleteBatch(ids);
        return Result.success();
    }
}
