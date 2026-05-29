<template>
  <div>
    <div class="card" style="margin-bottom: 5px">
      <el-input v-model="data.name" placeholder="请输入影厅名称" style="width: 300px; margin-right:10px" :prefix-icon="Search"/>
      <el-button type="primary" @click="load">查 询</el-button>
      <el-button type="warning" @click="reset">重 置</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-button type="info" @click="handleAdd">新 增</el-button>
      <el-button type="danger" @click="delBatch">批量删除</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table
          stripe
          :data="data.tableData"
          @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column label="影院名称" prop="title" />
        <el-table-column label="影厅名称" prop="name" />
        <el-table-column label="操作">
          <template #default="scope">
            <el-button style="font-size: 18px" link :icon="Edit" @click="handleUpdate(scope.row)" type="primary" />
            <el-button style="font-size: 18px" link :icon="Delete" @click="() => del(scope.row.id)" type="danger"/>
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

    <el-dialog v-model="data.formVisible" title="影厅房间信息" width="500" destroy-on-close>
      <el-form ref="formRef" :rules="data.rules" :model="data.form" style="padding-right: 50px;padding-top: 20px" label-width="85px">
        <el-form-item label="影院名称" prop="title">
          <el-input v-model="data.form.title" autocomplete="off" placeholder="请输入影院名称"/>
        </el-form-item>
        <el-form-item label="影厅名称" prop="name">
          <el-input v-model="data.form.name" autocomplete="off" placeholder="请输入影厅名称"/>
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
import {ElMessage, ElMessageBox, FormRules} from "element-plus";

interface RoomForm {
  id?: number;
  title?: string;
  name?: string;
}

const rules: FormRules = {
  title: [
    { required: true, message: '请输入影院名称', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入影厅名称', trigger: 'blur' }
  ]
};

// 表单引用
const formRef = ref();


const data = reactive({
  tableData: [] as RoomForm[],
  pageNumber: 1,
  pageSize: 10,
  total: 0,
  formVisible: false,
  name: '',
  form: {} as RoomForm, // 表单数据
  ids: [] as number[],
  rules: rules
});


// 加载电影分类列表
const load = () => {
  request.get('/api/v1/rooms/page', {
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
  request.post('/api/v1/rooms', data.form).then(res => {
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
  request.put('/api/v1/rooms', data.form).then(res => {
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

// 重置查询条件
const reset = () => {
  data.name = '';
  load();
}

// 初始加载数据
load();

const handleAdd = () => {
  data.formVisible = true;
  // 初始化空表单数据
  data.form = {} as RoomForm;
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


// 删除单个电影分类
const del = (id: number) => {
  ElMessageBox.confirm('删除数据后无法恢复，您确认删除吗?', '删除确认', { type: 'warning' })
      .then(() => {
        request.delete(`/api/v1/rooms/${id}`).then(res => {
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

// 处理选中行变更
const handleSelectionChange = (rows: RoomForm[]) => {
  data.ids = rows.map(row => row.id).filter((id): id is number => id !== undefined);
}

// 批量删除电影分类
const delBatch = () => {
  if (data.ids.length === 0) {
    ElMessage.warning('请先选择要删除的分类');
    return;
  }
  ElMessageBox.confirm(`确定删除选中的 ${data.ids.length} 条数据吗？删除后无法恢复`, '删除确认', { type: 'warning' })
      .then(() => {
        request.delete('/api/v1/rooms/batch', { data: data.ids }).then(res => {
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

</script>

<style scoped>


</style>
