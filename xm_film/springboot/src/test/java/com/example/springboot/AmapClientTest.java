package com.example.springboot;

import com.example.springboot.common.AmapClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AmapClientTest {

    private static final String KEY = "test-key";

    /** 实测自 GET /v5/place/text?types=080601&region=北京&show_fields=business */
    private static final String TEXT_JSON = """
            {"status":"1","info":"OK","infocode":"10000","count":"2","pois":[
              {"id":"B0FFHF8BWF","name":"万达影城(通州万达广场店)",
               "location":"116.658,39.902","type":"体育休闲服务;影剧院;电影院","typecode":"080601",
               "pname":"北京市","cityname":"北京市","adname":"通州区","adcode":"110112",
               "address":"新华西街58号万达广场A座4层","business":{"tel":"010-88880001"}},
              {"id":"B0FFHF8BWG","name":"CGV影城(朝阳大悦城店)",
               "location":"116.517,39.924","type":"体育休闲服务;影剧院;电影院","typecode":"080601",
               "pname":"北京市","cityname":"北京市","adname":"朝阳区","adcode":"110105",
               "address":"朝阳北路101号","business":{"tel":"010-88880002"}}
            ]}
            """;

    /** 缺 business（未要 show_fields）或缺地址的条目：不能把 null/空数组写成字符串 */
    private static final String TEXT_JSON_SPARSE = """
            {"status":"1","info":"OK","count":"1","pois":[
              {"id":"B0FFHF8BWH","name":"无名影院","typecode":"080601",
               "pname":"河北省","cityname":"石家庄市","adname":"桥西区","address":[],"business":[]}
            ]}
            """;

    /** 直辖市实测形态：顶层多一个省级条目「北京市」，真实区县挂在同级 level=city 的「北京城区」下 */
    private static final String DISTRICT_JSON = """
            {"status":"1","info":"OK","districts":[
              {"name":"北京市","adcode":"110000","level":"province","districts":[
                {"name":"北京城区","adcode":"110100","level":"city","districts":[]}]},
              {"name":"北京城区","adcode":"110100","level":"city","districts":[
                {"name":"东城区","adcode":"110101","level":"district"},
                {"name":"通州区","adcode":"110112","level":"district"}]}]}
            """;

    /** 地级市实测形态：第一层就是 level=city 节点 */
    private static final String DISTRICT_JSON_PREFECTURE = """
            {"status":"1","info":"OK","districts":[
              {"name":"石家庄市","adcode":"130100","level":"city","districts":[
                {"name":"长安区","adcode":"130102","level":"district"}]}]}
            """;

    private MockRestServiceServer server;
    private AmapClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new AmapClient(restTemplate, KEY);
    }

    private static String q(org.springframework.http.client.ClientHttpRequest r) {
        return URLDecoder.decode(r.getURI().getQuery(), StandardCharsets.UTF_8);
    }

    @Test
    void searchSendsKeyTypeAndRegionAndParsesPois() {
        server.expect(once(), request -> {
            assertThat(request.getURI().getPath()).isEqualTo("/v5/place/text");
            assertThat(q(request)).contains("key=" + KEY)
                    .contains("types=080601").contains("region=北京")
                    .contains("show_fields=business");
        }).andRespond(withSuccess(TEXT_JSON, MediaType.APPLICATION_JSON));

        AmapClient.PoiPage page = client.searchCinemas("北京", 1, 25);

        assertThat(page.count()).isEqualTo(2);
        assertThat(page.pois()).hasSize(2);
        AmapClient.CinemaPoi poi = page.pois().get(0);
        assertThat(poi.poiId()).isEqualTo("B0FFHF8BWF");
        assertThat(poi.name()).isEqualTo("万达影城(通州万达广场店)");
        assertThat(poi.province()).isEqualTo("北京市");
        assertThat(poi.city()).isEqualTo("北京市");
        assertThat(poi.district()).isEqualTo("通州区");
        assertThat(poi.address()).isEqualTo("新华西街58号万达广场A座4层");
        assertThat(poi.tel()).isEqualTo("010-88880001");
        server.verify();
    }

    @Test
    void searchToleratesArrayValuedMissingFields() {
        server.expect(once(), r -> assertThat(q(r)).contains("region=石家庄"))
                .andRespond(withSuccess(TEXT_JSON_SPARSE, MediaType.APPLICATION_JSON));

        AmapClient.CinemaPoi poi = client.searchCinemas("石家庄", 1, 25).pois().get(0);

        assertThat(poi.address()).isNull();
        assertThat(poi.tel()).isNull();
    }

    /** 直辖市：多一层省级，必须跳过去取 level=city 节点的区县 */
    @Test
    void districtsSkipsProvinceLayerForMunicipality() {
        server.expect(once(), r -> {
            assertThat(r.getURI().getPath()).isEqualTo("/v3/config/district");
            assertThat(q(r)).contains("keywords=北京").contains("subdistrict=1");
        }).andRespond(withSuccess(DISTRICT_JSON, MediaType.APPLICATION_JSON));

        assertThat(client.districts("北京"))
                .extracting(AmapClient.District::adcode)
                .containsExactly("110101", "110112");
    }

    /** 地级市：第一层即 city 节点，直接取之 */
    @Test
    void districtsReadsPrefectureCityDirectly() {
        server.expect(once(), r -> assertThat(q(r)).contains("keywords=石家庄"))
                .andRespond(withSuccess(DISTRICT_JSON_PREFECTURE, MediaType.APPLICATION_JSON));

        assertThat(client.districts("石家庄"))
                .extracting(AmapClient.District::adcode)
                .containsExactly("130102");
    }
}
