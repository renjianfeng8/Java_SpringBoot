<template>
  <div style="width: 85%; margin: 20px auto;">
    <div>
      <div class="card" style="margin-bottom: 5px">
        <el-input v-model="data.orders" placeholder="请输入订单号" style="width: 300px; margin-right:10px" :prefix-icon="Search"/>
        <el-select v-model="data.status" placeholder="请选择订单状态" style="width: 300px; margin-right:10px">
          <el-option label="待支付" value="待支付" />
          <el-option label="待取票" value="待取票" />
          <el-option label="已取票" value="已取票" />
          <el-option label="已取消" value="已取消" />
        </el-select>
        <el-button type="primary" @click="load">查 询</el-button>
        <el-button type="warning" @click="reset">重 置</el-button>
      </div>

      <div class="card" style="margin-bottom: 5px">
        <el-table stripe :data="data.tableData">
          <el-table-column type="expand">
            <template #default="props">
              <el-descriptions title="订单信息" :column="4" border>
                <el-descriptions-item label="电影图片">
                  <el-image style="width:36px;height:36px;object-fit:cover;"
                            :src="props.row.img"/>
                </el-descriptions-item>
                <el-descriptions-item label="订单号">{{props.row.orders}}</el-descriptions-item>
                <el-descriptions-item label="用户名称">{{props.row.userName}}</el-descriptions-item>
                <el-descriptions-item label="电影名称">{{props.row.filmName}}</el-descriptions-item>
                <el-descriptions-item label="影院名称">{{props.row.cinemaName}}</el-descriptions-item>
                <el-descriptions-item label="影厅房间">{{ props.row.roomName }}</el-descriptions-item>
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
              <el-image style="margin-top: 7px; width:36px;height:36px;border-radius:10%;object-fit:cover;
                      align-items:center;"
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
              <el-button v-if="scope.row.status === '待支付'" style="font-size: 14px" link type="warning" @click="() => cancelOrder(scope.row.id)">取消</el-button>
              <el-button style="font-size: 18px" link :icon="Delete" @click="() => del(scope.row.id)" type="danger"></el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="card" style="margin-bottom: 5px">
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
  </div>
</template>

<script setup lang="ts">
import { reactive } from "vue";
import { Delete, Search } from "@element-plus/icons-vue";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox } from "element-plus";
import { API_PATHS, ORDER_API, getOrderStatusType as getStatusType, apiById, apiPage } from '@/constants';

interface Ordered {
  id?: number;
  orders?: string;
  userId?: number;
  filmId?: number;
  img?: string;
  cinemaId?: number;
  roomId?: number;
  appointment?: string;
  total?: string;
  number?: number;
  status?: string;
  start?: string;
  seat?: string;
  // 新增后端返回的关联字段
  userName?: string;
  filmName?: string;
  cinemaName?: string;
  roomName?: string;
}

const data = reactive({
  tableData: [] as Ordered[],
  pageNumber: 1,
  pageSize: 10,
  total: 0,
  orders: null,
  status: undefined
});

const load = () => {
  request.get(apiPage(API_PATHS.ORDERS), {
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

const cancelOrder = async (id: number) => {
  try {
    const res = await request.put(ORDER_API.CANCEL(id))
    if (res.code === '200') {
      ElMessage.success('订单已取消')
      await load()
    } else {
      ElMessage.error(res.msg || '取消失败')
    }
  } catch (error) {
    // request.js has already shown the backend message.
  }
}

const reset = () => {
  data.orders = null;
  data.status = undefined;
  load();
}

// 初始加载
load()
</script>

<style scoped>
</style>
