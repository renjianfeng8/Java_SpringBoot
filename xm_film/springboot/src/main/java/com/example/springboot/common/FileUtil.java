package com.example.springboot.common;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

public class FileUtil {

    private FileUtil() {
    }

    /**
     * 允许的上传类型：MIME -> 落盘扩展名。
     * 扩展名取自本表而非客户端文件名 —— Content-Type 是客户端可随意声明的，
     * 若再按文件名给后缀，就能把 .html/.jsp 之类的内容落在 /files/** 下并按其类型回放。
     */
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif",
            "video/mp4", ".mp4",
            "video/webm", ".webm"
    );

    public static String uploadFile(MultipartFile file, String uploadDir) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传的文件不能为空");
        }

        String contentType = file.getContentType();
        String extension = contentType == null ? null : ALLOWED_TYPES.get(contentType);
        if (extension == null) {
            throw new IllegalArgumentException("不支持的文件类型: " + contentType);
        }

        return saveBytes(file.getBytes(), extension, uploadDir);
    }

    /**
     * 由字节流落盘，供 TMDB 导入这类**不经浏览器上传**的写入路径使用。
     * 命名规则与 {@link #uploadFile} 共用同一实现（UUID + 扩展名），
     * 因此媒体文件不存在「上传的」和「导入的」两种形态，只有一种。
     *
     * @param extension 含点号的扩展名（如 ".jpg"），来自 TMDB 的图片路径
     * @return 落盘后的文件名（不含目录）
     */
    public static String saveBytes(byte[] content, String extension, String uploadDir) throws IOException {
        ensureDir(uploadDir);

        String uniqueFileName = UUID.randomUUID() + extension;
        Path filePath = Paths.get(uploadDir, uniqueFileName);

        try {
            Files.write(filePath, content);
            return uniqueFileName;
        } catch (IOException e) {
            Files.deleteIfExists(filePath);
            throw new IOException("文件写入失败: " + e.getMessage(), e);
        }
    }

    private static void ensureDir(String uploadDir) throws IOException {
        File dir = new File(uploadDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("无法创建上传目录: " + uploadDir);
        }
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex == -1) {
            return "";
        }

        return fileName.substring(lastIndex);
    }
}
