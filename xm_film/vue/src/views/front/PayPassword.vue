<!--
  支付密码的设置 / 修改页。

  两种模式：
    修改（已设置）—— 验原支付密码，走 PUT /account/pay-password
    设置（未设置）—— 验登录密码，走 PUT /account/pay-password/reset
  未设置过的用户直接进「设置」；已设置的用户可点「忘记支付密码」切到同一条重设路径
  （验登录密码），这是忘记支付密码时唯一的自救出口，后端也只为它保留了那条通路。

  修改入口只有这一页 —— 支付弹窗发现未设置时也只是把人送到这里来（见 OrderPayDialog）。
-->
<template>
  <div class="pay-pwd-wrapper">
    <div class="card pay-pwd-card">
      <h1 class="pay-pwd-title">{{ isChange ? '修改支付密码' : '设置支付密码' }}</h1>
      <p class="pay-pwd-intro">
        支付密码是 6 位数字，用于余额支付时确认这笔交易，与登录密码相互独立。
      </p>
      <p class="pay-pwd-state">
        当前状态：{{ hasPayPassword ? '已设置' : '未设置' }}
      </p>

      <el-form ref="formRef" :rules="rules" :model="form" class="pay-pwd-form" label-width="120px"
               status-icon @submit.prevent>
        <el-form-item v-if="isChange" label="原支付密码" prop="oldPassword">
          <PayPasswordInput v-model="form.oldPassword" :disabled="submitting" />
        </el-form-item>

        <el-form-item v-else label="登录密码" prop="loginPassword">
          <el-input show-password v-model="form.loginPassword" autocomplete="off"
                    placeholder="请输入登录密码" @keyup.enter="submit" />
        </el-form-item>

        <el-form-item label="新支付密码" prop="payPassword">
          <PayPasswordInput v-model="form.payPassword" :disabled="submitting" />
        </el-form-item>

        <el-form-item label="确认新支付密码" prop="confirmPassword">
          <PayPasswordInput v-model="form.confirmPassword" :disabled="submitting" />
        </el-form-item>

        <div class="pay-pwd-actions">
          <el-button :loading="submitting" type="primary" class="pay-pwd-submit" @click="submit">
            {{ isChange ? '立即修改' : '确认设置' }}
          </el-button>
        </div>
      </el-form>

      <div class="pay-pwd-links">
        <template v-if="isChange">
          <el-link type="primary" :underline="false" @click="switchMode(false)">忘记支付密码？</el-link>
        </template>
        <template v-else-if="hasPayPassword">
          <el-link type="primary" :underline="false" @click="switchMode(true)">返回修改支付密码</el-link>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import request from '@/utils/request.js';
import { ACCOUNT_API } from '@/constants';
import PayPasswordInput from '@/components/PayPasswordInput.vue';

const formRef = ref();
const submitting = ref(false);
const hasPayPassword = ref(false);
/** 已设置 = 修改模式（验原支付密码）；未设置 = 设置模式（验登录密码） */
const mode = ref('change');

const isChange = computed(() => mode.value === 'change');

const form = reactive({
  oldPassword: '',
  loginPassword: '',
  payPassword: '',
  confirmPassword: '',
});

const SIX_DIGITS = /^\d{6}$/;

const rules = {
  oldPassword: [
    { required: true, message: '请输入原支付密码', trigger: 'change' },
    { pattern: SIX_DIGITS, message: '支付密码为 6 位数字', trigger: 'change' },
  ],
  loginPassword: [
    { required: true, message: '请输入登录密码', trigger: 'blur' },
  ],
  payPassword: [
    { required: true, message: '请输入新支付密码', trigger: 'change' },
    { pattern: SIX_DIGITS, message: '支付密码为 6 位数字', trigger: 'change' },
  ],
  confirmPassword: [
    {
      validator: (rule, value, callback) => {
        if (!value) callback(new Error('请再次输入新支付密码'));
        else if (value !== form.payPassword) callback(new Error('两次输入的支付密码不一致'));
        else callback();
      },
      trigger: 'change',
    },
  ],
};

const resetFields = () => {
  form.oldPassword = '';
  form.loginPassword = '';
  form.payPassword = '';
  form.confirmPassword = '';
  formRef.value?.clearValidate();
};

const switchMode = (change) => {
  mode.value = change ? 'change' : 'reset';
  resetFields();
};

const loadState = async () => {
  try {
    const res = await request.get(ACCOUNT_API.SUMMARY);
    if (res.code === '200') {
      hasPayPassword.value = Boolean(res.data?.hasPayPassword);
      mode.value = hasPayPassword.value ? 'change' : 'reset';
    }
  } catch (error) {
    // 网络类提示由 request.js 响应拦截器统一给出，这里保持默认的「设置」模式即可
    console.error('支付密码状态查询异常：', error);
  }
};

const submit = () => {
  formRef.value.validate(async (valid) => {
    if (!valid || submitting.value) return;
    submitting.value = true;
    try {
      const res = isChange.value
          ? await request.put(ACCOUNT_API.PAY_PASSWORD, {
            oldPassword: form.oldPassword,
            payPassword: form.payPassword,
          })
          : await request.put(ACCOUNT_API.PAY_PASSWORD_RESET, {
            loginPassword: form.loginPassword,
            payPassword: form.payPassword,
          });

      if (res.code === '200') {
        ElMessage.success(isChange.value ? '支付密码已修改' : '支付密码已设置');
        hasPayPassword.value = true;
        mode.value = 'change';
        resetFields();
      } else {
        // 密码错误、锁定这类失败要保留「已设置」的既有状态，只提示不动模式
        ElMessage.error(res.msg || '支付密码设置失败');
        form.oldPassword = '';
        form.payPassword = '';
        form.confirmPassword = '';
      }
    } finally {
      submitting.value = false;
    }
  });
};

onMounted(loadState);
</script>

<style scoped>
/* 与 front/Password.vue 同一套外观：同为「个人凭证」类页面，形状不另起一套 */
.pay-pwd-wrapper {
  display: flex;
  justify-content: center;
  padding: var(--space-40);
}

.pay-pwd-card {
  width: 50%;
  max-width: 560px;
  padding: var(--space-40) var(--space-24);
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
}

.pay-pwd-title {
  margin: 0 0 var(--space-4);
  font-size: var(--fs-lg);
  font-weight: var(--fw-bold);
}

.pay-pwd-intro,
.pay-pwd-state {
  margin: 0;
  font-size: var(--fs-sm);
  color: var(--el-text-color-regular);
}

.pay-pwd-state {
  margin-top: var(--space-8);
}

.pay-pwd-form {
  padding-top: var(--space-24);
  padding-right: var(--space-48);
}

.pay-pwd-actions {
  text-align: center;
}

.pay-pwd-submit {
  padding: var(--space-20) var(--space-32);
}

.pay-pwd-links {
  margin-top: var(--space-16);
  text-align: center;
}
</style>
