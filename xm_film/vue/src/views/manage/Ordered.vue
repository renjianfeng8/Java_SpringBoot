<template>
  <div class="crud-page">
    <div class="page-head">
      <h2 class="page-head__title">购票订单</h2>
    </div>

    <div class="card list-toolbar">
      <div class="list-toolbar__filters">
        <el-input v-model="searchForm.orders" placeholder="请输入订单号" aria-label="订单号"
                  class="search-input" :prefix-icon="Search" @keyup.enter="onSearch"/>
        <el-select v-model="searchForm.status" placeholder="请选择订单状态" aria-label="订单状态" class="field-md">
          <el-option v-for="status in ORDER_STATUS_OPTIONS" :key="status" :label="status" :value="status" />
        </el-select>
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
        <!-- 不可删除的订单（待支付/待取票/已取票）连勾选都不允许，批量删除自然不会带上它们 -->
        <el-table-column type="selection" width="55" :selectable="(row) => isOrderDeletable(row.status)"/>
        <el-table-column type="expand">
          <template #default="props">
            <el-descriptions title="订单信息" :column="4" border>
              <el-descriptions-item label="电影图片">
                <el-image class="cell-thumb" :src="props.row.img"/>
              </el-descriptions-item>
              <el-descriptions-item label="订单号">{{props.row.orders}}</el-descriptions-item>
              <el-descriptions-item label="用户名称">{{props.row.userName}}</el-descriptions-item>
              <el-descriptions-item label="电影名称">{{props.row.filmName}}</el-descriptions-item>
              <el-descriptions-item label="影院名称">{{props.row.cinemaName}}</el-descriptions-item>
              <el-descriptions-item label="影厅房间">{{ props.row.roomName || getRoomName(props.row.roomId) }}</el-descriptions-item>
              <el-descriptions-item label="座位号">{{props.row.seat}}</el-descriptions-item>
              <el-descriptions-item label="预约时间">{{props.row.start}}</el-descriptions-item>
              <el-descriptions-item label="电影票数量">{{props.row.number}}</el-descriptions-item>
              <el-descriptions-item label="单价（元）">
                {{ props.row.unitPrice != null ? `¥${props.row.unitPrice}` : '—' }}
              </el-descriptions-item>
              <el-descriptions-item label="总费用">{{props.row.total}}</el-descriptions-item>
              <el-descriptions-item label="订单状态">
                <el-tag :type="getStatusType(props.row.status)">
                  {{ props.row.status }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="支付时间">{{ props.row.payTime || '—' }}</el-descriptions-item>
              <el-descriptions-item label="实付金额">
                {{ props.row.payAmount != null ? `¥${props.row.payAmount}` : '—' }}
              </el-descriptions-item>
              <el-descriptions-item label="退票时间">{{ props.row.refundTime || '—' }}</el-descriptions-item>
              <el-descriptions-item label="退款金额">
                {{ props.row.refundAmount != null ? `¥${props.row.refundAmount}` : '—' }}
              </el-descriptions-item>
            </el-descriptions>
          </template>
        </el-table-column>
        <el-table-column label="订单号" prop="orders" show-overflow-tooltip />
        <el-table-column label="用户名称" prop="userName"/>
        <el-table-column label="电影名称" prop="filmName" show-overflow-tooltip />
        <el-table-column label="电影图片" prop="img">
          <template #default="scope">
            <el-image class="cell-thumb"
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
        <el-table-column label="单价" prop="unitPrice"/>
        <el-table-column label="总费用" prop="total"/>
        <el-table-column label="订单状态" prop="status">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="scope">
            <!-- 管理员刻意没有取票入口：取票是影院柜台的物理交付动作，只有放映该场次的影院能
                 如实断言，故后端 pickupOrder 只放行 CINEMA。服务端才是权限落点，这里删按钮
                 不是"以藏代守"；用户侧的取票走前台取票大厅凭码核销。 -->
            <!-- 只有终态废单可删除，与后端删除守卫同构 -->
            <el-button v-if="isOrderDeletable(scope.row.status)" class="row-action" link :icon="Delete"
                       aria-label="删除" @click="confirmDel(scope.row.id)" type="danger"></el-button>
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
  </div>
</template>

<script setup>
import { watch } from 'vue'
import { useRoute } from 'vue-router'
import { Delete, Search } from '@element-plus/icons-vue'
import { useCrud } from '@/composables/useCrud'
import { API_PATHS, ORDER_STATUS_OPTIONS, getOrderStatusType as getStatusType, isOrderDeletable } from '@/constants'
import request from '@/utils/request'

const crud = useCrud(API_PATHS.ORDERS)
const { dataList, total, pageNum, pageSize, searchForm, selectedIds, loading, error,
        confirmDel, confirmDelBatch, onSearch, onReset, onPageChange, onSizeChange, onSelectionChange } = crud

const roomData = []

function loadRoom() {
  return request.get(API_PATHS.ROOMS).then(res => {
    if (res.code === '200') { roomData.length = 0; roomData.push(...res.data) }
  })
}

function getRoomName(roomId) {
  if (!roomId) return '暂未关联影厅'
  return roomData.find(r => r.id === roomId)?.name || '暂未关联影厅'
}

const route = useRoute()

/* 同 Cinema.vue：待办卡带 ?status=待取票 进来。watch 而非顶层读取。 */
watch(() => route.query.status, (status) => {
  if (status) searchForm.status = status
  onSearch()
}, { immediate: true })

loadRoom()
</script>
