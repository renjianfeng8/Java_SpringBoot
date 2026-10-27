package com.example.springboot;

import com.example.springboot.entity.Actor;
import com.example.springboot.entity.Area;
import com.example.springboot.entity.Type;
import com.example.springboot.mapper.ActorMapper;
import com.example.springboot.mapper.AreaMapper;
import com.example.springboot.mapper.TypeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 绑定层守卫：`mvn compile` 不解析 mapper XML，语句 id 写错、#{参数名} 写错都能编译通过，
 * 只有真正执行才暴露。这里把 TMDB 导入用到的两条 type/area 语句各跑一遍。
 * <p>
 * 判重查询传一个不可能存在的标题，证明绑定有效且不触碰真实数据；
 * 自增 id 回填必须真插一行，靠 <b>类级 {@code @Transactional} 回滚</b> 收尾。
 * <p>
 * 回滚而不是 {@code finally deleteById}：断言失败、JVM 被杀、断电都不会走到 finally，
 * 脏行就留在库里；事务回滚由 Spring 在用例结束后无条件执行。
 * 那些 {@code finally} 块保留，是为了让 {@code selectByTitles} 在**同一事务内**
 * 看不到自己刚插的行之外的行 —— 实际上同名唯一性靠的是标题探针，不是删除。
 * <p>
 * {@code @ActiveProfiles("ci")} 指向独立空结构库 xm_film_test（见 application-ci.yml）：
 * 本类会写库，绝不能落在开发库 xm-film 上。
 */
@SpringBootTest
@ActiveProfiles("ci")
@Transactional
class TmdbLookupMapperLoadTest {

    private static final String PROBE_TITLE = "__tmdb_import_probe__";

    @Autowired
    TypeMapper typeMapper;

    @Autowired
    AreaMapper areaMapper;

    @Autowired
    ActorMapper actorMapper;

    @Test
    void typeExactLookupReturnsNullWhenAbsent() {
        assertThat(typeMapper.selectByTitles(List.of(PROBE_TITLE))).isEmpty();
    }

    @Test
    void areaExactLookupReturnsNullWhenAbsent() {
        assertThat(areaMapper.selectByTitleExact(PROBE_TITLE)).isNull();
    }

    /**
     * 导入靠回填的自增 id 把新类型填进表单 typeIds，拿不到 id 就会静默回填空数组，
     * 表现为「导入成功了但类型没填上」。本用例钉住 id 确实被回填。
     */
    @Test
    void typeInsertBackfillsGeneratedId() {
        Type type = new Type(null, PROBE_TITLE);
        try {
            typeMapper.insert(type);

            assertThat(type.getId()).isNotNull();
            assertThat(typeMapper.selectByTitles(List.of(PROBE_TITLE)))
                    .singleElement()
                    .extracting(Type::getId)
                    .isEqualTo(type.getId());
        } finally {
            if (type.getId() != null) {
                typeMapper.deleteById(type.getId());
            }
        }
    }

    @Test
    void areaInsertBackfillsGeneratedId() {
        Area area = new Area(null, PROBE_TITLE);
        try {
            areaMapper.insert(area);

            assertThat(area.getId()).isNotNull();
            assertThat(areaMapper.selectByTitleExact(PROBE_TITLE).getId()).isEqualTo(area.getId());
        } finally {
            if (area.getId() != null) {
                areaMapper.deleteById(area.getId());
            }
        }
    }

    /**
     * 演职人员行同理：拿不到自增 id，导入就无法把 actorId 回填给表单，
     * film.actor_id 会恒为 null、前台演职人员区永远是空的。
     */
    @Test
    void actorInsertBackfillsGeneratedId() {
        Actor actor = new Actor();
        actor.setActorName(PROBE_TITLE);
        // actor.title 是 NOT NULL 且没有默认值 —— 导入路径必须把影片名一并写进去，
        // 缺这一项插入会被数据库直接拒绝（这条约束就靠本用例钉住）
        actor.setTitle(PROBE_TITLE);
        try {
            actorMapper.insert(actor);

            assertThat(actor.getId()).isNotNull();
            Actor saved = actorMapper.selectById(actor.getId());
            assertThat(saved.getActorName()).isEqualTo(PROBE_TITLE);
            assertThat(saved.getTitle()).isEqualTo(PROBE_TITLE);
        } finally {
            if (actor.getId() != null) {
                actorMapper.deleteById(actor.getId());
            }
        }
    }
}
