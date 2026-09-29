/**
 * 票房格式化。
 * 数值来源是后端按 ordered 实时聚合的「本系统累计售票收入」，单位是元（见 FilmMapper.xml 的 filmRevenueJoin）。
 * 不再使用 film.box_office 静态列，也不再用万/亿口径 —— 本系统内的售票收入是几十到几百元量级，
 * 按万元渲染会恒显示 0.00万。
 */
export function formatBoxOffice(value) {
  const yuan = Number(value)
  if (!yuan) return '暂无数据'
  return `${yuan.toFixed(2)}元`
}

/**
 * 区间聚合金额格式化（今日票房）。
 * 与 formatBoxOffice 的唯一差别：0 是真实值而不是缺失值 —— 「今天还没卖出票」这个结论
 * 本身就是数据，空集上的 SUM 就是 0，渲染「暂无数据」会让用户以为取不到数。
 * 「取不到数据」由调用方的错误态负责，不走这里。
 */
export function formatYuan(value) {
  return `${Number(value || 0).toFixed(2)}元`
}

/**
 * 影片评分格式化。
 * 数值来源是 film.score —— 该片真实用户评价（mark.score）的均分，由后端 MarkService
 * 在评价增删改后回写；没有任何评价时该列是 NULL，表示"这片还没人评过"，渲染「暂无评分」。
 * 刻意不用 falsy 判断：0.0 是合法的真实评分（mark.score 允许 0~10），只有 null / undefined
 * 才代表没有评分 —— 与 formatBoxOffice 把 0 当缺失值正好相反，两者不可混用。
 */
export function formatScore(value) {
  if (value === null || value === undefined || value === '') return '暂无评分'
  return `${Number(value).toFixed(1)} 分`
}
