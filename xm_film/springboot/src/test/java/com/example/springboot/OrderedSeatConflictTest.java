package com.example.springboot;

import com.example.springboot.mapper.OrderedMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderedSeatConflictTest {

    @Autowired
    OrderedMapper orderedMapper;

    @Test
    void mapperLoadsCountSeatInUseStatement() {
        int count = orderedMapper.countSeatInUse(-1, "1排1座");
        assertThat(count).isZero();
    }
}
