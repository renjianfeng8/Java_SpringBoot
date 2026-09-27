package com.example.springboot;

import com.example.springboot.common.JwtUtils;
import com.example.springboot.common.config.AuthInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
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

    /** 匿名（无 token）请求是否被放行，覆盖 preHandle 的白名单分支 */
    private boolean anonymousAllowed(String path, String method) {
        AuthInterceptor interceptor = newInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        try {
            return interceptor.preHandle(request, response, new Object());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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

    // ========== Anonymous read allowlist (preHandle) ==========

    @Test
    void anonymousCanReadRecords() {
        assertThat(anonymousAllowed("/api/v1/records/page", "GET")).isTrue();
    }

    @Test
    void anonymousCanReadRecordById() {
        assertThat(anonymousAllowed("/api/v1/records/1", "GET")).isTrue();
    }

    @Test
    void anonymousCannotWriteRecords() {
        assertThat(anonymousAllowed("/api/v1/records", "POST")).isFalse();
    }

    @Test
    void anonymousCannotReadOrders() {
        assertThat(anonymousAllowed("/api/v1/orders/page", "GET")).isFalse();
    }
}
