<template>
  <ElConfigProvider :locale="zhCn">
    <ErrorBoundary boundaryName="App">
      <RouterView />
    </ErrorBoundary>
  </ElConfigProvider>
</template>

<script setup>
import { watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import ErrorBoundary from '@/components/ErrorBoundary.vue'

const route = useRoute()

// 前台挂品牌红主题；后台保持构建期默认蓝（index.scss 的 $colors）。
// 必须挂在 documentElement 上 —— EP 的 ElSelect / ElTooltip / ElMessage 会 teleport 到 body，
// 挂在布局容器上的变量传不到那些子树。见 tokens.scss 的 html.theme-front 说明。
watch(
  () => route.path,
  (path) => {
    document.documentElement.classList.toggle('theme-front', path.startsWith('/front'))
  },
  { immediate: true },
)
</script>
