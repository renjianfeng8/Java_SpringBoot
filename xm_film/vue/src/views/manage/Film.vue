<template>
  <div>
    <div class="card" style="margin-bottom: 5px">
      <el-input v-model="data.title" placeholder="请输入电影名称查询" style="width: 300px; margin-right:10px" :prefix-icon="Search"/>
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
        <el-table-column type="expand">
          <template #default="props">

            <el-descriptions title="电影信息" :column="4" border>
              <el-descriptions-item label="电影封面">
                <el-image style="width:36px;height:36px;object-fit:cover;"
                          :src="props.row.img"/>
              </el-descriptions-item>
              <el-descriptions-item label="电影名称">{{props.row.title}}</el-descriptions-item>
              <el-descriptions-item label="英文名称">{{props.row.english}}</el-descriptions-item>
              <el-descriptions-item label="上映日期">{{props.row.start}}</el-descriptions-item>
              <el-descriptions-item label="电影时长">{{props.row.time}}分钟</el-descriptions-item>
              <el-descriptions-item label="电影类型">
                <el-tag v-for="item in props.row.types" style="margin-right: 5px; margin-bottom: 5px" type="info">{{item}}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="电影语言">{{props.row.language}}</el-descriptions-item>
              <el-descriptions-item label="电影分辨率">{{props.row.resolution}}</el-descriptions-item>
              <el-descriptions-item label="电影简介">
                <el-popover placement="top-start" title="电影简介" :width="200" trigger="hover" :content="props.row.content">
                  <template #reference>
                    <div class="line" style="width: 80px">{{props.row.content}}</div>
                  </template>
                </el-popover>
              </el-descriptions-item>
              <el-descriptions-item label="制作公司">
                <el-popover placement="top-start" title="制作公司" :width="200" trigger="hover" :content="props.row.employee">
                  <template #reference>
                    <div class="line" style="width: 100px">{{props.row.employee}}</div>
                  </template>
                </el-popover>
              </el-descriptions-item>
              <el-descriptions-item label="电影区域">{{props.row.areaName}}</el-descriptions-item>
              <el-descriptions-item label="电影状态">
                  <el-tag :type="getStatusType(props.row.status)">
                    {{ props.row.status }}
                  </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="电影评分">
                <el-rate v-model="props.row.score" disabled show-score text-color="#ff9900" score-template="{value} 分"/>
              </el-descriptions-item>
            </el-descriptions>

          </template>
        </el-table-column>
        <el-table-column label="电影名称" prop="title" show-overflow-tooltip/>
        <el-table-column label="英文名称" prop="english" show-overflow-tooltip />
        <el-table-column label="封面" prop="img">
          <template #default="scope">
            <el-image style="margin-top: 7px; width:36px;height:36px;border-radius:10%;object-fit:cover;
                      align-items:center;"
                      v-if="scope.row.img"
                      :src="scope.row.img"
                      :preview-src-list="[scope.row.img]"
                      preview-teleported/>
          </template>
        </el-table-column>
        <el-table-column label="上映日期" prop="start" />
        <el-table-column label="电影时长" prop="time" />
        <el-table-column label="电影类型" prop="types" width="180">
          <template v-slot="scope">
            <el-tag v-for="item in scope.row.types" style="margin-right: 5px; margin-bottom: 5px" type="info">{{item}}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="语言" prop="language" />
        <el-table-column label="电影简介" prop="content" show-overflow-tooltip />
        <el-table-column label="制作区域" prop="areaName" />
        <el-table-column label="电影状态" prop="status">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ scope.row.status }}
            </el-tag>
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

    <el-dialog v-model="data.formVisible" title="电影信息" width="500" destroy-on-close>
      <el-form ref="formRef" :rules="data.rules" :model="data.form" style="padding-right: 50px;padding-top: 20px" label-width="85px">
        <el-form-item label="电影名称" prop="title">
          <el-input v-model="data.form.title" autocomplete="off" placeholder="请输入电影名称"/>
        </el-form-item>
        <el-form-item label="电影封面" prop="img">
          <el-upload
              :action="baseUrl + '/film/upload'"
              :on-success="handleFileUpload"
              :auto-upload="true"
              list-type="picture">
            <el-button type="primary">点击上传电影封面图</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="英文名称" prop="english">
          <el-input v-model="data.form.english" autocomplete="off" placeholder="请输入英文名称"/>
        </el-form-item>
        <el-form-item label="上映日期" prop="start">
          <el-date-picker v-model="data.form.start" type="date" value-format="YYYY-MM-DD"></el-date-picker>
        </el-form-item>
        <el-form-item label="电影时长" prop="time">
          <el-input-number v-model="data.form.time" :min="1" style="width: 220px"/>
        </el-form-item>
        <el-form-item label="电影类型" prop="typeId">
          <el-select v-model="data.form.ids" multiple placeholder="请选择电影类型" style="width: 350px" @change="handleTypeChange">
            <el-option v-for="item in data.typeData" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="电影语言" prop="language">
          <el-select v-model="data.form.language"  placeholder="请选择电影语言" style="width: 350px">
            <el-option label="普通话" value="普通话" />
            <el-option label="英语" value="英语" />
            <el-option label="港语" value="港语" />
            <el-option label="法语" value="法语" />
            <el-option label="日语" value="日语" />
            <el-option label="俄语" value="俄语" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="分辨率" prop="resolution">
          <el-select v-model="data.form.resolution"  placeholder="请选择分辨率" style="width: 350px">
            <el-option label="标准版" value="标准版" />
            <el-option label="2DIMAX" value="2DIMAX" />
            <el-option label="3DIMAX" value="3DIMAX" />
          </el-select>
        </el-form-item>
        <el-form-item label="电影简介" prop="content">
          <el-input type="textarea" :rows="4" v-model="data.form.content" autocomplete="off" placeholder="请输入电影简介"/>
        </el-form-item>
        <el-form-item label="制作公司" prop="employee">
          <el-input v-model="data.form.employee" autocomplete="off" placeholder="请输入制作公司"/>
        </el-form-item>
        <el-form-item label="电影区域" prop="areaId">
          <el-select v-model="data.form.areaId" placeholder="请选择制作区域" style="width: 350px">
            <el-option v-for="item in data.areaData" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="电影状态" prop="status">
         <el-radio-group v-model="data.form.status">
           <el-radio-button label="待上映" value="待上映" />
           <el-radio-button label="已上映" value="已上映" />
           <el-radio-button label="停止上映" value="停止上映" />

         </el-radio-group>
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
  title?: string;       // 电影名称
  english?: string;     // 英文名称
  img?: string;         // 封面图
  start?: string;       // 上映日期
  time?: number;        // 时长
  types?: string[];     // 电影类型（数组）
  language?: string;    // 语言
  content?: string;     // 简介
  areaName?: string;    // 区域名称
  resolution?: string;  // 分辨率
  employee?: string;    // 制作公司
  areaId?: number;      // 区域ID
  status?: string;      // 状态（待上映/已上映/停止上映）
  ids?: number[];       // 类型ID数组（用于表单）
}


interface TypeData {
  id: number;
  title: string;
}


interface AreaData {
  id: number;
  title: string;
}

const rules: FormRules = {
  title: [
    { required: true, message: '请输入电影名称', trigger: 'blur' }
  ],
  start: [
    { required: true, message: '请选择上映日期', trigger: 'change' }
  ],
  time: [
    { required: true, message: '请输入电影时长', trigger: 'blur' }
  ],
  language: [
    { required: true, message: '请选择电影语言', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入电影简介', trigger: 'blur' }
  ],
  areaId: [
    { required: true, message: '请选择制作区域', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择电影状态', trigger: 'change' }
  ]
};

const data = reactive({
  name: null as string | null,
  tableData: [] as FormData[],
  pageNumber: 1,
  pageSize: 10,
  total: 0,
  formVisible: false,
  form: {} as FormData,
  ids: [] as number[],
  typeData: [] as TypeData[],
  areaData: [] as AreaData[],
  rules: rules,
  title: null,
  status: null as string | null
});

const formRef = ref();

const loadType = () => {
  request.get('/type/selectAll').then(res => {
    if(res.code === '200') {
      data.typeData = res.data
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const loadArea = () => {
  request.get('/area/selectAll').then(res => {
    if(res.code === '200') {
      data.areaData = res.data
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const load = () => {
  request.get('/film/selectPage', {
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
  request.post('/film/add', data.form).then(res => {
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
  request.put('/film/update', data.form).then(res => {
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
    request.delete(`/film/delete/${id}`).then(res => {
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
    request.delete('/film/deleteBatch', { data: data.ids }).then(res => {
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
    data.form.img = res.data;
    ElMessage.success('电影封面上传成功');
  } else {
    ElMessage.error(res.msg || '电影封面上传失败');
  }
}

const reset = () => {
  data.title = null;
  load();
}

const handleTypeChange = (val: string[]) => {
  if (val.length > 4) {
    // 截取前4个选项
    data.form.ids = val.slice(0, 4).map(Number);
    ElMessage.warning('最多只能选择4种电影类型');
  } else {
    // 正常转换类型
    data.form.ids = val.map(Number);
  }
};

// 初始加载
load()

loadType()

loadArea()

const getStatusType = (status: string | undefined) => {
  switch (status) {
    case '待上映': return 'warning';
    case '已上映': return 'success';
    case '停止上映': return 'danger';
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

