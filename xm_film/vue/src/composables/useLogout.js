import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

/**
 * 退出登录的唯一实现：确认框、清会话、回登录页、成功/取消提示。
 * 前台外壳与两个后台外壳此前各自内联一份，改文案要同步三处。
 */
export function useLogout() {
  const router = useRouter()
  const { logout } = useAuth()

  function confirmLogout() {
    return ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
      .then(() => {
        logout()
        void router.push('/login')
        ElMessage.success('退出成功')
      })
      .catch(() => {
        ElMessage.info('已取消退出')
      })
  }

  return { confirmLogout }
}
