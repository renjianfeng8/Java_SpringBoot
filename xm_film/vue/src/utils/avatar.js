/**
 * 图片兜底（全站唯一口径）。
 *
 * 两类资源各有自己的默认图，别混用：
 *   - 用户/管理员/影院账号头像 → default_avatar.jpg（人像剪影）
 *   - 影院封面（`cinema.avatar`）→ default_cinema.jpg（幕布 + 座椅剪影）
 *
 * `avatar` 列存的是后端 `/files/<uuid>.jpg` 相对路径，从没上传过就是空。
 * 「空」与「传过但 /files 里的文件已被删（加载失败）」在页面上是同一种观感（破图），
 * 所以两者一律落到默认图：取地址走 {@link avatarSrc} / {@link cinemaCoverSrc}，
 * 破图走 {@link onAvatarError} / {@link onCinemaCoverError}
 * 或各组件自带的错误插槽（见下方说明）。
 *
 * 图片走 import 而非放 public/：默认图属于随前端一起发版的静态资产，
 * 交给 vite 打包能拿到带内容哈希的产物路径，也就不必依赖后端 /files 目录里恰好有这份文件。
 */
import defaultAvatar from '@/assets/imgs/default_avatar.jpg'
import defaultCinema from '@/assets/imgs/default_cinema.jpg'

export const DEFAULT_AVATAR = defaultAvatar

export const DEFAULT_CINEMA_COVER = defaultCinema

/** 取头像地址：空值一律给默认头像。非空原样返回，破图与否交给加载失败那条路径。 */
export function avatarSrc(value) {
  return value || DEFAULT_AVATAR
}

/** 取影院封面地址：空值给影院默认图（不是头像） */
export function cinemaCoverSrc(value) {
  return value || DEFAULT_CINEMA_COVER
}

/**
 * 原生 `<img>` 的 `@error` 处理：加载失败就换成默认图。
 *
 * 判据是「当前 src 是不是已经是默认图」，而不是「这张图兜过一次没有」——
 * 后者会把标记留在元素上，而同一张 `<img>` 会被反复复用（顶栏换人登录、
 * 列表 `:key="index"` 换页），下一个坏图就再也兜不住了。
 * 也不能拿 `img.src` 比对：那是浏览器解析后的绝对 URL，与 import 出来的相对
 * 产物路径永远不相等；`getAttribute` 拿到的才是 Vue 原样写进去的那个值。
 * 换成默认图后若默认图自身也 404，这里会因为 src 已等于默认图而直接返回，不会死循环。
 *
 * 只适用于原生 `<img>`（元素不会被卸载，改 src 即生效）；`el-image` / `el-avatar`
 * 加载失败时会卸载内部 img 并改走插槽，故它们不用这个函数，而用各自的
 * `#error` / 默认插槽渲染 `<img :src="DEFAULT_AVATAR">`。
 */
export function onAvatarError(event) {
  fallbackTo(event, DEFAULT_AVATAR)
}

/** 影院封面版：加载失败换成影院默认图 */
export function onCinemaCoverError(event) {
  fallbackTo(event, DEFAULT_CINEMA_COVER)
}

function fallbackTo(event, fallback) {
  const img = event?.target
  if (!img || img.getAttribute('src') === fallback) return
  img.src = fallback
}
