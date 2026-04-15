package com.example.springboot.service;

import com.example.springboot.entity.Account;
import com.example.springboot.entity.Cinema;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.CinemaMapper;
import com.fasterxml.jackson.databind.util.BeanUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
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
public class CinemaService {

    @Resource
    private CinemaMapper cinemaMapper;

    public List<Cinema> selectAll(Cinema cinema) {
        return cinemaMapper.selectAll(cinema);
    }

    public Cinema selectById(Integer id) {
        return cinemaMapper.selectById(id);
    }

    public List<Cinema> selectList(Cinema cinema) {
        System.out.println(cinema);
        return null;
    }

    public PageInfo<Cinema> selectPage(Cinema cinema, Integer filmId, Integer pageNum, Integer pageSize) {
        // 开启分页
        PageHelper.startPage(pageNum, pageSize);

        // 调用修改后的mapper方法，传入电影ID参数
        List<Cinema> list = cinemaMapper.selectByFilmId(cinema, filmId);

        // 封装分页结果
        return PageInfo.of(list);
    }

    public void add(Cinema cinema) {
        String username = cinema.getUsername(); //账号
        Cinema dbCinema = cinemaMapper.selectByUsername(username);
        if (dbCinema != null) { //账号已存在
            throw new CustomException("500","账号已存在,请更换别的账号");
        }
        if (cinema.getPassword() == null) { //密码没填
            cinema.setPassword("cinema123"); //默认密码cinema123
        }
        if (cinema.getName() == null) { //名字
            cinema.setName(cinema.getUsername());  //默认名称
        }
        //设置角色
        cinema.setRole("CINEMA"); //影院
        cinema.setStatus("待审核");
        cinemaMapper.insert(cinema);
    }

    public void update(Cinema cinema) {
        cinemaMapper.updateById(cinema);
    }

    public void delete(Integer id) {
        cinemaMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            this.delete(id);
        }
    }

    public Cinema login(Account account) {
        String username = account.getUsername(); //账号
        Cinema dbCinema = cinemaMapper.selectByUsername(username);
        if (dbCinema == null) { //没查询到账户
            throw new CustomException("500","账号不存在");
        }
        //数据库存在这个账号
        String password = account.getPassword();
        if (!dbCinema.getPassword().equals(password)) { //用户输入的密码和数据库账号的密码不匹配
            throw new CustomException("500","账号或密码错误");
        }
        return dbCinema;
    }

    public void updatePassword(Account account) {
        Integer id = account.getId();
        Cinema cinema = this.selectById(id);
        if (!cinema.getPassword().equals(account.getPassword())) { //页面传来的密码和数据库密码相比   不匹配就报错
            throw new CustomException("500","原密码错误");
        }
        cinema.setPassword(account.getNewPassword());  //设置新密码
        this.update(cinema);
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


    public void register(Account account) {
        Cinema cinema = new Cinema();
        BeanUtils.copyProperties(account,cinema);
        add(cinema);

    }
}
