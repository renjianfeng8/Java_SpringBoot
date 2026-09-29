package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.entity.Mark;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.FilmMapper;
import com.example.springboot.mapper.MarkMapper;
import com.example.springboot.mapper.OrderedMapper;
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
