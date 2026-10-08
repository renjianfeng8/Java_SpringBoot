package com.example.springboot.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;

@Configuration
public class TmdbConfig {

    @Value("${tmdb.proxy-host}")
    private String proxyHost;

    @Value("${tmdb.proxy-port}")
    private int proxyPort;

    /**
     * TMDB 专用 RestTemplate。必须显式挂代理 —— 本机直连 api.themoviedb.org 实测 8 秒无响应，
     * 走 127.0.0.1:7897 才通。Bean 单独命名，避免将来出现第二个 RestTemplate 时按类型注入产生歧义。
     */
    @Bean("tmdbRestTemplate")
    public RestTemplate tmdbRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(15));
        return new RestTemplate(factory);
    }
}
