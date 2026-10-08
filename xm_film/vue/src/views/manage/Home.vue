<template>
  <div class="dashboard">

    <!-- ① 核心指标 -->
    <section class="dashboard__section" aria-label="核心指标">
      <div class="kpi-grid" :aria-busy="loading.overview">
        <div v-for="card in kpiCards" :key="card.key" class="card kpi-card">
          <div class="kpi-card__head">
            <el-icon class="kpi-card__icon" aria-hidden="true"><component :is="card.icon" /></el-icon>
            <span class="kpi-card__label">{{ card.label }}</span>
          </div>

          <p v-if="card.value !== null" class="kpi-card__value">{{ card.value }}</p>
          <p v-else-if="failed.overview" class="kpi-card__failed">数据加载失败，请稍后重试</p>
          <div v-else class="kpi-card__skeleton"><el-skeleton :rows="1" animated /></div>

          <p v-if="card.foot && card.value !== null" class="kpi-card__foot">
            <template v-if="card.foot.kind === 'delta'">
              <span v-if="card.foot.value === null">较昨日 —</span>
              <template v-else>
                <!-- 箭头是装饰（aria-hidden），方向由后面的 +/- 文字承载，
                     不靠颜色或图形单独传信息（§3.7） -->
                <span aria-hidden="true">{{ card.foot.value >= 0 ? '▲' : '▼' }}</span>
                <span :class="card.foot.value >= 0 ? 'kpi-card__delta--up' : 'kpi-card__delta--down'">
                  较昨日 {{ card.foot.value >= 0 ? '+' : '-' }}{{ Math.abs(card.foot.value) }}%
                </span>
              </template>
            </template>
            <span v-else>{{ card.foot.text }}</span>
          </p>
        </div>
      </div>
    </section>

    <!-- ② 待办：两项都为 0 时整条不渲染 —— 恒显示「0」的待办条只会训练用户忽略它 -->
    <section v-if="todoItems.length" class="dashboard__section" aria-label="待办">
      <div class="todo-grid">
        <router-link v-for="item in todoItems" :key="item.key" class="card todo-card" :to="item.to">
          <span class="todo-card__label">{{ item.label }}</span>
          <span class="todo-card__count">{{ item.count }}</span>
          <span class="todo-card__action">{{ item.action }} ›</span>
        </router-link>
      </div>
    </section>

    <!-- ③ 近 7 日票房趋势 -->
    <section class="dashboard__section" aria-label="近 7 日票房趋势">
      <div class="section-head">
        <h2 class="section-head__title">近 7 日票房趋势</h2>
        <span class="dashboard__note">截至昨日</span>
        <el-button class="dashboard__refresh" link type="primary" :loading="refreshing" @click="reload">
          <!-- loading 时 EP 自带转圈图标，再留一个 Refresh 就是一个按钮两个图标（规则 86） -->
          <el-icon v-if="!refreshing"><Refresh /></el-icon>
          {{ refreshing ? '刷新中' : '刷新' }}
        </el-button>
      </div>

      <div class="card chart-card">
        <div v-if="failed.overview && !overview.revenueTrend.length" class="empty-hint">数据加载失败，请稍后重试</div>
        <div v-else-if="!overview.revenueTrend.length" class="chart-card__skeleton"><el-skeleton :rows="4" animated /></div>
        <div v-else ref="trendChart" class="chart-card__body"></div>
        <p v-if="overview.updatedAt" class="chart-card__foot">
          <span v-if="failed.overview" class="chart-card__foot-error">刷新失败，请稍后重试 · </span>
          数据截至 {{ updatedTime }}
        </p>
      </div>
    </section>

    <!-- ④ 分布拆解 -->
    <section class="dashboard__section" aria-label="分布拆解">
      <div class="chart-grid">
        <div class="card chart-card">
          <h3 class="chart-card__title">订单状态分布</h3>
          <div v-if="failed.overview && !orderTotal" class="empty-hint">数据加载失败，请稍后重试</div>
          <div v-else-if="!orderTotal && loading.overview" class="chart-card__skeleton"><el-skeleton :rows="4" animated /></div>
          <div v-else-if="!orderTotal" class="empty-hint">暂无数据</div>
          <div v-else class="status-card">
            <div class="status-ring">
              <!-- 画布只画图形：环内的总计与下方的图例都是真 DOM 文字，
                   画布上不再画第二份（canvas 里的字选不中也读不出来），故整体对读屏隐藏 -->
              <div ref="orderStatusChart" class="status-ring__chart" aria-hidden="true"></div>
              <!-- 环内的总计就是各段占比的分母，让读者不必自己去加那五行 -->
              <div class="status-ring__center">
                <span class="status-ring__total">{{ orderTotal }}</span>
                <span class="status-ring__unit">总计（笔）</span>
              </div>
            </div>
            <ul class="status-list">
              <li
                v-for="row in orderStatusRows"
                :key="row.name"
                class="status-row"
                :class="`status-row--${getOrderStatusType(row.name)}`"
              >
                <span class="status-row__dot" aria-hidden="true"></span>
                <span class="status-row__name">{{ row.name }}</span>
                <span class="status-row__value">{{ row.value }} 笔</span>
                <span class="status-row__percent">{{ row.percent.toFixed(1) }}%</span>
              </li>
            </ul>
          </div>
        </div>

        <div class="card chart-card">
          <h3 class="chart-card__title">电影类型分布</h3>
          <div v-if="failed.overview && !overview.filmType.length" class="empty-hint">数据加载失败，请稍后重试</div>
          <div v-else-if="!overview.filmType.length && loading.overview" class="chart-card__skeleton"><el-skeleton :rows="4" animated /></div>
          <div v-else-if="!overview.filmType.length" class="empty-hint">暂无数据</div>
          <div v-else ref="filmTypeChart" class="chart-card__body"></div>
        </div>
      </div>
    </section>

    <!-- ⑤ 双榜单 -->
    <section class="dashboard__section" aria-label="榜单">
      <div class="chart-grid">
        <div class="card chart-card">
          <h3 class="chart-card__title">票房 Top 5</h3>
          <div v-if="loading.boxOffice && !boxOfficeTop.length" class="rank-box__loading"><el-skeleton :rows="5" animated /></div>
          <div v-else-if="failed.boxOffice && !boxOfficeTop.length" class="empty-hint">数据加载失败，请稍后重试</div>
          <div v-else-if="!boxOfficeTop.length" class="empty-hint">暂无数据</div>
          <div v-else class="rank-box">
            <div v-for="(film, index) in boxOfficeTop" :key="film.id" class="rank-row">
              <span class="rank-badge" :class="rankBadgeClass(index)">{{ index + 1 }}</span>
              <span class="rank-row__title">{{ film.title }}</span>
              <span class="rank-row__value">{{ formatBoxOffice(film.boxOffice) }}</span>
            </div>
          </div>
        </div>

        <div class="card chart-card">
          <h3 class="chart-card__title">评分 Top 5</h3>
          <div v-if="loading.mark && !markTop.length" class="rank-box__loading"><el-skeleton :rows="5" animated /></div>
          <div v-else-if="failed.mark && !markTop.length" class="empty-hint">数据加载失败，请稍后重试</div>
          <div v-else-if="!markTop.length" class="empty-hint">暂无数据</div>
          <div v-else class="rank-box">
            <div v-for="(film, index) in markTop" :key="film.id" class="rank-row">
              <span class="rank-badge" :class="rankBadgeClass(index)">{{ index + 1 }}</span>
              <span class="rank-row__title">{{ film.title }}</span>
              <span class="rank-row__value rank-row__value--score">{{ formatScore(film.score) }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ⑥ 快捷入口 -->
    <section class="dashboard__section" aria-label="核心功能入口">
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
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from "vue";
import request from "@/utils/request.js";
import { Refresh, Money, Tickets, User, OfficeBuilding, CreditCard, VideoCamera } from "@element-plus/icons-vue";
// formatYuan 给「区间聚合」（今日票房、逐日趋势），0 是真实值；
// formatBoxOffice 给「累计票房」（榜单），0 表示该片还没有收入，渲染「暂无数据」。
// 两者刻意不混用 —— 见 utils/format.js 里两个函数各自的注释。
import { formatYuan, formatBoxOffice, formatScore } from "@/utils/format.js";
import { FILM_API, STATISTICS_API, ORDER_STATUS_OPTIONS, getOrderStatusType } from "@/constants";
import * as echarts from 'echarts/core';
import { BarChart, LineChart, PieChart } from 'echarts/charts';
import { GridComponent, TooltipComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';

// 只注册用到的三种图表：折线（票房趋势）/ 横向条（影片类型）/ 环形（订单状态分布）。
// PieChart 是被「请回来」的 —— 大盘早先用饼图，重构时换成 el-progress 顺手把它摇掉了；
// 订单状态分布改回环形后它重新进场。LabelLayout 仍不注册：那是给 labelLayout 回调用的，
// 本项目不开图表标签（文字一律由 DOM 承担），用不上。
// 想核对某个图表类型在不在产物里，**不能**用 grep 图表名 —— ECharts 的 lang 字典
// （typeNames）与事件分发里始终有 "pie" 这类字样，与注册了哪些图表无关。
// 判据是类型专有的实现符号，例如 pie 的 padAngle。
echarts.use([BarChart, LineChart, PieChart, GridComponent, TooltipComponent, CanvasRenderer]);

/* 刷新反馈的最短展示时长：本机聚合查询几十毫秒就返回，不兜底的话转圈一闪而过，
   用户无从确认「点过了」（规则 86）。与前台 front/Home.vue 取同一个值。 */
const MIN_REFRESH_MS = 450;
const RANK_LIMIT = 5;
/* 与后端 CinemaStatus 枚举、constants/index.js 的 CINEMA_STATUS 同字面量。
   写成常量而非「不等于未审核」，是为了将来真出现第三个审核状态时不会静默算错 */
const UNAUDITED = '未审核';
const APPROVED = '已审核';

const entries = [
  { path: '/manage/cinema', icon: OfficeBuilding, title: '影院管理', desc: '新增、编辑、审核影院信息' },
  { path: '/manage/film', icon: VideoCamera, title: '电影管理', desc: '维护电影信息、封面与排片' },
  { path: '/manage/user', icon: User, title: '用户管理', desc: '管理平台用户与权限分配' },
  { path: '/manage/ordered', icon: CreditCard, title: '购票记录', desc: '查看和管理用户的购票订单信息' },
];

const overview = reactive({
  summary: null as null | Record<string, any>,
  cinemaStatus: [] as Array<{ name: string; value: number }>,
  filmType: [] as Array<{ name: string; value: number }>,
  orderStatus: [] as Array<{ name: string; value: number }>,
  revenueTrend: [] as Array<{ date: string; revenue: number; orders: number }>,
  updatedAt: '',
});

const loading = reactive({ overview: false, boxOffice: false, mark: false });
const failed = reactive({ overview: false, boxOffice: false, mark: false });
const refreshing = ref(false);
const boxOfficeTop = ref<any[]>([]);
const markTop = ref<any[]>([]);

const trendChart = ref<HTMLElement | null>(null);
const filmTypeChart = ref<HTMLElement | null>(null);
const orderStatusChart = ref<HTMLElement | null>(null);

// ECharts 用 canvas 渲染，不解析 CSS 变量，只能在运行期把令牌值读出来（规范 §3.7）
const cssVar = (name: string, fallback = '') =>
  getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback;

const updatedTime = computed(() =>
  overview.updatedAt.length >= 19 ? overview.updatedAt.slice(11, 19) : '—'
);

/* 已审核数从分组结果派生 —— 后端已经算好，前端不再取第二次。
   影院总数不再在这里求和：它唯一的消费者是那张百分比卡，总数本身由
   summary.totalCinemas 给出（后端同一份分组派生），前端再算一遍是对同一事实的第二种口径。 */
const approvedCount = computed(() =>
  Number(overview.cinemaStatus.find((row) => row.name === APPROVED)?.value ?? 0)
);

/* 占比的分母 = 五态之和。与环上的扇区同源：同一份分组结果既画环又算占比，
   两个数字永远对得上；基准显式写在卡脚上，不另发一次 COUNT（规则 89）。 */
const orderTotal = computed(() =>
  overview.orderStatus.reduce((sum, row) => sum + Number(row.value), 0)
);

/* 订单状态分布的行。行序与配色都从 ORDER_STATUS_OPTIONS 派生 —— 它的键序就是订单页
   状态筛选下拉的顺序，颜色查同一张 ORDER_STATUS_MAP，于是这张卡、订单表格里的 el-tag、
   筛选下拉三处不会各走各的。
   GROUP BY 不返回没有订单的状态，这里补 0：图例的五行在任何数据下都稳定，
   不会因为某个状态清零就少一行、让读者以为系统里不存在该状态。 */
const orderStatusRows = computed(() => {
  const total = orderTotal.value;
  return ORDER_STATUS_OPTIONS.map((name) => {
    const value = Number(overview.orderStatus.find((row) => row.name === name)?.value ?? 0);
    return { name, value, percent: total ? (value / total) * 100 : 0 };
  });
});

/**
 * 环比。基准是趋势的末点（= 昨天），不另发请求：同一份趋势数据既画折线又算环比，
 * 两者永远对得上。除数为 0 或没有基准时返回 null，渲染成「较昨日 —」——
 * 给一个没有意义的百分比比不给更糟。
 */
const delta = (current: number | null, previous: number | null | undefined) => {
  if (current === null || current === undefined) return null;
  if (previous === null || previous === undefined || Number(previous) === 0) return null;
  return Math.round(((Number(current) - Number(previous)) / Math.abs(Number(previous))) * 1000) / 10;
};

const kpiCards = computed(() => {
  const s = overview.summary;
  const cards = [
    { key: 'todayRevenue', icon: Money, label: '今日票房', foot: null as any, value: null as string | null },
    { key: 'todayOrders', icon: Tickets, label: '今日订单（笔）', foot: null as any, value: null as string | null },
    { key: 'totalUsers', icon: User, label: '用户总数', foot: null as any, value: null as string | null },
    { key: 'totalCinemas', icon: OfficeBuilding, label: '影院总数', foot: null as any, value: null as string | null },
  ];
  if (!s) return cards;

  const last = overview.revenueTrend.length
    ? overview.revenueTrend[overview.revenueTrend.length - 1]
    : null;

  cards[0].value = formatYuan(s.todayRevenue);
  cards[0].foot = { kind: 'delta', value: delta(s.todayRevenue, last?.revenue) };
  cards[1].value = String(s.todayOrders);
  cards[1].foot = { kind: 'delta', value: delta(s.todayOrders, last?.orders) };
  cards[2].value = String(s.totalUsers);
  cards[3].value = String(s.totalCinemas);
  // 待审核数只在待办条出现一次，这里给另一半，避免同一个数字在页面上出现两遍
  cards[3].foot = { kind: 'text', text: `已审核 ${approvedCount.value} 家` };
  return cards;
});

/* 待办只放真有落地页的两项。处理中充值单据刻意不放：管理后台没有充值单据页，
   一个点不动的数字比不显示更糟。 */
const todoItems = computed(() => {
  const s = overview.summary;
  if (!s) return [];
  return [
    {
      key: 'pendingCinemas',
      label: '待审核影院',
      count: s.pendingCinemas,
      action: '去审核',
      to: { path: '/manage/cinema', query: { status: UNAUDITED } },
    },
    {
      key: 'pendingPickupOrders',
      label: '待取票订单',
      count: s.pendingPickupOrders,
      action: '去查看',
      to: { path: '/manage/ordered', query: { status: '待取票' } },
    },
  ].filter((item) => Number(item.count) > 0);
});

const rankBadgeClass = (index: number) =>
  index < 3 ? `rank-badge--top${index + 1}` : 'rank-badge--plain';

/* ---------- 取数 ---------- */

/* 三个取数函数都遵守同一条：**失败不清空已展示的数据**（规则 86）。
   瞬时故障不该把已知数值抹成错误文案，失败只翻 failed 标记，由各区块的次要行提示。 */
const loadOverview = async () => {
  loading.overview = true;
  try {
    const res = await request.get(STATISTICS_API.OVERVIEW);
    if (res.code === '200') {
      const d = res.data || {};
      overview.summary = d.summary || null;
      overview.cinemaStatus = d.cinemaStatus || [];
      overview.filmType = d.filmType || [];
      overview.orderStatus = d.orderStatus || [];
      overview.revenueTrend = d.revenueTrend || [];
      overview.updatedAt = d.updatedAt || '';
      failed.overview = false;
    } else {
      failed.overview = true;
    }
  } catch (error) {
    // 网络异常的统一提示由 request.js 的响应拦截器给出，这里只落错误态
    console.error('统计接口请求异常：', error);
    failed.overview = true;
  } finally {
    loading.overview = false;
  }
};

const loadFilmBoxOfficeTop = async () => {
  loading.boxOffice = true;
  try {
    const res = await request.get(FILM_API.BOX_OFFICE_TOP, { params: { topNum: 10 } });
    if (res.code === '200') {
      boxOfficeTop.value = (res.data || []).slice(0, RANK_LIMIT);
      failed.boxOffice = false;
    } else {
      failed.boxOffice = true;
    }
  } catch (error) {
    console.error('票房榜接口请求异常：', error);
    failed.boxOffice = true;
  } finally {
    loading.boxOffice = false;
  }
};

const loadFilmMarkTop = async () => {
  loading.mark = true;
  try {
    const res = await request.get(FILM_API.MARK_TOP, { params: { topNum: 10 } });
    if (res.code === '200') {
      markTop.value = (res.data || []).slice(0, RANK_LIMIT);
      failed.mark = false;
    } else {
      failed.mark = true;
    }
  } catch (error) {
    console.error('评分榜接口请求异常：', error);
    failed.mark = true;
  } finally {
    loading.mark = false;
  }
};

/* 请求无论多快都把转圈撑满 MIN_REFRESH_MS，读起来是「转圈停 → 数据落位」 */
const reload = async () => {
  if (refreshing.value) return;
  refreshing.value = true;
  const startedAt = Date.now();

  await Promise.all([loadOverview(), loadFilmBoxOfficeTop(), loadFilmMarkTop()]);

  const wait = Math.max(0, MIN_REFRESH_MS - (Date.now() - startedAt));
  if (wait) await new Promise((resolve) => window.setTimeout(resolve, wait));
  refreshing.value = false;
};

/* ---------- 图表 ---------- */

const initTrendChart = () => {
  const el = trendChart.value;
  if (!el) return;
  echarts.getInstanceByDom(el)?.dispose();

  const points = overview.revenueTrend;
  const chart = echarts.init(el);
  const primary = cssVar('--el-color-primary');

  chart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params: any[]) => {
        const point = points[params[0].dataIndex];
        return `${point.date}<br/>票房 ${formatYuan(point.revenue)}<br/>订单 ${point.orders} 笔`;
      },
    },
    grid: { left: 8, right: 16, top: 16, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: points.map((point) => point.date.slice(5)),
      axisLabel: { fontSize: 12 },
    },
    yAxis: { type: 'value', name: '票房（元）', min: 0, axisLabel: { fontSize: 12 } },
    series: [
      {
        name: '票房',
        type: 'line',
        symbolSize: 6,
        data: points.map((point) => Number(point.revenue)),
        lineStyle: { color: primary },
        itemStyle: { color: primary },
      },
    ],
  }, true);
};

const initFilmTypeChart = () => {
  const el = filmTypeChart.value;
  if (!el) return;
  echarts.getInstanceByDom(el)?.dispose();

  // 横向条：类别名是中文，竖柱要旋转 30° 才放得下。
  // ECharts 的类目轴从下往上画，升序排列才能让最大的那条落在顶部。
  const rows = [...overview.filmType].sort((a, b) => Number(a.value) - Number(b.value));
  const chart = echarts.init(el);

  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 8, right: 24, top: 8, bottom: 8, containLabel: true },
    xAxis: { type: 'value', name: '影片数量', min: 0, axisLabel: { fontSize: 12 } },
    yAxis: { type: 'category', data: rows.map((row) => row.name), axisLabel: { fontSize: 12 } },
    series: [
      {
        name: '影片数量',
        type: 'bar',
        barWidth: '60%',
        data: rows.map((row) => Number(row.value)),
        itemStyle: { borderRadius: 4, color: cssVar('--el-color-primary') },
      },
    ],
  }, true);
};

/* 订单状态分布：环形图（甜甜圈）+ DOM 图例。
   配色只可能来自令牌，而 canvas 不解析 CSS 变量，只能在运行期把值读出来（§3.7）；
   状态 → 令牌名复用 constants 的 ORDER_STATUS_MAP，与图例色点、订单表格的 el-tag 同色。
   环内总计与图例都用 DOM 写，画布上不开标签：一张环形图的结论全在文字里，
   而 canvas 的文字选不中也读不出来，留给读屏用户的就只剩一片空白。 */
const initOrderStatusChart = () => {
  const el = orderStatusChart.value;
  if (!el) return;
  echarts.getInstanceByDom(el)?.dispose();

  const rows = orderStatusRows.value;
  const chart = echarts.init(el);

  chart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (params: any) =>
        `${params.name}<br/>${params.value} 笔<br/>占比 ${Number(params.percent).toFixed(1)}%`,
    },
    series: [
      {
        type: 'pie',
        radius: ['62%', '92%'],
        center: ['50%', '50%'],
        // 不开图表标签（含引导线）：文字一律由 DOM 给，画布上再画一份既不可读又重复
        label: { show: false },
        labelLine: { show: false },
        // 扇区之间留一道卡底色的缝：待支付（warning）与已退票（danger）同属橙红，
        // 单靠色相不足以分段，缝隙让每段的边界始终明确
        itemStyle: { borderColor: cssVar('--el-bg-color', '#ffffff'), borderWidth: 2 },
        // 悬停把当前扇区弹出一小段：环不大，这是唯一能把「这是哪一段」讲清楚的反馈
        emphasis: { scale: true, scaleSize: 6 },
        data: rows.map((row) => ({
          name: row.name,
          value: row.value,
          itemStyle: { color: cssVar(`--el-color-${getOrderStatusType(row.name)}`) },
        })),
      },
    ],
  }, true);
};

/* 唯一的初始化路径：数据落位 → watch 触发 → 建图。
   页面挂载时不再单独调一次 —— 那样每次加载都会初始化两遍（旧实现的冗余，
   见 前端规范待办.md T-11）。空态时容器被 v-if 摘掉、ref 为 null，两函数各自挡掉。 */
watch(
  [() => overview.revenueTrend, () => overview.filmType, () => overview.orderStatus],
  async () => {
    await nextTick();
    initTrendChart();
    initFilmTypeChart();
    initOrderStatusChart();
  },
  { deep: true }
);

const handleResize = () => {
  if (trendChart.value) echarts.getInstanceByDom(trendChart.value)?.resize();
  if (filmTypeChart.value) echarts.getInstanceByDom(filmTypeChart.value)?.resize();
  if (orderStatusChart.value) echarts.getInstanceByDom(orderStatusChart.value)?.resize();
};

onMounted(() => {
  window.addEventListener('resize', handleResize);
  reload();
});

// 不销毁的话，ECharts 内部注册表仍持有画布 DOM 与 canvas —— v-if 摘掉容器后
// 那段 DOM 已经不是页面的一部分，实例却继续挂着。
onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  [trendChart.value, filmTypeChart.value, orderStatusChart.value].forEach((el) => {
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

/* ---------- ① KPI ---------- */

/* minmax(0, 1fr) 的 0 下限是承重件：裸 1fr 的 min-width 是 auto，
   长数字会把格子撑破、右侧那张卡溢出容器。 */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-16);
}

.kpi-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-8);
  padding: var(--space-16);
}

.kpi-card__head {
  display: flex;
  align-items: center;
  gap: var(--space-8);
}

.kpi-card__icon {
  font-size: 20px;
  color: var(--el-color-primary);
}

/* 12px 辅助文字用 --el-text-color-regular（6.11:1）而非 secondary（3.08:1，不达正文） */
.kpi-card__label {
  font-size: var(--fs-xs);
  color: var(--el-text-color-regular);
}

/* KPI 主指标取 §4.2 的 28px 档。字重只能是 700：formatYuan 返回的字符串含「元」，
   含中文就落进 §4.4 的「只能 400 / 700」，500 会给中文触发伪粗体。
   tabular-nums 让数字等宽，刷新前后不左右跳。 */
.kpi-card__value {
  margin: 0;
  font-size: var(--fs-3xl);
  font-weight: var(--fw-bold);
  line-height: var(--lh-loose);
  color: var(--el-text-color-primary);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.kpi-card__foot {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  margin: 0;
  font-size: var(--fs-xs);
  color: var(--el-text-color-regular);
  font-variant-numeric: tabular-nums;
}

/* 涨跌按「有利性」着色：票房与订单都是越多越好，故升用功能色成功、降用危险。
   这是状态通道，不是装饰 —— 且方向另有 +/- 文字与箭头承载，不单靠颜色（§3.7）。 */
.kpi-card__delta--up {
  color: var(--el-color-success);
}

.kpi-card__delta--down {
  color: var(--el-color-danger);
}

.kpi-card__failed {
  margin: 0;
  font-size: var(--fs-base);
  color: var(--el-text-color-regular);
}

.kpi-card__skeleton {
  padding: var(--space-4) 0;
}

/* ---------- ② 待办 ---------- */

.todo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: var(--space-16);
}

/* 待办卡是 <a>（router-link），要显式去掉下划线并继承文字色 */
.todo-card {
  display: flex;
  align-items: baseline;
  gap: var(--space-12);
  padding: var(--space-16);
  border: 1px solid var(--el-border-color-lighter);
  color: inherit;
  text-decoration: none;
  transition: border-color 100ms ease-out;
}

.todo-card:hover {
  border-color: var(--el-color-primary);
}

.todo-card__label {
  font-size: var(--fs-base);
  color: var(--el-text-color-regular);
}

.todo-card__count {
  font-size: var(--fs-2xl);
  font-weight: var(--fw-bold);
  color: var(--el-color-primary);
  font-variant-numeric: tabular-nums;
}

.todo-card__action {
  margin-left: auto;
  font-size: var(--fs-xs);
  color: var(--el-color-primary);
}

/* ---------- ③④⑤ 图表卡 ---------- */

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

/* 标题含中文，字重只能用 400 / 700（§4.4） */
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

.chart-card__skeleton {
  padding: var(--space-8) 0;
}

.chart-card__foot {
  margin: var(--space-8) 0 0;
  font-size: var(--fs-xs);
  color: var(--el-text-color-regular);
}

.chart-card__foot-error {
  color: var(--el-color-danger);
}

/* 标题带右侧的两枚：说明文字与刷新按钮。.section-head 是 flex + baseline，
   故刷新按钮用 margin-left: auto 顶到最右。 */
.dashboard__note {
  font-size: var(--fs-xs);
  color: var(--el-text-color-regular);
}

.dashboard__refresh {
  margin-left: auto;
}

/* ---------- 订单状态分布 ---------- */

/* 环形居中、图例在下通栏。不并排放：并排时环形与图例要分那点卡片宽，
   图例的定宽列（笔数 / 占比）一挤就先塌，而上下排布让图例拿到整幅卡宽。
   flex-grow 而不是写死高度：同排那张卡的内容自带固定高度（.chart-card__body 的 320px），
   栅格把两张卡拉成等高，这里填满标题之下剩下的空间 —— 于是 320px 只需写在它的归属处。 */
.status-card {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  align-items: center;
  gap: var(--space-16);
}

.status-ring {
  position: relative;
  flex-shrink: 0;
  width: 160px;
  height: 160px;
}

.status-ring__chart {
  width: 100%;
  height: 100%;
}

/* 环内的总计，叠在画布上：环心是空的不可能挡住图形，故这里不算「压淡层」；
   pointer-events: none 把鼠标放回画布，别把环心的悬停吃掉。 */
.status-ring__center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-4);
  pointer-events: none;
}

/* 主数字取 24px 档：环心直径约 100px，28px 档会顶到内圈上 */
.status-ring__total {
  font-size: var(--fs-2xl);
  font-weight: var(--fw-bold);
  line-height: var(--lh-loose);
  color: var(--el-text-color-primary);
  font-variant-numeric: tabular-nums;
}

.status-ring__unit {
  font-size: var(--fs-xs);
  color: var(--el-text-color-regular);
}

/* auto 基准而不是 0 基准：等高时靠 space-evenly 把五行摊开，内容量本身撑得住时
   也不会被压塌（0 基准下这一列的高度完全由剩余空间决定）。
   width: 100% 抵消父级的 align-items: center —— 否则这一列会缩到内容宽，
   定宽列也就失去了「五行右对齐成一条竖线」的效果。 */
.status-list {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  justify-content: space-evenly;
  gap: var(--space-8);
  width: 100%;
  margin: 0;
  padding: 0;
  list-style: none;
}

/* 局部令牌：状态 → 颜色。类名取自 ORDER_STATUS_MAP 的 el-tag type，
   故这五个类与订单表格里的标签色出自同一张表，不会各自漂移（§11.1 局部令牌）。 */
.status-row--primary { --status-color: var(--el-color-primary); }
.status-row--success { --status-color: var(--el-color-success); }
.status-row--warning { --status-color: var(--el-color-warning); }
.status-row--danger  { --status-color: var(--el-color-danger); }
.status-row--info    { --status-color: var(--el-color-info); }

.status-row {
  display: flex;
  align-items: center;
  gap: var(--space-12);
}

/* 色点把图例行与环上那个扇区对应起来 —— 承载信息，不算装饰，
   故取功能色基色（白底 4.56:1 ~ 5.46:1，过 §10.1 的 3:1）。 */
.status-row__dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  border-radius: var(--el-border-radius-circle);
  background-color: var(--status-color);
}

/* 状态名是这一行唯一的弹性列；窄到放不下时省略号收尾，而不是把定宽的两列挤走 */
.status-row__name {
  flex: 1;
  min-width: 0;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 笔数与占比各占一列定宽：五行的小数点与单位成一条竖线，扫一眼就能比大小 */
.status-row__value {
  flex-shrink: 0;
  width: 64px;
  text-align: right;
  color: var(--el-text-color-regular);
  font-variant-numeric: tabular-nums;
}

/* 占比是这一行的结论，比笔数重一档 */
.status-row__percent {
  flex-shrink: 0;
  width: 56px;
  text-align: right;
  color: var(--el-text-color-primary);
  font-variant-numeric: tabular-nums;
}

/* ---------- 榜单 ---------- */

.rank-box {
  display: flex;
  flex-direction: column;
}

.rank-box__loading {
  padding: var(--space-4) 0;
}

.rank-row {
  display: flex;
  align-items: center;
  gap: var(--space-12);
  padding: var(--space-8) 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.rank-row:last-child {
  border-bottom: none;
}

/* 名次徽章用 §3.6 的扩展色板（白字压其上达标）；1~3 名金 / 银 / 铜 */
.rank-badge {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  border-radius: var(--el-border-radius-circle);
  font-size: var(--fs-xs);
  font-weight: var(--fw-bold);
  line-height: 24px;
  text-align: center;
}

.rank-badge--top1 {
  background-color: var(--color-rank-1);
  color: var(--color-on-accent);
}

.rank-badge--top2 {
  background-color: var(--color-rank-2);
  color: var(--color-on-accent);
}

.rank-badge--top3 {
  background-color: var(--color-rank-3);
  color: var(--color-on-accent);
}

.rank-badge--plain {
  background-color: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
}

.rank-row__title {
  flex: 1;
  min-width: 0;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rank-row__value {
  flex-shrink: 0;
  font-weight: var(--fw-bold);
  color: var(--el-text-color-primary);
  font-variant-numeric: tabular-nums;
}

/* 评分含「分」字（中文），字重必须 400 / 700；白底评分色 4.68:1 达 AA（§3.6） */
.rank-row__value--score {
  color: var(--color-rating-text);
}

/* ---------- ⑥ 快捷入口 ---------- */

.entry-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: var(--space-16);
}

/* 入口卡是 <a>，要显式去掉下划线并继承文字色。
   hover 只改边框与标题色，不叠阴影 —— 全局 .card 已常驻 --el-box-shadow-lighter。 */
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
