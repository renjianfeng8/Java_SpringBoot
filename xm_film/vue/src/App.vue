<template>
  <ElConfigProvider :locale="zhCn">
    <ErrorBoundary boundaryName="App">
      <RouterView />
    </ErrorBoundary>
  </ElConfigProvider>
</template>

<script setup>
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import ErrorBoundary from '@/components/ErrorBoundary.vue'
</script>
