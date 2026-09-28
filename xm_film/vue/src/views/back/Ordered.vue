<template>
  <div>
    <div class="card page-card">
      <el-input v-model="data.orders" placeholder="请输入订单号" class="search-input" :prefix-icon="Search"/>
      <el-select v-model="data.status" placeholder="请选择订单状态" class="search-input">
        <el-option v-for="status in ORDER_STATUS_OPTIONS" :key="status" :label="status" :value="status" />
      </el-select>
      <el-button type="primary" @click="load">查 询</el-button>
      <el-button type="warning" @click="reset">重 置</el-button>
    </div>

    <div class="card page-card">
      <el-button type="danger" @click="delBatch">批量删除</el-button>
    </div>

    <div class="card page-card">
      <el-table stripe :data="data.tableData" @selection-change="handleSelectionChange">
        <!-- 不可删除的订单（待支付/待取票/已取票）连勾选都不允许，批量删除自然不会带上它们 -->
        <el-table-column type="selection" width="55" :selectable="(row) => isOrderDeletable(row.status)"/>
        <el-table-column type="expand">
          <template #default="props">

            <el-descriptions title="订单信息" :column="4" border>
              <el-descriptions-item label="电影图片">
                <el-image class="cell-thumb"
                          :src="props.row.img"/>
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
        <el-table-column label="操作">
          <template #default="scope">
            <el-button v-if="scope.row.status === '待取票'" link type="primary"
                       @click="() => pickupOrder(scope.row)">取票</el-button>
            <!-- 只有终态废单可删除，与后端删除守卫同构 -->
            <el-button v-if="isOrderDeletable(scope.row.status)" class="row-action" link :icon="Delete"
                       @click="() => del(scope.row.id)" type="danger"></el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card page-card">
      <el-pagination
          @size-change="load"
          @current-change="load"
          v-model:current-page="data.pageNumber"
          v-model:page-size="data.pageSize"
          :page-sizes="[5, 10, 15, 20]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="data.total"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from "vue";
import { Delete, Search } from "@element-plus/icons-vue";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox } from "element-plus";
import { API_PATHS, ORDER_API, ORDER_STATUS_OPTIONS, getOrderStatusType as getStatusType, apiBatch, apiById, apiPage, isOrderDeletable } from "@/constants";


interface Ordered {
  id?: number;  // 假设这个id同时代表影厅关联ID
  orders?: string;
  userId?: number;
  filmId?: number;
  img?: string;
  cinemaId?: number;
  appointment?: string;
  total?: string;
  unitPrice?: number;
  number?: number;
  status?: string;
  start?: string;
  seat?: string;
}

interface RoomData {
  id: number;
  name: string;
}


const data = reactive({
  tableData: [] as Ordered[],
  pageNumber: 1,
  pageSize: 10,
  total: 0,
  ids: [] as number[],
  RoomData: [] as RoomData[],
  orders: null,
  status: undefined
});


const loadRoom = () => {
  return request.get(API_PATHS.ROOMS).then(res => {
    if(res.code === '200') {
      data.RoomData = res.data;
    } else {
      ElMessage.error(res.msg)
    }
  }).catch(error => {
    console.error('获取影厅数据失败:', error);
    ElMessage.error('获取影厅数据失败');
  })
}

const load = () => {
  return request.get(apiPage(API_PATHS.ORDERS), {
    params: {
      pageNum: data.pageNumber,
      pageSize: data.pageSize,
      orders: data.orders,
      status: data.status
    }
  }).then(res => {
    if (res && res.data) {
      data.tableData = res.data.list || [];
      data.total = res.data.total || 0;
    }
  }).catch(error => {
    console.error('加载数据失败:', error);
    ElMessage.error('加载数据失败，请重试');
  });
}

const getRoomName = (roomId?: number) => {
  if (!roomId) return '无ID';
  const room = data.RoomData.find(room => room.id === roomId);
  if (!room) {
    return '未知影厅';
  }
  return room.name;
}

const del = (id: number) => {
  ElMessageBox.confirm('删除数据后无法恢复,您确认删除吗?', '删除确认', { type: 'warning' }).then(() => {
    request.delete(apiById(API_PATHS.ORDERS, id)).then(res => {
      if (res.code === '200') {
        ElMessage.success('操作成功')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch()
}

const delBatch = () => {
  if (data.ids.length === 0) {
    ElMessage.warning('请选择数据')
    return
  }
  ElMessageBox.confirm('删除数据后无法恢复,您确认删除吗?', '删除确认', { type: 'warning' }).then(() => {
    request.delete(apiBatch(API_PATHS.ORDERS), { data: data.ids }).then(res => {
      if (res.code === '200') {
        ElMessage.success('操作成功')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch()
}

// 取票：仅"待取票"订单可操作；取票由影院/管理端执行，用户端无权调用
const pickupOrder = async (order: Ordered) => {
  try {
    const res = await request.put(ORDER_API.PICKUP(order.id));
    if (res.code === '200') {
      ElMessage.success('取票成功');
      load();
    } else {
      ElMessage.error(res.msg || '取票失败');
    }
  } catch (error) {
    // request.js has already shown the backend message.
  }
};

const handleSelectionChange = (rows: Ordered[]) => {
  data.ids = rows.map(row => row.id).filter((id): id is number => id !== undefined);
}

const reset = () => {
  data.orders = null;
  data.status = undefined;
  load();
}

// 调整加载顺序，确保影厅数据先加载
const initLoad = async () => {
  await Promise.all([
    loadRoom()
  ]);
  load(); // 最后加载订单数据
}

// 执行初始加载
initLoad();
</script>

<style scoped>
</style>

