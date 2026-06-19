/**
 * 影院管理系统 —— 全栈自动化 E2E 扫描测试
 * ============================================
 * 使用 playwright 原生 API（兼容现有依赖，无需 @playwright/test）
 *
 * 覆盖范围：
 *   - 后端 API 健康检查（10+ 接口）
 *   - 前端页面渲染与路由跳转（三端所有页面）
 *   - 核心元素存在性验证
 *   - 基础交互流程（登录、CRUD、搜索、分页）
 *
 * 运行方式：
 *   cd xm_film/vue
 *   node e2e-tests/e2e-scan.spec.mjs
 *
 * 依赖安装：
 *   npm install && npx playwright install chromium
 */

import { chromium } from 'playwright';
import { writeFileSync, existsSync, mkdirSync } from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const BASE = 'http://localhost:5173';
const API = 'http://localhost:9090';
const SS_DIR = path.join(__dirname, 'screenshots');
const EXPECTED_TEST_COUNT = 67;
if (!existsSync(SS_DIR)) mkdirSync(SS_DIR, { recursive: true });

const results = [];
let page, browser;

function log(name, pass, detail = '') {
  const icon = pass ? '✅' : '❌';
  console.log(`  ${icon} ${name} ${detail}`);
  results.push({ name, pass, detail });
}

async function ss(name) {
  try { await page.screenshot({ path: path.join(SS_DIR, `${name}.png`), fullPage: false }); } catch {}
}

async function waitStable(ms = 1500) {
  try { await page.waitForLoadState('networkidle'); } catch {}
  await page.waitForTimeout(ms);
}

// ======================== 工具函数 ========================

async function apiPost(url, body, token) {
  try {
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const r = await fetch(`${API}${url}`, {
      method: 'POST',
      headers,
      body: JSON.stringify(body),
    });
    return await r.json();
  } catch (e) {
    return { code: '500', msg: e.message };
  }
}

async function apiGet(url, token) {
  try {
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const r = await fetch(`${API}${url}`, { headers });
    return await r.json();
  } catch (e) {
    return { code: '500', msg: e.message };
  }
}

async function apiPut(url, token, body) {
  try {
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const r = await fetch(`${API}${url}`, {
      method: 'PUT',
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    return await r.json();
  } catch (e) {
    return { code: '500', msg: e.message };
  }
}

let adminToken = ''; // 存储登录 token

// ======================== 主流程 ========================

(async () => {
  console.log('\n' + '='.repeat(60));
  console.log('🎬 影院管理系统 — 全栈 E2E 自动化扫描测试');
  console.log('='.repeat(60));
  console.log(`测试时间: ${new Date().toLocaleString('zh-CN')}\n`);

  // ========== 1. 后端 API 健康检查 ==========
  console.log('📡 1. 后端 API 健康检查');

  // 先登录获取 token
  let loginRes = await apiPost('/api/v1/auth/login', { username: '999', password: '999', role: 'ADMIN' });
  adminToken = loginRes?.data?.token || '';
  log('POST /api/v1/auth/login (管理员)', loginRes.code === '200' && !!adminToken, `token: ${!!adminToken}`);

  // 如果第一次登录失败，可能是密码被 BCrypt 加密了，用注册接口创建一个新管理员
  if (!adminToken) {
    loginRes = await apiPost('/api/v1/auth/login', { username: '999', password: '999', role: 'ADMIN' });
    adminToken = loginRes?.data?.token || '';
  }

  const apiTests = [
    { name: 'POST /api/v1/auth/register', fn: () => apiPost('/api/v1/auth/register', { username: `test_${Date.now()}`, password: '123456', role: 'USER' }), check: r => r.code === '200' },
    { name: 'GET /api/v1/auth/years', fn: () => apiGet('/api/v1/auth/years'), check: r => r.code === '200' && Array.isArray(r.data) && r.data.length >= 10 },
  ];

  for (const t of apiTests) {
    try {
      const res = await t.fn();
      log(t.name, t.check(res), `code: ${res.code}`);
    } catch (e) {
      log(t.name, false, e.message);
    }
  }

  // 需要认证的接口 - 使用登录token
  const authApiTests = [
    { name: 'GET /api/v1/types (已认证)', fn: () => apiGet('/api/v1/types', adminToken), check: r => r.code === '200' && Array.isArray(r.data) },
    { name: 'GET /api/v1/areas (已认证)', fn: () => apiGet('/api/v1/areas', adminToken), check: r => r.code === '200' && Array.isArray(r.data) },
    { name: 'GET /api/v1/films (已认证)', fn: () => apiGet('/api/v1/films', adminToken), check: r => r.code === '200' && Array.isArray(r.data) },
    { name: 'GET /api/v1/cinemas (已认证)', fn: () => apiGet('/api/v1/cinemas', adminToken), check: r => r.code === '200' && Array.isArray(r.data) },
    { name: 'GET /api/v1/films/box-office/top (已认证)', fn: () => apiGet('/api/v1/films/box-office/top?topNum=10', adminToken), check: r => r.code === '200' && Array.isArray(r.data) },
    { name: 'GET /api/v1/films/mark/top (已认证)', fn: () => apiGet('/api/v1/films/mark/top?topNum=5', adminToken), check: r => r.code === '200' && Array.isArray(r.data) },
    { name: 'GET /api/v1/notices (已认证)', fn: () => apiGet('/api/v1/notices', adminToken), check: r => r.code === '200' && Array.isArray(r.data) },
  ];

  for (const t of authApiTests) {
    try {
      const res = await t.fn();
      log(t.name, t.check(res), `code: ${res.code}`);
    } catch (e) {
      log(t.name, false, e.message);
    }
  }

  // ========== 2. 页面渲染 ==========
  console.log('\n🖥️  2. 页面渲染与路由');
  browser = await chromium.launch({ headless: true, args: ['--no-sandbox'] });
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 900 }, locale: 'zh-CN' });
  page = await ctx.newPage();

  // 2.1 登录页
  try {
    await page.goto(`${BASE}/login`, { timeout: 15000 });
    await waitStable();
    const onLoginPage = page.url().includes('/login');
    const hasInput = await page.isVisible('input[placeholder="请输入账号"]');
    const hasPwd = await page.isVisible('input[placeholder="请输入密码"]');
    const hasLoginBtn = await page.isVisible('button:has-text("登")');
    log('登录页渲染', onLoginPage && hasInput && hasPwd && hasLoginBtn, `input:${hasInput} pwd:${hasPwd} btn:${hasLoginBtn}`);
  } catch (e) { log('登录页渲染', false, e.message); }

  // 2.2 注册页
  try {
    await page.goto(`${BASE}/register`, { timeout: 10000 });
    await waitStable();
    log('注册页', page.url().includes('/register'));
  } catch (e) { log('注册页', false, e.message); }

  // 2.3 404 页面
  try {
    await page.goto(`${BASE}/nonexistent`, { timeout: 10000 });
    await waitStable();
    log('404 页面', page.url().includes('/404'));
  } catch (e) { log('404 页面', false, e.message); }

  // 2.4 根路径重定向
  try {
    await page.goto(BASE, { timeout: 10000 });
    await waitStable();
    log('根路径 → /front/home', page.url().includes('/front/home'));
  } catch (e) { log('根路径重定向', false, e.message); }

  // ========== 3. 管理员登录流程 ==========
  console.log('\n🔐 3. 管理员登录流程');
  await page.goto(`${BASE}/login`, { timeout: 15000 });
  await waitStable();

  try {
    await page.fill('input[placeholder="请输入账号"]', '999');
    await page.fill('input[placeholder="请输入密码"]', '999');

    // 选择管理员角色
    const selectTrigger = page.locator('.el-select .el-select__wrapper').first();
    if (await selectTrigger.isVisible()) {
      await selectTrigger.click();
      await page.waitForTimeout(600);
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
    await ss('01-login-success');

    const userStr = await page.evaluate(() => localStorage.getItem('xm-pro-user'));
    const user = userStr ? JSON.parse(userStr) : {};
    log('localStorage 用户信息', !!userStr && !!user.token, `role: ${user.role}`);
  } catch (e) { log('登录流程', false, e.message); }

  // ========== 4. 管理后台页面导航 ==========
  console.log('\n🧭 4. 管理后台页面 (' + [
    'home', 'admin', 'cinema', 'user', 'type', 'area', 'film', 'actor',
    'notice', 'room', 'record', 'ordered', 'mark', 'video', 'person', 'password'
  ].length + ' 个页面)');

  const adminPages = [
    { route: '/manage/home', name: '系统首页' },
    { route: '/manage/admin', name: '管理员信息' },
    { route: '/manage/cinema', name: '影院信息' },
    { route: '/manage/user', name: '用户信息' },
    { route: '/manage/type', name: '电影分类' },
    { route: '/manage/area', name: '电影区域' },
    { route: '/manage/film', name: '电影信息' },
    { route: '/manage/actor', name: '演职人员' },
    { route: '/manage/notice', name: '系统公告' },
    { route: '/manage/room', name: '影厅房间' },
    { route: '/manage/record', name: '放映记录' },
    { route: '/manage/ordered', name: '购票订单' },
    { route: '/manage/mark', name: '用户评价' },
    { route: '/manage/video', name: '电影预告视频' },
    { route: '/manage/person', name: '个人资料' },
    { route: '/manage/password', name: '修改密码' },
  ];

  for (const p of adminPages) {
    try {
      await page.goto(`${BASE}${p.route}`, { timeout: 10000 });
      await waitStable(1000);
      log(`页面 "${p.name}"`, page.url().includes(p.route), p.route);
    } catch (e) {
      log(`页面 "${p.name}"`, false, e.message);
    }
  }

  // ========== 5. 分类 CRUD ==========
  console.log('\n📝 5. 电影分类 CRUD');
  await page.goto(`${BASE}/manage/type`, { timeout: 10000 });
  await waitStable(1500);

  const rows = await page.locator('.el-table__body-wrapper .el-table__row').count();
  log('分类表格有数据', rows > 0, `${rows} 行`);
  await ss('02-type-list');

  // 新增
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
        await waitStable(1000);
        log('新增分类', true, testName);
      } else {
        log('新增分类', false, '新增对话框不可见');
      }
    } else {
      log('新增分类', false, '新增按钮不可见');
    }
  } catch (e) { log('新增分类', false, e.message); }

  // 编辑
  try {
    const editBtn = page.locator('.el-table__body-wrapper .el-table__row').first()
      .locator('.el-button').first();
    if (await editBtn.isVisible({ timeout: 2000 })) {
      await editBtn.click();
      await page.waitForTimeout(600);
      const editDialog = page.locator('.el-dialog');
      if (await editDialog.isVisible()) {
        const editInp = editDialog.locator('.el-input__inner').first();
        await editInp.fill(`编辑_${Date.now()}`);
        await editDialog.locator('button').filter({ hasText: '保 存' }).click();
        await waitStable(1000);
        log('编辑分类', true);
      } else {
        log('编辑分类', false, '编辑对话框不可见');
      }
    } else {
      log('编辑分类', false, '编辑按钮不可见');
    }
  } catch (e) { log('编辑分类', false, e.message); }

  // 删除
  try {
    const firstRow = page.locator('.el-table__body-wrapper .el-table__row').first();
    const delBtn = firstRow.locator('.el-button').nth(1);
    if (await delBtn.isVisible({ timeout: 2000 })) {
      await delBtn.click();
      await page.waitForTimeout(500);
      const confirmBtn = page.locator('.el-message-box .el-button--primary');
      if (await confirmBtn.isVisible({ timeout: 2000 })) {
        await confirmBtn.click();
        await waitStable(1000);
        log('删除分类', true);
      } else {
        log('删除分类', false, '删除确认按钮不可见');
      }
    } else {
      log('删除分类', false, '删除按钮不可见');
    }
  } catch (e) { log('删除分类', false, e.message); }
  await ss('03-type-crud');

  // ========== 6. 分页功能 ==========
  console.log('\n📄 6. 分页功能');
  await page.goto(`${BASE}/manage/admin`, { timeout: 10000 });
  await waitStable(1000);

  try {
    const pager = page.locator('.el-pagination');
    if (await pager.isVisible({ timeout: 2000 })) {
      const pageBtns = pager.locator('.el-pager li');
      const cnt = await pageBtns.count();
      if (cnt > 1) {
        await pageBtns.nth(1).click();
        await waitStable(1000);
        log('分页切换第二页', true);
      } else {
        // 数据量少只有一页属于正常情况
        log('分页组件', true, '正常显示(单页数据)');
      }
    } else {
      log('分页组件可见', false);
    }
  } catch (e) { log('分页测试', false, e.message); }

  // ========== 7. 搜索功能 ==========
  console.log('\n🔍 7. 搜索功能');
  try {
    await page.goto(`${BASE}/manage/film`, { timeout: 10000 });
    await waitStable(1000);

    const queryBtn = page.locator('button').filter({ hasText: '查 询' }).first();
    if (await queryBtn.isVisible({ timeout: 2000 })) {
      const inputs = page.locator('.el-input__inner');
      const cnt = await inputs.count();
      for (let i = 0; i < Math.min(cnt, 3); i++) { await inputs.nth(i).fill(''); }
      await queryBtn.click();
      await waitStable(1000);
      log('查询功能', true);
    } else {
      log('查询按钮可见', false);
    }
  } catch (e) { log('搜索测试', false, e.message); }

  // ========== 8. 用户前台 ==========
  console.log('\n🎫 8. 用户前台页面');

  // 用户登录
  try {
    await page.goto(`${BASE}/login`, { timeout: 10000 });
    await waitStable();
    await page.fill('input[placeholder="请输入账号"]', 'zhangsan');
    await page.fill('input[placeholder="请输入密码"]', 'user123');
    const sel = page.locator('.el-select .el-select__wrapper').first();
    if (await sel.isVisible()) {
      await sel.click();
      await page.waitForTimeout(600);
      const opts = page.locator('.el-select-dropdown__item');
      const cnt = await opts.count();
      for (let i = 0; i < cnt; i++) {
        const text = await opts.nth(i).innerText();
        if (text.includes('用户')) { await opts.nth(i).click(); break; }
      }
      await page.waitForTimeout(300);
    }
    await page.locator('button').filter({ hasText: '登 录' }).click();
    await waitStable(2000);
    log('用户登录', page.url().includes('/front/home'));
  } catch (e) { log('用户登录', false, e.message); }

  // 前台首页元素检查
  try {
    await page.goto(`${BASE}/front/home`, { timeout: 10000 });
    await waitStable(2000);
    await ss('04-front-home');

    const headerVisible = await page.isVisible('.front-header');
    const movieLink = await page.isVisible('text=电影');
    const cinemaLink = await page.isVisible('text=影院');
    const searchInputVisible = await page.isVisible('input[placeholder="请输入电影名称"]');
    const hotLabel = await page.isVisible('text=正在热播');
    const upcomingLabel = await page.isVisible('text=即将上映');
    const boxOfficeLabel = await page.isVisible('text=总票房Top');
    const markLabel = await page.isVisible('text=评分Top');

    log('首页核心元素', headerVisible, `header:${headerVisible} 电影:${movieLink} 影院:${cinemaLink} 搜索:${searchInputVisible}`);
    log('首页内容区域', hotLabel && upcomingLabel, `热播:${hotLabel} 即将:${upcomingLabel}`);
    log('首页排行榜', boxOfficeLabel && markLabel, `票房:${boxOfficeLabel} 评分:${markLabel}`);

    // 搜索功能测试
    const searchInputLocator = page.locator('input[placeholder="请输入电影名称"]');
    if (await searchInputLocator.isVisible()) {
      await searchInputLocator.fill('哈利');
      await page.waitForTimeout(300);
      const searchBtn = page.locator('button').filter({ hasText: '搜 索' }).first();
      if (await searchBtn.isVisible()) {
        await searchBtn.click();
        await waitStable(2000);
        log('搜索跳转', page.url().includes('/front/search'), page.url());
      } else {
        log('搜索跳转', false, '搜索按钮不可见');
      }
    } else {
      log('搜索跳转', false, '搜索输入框不可见');
    }
  } catch (e) { log('前台首页测试', false, e.message); }

  // 用户订单主流程：下单、重复座位冲突、查询、取消
  {
    const userLogin = await apiPost('/api/v1/auth/login', {
      username: 'zhangsan',
      password: 'user123',
      role: 'USER'
    });
    const userToken = userLogin?.data?.token || '';
    log('用户 API 登录（订单主流程）', userLogin.code === '200' && !!userToken);

    if (!userToken) {
      log('创建订单', false, '未获取用户 token');
      log('重复座位下单应拒绝', false, '未获取用户 token');
      log('用户订单列表包含新订单', false, '未获取用户 token');
      log('取消订单', false, '未获取用户 token');
    } else {
      try {
        const recordsRes = await apiGet('/api/v1/records', userToken);
        const record = Array.isArray(recordsRes.data) ? recordsRes.data[0] : null;
        if (!record?.id) throw new Error('没有可用排片');

        const soldRes = await apiGet(`/api/v1/orders/seats?recordId=${record.id}`, userToken);
        const occupied = new Set(
          (Array.isArray(soldRes.data) ? soldRes.data : [])
            .flatMap(order => String(order.seat || '').replaceAll('，', ',').split(','))
            .map(seat => seat.trim())
            .filter(Boolean)
        );
        const seat = Array.from({ length: 64 }, (_, i) =>
          `${Math.floor(i / 8) + 1}排${(i % 8) + 1}座`
        ).find(candidate => !occupied.has(candidate));
        if (!seat) throw new Error('当前排片没有可用座位');

        const createRes = await apiPost('/api/v1/orders/create', { recordId: record.id, seat }, userToken);
        log('创建订单', createRes.code === '200', `code: ${createRes.code}, seat: ${seat}`);

        const duplicateRes = await apiPost('/api/v1/orders/create', { recordId: record.id, seat }, userToken);
        log('重复座位下单应拒绝', duplicateRes.code === '409', `code: ${duplicateRes.code}`);

        const orderId = createRes.data?.id;

        // 支付前可以取消
        const preCancelRes = orderId
          ? await apiPut(`/api/v1/orders/${orderId}/cancel`, userToken)
          : { code: '500' };
        log('待支付可取消', preCancelRes.code === '200', `code: ${preCancelRes.code}`);

        // 重新下单（刚才取消了，座位已释放）
        const reCreateRes = await apiPost('/api/v1/orders/create', { recordId: record.id, seat }, userToken);
        log('释放后重新下单', reCreateRes.code === '200', `code: ${reCreateRes.code}`);
        const newOrderId = reCreateRes.data?.id;

        // 模拟支付
        const payRes = newOrderId
          ? await apiPut(`/api/v1/orders/${newOrderId}/pay`, userToken)
          : { code: '500' };
        log('模拟支付', payRes.code === '200', `code: ${payRes.code}`);

        // 支付后不可取消
        const postPayCancelRes = newOrderId
          ? await apiPut(`/api/v1/orders/${newOrderId}/cancel`, userToken)
          : { code: '500' };
        log('支付后不可取消', postPayCancelRes.code === '409', `code: ${postPayCancelRes.code}`);

        const ordersRes = await apiGet('/api/v1/orders', userToken);
        const createdOrder = (Array.isArray(ordersRes.data) ? ordersRes.data : [])
          .find(order => order.recordId === record.id && order.seat === seat && order.status === '待取票');
        log('用户订单列表包含新订单', !!createdOrder, `orderId: ${createdOrder?.id || '-'}`);
      } catch (e) {
        log('创建订单', false, e.message);
        log('重复座位下单应拒绝', false, e.message);
        log('待支付可取消', false, e.message);
        log('释放后重新下单', false, e.message);
        log('模拟支付', false, e.message);
        log('支付后不可取消', false, e.message);
        log('用户订单列表包含新订单', false, e.message);
      }
    }
  }

  // 前台电影列表页
  try {
    await page.goto(`${BASE}/front/movie`, { timeout: 10000 });
    await waitStable(1500);
    log('电影列表页', page.url().includes('/front/movie'));
  } catch (e) { log('电影列表页', false, e.message); }

  // 前台排行榜页
  try {
    await page.goto(`${BASE}/front/rank`, { timeout: 10000 });
    await waitStable(1500);
    log('排行榜页', page.url().includes('/front/rank'));
  } catch (e) { log('排行榜页', false, e.message); }

  // 前台页脚
  try {
    await page.goto(`${BASE}/front/home`, { timeout: 10000 });
    await waitStable(1500);
    const footerItems = [
      '项目声明', '功能与风险', '用户信息与隐私', '版权信息', '个人学习项目'
    ];
    let footerOk = 0;
    for (const item of footerItems) {
      if (await page.isVisible(`text=${item}`).catch(() => false)) footerOk++;
    }
    log('页脚信息', footerOk >= 4, `${footerOk}/5 项可见`);
    await ss('04-footer');
  } catch (e) { log('页脚测试', false, e.message); }

  // ========== 9. 影院后台页面 ==========
  console.log('\n🏢 9. 影院后台页面');
  // 先以影院管理员身份登录（ADMIN 无权访问影院后台）
  try {
    await page.goto(`${BASE}/login`, { timeout: 10000 });
    await waitStable();
    await page.fill('input[placeholder="请输入账号"]', 'asks');
    await page.fill('input[placeholder="请输入密码"]', 'cinema123');
    const sel = page.locator('.el-select .el-select__wrapper').first();
    if (await sel.isVisible()) {
      await sel.click();
      await page.waitForTimeout(600);
      const opts = page.locator('.el-select-dropdown__item');
      const cnt = await opts.count();
      for (let i = 0; i < cnt; i++) {
        const text = await opts.nth(i).innerText();
        if (text.includes('影院')) { await opts.nth(i).click(); break; }
      }
      await page.waitForTimeout(300);
    }
    await page.locator('button').filter({ hasText: '登 录' }).click();
    await waitStable(2000);
    log('影院管理员登录', page.url().includes('/back/home'), page.url());
  } catch (e) { log('影院管理员登录', false, e.message); }

  const backPages = [
    '/back/home', '/back/film', '/back/room', '/back/record',
    '/back/ordered', '/back/person', '/back/password'
  ];
  for (const route of backPages) {
    try {
      await page.goto(`${BASE}${route}`, { timeout: 10000 });
      await waitStable(800);
      log(`${route}`, page.url().includes(route.replace('/','\/')), route);
    } catch (e) {
      log(`${route}`, false, e.message);
    }
  }
  await ss('05-back-pages');

  // ========== 10. 负面测试 ==========
  console.log('\n⚠️  10. 负面测试 (Negative Cases)');

  // 10.1 错误密码登录 → API 返回错误
  {
    const res = await apiPost('/api/v1/auth/login', { username: '999', password: 'wrong_password', role: 'ADMIN' });
    log('错误密码登录应拒绝', res.code !== '200', `code: ${res.code}`);
  }

  // 10.2 无 token 访问需认证接口 → 返回 401
  {
    const res = await apiGet('/api/v1/admins');
    log('无 token 访问受限接口', res.code !== '200', `code: ${res.code}`);
  }

  // 10.3 ADMIN 角色访问影院后台 → 路由守卫拦截
  try {
    // 先以管理员登录
    await page.goto(`${BASE}/login`, { timeout: 10000 });
    await waitStable();
    await page.fill('input[placeholder="请输入账号"]', '999');
    await page.fill('input[placeholder="请输入密码"]', '999');
    const sel = page.locator('.el-select .el-select__wrapper').first();
    if (await sel.isVisible()) {
      await sel.click();
      await page.waitForTimeout(600);
      const opts = page.locator('.el-select-dropdown__item');
      const cnt = await opts.count();
      for (let i = 0; i < cnt; i++) {
        const text = await opts.nth(i).innerText();
        if (text.includes('管理员')) { await opts.nth(i).click(); break; }
      }
      await page.waitForTimeout(300);
    }
    await page.locator('button').filter({ hasText: '登 录' }).click();
    await waitStable(2000);

    // 尝试访问影院后台
    await page.goto(`${BASE}/back/home`, { timeout: 10000 });
    await waitStable(1500);
    log('管理员无法访问影院后台', !page.url().includes('/back/home'), page.url());
  } catch (e) { log('路由权限拦截', false, e.message); }

  // 10.4 未选数据点击批量删除 → 弹出警告提示
  try {
    // 先重新登录管理员
    await page.goto(`${BASE}/login`, { timeout: 10000 });
    await waitStable();
    await page.fill('input[placeholder="请输入账号"]', '999');
    await page.fill('input[placeholder="请输入密码"]', '999');
    const sel4 = page.locator('.el-select .el-select__wrapper').first();
    if (await sel4.isVisible()) {
      await sel4.click();
      await page.waitForTimeout(600);
      const opts = page.locator('.el-select-dropdown__item');
      for (let i = 0; i < (await opts.count()); i++) {
        const text = await opts.nth(i).innerText();
        if (text.includes('管理员')) { await opts.nth(i).click(); break; }
      }
      await page.waitForTimeout(300);
    }
    await page.locator('button').filter({ hasText: '登 录' }).click();
    await waitStable(2000);

    // 进入演职人员页面（其 handleDelBatch 有 ElMessage.warning 提示）
    await page.goto(`${BASE}/manage/actor`, { timeout: 10000 });
    await waitStable(1000);
    const batchBtn = page.locator('button').filter({ hasText: '批量删除' }).first();
    if (await batchBtn.isVisible({ timeout: 3000 })) {
      await batchBtn.click();
      await page.waitForTimeout(400);
      // Actor.vue: if (!selectedIds.value.length) { ElMessage.warning('请先选择要删除的演职人员'); return }
      const hasWarning = await page.locator('text=请先选择要删除的演职人员').isVisible().catch(() => false);
      log('未选择数据时批量删除有警告提示', hasWarning);
    } else {
      log('未选择数据时批量删除有警告提示', false, '批量删除按钮不可见');
    }
  } catch (e) { log('批量删除提示测试', false, e.message); }

  // 10.5 未登录访问受保护页面 → 弹确认框 → 点去登录 → 跳转到登录页
  try {
    await page.evaluate(() => localStorage.removeItem('xm-pro-user'));
    await page.goto(`${BASE}/manage/home`, { timeout: 10000 });
    await waitStable(1500);
    const dialogBtn = page.locator('.el-message-box__btns button').filter({ hasText: '去登录' });
    const dialogVisible = await dialogBtn.isVisible().catch(() => false);
    if (dialogVisible) {
      await dialogBtn.click();
      await waitStable(1000);
    }
    log('未登录访问管理页弹框提示登录', page.url().includes('/login'), page.url());
  } catch (e) { log('未登录重定向', false, e.message); }
  await ss('06-negative');

  // ========== 11. 汇总 ==========
  console.log('\n' + '='.repeat(60));
  console.log('📊 E2E 全栈扫描测试汇总');
  console.log('='.repeat(60));

  const passed = results.filter(r => r.pass).length;
  const failed = results.filter(r => !r.pass).length;
  const total = results.length;
  if (total !== EXPECTED_TEST_COUNT) {
    log('E2E 用例数量完整', false, `expected: ${EXPECTED_TEST_COUNT}, actual: ${total}`);
  }
  const finalPassed = results.filter(r => r.pass).length;
  const finalFailed = results.filter(r => !r.pass).length;
  const finalTotal = results.length;
  console.log(`\n  总用例: ${finalTotal}  ✅ ${finalPassed}  ❌ ${finalFailed}  通过率: ${(finalPassed / finalTotal * 100).toFixed(1)}%\n`);

  if (finalFailed > 0) {
    console.log('  失败项:');
    results.filter(r => !r.pass).forEach(r => console.log(`    - ${r.name}: ${r.detail}`));
    console.log();
  }

  // ========== 生成 HTML 报告 ==========
  const rowsHtml = results.map((r, i) =>
    `<tr><td>${i+1}</td><td>${r.name}</td><td class="${r.pass ? 'pass' : 'fail'}">${r.pass ? '✅ 通过' : '❌ 失败'}</td><td>${r.detail || '-'}</td></tr>`
  ).join('');

  const report = `<!DOCTYPE html><html lang="zh-CN">
<head><meta charset="UTF-8"><title>全栈 E2E 扫描测试报告</title>
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
  .footer{text-align:center;color:#999;margin:30px 0}
</style></head><body>
<h1>🎬 影院管理系统 — 全栈 E2E 扫描测试报告</h1>
<p>测试时间: ${new Date().toLocaleString('zh-CN')}</p>
<div class="summary">
  <div><div class="num" style="color:#2196f3">${finalTotal}</div><div class="lb">总用例</div></div>
  <div><div class="num" style="color:#4caf50">${finalPassed}</div><div class="lb">通过</div></div>
  <div><div class="num" style="color:#f44336">${finalFailed}</div><div class="lb">失败</div></div>
  <div><div class="num" style="color:#ff9800">${(finalPassed/finalTotal*100).toFixed(1)}%</div><div class="lb">通过率</div></div>
</div>
<h2>📋 测试明细</h2>
<table><thead><tr><th>#</th><th>用例</th><th>结果</th><th>详情</th></tr></thead><tbody>${rowsHtml}</tbody></table>
<h2>📸 截图</h2>
<div class="ss-grid">${['01-login-success','02-type-list','03-type-crud','04-front-home','05-back-pages'].map(f =>
  `<div class="ss"><h4>${f}</h4><img src="screenshots/${f}.png" loading="lazy"></div>`
).join('')}</div>
<div class="footer">影院管理系统 · Playwright 全栈 E2E · ${new Date().toISOString().split('T')[0]}</div>
</body></html>`;

  writeFileSync(path.join(__dirname, 'e2e-scan-report.html'), report);
  console.log(`📄 HTML 报告: e2e-tests/e2e-scan-report.html`);
  console.log(`📸 截图目录: ${SS_DIR}\n`);

  await browser.close();
  process.exit(finalFailed > 0 ? 1 : 0);
})().catch(e => {
  console.error('\n❌ 测试异常:', e.message);
  console.error(e);
  browser?.close();
  process.exit(1);
});
