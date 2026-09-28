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
          <span style="color: #333;">应付金额：</span>
          <span style="color: #ef4238;">¥{{ money(payable) }}</span>
        </div>
        <div style="display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 14px;">
          <span style="color: #666;">账户余额：</span>
          <span :style="{ color: insufficient ? '#ef4238' : '#333', fontWeight: 'bold' }">
            {{ balance === null ? '加载中…' : `¥${money(balance)}` }}
          </span>
        </div>
        <div v-if="!insufficient && balance !== null"
             style="display: flex; justify-content: space-between; font-size: 14px;">
          <span style="color: #666;">支付后余额：</span>
          <span style="color: #333;">¥{{ money(afterPay) }}</span>
        </div>

        <div v-if="insufficient"
             style="margin: 12px 0; padding: 8px 10px; background: #fef0f0;
                    color: #f56c6c; border-radius: 4px; font-size: 13px;">
          余额不足，还差 ¥{{ money(payable - balance) }}，请先充值后再支付。
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
        <button v-if="insufficient"
                @click="goRecharge"
                :disabled="submitting"
                style="padding: 8px 25px; border: none; border-radius: 4px;
                       background: #ef4238; color: white; cursor: pointer; font-size: 14px;">
          去充值
        </button>
        <button v-else @click="submitPayment"
                :disabled="countdown <= 0 || submitting || balance === null"
                :style="{
                  padding: '8px 25px', border: 'none', borderRadius: '4px',
                  background: (countdown > 0 && balance !== null) ? '#ef4238' : '#ccc',
                  color: 'white',
                  cursor: (countdown > 0 && balance !== null) ? 'pointer' : 'not-allowed',
                  fontSize: '14px'
                }">
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
