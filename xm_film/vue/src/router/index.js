import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { clearStoredUser, getStoredUser } from '@/utils/authStorage'

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
        { path: 'buyTicket', meta: { requiresAuth: true, roles: ['USER'], name: '购票' }, component: () => import('../views/front/BuyTicket.vue') },
        { path: 'person', meta: { requiresAuth: true, roles: ['USER'], name: '个人中心' }, component: () => import('../views/front/Person.vue') },
        { path: 'password', meta: { requiresAuth: true, roles: ['USER'], name: '修改密码' }, component: () => import('../views/front/Password.vue') },
        { path: 'movie', meta: { guest: true, name: '电影列表' }, component: () => import('../views/front/Movie.vue') },
        { path: 'cinema', meta: { guest: true, name: '影院列表' }, component: () => import('../views/front/Cinema.vue') },
        { path: 'rank', meta: { guest: true, name: '排行榜' }, component: () => import('../views/front/Rank.vue') },
        { path: 'filmDetail/:id', meta: { guest: true, name: '电影详情' }, component: () => import('../views/front/FilmDetail.vue') },
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
  document.title = to.meta.title || '电影购票网站'

  // 1. 访客页面（login/register/404）无条件放行
  if (to.meta.guest) {
    return next()
  }

  // 2. 需要登录的页面
  if (to.meta.requiresAuth) {
    const user = getStoredUser()
    if (!user?.token || !user?.role) {
      clearStoredUser()
      ElMessageBox.confirm('请先登录后再进行此操作', '提示', {
        confirmButtonText: '去登录',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        next(`/login?redirect=${to.path}`)
      }).catch(() => {
        next(false)
      })
      return
    }

    // 角色校验
    if (to.meta.roles && !to.meta.roles.includes(user.role)) {
      ElMessage.error('无权访问该页面')
      return next('/login')
    }
  }

  // 3. 无 meta 限制的路由直接放行
  next()
})

export default router
