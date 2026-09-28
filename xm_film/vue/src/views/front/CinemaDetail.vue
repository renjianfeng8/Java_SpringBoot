<template>
  <div class="cinema-detail">
    <!-- 1. 加载状态提示 -->
    <div v-if="loading" class="page-hint page-hint--loading">
      正在加载影院及电影信息...
    </div>

    <!-- 2. 错误提示 -->
    <div v-else-if="errorMsg" class="page-hint page-hint--error">
      <div>{{ errorMsg }}</div>
      <el-button class="page-hint__action" type="primary" @click="goBackToCinemaList">返回影院列表</el-button>
    </div>

    <!-- 3. 核心内容：影院完整信息 + 上线电影列表 -->
    <div v-else>
      <!-- 3.1 影院详情头部 -->
      <div class="cinema-hero">
        <div class="cinema-hero__inner">
          <!-- 影院图 -->
          <div class="cinema-hero__poster">
            <img
                :src="cinema.avatar"
                alt="影院图片"
                class="cinema-hero__img"
            >
          </div>

          <!-- 影院基本信息 -->
          <div class="cinema-hero__info">
            <div class="cinema-hero__name">{{ cinema.name || '未知影院' }}</div>

            <!-- 地址信息 -->
            <div class="cinema-hero__meta">
              <el-icon class="cinema-hero__meta-icon"><Location /></el-icon>
              地址: {{ cinema.address || '暂无地址信息' }}
            </div>

            <!-- 电话信息 -->
            <div class="cinema-hero__meta">
              <el-icon class="cinema-hero__meta-icon"><Phone /></el-icon>
              电话: {{ cinema.phone || '暂无联系电话' }}
            </div>

            <!-- 营业时间 -->
            <div class="cinema-hero__meta">
              <el-icon class="cinema-hero__meta-icon"><Clock /></el-icon>
              营业时间: {{ cinema.businessHours || '暂无营业时间信息' }}
            </div>

            <!-- 影院服务标题 -->
            <div class="cinema-hero__services-title">影院服务:</div>

            <!-- 服务标签容器 -->
            <div class="cinema-hero__services">
              <!-- 退票无忧 -->
              <div class="service-card">
                <div class="service-card__title service-card__title--refund">
                  <el-icon class="service-card__icon"><RefreshLeft /></el-icon>
                  退票无忧
                </div>
                <div class="service-card__desc">
                  未取票用户在放映前60分钟可退票
                </div>
              </div>

              <!-- 儿童优惠 -->
              <div class="service-card">
                <div class="service-card__title service-card__title--promo">
                  <el-icon class="service-card__icon"><User /></el-icon>
                  儿童优惠
                </div>
                <div class="service-card__desc">
                  1位成人可免费带1位不满1.3米儿童，儿童免票无座
                </div>
              </div>

              <!-- WiFi覆盖 -->
              <div class="service-card">
                <div class="service-card__title service-card__title--wifi">
                  <el-icon class="service-card__icon"><Connection /></el-icon>
                  WiFi覆盖
                </div>
                <div class="service-card__desc">
                  全场免费高速WiFi，观影期间也可顺畅连接
                </div>
              </div>

              <!-- 免费停车 -->
              <div class="service-card">
                <div class="service-card__title service-card__title--parking">
                  <el-icon class="service-card__icon"><Van /></el-icon>
                  免费停车
                </div>
                <div class="service-card__desc">
                  停车场位于长江西路辅路乐客来地面停车场和乐客来生活馆地下负二层均免费停车
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 3.2 核心：该影院上线的电影 -->
      <div class="films-section">
        <div class="films-panel">
          <!-- 电影列表标题 -->
          <h3 class="films-panel__title">
            【{{ cinema.name || '当前影院' }}】上映电影列表
          </h3>

          <!-- 电影列表内容 -->
          <div>
            <!-- 无电影数据提示 -->
            <div v-if="filmData.films.length === 0" class="empty-hint">
              <div class="empty-hint__title">暂无该影院的上映电影信息</div>
              <div class="empty-hint__desc">该影院可能暂未排片或暂无合作电影</div>
            </div>

            <!-- 电影列表容器（纵向排列，每个电影块包含海报信息+横向扩展的放映记录） -->
            <div class="film-container">
              <div v-for="film in filmData.films" :key="film.id" class="film-block">
                <!-- 电影基础信息（海报、名称、评分）- 左侧固定宽度 -->
                <div class="film-base-info">
                  <!-- 电影海报 -->
                  <div class="film-base-info__poster">
                    <img
                        :src="film.img"
                        alt="电影海报"
                        class="film-base-info__img"
                    >
                  </div>

                  <!-- 电影名称（图片下方） -->
                  <div class="film-base-info__title">
                    {{ film.title || '未知电影' }}
                  </div>

                  <!-- 评分（名称下方） -->
                  <div class="film-base-info__score">
                    <el-icon class="film-base-info__score-icon"><StarFilled /></el-icon>
                    {{ film.score }}分
                  </div>
                </div>

                <!-- 放映记录区域（右侧横向扩展，占满剩余宽度） -->
                <div class="film-record-section">
                  <!-- 放映记录标题 -->
                  <div class="film-record__title">
                    <el-icon class="film-record__title-icon"><VideoPlay /></el-icon>
                    放映场次
                  </div>

                  <!-- 放映记录加载中 -->
                  <div v-if="recordLoading[film.id]" class="record-placeholder">
                    <el-icon class="record-placeholder__icon"><Loading /></el-icon>
                    <div class="record-placeholder__text">加载中...</div>
                  </div>

                  <!-- 无放映记录 -->
                  <div v-else-if="!recordData[film.id] || recordData[film.id].list.length === 0" class="record-placeholder">
                    <el-icon><InfoFilled /></el-icon>暂无排片
                  </div>

                  <!-- 有放映记录（表格展开形式，横向铺满） -->
                  <div v-else class="record-table">
                    <!-- 表格头部（灰色背景，固定布局） -->
                    <div class="record-table__head">
                      <div class="record-table__col record-table__col--time">放映时间</div>
                      <div class="record-table__col record-table__col--room">影厅</div>
                      <div class="record-table__col record-table__col--price">售价</div>
                      <div class="record-table__col record-table__col--action">操作</div>
                    </div>
                    <!-- 表格内容（滚动容器） -->
                    <div class="record-table__body">
                      <div v-for="record in recordData[film.id].list" :key="record.id" class="record-table__row">
                        <!-- 放映时间（合并日期+时间） -->
                        <div class="record-table__col record-table__col--time record-table__cell--time">
                          {{ formatDate(record.start) }}<br>{{ formatTime(record.start) }}
                        </div>
                        <!-- 影厅名称 -->
                        <div class="record-table__col record-table__col--room record-table__cell--room">
                          {{ record.roomName || '未知影厅' }}
                        </div>
                        <!-- 售价 -->
                        <div class="record-table__col record-table__col--price record-table__cell--price">
                          {{ record.price }}
                        </div>
                        <!-- 操作：选座购票按钮/状态标签 -->
                        <div class="record-table__col record-table__col--action record-table__cell--action">
                          <!-- 可购票 = 未开场且未停售，与后端 RecordService.isPurchasable 同一规则 -->
                          <button
                              v-if="canBuy(record, film.time)"
                              class="buy-button"
                              @click="goToBuyTicket(cinemaId, film.id, record.id, record.roomId)"
                          >
                            选座购票
                          </button>
                          <!-- 其他状态：显示派生状态标签 + 不可点击提示 -->
                          <div v-else class="record-status">
                            <div :class="getStatusClass(recordState(record, film.time))" class="status-tag">
                              {{ recordState(record, film.time) }}
                            </div>
                            <span class="record-status__hint">不可购票</span>
                          </div>
                        </div>
                      </div>
                    </div>

                    <!-- 放映记录分页（底部居中，与表格对齐） -->
                    <div v-if="recordData[film.id] && recordData[film.id].total > recordData[film.id].pageSize"
                         class="record-table__pagination">
                      <el-pagination
                          @size-change="(val) => handleRecordSizeChange(film.id, val)"
                          @current-change="(val) => handleRecordCurrentChange(film.id, val)"
                          :current-page="recordData[film.id].pageNum"
                          :page-sizes="[5, 10, 15]"
                          :page-size="recordData[film.id].pageSize"
                          layout="total, sizes, prev, pager, next"
                          :total="recordData[film.id].total"
                          small
                      />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {reactive, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {ElMessage} from 'element-plus';
import {
  Clock, Connection, InfoFilled, Loading, Location, Phone, RefreshLeft, StarFilled, User, Van, VideoPlay,
} from '@element-plus/icons-vue';
import request from "@/utils/request.js";
import { API_PATHS, FILM_API, apiById, apiPage } from '@/constants';


// 1. 路由相关：参数提取与监听
const route = useRoute();
const router = useRouter();
const cinemaId = ref(Number(route.params.id));
const filmId = ref(route.query.filmId ? Number(route.query.filmId) : null);

// 2. 基础状态管理
const loading = ref(false);
const errorMsg = ref('');

// 影院ID无效或加载失败时，给出回到列表的出口，避免用户停留在死页面
const goBackToCinemaList = () => {
  router.push('/front/cinema');
};
const cinema = reactive({
  id: '',
  name: '',
  avatar: '',
  address: '',
  phone: '',
  rating: 0,
  hallCount: 0,
  todaySchedule: 0,
  businessHours: ''
});

// 3. 电影列表状态
const filmData = reactive({
  films: [],
  pageNum: 1,
  pageSize: 10,
  total: 0
});

// 4. 放映记录状态（按电影ID存储，支持多电影独立分页）
const recordLoading = ref({}); // 按电影ID存储加载状态
const recordData = reactive({}); // 格式：{ filmId: { list: [], pageNum: 1, pageSize: 5, total: 0 } }

// 6. 工具函数：日期格式化
// "2026-10-01 14:30:00" 在部分浏览器下 Date 无法解析，统一转成 ISO 形式
const toDate = (dateTimeStr) => {
  if (!dateTimeStr) return null;
  const date = new Date(String(dateTimeStr).replace(' ', 'T'));
  return Number.isNaN(date.getTime()) ? null : date;
};

const formatDate = (dateTimeStr) => {
  const date = toDate(dateTimeStr);
  if (!date) return '未知日期';
  return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`;
};

// 7. 工具函数：时间格式化
const formatTime = (dateTimeStr) => {
  const date = toDate(dateTimeStr);
  if (!date) return '未知时间';
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
};

// 7.1 场次展示状态由 start 派生（未开始/放映中/已结束），status 只作为人工停售开关
const DEFAULT_DURATION_MINUTES = 120;

const recordState = (record, durationMinutes) => {
  if (record.status === '停售') return '停售';
  const start = toDate(record.start);
  if (!start) return '未开始';
  const now = Date.now();
  if (now < start.getTime()) return '未开始';
  const duration = durationMinutes > 0 ? durationMinutes : DEFAULT_DURATION_MINUTES;
  return now < start.getTime() + duration * 60 * 1000 ? '放映中' : '已结束';
};

const canBuy = (record, durationMinutes) => recordState(record, durationMinutes) === '未开始';

// 8. 状态样式：返回语义类名，具体色值由 scoped 样式经令牌给出（规范 §3.3：承载文字须达 AA）
const getStatusClass = (status) => {
  switch (status) {
    case '未开始':
      return 'status-tag--upcoming';
    case '放映中':
      return 'status-tag--playing';
    case '停售':
    case '已结束':
      return 'status-tag--ended';
    default:
      return 'status-tag--default';
  }
};

// 9. 核心函数：加载影院详情
const fetchCinemaInfo = () => {
  const validCinemaId = Number(cinemaId.value);
  if (isNaN(validCinemaId) || validCinemaId <= 0) {
    errorMsg.value = `影院ID无效（当前值：${cinemaId.value}），请返回影院列表重试`;
    return Promise.reject('影院ID无效');
  }

  loading.value = true;
  return request.get(apiById(API_PATHS.CINEMAS, validCinemaId))
      .then(res => {
        if (res.code === '200' && res.data) {
          const {services, ...cinemaInfo} = res.data;
          Object.assign(cinema, cinemaInfo);
          return Promise.resolve();
        } else {
          const errMsg = `影院信息加载失败：${res.msg || '未找到该影院'}`;
          errorMsg.value = errMsg;
          ElMessage.error(errMsg);
          return Promise.reject(errMsg);
        }
      })
      .catch(err => {
        const errMsg = `网络异常：${err.message || '无法加载影院信息'}`;
        errorMsg.value = errMsg;
        ElMessage.error(errMsg);
        return Promise.reject(errMsg);
      })
      .finally(() => loading.value = false);
};

// 10. 核心函数：加载电影列表
const loadFilmList = () => {
  const validCinemaId = Number(cinemaId.value);
  if (isNaN(validCinemaId) || validCinemaId <= 0) {
    loading.value = false;
    errorMsg.value = `影院ID无效（当前值：${cinemaId.value}），无法加载电影列表`;
    return;
  }

  loading.value = true;
  const requestParams = {
    cinemaId: validCinemaId,
    pageNum: filmData.pageNum,
    pageSize: filmData.pageSize
  };
  if (filmId.value && !isNaN(Number(filmId.value))) {
    requestParams.filmId = Number(filmId.value);
  }

  request.get(FILM_API.BY_CINEMA, {params: requestParams})
      .then(res => {
        if (res.code === '200') {
          filmData.films = res.data || [];
          filmData.total = filmData.films.length;

          // 加载每个电影的放映记录（默认每页5条，提升展示效率）
          filmData.films.forEach(film => {
            fetchRecordList(validCinemaId, film.id);
          });

          // 定位目标电影（滚动到对应电影块）
          if (filmId.value && filmData.films.length > 0) {
            const targetIndex = filmData.films.findIndex(film => Number(film.id) === filmId.value);
            if (targetIndex > -1) {
              setTimeout(() => {
                const targetItem = document.querySelectorAll('.film-block')[targetIndex];
                if (targetItem) {
                  targetItem.scrollIntoView({behavior: 'smooth', block: 'start'});
                }
              }, 300);
            } else {
              ElMessage.info(`未找到ID为${filmId.value}的电影排片`);
            }
          }
        } else {
          const errMsg = `电影列表加载失败：${res.msg || '未知错误'}`;
          ElMessage.error(errMsg);
          filmData.films = [];
          filmData.total = 0;
        }
      })
      .catch(err => {
        const errMsg = `网络异常：${err.message || '无法加载电影列表'}`;
        ElMessage.error(errMsg);
        filmData.films = [];
        filmData.total = 0;
      })
      .finally(() => loading.value = false);
};

// 11. 核心函数：加载指定影院+电影的放映记录
const fetchRecordList = (cinemaId, filmId) => {
  // 初始化当前电影的放映记录状态（默认每页5条）
  if (!recordData[filmId]) {
    recordData[filmId] = {
      list: [],
      pageNum: 1,
      pageSize: 5, // 优化：默认每页显示5条，减少分页操作
      total: 0
    };
  }

  recordLoading.value[filmId] = true;
  // 构造请求参数（匹配后端Record实体类）
  const requestParams = {
    cinemaId: cinemaId,   // 与后端 Record 实体的 cinemaId 字段对应
    filmId: filmId,       // 与后端 Record 实体的 filmId 字段对应
    pageNum: recordData[filmId].pageNum,
    pageSize: recordData[filmId].pageSize
  };
  request.get(apiPage(API_PATHS.RECORDS), {params: requestParams})
      .then(res => {
        if (res.code === '200' && res.data) {
          // 适配后端PageInfo格式：{ list: [], total: 0, pageNum: 1, pageSize: 5 }
          recordData[filmId].list = res.data.list || [];
          recordData[filmId].total = res.data.total || 0;
          recordData[filmId].pageNum = res.data.pageNum || 1;
          recordData[filmId].pageSize = res.data.pageSize || 5;
        } else {
          const errMsg = `放映记录加载失败：${res.msg || '未知错误'}`;
          ElMessage.warning(errMsg);
          recordData[filmId].list = [];
          recordData[filmId].total = 0;
        }
      })
      .catch(err => {
        const errMsg = `网络异常：${err.message || '无法加载放映记录'}`;
        ElMessage.error(errMsg);
        recordData[filmId].list = [];
        recordData[filmId].total = 0;
      })
      .finally(() => {
        recordLoading.value[filmId] = false;
      });
};

// 12. 放映记录分页事件处理
// 每页条数改变
const handleRecordSizeChange = (filmId, pageSize) => {
  recordData[filmId].pageSize = pageSize;
  recordData[filmId].pageNum = 1; // 重置为第一页
  fetchRecordList(cinemaId.value, filmId);
};

// 当前页改变
const handleRecordCurrentChange = (filmId, pageNum) => {
  recordData[filmId].pageNum = pageNum;
  fetchRecordList(cinemaId.value, filmId);
};

// 13. 购票跳转函数
const goToBuyTicket = (cinemaId, filmId, recordId, roomId) => {
  if (isNaN(cinemaId) || isNaN(filmId) || isNaN(recordId)) {
    ElMessage.warning('参数无效，无法购票');
    return;
  }

  // 补充roomId默认值（避免null/0，默认传1=一号厅）
  const finalRoomId = roomId && roomId > 0 ? roomId : 1;

  const query = {
    cinemaId: cinemaId.toString(),
    filmId: filmId.toString(),
    recordId: recordId.toString(),
    roomId: finalRoomId.toString() // 新增：携带影厅ID
  };

  router.push({path: '/front/buyTicket', query}).catch(err => {
    if (err.name !== 'NavigationDuplicated') ElMessage.error('跳转失败，请稍后重试');
  });
};

// 14. 路由监听（影院/电影ID变化时重新加载数据）
watch([() => route.params.id, () => route.query.filmId], ([newCinemaId, newFilmId]) => {
  cinemaId.value = Number(newCinemaId);
  filmId.value = newFilmId ? Number(newFilmId) : null;
  // 先加载影院信息，再加载电影列表，最后加载放映记录
  fetchCinemaInfo().then(() => loadFilmList());
}, {immediate: true, deep: true});
</script>

<style scoped>
/* 页面容器：详情页内容最大宽度 1200px（规范 §7.1） */
.cinema-detail {
  width: 100%;
  margin: 0 auto;
}

/* 加载 / 错误提示 */
.page-hint {
  text-align: center;
  color: var(--el-text-color-regular);
}

.page-hint--loading {
  padding: var(--space-48);
}

.page-hint--error {
  padding: var(--space-20);
  color: var(--el-color-danger);
}

.page-hint__action {
  margin-top: var(--space-16);
}

/* ---------- 3.1 影院详情头部（深色表面，见规范 §2.7） ---------- */
.cinema-hero {
  background-color: var(--dark-bg-hero);
}

.cinema-hero__inner {
  display: flex;
  align-items: flex-start;
  width: 60%;
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--space-20) 0;
}

.cinema-hero__poster {
  margin-top: var(--space-4);
  flex-shrink: 0;
}

.cinema-hero__img {
  width: 250px;
  height: 300px;
  object-fit: cover;
  border-radius: var(--el-border-radius-base);
}

.cinema-hero__info {
  flex: 2;
  margin-left: var(--space-24);
  margin-top: var(--space-16);
  color: var(--dark-text);
}

.cinema-hero__name {
  font-size: var(--fs-3xl);
  font-weight: var(--fw-bold);
}

.cinema-hero__meta {
  display: flex;
  align-items: center;
  margin: var(--space-8) 0;
  font-size: var(--fs-base);
}

.cinema-hero__meta-icon {
  margin-right: var(--space-8);
}

.cinema-hero__services-title {
  margin: var(--space-12) 0 var(--space-8);
  font-size: var(--fs-md);
  font-weight: var(--fw-bold);
}

.cinema-hero__services {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-12);
  margin-bottom: var(--space-12);
}

.service-card {
  box-sizing: border-box;
  width: calc(50% - var(--space-4));
  padding: var(--space-8) var(--space-12);
  border-radius: var(--el-border-radius-base);
  background-color: rgba(255, 255, 255, 0.1);
}

/* 服务标签：与影院列表页（front/Cinema.vue）同一套功能色。
 * 统一用「功能色作底 + 白字」—— 该组合在浅底与深底上都达 AA，
 * 两页因此可以共用一组配色，不再各写一套。 */
.service-card__title {
  display: inline-flex;
  align-items: center;
  margin-bottom: var(--space-8);
  padding: var(--space-4) var(--space-12);
  border-radius: var(--el-border-radius-base);
  font-size: var(--fs-xs);
  font-weight: var(--fw-bold);
  color: var(--color-on-accent);
}

.service-card__title--refund {
  background-color: var(--el-color-primary);
}

.service-card__title--promo {
  background-color: var(--el-color-warning);
}

.service-card__title--wifi {
  background-color: var(--el-color-info);
}

.service-card__title--parking {
  background-color: var(--el-color-success);
}

.service-card__icon {
  margin-right: var(--space-8);
}

.service-card__desc {
  font-size: var(--fs-xs);
  line-height: var(--lh-base);
  opacity: 0.9;
}

/* ---------- 3.2 上映电影列表 ---------- */
.films-section {
  padding: var(--space-32) 0;
  background-color: var(--el-fill-color-lighter);
}

.films-panel {
  width: 60%;
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--space-20);
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
}

.films-panel__title {
  margin: 0 0 var(--space-20);
  padding-left: var(--space-12);
  border-left: 4px solid var(--color-brand);
  font-size: var(--fs-lg);
  font-weight: var(--fw-bold);
  color: var(--el-text-color-primary);
}

.empty-hint {
  padding: var(--space-48);
  border: 1px dashed var(--el-border-color-lighter);
  border-radius: var(--el-border-radius-base);
  text-align: center;
  color: var(--el-text-color-regular);
}

.empty-hint__title {
  margin-bottom: var(--space-12);
  font-size: var(--fs-md);
}

.empty-hint__desc {
  font-size: var(--fs-base);
  opacity: 0.7;
}

/* 电影容器：纵向排列每个电影块 */
.film-container {
  display: flex;
  flex-direction: column;
  gap: var(--space-24);
}

/* 电影块：横向布局（基础信息+放映记录） */
.film-block {
  display: flex;
  gap: var(--space-20);
  align-items: flex-start;
  padding: var(--space-16);
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-fill-color-lighter);
  transition: box-shadow 200ms ease-in-out;
}

/* 放映记录区域：右侧横向扩展，占满剩余宽度 */
.film-record-section {
  flex: 1;
  width: calc(100% - 160px);
}

/* 电影基础信息容器（左侧固定宽度） */
.film-base-info {
  width: 140px;
}

.film-base-info__poster {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 140px;
  height: 200px;
  margin-bottom: var(--space-8);
  border-radius: var(--el-border-radius-base);
  box-shadow: var(--el-box-shadow-lighter);
  overflow: hidden;
}

.film-base-info__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 200ms ease-in-out;
}

.film-base-info__img:hover {
  transform: scale(1.05);
}

.film-base-info__title {
  width: 140px;
  margin-bottom: var(--space-4);
  font-size: var(--fs-base);
  font-weight: var(--fw-bold);
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 评分文字：白底须用评分文字色（规范 §2.8） */
.film-base-info__score {
  display: flex;
  align-items: center;
  width: 140px;
  margin-bottom: var(--space-8);
  font-size: var(--fs-md);
  font-weight: var(--fw-bold);
  color: var(--color-rating-text);
}

.film-base-info__score-icon {
  margin-right: var(--space-4);
  font-size: var(--fs-base);
}

.film-record__title {
  display: flex;
  align-items: center;
  margin-bottom: var(--space-8);
  font-weight: var(--fw-bold);
  color: var(--el-text-color-regular);
}

.film-record__title-icon {
  margin-right: var(--space-4);
  font-size: var(--fs-xs);
}

.record-placeholder {
  padding: var(--space-20) 0;
  border: 1px dashed var(--el-border-color-lighter);
  border-radius: var(--el-border-radius-base);
  text-align: center;
  color: var(--el-text-color-regular);
}

.record-placeholder__icon {
  font-size: var(--fs-md);
  animation: rotating 2s linear infinite;
}

.record-placeholder__text {
  margin-top: var(--space-4);
}

/* 放映记录表格 */
.record-table {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--el-border-radius-base);
  overflow: hidden;
}

.record-table__head {
  display: flex;
  padding: var(--space-8) var(--space-16);
  border-bottom: 1px solid var(--el-border-color-lighter);
  background-color: var(--el-fill-color-light);
  font-weight: var(--fw-bold);
  color: var(--el-text-color-regular);
}

.record-table__body {
  max-height: 200px;
  overflow-y: auto;
}

.record-table__row {
  display: flex;
  align-items: center;
  padding: var(--space-12) var(--space-16);
  border-bottom: 1px solid var(--el-fill-color);
}

.record-table__col {
  text-align: center;
}

.record-table__col--time {
  width: 25%;
}

.record-table__col--room {
  width: 20%;
}

.record-table__col--price {
  width: 15%;
}

.record-table__col--action {
  width: 40%;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--space-12);
}

.record-table__cell--time {
  color: var(--el-text-color-primary);
  font-size: var(--fs-sm);
}

.record-table__cell--room {
  color: var(--el-text-color-regular);
  font-size: var(--fs-sm);
}

.record-table__cell--price {
  color: var(--el-color-primary);
  font-weight: var(--fw-bold);
  font-size: var(--fs-sm);
}

.buy-button {
  padding: var(--space-4) var(--space-12);
  border: none;
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-color-primary);
  color: var(--color-on-accent);
  font-size: var(--fs-xs);
  cursor: pointer;
  transition: background-color 100ms ease-out;
}

.buy-button:hover {
  background-color: var(--el-color-primary-dark-2);
}

.record-status {
  display: flex;
  align-items: center;
  gap: var(--space-8);
}

.record-status__hint {
  color: var(--el-text-color-regular);
  font-size: var(--fs-xs);
}

/* 状态标签：语义类，色值经令牌且承载文字须达 AA（规范 §3.3） */
.status-tag {
  display: inline-block;
  padding: var(--space-4) var(--space-8);
  border-radius: var(--el-border-radius-small);
  font-size: var(--fs-xs);
}

.status-tag--upcoming {
  color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
}

.status-tag--playing {
  color: var(--el-color-success);
  background-color: var(--el-color-success-light-9);
}

.status-tag--ended {
  color: var(--el-color-info);
  background-color: var(--el-color-info-light-9);
}

.status-tag--default {
  color: var(--el-color-warning);
  background-color: var(--el-color-warning-light-9);
}

.record-table__pagination {
  margin-top: var(--space-8);
  padding: var(--space-8) 0;
  border-top: 1px solid var(--el-border-color-lighter);
  background-color: var(--el-fill-color-lighter);
  text-align: center;
}
</style>
