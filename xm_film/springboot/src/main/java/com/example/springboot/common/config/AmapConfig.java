package com.example.springboot.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 高德专用 RestTemplate。高德在国内、直连可达，不挂代理——这一点与 tmdbRestTemplate 相反。
 * Bean 单独命名：TmdbConfig 的注释已提醒，出现第二个 RestTemplate 时按类型注入会歧义。
 */
@Configuration
public class AmapConfig {

    @Bean("amapRestTemplate")
    public RestTemplate amapRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(15));
        return new RestTemplate(factory);
    }
}
