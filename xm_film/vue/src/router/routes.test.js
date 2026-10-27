import { describe, expect, it } from 'vitest'
import { routes } from './routes'

/**
 * 路由门禁守卫（FE-05）。
 *
 * 新增一条路由忘了标门禁，不会报错、不会白屏，只会让页面裸奔——而守卫层的判定是
 * 「requiresAuth 或 roles 才拦」，所以漏标的后果是**静默公开**。这里把「每条路由都要
 * 显式表态」变成可执行断言：要么 requiresAuth/roles，要么 guest，要么 redirect。
 */

const ROLES = ['ADMIN', 'CINEMA', 'USER']

// Vue Router 会把父路由的 meta 合并进子路由；这里按同样规则拍平，否则
// 「门禁标在父路由上」的子路由会被误判成裸奔。
const flatten = (list, parentPath = '', inheritedMeta = {}) => list.flatMap((route) => {
  const fullPath = route.path.startsWith('/') ? route.path : `${parentPath}/${route.path}`
  const meta = { ...inheritedMeta, ...route.meta }
  return [{ ...route, meta, fullPath }, ...(route.children ? flatten(route.children, fullPath, meta) : [])]
})

const flat = flatten(routes)

const declaresGate = (route) => Boolean(route.meta?.requiresAuth || route.meta?.roles?.length)
const declaresPublic = (route) => route.meta?.guest === true || Boolean(route.redirect)

describe('路由门禁标记', () => {
  it('每条路由都显式表态：受保护或公开', () => {
    // 容器路由（带 children 的 /front/manage/back）不自己表态，由子路由逐条表态
    const undecided = flat
      .filter((route) => route.path !== '' && !route.redirect && !route.children)
      .filter((route) => !declaresGate(route) && !declaresPublic(route))
      .map((route) => route.fullPath)
    expect(undecided).toEqual([])
  })

  it('前台逐子标门禁，后台在父路由上标', () => {
    const front = flat.filter((route) => route.fullPath.startsWith('/front/'))
    expect(front.length).toBeGreaterThan(0)
    expect(front.filter((route) => !declaresGate(route) && !declaresPublic(route))).toEqual([])

    for (const prefix of ['/manage', '/back']) {
      const parent = routes.find((route) => route.path === prefix)
      expect(parent, prefix).toBeTruthy()
      expect(declaresGate(parent), prefix).toBe(true)
    }
  })

  it('没有自相矛盾的标记', () => {
    const contradictory = flat
      .filter((route) => route.meta?.guest === true && declaresGate(route))
      .map((route) => route.fullPath)
    expect(contradictory).toEqual([])
  })

  it('roles 只含已知角色且非空数组', () => {
    const invalid = flat
      .filter((route) => route.meta?.roles)
      .filter((route) => !Array.isArray(route.meta.roles)
        || route.meta.roles.length === 0
        || route.meta.roles.some((role) => !ROLES.includes(role)))
      .map((route) => `${route.fullPath}: ${JSON.stringify(route.meta.roles)}`)
    expect(invalid).toEqual([])
  })
})
