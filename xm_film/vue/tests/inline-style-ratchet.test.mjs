import assert from 'node:assert/strict'
import { readdir, readFile } from 'node:fs/promises'
import test from 'node:test'

const SRC = new URL('../src/', import.meta.url)

/**
 * 阶段 3「内联样式消解」的终态守卫（规范 §11.2：禁止内联 style）。
 *
 * 本测试曾是递减棘轮（每个文件登记上限，逐页下调，全部归零后再改断言）。
 * 现在 src 下已全部清零，棘轮收敛为终态断言：任何 .vue 出现 `style="` 即失败，
 * 含新增页面。需要按运行时数据改样式的，请用 `:class` + 语义类名，不要用 `:style`。
 */
const listVueFiles = async (directory, prefix = '') => {
  const entries = await readdir(directory, { withFileTypes: true })
  const found = []
  for (const entry of entries) {
    const relative = `${prefix}${entry.name}`
    if (entry.isDirectory()) {
      found.push(...(await listVueFiles(new URL(`${entry.name}/`, directory), `${relative}/`)))
    } else if (entry.name.endsWith('.vue')) {
      found.push(relative)
    }
  }
  return found.sort()
}

test('src 下无任何内联 style（规范 §11.2）', async () => {
  const files = await listVueFiles(SRC)
  assert.ok(files.length > 0, '未扫描到任何 .vue 文件，路径可能有误')

  const offenders = []
  for (const relative of files) {
    const source = await readFile(new URL(relative, SRC), 'utf8')
    const count = (source.match(/style="/g) || []).length
    if (count > 0) {
      offenders.push(`${relative}: ${count} 处`)
    }
  }

  assert.deepEqual(
    offenders,
    [],
    `以下文件仍含内联 style，请提取为类名 + 令牌：\n${offenders.join('\n')}`,
  )
})

test('扫描覆盖面包含全部页面目录', async () => {
  const files = await listVueFiles(SRC)
  for (const directory of ['components/', 'views/front/', 'views/back/', 'views/manage/']) {
    assert.ok(
      files.some((file) => file.startsWith(directory)),
      `未扫描到 ${directory} 下的 .vue，递归遍历可能失效`,
    )
  }
})
