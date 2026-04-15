package com.example.springboot.service;

import com.example.springboot.entity.Mark;
import com.example.springboot.mapper.MarkMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class MarkService {

    @Resource
    private MarkMapper markMapper;

    public List<Mark> selectAll(Mark mark) {
        return markMapper.selectAll(mark);
    }

    public Mark selectById(Integer id) {
        return markMapper.selectById(id);

    }

    public List<Mark> selectList(Mark mark) {
        System.out.println(mark);
        return null;
    }


    public PageInfo<Mark> selectPage( Mark mark, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Mark> list = markMapper.selectAll(mark);
        return PageInfo.of(list);
    }

    public void add(Mark mark) {
        markMapper.insert(mark);
    }

    public void update(Mark mark) {
        markMapper.updateById(mark);
    }

    public void delete(Integer id) {
        markMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            this.delete(id);
        }
    }

    /**
     * 文件上传功能
     * @param file 上传的文件
     * @param uploadDir 文件存储目录
     * @return 存储后的文件相对路径
     * @throws IOException 文件操作异常
     */
    public String uploadFile(MultipartFile file, String uploadDir) throws IOException {
        // 检查上传文件是否为空
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传的文件不能为空");
        }

        // 检查并创建上传目录
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                throw new IOException("无法创建上传目录: " + uploadDir);
            }
        }

        // 生成唯一文件名，避免文件重名
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFileName = UUID.randomUUID() + fileExtension;

        // 构建完整文件路径
        Path filePath = Paths.get(uploadDir, uniqueFileName);

        try {
            // 保存文件到指定路径
            Files.write(filePath, file.getBytes());
            // 返回文件相对路径（相对于上传目录）
            return uniqueFileName;
        } catch (IOException e) {
            // 上传失败时，尝试删除已创建的空文件
            if (Files.exists(filePath)) {
                Files.deleteIfExists(filePath);
            }
            throw new IOException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文件扩展名（包括点号）
     * @param fileName 文件名
     * @return 文件扩展名（例如：.jpg, .pdf）
     */
    private String getFileExtension(String fileName) {
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
