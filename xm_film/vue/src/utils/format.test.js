import { describe, expect, it } from 'vitest'
import { formatBoxOffice, formatFilmTypes, formatScore, formatScoreBadge, formatYuan } from '@/utils/format'
import {
  DEFAULT_AVATAR,
  DEFAULT_CINEMA_COVER,
  avatarSrc,
  cinemaCoverSrc,
  onAvatarError,
} from '@/utils/avatar'

describe('format', () => {
  it('treats zero box office as missing, but zero revenue as real data', () => {
    expect(formatBoxOffice(0)).toBe('暂无数据')
    expect(formatBoxOffice(null)).toBe('暂无数据')
    expect(formatBoxOffice(12.345)).toBe('12.35元')
    expect(formatYuan(0)).toBe('0.00元')
    expect(formatYuan(undefined)).toBe('0.00元')
    expect(formatYuan(8)).toBe('8.00元')
  })

  it('distinguishes no score from a real 0.0 score', () => {
    expect(formatScore(null)).toBe('暂无评分')
    expect(formatScore('')).toBe('暂无评分')
    expect(formatScore(0)).toBe('0.0 分')
    expect(formatScore(9.46)).toBe('9.5 分')
    expect(formatScoreBadge(null)).toBe('')
    expect(formatScoreBadge(0)).toBe('0.0')
    expect(formatScoreBadge(8.44)).toBe('8.4')
  })

  it('joins derived type titles and falls back when absent', () => {
    expect(formatFilmTypes({ typeList: [{ title: '科幻' }, { title: '动作' }] })).toBe('科幻 / 动作')
    expect(formatFilmTypes({ typeList: [] })).toBe('未知类型')
    expect(formatFilmTypes({})).toBe('未知类型')
    expect(formatFilmTypes(null)).toBe('未知类型')
  })
})

describe('avatar', () => {
  it('falls back to the right default per resource', () => {
    expect(avatarSrc('')).toBe(DEFAULT_AVATAR)
    expect(avatarSrc('/files/a.jpg')).toBe('/files/a.jpg')
    expect(cinemaCoverSrc(null)).toBe(DEFAULT_CINEMA_COVER)
    expect(cinemaCoverSrc('/files/b.jpg')).toBe('/files/b.jpg')
    expect(DEFAULT_AVATAR).not.toBe(DEFAULT_CINEMA_COVER)
  })

  it('replaces a broken image once and stops at the default', () => {
    const img = {
      src: '/files/broken.jpg',
      getAttribute: () => '/files/broken.jpg',
    }

    onAvatarError({ target: img })
    expect(img.src).toBe(DEFAULT_AVATAR)

    // 默认图自身再失败时不再改写，避免死循环
    img.getAttribute = () => DEFAULT_AVATAR
    onAvatarError({ target: img })
    expect(img.src).toBe(DEFAULT_AVATAR)
  })

  it('ignores events without a target', () => {
    expect(() => onAvatarError(undefined)).not.toThrow()
  })
})
