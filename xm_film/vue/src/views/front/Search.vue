<template>
  <div class="page-narrow search-results">
    <!-- 搜索信息栏 -->
    <div class="search-info">
      <h2 class="search-title">
        搜索结果 <span class="keyword">“{{ searchTitle }}”</span>
      </h2>
      <p class="result-count">找到 {{ filmList.length }} 部相关电影</p>
    </div>

    <!-- 结果列表：整卡是 router-link，原生可 Tab 聚焦、可回车进入详情 -->
    <div v-if="filmList.length" class="film-row-grid">
      <router-link
          v-for="film in filmList"
          :key="film.id"
          class="film-row"
          :to="`/front/filmDetail/${film.id}`"
      >
        <div class="film-row__poster">
          <img :src="film.img" :alt="`《${film.title}》海报`" class="film-row__img">
          <!-- 评分压在图上（§3.6：--color-rating 可用于图片叠加）。
               垫一层 --overlay-mask 就不需要给文字描边，也就不再手写 text-shadow（§7.2） -->
          <span v-if="scoreText(film)" class="film-row__score">{{ scoreText(film) }}</span>
        </div>

        <div class="film-row__body">
          <h3 class="film-row__name" :title="film.title">{{ film.title }}</h3>
          <p class="film-row__english">{{ film.english }}</p>
          <p class="film-row__meta">时长：{{ film.time }} 分钟</p>
          <p class="film-row__meta">上映时间：{{ film.start }}</p>
        </div>
      </router-link>
    </div>

    <!-- 空态与失败态文案固定且必须区分（规范 §11.2）：请求失败时说「暂无数据」
         会让用户以为库里确实没有，从而不去重试 -->
    <div v-else-if="error" class="empty-hint">数据加载失败，请稍后重试</div>
    <div v-else-if="!loading" class="search-empty">
      <p class="empty-hint">暂无数据</p>
      <el-button type="primary" @click="goBack">返回电影列表</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from "@/utils/request.js";
import { FILM_API } from '@/constants';
import { formatScoreBadge } from '@/utils/format.js';

const route = useRoute()
const router = useRouter()

const searchTitle = ref('')
const filmList = ref([])
const loading = ref(false)
const error = ref(false)

const goBack = () => {
  router.push('/front/movie')
}

// 徽章只放数字，无评价时为空串（见 utils/format.js#formatScoreBadge）
const scoreText = (film) => formatScoreBadge(film.score)

const fetchSearchResults = async () => {
  const title = route.query.title || ''
  searchTitle.value = title
  if (!title.trim()) return

  loading.value = true
  error.value = false
  try {
    const response = await request.get(FILM_API.SEARCH, { params: { title } })
    if (response.code === '200') {
      filmList.value = response.data || []
    } else {
      // 业务码非 200 走的是 HTTP 200，不经响应拦截器，必须自己落错误态 ——
      // 否则会落到「暂无数据」，让用户以为库里确实没有这部片
      error.value = true
      filmList.value = []
    }
  } catch (err) {
    // 网络 / 超时 / 5xx 的失败提示由 request.js 的响应拦截器统一给出，页面只落错误态
    console.error('搜索请求出错:', err)
    error.value = true
    filmList.value = []
  } finally {
    loading.value = false
  }
}

onMounted(fetchSearchResults)

// 顶栏搜索是同路由只换 query，组件会被复用、onMounted 不再触发，
// 没有这个 watch 就会一直显示上一次的结果（改 router.push 之前是整页重载，看不出问题）
watch(() => route.query.title, fetchSearchResults)
</script>

<style scoped>
.search-info {
  margin-bottom: var(--space-24);
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

/* 横向条目网格。此处不并入 FilmPosterCard：那个是竖版海报卡，
   这里是「海报缩略图 + 多行文字」的另一种形状，强行合并只会给组件加一堆开关 */
.film-row-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--space-24);
}

.film-row {
  display: flex;
  overflow: hidden;
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
  color: inherit;
  text-decoration: none;
  transition: transform 200ms ease-in-out, box-shadow 200ms ease-in-out;
}

.film-row:hover {
  transform: translateY(-4px);
  box-shadow: var(--el-box-shadow-light);
}

.film-row__poster {
  position: relative;
  flex-shrink: 0;
  width: 100px;
  aspect-ratio: 2 / 3;
  background-color: var(--el-fill-color-light);
}

.film-row__img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.film-row__score {
  position: absolute;
  right: var(--space-4);
  bottom: var(--space-4);
  padding: var(--space-4) var(--space-8);
  border-radius: var(--el-border-radius-small);
  background-color: var(--overlay-mask);
  color: var(--color-rating);
  font-size: var(--fs-xs);
  /* 纯数字，可用 500（§4.4） */
  font-weight: var(--fw-medium);
  line-height: var(--lh-loose);
}

.film-row__body {
  flex-grow: 1;
  min-width: 0;
  padding: var(--space-16);
}

.film-row__name {
  margin: 0 0 var(--space-8);
  font-size: var(--fs-md);
  font-weight: var(--fw-bold);
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 原名是副标题而不是标题，故用 p（原来写成 h3 会让读屏软件的标题跳级） */
.film-row__english {
  margin: 0 0 var(--space-4);
  font-size: var(--fs-xs);
  color: var(--el-text-color-regular);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.film-row__meta {
  margin: 0 0 var(--space-4);
  font-size: var(--fs-xs);
  color: var(--el-text-color-regular);
}

.search-empty {
  padding: var(--space-40) 0;
  text-align: center;
}
</style>
