<template>
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
      <el-button type="danger" @click="delBatch">批量删除</el-button>
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
              <!-- 修改展开面板中的影厅房间字段 -->
              <el-descriptions-item label="影厅房间">{{ getRoomName(props.row.roomId) }}</el-descriptions-item>
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
        <!-- 修改影厅名称列的参数 -->
        <el-table-column label="影厅名称">
          <template #default="prop">
            {{ getRoomName(prop.row.roomId) }}
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
</template>

<script setup lang="ts">
import { reactive } from "vue";
import { Delete, Search } from "@element-plus/icons-vue";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox } from "element-plus";


interface Ordered {
  id?: number;  // 假设这个id同时代表影厅关联ID
  orders?: string;
  userId?: number;
  filmId?: number;
  img?: string;
  cinemaId?: number;
  appointment?: string;
  total?: string;
  number?: number;
  status?: string;
  start?: string;
  seat?: string;
}

// 其他接口定义保持不变...
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


// 加载数据的方法保持不变...
const loadUser = () => {
  return request.get('/user/selectAll').then(res => {
    if(res.code === '200') {
      data.UserData = res.data
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const loadFilm = () => {
  return request.get('/film/selectAll').then(res => {
    if(res.code === '200') {
      data.FilmData = res.data
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const loadCinema = () => {
  return request.get('/cinema/selectAll').then(res => {
    if(res.code === '200') {
      data.CinemaData = res.data
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const loadRoom = () => {
  return request.get('/room/selectAll').then(res => {
    if(res.code === '200') {
      data.RoomData = res.data;
      console.log('加载的影厅数据:', data.RoomData);
    } else {
      ElMessage.error(res.msg)
    }
  }).catch(error => {
    console.error('获取影厅数据失败:', error);
    ElMessage.error('获取影厅数据失败');
  })
}

const load = () => {
  return request.get('/ordered/selectPage', {
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
      // 打印订单数据，确认id字段是否存在
      console.log('订单数据:', data.tableData);
    }
  }).catch(error => {
    console.error('加载数据失败:', error);
    ElMessage.error('加载数据失败，请重试');
  });
}

// 获取影厅名称的方法保持不变，但传入的参数变为row.id
const getRoomName = (roomId?: number) => {
  console.log('当前影厅ID:', roomId);
  if (!roomId) return '无ID';
  const room = data.RoomData.find(room => room.id === roomId);
  if (!room) {
    console.log('未找到对应的影厅记录，ID:', roomId);
    return '未知影厅';
  }
  return room.name;
}

// 其他方法保持不变...
const del = (id: number) => {
  ElMessageBox.confirm('删除数据后无法恢复,您确认删除吗?', '删除确认', { type: 'warning' }).then(() => {
    request.delete(`/ordered/delete/${id}`).then(res => {
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

const delBatch = () => {
  if (data.ids.length === 0) {
    ElMessage.warning('请选择数据')
    return
  }
  ElMessageBox.confirm('删除数据后无法恢复,您确认删除吗?', '删除确认', { type: 'warning' }).then(() => {
    request.delete('/ordered/deleteBatch', { data: data.ids }).then(res => {
      if (res.code === '200') {
        ElMessage.success('操作成功')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch()
}

const handleSelectionChange = (rows: Ordered[]) => {
  data.ids = rows.map(row => row.id).filter((id): id is number => id !== undefined);
  console.log(data.ids);
}

const reset = () => {
  data.orders = null;
  data.status = undefined;
  load();
}

// 调整加载顺序，确保影厅数据先加载
const initLoad = async () => {
  await Promise.all([
    loadRoom(),
    loadUser(),
    loadFilm(),
    loadCinema()
  ]);
  load(); // 最后加载订单数据
}

// 执行初始加载
initLoad();

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
