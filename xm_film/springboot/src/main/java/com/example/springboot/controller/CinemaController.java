package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.entity.Cinema;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.CinemaService;
import com.example.springboot.service.OrderedService;
import com.example.springboot.service.RecordService;
import com.example.springboot.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Tag(name = "影院管理", description = "影院 CRUD、分页查询（支持按电影筛选）")
@RestController
@RequestMapping("/api/v1/cinemas")
public class CinemaController extends BaseController<Cinema> {

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

    @Operation(summary = "分页查询影院", description = "支持按电影ID筛选正在上映该电影的影院")
    @Override
    @GetMapping("/page")
    public Result page(Cinema cinema,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        Integer filmId = null;
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            String filmIdStr = attrs.getRequest().getParameter("filmId");
            if (filmIdStr != null) {
                filmId = Integer.valueOf(filmIdStr);
            }
        }
        return Result.success(cinemaService.selectPage(cinema, filmId, pageNum, pageSize, !isAdmin()));
    }

    @Operation(summary = "查询全部影院", description = "未登录/非管理员只会看到「已审核」的影院")
    @Override
    @GetMapping
    public Result list(Cinema cinema) {
        return Result.success(cinemaService.selectAll(cinema, !isAdmin()));
    }

    @Operation(summary = "新增影院", description = "仅管理员；新影院初始状态为「未审核」")
    @Override
    @PostMapping
    public Result add(@RequestBody Cinema cinema) {
        requireAdmin();
        cinemaService.add(cinema);
        return Result.success();
    }

    @Override
    @PutMapping
    public Result update(@RequestBody Cinema cinema) {
        if (isAdmin()) {
            cinemaService.update(cinema);
            return Result.success();
        }
        if (isCinema()) {
            cinema.setId(currentUserId());
            cinema.setStatus(null);
            cinemaService.update(cinema);
            return Result.success();
        }
        throw new CustomException(ErrorCode.FORBIDDEN, "权限不足");
    }

    @Override
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        requireAdmin();
        ensureNoReferences(id);
        cinemaService.delete(id);
        return Result.success();
    }

    @Override
    @DeleteMapping("/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
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
        return "ADMIN".equals(currentRole());
    }

    private boolean isCinema() {
        return "CINEMA".equals(currentRole());
    }

    private void requireAdmin() {
        if (!isAdmin()) {
            throw new CustomException(ErrorCode.FORBIDDEN, "权限不足");
        }
    }

    private String currentRole() {
        HttpServletRequest request = currentRequest();
        return request == null ? null : (String) request.getAttribute("role");
    }

    private Integer currentUserId() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String userId = (String) request.getAttribute("userId");
        return userId == null ? null : Integer.valueOf(userId);
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
