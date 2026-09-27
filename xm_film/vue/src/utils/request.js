import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getStoredUser, clearStoredUser } from '@/utils/authStorage'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/',
  timeout: 30000,
})

let redirectingToLogin = false

/**
 * 登录态失效统一跳登录页，并带上来源路径供登录后回跳。
 * 只有 AuthInterceptor 直写 setStatus(401) 的响应才会走到这里；
 * 业务异常（含登录失败）经 GlobalExceptionHandler 返回的是 HTTP 200 + body code=401，
 * 不会命中该分支，所以不存在"登录页自己 401 再跳登录页"的死循环。
 */
function redirectToLogin() {
  if (redirectingToLogin) return
  const { pathname, search } = window.location
  if (pathname.startsWith('/login')) return
  redirectingToLogin = true
  window.location.href = `/login?redirect=${encodeURIComponent(pathname + search)}`
}

request.interceptors.request.use(
  config => {
    config.headers['Content-Type'] = 'application/json;charset=utf-8'

    const user = getStoredUser()
    if (user?.token) {
      config.headers.Authorization = `Bearer ${user.token}`
    }

    return config
  },
  error => Promise.reject(error)
)

request.interceptors.response.use(
  response => {
    const res = response.data
    return typeof res === 'string' ? (res ? JSON.parse(res) : res) : res
  },
  error => {
    if (error.code === 'ERR_NETWORK') {
      ElMessage.error('无法连接到服务器，请检查网络或后端是否启动')
      return Promise.reject(error)
    }

    if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查网络后重试')
      return Promise.reject(error)
    }

    const status = error.response?.status
    const backendMessage = error.response?.data?.msg

    if (status === 401) {
      clearStoredUser()
      redirectToLogin()
    }

    if (backendMessage) {
      ElMessage.error(backendMessage)
      return Promise.reject(error)
    }

    switch (status) {
      case 401:
        ElMessage.warning('登录状态已过期，请重新登录')
        break
      case 403:
        ElMessage.error('权限不足，无法访问')
        break
      case 404:
        ElMessage.error('未找到请求接口')
        break
      case 500:
        ElMessage.error('系统异常，请检查后端控制台报错')
        break
      default:
        if (status) {
          ElMessage.error(`请求失败 (${status})`)
        } else {
          ElMessage.error(`请求失败: ${error.message}`)
        }
    }

    return Promise.reject(error)
  }
)

export default request
