<template>
  <div>
    <div class="card page-card">
      <el-input v-model="searchForm.title"  placeholder="请输入电影名称查询" class="search-input" :prefix-icon="Search"/>
      <el-input  v-model="searchForm.start"  placeholder="按放映日期查询 (YYYY-MM-DD)" class="search-input" :prefix-icon="Search"/>
      <el-select v-model="searchForm.status" placeholder="请选择放映状态" class="search-input">
        <el-option label="正常" value="正常" />
        <el-option label="停售" value="停售" />
      </el-select>
      <el-button type="primary" @click="onSearch">查 询</el-button>
      <el-button type="warning" @click="onReset">重 置</el-button>
    </div>

    <div class="card page-card">
      <el-button type="danger" @click="handleDelBatch">批量删除</el-button>
    </div>

    <div class="card page-card">
      <el-table v-loading="loading" stripe :data="dataList" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="55"/>
        <el-table-column label="影院名称" prop="cinemaName"/>
        <el-table-column label="影厅名称" prop="roomName"/>
        <el-table-column label="电影名称" prop="title"/>
        <el-table-column label="放映时间" prop="start" />
        <el-table-column label="电影票价 (元)" prop="price" />
        <el-table-column label="放映状态" prop="status">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button class="row-action" link :icon="Delete" @click="() => handleDel(scope.row.id)" type="danger"></el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card page-card">
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
</template>

<script setup>
import { Delete, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import { API_PATHS, apiBatch, apiById, apiPage, getRecordStatusType as getStatusType } from '@/constants'
import request from '@/utils/request'

// 仅使用 useCrud 的响应式状态（后端 selectAll 已 JOIN 出 cinemaName / roomName）
const crud = useCrud(API_PATHS.RECORDS)
const { dataList, total, pageNum, pageSize, searchForm, selectedIds, loading, onSelectionChange } = crud

function load() {
  const params = { pageNum: pageNum.value, pageSize: pageSize.value, ...searchForm }
  // 本页自带 load()，需自行驱动 useCrud 暴露的 loading，否则表格的加载态永远不亮
  loading.value = true
  request.get(apiPage(API_PATHS.RECORDS), { params }).then(res => {
    if (res && res.data) {
      dataList.value = res.data.list || []
      total.value = res.data.total || 0
    }
  }).catch(() => ElMessage.error('加载数据失败，请重试'))
    .finally(() => { loading.value = false })
}

function onSearch() { pageNum.value = 1; load() }
function onReset() { Object.keys(searchForm).forEach(k => { searchForm[k] = undefined }); pageNum.value = 1; load() }
function onPageChange(p) { pageNum.value = p; load() }
function onSizeChange(s) { pageSize.value = s; pageNum.value = 1; load() }

function handleDel(id) {
  ElMessageBox.confirm('删除数据后无法恢复,您确认删除吗?', '删除确认', { type: 'warning' })
    .then(() => request.delete(apiById(API_PATHS.RECORDS, id)))
    .then(res => { if (res.code === '200') { ElMessage.success('操作成功'); load() } })
    .catch(() => {})
}

function handleDelBatch() {
  if (!selectedIds.value.length) { ElMessage.warning('请选择数据'); return }
  ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 条数据吗？删除后无法恢复`, '删除确认', { type: 'warning' })
    .then(() => request.delete(apiBatch(API_PATHS.RECORDS), { data: selectedIds.value }))
    .then(res => { if (res.code === '200') { ElMessage.success('操作成功'); load() } })
    .catch(() => {})
}

// 初始加载
load()

</script>

<style scoped>
</style>
