package com.example.springboot;

import com.example.springboot.entity.User;
import com.example.springboot.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 状态词表的库级兜底：Java 常量与前端下拉都对得上，但直连 SQL 仍能写任意字符串。
 * 这里确认 {@code schema.sql} 里的 CHECK 真的建到了测试库上（快照与库不同步时变红）。
 *
 * <p>只覆盖最容易漂移的一条（{@code user.role}）；其余 CHECK 由同一份快照生成。
 * {@code @Transactional} 让两笔写入都回滚，不留脏行。
 */
@SpringBootTest
@ActiveProfiles("ci")
@Transactional
class StatusConstraintLoadTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    void unknownStatusValueIsRejectedByDatabase() {
        User bad = new User();
        bad.setUsername("chk-" + UUID.randomUUID());
        bad.setPassword("unused");
        bad.setRole("HACKER");

        assertThatThrownBy(() -> userMapper.insert(bad))
                .isInstanceOf(org.springframework.dao.DataAccessException.class)
                .hasMessageContaining("chk_user_role");
    }

    @Test
    void knownStatusValueIsAccepted() {
        User ok = new User();
        ok.setUsername("chk-" + UUID.randomUUID());
        ok.setPassword("unused");
        ok.setRole("USER");

        userMapper.insert(ok);

        assertThat(ok.getId()).isNotNull();
    }
}
