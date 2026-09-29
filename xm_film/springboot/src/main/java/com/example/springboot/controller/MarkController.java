package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.dto.request.MarkLikeRequest;
import com.example.springboot.entity.Mark;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.MarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Tag(name = "评价管理", description = "用户评价 CRUD（写操作限定归属：仅 USER 可发表，非 ADMIN 只能改删自己的评价）")
@RestController
@RequestMapping("/api/v1/marks")
public class MarkController extends BaseController<Mark> {

    private final MarkService markService;

    public MarkController(MarkService markService) {
        super(markService);
        this.markService = markService;
    }

    @Operation(summary = "发表评价", description = "评价人取自 JWT，忽略请求体中的 userId")
    @Override
    @PostMapping
    public Result add(@RequestBody Mark mark) {
        requireUser("发表评价");
        mark.setId(null);
        mark.setUserId(currentUserId());
        markService.add(mark);
        return Result.success();
    }

    @Operation(summary = "修改评价", description = "仅本人或 ADMIN；评价人与目标影片不可改")
    @Override
    @PutMapping
    public Result update(@RequestBody Mark mark) {
        ensureOwnership(mark.getId());
        mark.setUserId(null);
        mark.setFilmId(null);
        markService.update(mark);
        return Result.success();
    }

    @Operation(summary = "删除评价", description = "仅本人或 ADMIN")
    @Override
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        ensureOwnership(id);
        markService.delete(id);
        return Result.success();
    }

    @Operation(summary = "批量删除评价", description = "仅本人或 ADMIN")
    @Override
    @DeleteMapping("/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        for (Integer id : ids) {
            ensureOwnership(id);
        }
        markService.deleteBatch(ids);
        return Result.success();
    }

    @Operation(summary = "按影片查询评价", description = "公开只读；liked/mine 由后端按 JWT 计算，匿名恒 false")
    @GetMapping("/by-film")
    public Result listByFilm(@RequestParam Integer filmId,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(markService.listByFilm(filmId, currentUserId(), currentRole(), pageNum, pageSize));
    }

    @Operation(summary = "点赞/取消点赞", description = "仅 USER；liked 为显式意图，重复调用幂等")
    @PutMapping("/{id}/like")
    public Result like(@PathVariable Integer id, @RequestBody MarkLikeRequest body) {
        requireUser("点赞");
        if (body == null || body.getLiked() == null) {
            // 不把"参数缺失"默认成"取消点赞" —— 那会让一个拼错的请求静默取消掉用户的赞
            throw new CustomException(ErrorCode.PARAM_INVALID, "缺少 liked 参数");
        }
        return Result.success(markService.setLike(id, currentUserId(), body.getLiked()));
    }

    /** ADMIN 可管理全部评价；其余角色只能操作自己的评价 */
    private void ensureOwnership(Integer markId) {
        if (isAdmin()) {
            return;
        }
        if (markId == null) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "缺少评价ID");
        }
        Mark dbMark = markService.selectById(markId);
        if (dbMark == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "评价不存在");
        }
        Integer userId = currentUserId();
        if (userId == null || !userId.equals(dbMark.getUserId())) {
            throw new CustomException(ErrorCode.FORBIDDEN, "只能操作自己的评价");
        }
    }

    /** 评价的 user_id 外键指向 user 表，影院/管理员账号的 id 不是用户 id，故只允许 USER 发表 / 点赞 */
    private void requireUser(String action) {
        if (!"USER".equals(currentRole())) {
            throw new CustomException(ErrorCode.FORBIDDEN, "仅用户可" + action);
        }
    }

    private boolean isAdmin() {
        return "ADMIN".equals(currentRole());
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
