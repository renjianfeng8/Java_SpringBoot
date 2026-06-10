package com.example.springboot;

import com.example.springboot.entity.Ordered;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.FilmMapper;
import com.example.springboot.mapper.OrderedMapper;
import com.example.springboot.mapper.RecordMapper;
import com.example.springboot.service.OrderedService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = OrderedService.class)
class OrderedServiceTest {

    @Autowired
    OrderedService orderedService;

    @MockBean
    OrderedMapper orderedMapper;

    @MockBean
    RecordMapper recordMapper;

    @MockBean
    FilmMapper filmMapper;

    @Test
    void userCannotCancelAnotherUsersOrder() {
        Ordered ordered = new Ordered();
        ordered.setId(1);
        ordered.setUserId(100);
        ordered.setStatus("待取票");
        when(orderedMapper.selectById(1)).thenReturn(ordered);

        assertThatThrownBy(() -> orderedService.cancelOrder(1, "USER", 200))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("无权");
    }

    @Test
    void cancelledOrderCannotBePickedUp() {
        Ordered ordered = new Ordered();
        ordered.setId(1);
        ordered.setCinemaId(10);
        ordered.setStatus("已取消");
        when(orderedMapper.selectById(1)).thenReturn(ordered);

        assertThatThrownBy(() -> orderedService.pickupOrder(1, "CINEMA", 10))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("状态");
    }
}
