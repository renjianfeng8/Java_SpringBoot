package com.example.springboot;

import com.example.springboot.mapper.OrderedMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import com.example.springboot.common.OrderStatusCriteria;

/** 只读守卫；仍走 ci profile，保证全仓 @SpringBootTest 只有一处数据源来源 */
@SpringBootTest
@ActiveProfiles("ci")
class OrderedSeatConflictTest {

    @Autowired
    OrderedMapper orderedMapper;

    @Test
    void mapperLoadsCountSeatInUseStatement() {
        int count = orderedMapper.countSeatInUse(-1, "1排1座", OrderStatusCriteria.INSTANCE);
        assertThat(count).isZero();
    }

    /** 座位占用列表必须带状态条件参数；缺 @Param("s") 时 XML 里的状态过滤会静默失效。 */
    @Test
    void mapperLoadsActiveSeatStatementWithStatusCriteria() {
        assertThat(orderedMapper.selectActiveByRecordId(-1, OrderStatusCriteria.INSTANCE)).isEmpty();
    }
}
