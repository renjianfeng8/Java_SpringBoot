<template>
  <div class="page-wide home-page">

    <!-- 左侧内容 -->
    <div class="home-main">

      <!-- Hero 轮播：取正在热映的影片。鼠标悬停或键盘聚焦时暂停自动切换，
           免得用户正在看/正要点击时内容被换走 -->
      <section
          v-if="heroFilms.length"
          class="hero"
          aria-label="热门影片"
          @mouseenter="pauseHero"
          @mouseleave="resumeHero"
          @focusin="pauseHero"
          @focusout="resumeHero"
      >
        <div class="hero__stage">
          <!-- 横向轨道，一张占一屏。heroSlides 末尾可能多一张首片克隆，
               滑到克隆后由 @transitionend 瞬移回真正的第一张，接缝不可见 -->
          <div
              ref="trackRef"
              class="hero__track"
              :class="{ 'hero__track--snap': heroSnapping }"
              :style="{ '--hero-index': `${heroIndex}` }"
              @transitionend="onTrackTransitionEnd"
          >
            <div
                v-for="(film, index) in heroSlides"
                :key="index"
                class="hero__slide"
                :class="{ 'hero__slide--active': index === heroIndex }"
                :aria-hidden="index !== heroIndex"
            >
              <!-- 首屏大图不用 loading="lazy"：轨道里靠后的几张横向排在视口外，
                   懒加载会一直不触发，滑过去是一片空白 -->
              <img class="hero__img" :src="film.img" :alt="`《${film.title}》剧照`">
              <div class="hero__scrim" aria-hidden="true"></div>
              <div class="hero__body">
                <h2 class="hero__title">{{ film.title }}</h2>
                <p class="hero__meta">{{ typeText(film) }}</p>
                <p v-if="scoreOf(film)" class="hero__score">{{ scoreOf(film) }}</p>
                <router-link class="hero__cta" :to="`/front/filmDetail/${film.id}`">购票</router-link>
              </div>
            </div>
          </div>
        </div>

        <div v-if="heroFilms.length > 1" class="hero__dots" @mouseleave="cancelHeroHover">
          <button
              v-for="(film, index) in heroFilms"
              :key="film.id"
              class="hero__dot"
              :class="{ 'hero__dot--active': index === heroDotIndex }"
              :aria-label="`切换到第 ${index + 1} 张：${film.title}`"
              :aria-current="index === heroDotIndex"
              @mouseenter="goHeroHover(index)"
              @click="goHero(index)"
          >
            <span class="hero__dot__bar" aria-hidden="true"></span>
          </button>
        </div>
      </section>

      <!-- 正在热映 -->
      <div class="section-head">
        <h2 class="section-head__title">正在热映（{{ data.data1.length }} 部）</h2>
        <!-- 「全部」不带筛选参数：影片列表页目前只支持 类型/年代/区域 三个筛选，
             没有按上映状态筛的口径（`/front/movie` 也从不读 query），
             带上一个没人消费的 ?type= 只会让人以为这里真的筛过 -->
        <router-link class="section-head__more" to="/front/movie">
          全部 ›
        </router-link>
      </div>

      <div class="poster-grid">
        <FilmPosterCard v-for="item in data.playingData" :key="item.id" :film="item">
          <el-button class="poster-buy" size="small" plain type="primary" @click="goToFilmDetail(item.id)">
            购票
          </el-button>
        </FilmPosterCard>
      </div>
      <div v-if="data.error" class="empty-hint">数据加载失败，请稍后重试</div>
      <div v-else-if="!data.playingData.length" class="empty-hint">暂无数据</div>

      <!-- 即将上映 -->
      <div class="section-head home-main__upcoming">
        <h2 class="section-head__title">即将上映（{{ data.data2.length }} 部）</h2>
        <router-link class="section-head__more" to="/front/movie">
          全部 ›
        </router-link>
      </div>

      <div class="poster-grid">
        <FilmPosterCard v-for="item in data.noPlayData" :key="item.id" :film="item">
          <div class="upcoming-date">{{ item.start }} 上映</div>
        </FilmPosterCard>
      </div>
      <div v-if="data.error" class="empty-hint">数据加载失败，请稍后重试</div>
      <div v-else-if="!data.noPlayData.length" class="empty-hint">暂无数据</div>
    </div>

    <!-- 右侧栏 -->
    <aside class="home-aside">
      <!-- 今日票房：GET /api/v1/films/box-office/today（匿名可读，游客也看得到） -->
      <div class="today-box">
        <div class="today-box__strip" aria-hidden="true">
          <span>今</span><span>日</span><span>票</span><span>房</span>
        </div>
        <div class="today-box__body" :aria-busy="loading.today">
          <!-- 仅首屏未拿到数据时占位；刷新时保留数字、只让按钮转圈，避免骨架屏闪烁 -->
          <div v-if="loading.today && todayBoxOffice.total === null" class="today-box__skeleton">
            <el-skeleton :rows="2" animated />
          </div>
          <template v-else>
            <div
                class="today-box__amount"
                :class="{ 'today-box__amount--fresh': todayBoxOffice.fresh }"
                aria-live="polite"
                @animationend="todayBoxOffice.fresh = false"
            >{{ todayBoxOffice.total === null ? '—' : formatYuan(todayBoxOffice.total) }}</div>
            <!-- 按钮让到时间戳这排：36px 大字独占整行，不再与按钮争那 208px，数字不折行 -->
            <div class="today-box__foot">
              <span class="today-box__time">
                <template v-if="todayBoxOffice.error && todayBoxOffice.total !== null">
                  <span class="today-box__error">刷新失败，请稍后重试</span> · 数据更新于 {{ todayTime }}
                </template>
                <template v-else-if="todayBoxOffice.error">数据加载失败，请稍后重试</template>
                <template v-else>更新于 {{ todayTime }}</template>
              </span>
              <el-button
                  class="today-box__refresh"
                  link
                  type="primary"
                  :loading="loading.today"
                  @click="loadTodayBoxOffice"
              >
                <!-- loading 时 EP 自带头像转圈；再留一个 Refresh 就是两个图标 -->
                <el-icon v-if="!loading.today"><Refresh /></el-icon>
                {{ loading.today ? '刷新中' : '刷新' }}
              </el-button>
            </div>
          </template>
        </div>
      </div>

      <!-- 总票房 Top 10 -->
      <section aria-label="总票房榜">
        <div class="section-head">
          <h2 class="section-head__title">总票房 Top 10</h2>
        </div>
        <div class="rank-box">
          <div v-if="loading.boxOffice" class="rank-box__loading">
            <el-skeleton :rows="10" :columns="3" avatar class="skeleton--sm" />
          </div>
          <template v-else-if="boxOfficeTop10.length">
            <router-link
                v-for="(movie, index) in boxOfficeTop10"
                :key="movie.id"
                class="rank-row"
                :to="`/front/filmDetail/${movie.id}`"
            >
              <div class="rank-badge" :class="rankBadgeClass(index)">{{ index + 1 }}</div>
              <div class="rank-row__body">
                <div class="rank-row__title">{{ movie.title }}</div>
                <div class="rank-row__meta">{{ typeText(movie) }}</div>
              </div>
              <div class="rank-row__value">{{ formatBoxOffice(movie.boxOffice) }}</div>
            </router-link>
          </template>
          <div v-else class="empty-hint">暂无数据</div>
        </div>
      </section>

      <!-- 评分 Top 5 -->
      <section class="home-aside__section" aria-label="评分榜">
        <div class="section-head">
          <h2 class="section-head__title">评分 Top 5</h2>
          <router-link class="section-head__more" to="/front/rank">查看完整榜单 ›</router-link>
        </div>
        <div class="rank-box">
          <div v-if="loading.mark" class="rank-box__loading">
            <el-skeleton :rows="5" :columns="3" avatar class="skeleton--lg" />
          </div>
          <template v-else-if="ratingTop5.length">
            <router-link
                v-for="(movie, index) in ratingTop5"
                :key="movie.id"
                class="rank-row rank-row--divided"
                :to="`/front/filmDetail/${movie.id}`"
            >
              <div class="rank-badge" :class="rankBadgeClass(index)">{{ index + 1 }}</div>
              <div class="rank-row__poster">
                <img :src="movie.img" :alt="`《${movie.title}》海报`" class="rank-row__img">
              </div>
              <div class="rank-row__body">
                <div class="rank-row__title">{{ movie.title }}</div>
                <div class="rank-row__meta">{{ typeText(movie) }}</div>
                <div class="rank-row__score">{{ formatScore(movie.score) }}</div>
              </div>
            </router-link>
          </template>
          <div v-else class="empty-hint">暂无数据</div>
        </div>
      </section>
    </aside>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';
import request from "@/utils/request.js";
import { API_PATHS, FILM_API } from '@/constants';
import { formatBoxOffice, formatYuan, formatScore, formatFilmTypes } from '@/utils/format.js';
import 'element-plus/theme-chalk/el-skeleton.css';
import 'element-plus/theme-chalk/el-button.css';

const router = useRouter();

const HERO_LIMIT = 5;
const HERO_INTERVAL = 5000;
const HERO_HOVER_DELAY = 120;
// 刷新反馈的最短展示时长：本地聚合查询几十毫秒就返回，不兜底的话转圈一闪而过
const MIN_REFRESH_MS = 450;

const data = reactive({
  data1: [],        // 已上映电影
  data2: [],        // 待上映电影
  playingData: [],  // 正在热映展示数据（前 8 条）
  noPlayData: [],   // 即将上映展示数据（前 8 条）
  error: false      // 与「暂无数据」区分：网络失败不该显示成"没有影片"
});

const boxOfficeTop10 = reactive([]);
const ratingTop5 = reactive([]);
const loading = reactive({
  boxOffice: false,
  mark: false,
  today: false
});

// 今日票房：total 首屏为 null 用于区分「还没拿到数据」与「今天票房确实是 0」
const todayBoxOffice = reactive({
  total: null,
  updatedAt: '',
  error: false,
  fresh: false   // 刚更新过：驱动数字高亮，animationend 时自清
});

// 时间戳只出时刻 —— widget 本身叫「今日票房」，日期是冗余的
const todayTime = computed(() => {
  const raw = todayBoxOffice.updatedAt;
  if (!raw) return '—';
  return raw.length >= 19 ? raw.slice(11, 19) : raw;
});

const goToFilmDetail = (filmId) => {
  if (!filmId) {
    ElMessage.warning('电影ID无效');
    return;
  }
  router.push(`/front/filmDetail/${filmId}`);
};

// 类型文案与评分格式化都走 utils/format.js，与榜单页共用同一份口径
const typeText = formatFilmTypes;
const scoreOf = (film) => (film.score === null || film.score === undefined ? '' : formatScore(film.score));
const rankBadgeClass = (index) => (index < 3 ? `rank-badge--top${index + 1}` : 'rank-badge--plain');

/* ---------- Hero 轮播 ---------- */
const heroFilms = computed(() => data.playingData.slice(0, HERO_LIMIT));

/* 轨道真正渲染的幻灯片 = 热映片 + 首片克隆（只有一张时不克隆，也谈不上轮播） */
const heroSlides = computed(() => {
  if (heroFilms.value.length < 2) return heroFilms.value;
  return [...heroFilms.value, heroFilms.value[0]];
});

const heroIndex = ref(0);
// 瞬移期间置 true，让轨道暂时关掉过渡，否则"跳回第一张"会被播成一次倒卷
const heroSnapping = ref(false);
const trackRef = ref(null);
let heroTimer = null;

/* 克隆张与第一张视觉等价，所以指示条一律按取模后的位置点亮。
   若直接用 heroIndex，每轮自动轮播滑到克隆张的那 300ms 里 5 个条会谁都不亮。 */
const heroDotIndex = computed(() =>
  heroFilms.value.length ? heroIndex.value % heroFilms.value.length : 0
);

// 悬停/聚焦暂停期间必须记住"暂停过"，否则用户点圆点换一张时 startHero 会把
// 计时器重新启动，5 秒后画面在他眼皮底下又自己跳走
const heroPaused = ref(false);

const stopHero = () => {
  if (heroTimer) {
    clearInterval(heroTimer);
    heroTimer = null;
  }
};

const prefersReducedMotion = () =>
  window.matchMedia('(prefers-reduced-motion: reduce)').matches;

/** 启动自动轮播。系统开启「减少动态效果」时不启动 —— 自动替换内容本身就是动效 */
const startHero = () => {
  stopHero();
  if (heroPaused.value || prefersReducedMotion() || heroSlides.value.length < 2) return;
  heroTimer = setInterval(() => {
    // 只前进、不取模：末位是克隆张，靠 onTrackTransitionEnd 瞬移回 0 才叫无缝
    heroIndex.value = Math.min(heroIndex.value + 1, heroSlides.value.length - 1);
  }, HERO_INTERVAL);
};

/* 滑到末位克隆张后瞬移回真正的第一张。
   加 --snap 与改 index 必须落在同一次渲染里，否则"关过渡"晚于"改位移"，
   中间那帧仍会照常播动画。改完立刻读一次布局，把"无过渡 + 位移 0"压进样式系统，
   再撤掉 --snap —— 此时 transform 没变，撤过渡不会触发任何动画。 */
const onTrackTransitionEnd = (event) => {
  if (event.propertyName !== 'transform') return;
  if (heroIndex.value !== heroSlides.value.length - 1) return;
  heroSnapping.value = true;
  heroIndex.value = 0;
  nextTick(() => {
    if (trackRef.value) void trackRef.value.offsetWidth;
    heroSnapping.value = false;
  });
};

const pauseHero = () => {
  heroPaused.value = true;
  stopHero();
};

const resumeHero = () => {
  heroPaused.value = false;
  startHero();
};

const goHero = (index) => {
  cancelHeroHover();
  // 已经（或视觉上已经）停在这一张：不挪轨道。停在克隆张时正好把"要不要倒卷"挡在外面
  if (index === heroDotIndex.value) return;
  heroIndex.value = index;
  // 未暂停时重新计时（手动翻页后不该马上又自动跳）；暂停中则由 startHero 直接返回
  startHero();
};

/* 鼠标扫过一排指示条时若每次 mouseenter 都即时翻页，轨道会在几十毫秒里连滑好几张，
   快到只剩一片过场白（闪白）。改为扫过不生效、在某个条上停住 HERO_HOVER_DELAY 才切：
   扫动途中每个新的 mouseenter 都会重置计时，只有最终停下的那一个条会被触发。 */
let heroHoverTimer = null;

const cancelHeroHover = () => {
  if (heroHoverTimer) {
    clearTimeout(heroHoverTimer);
    heroHoverTimer = null;
  }
};

const goHeroHover = (index) => {
  cancelHeroHover();
  heroHoverTimer = setTimeout(() => {
    heroHoverTimer = null;
    goHero(index);
  }, HERO_HOVER_DELAY);
};

// 数据是异步到达的，且 length 变化时旧的下标可能越界，故重算后归零并重启计时
watch(heroFilms, () => {
  heroIndex.value = 0;
  heroSnapping.value = false;
  startHero();
});

/* ---------- 数据加载 ---------- */

/**
 * 加载今日票房。失败只落错误态，不弹提示 —— 网络异常/超时/5xx 的提示由 request.js
 * 的响应拦截器统一给出，页面再弹一次会让同一次失败弹两遍。
 *
 * 请求无论多快都把 loading 撑满 MIN_REFRESH_MS：本地聚合查询几十毫秒就返回，
 * 不兜底的话转圈一闪而过，用户看不到「点过」这件事。数字高亮压到同一刻触发，
 * 读起来是「转圈停 → 数字亮一下」，而不是两者各走各的。
 * 失败时 total 不回退 —— 瞬时故障不该把已知数值抹成错误文案，改由时间戳那行提示。
 */
const loadTodayBoxOffice = () => {
  loading.today = true;
  todayBoxOffice.error = false;
  todayBoxOffice.fresh = false;
  const startedAt = Date.now();

  const commit = (mutate) => {
    const wait = Math.max(0, MIN_REFRESH_MS - (Date.now() - startedAt));
    window.setTimeout(() => {
      mutate();
      loading.today = false;
    }, wait);
  };

  request.get(FILM_API.BOX_OFFICE_TODAY).then(res => {
    if (res.code === '200') {
      const payload = res.data;
      commit(() => {
        const changed = todayBoxOffice.total !== payload.total;
        todayBoxOffice.total = payload.total;
        todayBoxOffice.updatedAt = payload.updatedAt;
        if (changed) todayBoxOffice.fresh = true;
      });
    } else {
      commit(() => { todayBoxOffice.error = true; });
    }
  }).catch(err => {
    console.error('今日票房接口请求异常：', err);
    commit(() => { todayBoxOffice.error = true; });
  });
};

const loadFilmBoxOfficeTop = () => {
  loading.boxOffice = true;
  request.get(FILM_API.BOX_OFFICE_TOP, {
    params: { topNum: 10 }
  }).then(res => {
    if (res.code === '200') {
      boxOfficeTop10.length = 0;
      boxOfficeTop10.push(...(res.data || []).slice(0, 10));
    } else {
      ElMessage.error(`票房数据加载失败：${res.msg || '未知错误'}`);
    }
  }).catch(err => {
    console.error('票房接口请求异常：', err);
    ElMessage.error('数据加载失败，请稍后重试');
  }).finally(() => {
    loading.boxOffice = false;
  });
};

const loadFilmMarkTop = () => {
  loading.mark = true;
  request.get(FILM_API.MARK_TOP, {
    params: { topNum: 10 }
  }).then(res => {
    if (res.code === '200') {
      ratingTop5.length = 0;
      ratingTop5.push(...(res.data || []).slice(0, 5));
    } else {
      ElMessage.error(`评分数据加载失败：${res.msg || '未知错误'}`);
    }
  }).catch(err => {
    console.error('评分接口请求异常：', err);
    ElMessage.error('数据加载失败，请稍后重试');
  }).finally(() => {
    loading.mark = false;
  });
};

const load = () => {
  request.get(API_PATHS.FILMS).then(res => {
    if (res.code === '200') {
      data.data1 = res.data.filter(v => v.status === '已上映');
      data.data2 = res.data.filter(v => v.status === '待上映');
      // 最多展示 8 条，避免页面过长
      data.playingData = data.data1.slice(0, 8);
      data.noPlayData = data.data2.slice(0, 8);
    } else {
      data.error = true;
      ElMessage.error(res.msg);
    }
  }).catch(err => {
    // 网络异常的统一提示由 request.js 的响应拦截器给出，这里只落错误态
    console.error('电影列表接口请求异常：', err);
    data.error = true;
  });
};

load();
loadTodayBoxOffice();
loadFilmBoxOfficeTop();
loadFilmMarkTop();

onMounted(startHero);
onUnmounted(() => {
  stopHero();
  cancelHeroHover();
});
</script>

<style scoped>
.home-page {
  display: flex;
  align-items: flex-start;
}

.home-main {
  flex: 1;
  min-width: 0;
}

.home-main__upcoming {
  margin-top: var(--space-32);
}

.poster-buy {
  width: 100%;
}

/* 即将上映的档期：白底文字用 --color-rating-text（4.68:1），不用深底金 */
.upcoming-date {
  font-size: var(--fs-xs);
  color: var(--color-rating-text);
}

/* ---------- Hero 轮播 ---------- */
.hero {
  position: relative;
}

.hero__stage {
  position: relative;
  /* 轨道横向溢出后，overflow: hidden 的盒子仍是"可滚动容器"：
     键盘聚焦主动页的「购票」时浏览器会替它滚动，把 scrollLeft 顶偏，
     而定位全靠 transform，滚动量一进来整套就错位。clip 不建立滚动容器，根除这条路；
     先写 hidden 给不认 clip 的浏览器兜底。 */
  overflow: hidden;
  overflow: clip;
  aspect-ratio: 16 / 7;
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-fill-color-light);
}

/* 轨道宽度仍是舞台的 100%，子项各 flex-basis:100% 横向溢出，
   于是 translateX 里的 100% 恰好等于一张 —— 不用按列数换算百分比。
   时长取大面板档；缓动取表内的 ease-in-out，纯 ease-in 是给"离场"的，
   轨道滑动同时有起步和到位两段，到位那下用 ease-in 会急刹。 */
.hero__track {
  display: flex;
  width: 100%;
  height: 100%;
  transform: translateX(calc(var(--hero-index, 0) * -100%));
  transition: transform 300ms ease-in-out;
}

/* 无缝循环的那一跳：关掉过渡，让轨道瞬移回第一张 */
.hero__track--snap {
  transition: none;
}

.hero__slide {
  position: relative;
  flex: 0 0 100%;
  height: 100%;
  /* 屏幕外的几张要排除在 Tab 顺序与无障碍树之外：横向虽被 overflow 裁掉，
     键盘仍能聚焦到裁剪切边之外的「购票」链接，形成"可聚焦但看不见"的陷阱。
     transition 必须保留 visibility —— 它是阶跃插值，整段过渡维持 visible、
     到终点才变 hidden，出画那张因此不会滑到一半就凭空消失。 */
  visibility: hidden;
  transition: visibility 300ms ease-in-out;
}

.hero__slide--active {
  visibility: visible;
}

.hero__img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 渐变遮罩让左侧文字有稳定底色。只含令牌与 transparent 关键字，无硬编码色值 */
.hero__scrim {
  position: absolute;
  inset: 0;
  background-image: linear-gradient(
      90deg,
      var(--dark-bg) 0%,
      var(--dark-bg) 45%,
      transparent 100%
  );
}

.hero__body {
  position: absolute;
  top: 50%;
  left: var(--space-48);
  max-width: 45%;
  transform: translateY(-50%);
}

/* 深色块上的文字一律用深色表面令牌 */
.hero__title {
  font-size: var(--fs-4xl);
  font-weight: var(--fw-bold);
  color: var(--dark-text);
}

/* 深底上的次级说明统一用 --dark-text-secondary（10.84:1），
   与两个详情页的 hero 取同一个令牌 —— 同一个语义槽位只应有一个答案 */
.hero__meta {
  margin-top: var(--space-8);
  font-size: var(--fs-base);
  color: var(--dark-text-secondary);
}

/* 深色底上的评分用 --color-rating（金），12.41:1 达 AA */
.hero__score {
  margin-top: var(--space-8);
  font-size: var(--fs-5xl);
  font-weight: var(--fw-bold);
  color: var(--color-rating);
  line-height: var(--lh-loose);
}

/* CTA 是 <a>（router-link），白字压主色 5.58:1 达 AA。
   hover / active 取状态映射：背景 light-3 / dark-2。 */
.hero__cta {
  display: inline-block;
  margin-top: var(--space-16);
  padding: var(--space-8) var(--space-24);
  border-radius: var(--el-border-radius-round);
  background-color: var(--el-color-primary);
  color: var(--color-on-accent);
  font-weight: var(--fw-bold);
  text-decoration: none;
  transition: background-color 100ms ease-out;
}

.hero__cta:hover {
  background-color: var(--el-color-primary-light-3);
}

.hero__cta:active {
  background-color: var(--el-color-primary-dark-2);
}

.hero__dots {
  display: flex;
  justify-content: center;
  gap: var(--space-8);
  margin-top: var(--space-12);
}

/* 命中区固定 36×24，条的伸缩只发生在按钮内部：
   若直接给「选中的条」加宽，整行会因 justify-content: center 重新居中，
   条从鼠标底下滑走 → mouseenter / mouseleave 互相触发 → 来回闪。
   顺带把命中区从 24×4 放大到 36×24 —— 原来那个点击目标小得离谱。 */
.hero__dot {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 24px;
  padding: 0;
  border: none;
  border-radius: var(--el-border-radius-small);
  background: none;
  cursor: pointer;
}

/* 键盘焦点必须可见（Focus）——按钮清了默认外观，要显式补描边 */
.hero__dot:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}

/* 未选中的条用 --el-border-color-dark、选中用主色，并靠长度区分，
   不只靠颜色传递「当前是哪一张」。
   加宽取常规交互 200ms；颜色是微小变化，取 100ms ease-out。 */
.hero__dot__bar {
  width: 20px;
  height: 4px;
  border-radius: var(--el-border-radius-small);
  background-color: var(--el-border-color-dark);
  transition: width 200ms ease-in-out, background-color 100ms ease-out;
}

/* hover 取状态映射的背景态 light-3 */
.hero__dot:hover .hero__dot__bar {
  background-color: var(--el-color-primary-light-3);
}

/* 选中态排在 hover 之后，压过 light-3 —— 鼠标停在当前这一张上时它仍是主色 */
.hero__dot--active .hero__dot__bar {
  width: 32px;
  background-color: var(--el-color-primary);
}

/* 按下反馈（Active）排最后：任何一条被按下都走 dark-2，压过选中与 hover */
.hero__dot:active .hero__dot__bar {
  background-color: var(--el-color-primary-dark-2);
}

/* ---------- 右侧栏 ---------- */
.home-aside {
  width: 280px;
  margin-left: var(--space-48);
}

.home-aside__section {
  margin-top: var(--space-32);
}

/* ---------- 今日票房 ---------- */
.today-box {
  display: flex;
  margin-bottom: var(--space-32);
  overflow: hidden;
  background-color: var(--el-fill-color-light);
  border-radius: var(--el-border-radius-base);
}

/* 色条上压的是白字，底色必须是「承载文字」的主色（前台 #BF352D，5.58:1）。
   --color-brand（前台 #ef4238）只用于不承载文字的图形 / 大标题 —— 白字压它仅 3.81:1。 */
.today-box__strip {
  display: flex;
  flex-shrink: 0;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 40px;
  padding: var(--space-12) 0;
  color: var(--color-on-accent);
  font-size: var(--fs-base);
  font-weight: var(--fw-bold);
  line-height: var(--lh-base);
  background-color: var(--el-color-primary);
}

.today-box__body {
  flex: 1;
  min-width: 0;
  padding: var(--space-12) var(--space-16);
}

.today-box__skeleton {
  padding: var(--space-4) 0;
}

/* 前台数据大字，--fs-5xl 给「评分 / 票房」。
   nowrap 兜住「元」被折到第二行：36px 字在 208px 内容区里一折行，
   刷新前后高度一变，整个数字就上下跳。 */
.today-box__amount {
  color: var(--el-text-color-primary);
  font-size: var(--fs-5xl);
  font-weight: var(--fw-bold);
  line-height: var(--lh-loose);
  white-space: nowrap;
}

/* 时间戳与刷新按钮同排；数字独占整行后，按钮挪到这里就不再挤数字 */
.today-box__foot {
  display: flex;
  align-items: center;
  gap: var(--space-8);
  margin-top: var(--space-8);
}

/* 数字刚更新：一次性的颜色脉冲，让「刷新到了」看得见。
   取主色基色（前台 #BF352D，浅底仍达正文对比度）回落到默认文字色；
   只在本组件 scoped 块引用，非共享关键帧，不进 global.css。 */
.today-box__amount--fresh {
  animation: today-amount-flash 500ms ease-out;
}

@keyframes today-amount-flash {
  from {
    color: var(--el-color-primary);
  }

  to {
    color: var(--el-text-color-primary);
  }
}

.today-box__error {
  color: var(--el-color-danger);
}

/* flex:1 + min-width:0 让步给按钮：失败提示文案长时自己折行，不把按钮顶出去 */
.today-box__time {
  flex: 1;
  min-width: 0;
  color: var(--el-text-color-regular);
  font-size: var(--fs-xs);
}

/* 「刷新」/「刷新中」两态宽度不同，不给按钮被压缩的余地 */
.today-box__refresh {
  flex-shrink: 0;
}

/* ---------- 榜单条 ---------- */
.rank-box {
  margin-top: var(--space-16);
  padding: var(--space-12) var(--space-4);
  border: 1px solid var(--el-border-color-light);
  border-radius: var(--el-border-radius-base);
}

.rank-box__loading {
  padding: var(--space-4) 0;
}

.skeleton--sm {
  --el-skeleton-avatar-size: 24px;
}

.skeleton--lg {
  --el-skeleton-avatar-size: 80px;
}

/* 整行是 router-link（原生 <a>，可 Tab 聚焦），因此不写 cursor: pointer 也不挂 @click */
.rank-row {
  display: flex;
  align-items: center;
  padding: var(--space-8) var(--space-4);
  color: inherit;
  text-decoration: none;
  transition: background-color 100ms ease-out;
}

.rank-row:hover {
  background-color: var(--el-fill-color-light);
}

.rank-row--divided {
  padding: var(--space-12) var(--space-4);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.rank-row--divided:last-child {
  border-bottom: none;
}

.rank-badge {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  border-radius: var(--el-border-radius-circle);
  font-size: var(--fs-base);
  font-weight: var(--fw-bold);
  line-height: 24px;
  text-align: center;
}

.rank-badge--top1 {
  background-color: var(--color-rank-1);
  color: var(--color-on-accent);
}

.rank-badge--top2 {
  background-color: var(--color-rank-2);
  color: var(--color-on-accent);
}

.rank-badge--top3 {
  background-color: var(--color-rank-3);
  color: var(--color-on-accent);
}

.rank-badge--plain {
  background-color: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
}

.rank-row__body {
  flex: 1;
  min-width: 0;
  margin-left: var(--space-12);
}

.rank-row__title {
  font-weight: var(--fw-bold);
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rank-row__meta {
  margin-top: var(--space-4);
  font-size: var(--fs-xs);
  color: var(--el-text-color-regular);
}

.rank-row__value {
  color: var(--el-color-primary);
  font-weight: var(--fw-bold);
  text-align: right;
}

.rank-row__poster {
  flex-shrink: 0;
  width: 60px;
  margin-left: var(--space-12);
}

.rank-row__img {
  width: 100%;
  height: 80px;
  border-radius: var(--el-border-radius-base);
  object-fit: cover;
}

/* 评分每行是「9.2 分」，含中文，故字重只能用 400 / 700。
   文字色用白底评分色 --color-rating-text（4.68:1） */
.rank-row__score {
  margin-top: var(--space-4);
  font-size: var(--fs-base);
  font-weight: var(--fw-bold);
  color: var(--color-rating-text);
}
</style>
