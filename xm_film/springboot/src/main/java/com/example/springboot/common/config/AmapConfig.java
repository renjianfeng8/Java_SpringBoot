package com.example.springboot.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;

/**
 * 高德专用 RestTemplate。高德在国内、直连可达，不挂代理——这一点与 tmdbRestTemplate 相反。
 * Bean 单独命名：TmdbConfig 的注释已提醒，出现第二个 RestTemplate 时按类型注入会歧义。
 */
@Configuration
public class AmapConfig {

    /**
     * 额外挂一个节流拦截器：高德个人 Key 的 QPS 上限实测约 5 次/秒（连发第 6 次即
     * CUQPS_HAS_EXCEEDED_THE_LIMIT），而导入会背靠背发几十个请求（每个城市最多 8 页 +
     * 逐区县再各翻若干页），不节流必然撞限。
     * 节流挂在 Bean 上而非 AmapClient 里 —— 单元测试用的是裸 RestTemplate，不会因此变慢。
     */
    @Bean("amapRestTemplate")
    public RestTemplate amapRestTemplate(@Value("${amap.request-interval-ms:300}") long intervalMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(15));
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.getInterceptors().add(new MinIntervalInterceptor(intervalMs));
        return restTemplate;
    }

    /** 维持相邻请求的最小间隔。synchronized 让多线程下的间隔也成立。 */
    private static final class MinIntervalInterceptor implements ClientHttpRequestInterceptor {

        private final long intervalMs;
        private long lastRequestAt;

        private MinIntervalInterceptor(long intervalMs) {
            this.intervalMs = intervalMs;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {
            awaitSlot();
            return execution.execute(request, body);
        }

        private synchronized void awaitSlot() {
            long wait = lastRequestAt + intervalMs - System.currentTimeMillis();
            if (wait > 0) {
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            lastRequestAt = System.currentTimeMillis();
        }
    }
}
