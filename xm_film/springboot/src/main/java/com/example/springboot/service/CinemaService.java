package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.Cinema;
import com.example.springboot.entity.CinemaDirectory;
import com.example.springboot.common.enums.CinemaStatus;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.dto.response.CinemaRegionView;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.CinemaDirectoryMapper;
import com.example.springboot.mapper.CinemaMapper;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 影院账号与资料的读写：登录/注册/密码维护、按影片与区域分页查询、区域树聚合，
 * 以及从影院名录批量导入。影院分「已审核 / 未审核」，公开列表只出已审核的。
 */
@Service
@Transactional(readOnly = true)
public class CinemaService extends BaseService<Cinema> {

    @Resource
    private CinemaMapper cinemaMapper;

    @Resource
    private CinemaDirectoryMapper directoryMapper;

    private static final String DEFAULT_PASSWORD = "cinema123";

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

    /** 前台区域级联：把 distinct(city,district) 行按城市归组，保持 SQL 的排序 */
    public List<CinemaRegionView> regions() {
        Map<String, List<String>> byCity = new LinkedHashMap<>();
        for (Map<String, Object> row : cinemaMapper.selectRegions()) {
            String city = (String) row.get("city");
            if (city == null || city.isBlank()) {
                continue;
            }
            String district = (String) row.get("district");
            List<String> districts = byCity.computeIfAbsent(city, k -> new ArrayList<>());
            if (district != null && !district.isBlank()) {
                districts.add(district);
            }
        }
        List<CinemaRegionView> result = new ArrayList<>();
        byCity.forEach((city, districts) -> result.add(new CinemaRegionView(city, districts)));
        return result;
    }

    /**
     * 从影院名录批量生成影院，幂等：已关联过名录的影院跳过。
     * 生成的影院只带 名称/电话/地址/区域，没有影厅与排片 —— 前台可见、不能真正购票。
     */
    @Transactional(rollbackFor = Exception.class)
    public int importFromDirectory(List<Integer> directoryIds) {
        if (directoryIds == null || directoryIds.isEmpty()) {
            return 0;
        }
        // 所有生成影院共用同一默认密码，故一次性算出哈希复用：安全性等价，且避免逐行 bcrypt 的昂贵开销
        String defaultHash = passwordEncoder.encode(DEFAULT_PASSWORD);
        Set<Integer> linked = new HashSet<>(cinemaMapper.selectLinkedDirectoryIds(directoryIds));
        int created = 0;
        for (CinemaDirectory row : directoryMapper.selectByIds(directoryIds)) {
            if (linked.contains(row.getId())) {
                continue;
            }
            Cinema cinema = new Cinema();
            cinema.setUsername("amap_" + row.getPoiId());
            cinema.setPassword(defaultHash);
            cinema.setRole(RoleEnum.CINEMA.name());
            cinema.setStatus(CinemaStatus.APPROVED);
            cinema.setName(row.getName());
            cinema.setPhone(row.getPhone());
            cinema.setAddress(row.getAddress());
            cinema.setDirectoryId(row.getId());
            cinemaMapper.insert(cinema);
            created++;
        }
        return created;
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
            cinema.setPassword(DEFAULT_PASSWORD);
        }
        if (cinema.getName() == null) {
            cinema.setName(cinema.getUsername());
        }
        cinema.setRole(RoleEnum.CINEMA.name());
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
                && !password.equals(dbCinema.getPassword())) {
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
