<!--
  取票大厅 —— 影院自助取票机的页面形态。

  为什么这个页面不需要登录：自助机不认识用户，它只认取票码。核销接口
  POST /api/v1/tickets/redeem 是全站唯一的匿名写入口，安全性由"码本身即凭证"保证
  （码随机不可猜、一次性、有效期到放映结束、不泄露订单明细），
  逐条论证见后端 TicketController 的类注释。

  "扫码"这一步被简化成手工输入取票码 —— 浏览器里没有摄像头扫码链路，
  而码的字符集刻意剔除了 I/L/O/0/1，就是为了这一步好抄。
-->
<template>
  <div class="page-narrow pickup-page">
    <el-card shadow="never">
      <template #header>
        <span class="pickup-title">取票大厅</span>
      </template>

      <p class="pickup-hint">
        输入订单的取票码自助出票。取票码在「购票记录」的待取票订单里，支付成功后也会直接展示。
      </p>

      <!-- 表单形态：可见 label + status-icon + 回车提交 + @submit.prevent 兜底 -->
      <el-form ref="formRef"
               :model="form"
               :rules="rules"
               label-position="top"
               status-icon
               class="pickup-form"
               @submit.prevent>
        <el-form-item label="取票码" prop="code">
          <!-- 图标必须用组件绑定：main.js 未全局注册图标集，字符串写法不会解析 -->
          <el-input v-model="form.code"
                    :prefix-icon="Ticket"
                    size="large"
                    placeholder="例如 8F3A-2C71"
                    @keyup.enter="submit" />
        </el-form-item>
        <!-- native-type="button"：避免 el-button 在 el-form 内触发原生提交，
             与 @keyup.enter 叠加成两次请求（明确禁止一次回车发两次） -->
        <el-button type="primary"
                   size="large"
                   native-type="button"
                   :loading="submitting"
                   class="pickup-form__submit"
                   @click="submit">
          取 票
        </el-button>
      </el-form>

      <el-alert v-if="errorText"
                :title="errorText"
                type="error"
                :closable="false"
                show-icon
                class="pickup-error" />

      <div v-if="voucher" class="ticket">
        <div class="ticket__head">出票成功</div>
        <div class="ticket__row">
          <span class="ticket__label">影片</span>
          <span class="ticket__value">{{ voucher.filmTitle }}</span>
        </div>
        <div class="ticket__row">
          <span class="ticket__label">场次</span>
          <span class="ticket__value">{{ voucher.start }}</span>
        </div>
        <div class="ticket__row">
          <span class="ticket__label">影院</span>
          <span class="ticket__value">{{ voucher.cinemaName }} {{ voucher.roomName }}</span>
        </div>
        <div class="ticket__row">
          <span class="ticket__label">座位</span>
          <span class="ticket__value">{{ voucher.seat }}</span>
        </div>
        <div class="ticket__row">
          <span class="ticket__label">张数</span>
          <span class="ticket__value">{{ voucher.number }} 张</span>
        </div>
        <div class="ticket__foot">该取票码已作废，不能重复使用。请凭票入场。</div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { Ticket } from '@element-plus/icons-vue';
import request from '@/utils/request.js';
import { TICKET_API } from '@/constants';

const formRef = ref(null);
const form = reactive({ code: '' });
const rules = {
  code: [{ required: true, message: '请输入取票码', trigger: 'blur' }]
};

const submitting = ref(false);
const errorText = ref('');
const voucher = ref(null);

const submit = async () => {
  if (submitting.value) return;
  try {
    await formRef.value?.validate();
  } catch (invalid) {
    return;
  }

  submitting.value = true;
  errorText.value = '';
  voucher.value = null;
  try {
    const res = await request.post(TICKET_API.REDEEM, { code: form.code.trim() });
    if (res.code === '200') {
      voucher.value = res.data;
      form.code = '';
      formRef.value?.clearValidate();
    } else {
      // 业务拒绝（码无效 / 已取出 / 已退票 / 未支付 / 场次已结束）由后端给出具体原因，
      // 这里原样呈现 —— 用一句"取票失败"糊过去，用户不知道下一步该做什么
      errorText.value = res.msg || '取票失败，请核对取票码';
    }
  } catch (error) {
    // 网络异常 / 超时 / 5xx 的提示已由 request.js 的响应拦截器统一给出，
    // 页面内只落错误态，不再重复弹一次
    console.error('取票接口请求异常：', error);
    errorText.value = '数据加载失败，请稍后重试';
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.pickup-page {
  padding-bottom: var(--space-40);
}

.pickup-title {
  font-size: var(--fs-xl);
  font-weight: var(--fw-bold);
}

.pickup-hint {
  margin-bottom: var(--space-20);
  font-size: var(--fs-base);
  color: var(--el-text-color-regular);
}

.pickup-form__submit {
  width: 100%;
}

.pickup-error {
  margin-top: var(--space-20);
}

/* 出票凭条 */
.ticket {
  margin-top: var(--space-20);
  padding: var(--space-20);
  border: 1px solid var(--el-color-primary);
  border-radius: var(--el-border-radius-base);
  background: var(--el-color-primary-light-9);
}

.ticket__head {
  margin-bottom: var(--space-16);
  font-size: var(--fs-lg);
  font-weight: var(--fw-bold);
  color: var(--el-color-primary);
  text-align: center;
}

.ticket__row {
  display: flex;
  margin-bottom: var(--space-8);
  font-size: var(--fs-base);
}

.ticket__label {
  flex-shrink: 0;
  width: 60px;
  color: var(--el-text-color-regular);
}

.ticket__value {
  color: var(--el-text-color-primary);
}

.ticket__foot {
  margin-top: var(--space-16);
  font-size: var(--fs-xs);
  color: var(--el-text-color-regular);
  text-align: center;
}
</style>
