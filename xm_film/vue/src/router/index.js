import { createRouter, createWebHistory, START_LOCATION } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { clearStoredUser, getStoredUser } from '@/utils/authStorage'
import { LOGIN_PROMPT_MESSAGE, SITE_NAME, loginPath, pathForRole } from '@/utils/navigation'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/front/home' },
    {
      path: '/manage',
      component: () => import('../views/Manage.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] },
      children: [
        { path: 'home', meta: { name: '管理首页', title: '电影购票网站' }, component: () => import('../views/manage/Home.vue') },
        { path: 'admin', meta: { name: '管理员信息' }, component: () => import('../views/manage/Admin.vue') },
        { path: 'user', meta: { name: '用户信息' }, component: () => import('../views/manage/User.vue') },
        { path: 'password', meta: { name: '修改密码' }, component: () => import('../views/manage/Password.vue') },
        { path: 'person', meta: { name: '个人资料' }, component: () => import('../views/manage/Person.vue') },
        { path: 'notice', meta: { name: '系统公告' }, component: () => import('../views/manage/Notice.vue') },
        { path: 'cinema', meta: { name: '影院信息' }, component: () => import('../views/manage/Cinema.vue') },
        { path: 'type', meta: { name: '电影分类' }, component: () => import('../views/manage/Type.vue') },
        { path: 'area', meta: { name: '电影地区' }, component: () => import('../views/manage/Area.vue') },
        { path: 'film', meta: { name: '电影信息' }, component: () => import('../views/manage/Film.vue') },
        { path: 'video', meta: { name: '电影预告' }, component: () => import('../views/manage/Video.vue') },
        { path: 'actor', meta: { name: '演职人员' }, component: () => import('../views/manage/Actor.vue') },
        { path: 'room', meta: { name: '影厅管理' }, component: () => import('../views/manage/Room.vue') },
        { path: 'cinemaDirectory', meta: { name: '影院名录' }, component: () => import('../views/manage/CinemaDirectory.vue') },
        { path: 'record', meta: { name: '放映记录' }, component: () => import('../views/manage/Record.vue') },
        { path: 'ordered', meta: { name: '购票订单' }, component: () => import('../views/manage/Ordered.vue') },
        { path: 'mark', meta: { name: '用户评价' }, component: () => import('../views/manage/Mark.vue') },
      ],
    },
    {
      path: '/back',
      component: () => import('../views/Back.vue'),
      meta: { requiresAuth: true, roles: ['CINEMA'] },
      children: [
        { path: 'home', meta: { name: '影院首页' }, component: () => import('../views/back/Home.vue') },
        { path: 'film', meta: { name: '电影信息' }, component: () => import('../views/back/Film.vue') },
        { path: 'room', meta: { name: '影厅管理' }, component: () => import('../views/back/Room.vue') },
        { path: 'record', meta: { name: '放映记录' }, component: () => import('../views/back/Record.vue') },
        { path: 'ordered', meta: { name: '购票订单' }, component: () => import('../views/back/Ordered.vue') },
        { path: 'person', meta: { name: '个人资料' }, component: () => import('../views/back/Person.vue') },
        { path: 'password', meta: { name: '修改密码' }, component: () => import('../views/back/Password.vue') },
      ],
    },
    {
      path: '/front',
      component: () => import('../views/Front.vue'),
      children: [
        { path: 'home', meta: { guest: true, name: '首页', title: '电影购票网站' }, component: () => import('../views/front/Home.vue') },
        { path: 'orders', meta: { requiresAuth: true, roles: ['USER'], name: '购票记录' }, component: () => import('../views/front/Orders.vue') },
        { path: 'account', meta: { requiresAuth: true, roles: ['USER'], name: '我的账户' }, component: () => import('../views/front/Account.vue') },
        { path: 'buyTicket', meta: { requiresAuth: true, roles: ['USER'], name: '购票' }, component: () => import('../views/front/BuyTicket.vue') },
        { path: 'person', meta: { requiresAuth: true, roles: ['USER'], name: '个人中心' }, component: () => import('../views/front/Person.vue') },
        { path: 'password', meta: { requiresAuth: true, roles: ['USER'], name: '修改密码' }, component: () => import('../views/front/Password.vue') },
        // 支付密码：设置 / 修改 / 忘记重设三态同页，余额支付的前置条件
        { path: 'payPassword', meta: { requiresAuth: true, roles: ['USER'], name: '支付密码' }, component: () => import('../views/front/PayPassword.vue') },
        { path: 'movie', meta: { guest: true, name: '电影列表' }, component: () => import('../views/front/Movie.vue') },
        { path: 'cinema', meta: { guest: true, name: '影院列表' }, component: () => import('../views/front/Cinema.vue') },
        { path: 'rank', meta: { guest: true, name: '排行榜' }, component: () => import('../views/front/Rank.vue') },
        // 取票大厅是自助机口径：免登录，凭取票码核销（见后端 TicketController）
        { path: 'pickup', meta: { guest: true, name: '取票大厅' }, component: () => import('../views/front/Pickup.vue') },
        { path: 'filmDetail/:id', meta: { guest: true, name: '电影详情' }, component: () => import('../views/front/FilmDetail.vue') },
        // 影评页：只从影片详情页与购票记录两处进入，不进顶部导航（guest 可读，游客也能看别人的评价）
        { path: 'filmMarks/:id', meta: { guest: true, name: '影评' }, component: () => import('../views/front/FilmMarks.vue') },
        { path: 'filmCinema/:id', meta: { guest: true, name: '选择影院' }, component: () => import('../views/front/FilmCinema.vue') },
        { path: 'cinemaDetail/:id', meta: { guest: true, name: '影院详情' }, component: () => import('../views/front/CinemaDetail.vue') },
        { path: 'search', meta: { guest: true, name: '搜索结果' }, component: () => import('../views/front/Search.vue') },
      ],
    },
    { path: '/404', meta: { name: '404', title: '404页面', guest: true }, component: () => import('../views/404.vue') },
    { path: '/login', meta: { name: '登录', title: '欢迎登录电影购票系统', guest: true }, component: () => import('../views/Login.vue') },
    { path: '/register', meta: { name: '注册', title: '欢迎注册电影购票系统', guest: true }, component: () => import('../views/Register.vue') },
    { path: '/:pathMatch(.*)*', redirect: '/404' },
  ],
})

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
