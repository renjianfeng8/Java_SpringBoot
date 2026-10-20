<template>

  <div class="profile-wrapper">
    <div class="card profile-card">

      <el-form ref="formRef" :rules="data.rules" :model="data.form" class="dialog-form" label-width="80px"
               status-icon @submit.prevent>

        <el-form-item label="用户名" prop="username">
          <el-input disabled v-model="data.form.username" autocomplete="off" placeholder="请输入用户名"/>
        </el-form-item>

        <el-form-item label="名称" prop="name">
          <el-input v-model="data.form.name" autocomplete="off" placeholder="请输入名称" @keyup.enter="updateUser"/>
        </el-form-item>

        <el-form-item label="电话" prop="phone">
          <el-input v-model="data.form.phone" autocomplete="off" placeholder="请输入电话" @keyup.enter="updateUser"/>
        </el-form-item>

        <div class="form-actions">
          <el-button @click="updateUser" type="primary" class="submit-button" >更新个人信息</el-button>
        </div>
      </el-form>

    </div>
  </div>
</template>

<script setup>
import { isSuccess, API_PATHS } from '@/constants';

import { reactive, ref, watch } from "vue";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";
import { useAuth } from "@/composables/useAuth";

const { user, setUser } = useAuth()

const formRef = ref()
const data = reactive({
  // 资料表单只取这几个可编辑字段：token 与角色属于会话，不能随 PUT 一起提交
  form: {
    id: user.value?.id ?? null,
    username: user.value?.username || '',
    name: user.value?.name || '',
    phone: user.value?.phone || '',
  },
  // 已提交过的用户快照（JSON），用于判断「服务端权威资料回来后要不要覆盖表单」
  base: '',
  rules: {
    username: [
      { required: true ,message: '请输入账号', trigger: 'blur'}
    ],
    name: [
      { required: true ,message: '请输入名称', trigger: 'blur'}
    ]
  }
})

function snapshot(form) {
  return JSON.stringify({ id: form.id, username: form.username, name: form.name, phone: form.phone })
}

function fillForm(current) {
  data.form.id = current?.id ?? null
  data.form.username = current?.username || ''
  data.form.name = current?.name || ''
  data.form.phone = current?.phone || ''
}

// 冷加载时表单先按存储快照渲染，/me 的权威资料随后到达。
// 只有「用户没动过表单」（当前值仍等于已提交快照）时才覆盖，避免冲掉用户刚输入的内容。
watch(user, (current) => {
  if (!current) return
  if (snapshot(data.form) === data.base) {
    fillForm(current)
    data.base = snapshot(data.form)
  }
}, { immediate: true })

/* /manage 的路由 meta.roles 只有 ADMIN（router/index.js），所以这里不存在原写法里
   那个 role === 'USER' 分支 —— 它永远不可达，且请求目标恒为 ADMINS。
   原先的 defineEmits(['updateUser']) 也没有任何消费方（外壳未监听），一并删除。 */
const updateUser = async () => {
  try {
    await formRef.value.validate()
  } catch {
    ElMessage.warning('请完成必填字段')
    return
  }
  request.put(API_PATHS.ADMINS, data.form).then(res => {
    if (isSuccess(res)) {
      ElMessage.success('更新成功')
      // 登录态只能经 useAuth 变更：只写 storage 副本不会更新内存里的
      // user，顶栏的用户名与头像要等整页刷新才变。
      setUser({ ...user.value, ...data.form })
      data.base = snapshot(data.form)
    } else {
      ElMessage.error(res.msg)
    }
  }).catch(() => {})
}

</script>
