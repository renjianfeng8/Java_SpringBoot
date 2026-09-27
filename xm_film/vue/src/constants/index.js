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
  MARK_TOP: `${API_PATHS.FILMS}/mark/top`,
}

/** 订单业务接口（非标准 CRUD） */
export const ORDER_API = {
  CREATE: `${API_PATHS.ORDERS}/create`,
  CANCEL: (id) => `${API_PATHS.ORDERS}/${id}/cancel`,
  PAY: (id) => `${API_PATHS.ORDERS}/${id}/pay`,
}

export const AUTH_API = {
  LOGIN: `${API_PATHS.AUTH}/login`,
  REGISTER: `${API_PATHS.AUTH}/register`,
  PASSWORD: `${API_PATHS.AUTH}/password`,
  YEARS: `${API_PATHS.AUTH}/years`,
  ME: `${API_PATHS.AUTH}/me`,
}

/** 状态 → el-tag 的 type，缺省 info */
export const ORDER_STATUS_MAP = {
  '待支付': 'warning',
  '待取票': 'success',
  '已取票': 'primary',
  '已取消': 'info',
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
