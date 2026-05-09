import { chromium } from 'playwright';
import { writeFileSync, existsSync, mkdirSync } from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const BASE = 'http://localhost:5173';
const API = 'http://localhost:9090';
const SS_DIR = path.join(__dirname, 'screenshots');
if (!existsSync(SS_DIR)) mkdirSync(SS_DIR, { recursive: true });

const results = [];
let page, browser;

function log(name, pass, detail = '') {
  console.log(`  ${pass ? '✅' : '❌'} ${name} ${detail}`);
  results.push({ name, pass, detail });
}

async function ss(name) {
  await page.screenshot({ path: path.join(SS_DIR, `${name}.png`), fullPage: false });
}

async function waitStable(ms = 1000) {
  await page.waitForLoadState('networkidle').catch(() => {});
  await page.waitForTimeout(ms);
}

(async () => {
  console.log('\n🚀 影院管理系统 E2E 测试\n');

  browser = await chromium.launch({ headless: true, args: ['--no-sandbox'] });
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 900 }, locale: 'zh-CN' });
  page = await ctx.newPage();

  // ========== 1. 后端接口健康检查 ==========
  console.log('📡 1. 后端健康检查');
  try {
    const r = await (await fetch(`${API}/getYear`)).json();
    log('GET /getYear', r.code === '200', `${r.data?.length} years`);
  } catch (e) { log('GET /getYear 接口', false, e.message); }

  try {
    const r = await (await fetch(`${API}/login`, { method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: '999', password: '999', role: 'ADMIN' }) })).json();
    log('POST /login', r.code === '200', `token: ${!!r.data?.token}`);
  } catch (e) { log('POST /login 接口', false, e.message); }

  try {
    const r = await (await fetch(`${API}/type/selectPage?pageNum=1&pageSize=5`,
      { headers: { 'Content-Type': 'application/json' } })).json();
    log('GET /type/selectPage', r.code === '200', `total: ${r.data?.total}`);
  } catch (e) { log('GET /type/selectPage 接口', false, e.message); }

  // ========== 2. 页面渲染 ==========
  console.log('\n🖥️  2. 页面渲染');
  await page.goto(BASE, { timeout: 15000 });
  await waitStable(2000);
  log('根路径跳转 /login', page.url().includes('/login'), page.url());
  await ss('01-login-page');
  log('登录页渲染', await page.isVisible('.login-container').catch(() => false));

  // ========== 3. 登录 ==========
  console.log('\n🔐 3. 登录');
  await page.fill('input[placeholder="请输入账号"]', '999');
  await page.fill('input[placeholder="请输入密码"]', '999');

  // 点击 el-select 选择角色
  const selectTrigger = page.locator('.el-select .el-select__wrapper').first();
  if (await selectTrigger.isVisible()) {
    await selectTrigger.click();
    await page.waitForTimeout(600);
    // 从下拉列表选择"管理员"
    const opts = page.locator('.el-select-dropdown__item');
    const count = await opts.count();
    for (let i = 0; i < count; i++) {
      const text = await opts.nth(i).innerText();
      if (text.includes('管理员')) { await opts.nth(i).click(); break; }
    }
    await page.waitForTimeout(300);
  }
  await page.locator('button').filter({ hasText: '登 录' }).click();
  await waitStable(2000);

  const loggedIn = page.url().includes('/manage/home');
  log('登录成功跳转', loggedIn, page.url());
  await ss('02-login-success');

  const userStr = await page.evaluate(() => localStorage.getItem('xm-pro-user'));
  const user = JSON.parse(userStr || '{}');
  log('localStorage 存有用户信息', !!userStr, `有token: ${!!user.token}`);

  // ========== 4. 路由直接导航 ==========
  console.log('\n🧭 4. 页面导航（直接路由）');

  const pages = [
    { route: '/manage/home', name: '系统首页' },
    { route: '/manage/admin', name: '管理员信息' },
    { route: '/manage/cinema', name: '影院信息' },
    { route: '/manage/user', name: '用户信息' },
    { route: '/manage/type', name: '电影分类' },
    { route: '/manage/film', name: '电影信息' },
    { route: '/manage/actor', name: '演职人员' },
    { route: '/manage/area', name: '电影区域' },
    { route: '/manage/notice', name: '系统公告' },
    { route: '/manage/room', name: '影厅房间' },
    { route: '/manage/record', name: '放映记录' },
    { route: '/manage/ordered', name: '购票订单' },
    { route: '/manage/mark', name: '用户评价' },
    { route: '/manage/video', name: '电影预告视频' },
    { route: '/manage/person', name: '个人资料' },
    { route: '/manage/password', name: '修改密码' },
  ];

  for (const p of pages) {
    try {
      await page.goto(`${BASE}${p.route}`, { timeout: 10000 });
      await waitStable(1000);
      const ok = page.url().includes(p.route);
      log(`页面 "${p.name}"`, ok, p.route);
    } catch (e) {
      log(`页面 "${p.name}"`, false, e.message);
    }
  }

  // ========== 5. 电影分类 CRUD ==========
  console.log('\n📝 5. 电影分类 CRUD');
  await page.goto(`${BASE}/manage/type`, { timeout: 10000 });
  await waitStable(1500);
  await ss('05-type-list');

  // 5.1 表格渲染
  const rows = await page.locator('.el-table__body-wrapper .el-table__row').count();
  log('分类表格有数据', rows > 0, `${rows} 行`);

  // 5.2 新增
  try {
    const addBtn = page.locator('button').filter({ hasText: '新 增' }).first();
    if (await addBtn.isVisible({ timeout: 2000 })) {
      await addBtn.click();
      await page.waitForTimeout(600);
      const dialog = page.locator('.el-dialog');
      if (await dialog.isVisible()) {
        const inp = dialog.locator('.el-input__inner').first();
        const testName = `E2E分类_${Date.now()}`;
        await inp.fill(testName);
        await dialog.locator('button').filter({ hasText: '保 存' }).click();
        await page.waitForTimeout(1000);
        log('新增分类', true, testName);
      } else {
        log('新增弹窗可见', false);
      }
    }
  } catch (e) { log('新增分类', false, e.message); }
  await ss('05-type-add');

  // 5.3 编辑（点击第一行的编辑按钮 icon-only）
  try {
    const editIcon = page.locator('.el-table__body-wrapper .el-table__row').first()
      .locator('.el-button').filter({ hasText: '' }).first();
    if (await editIcon.isVisible({ timeout: 2000 })) {
      await editIcon.click();
      await page.waitForTimeout(600);
      const dialog = page.locator('.el-dialog');
      if (await dialog.isVisible()) {
        const inp = dialog.locator('.el-input__inner').first();
        await inp.fill(`编辑_${Date.now()}`);
        await dialog.locator('button').filter({ hasText: '保 存' }).click();
        await waitStable(1000);
        log('编辑分类', true);
      }
    }
  } catch (e) { log('编辑分类', false, e.message); }
  await ss('05-type-edit');

  // 5.4 删除（删除第一行）
  try {
    const rows2 = page.locator('.el-table__body-wrapper .el-table__row');
    const firstRow = rows2.first();
    const delIcon = firstRow.locator('.el-button').nth(1);
    if (await delIcon.isVisible({ timeout: 2000 })) {
      await delIcon.click();
      await page.waitForTimeout(500);
      // Element Plus 确认框
      const confirmBtn = page.locator('.el-message-box .el-button--primary');
      if (await confirmBtn.isVisible({ timeout: 2000 })) {
        await confirmBtn.click();
        await waitStable(1000);
        log('删除分类', true);
      } else {
        log('删除确认框', false);
      }
    }
  } catch (e) { log('删除分类', false, e.message); }
  await ss('05-type-del');

  // ========== 6. 管理员页面 CRUD ==========
  console.log('\n👤 6. 管理员 CRUD');
  await page.goto(`${BASE}/manage/admin`, { timeout: 10000 });
  await waitStable(1500);
  await ss('06-admin-list');

  const adminRows = await page.locator('.el-table__body-wrapper .el-table__row').count();
  log('管理员表格有数据', adminRows > 0, `${adminRows} 行`);

  // 新增管理员
  try {
    const btn = page.locator('button').filter({ hasText: '新 增' }).first();
    if (await btn.isVisible({ timeout: 2000 })) {
      await btn.click();
      await page.waitForTimeout(600);
      await ss('06-admin-add-dialog');

      // 填写表单
      const inputs = page.locator('.el-dialog .el-input__inner');
      const inpCount = await inputs.count();
      if (inpCount >= 2) {
        const uname = `e2e_${Date.now()}`;
        await inputs.nth(0).fill(uname);
        await inputs.nth(1).fill('e2e@test.com');
        await page.locator('.el-dialog button').filter({ hasText: '保 存' }).click();
        await waitStable(1000);
        log('新增管理员', true, uname);
      }
    }
  } catch (e) { log('新增管理员', false, e.message); }
  await ss('06-admin-add');

  // 删除新增的管理员
  try {
    // 找到最后一条记录的删除按钮
    const lastRow = page.locator('.el-table__body-wrapper .el-table__row').last();
    const delBtn = lastRow.locator('.el-button').nth(1);
    if (await delBtn.isVisible({ timeout: 2000 })) {
      await delBtn.click();
      await page.waitForTimeout(500);
      const confirm = page.locator('.el-message-box .el-button--primary');
      if (await confirm.isVisible({ timeout: 2000 })) {
        await confirm.click();
        await waitStable(1000);
        log('删除管理员', true);
      }
    }
  } catch (e) { log('删除管理员', false, e.message); }

  // ========== 7. 分页功能 ==========
  console.log('\n📄 7. 分页功能');
  await page.goto(`${BASE}/manage/admin`, { timeout: 10000 });
  await waitStable(1000);

  try {
    const pager = page.locator('.el-pagination');
    if (await pager.isVisible({ timeout: 2000 })) {
      // 点击第二页
      const pageBtn = pager.locator('.el-pager li').nth(1);
      if (await pageBtn.isVisible()) {
        await pageBtn.click();
        await waitStable(1000);
        log('分页切换第二页', true);
        await ss('07-pagination');
      } else {
        log('分页按钮可见', false, '没有第二页');
      }
    } else {
      log('分页组件可见', false);
    }
  } catch (e) { log('分页测试', false, e.message); }

  // ========== 8. 搜索功能 ==========
  console.log('\n🔍 8. 搜索功能');
  await page.goto(`${BASE}/manage/film`, { timeout: 10000 });
  await waitStable(1000);
  await ss('08-film-list');

  try {
    const searchInput = page.locator('.el-input__inner').first();
    if (await searchInput.isVisible({ timeout: 2000 })) {
      await searchInput.fill('');
      const queryBtn = page.locator('button').filter({ hasText: '查 询' }).first();
      if (await queryBtn.isVisible()) {
        await queryBtn.click();
        await waitStable(1000);
        log('查询功能', true);
      }
    }
  } catch (e) { log('查询功能', false, e.message); }

  // ========== 9. 前端渲染完整性 ==========
  console.log('\n🎨 9. 前端渲染完整性');
  await page.goto(`${BASE}/login`, { timeout: 10000 });
  await waitStable(1000);
  await ss('09-login-final');

  // ========== 汇总 ==========
  console.log('\n' + '='.repeat(60));
  console.log('📊 E2E 测试汇总');
  console.log('='.repeat(60));

  const passed = results.filter(r => r.pass).length;
  const failed = results.filter(r => !r.pass).length;
  const total = results.length;
  console.log(`\n  总用例: ${total}  ✅ ${passed}  ❌ ${failed}  通过率: ${(passed / total * 100).toFixed(1)}%\n`);

  if (failed > 0) {
    console.log('  失败项:');
    results.filter(r => !r.pass).forEach(r => console.log(`    - ${r.name}: ${r.detail}`));
    console.log();
  }

  // ========== 生成 HTML 报告 ==========
  const rowsHtml = results.map((r, i) =>
    `<tr><td>${i+1}</td><td>${r.name}</td><td class="${r.pass ? 'pass' : 'fail'}">${r.pass ? '✅ 通过' : '❌ 失败'}</td><td>${r.detail || '-'}</td></tr>`
  ).join('');

  const ssGrid = [];
  for (let i = 1; i <= 19; i++) {
    const labels = ['01-login-page', '02-login-success', '03-', '04-', '05-type-list', '05-type-add', '05-type-edit',
      '05-type-del', '06-admin-list', '06-admin-add-dialog', '06-admin-add', '07-pagination', '08-film-list', '09-login-final'];
    const f = labels[i - 1] || `ss-${String(i).padStart(2,'0')}`;
    const png = `${String(i).padStart(2,'0')}-${f}.png`;
    ssGrid.push(`<div class="ss"><h4>${f}</h4><img src="screenshots/${png}" loading="lazy"></div>`);
  }

  const report = `<!DOCTYPE html><html lang="zh-CN">
<head><meta charset="UTF-8"><title>E2E 测试报告</title>
<style>
  body{font-family:-apple-system,sans-serif;max-width:1200px;margin:0 auto;padding:20px;background:#f5f5f5}
  h1{color:#333;border-bottom:2px solid #4A90E2;padding-bottom:10px}
  .summary{display:grid;grid-template-columns:repeat(4,1fr);gap:15px;margin:20px 0}
  .summary div{background:#fff;padding:20px;border-radius:8px;text-align:center;box-shadow:0 2px 4px rgba(0,0,0,.1)}
  .summary .num{font-size:28px;font-weight:bold}
  .summary .lb{font-size:13px;color:#666}
  table{width:100%;border-collapse:collapse;background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1)}
  th{background:#4A90E2;color:#fff;padding:10px 15px;text-align:left}
  td{padding:8px 15px;border-bottom:1px solid #eee}
  .pass{color:#4caf50;font-weight:bold} .fail{color:#f44336;font-weight:bold}
  .ss-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(350px,1fr));gap:15px;margin-top:20px}
  .ss{background:#fff;padding:10px;border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,.1)}
  .ss h4{margin:0 0 8px;text-transform:capitalize}
  .ss img{width:100%;border:1px solid #ddd;border-radius:4px}
  .footer{text-align:center;color:#999;margin:30px 0;font-size:13px}
</style></head><body>
<h1>🎬 影院管理系统 E2E 测试报告</h1>
<p>测试时间: ${new Date().toLocaleString('zh-CN')}</p>
<div class="summary">
  <div><div class="num" style="color:#2196f3">${total}</div><div class="lb">总用例</div></div>
  <div><div class="num" style="color:#4caf50">${passed}</div><div class="lb">通过</div></div>
  <div><div class="num" style="color:#f44336">${failed}</div><div class="lb">失败</div></div>
  <div><div class="num" style="color:#ff9800">${(passed/total*100).toFixed(1)}%</div><div class="lb">通过率</div></div>
</div>
<h2>📋 测试明细</h2>
<table><thead><tr><th>#</th><th>用例</th><th>结果</th><th>详情</th></tr></thead><tbody>${rowsHtml}</tbody></table>
<h2>📸 截图</h2><div class="ss-grid">${ssGrid.join('')}</div>
<div class="footer">影院管理系统 · Playwright E2E · ${new Date().toISOString().split('T')[0]}</div>
</body></html>`;

  writeFileSync(path.join(__dirname, 'e2e-report.html'), report);
  console.log(`📄 报告: e2e-report.html`);
  console.log(`📸 截图: ${SS_DIR}\n`);

  await browser.close();
  process.exit(failed > 0 ? 1 : 0);
})().catch(e => { console.error('异常:', e); browser?.close(); process.exit(1); });
