package com.example.springboot.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final String uploadDir;

    private final AuthInterceptor authInterceptor;

    public WebMvcConfig(@Value("${file.upload-dir}") String uploadDir, AuthInterceptor authInterceptor) {
        this.uploadDir = uploadDir;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + uploadDir + File.separator);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(
                        "/api/v1/health",
                        "/api/v1/auth/login",
                        "/api/v1/auth/register",
                        "/api/v1/auth/years",
                        "/api/v1/films/page",
                        "/api/v1/films/box-office/**",
                        "/api/v1/films/mark/**",
                        "/api/v1/films/search",
                        "/api/v1/films/by-cinema",
                        "/api/v1/notices/page"
                );
        // 排除表只放「必须绕过鉴权的非读端点」（登录/注册）与确定无副作用的读端点。
        // 注意：不要把 /api/v1/cinemas/page 或 /api/v1/types、/api/v1/areas 这类
        // 「类路径即集合根」的路径加进来 —— 排除是对该路径的所有方法生效，而继承自
        // BaseController 的 POST/PUT 就挂在同一个集合根上，一旦排除，拦截器不工作、
        // ADMIN_WRITE_PREFIXES 形同虚设，匿名就能增改。这些读端点的匿名放行统一交给
        // AuthInterceptor.PUBLIC_READ_PREFIXES（只放 GET），不要再往这张表里加。
    }
}
