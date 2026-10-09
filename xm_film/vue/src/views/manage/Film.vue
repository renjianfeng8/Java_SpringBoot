<template>
  <div class="crud-page">
    <div class="page-head">
      <h2 class="page-head__title">电影信息</h2>
      <div class="page-head__action">
        <el-button :icon="Download" @click="openImport">从 TMDB 导入</el-button>
        <el-button :icon="VideoPlay" :loading="backfilling" @click="runBackfill">补预告片</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新 增</el-button>
      </div>
    </div>

    <div class="card list-toolbar">
      <div class="list-toolbar__filters">
        <el-input v-model="searchForm.title" placeholder="请输入电影名称查询" aria-label="电影名称"
                  class="search-input" :prefix-icon="Search" @keyup.enter="onSearch"/>
        <el-button type="primary" @click="onSearch">查 询</el-button>
        <el-button type="warning" @click="onReset">重 置</el-button>
      </div>
      <div class="list-toolbar__actions">
        <span v-if="selectedIds.length" class="selection-count" aria-live="polite">已选 {{ selectedIds.length }} 项</span>
        <el-button type="danger" :disabled="!selectedIds.length" @click="confirmDelBatch">批量删除</el-button>
      </div>
    </div>

    <div class="card table-card">
      <el-table v-loading="loading" stripe size="small" :data="dataList" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="55"/>
        <el-table-column type="expand">
          <template #default="props">
            <el-descriptions title="电影信息" :column="4" border>
              <el-descriptions-item label="电影封面">
                <el-image class="cell-thumb" :src="props.row.img"/>
              </el-descriptions-item>
              <el-descriptions-item label="电影名称">{{props.row.title}}</el-descriptions-item>
              <el-descriptions-item label="英文名称">{{props.row.english}}</el-descriptions-item>
              <el-descriptions-item label="上映日期">{{props.row.start}}</el-descriptions-item>
              <el-descriptions-item label="电影时长">{{props.row.time}}分钟</el-descriptions-item>
              <el-descriptions-item label="电影类型">
                <el-tag v-for="item in props.row.typeList" class="tag-gap" :type="getTypeTagType(item)">{{ item.title }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="电影语言">{{props.row.language}}</el-descriptions-item>
              <el-descriptions-item label="电影分辨率">{{props.row.resolution}}</el-descriptions-item>
              <el-descriptions-item label="电影简介">
                <el-popover placement="top-start" title="电影简介" :width="200" trigger="hover" :content="props.row.content">
                  <template #reference>
                    <div class="line line--content">{{props.row.content}}</div>
                  </template>
                </el-popover>
              </el-descriptions-item>
              <el-descriptions-item label="制作公司">
                <el-popover placement="top-start" title="制作公司" :width="200" trigger="hover" :content="props.row.employee">
                  <template #reference>
                    <div class="line line--employee">{{props.row.employee}}</div>
                  </template>
                </el-popover>
              </el-descriptions-item>
              <el-descriptions-item label="电影区域">{{props.row.areaName}}</el-descriptions-item>
              <el-descriptions-item label="电影状态">
                  <el-tag :type="getStatusType(props.row.status)">
                    {{ props.row.status }}
                  </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="电影评分">
                <el-rate v-if="props.row.score != null" v-model="props.row.score" disabled show-score text-color="var(--color-rating-text)" score-template="{value} 分"/>
                <span v-else>暂无评分</span>
              </el-descriptions-item>
            </el-descriptions>
          </template>
        </el-table-column>
        <el-table-column label="电影名称" prop="title" show-overflow-tooltip/>
        <el-table-column label="英文名称" prop="english" show-overflow-tooltip />
        <el-table-column label="封面" prop="img">
          <template #default="scope">
            <el-image class="cell-thumb"
                      v-if="scope.row.img" :src="scope.row.img"
                      :preview-src-list="[scope.row.img]" preview-teleported/>
          </template>
        </el-table-column>
        <el-table-column label="上映日期" prop="start" />
        <el-table-column label="电影时长" prop="time" />
        <el-table-column label="电影类型" prop="typeList" width="180">
          <template v-slot="scope">
            <el-tag v-for="item in scope.row.typeList" class="tag-gap" :type="getTypeTagType(item)">{{ item.title }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="语言" prop="language" />
        <el-table-column label="电影简介" prop="content" show-overflow-tooltip />
        <el-table-column label="制作区域" prop="areaName" />
        <el-table-column label="电影状态" prop="status">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110">
          <template #default="scope">
            <el-button class="row-action" link :icon="Edit" aria-label="编辑" @click="openEdit(scope.row)" type="primary"></el-button>
            <el-button class="row-action" link :icon="Delete" aria-label="删除" @click="confirmDel(scope.row.id)" type="danger"></el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-hint">{{ error ? '数据加载失败，请稍后重试' : '暂无数据' }}</div>
        </template>
      </el-table>
      <div class="table-foot">
        <el-pagination
            @size-change="onSizeChange"
            @current-change="onPageChange"
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :page-sizes="[5, 10, 15, 20]"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="电影信息" width="500" destroy-on-close>
      <el-form ref="formRef" :rules="rules" :model="form" class="dialog-form" label-width="85px"
               status-icon @submit.prevent>
        <el-form-item label="电影名称" prop="title">
          <el-input v-model="form.title" autocomplete="off" placeholder="请输入电影名称" @keyup.enter="submit"/>
        </el-form-item>
        <el-form-item label="电影封面" prop="img">
          <el-upload :action="FILE_UPLOAD_URL" :headers="uploadHeaders"
                     :on-success="handleFileUpload" :on-error="handleUploadError"
                     :auto-upload="true" list-type="picture">
            <el-button type="primary">点击上传电影封面图</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="英文名称" prop="english">
          <el-input v-model="form.english" autocomplete="off" placeholder="请输入英文名称" @keyup.enter="submit"/>
        </el-form-item>
        <el-form-item label="上映日期" prop="start">
          <el-date-picker v-model="form.start" type="date" value-format="YYYY-MM-DD"></el-date-picker>
        </el-form-item>
        <el-form-item label="电影时长" prop="time">
          <el-input-number v-model="form.time" :min="1" class="field-md"/>
        </el-form-item>
        <el-form-item label="电影类型" prop="typeIds">
          <el-select v-model="form.typeIds" multiple placeholder="请选择电影类型" class="field-lg" @change="handleTypeChange">
            <el-option v-for="item in typeData" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="主演" prop="actorId">
          <el-select v-model="form.actorId" placeholder="请选择主演" class="field-lg" clearable filterable>
            <el-option v-for="item in actorData" :key="item.id" :label="item.actorName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="电影语言" prop="language">
          <el-select v-model="form.language" placeholder="请选择电影语言" class="field-lg">
            <el-option label="普通话" value="普通话" />
            <el-option label="英语" value="英语" />
            <el-option label="港语" value="港语" />
            <el-option label="法语" value="法语" />
            <el-option label="日语" value="日语" />
            <el-option label="俄语" value="俄语" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="分辨率" prop="resolution">
          <el-select v-model="form.resolution" placeholder="请选择分辨率" class="field-lg">
            <el-option label="标准版" value="标准版" />
            <el-option label="2DIMAX" value="2DIMAX" />
            <el-option label="3DIMAX" value="3DIMAX" />
          </el-select>
        </el-form-item>
        <el-form-item label="电影简介" prop="content">
          <el-input type="textarea" :rows="4" v-model="form.content" autocomplete="off" placeholder="请输入电影简介"/>
        </el-form-item>
        <el-form-item label="预告视频" prop="video">
          <el-input v-model="form.video" autocomplete="off" placeholder="YouTube 链接，从 TMDB 导入时自动填充"/>
        </el-form-item>
        <el-form-item label="制作公司" prop="employee">
          <el-input v-model="form.employee" autocomplete="off" placeholder="请输入制作公司" @keyup.enter="submit"/>
        </el-form-item>
        <el-form-item label="电影区域" prop="areaId">
          <el-select v-model="form.areaId" placeholder="请选择制作区域" class="field-lg">
            <el-option v-for="item in areaData" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="电影状态" prop="status">
         <el-radio-group v-model="form.status">
           <el-radio-button label="待上映" value="待上映" />
           <el-radio-button label="已上映" value="已上映" />
           <el-radio-button label="停止上映" value="停止上映" />
         </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="close">取 消</el-button>
          <el-button type="primary" @click="submit">保 存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- TMDB 导入：只产出一份预填值，最终仍由上面的「新增」表单确认后保存 -->
    <el-dialog v-model="importVisible" title="从 TMDB 导入影片" width="720">
      <el-input v-model="importQuery" placeholder="输入影片名（中英文均可）" aria-label="TMDB 搜索片名"
                :prefix-icon="Search" @keyup.enter="doImportSearch">
        <template #append>
          <el-button :loading="searching" @click="doImportSearch">搜 索</el-button>
        </template>
      </el-input>
      <p class="import-hint">选中后点「导 入」，字段会回填到新增表单，核对后再保存。海报与主演头像会下载到本机。</p>

      <div v-loading="searching" class="import-results">
        <el-empty v-if="!searching && importResults.length === 0"
                  description="输入片名搜索 TMDB" :image-size="60"/>
        <div v-for="item in importResults" :key="item.tmdbId"
             class="import-item" :class="{ 'import-item--active': importSelectedId === item.tmdbId }"
             role="button" tabindex="0" :aria-pressed="importSelectedId === item.tmdbId"
             @click="importSelectedId = item.tmdbId" @keyup.enter="importSelectedId = item.tmdbId">
          <img v-if="item.posterUrl" :src="item.posterUrl" alt="" class="import-item__poster" loading="lazy">
          <div v-else class="import-item__poster import-item__poster--empty" aria-hidden="true"></div>
          <div class="import-item__body">
            <div class="import-item__title">
              {{ item.title }}<span v-if="item.year" class="import-item__year">（{{ item.year }}）</span>
            </div>
            <div class="import-item__original">{{ item.originalTitle }}</div>
            <div class="import-item__overview">{{ item.overview || '暂无简介' }}</div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="importVisible = false">取 消</el-button>
          <el-button type="primary" :loading="importing" :disabled="!importSelectedId" @click="applyImport">
            导 入
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 补预告片：给 video 为空的历史影片按片名回查 TMDB。结果里列出没补上的影片及原因 -->
    <el-dialog v-model="backfillVisible" title="补预告片结果" width="520">
      <p class="backfill-summary">
        共处理 {{ backfillResult.scanned }} 部，补上 {{ backfillResult.updated }} 部。
      </p>
      <el-table v-if="backfillResult.missed.length" :data="backfillResult.missed" size="small" max-height="320">
        <el-table-column label="影片" prop="title" show-overflow-tooltip/>
        <el-table-column label="未补原因" prop="reason" width="160"/>
      </el-table>
      <p v-else class="backfill-summary">其余均已有预告片或本次已补上。</p>
      <template #footer>
        <el-button type="primary" @click="backfillVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { Delete, Download, Edit, Plus, Search, VideoPlay } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import { useFormDialog } from '@/composables/useFormDialog'
import { API_PATHS, FILE_UPLOAD_URL, TMDB_API, getFilmStatusType as getStatusType } from '@/constants'
import request from '@/utils/request'
import { uploadHeaders, handleUploadError } from '@/utils/upload'

const crud = useCrud(API_PATHS.FILMS)
const { dataList, total, pageNum, pageSize, searchForm, selectedIds, loading, error,
        confirmDel, confirmDelBatch, onSearch, onReset, onPageChange, onSizeChange, onSelectionChange } = crud
const { dialogVisible, formRef, form, rules, openAdd, openEdit, submit, close } = useFormDialog(crud, {
  defaultForm: {
    title: '', english: '', img: '', start: '', time: undefined,
    language: '', content: '', resolution: '', employee: '',
    areaId: undefined, actorId: undefined, status: '', typeIds: [], video: ''
  },
  rules: {
    title: [{ required: true, message: '请输入电影名称', trigger: 'blur' }],
    start: [{ required: true, message: '请选择上映日期', trigger: 'change' }],
    time: [{ required: true, message: '请输入电影时长', trigger: 'blur' }],
    language: [{ required: true, message: '请选择电影语言', trigger: 'change' }],
    content: [{ required: true, message: '请输入电影简介', trigger: 'blur' }],
    areaId: [{ required: true, message: '请选择制作区域', trigger: 'change' }],
    status: [{ required: true, message: '请选择电影状态', trigger: 'change' }]
  }
})

const typeData = ref([])
const areaData = ref([])
const actorData = ref([])

// 三个都返回 Promise：导入回填前必须等下拉数据刷新完，否则新回填的 id 在下拉里找不到标签
function loadType() {
  return request.get(API_PATHS.TYPES).then(res => {
    if (res.code === '200') typeData.value = res.data
  })
}

function loadArea() {
  return request.get(API_PATHS.AREAS).then(res => {
    if (res.code === '200') areaData.value = res.data
  })
}

function loadActor() {
  return request.get(API_PATHS.ACTORS).then(res => {
    if (res.code === '200') actorData.value = res.data
  })
}

crud.load()
loadType()
loadArea()
loadActor()

function handleFileUpload(res) {
  if (res.code === '200') { form.img = res.data; ElMessage.success('电影封面上传成功') }
  else { ElMessage.error(res.msg || '电影封面上传失败') }
}

function handleTypeChange(val) {
  if (val.length > 4) {
    form.typeIds = val.slice(0, 4)
    ElMessage.warning('最多只能选择4种电影类型')
  } else {
    form.typeIds = val
  }
}

function getTypeTagType(type) {
  const tagTypes = ['primary', 'success', 'warning', 'danger', 'info']
  const seed = type?.id ?? String(type?.title || '').charCodeAt(0) ?? 0
  return tagTypes[Math.abs(seed) % tagTypes.length]
}

// ===== TMDB 导入 =====
// 只做「搜片 → 取预填值 → 回填到新增表单」。这里没有写接口 —— 落库仍由上面的保存按钮
// 走 POST /api/v1/films，管理员不确认就不会多出一部影片。

const importVisible = ref(false)
const importQuery = ref('')
const importResults = ref([])
const importSelectedId = ref(null)
const searching = ref(false)
const importing = ref(false)

function openImport() {
  importQuery.value = ''
  importResults.value = []
  importSelectedId.value = null
  importVisible.value = true
}

async function doImportSearch() {
  const query = importQuery.value.trim()
  if (!query) {
    ElMessage.warning('请输入影片名')
    return
  }
  searching.value = true
  try {
    const res = await request.get(TMDB_API.SEARCH, { params: { query } })
    if (res.code === '200') {
      importResults.value = res.data || []
      importSelectedId.value = null
      if (!importResults.value.length) ElMessage.warning('TMDB 没有匹配的影片')
    } else {
      ElMessage.error(res.msg || '搜索失败')
    }
  } catch {
    // request.js 已统一弹提示
  } finally {
    searching.value = false
  }
}

async function applyImport() {
  if (!importSelectedId.value) return
  importing.value = true
  try {
    const res = await request.get(TMDB_API.MOVIE(importSelectedId.value))
    if (res.code !== '200') {
      ElMessage.error(res.msg || '导入失败')
      return
    }

    const preview = res.data || {}
    // 取详情这一步后端可能刚建了新的类型/地区/演职人员行，必须重取列表；
    // 否则回填的 id 在下拉里找不到对应标签，看起来像「没填上」
    await Promise.all([loadType(), loadArea(), loadActor()])

    openAdd()
    Object.assign(form, {
      title: preview.title ?? '',
      english: preview.english ?? '',
      start: preview.start ?? '',
      time: preview.time ?? undefined,
      language: preview.language ?? '',
      content: preview.content ?? '',
      img: preview.img ?? '',
      employee: preview.employee ?? '',
      status: preview.status ?? '',
      areaId: preview.areaId ?? undefined,
      actorId: preview.actorId ?? undefined,
      typeIds: preview.typeIds ?? [],
      video: preview.video ?? ''
    })
    importVisible.value = false

    // 降级信息必须让管理员看见：哪项没取到、类型被截断、图片下载失败
    if (preview.warnings?.length) {
      ElMessage.warning(preview.warnings.join('；'))
    }
    ElMessage.success('已回填，请核对后保存')
  } catch {
    // request.js 已统一弹提示
  } finally {
    importing.value = false
  }
}

// ===== 补预告片 =====
// 给 video 为空的历史影片按片名回查 TMDB。后端幂等：已有预告片的会跳过，重复点不会覆盖。
const backfilling = ref(false)
const backfillVisible = ref(false)
const backfillResult = reactive({ scanned: 0, updated: 0, missed: [] })

async function runBackfill() {
  backfilling.value = true
  try {
    const res = await request.post(TMDB_API.BACKFILL_VIDEOS)
    if (res.code === '200') {
      Object.assign(backfillResult, { scanned: 0, updated: 0, missed: [] }, res.data)
      backfillVisible.value = true
      ElMessage.success(`已补 ${backfillResult.updated} 部预告片`)
    } else {
      ElMessage.error(res.msg || '补预告片失败')
    }
  } catch {
    // request.js 已统一弹提示
  } finally {
    backfilling.value = false
  }
}
</script>
