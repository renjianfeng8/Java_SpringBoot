package com.example.springboot.common;

import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.exception.CustomException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * 高德接口客户端：只负责「发请求 + 认字段」，不做业务判断。
 * 字段名是高德的外部契约，改动由 AmapClientTest 用实测响应体兜住。
 * 分类码 080601 = 体育休闲服务;影剧院;电影院。电话在 business.tel，故必须带 show_fields=business。
 */
@Component
public class AmapClient {

    private static final String PLACE_TEXT_URL = "https://restapi.amap.com/v5/place/text";
    private static final String DISTRICT_URL = "https://restapi.amap.com/v3/config/district";

    /** 高德「电影院」分类码 */
    public static final String CINEMA_TYPE = "080601";

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

    /** 一页结果：count 是该 group 参数下的总命中数（用于判断是否超 200） */
    public record PoiPage(int count, List<CinemaPoi> pois) {
    }

    public record District(String name, String adcode) {
    }

    /** 按区划搜影院。region 可传城市名或 adcode。 */
    public PoiPage searchCinemas(String region, int pageNum, int pageSize) {
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
        return new PoiPage(intOrZero(root, "count"), pois);
    }

    /**
     * 取某市的区县列表（adcode），大城超 200 条时按区县细分抓取。
     * 实测：地级市第一层即 level=city 节点；直辖市（北京/上海/天津/重庆）多一层省级，
     * 真实区县在 city 节点下——故先挑 level=city 的那个，挑不到再退回第一个。
     */
    public List<District> districts(String cityName) {
        URI uri = UriComponentsBuilder.fromHttpUrl(DISTRICT_URL)
                .queryParam("key", key)
                .queryParam("keywords", cityName)
                .queryParam("subdistrict", 1)
                .queryParam("extensions", "base")
                .build()
                .encode()
                .toUri();

        JsonNode root = get(uri);
        List<District> result = new ArrayList<>();
        JsonNode districts = root.path("districts");
        if (!districts.isArray() || districts.isEmpty()) {
            return result;
        }
        JsonNode city = districts.get(0);
        for (JsonNode d : districts) {
            if ("city".equals(text(d, "level"))) {
                city = d;
                break;
            }
        }
        for (JsonNode d : city.path("districts")) {
            String adcode = text(d, "adcode");
            String name = text(d, "name");
            if (adcode != null && name != null) {
                result.add(new District(name, adcode));
            }
        }
        return result;
    }

    /** status != "1" 一律抛业务异常，让管理员看到高德给的原话（Key 无效/额度用尽等） */
    private JsonNode get(URI uri) {
        JsonNode root;
        try {
            root = restTemplate.getForObject(uri, JsonNode.class);
        } catch (RestClientException e) {
            throw new CustomException(ErrorCode.SYSTEM_ERROR, "高德接口不可达，请检查网络与 amap.key");
        }
        if (root == null || !"1".equals(text(root, "status"))) {
            String info = (root == null) ? "无响应" : text(root, "info");
            throw new CustomException(ErrorCode.SYSTEM_ERROR, "高德接口返回异常：" + info);
        }
        return root;
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

    private static int intOrZero(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value == null || value.isNull()) ? 0 : value.asInt(0);
    }
}
