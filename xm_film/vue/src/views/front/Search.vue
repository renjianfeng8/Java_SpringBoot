<template>
  <div class="search-results-container">
    <!-- 搜索信息栏 -->
    <div class="search-info">
      <h2 class="search-title">
        搜索结果 <span class="keyword">“{{ searchTitle }}”</span>
      </h2>
      <p class="result-count">找到 {{ filmList.length }} 部相关电影</p>
    </div>

    <!-- 搜索结果列表 -->
    <div class="film-grid" v-if="filmList.length > 0">
      <div class="film-card" v-for="film in filmList" :key="film.id" @click="goToDetail(film.id)">
        <div class="film-card__poster-wrap">
          <img :src="film.img" :alt="film.title" class="film-card__img">
          <div class="film-card__score">{{ film.score }}</div>
        </div>

        <div class="film-card__body">
          <h3 class="film-card__name">{{ film.title }}</h3>
          <h3 class="film-card__english">{{ film.english }}</h3>

          <div class="film-card__meta film-card__meta--tight">
            <span class="film-card__meta-text">时长: {{ film.time }}分钟</span>
          </div>
          <div class="film-card__meta">
            <span class="film-card__meta-text">上映时间: {{ film.start }}</span>
          </div>

        </div>
      </div>
    </div>

    <!-- 无结果状态 -->
    <div class="search-empty" v-else>
      <el-empty description="没有找到匹配的电影" />
      <el-button type="primary" @click="goBack">返回电影列表</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from "@/utils/request.js";
import { FILM_API } from '@/constants';

// 路由实例
const route = useRoute()
const router = useRouter()

// 响应式数据
const searchTitle = ref('')
const filmList = ref([])

// 跳转到电影详情页
const goToDetail = (id) => {
  router.push(`/front/filmDetail/${id}`)
}

// 返回电影列表页
const goBack = () => {
  router.push('/front/movie')
}

// 获取搜索结果数据
const fetchSearchResults = async () => {
  try {
    const title = route.query.title || ''
    searchTitle.value = title
    if (!title.trim()) return
    const response = await request.get(FILM_API.SEARCH, { params: { title } })
    filmList.value = response.code === '200' ? (response.data || []) : []
  } catch (error) {
    console.error('搜索请求出错:', error)
    ElMessage.error('搜索失败，请稍后重试')
  }
}

// 页面挂载时获取数据
onMounted(fetchSearchResults)
</script>

<style scoped>
.search-results-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--space-20);
}

.search-info {
  margin-bottom: var(--space-32);
  padding-bottom: var(--space-16);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.search-title {
  margin: 0 0 var(--space-12);
  font-size: var(--fs-2xl);
  font-weight: var(--fw-bold);
  color: var(--el-text-color-primary);
}

/* 关键词承载文字，用主色（5.58:1） */
.keyword {
  color: var(--el-color-primary);
}

.result-count {
  margin: 0;
  font-size: var(--fs-base);
  color: var(--el-text-color-regular);
}

/* 保留重复使用的类样式：网格容器和卡片基础样式 */
.film-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--space-24);
}

.film-card {
  display: flex;
  background: var(--el-bg-color);
  border-radius: var(--el-border-radius-base);
  overflow: hidden;
  box-shadow: var(--el-box-shadow-lighter);
  transition: transform 200ms ease-in-out, box-shadow 200ms ease-in-out;
  cursor: pointer;
}

.film-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--el-box-shadow-light);
}

.film-card__poster-wrap {
  position: relative;
  flex-shrink: 0;
  width: 100px;
}

.film-card__img {
  width: 100%;
  height: 150px;
  object-fit: cover;
}

/* 评分叠加在图片上，用装饰金（规范 §3.6：仅深底 / 图片叠加） */
.film-card__score {
  position: absolute;
  right: var(--space-4);
  bottom: var(--space-4);
  font-size: var(--fs-xs);
  color: var(--color-rating);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.6);
}

.film-card__body {
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  padding: var(--space-16);
}

.film-card__name {
  margin: 0 0 var(--space-8);
  font-size: var(--fs-md);
  font-weight: var(--fw-bold);
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.film-card__english {
  display: flex;
  gap: var(--space-12);
  margin: 0 0 var(--space-4);
  font-size: var(--fs-xs);
  font-weight: var(--fw-regular);
  color: var(--el-text-color-regular);
}

.film-card__meta {
  display: flex;
  font-size: var(--fs-xs);
  color: var(--el-text-color-regular);
}

.film-card__meta--tight {
  margin-bottom: var(--space-4);
}

.film-card__meta-text {
  display: inline;
}

.search-empty {
  padding: var(--space-64) 0;
  text-align: center;
}
</style>
