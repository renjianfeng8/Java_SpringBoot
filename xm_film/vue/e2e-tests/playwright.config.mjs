// Playwright 配置文件
export default {
  testDir: './',
  timeout: 60000,
  expect: { timeout: 10000 },
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report' }],
    ['json', { outputFile: 'playwright-report/results.json' }]
  ],
  projects: [
    {
      name: 'chromium',
      use: {
        browserName: 'chromium',
        viewport: { width: 1920, height: 900 },
        locale: 'zh-CN',
        timezoneId: 'Asia/Shanghai',
        screenshot: 'only-on-failure',
        trace: 'retain-on-failure',
      },
    },
  ],
};
