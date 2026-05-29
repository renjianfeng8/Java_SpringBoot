import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

export function useFormDialog(crud, options = {}) {
  const { defaultForm = {}, rules: formRules = {}, onSaved } = options
  const dialogVisible = ref(false)
  const isEdit = ref(false)
  const formRef = ref(null)
  const form = reactive({ ...defaultForm })
  const rules = formRules

  function openAdd() {
    isEdit.value = false
    Object.assign(form, defaultForm)
    dialogVisible.value = true
  }

  function openEdit(row) {
    isEdit.value = true
    Object.assign(form, row)
    dialogVisible.value = true
  }

  async function submit() {
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
  }

  function close() {
    dialogVisible.value = false
    formRef.value?.resetFields()
  }

  return { dialogVisible, isEdit, formRef, form, openAdd, openEdit, submit, close }
}
