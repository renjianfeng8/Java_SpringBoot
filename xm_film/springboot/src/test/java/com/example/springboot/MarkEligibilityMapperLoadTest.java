package com.example.springboot;

import com.example.springboot.common.OrderStatusCriteria;
import com.example.springboot.mapper.OrderedMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 绑定层守卫：countPickedUpByUserAndFilm 的 XML 曾引用 {@code #{pickedUp}}，
 * 而方法签名只有 userId / filmId —— Mockito 单测永远不会碰到 XML，
 * 于是「评论区与发评价」在真实数据库上恒抛 BindingException（500）。
 * 这里真正执行一次语句，钉住「参数名与 XML 对得上」。
 *
 * <p>只读 COUNT，不写库；用不存在的用户 id，结果恒为 0。
 */
@SpringBootTest
@ActiveProfiles("ci")
class MarkEligibilityMapperLoadTest {

    @Autowired
    OrderedMapper orderedMapper;

    @Test
    void mapperLoadsCountPickedUpByUserAndFilmStatement() {
        assertThat(orderedMapper.countPickedUpByUserAndFilm(-1, -1, OrderStatusCriteria.INSTANCE)).isZero();
    }
}
