<template>
  <div>
    <div class="card" style="margin-bottom: 5px">
      <el-input v-model="data.name" placeholder="请输入姓名查询" style="width: 300px; margin-right:10px" :prefix-icon="Search" />
      <el-button type="primary" @click="load">查 询</el-button>
      <el-button type="warning" @click="reset">重 置</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-button type="info" @click="handleAdd">新 增</el-button>
      <el-button type="danger" @click="delBatch">批量删除</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="data.tableData" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column label="账号" prop="username" />
        <el-table-column label="头像">
          <template #default="scope">
            <el-image
                style="width:40px;height:40px;border-radius:50%;object-fit:cover;margin-top: 5px"
                v-if="scope.row.avatar"
                :src="scope.row.avatar"
                :preview-src-list="[scope.row.avatar]"
                preview-teleported
            />
          </template>
        </el-table-column>
        <el-table-column label="姓名" prop="name" />
        <el-table-column label="电话" prop="phone" />
        <el-table-column label="邮箱" prop="email"  show-overflow-tooltip/>
        <el-table-column label="角色" prop="role">
          <template #default="scope">
            <el-tag :type="getRoleType(scope.row.role)">
              {{scope.row.role}}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button style="font-size: 18px" link :icon="Edit" @click="handleUpdate(scope.row)" type="primary" />
            <el-button style="font-size: 18px" link :icon="Delete" @click="() => del(scope.row.id)" type="danger" />
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

    <el-dialog v-model="data.formVisible" title="用户信息" width="500" destroy-on-close>
      <el-form ref="formRef" :rules="data.rules" :model="data.form" style="padding-right: 50px;padding-top: 20px" label-width="80px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="data.form.username" autocomplete="off" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="头像" prop="avatar">
          <el-upload
              :action="baseUrl + '/user/upload'"
              :on-success="handleFileUpload"
              :auto-upload="true"
              list-type="picture"
          >
            <el-button type="primary">点击上传</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="data.form.name" autocomplete="off" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="data.form.phone" autocomplete="off" placeholder="请输入电话"/>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="data.form.email" autocomplete="off" placeholder="请输入邮箱" />
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

const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:9090';

// 用户表单数据类型
interface UserForm {
  id?: number;
  username?: string;
  avatar?: string;
  name?: string;
  phone?: string;
  email?: string;
  role?: string;
}

// 表单验证规则
const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
};

// 响应式数据
const data = reactive({
  name: null,
  tableData: [] as UserForm[],
  pageNumber: 1,
  pageSize: 10,
  total: 0,
  formVisible: false,
  form: {} as UserForm,
  ids: [] as number[],
  rules: rules
});

// 表单引用
const formRef = ref();

// 加载用户列表
const load = () => {
  request.get('/user/selectPage', {
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
    console.error('加载用户数据失败:', error);
    ElMessage.error('加载数据失败，请重试');
  });
}

// 重置查询条件
const reset = () => {
  data.name = null;
  load();
}

// 初始加载数据
load();

// 处理新增用户
const handleAdd = () => {
  data.formVisible = true;
  // 初始化空表单数据
  data.form = {} as UserForm;
}

// 处理保存用户
const save = () => {
  formRef.value?.validate((valid: boolean) => {
    if (valid) {
      data.form.id ? update() : add();
    }
  });
}

// 新增用户
const add = () => {
  request.post('/user/add', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('用户添加成功');
      data.formVisible = false;
      load();
    } else {
      ElMessage.error(res.msg || '添加失败，请重试');
    }
  }).catch(() => {
    ElMessage.error('添加失败，请检查网络连接');
  });
}

// 更新用户
const update = () => {
  request.put('/user/update', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('用户更新成功');
      data.formVisible = false;
      load();
    } else {
      ElMessage.error(res.msg || '更新失败，请重试');
    }
  }).catch(() => {
    ElMessage.error('更新失败，请检查网络连接');
  });
}

// 处理编辑用户
const handleUpdate = (row: UserForm) => {
  data.form = JSON.parse(JSON.stringify(row)) as UserForm;
  data.formVisible = true;
}

// 删除单个用户
const del = (id: number) => {
  ElMessageBox.confirm('删除数据后无法恢复，您确认删除吗?', '删除确认', { type: 'warning' })
      .then(() => {
        request.delete(`/user/delete/${id}`).then(res => {
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
const handleSelectionChange = (rows: UserForm[]) => {
  data.ids = rows.map(row => row.id).filter((id): id is number => id !== undefined);
}

// 批量删除用户
const delBatch = () => {
  if (data.ids.length === 0) {
    ElMessage.warning('请先选择要删除的用户');
    return;
  }
  ElMessageBox.confirm(`确定删除选中的 ${data.ids.length} 条数据吗？删除后无法恢复`, '删除确认', { type: 'warning' })
      .then(() => {
        request.delete('/user/deleteBatch', { data: data.ids }).then(res => {
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

// 处理头像上传
const handleFileUpload = (res: any) => {
  if (res.code === '200') {
    data.form.avatar = res.data;
    ElMessage.success('头像上传成功');
  } else {
    ElMessage.error(res.msg || '头像上传失败');
  }
}

const getRoleType = (role : string) => {
  switch (role) {
    case 'ADMIN': return 'warning';
    case 'USER': return 'success';
    case 'CINEMA': return 'danger';
    default: return 'info';
  }
}

</script>

<style scoped>

</style>