package com.example.springboot.controller;

import com.example.springboot.common.AuthContext;
import com.example.springboot.common.BaseController;
import com.example.springboot.common.Result;
import com.example.springboot.common.enums.CinemaStatus;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.dto.request.CinemaFromDirectoryRequest;
import com.example.springboot.dto.request.CinemaRequest;
import com.example.springboot.dto.response.CinemaRegionView;
import com.example.springboot.dto.response.CinemaView;
import com.example.springboot.entity.Cinema;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.CinemaService;
import com.example.springboot.service.OrderedService;
import com.example.springboot.service.RecordService;
import com.example.springboot.service.RoomService;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cinemas")
public class CinemaController extends BaseController<Cinema, CinemaRequest, CinemaView> {

    private final CinemaService cinemaService;
    private final RoomService roomService;
    private final RecordService recordService;
    private final OrderedService orderedService;

    public CinemaController(CinemaService cinemaService,
                            RoomService roomService,
                            RecordService recordService,
                            OrderedService orderedService) {
        super(cinemaService);
        this.cinemaService = cinemaService;
        this.roomService = roomService;
        this.recordService = recordService;
        this.orderedService = orderedService;
    }

    @Override
    protected Cinema toEntity(CinemaRequest request) {
        Cinema cinema = new Cinema();
        cinema.setId(request.getId());
        cinema.setUsername(request.getUsername());
        cinema.setPassword(request.getPassword());
        cinema.setRole(request.getRole());
        cinema.setName(request.getName());
        cinema.setNewPassword(request.getNewPassword());
        cinema.setToken(request.getToken());
        cinema.setAvatar(request.getAvatar());
        cinema.setAddress(request.getAddress());
        cinema.setStatus(request.getStatus());
        cinema.setPhone(request.getPhone());
        cinema.setDescription(request.getDescription());
        cinema.setDirectoryId(request.getDirectoryId());
        cinema.setProvince(request.getProvince());
        cinema.setCity(request.getCity());
        cinema.setDistrict(request.getDistrict());
        return cinema;
    }

    @Override
    protected CinemaView toView(Cinema cinema) {
        CinemaView view = new CinemaView();
        view.setId(cinema.getId());
        view.setUsername(cinema.getUsername());
        view.setRole(cinema.getRole());
        view.setName(cinema.getName());
        view.setAvatar(cinema.getAvatar());
        view.setAddress(cinema.getAddress());
        view.setStatus(cinema.getStatus());
        view.setPhone(cinema.getPhone());
        view.setDescription(cinema.getDescription());
        view.setDirectoryId(cinema.getDirectoryId());
        view.setProvince(cinema.getProvince());
        view.setCity(cinema.getCity());
        view.setDistrict(cinema.getDistrict());
        return view;
    }

    @Override
    @GetMapping("/page")
    public Result<PageInfo<CinemaView>> page(CinemaRequest query,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Cinema> src = cinemaService.selectPage(toEntity(query), query.getFilmId(), pageNum, pageSize, !isAdmin());
        return Result.success(toPageView(src));
    }

    /** 详情与列表同口径：未审核影院不对外展示（管理员与后台审核不受限），否则列表藏得住、按 id 又查得到 */
    @Override
    @GetMapping("/{id}")
    public Result<CinemaView> getById(@PathVariable Integer id) {
        Cinema cinema = cinemaService.selectById(id);
        if (cinema == null || (!isAdmin() && !CinemaStatus.APPROVED.equals(cinema.getStatus()))) {
            return Result.success(null);
        }
        return Result.success(toView(cinema));
    }

    /** 前台区域级联选项。前缀已在 AuthInterceptor 的 PUBLIC_READ_PREFIXES，游客可读 */
    @GetMapping("/regions")
    public Result<List<CinemaRegionView>> regions() {
        return Result.success(cinemaService.regions());
    }

    /** 从名录批量生成影院（幂等）。仅管理员 —— 写角色门禁在方法内 */
    @PostMapping("/from-directory")
    public Result<Integer> importFromDirectory(@RequestBody CinemaFromDirectoryRequest request) {
        requireAdmin();
        List<Integer> ids = request == null ? null : request.getDirectoryIds();
        return Result.success(cinemaService.importFromDirectory(ids));
    }

    @Override
    @GetMapping
    public Result<List<CinemaView>> list(CinemaRequest query) {
        return Result.success(cinemaService.selectAll(toEntity(query), query.getFilmId(), !isAdmin())
                .stream().map(this::toView).toList());
    }

    @Override
    @PostMapping
    public Result<Void> add(@RequestBody CinemaRequest request) {
        requireAdmin();
        cinemaService.add(toEntity(request));
        return Result.success();
    }

    @Override
    @PutMapping
    public Result<Void> update(@RequestBody CinemaRequest request) {
        Cinema cinema = toEntity(request);
        if (isAdmin()) {
            cinemaService.update(cinema);
            return Result.success();
        }
        if (isCinema()) {
            cinema.setId(AuthContext.userId());
            cinema.setStatus(null);
            // 名录关联决定区域展示，属管理员字段，影院自助更新不得改动
            cinema.setDirectoryId(null);
            cinemaService.update(cinema);
            return Result.success();
        }
        throw new CustomException(ErrorCode.FORBIDDEN, "权限不足");
    }

    @Override
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        requireAdmin();
        ensureNoReferences(id);
        cinemaService.delete(id);
        return Result.success();
    }

    @Override
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody List<Integer> ids) {
        requireAdmin();
        for (Integer id : ids) {
            ensureNoReferences(id);
        }
        cinemaService.deleteBatch(ids);
        return Result.success();
    }

    /** 影院下挂影厅/排片/订单时禁止物理删除，避免连带删除交易凭证 */
    private void ensureNoReferences(Integer cinemaId) {
        int rooms = roomService.countByCinemaId(cinemaId);
        int records = recordService.countByCinemaId(cinemaId);
        int orders = orderedService.countByCinemaId(cinemaId);
        if (rooms > 0 || records > 0 || orders > 0) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT,
                    "该影院已有 " + rooms + " 个影厅、" + records + " 个排片、" + orders
                            + " 笔订单，无法删除");
        }
    }

    private boolean isAdmin() {
        return RoleEnum.ADMIN.name().equals(AuthContext.role());
    }

    private boolean isCinema() {
        return RoleEnum.CINEMA.name().equals(AuthContext.role());
    }

    private void requireAdmin() {
        if (!isAdmin()) {
            throw new CustomException(ErrorCode.FORBIDDEN, "权限不足");
        }
    }

}
