import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useCrud } from '@/composables/useCrud'

const { get, post, put, remove, successMessage, errorMessage, warningMessage, confirm } = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  remove: vi.fn(),
  successMessage: vi.fn(),
  errorMessage: vi.fn(),
  warningMessage: vi.fn(),
  confirm: vi.fn(),
}))

vi.mock('@/utils/request', () => ({
  default: { get, post, put, delete: remove },
}))

vi.mock('element-plus', () => ({
  ElMessage: { success: successMessage, error: errorMessage, warning: warningMessage },
  ElMessageBox: { confirm },
}))

function success(list = [], total = 0) {
  return { code: '200', data: { list, total } }
}

function deferred() {
  let resolve
  const promise = new Promise((res) => {
    resolve = res
  })
  return { promise, resolve }
}

describe('useCrud', () => {
  beforeEach(() => {
    get.mockReset()
    post.mockReset()
    put.mockReset()
    remove.mockReset()
    successMessage.mockReset()
    errorMessage.mockReset()
    warningMessage.mockReset()
    confirm.mockReset()
  })

  it('loads the page through the shared apiPage path', async () => {
    get.mockResolvedValue(success([{ id: 1 }], 9))
    const crud = useCrud('/api/v1/areas')

    await crud.load()

    expect(get).toHaveBeenCalledWith('/api/v1/areas/page', {
      params: { pageNum: 1, pageSize: 10 },
    })
    expect(crud.dataList.value).toEqual([{ id: 1 }])
    expect(crud.total.value).toBe(9)
    expect(crud.loading.value).toBe(false)
  })

  it('only lets the latest load land', async () => {
    const first = deferred()
    const second = deferred()
    get.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise)
    const crud = useCrud('/api/v1/areas')

    const firstLoad = crud.load()
    const secondLoad = crud.load()
    second.resolve(success([{ id: 2 }], 2))
    await secondLoad
    first.resolve(success([{ id: 1 }], 1))
    await firstLoad

    expect(crud.dataList.value).toEqual([{ id: 2 }])
    expect(crud.total.value).toBe(2)
    expect(crud.loading.value).toBe(false)
  })

  it('falls back to page 1 when an out-of-range page returns empty', async () => {
    get
      .mockResolvedValueOnce(success([], 30))
      .mockResolvedValueOnce(success([{ id: 7 }], 30))
    const crud = useCrud('/api/v1/areas')
    crud.pageNum.value = 4

    await crud.load()

    expect(get).toHaveBeenNthCalledWith(2, '/api/v1/areas/page', {
      params: { pageNum: 1, pageSize: 10 },
    })
    expect(crud.pageNum.value).toBe(1)
    expect(crud.dataList.value).toEqual([{ id: 7 }])
  })

  it('clears rows and surfaces the backend message on failure', async () => {
    get.mockResolvedValue({ code: '500', msg: '服务器忙' })
    const crud = useCrud('/api/v1/areas')

    await crud.load()

    expect(crud.dataList.value).toEqual([])
    expect(crud.total.value).toBe(0)
    expect(crud.error.value).toBe('服务器忙')
    expect(errorMessage).toHaveBeenCalledWith('服务器忙')
  })

  it('clears selection and table state after a successful add', async () => {
    const crud = useCrud('/api/v1/areas')
    crud.tableRef.value = { clearSelection: vi.fn() }
    crud.selectedIds.value = [1, 2]
    post.mockResolvedValue({ code: '200' })
    get.mockResolvedValue(success([], 0))

    const ok = await crud.add({ title: '华东' })

    expect(ok).toBe(true)
    expect(post).toHaveBeenCalledWith('/api/v1/areas', { title: '华东' })
    expect(crud.selectedIds.value).toEqual([])
    expect(crud.tableRef.value.clearSelection).toHaveBeenCalled()
    expect(successMessage).toHaveBeenCalledWith('新增成功')
  })

  it('returns false and shows the backend message when add fails', async () => {
    post.mockResolvedValue({ code: '400', msg: '名称不能为空' })
    const crud = useCrud('/api/v1/areas')

    const ok = await crud.add({ title: '' })

    expect(ok).toBe(false)
    expect(errorMessage).toHaveBeenCalledWith('名称不能为空')
  })

  it('refuses an empty batch delete without touching the API', async () => {
    const crud = useCrud('/api/v1/areas')

    const ok = await crud.delBatch([])

    expect(ok).toBe(false)
    expect(warningMessage).toHaveBeenCalledWith('请先选择要删除的数据')
    expect(remove).not.toHaveBeenCalled()
  })

  it('resets filters, page and selection on reset', async () => {
    get.mockResolvedValue(success([], 0))
    const crud = useCrud('/api/v1/areas')
    crud.searchForm.title = '华东'
    crud.searchForm.city = '上海'
    crud.pageNum.value = 3
    crud.selectedIds.value = [1]

    crud.onReset()
    await Promise.resolve()

    expect(crud.searchForm.title).toBeUndefined()
    expect(crud.searchForm.city).toBeUndefined()
    expect(crud.pageNum.value).toBe(1)
    expect(crud.selectedIds.value).toEqual([])
  })

  it('maps table selection changes to ids', () => {
    const crud = useCrud('/api/v1/areas')

    crud.onSelectionChange([{ id: 3 }, { id: 5 }])

    expect(crud.selectedIds.value).toEqual([3, 5])
  })
})
