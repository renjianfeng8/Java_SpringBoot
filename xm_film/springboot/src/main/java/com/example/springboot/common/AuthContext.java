package com.example.springboot.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 取当前请求的登录态（AuthInterceptor 解析 JWT 后写入的 request 属性）。
 *
 * 注意：{@code WebMvcConfig.excludePathPatterns} 排除的路径不经过 AuthInterceptor，
 * 这两个属性为空，控制器里的角色判断会静默失效（见 Bug.md BUG-036）。
 * 公开访问统一交给 {@code AuthInterceptor.PUBLIC_READ_PREFIXES}，不要往排除表里加路径。
 */
public final class AuthContext {

    private AuthContext() {}

    /** 当前角色（ADMIN / CINEMA / USER），未登录或被排除时为空 */
    public static String role() {
        HttpServletRequest request = request();
        return request == null ? null : (String) request.getAttribute("role");
    }

    /** 当前登录主体 ID，未登录或被排除时为空 */
    public static Integer userId() {
        HttpServletRequest request = request();
        if (request == null) {
            return null;
        }
        String userId = (String) request.getAttribute("userId");
        return userId == null ? null : Integer.valueOf(userId);
    }

    private static HttpServletRequest request() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
