package com.example.springboot.service;

import com.example.springboot.entity.Account;
import com.example.springboot.entity.Admin;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.AdminMapper;
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

@Service//标记为服务层组件
public class AdminService {

    @Resource
    private AdminMapper adminMapper;

    public List<Admin> selectAll(Admin admin) {
        return adminMapper.selectAll(admin);
    }

    public Admin selectById(Integer id) {
        return adminMapper.selectById(id);

    }

    public List<Admin> selectList(Admin admin) {
        System.out.println(admin);
        return null;
    }

    public PageInfo<Admin> selectPage( Admin admin, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Admin> list = adminMapper.selectAll(admin);
        return PageInfo.of(list);
    }

    public void add(Admin admin) {
        String username = admin.getUsername(); //账号
        Admin dbAdmin = adminMapper.selectByUsername(username);
        if (dbAdmin != null) { //账号已存在
            throw new CustomException("500","账号已存在,请更换别的账号");
        }
        if (admin.getPassword() == null) { //密码没填
            admin.setPassword("admin123"); //默认密码admin123
        }
        if (admin.getName() == null) { //名字
            admin.setName(admin.getUsername());  //默认名称
        }
        //设置角色
        admin.setRole("ADMIN"); //管理员角色
        adminMapper.insert(admin);
    }

    public void update(Admin admin) {
        adminMapper.updateById(admin);
    }

    public void delete(Integer id) {
        adminMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            this.delete(id);
        }
    }

    public Admin login(Account account) {
        String username = account.getUsername(); //账号
        Admin dbAdmin = adminMapper.selectByUsername(username);
        if (dbAdmin == null) { //没查询到账户
            throw new CustomException("500","账号不存在");
        }
        //数据库存在这个账号
        String password = account.getPassword();
        if (!dbAdmin.getPassword().equals(password)) { //用户输入的密码和数据库账号的密码不匹配
            throw new CustomException("500","账号或密码错误");
        }
        return dbAdmin;
    }

    public void updatePassword(Account account) {
        Integer id = account.getId();
        Admin admin = this.selectById(id);
        if (!admin.getPassword().equals(account.getPassword())) { //页面传来的密码和数据库密码相比   不匹配就报错
            throw new CustomException("500","原密码错误");
        }
        admin.setPassword(account.getNewPassword());  //设置新密码
        this.update(admin);
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
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

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
