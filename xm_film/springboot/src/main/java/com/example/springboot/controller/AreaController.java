package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.entity.Area;
import com.example.springboot.service.AreaService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 区域管理API控制器
 * 提供区域信息的增删改查及分页查询功能
 */
@RestController
@RequestMapping("/area")
public class AreaController {

    @Resource
    private AreaService areaService;
    

    /**
     * 查询区域列表（支持条件筛选）
     * @param area 包含筛选条件的区域对象（如用户名、状态等）
     * @return 返回符合条件的区域列表
     */
    @GetMapping("/selectAll")
    public Result selectAll(Area area) {
        List<Area> list = areaService.selectAll(area);
        return Result.success(list);
    }

    /**
     * 根据ID查询单个区域
     * @param id 区域ID
     * @return 返回对应区域信息
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Area area = areaService.selectById(id);
        return Result.success(area);
    }

    /**
     * 查询区域列表（轻量版，可能不含敏感字段）
     * @param area 包含筛选条件的区域对象
     * @return 返回符合条件的区域列表（可能为精简信息）
     */
    @GetMapping("/selectList")
    public Result selectList(Area area) {
        List<Area> list = areaService.selectList(area);
        return Result.success(list);
    }

    /**
     * 分页查询区域列表
     * @param area 包含筛选条件的区域对象
     * @param pageNum 页码，默认第1页
     * @param pageSize 每页数量，默认10条
     * @return 返回分页结果（包含总数、当前页数据等信息）
     */
    @GetMapping("/selectPage")
    public Result selectPage(Area area,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Area> pageInfo = areaService.selectPage(area, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    /**
     * 添加新区域
     * @param area 区域信息（需包含必要字段）
     * @return 返回操作结果
     */
    @PostMapping("/add")
    public Result add(@RequestBody Area area) {
        areaService.add(area);
        return Result.success();
    }

    /**
     * 更新区域信息
     * @param area 包含更新内容的区域信息（需包含ID）
     * @return 返回操作结果
     */
    @PutMapping("/update")
    public Result update(@RequestBody Area area) {
        areaService.update(area);
        return Result.success();
    }

    /**
     * 根据ID删除区域
     * @param id 区域ID
     * @return 返回操作结果
     */
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        areaService.delete(id);
        return Result.success();
    }

    /**
     * 批量删除区域
     * @param ids 包含多个区域ID的列表
     * @return 返回操作结果
     */
    @DeleteMapping("/deleteBatch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        areaService.deleteBatch(ids);
        return Result.success();
    }

    
}