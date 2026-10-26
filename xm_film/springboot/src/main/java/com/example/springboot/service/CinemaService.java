package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.common.PasswordMatcher;
import com.example.springboot.common.SecureCodes;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.Cinema;
import com.example.springboot.entity.CinemaDirectory;
import com.example.springboot.common.enums.CinemaStatus;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.dto.response.CinemaAccountImportView;
import com.example.springboot.dto.response.CinemaRegionItemView;
import com.example.springboot.dto.response.CinemaRegionView;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.CinemaDirectoryMapper;
import com.example.springboot.mapper.CinemaMapper;
import com.example.springboot.mapper.OrderedMapper;
import com.example.springboot.mapper.RecordMapper;
import com.example.springboot.mapper.RoomMapper;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    /** 导入影院时顺带问一次高德封面（只查 URL，不下载字节） */
    private CinemaCoverService coverService;

    private RoomMapper roomMapper;

    private RecordMapper recordMapper;

    private OrderedMapper orderedMapper;

    /**
     * 批量导入账号的初始口令字母表：剔除 I/L/O/0/1（人工抄写易混），
     * 大小写 + 数字，配合 12 位长度约 70 bit 熵。
     */
    private static final String IMPORTED_ACCOUNT_ALPHABET =
            "ABCDEFGHJKMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    /** 批量导入影院的初始口令长度：12 位、约 70 bit 熵，抵御离线猜测 */
    private static final int INITIAL_PASSWORD_LENGTH = 12;

    private final PasswordEncoder passwordEncoder = PasswordMatcher.instance();

    public CinemaService(CinemaMapper cinemaMapper, CinemaDirectoryMapper directoryMapper) {
        this.cinemaMapper = cinemaMapper;
        this.directoryMapper = directoryMapper;
    }

    /**
     * 删除守卫所需的只读计数源，用 setter 注入而不是扩构造器：
     * 构造器已被单测直接 new，扩参数会破坏既有用例；这三个 mapper 只在
     * {@link #deleteGuarded} 里用到，没有循环依赖风险。
     */
    @Autowired
    private void setReferenceMappers(RoomMapper roomMapper, RecordMapper recordMapper,
                                     OrderedMapper orderedMapper) {
        this.roomMapper = roomMapper;
        this.recordMapper = recordMapper;
        this.orderedMapper = orderedMapper;
    }

    /** 补图服务用 setter 注入：构造器已被单测直接 new，扩参数会破坏既有用例 */
    @Autowired
    public void setCoverService(CinemaCoverService coverService) {
        this.coverService = coverService;
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
     * <p><b>初始口令（Sonar {@code java:S6437}，2026-10-20）</b>：每个影院生成独立的
     * 12 位随机口令，库里只存 BCrypt 哈希；明文仅在本次响应里返回一次，供管理员分发。
     * 旧实现让所有导入影院共用硬编码的 {@code cinema123}，叠加「用户名 = amap_+poiId 可推导、
     * 登录无失败锁定」，等于公开凭证。
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
    public CinemaAccountImportView importFromDirectory(List<Integer> directoryIds) {
        if (directoryIds == null || directoryIds.isEmpty()) {
            return new CinemaAccountImportView(0, List.of());
        }
        Set<Integer> linked = new HashSet<>(cinemaMapper.selectLinkedDirectoryIds(directoryIds));
        int created = 0;
        List<CinemaAccountImportView.Account> accounts = new ArrayList<>();
        for (CinemaDirectory row : directoryMapper.selectByIds(directoryIds)) {
            if (linked.contains(row.getId())) {
                continue;
            }
            // 逐行生成口令，bcrypt 的成本必须按账号付：共用哈希等于共用口令
            String initialPassword = SecureCodes.of(IMPORTED_ACCOUNT_ALPHABET, INITIAL_PASSWORD_LENGTH);
            Cinema cinema = new Cinema();
            cinema.setUsername("amap_" + row.getPoiId());
            cinema.setPassword(passwordEncoder.encode(initialPassword));
            cinema.setRole(RoleEnum.CINEMA.name());
            cinema.setStatus(CinemaStatus.APPROVED);
            cinema.setName(row.getName());
            cinema.setPhone(row.getPhone());
            cinema.setAddress(row.getAddress());
            cinema.setDirectoryId(row.getId());
            // 封面：只查 URL 不下载（下载在事务外由 CinemaCoverService.fillOneCover 完成）。
            // 查不到就留空，前端 avatarSrc 兜底成默认图，不影响导入。
            if (coverService != null) {
                cinema.setAvatar(coverService.lookupCoverUrl(row.getPoiId()));
            }
            try {
                cinemaMapper.insert(cinema);
                created++;
                accounts.add(new CinemaAccountImportView.Account(cinema.getUsername(), initialPassword));
            } catch (DuplicateKeyException e) {
                // 并发导入的败者走到这里。唯一索引拦下的是「同一条名录被插第二次」，
                // 按幂等语义跳过即可 —— 用户看到的是「导入成功，新建 0 家」而不是 500。
                // 也可能是 username 撞了别的影院（poi_id 唯一，理论上不会），
                // 那种情况下跳过只是少建一家，不会错关联，代价可接受。
                linked.add(row.getId());
            } catch (PessimisticLockingFailureException e) {
                // 实测（2026-10-19）：两个管理员并发导入时，MySQL 报的往往不是唯一键冲突，
                // 而是死锁 —— 两个事务各自插入不同名录行，却争同一把 username 索引锁，
                // 插入顺序不同即互相等待，MySQL 选一个回滚。
                //
                // 捕父类型而不是 DeadlockLoserDataAccessException：同一冲突的另一半形态是
                // 等锁超时（CannotAcquireLockException），只捕死锁会让超时漏成 500（BL-47/BL-52）。
                //
                // 死锁是「可重试」，不是用户操作错误：给一句能照做的提示，而不是「系统错误」。
                // 事务随异常整体回滚（类上 rollbackFor = Exception.class），不会留半批数据。
                throw new CustomException(ErrorCode.BUSINESS_CONFLICT,
                        "有其他人正在导入同一批名录，请稍后重试");
            }
        }
        return new CinemaAccountImportView(created, accounts);
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
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT, "账号已存在,请更换别的账号");
        }
        if (cinema.getPassword() == null || cinema.getPassword().isBlank()) {
            // 不再回退到硬编码默认口令（Sonar java:S6437）：后台新增表单本就必填，
            // 直接调接口不传口令的请求应当在入口被拒绝，而不是拿到一个公开可查的初始口令。
            throw new CustomException(ErrorCode.PARAM_INVALID, "请设置影院的初始登录密码");
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
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT, "账号已存在,请更换别的账号");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Cinema cinema) {
        cinema.setPassword(null);
        cinema.setRole(null);
        super.update(cinema);
    }

    /**
     * 删除影院：先锁影院行，再数子表引用（影厅/排片/订单），最后删除。
     *
     * <p><b>为什么守卫必须在 Service 且在事务内</b>（原实现放在 Controller）：
     * Controller 的计数与 {@code delete} 分属两个事务，中间存在窗口 —— 管理员数到
     * 「0 个影厅」之后、DELETE 之前，影院角色可以新增一个影厅，DELETE 随即撞外键
     * RESTRICT。数据不会坏（外键挡住了），但管理员拿到的是通用提示
     * 「仍被其他数据引用」，而不是「已有 1 个影厅」。锁住影院行后，
     * {@code RoomService.addScoped} 的新增会被挡到删除提交之后（届时影院已不存在，新增被拒）。
     *
     * <p><b>批量删除按 id 升序加锁</b>：两个管理员同时提交 [A,B] 与 [B,A] 时，
     * 若各自按请求顺序加锁就会互相等待而死锁（BL-04 已实测该库会报死锁）。
     *
     * <p>计数与删除之间不再需要留缝，所以整段没有 SLEEP 之类的东西；
     * 这里刻意不把 delete 挪回 Controller —— 那正是本节要消除的两套口径。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteGuarded(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<Integer> ordered = ids.stream().filter(Objects::nonNull).distinct().sorted().toList();
        List<String> conflicts = new ArrayList<>();
        for (Integer id : ordered) {
            if (cinemaMapper.selectByIdForUpdate(id) == null) {
                throw new CustomException(ErrorCode.NOT_FOUND, "影院不存在，可能已被删除");
            }
            String conflict = describeReferences(id);
            if (conflict != null) {
                conflicts.add(conflict);
            }
        }
        if (!conflicts.isEmpty()) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT, String.join("；", conflicts) + "，无法删除");
        }
        deleteBatch(ordered);
    }

    /** 汇总影院下挂的影厅/排片/订单数，有任一条即返回可读提示，否则返回 null */
    private String describeReferences(Integer cinemaId) {
        int rooms = roomMapper.countByCinemaId(cinemaId);
        int records = recordMapper.countByCinemaId(cinemaId);
        int orders = orderedMapper.countByCinemaId(cinemaId);
        if (rooms == 0 && records == 0 && orders == 0) {
            return null;
        }
        return "ID " + cinemaId + " 已关联 " + rooms + " 个影厅、" + records + " 个排片、" + orders + " 笔订单";
    }

    /**
     * 锁影院行并返回；删除守卫的加锁读入口。
     * 供 {@code RoomService.addScoped} 在插入影厅前调用，使「新增影厅」与「删影院」互斥。
     * 须在事务内执行，否则 FOR UPDATE 退化为普通读。这里的注解保证两个调用方都安全：
     * {@code RoomService.addScoped} 是跨 bean 调用，会进来并加入其外层事务；
     * {@code deleteGuarded} 是同类内部调用（注解不经过代理），但本类方法自带事务，锁同样有效。
     */
    @Transactional(rollbackFor = Exception.class)
    public Cinema selectByIdForUpdate(Integer id) {
        return cinemaMapper.selectByIdForUpdate(id);
    }

    public Cinema login(Account account) {
        String username = account.getUsername();
        Cinema dbCinema = cinemaMapper.selectByUsername(username);
        if (dbCinema == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "账号不存在");
        }
        String password = account.getPassword();
        if (!PasswordMatcher.matches(passwordEncoder, password, dbCinema.getPassword())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        if (!CinemaStatus.APPROVED.equals(dbCinema.getStatus())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED,
                    "影院账号尚未通过审核，暂时无法登录");
        }
        return dbCinema;
    }

    /**
     * 登录计数定位：返回库中该用户名的账号 id，不存在时返回 null。
     * 比较交给库的 collation，因此大小写/重音/全角/尾随空格等拼写变体都落到同一 id。
     */
    public Integer findIdByUsername(String username) {
        Cinema dbCinema = cinemaMapper.selectByUsername(username);
        return dbCinema == null ? null : dbCinema.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Account account) {
        Integer id = account.getId();
        Cinema cinema = selectById(id);
        if (cinema == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "账号不存在");
        }
        if (!PasswordMatcher.matches(passwordEncoder, account.getPassword(), cinema.getPassword())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "原密码错误");
        }
        cinema.setPassword(passwordEncoder.encode(account.getNewPassword()));
        cinemaMapper.updatePassword(cinema);
    }

    /** 管理员重置登录密码：不校验原密码，权限由调用方限定为 ADMIN */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Integer id, String newPassword) {
        if (selectById(id) == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "影院账号不存在");
        }
        Cinema patch = new Cinema();
        patch.setId(id);
        patch.setPassword(passwordEncoder.encode(newPassword));
        cinemaMapper.updatePassword(patch);
    }

    @Transactional(rollbackFor = Exception.class)
    public void register(Account account) {
        Cinema cinema = new Cinema();
        BeanUtils.copyProperties(account, cinema);
        insertCinema(cinema);
    }
}
