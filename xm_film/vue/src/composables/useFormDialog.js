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
    // 先清掉上一次 openEdit 留下的残留键：openEdit 把整行拷进 form，而这里只合默认值，
    // Object.assign 不删键。不清的话 form.id 会带着上一行的 id 一起提交，而
    // admin / area / type / notice / video 五张表的 insert 显式写 id，
    // 「编辑 → 关闭 → 新增」就会撞主键冲突。
    Object.keys(form).forEach((key) => {
      if (!(key in defaultForm)) delete form[key]
    })
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

  return { dialogVisible, isEdit, formRef, form, rules, openAdd, openEdit, submit, close }
}
