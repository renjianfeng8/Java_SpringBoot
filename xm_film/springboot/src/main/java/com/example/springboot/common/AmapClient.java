package com.example.springboot.common;

import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.exception.CustomException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 高德接口客户端：只负责「发请求 + 认字段」，不做业务判断。
 * 字段名是高德的外部契约，改动由 AmapClientTest 用实测响应体兜住。
 * 分类码 080601 = 体育休闲服务;影剧院;电影院。电话在 business.tel，故必须带 show_fields=business。
 */
@Component
public class AmapClient {

    private static final String PLACE_TEXT_URL = "https://restapi.amap.com/v5/place/text";
    private static final String PLACE_DETAIL_URL = "https://restapi.amap.com/v5/place/detail";
    private static final String DISTRICT_URL = "https://restapi.amap.com/v3/config/district";

    /** 高德「电影院」分类码 */
    public static final String CINEMA_TYPE = "080601";

    /** 高德返回的并发超限标记。QPS 是瞬时的，退避重试即可，不该让整次导入失败。 */
    private static final String QPS_LIMIT_MARK = "QPS";
    private static final int QPS_RETRIES = 3;
    private static final long QPS_RETRY_BASE_MS = 200;

    /** district 响应字段名：同一文件出现 3 次以上，抽常量避免一处写错只在运行时表现为「抓到 0 条」 */
    private static final String FIELD_DISTRICTS = "districts";
    private static final String FIELD_LEVEL = "level";

    private final RestTemplate restTemplate;
    private final String key;

    public AmapClient(@Qualifier("amapRestTemplate") RestTemplate restTemplate,
                      @Value("${amap.key}") String key) {
        this.restTemplate = restTemplate;
        this.key = key;
    }

    /** 一条影院 POI。tel 可能为 null（未要 business 或该 POI 无电话） */
    public record CinemaPoi(String poiId, String name, String province, String city,
                            String district, String address, String tel) {
    }

    public record District(String name, String adcode) {
    }

    /**
     * 按区划搜影院，返回**该页**的 POI 列表。region 可传城市名或 adcode。
     * 注意：高德 v5 响应里的 count 是本页条数（恒等于返回条数），不是总命中数——
     * 实测北京 page_size=25 时 page1/2/3 各自 count=25，顶层也没有总数字段。
     * 故翻页只能靠"本页不满"或"响应当空"来终止，见 CinemaDirectoryImportService。
     */
    public List<CinemaPoi> searchCinemas(String region, int pageNum, int pageSize) {
        URI uri = UriComponentsBuilder.fromHttpUrl(PLACE_TEXT_URL)
                .queryParam("key", key)
                .queryParam("types", CINEMA_TYPE)
                .queryParam("region", region)
                .queryParam("page_size", pageSize)
                .queryParam("page_num", pageNum)
                .queryParam("show_fields", "business")
                .build()
                .encode()
                .toUri();

        JsonNode root = get(uri);
        List<CinemaPoi> pois = new ArrayList<>();
        for (JsonNode item : root.path("pois")) {
            pois.add(new CinemaPoi(
                    text(item, "id"),
                    text(item, "name"),
                    text(item, "pname"),
                    text(item, "cityname"),
                    text(item, "adname"),
                    text(item, "address"),
                    text(item.path("business"), "tel")));
        }
        return pois;
    }

    /**
     * 取某市的区县列表（adcode），大城超 200 条时按区县细分抓取。
     * 实测：地级市的 districts[0] 就是 level=city 节点；直辖市（北京/上海/天津/重庆）顶层
     * 多出一个省级条目（如「北京市」），真实区县挂在**同级**的 level=city 条目（「北京城区」）下，
     * 而省级条目 children 里的同名 city 节点是空的——故按 level=city 取，而不是取 districts[0]。
     */
    public List<District> districts(String cityName) {
        URI uri = UriComponentsBuilder.fromHttpUrl(DISTRICT_URL)
                .queryParam("key", key)
                .queryParam("keywords", cityName)
                .queryParam("subdistrict", 2)
                .queryParam("extensions", "base")
                .build()
                .encode()
                .toUri();

        JsonNode root = get(uri);
        List<District> result = new ArrayList<>();
        JsonNode districts = root.path(FIELD_DISTRICTS);
        if (!districts.isArray() || districts.isEmpty()) {
            return result;
        }
        JsonNode city = districts.get(0);
        boolean municipality = "province".equals(text(city, FIELD_LEVEL));
        for (JsonNode d : districts) {
            if ("city".equals(text(d, FIELD_LEVEL))) {
                city = d;
                municipality = false;
                break;
            }
        }
        // 直辖市实测只返回一个省级节点，且其下只有「北京城区」这一个 level=city 节点、无区县；
        // 区县再挂在 city 的下一层，因此必须多要一层（2026-10-19 实测：subdistrict=1 时
        // 北京只返回 1 个「北京城区」，导入因此永远抓到 0 条）。
        for (JsonNode d : city.path(FIELD_DISTRICTS)) {
            if (municipality && "city".equals(text(d, FIELD_LEVEL))) {
                city = d;
                break;
            }
        }
        for (JsonNode d : city.path(FIELD_DISTRICTS)) {
            String adcode = text(d, "adcode");
            String name = text(d, "name");
            if (adcode != null && name != null) {
                result.add(new District(name, adcode));
            }
        }
        return result;
    }

    /**
     * 取某个影院 POI 的首张门店图 URL。
     *
     * <p>搜索接口（{@link #searchCinemas}）不带 photos：高德 Web 服务里图片只在
     * 「详情」接口的 {@code show_fields=photos} 下返回。实测万州 10 家影院里 9 家能取到
     * 1~3 张，取第一张即可 —— 第一张是门店外观，后面的多是用户评论图。
     *
     * <p>无图 / POI 已下架（count=0）都返回 empty，不抛异常：图是锦上添花，
     * 缺图由前端 {@code avatarSrc} 兜底，不该让导入整批失败。
     */
    public Optional<String> firstPhotoUrl(String poiId) {
        if (poiId == null || poiId.isBlank()) {
            return Optional.empty();
        }
        URI uri = UriComponentsBuilder.fromHttpUrl(PLACE_DETAIL_URL)
                .queryParam("key", key)
                .queryParam("id", poiId)
                .queryParam("show_fields", "photos")
                .build()
                .encode()
                .toUri();
        JsonNode root = get(uri);
        JsonNode photos = root.path("pois").path(0).path("photos");
        if (!photos.isArray()) {
            return Optional.empty();
        }
        for (JsonNode photo : photos) {
            String url = text(photo, "url");
            if (url != null) {
                return Optional.of(url);
            }
        }
        return Optional.empty();
    }

    /** 下载图片字节；失败返回 empty，理由同 {@link #firstPhotoUrl} */
    public Optional<byte[]> downloadImage(String url) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        try {
            ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
            byte[] body = response.getBody();
            return body == null || body.length == 0 ? Optional.empty() : Optional.of(body);
        } catch (RestClientException e) {
            return Optional.empty();
        }
    }

    /**
     * status != "1" 一律抛业务异常，让管理员看到高德给的原话（Key 无效/额度用尽等）；
     * 唯一的例外是并发超限：它是瞬时的，退避重试几次，仍不行才抛。
     */
    private JsonNode get(URI uri) {        for (int attempt = 0; ; attempt++) {
            JsonNode root;
            try {
                root = restTemplate.getForObject(uri, JsonNode.class);
            } catch (RestClientException e) {
                throw new CustomException(ErrorCode.SYSTEM_ERROR, "高德接口不可达，请检查网络与 amap.key");
            }
            if (root != null && "1".equals(text(root, "status"))) {
                return root;
            }
            String info = (root == null) ? "无响应" : text(root, "info");
            if (info == null || !info.contains(QPS_LIMIT_MARK) || attempt >= QPS_RETRIES) {
                throw new CustomException(ErrorCode.SYSTEM_ERROR, "高德接口返回异常：" + info);
            }
            try {
                Thread.sleep(QPS_RETRY_BASE_MS << attempt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CustomException(ErrorCode.SYSTEM_ERROR, "高德接口请求被中断");
            }
        }
    }

    /** 高德缺字段时会返回空数组 [] 而非 null，asText() 会把它写成 "[]"，故排除数组 */
    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.isArray() || value.isObject()) {
            return null;
        }
        String s = value.asText();
        return (s == null || s.isBlank()) ? null : s;
    }
}
