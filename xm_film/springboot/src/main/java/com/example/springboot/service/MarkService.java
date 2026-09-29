package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.dto.response.FilmMarksView;
import com.example.springboot.dto.response.MarkLikeResult;
import com.example.springboot.dto.response.MarkView;
import com.example.springboot.entity.Mark;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.FilmMapper;
import com.example.springboot.mapper.MarkLikeMapper;
import com.example.springboot.mapper.MarkMapper;
import com.example.springboot.mapper.OrderedMapper;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class MarkService extends BaseService<Mark> {

    private static final double MIN_SCORE = 0.0;
    private static final double MAX_SCORE = 10.0;
    private static final int MAX_COMMENT_LENGTH = 255;

    @Resource
    private MarkMapper markMapper;

    @Resource
    private MarkLikeMapper markLikeMapper;

    @Resource
    private FilmMapper filmMapper;

    @Resource
    private OrderedMapper orderedMapper;

    @Override
    protected BaseMapper<Mark> mapper() {
        return markMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Mark mark) {
        if (mark.getUserId() == null) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "缺少评价人，请重新登录后再试");
        }
        if (mark.getFilmId() == null) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "请选择要评价的影片");
        }
        validateScore(mark.getScore());
        validateComment(mark.getMark());
        // 评价资格是这个功能的语义前提（"看过的才能评"），此前只有前端按钮在守：
        // front/Orders.vue 仅对 已取票 订单渲染「去评价」，而服务端不校验订单 ——
        // 直接 POST /api/v1/marks 能给任何没买过票的影片打分。此处补上服务端门禁。
        // 已取票是终态（退票与删除都进不来），因此这个资格一旦成立不会被推翻，
        // 修改评价时无需重复校验。
        if (orderedMapper.countPickedUpByUserAndFilm(mark.getUserId(), mark.getFilmId()) == 0) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT,
                    "请先取票后再评价：只能评价自己已取票场次对应的影片");
        }
        if (markMapper.countByUserAndFilm(mark.getUserId(), mark.getFilmId()) > 0) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT, "您已评价过该影片，可直接修改原有评价");
        }
        mapper().insert(mark);
        recalculateFilmScore(mark.getFilmId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Mark mark) {
        Mark dbMark = requireExisting(mark.getId());
        validateScore(mark.getScore());
        validateComment(mark.getMark());
        // 评价的归属与目标影片不允许改，防止把评价转移到别人/别的影片名下
        mark.setUserId(null);
        mark.setFilmId(null);
        mapper().updateById(mark);
        // 改分可能改变该片均分；改影片的字段已被上面置空，故按库中影片重算
        recalculateFilmScore(dbMark.getFilmId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        Mark dbMark = requireExisting(id);
        mapper().deleteById(id);
        recalculateFilmScore(dbMark.getFilmId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Integer> ids) {
        Set<Integer> filmIds = new HashSet<>();
        for (Integer id : ids) {
            Mark dbMark = markMapper.selectById(id);
            if (dbMark != null) {
                filmIds.add(dbMark.getFilmId());
            }
        }
        mapper().deleteBatch(ids);
        filmIds.forEach(this::recalculateFilmScore);
    }

    /**
     * 点赞 / 取消点赞。liked 是**显式意图**而非 toggle，连点与重试收敛到用户要的状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public MarkLikeResult setLike(Integer markId, Integer userId, boolean liked) {
        requireExisting(markId);
        if (liked) {
            // 主键 (mark_id, user_id) 让"已赞过"变成一次无副作用的空更新；不用 INSERT IGNORE，
            // 因为它会把外键错误也一并吞掉（见 MarkLikeMapper 的注释）。
            markLikeMapper.insertIfAbsent(markId, userId);
        } else {
            markLikeMapper.deleteByMarkAndUser(markId, userId);
        }
        // 回读权威状态，而不是假定请求生效：重复点赞时受影响行数同样是 0，
        // 且并发下两个请求都应拿到同一份真相。
        boolean nowLiked = markLikeMapper.countByMarkAndUser(markId, userId) > 0;
        return new MarkLikeResult(nowLiked, markLikeMapper.countByMarkId(markId));
    }

    /**
     * 某片的评价列表（分页，按赞数降序 → id 降序），附带本人评价与发表资格。
     *
     * liked / mine 交给 SQL 按访问者算（viewer 为 null 时一并为 false）；
     * 只有 USER 才有"我的评价 / 我的点赞"可言，其余角色（含游客）按匿名口径查。
     */
    public FilmMarksView listByFilm(Integer filmId, Integer viewerId, String role,
                                    Integer pageNum, Integer pageSize) {
        if (filmId == null) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "缺少影片ID");
        }
        if (filmMapper.selectById(filmId) == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "影片不存在");
        }
        // 非 USER 不查本人评价，也免得白跑一次已取票统计
        boolean isUser = "USER".equals(role) && viewerId != null;
        Integer viewer = isUser ? viewerId : null;
        PageMethod.startPage(pageNum, pageSize);
        List<MarkView> list = markMapper.selectFilmMarks(filmId, viewer);
        long total = new PageInfo<>(list).getTotal();
        MarkView my = isUser ? markMapper.selectFilmMarkOfUser(filmId, viewer) : null;
        // reviewable 只表示"够格发表"（已取票）；是否已评过由 my != null 回答
        boolean reviewable = isUser && orderedMapper.countPickedUpByUserAndFilm(viewerId, filmId) > 0;
        return new FilmMarksView(total, reviewable, my, list);
    }

    private Mark requireExisting(Integer id) {
        Mark dbMark = id == null ? null : markMapper.selectById(id);
        if (dbMark == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "评价不存在");
        }
        return dbMark;
    }

    private void recalculateFilmScore(Integer filmId) {
        if (filmId != null) {
            filmMapper.recalculateScore(filmId);
        }
    }

    private void validateScore(Double score) {
        if (score == null) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "请填写评分");
        }
        if (score < MIN_SCORE || score > MAX_SCORE) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "评分需在 0~10 之间");
        }
    }

    private void validateComment(String comment) {
        if (comment != null && comment.length() > MAX_COMMENT_LENGTH) {
            throw new CustomException(ErrorCode.PARAM_INVALID,
                    "评语不能超过 " + MAX_COMMENT_LENGTH + " 字");
        }
    }
}
