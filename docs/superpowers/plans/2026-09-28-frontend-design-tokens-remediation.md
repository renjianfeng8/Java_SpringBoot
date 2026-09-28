# 前端设计规范整改 · 计划 1：令牌基础设施与色板分端

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `xm_film/vue` 落地《标准前端视觉与交互设计规范.md》§2 的全局令牌层与前台红色主题，使后续所有页面整改有可引用的统一数值出处。

**Architecture:** 令牌分两类落地 —— 基础层里 **EP 已经产出的**（颜色 / 圆角 / 阴影 / 层级 / 中性色）直接复用其 `--el-*` 令牌，本计划不重复定义；EP **没有的**（字号阶梯 / 行高 / 字重 / 间距网格）写进新建的 `tokens.scss` 并声明在 `:root`。前台品牌红通过 `html.theme-front` 覆写 EP 的 primary 变量族，由 `App.vue` 按路由前缀切换 class —— **必须挂在 `<html>` 上**，因为 EP 的 `ElSelect` / `ElTooltip` / `ElMessage` 会把 DOM teleport 到 `body`，挂在布局容器上的变量传不进这些子树。

**Tech Stack:** Vue 3.5.13 · Vite 6.4.3 · Element Plus 2.9.11（`useSource: true` + `importStyle: 'sass'`）· Sass 1.89 · `node:test`（既有 `tests/` 目录）

---

## 范围说明

本计划是《标准前端视觉与交互设计规范.md》附录 B（24 项偏差）整改的**第 1 份计划**，只覆盖「阻断」档中属于基础设施的部分，交付后系统可正常运行、无视觉回归：

| 覆盖 | 附录 B 编号 |
|---|---|
| ✅ 本计划 | B#1 无全局令牌层 · B#4 无 `font-family` · B#5 后台两端样式重复 · B#7 `info` 游离紫 · B#10 `.card` 三值超标 · B#11 `--nav-gap` 25px · B#20 无 `prefers-reduced-motion` · B#23 `--fs-*` 作用域过窄 · B#24 未全局 `box-sizing` |
| ⛔ 后续计划 2 | B#2 内联 style（628 处）· B#3 三套第三方色板 · B#12/B#13 圆角与字号散值 · B#14 灰色偏离 · B#15/B#16/B#17 游离色与阴影 |
| ⛔ 后续计划 3 | B#8 字重 600 · B#18 图标风格 · B#19 `aria-label` · B#21 媒体查询 · B#22 栅格 |

**为什么这样切：** B#2（628 处内联 style）本身就是跨 19 个页面的大型子系统，与令牌层不是同一件事；先落令牌，计划 2 才有东西可迁移到。B#6（品牌红派生色阶）随本计划一起落，不单独成计划。

**规划期发现并已修正的规范错误**（规范文档已同步改）：
1. §2.4 原本为功能色另立 `--color-success` 等四个令牌 —— 实测 EP 已产出 `--el-color-success: #00B42A` / `--el-color-warning: #FF7D00` / `--el-color-danger: #F53F3F`，重复定义即两套来源，已改为复用 EP 令牌。
2. §3.4 原本列出 `light-1`…`light-9` 共 9 档派生色 —— 实测 EP 只产出 `light-3/5/7/8/9 + dark-2 + rgb` 七个，`light-1/2/4/6` 无产出也无引用，已删。
3. 附录 B#7 证据行号 `index.scss:7` 应为 `index.scss:6`（`'info'` 在第 6 行）。

---

## 文件结构

| 文件 | 动作 | 职责 |
|---|---|---|
| `src/assets/css/tokens.scss` | **新建** | §2 中 EP 未产出部分的令牌（字号 / 行高 / 字重 / 间距）声明于 `:root`；前台红主题块 `html.theme-front` |
| `src/assets/css/admin-layout.scss` | **新建** | 影院后台与管理后台共用的外壳样式（顶栏 / 侧栏 / 内容区 / 页脚），供两个外壳 `@use` |
| `src/assets/css/global.css` | 重写 | 全局重置（`box-sizing`）+ 基础元素样式 + `prefers-reduced-motion` 降级；只消费令牌不定义令牌 |
| `src/assets/css/index.scss` | 改 1 行 | EP Sass 变量覆写，`info` 改 `#909399` |
| `src/main.js` | 改 import | 引入 `tokens.scss`（**一次**） |
| `src/App.vue` | 加 watch | 按 `route.path` 前缀切换 `html.theme-front` |
| `src/views/Back.vue` | 改 | 删除重复样式块，改 `@use` 共用外壳样式；`.back-footer` → `.admin-footer` |
| `src/views/Manage.vue` | 改 | 同上；`.manage-footer` → `.admin-footer` |
| `src/views/Front.vue` | 改 | `--fs-*` 与 `--nav-gap` 收敛；清除 `#409eff` / `#e53935` 游离色 |
| `tests/design-tokens.test.mjs` | **新建** | 令牌合规校验（`node:test`，与既有 `tests/build-optimization.test.mjs` 同构） |
| `CLAUDE.md` | 改 | 目录树与「相关文档」补 `tokens.scss` / `admin-layout.scss`（文档链要求） |

> **关键约束：`tokens.scss` 不可被 `index.scss` `@use`。** `vite.config.js` 的 `css.preprocessorOptions.scss.additionalData` 会把 `@use "@/assets/css/index.scss" as *;` 注入**每一个** SCSS 块；若 `index.scss` 再 `@use tokens.scss`，令牌 CSS 会被注入到所有 SCSS 文件里重复产出。因此 `tokens.scss` 只经 `main.js` 引入一次。

---

## Task 1: 令牌合规校验（测试先行）

**Files:**
- Create: `tests/design-tokens.test.mjs`

- [ ] **Step 1: 写测试**

创建 `tests/design-tokens.test.mjs`，逐条断言本计划的每一项交付。此刻全部断言都应失败（文件尚未建立）。

```js
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const read = (relativePath) =>
  readFile(new URL(`../${relativePath}`, import.meta.url), 'utf8')

// §2.1 / §2.2 / §2.3 / §2.5 —— EP 未产出的部分，必须由 tokens.scss 补齐
const REQUIRED_TOKENS = [
  '--fs-xs', '--fs-sm', '--fs-base', '--fs-md', '--fs-lg', '--fs-xl',
  '--fs-2xl', '--fs-3xl', '--fs-4xl', '--fs-5xl', '--fs-6xl',
  '--lh-tight', '--lh-base', '--lh-loose',
  '--fw-regular', '--fw-medium', '--fw-bold',
  '--space-4', '--space-8', '--space-12', '--space-16', '--space-20',
  '--space-24', '--space-32', '--space-40', '--space-48', '--space-64',
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
  assert.match(block[1], /--el-color-primary:\s*#ef4238/i)
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

test('index.scss 修正游离的 info 色', async () => {
  const scss = await read('src/assets/css/index.scss')
  assert.match(scss, /'info':\s*\(\s*'base':\s*#909399\s*\)/i)
  assert.doesNotMatch(scss, /8438e1/i)
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
```

- [ ] **Step 2: 运行测试确认全部失败**

Run: `cd xm_film/vue && node --test tests/design-tokens.test.mjs`
Expected: FAIL —— `tokens.scss` 与 `admin-layout.scss` 均不存在，报 `ENOENT`。

- [ ] **Step 3: 提交**

```bash
git add xm_film/vue/tests/design-tokens.test.mjs
git commit -m "test: 新增前端设计令牌合规校验（当前全红）"
```

---

## Task 2: 落地 tokens.scss

**Files:**
- Create: `xm_film/vue/src/assets/css/tokens.scss`

- [ ] **Step 1: 创建文件**

```scss
/* 设计令牌总表 —— 唯一权威数值出处
 * 数值口径见《标准前端视觉与交互设计规范.md》§2。
 *
 * 只声明 EP 未产出的部分：字号阶梯 / 行高 / 字重 / 间距网格。
 * 颜色、圆角、阴影、层级、中性色 EP 已产出 --el-* 令牌（构建产物实测），
 * 一律直接引用，此处不重复定义。
 *
 * 由 main.js 引入一次。不可被 index.scss @use ——
 * vite.config.js 的 additionalData 会把 index.scss 注入每个 SCSS 块，
 * 那样令牌 CSS 会被重复产出到所有文件。
 */

:root {
  /* §2.1 字号（rem，基于根字号 16px；禁止零散 px 字号） */
  --fs-xs: 0.75rem;    /* 12px 辅助文字 / 标签 / 表单提示 */
  --fs-sm: 0.8125rem;  /* 13px 表格单元格 / 紧凑控件 */
  --fs-base: 0.875rem; /* 14px 正文基准 / 按钮 / 输入框 */
  --fs-md: 1rem;       /* 16px 模块小标题 / 弹窗标题 */
  --fs-lg: 1.125rem;   /* 18px 卡片标题 / 页面次级标题 */
  --fs-xl: 1.25rem;    /* 20px 区块大标题 */
  --fs-2xl: 1.5rem;    /* 24px 页面主标题 */
  --fs-3xl: 1.75rem;   /* 28px 前台页面大标题 */
  --fs-4xl: 2rem;      /* 32px 前台营销标题 */
  --fs-5xl: 2.25rem;   /* 36px 前台数据大字 */
  --fs-6xl: 3rem;      /* 48px 前台首屏主标题 */

  /* §2.2 行高 */
  --lh-tight: 1.3; /* 标题 ≥18px */
  --lh-base: 1.5;  /* 正文 14 / 16px */
  --lh-loose: 1.4; /* 辅助小字 ≤13px */

  /* §2.3 字重（仅三档）—— 禁用 600：中文字体栈的 Microsoft YaHei 只有
   * 400 / 700，写 600 会触发浏览器伪粗体，渲染比 700 更脏 */
  --fw-regular: 400;
  --fw-medium: 500; /* 仅用于拉丁字母与数字 */
  --fw-bold: 700;

  /* §2.5 间距（4px 原子网格；令牌名即数值） */
  --space-4: 4px;
  --space-8: 8px;
  --space-12: 12px;
  --space-16: 16px;
  --space-20: 20px;
  --space-24: 24px;
  --space-32: 32px;
  --space-40: 40px;
  --space-48: 48px;
  --space-64: 64px;
}

/* §3.2 / §3.4 前台主题：品牌红 #ef4238
 *
 * 必须挂在 html 上，不能只加在前台布局容器上：EP 的
 *   ElSelect 下拉 / ElTooltip  —— teleported: true      → 移到 body
 *   ElMessage / ElNotification —— appendTo: document.body → 移到 body
 * 都脱离布局容器子树、不继承容器上的变量。其中 ElMessage 正是 main.js 全局错误处理在用的组件。
 * 挂在 html 上的变量向下继承到 body 及全部子孙，一次性覆盖所有 EP 组件。
 *
 * 覆写集合与 EP 实际产出的变量名一一对应：EP 只产出
 * light-3 / light-5 / light-7 / light-8 / light-9 / dark-2 / -rgb，
 * 不存在 light-1 / -2 / -4 / -6，写了也没有组件会读。 */
html.theme-front {
  --el-color-primary: #ef4238;
  --el-color-primary-light-3: #F47B74; /* hover */
  --el-color-primary-light-5: #F7A19C; /* disabled */
  --el-color-primary-light-7: #FAC6C3;
  --el-color-primary-light-8: #FCD9D7;
  --el-color-primary-light-9: #FDECEB;
  --el-color-primary-dark-2: #BF352D; /* active */
  --el-color-primary-rgb: 239, 66, 56;
}
```

- [ ] **Step 2: 运行测试，确认令牌相关断言转绿**

Run: `cd xm_film/vue && node --test tests/design-tokens.test.mjs`
Expected: 前两条测试 PASS；其余仍 FAIL（`tokens.scss` 尚未被引入、其余文件未改）。

- [ ] **Step 3: 提交**

```bash
git add xm_film/vue/src/assets/css/tokens.scss
git commit -m "feat: 新增设计令牌总表 tokens.scss（字号/行高/字重/间距 + 前台红主题）"
```

---

## Task 3: 全局重置与字体继承

**Files:**
- Modify: `xm_film/vue/src/assets/css/global.css`（全文替换，现 13 行）
- Modify: `xm_film/vue/src/main.js:5`

- [ ] **Step 1: 重写 global.css**

```css
/* 全局重置 + 基础元素样式
 * 令牌定义在 tokens.scss（:root）；本文件只消费令牌，不定义令牌。 */

*,
*::before,
*::after {
  box-sizing: border-box;
}

body {
  margin: 0;
  padding: 0;
  /* 复用 EP 已产出的字体栈，让自定义元素的文字与 EP 组件文字同源 */
  font-family: var(--el-font-family);
  font-size: var(--fs-base);
  line-height: var(--lh-base);
  color: var(--el-text-color-primary);
  background-color: var(--el-bg-color-page);
}

.card {
  background-color: var(--el-bg-color);
  padding: var(--space-8);
  border-radius: var(--el-border-radius-base);
  box-shadow: var(--el-box-shadow-lighter);
}

/* §8.2 尊重系统「减少动态效果」设置 */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
    scroll-behavior: auto !important;
  }
}
```

> **刻意不做全站标题 / 列表边距清零。** 系统公告是 wangEditor 富文本（见 CLAUDE.md 评价与公告模块），一条全局 `h1..h6 { margin: 0 }` 或 `ul { list-style: none }` 会把公告正文的排版一并抹掉。这类重置按组件局部处理，不放进全局重置。

- [ ] **Step 2: 在 main.js 引入 tokens.scss**

`src/main.js` 第 5 行现为：

```js
import '@/assets/css/global.css'
```

改为（`tokens.scss` 必须排在 `global.css` 之前 —— `global.css` 消费其中的变量）：

```js
import '@/assets/css/tokens.scss'
import '@/assets/css/global.css'
```

- [ ] **Step 3: 运行测试**

Run: `cd xm_film/vue && node --test tests/design-tokens.test.mjs`
Expected: `global.css 承载全局重置…` PASS。

- [ ] **Step 4: 提交**

```bash
git add xm_film/vue/src/main.js xm_film/vue/src/assets/css/global.css
git commit -m "feat: 全局重置改为消费设计令牌，补齐字体继承与动效降级"
```

---

## Task 4: 修正游离的 info 色

**Files:**
- Modify: `xm_film/vue/src/assets/css/index.scss:6`

- [ ] **Step 1: 改值**

把 `'info': ('base': #8438e1)` 改为 `'info': ('base': #909399)`。修改后全文为：

```scss
@forward '../../../node_modules/element-plus/theme-chalk/src/common/var' with ($colors: (
    'primary': ('base': #165DFF ),
    'success': ('base': #00B42A),
    'warning': ('base': #FF7D00),
    'danger': ('base': #F53F3F),
    'info': ('base': #909399),
  ));
```

> `#909399` 是 EP `$colors.info` 的原值（`element-plus/theme-chalk/src/common/var.scss:37`）。原值 `#8438e1` 紫色既非 Arco 也非 EP 体系，属游离值。

- [ ] **Step 2: 运行测试**

Run: `cd xm_film/vue && node --test tests/design-tokens.test.mjs`
Expected: `index.scss 修正游离的 info 色` PASS。

- [ ] **Step 3: 提交**

```bash
git add xm_film/vue/src/assets/css/index.scss
git commit -m "fix: 修正游离的 info 色 #8438e1 → EP 原值 #909399"
```

---

## Task 5: 按路由切换前台红色主题

**Files:**
- Modify: `xm_film/vue/src/App.vue`

- [ ] **Step 1: 加路由监听**

整个 `src/App.vue` 替换为：

```vue
<template>
  <ElConfigProvider :locale="zhCn">
    <ErrorBoundary boundaryName="App">
      <RouterView />
    </ErrorBoundary>
  </ElConfigProvider>
</template>

<script setup>
import { watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import ErrorBoundary from '@/components/ErrorBoundary.vue'

const route = useRoute()

// 前台挂品牌红主题；后台保持构建期默认蓝（index.scss 的 $colors）。
// 必须挂在 documentElement 上 —— EP 的 ElSelect / ElTooltip / ElMessage 会 teleport 到 body，
// 挂在布局容器上的变量传不到那些子树。见 tokens.scss 的 html.theme-front 说明。
watch(
  () => route.path,
  (path) => {
    document.documentElement.classList.toggle('theme-front', path.startsWith('/front'))
  },
  { immediate: true },
)
</script>
```

> 根路径 `/` 经路由重定向到 `/front/home`（见 CLAUDE.md 页面清单），因此 `route.path` 在首屏即已是 `/front/home`，`immediate: true` 能正确命中。

- [ ] **Step 2: 运行测试**

Run: `cd xm_film/vue && node --test tests/design-tokens.test.mjs`
Expected: `前台主题 class 挂在 <html> 上并由路由切换` PASS。

- [ ] **Step 3: 提交**

```bash
git add xm_film/vue/src/App.vue
git commit -m "feat: 前台按路由挂 html.theme-front 切换品牌红主题"
```

---

## Task 6: Front.vue 移除组件级字号令牌与游离色

**Files:**
- Modify: `xm_film/vue/src/views/Front.vue`

- [ ] **Step 1: 删除组件级 `--fs-*` 声明，保留专有尺寸令牌**

`Front.vue:227-243` 现为：

```css
.front-header {
  /* Type Scale 字体标尺：浏览器根字号 16px 的固定倍数阶梯，禁止零散 px 字号
   * --fs-2xs 16×0.75 | --fs-sm 16×0.875 | --fs-md 16×1 | --fs-lg 16×1.125 | --fs-xl 16×1.25
   * 令牌声明在组件根节点上，随级联向下继承，不污染其它页面 */
  --fs-2xs: 0.75rem;
  --fs-sm: 0.875rem;
  --fs-md: 1rem;
  --fs-lg: 1.125rem;
  --fs-xl: 1.25rem;

  /* 尺寸与间距令牌：间距统一走 gap，不再逐个硬写 margin */
  --header-height: 60px;
  --header-padding-x: 20px;
  --group-gap: 16px;
  --nav-gap: 25px;
  --search-width: 200px;
  --header-min-width: 1120px;     /* 临界阈值：低于此宽度停止压缩，改为整页横向滚动 */
```

改为（字号上移为全局令牌，组件只留专有尺寸；`--nav-gap` 收敛到 4px 网格）：

```css
.front-header {
  /* 字号令牌已上移为全局令牌（tokens.scss 的 :root），组件不再重复声明。
   * 组件级只保留本组件专有的尺寸令牌，命名不得与全局令牌冲突。 */
  --header-height: 60px;
  --header-padding-x: 20px;
  --group-gap: 16px;
  --nav-gap: 24px;
  --search-width: 200px;
  --header-min-width: 1120px;     /* 临界阈值：低于此宽度停止压缩，改为整页横向滚动 */
```

- [ ] **Step 2: 改掉字号令牌的引用（同名不同义的迁移）**

新规范里 `--fs-sm` 是 **13px**，而 Front.vue 原来用的 `--fs-sm` 是 **14px**，且 `--fs-md` 是 16px。因此只有 `--fs-md` 可原样保留，`--fs-sm` 的两处引用必须改为 `--fs-base`：

| 行 | 现状 | 改为 | 原因 |
|---|---|---|---|
| `Front.vue:293` | `font-size: var(--fs-md);` | 不变 | 旧 `--fs-md` = 新 `--fs-md` = 16px |
| `Front.vue:336` | `font-size: var(--fs-md);` | 不变 | 同上 |
| `Front.vue:381` | `font-size: var(--fs-sm);` | `font-size: var(--fs-base);` | 旧 14px → 新 `--fs-base` = 14px |
| `Front.vue:480` | `font-size: var(--fs-sm);` | `font-size: var(--fs-base);` | 同上 |

> **不可整表改名。** 若把 `--fs-sm` 直接当同名令牌沿用，`.username` / `.header-link` 会从 14px 静默变成 13px。以上四处逐条按实际 rem 值核对。

- [ ] **Step 3: 清除游离色**

导航 hover 用的是 EP 默认蓝 `#409eff`，激活态用的是 Material 红 `#e53935`，两者都不是品牌红 `#ef4238` —— 前台导航当前同时存在三种红/蓝。改动如下：

`Front.vue:301`（`.nav-item:hover`）
```css
  color: #409eff;
```
改为
```css
  color: var(--el-color-primary);
```

`Front.vue:307-308`（`.active`）
```css
  color: #e53935 !important;
  font-weight: bold;
```
改为
```css
  color: var(--el-color-primary) !important;
  font-weight: var(--fw-bold);
```

`Front.vue:318`（`.active::after` 的下划线）
```css
  background-color: #e53935;
```
改为
```css
  background-color: var(--el-color-primary);
```

`Front.vue:420`（`.footer-title`）与 `Front.vue:430`（`.footer-title::after`）的 `#e53935`、`Front.vue:447`（`.footer-links li:hover`）的 `#e53935` 同样改为 `var(--el-color-primary)`。

`Front.vue:485`（`.header-link:hover`）
```css
  color: #409eff;
```
改为
```css
  color: var(--el-color-primary);
```

> 同一文件里还有 `font-weight: bold`（`Front.vue:308`、`416`）—— 已在上一步把 308 行改为数值；`416`（`.footer-title`）一并改为 `var(--fw-bold)`，因为 `bold` 关键字无法全库检索。

- [ ] **Step 4: 运行测试**

Run: `cd xm_film/vue && node --test tests/design-tokens.test.mjs`
Expected: `Front.vue 不再重复声明全局字号令牌…` PASS。

- [ ] **Step 5: 提交**

```bash
git add xm_film/vue/src/views/Front.vue
git commit -m "refactor: 前台字号令牌上移全局，导航色收敛到品牌红令牌"
```

---

## Task 7: 抽取后台两端共用的外壳样式

**Files:**
- Create: `xm_film/vue/src/assets/css/admin-layout.scss`
- Modify: `xm_film/vue/src/views/Back.vue`（样式块 162-264、模板页脚 class）
- Modify: `xm_film/vue/src/views/Manage.vue`（样式块 204-306、模板页脚 class）

**背景：** 两文件的样式块除去 `.back-footer` / `.manage-footer` 一个类名外**逐字相同**（顶栏 / 侧栏 / 内容区 / 用户区）。抽成共用文件后，两端改样式只需改一处。

- [ ] **Step 1: 创建共用外壳样式**

创建 `xm_film/vue/src/assets/css/admin-layout.scss`，内容为两端重复的那一份（已按令牌收敛 `#f5f7fa`→`--el-bg-color-page`、`15px`→`--fs-base`、`5px`→`--space-8`、圆角 `50%`→`--el-border-radius-circle`，并统一页脚类名为 `.admin-footer`）：

```scss
/* 影院后台 / 管理后台共用的外壳样式
 * 由 Back.vue 与 Manage.vue 各自 @use 进 <style scoped>，因此仍带组件作用域，
 * 不会外泄到前台。集中一份，避免双份维护。 */

.manage-container {
  width: 100%;
  min-height: 100vh;
  background-color: var(--el-bg-color-page);
  display: flex;
  flex-direction: column;
}

.manage-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 50px;
  padding: 0 var(--space-8);
  box-shadow: var(--el-box-shadow-lighter);
  z-index: var(--el-index-normal);
}

.manage-header-left {
  display: flex;
  align-items: center;
  gap: var(--space-8);
}

.logo {
  height: 25px;
  width: auto;
}

.title {
  font-size: var(--fs-base);
  margin: 0;
  font-weight: var(--fw-bold);
}

.manage-header-center {
  flex: 1;
  padding: 0 var(--space-20);
  text-align: center;
}

.manage-header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--space-8);
  cursor: pointer;
}

.avatar {
  width: 30px;
  height: 30px;
  border-radius: var(--el-border-radius-circle);
  object-fit: cover;
  border: 1px solid var(--el-border-color);
  transition: all 0.2s ease-in-out;
}

.avatar:hover {
  transform: scale(1.2);
}

.username {
  font-size: var(--fs-base);
  color: var(--el-text-color-secondary);
  font-weight: var(--fw-medium);
}

.manage-main {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.manage-main-left {
  width: 175px;
  transition: width 0.3s;
  border-right: 1px solid var(--el-border-color-lighter);
}

.manage-content {
  flex: 1;
  overflow-y: auto;
  background-color: var(--el-bg-color);
  padding: var(--space-16);
  min-height: calc(100vh - 160px);
}

.admin-footer {
  text-align: center;
  padding: var(--space-12);
  font-size: var(--fs-xs);
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
  border-top: 1px solid var(--el-border-color-lighter);
}
```

- [ ] **Step 2: Back.vue 改为引用共用样式**

把 `Back.vue:162-264` 的整个 `<style scoped>…</style>` 块替换为：

```vue
<style scoped lang="scss">
@use '@/assets/css/admin-layout' as *;
</style>
```

并把模板里的页脚 class 改名。先定位：

Run: `cd xm_film/vue && grep -n 'back-footer' src/views/Back.vue`

把命中的 `class="back-footer"` 改为 `class="admin-footer"`。

- [ ] **Step 3: Manage.vue 改为引用共用样式**

把 `Manage.vue:204-306` 的整个 `<style scoped>…</style>` 块替换为：

```vue
<style scoped lang="scss">
@use '@/assets/css/admin-layout' as *;
</style>
```

并把 `class="manage-footer"` 改为 `class="admin-footer"`：

Run: `cd xm_film/vue && grep -n 'manage-footer' src/views/Manage.vue`

- [ ] **Step 4: 运行测试**

Run: `cd xm_film/vue && node --test tests/design-tokens.test.mjs`
Expected: 全部 7 条 PASS。

- [ ] **Step 5: 提交**

```bash
git add xm_film/vue/src/assets/css/admin-layout.scss xm_film/vue/src/views/Back.vue xm_film/vue/src/views/Manage.vue
git commit -m "refactor: 抽取后台两端共用的外壳样式，消除双份维护"
```

---

## Task 8: 构建验证与文档链同步

- [ ] **Step 1: 生产构建必须通过**

Run: `cd xm_film/vue && npm run build`
Expected: 构建成功，无 Sass 报错。特别确认 `<style scoped lang="scss">` + `@use` 的写法在 `additionalData` 注入下不冲突（`additionalData` 会先注入 `@use "@/assets/css/index.scss" as *;`，再是组件的 `@use`，两者都在文件顶部，合法）。

- [ ] **Step 2: 确认主题变量确实进了产物**

Run: `cd xm_film/vue && grep -o 'theme-front[^}]*}' dist/assets/*.css | head -5`
Expected: 能在产物中看到 `html.theme-front` 规则与 `--el-color-primary: #ef4238`。

- [ ] **Step 3: 浏览器验证（用备用端口，不要占用用户的 5173）**

Run: `cd xm_film/vue && npm run dev -- --port 5174`

在浏览器打开 `http://localhost:5174/front/home`，逐项确认：

1. 顶部导航 hover 变**红**（不是蓝）—— 说明主题 class 生效。
2. 顶部导航激活项为红色下划线，颜色是 `#ef4238` 系。
3. 打开任一 `el-select` 下拉（如搜索/筛选），下拉项聚焦色为红 —— 这一条是验证 teleport 覆盖是否真的成功的关键。
4. 浏览器控制台执行 `document.documentElement.classList` → 应含 `theme-front`。
5. 切到 `http://localhost:5174/manage/home`，确认主题 class 消失且主色回到蓝 `#165DFF`。
6. 影院后台上下的顶栏高度与内容区背景与改动前一致（无视觉回归）。

> 需要登录的后台页面若因后端未启动而无法进入，只验证前台 + 公开页即可；**不要重启用户正在用的 9090 / 5173**（见项目验证约定）。

- [ ] **Step 4: 同步文档链**

`CLAUDE.md` 的前端目录树在 `assets/ # 静态资源（css / imgs）` 一行下方补充新增的两个样式文件；并在「相关文档」保持指向 `标准前端视觉与交互设计规范.md`（上一轮已加）。

- [ ] **Step 5: 确认无 EOL 污染后提交**

Run: `git diff --numstat && echo '---' && git diff --ignore-cr-at-eol --numstat`
Expected: 两个命令的行数一致（本仓库按文件保留换行符，`core.autocrlf=false`，无 `.gitattributes`；若不一致说明 Edit 改动了行尾，需还原）。

```bash
git add CLAUDE.md
git commit -m "docs: CLAUDE.md 补入 tokens.scss 与 admin-layout.scss"
```

---

## 验收标准

- [ ] `node --test tests/design-tokens.test.mjs` 7 条全绿
- [ ] `npm run build` 通过，产物含 `html.theme-front` 规则
- [ ] 前台导航 hover / 激活 / 下拉聚焦均为品牌红 `#ef4238`
- [ ] 后台主色仍为 `#165DFF`，顶栏与内容区无视觉回归
- [ ] 附录 B 中 B#1、B#4、B#5、B#7、B#10、B#11、B#20、B#23、B#24 九项可勾除（B#6 品牌红派生色阶随 Task 2 落地）
- [ ] `git diff --numstat` 与 `git diff --ignore-cr-at-eol --numstat` 行数一致

---

## 后续计划（不在本计划内）

- **计划 2 · 内联样式与第三方色板消解**（B#2 / B#3 / B#12 / B#13 / B#14 / B#15 / B#16 / B#17）：628 处内联 `style` 跨 19 页，前台 6 页（`CinemaDetail` 70 · `BuyTicket` 67 · `Home` 57 · `FilmDetail` 51 · `FilmCinema` 48 · `Rank` 38）优先。做法是把内联值按 §2 令牌表提取为类名，并用同一套 `tests/` 断言做**递减棘轮**（断言内联 `style` 计数 ≤ 当前值，每页改完下调一次）。
- **计划 3 · 可访问性与布局收口**（B#8 / B#18 / B#19 / B#21 / B#22）：`aria-label`（现 0 处）、图标风格同组统一、媒体查询断点体系。

---

## Self-Review

**1. 规范覆盖：** 本计划覆盖的 9 个附录 B 条目逐条有对应任务 —— B#1→Task 2、B#4→Task 3、B#5→Task 7、B#7→Task 4、B#10→Task 3、B#11→Task 6、B#20→Task 3、B#23→Task 6、B#24→Task 3。B#6（品牌红派生色阶）→ Task 2 的 `html.theme-front` 块。未覆盖项已在「后续计划」显式列出，非遗漏。

**2. 占位符扫描：** 无 TBD / TODO；每个改动步骤都给出了可直接粘贴的最终代码或精确的查找—替换对。Task 7 的页脚 class 改名给了 `grep -n` 定位命令而非臆测行号，因为该行号未在规划期读取。

**3. 类型与命名一致性：** 令牌名在 `tokens.scss`（Task 2）、`global.css` 消费（Task 3）、`Front.vue` 引用（Task 6）、`admin-layout.scss`（Task 7）、测试断言（Task 1）五处一致，已逐项核对：`--fs-xs/sm/base/md/lg/xl/2xl/3xl/4xl/5xl/6xl`、`--lh-tight/base/loose`、`--fw-regular/medium/bold`、`--space-4…64`。`THEME_FRONT_VARS` 八个变量名与 EP 构建产物实测输出一一对应。类名 `.admin-footer` 在 Task 7 的两个模板与共用样式中统一（替换了原 `.back-footer` / `.manage-footer`）。

**4. 已知风险与缓解：**
- *全局重置误伤富文本*：已刻意不重置标题/列表边距，理由与依据（wangEditor 公告正文）已写进 Task 3。
- *`tokens.scss` 被 additionalData 重复注入*：已在文件结构一节与 tokens.scss 头注释双重警示「不可被 index.scss `@use`」。
- *`--fs-sm` 同名不同义*：Task 6 Step 2 列出四处逐条映射表并显式警告不可整表改名（旧 14px vs 新 13px）。
- *`@use` 与 additionalData 的顺序*：Task 8 Step 1 把「构建通过」列为验证点。
