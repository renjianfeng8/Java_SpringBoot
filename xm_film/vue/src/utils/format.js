/**
 * 票房格式化。
 * film.box_office 的单位是「万元」（见 xm_film/sql/schema.sql），不是元。
 * 按行业惯例（猫眼/灯塔）：不足 1 亿显示「万」，达到 1 亿显示「亿」。
 */
export function formatBoxOffice(value) {
  const wan = Number(value)
  if (!wan) return '暂无数据'
  return wan >= 10000 ? `${(wan / 10000).toFixed(2)}亿` : `${wan}万`
}
