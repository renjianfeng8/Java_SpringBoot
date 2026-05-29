import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

export function useCrud(apiBase, options = {}) {
  const { defaultSort = 'id', defaultOrder = 'desc' } = options
  const dataList = ref([])
  const loading = ref(false)
  const pageNum = ref(1)
  const pageSize = ref(10)
  const total = ref(0)
  const searchForm = reactive({})
  const selectedIds = ref([])

  async function load() {
    loading.value = true
    try {
      const params = { pageNum: pageNum.value, pageSize: pageSize.value, ...searchForm }
      const res = await request.get(`${apiBase}/page`, { params })
      if (res.code === '200') {
        dataList.value = res.data.list || []
        total.value = res.data.total || 0
      } else {
        ElMessage.error(res.msg || '加载失败')
      }
    } finally {
      loading.value = false
    }
  }

  async function loadAll() {
    loading.value = true
    try {
      const res = await request.get(apiBase, { params: { ...searchForm } })
      if (res.code === '200') {
        dataList.value = res.data || []
      }
    } finally {
      loading.value = false
    }
  }

  async function add(row) {
    const res = await request.post(apiBase, row)
    if (res.code === '200') {
      ElMessage.success('新增成功')
      await load()
      return true
    }
    ElMessage.error(res.msg || '新增失败')
    return false
  }

  async function update(row) {
    const res = await request.put(apiBase, row)
    if (res.code === '200') {
      ElMessage.success('更新成功')
      await load()
      return true
    }
    ElMessage.error(res.msg || '更新失败')
    return false
  }

  async function del(id) {
    const res = await request.delete(`${apiBase}/${id}`)
    if (res.code === '200') {
      ElMessage.success('删除成功')
      await load()
      return true
    }
    ElMessage.error(res.msg || '删除失败')
    return false
  }

  async function delBatch(ids) {
    if (!ids || ids.length === 0) {
      ElMessage.warning('请先选择要删除的数据')
      return false
    }
    const res = await request.delete(`${apiBase}/batch`, { data: ids })
    if (res.code === '200') {
      ElMessage.success('批量删除成功')
      await load()
      return true
    }
    ElMessage.error(res.msg || '批量删除失败')
    return false
  }

  function onSearch() { pageNum.value = 1; load() }
  function onReset() { Object.keys(searchForm).forEach(k => { searchForm[k] = undefined }); pageNum.value = 1; load() }
  function onPageChange(p) { pageNum.value = p; load() }
  function onSizeChange(s) { pageSize.value = s; pageNum.value = 1; load() }
  function onSelectionChange(rows) { selectedIds.value = rows.map(r => r.id) }

  return { dataList, loading, pageNum, pageSize, total, searchForm, selectedIds,
           load, loadAll, add, update, del, delBatch,
           onSearch, onReset, onPageChange, onSizeChange, onSelectionChange }
}
