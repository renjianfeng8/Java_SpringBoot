export const API_PATHS = {
  AUTH: '/api/v1/auth',
  ADMINS: '/api/v1/admins',
  USERS: '/api/v1/users',
  CINEMAS: '/api/v1/cinemas',
  FILMS: '/api/v1/films',
  ACTORS: '/api/v1/actors',
  AREAS: '/api/v1/areas',
  TYPES: '/api/v1/types',
  NOTICES: '/api/v1/notices',
  ROOMS: '/api/v1/rooms',
  RECORDS: '/api/v1/records',
  ORDERS: '/api/v1/orders',
  MARKS: '/api/v1/marks',
  VIDEOS: '/api/v1/videos',
  TICKETS: '/api/v1/tickets',
  RECHARGES: '/api/v1/recharges',
  FUND_FLOWS: '/api/v1/fund-flows',
  STATISTICS: '/api/v1/statistics',
  ACCOUNT: '/api/v1/account',
  FILES: '/api/v1/files/upload',
  YEARS: '/api/v1/auth/years',
}

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:9090'
// Same-origin deploy (VITE_API_BASE_URL=/): use relative path → Nginx proxies /api/ → backend
// Cross-origin dev (default localhost:9090): use absolute URL
export const FILE_UPLOAD_URL = API_BASE_URL === '/'
  ? API_PATHS.FILES
  : `${API_BASE_URL}${API_PATHS.FILES}`

export const apiById = (base, id) => `${base}/${id}`
export const apiPage = (base) => `${base}/page`
export const apiBatch = (base) => `${base}/batch`

export const FILM_API = {
  SEARCH: `${API_PATHS.FILMS}/search`,
  BY_CINEMA: `${API_PATHS.FILMS}/by-cinema`,
  BOX_OFFICE_TOP: `${API_PATHS.FILMS}/box-office/top`,
  // 今日票房：走 /api/v1/films 这个匿名只读前缀，首页游客也能拿到
  BOX_OFFICE_TODAY: `${API_PATHS.FILMS}/box-office/today`,
  MARK_TOP: `${API_PATHS.FILMS}/mark/top`,
}

/** 订单业务接口（非标准 CRUD） */
export const ORDER_API = {
  CREATE: `${API_PATHS.ORDERS}/create`,
  CANCEL: (id) => `${API_PATHS.ORDERS}/${id}/cancel`,
  PAY: (id) => `${API_PATHS.ORDERS}/${id}/pay`,
  PICKUP: (id) => `${API_PATHS.ORDERS}/${id}/pickup`,
  REFUND: (id) => `${API_PATHS.ORDERS}/${id}/refund`,
  SEATS: `${API_PATHS.ORDERS}/seats`,
  // 单笔订单明细（含后端 join 出的影片/影院/影厅名与取票码），支付成功后取凭证用
  DETAIL: (id) => `${API_PATHS.ORDERS}/${id}`,
}

/**
 * 取票大厅：凭取票码核销出票。
 * 这是全站唯一免登录的写接口 —— 自助机不认识用户，码本身就是凭证（见 TicketController）。
 */
export const TICKET_API = {
  REDEEM: `${API_PATHS.TICKETS}/redeem`,
}

export const AUTH_API = {
  LOGIN: `${API_PATHS.AUTH}/login`,
  REGISTER: `${API_PATHS.AUTH}/register`,
  PASSWORD: `${API_PATHS.AUTH}/password`,
  YEARS: `${API_PATHS.AUTH}/years`,
  ME: `${API_PATHS.AUTH}/me`,
}

/** 账户余额（只返回当前登录用户自己的余额） */
export const ACCOUNT_API = {
  SUMMARY: `${API_PATHS.ACCOUNT}/summary`,
}

/** 充值单据：提交申请不改余额，回调成功才入账 */
export const RECHARGE_API = {
  CREATE: API_PATHS.RECHARGES,
  PAGE: apiPage(API_PATHS.RECHARGES),
  CALLBACK: (id) => `${API_PATHS.RECHARGES}/${id}/callback`,
}

/** 资金流水（只读账本） */
export const FUND_FLOW_API = {
  PAGE: apiPage(API_PATHS.FUND_FLOWS),
}

/** 后台可视化大盘的统计接口：数值全部由数据库实时聚合，前端不再拉全表自己算 */
export const STATISTICS_API = {
  OVERVIEW: `${API_PATHS.STATISTICS}/overview`,
}

/** 充值单据状态 → el-tag type，取值与后端 RechargeStatus 一致 */
export const RECHARGE_STATUS_MAP = {
  '处理中': 'warning',
  '已完成': 'success',
  '已失败': 'danger',
}

export function getRechargeStatusType(status) {
  return RECHARGE_STATUS_MAP[status] || 'info'
}

/**
 * 允许物理删除的订单状态，必须与后端 OrderedService.DELETABLE_STATUSES 保持一致。
 * 已成交订单只能走退票，删订单不得成为绕过退票与资金凭证的旁路。
 */
export const ORDER_DELETABLE_STATUSES = ['已取消', '已退票']

export function isOrderDeletable(status) {
  return ORDER_DELETABLE_STATUSES.includes(status)
}

/** 状态 → el-tag 的 type，缺省 info；键序即下拉选项顺序 */
export const ORDER_STATUS_MAP = {
  '待支付': 'warning',
  '待取票': 'success',
  '已取票': 'primary',
  '已退票': 'danger',
  '已取消': 'info',
}

/** 订单状态筛选下拉选项，直接由 ORDER_STATUS_MAP 派生，保证筛选项与状态色始终对齐 */
export const ORDER_STATUS_OPTIONS = Object.keys(ORDER_STATUS_MAP)

/** 影院审核状态取值，与后端 CinemaStatus 枚举、数据库词表保持一致 */
export const CINEMA_STATUS = {
  UNAUDITED: '未审核',
  APPROVED: '已审核',
}

/** 影院审核状态 → el-tag type；键序即下拉选项顺序 */
export const CINEMA_STATUS_MAP = {
  [CINEMA_STATUS.UNAUDITED]: 'warning',
  [CINEMA_STATUS.APPROVED]: 'success',
}

export const CINEMA_STATUS_OPTIONS = Object.keys(CINEMA_STATUS_MAP)

export function getCinemaStatusType(status) {
  return CINEMA_STATUS_MAP[status] || 'info'
}

export const FILM_STATUS_MAP = {
  '待上映': 'warning',
  '已上映': 'success',
  '停止上映': 'danger',
}

/** 放映场次的售卖状态；未开始/放映中/已结束由 start 派生，不落库 */
export const RECORD_STATUS_MAP = {
  '正常': 'success',
  '停售': 'info',
}

export function getOrderStatusType(status) {
  return ORDER_STATUS_MAP[status] || 'info'
}

export function getFilmStatusType(status) {
  return FILM_STATUS_MAP[status] || 'info'
}

export function getRecordStatusType(status) {
  return RECORD_STATUS_MAP[status] || 'info'
}
