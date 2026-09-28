<template>
  <div>
    <div class="card page-card">
      <el-input v-model="searchForm.name" placeholder="请输入影厅名称" class="search-input" :prefix-icon="Search" />
      <el-button type="primary" @click="onSearch">查 询</el-button>
      <el-button type="warning" @click="onReset">重 置</el-button>
    </div>
    <div class="card page-card">
      <el-button type="danger" @click="handleDelBatch">批量删除</el-button>
    </div>
    <div class="card page-card">
      <el-table v-loading="loading" stripe :data="dataList" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column label="影院名称" prop="title" />
        <el-table-column label="影厅名称" prop="name" />
        <el-table-column label="座位规模" width="120">
          <template #default="scope">{{ scope.row.seatRows }} 排 × {{ scope.row.seatCols }} 座</template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button class="row-action" link :icon="Delete" @click="handleDel(scope.row.id)" type="danger" />
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="card page-card">
      <el-pagination @size-change="onSizeChange" @current-change="onPageChange" v-model:current-page="pageNum" v-model:page-size="pageSize" :page-sizes="[5, 10, 15, 20]" background layout="total, sizes, prev, pager, next, jumper" :total="total" />
    </div>
  </div>
</template>

<script setup>
import { Delete, Search } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import { API_PATHS } from '@/constants'

const { dataList, total, pageNum, pageSize, searchForm, selectedIds, loading, load, del, delBatch, onSearch, onReset, onPageChange, onSizeChange, onSelectionChange } = useCrud(API_PATHS.ROOMS)

load()

function handleDel(id) {
  ElMessageBox.confirm('删除数据后无法恢复，您确认删除吗?', '删除确认', { type: 'warning' }).then(() => del(id)).catch()
}

function handleDelBatch() {
  if (!selectedIds.value.length) return
  ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 条数据吗？`, '删除确认', { type: 'warning' }).then(() => delBatch(selectedIds.value)).catch()
}
</script>

<style scoped>
.card {
  padding: var(--space-12);
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
}
</style>
