package com.example.springboot;

import com.example.springboot.common.JwtUtils;
import com.example.springboot.common.config.AuthInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AuthInterceptorAccessTest {

    private AuthInterceptor newInterceptor() {
        AuthInterceptor interceptor = new AuthInterceptor();
        ReflectionTestUtils.setField(interceptor, "jwtUtils", mock(JwtUtils.class));
        return interceptor;
    }

    private boolean hasAccess(AuthInterceptor interceptor, String path, String method, String role) {
        return (boolean) ReflectionTestUtils.invokeMethod(
                interceptor, "hasAccess", path, method, role);
    }

    // ========== Film write protection ==========

    @Test
    void cinemaCannotWriteFilms() {
        assertThat(hasAccess(newInterceptor(), "/api/v1/films", "POST", "CINEMA")).isFalse();
    }

    @Test
    void adminCanWriteFilms() {
        assertThat(hasAccess(newInterceptor(), "/api/v1/films", "POST", "ADMIN")).isTrue();
    }

    @Test
    void cinemaCanReadFilms() {
        assertThat(hasAccess(newInterceptor(), "/api/v1/films", "GET", "CINEMA")).isTrue();
    }

    // ========== Admin-only resources ==========

    @Test
    void cinemaCannotAccessAdminPrefix() {
        assertThat(hasAccess(newInterceptor(), "/api/v1/admins", "GET", "CINEMA")).isFalse();
    }

    @Test
    void userCannotAccessAdminPrefix() {
        assertThat(hasAccess(newInterceptor(), "/api/v1/admins", "GET", "USER")).isFalse();
    }

    // ========== Read-only for non-ADMIN: actors, areas, types, notices, videos ==========

    @Test
    void cinemaCanReadActors() {
        assertThat(hasAccess(newInterceptor(), "/api/v1/actors", "GET", "CINEMA")).isTrue();
    }

    @Test
    void cinemaCannotWriteActors() {
        assertThat(hasAccess(newInterceptor(), "/api/v1/actors", "POST", "CINEMA")).isFalse();
    }

    @Test
    void cinemaCanReadAreas() {
        assertThat(hasAccess(newInterceptor(), "/api/v1/areas", "GET", "CINEMA")).isTrue();
    }

    @Test
    void cinemaCannotWriteAreas() {
        assertThat(hasAccess(newInterceptor(), "/api/v1/areas", "PUT", "CINEMA")).isFalse();
    }

    @Test
    void cinemaCanReadTypes() {
        assertThat(hasAccess(newInterceptor(), "/api/v1/types", "GET", "CINEMA")).isTrue();
    }

    @Test
    void cinemaCannotWriteTypes() {
        assertThat(hasAccess(newInterceptor(), "/api/v1/types", "POST", "CINEMA")).isFalse();
    }

    @Test
    void cinemaCanReadNotices() {
        assertThat(hasAccess(newInterceptor(), "/api/v1/notices", "GET", "CINEMA")).isTrue();
    }

    @Test
    void cinemaCannotWriteNotices() {
        assertThat(hasAccess(newInterceptor(), "/api/v1/notices", "DELETE", "CINEMA")).isFalse();
    }

    @Test
    void cinemaCanReadVideos() {
        assertThat(hasAccess(newInterceptor(), "/api/v1/videos", "GET", "CINEMA")).isTrue();
    }

    @Test
    void cinemaCannotWriteVideos() {
        assertThat(hasAccess(newInterceptor(), "/api/v1/videos", "POST", "CINEMA")).isFalse();
    }

    // ========== USER read access ==========

    @Test
    void userCanReadActors() {
        assertThat(hasAccess(newInterceptor(), "/api/v1/actors", "GET", "USER")).isTrue();
    }

    @Test
    void userCannotWriteActors() {
        assertThat(hasAccess(newInterceptor(), "/api/v1/actors", "POST", "USER")).isFalse();
    }

    // ========== Order endpoint (service-level auth) ==========

    @Test
    void interceptorAllowsUserPostOrders() {
        assertThat(hasAccess(newInterceptor(), "/api/v1/orders", "POST", "USER")).isTrue();
    }
}
