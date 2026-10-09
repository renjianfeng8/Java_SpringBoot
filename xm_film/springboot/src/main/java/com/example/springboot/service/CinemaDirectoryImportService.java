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

    /** 高德 v5 单组参数硬上限：page_size ≤ 25、翻页最多 8 页 = 200 条（实测第 9 页起恒空） */
    private static final int PAGE_SIZE = 25;
    private static final int MAX_PAGES = 8;
    private static final int PAGE_LIMIT = MAX_PAGES * PAGE_SIZE; // 200
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

    /**
     * 城市级抓取取满 200（= 硬上限）说明还有更多没取到，改用区县细分再抓。
     * 高德 v5 响应没有总命中数字段，count 只是本页条数（实测恒等于本页返回数），
     * 所以只能靠"是否触顶"来判断，不能拿 count 跟 200 比。
     */
    private List<AmapClient.CinemaPoi> fetchCity(String city, List<String> warnings) {
        List<AmapClient.CinemaPoi> all = fetchAllPages(city);
        if (all.size() < PAGE_LIMIT) {
            return all;
        }

        List<AmapClient.District> districts = client.districts(city);
        if (districts.isEmpty()) {
            warnings.add(city + " 影院已达 " + PAGE_LIMIT + " 条上限且取不到区县列表，结果可能不完整");
            return all;
        }
        List<AmapClient.CinemaPoi> byDistrict = new ArrayList<>();
        for (AmapClient.District district : districts) {
            List<AmapClient.CinemaPoi> sub = fetchAllPages(district.adcode());
            if (sub.size() >= PAGE_LIMIT) {
                warnings.add(city + district.name() + " 影院仍达 " + PAGE_LIMIT + " 条上限，可能未取全");
            }
            byDistrict.addAll(sub);
        }
        return byDistrict;
    }

    /** 逐页抓到本页不满（或达 8 页硬上限）为止 —— 高德不给总数，只能这样收尾。 */
    private List<AmapClient.CinemaPoi> fetchAllPages(String region) {
        List<AmapClient.CinemaPoi> result = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            List<AmapClient.CinemaPoi> pois = client.searchCinemas(region, page, PAGE_SIZE);
            result.addAll(pois);
            if (pois.size() < PAGE_SIZE) {
                break;
            }
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
