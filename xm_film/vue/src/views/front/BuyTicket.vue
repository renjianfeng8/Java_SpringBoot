<template>
  <!-- 整个页面统一外层盒子 -->
  <div style="width: 100%; padding: 20px 0; background-color: #f9f9f9;">
    <div style="width: 70%; margin: 0 auto; background-color: white; padding: 30px; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.05);">
      <!-- 标题：居中加粗放大 -->
      <div style="text-align: center; font-size: 26px; font-weight: bold; margin-bottom: 25px; color: #333; border-bottom: 1px solid #eee; padding-bottom: 15px;">
        座位选择
      </div>

      <!-- 主体布局：左右分栏 -->
      <div style="display: flex; gap: 25px;">
        <!-- 左侧：座位选择区 -->
        <div style="flex: 3;">

          <!-- 座位状态图例（优化版） -->
          <div style="margin-bottom: 20px; border-radius: 4px; padding: 12px;">
            <!-- 图例内容（横向均匀分布） -->
            <div style="display: flex;  gap: 40px; ">
              <!-- 已售座位 -->
              <div style="display: flex; align-items: center; gap: 8px;">
                <div style="width: 22px; height: 22px; background: #fd4444;border-radius: 5px;"></div>
                <div style="font-size: 14px; color: #666;">已售座位</div>
              </div>
              <!-- 可选座位 -->
              <div style="display: flex; align-items: center; gap: 8px;">
                <div style="width: 22px; height: 22px; background: #4CAF50; border-radius: 5px;"></div>
                <div style="font-size: 14px; color: #666;">可选座位</div>
              </div>
              <!-- 已选座位 -->
              <div style="display: flex; align-items: center; gap: 8px;">
                <div style="width: 22px; height: 22px; background: #2196F3;  border-radius: 5px;"></div>
                <div style="font-size: 14px; color: #666;">已选座位</div>
              </div>
            </div>
          </div>

          <!-- 大荧幕（仅保留加粗灰色杠） -->
          <div style="width: 80%; height: 8px; background: #999; margin: 0 auto 35px; border-radius: 4px;"></div>

          <!-- 座位加载中/错误占位 -->
          <div v-if="loading" style="text-align: center; padding: 60px 0; color: #999;">
            <i class="el-icon-loading" style="font-size: 20px; animation: rotating 2s linear infinite;"></i>
            <div style="margin-top: 8px;">加载座位中...</div>
          </div>
          <div v-else-if="seatError" style="text-align: center; padding: 60px 0; color: #ef4238;">
            <i class="el-icon-error" style="font-size: 20px; margin-bottom: 8px;"></i>
            <div>{{ seatError }}</div>
          </div>

          <!-- 8x8座位矩阵 -->
          <div v-else style="display: flex; flex-direction: column; align-items: center; gap: 10px;  ">
            <div v-for="row in 8" :key="'row' + row" style="display: flex; gap: 10px;">
              <div v-for="col in 8" :key="`seat-${row}-${col}`"
                   :style="{
                     width: '22px',
                     height: '22px',
                     backgroundColor: getSeatColor(row, col),
                     cursor: isSeatAvailable(row, col) ? 'pointer' : 'not-allowed'}"
                   @click="selectSeat(row, col)"
                   class="seat-item"
              ></div>
            </div>
          </div>

          <!-- 已选座位 -->
          <div style="margin-top: 25px; padding: 10px; border-radius: 5px;">
            <div style="display: flex;">
              <div style="width: 50px;font-size: 15px;">座位:</div>
              <div style="flex: 1;">
                <div v-if="selectedSeats.length === 0" style="color: #999; font-size: 14px;">未选择座位</div>
                <div v-else style="display: flex; flex-wrap: wrap; gap: 8px;">
                   <span v-for="seat in selectedSeats" :key="seat" style="background: #e3f2fd;padding: 3px 8px;border-radius: 5px;"> {{ seat }}
                     <span @click="removeSeat(seat)" style="margin-left: 5px;color: #f44336;">×</span>
                   </span>
                </div>
              </div>
            </div>
          </div>

          <!-- 确认购票按钮 -->
          <div style="margin-top: 20px; text-align: center;">
            <button style="background: #ef4238; color: #fff; border: none; padding: 8px 30px; border-radius: 4px; cursor: pointer; font-size: 14px;"
                    :disabled="selectedSeats.length === 0 || loading || !isLogin"
                    :style="{ opacity: (selectedSeats.length === 0 || loading || !isLogin) ? 0.6 : 1 }"
                    @click="confirmBooking"
            >
              确认购票（{{ selectedSeats.length }}张）
            </button>
          </div>
        </div>

        <!-- 右侧：电影信息区 -->
        <div style="flex: 1; padding: 15px; border-radius: 4px; background-color: #fafafa;">
          <!-- 电影海报 -->
          <div style="margin-bottom: 10px;">
            <img :src="filmInfo.img " alt="电影海报"
                 style="width: 180px; height: 230px;">
          </div>

          <hr style="border: none; border-top: 1px solid #eee; margin: 15px 0;">

          <!-- 场次信息 -->
          <div style="margin-bottom: 15px;">
            <div style="font-weight: bold; margin-bottom: 8px; color: #333;">场次信息</div>
            <div style="display: flex; justify-content: space-between; margin-bottom: 6px; font-size: 14px;">
              <span style="color: #666;">影院：</span>
              <span style="color: #333;">{{ cinemaInfo.name || '未知' }}</span>
            </div>
            <div style="display: flex; justify-content: space-between; margin-bottom: 6px; font-size: 14px;">
              <span style="color: #666;">时间：</span>
              <span style="color: #333;">{{ formatShowTime(showInfo.start) }}</span>
            </div>
            <div style="display: flex; justify-content: space-between; margin-bottom: 6px; font-size: 14px;">
              <span style="color: #666;">票价：</span>
              <span style="color: #ef4238; font-weight: bold;">¥{{ showInfo.price || 0 }}/张</span>
            </div>
          </div>

          <hr style="border: none; border-top: 1px solid #eee; margin: 15px 0;">

          <!-- 订单汇总 -->
          <div style="padding: 10px; background: #f5f5f5; border-radius: 4px;">
            <div style="font-weight: bold; margin-bottom: 8px; color: #333;">订单汇总</div>
            <div style="display: flex; justify-content: space-between; margin-bottom: 6px; font-size: 14px;">
              <span style="color: #666;">座位数：</span>
              <span style="color: #333;">{{ selectedSeats.length }} 张</span>
            </div>
            <div style="display: flex; justify-content: space-between; margin-top: 10px; font-size: 16px; font-weight: bold; padding-top: 10px; border-top: 1px solid #eee;">
              <span style="color: #333;">总价：</span>
              <span style="color: #ef4238;">¥{{ calculateTotalPrice() }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watchEffect } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from "@/utils/request.js";
import { API_PATHS, apiById } from '@/constants';
import { clearStoredUser, getStoredUser } from '@/utils/authStorage';

// 1. Read the current signed-in user from shared auth storage.
const userInfo = ref(null); // 存储登录用户完整信息
const isLogin = ref(false); // 是否登录标记
const userId = ref(0); // 用户ID（后端Ordered实体需要）

// 初始化并监听登录状态（页面刷新或登录状态变化时自动更新）
const initUserInfo = () => {
  try {
    const storedUser = getStoredUser();
    if (storedUser) {
      const parsedUser = storedUser;
      // 校验用户信息合法性（需包含id和role字段，与登录接口返回一致）
      if (parsedUser.id && parsedUser.role) {
        userInfo.value = parsedUser;
        userId.value = Number(parsedUser.id);
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
const seats = ref([]); // 座位矩阵：0=可选，1=已售，2=已选
const selectedSeats = ref([]); // 已选座位（格式：["1排1座", ...]）

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
    Promise.all([
      initSeats(), // 初始化座位状态
      fetchBaseInfo() // 加载电影/影院/场次信息
    ])
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

// 初始化座位状态
const initSeats = () => {
  return new Promise((resolve) => {
    // 1. 先查询该场次已售座位（通过订单接口反向获取）
    request.get(`${API_PATHS.ORDERS}/seats`, {
      params: {
        recordId: Number(recordId)
      }
    }).then(res => {
      if (res.code === '200') {
        const soldSeats = [];
        // 收集所有已售座位（去重处理，避免重复标记）
        const seatSet = new Set();
        res.data.forEach(order => {
          if (order.seat) {
            order.seat.split(',').forEach(seat => {
              seatSet.add(seat);
            });
          }
        });
        soldSeats.push(...seatSet);
        // 2. 初始化8x8座位矩阵
        const seatMatrix = Array(8).fill().map(() => Array(8).fill(0));
        // 标记已售座位（格式："1排1座" → 行=1，列=1）
        soldSeats.forEach(seat => {
          const rowMatch = seat.match(/(\d+)排/);
          const colMatch = seat.match(/排(\d+)座/);
          if (rowMatch && colMatch) {
            const row = parseInt(rowMatch[1]);
            const col = parseInt(colMatch[1]);
            if (row >= 1 && row <= 8 && col >= 1 && col <= 8) {
              seatMatrix[row - 1][col - 1] = 1; // 1=已售
            }
          }
        });
        seats.value = seatMatrix;
        resolve();
      } else {
        seatError.value = res.msg || '座位数据加载失败';
        seats.value = Array(8).fill().map(() => Array(8).fill(0));
        resolve();
      }
    }).catch(() => {
      seatError.value = '座位数据加载失败，请稍后重试';
      seats.value = Array(8).fill().map(() => Array(8).fill(0));
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
    // 场次信息（获取影厅ID等关键字段）
    request.get(apiById(API_PATHS.RECORDS, Number(recordId))).then(res => {
      if (res.code === '200' && res.data) {
        showInfo.value = {
          id: res.data.id,
          roomId: res.data.roomId || 0, // 确保为数字，避免后端报错
          roomName: res.data.roomName,
          start: res.data.start,
          price: res.data.price || 0
        };
      } else {
        throw new Error(`场次信息加载失败：${res.msg || '未知错误'}`);
      }
    })
  ]);
};

// 座位操作工具函数
const getSeatColor = (row, col) => {
  const r = row - 1;
  const c = col - 1;
  if (!seats.value[r] || seats.value[r][c] === undefined) return '#f5f5f5';
  switch (seats.value[r][c]) {
    case 0: return '#4CAF50'; // 可选（绿色）
    case 1: return '#f30656';    // 已售（灰色）
    case 2: return '#2196F3'; // 已选（蓝色）
    default: return '#f5f5f5';
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

// 生成唯一订单编号（格式：年月日+4位随机数，确保唯一性）
const generateOrderNo = () => {
  const date = new Date();
  const dateStr = date.toISOString().slice(0, 10).replace(/-/g, ''); // 20240520
  const randomStr = Math.floor(Math.random() * 10000).toString().padStart(4, '0'); // 0001-9999
  return `${dateStr}${randomStr}`;
};

// 确认购票（适配后端/add接口和Ordered实体）
const confirmBooking = () => {

  if (selectedSeats.value.length === 0) {
    ElMessage.warning('请先选择座位');
    return;
  }

  // 构造符合后端Ordered实体的订单数据
  const orderData = {
    recordId: Number(recordId),
    seat: selectedSeats.value.join(',')
  };

  // 调用后端/add接口提交订单
  request.post(API_PATHS.ORDERS, orderData)
      .then(res => {
        if (res.code === '200') {
          ElMessage.success('订单创建成功！即将跳转到订单详情');
          // 跳转订单详情页（携带订单编号）
          setTimeout(() => {
            router.push({
              path: '/front/orders',
              query: {
                orderNo: orderData.orders // 用于查询订单详情
              }
            });
          }, 1500);
        } else {
          ElMessage.error(`订单创建失败：${res.msg || '未知错误'}`);
        }
      })
      .catch(err => {
        ElMessage.error(`网络异常：${err.message || '提交订单失败'}`);
      });
};
</script>

<style scoped>
/* 座位悬停效果 */
.seat-item:hover {
  transform: scale(1.2);
}

/* 加载动画（兼容Element Plus） */
@keyframes rotating {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>
