package com.example.springboot.controller;

import com.example.springboot.common.AuthContext;
import com.example.springboot.common.BaseController;
import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.dto.request.MarkLikeRequest;
import com.example.springboot.dto.request.MarkRequest;
import com.example.springboot.dto.response.FilmMarksView;
import com.example.springboot.dto.response.MarkEntityView;
import com.example.springboot.dto.response.MarkLikeView;
import com.example.springboot.entity.Mark;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.MarkService;
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
@RequestMapping("/api/v1/marks")
public class MarkController extends BaseController<Mark, MarkRequest, MarkEntityView> {

    private final MarkService markService;

    public MarkController(MarkService markService) {
        super(markService);
        this.markService = markService;
    }

    @Override
    protected Mark toEntity(MarkRequest request) {
        Mark mark = new Mark();
        mark.setId(request.getId());
        mark.setUserId(request.getUserId());
        mark.setFilmId(request.getFilmId());
        mark.setImg(request.getImg());
        mark.setScore(request.getScore());
        mark.setComment(request.getComment());
        mark.setUserName(request.getUserName());
        mark.setFilmName(request.getFilmName());
        mark.setFilmImg(request.getFilmImg());
        return mark;
    }

    @Override
    protected MarkEntityView toView(Mark mark) {
        MarkEntityView view = new MarkEntityView();
        view.setId(mark.getId());
        view.setUserId(mark.getUserId());
        view.setFilmId(mark.getFilmId());
        view.setImg(mark.getImg());
        view.setScore(mark.getScore());
        view.setComment(mark.getComment());
        view.setUserName(mark.getUserName());
        view.setFilmName(mark.getFilmName());
        view.setFilmImg(mark.getFilmImg());
        return view;
    }

    @Override
    @PostMapping
    public Result<Void> add(@RequestBody MarkRequest request) {
        Mark mark = toEntity(request);
        requireUser("发表评价");
        mark.setId(null);
        mark.setUserId(AuthContext.userId());
        markService.add(mark);
        return Result.success();
    }

    @Override
    @PutMapping
    public Result<Void> update(@RequestBody MarkRequest request) {
        Mark mark = toEntity(request);
        ensureOwnership(mark.getId());
        mark.setUserId(null);
        mark.setFilmId(null);
        markService.update(mark);
        return Result.success();
    }

    @Override
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        ensureOwnership(id);
        markService.delete(id);
        return Result.success();
    }

    @Override
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody List<Integer> ids) {
        for (Integer id : ids) {
            ensureOwnership(id);
        }
        markService.deleteBatch(ids);
        return Result.success();
    }

    @GetMapping("/by-film")
    public Result<FilmMarksView> listByFilm(@RequestParam Integer filmId,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(markService.listByFilm(filmId, AuthContext.userId(), AuthContext.role(), pageNum, pageSize));
    }

    @PutMapping("/{id}/like")
    public Result<MarkLikeView> like(@PathVariable Integer id, @RequestBody MarkLikeRequest body) {
        requireUser("点赞");
        if (body == null || body.getLiked() == null) {
            // 不把"参数缺失"默认成"取消点赞" —— 那会让一个拼错的请求静默取消掉用户的赞
            throw new CustomException(ErrorCode.PARAM_INVALID, "缺少 liked 参数");
        }
        return Result.success(markService.setLike(id, AuthContext.userId(), body.getLiked()));
    }

    /** ADMIN 可管理全部评价；其余角色只能操作自己的评价 */
    private void ensureOwnership(Integer markId) {
        if (isAdmin()) {
            return;
        }
        // 评价的 user_id 指向 user 表，而 admin/cinema 的 id 是另外两套自增序列。
        // 只比 id 不比角色，会让 id 恰好相同的影院账号操作到别的用户的评价（跨表撞号越权），
        // 故非 ADMIN 一律要求 USER —— 与发表/点赞的 requireUser 同一口径。
        requireUser("操作");
        if (markId == null) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "缺少评价ID");
        }
        Mark dbMark = markService.selectById(markId);
        if (dbMark == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "评价不存在");
        }
        Integer userId = AuthContext.userId();
        if (userId == null || !userId.equals(dbMark.getUserId())) {
            throw new CustomException(ErrorCode.FORBIDDEN, "只能操作自己的评价");
        }
    }

    /** 评价的 user_id 外键指向 user 表，影院/管理员账号的 id 不是用户 id，故只允许 USER 发表 / 点赞 */
    private void requireUser(String action) {
        if (!RoleEnum.USER.name().equals(AuthContext.role())) {
            throw new CustomException(ErrorCode.FORBIDDEN, "仅用户可" + action);
        }
    }

    private boolean isAdmin() {
        return RoleEnum.ADMIN.name().equals(AuthContext.role());
    }

}
