import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

// 深拷贝默认值：defaultForm 里的数组/对象若按下标共享（如 Film 的 typeIds: []），
// 一次就地改动会渗进后续每一次新增。
function cloneDefaults(value) {
  if (Array.isArray(value)) return value.map(cloneDefaults)
  if (value && typeof value === 'object') {
    const copy = {}
    Object.keys(value).forEach((key) => { copy[key] = cloneDefaults(value[key]) })
    return copy
  }
  return value
}

export function useFormDialog(crud, options = {}) {
  const { defaultForm = {}, rules: formRules = {}, onSaved } = options
  const dialogVisible = ref(false)
  const isEdit = ref(false)
  const submitting = ref(false)
  const formRef = ref(null)
  const form = reactive(cloneDefaults(defaultForm))

  function openAdd() {
    isEdit.value = false
    // 先清掉上一次 openEdit 留下的残留键：openEdit 把整行拷进 form，而这里只合默认值，
    // Object.assign 不删键。不清的话 form.id 会带着上一行的 id 一起提交，而
    // admin / area / type / notice / video 五张表的 insert 显式写 id，
    // 「编辑 → 关闭 → 新增」就会撞主键冲突。
    Object.keys(form).forEach((key) => {
      if (!(key in defaultForm)) delete form[key]
    })
    Object.assign(form, cloneDefaults(defaultForm))
    dialogVisible.value = true
  }

  function openEdit(row) {
    isEdit.value = true
    Object.assign(form, row)
    dialogVisible.value = true
  }

  async function submit() {
    // 提交期间吞掉重复点击：双击会让无 id 的表插两条，或让显式写 id 的表撞主键
    if (submitting.value) return
    submitting.value = true
    try {
      if (!formRef.value) return
      try {
        await formRef.value.validate()
      } catch {
        ElMessage.warning('请完成必填字段')
        return
      }

      const success = isEdit.value ? await crud.update(form) : await crud.add(form)
      if (success) {
        dialogVisible.value = false
        onSaved?.()
      }
    } finally {
      submitting.value = false
    }
  }

  function close() {
    dialogVisible.value = false
    formRef.value?.resetFields()
  }

  return { dialogVisible, isEdit, submitting, formRef, form, rules: formRules, openAdd, openEdit, submit, close }
}
