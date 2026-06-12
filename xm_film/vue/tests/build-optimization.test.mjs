import assert from 'node:assert/strict'
import { readdir, readFile, stat } from 'node:fs/promises'
import test from 'node:test'

const readSource = (relativePath) =>
  readFile(new URL(`../${relativePath}`, import.meta.url), 'utf8')

test('Element Plus and icons are imported on demand', async () => {
  const [main, app, manage, back] = await Promise.all([
    readSource('src/main.js'),
    readSource('src/App.vue'),
    readSource('src/views/Manage.vue'),
    readSource('src/views/Back.vue'),
  ])

  assert.doesNotMatch(main, /import\s+ElementPlus\s+from\s+['"]element-plus['"]/)
  assert.doesNotMatch(main, /element-plus\/dist\/index\.css/)
  assert.doesNotMatch(main, /import\s+\*\s+as\s+ElementPlusIconsVue/)
  assert.match(app, /ElConfigProvider/)
  assert.match(app, /element-plus\/es\/locale\/lang\/zh-cn/)
  assert.match(manage, /from\s+['"]@element-plus\/icons-vue['"]/)
  assert.match(back, /from\s+['"]@element-plus\/icons-vue['"]/)
})

test('ECharts uses modular entry points', async () => {
  const home = await readSource('src/views/manage/Home.vue')

  assert.doesNotMatch(home, /import\s+\*\s+as\s+echarts\s+from\s+['"]echarts['"]/)
  assert.match(home, /from\s+['"]echarts\/core['"]/)
  assert.match(home, /from\s+['"]echarts\/charts['"]/)
  assert.match(home, /from\s+['"]echarts\/components['"]/)
  assert.match(home, /from\s+['"]echarts\/renderers['"]/)
})

test('production JavaScript chunks stay below the Vite warning threshold', async () => {
  const assetsDirectory = new URL('../dist/assets/', import.meta.url)
  const files = (await readdir(assetsDirectory)).filter((file) => file.endsWith('.js'))
  const chunks = await Promise.all(
    files.map(async (file) => ({
      file,
      size: (await stat(new URL(file, assetsDirectory))).size,
    })),
  )
  const largestChunk = chunks.sort((left, right) => right.size - left.size)[0]

  assert.ok(
    largestChunk.size <= 500_000,
    `${largestChunk.file} is ${(largestChunk.size / 1000).toFixed(2)} kB`,
  )
})
