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
export const FILE_UPLOAD_URL = `${API_BASE_URL}${API_PATHS.FILES}`

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
}

export const TYPE_MAP = {
  1: '纪录', 4: '恐怖', 5: '喜剧', 6: '动漫',
  7: '伦理', 13: '爱情', 14: '动作', 15: '灾难',
  16: '体育', 17: '悬疑', 18: '历史', 19: '犯罪',
  20: '科幻', 21: '冒险', 22: '剧情', 23: '家庭',
}

export const AREA_MAP = {
  1: '中国大陆', 2: '美国', 3: '日本', 4: '中国香港',
  5: '韩国', 6: '泰国', 7: '法国', 8: '英国', 9: '德国',
  10: '印度', 11: '俄罗斯', 12: '意大利', 13: '西班牙',
  14: '波兰', 15: '中国台湾', 16: '葡萄牙',
}

export const ORDER_STATUS_MAP = {
  '待支付': { type: 'warning', label: '待支付' },
  '待取票': { type: 'success', label: '待取票' },
  '已取票': { type: 'primary', label: '已取票' },
  '已取消': { type: 'info', label: '已取消' },
}

export function getOrderStatusType(status) {
  return ORDER_STATUS_MAP[status]?.type || 'info'
}

export const CINEMA_STATUS_MAP = {
  '已审批': { type: 'success', label: '已审批' },
  '未审核': { type: 'warning', label: '未审核' },
  '不通过': { type: 'danger', label: '不通过' },
}

export const FILM_STATUS_MAP = {
  '已上映': { type: 'success', label: '已上映' },
  '待上映': { type: 'warning', label: '待上映' },
}
