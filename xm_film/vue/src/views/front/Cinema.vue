<template>
  <div class="page-narrow">
    <div class="cinema-filters">
      <el-select v-model="query.city" placeholder="全部城市" aria-label="城市" clearable
                 class="cinema-filters__item" @change="onCityChange">
        <el-option v-for="c in filters.cities" :key="c" :label="c" :value="c"/>
      </el-select>
      <el-select v-model="query.district" placeholder="全部区域" aria-label="区域" clearable
                 class="cinema-filters__item" :disabled="!query.city" @change="reload">
        <el-option v-for="d in districtOptions" :key="d" :label="d" :value="d"/>
      </el-select>
    </div>
    <!-- 影院列表：整卡是 router-link，可直接进入影院详情（此前整页没有任何跳转入口） -->
    <div class="cinema-list">
      <router-link
          v-for="cinema in data.filmData"
          :key="cinema.id"
          class="cinema-card"
          :to="`/front/cinemaDetail/${cinema.id}`"
      >
        <div class="cinema-card__poster">
          <img :src="avatarSrc(cinema.avatar)" :alt="`${cinema.name || '影院'} 图片`" class="cinema-card__img" @error="onAvatarError">
        </div>

        <div class="cinema-card__info">
          <h3 class="cinema-card__name">{{ cinema.name }}</h3>

          <!-- 影院服务标签：用功能色，白字压其上均达 AA -->
          <div class="cinema-card__tags">
            <span class="service-tag service-tag--refund">退票无忧</span>
            <span class="service-tag service-tag--promo">儿童优惠</span>
            <span class="service-tag service-tag--wifi">WiFi覆盖</span>
          </div>

          <div class="cinema-card__detail">
            <p class="cinema-card__detail-row">
              <span class="cinema-card__detail-label">电话:</span>{{ cinema.phone }}
            </p>
            <p class="cinema-card__detail-row">
              <span class="cinema-card__detail-label">地址:</span>{{ cinema.address }}
            </p>
          </div>
        </div>
      </router-link>
    </div>

    <div v-if="data.error" class="empty-hint">数据加载失败，请稍后重试</div>
    <div v-else-if="!data.filmData.length" class="empty-hint">暂无数据</div>

    <div v-if="data.total" class="cinema-list__pagination">
      <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          v-model:current-page="data.pageNum"
          v-model:page-size="data.pageSize"
          :page-sizes="[5, 10, 15]"
          background
          layout="total, prev, pager, next"
          :total="data.total"
      />
    </div>
  </div>
</template>

<script setup>
import { reactive, computed } from 'vue';
import request from "@/utils/request.js";
import { ElMessage } from 'element-plus';
import { API_PATHS, apiPage, CINEMA_API } from '@/constants';
import { avatarSrc, onAvatarError } from '@/utils/avatar';

const data = reactive({
  pageNum: 1,
  pageSize: 12,
  total: 0,
  filmData: [], // 影院数据
  error: false
})

const query = reactive({ city: null, district: null })
const filters = reactive({ cities: [], districtsByCity: {} })
const districtOptions = computed(() =>
    query.city ? (filters.districtsByCity[query.city] || []) : [])

// 拉不到筛选项只留空下拉，不打断列表；网络错误提示由 request.js 统一弹
const loadRegions = () => {
  request.get(CINEMA_API.REGIONS).then(res => {
    if (res.code === '200') {
      filters.cities = res.data.map(r => r.city)
      filters.districtsByCity = Object.fromEntries(res.data.map(r => [r.city, r.districts]))
    }
  }).catch(() => {})
}

const onCityChange = () => { query.district = null; reload() }
const reload = () => { data.pageNum = 1; load() }

// 请求序号：慢的旧响应后到不得覆盖新结果
let loadSeq = 0

const load = () => {
  const seq = ++loadSeq
  data.error = false
  request.get(apiPage(API_PATHS.CINEMAS), {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      city: query.city || undefined,
      district: query.district || undefined,
    }
  }).then(res => {
    if (seq !== loadSeq) return
    if (res.code === '200') {
      data.filmData = res.data.list
      data.total = res.data.total
    } else {
      // 业务码非 200 不经过响应拦截器（它只管网络/超时/HTTP 状态），故这里补一次提示
      ElMessage.error(res.msg)
      data.error = true
      data.filmData = []
      data.total = 0
    }
  }).catch(err => {
    if (seq !== loadSeq) return
    // 网络与 5xx 那类失败的提示由 request.js 的响应拦截器给出，这里只落错误态，
    // 否则同一次失败会弹两次
    console.error(err)
    data.error = true
    // 清掉上一次的结果，否则错误提示下面还挂着旧筛选的影院卡片
    data.filmData = []
    data.total = 0
  })
}

const handleSizeChange = (newSize) => {
  data.pageSize = newSize;
  data.pageNum = 1; // 重置为第一页
  load();
}

const handleCurrentChange = (newPage) => {
  data.pageNum = newPage;
  load();
};

loadRegions()
load()
</script>

<style scoped>
.cinema-filters {
  display: flex;
  gap: var(--space-12);
  padding: var(--space-16) 0;
}

.cinema-filters__item {
  width: 200px;
}

.cinema-list {
  padding: var(--space-16) 0;
}

.cinema-card {
  display: flex;
  margin-bottom: var(--space-16);
  padding: var(--space-16);
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  color: inherit;
  text-decoration: none;
  transition: transform 200ms ease-in-out, box-shadow 200ms ease-in-out;
}

.cinema-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--el-box-shadow-lighter);
}

.cinema-card__poster {
  flex-shrink: 0;
  width: 200px;
}

.cinema-card__img {
  display: block;
  width: 100%;
  aspect-ratio: 10 / 7;
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-fill-color-light);
  object-fit: cover;
}

.cinema-card__info {
  flex: 1;
  min-width: 0;
  margin-left: var(--space-16);
}

.cinema-card__name {
  margin: 0;
  font-size: var(--fs-lg);
  font-weight: var(--fw-bold);
  color: var(--el-text-color-primary);
}

.cinema-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-8);
  margin-top: var(--space-12);
}

/* 底色由共享层的 .service-tag--* 提供（与影院详情页同色），这里只管形状 */
.service-tag {
  padding: var(--space-4) var(--space-12);
  border-radius: var(--el-border-radius-base);
  font-size: var(--fs-xs);
  color: var(--color-on-accent);
}

.cinema-card__detail {
  margin-top: var(--space-12);
  color: var(--el-text-color-regular);
}

.cinema-card__detail-row {
  display: flex;
  align-items: center;
  margin: var(--space-8) 0;
}

/* 标签文字承载内容，用主色（5.58:1）而非品牌红（3.81:1） */
.cinema-card__detail-label {
  margin-right: var(--space-8);
  color: var(--el-color-primary);
}

.cinema-list__pagination {
  margin-top: var(--space-16);
}
</style>
