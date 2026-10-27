import { createRouter, createWebHistory, START_LOCATION } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { clearStoredUser, getStoredUser } from '@/utils/authStorage'
import { LOGIN_PROMPT_MESSAGE, SITE_NAME, loginPath, pathForRole } from '@/utils/navigation'

import { routes } from './routes'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

// 登录引导确认框的「在途」标志：受保护页上双击链接会重入守卫，
// 不拦的话会叠出两个框，先应答的那个把陈旧的 next 用掉（FE-08）。
let promptingLogin = false

router.beforeEach((to, from, next) => {
  const pageTitle = to.meta.title || SITE_NAME
  if (document.title !== pageTitle) {
    document.title = pageTitle
  }

  // 鉴权判定必须先于 guest，且 roles 本身即蕴含"需登录"：
  // Vue Router 会把父路由的 meta 合并进子路由，任何"只看其中一个标记"的写法都会在标记共存时 fail-open
  // （guest 压过 requiresAuth 就是 e3089365 修的这类；只判 requiresAuth 则会让未来的 roles-only 路由裸奔）。
  // 公开页（login/register/404 与前台游客页）既无 requiresAuth 也无 roles，落到末尾的 next() 直接放行。
  if (to.meta.requiresAuth || to.meta.roles) {
    const user = getStoredUser()
    if (!user?.token || !user?.role) {
      clearStoredUser()
      if (promptingLogin) {
        return next(false)
      }
      promptingLogin = true
      ElMessageBox.confirm(LOGIN_PROMPT_MESSAGE, '提示', {
        confirmButtonText: '去登录',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 必须带上 query 并编码：购票页的场次由 cinemaId/filmId/recordId/roomId 四个参数决定，
        // 只回跳 to.path 会让登录后落在一个没有参数的 /front/buyTicket 上（页面只能报"参数无效"）
        next(loginPath(to.fullPath))
      }).catch(() => {
        // 首次导航（地址栏直入 / 刷新 / 书签）没有上一页可回退：此时 next(false) 会中止唯一一次导航，
        // RouterView 匹配为空 → 白屏。改为落到前台首页，让用户仍留在应用内；
        // 非首次导航仍按「取消」处理，停留在当前页。
        if (from === START_LOCATION) {
          next('/front/home')
        } else {
          next(false)
        }
      }).finally(() => {
        promptingLogin = false
      })
      return
    }

    // 角色不符：回到该角色自己的主页，而不是登录页。
    // 已登录用户被踢到登录页既丢来路，又会在登录页看到一个「已登录」的会话。
    if (to.meta.roles && !to.meta.roles.includes(user.role)) {
      ElMessage.error('无权访问该页面')
      const home = pathForRole(user.role)
      return to.path === home ? next(false) : next(home)
    }
  }

  // 公开页 / 无 meta 限制的路由直接放行
  next()
})

export default router
