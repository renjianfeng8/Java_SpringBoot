export const API_BASE = process.env.E2E_API_BASE || 'http://localhost:9090/api/v1'

/** 接口登录：用于测试收尾清理，不经过页面 */
export async function apiLogin(request, { username, password, role }) {
  const response = await request.post(`${API_BASE}/auth/login`, { data: { username, password, role } })
  const body = await response.json()
  if (body.code !== '200') {
    throw new Error(`接口登录失败(${username}/${role}): ${body.msg}`)
  }
  return body.data
}

/** 页面登录：走真实表单与角色下拉，和用户操作一致 */
export async function uiLogin(page, { username, password, role = '用户' }) {
  await page.goto('/login')
  await page.getByPlaceholder('请输入账号').fill(username)
  await page.getByPlaceholder('请输入密码').fill(password)
  await page.locator('.el-select').click()
  await page.locator('.el-select-dropdown__item', { hasText: role }).first().click()
  await page.getByRole('button', { name: /登\s*录/ }).click()
  await page.waitForURL((url) => !url.pathname.startsWith('/login'), { timeout: 15_000 })
}

/** 当前页面的登录态令牌（页面上的接口调用与清理都用它，避免再登一次） */
export async function tokenOf(page) {
  return page.evaluate(() => JSON.parse(localStorage.getItem('xm-film-user') || '{}').token)
}
