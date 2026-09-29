<template>
  <div>
    <div class="card page-card">
      <el-input v-model="data.title" placeholder="请输入电影名称查询" class="search-input" :prefix-icon="Search"/>
      <el-button type="primary" @click="load">查 询</el-button>
      <el-button type="warning" @click="reset">重 置</el-button>
    </div>

    <div class="card page-card">
      <el-table stripe :data="data.tableData">
        <el-table-column type="expand">
          <template #default="props">

            <el-descriptions title="电影信息" :column="4" border>
              <el-descriptions-item label="电影封面">
                <el-image class="cell-thumb"
                          :src="props.row.img"/>
              </el-descriptions-item>
              <el-descriptions-item label="电影名称">{{props.row.title}}</el-descriptions-item>
              <el-descriptions-item label="英文名称">{{props.row.english}}</el-descriptions-item>
              <el-descriptions-item label="上映日期">{{props.row.start}}</el-descriptions-item>
              <el-descriptions-item label="电影时长">{{props.row.time}}分钟</el-descriptions-item>
              <el-descriptions-item label="电影类型">
                <el-tag v-for="item in props.row.typeList" class="tag-gap" type="info">{{item.title}}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="电影语言">{{props.row.language}}</el-descriptions-item>
              <el-descriptions-item label="电影分辨率">{{props.row.resolution}}</el-descriptions-item>
              <el-descriptions-item label="电影简介">
                <el-popover placement="top-start" title="电影简介" :width="200" trigger="hover" :content="props.row.content">
                  <template #reference>
                    <div class="line line--content">{{props.row.content}}</div>
                  </template>
                </el-popover>
              </el-descriptions-item>
              <el-descriptions-item label="制作公司">
                <el-popover placement="top-start" title="制作公司" :width="200" trigger="hover" :content="props.row.employee">
                  <template #reference>
                    <div class="line line--employee">{{props.row.employee}}</div>
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
                <el-rate v-if="props.row.score != null" v-model="props.row.score" disabled show-score text-color="var(--color-rating-text)" score-template="{value} 分"/>
                <span v-else>暂无评分</span>
              </el-descriptions-item>
            </el-descriptions>

          </template>
        </el-table-column>
        <el-table-column label="电影名称" prop="title"/>
        <el-table-column label="英文名称" prop="english" show-overflow-tooltip />
        <el-table-column label="封面" prop="img">
          <template #default="scope">
            <el-image class="cell-thumb"
                      v-if="scope.row.img"
                      :src="scope.row.img"
                      :preview-src-list="[scope.row.img]"
                      preview-teleported/>
          </template>
        </el-table-column>
        <el-table-column label="上映日期" prop="start" show-overflow-tooltip />
        <el-table-column label="电影时长" prop="time" />
        <el-table-column label="电影类型" prop="typeList" width="180">
          <template v-slot="scope">
            <el-tag v-for="item in scope.row.typeList" class="tag-gap" type="info">{{item.title}}</el-tag>
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

  </div>
</template>

<script setup lang="ts">
import { reactive } from "vue";
import { Search } from "@element-plus/icons-vue";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";
import { API_PATHS, apiPage, getFilmStatusType as getStatusType } from "@/constants";

interface FormData {
  id?: number;
  title?: string;       // 电影名称
  english?: string;     // 英文名称
  img?: string;         // 封面图
  start?: string;       // 上映日期
  time?: number;        // 时长
  typeList?: Array<{ id: number; title: string }>;  // 电影类型（后端已解析）
  language?: string;    // 语言
  content?: string;     // 简介
  areaName?: string;    // 区域名称
  resolution?: string;  // 分辨率
  employee?: string;    // 制作公司
  areaId?: number;      // 区域ID
  status?: string;      // 状态（待上映/已上映/停止上映）
}


const data = reactive({
  tableData: [] as FormData[],
  pageNumber: 1,
  pageSize: 10,
  total: 0,
  title: null
});

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

const reset = () => {
  data.title = null;
  load();
}

// 初始加载
load()


</script>

<style scoped>
.line {
  white-space: nowrap;        /* 禁止文本换行，强制单行显示 */
  overflow: hidden;           /* 超出容器的内容隐藏 */
  text-overflow: ellipsis;    /* 超出部分显示省略号 */
}

</style>


