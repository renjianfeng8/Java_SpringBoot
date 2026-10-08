package com.example.springboot.common;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * TMDB 接口客户端：只负责「发请求 + 认字段」，不做任何业务判断。
 * 这里出现的字段名是 TMDB 的外部契约，改动由 {@code TmdbClientTest} 用实测响应体兜住。
 * <p>
 * 一律带 {@code language=zh-CN} —— 实测 TMDB 会据此返回中文片名、中文类型名与中文简介，
 * 于是「英文类型 → 中文类型」这类对照表在本项目里根本不需要存在。
 */
@Component
public class TmdbClient {

    private static final String API_BASE = "https://api.themoviedb.org/3";
    private static final String IMAGE_BASE = "https://image.tmdb.org/t/p";
    private static final String LANGUAGE = "zh-CN";

    /** 导入时下载入库的海报尺寸：w500 一张约 50–100KB，original 单张好几 MB，不用 */
    public static final String POSTER_SIZE = "w500";
    /** 搜索结果缩略图，只用于弹窗里挑片，不进库 */
    private static final String SEARCH_POSTER_SIZE = "w200";
    /** 演员头像尺寸 */
    public static final String PROFILE_SIZE = "w185";

    private final RestTemplate restTemplate;
    private final String token;

    /** 国家代码 → 中文名的整表缓存。静态参考数据，进程内拉一次即可。 */
    private volatile Map<String, String> countryNamesCache;

    public TmdbClient(@Qualifier("tmdbRestTemplate") RestTemplate restTemplate,
                      @Value("${tmdb.token}") String token) {
        this.restTemplate = restTemplate;
        this.token = token;
    }

    /** 搜索结果条目，字段即前端挑片弹窗所需的最小集合 */
    public record SearchHit(int tmdbId, String title, String originalTitle, String year,
                            String posterUrl, String overview) {
    }

    /** 演职人员条目；character 是角色名（TMDB 的 cast[].character） */
    public record CastMember(String name, String character, String profilePath) {
    }

    /** 影片详情。只保留导入要用到的字段，其余 TMDB 数据一律不取。 */
    public record MovieDetail(String title, String originalTitle, String releaseDate, Integer runtime,
                             String overview, String posterPath, String languageCode, String countryCode,
                             String companyName, List<String> genreNames, CastMember topCast) {
    }

    /** 按片名搜索。TMDB 侧的中文检索对中文片名有效，对英文片名同样有效。 */
    public List<SearchHit> search(String query) {
        URI uri = UriComponentsBuilder.fromHttpUrl(API_BASE + "/search/movie")
                .queryParam("query", "{q}")
                .queryParam("language", LANGUAGE)
                .queryParam("include_adult", "false")
                .buildAndExpand(query)
                .encode()
                .toUri();

        JsonNode root = get(uri);
        List<SearchHit> hits = new ArrayList<>();
        for (JsonNode item : root.path("results")) {
            hits.add(new SearchHit(
                    item.path("id").asInt(),
                    text(item, "title"),
                    text(item, "original_title"),
                    yearOf(text(item, "release_date")),
                    imageUrl(SEARCH_POSTER_SIZE, text(item, "poster_path")),
                    text(item, "overview")));
        }
        return hits;
    }

    /**
     * 影片详情。append_to_response=credits 让演职人员随详情一次返回 ——
     * 实测有效，省掉第二次请求，也让「详情拿到一半、演员没拿到」这种中间态不存在。
     */
    public MovieDetail detail(int tmdbId) {
        URI uri = UriComponentsBuilder.fromHttpUrl(API_BASE + "/movie/" + tmdbId)
                .queryParam("language", LANGUAGE)
                .queryParam("append_to_response", "credits")
                .build()
                .encode()
                .toUri();

        JsonNode root = get(uri);
        return new MovieDetail(
                text(root, "title"),
                text(root, "original_title"),
                text(root, "release_date"),
                intOrNull(root, "runtime"),
                text(root, "overview"),
                text(root, "poster_path"),
                firstField(root, "spoken_languages", "iso_639_1"),
                firstField(root, "production_countries", "iso_3166_1"),
                firstField(root, "production_companies", "name"),
                arrayField(root.path("genres"), "name"),
                topCast(root));
    }

    /**
     * 国家代码 → 中文名。实测：这个接口传 language=zh-CN 时 {@code native_name} 就是中文
     * （不传则是英文），所以地区的中文名不必在本项目手写对照表。
     * <p>
     * 拿不到整表时返回空 Map 而不抛 —— 少一个地区名不该让整次导入失败，
     * 且失败不写缓存，下次导入会重试。
     */
    public Map<String, String> countryNames() {
        Map<String, String> cached = countryNamesCache;
        if (cached != null) {
            return cached;
        }
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(API_BASE + "/configuration/countries")
                    .queryParam("language", LANGUAGE)
                    .build()
                    .encode()
                    .toUri();
            Map<String, String> names = new HashMap<>();
            for (JsonNode item : get(uri)) {
                String code = text(item, "iso_3166_1");
                String name = text(item, "native_name");
                if (code != null && name != null) {
                    names.put(code, name);
                }
            }
            Map<String, String> snapshot = Map.copyOf(names);
            countryNamesCache = snapshot;
            return snapshot;
        } catch (RestClientException e) {
            return Map.of();
        }
    }

    /** 拼 TMDB 图片地址。path 为空表示该资源不存在，返回 null 由调用方降级。 */
    public String imageUrl(String size, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return IMAGE_BASE + "/" + size + path;
    }

    /**
     * 下载图片字节。图片缺失是常态（TMDB 不少演员没有头像），
     * 所以任何失败都降级成 empty，绝不让一张图打断整个导入。
     */
    public Optional<byte[]> downloadImage(String size, String path) {
        if (path == null || path.isBlank()) {
            return Optional.empty();
        }
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    URI.create(imageUrl(size, path)), HttpMethod.GET, HttpEntity.EMPTY, byte[].class);
            byte[] body = response.getBody();
            return (body == null || body.length == 0) ? Optional.empty() : Optional.of(body);
        } catch (RestClientException e) {
            return Optional.empty();
        }
    }

    private JsonNode get(URI uri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class).getBody();
    }

    /** 区分「字段不存在」与「字段是 null」：两者都返回 null，不让 asText() 把 null 写成字符串 "null" */
    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value == null || value.isNull()) ? null : value.asText();
    }

    private static Integer intOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value == null || !value.isNumber()) ? null : value.asInt();
    }

    /** TMDB 对未定档影片返回空串，取不到 4 位年份就返回空串而不是截出垃圾 */
    private static String yearOf(String releaseDate) {
        return (releaseDate == null || releaseDate.length() < 4) ? "" : releaseDate.substring(0, 4);
    }

    /** 取数组首元素的某字段：制片国家、语言、制作公司都可能有多个，本系统只留第一个 */
    private static String firstField(JsonNode root, String arrayField, String field) {
        JsonNode array = root.get(arrayField);
        if (array == null || !array.isArray() || array.isEmpty()) {
            return null;
        }
        return text(array.get(0), field);
    }

    /** 保持 TMDB 给出的顺序 —— genre 顺序即重要程度，导入只截前 4 个 */
    private static List<String> arrayField(JsonNode array, String field) {
        List<String> values = new ArrayList<>();
        for (JsonNode item : array) {
            String value = text(item, field);
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    private static CastMember topCast(JsonNode root) {
        JsonNode cast = root.path("credits").path("cast");
        if (!cast.isArray() || cast.isEmpty()) {
            return null;
        }
        JsonNode first = cast.get(0);
        return new CastMember(text(first, "name"), text(first, "character"), text(first, "profile_path"));
    }
}
