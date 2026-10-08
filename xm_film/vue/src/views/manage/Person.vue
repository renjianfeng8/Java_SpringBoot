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
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="data.form.email" autocomplete="off" placeholder="请输入邮箱" @keyup.enter="updateUser"/>
        </el-form-item>

        <div class="form-actions">
          <el-button @click="updateUser" type="primary" class="submit-button" >更新个人信息</el-button>
        </div>
      </el-form>

    </div>
  </div>
</template>

<script setup>

import { reactive, ref } from "vue";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";
import { API_PATHS } from '@/constants';
import { useAuth } from "@/composables/useAuth";

const { user, setUser } = useAuth()

const formRef = ref()
const data = reactive({
  form: { ...user.value },
  rules: {
    username: [
      { required: true ,message: '请输入账号', trigger: 'blur'}
    ],
    name: [
      { required: true ,message: '请输入名称', trigger: 'blur'}
    ],
    email: [
      { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
    ]
  }
})

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
    if (res.code === '200') {
      ElMessage.success('更新成功')
      // 登录态只能经 useAuth 变更：只写 storage 副本不会更新内存里的
      // user，顶栏的用户名与头像要等整页刷新才变。
      setUser({ ...user.value, ...data.form })
    } else {
      ElMessage.error(res.msg)
    }
  })
}

</script>
