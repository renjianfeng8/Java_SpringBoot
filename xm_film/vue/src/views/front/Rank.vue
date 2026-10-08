<template>
  <div class="rank-page">
    <h2 class="rank-page__title">电影排行榜</h2>

    <!-- 双列布局：Flex 自动等高 -->
    <div class="rank-columns">
      <!-- 票房排行榜 -->
      <section class="rank-card" aria-label="总票房榜">
        <div class="rank-card__header">
          <h3 class="rank-card__title">总票房 Top 榜</h3>
        </div>

        <div v-if="loading.boxOffice" class="rank-card__loading">
          <el-skeleton avatar :rows="10" :columns="3" />
        </div>

        <div v-else-if="error.boxOffice" class="empty-hint">数据加载失败，请稍后重试</div>
        <div v-else-if="!data.boxOfficeData.length" class="empty-hint">暂无数据</div>

        <div v-else class="rank-card__body">
          <router-link
              v-for="(film, index) in data.boxOfficeData"
              :key="film.id"
              class="rank-item"
              :to="`/front/filmDetail/${film.id}`"
          >
            <div :class="rankBadgeClass(index)" class="rank-item__badge">{{ index + 1 }}</div>

            <div class="rank-item__main">
              <el-image
                  :src="film.img"
                  :alt="`《${film.title}》海报`"
                  fit="cover"
                  lazy
                  class="rank-item__poster"
              />
              <div class="rank-item__info">
                <h4 class="rank-item__name">
                  {{ film.title }}
                  <span class="rank-item__english">({{ film.english || '无英文标题' }})</span>
                </h4>
                <div class="rank-item__meta">
                  <span class="rank-item__meta-block">类型：{{ typeText(film) }}</span>
                  <span class="rank-item__meta-inline">上映时间：{{ film.start || '未知时间' }}</span>
                  <span>时长：{{ film.time || '未知时长' }}</span>
                </div>
              </div>
            </div>

            <div class="rank-item__value">{{ formatBoxOffice(film.boxOffice) }}</div>
          </router-link>
        </div>
      </section>

      <!-- 评分排行榜 -->
      <section class="rank-card" aria-label="评分榜">
        <div class="rank-card__header">
          <h3 class="rank-card__title">评分 Top 榜</h3>
        </div>

        <div v-if="loading.mark" class="rank-card__loading">
          <el-skeleton avatar :rows="10" :columns="3" />
        </div>

        <div v-else-if="error.mark" class="empty-hint">数据加载失败，请稍后重试</div>
        <div v-else-if="!data.markData.length" class="empty-hint">暂无数据</div>

        <div v-else class="rank-card__body">
          <router-link
              v-for="(film, index) in data.markData"
              :key="film.id"
              class="rank-item"
              :to="`/front/filmDetail/${film.id}`"
          >
            <div :class="rankBadgeClass(index)" class="rank-item__badge">{{ index + 1 }}</div>

            <div class="rank-item__main">
              <el-image
                  :src="film.img"
                  :alt="`《${film.title}》海报`"
                  fit="cover"
                  lazy
                  class="rank-item__poster"
              />
              <div class="rank-item__info">
                <h4 class="rank-item__name">
                  {{ film.title }}
                  <span class="rank-item__english">({{ film.english || '无英文标题' }})</span>
                </h4>
                <div class="rank-item__meta">
                  <span class="rank-item__meta-block">类型：{{ typeText(film) }}</span>
                  <span class="rank-item__meta-inline">上映时间：{{ film.start || '未知时间' }}</span>
                  <span>时长：{{ film.time || '未知时长' }}</span>
                </div>
              </div>
            </div>

            <div class="rank-item__value rank-item__value--score">{{ formatScore(film.score) }}</div>
          </router-link>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue';
import request from "@/utils/request.js";
import { ElMessage } from 'element-plus';
import { FILM_API } from '@/constants';
import { formatBoxOffice, formatScore, formatFilmTypes } from '@/utils/format.js';
import 'element-plus/theme-chalk/el-skeleton.css';
import 'element-plus/theme-chalk/el-image.css';

const data = reactive({
  boxOfficeData: [],
  markData: [],
});

const loading = reactive({
  boxOffice: false,
  mark: false,
});

// 失败必须与空区分：请求挂了却渲染「暂无数据」，用户会以为库里确实没有，从而不去重试
const error = reactive({
  boxOffice: false,
  mark: false,
});

const typeText = formatFilmTypes;

// 前三名用奖牌色，其余用中性底（名次同时由数字表达，不依赖颜色）
const rankBadgeClass = (index) =>
  index < 3 ? `rank-item__badge--top${index + 1}` : 'rank-item__badge--plain';

const loadFilmBoxOfficeTop = () => {
  loading.boxOffice = true;
  error.boxOffice = false;
  request.get(FILM_API.BOX_OFFICE_TOP, { params: { topNum: 10 } })
      .then(res => {
        if (res.code === '200') {
          data.boxOfficeData = (res.data || []).slice(0, 10);
        } else {
          // 业务码非 200 不经过响应拦截器（它只管网络/超时/HTTP 状态），所以这里补一次提示；
          // 网络与 5xx 那类失败已经在拦截器里弹过，这里不重复弹
          ElMessage.error(`票房数据加载失败：${res.msg || '未知错误'}`);
          error.boxOffice = true;
        }
      })
      .catch(err => {
        // 失败提示由 request.js 的响应拦截器统一给出，这里只落错误态，不重复弹窗
        console.error('票房接口请求异常：', err);
        error.boxOffice = true;
      })
      .finally(() => {
        loading.boxOffice = false;
      });
};

const loadFilmMarkTop = () => {
  loading.mark = true;
  error.mark = false;
  request.get(FILM_API.MARK_TOP, { params: { topNum: 10 } })
      .then(res => {
        if (res.code === '200') {
          data.markData = (res.data || []).slice(0, 10);
        } else {
          ElMessage.error(`评分数据加载失败：${res.msg || '未知错误'}`);
          error.mark = true;
        }
      })
      .catch(err => {
        console.error('评分接口请求异常：', err);
        error.mark = true;
      })
      .finally(() => {
        loading.mark = false;
      });
};

loadFilmBoxOfficeTop();
loadFilmMarkTop();
</script>

<style scoped>
.rank-page {
  width: 80%;
  max-width: 1200px;
  margin: var(--space-20) auto;
}

.rank-page__title {
  margin: 0 0 var(--space-24);
  font-size: var(--fs-3xl);
  font-weight: var(--fw-bold);
  color: var(--el-text-color-primary);
  text-align: center;
}

.rank-columns {
  display: flex;
  gap: var(--space-24);
}

.rank-card {
  flex: 1;
  padding: var(--space-20);
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
}

.rank-card__header {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--space-20);
  padding: var(--space-12) 0;
  border-bottom: 1px solid var(--el-border-color);
}

/* 卡片标题承载文字，用主色（5.58:1）而非浅一档的品牌红 */
.rank-card__title {
  margin: 0;
  font-size: var(--fs-xl);
  font-weight: var(--fw-bold);
  color: var(--el-color-primary);
}

.rank-card__loading {
  padding: var(--space-32) 0;
}

.rank-card__body {
  padding-right: var(--space-12);
}

/* 整行是 router-link（原生 <a>，可 Tab 聚焦），故不写 cursor: pointer */
.rank-item {
  display: flex;
  align-items: center;
  padding: var(--space-12) var(--space-8);
  border-bottom: 1px solid var(--el-border-color-lighter);
  border-radius: var(--el-border-radius-base);
  color: inherit;
  text-decoration: none;
  transition: background-color 100ms ease-out;
}

.rank-item:hover {
  background-color: var(--el-fill-color-light);
}

.rank-item:last-child {
  border-bottom: none;
}

.rank-item__badge {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  margin-right: var(--space-16);
  border-radius: var(--el-border-radius-circle);
  font-size: var(--fs-base);
  font-weight: var(--fw-bold);
  line-height: 24px;
  text-align: center;
}

.rank-item__badge--top1 {
  background-color: var(--color-rank-1);
  color: var(--color-on-accent);
}

.rank-item__badge--top2 {
  background-color: var(--color-rank-2);
  color: var(--color-on-accent);
}

.rank-item__badge--top3 {
  background-color: var(--color-rank-3);
  color: var(--color-on-accent);
}

.rank-item__badge--plain {
  background-color: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
}

.rank-item__main {
  display: flex;
  flex: 1;
  align-items: center;
  min-width: 0;
}

/* 海报用与全站一致的 2:3 比例，不写死 height */
.rank-item__poster {
  flex-shrink: 0;
  width: 60px;
  aspect-ratio: 2 / 3;
  margin-right: var(--space-16);
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-fill-color-light);
}

.rank-item__info {
  flex: 1;
  min-width: 0;
}

.rank-item__name {
  margin: 0 0 var(--space-8);
  font-size: var(--fs-md);
  font-weight: var(--fw-bold);
  line-height: var(--lh-loose);
  color: var(--el-text-color-primary);
}

.rank-item__english {
  margin-left: var(--space-4);
  font-size: var(--fs-xs);
  font-weight: var(--fw-regular);
  color: var(--el-text-color-regular);
}

.rank-item__meta {
  font-size: var(--fs-xs);
  line-height: var(--lh-base);
  color: var(--el-text-color-regular);
}

.rank-item__meta-block {
  display: block;
  margin-bottom: var(--space-4);
}

.rank-item__meta-inline {
  margin-right: var(--space-16);
}

.rank-item__value {
  flex-shrink: 0;
  width: 120px;
  font-size: var(--fs-base);
  font-weight: var(--fw-bold);
  color: var(--el-color-primary);
  text-align: right;
}

/* 评分承载文字，用白底评分文字色（4.68:1） */
.rank-item__value--score {
  font-size: var(--fs-md);
  color: var(--color-rating-text);
}
</style>
