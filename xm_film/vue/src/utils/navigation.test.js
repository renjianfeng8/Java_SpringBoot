import { describe, expect, it } from 'vitest'
import {
  DEFAULT_HOME,
  LOGIN_PROMPT_MESSAGE,
  SITE_NAME,
  isInternalPath,
  isPathWithin,
  loginPath,
  pathForRole,
} from '@/utils/navigation'

describe('navigation', () => {
  it('maps each known role to its home, unknown roles fall back to USER home', () => {
    expect(pathForRole('ADMIN')).toBe('/manage/home')
    expect(pathForRole('CINEMA')).toBe('/back/home')
    expect(pathForRole('USER')).toBe(DEFAULT_HOME)
    expect(pathForRole('')).toBe(DEFAULT_HOME)
    expect(pathForRole(undefined)).toBe(DEFAULT_HOME)
  })

  it('only accepts internal absolute paths', () => {
    expect(isInternalPath('/front/home')).toBe(true)
    expect(isInternalPath('/login?redirect=%2Ffront')).toBe(true)
    expect(isInternalPath('//evil.com')).toBe(false)
    expect(isInternalPath('https://evil.com')).toBe(false)
    expect(isInternalPath('front/home')).toBe(false)
    expect(isInternalPath(null)).toBe(false)
    expect(isInternalPath(123)).toBe(false)
  })

  it('encodes redirect targets and refuses open redirects', () => {
    expect(loginPath('/front/orders?page=2')).toBe('/login?redirect=%2Ffront%2Forders%3Fpage%3D2')
    expect(loginPath('//evil.com')).toBe('/login')
    expect(loginPath('https://evil.com')).toBe('/login')
  })

  it('matches path segments, not raw string prefixes', () => {
    expect(isPathWithin('/front', '/front')).toBe(true)
    expect(isPathWithin('/front/orders', '/front')).toBe(true)
    expect(isPathWithin('/frontx', '/front')).toBe(false)
    expect(isPathWithin('/frontend/orders', '/front')).toBe(false)
    expect(isPathWithin('/login', '/front')).toBe(false)
    expect(isPathWithin(null, '/front')).toBe(false)
  })

  it('keeps the public constants stable', () => {
    expect(SITE_NAME).toBe('电影购票网站')
    expect(LOGIN_PROMPT_MESSAGE).toBe('请先登录后再进行此操作')
  })
})
