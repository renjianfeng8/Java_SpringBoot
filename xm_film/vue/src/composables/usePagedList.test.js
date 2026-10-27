import { beforeEach, describe, expect, it, vi } from 'vitest'
import { usePagedList } from '@/composables/usePagedList'

vi.mock('@/utils/request', () => ({ default: { get: vi.fn() } }))

function success(list, total) {
  return { code: '200', data: { list, total } }
}

function deferred() {
  let resolve
  let reject
  const promise = new Promise((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
}

describe('usePagedList', () => {
  let fetch

  beforeEach(() => {
    fetch = vi.fn()
  })

  it('loads the current page and exposes items/total', async () => {
    fetch.mockResolvedValue(success([{ id: 1 }], 42))
    const list = usePagedList({ fetch, pageSize: 12 })

    await list.load()

    expect(fetch).toHaveBeenCalledWith({ pageNum: 1, pageSize: 12 })
    expect(list.items.value).toEqual([{ id: 1 }])
    expect(list.state.total).toBe(42)
    expect(list.state.loading).toBe(false)
    expect(list.state.error).toBe('')
  })

  it('only lets the latest request land', async () => {
    const first = deferred()
    const second = deferred()
    fetch.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise)
    const list = usePagedList({ fetch })

    const firstLoad = list.load()
    const secondLoad = list.load()
    second.resolve(success([{ id: 2 }], 2))
    await secondLoad
    first.resolve(success([{ id: 1 }], 1))
    await firstLoad

    expect(list.items.value).toEqual([{ id: 2 }])
    expect(list.state.total).toBe(2)
    expect(list.state.loading).toBe(false)
  })

  it('clears rows and surfaces the backend message on business failure', async () => {
    fetch.mockResolvedValue({ code: '500', msg: '数据库开小差了' })
    const list = usePagedList({ fetch })

    await list.load()

    expect(list.items.value).toEqual([])
    expect(list.state.total).toBe(0)
    expect(list.state.error).toBe('数据库开小差了')
  })

  it('clears rows and uses the shared message on network failure', async () => {
    fetch.mockRejectedValue(new Error('network down'))
    const list = usePagedList({ fetch })

    await list.load()

    expect(list.items.value).toEqual([])
    expect(list.state.error).toBe('数据加载失败，请稍后重试')
    expect(list.state.loading).toBe(false)
  })

  it('falls back to page 1 when the last page became empty', async () => {
    fetch
      .mockResolvedValueOnce(success([], 20))
      .mockResolvedValueOnce(success([{ id: 9 }], 20))
    const list = usePagedList({ fetch })
    list.state.pageNum = 3

    await list.load()

    expect(fetch).toHaveBeenNthCalledWith(1, { pageNum: 3, pageSize: 10 })
    expect(fetch).toHaveBeenNthCalledWith(2, { pageNum: 1, pageSize: 10 })
    expect(list.state.pageNum).toBe(1)
    expect(list.items.value).toEqual([{ id: 9 }])
  })

  it('reload resets to page 1 and keeps the new filters', async () => {
    fetch.mockResolvedValue(success([], 0))
    const list = usePagedList({ fetch })
    list.state.pageNum = 4

    await list.reload({ status: '已上映' })

    expect(fetch).toHaveBeenLastCalledWith({ pageNum: 1, pageSize: 10, status: '已上映' })
    expect(list.state.pageNum).toBe(1)
  })

  it('supports custom pickers and page size changes', async () => {
    fetch.mockResolvedValue({ code: '200', data: { items: [{ id: 3 }], count: 88 } })
    const list = usePagedList({
      fetch,
      pageSize: 10,
      pick: (res) => ({ list: res.data.items, total: res.data.count }),
    })

    await list.load()
    expect(list.items.value).toEqual([{ id: 3 }])
    expect(list.state.total).toBe(88)

    await list.setPageSize(50)
    expect(fetch).toHaveBeenLastCalledWith({ pageNum: 1, pageSize: 50 })
  })
})
