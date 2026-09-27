package com.example.springboot.service;

import com.example.springboot.entity.Mark;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.FilmMapper;
import com.example.springboot.mapper.MarkMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarkServiceTest {

    @Mock
    private MarkMapper markMapper;

    @Mock
    private FilmMapper filmMapper;

    @InjectMocks
    private MarkService markService;

    private Mark request(Integer userId, Integer filmId, Double score) {
        Mark mark = new Mark();
        mark.setUserId(userId);
        mark.setFilmId(filmId);
        mark.setScore(score);
        mark.setMark("很好看");
        return mark;
    }

    @Test
    void add_shouldInsertAndRecalculateFilmScore() {
        Mark mark = request(6, 10, 9.5);
        when(markMapper.countByUserAndFilm(6, 10)).thenReturn(0);

        markService.add(mark);

        verify(markMapper).insert(mark);
        // 评价写入后必须回写影片均分，否则评分榜与评价继续脱节
        verify(filmMapper).recalculateScore(10);
    }

    @Test
    void add_withoutFilmId_shouldThrow() {
        Mark mark = request(6, null, 9.5);

        assertThrows(CustomException.class, () -> markService.add(mark));
        verify(markMapper, never()).insert(any());
    }

    @Test
    void add_withoutUserId_shouldThrow() {
        Mark mark = request(null, 10, 9.5);

        assertThrows(CustomException.class, () -> markService.add(mark));
        verify(markMapper, never()).insert(any());
    }

    @Test
    void add_withoutScore_shouldThrow() {
        Mark mark = request(6, 10, null);

        assertThrows(CustomException.class, () -> markService.add(mark));
        verify(markMapper, never()).insert(any());
    }

    @Test
    void add_withScoreAboveMax_shouldThrow() {
        Mark mark = request(6, 10, 10.1);

        assertThrows(CustomException.class, () -> markService.add(mark));
        verify(markMapper, never()).insert(any());
    }

    @Test
    void add_withNegativeScore_shouldThrow() {
        Mark mark = request(6, 10, -0.1);

        assertThrows(CustomException.class, () -> markService.add(mark));
        verify(markMapper, never()).insert(any());
    }

    @Test
    void add_shouldAcceptBoundaryScores() {
        Mark lowest = request(6, 10, 0.0);
        when(markMapper.countByUserAndFilm(6, 10)).thenReturn(0);
        markService.add(lowest);

        Mark highest = request(7, 10, 10.0);
        when(markMapper.countByUserAndFilm(7, 10)).thenReturn(0);
        markService.add(highest);

        verify(markMapper, times(2)).insert(any(Mark.class));
    }

    @Test
    void add_duplicateReviewBySameUser_shouldThrow() {
        Mark mark = request(6, 10, 9.5);
        when(markMapper.countByUserAndFilm(6, 10)).thenReturn(1);

        assertThrows(CustomException.class, () -> markService.add(mark));
        verify(markMapper, never()).insert(any());
        verify(filmMapper, never()).recalculateScore(anyInt());
    }

    @Test
    void add_withOverlongComment_shouldThrow() {
        Mark mark = request(6, 10, 9.5);
        mark.setMark("评".repeat(256));

        assertThrows(CustomException.class, () -> markService.add(mark));
        verify(markMapper, never()).insert(any());
    }

    @Test
    void update_withMissingMark_shouldThrow() {
        when(markMapper.selectById(99)).thenReturn(null);
        Mark mark = new Mark();
        mark.setId(99);

        assertThrows(CustomException.class, () -> markService.update(mark));
        verify(markMapper, never()).updateById(any());
    }

    @Test
    void update_shouldDropIdentityAndRecalculateStoredFilm() {
        Mark stored = new Mark();
        stored.setId(5);
        stored.setUserId(6);
        stored.setFilmId(10);
        when(markMapper.selectById(5)).thenReturn(stored);

        Mark request = new Mark();
        request.setId(5);
        request.setScore(8.0);
        request.setMark("改一下");
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
    void delete_shouldRecalculateFilmScore() {
        Mark stored = new Mark();
        stored.setId(5);
        stored.setFilmId(10);
        when(markMapper.selectById(5)).thenReturn(stored);

        markService.delete(5);

        verify(markMapper).deleteById(5);
        verify(filmMapper).recalculateScore(10);
    }

    @Test
    void deleteBatch_shouldRecalculateEachAffectedFilmOnce() {
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
}
