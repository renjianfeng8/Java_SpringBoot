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

    /** 带着无法解析的令牌访问时 preHandle 的结果（模拟令牌过期） */
    private boolean withBrokenToken(String path, String method) {
        AuthInterceptor interceptor = newInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.addHeader("Authorization", "Bearer not-a-real-token");
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

    // ========== Marks: 评价列表公开可读，写操作的角色与归属由 MarkController 校验 ==========

    @Test
    void anonymousCanReadMarks() {
        assertThat(anonymousAllowed("/api/v1/marks", "GET")).isTrue();
    }

    @Test
    void anonymousCannotWriteMarks() {
        assertThat(anonymousAllowed("/api/v1/marks", "POST")).isFalse();
    }

    @Test
    void interceptorLetsUserReachMarkWriteEndpoint() {
        // 拦截器只做前缀级判断，「仅 USER 可发表 / 非 ADMIN 只能改删自己的」在控制器内
        assertThat(hasAccess(newInterceptor(), "/api/v1/marks", "POST", "USER")).isTrue();
    }

    // ========== Cinemas: 读公开，写（新增/删除）在 CinemaController 内限管理员 ==========

    @Test
    void anonymousCanReadCinemas() {
        assertThat(anonymousAllowed("/api/v1/cinemas/page", "GET")).isTrue();
    }

    @Test
    void userCanReadCinemas() {
        assertThat(hasAccess(newInterceptor(), "/api/v1/cinemas", "GET", "USER")).isTrue();
    }

    // ========== 令牌过期：公开只读资源不应因此变成"必须登录" ==========

    @Test
    void brokenTokenStillReadsPublicResources() {
        assertThat(withBrokenToken("/api/v1/marks", "GET")).isTrue();
        assertThat(withBrokenToken("/api/v1/cinemas/page", "GET")).isTrue();
        assertThat(withBrokenToken("/api/v1/records/page", "GET")).isTrue();
    }

    @Test
    void brokenTokenCannotReadPrivateResources() {
        assertThat(withBrokenToken("/api/v1/orders/page", "GET")).isFalse();
        assertThat(withBrokenToken("/api/v1/admins", "GET")).isFalse();
    }

    @Test
    void brokenTokenCannotWritePublicResources() {
        assertThat(withBrokenToken("/api/v1/marks", "POST")).isFalse();
        assertThat(withBrokenToken("/api/v1/films", "POST")).isFalse();
    }

    // ========== 匿名写白名单：取票大厅核销是本仓库唯一的匿名写入口 ==========
    // 自助机不认识用户、只认取票码，所以核销必须免登录。放行面积被三处收窄，
    // 下面四个用例各钉住一处，任何一处放宽都会在这里失败。

    @Test
    void anonymousCanRedeemTicketCode() {
        assertThat(anonymousAllowed("/api/v1/tickets/redeem", "POST")).isTrue();
    }

    /** 精确匹配而非前缀：子路径不得被连带放行 */
    @Test
    void anonymousCannotReachSubPathOfRedeem() {
        assertThat(anonymousAllowed("/api/v1/tickets/redeem/extra", "POST")).isFalse();
        assertThat(anonymousAllowed("/api/v1/tickets/redeem-all", "POST")).isFalse();
    }

    /** 只放行 POST：同路径的其它方法不享受匿名放行 */
    @Test
    void anonymousCannotRedeemViaOtherMethods() {
        assertThat(anonymousAllowed("/api/v1/tickets/redeem", "GET")).isFalse();
        assertThat(anonymousAllowed("/api/v1/tickets/redeem", "PUT")).isFalse();
        assertThat(anonymousAllowed("/api/v1/tickets/redeem", "DELETE")).isFalse();
    }

    /** 放行的只有这一条路径，前缀下的其它端点照旧需要登录 */
    @Test
    void anonymousCannotReachOtherTicketEndpoints() {
        assertThat(anonymousAllowed("/api/v1/tickets", "POST")).isFalse();
        assertThat(anonymousAllowed("/api/v1/tickets/other", "POST")).isFalse();
    }

    /** 自助机上登录态早没了，带个过期令牌不该把这张码挡下来 */
    @Test
    void brokenTokenStillReachesTicketRedeem() {
        assertThat(withBrokenToken("/api/v1/tickets/redeem", "POST")).isTrue();
    }
}
