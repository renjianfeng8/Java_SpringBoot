<template>
  <div class="crud-page">
    <div class="page-head">
      <h2 class="page-head__title">系统公告</h2>
      <div class="page-head__action">
        <el-button type="primary" :icon="Plus" @click="openAdd">新 增</el-button>
      </div>
    </div>

    <div class="card list-toolbar">
      <div class="list-toolbar__filters">
        <el-input v-model="searchForm.title" placeholder="请输入公告标题" aria-label="公告标题"
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
        <el-table-column label="公告标题" prop="title" />
        <el-table-column label="公告内容" prop="content" show-overflow-tooltip />
        <el-table-column label="发布时间" prop="time" />
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

    <el-dialog v-model="dialogVisible" title="电影公告信息" width="500" destroy-on-close>
      <el-form ref="formRef" :rules="rules" :model="form" class="dialog-form" label-width="80px"
               status-icon @submit.prevent>
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" autocomplete="off" placeholder="请输入公告标题" @keyup.enter="submit" />
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
import { Delete, Edit, Plus, Search } from '@element-plus/icons-vue'
import { useCrud } from '@/composables/useCrud'
import { useFormDialog } from '@/composables/useFormDialog'
import { API_PATHS } from '@/constants'

const crud = useCrud(API_PATHS.NOTICES)
const { dataList, total, pageNum, pageSize, searchForm, selectedIds, loading, error,
        confirmDel, confirmDelBatch, onSearch, onReset, onPageChange, onSizeChange, onSelectionChange } = crud
const { dialogVisible, formRef, form, rules, openAdd, openEdit, submit, close } = useFormDialog(crud, {
  defaultForm: { title: '', content: '', time: new Date().toISOString().slice(0, 19).replace('T', ' ') },
  rules: {
    title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
    content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
    time: [{ required: true, message: '请选择发布时间', trigger: 'change' }]
  }
})

crud.load()
</script>
