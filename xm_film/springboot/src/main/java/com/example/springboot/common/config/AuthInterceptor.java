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

import java.util.Set;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

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

    @Resource
    private JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
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

        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"401\",\"msg\":\"登录已过期，请重新登录\"}");
        return false;
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

    private void writeForbidden(HttpServletResponse response) throws Exception {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"403\",\"msg\":\"权限不足\"}");
    }
}
