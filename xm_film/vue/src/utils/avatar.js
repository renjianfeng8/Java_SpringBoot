/**
 * 头像兜底（全站唯一口径）。
 *
 * `avatar` 列存的是后端 `/files/<uuid>.jpg` 相对路径，从没上传过就是空。
 * 「空」与「传过但 /files 里的文件已被删（加载失败）」在页面上是同一种观感（破图），
 * 所以两者一律落到 default_avatar.jpg：取地址走 {@link avatarSrc}，破图走 {@link onAvatarError}
 * 或各组件自带的错误插槽（见下方说明）。
 *
 * 图片走 import 而非放 public/：默认头像属于随前端一起发版的静态资产，
 * 交给 vite 打包能拿到带内容哈希的产物路径，也就不必依赖后端 /files 目录里恰好有这份文件。
 */
import defaultAvatar from '@/assets/imgs/default_avatar.jpg'

export const DEFAULT_AVATAR = defaultAvatar

/** 取头像地址：空值一律给默认头像。非空原样返回，破图与否交给加载失败那条路径。 */
export function avatarSrc(value) {
  return value || DEFAULT_AVATAR
}

/**
 * 原生 `<img>` 的 `@error` 处理：加载失败就换成默认头像。
 *
 * 用 dataset 打标做幂等 —— 万一默认头像自身也 404，不打标会无限重试。
 * 只适用于原生 `<img>`（元素不会被卸载，改 src 即生效）；`el-image` / `el-avatar`
 * 加载失败时会卸载内部 img 并改走插槽，故它们不用这个函数，而用各自的
 * `#error` / 默认插槽渲染 `<img :src="DEFAULT_AVATAR">`。
 */
export function onAvatarError(event) {
  const img = event?.target
  if (!img || img.dataset.avatarFallback) return
  img.dataset.avatarFallback = '1'
  img.src = DEFAULT_AVATAR
}
