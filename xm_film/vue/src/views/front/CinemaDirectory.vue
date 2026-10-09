<template>
  <div class="page-narrow">
    <div class="directory-filters">
      <el-select v-model="query.city" placeholder="全部城市" clearable class="directory-filters__item"
                 @change="reload">
        <el-option v-for="c in filterData.cities" :key="c" :label="c" :value="c"/>
      </el-select>
      <el-select v-model="query.brand" placeholder="全部院线" clearable class="directory-filters__item"
                 @change="reload">
        <el-option v-for="b in filterData.brands" :key="b" :label="b" :value="b"/>
      </el-select>
    </div>

    <el-table :data="rows" border class="directory-table">
      <el-table-column prop="name" label="影院名称" min-width="220"/>
      <el-table-column prop="brand" label="院线" width="140">
        <template #default="{ row }">{{ row.brand || '独立/其他' }}</template>
      </el-table-column>
      <el-table-column label="省 / 市 / 区" min-width="200">
        <template #default="{ row }">{{ [row.province, row.city, row.district].filter(Boolean).join(' / ') }}</template>
      </el-table-column>
      <el-table-column prop="address" label="详细地址" min-width="260"/>
      <el-table-column prop="phone" label="电话" width="160">
        <template #default="{ row }">{{ row.phone || '—' }}</template>
      </el-table-column>
    </el-table>

    <div v-if="data.error" class="empty-hint">数据加载失败，请稍后重试</div>
    <div v-else-if="!rows.length" class="empty-hint">暂无数据</div>

    <div v-if="total" class="directory-pagination">
      <el-pagination background layout="total, prev, pager, next" :total="total"
                     :page-size="query.pageSize" :current-page="query.pageNum"
                     @current-change="handlePage"/>
    </div>
  </div>
</template>

<script setup>
import { reactive, computed } from 'vue'
import request from '@/utils/request.js'
import { CINEMA_DIRECTORY_API } from '@/constants'

const query = reactive({ city: null, brand: null, pageNum: 1, pageSize: 15 })
const filterData = reactive({ cities: [], brands: [] })
const data = reactive({ list: [], total: 0, error: false })
const rows = computed(() => data.list)
const total = computed(() => data.total)

// 拉不到筛选项只让下拉留空，不打断列表加载；错误提示由 request.js 拦截器统一弹出
const loadFilters = () => {
  request.get(CINEMA_DIRECTORY_API.FILTERS).then(res => {
    if (res.code === '200') {
      filterData.cities = res.data.cities
      filterData.brands = res.data.brands
    }
  }).catch(() => {})
}

const load = () => {
  data.error = false
  request.get(CINEMA_DIRECTORY_API.PAGE, {
    params: { city: query.city || undefined, brand: query.brand || undefined,
              pageNum: query.pageNum, pageSize: query.pageSize }
  }).then(res => {
    if (res.code === '200') {
      data.list = res.data.list
      data.total = res.data.total
    } else {
      data.error = true
    }
  }).catch(() => { data.error = true })
}

const reload = () => { query.pageNum = 1; load() }
const handlePage = (p) => { query.pageNum = p; load() }

loadFilters()
load()
</script>

<style scoped>
.directory-filters { display: flex; gap: var(--space-12); padding: var(--space-16) 0; }
.directory-filters__item { width: 200px; }
.directory-pagination { margin-top: var(--space-16); }
</style>
