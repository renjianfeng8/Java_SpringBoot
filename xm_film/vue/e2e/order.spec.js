import { expect, test } from '@playwright/test'
import { API_BASE, tokenOf, uiLogin } from './helpers'

const FORMATTED = /^(.+)，可选$/

/**
 * 这一条钉住的是本轮的 P1：取消订单后座位不释放。
 * 根因是 mapper 方法漏 @Param("s")，状态条件恒真，占用列表把已取消的座位也算进去。
 * 全程不付款，所以只动余额为 0 的路径，收尾把废单删掉。
 */
test('下单→取消→座位重新可选，收尾删除废单', async ({ page, request }) => {
  await uiLogin(page, { username: 'u1', password: 'u1' })
  const token = await tokenOf(page)
  const auth = { Authorization: `Bearer ${token}` }

  const listed = await (
    await request.get(`${API_BASE}/records/page?pageNum=1&pageSize=50`, { headers: auth })
  ).json()
  const candidates = (listed.data?.list || [])
    .filter((record) => record.status === '正常' && new Date(record.start).getTime() > Date.now())
    .slice(0, 5)
  expect(candidates.length, '开发库里没有可购买的排片，请先跑 demo-data.sql').toBeGreaterThan(0)

  let created = null
  let seatLabel = null

  try {
    for (const record of candidates) {
      await page.goto(
        `/front/buyTicket?cinemaId=${record.cinemaId}&filmId=${record.filmId}`
        + `&recordId=${record.id}&roomId=${record.roomId}`
      )
      const free = page.locator('button[aria-label$="，可选"]').first()
      const appeared = await free.waitFor({ state: 'visible', timeout: 8_000 }).then(() => true).catch(() => false)
      if (!appeared) continue

      seatLabel = (await free.getAttribute('aria-label')).match(FORMATTED)[1]
      await free.click()

      const created$ = page.waitForResponse(
        (response) => response.url().includes('/orders/create') && response.request().method() === 'POST'
      )
      await page.getByRole('button', { name: /确认购票/ }).click()
      const body = await (await created$).json()
      expect(body.code, body.msg).toBe('200')
      created = body.data
      break
    }

    expect(created, '没找到有空位的场次').not.toBeNull()
    expect(created.id).toBeTruthy()

    await page.getByRole('button', { name: '取消订单' }).click()
    const confirm = page.locator('.el-message-box')
    await confirm.waitFor({ state: 'visible', timeout: 2_000 }).catch(() => {})
    if (await confirm.isVisible().catch(() => false)) {
      await confirm.getByRole('button', { name: /确\s*定/ }).click()
    }

    await page.reload()
    await expect(page.locator(`button[aria-label="${seatLabel}，可选"]`)).toBeVisible({ timeout: 15_000 })
  } finally {
    if (created?.id) {
      const deleted = await request.delete(`${API_BASE}/orders/${created.id}`, { headers: auth })
      expect((await deleted.json()).code).toBe('200')
    }
  }
})
