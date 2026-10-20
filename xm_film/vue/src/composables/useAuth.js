import { ref, computed } from 'vue'
import { isSuccess, AUTH_API } from '@/constants'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import {
  AUTH_CHANGE_EVENT,
  USER_KEY,
  getStoredUser,
  setStoredUser,
  clearStoredUser,
} from '@/utils/authStorage'

const globalUser = ref(null)
// 模块级单例：/auth/me 整个应用生命周期只验一次。init 仍每次同步 storage，
// 但不再让每个调用 useAuth() 的组件各自发一轮 GET /auth/me。
let verified = false

// storage 是持久化来源；request.js 的 401 处理与跨标签页改动都会走这里，
// 保证守卫/页面看到的是同一份会话，而不是「storage 已清、globalUser 还在」。
if (typeof window !== 'undefined') {
  window.addEventListener(AUTH_CHANGE_EVENT, (event) => {
    globalUser.value = event.detail ?? null
  })
  window.addEventListener('storage', (event) => {
    if (event.key === USER_KEY || event.key === null) {
      globalUser.value = getStoredUser()
    }
  })
}

export function useAuth() {
  const user = globalUser

  const token = computed(() => user.value?.token || '')
  // 登录态只看 storage 里的令牌：冷加载时先按已登录渲染，/me 失败再退出。
  // 此前还要求 !globalVerifying，导致首屏先闪一次「登录/注册」。
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isCinema = computed(() => user.value?.role === 'CINEMA')
  const isUser = computed(() => user.value?.role === 'USER')

  const hasRole = (role) => user.value?.role === role
  const hasAnyRole = (...roles) => roles.includes(user.value?.role)

  function init() {
    user.value = getStoredUser()
    if (verified) return
    verified = true
    verifyToken()
  }

  async function verifyToken() {
    // 令牌在发请求前取一次：/me 不返回 token（只有 login 才注入），
    // 解析时再从 storage 读会让「验签期间被登出」的响应把 token 写成 undefined。
    const currentToken = user.value?.token
    if (!currentToken) {
      return
    }
    try {
      const res = await request.get(AUTH_API.ME)
      // 验签期间登录态可能已变（登出 / 换号 / 他标签页清空），此结果作废，避免复活半个会话
      if (user.value?.token !== currentToken) return
      if (isSuccess(res)) {
        setUser({ ...res.data, token: currentToken })
      } else {
        logout()
      }
    } catch (e) {
      if (user.value?.token !== currentToken) return
      if (e.response?.status === 401 || e.response?.status === 403) {
        logout()
      }
    }
  }

  function setUser(userData) {
    setStoredUser(userData)
    user.value = userData
  }

  function logout() {
    clearStoredUser()
    user.value = null
  }

  async function login(credentials) {
    const res = await request.post(AUTH_API.LOGIN, credentials)
    if (isSuccess(res)) {
      setUser(res.data)
      ElMessage.success('登录成功')
      return res.data
    }
    throw new Error(res.msg || '登录失败')
  }

  init()

  return { user, token, isLoggedIn, isAdmin, isCinema, isUser,
           hasRole, hasAnyRole, init, setUser, logout, login }
}
