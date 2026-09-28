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
            <div ref="cinemaStatusChart" class="chart-content"></div>
          </div>
        </el-col>
        <!-- 电影类型占比柱状图 -->
        <el-col :span="12">
          <div class="chart-card">
            <div class="chart-title">电影类型占比</div>
            <div ref="filmTypeChart" class="chart-content"></div>
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
import { API_PATHS } from '@/constants';

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

// ECharts 用 canvas 渲染，不解析 CSS 变量，只能在运行期把令牌值读出来（规范 §3.1）
const cssVar = (name: string, fallback = '') =>
  getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback;

// 页面核心数据
const data = reactive({
  cinemaList: [] as Array<{ status: string }>,
  filmList: [] as Array<{ type_ids: number[] | string, typeIds?: number[] | string }>,
  typeList: [] as Array<{ id: number; title: string }>
});

// 影院状态统计数据
const cinemaStatusData = computed(() => {
  // 后端 CinemaStatus 只有「未审核 / 已审核」（见 constants 的 CINEMA_STATUS），
  // 原先列的「审核中 / 已驳回 / 已下架」在系统里不存在，恒为 0
  const statusMap: Record<string, number> = {
    '已审核': 0,
    '未审核': 0
  };

  // 遍历影院数据统计状态
  data.cinemaList.forEach((cinema) => {
    if (!cinema.status) return;

    // 状态转换：已审批 → 未审核
    let status = cinema.status === '已审批' ? '未审核' : cinema.status;

    if (statusMap.hasOwnProperty(status)) {
      statusMap[status]++;
    } else {
      statusMap['其他'] = (statusMap['其他'] || 0) + 1;
    }
  });

  // 兜底：如果所有状态都是0，添加默认数据避免图表空白
  const hasData = Object.values(statusMap).some(v => v > 0);
  if (!hasData) {
    statusMap['已审核'] = 1;
    statusMap['未审核'] = 1;
  }

  return {
    labels: Object.keys(statusMap),
    values: Object.values(statusMap)
  };
});

// 动态创建类型ID到名称的映射
const getTypeIdToName = () => {
  const typeMap: Record<number, string> = {};
  data.typeList.forEach(type => {
    typeMap[type.id] = type.title;
  });
  return typeMap;
};

// 电影类型统计数据
const filmTypeData = computed(() => {
  const typeIdToName = getTypeIdToName();
  const typeMap: Record<string, number> = {};

  data.filmList.forEach(film => {
    let typeIds: number[] = [];

    // 兼容 typeIds/type_ids 两种字段名
    const typeIdField = film.typeIds !== undefined ? film.typeIds : film.type_ids;

    if (typeof typeIdField === 'string') {
      try {
        const cleanStr = typeIdField.trim();
        typeIds = JSON.parse(cleanStr);
      } catch (e) {
        const numbers = typeIdField.match(/\d+/g) || [];
        typeIds = numbers.map(Number);
      }
    } else if (Array.isArray(typeIdField)) {
      typeIds = typeIdField;
    }

    typeIds = typeIds.filter(id => !isNaN(Number(id))).map(Number);

    typeIds.forEach(id => {
      const typeName = typeIdToName[id] || `未知类型(${id})`;
      typeMap[typeName] = (typeMap[typeName] || 0) + 1;
    });
  });

  // 兜底：如果没有统计到任何类型，添加默认数据
  if (Object.keys(typeMap).length === 0) {
    typeMap['剧情'] = 5;
    typeMap['动作'] = 3;
    typeMap['爱情'] = 2;
  }

  return {
    labels: Object.keys(typeMap),
    values: Object.values(typeMap)
  };
});

// 初始化影院状态饼图
const initCinemaStatusChart = () => {
  if (!cinemaStatusChart.value) return;

  // 销毁旧实例
  const chartInstance = echarts.getInstanceByDom(cinemaStatusChart.value);
  if (chartInstance) {
    chartInstance.dispose();
  }

  const chart = echarts.init(cinemaStatusChart.value);
  // 状态配色取自令牌（§2.5 功能色），不再自成一表
  const statusColorMap: Record<string, string> = {
    '已审核': cssVar('--el-color-success'),
    '未审核': cssVar('--el-color-warning'),
    '其他': cssVar('--el-text-color-secondary')
  };

  // 构建饼图数据
  const pieData = cinemaStatusData.value.labels
      .map((label, index) => ({
        name: label,
        value: cinemaStatusData.value.values[index],
        itemStyle: {
          color: statusColorMap[label] || cssVar('--el-text-color-secondary')
        }
      }))
      .filter(item => item.value > 0);

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
        data: filmTypeData.value.labels,
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
        data: filmTypeData.value.values,
        itemStyle: { borderRadius: 4 }
      }
    ],
    color: [cssVar('--el-color-primary')]
  };
  chart.setOption(option, true);
};

// 加载电影列表数据
const getFilmList = async () => {
  try {
    const res = await request.get(API_PATHS.FILMS);
    data.filmList = res.data || [];
  } catch (error) {
    ElMessage.warning("电影数据加载失败，不影响核心功能使用");
  }
};

// 初始化页面所有数据
const initData = async () => {
  try {
    const cinemaRes = await request.get(API_PATHS.CINEMAS);
    data.cinemaList = cinemaRes.data || [];

    const typeRes = await request.get(API_PATHS.TYPES);
    data.typeList = typeRes.data || [];

    // 加载电影数据
    await getFilmList();

    // 等待DOM更新后初始化图表
    await nextTick();
    initCinemaStatusChart();
    initFilmTypeChart();
  } catch (error) {
    ElMessage.error("系统数据加载异常，请刷新页面重试");

    // 即使接口失败，也初始化图表（使用兜底数据）
    await nextTick();
    initCinemaStatusChart();
    initFilmTypeChart();
  }
};

// 监听数据变化更新图表
watch(
    [cinemaStatusData, filmTypeData],
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
