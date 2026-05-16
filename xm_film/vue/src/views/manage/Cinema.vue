<template>
  <div>
    <div class="card" style="margin-bottom: 5px">
      <el-input v-model="data.name" placeholder="请输入影院名称查询" style="width: 300px; margin-right:10px" :prefix-icon="Search"/>
      <el-button type="primary" @click="load">查 询</el-button>
      <el-button type="warning" @click="reset">重 置</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-button type="info" @click="handleAdd">新 增</el-button>
      <el-button type="danger" @click="delBatch">批量删除</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="data.tableData" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50"/>
        <el-table-column type="expand">
          <template #default="props">
            <el-descriptions title="影院信息" :column="4" border>
              <el-descriptions-item label="账号">{{ props.row.username }}</el-descriptions-item>
              <el-descriptions-item label="角色">{{ props.row.role }}</el-descriptions-item>
              <el-descriptions-item label="头像">
                <el-image style="width:36px;height:36px;object-fit:cover;" :src="props.row.avatar"/>
              </el-descriptions-item>
              <el-descriptions-item label="电影院名称">{{ props.row.name }}</el-descriptions-item>
              <el-descriptions-item label="手机号">{{ props.row.phone }}</el-descriptions-item>
              <el-descriptions-item label="邮箱">{{ props.row.email }}</el-descriptions-item>
              <el-descriptions-item label="影院地址">{{ props.row.address }}</el-descriptions-item>
              <el-descriptions-item label="负责人姓名">{{ props.row.leader }}</el-descriptions-item>
              <el-descriptions-item label="身份证号">
                <el-popover placement="top-start" title="身份证号码" :width="200" trigger="hover" :content="props.row.code">
                  <template #reference>
                    <div class="line" style="width: 50px">{{ props.row.code }}</div>
                  </template>
                </el-popover>
              </el-descriptions-item>
              <el-descriptions-item label="营业执照">
                <el-image style="width:36px;height:36px;object-fit:cover;" :src="props.row.certificate"/>
              </el-descriptions-item>
              <el-descriptions-item label="审核状态">
                <!-- 核心修改：使用转换后的状态显示 -->
                <el-tag :type="getStatusType(getTransformedStatus(props.row.status))">
                  {{ getTransformedStatus(props.row.status) }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </template>
        </el-table-column>
        <el-table-column label="账号" prop="username"/>
        <el-table-column label="头像">
          <template #default="scope">
            <el-image style="margin-top: 5px;width:40px;height:40px;border-radius:50%;object-fit:cover;"
                      v-if="scope.row.avatar"
                      :src="scope.row.avatar"
                      :preview-src-list="[scope.row.avatar]"
                      preview-teleported/>
          </template>
        </el-table-column>
        <el-table-column label="影院名称" prop="name" />
        <el-table-column label="电话" prop="phone" show-overflow-tooltip/>
        <el-table-column label="邮箱" prop="email" show-overflow-tooltip/>
        <el-table-column label="地址" prop="address" show-overflow-tooltip/>
        <el-table-column label="审核状态" prop="status">
          <template #default="scope">
            <!-- 核心修改：使用转换后的状态显示 -->
            <el-tag :type="getStatusType(getTransformedStatus(scope.row.status))">
              {{ getTransformedStatus(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" prop="role">
          <template #default="scope">
            <el-tag>{{scope.row.role}}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button style="font-size: 18px" link :icon="Edit" @click="handleUpdate(scope.row)" type="primary"></el-button>
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

    <el-dialog v-model="data.formVisible" title="影院信息" width="500" destroy-on-close>
      <el-form ref="formRef" :rules="data.rules" :model="data.form" style="padding-right: 50px;padding-top: 20px" label-width="85px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="data.form.username" autocomplete="off" placeholder="请输入账号"/>
        </el-form-item>
        <el-form-item label="头像" prop="avatar">
          <el-upload
              :action="baseUrl + '/cinema/upload'"
              :on-success="handleFileUpload"
              :auto-upload="true"
              list-type="picture">
            <el-button type="primary">点击上传</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="影院名称" prop="name">
          <el-input v-model="data.form.name" autocomplete="off" placeholder="请输入影院名称"/>
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="data.form.phone" autocomplete="off" placeholder="请输入电话"/>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="data.form.email" autocomplete="off" placeholder="请输入邮箱"/>
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input type="textarea" v-model="data.form.address" autocomplete="off" placeholder="请输入影院地址"/>
        </el-form-item>
        <el-form-item label="负责人" prop="leader">
          <el-input v-model="data.form.leader" autocomplete="off" placeholder="请输入负责人姓名"/>
        </el-form-item>
        <el-form-item label="身份证号" prop="code">
          <el-input v-model="data.form.code" autocomplete="off" placeholder="请输入负责人身份证号"/>
        </el-form-item>
        <el-form-item label="营业执照" prop="certificate">
          <el-upload :action="baseUrl + '/cinema/upload'" :on-success="handleCertificateUpload" list-type="picture">
            <el-button type="primary">上传影院的营业执照</el-button>
          </el-upload>
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

const baseUrl = import.meta.env.DEV ? 'http://localhost:9090' : import.meta.env.VITE_BASE_API || '';

interface FormData {
  id?: number;
  username?: string;
  avatar?: string;
  name?: string;
  phone?: string;
  email?: string;
  address?: string;
  leader?: string;
  code?: string;
  certificate?: string;
  status?: string;
  role?: string;
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入影院名称', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
  ],
  address: [
    { required: true, message: '请输入影院地址', trigger: 'blur' },
  ],
  leader: [
    { required: true, message: '请输入负责人姓名', trigger: 'blur' },
  ],
  code: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
  ],
  certificate: [
    {required: true, message: '请上传营业执照', trigger: 'change'},
  ]
};

const data = reactive({
  name: null,
  tableData: [] as FormData[],
  pageNumber: 1,
  pageSize: 10,
  leader: null,
  total: 0,
  formVisible: false,
  phone: null,
  form: {} as FormData,
  ids: [] as number[],
  rules: rules
});

const formRef = ref();

const load = () => {
  request.get('/cinema/selectPage', {
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
    console.error('加载数据失败:', error);
    ElMessage.error('加载数据失败，请重试');
  });
}

const handleAdd = () => {
  data.formVisible = true;
  // 初始化空表单数据
  data.form = {} as FormData;
}

const handleUpdate = (row: FormData) => {
  // 使用类型断言确保row是FormData类型
  data.form = JSON.parse(JSON.stringify(row)) as FormData;
  data.formVisible = true;
}

const save = () => {
  // 显式声明valid参数的类型
  formRef.value?.validate((valid: boolean) => {
    if (valid) {
      data.form.id ? update() : add()
    }
  })
}

const add = () => {
  request.post('/cinema/add', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('操作成功')
      data.formVisible = false
      load()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const update = () => {
  request.put('/cinema/update', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('操作成功')
      data.formVisible = false
      load()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const del = (id: number) => {
  ElMessageBox.confirm('删除数据后无法恢复,您确认删除吗?', '删除确认', { type: 'warning' }).then(() => {
    request.delete(`/cinema/delete/${id}`).then(res => {
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
    request.delete('/cinema/deleteBatch', { data: data.ids }).then(res => {
      if (res.code === '200') {
        ElMessage.success('操作成功')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch()
}

const handleSelectionChange = (rows: FormData[]) => {
  data.ids = rows.map(row => row.id).filter((id): id is number => id !== undefined);
  console.log(data.ids);
}

const handleFileUpload = (res: any) => {
  if (res.code === '200') {
    data.form.avatar = res.data;
    ElMessage.success('头像上传成功');
  } else {
    ElMessage.error(res.msg || '头像上传失败');
  }
}

const handleCertificateUpload = (res: any) => {
  if (res.code === '200') {
    data.form.certificate = res.data;
    ElMessage.success('营业执照上传成功');
  } else {
    ElMessage.error(res.msg || '营业执照上传失败');
  }
}

const reset = () => {
  data.name = null;
  load();
}

// 初始加载
load();

// 新增：状态转换函数 - 已审批 → 未审核
const getTransformedStatus = (status: string | undefined) => {
  if (!status) return '未知状态';
  // 核心转换逻辑
  if (status === '未审核') return '待审核';
  // 兼容其他状态别名
  if (status === '审核通过') return '已审核';
  return status;
}

// 优化：状态标签颜色适配（包含新的状态体系）
const getStatusType = (status: string | undefined) => {
  if (!status) return 'info';

  switch (status) {
    case '未审核': return 'warning';    // 原"已审批"转换后
    case '待审核': return 'info';       // 原有状态
    case '已审核': return 'success';    // 原"审核通过"转换后
    case '审核拒绝': return 'danger';   // 原有状态
    case '审核通过': return 'success';  // 兼容原有显示
    case '已审批': return 'warning';    // 兼容原始数据
    default: return 'info';
  }
}

</script>

<style scoped>
.line {
  white-space: nowrap;        /* 禁止文本换行，强制单行显示 */
  overflow: hidden;           /* 超出容器的内容隐藏 */
  text-overflow: ellipsis;    /* 超出部分显示省略号 */
}
</style>