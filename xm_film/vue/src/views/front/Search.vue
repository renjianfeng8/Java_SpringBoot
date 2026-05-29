<template>
  <div class="search-results-container" style="padding: 20px; max-width: 1200px; margin: 0 auto;">
    <!-- 搜索信息栏 -->
    <div class="search-info" style="margin-bottom: 30px; padding-bottom: 15px; border-bottom: 1px solid #eee;">
      <h2 style="font-size: 22px; margin-bottom: 10px; color: #333;">
        搜索结果 <span class="keyword" style="color: #e53935; font-weight: bold;">“{{ searchTitle }}”</span>
      </h2>
      <p class="result-count" style="color: #666; font-size: 14px;">找到 {{ filmList.length }} 部相关电影</p>
    </div>

    <!-- 搜索结果列表 -->
    <div class="film-grid" v-if="filmList.length > 0">
      <div class="film-card" v-for="film in filmList" :key="film.id" @click="goToDetail(film.id)">
        <div style="position: relative; width: 100px; flex-shrink: 0;">
          <img :src="film.img" :alt="film.title" style="width: 100%; height: 150px; object-fit: cover;">
          <div style="position: absolute; bottom: 5px; right: 3px; color: #ffd700; font-size: 12px;">{{ film.score }}</div>
        </div>

        <div style="padding: 15px; flex-grow: 1; display: flex; flex-direction: column;">
          <h3 style="font-size: 16px; margin-bottom: 8px; color: #333; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">{{ film.title }}</h3>
          <h3 style="display: flex; gap: 10px; margin-bottom: 5px; font-size: 12px; color: #666; margin-top: 0;">{{ film.english }}</h3>

          <div style="display: flex; font-size: 12px; color: #666;margin-bottom: 5px; ">
            <span style="display: inline;">时长: {{ film.time }}分钟</span>
          </div>
          <div style="display: flex; font-size: 12px; color: #666; ; ">
            <span style="display: inline;">上映时间: {{ film.start }}</span>
          </div>

        </div>
      </div>
    </div>

    <!-- 无结果状态 -->
    <div style="text-align: center; padding: 60px 0;" v-else>
      <el-empty description="没有找到匹配的电影" />
      <el-button type="primary" @click="goBack">返回电影列表</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from "@/utils/request.js";

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
    const response = await request.get('/api/v1/films/by-title', { params: { title } })
    filmList.value = response.code === '200' ? (response.data || []) : []
  } catch (error) {
    console.error('搜索请求出错:', error)
  }
}

// 页面挂载时获取数据
onMounted(fetchSearchResults)
</script>

<style scoped>
/* 保留重复使用的类样式：网格容器和卡片基础样式 */
.film-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 25px;
}

.film-card {
  display: flex;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s, box-shadow 0.3s;
  cursor: pointer;
}

.film-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.15);
}
</style>
