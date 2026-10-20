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
        <el-select v-model="query.linked" aria-label="收录状态" class="field-md" @change="reload">
          <el-option label="全部收录状态" :value="null"/>
          <el-option label="已收录" :value="true"/>
          <el-option label="未收录" :value="false"/>
        </el-select>
      </div>
      <div class="list-toolbar__actions">
        <span v-if="selectedIds.length" class="selection-count" aria-live="polite">已选 {{ selectedIds.length }} 项</span>
        <el-button type="primary" :disabled="!selectedIds.length" @click="confirmImport">导入为影院</el-button>
      </div>
    </div>

    <div class="card table-card">
      <el-table ref="tableRef" v-loading="loading" stripe size="small" :data="rows" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="55" :selectable="canSelectRow"/>
        <el-table-column prop="name" label="影院名称" min-width="220"/>
        <el-table-column prop="brand" label="院线" width="140"/>
        <el-table-column prop="province" label="省" width="110"/>
        <el-table-column prop="city" label="市" width="110"/>
        <el-table-column prop="district" label="区县" width="110"/>
        <el-table-column prop="address" label="详细地址" min-width="240" show-overflow-tooltip/>
        <el-table-column prop="phone" label="电话" width="150"/>
        <el-table-column label="收录状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.linked ? 'success' : 'info'" size="small" effect="plain">
              {{ row.linked ? '已收录' : '未收录' }}
            </el-tag>
          </template>
        </el-table-column>
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
    <el-dialog v-model="credentialDialog.visible" title="影院账号与初始口令" width="560" destroy-on-close>
      <el-alert type="warning" :closable="false" show-icon
                title="初始口令只显示这一次，关闭后无法再次查看，请立即复制并分发。"/>
      <el-table :data="credentialDialog.accounts" size="small" max-height="320" class="credential-table">
        <el-table-column prop="username" label="登录账号" min-width="180"
                         align="left" header-align="left"/>
        <el-table-column prop="initialPassword" label="初始口令" min-width="180"
                         align="left" header-align="left"/>
        <template #empty>
          <div class="empty-hint">本次没有新建影院账号</div>
        </template>
      </el-table>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="credentialDialog.visible = false">关闭</el-button>
          <el-button type="primary" @click="copyCredentials">复制全部</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { isSuccess, CINEMA_DIRECTORY_API, CINEMA_API, DIRECTORY_IMPORT_CITIES } from '@/constants'
import { reactive, computed, ref } from 'vue'
import { Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const query = reactive({ city: null, brand: null, linked: null, pageNum: 1, pageSize: 10 })
const filterData = reactive({ cities: [], brands: [] })
const data = reactive({ list: [], total: 0, error: false, loading: false })
const rows = computed(() => data.list)
const total = computed(() => data.total)
const loading = computed(() => data.loading)
const error = computed(() => data.error)

const selectedIds = ref([])
const onSelectionChange = (rows) => { selectedIds.value = rows.map(r => r.id) }

// 初始口令只在导入响应里出现一次，关掉对话框就再也取不到，因此单独弹窗展示
const credentialDialog = reactive({ visible: false, accounts: [] })

const copyCredentials = () => {
  const text = credentialDialog.accounts
    .map(a => `${a.username}\t${a.initialPassword}`)
    .join('\n')
  if (!navigator.clipboard) {
    ElMessage.warning('当前浏览器不支持自动复制，请手动选择文本')
    return
  }
  navigator.clipboard.writeText(text)
    .then(() => ElMessage.success('已复制到剪贴板'))
    .catch(() => ElMessage.warning('复制失败，请手动选择文本'))
}

const tableRef = ref(null)
// 已收录（linked）的行在后端会被幂等跳过，勾选它只会得到「新增 0 条」的困惑，故直接禁选
const canSelectRow = (row) => !row.linked

// 把选中的名录行生成为真实影院；幂等，已导入过的行不会重复建
const confirmImport = () => {
  ElMessageBox.confirm(
    `将这 ${selectedIds.value.length} 条名录导入为影院吗？导入后前台立即可见（不含影厅/排片）。`,
    '导入确认', { type: 'warning' }
  ).then(async () => {
    try {
      const res = await request.post(CINEMA_API.FROM_DIRECTORY, { directoryIds: selectedIds.value })
      if (isSuccess(res)) {
        const created = res.data?.created ?? 0
        const accounts = res.data?.accounts ?? []
        ElMessage.success(`新增 ${created} 条影院`)
        if (accounts.length) {
          credentialDialog.accounts = accounts
          credentialDialog.visible = true
        }
        tableRef.value?.clearSelection()
        load()
      } else {
        ElMessage.error(res.msg)
      }
    } catch (e) {
      // 网络/HTTP 错误提示由 request.js 拦截器统一弹出
    }
  }).catch(() => {})
}

const importDialog = reactive({
  visible: false, cities: [], preview: null, previewing: false, importing: false,
})

const loadFilters = () => request.get(CINEMA_DIRECTORY_API.FILTERS).then(res => {
  if (isSuccess(res)) { filterData.cities = res.data.cities; filterData.brands = res.data.brands }
}).catch(() => {})

let loadSeq = 0

const load = () => {
  const seq = ++loadSeq
  data.error = false
  data.loading = true
  request.get(CINEMA_DIRECTORY_API.PAGE, {
    // linked 是布尔：用 || 会把「未收录」(false) 吞成「全部」，故只判 null/undefined
    params: { city: query.city || undefined, brand: query.brand || undefined,
              linked: query.linked ?? undefined,
              pageNum: query.pageNum, pageSize: query.pageSize }
  }).then(res => {
    if (seq !== loadSeq) return
    if (isSuccess(res)) { data.list = res.data.list; data.total = res.data.total }
    else { data.error = true }
  }).catch(() => { if (seq === loadSeq) data.error = true })
    .finally(() => { if (seq === loadSeq) data.loading = false })
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
      if (isSuccess(res)) importDialog.preview = res.data
      else ElMessage.error(res.msg)
    })
    .catch(() => {}) // 网络异常的错误提示由 request.js 拦截器统一弹出
    .finally(() => { importDialog.previewing = false })
}

const doImport = () => {
  importDialog.importing = true
  request.post(CINEMA_DIRECTORY_API.IMPORT, { cities: importDialog.cities })
    .then(res => {
      if (isSuccess(res)) {
        ElMessage.success(`本次新增 ${res.data} 条`)
        importDialog.visible = false
        importDialog.preview = null
        loadFilters()
        reload()
      } else {
        ElMessage.error(res.msg)
      }
    })
    .catch(() => {}) // 网络异常的错误提示由 request.js 拦截器统一弹出
    .finally(() => { importDialog.importing = false })
}

loadFilters()
load()
</script>

<style scoped>
.import-preview { margin-top: var(--space-16); line-height: 1.8; }
.import-preview__sample { color: var(--el-text-color-secondary); font-size: var(--fs-xs); }
.import-preview__warn { color: var(--el-color-warning); }
.credential-table { margin-top: var(--space-16); }
</style>
