package com.example.springboot.common;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public abstract class BaseController<T> {

    protected final BaseService<T> service;

    public BaseController(BaseService<T> service) {
        this.service = service;
    }

    @Operation(summary = "查询全部", description = "查询所有记录，支持按条件筛选")
    @GetMapping
    public Result list(T entity) {
        return Result.success(service.selectAll(entity));
    }

    @Operation(summary = "按ID查询")
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        return Result.success(service.selectById(id));
    }

    @Operation(summary = "分页查询", description = "支持按条件筛选 + 分页")
    @GetMapping("/page")
    public Result page(T entity,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        return Result.success(new PageInfo<>(service.selectAll(entity)));
    }

    @Operation(summary = "新增")
    @PostMapping
    public Result add(@RequestBody T entity) {
        service.add(entity);
        return Result.success();
    }

    @Operation(summary = "更新")
    @PutMapping
    public Result update(@RequestBody T entity) {
        service.update(entity);
        return Result.success();
    }

    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        service.delete(id);
        return Result.success();
    }

    @Operation(summary = "批量删除")
    @DeleteMapping("/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        service.deleteBatch(ids);
        return Result.success();
    }
}
