/**
 * 两个后台外壳（管理员 / 影院）的菜单数据。
 * 外壳组件只负责布局，菜单项与分组在这里维护，避免两个 90% 相同的模板各自演进。
 *
 * 图标传组件：外壳已用 `<component :is="item.icon" />` 渲染，
 * 未使用的图标由打包器按引用摇掉。
 */
import {
  Avatar,
  Bell,
  Calendar,
  CreditCard,
  Document,
  Grid,
  House,
  Location,
  Lock,
  OfficeBuilding,
  Setting,
  Star,
  Tickets,
  User,
  VideoCamera,
  VideoPlay,
} from '@element-plus/icons-vue'

const HOME_ITEM = { label: '系统首页', path: 'home', icon: House }

function withPrefix(items, prefix) {
  return items.map((item) => ({ ...item, path: `${prefix}${item.path}` }))
}

/** 管理员后台侧边栏（路径为 /manage 下的绝对路径） */
export const ADMIN_MENU = {
  title: '电影购票网站 - 管理员后台',
  home: '/manage/home',
  homeItem: { ...HOME_ITEM, path: '/manage/home' },
  groups: [
    {
      key: 'users',
      label: '用户管理',
      icon: User,
      items: withPrefix([
        { label: '管理员信息', path: '/admin', icon: Setting },
        { label: '影院信息', path: '/cinema', icon: OfficeBuilding },
        { label: '用户信息', path: '/user', icon: User },
      ], '/manage'),
    },
    {
      key: 'info',
      label: '信息管理',
      icon: Document,
      items: withPrefix([
        { label: '系统公告', path: '/notice', icon: Bell },
        { label: '电影分类', path: '/type', icon: Grid },
        { label: '电影区域', path: '/area', icon: Location },
        { label: '电影信息', path: '/film', icon: VideoCamera },
        { label: '电影预告视频', path: '/video', icon: VideoPlay },
        { label: '演职人员', path: '/actor', icon: User },
        { label: '影厅房间', path: '/room', icon: Tickets },
        { label: '影院名录', path: '/cinemaDirectory', icon: OfficeBuilding },
        { label: '放映记录', path: '/record', icon: Calendar },
        { label: '购票订单', path: '/ordered', icon: CreditCard },
        { label: '用户评价', path: '/mark', icon: Star },
        { label: '个人资料', path: '/person', icon: Avatar },
        { label: '修改密码', path: '/password', icon: Lock },
      ], '/manage'),
    },
  ],
}

/** 影院后台侧边栏（路径为 /back 下的绝对路径） */
export const CINEMA_MENU = {
  title: '电影购票网站 - 影院后台',
  home: '/back/home',
  homeItem: { ...HOME_ITEM, path: '/back/home' },
  groups: [
    {
      key: 'info',
      label: '信息管理',
      icon: Document,
      items: withPrefix([
        { label: '电影信息', path: '/film', icon: VideoCamera },
        { label: '影厅房间', path: '/room', icon: Tickets },
        { label: '放映记录', path: '/record', icon: Calendar },
        { label: '购票订单', path: '/ordered', icon: CreditCard },
      ], '/back'),
    },
    {
      key: 'person',
      label: '个人中心',
      icon: User,
      items: withPrefix([
        { label: '个人资料', path: '/person', icon: Avatar },
        { label: '修改密码', path: '/password', icon: Lock },
      ], '/back'),
    },
  ],
}
