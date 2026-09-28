<template>
  <div v-if="hasError" class="error-boundary-fallback">
    <div class="error-boundary-content">
      <el-icon class="error-icon" :size="48" color="var(--el-color-danger)">
        <WarningFilled />
      </el-icon>
      <h2 class="error-boundary-title">页面渲染异常</h2>
      <p class="error-boundary-message">
        组件加载时发生了意外错误，请尝试刷新
      </p>
      <p v-if="errorMessage" class="error-boundary-detail">
        {{ errorMessage }}
      </p>
      <el-button type="primary" @click="handleRetry" class="retry-btn">
        重新加载
      </el-button>
      <el-button @click="handleGoBack" v-if="canGoBack">
        返回上一页
      </el-button>
    </div>
  </div>
  <template v-else>
    <slot />
  </template>
</template>

<script setup>
import { ref, onErrorCaptured } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  boundaryName: { type: String, default: '未知区域' }
})

const router = useRouter()
const hasError = ref(false)
const errorMessage = ref('')
const canGoBack = ref(window.history.length > 1)

onErrorCaptured((err, instance, info) => {
  hasError.value = true
  errorMessage.value = err.message || String(err)
  console.error(`[ErrorBoundary/${props.boundaryName}]`, err, info)
  return false
})

function handleRetry() {
  hasError.value = false
  errorMessage.value = ''
}

function handleGoBack() {
  router.back()
}
</script>

<style scoped>
.error-boundary-fallback {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
  padding: var(--space-40);
}

.error-boundary-content {
  max-width: 480px;
  text-align: center;
}

.error-icon {
  margin-bottom: var(--space-16);
}

.error-boundary-title {
  margin: 0 0 var(--space-8);
  font-size: var(--fs-xl);
  color: var(--el-text-color-primary);
}

/* 正文尺寸的文字须达 4.5:1；--el-text-color-secondary 只有 3.08:1，仅够大文本 */
.error-boundary-message {
  margin: 0 0 var(--space-12);
  font-size: var(--fs-base);
  color: var(--el-text-color-regular);
}

.error-boundary-detail {
  max-height: 60px;
  margin: 0 0 var(--space-24);
  font-size: var(--fs-xs);
  color: var(--el-text-color-regular);
  word-break: break-all;
  overflow: hidden;
}

.retry-btn {
  margin-right: var(--space-8);
}
</style>
