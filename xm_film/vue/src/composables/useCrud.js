import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

export function useCrud(apiBase, options = {}) {
  const { defaultSort = 'id', defaultOrder = 'desc' } = options
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

  async function load() {
    loading.value = true
    error.value = ''
    try {
      const params = { pageNum: pageNum.value, pageSize: pageSize.value, ...searchForm }
      const res = await request.get(`${apiBase}/page`, { params })
      if (res.code === '200') {
        dataList.value = res.data.list || []
        total.value = res.data.total || 0
      } else {
        fail(res.msg || '加载失败')
        ElMessage.error(error.value)
      }
    } catch (e) {
      fail('数据加载失败，请稍后重试')
    } finally {
      loading.value = false
    }
  }

  async function loadAll() {
    loading.value = true
    error.value = ''
    try {
      const res = await request.get(apiBase, { params: { ...searchForm } })
      if (res.code === '200') {
        dataList.value = res.data || []
      } else {
        fail(res.msg || '加载失败')
        ElMessage.error(error.value)
      }
    } catch (e) {
      fail('数据加载失败，请稍后重试')
    } finally {
      loading.value = false
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
      const res = await request.delete(`${apiBase}/${id}`)
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
      const res = await request.delete(`${apiBase}/batch`, { data: ids })
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

  function onSearch() { pageNum.value = 1; load() }
  function onReset() { Object.keys(searchForm).forEach(k => { searchForm[k] = undefined }); pageNum.value = 1; load() }
  function onPageChange(p) { pageNum.value = p; load() }
  function onSizeChange(s) { pageSize.value = s; pageNum.value = 1; load() }
  function onSelectionChange(rows) { selectedIds.value = rows.map(r => r.id) }

  return { dataList, loading, error, pageNum, pageSize, total, searchForm, selectedIds,
           load, loadAll, add, update, del, delBatch,
           onSearch, onReset, onPageChange, onSizeChange, onSelectionChange }
}
