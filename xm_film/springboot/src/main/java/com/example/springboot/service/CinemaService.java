package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.Cinema;
import com.example.springboot.common.enums.ErrorCode;
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

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CinemaService extends BaseService<Cinema> {

    @Resource
    private CinemaMapper cinemaMapper;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    protected BaseMapper<Cinema> mapper() {
        return cinemaMapper;
    }

    public PageInfo<Cinema> selectPage(Cinema cinema, Integer filmId, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Cinema> list = cinemaMapper.selectByFilmId(cinema, filmId);
        return PageInfo.of(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Cinema cinema) {
        String username = cinema.getUsername();
        Cinema dbCinema = cinemaMapper.selectByUsername(username);
        if (dbCinema != null) {
            throw new CustomException(ErrorCode.SYSTEM_ERROR.code(), "账号已存在,请更换别的账号");
        }
        if (cinema.getPassword() == null) {
            cinema.setPassword("cinema123");
        }
        if (cinema.getName() == null) {
            cinema.setName(cinema.getUsername());
        }
        cinema.setRole("CINEMA");
        cinema.setStatus("待审核");
        cinema.setPassword(passwordEncoder.encode(cinema.getPassword()));
        mapper().insert(cinema);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Cinema cinema) {
        cinema.setPassword(null);
        cinema.setRole(null);
        mapper().updateById(cinema);
    }

    public Cinema login(Account account) {
        String username = account.getUsername();
        Cinema dbCinema = cinemaMapper.selectByUsername(username);
        if (dbCinema == null) {
            throw new CustomException(ErrorCode.SYSTEM_ERROR.code(), "账号不存在");
        }
        String password = account.getPassword();
        if (!passwordEncoder.matches(password, dbCinema.getPassword())) {
            if (!dbCinema.getPassword().equals(password)) {
                throw new CustomException(ErrorCode.SYSTEM_ERROR.code(), "账号或密码错误");
            }
        }
        return dbCinema;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Account account) {
        Integer id = account.getId();
        Cinema cinema = selectById(id);
        if (cinema == null) {
            throw new CustomException(ErrorCode.SYSTEM_ERROR.code(), "账号不存在");
        }
        if (!passwordEncoder.matches(account.getPassword(), cinema.getPassword())) {
            if (!cinema.getPassword().equals(account.getPassword())) {
                throw new CustomException(ErrorCode.SYSTEM_ERROR.code(), "原密码错误");
            }
        }
        cinema.setPassword(passwordEncoder.encode(account.getNewPassword()));
        cinemaMapper.updatePassword(cinema);
    }

    @Transactional(rollbackFor = Exception.class)
    public void register(Account account) {
        Cinema cinema = new Cinema();
        BeanUtils.copyProperties(account, cinema);
        add(cinema);
    }
}
