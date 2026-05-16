package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.entity.Mark;
import com.example.springboot.service.MarkService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 评价管理API控制器
 * 提供评价信息的增删改查及分页查询功能
 */
@RestController
@RequestMapping("/mark")
public class MarkController {

    @Resource
    private MarkService markService;

    // 注入文件上传目录配置
    @Value("${file.upload-dir}")
    private String uploadDir;

    // 注入文件访问前缀配置
    @Value("${file.access-prefix}")
    private String accessPrefix;

    private static final Logger log = LoggerFactory.getLogger(MarkController.class);
    

    /**
     * 查询评价列表（支持条件筛选）
     * @param mark 包含筛选条件的评价对象（如用户名、状态等）
     * @return 返回符合条件的评价列表
     */
    @GetMapping("/selectAll")
    public Result selectAll(Mark mark) {
        List<Mark> list = markService.selectAll(mark);
        return Result.success(list);
    }

    /**
     * 根据ID查询单个评价
     * @param id 评价ID
     * @return 返回对应评价信息
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Mark mark = markService.selectById(id);
        return Result.success(mark);
    }

    /**
     * 查询评价列表（轻量版，可能不含敏感字段）
     * @param mark 包含筛选条件的评价对象
     * @return 返回符合条件的评价列表（可能为精简信息）
     */
    @GetMapping("/selectList")
    public Result selectList(Mark mark) {
        List<Mark> list = markService.selectList(mark);
        return Result.success(list);
    }

    /**
     * 分页查询评价列表
     * @param mark 包含筛选条件的评价对象
     * @param pageNum 页码，默认第1页
     * @param pageSize 每页数量，默认10条
     * @return 返回分页结果（包含总数、当前页数据等信息）
     */
    @GetMapping("/selectPage")
    public Result selectPage(Mark mark,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Mark> pageInfo = markService.selectPage(mark, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    /**
     * 添加新评价
     * @param mark 评价信息（需包含必要字段）
     * @return 返回操作结果
     */
    @PostMapping("/add")
    public Result add(@RequestBody Mark mark) {
        markService.add(mark);
        return Result.success();
    }

    /**
     * 更新评价信息
     * @param mark 包含更新内容的评价信息（需包含ID）
     * @return 返回操作结果
     */
    @PutMapping("/update")
    public Result update(@RequestBody Mark mark) {
        markService.update(mark);
        return Result.success();
    }

    /**
     * 根据ID删除评价
     * @param id 评价ID
     * @return 返回操作结果
     */
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        markService.delete(id);
        return Result.success();
    }

    /**
     * 批量删除评价
     * @param ids 包含多个评价ID的列表
     * @return 返回操作结果
     */
    @DeleteMapping("/deleteBatch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        markService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 文件上传接口
     */
    @PostMapping("/upload")
    public Result uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return Result.error("400", "上传文件不能为空");
            }

            String fileName = markService.uploadFile(file, uploadDir);

            String fileUrl = accessPrefix + fileName;

            // 打印调试信息
            log.info("生成的文件URL: {}", fileUrl);

            return Result.success(fileUrl);
        } catch (IllegalArgumentException e) {
            return Result.error("400", e.getMessage());
        } catch (IOException e) {
            log.error("File upload failed", e);
            return Result.error("500", "文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("File upload failed", e);
            return Result.error("500", "服务器内部错误");
        }
    }

    
}