import { createRouter, createWebHistory } from 'vue-router'

/**
 * 路由配置
 * 包含路由路径、组件映射、导航守卫等设置
 */
const router = createRouter({
    // 使用HTML5 History模式，基于浏览器History API实现无刷新导航
    history: createWebHistory(import.meta.env.BASE_URL),

    // 路由映射表
    routes: [
        // 根路径重定向到登录页
        { path: '/', redirect: '/login' },

        // 管理页面（包含子路由）
        {
            path: '/manage',
            component: () => import('../views/Manage.vue'), // 懒加载布局组件
            children: [
                { path: 'home',  meta: { name: '电影购票网站', title:'电影购票网站' }, component: () => import('../views/manage/Home.vue') },
                { path: 'admin',  meta: { name: '管理员信息' }, component: () => import('../views/manage/Admin.vue') },
                { path: 'user',  meta: { name: '用户信息' }, component: () => import('../views/manage/User.vue') },
                { path: 'password', meta: { name: '修改密码' }, component: () => import('../views/manage/Password.vue') },
                { path: 'person',  meta: { name: '个人资料' }, component: () => import('../views/manage/Person.vue') },
                { path: 'notice',  meta: { name: '系统公告' }, component: () => import('../views/manage/Notice.vue') },
                { path: 'cinema',  meta: { name: '影院信息' }, component: () => import('../views/manage/Cinema.vue') },
                { path: 'type',  meta: { name: '电影分类' }, component: () => import('../views/manage/Type.vue') },
                { path: 'area',  meta: { name: '电影区域' }, component: () => import('../views/manage/Area.vue') },
                { path: 'film',  meta: { name: '电影信息' }, component: () => import('../views/manage/Film.vue') },
                { path: 'video',  meta: { name: '电影视频预告' }, component: () => import('../views/manage/Video.vue') },
                { path: 'actor',  meta: { name: '演职人员' }, component: () => import('../views/manage/Actor.vue') },
                { path: 'room',  meta: { name: '影厅房间' }, component: () => import('../views/manage/Room.vue') },
                { path: 'record',  meta: { name: '放映记录' }, component: () => import('../views/manage/Record.vue') },
                { path: 'ordered',  meta: { name: '购票订单' }, component: () => import('../views/manage/Ordered.vue') },
                { path: 'mark',  meta: { name: '用户评价' }, component: () => import('../views/manage/Mark.vue') },
            ]
        },
        // 影院后台页面 (影院管理员)
        {
            path: '/back',
            component: () => import('../views/Back.vue'), // 影院管理员布局
            children: [
                { path: 'home', meta: { name: '影院首页' }, component: () => import('../views/back/Home.vue') },
                { path: 'film', meta: { name: '电影信息' }, component: () => import('../views/back/Film.vue') },
                { path: 'room', meta: { name: '影厅管理' }, component: () => import('../views/back/Room.vue') },
                { path: 'record', meta: { name: '放映记录' }, component: () => import('../views/back/Record.vue') },
                { path: 'ordered', meta: { name: '购票订单' }, component: () => import('../views/back/Ordered.vue') },
                { path: 'person',  meta: { name: '个人资料' }, component: () => import('../views/back/Person.vue') },
                { path: 'password',  meta: { name: '修改密码' }, component: () => import('../views/back/Password.vue') },

            ]
        },
        // 用户前台
        {
            path: '/front',
            component: () => import('../views/Front.vue'),
            children: [
                { path: 'home',  meta: { name: '电影购票网站', title: '电影购票网站' }, component: () => import('../views/front/Home.vue') },
                { path: 'person', component: () => import('../views/front/Person.vue') },
                { path: 'password', component: () => import('../views/front/Password.vue') },
                { path: 'movie', component: () => import('../views/front/Movie.vue') },
                { path: 'cinema', component: () => import('../views/front/Cinema.vue') },
                { path: 'rank', component: () => import('../views/front/Rank.vue') },
                { path: 'orders', component: () => import('../views/front/Orders.vue') },
                // 添加电影详情页路由
                { path: 'filmDetail/:id', component: () => import('../views/front/FilmDetail.vue') },
                { path: 'filmCinema/:id', component: () => import('../views/front/FilmCinema.vue') },
                { path: 'cinemaDetail/:id', component: () => import('../views/front/CinemaDetail.vue') },
                // 搜索结果页路由
                { path: 'search', component: () => import('../views/front/Search.vue') },
                { path: 'buyTicket', component: () => import('../views/front/BuyTicket.vue') }
            ]
        },

        // 404错误页
        { path: '/404', name: 'NOT FOUND', meta: { name: '404找不到页面',title:'404页面' }, component: () => import('../views/404.vue') },

        // 登录/注册页面
        { path: '/login', name: 'login', meta: { name: '登录系统',title:'欢迎登录电影购票系统' }, component: () => import('../views/Login.vue') },
        { path: '/register', name: 'register', meta: { name: '注册页面',title:'欢迎注册电影购票系统'  }, component: () => import('../views/Register.vue') },

        // 通配符路由，匹配所有未定义路径并重定向到404页
        { path: '/:pathMatch(.*)', redirect: '/404' }
    ]
})

/**
 * 全局前置守卫
 * 在路由跳转前执行，可用于权限验证、页面标题设置等
 */
router.beforeEach((to, from, next) => {
    // 更新页面标题为路由元信息中的title
    document.title = to.meta.title || '电影购票网站';
    // 必须调用next()才能继续路由跳转
    next();
})

export default router
