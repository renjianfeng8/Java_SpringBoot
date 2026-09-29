<template>
  <div class="home-container">
    <!-- ECharts 数据可视化分析区域 -->
    <div class="card mb-2">
      <div class="section-title section-title--spaced">数据可视化分析</div>
      <el-row :gutter="24">
        <!-- 影院状态分布饼图 -->
        <el-col :span="12">
          <div class="chart-card">
            <div class="chart-title">影院状态分布</div>
            <div v-if="hasCinemaStatus" ref="cinemaStatusChart" class="chart-content"></div>
            <div v-else class="chart-empty">暂无影院数据</div>
          </div>
        </el-col>
        <!-- 电影类型占比柱状图 -->
        <el-col :span="12">
          <div class="chart-card">
            <div class="chart-title">电影类型占比</div>
            <div v-if="hasFilmType" ref="filmTypeChart" class="chart-content"></div>
            <div v-else class="chart-empty">暂无电影数据</div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 核心功能入口区域 -->
    <div class="card mb-2">
      <div class="section-title-wrapper">
        <div class="title-tag"></div>
        <div class="section-title">核心功能入口</div>
      </div>
      <el-row :gutter="24">
        <!-- 影院管理 -->
        <el-col :span="6">
          <el-card class="function-card" @click="goToPage('/manage/cinema')" hoverable>
            <div class="card-content">
              <div class="card-title">影院管理</div>
              <div class="card-desc">新增、编辑、审核影院信息</div>
            </div>
          </el-card>
        </el-col>
        <!-- 电影管理 -->
        <el-col :span="6">
          <el-card class="function-card" @click="goToPage('/manage/film')" hoverable>
            <div class="card-content">
              <div class="card-title">电影管理</div>
              <div class="card-desc">维护电影信息、封面与排片</div>
            </div>
          </el-card>
        </el-col>
        <!-- 用户管理 -->
        <el-col :span="6">
          <el-card class="function-card" @click="goToPage('/manage/user')" hoverable>
            <div class="card-content">
              <div class="card-title">用户管理</div>
              <div class="card-desc">管理平台用户与权限分配</div>
            </div>
          </el-card>
        </el-col>
        <!-- 购票记录 -->
        <el-col :span="6">
          <el-card class="function-card" @click="goToPage('/manage/ordered')" hoverable>
            <div class="card-content">
              <div class="card-title">购票记录</div>
              <div class="card-desc">查看和管理用户的购票订单信息</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted, onUnmounted, computed, ref, watch, nextTick } from "vue";
import { useRouter } from "vue-router";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";
import * as echarts from 'echarts/core';
import { BarChart, PieChart } from 'echarts/charts';
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components';
import { LabelLayout } from 'echarts/features';
import { CanvasRenderer } from 'echarts/renderers';
import { STATISTICS_API } from '@/constants';

echarts.use([
  BarChart,
  PieChart,
  GridComponent,
  LegendComponent,
  TooltipComponent,
  LabelLayout,
  CanvasRenderer,
]);

// 路由实例
const router = useRouter();

// 跳转页面方法
const goToPage = (path: string) => {
  router.push(path);
}

// ECharts 容器引用
const cinemaStatusChart = ref<HTMLElement | null>(null);
const filmTypeChart = ref<HTMLElement | null>(null);

// 统一的页面resize处理函数（避免重复添加/累积监听器）
const handleResize = () => {
  const statusChart = echarts.getInstanceByDom(cinemaStatusChart.value!);
  if (statusChart) statusChart.resize();
  const typeChart = echarts.getInstanceByDom(filmTypeChart.value!);
  if (typeChart) typeChart.resize();
};

// ECharts 用 canvas 渲染，不解析 CSS 变量，只能在运行期把令牌值读出来（规范 §3.7）
const cssVar = (name: string, fallback = '') =>
  getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback;

// 大盘统计：数值全部来自 /statistics/overview 的实时聚合，前端不再拉全表自己算
const stats = reactive({
  cinemaStatus: [] as Array<{ name: string; value: number }>,
  filmType: [] as Array<{ name: string; value: number }>
});

// 图表是否有真实数据。无数据时渲染「暂无数据」占位，不再用假数据填充 ——
// 画一张有数据的图会让人以为系统里真有那些影院 / 电影（规范 §608）。
const hasCinemaStatus = computed(() => stats.cinemaStatus.length > 0);
const hasFilmType = computed(() => stats.filmType.length > 0);

const loadStats = async () => {
  try {
    const res = await request.get(STATISTICS_API.OVERVIEW);
    if (res.code === '200') {
      stats.cinemaStatus = res.data?.cinemaStatus || [];
      stats.filmType = res.data?.filmType || [];
    } else {
      ElMessage.error(res.msg);
    }
  } catch (error) {
    // 网络异常的统一提示由 request.js 响应拦截器给出；这里保持空数据 → 图表显示占位
    console.error('统计接口请求异常：', error);
  }
};

// 初始化影院状态饼图
const initCinemaStatusChart = () => {
  if (!cinemaStatusChart.value) return;

  // 销毁旧实例
  const chartInstance = echarts.getInstanceByDom(cinemaStatusChart.value);
  if (chartInstance) {
    chartInstance.dispose();
  }

  const chart = echarts.init(cinemaStatusChart.value);
  // 状态配色取自令牌（§3.3 功能色），不再自成一表
  const statusColorMap: Record<string, string> = {
    '已审核': cssVar('--el-color-success'),
    '未审核': cssVar('--el-color-warning')
  };

  // 后端已按 status 分组，这里只做配色映射
  const pieData = stats.cinemaStatus.map((item) => ({
    name: item.name,
    value: item.value,
    itemStyle: {
      color: statusColorMap[item.name] || cssVar('--el-text-color-secondary')
    }
  }));

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      textStyle: { fontSize: 12 }
    },
    series: [
      {
        name: '影院状态',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 4,
          borderColor: cssVar('--el-bg-color'),
          borderWidth: 2
        },
        label: { show: false, position: 'center' },
        emphasis: {
          label: { show: true, fontSize: 16, fontWeight: 'bold' }
        },
        labelLine: { show: false },
        data: pieData
      }
    ]
  };

  chart.setOption(option, true);
};



// 初始化电影类型柱状图
const initFilmTypeChart = () => {
  if (!filmTypeChart.value) return;
  // 销毁旧实例
  const chartInstance = echarts.getInstanceByDom(filmTypeChart.value);
  if (chartInstance) {
    chartInstance.dispose();
  }
  const chart = echarts.init(filmTypeChart.value);
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: [
      {
        type: 'category',
        data: stats.filmType.map((item) => item.name),
        axisTick: { alignWithLabel: true },
        axisLabel: { fontSize: 12, rotate: 30 }
      }
    ],
    yAxis: [
      {
        type: 'value',
        name: '影片数量',
        min: 0
      }
    ],
    series: [
      {
        name: '电影数量',
        type: 'bar',
        barWidth: '60%',
        data: stats.filmType.map((item) => item.value),
        itemStyle: { borderRadius: 4 }
      }
    ],
    color: [cssVar('--el-color-primary')]
  };
  chart.setOption(option, true);
};

// 初始化页面所有数据
const initData = async () => {
  await loadStats();

  // 等待DOM更新后初始化图表
  await nextTick();
  initCinemaStatusChart();
  initFilmTypeChart();
};

// 监听数据变化更新图表
watch(
    [() => stats.cinemaStatus, () => stats.filmType],
    async () => {
      await nextTick();
      initCinemaStatusChart();
      initFilmTypeChart();
    },
    { deep: true }
);

// 页面挂载初始化
onMounted(() => {
  initData();
  window.addEventListener('resize', handleResize);
});

// 页面卸载时移除resize监听器
onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
});
</script>

<style scoped>
.card {
  padding: var(--space-12);
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
}

.mb-2 {
  margin-bottom: var(--space-8);
}

.home-container {
  min-height: 100vh;
  padding: var(--space-20);
}

.section-title {
  font-size: var(--fs-lg);
  font-weight: var(--fw-bold);
  color: var(--el-text-color-primary);
}

.chart-card {
  height: 400px;
  padding: var(--space-20);
  border-radius: var(--el-border-radius-base);
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
}

.chart-title {
  margin-bottom: var(--space-16);
  font-size: var(--fs-md);
  color: var(--el-text-color-primary);
}

.chart-content {
  width: 100%;
  height: calc(100% - 40px);
}

/* 无真实数据时的占位（不再用假数据把图表填满） */
.chart-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: calc(100% - 40px);
  color: var(--el-text-color-regular);
}

.section-title-wrapper {
  display: flex;
  align-items: center;
  margin-bottom: var(--space-20);
}

/* 标题前的竖条：装饰图形，用品牌色（后台默认蓝） */
.title-tag {
  width: 4px;
  height: 20px;
  margin-right: var(--space-12);
  border-radius: var(--el-border-radius-small);
  background: var(--color-brand);
}

.function-card {
  height: 100%;
  border: none;
  border-radius: var(--el-border-radius-base);
  box-shadow: var(--el-box-shadow-lighter);
  transition: box-shadow 200ms ease-in-out;
  cursor: pointer;
}

.card-content {
  padding: var(--space-32) 0;
  text-align: center;
}

.card-title {
  margin-bottom: var(--space-8);
  font-size: var(--fs-lg);
  font-weight: var(--fw-bold);
  color: var(--el-text-color-primary);
}

/* 14px 正文须达 4.5:1；--el-text-color-secondary 只有 3.08:1 */
.card-desc {
  font-size: var(--fs-base);
  line-height: var(--lh-base);
  color: var(--el-text-color-regular);
}
</style>
