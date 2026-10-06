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
          <div
              v-for="(film, index) in heroFilms"
              :key="film.id"
              class="hero__slide"
              :class="{ 'hero__slide--active': index === heroIndex }"
              :aria-hidden="index !== heroIndex"
          >
            <img class="hero__img" :src="film.img" :alt="`《${film.title}》剧照`" loading="lazy">
            <div class="hero__scrim" aria-hidden="true"></div>
            <div class="hero__body">
              <h2 class="hero__title">{{ film.title }}</h2>
              <p class="hero__meta">{{ typeText(film) }}</p>
              <p v-if="scoreOf(film)" class="hero__score">{{ scoreOf(film) }}</p>
              <router-link class="hero__cta" :to="`/front/filmDetail/${film.id}`">购票</router-link>
            </div>
          </div>
        </div>

        <div v-if="heroFilms.length > 1" class="hero__dots">
          <button
              v-for="(film, index) in heroFilms"
              :key="film.id"
              class="hero__dot"
              :class="{ 'hero__dot--active': index === heroIndex }"
              :aria-label="`切换到第 ${index + 1} 张：${film.title}`"
              :aria-current="index === heroIndex"
              @click="goHero(index)"
          ></button>
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
        <FilmPosterCard v-for="item in data.noPlayData" :key="item.id" :film="item" :cta="''">
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
        <div class="today-box__body">
          <!-- 仅首屏未拿到数据时占位；刷新时保留数字、只让按钮转圈，避免骨架屏闪烁 -->
          <div v-if="loading.today && todayBoxOffice.total === null" class="today-box__skeleton">
            <el-skeleton :rows="2" animated />
          </div>
          <template v-else>
            <div class="today-box__row">
              <div v-if="todayBoxOffice.error" class="today-box__error">数据加载失败，请稍后重试</div>
              <div v-else class="today-box__amount">{{ formatYuan(todayBoxOffice.total) }}</div>
              <el-button
                  link
                  type="primary"
                  :loading="loading.today"
                  @click="loadTodayBoxOffice"
              >
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
            <div v-if="!todayBoxOffice.error" class="today-box__time">
              北京时间：{{ todayBoxOffice.updatedAt || '—' }}
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
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
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
  error: false
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
const heroIndex = ref(0);
let heroTimer = null;

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

/** 启动自动轮播。系统开启「减少动态效果」时不启动 —— 自动替换内容本身就是动效（§10.2） */
const startHero = () => {
  stopHero();
  if (heroPaused.value || prefersReducedMotion() || heroFilms.value.length < 2) return;
  heroTimer = setInterval(() => {
    heroIndex.value = (heroIndex.value + 1) % heroFilms.value.length;
  }, HERO_INTERVAL);
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
  heroIndex.value = index;
  // 未暂停时重新计时（手动翻页后不该马上又自动跳）；暂停中则由 startHero 直接返回
  startHero();
};

// 数据是异步到达的，且 length 变化时旧的下标可能越界，故重算后归零并重启计时
watch(heroFilms, () => {
  heroIndex.value = 0;
  startHero();
});

/* ---------- 数据加载 ---------- */

/**
 * 加载今日票房。失败只落错误态，不弹提示 —— 网络异常/超时/5xx 的提示由 request.js
 * 的响应拦截器统一给出，页面再弹一次会让同一次失败弹两遍（规范 §11.2）。
 */
const loadTodayBoxOffice = () => {
  loading.today = true;
  todayBoxOffice.error = false;
  request.get(FILM_API.BOX_OFFICE_TODAY).then(res => {
    if (res.code === '200') {
      todayBoxOffice.total = res.data.total;
      todayBoxOffice.updatedAt = res.data.updatedAt;
    } else {
      todayBoxOffice.error = true;
    }
  }).catch(err => {
    console.error('今日票房接口请求异常：', err);
    todayBoxOffice.error = true;
  }).finally(() => {
    loading.today = false;
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
onUnmounted(stopHero);
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
  overflow: hidden;
  aspect-ratio: 16 / 7;
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-fill-color-light);
}

.hero__slide {
  position: absolute;
  inset: 0;
  opacity: 0;
  /* visibility 同时解决两件事，缺一不可：
     ① 非激活页不接收指针事件 —— 五张都是 absolute + inset:0，后置兄弟节点绘制在上层，
        只写 opacity:0 的话点击会被最后一张（透明）的链接吃掉，跳到错的影片；
     ② 非激活页不进 Tab 顺序与无障碍树 —— 否则键盘能聚焦到看不见的「购票」上，
        且它带着 aria-hidden，形成"可聚焦但不可见不可读"的焦点陷阱。 */
  visibility: hidden;
  /* 大面板过渡取 §八 的 300ms ease-in（离场缓动） */
  transition: opacity 300ms ease-in, visibility 300ms ease-in;
}

.hero__slide--active {
  opacity: 1;
  visibility: visible;
}

.hero__img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 渐变遮罩让左侧文字有稳定底色。只含令牌与 transparent 关键字，无硬编码色值（§3.7） */
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

/* 深色块上的文字一律用 §3.5 深色表面令牌 */
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

/* 深色底上的评分用 --color-rating（金），12.41:1 达 AA（§3.6） */
.hero__score {
  margin-top: var(--space-8);
  font-size: var(--fs-5xl);
  font-weight: var(--fw-bold);
  color: var(--color-rating);
  line-height: var(--lh-loose);
}

/* CTA 是 <a>（router-link），白字压主色 5.58:1 达 AA。
   hover / active 取 §3.7 状态映射：背景 light-3 / dark-2。 */
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

/* 圆点用 --el-border-color-dark 作默认、主色作选中；同时靠尺寸区分，
   不只靠颜色传递「当前是哪一张」（§3.7） */
.hero__dot {
  width: 24px;
  height: 4px;
  padding: 0;
  border: none;
  border-radius: var(--el-border-radius-small);
  background-color: var(--el-border-color-dark);
  cursor: pointer;
  transition: background-color 100ms ease-out;
}

.hero__dot--active {
  background-color: var(--el-color-primary);
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
   --color-brand（前台 #ef4238）按 §3.2 只用于不承载文字的图形 / 大标题 —— 白字压它仅 3.81:1。 */
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

.today-box__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 前台数据大字，§4.2 指定 --fs-5xl 给「评分 / 票房」 */
.today-box__amount {
  color: var(--el-text-color-primary);
  font-size: var(--fs-5xl);
  font-weight: var(--fw-bold);
  line-height: var(--lh-loose);
}

.today-box__error {
  color: var(--el-text-color-regular);
  font-size: var(--fs-sm);
}

.today-box__time {
  margin-top: var(--space-8);
  color: var(--el-text-color-regular);
  font-size: var(--fs-xs);
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

/* 评分每行是「9.2 分」，含中文，故字重只能用 400 / 700（§4.4）。
   文字色用白底评分色 --color-rating-text（4.68:1） */
.rank-row__score {
  margin-top: var(--space-4);
  font-size: var(--fs-base);
  font-weight: var(--fw-bold);
  color: var(--color-rating-text);
}
</style>
