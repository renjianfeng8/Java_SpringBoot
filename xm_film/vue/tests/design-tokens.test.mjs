import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const read = (relativePath) =>
  readFile(new URL(`../${relativePath}`, import.meta.url), 'utf8')

// §2.1 / §2.2 / §2.3 / §2.5 / §2.7 / §2.8 / §2.11 —— EP 未产出的部分，必须由 tokens.scss 补齐
const REQUIRED_TOKENS = [
  '--fs-xs', '--fs-sm', '--fs-base', '--fs-md', '--fs-lg', '--fs-xl',
  '--fs-2xl', '--fs-3xl', '--fs-4xl', '--fs-5xl',
  '--lh-base', '--lh-loose',
  '--fw-regular', '--fw-medium', '--fw-bold',
  '--space-4', '--space-8', '--space-12', '--space-16', '--space-20',
  '--space-24', '--space-32', '--space-40', '--space-48', '--space-64',
  '--dark-bg', '--dark-bg-hero', '--dark-text', '--dark-text-secondary',
  '--dark-text-muted', '--dark-text-faint', '--dark-divider',
  '--color-rating', '--color-rating-text',
  '--color-rank-1', '--color-rank-2', '--color-rank-3',
  '--color-seat-available', '--color-seat-taken', '--color-seat-selected', '--color-seat-mine',
  '--color-on-accent', '--dark-bg-video', '--surface-glass', '--overlay-mask',
  '--shadow-edge', '--color-brand',
]

// §3.4 —— 与 EP 实际产出的 primary 变量族一一对应，多写即为死代码
const THEME_FRONT_VARS = [
  '--el-color-primary',
  '--el-color-primary-light-3',
  '--el-color-primary-light-5',
  '--el-color-primary-light-7',
  '--el-color-primary-light-8',
  '--el-color-primary-light-9',
  '--el-color-primary-dark-2',
  '--el-color-primary-rgb',
]

// §3.5 —— 前台文字红 #BF352D 的派生族，按 EP 公式算得
const THEME_FRONT_RAMP = {
  '--el-color-primary': '#BF352D',
  '--el-color-primary-light-3': '#D2726C',
  '--el-color-primary-light-5': '#DF9A96',
  '--el-color-primary-light-7': '#ECC2C0',
  '--el-color-primary-light-8': '#F2D7D5',
  '--el-color-primary-light-9': '#F9EBEA',
  '--el-color-primary-dark-2': '#992A24',
  '--el-color-primary-rgb': '191, 53, 45',
  '--color-brand': '#ef4238',
}

// §2.5 —— 功能色重定值，EP 原值实测全部不达 AA
const FUNCTIONAL_COLORS = {
  primary: '#165DFF',
  success: '#007E1D',
  warning: '#B35800',
  danger: '#C43232',
  info: '#73767A',
}

const luminance = (hexColor) => {
  const s = hexColor.replace('#', '')
  const [r, g, b] = [0, 2, 4].map((i) => {
    const c = parseInt(s.slice(i, i + 2), 16) / 255
    return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4)
  })
  return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

const contrast = (foreground, background) => {
  const [hi, lo] = [luminance(foreground), luminance(background)].sort((a, b) => b - a)
  return (hi + 0.05) / (lo + 0.05)
}

test('tokens.scss 在 :root 声明 §2 全部基础层令牌', async () => {
  const tokens = await read('src/assets/css/tokens.scss')
  assert.match(tokens, /:root\s*\{/, 'tokens.scss 缺少 :root 块')
  for (const token of REQUIRED_TOKENS) {
    assert.ok(tokens.includes(`${token}:`), `${token} 未在 tokens.scss 声明`)
  }
})

test('theme-front 只覆写 EP 实际产出的 primary 变量', async () => {
  const tokens = await read('src/assets/css/tokens.scss')
  const block = tokens.match(/html\.theme-front\s*\{([\s\S]*?)\}/)
  assert.ok(block, 'tokens.scss 缺少 html.theme-front 块')
  const declared = [...new Set(
    [...block[1].matchAll(/(--el-color-primary[a-z0-9-]*)\s*:/g)].map((m) => m[1]),
  )].sort()
  assert.deepEqual(declared, [...THEME_FRONT_VARS].sort())
})

test('theme-front 派生族与规范 §3.5 逐一一致', async () => {
  const tokens = await read('src/assets/css/tokens.scss')
  const block = tokens.match(/html\.theme-front\s*\{([\s\S]*?)\}/)[1]
  for (const [token, value] of Object.entries(THEME_FRONT_RAMP)) {
    assert.ok(
      block.includes(`${token}: ${value};`),
      `${token} 应为 ${value}（§3.5 按 EP 公式派生值）`,
    )
  }
})

test('index.scss 的 EP 色板覆写全部取 §2.5 的 AA 值', async () => {
  const scss = await read('src/assets/css/index.scss')
  for (const [name, value] of Object.entries(FUNCTIONAL_COLORS)) {
    const pattern = new RegExp(`'${name}':\\s*\\(\\s*'base':\\s*${value}\\s*\\)`, 'i')
    assert.match(scss, pattern, `${name} 未取 §2.5 的 AA 值 ${value}`)
  }
  assert.doesNotMatch(scss, /8438e1/i, 'index.scss 仍残留游离紫')
})

test('承载文字的颜色全部达 WCAG AA 4.5:1（§8.1）', async () => {
  // 白底上的文字
  for (const [label, color] of [
    ['后台主色蓝', '#165DFF'],
    ['前台文字红', '#BF352D'],
    ['success', '#007E1D'],
    ['warning', '#B35800'],
    ['danger', '#C43232'],
    ['info', '#73767A'],
    ['白底评分文字', '#a86400'],
  ]) {
    const ratio = contrast(color, '#ffffff')
    assert.ok(ratio >= 4.5, `${label} ${color} 白底仅 ${ratio.toFixed(2)}:1，不足 4.5:1`)
  }

  // 白字压在实色底上
  for (const [label, background] of [
    ['主按钮底', '#BF352D'],
    ['success 底', '#007E1D'],
    ['warning 底', '#B35800'],
    ['danger 底', '#C43232'],
  ]) {
    const ratio = contrast('#ffffff', background)
    assert.ok(ratio >= 4.5, `白字压 ${label} ${background} 仅 ${ratio.toFixed(2)}:1，不足 4.5:1`)
  }

  // 深色表面上的文字
  for (const [label, color] of [
    ['--dark-text', '#ffffff'],
    ['--dark-text-secondary', '#cccccc'],
    ['--dark-text-muted', '#aaaaaa'],
    ['--dark-text-faint', '#8a8a8a'],
    ['--color-rating', '#ffd700'],
  ]) {
    const ratio = contrast(color, '#1a1a1a')
    assert.ok(ratio >= 4.5, `${label} ${color} 深底仅 ${ratio.toFixed(2)}:1，不足 4.5:1`)
  }
})

test('装饰红 #ef4238 不得承载正文（§2.4 拆档约束）', async () => {
  const ratio = contrast('#ef4238', '#ffffff')
  assert.ok(ratio < 4.5, '前提变化：若 #ef4238 已达标，应重新评估 §2.4 的拆档设计')
  assert.ok(ratio >= 3, `装饰红需 ≥3:1，实测 ${ratio.toFixed(2)}:1`)
  // 深底上反而只能用装饰红，文字红会掉到 3:1 附近
  assert.ok(contrast('#BF352D', '#1a1a1a') < 4.5, '深底上不可用文字红')
})

test('前台主题 class 挂在 <html> 上并由路由切换', async () => {
  const app = await read('src/App.vue')
  assert.match(app, /documentElement/)
  assert.match(app, /theme-front/)
  assert.match(app, /startsWith\(['"]\/front['"]\)/)
})

test('global.css 承载全局重置且不再硬编码灰色', async () => {
  const css = await read('src/assets/css/global.css')
  assert.match(css, /box-sizing:\s*border-box/)
  assert.match(css, /font-family:\s*var\(--el-font-family/)
  assert.match(css, /prefers-reduced-motion/)
  assert.doesNotMatch(css, /#333\b/i, 'global.css 仍硬编码 #333')
  assert.doesNotMatch(css, /padding:\s*5px/, '.card padding 未收敛')
  assert.doesNotMatch(css, /border-radius:\s*5px/, '.card 圆角未收敛')
})

test('Front.vue 不再重复声明全局字号令牌，且已清除游离色', async () => {
  const front = await read('src/views/Front.vue')
  assert.doesNotMatch(front, /--fs-2xs/, 'Front.vue 仍声明组件级字号令牌')
  assert.doesNotMatch(front, /#409eff/i, 'Front.vue 仍残留 EP 默认蓝')
  assert.doesNotMatch(front, /#e53935/i, 'Front.vue 仍残留 Material 红')
  assert.doesNotMatch(front, /--nav-gap:\s*25px/, '--nav-gap 未收敛到 4px 网格')
})

test('后台两端不再各自维护一份外壳样式', async () => {
  const [back, manage, shared] = await Promise.all([
    read('src/views/Back.vue'),
    read('src/views/Manage.vue'),
    read('src/assets/css/admin-layout.scss'),
  ])
  assert.match(shared, /\.manage-header\s*\{/)
  for (const [name, shell] of [['Back.vue', back], ['Manage.vue', manage]]) {
    assert.match(shell, /@use\s+['"]@\/assets\/css\/admin-layout['"]/, `${name} 未引用共用外壳样式`)
    assert.doesNotMatch(shell, /\.manage-container\s*\{/, `${name} 仍内联外壳样式`)
  }
})

test('登录 / 注册共用一份认证页外壳样式（§7.2）', async () => {
  const [login, register, shared] = await Promise.all([
    read('src/views/Login.vue'),
    read('src/views/Register.vue'),
    read('src/assets/css/auth-layout.scss'),
  ])
  assert.match(shared, /\.auth-container\s*\{/, 'auth-layout.scss 缺少 .auth-container')
  assert.match(shared, /\.auth-card\s*\{/, 'auth-layout.scss 缺少 .auth-card')
  for (const [name, page] of [['Login.vue', login], ['Register.vue', register]]) {
    assert.match(page, /@use\s+['"]@\/assets\/css\/auth-layout['"]/, `${name} 未引用共用认证页样式`)
    assert.doesNotMatch(page, /\.auth-(container|card)\s*\{/, `${name} 仍内联认证页外壳样式`)
  }
})

test('认证页卡片宽度单一来源，容器不裁切（§7.2）', async () => {
  const shared = await read('src/assets/css/auth-layout.scss')

  // 卡片宽度只能由 .auth-card 的 max-width 决定；内部控件一律 100% 跟随父级
  assert.match(shared, /max-width:\s*380px/, 'auth-layout.scss 未定义卡片宽度来源')
  assert.doesNotMatch(
    shared,
    /overflow:\s*hidden/,
    '认证页容器用 overflow: hidden 裁切内容，矮视口下无法滚动',
  )

  for (const file of ['src/views/Login.vue', 'src/views/Register.vue']) {
    const source = await read(file)
    // 负向后行断言排除 max-width / min-width
    assert.doesNotMatch(
      source,
      /(?<!-)\bwidth:\s*\d+px/,
      `${file} 在卡片内部写死了宽度，应交给 .auth-card 的 max-width 决定`,
    )
  }
})

test('认证页表单具备可访问名称、错误图标与回车提交（§8.2 / §六）', async () => {
  for (const file of ['src/views/Login.vue', 'src/views/Register.vue']) {
    const source = await read(file)

    // 前缀图标必须绑定组件：字符串写法不会被解析（图标集未全局注册）
    assert.doesNotMatch(
      source,
      /(?<!:)prefix-icon="/,
      `${file} 仍用字符串 prefix-icon，图标不会渲染，应改为 :prefix-icon="User"`,
    )

    assert.match(source, /label-position="top"/, `${file} 未使用顶部可见标签（§8.2）`)
    assert.match(source, /status-icon/, `${file} 未开启 status-icon，错误反馈缺图标（§六）`)
    assert.match(source, /@keyup\.enter=/, `${file} 未支持回车提交（§8.2）`)

    const items = [...source.matchAll(/<el-form-item\b([^>]*)>/g)].map((m) => m[1])
    assert.ok(items.length >= 3, `${file} 表单项数量异常：${items.length}`)
    for (const attrs of items) {
      assert.match(attrs, /\slabel="[^"]+"/, `${file} 存在无可见 label 的表单项：${attrs.trim()}`)
      assert.match(attrs, /\sprop="[^"]+"/, `${file} 存在无 prop 的表单项：${attrs.trim()}`)
    }
  }
})
