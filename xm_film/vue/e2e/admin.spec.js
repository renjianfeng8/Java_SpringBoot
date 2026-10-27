import { expect, test } from '@playwright/test'
import { API_BASE, tokenOf, uiLogin } from './helpers'

/**
 * 这一条钉住的是本轮的 P1：管理端影院列表曾经忽略 status 参数，
 * 首页「待审核 1」点进去列出全部 87 家。
 */
test('影院列表按「未审核」筛选后只剩未审核', async ({ page }) => {
  await uiLogin(page, { username: '999', password: '999', role: '管理员' })

  const statuses = []
  page.on('response', async (response) => {
    if (!response.url().includes('/cinemas/page') || !response.url().includes('status=')) return
    const body = await response.json().catch(() => null)
    if (body?.code === '200') statuses.push(...body.data.list.map((row) => row.status))
  })

  await page.goto(`/manage/cinema?status=${encodeURIComponent('未审核')}`)
  await expect.poll(() => statuses.length, { timeout: 15_000 }).toBeGreaterThan(0)

  expect(statuses.every((status) => status === '未审核')).toBe(true)
  await expect(page.locator('.el-table__body tr').first()).toBeVisible()
})

test('重复分类被拒，提示可操作', async ({ page, request }) => {
  await uiLogin(page, { username: '999', password: '999', role: '管理员' })

  const token = await tokenOf(page)
  const types = await (
    await request.get(`${API_BASE}/types/page?pageNum=1&pageSize=1`, {
      headers: { Authorization: `Bearer ${token}` },
    })
  ).json()
  const existing = types.data?.list?.[0]?.title
  test.skip(!existing, '库里没有分类，无法验证重名')

  await page.goto('/manage/type')
  await page.getByRole('button', { name: /新\s*增/ }).click()

  const dialog = page.locator('.el-dialog')
  await dialog.locator('input').first().fill(existing)
  const submitted = page.waitForResponse(
    (response) => response.url().includes('/api/v1/types') && response.request().method() === 'POST'
  )
  await dialog.getByRole('button', { name: /保\s*存/ }).click()

  const body = await (await submitted).json()
  expect(body.code).toBe('409')
  expect(body.msg).toContain('已存在')
  await expect(page.locator('.el-message').last()).toContainText('已存在')
})
