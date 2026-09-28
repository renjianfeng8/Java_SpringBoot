<template>
  <div class="rank-page">
    <!-- 页面标题 -->
    <h2 class="rank-page__title">电影排行榜</h2>

    <!-- 双列布局：Flex 自动等高 -->
    <div class="rank-columns">
      <!-- 票房排行榜卡片 -->
      <div class="rank-card">
        <div class="rank-card__header">
          <span class="rank-card__title">总票房Top榜</span>
        </div>

        <div v-if="loading.boxOffice" class="rank-card__loading">
          <el-skeleton avatar :rows="10" :columns="3" />
        </div>

        <div v-else class="rank-card__body">
          <div
              v-for="(film, index) in data.boxOfficeData"
              :key="film.id"
              class="rank-item">
            <div :class="rankBadgeClass(index)" class="rank-item__badge">
              {{ index + 1 }}
            </div>

            <div class="rank-item__main">
              <el-image
                  :src="film.img"
                  fit="cover"
                  placeholder
                  class="rank-item__poster"
              />
              <div class="rank-item__info">
                <h4 class="rank-item__name">
                  {{ film.title }}
                  <span class="rank-item__english">({{ film.english || '无英文标题' }})</span>
                </h4>
                <div class="rank-item__meta">
                  <span class="rank-item__meta-block">类型：{{ film.typeList?.map(t => t.title).join(' / ') || '未知类型' }}</span>
                  <span class="rank-item__meta-inline">上映时间：{{ film.start || '未知时间' }}</span>
                  <span>时长：{{ film.time || '未知时长' }}</span>
                </div>
              </div>
            </div>

            <div class="rank-item__value">
              {{ formatBoxOffice(film.boxOffice) }}
            </div>
          </div>
        </div>
      </div>

      <!-- 评分排行榜卡片 -->
      <div class="rank-card">
        <div class="rank-card__header">
          <span class="rank-card__title">评分Top榜</span>
        </div>

        <div v-if="loading.mark" class="rank-card__loading">
          <el-skeleton avatar :rows="10" :columns="3" />
        </div>

        <div v-else-if="!data.markData.length" class="rank-card__empty">
          <el-empty description="暂无评分数据" />
        </div>

        <div v-else class="rank-card__body">
          <div
              v-for="(film, index) in data.markData"
              :key="film.id"
              class="rank-item"
          >
            <div :class="rankBadgeClass(index)" class="rank-item__badge">
              {{ index + 1 }}
            </div>

            <div class="rank-item__main">
              <el-image
                  :src="film.img"
                  fit="cover"
                  placeholder
                  class="rank-item__poster"
              />
              <div class="rank-item__info">
                <h4 class="rank-item__name">
                  {{ film.title }}
                  <span class="rank-item__english">({{ film.english || '无英文标题' }})</span>
                </h4>
                <div class="rank-item__meta">
                  <span class="rank-item__meta-block">类型：{{ film.typeList?.map(t => t.title).join(' / ') || '未知类型' }}</span>
                  <span class="rank-item__meta-inline">上映时间：{{ film.start || '未知时间' }}</span>
                  <span>时长：{{ film.time || '未知时长' }}</span>
                </div>
              </div>
            </div>

            <div class="rank-item__value rank-item__value--score">
              {{ film.score || 0 }} 分
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue';
import request from "@/utils/request.js";
import { ElMessage } from 'element-plus';
import { FILM_API } from '@/constants';
import { formatBoxOffice } from '@/utils/format.js';
import 'element-plus/theme-chalk/el-skeleton.css';
import 'element-plus/theme-chalk/el-empty.css';
import 'element-plus/theme-chalk/el-image.css';
import 'element-plus/theme-chalk/el-button.css';

const data = reactive({
  boxOfficeData: [],
  markData: [],
});

const loading = reactive({
  boxOffice: false,
  mark: false,
});

// 前三名用奖牌色，其余用中性底（规范 §2.8；名次同时由数字表达，不依赖颜色）
const rankBadgeClass = (index) =>
  index < 3 ? `rank-item__badge--top${index + 1}` : 'rank-item__badge--plain';

const loadFilmBoxOfficeTop = () => {
  loading.boxOffice = true;
  request.get(FILM_API.BOX_OFFICE_TOP, {
    params: { topNum: 10 }
  }).then(res => {
    if (res.code === '200') {
      data.boxOfficeData = (res.data || []).slice(0, 10);
    } else {
      ElMessage.error(`票房数据加载失败：${res.msg || '未知错误'}`);
    }
  }).catch(err => {
    console.error('票房接口请求异常：', err);
    ElMessage.error('网络异常，无法加载票房数据');
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
      data.markData = (res.data || []).slice(0, 10);
    } else {
      ElMessage.error(`评分数据加载失败：${res.msg || '未知错误'}`);
    }
  }).catch(err => {
    console.error('评分接口请求异常：', err);
    ElMessage.error('网络异常，无法加载评分数据');
  }).finally(() => {
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
  font-size: var(--fs-xl);
  font-weight: var(--fw-bold);
  color: var(--el-color-primary);
}

.rank-card__loading {
  padding: var(--space-32) 0;
}

.rank-card__empty {
  padding: var(--space-64) 0;
  text-align: center;
}

.rank-card__body {
  padding-right: var(--space-12);
}

.rank-item {
  position: relative;
  display: flex;
  align-items: center;
  padding: var(--space-12) 0;
  border-bottom: 1px dashed var(--el-border-color);
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
}

.rank-item__poster {
  width: 60px;
  height: 80px;
  margin-right: var(--space-16);
  border-radius: var(--el-border-radius-base);
  object-fit: cover;
}

.rank-item__info {
  flex: 1;
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
