package com.example.springboot.controller;

import com.example.springboot.common.FileUtil;
import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "文件上传", description = "图片/视频文件上传接口")
@RestController
@RequestMapping("/api/v1/files")
public class FileUploadController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.access-prefix}")
    private String accessPrefix;

    private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);

    @Operation(summary = "上传文件", description = "支持图片和视频文件上传，返回可访问的URL")
    @PostMapping("/upload")
    public Result uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return Result.error(ErrorCode.PARAM_INVALID.code(), "上传文件不能为空");
            }
            String fileName = FileUtil.uploadFile(file, uploadDir);
            String fileUrl = accessPrefix + fileName;
            log.info("生成的文件URL: {}", fileUrl);
            return Result.success(fileUrl);
        } catch (IllegalArgumentException e) {
            return Result.error(ErrorCode.PARAM_INVALID.code(), e.getMessage());
        } catch (IOException e) {
            log.error("File upload failed", e);
            return Result.error(ErrorCode.SYSTEM_ERROR.code(), "文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("File upload failed", e);
            return Result.error(ErrorCode.SYSTEM_ERROR.code(), "服务器内部错误");
        }
    }
}
