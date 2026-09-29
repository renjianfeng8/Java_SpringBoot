<template>
  <!-- 整个页面统一外层盒子 -->
  <div class="buy-page">
    <div class="buy-panel">
      <!-- 标题：居中加粗放大 -->
      <div class="buy-title">
        座位选择
      </div>

      <!-- 主体布局：左右分栏 -->
      <div class="buy-layout">
        <!-- 左侧：座位选择区 -->
        <div class="buy-main">

          <!-- 座位状态图例 -->
          <div class="seat-legend">
            <div class="seat-legend__row">
              <div class="seat-legend__item">
                <div class="seat-swatch seat-swatch--taken"></div>
                <div class="seat-legend__label">已售座位</div>
              </div>
              <div class="seat-legend__item">
                <div class="seat-swatch seat-swatch--available"></div>
                <div class="seat-legend__label">可选座位</div>
              </div>
              <div class="seat-legend__item">
                <div class="seat-swatch seat-swatch--selected"></div>
                <div class="seat-legend__label">已选座位</div>
              </div>
              <!-- 本人未支付锁座：只有存在未支付订单时才出现 -->
              <div v-if="myPendingOrders.length > 0" class="seat-legend__item">
                <div class="seat-swatch seat-swatch--mine"></div>
                <div class="seat-legend__label">我的未支付</div>
              </div>
            </div>
          </div>

          <!-- 大荧幕 -->
          <div class="seat-screen"></div>

          <!-- 座位加载中/错误占位 -->
          <div v-if="loading" class="seat-hint">
            <el-icon class="seat-hint__icon seat-hint__icon--spin"><Loading /></el-icon>
            <div class="seat-hint__text">加载座位中...</div>
          </div>
          <div v-else-if="seatError" class="seat-hint seat-hint--error">
            <el-icon class="seat-hint__icon"><CircleCloseFilled /></el-icon>
            <div>{{ seatError }}</div>
            <el-button class="seat-hint__action" type="primary" @click="router.push('/front/cinema')">
              返回影院列表
            </el-button>
          </div>

          <!-- 座位矩阵：行列数取自所属影厅的 seat_rows / seat_cols 配置 -->
          <div v-else class="seat-map">
            <div v-for="row in seatRows" :key="'row' + row" class="seat-map__row">
              <div v-for="col in seatCols" :key="`seat-${row}-${col}`"
                   :class="[getSeatClass(row, col), isSeatAvailable(row, col) ? 'seat-item--clickable' : 'seat-item--locked']"
                   class="seat-item"
                   @click="selectSeat(row, col)"
              ></div>
            </div>
          </div>

          <!-- 本人未支付锁座：可直接继续支付或取消释放，不必重新选座 -->
          <div v-if="myPendingOrders.length > 0" class="pending-box">
            <div v-for="pending in myPendingOrders" :key="pending.id" class="pending-box__row">
              <span>未支付订单 {{ pending.orders }}（{{ pending.seat }}）</span>
              <span class="pending-box__actions">
                <el-button link type="warning" @click="continuePay(pending)">继续支付</el-button>
                <el-button link type="danger" @click="cancelPendingOrder(pending)">取消锁座</el-button>
              </span>
            </div>
          </div>

          <!-- 已选座位 -->
          <div class="selected-seats">
            <div class="selected-seats__row">
              <div class="selected-seats__label">座位:</div>
              <div class="selected-seats__body">
                <div v-if="selectedSeats.length === 0" class="selected-seats__empty">未选择座位</div>
                <div v-else class="selected-seats__list">
                   <span v-for="seat in selectedSeats" :key="seat" class="seat-chip"> {{ seat }}
                     <span class="seat-chip__remove" @click="removeSeat(seat)">×</span>
                   </span>
                </div>
              </div>
            </div>
          </div>

          <!-- 确认购票按钮：disabled 时同时挡住"无座位/未登录/加载中/提交中" -->
          <div class="submit-row">
            <button class="submit-button"
                    :disabled="!canSubmit"
                    @click="confirmBooking"
            >
              {{ submitting ? '提交中…' : `确认购票（${selectedSeats.length}张）` }}
            </button>
          </div>
        </div>

        <!-- 右侧：电影信息区 -->
        <div class="film-aside">
          <!-- 电影海报 -->
          <div class="film-aside__poster-wrap">
            <img :src="filmInfo.img " alt="电影海报" class="film-aside__poster">
          </div>

          <hr class="divider">

          <!-- 场次信息 -->
          <div class="info-block">
            <div class="info-block__title">场次信息</div>
            <div class="info-row">
              <span class="info-row__label">影院：</span>
              <span class="info-row__value">{{ cinemaInfo.name || '未知' }}</span>
            </div>
            <div class="info-row">
              <span class="info-row__label">时间：</span>
              <span class="info-row__value">{{ formatShowTime(showInfo.start) }}</span>
            </div>
            <div class="info-row">
              <span class="info-row__label">票价：</span>
              <span class="info-row__price">¥{{ showInfo.price || 0 }}/张</span>
            </div>
          </div>

          <hr class="divider">

          <!-- 订单汇总 -->
          <div class="total-box">
            <div class="info-block__title">订单汇总</div>
            <div class="info-row">
              <span class="info-row__label">座位数：</span>
              <span class="info-row__value">{{ selectedSeats.length }} 张</span>
            </div>
            <div class="total-box__row">
              <span class="total-box__label">总价：</span>
              <span class="total-box__value">¥{{ calculateTotalPrice() }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 支付弹窗：与订单列表页"继续支付"共用同一组件 -->
  <OrderPayDialog
      v-model="paymentDialogVisible"
      :order="currentOrder"
      @paid="onPaid"
      @cancelled="initSeats"
      @timeout="initSeats"
  />
</template>

<script setup>
import { computed, ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { CircleCloseFilled, Loading } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from "@/utils/request.js";
import { API_PATHS, ORDER_API, apiById } from '@/constants';
import { clearStoredUser, getStoredUser } from '@/utils/authStorage';
import OrderPayDialog from '@/components/OrderPayDialog.vue';

// 1. Read the current signed-in user from shared auth storage.
const userInfo = ref(null); // 存储登录用户完整信息
const isLogin = ref(false); // 是否登录标记

// 初始化并监听登录状态（页面刷新或登录状态变化时自动更新）
const initUserInfo = () => {
  try {
    const storedUser = getStoredUser();
    if (storedUser) {
      const parsedUser = storedUser;
      // 校验用户信息合法性（需包含id和role字段，与登录接口返回一致）
      if (parsedUser.id && parsedUser.role) {
        userInfo.value = parsedUser;
        isLogin.value = true;
        // 仅允许USER角色购票（与登录后跳转逻辑一致）
        if (parsedUser.role !== 'USER') {
          isLogin.value = false;
          seatError.value = '仅普通用户可进行购票操作';
        }
      } else {
        // 存储的用户信息不完整，清除无效数据
        clearStoredUser();
        isLogin.value = false;
      }
    } else {
      isLogin.value = false;
    }
  } catch (err) {
    // Clear invalid cached auth data.
    clearStoredUser();
    isLogin.value = false;
    ElMessage.warning('登录信息失效，请重新登录');
  }
};

// 路由参数接收
const route = useRoute();
const router = useRouter();
const { cinemaId, filmId, recordId, roomId } = route.query;

// 状态初始化
const loading = ref(true);
const seatError = ref('');
const seats = ref([]); // 座位矩阵：0=可选，1=他人占用，2=已选，3=本人未支付锁座
const selectedSeats = ref([]); // 已选座位（格式：["1排1座", ...]）
const seatRows = ref(8); // 选座图行数，取自场次所属影厅
const seatCols = ref(8); // 选座图列数
const myPendingOrders = ref([]); // 本人待支付订单，可继续支付或取消锁座

// 支付弹窗状态：倒计时与支付/取消逻辑由 OrderPayDialog 承担
const paymentDialogVisible = ref(false);
const currentOrder = ref(null);

// 下单在途标记：没有它时双击会发出两次 POST，第二个必然被"座位已售"拒绝
const submitting = ref(false);
const canSubmit = computed(() =>
    selectedSeats.value.length > 0 && !loading.value && isLogin.value && !submitting.value
);

// 支付成功后不再跳转：OrderPayDialog 就地切成取票凭证（取票码 + 「去取票大厅」）。
// 此前跳 /front/orders 只会看到「待取票」状态，用户不知道下一步该去哪出票。
// 这里只刷新选座图 —— 刚买的座位已从「本人未支付锁座」变为已售。
const onPaid = () => {
  initSeats();
};

// 对本人未支付订单继续支付：直接复用支付弹窗，不必重新选座
const continuePay = (order) => {
  currentOrder.value = order;
  paymentDialogVisible.value = true;
};

const cancelPendingOrder = (order) => {
  ElMessageBox.confirm('取消后座位将被释放，确认取消该未支付订单吗？', '取消确认', { type: 'warning' })
      .then(() => {
        request.put(ORDER_API.CANCEL(order.id)).then(res => {
          if (res.code === '200') {
            ElMessage.success('订单已取消');
            initSeats();
          } else {
            ElMessage.error(res.msg || '取消失败');
          }
        });
      })
      .catch(() => {});
};

// 数据存储（与后端实体字段对应）
const filmInfo = ref({
  id: '',
  title: '',
  img: '',
  type: '',
  director: '',
  actors: ''
});
const cinemaInfo = ref({
  id: '',
  name: '',
  address: ''
});
const showInfo = ref({
  id: '', // 场次ID（对应recordId）
  roomId: '', // 影厅ID（后端Ordered需要）
  roomName: '',
  start: '', // 放映时间（后端Ordered需要）
  price: 0
});

// 页面初始化：先校验登录状态，再加载数据
onMounted(() => {
  // 第一步：初始化登录状态
  initUserInfo();

  // 第二步：参数校验（与影院页逻辑一致）
  if (!cinemaId || !filmId || !recordId || isNaN(Number(cinemaId)) || isNaN(Number(filmId)) || isNaN(Number(recordId))) {
    seatError.value = '参数无效，无法加载购票信息';
    loading.value = false;
    ElMessage.error(seatError.value);
    setTimeout(() => router.push('/front/cinema'), 1500);
    return;
  }

  // 第三步：加载页面数据（仅登录且为USER角色时加载）
  if (isLogin.value) {
    // 座位图行列数取自场次所属影厅，必须先取到场次信息再初始化座位
    fetchBaseInfo()
        .then(() => initSeats())
        .then(() => {
          loading.value = false;
        })
        .catch(err => {
          loading.value = false;
          seatError.value = err.message || '数据加载失败，请稍后重试';
          ElMessage.error(seatError.value);
        });
  } else {
    // 未登录或非USER角色，停止加载数据
    loading.value = false;
    if (!seatError.value) {
      seatError.value = '请先登录普通用户账号进行购票';
    }
  }
});

// 初始化座位状态：0=可选 1=他人占用 2=已选 3=本人未支付锁座
const initSeats = () => {
  return new Promise((resolve) => {
    const rows = seatRows.value;
    const cols = seatCols.value;
    const emptyMatrix = () => Array(rows).fill().map(() => Array(cols).fill(0));

    request.get(ORDER_API.SEATS, {
      params: {
        recordId: Number(recordId)
      }
    }).then(res => {
      if (res.code !== '200') {
        seatError.value = res.msg || '座位数据加载失败';
        seats.value = emptyMatrix();
        resolve();
        return;
      }

      const seatMatrix = emptyMatrix();
      const myOrders = [];
      (res.data || []).forEach(order => {
        // 归属由后端按 JWT 判定，响应里不再有 userId / 他人订单明细
        const myPending = order.mine && order.status === '待支付';
        if (myPending) {
          // 支付弹窗读的是订单形态的对象，这里把选座视角的字段映射过去
          myOrders.push({
            id: order.orderId,
            orders: order.orders,
            seat: order.seat,
            status: order.status,
            total: order.total,
            pendingTimeoutAt: order.pendingTimeoutAt
          });
        }
        // 已被前一张订单标记的座位不覆盖，避免多订单重叠时着色抖动
        (order.seat || '').split(',').forEach(rawSeat => {
          const seat = rawSeat.trim();
          const rowMatch = seat.match(/(\d+)排/);
          const colMatch = seat.match(/排(\d+)座/);
          if (!rowMatch || !colMatch) return;
          const row = parseInt(rowMatch[1]);
          const col = parseInt(colMatch[1]);
          if (row < 1 || row > rows || col < 1 || col > cols) return;
          // 本人未支付锁座单独着色，其余一律按已占用处理
          seatMatrix[row - 1][col - 1] = myPending ? 3 : 1;
        });
      });

      seats.value = seatMatrix;
      myPendingOrders.value = myOrders;
      resolve();
    }).catch(() => {
      seatError.value = '座位数据加载失败，请稍后重试';
      seats.value = emptyMatrix();
      resolve();
    });
  });
};

// 加载基础信息（电影/影院/场次）
const fetchBaseInfo = () => {
  return Promise.all([
    // 电影信息（与影院页接口一致）
    request.get(apiById(API_PATHS.FILMS, Number(filmId))).then(res => {
      if (res.code === '200' && res.data) {
        filmInfo.value = res.data;
      } else {
        throw new Error(`电影信息加载失败：${res.msg || '未知错误'}`);
      }
    }),
    // 影院信息（与影院页接口一致）
    request.get(apiById(API_PATHS.CINEMAS, Number(cinemaId))).then(res => {
      if (res.code === '200' && res.data) {
        cinemaInfo.value = res.data;
      } else {
        throw new Error(`影院信息加载失败：${res.msg || '未知错误'}`);
      }
    }),
    // 场次信息（获取影厅、座位布局等关键字段）
    request.get(apiById(API_PATHS.RECORDS, Number(recordId))).then(res => {
      if (res.code === '200' && res.data) {
        showInfo.value = {
          id: res.data.id,
          roomId: res.data.roomId || 0, // 确保为数字，避免后端报错
          roomName: res.data.roomName,
          start: res.data.start,
          price: res.data.price || 0
        };
        // 座位图规模由所属影厅决定；后端未配置时退回 8×8
        seatRows.value = res.data.roomSeatRows > 0 ? res.data.roomSeatRows : 8;
        seatCols.value = res.data.roomSeatCols > 0 ? res.data.roomSeatCols : 8;
      } else {
        throw new Error(`场次信息加载失败：${res.msg || '未知错误'}`);
      }
    })
  ]);
};

// 座位操作工具函数
// 座位状态 → 语义类名；色值统一由 scoped 样式经令牌给出（规范 §3.7）
const getSeatClass = (row, col) => {
  const r = row - 1;
  const c = col - 1;
  if (!seats.value[r] || seats.value[r][c] === undefined) return 'seat-item--empty';
  switch (seats.value[r][c]) {
    case 0: return 'seat-item--available'; // 可选
    case 1: return 'seat-item--taken';     // 他人占用
    case 2: return 'seat-item--selected';  // 已选
    case 3: return 'seat-item--mine';      // 本人未支付锁座
    default: return 'seat-item--empty';
  }
};

const isSeatAvailable = (row, col) => {
  const r = row - 1;
  const c = col - 1;
  return seats.value[r] && seats.value[r][c] === 0;
};

const selectSeat = (row, col) => {
  // 未登录时禁止选座
  if (!isLogin.value) {
    ElMessage.warning('请先登录后选择座位');
    router.push('/login');
    return;
  }
  if (!isSeatAvailable(row, col)) return;

  const r = row - 1;
  const c = col - 1;
  const seatLabel = `${row}排${col}座`;

  seats.value[r][c] = 2;
  selectedSeats.value.push(seatLabel);
  ElMessage.success(`已选：${seatLabel}`);
};

const removeSeat = (seatLabel) => {
  const rowMatch = seatLabel.match(/(\d+)排/);
  const colMatch = seatLabel.match(/排(\d+)座/);
  if (!rowMatch || !colMatch) return;

  const row = parseInt(rowMatch[1]);
  const col = parseInt(colMatch[1]);
  const r = row - 1;
  const c = col - 1;

  seats.value[r][c] = 0;
  selectedSeats.value = selectedSeats.value.filter(s => s !== seatLabel);
  ElMessage.info(`已取消：${seatLabel}`);
};

// 辅助工具函数
const formatShowTime = (timeStr) => {
  if (!timeStr) return '未知时间';
  try {
    const date = new Date(timeStr);
    return `${date.getFullYear()}-${(date.getMonth()+1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
  } catch (err) {
    return '时间格式错误';
  }
};

const calculateTotalPrice = () => {
  const price = Number(showInfo.value.price) || 0;
  return (price * selectedSeats.value.length).toFixed(2); // 保留2位小数，符合金额格式
};

// 确认购票（使用 DTO 端点处理参数校验）
const confirmBooking = async () => {
  if (submitting.value) return;
  if (selectedSeats.value.length === 0) {
    ElMessage.warning('请先选择座位');
    return;
  }

  submitting.value = true;
  try {
    const res = await request.post(ORDER_API.CREATE, {
      recordId: Number(recordId),
      seat: selectedSeats.value.join(',')
    });
    if (res.code === '200' && res.data) {
      currentOrder.value = res.data;
      paymentDialogVisible.value = true;
    } else {
      ElMessage.error(res.msg || '下单失败');
    }
  } catch (error) {
    // request.js has already shown the backend message.
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.buy-page {
  width: 100%;
  padding: var(--space-20) 0;
  background-color: var(--el-fill-color-lighter);
}

.buy-panel {
  width: 70%;
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--space-32);
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
}

.buy-title {
  margin-bottom: var(--space-24);
  padding-bottom: var(--space-16);
  border-bottom: 1px solid var(--el-border-color-lighter);
  font-size: var(--fs-3xl);
  font-weight: var(--fw-bold);
  color: var(--el-text-color-primary);
  text-align: center;
}

.buy-layout {
  display: flex;
  gap: var(--space-24);
}

.buy-main {
  flex: 3;
}

/* ---------- 座位图例 ---------- */
.seat-legend {
  margin-bottom: var(--space-20);
  padding: var(--space-12);
  border-radius: var(--el-border-radius-base);
}

.seat-legend__row {
  display: flex;
  gap: var(--space-40);
}

.seat-legend__item {
  display: flex;
  align-items: center;
  gap: var(--space-8);
}

.seat-swatch {
  width: 22px;
  height: 22px;
  border-radius: var(--el-border-radius-base);
}

/* 图例色与座位实际用色同源，避免"图例说红、座位画粉" */
.seat-swatch--taken {
  background: var(--color-seat-taken);
}

.seat-swatch--available {
  background: var(--color-seat-available);
}

.seat-swatch--selected {
  background: var(--color-seat-selected);
}

.seat-swatch--mine {
  background: var(--color-seat-mine);
}

.seat-legend__label {
  font-size: var(--fs-base);
  color: var(--el-text-color-regular);
}

/* 大荧幕灰条：这里刻意用文字色令牌当填充。
 * EP 的 --el-fill-* 全是最浅档（#f0f2f5~#e6e8eb），换上会让荧幕条几乎不可见；
 * 本项目也没有"中间灰填充"令牌。这是唯一一处这样的用法。 */
.seat-screen {
  width: 80%;
  height: 8px;
  margin: 0 auto var(--space-32);
  border-radius: var(--el-border-radius-base);
  background: var(--el-text-color-secondary);
}

/* ---------- 座位矩阵 ---------- */
.seat-map {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-12);
}

.seat-map__row {
  display: flex;
  gap: var(--space-12);
}

.seat-item {
  width: 22px;
  height: 22px;
}

.seat-item--clickable {
  cursor: pointer;
}

.seat-item--locked {
  cursor: not-allowed;
}

.seat-item--available {
  background-color: var(--color-seat-available);
}

.seat-item--taken {
  background-color: var(--color-seat-taken);
}

.seat-item--selected {
  background-color: var(--color-seat-selected);
}

.seat-item--mine {
  background-color: var(--color-seat-mine);
}

.seat-item--empty {
  background-color: var(--el-fill-color);
}

/* 座位悬停效果 */
.seat-item:hover {
  transform: scale(1.2);
}

/* ---------- 加载 / 错误占位 ---------- */
.seat-hint {
  padding: var(--space-64) 0;
  color: var(--el-text-color-regular);
  text-align: center;
}

.seat-hint--error {
  color: var(--el-color-danger);
}

.seat-hint__icon {
  font-size: var(--fs-xl);
  margin-bottom: var(--space-8);
}

.seat-hint__icon--spin {
  animation: rotating 2s linear infinite;
}

.seat-hint__text {
  margin-top: var(--space-8);
}

.seat-hint__action {
  margin-top: var(--space-16);
}

/* ---------- 本人未支付锁座 ---------- */
.pending-box {
  margin-top: var(--space-20);
  padding: var(--space-12);
  border: 1px solid var(--el-color-warning-light-8);
  border-radius: var(--el-border-radius-base);
  background: var(--el-color-warning-light-9);
}

.pending-box__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: var(--fs-base);
  color: var(--el-color-warning);
}

.pending-box__actions {
  white-space: nowrap;
}

/* ---------- 已选座位 ---------- */
.selected-seats {
  margin-top: var(--space-24);
  padding: var(--space-12);
  border-radius: var(--el-border-radius-base);
}

.selected-seats__row {
  display: flex;
}

.selected-seats__label {
  width: 50px;
  font-size: var(--fs-md);
}

.selected-seats__body {
  flex: 1;
}

.selected-seats__empty {
  font-size: var(--fs-base);
  color: var(--el-text-color-regular);
}

.selected-seats__list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-8);
}

.seat-chip {
  padding: var(--space-4) var(--space-8);
  border-radius: var(--el-border-radius-base);
  background: var(--el-color-primary-light-9);
}

.seat-chip__remove {
  margin-left: var(--space-4);
  color: var(--el-color-danger);
  cursor: pointer;
}

/* ---------- 提交 ---------- */
.submit-row {
  margin-top: var(--space-20);
  text-align: center;
}

/* 主按钮底承载白字，须达 AA（白字压 #BF352D 为 5.58:1） */
.submit-button {
  padding: var(--space-8) var(--space-32);
  border: none;
  border-radius: var(--el-border-radius-base);
  background: var(--el-color-primary);
  color: var(--color-on-accent);
  font-size: var(--fs-base);
  cursor: pointer;
}

.submit-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ---------- 右侧电影信息 ---------- */
.film-aside {
  flex: 1;
  padding: var(--space-16);
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-fill-color-lighter);
}

.film-aside__poster-wrap {
  margin-bottom: var(--space-12);
}

.film-aside__poster {
  width: 180px;
  height: 230px;
  object-fit: cover;
}

.divider {
  margin: var(--space-16) 0;
  border: none;
  border-top: 1px solid var(--el-border-color-lighter);
}

.info-block {
  margin-bottom: var(--space-16);
}

.info-block__title {
  margin-bottom: var(--space-8);
  font-weight: var(--fw-bold);
  color: var(--el-text-color-primary);
}

.info-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: var(--space-8);
  font-size: var(--fs-base);
}

.info-row__label {
  color: var(--el-text-color-regular);
}

.info-row__value {
  color: var(--el-text-color-primary);
}

.info-row__price {
  font-weight: var(--fw-bold);
  color: var(--el-color-primary);
}

.total-box {
  padding: var(--space-12);
  border-radius: var(--el-border-radius-base);
  background: var(--el-fill-color-light);
}

.total-box__row {
  display: flex;
  justify-content: space-between;
  margin-top: var(--space-12);
  padding-top: var(--space-12);
  border-top: 1px solid var(--el-border-color-lighter);
  font-size: var(--fs-md);
  font-weight: var(--fw-bold);
}

.total-box__label {
  color: var(--el-text-color-primary);
}

.total-box__value {
  color: var(--el-color-primary);
}

/* 加载动画（兼容Element Plus） */
@keyframes rotating {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>
