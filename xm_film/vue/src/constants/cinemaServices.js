/**
 * 影院服务三项的唯一出处（FE-57）。
 *
 * 同一组服务出现在三个页面，只是渲染形状不同：
 *   - 影院列表页（Cinema.vue）  : 带图标的小标签，纯文本
 *   - 影院详情页（CinemaDetail）: 带图标的标题 + 一句说明
 *   - 选影院页（FilmCinema.vue）: 纯文本标签，无图标
 * 共享的是「有哪些服务、叫什么、什么底色、说明是什么」，形状由各页自行渲染。
 *
 * `tone` 对应 front-pages.scss 的 `.service-tag--*` 底色类：
 * 底色在此共享、文案也在此共享，避免「同一底色挂两个不同名字」。
 */
import { Connection, RefreshLeft, User } from '@element-plus/icons-vue'

export const CINEMA_SERVICES = [
  {
    key: 'refund',
    tone: 'refund',
    label: '退票无忧',
    icon: RefreshLeft,
    desc: '未取票用户在放映前 60 分钟可退票',
  },
  {
    key: 'promo',
    tone: 'promo',
    label: '儿童优惠',
    icon: User,
    desc: '1 位成人可免费带 1 位不满 1.3 米儿童，免票无座',
  },
  {
    key: 'wifi',
    tone: 'wifi',
    label: 'WiFi覆盖',
    icon: Connection,
    desc: '全场免费高速 WiFi，观影期间也可顺畅连接',
  },
]

/** 底色类名：与 front-pages.scss 的 .service-tag--{tone} 一一对应 */
export const serviceToneClass = (service) => `service-tag--${service.tone}`
