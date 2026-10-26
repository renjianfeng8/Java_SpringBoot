package com.example.springboot.service;

import com.example.springboot.common.OrderStatusCriteria;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.dto.response.FilmMarksView;
import com.example.springboot.dto.response.MarkLikeView;
import com.example.springboot.dto.response.MarkView;
import com.example.springboot.entity.Film;
import com.example.springboot.entity.Mark;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.FilmMapper;
import com.example.springboot.mapper.MarkLikeMapper;
import com.example.springboot.mapper.MarkMapper;
import com.example.springboot.mapper.OrderedMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("java:S5778") // 测试 lambda 内除被测调用外还有 helper 调用，规则只影响可读性
class MarkServiceTest {

    @Mock
    private MarkMapper markMapper;

    @Mock
    private MarkLikeMapper markLikeMapper;

    @Mock
    private FilmMapper filmMapper;

    @Mock
    private OrderedMapper orderedMapper;

    @InjectMocks
    private MarkService markService;

    /** 评价门禁的前置条件：该用户对该影片有已取票订单 */
    private void givenPickedUpTicket(Integer userId, Integer filmId) {
        when(orderedMapper.countPickedUpByUserAndFilm(userId, filmId, OrderStatusCriteria.INSTANCE)).thenReturn(1);
    }

    private Mark request(Integer userId, Integer filmId, Double score) {
        Mark mark = new Mark();
        mark.setUserId(userId);
        mark.setFilmId(filmId);
        mark.setScore(score);
        mark.setComment("很好看");
        return mark;
    }

    @Test
    void addShouldInsertAndRecalculateFilmScore() {
        Mark mark = request(6, 10, 9.5);
        givenPickedUpTicket(6, 10);
        when(markMapper.countByUserAndFilm(6, 10)).thenReturn(0);

        markService.add(mark);

        verify(markMapper).insert(mark);
        // 评价写入后必须回写影片均分，否则评分榜与评价继续脱节
        verify(filmMapper).recalculateScore(10);
    }

    @Test
    void addWithoutFilmIdShouldThrow() {
        Mark mark = request(6, null, 9.5);

        assertThrows(CustomException.class, () -> markService.add(mark));
        verify(markMapper, never()).insert(any());
    }

    @Test
    void addWithoutUserIdShouldThrow() {
        Mark mark = request(null, 10, 9.5);

        assertThrows(CustomException.class, () -> markService.add(mark));
        verify(markMapper, never()).insert(any());
    }

    @Test
    void addWithoutScoreShouldThrow() {
        Mark mark = request(6, 10, null);

        assertThrows(CustomException.class, () -> markService.add(mark));
        verify(markMapper, never()).insert(any());
    }

    @Test
    void addWithScoreAboveMaxShouldThrow() {
        Mark mark = request(6, 10, 10.1);

        assertThrows(CustomException.class, () -> markService.add(mark));
        verify(markMapper, never()).insert(any());
    }

    @Test
    void addWithNegativeScoreShouldThrow() {
        Mark mark = request(6, 10, -0.1);

        assertThrows(CustomException.class, () -> markService.add(mark));
        verify(markMapper, never()).insert(any());
    }

    @Test
    void addShouldAcceptBoundaryScores() {
        Mark lowest = request(6, 10, 0.0);
        givenPickedUpTicket(6, 10);
        when(markMapper.countByUserAndFilm(6, 10)).thenReturn(0);
        markService.add(lowest);

        Mark highest = request(7, 10, 10.0);
        givenPickedUpTicket(7, 10);
        when(markMapper.countByUserAndFilm(7, 10)).thenReturn(0);
        markService.add(highest);

        verify(markMapper, times(2)).insert(any(Mark.class));
    }

    @Test
    void addDuplicateReviewBySameUserShouldThrow() {
        Mark mark = request(6, 10, 9.5);
        givenPickedUpTicket(6, 10);
        when(markMapper.countByUserAndFilm(6, 10)).thenReturn(1);

        assertThrows(CustomException.class, () -> markService.add(mark));
        verify(markMapper, never()).insert(any());
        verify(filmMapper, never()).recalculateScore(anyInt());
    }

    /**
     * 评价资格的服务端门禁：此前这条规则只有前端按钮在守（front/Orders.vue 仅对
     * 已取票订单渲染「去评价」），直接 POST /api/v1/marks 能给没买过票的影片打分。
     */
    @Test
    void addWithoutPickedUpTicketShouldThrow() {
        Mark mark = request(6, 10, 9.5);
        when(orderedMapper.countPickedUpByUserAndFilm(6, 10, OrderStatusCriteria.INSTANCE)).thenReturn(0);

        assertThrows(CustomException.class, () -> markService.add(mark));
        verify(markMapper, never()).insert(any());
        verify(filmMapper, never()).recalculateScore(anyInt());
    }

    /** 门禁按"该片是否取过票"判定，不看别的影片的票 —— 取过 A 片不能评 B 片 */
    @Test
    void addWithPickedUpTicketForAnotherFilmShouldThrow() {
        Mark mark = request(6, 11, 9.5);
        when(orderedMapper.countPickedUpByUserAndFilm(6, 11, OrderStatusCriteria.INSTANCE)).thenReturn(0);

        assertThrows(CustomException.class, () -> markService.add(mark));
        verify(markMapper, never()).insert(any());
    }

    @Test
    void addWithOverlongCommentShouldThrow() {
        Mark mark = request(6, 10, 9.5);
        mark.setComment("评".repeat(256));

        assertThrows(CustomException.class, () -> markService.add(mark));
        verify(markMapper, never()).insert(any());
    }

    @Test
    void updateWithMissingMarkShouldThrow() {
        when(markMapper.selectById(99)).thenReturn(null);
        Mark mark = new Mark();
        mark.setId(99);

        assertThrows(CustomException.class, () -> markService.update(mark));
        verify(markMapper, never()).updateById(any());
    }

    @Test
    void updateShouldDropIdentityAndRecalculateStoredFilm() {
        Mark stored = new Mark();
        stored.setId(5);
        stored.setUserId(6);
        stored.setFilmId(10);
        when(markMapper.selectById(5)).thenReturn(stored);

        Mark request = new Mark();
        request.setId(5);
        request.setScore(8.0);
        request.setComment("改一下");
        request.setUserId(7);   // 试图把评价转到别人名下
        request.setFilmId(99);  // 试图把评价转到别的影片

        markService.update(request);

        assertNull(request.getUserId());
        assertNull(request.getFilmId());
        verify(markMapper).updateById(request);
        // 按库中原影片重算，而不是请求体里的 99
        verify(filmMapper).recalculateScore(10);
    }

    @Test
    void deleteShouldRecalculateFilmScore() {
        Mark stored = new Mark();
        stored.setId(5);
        stored.setFilmId(10);
        when(markMapper.selectById(5)).thenReturn(stored);

        markService.delete(5);

        verify(markMapper).deleteById(5);
        verify(filmMapper).recalculateScore(10);
    }

    @Test
    void deleteBatchShouldRecalculateEachAffectedFilmOnce() {
        Mark first = new Mark();
        first.setId(1);
        first.setFilmId(10);
        Mark second = new Mark();
        second.setId(2);
        second.setFilmId(10);
        Mark third = new Mark();
        third.setId(3);
        third.setFilmId(11);
        when(markMapper.selectById(1)).thenReturn(first);
        when(markMapper.selectById(2)).thenReturn(second);
        when(markMapper.selectById(3)).thenReturn(third);

        markService.deleteBatch(List.of(1, 2, 3));

        verify(markMapper).deleteBatch(List.of(1, 2, 3));
        verify(filmMapper, times(1)).recalculateScore(10);
        verify(filmMapper, times(1)).recalculateScore(11);
    }

    // ==================== 点赞 / 取消点赞 ====================
    // 注意：这些用例只能钉住"调了哪条语句、返回了什么"，SQL 本身（相关子查询聚合赞数、
    // 唯一键去重）由阶段 4 的真实库脚本验证，Mockito 打桩后测的是桩而不是谓词。

    private Mark storedMark(Integer id, Integer userId, Integer filmId) {
        Mark mark = new Mark();
        mark.setId(id);
        mark.setUserId(userId);
        mark.setFilmId(filmId);
        return mark;
    }

    private Film storedFilm(Integer id) {
        Film film = new Film();
        film.setId(id);
        return film;
    }

    @Test
    void setLikeWhenNotYetLikedShouldInsertAndReportReadBackState() {
        when(markMapper.selectById(5)).thenReturn(storedMark(5, 6, 10));
        when(markLikeMapper.countByMarkAndUser(5, 6)).thenReturn(1);
        when(markLikeMapper.countByMarkId(5)).thenReturn(1);

        MarkLikeView result = markService.setLike(5, 6, true);

        verify(markLikeMapper).insertIfAbsent(5, 6);
        verify(markLikeMapper, never()).deleteByMarkAndUser(anyInt(), anyInt());
        // 返回值是回读的权威状态，不是"假定生效"
        assertTrue(result.getLiked());
        assertEquals(1, result.getLikeCount());
    }

    /**
     * 去重靠主键而不是代码：已赞过时 insertIfAbsent 依旧被调用（受影响行数为 0），
     * 赞数不变。这正是"回读权威状态"而非用返回值判断的原因。
     */
    @Test
    void setLikeWhenAlreadyLikedShouldStayLikedWithUnchangedCount() {
        when(markMapper.selectById(5)).thenReturn(storedMark(5, 6, 10));
        when(markLikeMapper.countByMarkAndUser(5, 6)).thenReturn(1);
        when(markLikeMapper.countByMarkId(5)).thenReturn(1);

        MarkLikeView result = markService.setLike(5, 6, true);

        verify(markLikeMapper).insertIfAbsent(5, 6);
        assertTrue(result.getLiked());
        assertEquals(1, result.getLikeCount());
    }

    @Test
    void setLikeUnlikeShouldDeleteAndReportZero() {
        when(markMapper.selectById(5)).thenReturn(storedMark(5, 6, 10));
        when(markLikeMapper.countByMarkAndUser(5, 6)).thenReturn(0);
        when(markLikeMapper.countByMarkId(5)).thenReturn(0);

        MarkLikeView result = markService.setLike(5, 6, false);

        verify(markLikeMapper).deleteByMarkAndUser(5, 6);
        verify(markLikeMapper, never()).insertIfAbsent(anyInt(), anyInt());
        assertFalse(result.getLiked());
        assertEquals(0, result.getLikeCount());
    }

    /** 取消一个从未点过的赞：删除影响 0 行，不报错，如实回报未赞 */
    @Test
    void setLikeUnlikeNeverLikedShouldNotThrow() {
        when(markMapper.selectById(5)).thenReturn(storedMark(5, 6, 10));
        when(markLikeMapper.countByMarkAndUser(5, 6)).thenReturn(0);

        MarkLikeView result = markService.setLike(5, 6, false);

        verify(markLikeMapper).deleteByMarkAndUser(5, 6);
        assertFalse(result.getLiked());
        assertEquals(0, result.getLikeCount());
    }

    @Test
    void setLikeWithMissingMarkShouldThrowNotFound() {
        when(markMapper.selectById(99)).thenReturn(null);

        CustomException ex = assertThrows(CustomException.class, () -> markService.setLike(99, 6, true));

        assertEquals(ErrorCode.NOT_FOUND.code(), ex.getCode());
        verifyNoInteractions(markLikeMapper);
    }

    // ==================== 按影片查询评价 ====================

    @Test
    void listByFilmAnonymousShouldQueryWithoutViewerAndSkipEligibility() {
        when(filmMapper.selectById(10)).thenReturn(storedFilm(10));
        when(markMapper.selectFilmMarks(10, null)).thenReturn(List.of(new MarkView()));

        FilmMarksView view = markService.listByFilm(10, null, null, 1, 10);

        verify(markMapper).selectFilmMarks(10, null);
        verify(markMapper, never()).selectFilmMarkOfUser(anyInt(), any());
        assertNull(view.getMy());
        assertFalse(view.getReviewable());
        // 匿名既无"我的评价"也无"我是否够格"，不该白跑一次已取票统计
        verifyNoInteractions(orderedMapper);
    }

    /** 非 USER 角色没有用户身份，按匿名口径查，同样不碰已取票统计 */
    @Test
    void listByFilmByCinemaRoleShouldBeTreatedAsAnonymous() {
        when(filmMapper.selectById(10)).thenReturn(storedFilm(10));
        when(markMapper.selectFilmMarks(10, null)).thenReturn(List.of());

        FilmMarksView view = markService.listByFilm(10, 7, RoleEnum.CINEMA.name(), 1, 10);

        verify(markMapper).selectFilmMarks(10, null);
        assertNull(view.getMy());
        assertFalse(view.getReviewable());
        verifyNoInteractions(orderedMapper);
    }

    @Test
    void listByFilmByUserWithoutPickedTicketShouldNotBeReviewable() {
        when(filmMapper.selectById(10)).thenReturn(storedFilm(10));
        when(markMapper.selectFilmMarks(10, 6)).thenReturn(List.of());
        when(markMapper.selectFilmMarkOfUser(10, 6)).thenReturn(null);
        when(orderedMapper.countPickedUpByUserAndFilm(6, 10, OrderStatusCriteria.INSTANCE)).thenReturn(0);

        FilmMarksView view = markService.listByFilm(10, 6, RoleEnum.USER.name(), 1, 10);

        verify(markMapper).selectFilmMarks(10, 6);
        assertFalse(view.getReviewable());
        assertNull(view.getMy());
    }

    @Test
    void listByFilmByUserWithPickedTicketAndReviewShouldExposeOwnReview() {
        when(filmMapper.selectById(10)).thenReturn(storedFilm(10));
        MarkView mine = new MarkView();
        mine.setId(5);
        when(markMapper.selectFilmMarks(10, 6)).thenReturn(List.of(mine));
        when(markMapper.selectFilmMarkOfUser(10, 6)).thenReturn(mine);
        when(orderedMapper.countPickedUpByUserAndFilm(6, 10, OrderStatusCriteria.INSTANCE)).thenReturn(1);

        FilmMarksView view = markService.listByFilm(10, 6, RoleEnum.USER.name(), 1, 10);

        assertTrue(view.getReviewable());
        assertNotNull(view.getMy());
        assertEquals(1, view.getTotal());
    }

    @Test
    void listByFilmWithMissingFilmShouldThrowNotFound() {
        when(filmMapper.selectById(99)).thenReturn(null);

        CustomException ex = assertThrows(CustomException.class,
                () -> markService.listByFilm(99, 6, RoleEnum.USER.name(), 1, 10));

        assertEquals(ErrorCode.NOT_FOUND.code(), ex.getCode());
        verifyNoInteractions(markMapper);
    }

    @Test
    void listByFilmWithoutFilmIdShouldThrowParamInvalid() {
        CustomException ex = assertThrows(CustomException.class,
                () -> markService.listByFilm(null, 6, RoleEnum.USER.name(), 1, 10));

        assertEquals(ErrorCode.PARAM_INVALID.code(), ex.getCode());
        verifyNoInteractions(filmMapper, markMapper, orderedMapper);
    }

    /**
     * 回归守卫：投影不得下发作者 id，否则前端能自己比对归属 ——
     * 归属判定必须在后端完成，以 mine 布尔量下发。
     */
    @Test
    void markViewShouldNotSerializeAuthorUserId() throws Exception {
        MarkView view = new MarkView(5, "张三", "avatar.png", 9.5, "很好看", 3, true, false);

        String json = new ObjectMapper().writeValueAsString(view);

        assertFalse(json.contains("\"userId\""), "投影不得包含 userId");
        assertTrue(json.contains("\"mine\""));
        assertTrue(json.contains("\"liked\""));
    }
}
