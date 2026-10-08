import { ElMessage } from 'element-plus'
import { getStoredUser } from '@/utils/authStorage'

/**
 * `el-upload` 的请求头。
 *
 * `el-upload` 用自带的 XHR 发请求，不经过 `utils/request.js` 的 axios 拦截器，
 * 因此拿不到那里统一注入的 Authorization —— 不显式带上，`/api/v1/files/upload`
 * 会被 `AuthInterceptor` 判 401（该路径不在 `excludePathPatterns`，
 * 也不在匿名写白名单里）。所有 `el-upload` 都必须绑 `:headers="uploadHeaders"`。
 *
 * 用 getter 而不是普通对象：el-upload 在**发请求时**才 `Object.entries(headers)`，
 * 而令牌要登录后才写入 storage —— 普通对象会在模块加载时就把值固定成空。
 * 未登录时返回 null，el-upload 会跳过该头（`ajax.mjs` 里 `isNil(value)` 判定）。
 */
export const uploadHeaders = {
  get Authorization() {
    const token = getStoredUser()?.token
    return token ? `Bearer ${token}` : null
  }
}

/**
 * `el-upload` 的上传失败反馈。非 2xx 走 `on-error` 而非 `on-success`，
 * 所以 `handleFileUpload` 那类成功回调在失败时根本不会被调用 ——
 * 不接这个事件，上传失败就是完全静默的。
 */
export function handleUploadError(error) {
  if (error?.status === 401) {
    ElMessage.error('登录已过期，请重新登录')
    return
  }
  ElMessage.error(error?.message ? `上传失败：${error.message}` : '上传失败')
}
