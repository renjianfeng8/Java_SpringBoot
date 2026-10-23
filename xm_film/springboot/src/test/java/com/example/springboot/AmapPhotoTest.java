package com.example.springboot;

import com.example.springboot.common.AmapClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 影院封面：高德只在「详情」接口的 show_fields=photos 下给图，搜索接口没有。
 * 这里钉住两件事：取的是第一张图的 url、没图/下架时返回 empty 而不是抛异常。
 */
class AmapPhotoTest {

    private static final String KEY = "test-key";

    private MockRestServiceServer server;

    private AmapClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new AmapClient(restTemplate, KEY);
    }

    @Test
    void returnsFirstPhotoUrl() {
        server.expect(once(), requestTo(org.hamcrest.Matchers.containsString("place/detail")))
                .andRespond(withSuccess("""
                        {"status":"1","info":"OK","count":"1","pois":[
                          {"id":"B00178ZVUC","name":"万达影城",
                           "photos":[{"title":"","url":"https://store.is.autonavi.com/showpic/abc"},
                                     {"title":"","url":"https://aos-comment.amap.com/x.jpg"}]}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        Optional<String> url = client.firstPhotoUrl("B00178ZVUC");

        assertThat(url).contains("https://store.is.autonavi.com/showpic/abc");
        server.verify();
    }

    /** POI 已下架时高德返回 count=0、pois 为空数组：必须 empty，不能 500 */
    @Test
    void returnsEmptyWhenPoiHasNoPhotos() {
        server.expect(once(), requestTo(org.hamcrest.Matchers.containsString("place/detail")))
                .andRespond(withSuccess("""
                        {"status":"1","info":"OK","count":"0","pois":[]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.firstPhotoUrl("B0K2NBNCCY")).isEmpty();
    }

    /** 下载失败只返回 empty：封面是锦上添花，不该阻断导入或补图 */
    @Test
    void downloadFailureReturnsEmpty() {
        server.expect(once(), requestTo("https://store.is.autonavi.com/gone.jpg"))
                .andRespond(withServerError());

        assertThat(client.downloadImage("https://store.is.autonavi.com/gone.jpg")).isEmpty();
    }

    @Test
    void blankInputsReturnEmptyWithoutCallingApi() {
        assertThat(client.firstPhotoUrl("")).isEmpty();
        assertThat(client.firstPhotoUrl(null)).isEmpty();
        assertThat(client.downloadImage(null)).isEmpty();
        server.verify();
    }
}
