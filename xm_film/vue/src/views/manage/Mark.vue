<template>
  <div>
    <div class="card" style="margin-bottom: 5px">
      <el-input v-model="searchForm.filmName" placeholder="请输入电影名称查询" style="width: 300px; margin-right:10px" :prefix-icon="Search" />
      <el-button type="primary" @click="onSearch">查 询</el-button>
      <el-button type="warning" @click="onReset">重 置</el-button>
    </div>
    <div class="card" style="margin-bottom: 5px">
      <el-button type="danger" @click="handleDelBatch">批量删除</el-button>
    </div>
    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="dataList" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column label="用户名称" prop="userName" />
        <el-table-column label="电影名称" prop="filmName" show-overflow-tooltip />
        <el-table-column label="电影图片" prop="img">
          <template #default="scope">
            <el-image style="margin-top:7px;width:36px;height:36px;border-radius:10%;object-fit:cover" v-if="scope.row.img" :src="scope.row.img" :preview-src-list="[scope.row.img]" preview-teleported />
          </template>
        </el-table-column>
        <el-table-column label="用户评分" prop="mark" />
        <el-table-column label="操作">
          <template #default="scope">
            <el-button style="font-size: 18px" link :icon="Delete" @click="() => handleDel(scope.row.id)" type="danger" />
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="card" style="margin-bottom: 5px">
      <el-pagination @size-change="onSizeChange" @current-change="onPageChange" v-model:current-page="pageNum" v-model:page-size="pageSize" :page-sizes="[5, 10, 15, 20]" background layout="total, sizes, prev, pager, next, jumper" :total="total" />
    </div>
  </div>
</template>

<script setup>
import { Delete, Search } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import { API_PATHS } from '@/constants'

const { dataList, total, pageNum, pageSize, searchForm, selectedIds, del, delBatch, onSearch, onReset, onPageChange, onSizeChange, onSelectionChange } = useCrud(API_PATHS.MARKS)

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
