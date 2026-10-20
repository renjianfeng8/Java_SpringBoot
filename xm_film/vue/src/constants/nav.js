import { API_PATHS } from '@/constants'
import { isPathWithin } from '@/utils/navigation'

/**
 * 前台顶部导航的数据源（FE-59）。
 *
 * 绑定路由 `meta.nav`：路由表是唯一出处，导航项不再手工维护第二份。
 *   - `meta.nav` 为对象时，该项进导航，`label` 是文案、`sections` 是高亮归属段
 *     （详情/周边页要与列表页同时点亮时，在 `sections` 里列出这些前缀）
 *   - 未声明 `meta.nav` 的前台路由不进导航（如个人中心、修改密码，只从头像下拉进入）
 *
 * 断言：导航只从路由表派生，新增前台路由要么声明 `meta.nav`，要么显式不进导航，
 * 不存在「加路由忘加导航项」的第二份手工清单。
 */
export function navItemsOf(router) {
  const items = []

  router.getRoutes().forEach((record) => {
    const nav = record.meta?.nav
    if (!nav || !record.path) return
    items.push({
      path: record.path,
      label: nav.label,
      sections: nav.sections || [record.path],
      // 只有 USER 能进的项：与路由 meta.roles 对齐，游客不渲染
      requiresUser: nav.requiresUser ?? (record.meta?.roles?.includes('USER') && !record.meta?.guest),
    })
  })

  return items
}

/** 当前路径是否落在该导航项的某个 section 内（相等或 prefix + '/'，不做裸 startsWith） */
export function isNavActive(item, currentPath) {
  return item.sections.some((prefix) => isPathWithin(currentPath, prefix))
}

export { API_PATHS }
