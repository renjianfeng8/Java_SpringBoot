package com.example.springboot.common;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public abstract class BaseController<T> {

    protected final BaseService<T> service;

    public BaseController(BaseService<T> service) {
        this.service = service;
    }

    @GetMapping
    public Result list(T entity) {
        return Result.success(service.selectAll(entity));
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        return Result.success(service.selectById(id));
    }

    @GetMapping("/page")
    public Result page(T entity,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        return Result.success(new PageInfo<>(service.selectAll(entity)));
    }

    @PostMapping
    public Result add(@RequestBody T entity) {
        service.add(entity);
        return Result.success();
    }

    @PutMapping
    public Result update(@RequestBody T entity) {
        service.update(entity);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        service.delete(id);
        return Result.success();
    }

    @DeleteMapping("/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        service.deleteBatch(ids);
        return Result.success();
    }
}
