<template>
  <div style="width: 85%; margin: 20px auto;">
    <div>
      <div class="card" style="margin-bottom: 5px">
        <el-input v-model="data.orders" placeholder="请输入订单号" style="width: 300px; margin-right:10px" :prefix-icon="Search"/>
        <el-select v-model="data.status" placeholder="请选择放映状态" style="width: 300px; margin-right:10px">
          <el-option label="已取票" value="已取票" />
          <el-option label="待取票" value="待取票" />
        </el-select>
        <el-button type="primary" @click="load">查 询</el-button>
        <el-button type="warning" @click="reset">重 置</el-button>
      </div>

      <div class="card" style="margin-bottom: 5px">
        <el-table stripe :data="data.tableData" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="55"/>
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
                <!-- 关键修改1：改用后端返回的roomName，或传roomId给getRoomName -->
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
              <el-image style="margin-top: 7px; width:36px;height:36px;border-radius:10%;object-fit:cover;
                      align-items:center;"
                        v-if="scope.row.img"
                        :src="scope.row.img"
                        :preview-src-list="[scope.row.img]"
                        preview-teleported/>
            </template>
          </el-table-column>
          <el-table-column label="影院名称" prop="cinemaName"/>
          <!-- 关键修改2：改用后端返回的roomName，或传roomId给getRoomName -->
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
              <el-button v-if="scope.row.status === '待取票'" style="font-size: 14px" link type="warning" @click="() => cancelOrder(scope.row.id)">取消</el-button>
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
import { API_PATHS, apiById, apiPage } from '@/constants';

// 关键修改3：扩展Ordered接口，增加后端返回的字段
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
  roomName?: string; // 新增roomName字段
}

interface UserData {
  id: number;
  name: string;
}

interface FilmData {
  id: number;
  title: string;
}

interface CinemaData {
  id: number;
  name: string;
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
  formVisible: false,
  form: {} as Ordered,
  ids: [] as number[],
  UserData: [] as UserData[],
  FilmData: [] as FilmData[],
  CinemaData: [] as CinemaData[],
  RoomData: [] as RoomData[],
  orders: null,
  start: null,
  seat: null,
  status: undefined
});

const loadUser = () => {
  request.get(API_PATHS.USERS).then(res => {
    if(res.code === '200') {
      data.UserData = res.data
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const loadFilm = () => {
  request.get(API_PATHS.FILMS).then(res => {
    if(res.code === '200') {
      data.FilmData = res.data
    } else {
      ElMessage.error(res.msg)
    }
  })
}

// 关键修改4：函数逻辑优化，兜底文本和后端保持一致
const getRoomName = (roomId?: number) => {
  if (!roomId) return '暂未关联影厅'; // 改为和后端一致的兜底文本
  const room = data.RoomData.find(room => room.id === roomId);
  if (!room) {
    return '暂未关联影厅'; // 改为和后端一致的兜底文本
  }
  return room.name;
}

const loadCinema = () => {
  request.get(API_PATHS.CINEMAS).then(res => {
    if(res.code === '200') {
      data.CinemaData = res.data
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const loadRoom = () => {
  request.get(API_PATHS.ROOMS).then(res => {
    if(res.code === '200') {
      data.RoomData = res.data
    } else {
      ElMessage.error(res.msg)
    }
  })
}

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
        data.formVisible = false
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch()
}

const cancelOrder = async (id: number) => {
  try {
    const res = await request.put(`/api/v1/orders/${id}/cancel`)
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

const handleSelectionChange = (rows: Ordered[]) => {
  data.ids = rows.map(row => row.id).filter((id): id is number => id !== undefined);
}

const reset = () => {
  data.orders = null;
  data.status = undefined;
  load();
}

// 初始加载
load()

const getStatusType = (status: string | undefined) => {
  switch (status) {
    case '已取票': return 'warning';
    case '待取票': return 'success';
    default: return 'info';
  }
}
</script>

<style scoped>
</style>
