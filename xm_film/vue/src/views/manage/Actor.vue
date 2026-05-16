<template>
  <div>
    <div class="card" style="margin-bottom: 5px">
      <el-input v-model="data.actor" placeholder="请输入演员名称查询" style="width: 300px; margin-right:10px" :prefix-icon="Search" />
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
          empty-text="暂无数据"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column label="电影名称" prop="title" />
        <el-table-column label="电影图片" prop="img" >
          <template #default="scope">
            <el-image
                style="width:40px;height:40px;border-radius:5%;object-fit:cover;margin-top: 5px"
                v-if="scope.row.img"
                :src="scope.row.img"
                :preview-src-list="[scope.row.img]"
                preview-teleported
            />
          </template>
        </el-table-column>
        <el-table-column label="演员名称" prop="actor" />
        <el-table-column label="饰演角色" prop="figure" />
        <el-table-column label="演员照片" prop="picture" >
          <template #default="scope">
            <el-image
                style="width:40px;height:40px;border-radius:5%;object-fit:cover;margin-top: 5px"
                v-if="scope.row.picture"
                :src="scope.row.picture"
                :preview-src-list="[scope.row.picture]"
                preview-teleported
            />
          </template>
        </el-table-column>
        <el-table-column label="角色评级" prop="grade">
          <template #default="scope">
            <el-tag :type="getGradeType(scope.row.grade)">
              {{scope.row.grade}}
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
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          v-model:current-page="data.pageNumber"
          v-model:page-size="data.pageSize"
          :page-sizes="[5, 10, 15, 20]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="data.total"
      />
    </div>

    <el-dialog v-model="data.formVisible" title="演职人员信息" width="500" destroy-on-close>
      <el-form ref="formRef" :rules="data.rules" :model="data.form" style="padding-right: 50px;padding-top: 20px" label-width="80px">
        <el-form-item label="电影名称" prop="title">
          <el-input v-model="data.form.title" autocomplete="off" placeholder="请输入电影名称" />
        </el-form-item>
        <el-form-item label="电影图片" prop="img">
          <el-upload
              :action="baseUrl + '/actor/upload'"
              :on-success="handleMovieImgUpload"
              :auto-upload="true"
              list-type="picture"
          >
            <el-button type="primary">点击上传</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="演员名称" prop="actor">
          <el-input v-model="data.form.actor" autocomplete="off" placeholder="请输入演员名称" />
        </el-form-item>
        <el-form-item label="饰演角色" prop="figure">
          <el-input v-model="data.form.figure" autocomplete="off" placeholder="请输入饰演角色名称" />
        </el-form-item>
        <el-form-item label="演员照片" prop="picture">
          <el-upload
              :action="baseUrl + '/actor/upload'"
              :on-success="handleActorImgUpload"
              :auto-upload="true"
              list-type="picture"
          >
            <el-button type="primary">点击上传</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="所属角色" prop="grade">
          <el-select v-model="data.form.grade" placeholder="请选择所属角色">
            <el-option label="导演" value="导演" />
            <el-option label="主演" value="主演" />
            <el-option label="编剧" value="编剧" />
            <el-option label="二级演员" value="二级演员" />
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
import { Delete, Edit, Search } from "@element-plus/icons-vue";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox, FormRules } from "element-plus";

const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:9090';

// 演职人员表单数据类型
interface ActorForm {
  id?: number;
  title?: string;       // 电影名称
  img?: string;         // 电影图片
  actor?: string;       // 主演
  figure?: string;      // 饰演角色
  picture?: string;     // 人员照片
  grade?: string;       // 角色评级
}

// 表单验证规则
const rules: FormRules = {
  title: [
    { required: true, message: '请输入电影名称', trigger: 'blur' }
  ],
  actor: [
    { required: true, message: '请输入主演名称', trigger: 'blur' }
  ],
  figure: [
    { required: true, message: '请输入饰演角色名称', trigger: 'blur' }
  ],
  grade: [
    { required: true, message: '请选择角色评级', trigger: 'change' }
  ]
};

// 响应式数据
const data = reactive({
  actor: '',
  tableData: [] as ActorForm[],
  pageNumber: 1,
  pageSize: 10,
  total: 0,
  formVisible: false,
  form: {} as ActorForm,
  ids: [] as number[],
  rules: rules
});

// 表单引用
const formRef = ref();

// 处理分页大小变化
const handleSizeChange = (newSize: number) => {
  data.pageSize = newSize;
  load();
};

// 处理页码变化
const handleCurrentChange = (newPage: number) => {
  data.pageNumber = newPage;
  load();
};

// 加载演职人员列表
const load = () => {
  request.get('/actor/selectPage', {
    params: {
      pageNum: data.pageNumber,
      pageSize: data.pageSize,
      actor: data.actor
    }
  }).then(res => {
    if (res && res.data) {
      data.tableData = res.data.list || [];
      data.total = res.data.total || 0;
    }
  }).catch(error => {
    console.error('加载演职人员数据失败:', error);
    ElMessage.error('加载数据失败，请重试');
  });
}

// 重置查询条件
const reset = () => {
  data.pageNumber = 1;
  data.actor = '';
  load();
}

// 初始加载数据
load();

// 处理新增演职人员
const handleAdd = () => {
  data.formVisible = true;
  data.form = {} as ActorForm;
}

// 处理保存演职人员
const save = () => {
  formRef.value?.validate((valid: boolean) => {
    if (valid) {
      data.form.id ? update() : add();
    }
  });
}

// 新增演职人员
const add = () => {
  request.post('/actor/add', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('演职人员添加成功');
      data.formVisible = false;
      load();
    } else {
      ElMessage.error(res.msg || '添加失败，请重试');
    }
  }).catch(() => {
    ElMessage.error('添加失败，请检查网络连接');
  });
}

// 更新演职人员
const update = () => {
  request.put('/actor/update', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('演职人员更新成功');
      data.formVisible = false;
      load();
    } else {
      ElMessage.error(res.msg || '更新失败，请重试');
    }
  }).catch(() => {
    ElMessage.error('更新失败，请检查网络连接');
  });
}

// 处理编辑演职人员
const handleUpdate = (row: ActorForm) => {
  data.form = JSON.parse(JSON.stringify(row)) as ActorForm;
  data.formVisible = true;
}

// 删除单个演职人员
const del = (id: number) => {
  ElMessageBox.confirm('删除数据后无法恢复，您确认删除吗?', '删除确认', { type: 'warning' })
      .then(() => {
        request.delete(`/actor/delete/${id}`).then(res => {
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
const handleSelectionChange = (rows: ActorForm[]) => {
  data.ids = rows.map(row => row.id).filter((id): id is number => id !== undefined);
}

// 批量删除演职人员
const delBatch = () => {
  if (data.ids.length === 0) {
    ElMessage.warning('请先选择要删除的演职人员');
    return;
  }
  ElMessageBox.confirm(`确定删除选中的 ${data.ids.length} 条数据吗？删除后无法恢复`, '删除确认', { type: 'warning' })
      .then(() => {
        request.delete('/actor/deleteBatch', { data: data.ids }).then(res => {
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

// 处理电影图片上传
const handleMovieImgUpload = (res: any) => {
  if (res.code === '200') {
    data.form.img = res.data;
    ElMessage.success('电影图片上传成功');
  } else {
    ElMessage.error(res.msg || '电影图片上传失败');
  }
}

// 处理主演照片上传
const handleActorImgUpload = (res: any) => {
  if (res.code === '200') {
    data.form.picture = res.data;
    ElMessage.success('主演照片上传成功');
  } else {
    ElMessage.error(res.msg || '主演照片上传失败');
  }
}

// 获取角色评级对应的标签类型
const getGradeType = (grade: string) => {
  switch (grade) {
    case '导演': return 'warning';
    case '主演': return 'success';
    case '编剧': return 'info';
    case '二级演员': return 'primary';
    default: return 'info';
  }
}
</script>

<style scoped>

</style>