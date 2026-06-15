import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { ElNotification } from 'element-plus'
import '@/assets/css/global.css'

const app = createApp(App)

app.use(router)

app.config.errorHandler = (err, instance, info) => {
  console.error('[GlobalErrorHandler]', err, info)
  const message = err?.message || String(err)
  ElNotification({
    title: '系统异常',
    message: message.length > 120 ? message.slice(0, 120) + '…' : message,
    type: 'error',
    duration: 5000,
  })
}

app.mount('#app')
