<template>
  <div>
    <div class="card page-card">
      <el-input v-model="data.name" placeholder="请输入影厅名称" class="search-input" :prefix-icon="Search"/>
      <el-button type="primary" @click="load">查 询</el-button>
      <el-button type="warning" @click="reset">重 置</el-button>
    </div>

    <div class="card page-card">
      <el-button type="info" @click="handleAdd">新 增</el-button>
      <el-button type="danger" @click="delBatch">批量删除</el-button>
    </div>

    <div class="card page-card">
      <el-table
          stripe
          :data="data.tableData"
          @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column label="影院名称" prop="title" />
        <el-table-column label="影厅名称" prop="name" />
        <el-table-column label="座位规模" width="120">
          <template #default="scope">{{ scope.row.seatRows }} 排 × {{ scope.row.seatCols }} 座</template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button class="row-action" link :icon="Edit" @click="handleUpdate(scope.row)" type="primary" />
            <el-button class="row-action" link :icon="Delete" @click="() => del(scope.row.id)" type="danger"/>
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

    <el-dialog v-model="data.formVisible" title="影厅房间信息" width="500" destroy-on-close>
      <el-form ref="formRef" :rules="data.rules" :model="data.form" class="dialog-form" label-width="85px">
        <el-form-item label="影院名称">
          <el-input :model-value="cinemaName" disabled/>
        </el-form-item>
        <el-form-item label="影厅名称" prop="name">
          <el-input v-model="data.form.name" autocomplete="off" placeholder="请输入影厅名称"/>
        </el-form-item>
        <el-form-item label="座位行数" prop="seatRows">
          <el-input-number v-model="data.form.seatRows" :min="1" :max="50" class="field-full"/>
        </el-form-item>
        <el-form-item label="座位列数" prop="seatCols">
          <el-input-number v-model="data.form.seatCols" :min="1" :max="50" class="field-full"/>
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
import { API_PATHS, apiBatch, apiById, apiPage } from "@/constants";
import { useAuth } from "@/composables/useAuth";

interface RoomForm {
  id?: number;
  title?: string;
  name?: string;
  seatRows?: number;
  seatCols?: number;
}

// 影院名称由后端按 token 的影院归属派生，前端不再手填（避免同一影院出现多个影院名）
const { user } = useAuth();
const cinemaName = computed(() => user.value?.name || '当前影院');

const formRef = ref();

const data = reactive({
  tableData: [] as RoomForm[],
  pageNumber: 1,
  pageSize: 10,
  total: 0,
  formVisible: false,
  name: '',
  form: {} as RoomForm,
  ids: [] as number[],
  rules: {
    name: [
      { required: true, message: '请输入影厅名称', trigger: 'blur' }
    ],
    seatRows: [
      { required: true, message: '请输入座位行数', trigger: 'blur' }
    ],
    seatCols: [
      { required: true, message: '请输入座位列数', trigger: 'blur' }
    ]
  }
});

const load = () => {
  request.get(apiPage(API_PATHS.ROOMS), {
    params: {
      pageNum: data.pageNumber,
      pageSize: data.pageSize,
      name: data.name
    }
  }).then(res => {
    if (res && res.data) {
      data.tableData = res.data.list || [];
      data.total = res.data.total || 0;
    }
  }).catch(error => {
    console.error('加载电影分类数据失败:', error);
    ElMessage.error('加载数据失败，请重试');
  });
}

const add = () => {
  request.post(API_PATHS.ROOMS, data.form).then(res => {
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
  request.put(API_PATHS.ROOMS, data.form).then(res => {
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

const reset = () => {
  data.name = '';
  load();
}

const handleAdd = () => {
  data.formVisible = true;
  // 新影厅默认 8×8，与 room 表列默认值一致
  data.form = { seatRows: 8, seatCols: 8 } as RoomForm;
}

const handleUpdate = (row: RoomForm) => {
  data.form = JSON.parse(JSON.stringify(row)) as RoomForm;
  data.formVisible = true;
}

const save = () => {
  formRef.value?.validate((valid: boolean) => {
    if (valid) {
      data.form.id ? update() : add();
    }
  });
}

const del = (id: number) => {
  ElMessageBox.confirm('删除数据后无法恢复，您确认删除吗?', '删除确认', { type: 'warning' })
      .then(() => {
        request.delete(apiById(API_PATHS.ROOMS, id)).then(res => {
          if (res.code === '200') {
            ElMessage.success('删除成功');
            load();
          } else {
            ElMessage.error(res.msg || '删除失败，请重试');
          }
        }).catch(() => {
          ElMessage.error('删除失败，请检查网络连接');
        });
      })
      .catch();
}

const handleSelectionChange = (rows: RoomForm[]) => {
  data.ids = rows.map(row => row.id).filter((id): id is number => id !== undefined);
}

const delBatch = () => {
  if (data.ids.length === 0) {
    ElMessage.warning('请先选择要删除的分类');
    return;
  }
  ElMessageBox.confirm(`确定删除选中的 ${data.ids.length} 条数据吗？删除后无法恢复`, '删除确认', { type: 'warning' })
      .then(() => {
        request.delete(apiBatch(API_PATHS.ROOMS), { data: data.ids }).then(res => {
          if (res.code === '200') {
            ElMessage.success('批量删除成功');
            load();
          } else {
            ElMessage.error(res.msg || '删除失败，请重试');
          }
        }).catch(() => {
          ElMessage.error('删除失败，请检查网络连接');
        });
      })
      .catch();
}

load();
</script>
