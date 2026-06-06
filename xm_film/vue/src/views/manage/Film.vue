<template>
  <div>
    <div class="card" style="margin-bottom: 5px">
      <el-input v-model="searchForm.title" placeholder="请输入电影名称查询" style="width: 300px; margin-right:10px" :prefix-icon="Search"/>
      <el-button type="primary" @click="onSearch">查 询</el-button>
      <el-button type="warning" @click="onReset">重 置</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-button type="info" @click="openAdd">新 增</el-button>
      <el-button type="danger" @click="handleDelBatch">批量删除</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="dataList" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="55"/>
        <el-table-column type="expand">
          <template #default="props">
            <el-descriptions title="电影信息" :column="4" border>
              <el-descriptions-item label="电影封面">
                <el-image style="width:36px;height:36px;object-fit:cover;" :src="props.row.img"/>
              </el-descriptions-item>
              <el-descriptions-item label="电影名称">{{props.row.title}}</el-descriptions-item>
              <el-descriptions-item label="英文名称">{{props.row.english}}</el-descriptions-item>
              <el-descriptions-item label="上映日期">{{props.row.start}}</el-descriptions-item>
              <el-descriptions-item label="电影时长">{{props.row.time}}分钟</el-descriptions-item>
              <el-descriptions-item label="电影类型">
                <el-tag v-for="item in props.row.typeList" style="margin-right: 5px; margin-bottom: 5px" :type="getTypeTagType(item)">{{ item.title }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="电影语言">{{props.row.language}}</el-descriptions-item>
              <el-descriptions-item label="电影分辨率">{{props.row.resolution}}</el-descriptions-item>
              <el-descriptions-item label="电影简介">
                <el-popover placement="top-start" title="电影简介" :width="200" trigger="hover" :content="props.row.content">
                  <template #reference>
                    <div class="line" style="width: 80px">{{props.row.content}}</div>
                  </template>
                </el-popover>
              </el-descriptions-item>
              <el-descriptions-item label="制作公司">
                <el-popover placement="top-start" title="制作公司" :width="200" trigger="hover" :content="props.row.employee">
                  <template #reference>
                    <div class="line" style="width: 100px">{{props.row.employee}}</div>
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
                <el-rate v-model="props.row.score" disabled show-score text-color="#ff9900" score-template="{value} 分"/>
              </el-descriptions-item>
            </el-descriptions>
          </template>
        </el-table-column>
        <el-table-column label="电影名称" prop="title" show-overflow-tooltip/>
        <el-table-column label="英文名称" prop="english" show-overflow-tooltip />
        <el-table-column label="封面" prop="img">
          <template #default="scope">
            <el-image style="margin-top: 7px; width:36px;height:36px;border-radius:10%;object-fit:cover;align-items:center;"
                      v-if="scope.row.img" :src="scope.row.img"
                      :preview-src-list="[scope.row.img]" preview-teleported/>
          </template>
        </el-table-column>
        <el-table-column label="上映日期" prop="start" />
        <el-table-column label="电影时长" prop="time" />
        <el-table-column label="电影类型" prop="typeList" width="180">
          <template v-slot="scope">
            <el-tag v-for="item in scope.row.typeList" style="margin-right: 5px; margin-bottom: 5px" :type="getTypeTagType(item)">{{ item.title }}</el-tag>
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
        <el-table-column label="操作">
          <template #default="scope">
            <el-button style="font-size: 18px" link :icon="Edit" @click="openEdit(scope.row)" type="primary"></el-button>
            <el-button style="font-size: 18px" link :icon="Delete" @click="() => handleDel(scope.row.id)" type="danger"></el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card" style="margin-bottom: 5px">
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

    <el-dialog v-model="dialogVisible" title="电影信息" width="500" destroy-on-close>
      <el-form ref="formRef" :rules="rules" :model="form" style="padding-right: 50px;padding-top: 20px" label-width="85px">
        <el-form-item label="电影名称" prop="title">
          <el-input v-model="form.title" autocomplete="off" placeholder="请输入电影名称"/>
        </el-form-item>
        <el-form-item label="电影封面" prop="img">
          <el-upload :action="FILE_UPLOAD_URL" :on-success="handleFileUpload"
                     :auto-upload="true" list-type="picture">
            <el-button type="primary">点击上传电影封面图</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="英文名称" prop="english">
          <el-input v-model="form.english" autocomplete="off" placeholder="请输入英文名称"/>
        </el-form-item>
        <el-form-item label="上映日期" prop="start">
          <el-date-picker v-model="form.start" type="date" value-format="YYYY-MM-DD"></el-date-picker>
        </el-form-item>
        <el-form-item label="电影时长" prop="time">
          <el-input-number v-model="form.time" :min="1" style="width: 220px"/>
        </el-form-item>
        <el-form-item label="电影类型" prop="typeIds">
          <el-select v-model="form.typeIds" multiple placeholder="请选择电影类型" style="width: 350px" @change="handleTypeChange">
            <el-option v-for="item in typeData" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="电影语言" prop="language">
          <el-select v-model="form.language" placeholder="请选择电影语言" style="width: 350px">
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
          <el-select v-model="form.resolution" placeholder="请选择分辨率" style="width: 350px">
            <el-option label="标准版" value="标准版" />
            <el-option label="2DIMAX" value="2DIMAX" />
            <el-option label="3DIMAX" value="3DIMAX" />
          </el-select>
        </el-form-item>
        <el-form-item label="电影简介" prop="content">
          <el-input type="textarea" :rows="4" v-model="form.content" autocomplete="off" placeholder="请输入电影简介"/>
        </el-form-item>
        <el-form-item label="制作公司" prop="employee">
          <el-input v-model="form.employee" autocomplete="off" placeholder="请输入制作公司"/>
        </el-form-item>
        <el-form-item label="电影区域" prop="areaId">
          <el-select v-model="form.areaId" placeholder="请选择制作区域" style="width: 350px">
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

  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Delete, Edit, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import { useFormDialog } from '@/composables/useFormDialog'
import { API_PATHS, FILE_UPLOAD_URL } from '@/constants'
import request from '@/utils/request'

const crud = useCrud(API_PATHS.FILMS)
const { dataList, total, pageNum, pageSize, searchForm, selectedIds,
        del, delBatch, onSearch, onReset, onPageChange, onSizeChange, onSelectionChange } = crud
const { dialogVisible, formRef, form, rules, openAdd, openEdit, submit, close } = useFormDialog(crud, {
  defaultForm: {
    title: '', english: '', img: '', start: '', time: undefined,
    language: '', content: '', resolution: '', employee: '',
    areaId: undefined, status: '', typeIds: []
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

function loadType() {
  request.get(API_PATHS.TYPES).then(res => {
    if (res.code === '200') typeData.value = res.data
  })
}

function loadArea() {
  request.get(API_PATHS.AREAS).then(res => {
    if (res.code === '200') areaData.value = res.data
  })
}

crud.load()
loadType()
loadArea()

function handleDel(id) {
  ElMessageBox.confirm('删除数据后无法恢复,您确认删除吗?', '删除确认', { type: 'warning' })
    .then(() => del(id)).catch()
}

function handleDelBatch() {
  if (!selectedIds.value.length) { ElMessage.warning('请选择数据'); return }
  ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 条数据吗？`, '删除确认', { type: 'warning' })
    .then(() => delBatch(selectedIds.value)).catch()
}

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

function getStatusType(status) {
  switch (status) {
    case '待上映': return 'warning'
    case '已上映': return 'success'
    case '停止上映': return 'danger'
    default: return 'info'
  }
}
</script>

<style scoped>
.line {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
