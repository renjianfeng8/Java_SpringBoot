import { ORDER_STATUS_MAP, CINEMA_STATUS_MAP, FILM_STATUS_MAP } from '@/constants'

export function getOrderStatusType(status) {
  return ORDER_STATUS_MAP[status]?.type || 'info'
}

export function getCinemaStatusType(status) {
  return CINEMA_STATUS_MAP[status]?.type || 'info'
}

export function getFilmStatusType(status) {
  return FILM_STATUS_MAP[status]?.type || 'info'
}
