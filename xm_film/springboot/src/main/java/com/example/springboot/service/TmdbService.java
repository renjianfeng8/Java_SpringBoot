package com.example.springboot.service;

import com.example.springboot.common.FileUtil;
import com.example.springboot.common.TmdbClient;
import com.example.springboot.common.TmdbMapping;
import com.example.springboot.dto.response.TmdbImportPreview;
import com.example.springboot.entity.Actor;
import com.example.springboot.entity.Area;
import com.example.springboot.entity.Type;
import com.example.springboot.mapper.ActorMapper;
import com.example.springboot.mapper.AreaMapper;
import com.example.springboot.mapper.TypeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 把 TMDB 的一部影片整理成「影片表单预填值」。
 *
 * **这里不是事务方法，是有意的。** 类型行、地区行、演职人员行以及图片文件都必须在
 * 影片保存**之前**就绪：表单的下拉框要靠 id 才能选中。也就是说这些写入先于影片存在，
 * 若套上事务并在末尾回滚，回滚掉的是数据库行、留下的却是磁盘文件，反倒不一致。
 * 代价是管理员取消保存会留下孤儿类型/地区/演员行与孤儿图片 —— 每项几十 KB 或一行，
 * 已确认可接受，换取的是「只读端点，落库仍走既有新增影片接口」这条唯一口径。
 */
@Service
public class TmdbService {

    private static final Logger log = LoggerFactory.getLogger(TmdbService.class);

    /** actor.grade 的取值域，与演职人员管理页的下拉一致 */
    private static final String LEAD_ROLE = "主演";

    private final TmdbClient client;
    private final TypeMapper typeMapper;
    private final AreaMapper areaMapper;
    private final ActorMapper actorMapper;
    private final String uploadDir;
    private final String accessPrefix;

    public TmdbService(TmdbClient client,
                       TypeMapper typeMapper,
                       AreaMapper areaMapper,
                       ActorMapper actorMapper,
                       @Value("${file.upload-dir}") String uploadDir,
                       @Value("${file.access-prefix}") String accessPrefix) {
        this.client = client;
        this.typeMapper = typeMapper;
        this.areaMapper = areaMapper;
        this.actorMapper = actorMapper;
        this.uploadDir = uploadDir;
        this.accessPrefix = accessPrefix;
    }

    public List<TmdbClient.SearchHit> search(String query) {
        return client.search(query);
    }

    /** 拉详情并整理成预填值，顺带把地区/类型/演职人员行补齐。 */
    public TmdbImportPreview preview(int tmdbId) {
        TmdbClient.MovieDetail detail = client.detail(tmdbId);
        List<String> warnings = new ArrayList<>();

        TmdbImportPreview preview = new TmdbImportPreview();
        preview.setTitle(detail.title());
        preview.setEnglish(detail.originalTitle());
        preview.setStart(blankToNull(detail.releaseDate()));
        preview.setTime(detail.runtime());
        preview.setContent(detail.overview());
        preview.setEmployee(detail.companyName());
        preview.setLanguage(TmdbMapping.languageFormOption(detail.languageCode()));
        preview.setStatus(TmdbMapping.deriveStatus(detail.releaseDate(), LocalDate.now()));
        preview.setVideo(detail.trailerUrl());
        preview.setImg(download("海报", detail.posterPath(), TmdbClient.POSTER_SIZE, warnings));

        preview.setTypeIds(resolveTypes(detail.genreNames(), warnings));
        resolveArea(detail.countryCode(), preview, warnings);
        resolveLeadActor(detail, preview, warnings);

        preview.setWarnings(warnings);
        return preview;
    }

    /** 类型名（TMDB 在 zh-CN 下直接给中文）→ type 表 id。有就复用，没有才建。 */
    private List<Integer> resolveTypes(List<String> genreNames, List<String> warnings) {
        List<String> names = TmdbMapping.trimToFormLimit(genreNames);
        if (names.size() < genreNames.size()) {
            warnings.add("TMDB 给出 " + genreNames.size() + " 个类型，表单最多 4 个，已截取前 " + names.size() + " 个");
        }

        List<Integer> typeIds = new ArrayList<>();
        for (String name : names) {
            Type existing = typeMapper.selectByTitleExact(name);
            if (existing != null) {
                typeIds.add(existing.getId());
                continue;
            }
            Type created = new Type(null, name);
            typeMapper.insert(created);
            typeIds.add(created.getId());
        }
        return typeIds;
    }

    /** 制片国家代码 → area 表 id。中文名取自 TMDB 自己的国家表，本项目不维护对照表。 */
    private void resolveArea(String countryCode, TmdbImportPreview preview, List<String> warnings) {
        if (countryCode == null || countryCode.isBlank()) {
            return;
        }
        String name = client.countryNames().get(countryCode);
        if (name == null) {
            warnings.add("未识别制片地区「" + countryCode + "」，请手工选择");
            return;
        }

        Area existing = areaMapper.selectByTitleExact(name);
        if (existing != null) {
            preview.setAreaId(existing.getId());
            preview.setAreaName(existing.getTitle());
            return;
        }
        Area created = new Area(null, name);
        areaMapper.insert(created);
        preview.setAreaId(created.getId());
        preview.setAreaName(name);
    }

    /**
     * 只取 cast 首位建一行演职人员。actor 表的一行是「一个人 + 一部电影」的组合，
     * 且 film.actor_id 只指向一行，所以整张 cast 表塞不进现有模型。
     * title/img 是该表自带的影片冗余字段，前台演职人员区会用到，故一并填上。
     */
    private void resolveLeadActor(TmdbClient.MovieDetail detail, TmdbImportPreview preview, List<String> warnings) {
        TmdbClient.CastMember cast = detail.topCast();
        if (cast == null || cast.name() == null || cast.name().isBlank()) {
            return;
        }

        Actor actor = new Actor();
        actor.setActorName(cast.name());
        actor.setFigure(cast.character());
        actor.setGrade(LEAD_ROLE);
        actor.setTitle(detail.title());
        actor.setImg(preview.getImg());
        actor.setPicture(download("演员头像", cast.profilePath(), TmdbClient.PROFILE_SIZE, warnings));

        actorMapper.insert(actor);
        preview.setActorId(actor.getId());
        preview.setActorName(cast.name());
    }

    /**
     * 下载并落盘，返回本地访问地址。任何一步失败都只记一条 warning 后返回 null：
     * 一张图不该打断整次导入，也绝不下发 TMDB 的远程地址（那会让 film.img 出现第二种形态）。
     */
    private String download(String label, String remotePath, String size, List<String> warnings) {
        Optional<byte[]> bytes = client.downloadImage(size, remotePath);
        if (bytes.isEmpty()) {
            if (remotePath != null && !remotePath.isBlank()) {
                warnings.add(label + "下载失败，请手工上传");
            }
            return null;
        }
        try {
            String fileName = FileUtil.saveBytes(bytes.get(), FileUtil.getFileExtension(remotePath), uploadDir);
            return accessPrefix + fileName;
        } catch (IOException e) {
            log.warn("TMDB {} 落盘失败: {}", label, e.getMessage());
            warnings.add(label + "保存失败，请手工上传");
            return null;
        }
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
