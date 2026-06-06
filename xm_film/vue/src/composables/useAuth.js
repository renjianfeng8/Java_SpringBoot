import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { getStoredUser, setStoredUser, clearStoredUser } from '@/utils/authStorage'
import { AUTH_API } from '@/constants'

const globalUser = ref(null)

export function useAuth() {
  const user = globalUser

  const token = computed(() => user.value?.token || '')
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isCinema = computed(() => user.value?.role === 'CINEMA')
  const isUser = computed(() => user.value?.role === 'USER')

  const hasRole = (role) => user.value?.role === role
  const hasAnyRole = (...roles) => roles.includes(user.value?.role)

  function init() {
    user.value = getStoredUser()
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
    if (res.code === '200') {
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
