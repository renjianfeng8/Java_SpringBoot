import { ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { LOGIN_PROMPT_MESSAGE, loginPath } from '@/utils/navigation'

/**
 * 游客触发写操作时的登录引导。
 * 与路由守卫使用同一句文案和同一个 loginPath 规则，避免两处回跳口径漂移。
 */
export function useLoginPrompt() {
  const route = useRoute()
  const router = useRouter()

  function promptLogin(fromPath = route.fullPath) {
    return ElMessageBox.confirm(LOGIN_PROMPT_MESSAGE, '提示', {
      confirmButtonText: '去登录',
      cancelButtonText: '取消',
      type: 'warning',
    })
      .then(() => router.push(loginPath(fromPath)))
      .catch(() => {})
  }

  return { promptLogin }
}
