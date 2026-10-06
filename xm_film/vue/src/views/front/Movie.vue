<template>
  <div class="page-narrow">

    <div class="filter-panel">

      <div class="filter-row">
        <div class="filter-label">类型 :</div>
        <div class="filter-options">
          <button
              class="filter-chip"
              :class="{ 'filter-chip--active': !data.typeFlag }"
              @click="changeTypeFlag(null)"
          >全部</button>
          <button
              v-for="item in data.typeData"
              :key="item.id"
              class="filter-chip"
              :class="{ 'filter-chip--active': data.typeFlag === item.id }"
              @click="changeTypeFlag(item.id)"
          >{{ item.title }}</button>
        </div>
      </div>

      <div class="filter-row">
        <div class="filter-label">年代 :</div>
        <div class="filter-options">
          <button
              class="filter-chip"
              :class="{ 'filter-chip--active': !data.yearFlag }"
              @click="changeYearFlag(null)"
          >全部</button>
          <button
              v-for="item in data.yearData"
              :key="item"
              class="filter-chip"
              :class="{ 'filter-chip--active': data.yearFlag === item }"
              @click="changeYearFlag(item)"
          >{{ item }}</button>
        </div>
      </div>

      <div class="filter-row">
        <div class="filter-label">区域 :</div>
        <div class="filter-options">
          <button
              class="filter-chip"
              :class="{ 'filter-chip--active': !data.areaFlag }"
              @click="changeAreaFlag(null)"
          >全部</button>
          <button
              v-for="item in data.areaData"
              :key="item.id"
              class="filter-chip"
              :class="{ 'filter-chip--active': data.areaFlag === item.id }"
              @click="changeAreaFlag(item.id)"
          >{{ item.title }}</button>
        </div>
      </div>

      <div class="filter-row filter-row--last">
        <div class="filter-label">状态 :</div>
        <div class="filter-options">
          <button
              class="filter-chip"
              :class="{ 'filter-chip--active': !data.statusFlag }"
              @click="changeStatusFlag(null)"
          >全部</button>
          <button
              v-for="item in FILM_STATUS_OPTIONS"
              :key="item"
              class="filter-chip"
              :class="{ 'filter-chip--active': data.statusFlag === item }"
              @click="changeStatusFlag(item)"
          >{{ item }}</button>
        </div>
      </div>

    </div>

    <div v-if="data.loading" class="poster-grid" aria-hidden="true">
      <div v-for="n in 8" :key="n" class="poster-skeleton"></div>
    </div>

    <div v-else class="poster-grid">
      <FilmPosterCard v-for="item in data.filmData" :key="item.id" :film="item">
        <el-tag size="small" :type="getStatusType(item.status)">{{ item.status }}</el-tag>
      </FilmPosterCard>
    </div>

    <div v-if="data.error" class="empty-hint">数据加载失败，请稍后重试</div>
    <div v-else-if="!data.loading && !data.filmData.length" class="empty-hint">暂无数据</div>

    <div v-if="data.total" class="film-grid__pagination">
      <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :page-sizes="[12, 24, 36]"
          background
          layout="total, prev, pager, next"
          :total="data.total"
      />
    </div>

  </div>
</template>

<script setup>
import { reactive } from "vue";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";
import { API_PATHS, apiPage, FILM_STATUS_OPTIONS, getFilmStatusType as getStatusType } from '@/constants';

const data = reactive({
  typeFlag: null,
  yearFlag: null,
  areaFlag: null,
  statusFlag: null,
  typeData: [],
  areaData: [],
  yearData: [],
  pageNum: 1,
  pageSize: 12,
  total: 0,
  filmData: [],
  loading: false,
  error: false
})

const load = () => {
  data.loading = true;
  request.get(apiPage(API_PATHS.FILMS), {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      typeId: data.typeFlag,
      areaId: data.areaFlag,
      year: data.yearFlag,
      status: data.statusFlag
    }
  }).then(res => {
    if (res.code === '200') {
      data.filmData = res.data.list
      data.total = res.data.total
      data.error = false
    } else {
      data.error = true
      ElMessage.error(res.msg)
    }
  }).catch(err => {
    // 网络异常的统一提示由 request.js 的响应拦截器给出，这里只落错误态
    console.error('电影列表接口请求异常：', err)
    data.error = true
  }).finally(() => {
    data.loading = false
  })
}

const loadType = () => {
  request.get(API_PATHS.TYPES).then(res => {
    if (res.code === '200') {
      data.typeData = res.data
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const loadArea = () => {
  request.get(API_PATHS.AREAS).then(res => {
    if (res.code === '200') {
      data.areaData = res.data
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const loadYear = () => {
  request.get(API_PATHS.YEARS).then(res => {
    if (res.code === '200') {
      data.yearData = res.data
    } else {
      ElMessage.error(res.msg)
    }
  })
}

// 切换筛选项后回到第一页：否则在第 3 页筛选只剩 1 页数据时会停在空页
const applyFilter = (assign) => {
  assign();
  data.pageNum = 1;
  load();
}

const changeTypeFlag = (id) => applyFilter(() => { data.typeFlag = id })
const changeYearFlag = (year) => applyFilter(() => { data.yearFlag = year })
const changeAreaFlag = (id) => applyFilter(() => { data.areaFlag = id })
const changeStatusFlag = (status) => applyFilter(() => { data.statusFlag = status })

const handleSizeChange = (newSize) => {
  data.pageSize = newSize;
  load();
}

const handleCurrentChange = (newPage) => {
  data.pageNum = newPage;
  load();
};

loadType()
loadArea()
loadYear()
load()
</script>

<style scoped>
.film-grid__pagination {
  margin-top: var(--space-24);
}

/* 加载占位：与海报同比例、同圆角的灰块，避免列表到位时页面跳高 */
.poster-skeleton {
  aspect-ratio: 2 / 3;
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-fill-color-light);
}
</style>
