<template>
  <div>
    <div class="card mb-2">
      <el-input v-model="searchForm.title" placeholder="请输入公告标题" class="mr-2 w-72" :prefix-icon="Search" />
      <el-button type="primary" @click="onSearch">查 询</el-button>
      <el-button type="warning" @click="onReset">重 置</el-button>
    </div>

    <div class="card mb-2">
      <el-button type="info" @click="openAdd">新 增</el-button>
      <el-button type="danger" @click="handleDelBatch">批量删除</el-button>
    </div>

    <div class="card mb-2">
      <el-table stripe :data="dataList" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column label="公告标题" prop="title" />
        <el-table-column label="公告内容" prop="content" show-overflow-tooltip />
        <el-table-column label="发布时间" prop="time" />
        <el-table-column label="操作">
          <template #default="scope">
            <el-button style="font-size: 18px" link :icon="Edit" @click="openEdit(scope.row)" type="primary" />
            <el-button style="font-size: 18px" link :icon="Delete" @click="handleDel(scope.row.id)" type="danger" />
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card mb-2">
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

    <el-dialog v-model="dialogVisible" title="电影公告信息" width="500" destroy-on-close>
      <el-form ref="formRef" :rules="rules" :model="form" class="pt-5 pr-12" label-width="80px">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" autocomplete="off" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="4" autocomplete="off" placeholder="请输入公告内容" />
        </el-form-item>
        <el-form-item label="发布时间" prop="time">
          <el-date-picker v-model="form.time" type="datetime" placeholder="选择发布时间" value-format="YYYY-MM-DD HH:mm:ss" />
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
import { Delete, Edit, Search } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import { useFormDialog } from '@/composables/useFormDialog'
import { API_PATHS } from '@/constants'

const crud = useCrud(API_PATHS.NOTICES)
const { dataList, total, pageNum, pageSize, searchForm, selectedIds,
        del, delBatch, onSearch, onReset, onPageChange, onSizeChange, onSelectionChange } = crud
const { dialogVisible, formRef, form, rules, openAdd, openEdit, submit, close } = useFormDialog(crud, {
  defaultForm: { title: '', content: '', time: new Date().toISOString().slice(0, 19).replace('T', ' ') },
  rules: {
    title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
    content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
    time: [{ required: true, message: '请选择发布时间', trigger: 'change' }]
  }
})

crud.load()

function handleDel(id) {
  ElMessageBox.confirm('删除数据后无法恢复，您确认删除吗?', '删除确认', { type: 'warning' })
    .then(() => del(id)).catch()
}

function handleDelBatch() {
  if (!selectedIds.value.length) return
  ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 条数据吗？`, '删除确认', { type: 'warning' })
    .then(() => delBatch(selectedIds.value)).catch()
}
</script>

<style scoped>
.card { box-shadow: 0 1px 4px rgba(0,21,41,.08); border-radius: 4px; padding: 10px; background-color: #fff; }
.mb-2 { margin-bottom: 8px; }
.mr-2 { margin-right: 8px; }
.w-72 { width: 18rem; }
.pt-5 { padding-top: 1.25rem; }
.pr-12 { padding-right: 3rem; }
</style>
