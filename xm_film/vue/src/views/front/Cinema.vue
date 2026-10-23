<template>
  <div class="cinema-page">
    <!-- 页头：深色横幅。名录页原本直接从两个下拉开始，没有「这是什么页面」的身份；
         这里补一条与三个详情页 hero 同族的深色表面，全宽出血，内容层与下方列表同宽 -->
    <header class="cinema-masthead">
      <div class="cinema-masthead__inner measure-inline">
        <div class="cinema-masthead__text">
          <h1 class="cinema-masthead__title">影院</h1>
          <p class="cinema-masthead__desc">按城市与区域筛选，查看影院信息与当日排片</p>
        </div>
        <!-- aria-live：换筛选后结果数会变，读屏用户靠这一句才知道列表已刷新 -->
        <div class="cinema-masthead__count" aria-live="polite">
          <!-- null 表示还没取到结果（首屏 / 失败），与「确实 0 家」区分开 -->
          <span class="cinema-masthead__num">{{ data.total === null ? '—' : data.total }}</span>
          <span class="cinema-masthead__unit">家影院</span>
        </div>
      </div>
    </header>

    <div class="page-narrow">
      <!-- 筛选：与影片列表页（Movie.vue）同一套 filter-panel / filter-chip。
           两个前台列表页此前一个用下拉、一个用按钮，观感与操作都不同源 -->
      <div class="filter-panel">
        <div class="filter-row">
          <div class="filter-label">城市 :</div>
          <!-- fieldset + aria-label：.filter-label 是并列的 <div>，与按钮没有
               for / aria-labelledby 关联，不加这一层的话读屏只会念「北京 按钮」，
               且两行的「全部」按钮无法区分（改按钮前这里是 el-select 的 aria-label） -->
          <fieldset class="filter-options" aria-label="按城市筛选">
            <button
                class="filter-chip"
                :class="{ 'filter-chip--active': !query.city }"
                @click="selectCity(null)"
            >全部</button>
            <button
                v-for="city in cityOptions"
                :key="city"
                class="filter-chip"
                :class="{ 'filter-chip--active': query.city === city }"
                @click="selectCity(city)"
            >{{ city }}</button>
          </fieldset>
        </div>

        <div class="filter-row filter-row--last">
          <div class="filter-label">区域 :</div>
          <fieldset class="filter-options" aria-label="按区域筛选">
            <!-- 区域依赖城市，未选城市时给出说明而不是一排必然为空的选项 -->
            <span v-if="!query.city" class="filter-hint">请先选择城市</span>
            <template v-else>
              <button
                  class="filter-chip"
                  :class="{ 'filter-chip--active': !query.district }"
                  @click="selectDistrict(null)"
              >全部</button>
              <button
                  v-for="district in districtOptions"
                  :key="district"
                  class="filter-chip"
                  :class="{ 'filter-chip--active': query.district === district }"
                  @click="selectDistrict(district)"
              >{{ district }}</button>
            </template>
          </fieldset>
        </div>
      </div>

      <!-- 骨架：与真实卡片同形状（240px 的 10:7 图 + 三行文字），数据到位时不跳高。
           灰块不是数据，不违反「禁止用假数据填充」 -->
      <div v-if="data.loading" class="cinema-list" aria-hidden="true">
        <div v-for="n in SKELETON_ROWS" :key="n" class="cinema-skeleton">
          <div class="cinema-skeleton__poster"></div>
          <div class="cinema-skeleton__lines">
            <div class="cinema-skeleton__line cinema-skeleton__line--title"></div>
            <div class="cinema-skeleton__line cinema-skeleton__line--short"></div>
            <div class="cinema-skeleton__line"></div>
          </div>
        </div>
      </div>

      <!-- 影院列表：整卡是 router-link，可直接进入影院详情（此前整页没有任何跳转入口） -->
      <div v-else-if="data.filmData.length" class="cinema-list">
        <router-link
            v-for="cinema in data.filmData"
            :key="cinema.id"
            class="cinema-card"
            :to="`/front/cinemaDetail/${cinema.id}`"
        >
          <div class="cinema-card__poster">
            <img :src="cinemaCoverSrc(cinema.avatar)" :alt="`${cinema.name || '影院'} 图片`" class="cinema-card__img" loading="lazy" @error="onCinemaCoverError">
          </div>

          <div class="cinema-card__info">
            <div class="cinema-card__head">
              <h3 class="cinema-card__name">{{ cinema.name }}</h3>
              <!-- 入口提示：整卡都是链接，它不是唯一入口，只是把「进去能做什么」写明白 -->
              <span class="cinema-card__enter">
                查看场次
                <el-icon><ArrowRight /></el-icon>
              </span>
            </div>

            <!-- 影院服务标签：文案与底色取自 constants/cinemaServices.js，与详情页同源 -->
            <div class="cinema-card__tags">
              <span
                  v-for="service in CINEMA_SERVICES"
                  :key="service.key"
                  class="service-tag"
                  :class="serviceToneClass(service)"
              >
                <el-icon><component :is="service.icon" /></el-icon>{{ service.label }}
              </span>
            </div>

            <div class="cinema-card__detail">
              <p class="cinema-card__detail-row">
                <el-icon class="cinema-card__detail-icon"><Location /></el-icon>{{ cinema.address }}
              </p>
              <p class="cinema-card__detail-row">
                <el-icon class="cinema-card__detail-icon"><Phone /></el-icon>{{ cinema.phone }}
              </p>
            </div>
          </div>
        </router-link>
      </div>

      <!-- 失败与空态文案不同：网络失败不该说成「这家影院不存在」 -->
      <div v-else-if="data.error" class="empty-hint">
        <p class="empty-hint__text">数据加载失败，请稍后重试</p>
        <el-button class="empty-hint__action" link type="primary" @click="load">重新加载</el-button>
      </div>
      <div v-else class="empty-hint">
        <p class="empty-hint__text">{{ hasFilter ? '没有符合筛选条件的影院' : '暂无数据' }}</p>
        <el-button v-if="hasFilter" class="empty-hint__action" link type="primary" @click="clearFilters">
          清空筛选条件
        </el-button>
      </div>

      <div v-if="data.total" class="cinema-list__pagination">
        <el-pagination
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            :current-page="data.pageNum"
            :page-size="data.pageSize"
            :page-sizes="[5, 10, 15]"
            background
            layout="total, sizes, prev, pager, next"
            :total="data.total || 0"
            :disabled="data.loading"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { isSuccess, API_PATHS, apiPage, CINEMA_API } from '@/constants';
import { reactive, computed } from 'vue';
import request from "@/utils/request.js";
import { ElMessage } from 'element-plus';
import { ArrowRight, Location, Phone } from '@element-plus/icons-vue';
import { CINEMA_SERVICES, serviceToneClass } from '@/constants/cinemaServices';
import { usePagedList } from '@/composables/usePagedList';
import { cinemaCoverSrc, onCinemaCoverError } from '@/utils/avatar';

// 骨架条数：一屏能看到的卡片量，多了白渲染
const SKELETON_ROWS = 4;

// 只有筛选状态留在本页；分页数据交给 usePagedList（FE-49 / FE-65）。
// `null` 表示尚未取到结果（页头显示「—」），而 usePagedList 的 total 语义是「已取到的条数」，
// 两者必须分开，否则失败态会被渲染成「确实 0 家」。
const list = usePagedList({
  pageSize: 12,
  fetch: (params) => request.get(apiPage(API_PATHS.CINEMAS), { params }),
});

const data = reactive({
  pageNum: 1,
  pageSize: 12,
  total: null,
  filmData: [],
  loading: false,
  error: false
});

const query = reactive({ city: null, district: null })
// 城市列表不再单独存一份：它就是 districtsByCity 的键。
// 两份都由同一个 res.data 派生、又各自赋值，是必得手工同步的重复状态。
// 后端 regions() 用 LinkedHashMap 且保证 city 非空，Object.keys 的顺序与展示顺序一致。
const filters = reactive({ districtsByCity: {} })
const cityOptions = computed(() => Object.keys(filters.districtsByCity))
const districtOptions = computed(() =>
    query.city ? (filters.districtsByCity[query.city] || []) : [])
const hasFilter = computed(() => Boolean(query.city || query.district))

// 拉不到筛选项只留空下拉，不打断列表；网络错误提示由 request.js 统一弹
const loadRegions = () => {
  request.get(CINEMA_API.REGIONS).then(res => {
    if (isSuccess(res)) {
      filters.districtsByCity = Object.fromEntries(res.data.map(r => [r.city, r.districts]))
    }
  }).catch(() => {})
}

// 点已选中的那一项不重复发请求（chip 会重复触发 click，下拉的 change 不会）
const selectCity = (city) => {
  if (query.city === city) return
  query.city = city
  // 换城市后旧区域未必属于新城市，必须一并清掉，否则会筛出空结果
  query.district = null
  reload()
}

const selectDistrict = (district) => {
  if (query.district === district) return
  query.district = district
  reload()
}

const clearFilters = () => {
  query.city = null
  query.district = null
  reload()
}

// 换筛选条件并回到第 1 页（reload 已把页码置 1，这里只需按结果刷新模板状态）
const reload = () => list.reload({
  city: query.city || undefined,
  district: query.district || undefined,
}).then(syncState);

// 把 usePagedList 的状态同步到模板用的 data：
// 成功即覆盖；失败清空并把总数置回 null（未取到结果），避免被渲染成「确实 0 家」
function syncState() {
  data.pageNum = list.state.pageNum;
  data.pageSize = list.state.pageSize;
  data.loading = list.state.loading;
  if (list.state.error) {
    // 业务码非 200 不经过响应拦截器（它只管网络/超时/HTTP 状态），故这里补一次提示
    ElMessage.error(list.state.error);
    data.error = true;
    data.filmData = [];
    data.total = null;
    return;
  }
  data.error = false;
  data.filmData = [...list.items.value];
  data.total = list.state.total;
}

const load = () => list.load().then(syncState);

// v-model 已把页码/条数写进 list.state，这里只需按新值取数
const handleSizeChange = () => list.load().then(syncState);
const handleCurrentChange = () => list.load().then(syncState);

loadRegions()
load()
</script>

<style scoped>
/* ---------- 页头 ---------- */
.cinema-masthead {
  background-color: var(--dark-bg-hero);
}

/* 页头内层与下方筛选/列表同一测度：测度本身在 front-pages.scss 的 .measure-inline，
   这里只补布局与内边距（.page-narrow 自带纵向 margin，页头里不能用）。 */
.cinema-masthead__inner {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-24);
  padding: var(--space-32) 0 var(--space-24);
}

.cinema-masthead__title {
  margin: 0;
  font-size: var(--fs-3xl);
  font-weight: var(--fw-bold);
  color: var(--dark-text);
}

/* 深底上的次级说明统一取 --dark-text-secondary（10.94:1），与各详情页 hero 同一个槽位 */
.cinema-masthead__desc {
  margin: var(--space-8) 0 0;
  font-size: var(--fs-base);
  color: var(--dark-text-secondary);
}

.cinema-masthead__count {
  display: flex;
  flex-shrink: 0;
  align-items: baseline;
  gap: var(--space-8);
}

/* 数字承载内容，故用 --dark-text（17.57:1）。
 * 不用 --color-brand：它在深底上做文字只有 3.81:1，不达 AA */
.cinema-masthead__num {
  font-size: var(--fs-5xl);
  font-weight: var(--fw-bold);
  line-height: 1;
  color: var(--dark-text);
}

.cinema-masthead__unit {
  font-size: var(--fs-base);
  color: var(--dark-text-secondary);
}

/* ---------- 筛选 ---------- */
/* 未选城市时区域行的说明文字。承载信息，用常规文字色（6.11:1）；
 * 不用 placeholder 档 —— 那是给占位符的，白底上不足 3:1 */
.filter-hint {
  font-size: var(--fs-base);
  color: var(--el-text-color-regular);
}

/* ---------- 列表 ---------- */
.cinema-list {
  margin-top: var(--space-20);
}

/* 骨架 */
.cinema-skeleton {
  display: flex;
  align-items: flex-start;
  gap: var(--space-20);
  margin-bottom: var(--space-16);
  padding: var(--space-16);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--el-border-radius-base);
}

.cinema-skeleton__poster {
  flex-shrink: 0;
  width: 240px;
  aspect-ratio: 10 / 7;
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-fill-color-light);
}

.cinema-skeleton__lines {
  flex: 1;
  min-width: 0;
  padding-top: var(--space-4);
}

.cinema-skeleton__line {
  height: 14px;
  margin-bottom: var(--space-12);
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-fill-color-light);
}

.cinema-skeleton__line--title {
  width: 40%;
  height: 20px;
}

.cinema-skeleton__line--short {
  width: 60%;
}

/* ---------- 影院卡片 ---------- */
.cinema-card {
  display: flex;
  align-items: flex-start;
  gap: var(--space-20);
  margin-bottom: var(--space-16);
  padding: var(--space-16);
  border: 1px solid var(--el-border-color-light);
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-bg-color);
  color: inherit;
  text-decoration: none;
  transition: transform 200ms ease-in-out, box-shadow 200ms ease-in-out, border-color 100ms ease-out;
}

.cinema-card:hover {
  transform: translateY(-2px);
  border-color: var(--el-color-primary-light-7);
  box-shadow: var(--el-box-shadow-light);
}

/* 整卡是 <a>（router-link），原生可 Tab 聚焦，焦点必须可见 */
.cinema-card:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}

/* 图片容器裁切圆角，缩放发生在里面的 <img> 上，卡片本身不跟着放大（文字不会糊） */
.cinema-card__poster {
  flex-shrink: 0;
  width: 240px;
  overflow: hidden;
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-fill-color-light);
}

.cinema-card__img {
  display: block;
  width: 100%;
  aspect-ratio: 10 / 7;
  object-fit: cover;
  transition: transform 300ms ease-out;
}

.cinema-card:hover .cinema-card__img {
  transform: scale(1.05);
}

.cinema-card__info {
  flex: 1;
  min-width: 0;
}

.cinema-card__head {
  display: flex;
  align-items: baseline;
  gap: var(--space-12);
}

/* 影院名过长时截断让位给右侧入口提示，而不是把提示挤出容器 */
.cinema-card__name {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  font-size: var(--fs-lg);
  font-weight: var(--fw-bold);
  color: var(--el-text-color-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 顶到行尾的入口提示。常规文字色 6.11:1，hover 转主色 5.58:1，两态都达 AA */
.cinema-card__enter {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: var(--space-4);
  margin-left: auto;
  font-size: var(--fs-sm);
  color: var(--el-text-color-regular);
  white-space: nowrap;
  transition: color 100ms ease-out, transform 200ms ease-in-out;
}

.cinema-card:hover .cinema-card__enter {
  color: var(--el-color-primary);
  transform: translateX(2px);
}

.cinema-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-8);
  margin-top: var(--space-12);
}

/* 底色由共享层的 .service-tag--* 提供（与影院详情页同色），这里只管形状 */
.service-tag {
  display: inline-flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4) var(--space-12);
  border-radius: var(--el-border-radius-base);
  font-size: var(--fs-xs);
  color: var(--color-on-accent);
}

.cinema-card__detail {
  margin-top: var(--space-12);
}

.cinema-card__detail-row {
  display: flex;
  align-items: center;
  gap: var(--space-8);
  margin: var(--space-8) 0;
  font-size: var(--fs-base);
  color: var(--el-text-color-regular);
}

/* 图标是纯装饰 —— 其后紧跟同义文字，故不另设可访问名 */
.cinema-card__detail-icon {
  flex-shrink: 0;
  color: var(--el-color-primary);
}

/* ---------- 空态 / 分页 ---------- */
.empty-hint__text {
  margin: 0;
}

.empty-hint__action {
  margin-top: var(--space-8);
}

.cinema-list__pagination {
  display: flex;
  justify-content: center;
  margin-top: var(--space-24);
  max-width: 100%;
}

/* 分页器不参与 flex 压缩：页数多时内部宽度分配会被挤到只显示当前页 */
.cinema-list__pagination :deep(.el-pagination) {
  flex-shrink: 0;
}
</style>
