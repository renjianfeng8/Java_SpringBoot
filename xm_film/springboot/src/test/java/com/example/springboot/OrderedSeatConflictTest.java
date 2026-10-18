package com.example.springboot;

import com.example.springboot.mapper.OrderedMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** 只读守卫；仍走 ci profile，保证全仓 @SpringBootTest 只有一处数据源来源 */
@SpringBootTest
@ActiveProfiles("ci")
class OrderedSeatConflictTest {

    @Autowired
    OrderedMapper orderedMapper;

    @Test
    void mapperLoadsCountSeatInUseStatement() {
        int count = orderedMapper.countSeatInUse(-1, "1排1座");
        assertThat(count).isZero();
    }
}
