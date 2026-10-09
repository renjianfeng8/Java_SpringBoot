<template>
  <div class="crud-page">
    <div class="page-head">
      <h2 class="page-head__title">影院名录</h2>
      <div class="page-head__action">
        <el-button type="primary" :icon="Upload" @click="openImport">从高德导入</el-button>
      </div>
    </div>

    <div class="card list-toolbar">
      <div class="list-toolbar__filters">
        <el-select v-model="query.city" placeholder="全部城市" aria-label="城市" clearable class="field-md"
                   @change="reload">
          <el-option v-for="c in filterData.cities" :key="c" :label="c" :value="c"/>
        </el-select>
        <el-select v-model="query.brand" placeholder="全部院线" aria-label="院线" clearable class="field-md"
                   @change="reload">
          <el-option v-for="b in filterData.brands" :key="b" :label="b" :value="b"/>
        </el-select>
      </div>
    </div>

    <div class="card table-card">
      <el-table v-loading="loading" stripe size="small" :data="rows">
        <el-table-column prop="name" label="影院名称" min-width="220"/>
        <el-table-column prop="brand" label="院线" width="140"/>
        <el-table-column prop="province" label="省" width="110"/>
        <el-table-column prop="city" label="市" width="110"/>
        <el-table-column prop="district" label="区县" width="110"/>
        <el-table-column prop="address" label="详细地址" min-width="240" show-overflow-tooltip/>
        <el-table-column prop="phone" label="电话" width="150"/>
        <template #empty>
          <div class="empty-hint">{{ error ? '数据加载失败，请稍后重试' : '暂无数据' }}</div>
        </template>
      </el-table>
      <div class="table-foot">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       :page-size="query.pageSize" :current-page="query.pageNum"
                       @current-change="handlePage"/>
      </div>
    </div>

    <el-dialog v-model="importDialog.visible" title="从高德导入影院名录" width="560" destroy-on-close>
      <el-select v-model="importDialog.cities" multiple filterable placeholder="选择城市（可多选）"
                 aria-label="导入城市" class="field-full">
        <el-option v-for="c in DIRECTORY_IMPORT_CITIES" :key="c" :label="c" :value="c"/>
      </el-select>

      <div v-if="importDialog.preview" class="import-preview">
        <p>抓取 {{ importDialog.preview.total }} 条：新增
          <strong>{{ importDialog.preview.freshCount }}</strong>，
          已存在跳过 <strong>{{ importDialog.preview.duplicateCount }}</strong></p>
        <p class="import-preview__sample">{{ importDialog.preview.sample.join('、') }}</p>
        <p v-for="w in importDialog.preview.warnings" :key="w" class="import-preview__warn">{{ w }}</p>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button :loading="importDialog.previewing" @click="doPreview">抓取预览</el-button>
          <el-button type="primary" :disabled="!importDialog.preview" :loading="importDialog.importing"
                     @click="doImport">确认导入</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, computed } from 'vue'
import { Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { CINEMA_DIRECTORY_API, DIRECTORY_IMPORT_CITIES } from '@/constants'

const query = reactive({ city: null, brand: null, pageNum: 1, pageSize: 10 })
const filterData = reactive({ cities: [], brands: [] })
const data = reactive({ list: [], total: 0, error: false, loading: false })
const rows = computed(() => data.list)
const total = computed(() => data.total)
const loading = computed(() => data.loading)
const error = computed(() => data.error)

const importDialog = reactive({
  visible: false, cities: [], preview: null, previewing: false, importing: false,
})

const loadFilters = () => request.get(CINEMA_DIRECTORY_API.FILTERS).then(res => {
  if (res.code === '200') { filterData.cities = res.data.cities; filterData.brands = res.data.brands }
}).catch(() => {})

const load = () => {
  data.error = false
  data.loading = true
  request.get(CINEMA_DIRECTORY_API.PAGE, {
    params: { city: query.city || undefined, brand: query.brand || undefined,
              pageNum: query.pageNum, pageSize: query.pageSize }
  }).then(res => {
    if (res.code === '200') { data.list = res.data.list; data.total = res.data.total }
    else { data.error = true }
  }).catch(() => { data.error = true })
    .finally(() => { data.loading = false })
}

const reload = () => { query.pageNum = 1; load() }
const handlePage = (p) => { query.pageNum = p; load() }

const requireCities = () => {
  if (!importDialog.cities.length) { ElMessage.warning('请先选择城市'); return false }
  return true
}

const openImport = () => {
  importDialog.visible = true
  importDialog.preview = null
}

const doPreview = () => {
  if (!requireCities()) return
  importDialog.previewing = true
  request.post(CINEMA_DIRECTORY_API.IMPORT_PREVIEW, { cities: importDialog.cities })
    .then(res => {
      if (res.code === '200') importDialog.preview = res.data
      else ElMessage.error(res.msg)
    })
    .finally(() => { importDialog.previewing = false })
}

const doImport = () => {
  importDialog.importing = true
  request.post(CINEMA_DIRECTORY_API.IMPORT, { cities: importDialog.cities })
    .then(res => {
      if (res.code === '200') {
        ElMessage.success(`本次新增 ${res.data} 条`)
        importDialog.visible = false
        importDialog.preview = null
        loadFilters()
        reload()
      } else {
        ElMessage.error(res.msg)
      }
    })
    .finally(() => { importDialog.importing = false })
}

loadFilters()
load()
</script>

<style scoped>
.import-preview { margin-top: var(--space-16); line-height: 1.8; }
.import-preview__sample { color: var(--el-text-color-secondary); font-size: var(--fs-xs); }
.import-preview__warn { color: var(--el-color-warning); }
</style>
