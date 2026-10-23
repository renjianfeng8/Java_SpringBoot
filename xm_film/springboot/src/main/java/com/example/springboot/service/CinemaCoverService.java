package com.example.springboot.service;

import com.example.springboot.common.AmapClient;
import com.example.springboot.common.FileUtil;
import com.example.springboot.mapper.CinemaDirectoryMapper;
import com.example.springboot.mapper.CinemaMapper;
import com.example.springboot.entity.CinemaDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * 把影院封面从高德补到 {@code cinema.avatar}。
 *
 * <h2>为什么分两步</h2>
 * 高德只给图片 URL，而本项目其它图片都落在 {@code /files（file.upload-dir）}。
 * 「查 URL」很便宜、可放在导入的事务里；「下载字节」是外部 IO，按工程约定不得出现在
 * {@code @Transactional} 方法体内（BL-45），所以下载 + 落库单独成事务，每次只包一个影院。
 *
 * <h2>为什么失败不抛</h2>
 * 封面是锦上添花：POI 被高德下架、图片 404、网络抖动都只该让这家影院继续用默认图。
 * 抛异常会让「从名录导入影院」整批回滚（导入是管理员一次性动作，不能因为一张图失败）。
 */
@Service
public class CinemaCoverService {

    private static final Logger log = LoggerFactory.getLogger(CinemaCoverService.class);

    /** 高德返回的可能不是 jpg，但 FileUtil 按扩展名落盘，统一用 .jpg（浏览器按内容嗅探） */
    private static final String IMAGE_EXTENSION = ".jpg";

    private final AmapClient amapClient;

    private final CinemaMapper cinemaMapper;

    private final CinemaDirectoryMapper directoryMapper;

    private final TransactionTemplate transactionTemplate;

    private final String uploadDir;

    private final String accessPrefix;

    public CinemaCoverService(AmapClient amapClient, CinemaMapper cinemaMapper,
                              CinemaDirectoryMapper directoryMapper, TransactionTemplate transactionTemplate,
                              @Value("${file.upload-dir}") String uploadDir,
                              @Value("${file.access-prefix}") String accessPrefix) {
        this.amapClient = amapClient;
        this.cinemaMapper = cinemaMapper;
        this.directoryMapper = directoryMapper;
        this.transactionTemplate = transactionTemplate;
        this.uploadDir = uploadDir;
        this.accessPrefix = accessPrefix;
    }

    /** 导入影院时调用：只查 URL，不下载。取不到就留空，由前端兜底。 */
    public String lookupCoverUrl(String poiId) {
        try {
            Optional<String> url = amapClient.firstPhotoUrl(poiId);
            if (url.isEmpty()) {
                log.info("高德无封面 poi={}（POI 可能已下架或无门店图），保留默认图", poiId);
            }
            return url.orElse(null);
        } catch (RuntimeException e) {
            log.warn("高德取影院封面失败 poi={}: {}", poiId, e.getMessage());
            return null;
        }
    }

    /**
     * 补齐所有「有高德名录、但还没有封面」的影院：逐家下载并转存到 upload-dir。
     *
     * @return 实际补上的数量
     */
    public int backfillMissingCovers() {
        int updated = 0;
        for (var target : cinemaMapper.selectMissingCovers()) {
            if (fillOneCover(target.cinemaId(), target.poiId())) {
                updated++;
            }
        }
        return updated;
    }

    /** 补齐一家影院；返回是否真的写入了新封面 */
    public boolean fillOneCover(Integer cinemaId, String poiId) {
        // 先取名录（若调用方只给了 cinemaId）：影院不一定关联了名录
        String resolvedPoi = poiId;
        if (resolvedPoi == null) {
            CinemaDirectory directory = directoryOf(cinemaId);
            resolvedPoi = directory == null ? null : directory.getPoiId();
        }
        if (resolvedPoi == null) {
            return false;
        }
        String url = lookupCoverUrl(resolvedPoi);
        if (url == null) {
            return false;
        }
        Optional<byte[]> bytes = amapClient.downloadImage(url);
        if (bytes.isEmpty()) {
            return false;
        }
        String storedUrl;
        try {
            String fileName = FileUtil.saveBytes(bytes.get(), IMAGE_EXTENSION, uploadDir);
            storedUrl = accessPrefix + fileName;
        } catch (IOException e) {
            log.warn("影院封面落盘失败 cinema={}: {}", cinemaId, e.getMessage());
            return false;
        }
        // 只包写库这一段：文件已落盘，DB 写失败最多留下一张孤儿图，不影响数据一致性
        transactionTemplate.executeWithoutResult(status -> cinemaMapper.updateAvatar(cinemaId, storedUrl));
        log.info("影院封面已补 cinema={} poi={} url={}", cinemaId, resolvedPoi, storedUrl);
        return true;
    }

    private CinemaDirectory directoryOf(Integer cinemaId) {
        var cinema = cinemaMapper.selectById(cinemaId);
        if (cinema == null || cinema.getDirectoryId() == null) {
            return null;
        }
        return directoryMapper.selectByIds(List.of(cinema.getDirectoryId())).stream()
                .findFirst().orElse(null);
    }
}
