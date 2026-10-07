<template>
  <div class="cinema-detail">
    <!-- 1. 加载骨架：影院图是横版（10:7），骨架必须同形，数据到位时不跳高 -->
    <DetailSkeleton v-if="loading" wide />

    <!-- 2. 错误提示：影院信息拿不到就没有页面主体，只给一条回列表的出口 -->
    <div v-else-if="errorMsg" class="page-hint page-hint--error">
      <div>{{ errorMsg }}</div>
      <el-button class="page-hint__action" type="primary" @click="goBackToCinemaList">返回影院列表</el-button>
    </div>

    <!-- 3. 核心内容：影院信息横幅 + 日期条 + 上映影片的场次 -->
    <template v-else>
      <!-- 3.1 影院信息横幅（深色表面，见规范 §3.5） -->
      <div class="cinema-hero">
        <!-- 模糊影院图铺底给纯色头图做出层次。纯装饰，故 alt 留空并 aria-hidden -->
        <img v-if="cinema.avatar" :src="cinema.avatar" alt="" aria-hidden="true" class="cinema-hero__backdrop">
        <div class="cinema-hero__scrim" aria-hidden="true"></div>

        <div class="cinema-hero__inner">
          <div class="cinema-hero__poster">
            <img
                :src="cinema.avatar"
                :alt="`${cinema.name || '影院'} 图片`"
                class="cinema-hero__img"
            >
          </div>

          <div class="cinema-hero__info">
            <h1 class="cinema-hero__name">{{ cinema.name || '未知影院' }}</h1>

            <div class="cinema-hero__meta">
              <el-icon class="cinema-hero__meta-icon"><Location /></el-icon>
              {{ cinema.address || '暂无地址信息' }}
            </div>

            <div class="cinema-hero__meta">
              <el-icon class="cinema-hero__meta-icon"><Phone /></el-icon>
              {{ cinema.phone || '暂无联系电话' }}
            </div>

            <!-- 今日可购场次：由已取回的场次客户端计数，是真实派生值而非装饰数。
                 为 0 时整行不渲染 —— 写「今日 0 场」只是噪音 -->
            <div v-if="todaySessionCount" class="cinema-hero__meta">
              <el-icon class="cinema-hero__meta-icon"><Clock /></el-icon>
              今日可购 {{ todaySessionCount }} 场
            </div>

            <!-- 影院服务。底色走共享层的 .service-tag--*，与影院列表页同色（规范 §3.6） -->
            <div class="cinema-hero__services">
              <div class="service-item">
                <div class="service-item__title service-tag--refund">
                  <el-icon class="service-item__icon"><RefreshLeft /></el-icon>
                  退票无忧
                </div>
                <div class="service-item__desc">未取票用户在放映前 60 分钟可退票</div>
              </div>

              <div class="service-item">
                <div class="service-item__title service-tag--promo">
                  <el-icon class="service-item__icon"><User /></el-icon>
                  儿童优惠
                </div>
                <div class="service-item__desc">1 位成人可免费带 1 位不满 1.3 米儿童，免票无座</div>
              </div>

              <div class="service-item">
                <div class="service-item__title service-tag--wifi">
                  <el-icon class="service-item__icon"><Connection /></el-icon>
                  WiFi覆盖
                </div>
                <div class="service-item__desc">全场免费高速 WiFi，观影期间也可顺畅连接</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 3.2 日期条 + 上映影片场次 -->
      <div class="films-section">
        <div class="films-inner">
          <!-- 日期条：7 天固定窗口，选中项由三通道（颜色 + 字重 + 下划线）表达，
               视觉之外另挂 aria-pressed（规范 §3.7 禁止仅靠颜色传递状态） -->
          <div v-if="!filmsError && films.length" class="date-bar" role="group" aria-label="选择放映日期">
            <button
                v-for="tab in dateTabs"
                :key="tab.key"
                type="button"
                class="date-tab"
                :class="{ 'date-tab--active': tab.key === selectedDate }"
                :aria-pressed="tab.key === selectedDate"
                @click="selectedDate = tab.key"
            >
              <span class="date-tab__week">{{ tab.week }}</span>
              <span class="date-tab__date">{{ tab.date }}</span>
            </button>
          </div>

          <!-- 影片列表加载失败：与「暂无排片」是两回事，文案必须分开（规则 72） -->
          <div v-if="filmsError" class="empty-hint">数据加载失败，请稍后重试</div>

          <!-- 该影院一部片都没排 -->
          <div v-else-if="!films.length" class="empty-panel">
            <div class="empty-panel__title">暂无该影院的上映电影信息</div>
            <div class="empty-panel__desc">该影院可能暂未排片或暂无合作电影</div>
          </div>

          <!-- 有排片但所选日期无未开场场次 -->
          <div v-else-if="!rows.length" class="empty-panel">
            <div class="empty-panel__title">所选日期暂无场次</div>
            <div class="empty-panel__desc">试试切换上方其他日期</div>
          </div>

          <div v-else class="film-list">
            <article
                v-for="row in rows"
                :key="row.film.id"
                class="film-row"
                :data-film-id="row.film.id"
            >
              <div class="film-row__poster">
                <img
                    :src="row.film.img"
                    :alt="`《${row.film.title || '影片'}》海报`"
                    class="film-row__img"
                >
              </div>

              <div class="film-row__main">
                <div class="film-row__head">
                  <h2 class="film-row__title">{{ row.film.title || '未知电影' }}</h2>
                  <div v-if="row.meta" class="film-row__meta">{{ row.meta }}</div>
                </div>

                <div class="film-row__score">
                  <el-icon class="film-row__score-icon"><StarFilled /></el-icon>
                  {{ formatScore(row.film.score) }}
                </div>

                <!-- 该片场次请求失败：保留这一行并显式说明，不当作「无场次」静默隐藏 -->
                <div v-if="row.failed" class="film-row__error">场次加载失败</div>

                <div v-else class="showtime-list">
                  <button
                      v-for="record in row.sessions"
                      :key="record.id"
                      type="button"
                      class="showtime"
                      :title="`${formatTime(record.start)} ${record.roomName || '未知影厅'}`"
                      @click="goToBuyTicket(row.film.id, record)"
                  >
                    <span class="showtime__time">{{ formatTime(record.start) }}</span>
                    <span class="showtime__room">{{ record.roomName || '未知影厅' }}</span>
                  </button>
                </div>
              </div>
            </article>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  Clock, Connection, Location, Phone, RefreshLeft, StarFilled, User,
} from '@element-plus/icons-vue';
import request from "@/utils/request.js";
import { formatScore } from '@/utils/format.js';
import { API_PATHS, FILM_API, apiById, apiPage } from '@/constants';

// 1. 路由相关：参数提取与监听
const route = useRoute();
const router = useRouter();
const cinemaId = ref(Number(route.params.id));
const filmId = ref(route.query.filmId ? Number(route.query.filmId) : null);

// 2. 基础状态
const loading = ref(false);
const errorMsg = ref('');

// 只挑渲染要用的字段，不整包 Object.assign —— 影院详情接口返回的是 Cinema 全字段，
// 它继承 Account，带 role / token / newPassword 这类与前台无关的键
const cinema = reactive({
  id: '',
  name: '',
  avatar: '',
  address: '',
  phone: '',
});

// 3. 影片与场次状态
const films = ref([]);
const filmsError = ref(false);
const sessionsByFilm = reactive({});  // { [filmId]: Record[] }：该片全部场次
const sessionError = reactive({});    // { [filmId]: true }：该片场次请求失败

// 4. 日期条状态
const DATE_TAB_DAYS = 7;
const WEEK_LABELS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
const selectedDate = ref('');

// 5. 工具函数：日期
// 日期键一律用本地时区的 YYYY-MM-DD。不用 toISOString().slice(0,10) —— 那是 UTC，
// 东八区下午场会被算到前一天
const dateKeyOf = (date) => {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
};

// 场次时间形如 "2026-10-07 14:30:00"，取前 10 位即日期，不依赖 Date 解析
const recordDateKey = (start) => String(start || '').slice(0, 10);

// "2026-10-07 14:30:00" 在部分浏览器下 Date 无法解析，统一转成 ISO 形式
const toDate = (dateTimeStr) => {
  if (!dateTimeStr) return null;
  const date = new Date(String(dateTimeStr).replace(' ', 'T'));
  return Number.isNaN(date.getTime()) ? null : date;
};

const formatTime = (dateTimeStr) => {
  const date = toDate(dateTimeStr);
  if (!date) return '--:--';
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
};

// 6. 日期条：今天起 7 天
// 用 new Date() 现算，故跨零点不会自动翻页 —— 与场次可购性的判定方式一致（渲染时取当前时刻）
const dateTabs = computed(() => {
  const base = new Date();
  base.setHours(0, 0, 0, 0);
  return Array.from({ length: DATE_TAB_DAYS }, (_, offset) => {
    const day = new Date(base);
    day.setDate(base.getDate() + offset);
    const key = dateKeyOf(day);
    return {
      key,
      week: offset === 0 ? '今天' : offset === 1 ? '明天' : WEEK_LABELS[day.getDay()],
      date: key.slice(5),
    };
  });
});

// 7. 可购性：与后端 RecordService.isPurchasable 同一规则（start 晚于当前 且 status != 停售）。
// 只渲染可购场次，所以这里不派生「放映中 / 已结束」——那些场次整条不出现
const canBuy = (record) => {
  if (!record || record.status === '停售') return false;
  const start = toDate(record.start);
  return start !== null && start.getTime() > Date.now();
};

// 8. 影片副信息：时长 · 语言 · 格式
// 刻意不含类型 —— films/by-cinema 不填 typeList（FilmService.selectByCinema 未调 fillFilmTypes），
// 而类型是后端派生字段，前端没有第二种取法（见 Bug.md 规则 87）
const filmMeta = (film) => {
  const parts = [];
  if (film.time) parts.push(`${film.time}分钟`);
  if (film.language) parts.push(film.language);
  if (film.resolution) parts.push(film.resolution);
  return parts.join(' · ');
};

// 9. 当前选中日期下要渲染的影片行
// 该日无场次的影片整行不渲染（不是显示「暂无排片」——
// 一屏十行「暂无排片」比不显示更吵）。请求失败的行例外，见下
const rows = computed(() => films.value
    .map(film => ({
      film,
      meta: filmMeta(film),
      failed: Boolean(sessionError[film.id]),
      // 后端已按「未来场次在前、start 升序」排好（RecordMapper.xml），前端不再重排
      sessions: (sessionsByFilm[film.id] || []).filter(
          record => recordDateKey(record.start) === selectedDate.value && canBuy(record),
      ),
    }))
    .filter(row => row.sessions.length > 0 || row.failed));

// 10. 影院横幅上的「今日可购 N 场」
const todaySessionCount = computed(() => {
  const key = dateKeyOf(new Date());
  return films.value.reduce((sum, film) => sum
      + (sessionsByFilm[film.id] || []).filter(
          record => recordDateKey(record.start) === key && canBuy(record),
      ).length, 0);
});

// 11. 影院信息
const goBackToCinemaList = () => {
  router.push('/front/cinema');
};

const fetchCinemaInfo = async () => {
  const validCinemaId = Number(cinemaId.value);
  if (!Number.isInteger(validCinemaId) || validCinemaId <= 0) {
    errorMsg.value = `影院ID无效（当前值：${cinemaId.value}），请返回影院列表重试`;
    return;
  }

  try {
    const res = await request.get(apiById(API_PATHS.CINEMAS, validCinemaId));
    if (res.code === '200' && res.data) {
      Object.assign(cinema, {
        id: res.data.id,
        name: res.data.name,
        avatar: res.data.avatar,
        address: res.data.address,
        phone: res.data.phone,
      });
    } else {
      errorMsg.value = `影院信息加载失败：${res.msg || '未找到该影院'}`;
    }
  } catch {
    // 网络 / 超时 / 5xx 的提示由 request.js 的响应拦截器统一给出（规则 72），
    // 这里只落错误态 —— 页面内再弹一次就是同一次失败弹两遍
    errorMsg.value = '数据加载失败，请稍后重试';
  }
};

// 12. 影片列表（该影院有排片的影片）
const fetchFilms = async () => {
  filmsError.value = false;
  try {
    // 该接口返回完整列表，后端不分页：传 pageNum / pageSize 不会被读取
    const res = await request.get(FILM_API.BY_CINEMA, {
      params: { cinemaId: Number(cinemaId.value) },
    });
    if (res.code === '200' && Array.isArray(res.data)) {
      films.value = res.data;
    } else {
      films.value = [];
      filmsError.value = true;
    }
  } catch {
    films.value = [];
    filmsError.value = true;
  }
};

// 13. 每部影片的场次：一次取回该片全部场次
//
// 取全量而不是「按日期逐次查」的理由：日期条切换要 0 请求。若按日期查，
// 每切一天就要对每部影片各发一次请求（10 部片 = 10 次），切日期变成有延迟的操作。
// 代价是 SESSION_PAGE_SIZE 这个硬上限：某片场次多于此数会被静默截断，日期条相应缺天。
// 排片在 `record` 表里按影院+影片+日期分布，单片单影院远达不到这个量级，故取 200 留足余量
const SESSION_PAGE_SIZE = 200;

const fetchFilmSessions = async (filmIdValue) => {
  try {
    const res = await request.get(apiPage(API_PATHS.RECORDS), {
      params: {
        cinemaId: Number(cinemaId.value),
        filmId: filmIdValue,
        pageNum: 1,
        pageSize: SESSION_PAGE_SIZE,
      },
    });
    if (res.code === '200' && res.data) {
      sessionsByFilm[filmIdValue] = res.data.list || [];
      delete sessionError[filmIdValue];
    } else {
      // 业务码非 200 不经响应拦截器，但这里也不弹提示：一屏十部片失败就是十个弹窗
      sessionError[filmIdValue] = true;
    }
  } catch {
    sessionError[filmIdValue] = true;
  }
};

// 14. 深链：?filmId= 指向的影片
// 该片在最近 7 天里哪天还有未开场场次就选中那天，深链落点才不是空窗
const applyFilmDeepLink = async () => {
  const target = filmId.value;
  if (!target) return;

  if (!films.value.some(film => Number(film.id) === target)) {
    ElMessage.info('未找到该影片在本影院的排片');
    return;
  }

  const sessions = sessionsByFilm[target] || [];
  const hit = dateTabs.value.find(
      tab => sessions.some(record => recordDateKey(record.start) === tab.key && canBuy(record)),
  );
  if (!hit) {
    ElMessage.info('该影片近 7 天暂无场次');
    return;
  }

  selectedDate.value = hit.key;
  await nextTick();
  const targetRow = document.querySelector(`[data-film-id="${target}"]`);
  if (targetRow) targetRow.scrollIntoView({ behavior: 'smooth', block: 'start' });
};

// 15. 加载编排
// 首屏顺序：影院信息 → 影片列表 → 各片场次（并发）。任一步失败留着已取到的数据，
// 由对应的错误态分支承接，不整页抹掉（规则 86 ②）
// 最短骨架时长：本机三组请求百毫秒内就能回，骨架会一闪而过，读起来像没加载（规则 86）
const MIN_SKELETON_MS = 400;

const load = async () => {
  loading.value = true;
  films.value = [];
  filmsError.value = false;
  Object.keys(sessionError).forEach(key => delete sessionError[key]);
  Object.keys(sessionsByFilm).forEach(key => delete sessionsByFilm[key]);
  selectedDate.value = dateKeyOf(new Date());

  const startedAt = Date.now();
  try {
    await fetchCinemaInfo();
    if (!errorMsg.value) {
      await fetchFilms();
      if (!filmsError.value && films.value.length) {
        // 等齐再渲染：逐行落位会让行数反复跳
        await Promise.all(films.value.map(film => fetchFilmSessions(film.id)));
        await applyFilmDeepLink();
      }
    }
  } finally {
    const remaining = MIN_SKELETON_MS - (Date.now() - startedAt);
    if (remaining > 0) await new Promise(resolve => setTimeout(resolve, remaining));
    loading.value = false;
  }
};

// 16. 购票跳转
const goToBuyTicket = (targetFilmId, record) => {
  // roomId 缺失时不再静默兜底成"一号厅"——那会跳到不属于该场次的影厅，
  // 选座页按影厅边界校验座位必然失败，比明确拦住更糟
  if (!record.roomId || record.roomId <= 0) {
    ElMessage.warning('该场次缺少影厅信息，无法购票');
    return;
  }

  router.push({
    path: '/front/buyTicket',
    query: {
      cinemaId: String(cinemaId.value),
      filmId: String(targetFilmId),
      recordId: String(record.id),
      roomId: String(record.roomId),
    },
  });
};

// 17. 路由监听（影院 / 影片参数变化时整体重载）
watch([() => route.params.id, () => route.query.filmId], ([newCinemaId, newFilmId]) => {
  cinemaId.value = Number(newCinemaId);
  filmId.value = newFilmId ? Number(newFilmId) : null;
  errorMsg.value = '';
  load();
}, { immediate: true });
</script>

<style scoped>
.cinema-detail {
  width: 100%;
}

/* 错误提示（加载态已改为骨架屏，见 front-pages.scss 的 .detail-skeleton） */
.page-hint {
  text-align: center;
  color: var(--el-text-color-regular);
}

.page-hint--error {
  padding: var(--space-20);
  color: var(--el-color-danger);
}

.page-hint__action {
  margin-top: var(--space-16);
}

/* ---------- 3.1 影院信息横幅（深色表面，见规范 §3.5） ---------- */
.cinema-hero {
  position: relative;
  overflow: hidden;
  background-color: var(--dark-bg-hero);
}

/* 模糊影院图铺底：把纯色块做出纵深。纯装饰块，故用 opacity 压淡（规则 80） */
.cinema-hero__backdrop {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: blur(24px);
  opacity: 0.3;
}

/* 叠影：左侧压暗保证文字底色稳定。渐变只含令牌与 transparent 关键字（§3.7） */
.cinema-hero__scrim {
  position: absolute;
  inset: 0;
  background-image: linear-gradient(
      90deg,
      var(--dark-bg-hero) 0%,
      var(--dark-bg-hero) 30%,
      transparent 100%
  );
}

/* 内容层只需 position: relative 即可压在铺底与叠影之上：
   三者 z-index 均为 auto，按 DOM 顺序绘制（§7.3） */
.cinema-hero__inner {
  position: relative;
  display: flex;
  align-items: flex-start;
  width: 60%;
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--space-32) 0;
}

.cinema-hero__poster {
  flex-shrink: 0;
}

/* 影院图取 10:7 横版，与影院列表页（front/Cinema.vue）同比例。
   影院没有「海报」，一张门脸照裁成 5:6 竖条是错的比例尺 */
.cinema-hero__img {
  display: block;
  width: 240px;
  aspect-ratio: 10 / 7;
  object-fit: cover;
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-fill-color-light);
}

.cinema-hero__info {
  flex: 1;
  min-width: 0;
  margin-left: var(--space-24);
  color: var(--dark-text);
}

.cinema-hero__name {
  margin: 0 0 var(--space-12);
  font-size: var(--fs-3xl);
  font-weight: var(--fw-bold);
}

.cinema-hero__meta {
  display: flex;
  align-items: center;
  margin: var(--space-8) 0;
  font-size: var(--fs-base);
  color: var(--dark-text-secondary);
}

.cinema-hero__meta-icon {
  margin-right: var(--space-8);
}

/* 服务：横排三列小卡。深色底上不设面层 —— §3.5 的深色族里没有「深底上的浅色面层」，
   --surface-glass 是 80% 白、专供压照片，用在这里会过亮。
   分组感由彩色标题片与间距提供，不依赖面层 */
.cinema-hero__services {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-16);
  margin-top: var(--space-16);
}

.service-item {
  min-width: 0;
}

/* 服务标签形状（带图标的片）。底色走共享层的 .service-tag--*，
   与影院列表页取同一组功能色，两页不再各写一套 */
.service-item__title {
  display: inline-flex;
  align-items: center;
  margin-bottom: var(--space-8);
  padding: var(--space-4) var(--space-12);
  border-radius: var(--el-border-radius-base);
  font-size: var(--fs-xs);
  font-weight: var(--fw-bold);
  color: var(--color-on-accent);
}

.service-item__icon {
  margin-right: var(--space-8);
}

/* 用 --dark-text-secondary（10.94:1）而不是 opacity 压淡：
   透明度是对比度的隐性扣减，令牌是可核对的档位（规则 80）。
   小字用 --lh-loose（§4.3） */
.service-item__desc {
  font-size: var(--fs-xs);
  line-height: var(--lh-loose);
  color: var(--dark-text-secondary);
}

/* ---------- 3.2 影片与场次 ---------- */
.films-section {
  padding: var(--space-32) 0;
}

.films-inner {
  width: 60%;
  max-width: 1200px;
  margin: 0 auto;
}

/* 日期条吸顶。前台顶栏没有定位、会随页面滚走（Front.vue），所以这里 top: 0
   就是视口顶端。z-index 取内容层 1：自定义 z-index 必须 < 1000（§7.3）。
   必须自带底色，否则滚过的影片行会从文字缝隙里透出来 */
.date-bar {
  position: sticky;
  top: 0;
  z-index: var(--el-index-normal);
  display: flex;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background-color: var(--el-bg-color);
}

/* 筛选项用 <button> 而非 div —— 原生可 Tab 聚焦、可回车/Space 触发（规范 §10.2）；
   也不禁掉浏览器默认聚焦描边（§9.1 要求焦点可见） */
.date-tab {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: var(--space-4);
  align-items: center;
  padding: var(--space-8) var(--space-4);
  border: none;
  border-bottom: 3px solid transparent;
  background: none;
  font-family: inherit;
  color: var(--el-text-color-regular);
  cursor: pointer;
  transition: color 100ms ease-out, border-color 100ms ease-out;
}

.date-tab:hover {
  color: var(--el-color-primary);
}

.date-tab__week {
  font-size: var(--fs-base);
}

.date-tab__date {
  font-size: var(--fs-xs);
}

/* 选中态三通道：颜色 + 字重 + 下划线。禁止仅靠颜色传递状态（§3.7） */
.date-tab--active {
  border-bottom-color: var(--el-color-primary);
  color: var(--el-color-primary);
}

.date-tab--active .date-tab__week {
  font-weight: var(--fw-bold);
}

.film-list {
  margin-top: var(--space-8);
}

.film-row {
  display: flex;
  gap: var(--space-20);
  align-items: flex-start;
  padding: var(--space-24) 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  /* 深链滚动到该行时，给吸顶的日期条让出高度，否则片名被压在日期条底下 */
  scroll-margin-top: var(--space-64);
}

.film-row:last-child {
  border-bottom: none;
}

.film-row__poster {
  flex-shrink: 0;
}

.film-row__img {
  display: block;
  width: 96px;
  height: 134px;
  object-fit: cover;
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-fill-color-light);
}

.film-row__main {
  flex: 1;
  min-width: 0;
}

.film-row__head {
  display: flex;
  align-items: baseline;
  gap: var(--space-12);
}

/* 片名过长时截断让位给右侧副信息，而不是把副信息挤出容器 */
.film-row__title {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  font-size: var(--fs-lg);
  font-weight: var(--fw-bold);
  color: var(--el-text-color-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 副信息顶到行尾，与片名同一基线 */
.film-row__meta {
  flex-shrink: 0;
  margin-left: auto;
  font-size: var(--fs-xs);
  color: var(--el-text-color-regular);
  white-space: nowrap;
}

/* 评分文字：白底须用评分文字色（规范 §3.6，4.68:1）。数字可用 --fw-medium，故不参与
   「含中文只能用 400/700」那条（§4.4） */
.film-row__score {
  display: flex;
  align-items: center;
  margin-top: var(--space-4);
  font-size: var(--fs-md);
  font-weight: var(--fw-medium);
  color: var(--color-rating-text);
}

.film-row__score-icon {
  margin-right: var(--space-4);
}

/* 场次加载失败：与「该日无场次」区分开，后者整行不渲染，前者保留并说明（规则 72） */
.film-row__error {
  margin-top: var(--space-12);
  font-size: var(--fs-base);
  color: var(--el-color-danger);
}

.showtime-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-12);
  margin-top: var(--space-16);
}

/* 场次块。形状取 4px 基准圆角而非 --el-border-radius-round（20px）：
   在 88×48 的块上 20px 已经把两行文字挤成药丸，圆角越大可读面积越小 */
.showtime {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  align-items: center;
  width: 88px;
  padding: var(--space-8) var(--space-4);
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-bg-color);
  font-family: inherit;
  cursor: pointer;
  transition: color 100ms ease-out, border-color 100ms ease-out, background-color 100ms ease-out;
}

.showtime:hover {
  border-color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
}

.showtime:active {
  border-color: var(--el-color-primary-dark-2);
}

/* hover / active 时内部文字随边框一起走：文字态不能用 light-3（§3.7），
   主色 5.58:1 与 dark-2 均达 AA */
.showtime:hover .showtime__time,
.showtime:hover .showtime__room {
  color: var(--el-color-primary);
}

.showtime:active .showtime__time,
.showtime:active .showtime__room {
  color: var(--el-color-primary-dark-2);
}

.showtime__time {
  font-size: var(--fs-md);
  font-weight: var(--fw-medium);
  color: var(--el-text-color-primary);
}

/* 影厅名承载文字，用常规文字色（6.11:1）；原来靠 opacity 压淡会把对比度降到 4.0:1 以下（§10.1） */
.showtime__room {
  max-width: 100%;
  overflow: hidden;
  font-size: var(--fs-xs);
  color: var(--el-text-color-regular);
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 带标题与说明的虚线空态面板。刻意不叫 .empty-hint ——
   那是共享层里「单行占位」的名字，同名装不同样式会按规则 79 玄学取胜负 */
.empty-panel {
  padding: var(--space-48);
  border: 1px dashed var(--el-border-color-lighter);
  border-radius: var(--el-border-radius-base);
  color: var(--el-text-color-regular);
  text-align: center;
}

.empty-panel__title {
  margin-bottom: var(--space-12);
  font-size: var(--fs-md);
}

.empty-panel__desc {
  font-size: var(--fs-base);
}
</style>
