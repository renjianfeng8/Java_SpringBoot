<!--
  订单支付弹窗（账户余额支付）。

  选座页（确认购票后立即支付）与订单列表页（待支付订单"继续支付"）共用同一套
  倒计时与支付/取消逻辑，避免两处各写一遍。

  余额不足时不关闭弹窗：提示差额并给出去充值入口，订单留在待支付、座位继续锁定，
  用户充值回来后仍可从这个弹窗完成支付。

  父组件用法：
    <OrderPayDialog v-model="visible" :order="currentOrder"
                    @paid="..." @cancelled="..." @timeout="..." />
-->
<template>
  <div v-if="modelValue" class="pay-mask">
    <div class="pay-dialog">
      <div class="pay-dialog__title">
        确认支付
      </div>
      <div class="pay-dialog__body">
        <div class="pay-row">
          <span class="pay-row__label">订单编号：</span>
          <span>{{ order?.orders }}</span>
        </div>
        <div class="pay-row">
          <span class="pay-row__label">座位：</span>
          <span>{{ order?.seat }}</span>
        </div>
        <div class="pay-row pay-row--amount">
          <span class="pay-row__label">应付金额：</span>
          <span class="pay-amount">¥{{ money(payable) }}</span>
        </div>
        <div class="pay-row pay-row--small">
          <span class="pay-row__label">账户余额：</span>
          <span :class="insufficient ? 'pay-balance--low' : 'pay-balance'" class="pay-balance--strong">
            {{ balance === null ? '加载中…' : `¥${money(balance)}` }}
          </span>
        </div>
        <div v-if="!insufficient && balance !== null" class="pay-row pay-row--small">
          <span class="pay-row__label">支付后余额：</span>
          <span class="pay-value">¥{{ money(afterPay) }}</span>
        </div>

        <div v-if="insufficient" class="pay-warning">
          余额不足，还差 ¥{{ money(payable - balance) }}，请先充值后再支付。
        </div>

        <div v-if="countdown > 0" class="pay-countdown">
          剩余支付时间：
          <span :class="countdown <= 30 ? 'countdown--urgent' : 'countdown--normal'">
            {{ formatCountdown(countdown) }}
          </span>
        </div>
        <div v-else class="pay-timeout">
          支付已超时
        </div>
      </div>
      <div class="pay-dialog__actions">
        <button @click="cancelOrder"
                :disabled="submitting"
                class="pay-button pay-button--cancel">
          取消订单
        </button>
        <button v-if="insufficient"
                @click="goRecharge"
                :disabled="submitting"
                class="pay-button pay-button--primary">
          去充值
        </button>
        <button v-else @click="submitPayment"
                :disabled="countdown <= 0 || submitting || balance === null"
                :class="(countdown > 0 && balance !== null) ? 'pay-button--primary' : 'pay-button--disabled'"
                class="pay-button">
          余额支付
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onUnmounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import request from '@/utils/request.js';
import { ACCOUNT_API, ORDER_API } from '@/constants';

const DEFAULT_PAY_SECONDS = 300;

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  order: { type: Object, default: null },
});

const emit = defineEmits(['update:modelValue', 'paid', 'cancelled', 'timeout']);

const router = useRouter();

const countdown = ref(0);
const submitting = ref(false);
const balance = ref(null);
let countdownTimer = null;

const money = (value) => Number(value ?? 0).toFixed(2);
const payable = computed(() => Number(props.order?.total ?? 0));
const insufficient = computed(() => balance.value !== null && balance.value < payable.value);
const afterPay = computed(() => (balance.value === null ? 0 : balance.value - payable.value));

const stopCountdown = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer);
    countdownTimer = null;
  }
};

const close = () => emit('update:modelValue', false);

/** 余额取自后端，不在前端缓存，保证下单/退款后看到的都是当前值 */
const loadBalance = async () => {
  try {
    const res = await request.get(ACCOUNT_API.SUMMARY);
    balance.value = res.code === '200' ? Number(res.data?.balance ?? 0) : null;
  } catch (error) {
    balance.value = null;
  }
};

const goRecharge = () => {
  close();
  router.push('/front/account');
};

const resolveRemainingSeconds = () => {
  const timeoutStr = props.order?.pendingTimeoutAt;
  if (!timeoutStr) return DEFAULT_PAY_SECONDS;
  const timeoutDate = new Date(String(timeoutStr).replace(' ', 'T'));
  if (Number.isNaN(timeoutDate.getTime())) return DEFAULT_PAY_SECONDS;
  return Math.max(0, Math.floor((timeoutDate - Date.now()) / 1000));
};

const startCountdown = () => {
  stopCountdown();
  countdown.value = resolveRemainingSeconds();
  countdownTimer = setInterval(() => {
    countdown.value -= 1;
    if (countdown.value <= 0) {
      stopCountdown();
      // 前端倒计时归零只是把弹窗收掉；真正的取消由后端定时任务完成，
      // 所以这里不能说"已取消"，只能提示即将取消。
      ElMessage.warning('支付时间已到，未支付的订单将自动取消');
      close();
      emit('timeout');
    }
  }, 1000);
};

watch(
    () => [props.modelValue, props.order?.id],
    ([visible]) => {
      if (visible && props.order?.id) {
        submitting.value = false;
        balance.value = null;
        startCountdown();
        loadBalance();
      } else {
        stopCountdown();
      }
    },
    { immediate: true }
);

onUnmounted(stopCountdown);

const formatCountdown = (seconds) => {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${s.toString().padStart(2, '0')}`;
};

const submitPayment = async () => {
  if (!props.order?.id || submitting.value) return;
  submitting.value = true;
  try {
    const res = await request.put(ORDER_API.PAY(props.order.id));
    if (res.code === '200') {
      stopCountdown();
      ElMessage.success('支付成功');
      close();
      emit('paid');
    } else {
      ElMessage.error(res.msg || '支付失败');
      if (res.msg && res.msg.includes('超时')) {
        stopCountdown();
        close();
        emit('timeout');
      } else {
        // 失败可能是余额变动导致，重新拉取让按钮与提示同步
        await loadBalance();
      }
    }
  } finally {
    submitting.value = false;
  }
};

const cancelOrder = async () => {
  if (!props.order?.id || submitting.value) return;
  submitting.value = true;
  try {
    const res = await request.put(ORDER_API.CANCEL(props.order.id));
    if (res.code === '200') {
      stopCountdown();
      ElMessage.success('订单已取消');
      close();
      emit('cancelled');
    } else {
      ElMessage.error(res.msg || '取消失败');
    }
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.pay-mask {
  position: fixed;
  top: 0;
  left: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: var(--overlay-mask);
  z-index: var(--el-index-popper);
}

.pay-dialog {
  width: 400px;
  padding: var(--space-32);
  border-radius: var(--el-border-radius-base);
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-light);
}

.pay-dialog__title {
  margin-bottom: var(--space-20);
  font-size: var(--fs-xl);
  font-weight: var(--fw-bold);
  text-align: center;
}

.pay-dialog__body {
  margin-bottom: var(--space-16);
}

.pay-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: var(--space-8);
}

.pay-row--amount {
  font-size: var(--fs-lg);
  font-weight: var(--fw-bold);
}

.pay-row--small {
  font-size: var(--fs-base);
}

.pay-row__label {
  color: var(--el-text-color-regular);
}

/* 金额与余额承载文字，用达 AA 的令牌 */
.pay-amount {
  color: var(--el-color-primary);
}

.pay-balance,
.pay-value {
  color: var(--el-text-color-primary);
}

.pay-balance--strong {
  font-weight: var(--fw-bold);
}

.pay-balance--low {
  color: var(--el-color-danger);
}

.pay-warning {
  margin: var(--space-12) 0;
  padding: var(--space-8) var(--space-12);
  border-radius: var(--el-border-radius-base);
  background: var(--el-color-danger-light-9);
  color: var(--el-color-danger);
  font-size: var(--fs-sm);
}

.pay-countdown {
  margin: var(--space-16) 0;
  font-size: var(--fs-base);
  color: var(--el-text-color-secondary);
  text-align: center;
}

.countdown--normal {
  font-size: var(--fs-lg);
  font-weight: var(--fw-bold);
  color: var(--el-text-color-primary);
}

.countdown--urgent {
  font-size: var(--fs-lg);
  font-weight: var(--fw-bold);
  color: var(--el-color-danger);
}

.pay-timeout {
  margin: var(--space-16) 0;
  color: var(--el-color-danger);
  font-weight: var(--fw-bold);
  text-align: center;
}

.pay-dialog__actions {
  display: flex;
  justify-content: center;
  gap: var(--space-16);
}

.pay-button {
  padding: var(--space-8) var(--space-24);
  border-radius: var(--el-border-radius-base);
  font-size: var(--fs-base);
}

.pay-button--cancel {
  border: 1px solid var(--el-border-color);
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
  cursor: pointer;
}

/* 主按钮底承载白字，须达 AA（白字压 #BF352D 为 5.58:1） */
.pay-button--primary {
  border: none;
  background: var(--el-color-primary);
  color: var(--color-on-accent);
  cursor: pointer;
}

.pay-button--disabled {
  border: none;
  background: var(--el-text-color-disabled);
  color: var(--color-on-accent);
  cursor: not-allowed;
}
</style>
