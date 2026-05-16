<template>
  <div>
    <div class="card" style="margin-bottom: 5px">
      <el-input v-model="data.title"  placeholder="请输入电影名称查询" style="width: 300px; margin-right:10px" :prefix-icon="Search"/>
      <el-input  v-model="data.start"  placeholder="按放映日期查询 (YYYY-MM-DD)" style="width: 300px; margin-right:10px" :prefix-icon="Search"/>
      <el-select v-model="data.status" placeholder="请选择放映状态" style="width: 300px; margin-right:10px">
        <el-option label="待上映" value="待上映" />
        <el-option label="已上映" value="已上映" />
        <el-option label="停止上映" value="停止上映" />
      </el-select>
      <el-button type="primary" @click="load">查 询</el-button>
      <el-button type="warning" @click="reset">重 置</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-button type="info" @click="handleAdd">新 增</el-button>
      <el-button type="danger" @click="delBatch">批量删除</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="data.tableData" @selection-change="handleSelectionChange">
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
            <el-button style="font-size: 18px" link :icon="Edit" @click="handleUpdate(scope.row)" type="primary" />
            <el-button style="font-size: 18px" link :icon="Delete" @click="() => del(scope.row.id)" type="danger"></el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card" style="  margin-bottom: 5px">
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

    <el-dialog v-model="data.formVisible" title="放映记录" width="500" destroy-on-close>
      <el-form ref="formRef" :rules="data.rules" :model="data.form" style="padding-right: 50px;padding-top: 20px" label-width="85px">
        <el-form-item label="影院名称" prop="cinemaId">
          <el-select v-model="data.form.cinemaId" placeholder="请选择影院" clearable>
            <el-option v-for="cinema in data.cinemaData" :key="cinema.id" :label="cinema.name" :value="cinema.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="影厅名称" prop="roomId">
          <el-select v-model="data.form.roomId" placeholder="请选择影厅" clearable>
            <el-option v-for="room in data.roomData" :key="room.id" :label="room.name" :value="room.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="电影名称" prop="title">
          <el-input v-model="data.form.title" autocomplete="off" placeholder="请输入电影名称"/>
        </el-form-item>
        <el-form-item label="放映时间" prop="start">
          <el-date-picker v-model="data.form.start"
              type="datetime"
              placeholder="选择放映时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm"
              style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="电影票价" prop="price">
          <el-input
              v-model="data.form.price"
              autocomplete="off"
              placeholder="请输入电影票价"
              type="number"
              min="0"
              step="0.1"
          />
        </el-form-item>
        <el-form-item label="放映状态" prop="status">
          <el-select v-model="data.form.status" placeholder="请选择放映状态">
            <el-option label="待上映" value="待上映" />
            <el-option label="已上映" value="已上映" />
            <el-option label="停止上映" value="停止上映" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="data.formVisible = false">取 消</el-button>
          <el-button type="primary" @click="save">保 存</el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import {Delete, Edit, Search} from "@element-plus/icons-vue";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox } from "element-plus";

interface Record {
  id?: number;
  cinemaId?: number;
  roomId?: number;
  title?: string;
  price?: string;
  start?: string;
  status?: string;
  cinemaName?: string;
  roomName?: string;
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
  tableData: [] as Record[],
  pageNumber: 1,
  pageSize: 10,
  total: 0,
  formVisible: false,
  form: {} as Record,
  ids: [] as number[],
  cinemaData: [] as CinemaData[],
  roomData: [] as RoomData[],
  title: null,
  start: null,
  status: undefined as string | undefined,
});

const delBatch = () => {
  if (data.ids.length === 0) {
    ElMessage.warning('请选择数据')
    return
  }
  ElMessageBox.confirm('删除数据后无法恢复,您确认删除吗?', '删除确认', { type: 'warning' }).then(() => {
    request.delete('/record/deleteBatch', { data: data.ids }).then(res => {
      if (res.code === '200') {
        ElMessage.success('操作成功')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch()
}

const handleSelectionChange = (rows: Record[]) => {
  data.ids = rows.map(row => row.id).filter((id): id is number => id !== undefined);
};

const reset = () => {
  data.title = null;
  data.start = null;
  data.status = undefined;
  load();
};

const load = () => {
  request.get('/record/selectPage', {
    params: {
      pageNum: data.pageNumber,
      pageSize: data.pageSize,
      title: data.title,
      start: data.start,
      status: data.status,
    }
  }).then(res => {
    if (res && res.data) {
      const rawList = res.data.list || [];
      // 2. 遍历数据，匹配影院名称和影厅名称
      data.tableData = rawList.map((record: Record) => {
        // 匹配影院名称：从cinemaData中找到id等于record.cinemaId的项
        const cinema = data.cinemaData.find(c => c.id === record.cinemaId);
        // 匹配影厅名称：从roomData中找到id等于record.roomId的项
        const room = data.roomData.find(r => r.id === record.roomId);
        return {
          ...record,
          cinemaName: cinema?.name,
          roomName: room?.name
        };
      });
      data.total = res.data.total || 0;
    }
  }).catch(error => {
    console.error('加载数据失败:', error);
    ElMessage.error('加载数据失败，请重试');
  });
};


const del = (id: number) => {
  ElMessageBox.confirm('删除数据后无法恢复,您确认删除吗?', '删除确认', { type: 'warning' }).then(() => {
    request.delete(`/record/delete/${id}`).then(res => {
      if (res.code === '200') {
        ElMessage.success('操作成功');
        load();
        data.formVisible = false;
      } else {
        ElMessage.error(res.msg);
      }
    });
  }).catch(() => {});
};


const add = () => {
  request.post('/record/add', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('添加成功');
      data.formVisible = false;
      load();
    } else {
      ElMessage.error(res.msg || '添加失败，请重试');
    }
  }).catch(() => {
    ElMessage.error('添加失败，请检查网络连接');
  });
}

const update = () => {
  request.put('/record/update', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('更新成功');
      data.formVisible = false;
      load();
    } else {
      ElMessage.error(res.msg || '更新失败，请重试');
    }
  }).catch(() => {
    ElMessage.error('更新失败，请检查网络连接');
  });
}

const save = () => {
  formRef.value?.validate((valid: boolean) => {
    if (valid) {
      data.form.id ? update() : add();
    }
  });
}


const getStatusType = (status: string | undefined) => {
  switch (status) {
    case '待上映': return 'warning';
    case '已上映': return 'success';
    case '停止上映': return 'danger';
    default: return 'info';
  }
};


const loadCinema = () => {
  return request.get('/cinema/selectAll').then(res => {
    if(res.code === '200') {
      data.cinemaData = res.data;
    } else {
      ElMessage.error(res.msg);
    }
  });
};


const loadRoom = () => {
  return request.get('/room/selectAll').then(res => {
    if(res.code === '200') {
      data.roomData = res.data;
    } else {
      ElMessage.error(res.msg);
    }
  });
};
// 初始加载：先加载影院和影厅数据，再加载表格
const init = async () => {
  // 等待影院和影厅数据加载完成
  await Promise.all([
    loadCinema(),
    loadRoom()
  ]);
  // 再加载表格数据
  load();
};

// 执行初始化
init();

const formRef = ref();


const handleAdd = () => {
  data.formVisible = true;
  // 初始化空表单数据
  data.form = {};
}


const handleUpdate = (row) => {
  data.form = JSON.parse(JSON.stringify(row));
  data.formVisible = true;
}

</script>

<style scoped>

</style>