<template>
  <div>
    <div class="card page-card">
      <el-input v-model="data.title"  placeholder="请输入电影名称查询" class="search-input" :prefix-icon="Search"/>
      <el-input  v-model="data.start"  placeholder="按放映日期查询 (YYYY-MM-DD)" class="search-input" :prefix-icon="Search"/>
      <el-select v-model="data.status" placeholder="请选择放映状态" class="search-input">
        <el-option label="正常" value="正常" />
        <el-option label="停售" value="停售" />
      </el-select>
      <el-button type="primary" @click="load">查 询</el-button>
      <el-button type="warning" @click="reset">重 置</el-button>
    </div>

    <div class="card page-card">
      <el-button type="info" @click="handleAdd">新 增</el-button>
      <el-button type="danger" @click="delBatch">批量删除</el-button>
    </div>

    <div class="card page-card">
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
            <el-button class="row-action" link :icon="Edit" @click="handleUpdate(scope.row)" type="primary" />
            <el-button class="row-action" link :icon="Delete" @click="() => del(scope.row.id)" type="danger"></el-button>
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

    <el-dialog v-model="data.formVisible" title="放映记录" width="500" destroy-on-close>
      <el-form ref="formRef" :rules="data.rules" :model="data.form" class="dialog-form" label-width="85px">
        <el-form-item label="影院名称">
          <el-input :model-value="cinemaName" disabled/>
        </el-form-item>
        <el-form-item label="影厅名称" prop="roomId">
          <el-select v-model="data.form.roomId" placeholder="请选择影厅" clearable>
            <el-option v-for="room in data.roomData" :key="room.id" :label="room.name" :value="room.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="影片" prop="filmId">
          <el-select v-model="data.form.filmId" placeholder="请选择影片" clearable filterable
                     @change="handleFilmChange">
            <el-option v-for="film in data.filmData" :key="film.id" :label="film.title" :value="film.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="电影名称" prop="title">
          <el-input v-model="data.form.title" disabled placeholder="由所选影片自动带出"/>
        </el-form-item>
        <el-form-item label="放映时间" prop="start">
          <el-date-picker v-model="data.form.start"
              type="datetime"
              placeholder="选择放映时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm"
              class="field-full"
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
            <el-option label="正常" value="正常" />
            <el-option label="停售" value="停售" />
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
import { computed, reactive, ref } from "vue";
import { Delete, Edit, Search } from "@element-plus/icons-vue";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox } from "element-plus";
import { API_PATHS, apiBatch, apiById, apiPage, getRecordStatusType as getStatusType } from "@/constants";
import { useAuth } from "@/composables/useAuth";

interface Record {
  id?: number;
  cinemaId?: number;
  roomId?: number;
  filmId?: number;
  title?: string;
  price?: string;
  start?: string;
  status?: string;
  cinemaName?: string;
  roomName?: string;
}

interface RoomData {
  id: number;
  name: string;
}

interface FilmData {
  id: number;
  title: string;
}

// 影院端只能为本影院排片：影院范围由后端按 token 强制，前端不再提供影院选择
const { user } = useAuth();
const cinemaName = computed(() => user.value?.name || '当前影院');

const data = reactive({
  tableData: [] as Record[],
  pageNumber: 1,
  pageSize: 10,
  total: 0,
  formVisible: false,
  form: {} as Record,
  ids: [] as number[],
  roomData: [] as RoomData[],
  filmData: [] as FilmData[],
  title: null as string | null,
  start: null as string | null,
  status: undefined as string | undefined,
  rules: {
    roomId: [{ required: true, message: '请选择影厅', trigger: 'change' }],
    filmId: [{ required: true, message: '请选择影片', trigger: 'change' }],
    start: [{ required: true, message: '请选择放映时间', trigger: 'change' }],
    price: [{ required: true, message: '请输入电影票价', trigger: 'blur' }],
    status: [{ required: true, message: '请选择放映状态', trigger: 'change' }],
  },
});

const delBatch = () => {
  if (data.ids.length === 0) {
    ElMessage.warning('请选择数据')
    return
  }
  ElMessageBox.confirm('删除数据后无法恢复,您确认删除吗?', '删除确认', { type: 'warning' }).then(() => {
    request.delete(apiBatch(API_PATHS.RECORDS), { data: data.ids }).then(res => {
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
  request.get(apiPage(API_PATHS.RECORDS), {
    params: {
      pageNum: data.pageNumber,
      pageSize: data.pageSize,
      title: data.title,
      start: data.start,
      status: data.status,
    }
  }).then(res => {
    if (res && res.data) {
      // 后端 selectAll 已 JOIN 出 cinemaName / roomName，前端不再自行拼接
      data.tableData = res.data.list || [];
      data.total = res.data.total || 0;
    }
  }).catch(error => {
    console.error('加载数据失败:', error);
    ElMessage.error('加载数据失败，请重试');
  });
};

const del = (id: number) => {
  ElMessageBox.confirm('删除数据后无法恢复,您确认删除吗?', '删除确认', { type: 'warning' }).then(() => {
    request.delete(apiById(API_PATHS.RECORDS, id)).then(res => {
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
  request.post(API_PATHS.RECORDS, data.form).then(res => {
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
  request.put(API_PATHS.RECORDS, data.form).then(res => {
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

const formRef = ref();

const save = () => {
  formRef.value?.validate((valid: boolean) => {
    if (valid) {
      data.form.id ? update() : add();
    }
  });
}

const loadRoom = () => {
  return request.get(API_PATHS.ROOMS).then(res => {
    if (res.code === '200') {
      data.roomData = res.data;
    } else {
      ElMessage.error(res.msg);
    }
  });
};

// 片库为全局资源，影院从中选片排期；影院上映列表由排片反推
const loadFilm = () => {
  return request.get(API_PATHS.FILMS).then(res => {
    if (res.code === '200') {
      data.filmData = res.data;
    } else {
      ElMessage.error(res.msg);
    }
  });
};

// 影片选定后回填名称（后端以 filmId 为准重新回填，前端仅做展示）
const handleFilmChange = (filmId: number) => {
  data.form.title = data.filmData.find(film => film.id === filmId)?.title || '';
};

// 初始加载：先加载影厅和影片，再加载表格
const init = async () => {
  await Promise.all([loadRoom(), loadFilm()]);
  load();
};

// 执行初始化
init();

const handleAdd = () => {
  data.formVisible = true;
  // 初始化空表单数据
  data.form = {};
}

const handleUpdate = (row: Record) => {
  data.form = JSON.parse(JSON.stringify(row));
  data.formVisible = true;
}

</script>
