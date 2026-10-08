<!--
  支付密码的设置 / 修改页 —— 两步步进器。

  第一步「验证身份」只出一个凭证输入：已设置的用户是 6 格原支付密码（填满即自动验证），
  未设置的用户是登录密码（回车验证）。服务端验过之后才过渡到第二步「设置新密码」。
  第二步收新密码与再次确认，由显式的「确认设置」按钮提交。

  第一步的验证走 /pay-password/verify-old 与 /pay-password/verify-login（只验不写），
  第二步仍走原来那两个写端点（PUT /pay-password 与 PUT /pay-password/reset），
  并把第一步的凭证带上重发一次 —— 第一步的"通过"只是一道 UI 闸门，真正的门在第二步原样再验，
  所以第一步永远不会成为绕过写入校验的旁路。

  两种模式：
    修改（已设置）—— 第一步验原支付密码，走 PUT /account/pay-password
    设置（未设置）—— 第一步验登录密码，走 PUT /account/pay-password/reset
  已设置的用户可点「忘记支付密码」切到后者的路径（验登录密码），这是忘记时的唯一自救出口。

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

      <ol class="pay-pwd-steps">
        <li class="pay-pwd-steps__item" :class="{ 'is-active': step === 1, 'is-done': step === 2 }">
          <span class="pay-pwd-steps__dot">1</span>验证身份
        </li>
        <li class="pay-pwd-steps__item" :class="{ 'is-active': step === 2 }">
          <span class="pay-pwd-steps__dot">2</span>设置新密码
        </li>
      </ol>

      <el-form class="pay-pwd-form" label-width="120px" @submit.prevent>
        <Transition name="pwd-step" mode="out-in" @after-enter="focusStepInput">
          <!-- ============ 第一步：验证身份 ============ -->
          <div v-if="step === 1" key="verify">
            <el-form-item v-if="isChange" label="原支付密码">
              <PayPasswordInput ref="oldPwdRef" v-model="form.oldPassword" :disabled="submitting"
                                @complete="verifyIdentity" />
            </el-form-item>
            <el-form-item v-else label="登录密码">
              <el-input show-password v-model="form.loginPassword" autocomplete="off"
                        placeholder="请输入登录密码，回车继续"
                        :disabled="submitting" @keyup.enter="verifyIdentity" />
            </el-form-item>
          </div>

          <!-- ============ 第二步：设置新密码 ============ -->
          <div v-else key="set">
            <el-form-item label="新支付密码">
              <PayPasswordInput ref="newPwdRef" v-model="form.payPassword" :disabled="submitting" />
            </el-form-item>
            <el-form-item label="确认新支付密码">
              <PayPasswordInput v-model="form.confirmPassword" :disabled="submitting" />
            </el-form-item>
          </div>
        </Transition>

        <!-- 第一步不设按钮：改密态填满 6 格即自动验证，设置态按回车 —— 第二步才给显式提交 -->
        <div v-if="step === 2" class="pay-pwd-actions">
          <el-button :loading="submitting" type="primary" class="pay-pwd-submit" @click="submit">
            {{ isChange ? '立即修改' : '确认设置' }}
          </el-button>
        </div>
      </el-form>

      <div class="pay-pwd-links">
        <template v-if="step === 2">
          <el-link type="primary" :underline="false" @click="backToVerify">返回上一步</el-link>
        </template>
        <template v-else-if="isChange">
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
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import request from '@/utils/request.js';
import { ACCOUNT_API } from '@/constants';
import PayPasswordInput from '@/components/PayPasswordInput.vue';

const submitting = ref(false);
const hasPayPassword = ref(false);
/** 已设置 = 修改模式（第一步验原支付密码）；未设置 = 设置模式（第一步验登录密码） */
const mode = ref('change');
/** 1 = 验证身份，2 = 设置新密码 */
const step = ref(1);

const isChange = computed(() => mode.value === 'change');

const form = reactive({
  oldPassword: '',
  loginPassword: '',
  payPassword: '',
  confirmPassword: '',
});

const oldPwdRef = ref();
const newPwdRef = ref();

const SIX_DIGITS = /^\d{6}$/;

const resetFields = () => {
  form.oldPassword = '';
  form.loginPassword = '';
  form.payPassword = '';
  form.confirmPassword = '';
};

const switchMode = (change) => {
  mode.value = change ? 'change' : 'reset';
  step.value = 1;
  resetFields();
};

/**
 * 面板过渡结束后再聚焦。out-in 模式下新面板要等旧面板淡出完才入 DOM，
 * 所以切换之后紧跟的 nextTick 里它还不在，聚焦必须挂在这个钩子上。
 */
const focusStepInput = () => {
  if (step.value === 2) {
    newPwdRef.value?.focus();
  } else {
    oldPwdRef.value?.focus();
  }
};

const backToVerify = () => {
  if (submitting.value) return;
  step.value = 1;
  form.payPassword = '';
  form.confirmPassword = '';
};

const loadState = async () => {
  try {
    const res = await request.get(ACCOUNT_API.SUMMARY);
    const known = res.code === '200';
    if (known) {
      hasPayPassword.value = Boolean(res.data?.hasPayPassword);
    }
    // 状态取不到时退回「设置」模式：改密模式要的「原支付密码」只有已设置的用户才有，
    // 设置模式（验登录密码）对两种状态都走得通 —— 已设置的用户来这里重设也是合法路径。
    mode.value = known && hasPayPassword.value ? 'change' : 'reset';
  } catch (error) {
    // 网络类提示由 request.js 响应拦截器统一给出，这里只把模式退回两态通用的「设置」态
    console.error('支付密码状态查询异常：', error);
    mode.value = 'reset';
  }
};

/** 第一步：只验不写。验过才放行到第二步，失败就地把输入清掉重来 */
const verifyIdentity = async () => {
  if (submitting.value) return;

  if (isChange.value && !SIX_DIGITS.test(form.oldPassword)) {
    ElMessage.error('请输入 6 位原支付密码');
    return;
  }
  if (!isChange.value && !form.loginPassword) {
    ElMessage.error('请输入登录密码');
    return;
  }

  submitting.value = true;
  try {
    const res = isChange.value
        ? await request.post(ACCOUNT_API.VERIFY_OLD_PASSWORD, { oldPassword: form.oldPassword })
        : await request.post(ACCOUNT_API.VERIFY_LOGIN_PASSWORD, { loginPassword: form.loginPassword });

    if (res.code === '200') {
      // 聚焦交给过渡的 after-enter 钩子（此刻新面板还没入 DOM）
      step.value = 2;
      return;
    }

    // 密码错误、锁定这类失败都留在第一步，清空输入后焦点回第一格。
    // 这里没有面板过渡，直接在 nextTick 里聚焦即可。
    ElMessage.error(res.msg || '身份验证失败');
    if (isChange.value) {
      form.oldPassword = '';
      await nextTick();
      oldPwdRef.value?.focus();
    } else {
      form.loginPassword = '';
    }
  } finally {
    submitting.value = false;
  }
};

/** 第二步：写入口仍带第一步的凭证重发一次，第一步的通过从不被信任 */
const submit = async () => {
  if (submitting.value) return;
  if (!SIX_DIGITS.test(form.payPassword)) {
    ElMessage.error('请输入 6 位新支付密码');
    return;
  }
  if (form.payPassword !== form.confirmPassword) {
    ElMessage.error('两次输入的支付密码不一致');
    return;
  }

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
      step.value = 1;
      resetFields();
      return;
    }

    // 密码错误、锁定这类失败要保留「已设置」的既有状态，只清空两个新密码格
    ElMessage.error(res.msg || '支付密码设置失败');
    form.payPassword = '';
    form.confirmPassword = '';
  } finally {
    submitting.value = false;
  }
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

/* 两步高度不同（第二步多一格里），给个下限让卡片在切换时不长高 —— 否则再顺的淡入也白搭 */
.pay-pwd-card {
  width: 50%;
  max-width: 560px;
  min-height: 420px;
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

.pay-pwd-steps {
  display: flex;
  gap: var(--space-24);
  margin: var(--space-20) 0 0;
  padding: 0;
  list-style: none;
}

.pay-pwd-steps__item {
  display: flex;
  align-items: center;
  gap: var(--space-8);
  font-size: var(--fs-sm);
  color: var(--el-text-color-placeholder);
}

.pay-pwd-steps__item.is-active,
.pay-pwd-steps__item.is-done {
  color: var(--el-text-color-primary);
}

.pay-pwd-steps__dot {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--el-fill-color);
  color: var(--el-text-color-regular);
  font-size: var(--fs-xs);
}

.pay-pwd-steps__item.is-active .pay-pwd-steps__dot {
  background: var(--el-color-primary);
  color: var(--color-on-accent);
}

/* 已完成的第一步只保留描边，不抢当前步的视线 */
.pay-pwd-steps__item.is-done .pay-pwd-steps__dot {
  border: 1px solid var(--el-color-primary);
  background: var(--el-bg-color);
  color: var(--el-color-primary);
}

.pay-pwd-form {
  padding-top: var(--space-24);
  padding-right: var(--space-48);
}

/* 两步切换：淡入 + 轻微上移，进出串行（out-in）以免两个面板同时在流里把卡片顶高 */
.pwd-step-enter-active,
.pwd-step-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.pwd-step-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.pwd-step-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

@media (prefers-reduced-motion: reduce) {
  .pwd-step-enter-active,
  .pwd-step-leave-active {
    transition: none;
  }
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
