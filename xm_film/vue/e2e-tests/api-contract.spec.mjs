/**
 * 前后端 API 契约测试
 * =====================
 * 验证前端依赖的 API 端点存在且返回期望的数据结构。
 * 防止 API 路径/字段名前后端不同步（Bug.md 中最频繁的 Bug 类别）。
 *
 * 运行方式：
 *   node e2e-tests/api-contract.spec.mjs
 *
 * 依赖：后端运行在 http://localhost:9090
 */

const API = 'http://localhost:9090';

// ======================== 测试基础设施 ========================

let passed = 0;
let failed = 0;
let createdUserIds = [];

function assert(condition, label, detail = '') {
  if (condition) {
    console.log(`  ✅ ${label} ${detail}`);
    passed++;
  } else {
    console.log(`  ❌ ${label} ${detail}`);
    failed++;
  }
}

async function apiGet(url, token) {
  const headers = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const r = await fetch(`${API}${url}`, { headers });
  return { status: r.status, body: await r.json() };
}

async function apiPost(url, body, token) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const r = await fetch(`${API}${url}`, {
    method: 'POST', headers, body: JSON.stringify(body),
  });
  return { status: r.status, body: await r.json() };
}

async function apiPut(url, body, token) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const r = await fetch(`${API}${url}`, {
    method: 'PUT', headers, body: JSON.stringify(body),
  });
  return { status: r.status, body: await r.json() };
}

async function apiDelete(url, token) {
  const headers = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const r = await fetch(`${API}${url}`, { method: 'DELETE', headers });
  return { status: r.status, body: await r.json() };
}

// ======================== 认证工具 ========================

let adminToken = null;

async function loginAdmin() {
  const { body } = await apiPost('/api/v1/auth/login', {
    username: '999', password: '999', role: 'ADMIN',
  });
  if (body.code === '200' && body.data?.token) {
    adminToken = body.data.token;
  }
  return adminToken;
}

// ======================== 契约测试 ========================

async function run() {
  console.log('\n===================================');
  console.log('  API 契约测试');
  console.log('===================================\n');

  // ---- 1. 认证接口 ----
  console.log('--- 1. 认证接口 ---');
  {
    // 1a. 登录成功
    const r1 = await apiPost('/api/v1/auth/login', { username: '999', password: '999', role: 'ADMIN' });
    assert(r1.status === 200, 'POST /api/v1/auth/login', '(状态 200)');
    assert(r1.body.code === '200', '  → code 字段', `(=${r1.body.code})`);
    assert(r1.body.data?.token, '  → 返回 token');
    assert(r1.body.data?.role === 'ADMIN', '  → 返回 role');
    assert(r1.body.data?.username, '  → 返回 username');
    adminToken = r1.body.data?.token;

    // 1b. 登录失败（错误密码）
    const r2 = await apiPost('/api/v1/auth/login', { username: '999', password: 'wrong', role: 'ADMIN' });
    assert(r2.body.code !== '200', 'POST /api/v1/auth/login (错误密码)', `→ code=${r2.body.code}`);

    // 1c. 注册接口存在（记录 ID 用于清理）
    const r3 = await apiPost('/api/v1/auth/register', { username: '_test_contract', password: 'test123', role: 'USER' });
    if (r3.body.code === '200' && r3.body.data?.id) {
      createdUserIds.push({ type: 'user', id: r3.body.data.id });
    }
    assert(r3.status === 200, 'POST /api/v1/auth/register', '(接口可达)');
  }

  // ---- 2. 健康检查 ----
  console.log('\n--- 2. 健康检查 ---');
  {
    const r = await apiGet('/api/v1/health');
    assert(r.status === 200, 'GET /api/v1/health', '(状态 200)');
    assert(r.body.code === '200', '  → 返回 code');
  }

  // ---- 3. 公共资源 CRUD 端点 ----
  console.log('\n--- 3. 资源 CRUD 端点 ---');

  const endpoints = [
    { name: 'admins', path: '/api/v1/admins' },
    { name: 'users', path: '/api/v1/users' },
    { name: 'cinemas', path: '/api/v1/cinemas' },
    { name: 'films', path: '/api/v1/films' },
    { name: 'actors', path: '/api/v1/actors' },
    { name: 'areas', path: '/api/v1/areas' },
    { name: 'types', path: '/api/v1/types' },
    { name: 'notices', path: '/api/v1/notices' },
    { name: 'rooms', path: '/api/v1/rooms' },
    { name: 'records', path: '/api/v1/records' },
    { name: 'orders', path: '/api/v1/orders' },
    { name: 'marks', path: '/api/v1/marks' },
    { name: 'videos', path: '/api/v1/videos' },
  ];

  for (const ep of endpoints) {
    // 3a. GET list
    const r1 = await apiGet(ep.path, adminToken);
    assert(r1.status === 200, `GET ${ep.path}`, '(状态 200)');
    assert(r1.body.code === '200', `  → ${ep.name} code 字段`);

    // 3b. GET page (分页)
    const r2 = await apiGet(`${ep.path}/page?pageNum=1&pageSize=5`, adminToken);
    assert(r2.status === 200, `GET ${ep.path}/page`, '(状态 200)');
    assert(r2.body.code === '200', `  → ${ep.name} 分页成功`);
    if (r2.body.data) {
      assert('list' in r2.body.data, `  → 返回 list 数组`);
      assert('total' in r2.body.data, `  → 返回 total 字段`);
    }

    // 3c. GET by id=1
    const r4 = await apiGet(`${ep.path}/1`, adminToken);
    assert(r4.status === 200, `GET ${ep.path}/1`, '(状态 200)');
  }

  // ---- 4. OpenAPI 规范验证（零数据污染） ----
  console.log('\n--- 4. OpenAPI 规范 ---');
  {
    const spec = await apiGet('/v3/api-docs', adminToken);
    if (spec.status === 200 && spec.body?.paths) {
      const paths = Object.keys(spec.body.paths);
      const expectedEndpoints = [
        '/api/v1/admins', '/api/v1/users', '/api/v1/cinemas', '/api/v1/films',
        '/api/v1/actors', '/api/v1/areas', '/api/v1/types', '/api/v1/notices',
        '/api/v1/rooms', '/api/v1/records', '/api/v1/orders', '/api/v1/marks',
        '/api/v1/videos',
      ];
      for (const ep of expectedEndpoints) {
        assert(paths.includes(ep), `OpenAPI 包含 ${ep}`);
        const methods = Object.keys(spec.body.paths[ep]).join(',').toUpperCase();
        assert(methods.includes('POST'), `  → ${ep} 支持 POST (${methods})`);
      }
      assert(paths.includes('/api/v1/auth/login'), 'OpenAPI 包含 /api/v1/auth/login');
      assert(paths.includes('/api/v1/films/search'), 'OpenAPI 包含 /api/v1/films/search');
      assert(paths.includes('/api/v1/files/upload'), 'OpenAPI 包含 /api/v1/files/upload');
    } else {
      assert(false, '获取 OpenAPI spec', `(状态 ${spec.status})`);
    }
  }

  // ---- 5. 业务接口 ----
  console.log('\n--- 5. 业务接口 ---');

  {
    // 5a. 电影搜索
    const r = await apiGet('/api/v1/films/search?title=电影');
    if (r.status === 200 && r.body.code === '200') {
      assert(Array.isArray(r.body.data), 'GET /api/v1/films/search', '(返回数组)');
    } else {
      assert(false, 'GET /api/v1/films/search', `(${r.status})`);
    }
  }

  {
    // 5b. 按影院查电影
    const r = await apiGet('/api/v1/films/by-cinema?cinemaId=1');
    assert(r.status === 200, 'GET /api/v1/films/by-cinema', '(状态 200)');
  }

  {
    // 5c. 票房排行榜
    const r = await apiGet('/api/v1/films/box-office/top');
    if (r.status === 200 && r.body.code === '200') {
      assert(Array.isArray(r.body.data), 'GET /api/v1/films/box-office/top', '(返回数组)');
    }
  }

  {
    // 5d. 评分排行榜
    const r = await apiGet('/api/v1/films/mark/top?topNum=5');
    if (r.status === 200 && r.body.code === '200') {
      assert(Array.isArray(r.body.data), 'GET /api/v1/films/mark/top', '(返回数组)');
    }
  }

  {
    // 5e. 年份列表
    const r = await apiGet('/api/v1/auth/years');
    if (r.status === 200 && r.body.code === '200') {
      assert(Array.isArray(r.body.data), 'GET /api/v1/auth/years', '(返回数组)');
    }
  }

  {
    // 5f. 订单创建（验证端点存在）
    const r = await apiPost('/api/v1/orders/create', { recordId: 0, seat: '' }, adminToken);
    // 参数无效但端点应存在，返回 4xx 而非 404
    assert(r.status !== 404, 'POST /api/v1/orders/create', `(端点存在，返回 ${r.status})`);
  }

  {
    // 5g. 订单取消（验证端点存在）
    if (adminToken) {
      const r = await apiPut('/api/v1/orders/0/cancel', {}, adminToken);
      assert(r.status !== 404, 'PUT /api/v1/orders/:id/cancel', `(端点存在，返回 ${r.status})`);
    }
  }

  {
    // 5h. 文件上传（验证端点存在）
    if (adminToken) {
      const formData = new FormData();
      const r = await fetch(`${API}/api/v1/files/upload`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${adminToken}` },
        body: formData,
      });
      // 无文件上传应返回 4xx 而非 404
      assert(r.status !== 404, 'POST /api/v1/files/upload', `(端点存在，返回 ${r.status})`);
    }
  }

  // ---- 6. 负面测试 ----
  console.log('\n--- 6. 负面测试 ---');

  {
    // 6a. 无 token 访问受保护资源
    const r = await apiGet('/api/v1/admins/page');
    assert(r.status === 401 || r.body.code !== '200',
      '无 token 访问受保护资源', `(返回 ${r.status})`);
  }

  {
    // 6b. 无效 token
    const r = await apiGet('/api/v1/admins/page', 'invalid-token');
    assert(r.status === 401 || r.body.code !== '200',
      '无效 token 访问受保护资源', `(返回 ${r.status})`);
  }

  {
    // 6c. 不存在路由 → 404（需带 token 绕过拦截器）
    const r = await apiGet('/api/v1/nonexistent', adminToken);
    assert(r.status === 404,
      'GET /api/v1/nonexistent', `(返回 ${r.status})`);
  }

  // ---- 7. 清理 ----
  console.log('\n--- 7. 清理测试数据 ---');
  for (const record of createdUserIds) {
    const r = await apiDelete(`/api/v1/users/${record.id}`, adminToken);
    if (r.status === 200) {
      console.log(`  🗑️ 清理 ${record.type} id=${record.id}`);
    }
  }

  // ---- 结果汇总 ----
  const total = passed + failed;
  console.log(`\n===================================`);
  console.log(`  契约测试完成: ${passed}/${total} 通过`);
  console.log(`===================================\n`);

  process.exit(failed > 0 ? 1 : 0);
}

run().catch(err => {
  console.error('契约测试异常:', err);
  process.exit(1);
});
