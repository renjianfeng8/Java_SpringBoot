import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getStoredUser, clearStoredUser } from '@/utils/authStorage'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/',
  timeout: 30000,
})

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
