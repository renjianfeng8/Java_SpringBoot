package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.entity.Notice;
import com.example.springboot.service.NoticeService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公告管理API控制器
 * 提供公告信息的增删改查及分页查询功能
 */
@RestController
@RequestMapping("/notice")
public class NoticeController {

    @Resource
    private NoticeService noticeService;
    

    /**
     * 查询公告列表（支持条件筛选）
     * @param notice 包含筛选条件的公告对象（如用户名、状态等）
     * @return 返回符合条件的公告列表
     */
    @GetMapping("/selectAll")
    public Result selectAll(Notice notice) {
        List<Notice> list = noticeService.selectAll(notice);
        return Result.success(list);
    }

    /**
     * 根据ID查询单个公告
     * @param id 公告ID
     * @return 返回对应公告信息
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Notice notice = noticeService.selectById(id);
        return Result.success(notice);
    }


    /**
     * 查询公告列表（轻量版，可能不含敏感字段）
     * @param notice 包含筛选条件的公告对象
     * @return 返回符合条件的公告列表（可能为精简信息）
     */
    @GetMapping("/selectList")
    public Result selectList(Notice notice) {
        List<Notice> list = noticeService.selectList(notice);
        return Result.success(list);
    }

    /**
     * 分页查询公告列表
     * @param notice 包含筛选条件的公告对象
     * @param pageNum 页码，默认第1页
     * @param pageSize 每页数量，默认10条
     * @return 返回分页结果（包含总数、当前页数据等信息）
     */
    @GetMapping("/selectPage")
    public Result selectPage(Notice notice,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Notice> pageInfo = noticeService.selectPage(notice, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    /**
     * 添加新公告
     * @param notice 公告信息（需包含必要字段）
     * @return 返回操作结果
     */
    @PostMapping("/add")
    public Result add(@RequestBody Notice notice) {
        noticeService.add(notice);
        return Result.success();
    }

    /**
     * 更新公告信息
     * @param notice 包含更新内容的公告信息（需包含ID）
     * @return 返回操作结果
     */
    @PutMapping("/update")
    public Result update(@RequestBody Notice notice) {
        noticeService.update(notice);
        return Result.success();
    }

    /**
     * 根据ID删除公告
     * @param id 公告ID
     * @return 返回操作结果
     */
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        noticeService.delete(id);
        return Result.success();
    }

    /**
     * 批量删除公告
     * @param ids 包含多个公告ID的列表
     * @return 返回操作结果
     */
    @DeleteMapping("/deleteBatch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        noticeService.deleteBatch(ids);
        return Result.success();
    }

    
}