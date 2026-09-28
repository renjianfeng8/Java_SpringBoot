<template>
  <div class="film-cinema">
    <!-- 1. 加载状态提示 -->
    <div v-if="loading" class="page-hint page-hint--loading">
      正在加载电影及影院信息...
    </div>

    <!-- 2. 错误提示 -->
    <div v-else-if="errorMsg" class="page-hint page-hint--error">
      {{ errorMsg }}
    </div>

    <!-- 3. 核心内容：电影完整信息 + 目标影院列表 -->
    <div v-else>
      <!-- 3.1 电影详情头部（完整信息展示） -->
      <div class="film-hero">
        <div class="film-hero__inner">
          <!-- 电影海报 -->
          <div class="film-hero__poster">
            <img :src="film.img " alt="电影海报" class="film-hero__img">
          </div>

          <!-- 电影基本信息 -->
          <div class="film-hero__info">
            <div class="film-hero__title">{{ film.title || '未知电影' }}</div>
            <div class="film-hero__meta">{{ film.english || '无英文标题' }}</div>
            <div class="film-hero__meta">{{ film.types.join(' / ') || '未知类型' }}</div>
            <div class="film-hero__meta">
              {{ film.area || '未知地区' }} / {{ film.time || '未知时长' }} / {{ film.language || '未知语言' }}
            </div>
            <div class="film-hero__meta">
              上映时间：{{ film.start || '未知' }} / 格式：{{ film.resolution || '未知' }}
            </div>
            <el-button
                class="film-hero__action"
                @click="goToFilmDetail(film.id)"
            >
              查看更多电影详情
            </el-button>
          </div>

          <!-- 评分和票房（突出展示） -->
          <div class="film-hero__stats">
            <div class="film-hero__stat">
              <div class="film-hero__stat-label">影片口碑</div>
              <div class="film-hero__stat-value">{{ film.score || 0 }}分</div>
            </div>
            <div>
              <div class="film-hero__stat-label">累计票房</div>
              <div class="film-hero__stat-value">{{ formatBoxOffice(film.boxOffice) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 3.2 核心：当前电影上映影院列表（与后端筛选逻辑对齐） -->
      <div class="cinemas-section">
        <div class="cinemas-panel">
          <!-- 影院列表标题（明确关联当前电影） -->
          <h3 class="cinemas-panel__title">
            【{{ film.title || '当前电影' }}】上映影院列表
          </h3>

          <!-- 影院列表内容 -->
          <div>
            <!-- 无影院数据提示（优化文案） -->
            <div v-if="cinemaData.filmData.length === 0" class="empty-hint">
              <div class="empty-hint__title">暂无该电影的上映影院信息</div>
              <div class="empty-hint__desc">可能该电影尚未排片或暂无合作影院</div>
            </div>

            <!-- 循环渲染影院卡片（优化布局和交互） -->
            <div v-for="(cinema, index) in cinemaData.filmData" :key="index" class="cinema-card">
              <!-- 1. 图片容器 -->
              <div class="cinema-card__poster">
                <img :src="cinema.avatar" alt="影院图片" class="cinema-card__img">
              </div>

              <!-- 2. 信息容器 -->
              <div class="cinema-card__info">
                <!-- 影院名称 -->
                <div class="cinema-card__name-row">
                  <div class="cinema-card__name">{{ cinema.name || '未知影院' }}</div>
                </div>

                <!-- 影院服务标签：用功能色，白字压其上均达 AA（规范 §2.5） -->
                <div class="cinema-card__tags">
                  <div class="service-tag service-tag--refund">退票无忧</div>
                  <div class="service-tag service-tag--promo">儿童优惠</div>
                  <div class="service-tag service-tag--wifi">WiFi覆盖</div>
                  <div class="service-tag service-tag--parking">免费停车</div>
                </div>

                <!-- 影院详细信息 -->
                <div class="cinema-card__detail">
                  <!-- 电话 -->
                  <div class="cinema-card__detail-row">
                    <span class="cinema-card__detail-label">电话：</span>
                    <span>{{ cinema.phone || '暂无' }}</span>
                  </div>

                  <!-- 邮箱 -->
                  <div class="cinema-card__detail-row">
                    <span class="cinema-card__detail-label">邮箱：</span>
                    <span>{{ cinema.email || '暂无' }}</span>
                  </div>

                  <!-- 地址（控制单行显示，避免高度过高） -->
                  <div class="cinema-card__detail-row cinema-card__detail-row--ellipsis">
                    <span class="cinema-card__detail-label">地址：</span>
                    <span class="cinema-card__detail-value">{{ cinema.address || '暂无' }}</span>
                  </div>
                </div>
              </div>

              <!-- 3. 购票按钮容器 -->
              <div class="cinema-card__action">
                <el-button
                    type="primary"
                    size="default"
                    class="cinema-card__buy"
                    @click="goCinemaDetail(cinema.id, film.id)"
                >
                  立即购票
                </el-button>
              </div>
            </div>
          </div>

          <!-- 分页组件（确保与后端参数同步） -->
          <div class="cinemas-panel__pagination" v-if="cinemaData.total > 0">
            <el-pagination
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
                :current-page="cinemaData.pageNum"
                :page-sizes="[5, 10, 15]"
                :page-size="cinemaData.pageSize"
                :total="cinemaData.total"
                background
                layout="total, prev, pager, next, jumper"
                :disabled="cinemaData.total === 0"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {reactive, ref, onMounted} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {ElMessage} from 'element-plus';
import request from "@/utils/request.js";
import 'element-plus/theme-chalk/el-pagination.css';
import 'element-plus/theme-chalk/el-button.css';
import { API_PATHS, apiById, apiPage } from '@/constants';
import { formatBoxOffice } from '@/utils/format.js';

// 1. 路由相关（获取电影ID + 路由跳转）
const route = useRoute();
const router = useRouter();
const filmId = route.params.id; // 从路由参数获取当前电影ID（核心筛选条件）

// 2. 电影信息状态（补充完整字段，与后端返回对齐）
const loading = ref(false);
const errorMsg = ref('');
const film = reactive({
  id: '',
  title: '',
  english: '',
  img: '',
  score: 0,
  boxOffice: 0,
  types: [],
  area: '',
  time: '',
  language: '',
  resolution: '',
  start: '', // 上映时间
});

// 3. 影院列表状态（与后端分页参数严格对齐）
const cinemaData = reactive({
  filmData: [], // 影院列表数据
  pageNum: 1,   // 当前页码（默认1，与后端@RequestParam(defaultValue="1")对齐）
  pageSize: 10, // 每页条数（默认10，与后端@RequestParam(defaultValue="10")对齐）
  total: 0      // 总影院数（后端返回的PageInfo.total）
});

// 4. 路由跳转函数
// 跳转到电影详情页
const goToFilmDetail = (filmId) => {
  if (!filmId || isNaN(Number(filmId))) {
    ElMessage.warning('电影ID无效，无法查看详情');
    return;
  }
  router.push({path: `/front/filmDetail/${filmId}`});
};


const goCinemaDetail = (cinemaId, filmId) => {
  if (!cinemaId || !filmId) {
    ElMessage.warning('参数无效，无法查看影院详情');
    return;
  }
  // 跳转到cinemaDetail页面，使用影院ID作为路径参数，同时携带电影ID作为查询参数
  router.push({
    path: `/front/cinemaDetail/${cinemaId}`,  // 路径参数：影院ID
    query: { filmId: filmId }                 // 查询参数：电影ID（可选，根据需要）
  });
};

// 5. 加载电影完整信息（补充所有字段，与后端返回对齐）
const fetchFilmFullInfo = () => {
  // 前置校验：电影ID无效直接提示
  if (!filmId || isNaN(Number(filmId))) {
    errorMsg.value = '电影ID无效，请返回电影列表重试';
    return Promise.reject('电影ID无效');
  }

  return request.get(apiById(API_PATHS.FILMS, filmId))
      .then(res => {
        if (res.code === '200' && res.data) {
          const data = res.data;
          // 完整赋值电影信息（覆盖所有前端展示字段）
          Object.assign(film, {
            id: data.id,
            title: data.title?.trim() || '未知电影',
            english: data.english || '无英文标题',
            img: data.img,
            score: data.score || 0,
            boxOffice: data.boxOffice || 0,
            types: (data.typeList || []).map(t => t.title),
            area: data.areaName || '未知地区',
            time: data.time ? `${data.time}分钟` : '未知时长',
            language: data.language || '未知语言',
            resolution: data.resolution || '未知格式',
            start: data.start || '未知'
          });
          return Promise.resolve(); // 加载成功，允许后续加载影院
        } else {
          const errMsg = `电影信息加载失败：${res.msg || '未找到该电影'}`;
          errorMsg.value = errMsg;
          ElMessage.error(errMsg);
          return Promise.reject(errMsg);
        }
      })
      .catch(err => {
        const errMsg = `网络异常：${err.message || '无法加载电影信息'}`;
        errorMsg.value = errMsg;
        ElMessage.error(errMsg);
        return Promise.reject(errMsg);
      });
};

// 6. 核心功能：加载“当前电影上映的影院”（与后端接口参数严格对齐）
const loadCinemaList = () => {
  // 前置校验：电影ID无效不发起请求
  if (!filmId || isNaN(Number(filmId))) {
    loading.value = false;
    errorMsg.value = '电影ID无效，无法筛选影院';
    return;
  }

  loading.value = true;
  // 发起请求：携带 pageNum、pageSize、filmId 三个参数（与后端Controller参数对齐）
  request.get(apiPage(API_PATHS.CINEMAS), {
    params: {
      pageNum: cinemaData.pageNum,   // 分页页码
      pageSize: cinemaData.pageSize, // 每页条数
      filmId: Number(filmId)         // 电影ID（转为数字，与后端Integer类型对齐）
    }
  })
      .then(res => {
        if (res.code === '200' && res.data) {
          // 后端返回PageInfo对象，提取list和total（与后端Service返回的PageInfo对齐）
          cinemaData.filmData = res.data.list || [];
          cinemaData.total = res.data.total || 0;
        } else {
          const errMsg = `影院列表加载失败：${res.msg || '未知错误'}`;
          ElMessage.error(errMsg);
          cinemaData.filmData = [];
          cinemaData.total = 0;
        }
      })
      .catch(err => {
        const errMsg = `网络异常：${err.message || '无法加载影院列表'}`;
        ElMessage.error(errMsg);
        cinemaData.filmData = [];
        cinemaData.total = 0;
      })
      .finally(() => {
        loading.value = false; // 无论成功失败，关闭加载状态
      });
};

// 7. 分页事件处理（与后端分页逻辑联动）
// 每页条数改变
const handleSizeChange = (newSize) => {
  cinemaData.pageSize = newSize;
  cinemaData.pageNum = 1; // 重置为第一页
  loadCinemaList(); // 重新加载筛选后的影院列表
};

// 页码改变
const handleCurrentChange = (newPage) => {
  cinemaData.pageNum = newPage;
  loadCinemaList(); // 重新加载筛选后的影院列表
};

// 8. 页面初始化：先加载电影信息，再加载对应影院（确保筛选条件有效）
onMounted(() => {
  // 先加载电影信息，成功后再加载影院列表
  fetchFilmFullInfo().then(() => {
    loadCinemaList();
  });
});
</script>

<style scoped>
.film-cinema {
  width: 100%;
  margin: 0 auto;
}

/* 加载 / 错误提示 */
.page-hint {
  text-align: center;
  color: var(--el-text-color-regular);
}

.page-hint--loading {
  padding: var(--space-48);
}

.page-hint--error {
  padding: var(--space-20);
  color: var(--el-color-danger);
}

/* ---------- 3.1 电影详情头部（深色表面，规范 §2.7） ---------- */
.film-hero {
  background-color: var(--dark-bg-hero);
}

.film-hero__inner {
  display: flex;
  align-items: flex-start;
  width: 60%;
  max-width: 1200px;
  margin: 0 auto;
}

.film-hero__poster {
  margin-top: var(--space-4);
}

.film-hero__img {
  width: 250px;
  height: 300px;
  object-fit: cover;
}

.film-hero__info {
  flex: 2;
  margin-top: var(--space-16);
  margin-left: var(--space-24);
  color: var(--dark-text);
}

.film-hero__title {
  font-size: var(--fs-3xl);
  font-weight: var(--fw-bold);
}

.film-hero__meta {
  margin: var(--space-8) 0;
  font-size: var(--fs-base);
}

/* 深底上的 CTA：实底主色 + 白字（白字压 #BF352D 为 5.58:1） */
.film-hero__action {
  width: 70%;
  height: 45px;
  margin-top: var(--space-20);
  border: none;
  background-color: var(--el-color-primary);
  color: var(--color-on-accent);
  font-size: var(--fs-lg);
}

.film-hero__stats {
  display: flex;
  flex: 1;
  flex-direction: column;
  justify-content: center;
  margin-top: var(--space-20);
  color: var(--dark-text);
  text-align: center;
}

.film-hero__stat {
  margin-bottom: var(--space-32);
}

.film-hero__stat-label {
  font-size: var(--fs-md);
  opacity: 0.8;
}

.film-hero__stat-value {
  margin: var(--space-12) 0;
  font-size: var(--fs-5xl);
  font-weight: var(--fw-bold);
}

/* ---------- 3.2 上映影院列表 ---------- */
.cinemas-section {
  padding: var(--space-32) 0;
  background-color: var(--el-fill-color-lighter);
}

.cinemas-panel {
  width: 60%;
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--space-20);
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
}

.cinemas-panel__title {
  margin: 0 0 var(--space-20);
  padding-left: var(--space-12);
  border-left: 4px solid var(--color-brand);
  font-size: var(--fs-lg);
  font-weight: var(--fw-bold);
  color: var(--el-text-color-primary);
}

.empty-hint {
  padding: var(--space-48);
  border: 1px dashed var(--el-border-color-lighter);
  border-radius: var(--el-border-radius-base);
  text-align: center;
  color: var(--el-text-color-secondary);
}

.empty-hint__title {
  margin-bottom: var(--space-12);
  font-size: var(--fs-md);
}

.empty-hint__desc {
  font-size: var(--fs-base);
  opacity: 0.7;
}

.cinema-card {
  display: flex;
  align-items: stretch;
  gap: var(--space-16);
  margin-bottom: var(--space-16);
  padding: var(--space-16);
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  transition: box-shadow 200ms ease-in-out;
}

.cinema-card:hover {
  box-shadow: var(--el-box-shadow-lighter);
}

.cinema-card__poster {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 180px;
  height: 145px;
  overflow: hidden;
}

.cinema-card__img {
  width: 100%;
  height: 100%;
  border-radius: var(--el-border-radius-base);
  object-fit: cover;
}

.cinema-card__info {
  display: flex;
  flex: 1;
  flex-direction: column;
  justify-content: space-between;
  min-height: 120px;
}

.cinema-card__name-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cinema-card__name {
  font-size: var(--fs-lg);
  font-weight: var(--fw-bold);
  color: var(--el-text-color-primary);
}

.cinema-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-8);
  margin: var(--space-8) 0;
}

.service-tag {
  padding: var(--space-4) var(--space-8);
  border-radius: var(--el-border-radius-base);
  font-size: var(--fs-xs);
  color: var(--color-on-accent);
}

.service-tag--refund {
  background-color: var(--el-color-primary);
}

.service-tag--promo {
  background-color: var(--el-color-warning);
}

.service-tag--wifi {
  background-color: var(--el-color-info);
}

.service-tag--parking {
  background-color: var(--el-color-success);
}

.cinema-card__detail {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: var(--space-4);
  margin-top: var(--space-4);
  font-size: var(--fs-base);
  color: var(--el-text-color-regular);
}

.cinema-card__detail-row {
  display: flex;
  align-items: center;
}

.cinema-card__detail-row--ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 标签文字承载内容，用主色（5.58:1）而非品牌红（3.81:1） */
.cinema-card__detail-label {
  min-width: 50px;
  margin-right: var(--space-8);
  color: var(--el-color-primary);
}

.cinema-card__detail-value {
  flex: 1;
  word-break: break-all;
}

.cinema-card__action {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 120px;
}

.cinema-card__buy {
  width: 100%;
  height: 40px;
  font-size: var(--fs-md);
}

.cinemas-panel__pagination {
  margin-top: var(--space-20);
  text-align: right;
}
</style>
