export const USER_KEY = 'xm-pro-user'

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
}

export function clearStoredUser() {
  localStorage.removeItem(USER_KEY)
}
