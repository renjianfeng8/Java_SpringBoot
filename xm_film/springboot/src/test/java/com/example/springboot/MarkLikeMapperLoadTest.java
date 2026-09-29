package com.example.springboot;

import com.example.springboot.mapper.MarkLikeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 绑定层守卫：mvn compile 不解析 mapper XML，命名空间 / 语句 id / #{参数名} 写错都能编译通过，
 * 只有启动 Spring 上下文并真正执行语句时才会暴露。这里逐条跑一遍 MarkLikeMapper 的四条语句。
 * <p>
 * 全部传不存在的 id(-1)：既能证明绑定有效，又保证不写入/不删除任何真实数据，可反复运行、不留痕。
 */
@SpringBootTest
@ActiveProfiles("ci")
class MarkLikeMapperLoadTest {

    @Autowired
    MarkLikeMapper markLikeMapper;

    @Test
    void mapperLoadsCountByMarkAndUserStatement() {
        assertThat(markLikeMapper.countByMarkAndUser(-1, -1)).isZero();
    }

    @Test
    void mapperLoadsCountByMarkIdStatement() {
        assertThat(markLikeMapper.countByMarkId(-1)).isZero();
    }

    @Test
    void mapperLoadsDeleteByMarkAndUserStatement() {
        assertThat(markLikeMapper.deleteByMarkAndUser(-1, -1)).isZero();
    }

    /**
     * 外键错误必须冒出来：mark_id = -1 在 mark 表里不存在，插入违反 mark_like 的 mark_id 外键约束。
     * 这是 Fix 1 的回归守卫 —— 若改回 INSERT IGNORE，错误会被降级成 ROW_COUNT()=0，
     * 本用例将因"没抛异常"而失败，正是要挡住的那种静默错误。
     */
    @Test
    void insertIfAbsentRaisesIntegrityErrorForMissingMark() {
        assertThatThrownBy(() -> markLikeMapper.insertIfAbsent(-1, -1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
