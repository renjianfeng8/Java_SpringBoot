package com.example.springboot.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        // 创建CORS配置对象，用于配置跨域规则
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*"); // 允许所有域名进行跨域调用（生产环境建议指定具体域名）
        config.addAllowedHeader("*");        // 允许请求携带任何头信息
        config.addAllowedMethod("*");        // 允许使用任何HTTP请求方法（GET、POST等）
        config.setAllowCredentials(true);    // 允许携带认证凭证（如Cookie）

        // 配置URL映射源，将CORS规则应用到所有接口
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        // 返回CORS过滤器实例，实现跨域请求处理
        return new CorsFilter(source);
    }
}