<template>
  <div>
    <div class="card" style="margin-bottom: 5px">
      <el-input v-model="searchForm.orders" placeholder="请输入订单号" style="width: 300px; margin-right:10px" :prefix-icon="Search"/>
      <el-select v-model="searchForm.status" placeholder="请选择放映状态" style="width: 300px; margin-right:10px">
        <el-option label="已取票" value="已取票" />
        <el-option label="待取票" value="待取票" />
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
        <el-table-column type="expand">
          <template #default="props">
            <el-descriptions title="订单信息" :column="4" border>
              <el-descriptions-item label="电影图片">
                <el-image style="width:36px;height:36px;object-fit:cover;" :src="props.row.img"/>
              </el-descriptions-item>
              <el-descriptions-item label="订单号">{{props.row.orders}}</el-descriptions-item>
              <el-descriptions-item label="用户名称">{{props.row.userName}}</el-descriptions-item>
              <el-descriptions-item label="电影名称">{{props.row.filmName}}</el-descriptions-item>
              <el-descriptions-item label="影院名称">{{props.row.cinemaName}}</el-descriptions-item>
              <el-descriptions-item label="影厅房间">{{ props.row.roomName || getRoomName(props.row.roomId) }}</el-descriptions-item>
              <el-descriptions-item label="座位号">{{props.row.seat}}</el-descriptions-item>
              <el-descriptions-item label="预约时间">{{props.row.start}}</el-descriptions-item>
              <el-descriptions-item label="电影票数量">{{props.row.number}}</el-descriptions-item>
              <el-descriptions-item label="总费用">{{props.row.total}}</el-descriptions-item>
              <el-descriptions-item label="订单状态">
                <el-tag :type="getStatusType(props.row.status)">
                  {{ props.row.status }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </template>
        </el-table-column>
        <el-table-column label="订单号" prop="orders" show-overflow-tooltip />
        <el-table-column label="用户名称" prop="userName"/>
        <el-table-column label="电影名称" prop="filmName" show-overflow-tooltip />
        <el-table-column label="电影图片" prop="img">
          <template #default="scope">
            <el-image style="margin-top: 7px; width:36px;height:36px;border-radius:10%;object-fit:cover;align-items:center;"
                      v-if="scope.row.img"
                      :src="scope.row.img"
                      :preview-src-list="[scope.row.img]"
                      preview-teleported/>
          </template>
        </el-table-column>
        <el-table-column label="影院名称" prop="cinemaName"/>
        <el-table-column label="影厅名称">
          <template #default="prop">
            {{ prop.row.roomName || getRoomName(prop.row.roomId) }}
          </template>
        </el-table-column>
        <el-table-column label="预约时间" prop="start" show-overflow-tooltip />
        <el-table-column label="电影票数量" prop="number"/>
        <el-table-column label="总费用" prop="total"/>
        <el-table-column label="订单状态" prop="status">
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

    <div class="card" style="margin-bottom: 5px">
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

const crud = useCrud(API_PATHS.ORDERS)
const { dataList, total, pageNum, pageSize, searchForm, selectedIds,
        del, delBatch, onSearch, onReset, onPageChange, onSizeChange, onSelectionChange } = crud

const roomData = []

function loadRoom() {
  return request.get('/api/v1/rooms').then(res => {
    if (res.code === '200') { roomData.length = 0; roomData.push(...res.data) }
  })
}

function getRoomName(roomId) {
  if (!roomId) return '暂未关联影厅'
  return roomData.find(r => r.id === roomId)?.name || '暂未关联影厅'
}

function handleDel(id) {
  ElMessageBox.confirm('删除数据后无法恢复,您确认删除吗?', '删除确认', { type: 'warning' })
    .then(() => del(id)).catch()
}

function handleDelBatch() {
  if (!selectedIds.value.length) { ElMessage.warning('请选择数据'); return }
  ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 条数据吗？`, '删除确认', { type: 'warning' })
    .then(() => delBatch(selectedIds.value)).catch()
}

function getStatusType(status) {
  switch (status) {
    case '已取票': return 'warning'
    case '待取票': return 'success'
    default: return 'info'
  }
}

crud.load()
loadRoom()
</script>

<style scoped>
</style>
