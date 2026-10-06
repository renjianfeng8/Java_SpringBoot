<template>
  <div class="dashboard">
    <!-- ECharts 数据可视化分析区域 -->
    <section class="dashboard__section">
      <div class="section-head">
        <h2 class="section-head__title">数据可视化分析</h2>
      </div>

      <div class="chart-grid">
        <div class="card chart-card">
          <h3 class="chart-card__title">影院状态分布</h3>
          <div v-if="hasCinemaStatus" ref="cinemaStatusChart" class="chart-card__body"></div>
          <div v-else class="empty-hint">暂无影院数据</div>
        </div>

        <div class="card chart-card">
          <h3 class="chart-card__title">电影类型占比</h3>
          <div v-if="hasFilmType" ref="filmTypeChart" class="chart-card__body"></div>
          <div v-else class="empty-hint">暂无电影数据</div>
        </div>
      </div>
    </section>

    <!-- 核心功能入口区域 -->
    <section class="dashboard__section">
      <div class="section-head">
        <h2 class="section-head__title">核心功能入口</h2>
      </div>

      <div class="entry-grid">
        <router-link v-for="entry in entries" :key="entry.path" class="entry-card" :to="entry.path">
          <el-icon class="entry-card__icon"><component :is="entry.icon" /></el-icon>
          <span class="entry-card__title">{{ entry.title }}</span>
          <span class="entry-card__desc">{{ entry.desc }}</span>
        </router-link>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted, onUnmounted, computed, ref, watch, nextTick } from "vue";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";
import { CreditCard, OfficeBuilding, User, VideoCamera } from "@element-plus/icons-vue";
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

/* 功能入口用 router-link 渲染（真 <a href>，天然进 Tab 序、回车可激活）。
   图标与侧栏给同一目的地分配的图标一致，入口卡顺带教了导航。 */
const entries = [
  { path: '/manage/cinema', icon: OfficeBuilding, title: '影院管理', desc: '新增、编辑、审核影院信息' },
  { path: '/manage/film', icon: VideoCamera, title: '电影管理', desc: '维护电影信息、封面与排片' },
  { path: '/manage/user', icon: User, title: '用户管理', desc: '管理平台用户与权限分配' },
  { path: '/manage/ordered', icon: CreditCard, title: '购票记录', desc: '查看和管理用户的购票订单信息' },
]

// ECharts 容器引用
const cinemaStatusChart = ref<HTMLElement | null>(null);
const filmTypeChart = ref<HTMLElement | null>(null);

// 统一的页面 resize 处理函数（避免重复添加/累积监听器）。
// 图表为空态时容器是 v-if 掉的、ref 为 null，必须先挡掉再问 ECharts 要实例。
const handleResize = () => {
  if (cinemaStatusChart.value) {
    echarts.getInstanceByDom(cinemaStatusChart.value)?.resize();
  }
  if (filmTypeChart.value) {
    echarts.getInstanceByDom(filmTypeChart.value)?.resize();
  }
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
// 画一张有数据的图会让人以为系统里真有那些影院 / 电影（规范 §11.2）。
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
          // 图表垫在白色 .card 上，饼图切片的分隔线取卡片底色
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

  // 等待 DOM 更新后初始化图表
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

// 页面卸载时移除 resize 监听器并销毁图表实例。
// 不销毁的话，ECharts 内部注册表仍持有画布 DOM 与 canvas —— v-if 摘掉容器后
// 那段 DOM 已经不是页面的一部分，实例却继续挂着。
onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  [cinemaStatusChart.value, filmTypeChart.value].forEach((el) => {
    if (el) echarts.getInstanceByDom(el)?.dispose();
  });
});
</script>

<style scoped>
/* 页面底色与内边距归外壳（.manage-content 已铺灰底 + 16px 内边距），
   这里再声明 min-height: 100vh 会凭空多出外壳头部的高度、把页脚顶出视口（规则 81）。 */
.dashboard {
  display: flex;
  flex-direction: column;
  gap: var(--space-24);
}

/* 区块内部（标题带 → 内容）比区块之间更紧，读起来才是一组 */
.dashboard__section {
  display: flex;
  flex-direction: column;
  gap: var(--space-16);
}

/* minmax(0, 1fr) 的 0 下限是承重件：裸 1fr 的 min-width 是 auto，
   会被 ECharts 画布的固定像素宽撑破，右侧那张卡就会溢出容器。 */
.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-16);
}

.chart-card {
  display: flex;
  flex-direction: column;
  padding: var(--space-16);
}

/* 标题含中文，字重只能用 400 / 700（§4.4）。 */
.chart-card__title {
  margin: 0 0 var(--space-16);
  font-size: var(--fs-md);
  font-weight: var(--fw-bold);
  color: var(--el-text-color-primary);
}

/* 固定高度而非 calc(100% - 40px)：ECharts 初始化需要一个确定高度的画布盒，
   减去"猜出来的标题高度"会在标题折行时算错。 */
.chart-card__body {
  height: 320px;
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: var(--space-16);
}

/* 入口卡是 <a>（router-link），要显式去掉下划线并继承文字色。
   hover 只改边框与标题色，不叠阴影 —— 全局 .card 已常驻 --el-box-shadow-lighter，
   再叠一层阴影需要先把全部 .card 降档，会外溢到其他端。 */
.entry-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-8);
  padding: var(--space-24);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-bg-color);
  color: inherit;
  text-decoration: none;
  transition: border-color 100ms ease-out;
}

.entry-card:hover {
  border-color: var(--el-color-primary);
}

.entry-card__icon {
  font-size: 24px;
  color: var(--el-color-primary);
}

.entry-card:hover .entry-card__title {
  color: var(--el-color-primary);
}

.entry-card__title {
  font-size: var(--fs-md);
  font-weight: var(--fw-bold);
  color: var(--el-text-color-primary);
  transition: color 100ms ease-out;
}

/* 14px 正文须达 4.5:1；--el-text-color-secondary 只有 3.08:1 */
.entry-card__desc {
  font-size: var(--fs-base);
  line-height: var(--lh-loose);
  color: var(--el-text-color-regular);
}
</style>
