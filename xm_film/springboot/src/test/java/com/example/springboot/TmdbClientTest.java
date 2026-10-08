package com.example.springboot;

import com.example.springboot.common.TmdbClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * TMDB 字段名是外部契约，写错了编译期毫无提示，只在运行时表现成「导入后某个字段是空的」。
 * 本测试用实测抓下来的真实响应体把字段名钉死，不联网。
 */
class TmdbClientTest {

    private static final String TOKEN = "test-token";

    /** 实测自 GET /search/movie?query=Fight%20Club&language=zh-CN，字段与外层结构原样保留 */
    private static final String SEARCH_JSON = """
            {"page":1,"total_results":1,"results":[
              {"adult":false,"backdrop_path":"/c6OLXfKAk5BKeR6broC8pYiCquX.jpg",
               "genre_ids":[18,53],"id":550,"title":"搏击俱乐部","original_language":"en",
               "original_title":"Fight Club","overview":"杰克是一个充满中年危机意识的人。",
               "poster_path":"/qkrH7rjYKU4KOwwIl6wF6H3ISvm.jpg","release_date":"1999-10-15"}
            ]}
            """;

    /** 未定档影片的 release_date 是空串，年份不能因此变成 null 或抛异常 */
    private static final String SEARCH_JSON_NO_DATE = """
            {"page":1,"results":[
              {"id":999,"title":"未定档影片","original_title":"Unreleased",
               "overview":"","poster_path":null,"release_date":""}
            ]}
            """;

    /** 实测自 GET /movie/550?language=zh-CN&append_to_response=credits */
    private static final String DETAIL_JSON = """
            {"id":550,"title":"搏击俱乐部","original_title":"Fight Club",
             "overview":"杰克是一个充满中年危机意识的人。","release_date":"1999-10-15","runtime":139,
             "poster_path":"/qkrH7rjYKU4KOwwIl6wF6H3ISvm.jpg",
             "genres":[{"id":18,"name":"剧情"},{"id":53,"name":"惊悚"}],
             "spoken_languages":[{"english_name":"English","iso_639_1":"en","name":"English"}],
             "production_countries":[{"iso_3166_1":"DE","name":"Germany"},{"iso_3166_1":"US","name":"United States of America"}],
             "production_companies":[{"id":508,"name":"Regency Enterprises","origin_country":"US"}],
             "credits":{"cast":[
               {"name":"爱德华·诺顿","character":"Narrator","profile_path":"/8nytsqL59SFJTVYVrN72k6qkGgJ.jpg","order":0},
               {"name":"布拉德·皮特","character":"Tyler Durden","profile_path":"/ajNaPmXVVMJFg9GWmu6MJzTaXdV.jpg","order":1}
             ]}}
            """;

    /** 冷门片可能整块缺字段：没有演职人员、没有语言、没有制片国家 */
    private static final String DETAIL_JSON_SPARSE = """
            {"id":7,"title":"极简条目","runtime":null,"release_date":"",
             "poster_path":null,"genres":[],"credits":{"cast":[]}}
            """;

    private MockRestServiceServer server;
    private TmdbClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new TmdbClient(restTemplate, TOKEN);
    }

    private static String decodedQuery(ClientHttpRequest request) {
        return URLDecoder.decode(request.getURI().getQuery(), StandardCharsets.UTF_8);
    }

    // ========== 搜索 ==========

    @Test
    void searchSendsBearerTokenAndChineseLanguageAndParsesHits() {
        server.expect(ExpectedCount.once(), request -> {
            assertThat(request.getURI().getPath()).isEqualTo("/3/search/movie");
            assertThat(request.getHeaders().getFirst("Authorization")).isEqualTo("Bearer " + TOKEN);
            assertThat(decodedQuery(request)).contains("language=zh-CN").contains("query=Fight Club");
        }).andRespond(withSuccess(SEARCH_JSON, MediaType.APPLICATION_JSON));

        var hits = client.search("Fight Club");

        assertThat(hits).hasSize(1);
        TmdbClient.SearchHit hit = hits.get(0);
        assertThat(hit.tmdbId()).isEqualTo(550);
        assertThat(hit.title()).isEqualTo("搏击俱乐部");
        assertThat(hit.originalTitle()).isEqualTo("Fight Club");
        assertThat(hit.year()).isEqualTo("1999");
        assertThat(hit.overview()).startsWith("杰克");
        assertThat(hit.posterUrl())
                .isEqualTo("https://image.tmdb.org/t/p/w200/qkrH7rjYKU4KOwwIl6wF6H3ISvm.jpg");
        server.verify();
    }

    @Test
    void searchHandlesEmptyReleaseDateAndMissingPoster() {
        server.expect(ExpectedCount.once(), request -> assertThat(decodedQuery(request)).contains("query="))
                .andRespond(withSuccess(SEARCH_JSON_NO_DATE, MediaType.APPLICATION_JSON));

        var hits = client.search("未定档");

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).year()).isEmpty();
        assertThat(hits.get(0).posterUrl()).isNull();
    }

    // ========== 详情 ==========

    @Test
    void detailParsesEveryFieldTheImportBackfills() {
        server.expect(ExpectedCount.once(), request -> {
            assertThat(request.getURI().getPath()).isEqualTo("/3/movie/550");
            assertThat(decodedQuery(request)).contains("language=zh-CN").contains("append_to_response=credits");
        }).andRespond(withSuccess(DETAIL_JSON, MediaType.APPLICATION_JSON));

        TmdbClient.MovieDetail detail = client.detail(550);

        assertThat(detail.title()).isEqualTo("搏击俱乐部");
        assertThat(detail.originalTitle()).isEqualTo("Fight Club");
        assertThat(detail.releaseDate()).isEqualTo("1999-10-15");
        assertThat(detail.runtime()).isEqualTo(139);
        assertThat(detail.overview()).startsWith("杰克");
        assertThat(detail.posterPath()).isEqualTo("/qkrH7rjYKU4KOwwIl6wF6H3ISvm.jpg");
        // genre 顺序即重要程度，导入要截前 4 个，故必须保持原序
        assertThat(detail.genreNames()).containsExactly("剧情", "惊悚");
        assertThat(detail.languageCode()).isEqualTo("en");
        assertThat(detail.countryCode()).isEqualTo("DE");
        assertThat(detail.companyName()).isEqualTo("Regency Enterprises");
        assertThat(detail.topCast()).isNotNull();
        assertThat(detail.topCast().name()).isEqualTo("爱德华·诺顿");
        assertThat(detail.topCast().character()).isEqualTo("Narrator");
        assertThat(detail.topCast().profilePath()).isEqualTo("/8nytsqL59SFJTVYVrN72k6qkGgJ.jpg");
        server.verify();
    }

    @Test
    void detailToleratesMissingOptionalBlocks() {
        server.expect(ExpectedCount.once(), request -> assertThat(request.getURI().getPath()).isEqualTo("/3/movie/7"))
                .andRespond(withSuccess(DETAIL_JSON_SPARSE, MediaType.APPLICATION_JSON));

        TmdbClient.MovieDetail detail = client.detail(7);

        assertThat(detail.runtime()).isNull();
        assertThat(detail.posterPath()).isNull();
        assertThat(detail.languageCode()).isNull();
        assertThat(detail.countryCode()).isNull();
        assertThat(detail.companyName()).isNull();
        assertThat(detail.topCast()).isNull();
        assertThat(detail.genreNames()).isEmpty();
    }

    // ========== 国家名（地区的中文名来源） ==========

    /** 实测自 GET /configuration/countries?language=zh-CN：带 language 时 native_name 是中文 */
    private static final String COUNTRIES_JSON = """
            [{"iso_3166_1":"AD","english_name":"Andorra","native_name":"安道尔"},
             {"iso_3166_1":"DE","english_name":"Germany","native_name":"德国"},
             {"iso_3166_1":"US","english_name":"United States of America","native_name":"美国"}]
            """;

    @Test
    void countryNamesMapCodesToChineseNames() {
        server.expect(ExpectedCount.once(), request -> {
            assertThat(request.getURI().getPath()).isEqualTo("/3/configuration/countries");
            assertThat(decodedQuery(request)).contains("language=zh-CN");
        }).andRespond(withSuccess(COUNTRIES_JSON, MediaType.APPLICATION_JSON));

        assertThat(client.countryNames())
                .containsEntry("DE", "德国")
                .containsEntry("US", "美国");
        server.verify();
    }

    /** 整表是静态参考数据，第二次调用不该再打一次接口（ExpectedCount.once 会把多发的那次判失败） */
    @Test
    void countryNamesAreCachedAcrossCalls() {
        server.expect(ExpectedCount.once(), request -> assertThat(request.getURI().getPath())
                .isEqualTo("/3/configuration/countries"))
                .andRespond(withSuccess(COUNTRIES_JSON, MediaType.APPLICATION_JSON));

        client.countryNames();
        assertThat(client.countryNames()).containsEntry("DE", "德国");
        server.verify();
    }

    /** 接口挂了只该丢掉地区名，不该让整次导入炸掉 */
    @Test
    void countryNamesDegradeToEmptyMapOnFailure() {
        server.expect(ExpectedCount.once(), request -> assertThat(request.getURI().getPath())
                .isEqualTo("/3/configuration/countries"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThat(client.countryNames()).isEmpty();
    }

    // ========== 图片下载 ==========

    @Test
    void downloadImageReturnsBytes() {
        byte[] bytes = {1, 2, 3};
        server.expect(ExpectedCount.once(), request -> assertThat(request.getURI().toString())
                        .isEqualTo("https://image.tmdb.org/t/p/w500/qkrH7rjYKU4KOwwIl6wF6H3ISvm.jpg"))
                .andRespond(withSuccess(bytes, MediaType.IMAGE_JPEG));

        Optional<byte[]> result = client.downloadImage("w500", "/qkrH7rjYKU4KOwwIl6wF6H3ISvm.jpg");

        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly((byte) 1, (byte) 2, (byte) 3);
        server.verify();
    }

    /** 图片缺失是常态（TMDB 不少条目没有头像），必须降级成 empty 而不是抛异常打断整个导入 */
    @Test
    void downloadImageReturnsEmptyOn404() {
        server.expect(ExpectedCount.once(), request -> assertThat(request.getURI().toString())
                        .isEqualTo("https://image.tmdb.org/t/p/w185/missing.jpg"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.downloadImage("w185", "/missing.jpg")).isEmpty();
    }

    @Test
    void downloadImageSkipsRequestWhenPathIsMissing() {
        assertThat(client.downloadImage("w185", null)).isEmpty();
        assertThat(client.downloadImage("w185", "")).isEmpty();
        server.verify();
    }
}
