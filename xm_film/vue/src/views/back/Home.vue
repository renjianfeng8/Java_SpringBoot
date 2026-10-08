<template>
  <div class="welcome-card">
    <span>您好！欢迎使用电影购票管理系统！</span>
  </div>

  <div class="list-card">
    <div class="list-header">
      <h2>公告列表</h2>
      <span class="total-count">共 {{ data.total }} 条公告</span>
    </div>

    <el-table stripe :data="data.tableData" border class="field-full" empty-text="暂无匹配的公告数据">
      <el-table-column label="序号" type="index" width="60" align="center" />
      <el-table-column label="公告名称" prop="title" width="220" />
      <el-table-column label="公告内容" prop="content">
        <template #default="scope">
          <div class="content-ellipsis" :title="scope.row.content">
            {{ scope.row.content || '无内容' }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="发布时间" prop="time" width="180" align="center">
        <template #default="scope">
          {{ formatTime(scope.row.time) }}
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive } from "vue";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";
import { API_PATHS, apiPage } from "@/constants";

interface Notice {
  title?: string;
  content?: string;
  time?: string;
}

const data = reactive({
  tableData: [] as Notice[],
  total: 0,
});

const formatTime = (time: string | number | undefined) => {
  if (!time) return "未知时间";

  let date: Date;
  if (typeof time === "number") {
    date = new Date(time);
  } else if (/^\d{13}$/.test(time)) {
    date = new Date(Number(time));
  } else {
    date = new Date(time.replace("T", " "));
  }

  if (isNaN(date.getTime())) return "无效时间";

  const year = date.getFullYear();
  const month = (date.getMonth() + 1).toString().padStart(2, "0");
  const day = date.getDate().toString().padStart(2, "0");
  const hour = date.getHours().toString().padStart(2, "0");
  const minute = date.getMinutes().toString().padStart(2, "0");

  return `${year}-${month}-${day} ${hour}:${minute}`;
};

const load = () => {
  request.get(apiPage(API_PATHS.NOTICES)).then(res => {
    if (res && res.data) {
      data.tableData = res.data.list || [];
      data.total = res.data.total || 0;
    }
  }).catch(error => {
    console.error("加载公告失败:", error);
    ElMessage.error("公告加载失败，请稍后重试");
  });
};

onMounted(() => {
  load();
});
</script>

<style scoped>
.welcome-card {
  padding: var(--space-12) var(--space-20);
  margin-bottom: var(--space-16);
  border-radius: var(--el-border-radius-base);
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
}

.welcome-card span {
  font-size: var(--fs-md);
  color: var(--el-text-color-primary);
  /* 中文文本只能用 400 / 700 */
  font-weight: var(--fw-bold);
}

.list-card {
  padding: var(--space-20);
  border-radius: var(--el-border-radius-base);
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-16);
  padding-bottom: var(--space-12);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.list-header h2 {
  margin: 0;
  font-size: var(--fs-xl);
  color: var(--el-text-color-primary);
  font-weight: var(--fw-bold);
}

.total-count {
  font-size: var(--fs-base);
  color: var(--el-text-color-regular);
}

.content-ellipsis {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: var(--lh-base);
  color: var(--el-text-color-regular);
}
</style>
