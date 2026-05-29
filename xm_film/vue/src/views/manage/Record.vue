<template>
  <div>
    <div class="card" style="margin-bottom: 5px">
      <el-input v-model="searchForm.title"  placeholder="请输入电影名称查询" style="width: 300px; margin-right:10px" :prefix-icon="Search"/>
      <el-input  v-model="searchForm.start"  placeholder="按放映日期查询 (YYYY-MM-DD)" style="width: 300px; margin-right:10px" :prefix-icon="Search"/>
      <el-select v-model="searchForm.status" placeholder="请选择放映状态" style="width: 300px; margin-right:10px">
        <el-option label="待上映" value="待上映" />
        <el-option label="已上映" value="已上映" />
        <el-option label="停止上映" value="停止上映" />
      </el-select>
      <el-button type="primary" @click="onSearch">查 询</el-button>
      <el-button type="warning" @click="onReset">重 置</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-button type="danger" @click="handleDelBatch">批量删除</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="dataList" @selection-change="onSelectionChange">
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
            <el-button style="font-size: 18px" link :icon="Delete" @click="() => handleDel(scope.row.id)" type="danger"></el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card" style="  margin-bottom: 5px">
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
import { API_PATHS } from '@/constants'
import request from '@/utils/request'

// 仅使用 useCrud 的响应式状态（dataList 需自定义 load 做名称映射）
const crud = useCrud(API_PATHS.RECORDS)
const { dataList, total, pageNum, pageSize, searchForm, selectedIds, onSelectionChange } = crud

const cinemaData = []
const roomData = []

function loadCinema() {
  return request.get('/api/v1/cinemas').then(res => {
    if (res.code === '200') { cinemaData.length = 0; cinemaData.push(...res.data) }
  })
}

function loadRoom() {
  return request.get('/api/v1/rooms').then(res => {
    if (res.code === '200') { roomData.length = 0; roomData.push(...res.data) }
  })
}

// 自定义 load：映射影院/影厅名称
function load() {
  const params = { pageNum: pageNum.value, pageSize: pageSize.value, ...searchForm }
  request.get('/api/v1/records/page', { params }).then(res => {
    if (res && res.data) {
      dataList.value = (res.data.list || []).map(record => ({
        ...record,
        cinemaName: cinemaData.find(c => c.id === record.cinemaId)?.name,
        roomName: roomData.find(r => r.id === record.roomId)?.name
      }))
      total.value = res.data.total || 0
    }
  }).catch(() => ElMessage.error('加载数据失败，请重试'))
}

function onSearch() { pageNum.value = 1; load() }
function onReset() { Object.keys(searchForm).forEach(k => { searchForm[k] = undefined }); pageNum.value = 1; load() }
function onPageChange(p) { pageNum.value = p; load() }
function onSizeChange(s) { pageSize.value = s; pageNum.value = 1; load() }

function handleDel(id) {
  ElMessageBox.confirm('删除数据后无法恢复,您确认删除吗?', '删除确认', { type: 'warning' })
    .then(() => request.delete(`/api/v1/records/${id}`))
    .then(res => { if (res.code === '200') { ElMessage.success('操作成功'); load() } })
    .catch(() => {})
}

function handleDelBatch() {
  if (!selectedIds.value.length) { ElMessage.warning('请选择数据'); return }
  ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 条数据吗？删除后无法恢复`, '删除确认', { type: 'warning' })
    .then(() => request.delete('/api/v1/records/batch', { data: selectedIds.value }))
    .then(res => { if (res.code === '200') { ElMessage.success('操作成功'); load() } })
    .catch(() => {})
}

// 初始加载
Promise.all([loadCinema(), loadRoom()]).then(() => load())

function getStatusType(status) {
  switch (status) {
    case '待上映': return 'warning'
    case '已上映': return 'success'
    case '停止上映': return 'danger'
    default: return 'info'
  }
}
</script>

<style scoped>
</style>
