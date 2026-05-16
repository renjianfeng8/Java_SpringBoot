package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.entity.Record;
import com.example.springboot.service.RecordService;
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
 * 放映记录管理API控制器
 * 提供放映记录信息的增删改查及分页查询功能
 */
@RestController
@RequestMapping("/record")
public class RecordController {

    @Resource
    private RecordService recordService;

    // 注入文件上传目录配置
    @Value("${file.upload-dir}")
    private String uploadDir;

    // 注入文件访问前缀配置
    @Value("${file.access-prefix}")
    private String accessPrefix;

    private static final Logger log = LoggerFactory.getLogger(RecordController.class);




    @GetMapping("/selectAll")
    public Result selectAll(Record record) {
        List<Record> list = recordService.selectAll(record);
        return Result.success(list);
    }


    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Record record = recordService.selectById(id);
        return Result.success(record);
    }


    @GetMapping("/selectList")
    public Result selectList(Record record) {
        List<Record> list = recordService.selectList(record);
        return Result.success(list);
    }



    @GetMapping("/selectPage")
    public Result selectPage(Record record,  // 自动接收 cinemaId、filmId 等字段
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Record> pageInfo = recordService.selectPage(record, pageNum, pageSize);
        return Result.success(pageInfo);
    }


    @PostMapping("/add")
    public Result add(@RequestBody Record record) {
        recordService.add(record);
        return Result.success();
    }


    @PutMapping("/update")
    public Result update(@RequestBody Record record) {
        recordService.update(record);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        recordService.delete(id);
        return Result.success();
    }


    @DeleteMapping("/deleteBatch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        recordService.deleteBatch(ids);
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

            String fileName = recordService.uploadFile(file, uploadDir);

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