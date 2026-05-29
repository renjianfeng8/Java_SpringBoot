<template>
    <!-- 欢迎提示栏 -->
    <div class="welcome-card">
      <span>您好！欢迎使用电影购票管理系统！</span>
    </div>
      <!-- 公告列表 -->
      <div class="list-card">
        <div class="list-header">
          <h2>公告列表</h2>
          <span class="total-count">共 {{ data.total }} 条公告</span>
        </div>

        <!-- 表格展示 -->
        <el-table stripe :data="data.tableData" border style="width: 100%" empty-text="暂无匹配的公告数据">
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
              {{ formatTime(scope.row.time) }} <!-- 格式化时间显示 -->
            </template>
          </el-table-column>
        </el-table>


      </div>
</template>

<script setup lang="ts">
import { reactive, onMounted } from "vue";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";

// 公告数据类型定义
interface Notice {
  id?: number;
  title?: string;
  content?: string;
  time?: string; // 时间格式：YYYY-MM-DD HH:mm:ss 等
}

// 响应式数据（仅保留查询、列表、分页相关）
const data = reactive({
  tableData: [] as Notice[], // 公告列表数据
  total: 0,      // 总公告数
  query: { title: undefined } // 搜索条件（公告名称）
});

// 时间格式化函数（解决时间显示问题）
const formatTime = (time: string | number | undefined) => {
  if (!time) return "未知时间";
  let date: Date;
  // 处理不同格式的时间输入
  if (typeof time === "string") {
    // 转换 ISO 格式（如 "2024-05-01T12:00:00"）为标准格式
    time = time.replace("T", " ");
    // 处理时间戳字符串（如 "1690000000000"）
    if (/^\d{13}$/.test(time)) {
      date = new Date(Number(time));
    } else {
      date = new Date(time);
    }
  } else if (typeof time === "number") {
    // 处理数字时间戳
    date = new Date(time);
  } else {
    return "未知时间";
  }

  // 验证时间有效性
  if (isNaN(date.getTime())) return "无效时间";

  // 格式化输出为「YYYY-MM-DD HH:mm」（去掉秒数，简洁易读）
  const year = date.getFullYear();
  const month = (date.getMonth() + 1).toString().padStart(2, "0");
  const day = date.getDate().toString().padStart(2, "0");
  const hour = date.getHours().toString().padStart(2, "0");
  const minute = date.getMinutes().toString().padStart(2, "0");

  return `${year}-${month}-${day} ${hour}:${minute}`;
};

// 加载公告列表（支持分页和搜索）
const load = () => {
  request.get("/api/v1/notices/page", {
    params: {
      title: data.query.title // 搜索条件（模糊匹配公告名称）
    }
  }).then(res => {
    if (res && res.data) {
      data.tableData = res.data.list || [];
      data.total = res.data.total || 0;
    }
  }).catch(error => {
    console.error("加载公告失败:", error);
    ElMessage.error("公告加载失败，请稍后重试");
  });
};

// 页面挂载时初始化加载公告
onMounted(() => {
  load();
});
</script>

<style scoped>


/* 欢迎提示栏 */
.welcome-card {
  background: #fff;
  border-radius: 8px;
  padding: 12px 20px;
  margin-bottom: 15px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

.welcome-card span {
  font-size: 16px;
  color: #333;
  font-weight: 500;
}

/* 列表卡片 */
.list-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

/* 列表头部（标题+总数） */
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.list-header h2 {
  font-size: 20px;
  color: #2c3e50;
  margin: 0;
  font-weight: 600;
}

.total-count {
  font-size: 14px;
  color: #606266;
}

/* 表格内容省略（最多显示2行） */
.content-ellipsis {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.6;
  color: #606266;
}


</style>
