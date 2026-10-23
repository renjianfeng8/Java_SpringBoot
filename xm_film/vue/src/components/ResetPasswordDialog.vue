<template>
  <el-dialog :model-value="modelValue" title="重置登录密码" width="420"
             destroy-on-close @update:model-value="emit('update:modelValue', $event)" @closed="onClosed">
    <p class="reset-target">
      账号：<strong>{{ account?.username }}</strong>
      <span v-if="account?.name">（{{ account.name }}）</span>
    </p>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" @submit.prevent>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="form.newPassword" show-password autocomplete="new-password"
                  placeholder="至少 6 位" @keyup.enter="submit"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="emit('update:modelValue', false)">取 消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确 定</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { AUTH_API, isSuccess } from '@/constants'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  account: { type: Object, default: null },
  role: { type: String, required: true },
})
const emit = defineEmits(['update:modelValue'])

const formRef = ref(null)
const submitting = ref(false)
const form = reactive({ newPassword: '' })
const rules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 64, message: '新密码长度需为 6-64 位', trigger: 'blur' },
  ],
}

function onClosed() {
  form.newPassword = ''
  formRef.value?.clearValidate()
}

function submit() {
  formRef.value?.validate(async (valid) => {
    if (!valid || !props.account?.id) return
    submitting.value = true
    try {
      const res = await request.put(AUTH_API.RESET_PASSWORD, {
        id: props.account.id,
        role: props.role,
        newPassword: form.newPassword,
      })
      if (isSuccess(res)) {
        ElMessage.success('密码已重置')
        emit('update:modelValue', false)
      } else {
        ElMessage.error(res.msg || '重置失败')
      }
    } catch (e) {
      // 网络/HTTP 错误提示由 request.js 统一弹出
    } finally {
      submitting.value = false
    }
  })
}
</script>

<style scoped>
.reset-target {
  margin: 0 0 var(--space-16);
  color: var(--el-text-color-regular);
}
</style>
