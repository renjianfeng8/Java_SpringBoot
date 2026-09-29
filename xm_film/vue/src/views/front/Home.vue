<template>
  <div class="home-page">

    <!-- 左侧内容（完善跳转逻辑） -->
    <div class="home-main">
      <!-- 正在热播区域 -->
      <div class="section-head">
        <div class="section-head__title">正在热播 ({{data.data1.length}}) 部</div>
        <!-- 「全部」按钮：跳转到电影列表页（展示所有已上映电影） -->
        <div
            class="section-head__more"
            @click="goToMovieList('playing')"
        >
          全部 >
        </div>
      </div>

      <div class="film-grid">
        <el-row :gutter="15">
          <!-- 正在热播电影：海报和购票按钮均跳转详情页 -->
          <el-col :span="6" v-for="item in data.playingData" :key="item.id" class="film-grid__col">
            <!-- 海报点击跳转详情 -->
            <div
                class="film-card__poster-link"
                @click="goToFilmDetail(item.id)"
            >
              <img :src="item.img" alt="电影海报" class="film-card__poster">
            </div>
            <!-- 购票按钮：跳转到详情页（后续可在详情页跳转选座） -->
            <el-button
                class="film-card__buy"
                @click="goToFilmDetail(item.id)"
            >
              购票
            </el-button>
          </el-col>
        </el-row>
        <div v-if="data.error" class="empty-hint">数据加载失败，请稍后重试</div>
        <div v-else-if="!data.playingData.length" class="empty-hint">暂无数据</div>
      </div>

      <!-- 即将上映区域 -->
      <div class="home-main__upcoming">
        <div class="section-head">
          <div class="section-head__title section-head__title--upcoming">即将上映 ({{data.data2.length}}) 部</div>
          <!-- 「全部」按钮：跳转到电影列表页（展示所有待上映电影） -->
          <div
              class="section-head__more section-head__more--upcoming"
              @click="goToMovieList('upcoming')"
          >
            全部 >
          </div>
        </div>

        <div class="film-grid">
          <el-row :gutter="15">
            <!-- 即将上映电影：整卡片点击跳转详情页 -->
            <el-col :span="6" v-for="item in data.noPlayData" :key="item.id" class="film-grid__col film-grid__col--clickable">
              <div @click="goToFilmDetail(item.id)" class="film-card__upcoming">
                <img :src="item.img" alt="电影海报" class="film-card__poster">
                <div class="film-card__title">{{item.title}}</div>
                <div class="film-card__time">{{item.start}} 上映</div>
              </div>
            </el-col>
          </el-row>
          <div v-if="data.error" class="empty-hint">数据加载失败，请稍后重试</div>
          <div v-else-if="!data.noPlayData.length" class="empty-hint">暂无数据</div>
        </div>
      </div>
    </div>

    <!-- 右侧内容（完善票房/评分列表跳转） -->
    <div class="home-aside">
      <!-- 1. 总票房Top 10（添加电影标题跳转详情） -->
      <div>
        <div class="aside-title">总票房Top 10</div>
        <div class="rank-box">
          <div v-if="loading.boxOffice" class="rank-box__loading">
            <el-skeleton :rows="10" :columns="3" avatar class="skeleton--sm" />
          </div>
          <div v-else-if="boxOfficeTop10.length">
            <div v-for="(movie, index) in boxOfficeTop10" :key="movie.id" class="rank-row" @click="goToFilmDetail(movie.id)">
              <!-- 排名标识 -->
              <div
                :class="index < 3 ? `rank-badge--top${index + 1}` : 'rank-badge--plain'"
                class="rank-badge">
              {{ index + 1 }}
            </div>

              <div class="rank-row__body">
                <!-- 电影标题点击跳转 -->
                <div class="rank-row__title">{{ movie.title }}</div>
                <div class="rank-row__meta">
                  {{ movie.typeList?.map(t => t.title).join(' / ') || '未知类型' }} | {{ movie.start || '未知时间' }}
                </div>
              </div>

              <div class="rank-row__value">{{ formatBoxOffice(movie.boxOffice) }}</div>
            </div>
          </div>
          <div v-else class="empty-hint">暂无数据</div>
        </div>
      </div>

      <!-- 2. 评分Top 5（添加电影标题/海报跳转详情，星级改为分数） -->
      <div class="home-aside__section">
        <div class="section-head">
          <div class="section-head__title">评分Top 5</div>
          <!-- 「查看完整榜单」跳转排行榜页 -->
          <div
              class="section-head__more section-head__more--wide"
              @click="goToRankPage()"
          >
            查看完整榜单>
          </div>
        </div>
        <div class="rank-box rank-box--spaced">
          <div v-if="loading.mark" class="rank-box__loading">
            <el-skeleton :rows="5" :columns="3" avatar class="skeleton--lg" />
          </div>
          <div v-else-if="ratingTop5.length">
            <div v-for="(movie, index) in ratingTop5" :key="movie.id" class="rank-row rank-row--divided" @click="goToFilmDetail(movie.id)">
              <!-- 排名标识 -->
              <div
                :class="index < 3 ? `rank-badge--top${index + 1}` : 'rank-badge--plain'"
                class="rank-badge">
              {{ index + 1 }}
            </div>

              <!-- 海报点击跳转 -->
              <div class="rank-row__poster">
                <img :src="movie.img" alt="电影海报" class="rank-row__img">
              </div>

              <div class="rank-row__body">
                <!-- 电影标题点击跳转 -->
                <div class="rank-row__title">{{ movie.title }}</div>
                <div class="rank-row__meta">{{ movie.typeList?.map(t => t.title).join(' / ') || '未知类型' }}</div>
                <!-- 评分：移除el-rate星级，改为分数显示 -->
                <div class="rank-row__score">
                  {{ movie.score || 0 }} 分
                </div>
              </div>
            </div>
          </div>
          <div v-else class="empty-hint">暂无数据</div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import request from "@/utils/request.js";
import { API_PATHS, FILM_API } from '@/constants';
import { formatBoxOffice } from '@/utils/format.js';
// 引入Element Plus样式（移除el-rate相关样式）
import 'element-plus/theme-chalk/el-skeleton.css';
import 'element-plus/theme-chalk/el-button.css';

// 路由实例初始化
const router = useRouter();

// 左侧正在热播/即将上映数据（保持不变）
const data = reactive({
  data1: [], // 已上映电影
  data2: [], // 待上映电影
  playingData: [], // 正在热播展示数据（前8条）
  noPlayData: [], // 即将上映展示数据（前8条）
  error: false // 电影列表加载失败：与"暂无数据"区分，避免把网络错误显示成没有影片
});

// 右侧接口数据（保持不变）
const boxOfficeTop10 = reactive([]); // 总票房Top10
const ratingTop5 = reactive([]);     // 评分Top5
const loading = reactive({           // 加载状态
  boxOffice: false,
  mark: false
});

/**
 * 1. 跳转到电影详情页
 * @param {number} filmId - 电影ID
 */
const goToFilmDetail = (filmId) => {
  if (!filmId) {
    ElMessage.warning('电影ID无效');
    return;
  }
  router.push({
    path: `/front/filmDetail/${filmId}`
  });
};

/**
 * 2. 跳转到电影列表页（区分“正在热播”和“即将上映”）
 * @param {string} type - 类型（playing：已上映；upcoming：待上映）
 */
const goToMovieList = (type) => {
  // 跳转到 /front/movie，并携带查询参数（用于筛选电影类型）
  router.push({
    path: '/front/movie',
    query: { type }
  });
};

const goToRankPage = () => {
  router.push('/front/rank');
};

// 加载总票房Top10数据
const loadFilmBoxOfficeTop = () => {
  loading.boxOffice = true;
  request.get(FILM_API.BOX_OFFICE_TOP, {
    params: {topNum: 10}
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

// 加载评分Top5数据
const loadFilmMarkTop = () => {
  loading.mark = true;
  request.get(FILM_API.MARK_TOP, {
    params: {topNum: 10}
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

// 电影数据加载
const load = () => {
  request.get(API_PATHS.FILMS).then(res => {
    if (res.code === '200') {
      data.data1 = res.data.filter(v => v.status === '已上映');
      data.data2 = res.data.filter(v => v.status === '待上映');
      // 最多展示8条数据，避免页面过长
      data.playingData = data.data1.length > 8 ? data.data1.slice(0, 8) : data.data1;
      data.noPlayData = data.data2.length > 8 ? data.data2.slice(0, 8) : data.data2;
    } else {
      data.error = true;
      ElMessage.error(res.msg);
    }
  }).catch(err => {
    // 网络异常的统一提示由 request.js 的响应拦截器给出，这里只落错误态，
    // 让占位显示"数据加载失败，请稍后重试"，避免同一错误弹两个提示
    console.error('电影列表接口请求异常：', err);
    data.error = true;
  });
};

// 页面初始化加载所有数据
load();
loadFilmBoxOfficeTop();
loadFilmMarkTop();
</script>

<style scoped>
.home-page {
  display: flex;
  width: 75%;
  max-width: 1200px;
  margin: var(--space-20) auto;
}

.home-main {
  flex: 1;
}

.home-main__upcoming {
  flex: 1;
  margin-top: var(--space-24);
}

/* 区块标题行 */
.section-head {
  display: flex;
  align-items: center;
}

.section-head__title {
  flex: 1;
  font-size: var(--fs-xl);
  color: var(--el-color-primary);
}

.section-head__title--upcoming {
  color: var(--el-color-primary);
}

.section-head__more {
  width: 60px;
  color: var(--el-color-primary);
  text-align: right;
  cursor: pointer;
}

.section-head__more--wide {
  width: 100px;
}

/* 电影网格 */
.film-grid {
  margin-top: var(--space-20);
}

.film-grid__col {
  margin-bottom: var(--space-20);
}

.film-grid__col--clickable {
  cursor: pointer;
}

.film-card__poster-link {
  margin-bottom: var(--space-8);
  cursor: pointer;
}

.film-card__poster {
  width: 100%;
  height: 260px;
  border-radius: var(--el-border-radius-base);
  object-fit: cover;
  transition: transform 200ms ease-in-out;
}

.film-card__poster:hover {
  transform: scale(1.02);
}

.film-card__buy {
  width: 100%;
  height: 35px;
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}

.film-card__upcoming {
  transition: box-shadow 200ms ease-in-out;
}

.film-card__upcoming:hover {
  box-shadow: var(--el-box-shadow-lighter);
}

.film-card__title {
  margin-top: var(--space-4);
  font-size: var(--fs-xl);
  font-weight: var(--fw-bold);
  font-style: italic;
}

/* 上映时间承载文字，用白底评分文字色（4.68:1） */
.film-card__time {
  margin-top: var(--space-4);
  font-size: var(--fs-md);
  color: var(--color-rating-text);
}

/* ---------- 右侧栏 ---------- */
.home-aside {
  width: 280px;
  margin-left: var(--space-48);
}

.home-aside__section {
  margin-top: var(--space-40);
}

.aside-title {
  margin: var(--space-8) 0;
  font-size: var(--fs-xl);
  color: var(--el-color-primary);
}

.rank-box {
  padding: var(--space-12) var(--space-4);
  border: 1px solid var(--el-color-primary);
  border-radius: var(--el-border-radius-base);
}

.rank-box--spaced {
  margin-top: var(--space-20);
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

.rank-row {
  display: flex;
  align-items: center;
  padding: var(--space-8) var(--space-4);
  cursor: pointer;
  transition: background-color 200ms ease-in-out;
}

.rank-row:hover {
  background-color: var(--el-fill-color-light);
}

.rank-row--divided {
  padding: var(--space-12) var(--space-4);
  border-bottom: 1px dashed var(--el-border-color);
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

/* 评分承载文字，用白底评分文字色（4.68:1） */
.rank-row__score {
  margin-top: var(--space-4);
  font-size: var(--fs-base);
  font-weight: var(--fw-medium);
  color: var(--color-rating-text);
}

/* 无数据 / 加载失败的占位（规范 §608：禁止用假数据填充，无数据渲染「暂无数据」） */
.empty-hint {
  padding: var(--space-20) 0;
  font-size: var(--fs-base);
  color: var(--el-text-color-regular);
  text-align: center;
}
</style>
