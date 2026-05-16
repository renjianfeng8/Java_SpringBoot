package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.entity.Actor;
import com.example.springboot.service.ActorService;
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
 * 演职人员管理API控制器
 * 提供演职人员信息的增删改查及分页查询功能
 */
@RestController
@RequestMapping("/actor")
public class ActorController {

    @Resource
    private ActorService actorService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.access-prefix}")
    private String accessPrefix;

    private static final Logger log = LoggerFactory.getLogger(ActorController.class);



    @GetMapping("/selectAll")
    public Result selectAll(Actor actor) {
        List<Actor> list = actorService.selectAll(actor);
        return Result.success(list);
    }

    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Actor actor = actorService.selectById(id);
        return Result.success(actor);
    }

    @GetMapping("/selectList")
    public Result selectList(Actor actor) {
        List<Actor> list = actorService.selectList(actor);
        return Result.success(list);
    }

    /**
     * 分页查询演职人员列表
     * @param actor 包含筛选条件的演职人员对象
     * @param pageNum 页码，默认第1页
     * @param pageSize 每页数量，默认10条
     * @return 返回分页结果（包含总数、当前页数据等信息）
     */
    @GetMapping("/selectPage")
    public Result selectPage(Actor actor,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Actor> pageInfo = actorService.selectPage(actor, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    @PostMapping("/add")
    public Result add(@RequestBody Actor actor) {
        actorService.add(actor);
        return Result.success();
    }


    @PutMapping("/update")
    public Result update(@RequestBody Actor actor) {
        actorService.update(actor);
        return Result.success();
    }


    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        actorService.delete(id);
        return Result.success();
    }


    @DeleteMapping("/deleteBatch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        actorService.deleteBatch(ids);
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

            String fileName = actorService.uploadFile(file, uploadDir);

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