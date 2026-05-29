export function formatBoxOffice(value) {
  if (!value && value !== 0) return '0'
  return Number(value).toLocaleString('zh-CN')
}

export function formatTime(minutes) {
  if (!minutes && minutes !== 0) return ''
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return h > 0 ? `${h}h${m}min` : `${m}min`
}

export function formatDate(dateStr) {
  if (!dateStr) return ''
  return dateStr.substring(0, 10)
}
