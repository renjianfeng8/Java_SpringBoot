<template>
  <div class="profile-wrapper">
    <div class="card profile-card">

      <el-form ref="formRef" :rules="data.rules" :model="data.form" class="dialog-form" label-width="100px"
               status-icon @submit.prevent>
        <el-form-item label="原密码" prop="password">
          <el-input show-password v-model="data.form.password" autocomplete="off" placeholder="请输入原密码" @keyup.enter="updatePassword"/>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input show-password v-model="data.form.newPassword" autocomplete="off" placeholder="请输入新密码" @keyup.enter="updatePassword"/>
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword" required>
          <el-input show-password v-model="data.form.confirmPassword" autocomplete="off" placeholder="请再次确认新密码" @keyup.enter="updatePassword"/>
        </el-form-item>
        <div class="form-actions">
          <el-button @click="updatePassword" type="primary" class="submit-button" >立即修改</el-button>
        </div>
      </el-form>

    </div>
  </div>
</template>

<script setup>

import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";
import { AUTH_API } from '@/constants';
import { useAuth } from "@/composables/useAuth";

const router = useRouter()
// 登录态的唯一来源是 useAuth 的模块级 ref（规则 76），storage 只是它的持久化副本
const { user, logout } = useAuth()

const formRef = ref()

const validatePass = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次确认新密码'))
  } else if (value !== data.form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const data = reactive({
  form: {},
  rules: {
    password: [
      {required: true, message: '请输入原密码', trigger: 'blur'}
    ],
    newPassword: [
      {required: true, message: '请输入新密码', trigger: 'blur'}
    ],
    confirmPassword: [
      { validator: validatePass, trigger: 'blur'}
    ]
  }
})

const updatePassword = () => {
  data.form.id = user.value?.id
  data.form.role = user.value?.role
  formRef.value?.validate((valid) =>{
    if (valid) {
      request.put(AUTH_API.PASSWORD, data.form).then(res => {
        if (res.code === '200') {
          ElMessage.success('修改成功')
          // 改密后必须走 useAuth.logout()：它同时清单例与 storage 副本。
          // 旧写法只调 clearStoredUser()（只清副本），内存里仍持 token、外壳继续渲染成
          // 已登录 —— 原先那句 location.href 整页重载正是用来盖住这一点的（规则 76）。
          // 站内跳转一律 router.push，不用 window.location.href（规则 77）。
          logout()
          router.push('/login')
        } else {
          ElMessage.error(res.msg)
        }
      })
    }
  })
}

</script>
