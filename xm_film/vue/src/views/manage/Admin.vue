<template>
  <div class="crud-page">
    <div class="page-head">
      <h2 class="page-head__title">管理员信息</h2>
      <div class="page-head__action">
        <el-button type="primary" :icon="Plus" @click="openAdd">新 增</el-button>
      </div>
    </div>

    <div class="card list-toolbar">
      <div class="list-toolbar__filters">
        <el-input v-model="searchForm.name" placeholder="请输入姓名查询" aria-label="姓名"
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
        <el-table-column label="账号" prop="username" />
        <el-table-column label="头像" prop="avatar">
          <template #default="scope">
            <el-image class="cell-avatar" v-if="scope.row.avatar" :src="scope.row.avatar" :preview-src-list="[scope.row.avatar]" preview-teleported />
          </template>
        </el-table-column>
        <el-table-column label="姓名" prop="name" />
        <el-table-column label="电话" prop="phone" />
        <el-table-column label="邮箱" prop="email" show-overflow-tooltip />
        <el-table-column label="角色" prop="role">
          <template #default="scope">
            <el-tag :type="getRoleType(scope.row.role)">{{ scope.row.role }}</el-tag>
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

    <el-dialog v-model="dialogVisible" title="管理员信息" width="500" destroy-on-close>
      <el-form ref="formRef" :rules="rules" :model="form" class="dialog-form" label-width="80px"
               status-icon @submit.prevent>
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" autocomplete="off" placeholder="请输入账号" @keyup.enter="submit" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="form.password" show-password autocomplete="off" placeholder="请输入初始密码" @keyup.enter="submit" />
        </el-form-item>
        <el-form-item label="头像" prop="avatar">
          <el-upload :action="FILE_UPLOAD_URL" :headers="uploadHeaders"
                     :on-success="handleFileUpload" :on-error="handleUploadError"
                     :auto-upload="true" list-type="picture">
            <el-button type="primary">点击上传</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" autocomplete="off" placeholder="请输入姓名" @keyup.enter="submit" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" autocomplete="off" placeholder="请输入电话" @keyup.enter="submit" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" autocomplete="off" placeholder="请输入邮箱" @keyup.enter="submit" />
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
import { API_PATHS, FILE_UPLOAD_URL, getRoleType } from '@/constants'
import { uploadHeaders, handleUploadError } from '@/utils/upload'

const crud = useCrud(API_PATHS.ADMINS)
const { dataList, total, pageNum, pageSize, searchForm, selectedIds, loading, error,
        confirmDel, confirmDelBatch, onSearch, onReset, onPageChange, onSizeChange, onSelectionChange } = crud
const { dialogVisible, isEdit, formRef, form, rules, openAdd, openEdit, submit, close } = useFormDialog(crud, {
  defaultForm: { username: '', password: '', avatar: '', name: '', phone: '', email: '' },
  rules: {
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    // 只在新增态生效：编辑态的密码框被 v-if 卸载，AdminService.update 又显式置空 password
    password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }],
    name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
  }
})

crud.load()

function handleFileUpload(res) {
  if (res.code === '200') { form.avatar = res.data; ElMessage.success('头像上传成功') }
  else { ElMessage.error(res.msg || '头像上传失败') }
}
</script>
