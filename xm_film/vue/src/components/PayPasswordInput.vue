<!--
  支付密码 6 格输入。

  支付弹窗与「支付密码」设置页共用这一个实现：两处输入的都是同一件东西（6 位数字），
  各写一份 6 格必然会在自动跳格、粘贴、退格这些边角上分叉。

  组件持有 6 个格子，对外只暴露拼起来的字符串（v-model）与 `complete`（填满即触发，
  调用方据此自动提交）。父组件若把 v-model 置空，格子会跟着清空 —— 校验失败后就靠这个复位。

  父组件用法：
    <PayPasswordInput ref="pwdRef" v-model="payPassword" :disabled="submitting"
                      @complete="submit" />
-->
<template>
  <div class="pay-pwd" @paste="handlePaste">
    <input
      v-for="(digit, index) in digits"
      :key="index"
      :ref="(el) => setInputRef(el, index)"
      :value="digit"
      :disabled="disabled"
      class="pay-pwd__cell"
      :class="{ 'pay-pwd__cell--filled': digit !== '' }"
      type="password"
      inputmode="numeric"
      autocomplete="off"
      maxlength="1"
      :aria-label="`支付密码第 ${index + 1} 位`"
      @input="handleInput(index, $event)"
      @keydown="handleKeydown(index, $event)"
      @focus="handleFocus(index, $event)"
    />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';

const LENGTH = 6;

const props = defineProps({
  modelValue: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
});

const emit = defineEmits(['update:modelValue', 'complete']);

const digits = ref(Array(LENGTH).fill(''));
const inputs = ref([]);

const setInputRef = (el, index) => {
  if (el) inputs.value[index] = el;
};

const onlyDigits = (text) => String(text ?? '').replace(/\D/g, '');
const toCells = (value) => {
  const chars = onlyDigits(value).slice(0, LENGTH).split('');
  return Array.from({ length: LENGTH }, (_, i) => chars[i] ?? '');
};

/** 外层复位（校验失败清空、提交中禁用）时重建格子 */
watch(
    () => props.modelValue,
    (value) => {
      if (onlyDigits(value) === digits.value.join('')) return;
      digits.value = toCells(value);
    },
);

const emitValue = () => {
  const value = digits.value.join('');
  emit('update:modelValue', value);
  if (value.length === LENGTH) emit('complete', value);
};

const focusAt = (index) => {
  const target = inputs.value[Math.max(0, Math.min(LENGTH - 1, index))];
  target?.focus();
  target?.select?.();
};

/** 粘贴的是一整串时，从当前空位起逐格铺开，而不是只塞进聚焦的那一格 */
const distribute = (text, startIndex) => {
  const chars = onlyDigits(text).slice(0, LENGTH).split('');
  if (!chars.length) return;
  const merged = [...digits.value];
  let cursor = startIndex;
  chars.forEach((char) => {
    if (cursor >= LENGTH) return;
    merged[cursor] = char;
    cursor += 1;
  });
  digits.value = merged;
  focusAt(cursor);
  emitValue();
};

const handleFocus = (index, event) => event.target.select();

const handleInput = (index, event) => {
  const raw = event.target.value;
  if (raw.length > 1) {
    distribute(raw, index);
    // distribute 内部已把 digits 更新到位；这里同步一次 DOM，避免受控 :value 未变时残留原文
    syncDom();
    return;
  }

  const digit = onlyDigits(raw);
  if (!digit) {
    // 输入的是非数字：受控 :value 与上次相同则不会重渲染，DOM 里会留下这个字符，必须手动抹掉
    digits.value[index] = '';
    event.target.value = '';
    emitValue();
    return;
  }

  digits.value[index] = digit;
  event.target.value = digit;
  if (index < LENGTH - 1) focusAt(index + 1);
  emitValue();
};

const syncDom = () => {
  digits.value.forEach((digit, index) => {
    if (inputs.value[index]) inputs.value[index].value = digit;
  });
};

const handleKeydown = (index, event) => {
  const { key } = event;

  if (key === 'Backspace') {
    event.preventDefault();
    if (digits.value[index]) {
      digits.value[index] = '';
      event.target.value = '';
    } else if (index > 0) {
      // 当前格已空：退一格并抹掉上一格，符合「一路退着删」的直觉
      digits.value[index - 1] = '';
      syncDom();
      focusAt(index - 1);
    }
    emitValue();
    return;
  }

  if (key === 'Delete') {
    event.preventDefault();
    digits.value[index] = '';
    event.target.value = '';
    emitValue();
    return;
  }

  if (key === 'ArrowLeft') {
    event.preventDefault();
    focusAt(index - 1);
    return;
  }

  if (key === 'ArrowRight') {
    event.preventDefault();
    focusAt(index + 1);
    return;
  }

  // 其余可打印字符一律拦下，格子只接受 0-9（粘贴走 paste 事件）
  if (key.length === 1 && !/\d/.test(key) && !event.ctrlKey && !event.metaKey) {
    event.preventDefault();
  }
};

const handlePaste = (event) => {
  const text = event.clipboardData?.getData('text') ?? '';
  if (!onlyDigits(text)) return;
  event.preventDefault();
  const firstEmpty = digits.value.findIndex((digit) => digit === '');
  distribute(text, firstEmpty === -1 ? 0 : firstEmpty);
  syncDom();
};

defineExpose({
  focus: () => {
    const firstEmpty = digits.value.findIndex((digit) => digit === '');
    focusAt(firstEmpty === -1 ? LENGTH - 1 : firstEmpty);
  },
});
</script>

<style scoped>
.pay-pwd {
  display: flex;
  justify-content: center;
  gap: var(--space-8);
}

.pay-pwd__cell {
  width: 44px;
  height: 52px;
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
  font-size: var(--fs-xl);
  text-align: center;
  caret-color: var(--el-color-primary);
  outline: none;
  transition: border-color 0.15s ease;
}

.pay-pwd__cell:focus {
  border-color: var(--el-color-primary);
}

.pay-pwd__cell--filled {
  border-color: var(--el-color-primary-light-5);
}

.pay-pwd__cell:disabled {
  background: var(--el-fill-color-light);
  color: var(--el-text-color-disabled);
  cursor: not-allowed;
}
</style>
