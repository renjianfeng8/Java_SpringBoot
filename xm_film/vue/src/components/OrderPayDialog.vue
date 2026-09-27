<!--
  订单支付弹窗（模拟支付）。

  选座页（确认购票后立即支付）与订单列表页（待支付订单"继续支付"）共用同一套
  倒计时与支付/取消逻辑，避免两处各写一遍。

  父组件用法：
    <OrderPayDialog v-model="visible" :order="currentOrder"
                    @paid="..." @cancelled="..." @timeout="..." />
-->
<template>
  <div v-if="modelValue"
       style="position: fixed; top: 0; left: 0; width: 100%; height: 100%;
              background: rgba(0,0,0,0.5); display: flex; align-items: center;
              justify-content: center; z-index: 1000;">
    <div style="background: white; border-radius: 8px; padding: 30px; width: 400px;
                box-shadow: 0 4px 12px rgba(0,0,0,0.15);">
      <div style="font-size: 20px; font-weight: bold; text-align: center; margin-bottom: 20px;">
        确认支付
      </div>
      <div style="margin-bottom: 15px;">
        <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
          <span style="color: #666;">订单编号：</span>
          <span>{{ order?.orders }}</span>
        </div>
        <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
          <span style="color: #666;">座位：</span>
          <span>{{ order?.seat }}</span>
        </div>
        <div style="display: flex; justify-content: space-between; margin-bottom: 8px;
                    font-size: 18px; font-weight: bold;">
          <span style="color: #333;">总价：</span>
          <span style="color: #ef4238;">¥{{ order?.total }}</span>
        </div>
        <div v-if="countdown > 0"
             style="text-align: center; margin: 15px 0; font-size: 14px; color: #999;">
          剩余支付时间：
          <span :style="{ color: countdown <= 30 ? '#ef4238' : '#333',
                          fontWeight: 'bold', fontSize: '18px' }">
            {{ formatCountdown(countdown) }}
          </span>
        </div>
        <div v-else style="text-align: center; margin: 15px 0; color: #ef4238; font-weight: bold;">
          支付已超时
        </div>
      </div>
      <div style="display: flex; gap: 15px; justify-content: center;">
        <button @click="cancelOrder"
                :disabled="submitting"
                style="padding: 8px 25px; border: 1px solid #ddd; border-radius: 4px;
                       background: white; cursor: pointer; font-size: 14px;">
          取消订单
        </button>
        <button @click="submitPayment"
                :disabled="countdown <= 0 || submitting"
                :style="{
                  padding: '8px 25px', border: 'none', borderRadius: '4px',
                  background: countdown > 0 ? '#ef4238' : '#ccc',
                  color: 'white', cursor: countdown > 0 ? 'pointer' : 'not-allowed',
                  fontSize: '14px'
                }">
          模拟支付
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onUnmounted, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import request from '@/utils/request.js';
import { ORDER_API } from '@/constants';

const DEFAULT_PAY_SECONDS = 300;

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  order: { type: Object, default: null },
});

const emit = defineEmits(['update:modelValue', 'paid', 'cancelled', 'timeout']);

const countdown = ref(0);
const submitting = ref(false);
let countdownTimer = null;

const stopCountdown = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer);
    countdownTimer = null;
  }
};

const close = () => emit('update:modelValue', false);

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
      ElMessage.warning('支付超时，订单已自动取消');
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
        startCountdown();
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
