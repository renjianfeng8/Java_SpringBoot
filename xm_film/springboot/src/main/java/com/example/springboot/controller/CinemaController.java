package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.entity.Cinema;
import com.example.springboot.service.CinemaService;
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
 * 影院管理API控制器
 * 提供影院信息的增删改查及分页查询功能
 */
@RestController
@RequestMapping("/cinema")
public class CinemaController {

    @Resource
    private CinemaService cinemaService;

    // 注入文件上传目录配置
    @Value("${file.upload-dir}")
    private String uploadDir;

    // 注入文件访问前缀配置
    @Value("${file.access-prefix}")
    private String accessPrefix;

    private static final Logger log = LoggerFactory.getLogger(CinemaController.class);

    /**
     * 查询影院列表（支持条件筛选）
     * @param cinema 包含筛选条件的影院对象（如用户名、状态等）
     * @return 返回符合条件的影院列表
     */
    @GetMapping("/selectAll")
    public Result selectAll(Cinema cinema) {
        List<Cinema> list = cinemaService.selectAll(cinema);
        return Result.success(list);
    }

    /**
     * 根据ID查询单个影院
     * @param id 影院ID
     * @return 返回对应影院信息
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Cinema cinema = cinemaService.selectById(id);
        return Result.success(cinema);
    }

    /**
     * 查询影院列表（轻量版，可能不含敏感字段）
     * @param cinema 包含筛选条件的影院对象
     * @return 返回符合条件的影院列表（可能为精简信息）
     */
    @GetMapping("/selectList")
    public Result selectList(Cinema cinema) {
        List<Cinema> list = cinemaService.selectList(cinema);
        return Result.success(list);
    }

    /**
     * 分页查询影院列表
     * @param cinema 包含筛选条件的影院对象
     * @param pageNum 页码，默认第1页
     * @param pageSize 每页数量，默认10条
     * @return 返回分页结果（包含总数、当前页数据等信息）
     */
    @GetMapping("/selectPage")
    public Result selectPage(
            Cinema cinema,
            @RequestParam(required = false) Integer filmId, // 新增：接收电影ID参数
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Cinema> pageInfo = cinemaService.selectPage(cinema, filmId, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    /**
     * 添加新影院
     * @param cinema 影院信息（需包含必要字段）
     * @return 返回操作结果
     */
    @PostMapping("/add")
    public Result add(@RequestBody Cinema cinema) {
        cinemaService.add(cinema);
        return Result.success();
    }

    /**
     * 更新影院信息
     * @param cinema 包含更新内容的影院信息（需包含ID）
     * @return 返回操作结果
     */
    @PutMapping("/update")
    public Result update(@RequestBody Cinema cinema) {
        cinemaService.update(cinema);
        return Result.success();
    }

    /**
     * 根据ID删除影院
     * @param id 影院ID
     * @return 返回操作结果
     */
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        cinemaService.delete(id);
        return Result.success();
    }

    /**
     * 批量删除影院
     * @param ids 包含多个影院ID的列表
     * @return 返回操作结果
     */
    @DeleteMapping("/deleteBatch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        cinemaService.deleteBatch(ids);
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

            String fileName = cinemaService.uploadFile(file, uploadDir);

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