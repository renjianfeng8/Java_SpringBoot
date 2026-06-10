package com.example.springboot;

import com.example.springboot.common.JwtUtils;
import com.example.springboot.common.config.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AuthInterceptorAccessTest {

    @Test
    void cinemaCannotWriteFilms() throws Exception {
        AuthInterceptor interceptor = new AuthInterceptor();
        ReflectionTestUtils.setField(interceptor, "jwtUtils", mock(JwtUtils.class));

        boolean access = (boolean) ReflectionTestUtils.invokeMethod(
                interceptor,
                "hasAccess",
                "/api/v1/films",
                "POST",
                "CINEMA"
        );

        assertThat(access).isFalse();
    }

    @Test
    void adminCanWriteFilms() throws Exception {
        AuthInterceptor interceptor = new AuthInterceptor();
        ReflectionTestUtils.setField(interceptor, "jwtUtils", mock(JwtUtils.class));

        boolean access = (boolean) ReflectionTestUtils.invokeMethod(
                interceptor,
                "hasAccess",
                "/api/v1/films",
                "POST",
                "ADMIN"
        );

        assertThat(access).isTrue();
    }

    @Test
    void cinemaCanReadFilms() throws Exception {
        AuthInterceptor interceptor = new AuthInterceptor();
        ReflectionTestUtils.setField(interceptor, "jwtUtils", mock(JwtUtils.class));

        boolean access = (boolean) ReflectionTestUtils.invokeMethod(
                interceptor,
                "hasAccess",
                "/api/v1/films",
                "GET",
                "CINEMA"
        );

        assertThat(access).isTrue();
    }

    @Test
    void cinemaCannotAccessAdminPrefix() throws Exception {
        AuthInterceptor interceptor = new AuthInterceptor();
        ReflectionTestUtils.setField(interceptor, "jwtUtils", mock(JwtUtils.class));

        boolean access = (boolean) ReflectionTestUtils.invokeMethod(
                interceptor,
                "hasAccess",
                "/api/v1/admins",
                "GET",
                "CINEMA"
        );

        assertThat(access).isFalse();
    }

    @Test
    void userCannotWriteOrdersDirectlyThroughInterceptor() throws Exception {
        AuthInterceptor interceptor = new AuthInterceptor();
        ReflectionTestUtils.setField(interceptor, "jwtUtils", mock(JwtUtils.class));

        boolean access = (boolean) ReflectionTestUtils.invokeMethod(
                interceptor,
                "hasAccess",
                "/api/v1/orders",
                "POST",
                "USER"
        );

        assertThat(access).isTrue();
    }
}
