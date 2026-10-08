package com.example.springboot.service;

import com.example.springboot.common.TmdbClient;
import com.example.springboot.dto.response.TmdbImportPreview;
import com.example.springboot.entity.Actor;
import com.example.springboot.entity.Area;
import com.example.springboot.entity.Type;
import com.example.springboot.mapper.ActorMapper;
import com.example.springboot.mapper.AreaMapper;
import com.example.springboot.mapper.TypeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 导入编排的行为契约。重点不在「字段搬对了」，而在三条口径：
 * 1) 图片只入本地库、失败就留空，绝不下发 TMDB 远程地址；
 * 2) 类型/地区有就复用、没有才建，且截断发生在建行之前（不产生多余类型行）；
 * 3) 演职人员行建好后把自增 id 回填给表单，否则 film.actor_id 恒为 null。
 */
class TmdbServiceTest {

    private static final String ACCESS_PREFIX = "http://localhost:9090/files/";
    private static final String POSTER_PATH = "/poster.jpg";
    private static final String AVATAR_PATH = "/avatar.jpg";

    @TempDir
    Path uploadDir;

    private final AtomicInteger nextTypeId = new AtomicInteger(100);

    private TmdbClient client;
    private TypeMapper typeMapper;
    private AreaMapper areaMapper;
    private ActorMapper actorMapper;
    private TmdbService service;

    @BeforeEach
    void setUp() {
        client = mock(TmdbClient.class);
        typeMapper = mock(TypeMapper.class);
        areaMapper = mock(AreaMapper.class);
        actorMapper = mock(ActorMapper.class);
        service = new TmdbService(client, typeMapper, areaMapper, actorMapper,
                uploadDir.toString(), ACCESS_PREFIX);
    }

    private TmdbClient.MovieDetail detail(String releaseDate, List<String> genres, TmdbClient.CastMember cast) {
        return new TmdbClient.MovieDetail("搏击俱乐部", "Fight Club", releaseDate, 139,
                "杰克是一个充满中年危机意识的人。", POSTER_PATH, "en", "DE", "Regency Enterprises",
                genres, cast);
    }

    private TmdbClient.CastMember leadCast() {
        return new TmdbClient.CastMember("爱德华·诺顿", "Narrator", AVATAR_PATH);
    }

    /** 让 mock 的 insert 像真实 useGeneratedKeys 那样把自增 id 写回实体 */
    private void stubGeneratedKeys() {
        doAnswer(inv -> {
            ((Type) inv.getArgument(0)).setId(nextTypeId.getAndIncrement());
            return null;
        }).when(typeMapper).insert(any());
        doAnswer(inv -> {
            ((Area) inv.getArgument(0)).setId(200);
            return null;
        }).when(areaMapper).insert(any());
        doAnswer(inv -> {
            ((Actor) inv.getArgument(0)).setId(300);
            return null;
        }).when(actorMapper).insert(any());
    }

    private void stubHappyPath(List<String> genres) {
        when(client.detail(550)).thenReturn(detail("1999-10-15", genres, leadCast()));
        when(client.countryNames()).thenReturn(Map.of("DE", "德国"));
        when(client.downloadImage(anyString(), anyString())).thenReturn(Optional.of(new byte[]{1, 2, 3}));
        stubGeneratedKeys();
    }

    // ========== 字段回填 ==========

    @Test
    void previewBackfillsEveryFieldTheFormExpects() {
        stubHappyPath(List.of("剧情", "惊悚"));

        TmdbImportPreview preview = service.preview(550);

        assertThat(preview.getTitle()).isEqualTo("搏击俱乐部");
        assertThat(preview.getEnglish()).isEqualTo("Fight Club");
        assertThat(preview.getStart()).isEqualTo("1999-10-15");
        assertThat(preview.getTime()).isEqualTo(139);
        assertThat(preview.getLanguage()).isEqualTo("英语");
        assertThat(preview.getContent()).startsWith("杰克");
        assertThat(preview.getEmployee()).isEqualTo("Regency Enterprises");
        assertThat(preview.getStatus()).isEqualTo("已上映");
        assertThat(preview.getTypeIds()).containsExactly(100, 101);
        assertThat(preview.getAreaId()).isEqualTo(200);
        assertThat(preview.getAreaName()).isEqualTo("德国");
        assertThat(preview.getActorId()).isEqualTo(300);
        assertThat(preview.getActorName()).isEqualTo("爱德华·诺顿");
        assertThat(preview.getWarnings()).isEmpty();
    }

    /** 未定档影片：TMDB 给空串，导入留空交必填校验，而不是猜成已上映 */
    @Test
    void previewLeavesStartAndStatusBlankWhenReleaseDateMissing() {
        when(client.detail(550)).thenReturn(detail("", List.of(), null));
        when(client.countryNames()).thenReturn(Map.of());
        when(client.downloadImage(anyString(), anyString())).thenReturn(Optional.empty());

        TmdbImportPreview preview = service.preview(550);

        assertThat(preview.getStart()).isNull();
        assertThat(preview.getStatus()).isEmpty();
    }

    @Test
    void previewMarksUnreleasedFilmAsUpcoming() {
        when(client.detail(550)).thenReturn(detail("2999-01-01", List.of(), null));
        when(client.countryNames()).thenReturn(Map.of());
        when(client.downloadImage(anyString(), anyString())).thenReturn(Optional.empty());

        assertThat(service.preview(550).getStatus()).isEqualTo("待上映");
    }

    @Test
    void previewLeavesLanguageBlankWhenTmdbHasNoSpokenLanguage() {
        // languageCode 为 null（TMDB 的 spoken_languages 整块缺失），映射应留空而不是猜「其他」
        when(client.detail(550)).thenReturn(new TmdbClient.MovieDetail("搏击俱乐部", "Fight Club",
                "1999-10-15", 139, "简介", null, null, null, null, List.of(), null));
        when(client.countryNames()).thenReturn(Map.of());
        when(client.downloadImage(anyString(), anyString())).thenReturn(Optional.empty());

        TmdbImportPreview preview = service.preview(550);

        assertThat(preview.getLanguage()).isEmpty();
        // 其余可映射字段不受影响，仍然回填
        assertThat(preview.getTitle()).isEqualTo("搏击俱乐部");
    }

    // ========== 类型：复用 / 新建 / 先截断后建行 ==========

    @Test
    void previewReusesExistingTypeRowsInsteadOfCreating() {
        stubHappyPath(List.of("剧情"));
        when(typeMapper.selectByTitleExact("剧情")).thenReturn(new Type(9, "剧情"));

        TmdbImportPreview preview = service.preview(550);

        assertThat(preview.getTypeIds()).containsExactly(9);
        verify(typeMapper, never()).insert(any());
    }

    @Test
    void previewCreatesMissingTypeRow() {
        stubHappyPath(List.of("剧情"));

        TmdbImportPreview preview = service.preview(550);

        assertThat(preview.getTypeIds()).containsExactly(100);
        verify(typeMapper).insert(any());
    }

    /** 先截断再建行：否则第 5 个类型会被建出来却没人用，凭空多一行类型 */
    @Test
    void previewTrimsGenresBeforeCreatingAnyRow() {
        stubHappyPath(List.of("剧情", "惊悚", "动作", "科幻", "悬疑"));

        TmdbImportPreview preview = service.preview(550);

        assertThat(preview.getTypeIds()).hasSize(4);
        verify(typeMapper, times(4)).insert(any());
        assertThat(preview.getWarnings()).anySatisfy(w -> assertThat(w).contains("最多 4 个"));
    }

    // ========== 地区 ==========

    @Test
    void previewReusesExistingArea() {
        stubHappyPath(List.of());
        when(areaMapper.selectByTitleExact("德国")).thenReturn(new Area(8, "德国"));

        TmdbImportPreview preview = service.preview(550);

        assertThat(preview.getAreaId()).isEqualTo(8);
        verify(areaMapper, never()).insert(any());
    }

    @Test
    void previewCreatesMissingArea() {
        stubHappyPath(List.of());

        assertThat(service.preview(550).getAreaId()).isEqualTo(200);
        verify(areaMapper).insert(any());
    }

    @Test
    void previewLeavesAreaBlankAndWarnsWhenCountryCodeUnknown() {
        stubHappyPath(List.of());
        when(client.countryNames()).thenReturn(Map.of("US", "美国"));

        TmdbImportPreview preview = service.preview(550);

        assertThat(preview.getAreaId()).isNull();
        assertThat(preview.getWarnings()).anySatisfy(w -> assertThat(w).contains("DE"));
    }

    // ========== 图片：只入本地 ==========

    @Test
    void previewStoresPosterLocallyAndReturnsLocalUrl() throws Exception {
        stubHappyPath(List.of());

        TmdbImportPreview preview = service.preview(550);

        assertThat(preview.getImg()).startsWith(ACCESS_PREFIX).endsWith(".jpg");
        String fileName = preview.getImg().substring(ACCESS_PREFIX.length());
        assertThat(uploadDir.resolve(fileName)).exists();
        assertThat(Files.readAllBytes(uploadDir.resolve(fileName))).containsExactly((byte) 1, (byte) 2, (byte) 3);
    }

    @Test
    void previewLeavesImgNullAndWarnsWhenPosterDownloadFails() {
        stubHappyPath(List.of());
        when(client.downloadImage(eq(TmdbClient.POSTER_SIZE), eq(POSTER_PATH))).thenReturn(Optional.empty());

        TmdbImportPreview preview = service.preview(550);

        assertThat(preview.getImg()).isNull();
        assertThat(preview.getWarnings()).anySatisfy(w -> assertThat(w).contains("海报"));
    }

    // ========== 演职人员：首位主演 ==========

    @Test
    void previewCreatesLeadActorCarryingRoleFilmTitleAndLocalImages() {
        stubHappyPath(List.of());

        service.preview(550);

        ArgumentCaptor<Actor> captor = ArgumentCaptor.forClass(Actor.class);
        verify(actorMapper).insert(captor.capture());
        Actor actor = captor.getValue();
        assertThat(actor.getActorName()).isEqualTo("爱德华·诺顿");
        assertThat(actor.getFigure()).isEqualTo("Narrator");
        assertThat(actor.getGrade()).isEqualTo("主演");
        // actor 表自带的影片冗余字段，前台演职人员区会读
        assertThat(actor.getTitle()).isEqualTo("搏击俱乐部");
        assertThat(actor.getImg()).startsWith(ACCESS_PREFIX);
        assertThat(actor.getPicture()).startsWith(ACCESS_PREFIX);
    }

    /** 头像缺失很常见：演职人员行照建，只是没有照片 */
    @Test
    void previewKeepsActorButLeavesPictureNullWhenAvatarDownloadFails() {
        stubHappyPath(List.of());
        when(client.downloadImage(eq(TmdbClient.PROFILE_SIZE), eq(AVATAR_PATH))).thenReturn(Optional.empty());

        TmdbImportPreview preview = service.preview(550);

        ArgumentCaptor<Actor> captor = ArgumentCaptor.forClass(Actor.class);
        verify(actorMapper).insert(captor.capture());
        assertThat(captor.getValue().getPicture()).isNull();
        assertThat(preview.getActorId()).isEqualTo(300);
        assertThat(preview.getWarnings()).anySatisfy(w -> assertThat(w).contains("演员头像"));
    }

    @Test
    void previewSkipsActorWhenTmdbHasNoCast() {
        stubHappyPath(List.of());
        when(client.detail(550)).thenReturn(detail("1999-10-15", List.of(), null));

        TmdbImportPreview preview = service.preview(550);

        assertThat(preview.getActorId()).isNull();
        assertThat(preview.getActorName()).isNull();
        verify(actorMapper, never()).insert(any());
    }
}
