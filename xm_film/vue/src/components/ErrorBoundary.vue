<template>
  <div v-if="hasError" class="error-boundary-fallback">
    <div class="error-boundary-content">
      <el-icon class="error-icon" :size="48" color="#f56c6c">
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
import { WarningFilled } from '@element-plus/icons-vue'

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
  padding: 40px;
}
.error-boundary-content {
  text-align: center;
  max-width: 480px;
}
.error-icon {
  margin-bottom: 16px;
}
.error-boundary-title {
  font-size: 20px;
  color: #303133;
  margin: 0 0 8px;
}
.error-boundary-message {
  font-size: 14px;
  color: #909399;
  margin: 0 0 12px;
}
.error-boundary-detail {
  font-size: 12px;
  color: #c0c4cc;
  margin: 0 0 24px;
  word-break: break-all;
  max-height: 60px;
  overflow: hidden;
}
.retry-btn {
  margin-right: 8px;
}
</style>
