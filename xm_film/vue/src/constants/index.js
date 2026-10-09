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
  TMDB: '/api/v1/tmdb',
}

/**
 * API 基础路径：`/` = 同源（请求发往页面所在的源），其余为后端绝对地址。
 * 开发环境由 `.env.development` 给出 http://localhost:9090 —— vite dev server 只代理 /files，不代理 /api。
 *
 * 回退值必须是 `/`：`.env` 不入库，全新克隆拿不到该变量，
 * 若回退成 http://localhost:9090，上传与文件地址会指向使用者本机。
 * 本变量是全仓唯一读取点，`utils/request.js` 也从这里取。
 */
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/'

/** 文件上传端点（el-upload 的 action）：同源走相对路径，跨域开发走绝对地址 */
export const FILE_UPLOAD_URL = API_BASE_URL === '/'
  ? API_PATHS.FILES
  : `${API_BASE_URL}${API_PATHS.FILES}`

export const apiById = (base, id) => `${base}/${id}`
export const apiPage = (base) => `${base}/page`
export const apiBatch = (base) => `${base}/batch`

/**
 * TMDB 导入接口（后台影片页专用）。
 * 全部在 /api/v1/tmdb 前缀下，后端属 admin-only：详情那次会建类型/地区/演职人员行并下载图片，
 * 补预告片那次会改写已有影片的 video 列。
 * 这里没有「导入影片」的写入端点 —— 拿到预填值后仍走 FILMS 的保存，建影片只有一条路径。
 */
export const TMDB_API = {
  SEARCH: `${API_PATHS.TMDB}/search`,
  // 路径中间多一段 /movie，不能用 apiById（它只拼 `${base}/${id}`）——
  // 用它拼出来是 /api/v1/tmdb/550，后端映射的是 /api/v1/tmdb/movie/550，会 404。
  MOVIE: (tmdbId) => `${API_PATHS.TMDB}/movie/${tmdbId}`,
  // 给 video 为空的历史影片补预告片（POST，幂等）
  BACKFILL_VIDEOS: `${API_PATHS.TMDB}/backfill-videos`,
}

export const FILM_API = {
  SEARCH: `${API_PATHS.FILMS}/search`,
  BY_CINEMA: `${API_PATHS.FILMS}/by-cinema`,
  BOX_OFFICE_TOP: `${API_PATHS.FILMS}/box-office/top`,
  // 今日票房：走 /api/v1/films 这个匿名只读前缀，首页游客也能拿到
  BOX_OFFICE_TODAY: `${API_PATHS.FILMS}/box-office/today`,
  MARK_TOP: `${API_PATHS.FILMS}/mark/top`,
}

/**
 * 影评业务接口。
 * BY_FILM 是「某片全部评价」的唯一入口：列表按赞数降序 → id 降序，
 * 影片详情页的「热评」只是它的前 3 条，不存在第二条排序或第二个端点。
 * liked / mine 由后端按访问者算好（响应刻意不带 userId），前端不推导归属。
 */
export const MARK_API = {
  BY_FILM: `${API_PATHS.MARKS}/by-film`,
  LIKE: (id) => `${API_PATHS.MARKS}/${id}/like`,
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
  ME: `${API_PATHS.AUTH}/me`,
}

/** 账户余额（只返回当前登录用户自己的余额） */
export const ACCOUNT_API = {
  SUMMARY: `${API_PATHS.ACCOUNT}/summary`,
  // 支付密码的两个写入口：CHANGE 验原支付密码，RESET 验登录密码（首次设置与忘记重设同一路径）
  PAY_PASSWORD: `${API_PATHS.ACCOUNT}/pay-password`,
  PAY_PASSWORD_RESET: `${API_PATHS.ACCOUNT}/pay-password/reset`,
  // 两个只验不写的入口，供设置页第一步的「验证身份」用（写仍走上面对应的那个端点）
  VERIFY_OLD_PASSWORD: `${API_PATHS.ACCOUNT}/pay-password/verify-old`,
  VERIFY_LOGIN_PASSWORD: `${API_PATHS.ACCOUNT}/pay-password/verify-login`,
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

/** 影片状态 → el-tag type；键序即前台筛选项顺序（tag 取色按 key 查，与顺序无关） */
export const FILM_STATUS_MAP = {
  '已上映': 'success',
  '待上映': 'warning',
  '停止上映': 'danger',
}

/** 影片状态筛选项，直接由 FILM_STATUS_MAP 派生，保证筛选项与状态色始终对齐 */
export const FILM_STATUS_OPTIONS = Object.keys(FILM_STATUS_MAP)

/** 角色 → el-tag type，取值与后端 RoleEnum 一致。缺省 success 与原内联三元式的兜底分支保持一致 */
export const ROLE_TAG_MAP = {
  'ADMIN': 'warning',
  'CINEMA': 'danger',
  'USER': 'success',
}

export function getRoleType(role) {
  return ROLE_TAG_MAP[role] || 'success'
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
