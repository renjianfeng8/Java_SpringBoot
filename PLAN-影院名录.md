# 高德影院名录导入 · 实施计划

执行方式：用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务执行本计划，按 `- [ ]` 复选框跟踪进度。

目标：从高德 POI 抓取中国大陆 45 个主要城市的电影院，落进一张独立的 `cinema_directory` 只读名录表，后台提供"预览-确认"式批量导入，前台提供公开的"影院名录"浏览页。

架构：复刻 TMDB 导入范式——`AmapClient`（只发请求/认字段）→ `CinemaDirectoryImportService`（抓取 + 去重 + 细分，产出预览，不落库）→ 管理员确认后 `import` 才批量 `INSERT IGNORE`。目录表与 `cinema` 账号表物理隔离，不碰登录/审核/统计。前台名录页只读、免登录。

技术栈：Spring Boot 3.3.13，Java 17，MyBatis + PageHelper，JUnit5 + AssertJ + `MockRestServiceServer`，Vue 3 + Element Plus。

## 前置条件（开工前你需要做）

1. 申请高德 Key：https://console.amap.com → 应用管理 → 创建应用 → 添加 Key → 服务平台选 Web服务。拿到后填进 `application.yml` 的 `amap.key`。
2. 建表：本仓库已无 `init.sql`（上一轮整体删除），表结构不在版本库里，需手工在本地 `xm-film` 库执行 Task 1 的 DDL。
3. 高德接口事实（已在计划中固化，勿凭记忆改）：
   - 分类码 `080601` = 体育休闲服务 › 影剧院 › 电影院。
   - v5 `place/text`：`page_size` 1–25、`page_num` 1–100，同组参数翻页最多 200 条，超 200 必须按区县细分。
   - 基础字段默认返回：`name/id/location/typecode/pname(省)/cityname(市)/adname(区县)/address/adcode`。
   - `tel` 属 `business` 对象，必须传 `show_fields=business` 才返回（路径 `business.tel`）。

## 文件结构

后端新增：

| 文件 | 职责 |
|---|---|
| `common/AmapClient.java` | 只发请求 + 认字段（place/text、district 两接口） |
| `common/config/AmapConfig.java` | `amapRestTemplate` Bean（直连、带超时、无代理） |
| `common/CinemaBrand.java` | 名称 → 连锁品牌 派生 |
| `entity/CinemaDirectory.java` | 名录实体 |
| `mapper/CinemaDirectoryMapper.java` + `resources/mapper/CinemaDirectoryMapper.xml` | 查询/去重插入 |
| `dto/request/CinemaImportRequest.java` | `{ List<String> cities }` |
| `dto/response/CinemaImportPreview.java` | 预览结果 |
| `service/CinemaDirectoryService.java` | 名录查询（分页/筛选项） |
| `service/CinemaDirectoryImportService.java` | 抓取 + 去重 + 细分 + 导入 |
| `controller/CinemaDirectoryController.java` | 4 个端点 |

后端修改：`common/config/AuthInterceptor.java`（白名单）、`resources/application.yml`（amap.key）。

前端新增：`views/front/CinemaDirectory.vue`、`views/manage/CinemaDirectory.vue`。

前端修改：`constants/index.js`、`router/index.js`、`views/Front.vue`（导航）、`views/Manage.vue`（菜单）。

测试：`AmapClientTest`、`CinemaBrandTest`、`CinemaDirectoryImportServiceTest`（新增），`AuthInterceptorAccessTest`（修改）。

## Task 1: 建表

Files：无仓库文件（DDL 手工执行）。

- [ ] Step 1: 在本地 MySQL 执行 DDL。

```sql
CREATE TABLE IF NOT EXISTS cinema_directory (
  id INT NOT NULL AUTO_INCREMENT,
  poi_id VARCHAR(32) NOT NULL COMMENT '高德 POI id，重导入去重键',
  name VARCHAR(100) NOT NULL COMMENT '影院名称',
  brand VARCHAR(50) DEFAULT NULL COMMENT '派生连锁品牌',
  province VARCHAR(30) DEFAULT NULL,
  city VARCHAR(30) DEFAULT NULL,
  district VARCHAR(30) DEFAULT NULL,
  address VARCHAR(255) DEFAULT NULL,
  phone VARCHAR(50) DEFAULT NULL,
  source VARCHAR(20) NOT NULL DEFAULT 'amap',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cinema_directory_poi_id (poi_id),
  KEY idx_cinema_directory_city (city),
  KEY idx_cinema_directory_brand (brand)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='影院名录（高德 POI 导入，只读展示）';
```

- [ ] Step 2: 验证表已建。

运行：`mysql -u root -p123456 xm-film -e "SHOW COLUMNS FROM cinema_directory;"`

预期：输出 11 行字段（id…create_time）。

## Task 2: 实体 + Mapper

Files：Create `entity/CinemaDirectory.java`, `mapper/CinemaDirectoryMapper.java`, `resources/mapper/CinemaDirectoryMapper.xml`。

- [ ] Step 1: 建实体。

```java
package com.example.springboot.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 影院名录：高德 POI 导入的只读门店资料，与账号表 cinema 无关 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CinemaDirectory {
    private Integer id;
    private String poiId;
    private String name;
    private String brand;
    private String province;
    private String city;
    private String district;
    private String address;
    private String phone;
    private String source;
    private LocalDateTime createTime;
}
```

- [ ] Step 2: 建 Mapper 接口。

```java
package com.example.springboot.mapper;

import com.example.springboot.entity.CinemaDirectory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CinemaDirectoryMapper {

    /** 名录分页/筛选（PageHelper 在此语句前 startPage）；query 的非空字段即筛选条件 */
    List<CinemaDirectory> selectAll(CinemaDirectory query);

    /** 幂等插入：poi_id 唯一，重复的静默跳过，返回本次真正插入的行数（0 或 1） */
    int insertIgnore(CinemaDirectory poi);

    /** 批量查已存在的 poi_id，供预览算「新增 / 重复」 */
    List<String> selectExistingPoiIds(@Param("poiIds") List<String> poiIds);

    List<String> distinctCities();

    List<String> distinctBrands();
}
```

- [ ] Step 3: 建 XML。

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.example.springboot.mapper.CinemaDirectoryMapper">

    <select id="selectAll" resultType="com.example.springboot.entity.CinemaDirectory">
        SELECT * FROM cinema_directory
        <where>
            <if test="name != null">AND name LIKE CONCAT('%', #{name}, '%')</if>
            <if test="province != null">AND province = #{province}</if>
            <if test="city != null">AND city = #{city}</if>
            <if test="district != null">AND district = #{district}</if>
            <if test="brand != null">AND brand = #{brand}</if>
        </where>
        ORDER BY id DESC
    </select>

    <insert id="insertIgnore" parameterType="com.example.springboot.entity.CinemaDirectory">
        INSERT IGNORE INTO cinema_directory
            (poi_id, name, brand, province, city, district, address, phone, source)
        VALUES
            (#{poiId}, #{name}, #{brand}, #{province}, #{city}, #{district}, #{address}, #{phone}, #{source})
    </insert>

    <select id="selectExistingPoiIds" resultType="java.lang.String">
        SELECT poi_id FROM cinema_directory
        WHERE poi_id IN
        <foreach collection="poiIds" item="id" open="(" separator="," close=")">#{id}</foreach>
    </select>

    <select id="distinctCities" resultType="java.lang.String">
        SELECT DISTINCT city FROM cinema_directory WHERE city IS NOT NULL AND city != '' ORDER BY city
    </select>

    <select id="distinctBrands" resultType="java.lang.String">
        SELECT DISTINCT brand FROM cinema_directory WHERE brand IS NOT NULL AND brand != '' ORDER BY brand
    </select>

</mapper>
```

- [ ] Step 4: 编译验证。

运行：`cd xm_film/springboot && mvn -q compile`

预期：BUILD SUCCESS。

- [ ] Step 5: 提交。

```bash
git add xm_film/springboot/src/main/java/com/example/springboot/entity/CinemaDirectory.java xm_film/springboot/src/main/java/com/example/springboot/mapper/CinemaDirectoryMapper.java xm_film/springboot/src/main/resources/mapper/CinemaDirectoryMapper.xml
git commit -m "feat: 影院名录实体与 Mapper —— 独立于账号表的只读目录"
```

## Task 3: AmapClient（含 RestTemplate Bean 与 Key 配置）

Files：Modify `resources/application.yml`; Create `common/config/AmapConfig.java`, `common/AmapClient.java`, `test/.../AmapClientTest.java`。

- [ ] Step 1: 加配置。

`application.yml` 末尾追加：

```yaml
# 高德开放平台 Web 服务 Key，仅后台「从高德导入影院名录」使用。
# 国内直连，不经代理（与 TMDB 不同）。Key 在 console.amap.com 申请，服务平台选「Web服务」。
amap:
  key: 在此填入你申请的Key
```

- [ ] Step 2: 建 Bean（独立命名，避免将来按类型注入歧义）。

```java
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
```

- [ ] Step 3: 写失败的测试。

`test/java/com/example/springboot/AmapClientTest.java`。字段名是高德外部契约，用实测响应体钉死（下面的 JSON 是 v5 结构的实测形态，Task 8 Step 1 会用真 Key 复核并按实际微调）。

```java
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

    private static final String DISTRICT_JSON = """
            {"status":"1","info":"OK","districts":[{"name":"北京市","adcode":"110000",
              "level":"province","districts":[
                {"name":"东城区","adcode":"110101","level":"district"},
                {"name":"通州区","adcode":"110112","level":"district"}
              ]}]}
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

    @Test
    void districtsParsesSubDistricts() {
        server.expect(once(), r -> {
            assertThat(r.getURI().getPath()).isEqualTo("/v3/config/district");
            assertThat(q(r)).contains("keywords=北京").contains("subdistrict=1");
        }).andRespond(withSuccess(DISTRICT_JSON, MediaType.APPLICATION_JSON));

        assertThat(client.districts("北京"))
                .extracting(AmapClient.District::adcode)
                .containsExactly("110101", "110112");
    }
}
```

- [ ] Step 4: 跑测试确认失败。

运行：`cd xm_film/springboot && mvn -q -Dtest=AmapClientTest test`

预期：编译失败（`AmapClient` 不存在）。

- [ ] Step 5: 实现 AmapClient。

```java
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

    /** 取某市的区县列表（adcode），大城超 200 条时按区县细分抓取。 */
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
        if (districts.isArray() && !districts.isEmpty()) {
            for (JsonNode d : districts.get(0).path("districts")) {
                String adcode = text(d, "adcode");
                String name = text(d, "name");
                if (adcode != null && name != null) {
                    result.add(new District(name, adcode));
                }
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
```

注意：`text()` 对 `business` 传的是对象但只取 `tel` 子字段——`text(item.path("business"), "tel")` 取的是 `business.tel`，是字符串，不受影响。

- [ ] Step 6: 跑测试确认通过。

运行：`mvn -q -Dtest=AmapClientTest test`

预期：3 个用例全绿。

- [ ] Step 7: 提交。

```bash
git add xm_film/springboot/src/main/resources/application.yml xm_film/springboot/src/main/java/com/example/springboot/common/config/AmapConfig.java xm_film/springboot/src/main/java/com/example/springboot/common/AmapClient.java xm_film/springboot/src/test/java/com/example/springboot/AmapClientTest.java
git commit -m "feat: 高德 POI 客户端 —— v5 搜索接口 + 区县接口，字段契约用实测响应钉住"
```

## Task 4: 品牌派生

Files：Create `common/CinemaBrand.java`, `test/.../CinemaBrandTest.java`。

- [ ] Step 1: 写失败的测试。

```java
package com.example.springboot;

import com.example.springboot.common.CinemaBrand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CinemaBrandTest {

    @Test
    void matchesKnownChainByKeyword() {
        assertThat(CinemaBrand.derive("万达影城(通州万达广场店)")).isEqualTo("万达影城");
        assertThat(CinemaBrand.derive("横店电影城(西单店)")).isEqualTo("横店影视");
        assertThat(CinemaBrand.derive("大地影院(望京店)")).isEqualTo("大地影院");
    }

    /** ASCII 品牌名大小写不敏感 */
    @Test
    void matchesAsciiBrandCaseInsensitively() {
        assertThat(CinemaBrand.derive("cgv影城(奥体店)")).isEqualTo("CGV影城");
        assertThat(CinemaBrand.derive("ume国际影城")).isEqualTo("UME影城");
    }

    /** 具体品牌优先于其子串："橙天嘉禾" 必须先于 "嘉禾" 命中 */
    @Test
    void moreSpecificBrandWinsOverSubstring() {
        assertThat(CinemaBrand.derive("橙天嘉禾影城(合生汇店)")).isEqualTo("橙天嘉禾");
    }

    @Test
    void unmatchedReturnsNullAndNullIsSafe() {
        assertThat(CinemaBrand.derive("某某私人影院")).isNull();
        assertThat(CinemaBrand.derive(null)).isNull();
    }
}
```

- [ ] Step 2: 跑测试确认失败。

运行：`mvn -q -Dtest=CinemaBrandTest test`

预期：编译失败（`CinemaBrand` 不存在）。

- [ ] Step 3: 实现。

```java
package com.example.springboot.common;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 影院名称 → 连锁品牌。高德不返回「所属院线」，只能按名称关键词派生。
 * 规则有序：长/具体词在前（如「橙天嘉禾」先于「嘉禾」），首个命中即返回。
 * 未命中返回 null（前端显示为「独立/其他」），词表可按需增补。
 */
public final class CinemaBrand {

    private static final List<Map.Entry<String, String>> RULES = List.of(
            Map.entry("万达", "万达影城"),
            Map.entry("CGV", "CGV影城"),
            Map.entry("横店", "横店影视"),
            Map.entry("大地", "大地影院"),
            Map.entry("金逸", "金逸影城"),
            Map.entry("UME", "UME影城"),
            Map.entry("博纳", "博纳国际影城"),
            Map.entry("中影", "中影国际影城"),
            Map.entry("幸福蓝海", "幸福蓝海国际影城"),
            Map.entry("保利", "保利国际影城"),
            Map.entry("卢米埃", "卢米埃影城"),
            Map.entry("橙天嘉禾", "橙天嘉禾"),
            Map.entry("嘉禾", "橙天嘉禾"),
            Map.entry("百丽宫", "百丽宫影城"),
            Map.entry("英皇", "英皇电影城"),
            Map.entry("太平洋", "太平洋影城"),
            Map.entry("苏宁", "苏宁影城"),
            Map.entry("SFC", "SFC上影影城"),
            Map.entry("上影", "SFC上影影城"),
            Map.entry("星轶", "星轶影城")
    );

    private CinemaBrand() {
    }

    public static String derive(String name) {
        if (name == null) {
            return null;
        }
        String upper = name.toUpperCase(Locale.ROOT);
        for (Map.Entry<String, String> rule : RULES) {
            if (upper.contains(rule.getKey().toUpperCase(Locale.ROOT))) {
                return rule.getValue();
            }
        }
        return null;
    }
}
```

- [ ] Step 4: 跑测试确认通过。

运行：`mvn -q -Dtest=CinemaBrandTest test`

预期：4 个用例全绿。

- [ ] Step 5: 提交。

```bash
git add xm_film/springboot/src/main/java/com/example/springboot/common/CinemaBrand.java xm_film/springboot/src/test/java/com/example/springboot/CinemaBrandTest.java
git commit -m "feat: 影院连锁品牌按名称关键词派生（有序规则表）"
```

## Task 5: 预览 DTO + 请求 DTO

Files：Create `dto/response/CinemaImportPreview.java`, `dto/request/CinemaImportRequest.java`。

- [ ] Step 1: 建 DTO。

```java
package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 高德导入的预览。只是一份草稿，不代表库里已经多了这些行。
 * 与 TMDB 的 TmdbImportPreview 同一思路：确认之后才走 import 落库。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CinemaImportPreview {

    /** 抓取到的唯一 POI 数（按 poi_id 去重后） */
    private int total;

    /** 其中库中不存在、将被新增的条数 */
    private int freshCount;

    /** 其中库中已存在、将被跳过的条数 */
    private int duplicateCount;

    /** 前若干条名称，供管理员核对抓的是不是影院 */
    private List<String> sample;

    /** 降级说明：超 200 未取全、区县取不到等 */
    private List<String> warnings;
}
```

```java
package com.example.springboot.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class CinemaImportRequest {
    /** 要导入的城市名列表（与高德 region 可传的城市名一致） */
    private List<String> cities;
}
```

- [ ] Step 2: 编译。

运行：`mvn -q compile`

预期：BUILD SUCCESS。

- [ ] Step 3: 提交。

```bash
git add xm_film/springboot/src/main/java/com/example/springboot/dto/response/CinemaImportPreview.java xm_film/springboot/src/main/java/com/example/springboot/dto/request/CinemaImportRequest.java
git commit -m "feat: 影院导入预览/请求 DTO"
```

## Task 6: 导入服务（抓取 + 去重 + 超 200 细分）

Files：Create `service/CinemaDirectoryImportService.java`, `test/.../service/CinemaDirectoryImportServiceTest.java`。

- [ ] Step 1: 写失败的测试（Mockito mock `AmapClient` 与 `CinemaDirectoryMapper`）。

```java
package com.example.springboot.service;

import com.example.springboot.common.AmapClient;
import com.example.springboot.dto.response.CinemaImportPreview;
import com.example.springboot.entity.CinemaDirectory;
import com.example.springboot.mapper.CinemaDirectoryMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CinemaDirectoryImportServiceTest {

    private static AmapClient.CinemaPoi poi(String id, String name, String city) {
        return new AmapClient.CinemaPoi(id, name, "北京市", city, "通州区", "某路1号", "010-1");
    }

    private CinemaDirectoryImportService service(AmapClient client, CinemaDirectoryMapper mapper) {
        return new CinemaDirectoryImportService(client, mapper, new CinemaDirectoryService());
    }

    @Test
    void previewCountsFreshVersusDuplicateAndDoesNotInsert() {
        AmapClient client = mock(AmapClient.class);
        CinemaDirectoryMapper mapper = mock(CinemaDirectoryMapper.class);
        when(client.searchCinemas(eq("北京"), eq(1), anyInt())).thenReturn(
                new AmapClient.PoiPage(2, List.of(poi("A", "万达影城(a)", "北京市"),
                        poi("B", "CGV影城(b)", "北京市"))));
        when(mapper.selectExistingPoiIds(anyList())).thenReturn(List.of("A")); // A 已存在

        CinemaImportPreview preview = service(client, mapper).preview(List.of("北京"));

        assertThat(preview.getTotal()).isEqualTo(2);
        assertThat(preview.getFreshCount()).isEqualTo(1);
        assertThat(preview.getDuplicateCount()).isEqualTo(1);
        verify(mapper, never()).insertIgnore(any());
    }

    @Test
    void importInsertsOnlyFreshRowsWithDerivedBrand() {
        AmapClient client = mock(AmapClient.class);
        CinemaDirectoryMapper mapper = mock(CinemaDirectoryMapper.class);
        when(client.searchCinemas(eq("北京"), eq(1), anyInt())).thenReturn(
                new AmapClient.PoiPage(1, List.of(poi("A", "万达影城(a)", "北京市"))));
        when(mapper.selectExistingPoiIds(anyList())).thenReturn(List.of());

        int inserted = service(client, mapper).importAll(List.of("北京"));

        ArgumentCaptor<CinemaDirectory> captor = ArgumentCaptor.forClass(CinemaDirectory.class);
        verify(mapper, times(1)).insertIgnore(captor.capture());
        assertThat(inserted).isEqualTo(1);
        assertThat(captor.getValue().getPoiId()).isEqualTo("A");
        assertThat(captor.getValue().getBrand()).isEqualTo("万达影城");
        assertThat(captor.getValue().getSource()).isEqualTo("amap");
    }

    /** count > 200：必须按区县细分，各区县各自抓，而不是只翻到 200 条就停 */
    @Test
    void subdividesByDistrictWhenCityExceedsTwoHundred() {
        AmapClient client = mock(AmapClient.class);
        CinemaDirectoryMapper mapper = mock(CinemaDirectoryMapper.class);
        when(client.searchCinemas(eq("北京"), eq(1), anyInt()))
                .thenReturn(new AmapClient.PoiPage(300, List.of(poi("A", "万达影城(a)", "北京市"))));
        when(client.districts("北京")).thenReturn(List.of(
                new AmapClient.District("东城区", "110101"),
                new AmapClient.District("通州区", "110112")));
        when(client.searchCinemas(eq("110101"), eq(1), anyInt()))
                .thenReturn(new AmapClient.PoiPage(1, List.of(poi("D1", "影院1", "北京市"))));
        when(client.searchCinemas(eq("110112"), eq(1), anyInt()))
                .thenReturn(new AmapClient.PoiPage(1, List.of(poi("D2", "影院2", "北京市"))));
        when(mapper.selectExistingPoiIds(anyList())).thenReturn(List.of());

        CinemaImportPreview preview = service(client, mapper).preview(List.of("北京"));

        // 城市级那次只用于读 count，真正入库的两个区县 POI 才计数
        assertThat(preview.getTotal()).isEqualTo(2);
        verify(client).districts("北京");
    }
}
```

- [ ] Step 2: 跑测试确认失败。

运行：`mvn -q -Dtest=CinemaDirectoryImportServiceTest test`

预期：编译失败（服务与 `CinemaDirectoryService` 未实现）。

- [ ] Step 3: 先建查询服务 `CinemaDirectoryService`（测试构造里已用到）。

```java
package com.example.springboot.service;

import com.example.springboot.entity.CinemaDirectory;
import com.example.springboot.mapper.CinemaDirectoryMapper;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CinemaDirectoryService {

    @Resource
    private CinemaDirectoryMapper mapper;

    public PageInfo<CinemaDirectory> selectPage(CinemaDirectory query, Integer pageNum, Integer pageSize) {
        PageMethod.startPage(pageNum, pageSize);
        return PageInfo.of(mapper.selectAll(query));
    }

    public List<String> distinctCities() {
        return mapper.distinctCities();
    }

    public List<String> distinctBrands() {
        return mapper.distinctBrands();
    }
}
```

- [ ] Step 4: 实现导入服务。

```java
package com.example.springboot.service;

import com.example.springboot.common.AmapClient;
import com.example.springboot.common.CinemaBrand;
import com.example.springboot.dto.response.CinemaImportPreview;
import com.example.springboot.entity.CinemaDirectory;
import com.example.springboot.mapper.CinemaDirectoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 从高德抓影院、整理成名录。
 * 高德 v5 同一组请求参数翻页上限 200 条，故某市命中数 >200 时按区县细分再抓（见 fetchCity）。
 * 预览与导入都各自抓一次：确认两步之间数据可能变，且 poi_id 唯一让导入天然幂等，
 * 值得多花一次调用换取「预览即所见」。
 */
@Service
public class CinemaDirectoryImportService {

    /** 高德单组参数上限：page_size ≤ 25、翻页最多 200 条 */
    private static final int PAGE_SIZE = 25;
    private static final int MAX_PAGES = 8; // 8 × 25 = 200
    private static final int SUBDIVIDE_THRESHOLD = 200;
    private static final int SAMPLE_SIZE = 10;

    private final AmapClient client;
    private final CinemaDirectoryMapper mapper;
    private final CinemaDirectoryService directoryService;

    public CinemaDirectoryImportService(AmapClient client,
                                        CinemaDirectoryMapper mapper,
                                        CinemaDirectoryService directoryService) {
        this.client = client;
        this.mapper = mapper;
        this.directoryService = directoryService;
    }

    public CinemaImportPreview preview(List<String> cities) {
        List<String> warnings = new ArrayList<>();
        List<AmapClient.CinemaPoi> unique = fetchUnique(cities, warnings);

        List<String> ids = unique.stream().map(AmapClient.CinemaPoi::poiId).toList();
        Set<String> existing = existingPoiIds(ids);
        int duplicates = (int) ids.stream().filter(existing::contains).count();

        CinemaImportPreview preview = new CinemaImportPreview();
        preview.setTotal(unique.size());
        preview.setDuplicateCount(duplicates);
        preview.setFreshCount(unique.size() - duplicates);
        preview.setSample(unique.stream().limit(SAMPLE_SIZE).map(AmapClient.CinemaPoi::name).toList());
        preview.setWarnings(warnings);
        return preview;
    }

    /** 重新抓一次并幂等插入：刚被并发导入过的 poi_id 由 INSERT IGNORE 静默跳过。返回真正插入数。 */
    @Transactional(rollbackFor = Exception.class)
    public int importAll(List<String> cities) {
        List<String> warnings = new ArrayList<>();
        List<AmapClient.CinemaPoi> unique = fetchUnique(cities, warnings);
        Set<String> existing = existingPoiIds(unique.stream().map(AmapClient.CinemaPoi::poiId).toList());

        int inserted = 0;
        for (AmapClient.CinemaPoi poi : unique) {
            if (existing.contains(poi.poiId())) {
                continue;
            }
            inserted += mapper.insertIgnore(toEntity(poi));
        }
        return inserted;
    }

    private List<AmapClient.CinemaPoi> fetchUnique(List<String> cities, List<String> warnings) {
        Map<String, AmapClient.CinemaPoi> byId = new LinkedHashMap<>();
        for (String city : cities) {
            for (AmapClient.CinemaPoi poi : fetchCity(city, warnings)) {
                if (poi.poiId() != null) {
                    byId.putIfAbsent(poi.poiId(), poi);
                }
            }
        }
        return new ArrayList<>(byId.values());
    }

    private List<AmapClient.CinemaPoi> fetchCity(String city, List<String> warnings) {
        AmapClient.PoiPage first = client.searchCinemas(city, 1, PAGE_SIZE);
        if (first.count() <= SUBDIVIDE_THRESHOLD) {
            return collectRemainingPages(city, 1, first);
        }

        List<AmapClient.District> districts = client.districts(city);
        if (districts.isEmpty()) {
            warnings.add(city + " 影院超过 " + SUBDIVIDE_THRESHOLD + " 条且取不到区县列表，结果可能不完整");
            return collectRemainingPages(city, 1, first);
        }
        List<AmapClient.CinemaPoi> all = new ArrayList<>();
        for (AmapClient.District district : districts) {
            AmapClient.PoiPage page = client.searchCinemas(district.adcode(), 1, PAGE_SIZE);
            if (page.count() > SUBDIVIDE_THRESHOLD) {
                warnings.add(city + district.name() + " 影院仍超 " + SUBDIVIDE_THRESHOLD + " 条，可能未取全");
            }
            all.addAll(collectRemainingPages(district.adcode(), 1, page));
        }
        return all;
    }

    /** first 是第 1 页，继续翻到第 2..min(MAX_PAGES, ceil(count/pageSize)) 页 */
    private List<AmapClient.CinemaPoi> collectRemainingPages(String region, int firstPage,
                                                             AmapClient.PoiPage first) {
        List<AmapClient.CinemaPoi> result = new ArrayList<>(first.pois());
        int totalPages = Math.min(MAX_PAGES,
                (int) Math.ceil(first.count() / (double) PAGE_SIZE));
        for (int page = firstPage + 1; page <= totalPages; page++) {
            AmapClient.PoiPage next = client.searchCinemas(region, page, PAGE_SIZE);
            if (next.pois().isEmpty()) {
                break;
            }
            result.addAll(next.pois());
        }
        return result;
    }

    private Set<String> existingPoiIds(List<String> ids) {
        if (ids.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(mapper.selectExistingPoiIds(ids));
    }

    private static CinemaDirectory toEntity(AmapClient.CinemaPoi poi) {
        CinemaDirectory entity = new CinemaDirectory();
        entity.setPoiId(poi.poiId());
        entity.setName(poi.name());
        entity.setBrand(CinemaBrand.derive(poi.name()));
        entity.setProvince(poi.province());
        entity.setCity(poi.city());
        entity.setDistrict(poi.district());
        entity.setAddress(poi.address());
        entity.setPhone(poi.tel());
        entity.setSource("amap");
        return entity;
    }
}
```

注意：`CinemaDirectoryService` 在本测试里只被构造传入、未被使用，属预留（Task 7 控制器会用它）。若审查认为多余，可让构造函数只收 `AmapClient` 与 `CinemaDirectoryMapper`，并在测试里去掉第三个参数——二选一，实现时择一并让 Task 6/7 一致。

- [ ] Step 5: 跑测试确认通过。

运行：`mvn -q -Dtest=CinemaDirectoryImportServiceTest test`

预期：3 个用例全绿。

- [ ] Step 6: 提交。

```bash
git add xm_film/springboot/src/main/java/com/example/springboot/service/CinemaDirectoryService.java xm_film/springboot/src/main/java/com/example/springboot/service/CinemaDirectoryImportService.java xm_film/springboot/src/test/java/com/example/springboot/service/CinemaDirectoryImportServiceTest.java
git commit -m "feat: 影院名录导入服务 —— 城市抓取、超200按区县细分、poi_id 幂等去重"
```

## Task 7: 控制器 + 拦截器门禁

Files：Create `controller/CinemaDirectoryController.java`; Modify `common/config/AuthInterceptor.java`, `test/.../AuthInterceptorAccessTest.java`。

- [ ] Step 1: 写失败的拦截器测试（追加到 `AuthInterceptorAccessTest`）。

```java
    // ========== 影院名录：GET 公开，导入仅管理员 ==========

    @Test
    void anonymousCanReadCinemaDirectory() {
        assertThat(anonymousAllowed("/api/v1/cinema-directory/page", "GET")).isTrue();
    }

    @Test
    void userAndCinemaCannotRunCinemaDirectoryImport() {
        assertThat(hasAccess(newInterceptor(), "/api/v1/cinema-directory/import", "POST", "USER")).isFalse();
        assertThat(hasAccess(newInterceptor(), "/api/v1/cinema-directory/import", "POST", "CINEMA")).isFalse();
        assertThat(hasAccess(newInterceptor(), "/api/v1/cinema-directory/import/preview", "POST", "USER")).isFalse();
    }

    @Test
    void adminCanRunCinemaDirectoryImport() {
        assertThat(hasAccess(newInterceptor(), "/api/v1/cinema-directory/import", "POST", "ADMIN")).isTrue();
    }

    @Test
    void anonymousCannotRunCinemaDirectoryImport() {
        assertThat(anonymousAllowed("/api/v1/cinema-directory/import", "POST")).isFalse();
    }
```

- [ ] Step 2: 跑测试确认失败。

运行：`mvn -q -Dtest=AuthInterceptorAccessTest test`

预期：新增的 4 个用例失败（前缀尚未登记）。

- [ ] Step 3: 改 `AuthInterceptor`。

`PUBLIC_READ_PREFIXES` 末尾加一条：

```java
            // 影院名录是公开资料，游客可读
            "/api/v1/cinema-directory"
```

`ADMIN_WRITE_PREFIXES` 末尾加一条：

```java
            // 影院名录导入：抓取会写库（建目录行），非管理员不得触发；前缀命中 /import 与 /import/preview
            "/api/v1/cinema-directory/import"
```

- [ ] Step 4: 建控制器。

```java
package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.dto.request.CinemaImportRequest;
import com.example.springboot.entity.CinemaDirectory;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.CinemaDirectoryImportService;
import com.example.springboot.service.CinemaDirectoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 影院名录。GET 是公开只读资源；/import 与 /import/preview 会触发高德抓取与写库，
 * 由 AuthInterceptor 的 ADMIN_WRITE_PREFIXES 拦在非管理员之外（与本仓 film 写同款前缀级门禁）。
 * 目录行只从高德来，没有第二条写路径。
 */
@RestController
@RequestMapping("/api/v1/cinema-directory")
public class CinemaDirectoryController {

    private final CinemaDirectoryService directoryService;
    private final CinemaDirectoryImportService importService;

    public CinemaDirectoryController(CinemaDirectoryService directoryService,
                                     CinemaDirectoryImportService importService) {
        this.directoryService = directoryService;
        this.importService = importService;
    }

    @GetMapping("/page")
    public Result page(CinemaDirectory query,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "12") Integer pageSize) {
        return Result.success(directoryService.selectPage(query, pageNum, pageSize));
    }

    /** 前台筛选下拉与后台导入选城都用得到 */
    @GetMapping("/filters")
    public Result filters() {
        return Result.success(Map.of(
                "cities", directoryService.distinctCities(),
                "brands", directoryService.distinctBrands()));
    }

    @PostMapping("/import/preview")
    public Result preview(@RequestBody CinemaImportRequest request) {
        return Result.success(importService.preview(requireCities(request)));
    }

    @PostMapping("/import")
    public Result doImport(@RequestBody CinemaImportRequest request) {
        return Result.success(importService.importAll(requireCities(request)));
    }

    private static List<String> requireCities(CinemaImportRequest request) {
        if (request == null || request.getCities() == null || request.getCities().isEmpty()) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "请至少选择一个城市");
        }
        return request.getCities();
    }
}
```

- [ ] Step 5: 跑测试确认通过。

运行：`mvn -q -Dtest=AuthInterceptorAccessTest test`

预期：全部用例（含新增 4 个）绿。

- [ ] Step 6: 提交。

```bash
git add xm_film/springboot/src/main/java/com/example/springboot/controller/CinemaDirectoryController.java xm_film/springboot/src/main/java/com/example/springboot/common/config/AuthInterceptor.java xm_film/springboot/src/test/java/com/example/springboot/AuthInterceptorAccessTest.java
git commit -m "feat: 影院名录接口 —— GET 公开、导入走 ADMIN_WRITE 前缀门禁"
```

## Task 8: 用真 Key 实测，校正字段契约

Files：Modify `test/.../AmapClientTest.java`（如实测结构不同才改）。

- [ ] Step 1: 真实抓一条，核对字段。

需要你已把 Key 填进 `application.yml`。用 `!` 前缀在本机跑；本机 HTTP 代理不影响 `curl` 直连国内域名，若报 502 是代理介入，加 `--noproxy '*'`。

```
!curl -s "https://restapi.amap.com/v5/place/text?key=<你的Key>&types=080601&region=北京&page_size=25&page_num=1&show_fields=business" | head -c 2000
```

预期：`status":"1"`，`pois[]` 内每条含 `id/name/pname/cityname/adname/address/typecode`，`business.tel` 有电话。

- [ ] Step 2: 若结构与 `AmapClientTest` 的 JSON 有出入，按实测改测试常量与 `AmapClient` 取值路径；无出入则跳过。

- [ ] Step 3: 跑全量后端测试。

运行：`mvn -q test`

预期：全绿（含既有 200+ 用例）。

- [ ] Step 4: 提交（若第 2 步有改动）。

```bash
git add -A xm_film/springboot/src
git commit -m "fix: 按高德实测响应校正字段契约"
```

## Task 9: 前端常量 + 路由 + 导航

Files：Modify `constants/index.js`, `router/index.js`, `views/Front.vue`, `views/Manage.vue`。

- [ ] Step 1: 常量（`constants/index.js`）。

`API_PATHS` 内加 `CINEMA_DIRECTORY: '/api/v1/cinema-directory',`，并追加：

```js
/** 影院名录（高德导入的只读资料）。PAGE/FILTERS 匿名可读；IMPORT* 仅管理员 */
export const CINEMA_DIRECTORY_API = {
  PAGE: apiPage(API_PATHS.CINEMA_DIRECTORY),
  FILTERS: `${API_PATHS.CINEMA_DIRECTORY}/filters`,
  IMPORT_PREVIEW: `${API_PATHS.CINEMA_DIRECTORY}/import/preview`,
  IMPORT: `${API_PATHS.CINEMA_DIRECTORY}/import`,
}

/** 后台导入可选城市：一线 + 新一线 + 省会 */
export const DIRECTORY_IMPORT_CITIES = [
  '北京','上海','广州','深圳','成都','重庆','杭州','武汉','西安','南京',
  '天津','苏州','长沙','郑州','东莞','青岛','沈阳','宁波','昆明','合肥',
  '佛山','福州','厦门','哈尔滨','济南','大连','南宁','石家庄','长春','泉州',
  '贵阳','南昌','常州','南通','嘉兴','徐州','太原','烟台','兰州','珠海',
  '海口','乌鲁木齐','呼和浩特','银川','西宁',
]
```

URL 契约：`PAGE` = `/api/v1/cinema-directory/page`、`FILTERS` = `/api/v1/cinema-directory/filters`、`IMPORT*` = `/api/v1/cinema-directory/import[/preview]`，逐段比对后端 `@RequestMapping` + `@GetMapping/@PostMapping`——构建通过不代表契约通。

- [ ] Step 2: 路由（`router/index.js`）。

`/manage` 的 children 追加：

```js
        { path: 'cinemaDirectory', meta: { name: '影院名录' }, component: () => import('../views/manage/CinemaDirectory.vue') },
```

`/front` 的 children 追加：

```js
        { path: 'cinemaDirectory', meta: { guest: true, name: '影院名录' }, component: () => import('../views/front/CinemaDirectory.vue') },
```

- [ ] Step 3: 前台导航（`views/Front.vue` 的 `NAV_ITEMS`）。

在 `电影` 与 `影院` 项之间或之后追加：

```js
  { path: '/front/cinemaDirectory', label: '影院名录', sections: ['/front/cinemaDirectory'] },
```

- [ ] Step 4: 后台菜单（`views/Manage.vue`）。

在「信息管理」子菜单内（`index="/manage/room"` 之后）追加：

```html
            <el-menu-item index="/manage/cinemaDirectory">
              <el-icon><OfficeBuilding /></el-icon>
              <span>影院名录</span>
            </el-menu-item>
```

`OfficeBuilding` 已在该文件 import 使用，无需新增 import；若报未定义再补。

- [ ] Step 5: 占位组件先建，保证可编译。

`views/front/CinemaDirectory.vue` 与 `views/manage/CinemaDirectory.vue` 先写最小骨架（Task 10、11 填充）：

```vue
<template><div class="page-narrow">影院名录</div></template>
<script setup></script>
```

- [ ] Step 6: 构建验证。

运行：`cd xm_film/vue && npm run build`

预期：构建成功，dist 含 `CinemaDirectory` 相关 chunk。

- [ ] Step 7: 提交。

```bash
git add xm_film/vue/src/constants/index.js xm_film/vue/src/router/index.js xm_film/vue/src/views/Front.vue xm_film/vue/src/views/Manage.vue xm_film/vue/src/views/front/CinemaDirectory.vue xm_film/vue/src/views/manage/CinemaDirectory.vue
git commit -m "feat(web): 影院名录前端骨架 —— 常量、路由、前后台入口"
```

## Task 10: 前台「影院名录」页

Files：Modify `views/front/CinemaDirectory.vue`。

- [ ] Step 1: 实现页面（复用 `front/Cinema.vue` 的卡片风格；筛选 = 城市 + 品牌；无购票链路）。

```vue
<template>
  <div class="page-narrow">
    <div class="directory-filters">
      <el-select v-model="query.city" placeholder="全部城市" clearable class="directory-filters__item"
                 @change="reload">
        <el-option v-for="c in filterData.cities" :key="c" :label="c" :value="c"/>
      </el-select>
      <el-select v-model="query.brand" placeholder="全部院线" clearable class="directory-filters__item"
                 @change="reload">
        <el-option v-for="b in filterData.brands" :key="b" :label="b" :value="b"/>
      </el-select>
    </div>

    <el-table :data="rows" border class="directory-table">
      <el-table-column prop="name" label="影院名称" min-width="220"/>
      <el-table-column prop="brand" label="院线" width="140">
        <template #default="{ row }">{{ row.brand || '独立/其他' }}</template>
      </el-table-column>
      <el-table-column label="省 / 市 / 区" min-width="200">
        <template #default="{ row }">{{ [row.province, row.city, row.district].filter(Boolean).join(' / ') }}</template>
      </el-table-column>
      <el-table-column prop="address" label="详细地址" min-width="260"/>
      <el-table-column prop="phone" label="电话" width="160">
        <template #default="{ row }">{{ row.phone || '—' }}</template>
      </el-table-column>
    </el-table>

    <div v-if="data.error" class="empty-hint">数据加载失败，请稍后重试</div>
    <div v-else-if="!rows.length" class="empty-hint">暂无数据</div>

    <div v-if="total" class="directory-pagination">
      <el-pagination background layout="total, prev, pager, next" :total="total"
                     :page-size="query.pageSize" :current-page="query.pageNum"
                     @current-change="handlePage"/>
    </div>
  </div>
</template>

<script setup>
import { reactive, computed } from 'vue'
import request from '@/utils/request.js'
import { CINEMA_DIRECTORY_API } from '@/constants'

const query = reactive({ city: null, brand: null, pageNum: 1, pageSize: 15 })
const filterData = reactive({ cities: [], brands: [] })
const data = reactive({ list: [], total: 0, error: false })
const rows = computed(() => data.list)
const total = computed(() => data.total)

const loadFilters = () => {
  request.get(CINEMA_DIRECTORY_API.FILTERS).then(res => {
    if (res.code === '200') {
      filterData.cities = res.data.cities
      filterData.brands = res.data.brands
    }
  })
}

const load = () => {
  data.error = false
  request.get(CINEMA_DIRECTORY_API.PAGE, {
    params: { city: query.city || undefined, brand: query.brand || undefined,
              pageNum: query.pageNum, pageSize: query.pageSize }
  }).then(res => {
    if (res.code === '200') {
      data.list = res.data.list
      data.total = res.data.total
    } else {
      data.error = true
    }
  }).catch(() => { data.error = true })
}

const reload = () => { query.pageNum = 1; load() }
const handlePage = (p) => { query.pageNum = p; load() }

loadFilters()
load()
</script>

<style scoped>
.directory-filters { display: flex; gap: var(--space-12); padding: var(--space-16) 0; }
.directory-filters__item { width: 200px; }
.directory-pagination { margin-top: var(--space-16); }
</style>
```

- [ ] Step 2: 构建验证。

运行：`npm run build`

预期：成功。

- [ ] Step 3: 提交。

```bash
git add xm_film/vue/src/views/front/CinemaDirectory.vue
git commit -m "feat(web): 前台影院名录页 —— 按城市/院线筛选的只读浏览"
```

## Task 11: 后台「影院名录」管理页（含高德导入对话框）

Files：Modify `views/manage/CinemaDirectory.vue`。

- [ ] Step 1: 实现页面。

```vue
<template>
  <div>
    <div class="toolbar">
      <el-button type="primary" @click="openImport">从高德导入</el-button>
      <el-select v-model="query.city" placeholder="全部城市" clearable @change="reload">
        <el-option v-for="c in filterData.cities" :key="c" :label="c" :value="c"/>
      </el-select>
      <el-select v-model="query.brand" placeholder="全部院线" clearable @change="reload">
        <el-option v-for="b in filterData.brands" :key="b" :label="b" :value="b"/>
      </el-select>
    </div>

    <el-table :data="rows" border>
      <el-table-column prop="name" label="影院名称" min-width="220"/>
      <el-table-column prop="brand" label="院线" width="140"/>
      <el-table-column prop="province" label="省" width="110"/>
      <el-table-column prop="city" label="市" width="110"/>
      <el-table-column prop="district" label="区县" width="110"/>
      <el-table-column prop="address" label="详细地址" min-width="240"/>
      <el-table-column prop="phone" label="电话" width="150"/>
    </el-table>

    <div class="pagination">
      <el-pagination background layout="total, prev, pager, next" :total="total"
                     :page-size="query.pageSize" :current-page="query.pageNum"
                     @current-change="handlePage"/>
    </div>

    <el-dialog v-model="importDialog.visible" title="从高德导入影院名录" width="560px">
      <el-select v-model="importDialog.cities" multiple filterable placeholder="选择城市（可多选）"
                 class="import-cities">
        <el-option v-for="c in DIRECTORY_IMPORT_CITIES" :key="c" :label="c" :value="c"/>
      </el-select>

      <div v-if="importDialog.preview" class="import-preview">
        <p>抓取 {{ importDialog.preview.total }} 条：新增
          <strong>{{ importDialog.preview.freshCount }}</strong>，
          已存在跳过 <strong>{{ importDialog.preview.duplicateCount }}</strong></p>
        <p class="import-preview__sample">{{ importDialog.preview.sample.join('、') }}</p>
        <p v-for="w in importDialog.preview.warnings" :key="w" class="import-preview__warn">{{ w }}</p>
      </div>

      <template #footer>
        <el-button :loading="importDialog.previewing" @click="doPreview">抓取预览</el-button>
        <el-button type="primary" :disabled="!importDialog.preview" :loading="importDialog.importing"
                   @click="doImport">确认导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request.js'
import { CINEMA_DIRECTORY_API, DIRECTORY_IMPORT_CITIES } from '@/constants'

const query = reactive({ city: null, brand: null, pageNum: 1, pageSize: 10 })
const filterData = reactive({ cities: [], brands: [] })
const data = reactive({ list: [], total: 0 })
const rows = computed(() => data.list)
const total = computed(() => data.total)

const importDialog = reactive({
  visible: false, cities: [], preview: null, previewing: false, importing: false,
})

const loadFilters = () => request.get(CINEMA_DIRECTORY_API.FILTERS).then(res => {
  if (res.code === '200') { filterData.cities = res.data.cities; filterData.brands = res.data.brands }
})

const load = () => request.get(CINEMA_DIRECTORY_API.PAGE, {
  params: { city: query.city || undefined, brand: query.brand || undefined,
            pageNum: query.pageNum, pageSize: query.pageSize }
}).then(res => { if (res.code === '200') { data.list = res.data.list; data.total = res.data.total } })

const reload = () => { query.pageNum = 1; load() }
const handlePage = (p) => { query.pageNum = p; load() }

const requireCities = () => {
  if (!importDialog.cities.length) { ElMessage.warning('请先选择城市'); return false }
  return true
}

const openImport = () => {
  importDialog.visible = true
  importDialog.preview = null
}

const doPreview = () => {
  if (!requireCities()) return
  importDialog.previewing = true
  request.post(CINEMA_DIRECTORY_API.IMPORT_PREVIEW, { cities: importDialog.cities })
    .then(res => {
      if (res.code === '200') importDialog.preview = res.data
      else ElMessage.error(res.msg)
    })
    .finally(() => { importDialog.previewing = false })
}

const doImport = () => {
  importDialog.importing = true
  request.post(CINEMA_DIRECTORY_API.IMPORT, { cities: importDialog.cities })
    .then(res => {
      if (res.code === '200') {
        ElMessage.success(`本次新增 ${res.data} 条`)
        importDialog.visible = false
        importDialog.preview = null
        loadFilters()
        reload()
      } else {
        ElMessage.error(res.msg)
      }
    })
    .finally(() => { importDialog.importing = false })
}

loadFilters()
load()
</script>

<style scoped>
.toolbar { display: flex; gap: var(--space-12); margin-bottom: var(--space-16); }
.import-cities { width: 100%; }
.import-preview { margin-top: var(--space-16); line-height: 1.8; }
.import-preview__sample { color: var(--el-text-color-secondary); font-size: var(--fs-xs); }
.import-preview__warn { color: var(--el-color-warning); }
.pagination { margin-top: var(--space-16); }
</style>
```

- [ ] Step 2: 构建验证。

运行：`npm run build`

预期：成功。

- [ ] Step 3: 提交。

```bash
git add xm_film/vue/src/views/manage/CinemaDirectory.vue
git commit -m "feat(web): 后台影院名录页 —— 列表、筛选与「从高德导入」预览-确认对话框"
```

## Task 12: 端到端联调（需你的 Key）

- [ ] Step 1: 起后端（隔离验证口径：用备用端口 + 临时库，别打扰你自己的 9090/5173）。
- [ ] Step 2: 后台导入一个城市（如"北京"），确认预览新增数 → 确认导入 → 名录页出现数据、品牌派生正确。
- [ ] Step 3: 重跑一次同城导入，预期新增 0（`poi_id` 幂等）。
- [ ] Step 4: 换一个超 200 的城市（如"上海"），看 `warnings` 是否出现细分提示、条数是否显著多于随机抽样。
- [ ] Step 5: 匿名访问前台名录页，确认免登录可读、筛选可用。

## 计划自审

规格覆盖：高德源、独立表 `cinema_directory`、前台公开名录页、后台预览-确认导入、45 城、无经纬度、Key 由你提供，均有对应任务。

占位符扫描：无 TBD；唯一"待填"是 `amap.key` 的配置值（属前置条件，非计划缺口）。

类型一致性：`CinemaPoi.poiId/province/city/district/address/tel` 全程一致；`CinemaDirectory` 字段 ↔ XML `#{...}` ↔ 表列名（`map-underscore-to-camel-case` 已开，`poi_id→poiId`）对齐；`insertIgnore`/`selectExistingPoiIds`/`distinctCities`/`distinctBrands` 在接口、XML、服务、测试四处同名。

待裁定两处：Task 6 里 `CinemaDirectoryImportService` 是否注入未用的 `CinemaDirectoryService`（计划给了"二选一"说明）；Task 8 的实测校正需等 Key。
