<template>
  <div class="crud-page">
    <div class="page-head">
      <h2 class="page-head__title">电影预告</h2>
      <div class="page-head__action">
        <el-button type="primary" :icon="Plus" @click="openAdd">新 增</el-button>
      </div>
    </div>

    <div class="card list-toolbar">
      <div class="list-toolbar__filters">
        <el-input v-model="searchForm.title" placeholder="请输入电影名称查询" aria-label="电影名称"
                  class="search-input" :prefix-icon="Search" @keyup.enter="onSearch" />
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
        <el-table-column type="selection" width="55" />
        <el-table-column label="电影名称" prop="title" />
        <el-table-column label="电影图片" prop="img">
          <template #default="scope">
            <el-image class="cell-thumb" v-if="scope.row.img" :src="scope.row.img" :preview-src-list="[scope.row.img]" preview-teleported />
          </template>
        </el-table-column>
        <el-table-column label="视频名称" prop="name" />
        <el-table-column label="预告视频" prop="preview" width="220">
          <template #default="scope">
            <video v-if="scope.row.preview" :src="scope.row.preview" class="small-video" controls>
              <track kind="captions" src="/subtitles/trailer-zh.vtt" srclang="zh" label="中文字幕" />
            </video>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" prop="start" />
        <el-table-column label="操作" width="110">
          <template #default="scope">
            <el-button class="row-action" link :icon="Edit" aria-label="编辑" @click="openEdit(scope.row)" type="primary" />
            <el-button class="row-action" link :icon="Delete" aria-label="删除" @click="confirmDel(scope.row.id)" type="danger" />
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

    <el-dialog v-model="dialogVisible" title="电影预告视频" width="500" destroy-on-close>
      <el-form ref="formRef" :rules="rules" :model="form" class="dialog-form" label-width="85px"
               status-icon @submit.prevent>
        <el-form-item label="电影名称" prop="title">
          <el-input v-model="form.title" autocomplete="off" placeholder="请输入电影名称" @keyup.enter="submit" />
        </el-form-item>
        <el-form-item label="电影图片" prop="img">
          <el-upload :action="FILE_UPLOAD_URL" :on-success="handleFileUpload" :auto-upload="true" list-type="picture">
            <el-button type="primary">点击上传电影图片</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="视频名称" prop="name">
          <el-input v-model="form.name" autocomplete="off" placeholder="请输入视频名称" @keyup.enter="submit" />
        </el-form-item>
        <el-form-item label="发布时间" prop="start">
          <el-date-picker v-model="form.start" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="预告视频" prop="preview">
          <el-upload :action="FILE_UPLOAD_URL" :on-success="handleVideoUpload" :auto-upload="true" list-type="text">
            <el-button type="primary">点击上传视频</el-button>
          </el-upload>
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
import { Delete, Edit, Plus, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import { useFormDialog } from '@/composables/useFormDialog'
import { API_PATHS, FILE_UPLOAD_URL } from '@/constants'

const crud = useCrud(API_PATHS.VIDEOS)
const { dataList, total, pageNum, pageSize, searchForm, selectedIds, loading, error,
        confirmDel, confirmDelBatch, onSearch, onReset, onPageChange, onSizeChange, onSelectionChange } = crud
const { dialogVisible, formRef, form, rules, openAdd, openEdit, submit, close } = useFormDialog(crud, {
  defaultForm: { title: '', name: '', img: '', preview: '', start: '' },
  rules: {
    title: [{ required: true, message: '请输入电影名称', trigger: 'blur' }],
    start: [{ required: true, message: '请选择上映日期', trigger: 'change' }]
  }
})

crud.load()

function handleFileUpload(res) {
  if (res.code === '200') { form.img = res.data; ElMessage.success('图片上传成功') }
  else { ElMessage.error(res.msg || '图片上传失败') }
}

function handleVideoUpload(res) {
  if (res.code === '200') { form.preview = res.data; ElMessage.success('视频上传成功') }
  else { ElMessage.error(res.msg || '视频上传失败') }
}
</script>

<style scoped>
.small-video {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
}
</style>
