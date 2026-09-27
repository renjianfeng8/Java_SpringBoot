package com.example.springboot.common.config;

import com.example.springboot.common.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Set;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    private static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";

    private static final Set<String> ADMIN_ONLY_PREFIXES = Set.of(
            "/api/v1/admins"
    );

    private static final Set<String> ADMIN_WRITE_PREFIXES = Set.of(
            "/api/v1/films",
            "/api/v1/actors",
            "/api/v1/areas",
            "/api/v1/types",
            "/api/v1/notices",
            "/api/v1/videos"
    );

    private static final Set<String> PUBLIC_READ_PREFIXES = Set.of(
            "/api/v1/films",
            "/api/v1/cinemas",
            "/api/v1/types",
            "/api/v1/areas",
            "/api/v1/notices",
            "/api/v1/actors",
            // 影院详情的放映场次列表需匿名可读，否则公开页会 401
            "/api/v1/records",
            // 影片详情页的评价列表是公开内容，需匿名可读
            "/api/v1/marks"
    );

    @Resource
    private JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");

        // Allow anonymous GET requests to public resources
        if (token == null) {
            if (isAnonymousRead(request)) {
                return true;
            }
            writeUnauthorized(response);
            return false;
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                Claims claims = jwtUtils.parseToken(token);
                String role = claims.get("role", String.class);
                request.setAttribute("userId", claims.getSubject());
                request.setAttribute("role", role);

                if (!hasAccess(request.getRequestURI(), request.getMethod(), role)) {
                    writeForbidden(response);
                    return false;
                }
                return true;
            } catch (Exception e) {
                log.warn("JWT token parsing failed: {}", e.getMessage());
            }
        }

        // 令牌失效/过期时，公开只读资源仍按匿名放行。否则游客带着过期令牌浏览公开页会被判 401，
        // 而前端 401 处理会把人踢去登录页 —— 公开内容就变成了事实上的必须登录。
        if (isAnonymousRead(request)) {
            return true;
        }

        writeUnauthorized(response);
        return false;
    }

    /** 匿名可读判定：GET + 命中公开只读前缀 */
    private boolean isAnonymousRead(HttpServletRequest request) {
        return "GET".equalsIgnoreCase(request.getMethod())
                && PUBLIC_READ_PREFIXES.stream().anyMatch(p -> request.getRequestURI().startsWith(p));
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(401);
        response.setContentType(JSON_CONTENT_TYPE);
        response.getWriter().write("{\"code\":\"401\",\"msg\":\"登录已过期，请重新登录\"}");
    }

    private boolean hasAccess(String path, String method, String role) {
        if ("ADMIN".equals(role)) {
            return true;
        }
        if (ADMIN_ONLY_PREFIXES.stream().anyMatch(path::startsWith)) {
            return false;
        }
        return !isWriteMethod(method) || ADMIN_WRITE_PREFIXES.stream().noneMatch(path::startsWith);
    }

    private boolean isWriteMethod(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method);
    }

    private void writeForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(403);
        response.setContentType(JSON_CONTENT_TYPE);
        response.getWriter().write("{\"code\":\"403\",\"msg\":\"权限不足\"}");
    }
}
