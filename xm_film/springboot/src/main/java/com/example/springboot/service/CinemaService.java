package com.example.springboot.service;

import com.example.springboot.common.FileUtil;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.Cinema;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.CinemaMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
@Service
@Transactional(rollbackFor = Exception.class)
public class CinemaService {

    @Resource
    private CinemaMapper cinemaMapper;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public List<Cinema> selectAll(Cinema cinema) {
        return cinemaMapper.selectAll(cinema);
    }

    public Cinema selectById(Integer id) {
        return cinemaMapper.selectById(id);
    }

    public List<Cinema> selectList(Cinema cinema) {
        return cinemaMapper.selectAll(cinema);
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
        cinema.setPassword(passwordEncoder.encode(cinema.getPassword())); // BCrypt加密密码
        cinemaMapper.insert(cinema);
    }

    public void update(Cinema cinema) {
        cinema.setPassword(null);
        cinema.setRole(null);
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
        if (!passwordEncoder.matches(password, dbCinema.getPassword())) {
            // 兼容 data.sql 明文初始密码
            if (!dbCinema.getPassword().equals(password)) {
                throw new CustomException("500","账号或密码错误");
            }
        }
        return dbCinema;
    }

    public void updatePassword(Account account) {
        Integer id = account.getId();
        Cinema cinema = this.selectById(id);
        if (cinema == null) {
            throw new CustomException("500", "账号不存在");
        }
        if (!passwordEncoder.matches(account.getPassword(), cinema.getPassword())) {
            // 兼容 data.sql 明文初始密码
            if (!cinema.getPassword().equals(account.getPassword())) {
                throw new CustomException("500","原密码错误");
            }
        }
        cinema.setPassword(passwordEncoder.encode(account.getNewPassword()));  //设置加密后的新密码
        this.update(cinema);
    }

    public String uploadFile(MultipartFile file, String uploadDir) throws IOException {
        return FileUtil.uploadFile(file, uploadDir);
    }


    public void register(Account account) {
        Cinema cinema = new Cinema();
        BeanUtils.copyProperties(account,cinema);
        add(cinema);

    }
}
