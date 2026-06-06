<template>
  <div>
    <div class="card" style="margin-bottom: 5px">
      <el-input v-model="data.title" placeholder="请输入电影名称查询" style="width: 300px; margin-right:10px" :prefix-icon="Search"/>
      <el-button type="primary" @click="load">查 询</el-button>
      <el-button type="warning" @click="reset">重 置</el-button>
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
        <el-table-column label="电影名称" prop="title"/>
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
        <el-table-column label="上映日期" prop="start" show-overflow-tooltip />
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

  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { Search } from "@element-plus/icons-vue";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";
import { API_PATHS, apiPage } from "@/constants";

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
  title: null,
  status: null as string | null
});

const formRef = ref();

const loadType = () => {
  request.get(API_PATHS.TYPES).then(res => {
    if(res.code === '200') {
      data.typeData = res.data
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const loadArea = () => {
  request.get(API_PATHS.AREAS).then(res => {
    if(res.code === '200') {
      data.areaData = res.data
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const load = () => {
  request.get(apiPage(API_PATHS.FILMS), {
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

const handleSelectionChange = (rows: FormData[]) => {
  data.ids = rows.map(row => row.id).filter((id): id is number => id !== undefined);
}

const reset = () => {
  data.title = null;
  load();
}

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


