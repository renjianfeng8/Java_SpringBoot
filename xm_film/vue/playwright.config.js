import { defineConfig } from '@playwright/test'

/**
 * 冒烟层：只覆盖「单测照不到的接缝」——表单、跨页流程、SQL 条件、库约束。
 * 前置条件见 e2e/README.md：后端 9090（开发库）已启动，前端由下面自动拉起。
 */
export default defineConfig({
  testDir: './e2e',
  // 共用同一个后端与数据库：并行会在座位、账号、订单上互相踩
  workers: 1,
  fullyParallel: false,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://localhost:5173',
    // 用系统 Chrome：省掉 ~100MB 浏览器下载；ubuntu-latest 与开发机都自带
    channel: 'chrome',
    trace: 'retain-on-failure',
  },
  webServer: {
    command: 'npm run dev -- --port 5173 --strictPort',
    url: 'http://localhost:5173',
    reuseExistingServer: true,
    timeout: 120_000,
  },
})
