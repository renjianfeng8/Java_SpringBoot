package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.entity.Video;
import com.example.springboot.service.VideoService;
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
 * 预告视频管理API控制器
 * 提供预告视频信息的增删改查及分页查询功能
 */
@RestController
@RequestMapping("/video")
public class VideoController {

    @Resource
    private VideoService videoService;

    // 注入文件上传目录配置
    @Value("${file.upload-dir}")
    private String uploadDir;

    // 注入文件访问前缀配置
    @Value("${file.access-prefix}")
    private String accessPrefix;

    private static final Logger log = LoggerFactory.getLogger(VideoController.class);
    

    /**
     * 查询预告视频列表（支持条件筛选）
     * @param video 包含筛选条件的预告视频对象（如用户名、状态等）
     * @return 返回符合条件的预告视频列表
     */
    @GetMapping("/selectAll")
    public Result selectAll(Video video) {
        List<Video> list = videoService.selectAll(video);
        return Result.success(list);
    }

    /**
     * 根据ID查询单个预告视频
     * @param id 预告视频ID
     * @return 返回对应预告视频信息
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Video video = videoService.selectById(id);
        return Result.success(video);
    }

    /**
     * 查询预告视频列表（轻量版，可能不含敏感字段）
     * @param video 包含筛选条件的预告视频对象
     * @return 返回符合条件的预告视频列表（可能为精简信息）
     */
    @GetMapping("/selectList")
    public Result selectList(Video video) {
        List<Video> list = videoService.selectList(video);
        return Result.success(list);
    }

    /**
     * 分页查询预告视频列表
     * @param video 包含筛选条件的预告视频对象
     * @param pageNum 页码，默认第1页
     * @param pageSize 每页数量，默认10条
     * @return 返回分页结果（包含总数、当前页数据等信息）
     */
    @GetMapping("/selectPage")
    public Result selectPage(Video video,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Video> pageInfo = videoService.selectPage(video, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    /**
     * 添加新预告视频
     * @param video 预告视频信息（需包含必要字段）
     * @return 返回操作结果
     */
    @PostMapping("/add")
    public Result add(@RequestBody Video video) {
        videoService.add(video);
        return Result.success();
    }

    /**
     * 更新预告视频信息
     * @param video 包含更新内容的预告视频信息（需包含ID）
     * @return 返回操作结果
     */
    @PutMapping("/update")
    public Result update(@RequestBody Video video) {
        videoService.update(video);
        return Result.success();
    }

    /**
     * 根据ID删除预告视频
     * @param id 预告视频ID
     * @return 返回操作结果
     */
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        videoService.delete(id);
        return Result.success();
    }

    /**
     * 批量删除预告视频
     * @param ids 包含多个预告视频ID的列表
     * @return 返回操作结果
     */
    @DeleteMapping("/deleteBatch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        videoService.deleteBatch(ids);
        return Result.success();
    }


    @PostMapping("/upload")
    public Result uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return Result.error("400", "上传文件不能为空");
            }
            String fileName = videoService.uploadFile(file, uploadDir);
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