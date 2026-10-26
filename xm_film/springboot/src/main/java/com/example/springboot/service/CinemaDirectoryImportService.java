package com.example.springboot.service;

import com.example.springboot.common.AmapClient;
import com.example.springboot.common.CinemaBrand;
import com.example.springboot.dto.response.CinemaImportView;
import com.example.springboot.entity.CinemaDirectory;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.CinemaDirectoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final TransactionTemplate transactionTemplate;

    public CinemaDirectoryImportService(AmapClient client, CinemaDirectoryMapper mapper) {
        this(client, mapper, null);
    }

    @Autowired
    public CinemaDirectoryImportService(AmapClient client, CinemaDirectoryMapper mapper,
                                        TransactionTemplate transactionTemplate) {
        this.client = client;
        this.mapper = mapper;
        this.transactionTemplate = transactionTemplate;
    }

    public CinemaImportView preview(List<String> cities) {
        List<String> warnings = new ArrayList<>();
        List<AmapClient.CinemaPoi> unique = fetchUnique(cities, warnings);

        List<String> ids = unique.stream().map(AmapClient.CinemaPoi::poiId).toList();
        Set<String> existing = existingPoiIds(ids);
        int duplicates = (int) ids.stream().filter(existing::contains).count();

        CinemaImportView preview = new CinemaImportView();
        preview.setTotal(unique.size());
        preview.setDuplicateCount(duplicates);
        preview.setFreshCount(unique.size() - duplicates);
        preview.setSample(unique.stream().limit(SAMPLE_SIZE).map(AmapClient.CinemaPoi::name).toList());
        preview.setWarnings(warnings);
        return preview;
    }

    /**
     * 重新抓一次并幂等插入：刚被并发导入过的 poi_id 由 INSERT IGNORE 静默跳过。返回真正插入数。
     *
     * <p><b>事务边界</b>：抓取在事务之外，只有写库阶段开事务。原实现把整个方法包在
     * {@code @Transactional} 里，于是可能十余次外部 HTTP（1–10 秒）全程占着数据库连接
     * 与行锁，既拖垮连接池，又让并发导入互相排队。这里用 {@link TransactionTemplate}
     * 而不是再拆一个 bean：事务范围显式可见，也不必依赖 Spring AOP 代理
     * （同类内部调用加 {@code @Transactional} 是不生效的，拆方法最容易踩这个坑）。
     */
    public int importAll(List<String> cities) {
        List<String> warnings = new ArrayList<>();
        List<AmapClient.CinemaPoi> unique = fetchUnique(cities, warnings);
        if (transactionTemplate == null) {
            // 仅单元测试用两参构造器时走到这里：没有事务设施，直接执行写库逻辑
            return persist(unique);
        }
        Integer inserted = transactionTemplate.execute(status -> persist(unique));
        return inserted == null ? 0 : inserted;
    }

    /** 写库内核：必须在事务内调用。幂等性由 poi_id 唯一索引保证，不依赖事务外那次预查。 */
    int persist(List<AmapClient.CinemaPoi> unique) {
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
     * 区县抓取的并发超限是可预期事件：逐区县 × 逐页抓取会背靠背打几十个请求，
     * 单是直辖市就有 16 个区县。任一个区县最终失败，目前会抛异常让整个预览/导入失败。
     * 这里改为记录 warning 并跳过该区县 —— 漏一个区县远好过让整次操作白跑。
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
            List<AmapClient.CinemaPoi> sub;
            try {
                sub = fetchAllPages(district.adcode());
            } catch (CustomException e) {
                warnings.add(city + district.name() + " 抓取失败（" + e.getMessage() + "），该区县结果缺失");
                continue;
            }
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
