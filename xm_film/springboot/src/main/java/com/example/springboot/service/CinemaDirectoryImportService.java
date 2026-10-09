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

    public CinemaDirectoryImportService(AmapClient client, CinemaDirectoryMapper mapper) {
        this.client = client;
        this.mapper = mapper;
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
