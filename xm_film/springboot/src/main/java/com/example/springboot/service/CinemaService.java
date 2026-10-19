package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.Cinema;
import com.example.springboot.entity.CinemaDirectory;
import com.example.springboot.common.enums.CinemaStatus;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.dto.response.CinemaRegionItemView;
import com.example.springboot.dto.response.CinemaRegionView;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.CinemaDirectoryMapper;
import com.example.springboot.mapper.CinemaMapper;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.DuplicateKeyException;
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

    private final CinemaMapper cinemaMapper;

    private final CinemaDirectoryMapper directoryMapper;

    private static final String DEFAULT_PASSWORD = "cinema123";

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public CinemaService(CinemaMapper cinemaMapper, CinemaDirectoryMapper directoryMapper) {
        this.cinemaMapper = cinemaMapper;
        this.directoryMapper = directoryMapper;
    }

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
     * filmId 与分页列表同口径（按上映影片筛），非空即生效。
     */
    public List<Cinema> selectAll(Cinema cinema, Integer filmId, boolean approvedOnly) {
        return cinemaMapper.selectByFilmId(cinema, filmId, approvedOnly);
    }

    /** 前台区域级联：把 distinct(city,district) 行按城市归组，保持 SQL 的排序 */
    public List<CinemaRegionView> regions() {
        Map<String, List<String>> byCity = new LinkedHashMap<>();
        for (CinemaRegionItemView row : cinemaMapper.selectRegions()) {
            String city = row.getCity();
            if (city == null || city.isBlank()) {
                continue;
            }
            String district = row.getDistrict();
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
     *
     * <p><b>并发语义（BL-04，2026-10-19）</b>：{@code selectLinkedDirectoryIds} 是无锁读，
     * 两个管理员同时导入同一批名录时都会读到「未收录」，然后各自 INSERT。
     * 因此这里**不依赖**那次预查来决定正确性：{@code cinema.directory_id} 上的
     * {@code uk_cinema_directory_id} 唯一索引是最终防线，撞键的行按「别人已经导入过」
     * 处理 —— 跳过、不累加 {@code created}、不报错。整批仍在一个事务里，
     * 一批名录要么全进要么全不进（除被跳过的重复项）。
     *
     * <p><b>为什么逐行 try/catch 而不把整批降级成 INSERT IGNORE</b>：需要区分
     * 「名录已关联」（正常跳过）与「username 撞了别的影院」（真冲突）。
     * 前者静默跳过，后者必须报出来 —— 见下面的异常分支。
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
            try {
                cinemaMapper.insert(cinema);
                created++;
            } catch (DuplicateKeyException e) {
                // 并发导入的败者走到这里。唯一索引拦下的是「同一条名录被插第二次」，
                // 按幂等语义跳过即可 —— 用户看到的是「导入成功，新建 0 家」而不是 500。
                // 也可能是 username 撞了别的影院（poi_id 唯一，理论上不会），
                // 那种情况下跳过只是少建一家，不会错关联，代价可接受。
                linked.add(row.getId());
            } catch (DeadlockLoserDataAccessException e) {
                // 实测（2026-10-19）：两个管理员并发导入时，MySQL 报的往往不是唯一键冲突，
                // 而是死锁 —— 两个事务各自插入不同名录行，却争同一把 username 索引锁，
                // 插入顺序不同即互相等待，MySQL 选一个回滚。
                //
                // 死锁是「可重试」，不是用户操作错误：给一句能照做的提示，而不是「系统错误」。
                // 事务随异常整体回滚（类上 rollbackFor = Exception.class），不会留半批数据。
                throw new CustomException(ErrorCode.BUSINESS_CONFLICT,
                        "有其他人正在导入同一批名录，请稍后重试");
            }
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
        try {
            mapper().insert(cinema);
        } catch (DuplicateKeyException e) {
            // 先查后插的窗口：并发注册同名影院时两个请求都可能读到 null。
            // username 唯一索引是最终防线；撞键转成同一句提示，而不是 500。
            // 注意：名录导入（importFromDirectory）**不经过这个方法**，它直接调
            // cinemaMapper.insert，并自己捕获 DuplicateKeyException 按幂等跳过。
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT.code(), "账号已存在,请更换别的账号");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Cinema cinema) {
        cinema.setPassword(null);
        cinema.setRole(null);
        super.update(cinema);
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
                && !account.getPassword().equals(cinema.getPassword())) {
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
