import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getStoredUser } from '@/utils/authStorage'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:9090',
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
    const status = error.response?.status

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
        ElMessage.error(`请求失败: ${error.message}`)
    }

    return Promise.reject(error)
  }
)

export default request
