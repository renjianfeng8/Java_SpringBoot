<template>
  <div>
    <div class="card mb-2">
      <el-input
          v-model="data.query.title"
      placeholder="请输入区域名称"
      class="mr-2 w-72"
      :prefix-icon="Search"
      />
      <el-button type="primary" @click="load">查 询</el-button>
      <el-button type="warning" @click="reset">重 置</el-button>
    </div>

    <div class="card mb-2">
      <el-button type="info" @click="handleAdd">新 增</el-button>
      <el-button type="danger" @click="delBatch">批量删除</el-button>
    </div>

    <div class="card mb-2">
      <el-table
          stripe
          :data="data.tableData"
          @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column label="区域名称" prop="title" />
        <el-table-column label="操作">
          <template #default="scope">
            <el-button
                style="font-size: 18px"
                link
                :icon="Edit"
                @click="handleUpdate(scope.row)"
                type="primary"
            />
            <el-button
                style="font-size: 18px"
                link
                :icon="Delete"
                @click="() => del(scope.row.id)"
                type="danger"
            />
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card mb-2">
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

    <el-dialog v-model="data.formVisible" title="电影区域信息" width="500" destroy-on-close>
      <el-form ref="formRef" :rules="data.rules" :model="data.form" class="pt-5 pr-12" label-width="80px">
      <el-form-item label="区域名称" prop="title">
        <el-input v-model="data.form.title" autocomplete="off" placeholder="请输入区域名称"/>
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
import { Delete, Edit, Search } from "@element-plus/icons-vue";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox, FormRules } from "element-plus";

// 电影区域表单数据类型
interface CategoryForm {
  id?: number;
  title?: string;
}

// 表单验证规则
const rules: FormRules = {
  title: [
    { required: true, message: '请输入电影区域', trigger: 'blur' }
  ]
};

// 响应式数据 - 将查询条件和表单数据分离
const data = reactive({
  tableData: [] as CategoryForm[],
  pageNumber: 1,
  pageSize: 10,
  total: 0,
  formVisible: false,
  query: { // 查询条件
    title: undefined
  },
  form: {} as CategoryForm, // 表单数据
  ids: [] as number[],
  rules: rules
});

// 表单引用
const formRef = ref();

// 加载电影区域列表
const load = () => {
  request.get('/area/selectPage', {
    params: {
      pageNum: data.pageNumber,
      pageSize: data.pageSize,
      title: data.query.title // 使用查询条件
    }
  }).then(res => {
    if (res && res.data) {
      data.tableData = res.data.list || [];
      data.total = res.data.total || 0;
    }
  }).catch(error => {
    console.error('加载电影区域数据失败:', error);
    ElMessage.error('加载数据失败，请重试');
  });
}

// 重置查询条件
const reset = () => {
  data.query.title = undefined; // 重置查询条件
  load();
}

// 初始加载数据
load();

// 处理新增电影区域
const handleAdd = () => {
  data.formVisible = true;
  // 初始化空表单数据
  data.form = {} as CategoryForm;
}

// 处理保存电影区域
const save = () => {
  formRef.value?.validate((valid: boolean) => {
    if (valid) {
      data.form.id ? update() : add();
    } else {
      ElMessage.warning('请完成必填字段');
      return false;
    }
  });
}

// 新增电影区域
const add = () => {
  request.post('/area/add', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('电影区域添加成功');
      data.formVisible = false;
      load(); // 保持当前查询条件不变
    } else {
      ElMessage.error(res.msg || '添加失败，请重试');
    }
  }).catch(() => {
    ElMessage.error('添加失败，请检查网络连接');
  });
}

// 更新电影区域
const update = () => {
  request.put('/area/update', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('电影区域更新成功');
      data.formVisible = false;
      load(); // 保持当前查询条件不变
    } else {
      ElMessage.error(res.msg || '更新失败，请重试');
    }
  }).catch(() => {
    ElMessage.error('更新失败，请检查网络连接');
  });
}

// 处理编辑电影区域
const handleUpdate = (row: CategoryForm) => {
  data.form = JSON.parse(JSON.stringify(row)) as CategoryForm;
  data.formVisible = true;
}

// 删除单个电影区域
const del = (id: number) => {
  ElMessageBox.confirm('删除数据后无法恢复，您确认删除吗?', '删除确认', { type: 'warning' })
      .then(() => {
        request.delete(`/area/delete/${id}`).then(res => {
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
const handleSelectionChange = (rows: CategoryForm[]) => {
  data.ids = rows.map(row => row.id).filter((id): id is number => id !== undefined);
}

// 批量删除电影区域
const delBatch = () => {
  if (data.ids.length === 0) {
    ElMessage.warning('请先选择要删除的区域');
    return;
  }
  ElMessageBox.confirm(`确定删除选中的 ${data.ids.length} 条数据吗？删除后无法恢复`, '删除确认', { type: 'warning' })
      .then(() => {
        request.delete('/area/deleteBatch', { data: data.ids }).then(res => {
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

.mb-2 {
  margin-bottom: 8px;
}

.mr-2 {
  margin-right: 8px;
}

.w-72 {
  width: 18rem;
}

.pt-5 {
  padding-top: 1.25rem;
}

.pr-12 {
  padding-right: 3rem;
}
</style>