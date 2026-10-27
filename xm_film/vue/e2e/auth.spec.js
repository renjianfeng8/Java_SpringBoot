import { expect, test } from '@playwright/test'
import { API_BASE, apiLogin, uiLogin } from './helpers'

test('未登录访问购票记录：先提示，再去登录页', async ({ page }) => {
  await page.goto('/front/orders')
  const box = page.locator('.el-message-box')
  await expect(box).toBeVisible()
  await box.getByRole('button', { name: '去登录' }).click()
  await expect(page).toHaveURL(/\/login/)
})

test('注册页能真正提交，新账号能登录', async ({ page, request }) => {
  const username = `e2e_${Date.now()}`
  const password = 'e2e123456'
  let userId = null

  try {
    await page.goto('/register')
    await page.getByPlaceholder('请输入账号').fill(username)
    await page.getByPlaceholder('请输入密码').fill(password)
    await page.getByPlaceholder('请确认密码').fill(password)
    await page.getByRole('button', { name: /注\s*册/ }).click()

    // 注册页的 bug 是「validator 签名错 → 静默不提交」，所以必须断言跳转发生
    await expect(page).toHaveURL(/\/login$/)
    await expect(page.locator('.el-message')).toContainText('注册成功')

    await uiLogin(page, { username, password })
    await expect(page).toHaveURL(/\/front\/home/)
  } finally {
    const admin = await apiLogin(request, { username: '999', password: '999', role: 'ADMIN' })
    const auth = { Authorization: `Bearer ${admin.token}` }
    const list = await request.get(
      `${API_BASE}/users/page?pageNum=1&pageSize=10&username=${encodeURIComponent(username)}`,
      { headers: auth }
    )
    userId = (await list.json()).data?.list?.[0]?.id ?? null
    if (userId) {
      await request.delete(`${API_BASE}/users/${userId}`, { headers: auth })
    }
  }
})
