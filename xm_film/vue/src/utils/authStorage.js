export const USER_KEY = 'xm-film-user'

/**
 * 会话变化事件：storage 是唯一持久化来源，但 useAuth 还持有一份模块级 ref。
 * 任何绕过 useAuth 的写入/清理（request.js 的 401、跨标签页）都要通知它同步，
 * 否则守卫已经按新 storage 判了，页面还挂着旧用户。
 */
export const AUTH_CHANGE_EVENT = 'xm-film-auth-change'

function notify(user) {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent(AUTH_CHANGE_EVENT, { detail: user }))
  }
}

export function getStoredUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    clearStoredUser()
    return null
  }
}

export function setStoredUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
  notify(user)
}

export function clearStoredUser() {
  localStorage.removeItem(USER_KEY)
  notify(null)
}
