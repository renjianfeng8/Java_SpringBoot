package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.entity.Ordered;
import com.example.springboot.service.OrderedService;
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
 * 订单管理API控制器
 * 提供订单信息的增删改查及分页查询功能
 */
@RestController
@RequestMapping("/ordered")
public class OrderedController {

    @Resource
    private OrderedService orderedService;

    // 注入文件上传目录配置
    @Value("${file.upload-dir}")
    private String uploadDir;

    // 注入文件访问前缀配置
    @Value("${file.access-prefix}")
    private String accessPrefix;

    private static final Logger log = LoggerFactory.getLogger(OrderedController.class);


    /**
     * 查询订单列表（支持条件筛选）
     * @param ordered 包含筛选条件的订单对象（如用户名、状态等）
     * @return 返回符合条件的订单列表
     */
    @GetMapping("/selectAll")
    public Result selectAll(Ordered ordered) {
        List<Ordered> list = orderedService.selectAll(ordered);
        return Result.success(list);
    }

    /**
     * 根据ID查询单个订单
     * @param id 订单ID
     * @return 返回对应订单信息
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Ordered ordered = orderedService.selectById(id);
        return Result.success(ordered);
    }


    @GetMapping("/selectList")
    public Result selectList(Ordered ordered) {
        List<Ordered> list = orderedService.selectList(ordered);
        return Result.success(list);
    }

    /**
     * 分页查询订单列表
     * @param ordered 包含筛选条件的订单对象
     * @param pageNum 页码，默认第1页
     * @param pageSize 每页数量，默认10条
     * @return 返回分页结果（包含总数、当前页数据等信息）
     */
    @GetMapping("/selectPage")
    public Result selectPage(Ordered ordered,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Ordered> pageInfo = orderedService.selectPage(ordered, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    /**
     * 添加新订单
     * @param ordered 订单信息（需包含必要字段）
     * @return 返回操作结果
     */
    @PostMapping("/add")
    public Result add(@RequestBody Ordered ordered) {
        orderedService.add(ordered);
        return Result.success();
    }

    /**
     * 更新订单信息
     * @param ordered 包含更新内容的订单信息（需包含ID）
     * @return 返回操作结果
     */
    @PutMapping("/update")
    public Result update(@RequestBody Ordered ordered) {
        orderedService.update(ordered);
        return Result.success();
    }

    /**
     * 根据ID删除订单
     * @param id 订单ID
     * @return 返回操作结果
     */
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        orderedService.delete(id);
        return Result.success();
    }

    /**
     * 批量删除订单
     * @param ids 包含多个订单ID的列表
     * @return 返回操作结果
     */
    @DeleteMapping("/deleteBatch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        orderedService.deleteBatch(ids);
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

            String fileName = orderedService.uploadFile(file, uploadDir);

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