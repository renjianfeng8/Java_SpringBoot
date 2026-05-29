<template>
  <div>
    <div class="card" style="margin-bottom: 5px">
      <el-input v-model="searchForm.name" placeholder="请输入姓名查询" style="width: 300px; margin-right:10px" :prefix-icon="Search" />
      <el-button type="primary" @click="onSearch">查 询</el-button>
      <el-button type="warning" @click="onReset">重 置</el-button>
    </div>
    <div class="card" style="margin-bottom: 5px">
      <el-button type="info" @click="openAdd">新 增</el-button>
      <el-button type="danger" @click="handleDelBatch">批量删除</el-button>
    </div>
    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="dataList" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column label="账号" prop="username" />
        <el-table-column label="头像" prop="avatar">
          <template #default="scope">
            <el-image style="width:40px;height:40px;border-radius:50%;object-fit:cover;margin-top:5px" v-if="scope.row.avatar" :src="scope.row.avatar" :preview-src-list="[scope.row.avatar]" preview-teleported />
          </template>
        </el-table-column>
        <el-table-column label="姓名" prop="name" />
        <el-table-column label="电话" prop="phone" />
        <el-table-column label="邮箱" prop="email" show-overflow-tooltip />
        <el-table-column label="角色" prop="role">
          <template #default="scope">
            <el-tag :type="scope.row.role === 'ADMIN' ? 'warning' : scope.row.role === 'CINEMA' ? 'danger' : 'success'">{{ scope.row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button style="font-size: 18px" link :icon="Edit" @click="openEdit(scope.row)" type="primary" />
            <el-button style="font-size: 18px" link :icon="Delete" @click="handleDel(scope.row.id)" type="danger" />
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="card" style="margin-bottom: 5px">
      <el-pagination @size-change="onSizeChange" @current-change="onPageChange" v-model:current-page="pageNum" v-model:page-size="pageSize" :page-sizes="[5, 10, 15, 20]" background layout="total, sizes, prev, pager, next, jumper" :total="total" />
    </div>
    <el-dialog v-model="dialogVisible" title="管理员信息" width="500" destroy-on-close>
      <el-form ref="formRef" :rules="rules" :model="form" style="padding-right:50px;padding-top:20px" label-width="80px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" autocomplete="off" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" autocomplete="off" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" autocomplete="off" placeholder="请输入电话" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" autocomplete="off" placeholder="请输入邮箱" />
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

const crud = useCrud(API_PATHS.ADMINS)
const { dataList, total, pageNum, pageSize, searchForm, selectedIds, del, delBatch, onSearch, onReset, onPageChange, onSizeChange, onSelectionChange } = crud
const { dialogVisible, formRef, form, rules, openAdd, openEdit, submit, close } = useFormDialog(crud, {
  defaultForm: { username: '', name: '', phone: '', email: '' },
  rules: {
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
  }
})

crud.load()

function handleDel(id) {
  ElMessageBox.confirm('删除数据后无法恢复，您确认删除吗?', '删除确认', { type: 'warning' }).then(() => del(id)).catch()
}

function handleDelBatch() {
  if (!selectedIds.length) return
  ElMessageBox.confirm(`确定删除选中的 ${selectedIds.length} 条数据吗？`, '删除确认', { type: 'warning' }).then(() => delBatch(selectedIds)).catch()
}
</script>

<style scoped>
.card { box-shadow: 0 1px 4px rgba(0,21,41,.08); border-radius: 4px; padding: 10px; background-color: #fff; }
</style>
