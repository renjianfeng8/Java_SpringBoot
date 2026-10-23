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
        <el-select v-model="searchForm.filmId" placeholder="电影" aria-label="电影" class="field-md" clearable filterable>
          <el-option v-for="film in filmOptions" :key="film.id" :label="film.title" :value="film.id" />
        </el-select>
        <el-select v-model="searchForm.cinemaId" placeholder="影院" aria-label="影院" class="field-md" clearable filterable>
          <el-option v-for="cinema in cinemaOptions" :key="cinema.id" :label="cinema.name" :value="cinema.id" />
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
      <el-table ref="tableRef" row-key="id" v-loading="loading" stripe size="small" :data="dataList" @selection-change="onSelectionChange">
        <!-- 不可删除的订单（待支付/待取票/已取票）连勾选都不允许，批量删除自然不会带上它们 -->
        <el-table-column type="selection" width="55" reserve-selection :selectable="(row) => isOrderDeletable(row.status)"/>
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
              <el-descriptions-item label="影厅房间">{{ props.row.roomName }}</el-descriptions-item>
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
            {{ prop.row.roomName }}
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
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <!-- 管理员刻意没有取票入口：取票是影院柜台的物理交付动作，只有放映该场次的影院能
                 如实断言，故后端 pickupOrder 只放行 CINEMA。服务端才是权限落点，这里删按钮
                 不是"以藏代守"；用户侧的取票走前台取票大厅凭码核销。 -->
            <div class="row-actions">
              <el-button v-if="scope.row.status === '待支付'" link type="warning"
                         @click="cancelOrder(scope.row)">取消</el-button>
              <el-button v-if="scope.row.status === '待取票'" link type="danger"
                         @click="refundOrder(scope.row)">退票</el-button>
              <!-- 只有终态废单可删除，与后端删除守卫同构 -->
              <el-button v-if="isOrderDeletable(scope.row.status)" class="row-action" link :icon="Delete"
                         aria-label="删除" @click="confirmDel(scope.row.id)" type="danger"></el-button>
            </div>
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
            :page-sizes="[5, 10, 15, 20, 50, 100]"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Delete, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import request from '@/utils/request'
import {
  isSuccess, API_PATHS, ORDER_API, ORDER_STATUS_OPTIONS, getOrderStatusType as getStatusType, isOrderDeletable
} from '@/constants'

const crud = useCrud(API_PATHS.ORDERS)
const { dataList, total, pageNum, pageSize, searchForm, selectedIds, tableRef, loading, error,
        confirmDel, confirmDelBatch, onSearch, onReset, onPageChange, onSizeChange, onSelectionChange } = crud

// 筛选下拉的候选值。两处列表都不大，进页时一次取回，避免远程搜索的额外交互。
const filmOptions = ref([])
const cinemaOptions = ref([])
request.get(API_PATHS.FILMS).then(res => {
  if (isSuccess(res)) filmOptions.value = res.data || []
}).catch(() => {})
request.get(API_PATHS.CINEMAS).then(res => {
  if (isSuccess(res)) cinemaOptions.value = res.data || []
}).catch(() => {})

const route = useRoute()

/* 同 Cinema.vue：待办卡带 ?status=待取票 进来。watch 而非顶层读取。 */
watch(() => route.query.status, (status) => {
  if (status) searchForm.status = status
  onSearch()
}, { immediate: true })

// 管理端操作沿用后端既有状态机：待支付才能取消，待取票才能退票。
// 退票的「开场前 60 分钟」截止由后端统一判断，管理员也不绕过 —— 前端只把失败原因原样显示。
function cancelOrder(row) {
  ElMessageBox.confirm(`确定取消订单 ${row.orders} 吗？取消后座位立即释放。`, '取消订单', { type: 'warning' })
    .then(async () => {
      try {
        const res = await request.put(ORDER_API.CANCEL(row.id))
        if (isSuccess(res)) {
          ElMessage.success('订单已取消')
          await crud.load()
          crud.clearSelection()
        } else {
          ElMessage.error(res.msg || '取消失败')
        }
      } catch (e) {
        // request.js 已统一提示网络/HTTP 错误
      }
    })
    .catch(() => {})
}

function refundOrder(row) {
  ElMessageBox.confirm(
    `确定为订单 ${row.orders} 退票吗？退款 ¥${row.total ?? 0} 将退回用户余额。`,
    '退票确认', { type: 'warning' }
  ).then(async () => {
    try {
      const res = await request.put(ORDER_API.REFUND(row.id))
      if (isSuccess(res)) {
        ElMessage.success('退票成功，款项已退回用户余额')
        await crud.load()
        crud.clearSelection()
      } else {
        ElMessage.error(res.msg || '退票失败')
      }
    } catch (e) {
      // request.js 已统一提示网络/HTTP 错误
    }
  }).catch(() => {})
}
</script>
