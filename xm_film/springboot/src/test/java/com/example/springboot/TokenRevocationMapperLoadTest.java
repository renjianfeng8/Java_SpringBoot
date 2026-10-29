package com.example.springboot;

import com.example.springboot.common.JwtUtils;
import com.example.springboot.mapper.AdminMapper;
import com.example.springboot.mapper.CinemaMapper;
import com.example.springboot.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 令牌吊销点的两条 SQL 必须真的绑得上。
 *
 * <p>此前接口方法加了 {@code selectRevokedBefore/updateRevokedBefore}，XML 里却没写语句：
 * MyBatis 在每次解析令牌时抛 {@code Invalid bound statement}，被 AuthInterceptor 兜成 401，
 * 表现为「登录成功后任何需要登录的接口都把人踢回登录页」。JwtUtilsTest 用桩服务替掉了吊销查询，
 * 所以单测全绿也发现不了 —— 这里用真 JwtUtils + 真 Mapper 把这条链路走一遍。
 *
 * <p>探测 id 用 -1（账号不存在）：查询返回 null、更新影响 0 行，不需要造数据，也不污染库。
 */
@SpringBootTest
@ActiveProfiles("ci")
@Transactional
class TokenRevocationMapperLoadTest {

    private static final int ABSENT_ID = -1;

    @Autowired
    UserMapper userMapper;

    @Autowired
    AdminMapper adminMapper;

    @Autowired
    CinemaMapper cinemaMapper;

    @Autowired
    JwtUtils jwtUtils;

    @Test
    void revocationStatementsAreBoundForAllThreeRoles() {
        assertThat(userMapper.selectRevokedBefore(ABSENT_ID)).isNull();
        assertThat(userMapper.updateRevokedBefore(ABSENT_ID, 0L)).isZero();
        assertThat(adminMapper.selectRevokedBefore(ABSENT_ID)).isNull();
        assertThat(adminMapper.updateRevokedBefore(ABSENT_ID, 0L)).isZero();
        assertThat(cinemaMapper.selectRevokedBefore(ABSENT_ID)).isNull();
        assertThat(cinemaMapper.updateRevokedBefore(ABSENT_ID, 0L)).isZero();
    }

    @Test
    void freshlyIssuedTokenParsesThroughRealRevocationLookup() {
        for (String role : new String[]{"USER", "ADMIN", "CINEMA"}) {
            String token = jwtUtils.generateToken(ABSENT_ID, role);

            assertThat(jwtUtils.parseToken(token).getSubject()).isEqualTo(String.valueOf(ABSENT_ID));
        }
    }
}
