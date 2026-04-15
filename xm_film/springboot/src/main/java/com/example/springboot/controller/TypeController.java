package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.entity.Type;
import com.example.springboot.service.TypeService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员管理API控制器
 * 提供管理员信息的增删改查及分页查询功能
 */
@RestController
@RequestMapping("/type")
public class TypeController {

    @Resource
    private TypeService typeService;
    

    /**
     * 查询管理员列表（支持条件筛选）
     * @param type 包含筛选条件的管理员对象（如用户名、状态等）
     * @return 返回符合条件的管理员列表
     */
    @GetMapping("/selectAll")
    public Result selectAll(Type type) {
        List<Type> list = typeService.selectAll(type);
        return Result.success(list);
    }

    /**
     * 根据ID查询单个管理员
     * @param id 管理员ID
     * @return 返回对应管理员信息
     */
    @GetMapping("/selectById/{id}")
    public Result selectByID(@PathVariable Integer id) {
        Type type = typeService.selectById(id);
        return Result.success(type);
    }

    /**
     * 查询管理员列表（轻量版，可能不含敏感字段）
     * @param type 包含筛选条件的管理员对象
     * @return 返回符合条件的管理员列表（可能为精简信息）
     */
    @GetMapping("/selectList")
    public Result selectList(Type type) {
        List<Type> list = typeService.selectList(type);
        return Result.success(list);
    }

    /**
     * 分页查询管理员列表
     * @param type 包含筛选条件的管理员对象
     * @param pageNum 页码，默认第1页
     * @param pageSize 每页数量，默认10条
     * @return 返回分页结果（包含总数、当前页数据等信息）
     */
    @GetMapping("/selectPage")
    public Result selectPage(Type type,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Type> pageInfo = typeService.selectPage(type, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    /**
     * 添加新管理员
     * @param type 管理员信息（需包含必要字段）
     * @return 返回操作结果
     */
    @PostMapping("/add")
    public Result add(@RequestBody Type type) {
        typeService.add(type);
        return Result.success();
    }

    /**
     * 更新管理员信息
     * @param type 包含更新内容的管理员信息（需包含ID）
     * @return 返回操作结果
     */
    @PutMapping("/update")
    public Result update(@RequestBody Type type) {
        typeService.update(type);
        return Result.success();
    }

    /**
     * 根据ID删除管理员
     * @param id 管理员ID
     * @return 返回操作结果
     */
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        typeService.delete(id);
        return Result.success();
    }

    /**
     * 批量删除管理员
     * @param ids 包含多个管理员ID的列表
     * @return 返回操作结果
     */
    @DeleteMapping("/deleteBatch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        typeService.deleteBatch(ids);
        return Result.success();
    }

    
}