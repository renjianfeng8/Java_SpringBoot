<template>
  <div>
    <div class="card" style="margin-bottom: 5px">
      <el-input
          v-model="data.title"
          placeholder="请输入电影名称查询"
          style="width: 300px; margin-right:10px"
          :prefix-icon="Search"
      />
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
        <el-table-column type="selection" width="70"/>
        <el-table-column label="电影名称" prop="title" />
        <el-table-column label="电影图片" prop="img">
          <template #default="scope">
            <el-image
                style="margin-top: 7px; width:36px;height:36px;border-radius:10%;object-fit:cover"
                v-if="scope.row.img"
                :src="scope.row.img"
                :preview-src-list="[scope.row.img]"
                preview-teleported
            />
          </template>
        </el-table-column>
        <el-table-column label="视频名称" prop="name" />
        <el-table-column label="预告视频" prop="preview" width="220"> <!-- 宽度稍作调整，容纳按钮 -->
          <template #default="scope">
            <div v-if="scope.row.preview" class="video-wrapper">
              <video
                  :src="scope.row.preview"
                  class="small-video"
                  controls
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" prop="start" />
        <el-table-column label="操作">
          <template #default="scope">
            <el-button
                style="font-size: 18px"
                link
                :icon="Edit"
                @click="handleUpdate(scope.row)"
                type="primary"
            ></el-button>
            <el-button
                style="font-size: 18px"
                link
                :icon="Delete"
                @click="del(scope.row.id)"
                type="danger"
            ></el-button>
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

    <!-- 表单弹窗 -->
    <el-dialog
        v-model="data.formVisible"
        title="电影预告视频"
        width="500"
        destroy-on-close
    >
      <el-form
          ref="formRef"
          :rules="data.rules"
          :model="data.form"
          style="padding-right: 50px;padding-top: 20px"
          label-width="85px"
      >
        <el-form-item label="电影名称" prop="title">
          <el-input v-model="data.form.title" autocomplete="off" placeholder="请输入电影名称"/>
        </el-form-item>
        <el-form-item label="电影图片" prop="img">
          <el-upload
              :action="baseUrl + '/video/upload'"
              :on-success="handleFileUpload"
              :auto-upload="true"
              list-type="picture"
          >
            <el-button type="primary">点击上传电影图片</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="视频名称" prop="name">
          <el-input v-model="data.form.name" autocomplete="off" placeholder="请输入视频名称"/>
        </el-form-item>
        <el-form-item label="发布时间" prop="start">
          <el-date-picker
              v-model="data.form.start"
              type="date"
              value-format="YYYY-MM-DD"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="预告视频" prop="preview">
          <el-upload
              :action="baseUrl + '/video/upload'"
              :on-success="handleVideoUpload"
              :auto-upload="true"
              list-type="text"
          >
            <el-button type="primary">点击上传视频</el-button>
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

// 基础URL配置
const baseUrl = import.meta.env.DEV ? 'http://localhost:9090' : import.meta.env.VITE_BASE_API || '';

// 表单数据类型定义
interface FormData {
  id?: number;
  title?: string;       // 电影名称
  img?: string;         // 封面图
  name?: string;        // 视频名称
  start?: string;       // 发布时间
  preview?: string;     // 视频地址
}

// 表单引用
const formRef = ref();

// 核心数据对象
const data = reactive({
  title: '',            // 搜索框绑定值
  tableData: [] as FormData[],  // 表格数据
  pageNumber: 1,        // 当前页码
  pageSize: 10,         // 每页条数
  total: 0,             // 总条数
  formVisible: false,   // 弹窗显示状态
  form: {} as FormData, // 表单数据
  rules: {              // 表单验证规则
    title: [
      { required: true, message: '请输入电影名称', trigger: 'blur' }
    ],
    start: [
      { required: true, message: '请选择上映日期', trigger: 'change' }
    ],
  } as FormRules,
  ids: [] as number[]    // 批量选择的ID数组
});

// 加载表格数据
const load = () => {
  request.get('/video/selectPage', {
    params: {
      pageNum: data.pageNumber,
      pageSize: data.pageSize,
      title: data.title,
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
};

// 新增按钮处理
const handleAdd = () => {
  data.formVisible = true;
  data.form = {} as FormData;  // 清空表单
};

// 编辑按钮处理
const handleUpdate = (row: FormData) => {
  data.form = JSON.parse(JSON.stringify(row));  // 深拷贝行数据
  data.formVisible = true;
};

// 保存表单
const save = () => {
  formRef.value?.validate((valid: boolean) => {
    if (valid) {
      data.form.id ? update() : add();
    }
  });
};

// 新增数据
const add = () => {
  request.post('/video/add', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('新增成功');
      data.formVisible = false;
      load();
    } else {
      ElMessage.error(res.msg || '新增失败');
    }
  });
};

// 更新数据
const update = () => {
  request.put('/video/update', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('更新成功');
      data.formVisible = false;
      load();
    } else {
      ElMessage.error(res.msg || '更新失败');
    }
  });
};

// 删除单条数据
const del = (id: number) => {
  ElMessageBox.confirm(
      '删除数据后无法恢复，您确认删除吗?',
      '删除确认',
      { type: 'warning' }
  ).then(() => {
    request.delete(`/video/delete/${id}`).then(res => {
      if (res.code === '200') {
        ElMessage.success('删除成功');
        load();
      } else {
        ElMessage.error(res.msg || '删除失败');
      }
    });
  }).catch(() => {});
};

// 批量删除
const delBatch = () => {
  if (data.ids.length === 0) {
    ElMessage.warning('请选择需要删除的数据');
    return;
  }
  ElMessageBox.confirm(
      '删除数据后无法恢复，您确认删除吗?',
      '删除确认',
      { type: 'warning' }
  ).then(() => {
    request.delete('/video/deleteBatch', { data: data.ids }).then(res => {
      if (res.code === '200') {
        ElMessage.success('批量删除成功');
        load();
        data.ids = [];
      } else {
        ElMessage.error(res.msg || '批量删除失败');
      }
    });
  }).catch(() => {});
};

// 图片上传成功回调
const handleFileUpload = (res: any) => {
  if (res.code === '200') {
    data.form.img = res.data;
    ElMessage.success('图片上传成功');
  } else {
    ElMessage.error(res.msg || '图片上传失败');
  }
};

// 视频上传成功回调
const handleVideoUpload = (res: any) => {
  if (res.code === '200') {
    data.form.preview = res.data;
    ElMessage.success('视频上传成功');
  } else {
    ElMessage.error(res.msg || '视频上传失败');
  }
};

// 处理表格选择变化
const handleSelectionChange = (rows: FormData[]) => {
  data.ids = rows
      .map(row => row.id)
      .filter((id): id is number => typeof id === 'number');
};

// 重置搜索条件
const reset = () => {
  data.title = '';
  data.pageNumber = 1;
  load();
};

// 分页大小变化
const handleSizeChange = (newSize: number) => {
  data.pageSize = newSize;
  load();
};

// 页码变化
const handleCurrentChange = (newPage: number) => {
  data.pageNumber = newPage;
  load();
};


// 初始加载数据
load();

</script>

<style scoped>

.small-video {
  width: 100%;
  height: 100%;
  object-fit: cover; /* 保持视频比例填充容器 */
  cursor: pointer;
}

</style>