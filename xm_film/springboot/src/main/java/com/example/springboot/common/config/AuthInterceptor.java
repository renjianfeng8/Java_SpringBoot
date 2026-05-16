package com.example.springboot.common.config;

import com.example.springboot.common.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

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

                // 基于路径的角色访问控制
                String path = request.getRequestURI();
                if (path.startsWith("/admin/") && !"ADMIN".equals(role)) {
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":\"403\",\"msg\":\"权限不足，仅管理员可访问\"}");
                    return false;
                }
                return true;
            } catch (Exception ignored) {
            }
        }

        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"401\",\"msg\":\"登录已过期，请重新登录\"}");
        return false;
    }
}
