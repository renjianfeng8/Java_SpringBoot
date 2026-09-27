package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.Cinema;
import com.example.springboot.common.enums.CinemaStatus;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.CinemaMapper;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
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

    public PageInfo<Cinema> selectPage(Cinema cinema, Integer filmId, Integer pageNum, Integer pageSize,
                                       boolean approvedOnly) {
        PageMethod.startPage(pageNum, pageSize);
        List<Cinema> list = cinemaMapper.selectByFilmId(cinema, filmId, approvedOnly);
        return PageInfo.of(list);
    }

    /**
     * 影院列表。approvedOnly 由调用方按角色决定：未登录/非管理员只看到「已审核」，
     * 管理员看到全部 —— 否则后台审核列表会连待审核的影院都查不出来，无法审核。
     */
    public List<Cinema> selectAll(Cinema cinema, boolean approvedOnly) {
        return cinemaMapper.selectByFilmId(cinema, null, approvedOnly);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Cinema cinema) {
        insertCinema(cinema);
    }

    private void insertCinema(Cinema cinema) {
        String username = cinema.getUsername();
        Cinema dbCinema = cinemaMapper.selectByUsername(username);
        if (dbCinema != null) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT.code(), "账号已存在,请更换别的账号");
        }
        if (cinema.getPassword() == null) {
            cinema.setPassword("cinema123");
        }
        if (cinema.getName() == null) {
            cinema.setName(cinema.getUsername());
        }
        cinema.setRole("CINEMA");
        cinema.setStatus(CinemaStatus.UNAUDITED);
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
            throw new CustomException(ErrorCode.UNAUTHORIZED.code(), "账号不存在");
        }
        String password = account.getPassword();
        if (!passwordEncoder.matches(password, dbCinema.getPassword())
                && !dbCinema.getPassword().equals(password)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED.code(), "账号或密码错误");
        }
        if (!CinemaStatus.APPROVED.equals(dbCinema.getStatus())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED.code(),
                    "影院账号尚未通过审核，暂时无法登录");
        }
        return dbCinema;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Account account) {
        Integer id = account.getId();
        Cinema cinema = selectById(id);
        if (cinema == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED.code(), "账号不存在");
        }
        if (!passwordEncoder.matches(account.getPassword(), cinema.getPassword())
                && !cinema.getPassword().equals(account.getPassword())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED.code(), "原密码错误");
        }
        cinema.setPassword(passwordEncoder.encode(account.getNewPassword()));
        cinemaMapper.updatePassword(cinema);
    }

    @Transactional(rollbackFor = Exception.class)
    public void register(Account account) {
        Cinema cinema = new Cinema();
        BeanUtils.copyProperties(account, cinema);
        insertCinema(cinema);
    }
}
