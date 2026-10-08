<template>
  <div class="crud-page">
    <div class="page-head">
      <h2 class="page-head__title">演职人员</h2>
      <div class="page-head__action">
        <el-button type="primary" :icon="Plus" @click="openAdd">新 增</el-button>
      </div>
    </div>

    <div class="card list-toolbar">
      <div class="list-toolbar__filters">
        <el-input v-model="searchForm.actorName" placeholder="请输入演员名称查询" aria-label="演员名称"
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
            <el-image class="cell-avatar"
                      v-if="scope.row.img" :src="scope.row.img"
                      :preview-src-list="[scope.row.img]" preview-teleported />
          </template>
        </el-table-column>
        <el-table-column label="演员名称" prop="actorName" />
        <el-table-column label="饰演角色" prop="figure" />
        <el-table-column label="演员照片" prop="picture">
          <template #default="scope">
            <el-image class="cell-avatar"
                      v-if="scope.row.picture" :src="scope.row.picture"
                      :preview-src-list="[scope.row.picture]" preview-teleported />
          </template>
        </el-table-column>
        <el-table-column label="角色评级" prop="grade">
          <template #default="scope">
            <el-tag :type="getGradeType(scope.row.grade)">{{ scope.row.grade }}</el-tag>
          </template>
        </el-table-column>
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

    <el-dialog v-model="dialogVisible" title="演职人员信息" width="500" destroy-on-close>
      <el-form ref="formRef" :rules="rules" :model="form" class="dialog-form" label-width="80px"
               status-icon @submit.prevent>
        <el-form-item label="电影名称" prop="title">
          <el-input v-model="form.title" autocomplete="off" placeholder="请输入电影名称" @keyup.enter="submit" />
        </el-form-item>
        <el-form-item label="电影图片" prop="img">
          <el-upload :action="FILE_UPLOAD_URL" :headers="uploadHeaders"
                     :on-success="handleMovieImgUpload" :on-error="handleUploadError"
                     :auto-upload="true" list-type="picture">
            <el-button type="primary">点击上传</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="演员名称" prop="actorName">
          <el-input v-model="form.actorName" autocomplete="off" placeholder="请输入演员名称" @keyup.enter="submit" />
        </el-form-item>
        <el-form-item label="饰演角色" prop="figure">
          <el-input v-model="form.figure" autocomplete="off" placeholder="请输入饰演角色名称" @keyup.enter="submit" />
        </el-form-item>
        <el-form-item label="演员照片" prop="picture">
          <el-upload :action="FILE_UPLOAD_URL" :headers="uploadHeaders"
                     :on-success="handleActorImgUpload" :on-error="handleUploadError"
                     :auto-upload="true" list-type="picture">
            <el-button type="primary">点击上传</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="所属角色" prop="grade">
          <el-select v-model="form.grade" placeholder="请选择所属角色">
            <el-option label="导演" value="导演" />
            <el-option label="主演" value="主演" />
            <el-option label="编剧" value="编剧" />
            <el-option label="二级演员" value="二级演员" />
          </el-select>
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
import { uploadHeaders, handleUploadError } from '@/utils/upload'

const crud = useCrud(API_PATHS.ACTORS)
const { dataList, total, pageNum, pageSize, searchForm, selectedIds, loading, error,
        confirmDel, confirmDelBatch, onSearch, onReset, onPageChange, onSizeChange, onSelectionChange } = crud
const { dialogVisible, formRef, form, rules, openAdd, openEdit, submit, close } = useFormDialog(crud, {
  defaultForm: { title: '', actorName: '', figure: '', picture: '', img: '', grade: '' },
  rules: {
    title: [{ required: true, message: '请输入电影名称', trigger: 'blur' }],
    actorName: [{ required: true, message: '请输入主演名称', trigger: 'blur' }],
    figure: [{ required: true, message: '请输入饰演角色名称', trigger: 'blur' }],
    grade: [{ required: true, message: '请选择角色评级', trigger: 'change' }]
  }
})

crud.load()

function handleMovieImgUpload(res) {
  if (res.code === '200') { form.img = res.data; ElMessage.success('电影图片上传成功') }
  else { ElMessage.error(res.msg || '电影图片上传失败') }
}

function handleActorImgUpload(res) {
  if (res.code === '200') { form.picture = res.data; ElMessage.success('主演照片上传成功') }
  else { ElMessage.error(res.msg || '主演照片上传失败') }
}

function getGradeType(grade) {
  switch (grade) {
    case '导演': return 'warning'
    case '主演': return 'success'
    case '编剧': return 'info'
    case '二级演员': return 'primary'
    default: return 'info'
  }
}
</script>
