import { reactive, ref } from 'vue'
import request from '@/utils/request'
import { apiPage, isSuccess } from '@/constants'

/**
 * 只读分页列表的唯一实现（FE-49 / FE-65）。
 *
 * `useCrud` 服务后台 CRUD 页（增删改 + 选中）；前台/影院端的只读列表此前各自手写
 * pageNum / loadSeq / 越界回退，慢响应覆盖新结果这类缺陷只在一部分页面修过。
 * 这里收敛成一处，页面只声明「怎么取数」。
 *
 *   const list = usePagedList({
 *     pageSize: 12,
 *     fetch: (params) => request.get(apiPage(API_PATHS.FILMS), { params }),
 *     pick: (res) => ({ list: res.data.list, total: res.data.total }),  // 可选
 *   })
 *   list.load()                             // 按当前页码取一页
 *   list.reload({ status: '正在热映' })      // 换筛选条件并回到第 1 页
 *
 * 门面：`list.items` 是数据本身（表格直接绑它），`list.state.pageNum` 等是分页状态。
 */
export function usePagedList({ fetch, pageSize = 10, pick, errorMessage = '数据加载失败，请稍后重试' } = {}) {
  const items = ref([])
  const state = reactive({
    pageNum: 1,
    pageSize,
    total: 0,
    loading: false,
    error: '',
  })

  // 筛选条件由调用方传参而不是复制进 state：页面里往往已经是响应式对象，再存一份就是两处真相
  let params = {}
  let seq = 0

  /** 返回 Promise<void>：调用方 wait list.load() 之后再读取状态 */
  async function load() {
    const current = ++seq
    state.loading = true
    state.error = ''
    try {
      const res = await fetch({ pageNum: state.pageNum, pageSize: state.pageSize, ...params })
      if (current !== seq) return
      const page = isSuccess(res)
        ? (pick ? pick(res) : { list: res.data?.list || [], total: res.data?.total || 0 })
        : null
      if (!page) {
        state.error = res?.msg || errorMessage
        items.value = []
        state.total = 0
        return
      }
      items.value = page.list || []
      state.total = page.total || 0
      // 删空末页 / 总数变少时后端只返回空列表（PageHelper 未开 reasonable）：退回第 1 页再取
      if (items.value.length === 0 && state.total > 0 && state.pageNum > 1) {
        state.pageNum = 1
        return load()
      }
    } catch {
      // 网络/HTTP 的提示由 request.js 统一给出，这里只落错误态，避免同一次失败弹两次
      if (current !== seq) return
      state.error = errorMessage
      items.value = []
      state.total = 0
    } finally {
      if (current === seq) state.loading = false
    }
  }

  /** 换筛选条件并回到第 1 页 */
  function reload(nextParams = params) {
    params = nextParams
    state.pageNum = 1
    return load()
  }

  function setPage(page) {
    state.pageNum = page
    return load()
  }

  function setPageSize(size) {
    state.pageSize = size
    state.pageNum = 1
    return load()
  }

  return { items, state, load, reload, setPage, setPageSize }
}
