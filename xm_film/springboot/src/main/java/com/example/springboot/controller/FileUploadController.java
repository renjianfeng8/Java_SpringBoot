package com.example.springboot.controller;

import com.example.springboot.common.AuthContext;
import com.example.springboot.common.FileUtil;
import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RoleEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/files")
public class FileUploadController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.access-prefix}")
    private String accessPrefix;

    private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);

    /** 非管理员只能传图片，且单张不超过 2MB；视频与 50MB 大文件只留给后台管理员。 */
    private static final long MAX_USER_IMAGE_BYTES = 2L * 1024 * 1024;

    /** 任何角色都不得把磁盘写满：低于 1GB 余量直接拒绝上传。 */
    private static final long MIN_FREE_BYTES = 1024L * 1024 * 1024;

    @PostMapping("/upload")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return Result.error(ErrorCode.PARAM_INVALID, "上传文件不能为空");
            }
            if (!RoleEnum.ADMIN.name().equals(AuthContext.role())) {
                if (file.getSize() > MAX_USER_IMAGE_BYTES) {
                    return Result.error(ErrorCode.PARAM_INVALID, "图片不能超过 2MB");
                }
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return Result.error(ErrorCode.PARAM_INVALID, "仅支持上传图片");
                }
            }
            if (!FileUtil.hasUsableSpace(uploadDir, MIN_FREE_BYTES)) {
                return Result.error(ErrorCode.SYSTEM_ERROR, "服务器存储空间不足，请联系管理员");
            }
            String fileName = FileUtil.uploadFile(file, uploadDir);
            String fileUrl = accessPrefix + fileName;
            log.info("生成的文件URL: {}", fileUrl);
            return Result.success(fileUrl);
        } catch (IllegalArgumentException e) {
            return Result.error(ErrorCode.PARAM_INVALID, e.getMessage());
        } catch (IOException e) {
            log.error("File upload failed", e);
            return Result.error(ErrorCode.SYSTEM_ERROR, "文件上传失败: " + e.getMessage());
        }
    }
}
