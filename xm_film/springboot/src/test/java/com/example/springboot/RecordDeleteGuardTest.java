package com.example.springboot;

import com.example.springboot.entity.Record;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.FilmMapper;
import com.example.springboot.mapper.RecordMapper;
import com.example.springboot.mapper.RoomMapper;
import com.example.springboot.service.OrderedService;
import com.example.springboot.service.RecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BL-02 守卫：删除排片必须<b>先锁排片行</b>，再数订单。
 *
 * <p>背景（2026-10-18 实测）：删除路径原先是 {@code selectById}（非锁定读）+
 * 非锁定 COUNT，然后 DELETE。管理员数到 0 笔后、DELETE 前用户下单并提交，
 * 外键 {@code ordered_ibfk_1 ON DELETE RESTRICT} 挡住删除，但异常漏成 500
 * 「系统错误」——实测 25 轮里 21 次撞上竞态，其中 19 次返回「系统错误」。
 *
 * <p><b>澄清</b>：订单不会悬空（外键 RESTRICT 保证）。真正的缺陷是「管理员拿到 500
 * 而不是可操作提示」，以及「本可正确判定的窗口被浪费」。
 *
 * <p><b>为什么用 Mockito 而不是 @SpringBootTest</b>：本类要断言「删除路径调用了哪个
 * mapper 方法」。真实数据库只能验证「有订单就拒绝」——串行下无论锁不锁都成立，
 * 实测把锁去掉后那种测试依然全绿，它没有覆盖锁定行为。这里直接盯调用序列。
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("java:S5778") // 测试 lambda 内除被测调用外还有 helper 调用，规则只影响可读性
class RecordDeleteGuardTest {

    @Mock
    RecordMapper recordMapper;

    @Mock
    FilmMapper filmMapper;

    @Mock
    RoomMapper roomMapper;

    @Mock
    OrderedService orderedService;

    private RecordService recordService;

    @BeforeEach
    void setUp() {
        recordService = new RecordService(recordMapper, filmMapper, roomMapper, orderedService);
    }

    private Record recordOf(int id) {
        Record r = new Record();
        r.setId(id);
        r.setCinemaId(1);
        r.setRoomId(1);
        return r;
    }

    /** 删除路径必须执行锁定读，且不得退回到非锁定的 selectById */
    @Test
    void deleteScopedLocksRecordRow() {
        when(recordMapper.selectByIdForUpdate(7)).thenReturn(recordOf(7));
        when(orderedService.countByRecordId(7)).thenReturn(0);

        recordService.deleteScoped(7, "ADMIN", null);

        verify(recordMapper).selectByIdForUpdate(7);
        verify(recordMapper, never()).selectById(any());
        verify(recordMapper).deleteById(7);
    }

    /** 批量删除同样逐条加锁 */
    @Test
    void deleteBatchScopedLocksEachRow() {
        when(recordMapper.selectByIdForUpdate(7)).thenReturn(recordOf(7));
        when(recordMapper.selectByIdForUpdate(8)).thenReturn(recordOf(8));
        when(orderedService.countByRecordId(any())).thenReturn(0);

        recordService.deleteBatchScoped(List.of(7, 8), "ADMIN", null);

        verify(recordMapper).selectByIdForUpdate(7);
        verify(recordMapper).selectByIdForUpdate(8);
        verify(recordMapper, never()).selectById(any());
    }

    /** 已有订单的排片：抛业务异常，且绝不执行删除 */
    @Test
    void recordWithOrdersIsRejected() {
        when(recordMapper.selectByIdForUpdate(7)).thenReturn(recordOf(7));
        when(orderedService.countByRecordId(7)).thenReturn(3);

        assertThatThrownBy(() -> recordService.deleteScoped(7, "ADMIN", null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("订单");

        verify(recordMapper, never()).deleteById(any());
    }

    /** 批量删除时被引用的行必须点名，不能只说「存在订单」 */
    @Test
    void deleteBatchScopedReportsWhichRowsAreBlocked() {
        Record blocked = recordOf(7);
        blocked.setTitle("测试场次");
        when(recordMapper.selectByIdForUpdate(7)).thenReturn(blocked);
        when(recordMapper.selectByIdForUpdate(8)).thenReturn(recordOf(8));
        when(orderedService.countByRecordId(7)).thenReturn(2);
        when(orderedService.countByRecordId(8)).thenReturn(0);

        assertThatThrownBy(() -> recordService.deleteBatchScoped(List.of(7, 8), "ADMIN", null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("整批未删除")
                .hasMessageContaining("排片 #7")
                .hasMessageContaining("测试场次");

        verify(recordMapper, never()).deleteBatch(any());
    }

    /** 排片不存在时给 404，而不是空指针 */
    @Test
    void missingRecordIsNotFound() {
        when(recordMapper.selectByIdForUpdate(99)).thenReturn(null);

        assertThatThrownBy(() -> recordService.deleteScoped(99, "ADMIN", null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("排片不存在");
    }
}
