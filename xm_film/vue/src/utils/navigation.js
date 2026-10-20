/**
 * 站内导航的唯一口径：角色主页、登录回跳、路径边界匹配。
 *
 * 这些规则此前散在 router、Login、404、request、FilmMarks 与两个后台外壳里，
 * 改一处忘一处就会出现「登录后落错主页」「退出后回跳丢参数」「/loginx 被当成登录页」。
 */

export const SITE_NAME = '电影购票网站'
export const DEFAULT_HOME = '/front/home'

const ROLE_HOME = Object.freeze({
  USER: '/front/home',
  CINEMA: '/back/home',
  ADMIN: '/manage/home',
})

/** 角色对应的默认主页；未知/空角色按普通用户处理 */
export function pathForRole(role) {
  return ROLE_HOME[role] || DEFAULT_HOME
}

/** 只接受站内绝对路径；'//evil.com' 是协议相对 URL，必须排除 */
export function isInternalPath(path) {
  return typeof path === 'string' && path.startsWith('/') && !path.startsWith('//')
}

/** 登录页地址；来源不是站内路径时不带 redirect，避免开放重定向 */
export function loginPath(fromPath) {
  return isInternalPath(fromPath)
    ? `/login?redirect=${encodeURIComponent(fromPath)}`
    : '/login'
}

/** 路径是否落在某个前缀段内（相等，或 prefix + '/' 开头），不做裸 startsWith */
export function isPathWithin(path, prefix) {
  return typeof path === 'string'
    && (path === prefix || path.startsWith(`${prefix}/`))
}

export const LOGIN_PROMPT_MESSAGE = '请先登录后再进行此操作'
