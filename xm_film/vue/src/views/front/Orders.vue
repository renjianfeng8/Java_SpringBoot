<template>
  <div class="page-wide">
    <div>
      <div class="card page-card">
        <!-- 搜索控件带 aria-label：placeholder 一输入就消失，不构成可访问名称（规范 §9.3） -->
        <el-input v-model="data.orders" placeholder="请输入订单号" aria-label="订单号" class="search-input" :prefix-icon="Search"/>
        <el-select v-model="data.status" placeholder="请选择订单状态" aria-label="订单状态" class="search-input">
          <el-option v-for="status in ORDER_STATUS_OPTIONS" :key="status" :label="status" :value="status" />
        </el-select>
        <el-button type="primary" @click="load">查 询</el-button>
        <el-button type="warning" @click="reset">重 置</el-button>
      </div>

      <div class="card page-card">
        <el-table stripe :data="data.tableData">
          <el-table-column type="expand" min-width="60">
            <template #default="props">
              <el-descriptions title="订单信息" :column="4" border>
                <el-descriptions-item label="电影图片">
                  <el-image class="cell-thumb"
                            :src="props.row.img"
                            :alt="`《${props.row.filmName || '影片'}》海报`"/>
                </el-descriptions-item>
                <el-descriptions-item label="订单号">{{props.row.orders}}</el-descriptions-item>
                <!-- 取票码：支付成功时生成，在取票大厅凭它核销出票（终态订单没有码） -->
                <el-descriptions-item label="取票码">
                  <span v-if="props.row.pickupCode" class="pickup-code">{{ props.row.pickupCode }}</span>
                  <span v-else>—</span>
                </el-descriptions-item>
                <el-descriptions-item label="用户名称">{{props.row.userName}}</el-descriptions-item>
                <el-descriptions-item label="电影名称">{{props.row.filmName}}</el-descriptions-item>
                <el-descriptions-item label="影院名称">{{props.row.cinemaName}}</el-descriptions-item>
                <el-descriptions-item label="影厅房间">{{ props.row.roomName }}</el-descriptions-item>
                <el-descriptions-item label="座位号">{{props.row.seat}}</el-descriptions-item>
                <el-descriptions-item label="预约时间">{{props.row.start}}</el-descriptions-item>
                <el-descriptions-item label="电影票数量">{{props.row.number}}</el-descriptions-item>
                <el-descriptions-item label="单价（元）">
                  {{ props.row.unitPrice != null ? `¥${props.row.unitPrice}` : '—' }}
                </el-descriptions-item>
                <el-descriptions-item label="总费用">{{props.row.total}}</el-descriptions-item>
                <el-descriptions-item label="订单状态">
                  <el-tag :type="getStatusType(props.row.status)">
                    {{ props.row.status }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="支付时间">{{ props.row.payTime || '—' }}</el-descriptions-item>
                <el-descriptions-item label="实付金额">
                  {{ props.row.payAmount != null ? `¥${props.row.payAmount}` : '—' }}
                </el-descriptions-item>
                <el-descriptions-item label="退票时间">{{ props.row.refundTime || '—' }}</el-descriptions-item>
                <el-descriptions-item label="退款金额">
                  {{ props.row.refundAmount != null ? `¥${props.row.refundAmount}` : '—' }}
                </el-descriptions-item>
              </el-descriptions>
            </template>
          </el-table-column>
          <el-table-column label="订单号" prop="orders" show-overflow-tooltip />
          <el-table-column label="用户名称" prop="userName"/>
          <el-table-column label="电影名称" prop="filmName" show-overflow-tooltip />
          <el-table-column label="电影图片" prop="img">
            <template #default="scope">
              <el-image class="cell-thumb cell-thumb--spaced"
                        v-if="scope.row.img"
                        :src="scope.row.img"
                        :preview-src-list="[scope.row.img]"
                        preview-teleported/>
            </template>
          </el-table-column>
          <el-table-column label="影院名称" prop="cinemaName"/>
          <el-table-column label="影厅名称">
            <template #default="prop">
              {{ prop.row.roomName }}
            </template>
          </el-table-column>
          <el-table-column label="预约时间" prop="start" show-overflow-tooltip />
          <el-table-column label="电影票数量" prop="number"/>
          <el-table-column label="单价" prop="unitPrice" min-width="70"/>
          <el-table-column label="总费用" prop="total"/>
          <el-table-column label="订单状态" prop="status">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <!-- 操作列必须定宽：el-table 把未指定宽度的列一律按 80px 起算，13 列挤在 1200px
               容器里操作列只剩 ~89px，装不下「继续支付 + 取消」这类两个文字按钮（需 ~100px），
               折行后第二个按钮又被 EP 的 12px 兄弟边距推右错开（BUG-049）。
               140px 里的 30px 由展开列与单价列让出（它们的内容本来不需要 80px），
               表格最小总宽因此只从 1040px 涨到 1070px，横向滚动阈值几乎不动。 -->
          <el-table-column label="操作" width="140">
            <template #default="scope">
              <div class="row-actions">
                <el-button v-if="scope.row.status === '待支付'" link type="danger"
                           @click="() => continuePay(scope.row)">继续支付</el-button>
                <el-button v-if="scope.row.status === '待支付'" link type="warning" @click="() => cancelOrder(scope.row.id)">取消</el-button>
                <el-button v-if="scope.row.status === '待取票'" link type="primary"
                           @click="goPickupHall">取票</el-button>
                <el-button v-if="scope.row.status === '待取票'" link type="warning"
                           @click="() => refundOrder(scope.row)">退票</el-button>
                <el-button v-if="scope.row.status === '已取票'" link type="primary"
                           @click="goReview(scope.row)">
                  {{ markOf(scope.row.filmId) ? '修改评价' : '去评价' }}
                </el-button>
                <!-- 只有终态废单可删除；已成交订单必须走退票，与后端删除守卫同构。
                     纯图标按钮必须带 aria-label，否则读屏软件只念出一个"按钮"（规范 §10.2） -->
                <el-button v-if="isOrderDeletable(scope.row.status)" class="row-action" link :icon="Delete"
                           aria-label="删除该订单"
                           @click="() => del(scope.row.id)" type="danger"></el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="card page-card">
        <el-pagination
            @size-change="load"
            @current-change="load"
            v-model:current-page="data.pageNumber"
            v-model:page-size="data.pageSize"
            :page-sizes="[5, 10, 15, 20]"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="data.total"
        />
      </div>
    </div>
  </div>

  <OrderPayDialog v-model="payDialogVisible" :order="payingOrder"
                  @paid="load" @cancelled="load" @timeout="load" />
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { Delete, Search } from "@element-plus/icons-vue";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox } from "element-plus";
import { API_PATHS, ORDER_API, ORDER_STATUS_OPTIONS, getOrderStatusType as getStatusType, apiById, apiPage, isOrderDeletable } from '@/constants';
import OrderPayDialog from '@/components/OrderPayDialog.vue';
import { useAuth } from '@/composables/useAuth';

interface MarkRow {
  id?: number;
  filmId?: number;
  score?: number;
  mark?: string;
}

interface Ordered {
  id?: number;
  orders?: string;
  userId?: number;
  filmId?: number;
  img?: string;
  cinemaId?: number;
  roomId?: number;
  appointment?: string;
  total?: string;
  unitPrice?: number;
  number?: number;
  status?: string;
  start?: string;
  seat?: string;
  // 资金凭证（后端写入，只读展示）
  payTime?: string;
  payAmount?: number;
  refundTime?: string;
  refundAmount?: number;
  // 取票码（支付成功时由后端生成，一单一码；在取票大厅凭它核销出票）
  pickupCode?: string;
  // 新增后端返回的关联字段
  userName?: string;
  filmName?: string;
  cinemaName?: string;
  roomName?: string;
}

const data = reactive({
  tableData: [] as Ordered[],
  pageNumber: 1,
  pageSize: 10,
  total: 0,
  orders: null,
  status: undefined
});

const router = useRouter();

// 待取票订单的取票入口：取票码在展开行的「取票码」一栏，到大厅凭码核销出票
const goPickupHall = () => {
  router.push('/front/pickup');
};

const load = () => {
  request.get(apiPage(API_PATHS.ORDERS), {
    params: {
      pageNum: data.pageNumber,
      pageSize: data.pageSize,
      orders: data.orders,
      status: data.status
    }
  }).then(res => {
    if (res && res.data) {
      data.tableData = res.data.list || [];
      data.total = res.data.total || 0;
    }
  }).catch(error => {
    console.error('加载数据失败:', error);
    ElMessage.error('加载数据失败，请重试');
  });
}

const del = (id: number) => {
  ElMessageBox.confirm('删除数据后无法恢复,您确认删除吗?', '删除确认', { type: 'warning' }).then(() => {
    request.delete(apiById(API_PATHS.ORDERS, id)).then(res => {
      if (res.code === '200') {
        ElMessage.success('操作成功')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch()
}

const cancelOrder = async (id: number) => {
  try {
    const res = await request.put(ORDER_API.CANCEL(id))
    if (res.code === '200') {
      ElMessage.success('订单已取消')
      await load()
    } else {
      ElMessage.error(res.msg || '取消失败')
    }
  } catch (error) {
    // request.js has already shown the backend message.
  }
}

const reset = () => {
  data.orders = null;
  data.status = undefined;
  load();
}

// 继续支付：复用选座页同一套支付弹窗（含 5 分钟倒计时与超时自动取消）
const payDialogVisible = ref(false);
const payingOrder = ref(null);

const continuePay = (order: Ordered) => {
  payingOrder.value = order;
  payDialogVisible.value = true;
}

// 退票：规则与后端一致 —— 仅"待取票"且距放映 60 分钟以上
const refundOrder = (order: Ordered) => {
  ElMessageBox.confirm(
      `退票后座位释放、款项退回。确认退掉订单 ${order.orders} 吗？`,
      '退票确认', { type: 'warning' }
  ).then(async () => {
    try {
      const res = await request.put(ORDER_API.REFUND(order.id))
      if (res.code === '200') {
        ElMessage.success('退票成功')
        await load()
      } else {
        ElMessage.error(res.msg || '退票失败')
      }
    } catch (error) {
      // request.js has already shown the backend message.
    }
  }).catch(() => {})
}

// 评价：闭环的最后一环 —— 已取票订单可对影片评分/评语，评分会回写影片均分与评分榜
const { user } = useAuth();
const myMarks = ref<MarkRow[]>([]);

const markOf = (filmId?: number): MarkRow | undefined =>
    myMarks.value.find(item => item.filmId === filmId);

const loadMyMarks = async () => {
  const userId = user.value?.id;
  if (!userId) return;
  try {
    const res = await request.get(API_PATHS.MARKS, { params: { userId } });
    if (res.code === '200') {
      myMarks.value = res.data || [];
    }
  } catch (error) {
    // request.js has already shown the backend message.
  }
}

// 评价表单已搬到影评页（/front/filmMarks/:id）—— 发表与点赞要在同一条赞序列表上，
// 弹窗里看不到别人的评价，把「修改」做成跳转后闭环才完整
const goReview = (order: Ordered) => {
  if (!order.filmId) {
    ElMessage.warning('该订单缺少影片信息，无法评价');
    return;
  }
  // 路由没有 name（meta.name 是标题文案），导航一律走 path
  router.push(`/front/filmMarks/${order.filmId}`);
}

// 初始加载
load()
loadMyMarks()
</script>

<style scoped>
/* 取票码要能一眼抄准：加大字号与字距，用承载文字的主色（前台 #BF352D，5.58:1） */
.pickup-code {
  font-size: var(--fs-md);
  font-weight: var(--fw-bold);
  letter-spacing: 1px;
  color: var(--el-color-primary);
}
</style>
