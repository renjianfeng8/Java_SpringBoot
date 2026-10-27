import { describe, expect, it } from 'vitest'
import {
  ACTOR_ROLE_TYPE_MAP,
  API_PATHS,
  CINEMA_STATUS,
  FILM_STATUS_OPTIONS,
  ORDER_STATUS_OPTIONS,
  RECHARGE_STATUS_OPTIONS,
  ROLE_OPTIONS,
  SUCCESS_CODE,
  apiBatch,
  apiById,
  apiPage,
  getActorRoleType,
  getCinemaStatusType,
  getFilmStatusType,
  getOrderStatusType,
  getRechargeStatusType,
  getRecordStatusType,
  getRoleType,
  isOrderDeletable,
  isSuccess,
} from '@/constants'

describe('constants', () => {
  it('recognises only the string success code', () => {
    expect(SUCCESS_CODE).toBe('200')
    expect(isSuccess({ code: '200' })).toBe(true)
    expect(isSuccess({ code: 200 })).toBe(false)
    expect(isSuccess({ code: '500' })).toBe(false)
    expect(isSuccess({ code: '' })).toBe(false)
    expect(isSuccess(null)).toBe(false)
    expect(isSuccess(undefined)).toBe(false)
  })

  it('builds the three REST helpers without duplicate slashes', () => {
    expect(apiPage(API_PATHS.FILMS)).toBe('/api/v1/films/page')
    expect(apiById(API_PATHS.FILMS, 7)).toBe('/api/v1/films/7')
    expect(apiBatch(API_PATHS.FILMS)).toBe('/api/v1/films/batch')
  })

  it('falls back to info for unknown tag values', () => {
    expect(getRechargeStatusType('已完成')).toBe('success')
    expect(getRechargeStatusType('未知')).toBe('info')
    expect(getOrderStatusType('待支付')).toBe('warning')
    expect(getOrderStatusType('未知')).toBe('info')
    expect(getFilmStatusType('已上映')).toBe('success')
    expect(getFilmStatusType('未知')).toBe('info')
    expect(getRecordStatusType('停售')).toBe('info')
    expect(getRecordStatusType('未知')).toBe('info')
    expect(getCinemaStatusType(CINEMA_STATUS.APPROVED)).toBe('success')
    expect(getCinemaStatusType('未知')).toBe('info')
    expect(getRoleType('ADMIN')).toBe('warning')
    expect(getRoleType('UNKNOWN')).toBe('success')
  })

  it('derives filter options from the same maps as the tag colours', () => {
    expect(ORDER_STATUS_OPTIONS).toEqual(['待支付', '待取票', '已取票', '已退票', '已取消'])
    expect(RECHARGE_STATUS_OPTIONS).toEqual(['处理中', '已完成', '已失败'])
    expect(FILM_STATUS_OPTIONS).toEqual(['已上映', '待上映', '停止上映'])
    expect(ROLE_OPTIONS).toEqual(['ADMIN', 'CINEMA', 'USER'])
  })

  it('only allows physically deleting cancelled or refunded orders', () => {
    expect(isOrderDeletable('已取消')).toBe(true)
    expect(isOrderDeletable('已退票')).toBe(true)
    expect(isOrderDeletable('待支付')).toBe(false)
    expect(isOrderDeletable('已取票')).toBe(false)
    expect(isOrderDeletable(undefined)).toBe(false)
  })

  it('maps actor grades to detail groups, defaulting to lead actor', () => {
    expect(getActorRoleType('导演')).toBe(1)
    expect(getActorRoleType('主演')).toBe(2)
    expect(getActorRoleType('编剧')).toBe(3)
    expect(getActorRoleType('二级演员')).toBe(4)
    expect(getActorRoleType('未知')).toBe(2)
    expect(Object.keys(ACTOR_ROLE_TYPE_MAP)).toHaveLength(4)
  })
})
