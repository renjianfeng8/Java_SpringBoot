import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { apiPage, apiById, apiBatch } from '@/constants'

export function useCrud(apiBase) {
  const dataList = ref([])
  const loading = ref(false)
  const error = ref('')
  const pageNum = ref(1)
  const pageSize = ref(10)
  const total = ref(0)
  const searchForm = reactive({})
  const selectedIds = ref([])

  // 网络/HTTP 错误由 request.js 统一弹提示，这里只负责清空数据兜底，
  // 避免 reject 逃逸成未处理的 Promise rejection，并让模板能显示空态。
  function fail(message) {
    dataList.value = []
    total.value = 0
    error.value = message
  }

  // 递增序号：只让最后一次 load 的结果落地，避免慢响应覆盖新结果、提前收起 loading
  let loadSeq = 0

  async function load() {
    const seq = ++loadSeq
    loading.value = true
    error.value = ''
    try {
      const params = { pageNum: pageNum.value, pageSize: pageSize.value, ...searchForm }
      const res = await request.get(apiPage(apiBase), { params })
      if (seq !== loadSeq) return
      if (res.code === '200') {
        dataList.value = res.data.list || []
        total.value = res.data.total || 0
        // 删空当前页后回退到第 1 页：PageHelper 未开 reasonable，越界页只是返回空列表
        if (dataList.value.length === 0 && total.value > 0 && pageNum.value > 1) {
          pageNum.value = 1
          return load()
        }
      } else {
        fail(res.msg || '加载失败')
        ElMessage.error(error.value)
      }
    } catch (e) {
      // request.js 已统一弹出网络/HTTP 提示，这里仅清空数据兜底，异常不再上抛
      if (seq !== loadSeq) return
      fail('数据加载失败，请稍后重试')
    } finally {
      if (seq === loadSeq) loading.value = false
    }
  }

  async function add(row) {
    try {
      const res = await request.post(apiBase, row)
      if (res.code === '200') {
        ElMessage.success('新增成功')
        await load()
        return true
      }
      ElMessage.error(res.msg || '新增失败')
      return false
    } catch (e) {
      return false
    }
  }

  async function update(row) {
    try {
      const res = await request.put(apiBase, row)
      if (res.code === '200') {
        ElMessage.success('更新成功')
        await load()
        return true
      }
      ElMessage.error(res.msg || '更新失败')
      return false
    } catch (e) {
      return false
    }
  }

  async function del(id) {
    try {
      const res = await request.delete(apiById(apiBase, id))
      if (res.code === '200') {
        ElMessage.success('删除成功')
        await load()
        return true
      }
      ElMessage.error(res.msg || '删除失败')
      return false
    } catch (e) {
      return false
    }
  }

  async function delBatch(ids) {
    if (!ids || ids.length === 0) {
      ElMessage.warning('请先选择要删除的数据')
      return false
    }
    try {
      const res = await request.delete(apiBatch(apiBase), { data: ids })
      if (res.code === '200') {
        ElMessage.success('批量删除成功')
        await load()
        return true
      }
      ElMessage.error(res.msg || '批量删除失败')
      return false
    } catch (e) {
      return false
    }
  }

  /**
   * 单条删除的确认框。13 个列表页此前各自内联一份，文案逐页有出入
   * （「,您」半角逗号两处、「删除后无法恢复」半数缺）。收进这里统一口径。
   */
  function confirmDel(id) {
    return ElMessageBox.confirm('删除数据后无法恢复，您确认删除吗？', '删除确认', { type: 'warning' })
      .then(() => del(id))
      .catch(() => {})
  }

  /**
   * 批量删除的确认框。调用方在未选中时把按钮置为 disabled，故这里不重复
   * 「请选择数据」的守卫（delBatch 自身仍保留一条兜底）。
   */
  function confirmDelBatch() {
    const ids = selectedIds.value
    if (!ids.length) return
    return ElMessageBox.confirm(`确定删除选中的 ${ids.length} 条数据吗？删除后无法恢复`, '删除确认', { type: 'warning' })
      .then(() => delBatch(ids))
      .catch(() => {})
  }

  function onSearch() { pageNum.value = 1; load() }
  function onReset() { Object.keys(searchForm).forEach(k => { searchForm[k] = undefined }); pageNum.value = 1; load() }
  function onPageChange(p) { pageNum.value = p; load() }
  function onSizeChange(s) { pageSize.value = s; pageNum.value = 1; load() }
  function onSelectionChange(rows) { selectedIds.value = rows.map(r => r.id) }

  return { dataList, loading, error, pageNum, pageSize, total, searchForm, selectedIds,
           load, add, update, del, delBatch, confirmDel, confirmDelBatch,
           onSearch, onReset, onPageChange, onSizeChange, onSelectionChange }
}
