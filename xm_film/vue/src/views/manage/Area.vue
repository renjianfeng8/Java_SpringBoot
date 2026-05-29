<template>
  <div>
    <div class="card mb-2">
      <el-input v-model="crud.searchForm.title" placeholder="请输入区域名称" class="mr-2 w-72" :prefix-icon="Search" />
      <el-button type="primary" @click="crud.onSearch">查 询</el-button>
      <el-button type="warning" @click="crud.onReset">重 置</el-button>
    </div>

    <div class="card mb-2">
      <el-button type="info" @click="dialog.openAdd">新 增</el-button>
      <el-button type="danger" @click="handleDelBatch">批量删除</el-button>
    </div>

    <div class="card mb-2">
      <el-table stripe :data="crud.dataList.value" @selection-change="crud.onSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column label="区域名称" prop="title" />
        <el-table-column label="操作">
          <template #default="scope">
            <el-button style="font-size: 18px" link :icon="Edit" @click="dialog.openEdit(scope.row)" type="primary" />
            <el-button style="font-size: 18px" link :icon="Delete" @click="handleDel(scope.row.id)" type="danger" />
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card mb-2">
      <el-pagination
          @size-change="crud.onSizeChange"
          @current-change="crud.onPageChange"
          v-model:current-page="crud.pageNum.value"
          v-model:page-size="crud.pageSize.value"
          :page-sizes="[5, 10, 15, 20]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="crud.total.value"
      />
    </div>

    <el-dialog v-model="dialog.dialogVisible.value" title="电影区域信息" width="500" destroy-on-close>
      <el-form ref="dialog.formRef" :rules="rules" :model="dialog.form" class="pt-5 pr-12" label-width="80px">
        <el-form-item label="区域名称" prop="title">
          <el-input v-model="dialog.form.title" autocomplete="off" placeholder="请输入区域名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialog.close">取 消</el-button>
          <el-button type="primary" @click="dialog.submit">保 存</el-button>
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

const crud = useCrud(API_PATHS.AREAS)
const dialog = useFormDialog(crud, {
  defaultForm: { title: '' },
  rules: { title: [{ required: true, message: '请输入电影区域', trigger: 'blur' }] }
})
const { rules } = dialog

crud.load()

function handleDel(id) {
  ElMessageBox.confirm('删除数据后无法恢复，您确认删除吗?', '删除确认', { type: 'warning' })
    .then(() => crud.del(id)).catch()
}

function handleDelBatch() {
  if (!crud.selectedIds.value.length) return
  const ids = crud.selectedIds.value
  ElMessageBox.confirm(`确定删除选中的 ${ids.length} 条数据吗？删除后无法恢复`, '删除确认', { type: 'warning' })
    .then(() => crud.delBatch(ids)).catch()
}
</script>

<style scoped>
.mb-2 { margin-bottom: 8px; }
.mr-2 { margin-right: 8px; }
.w-72 { width: 18rem; }
.pt-5 { padding-top: 1.25rem; }
.pr-12 { padding-right: 3rem; }
</style>
