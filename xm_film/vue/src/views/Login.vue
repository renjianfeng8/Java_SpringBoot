<template>
  <div class="auth-container">
    <div class="auth-card">
      <div class="auth-title">欢迎登录电影购票系统</div>
      <el-form
        ref="formRef"
        :rules="data.rules"
        :model="data.form"
        label-position="top"
        status-icon
        @submit.prevent
      >
        <el-form-item label="账号" prop="username">
          <el-input
            v-model="data.form.username"
            placeholder="请输入账号"
            :prefix-icon="User"
            @keyup.enter="login"
          ></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            show-password
            v-model="data.form.password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            @keyup.enter="login"
          ></el-input>
        </el-form-item>
        <el-form-item label="登录身份" prop="role">
          <el-select v-model="data.form.role" class="auth-field">
            <el-option value="USER" label="用户"></el-option>
            <el-option value="CINEMA" label="电影院"></el-option>
            <el-option value="ADMIN" label="管理员"></el-option>
          </el-select>
        </el-form-item>
        <div class="auth-actions">
          <el-button @click="login" type="primary" class="auth-submit" size="large">登 录</el-button>
        </div>
        <div class="auth-hint">还没有账号? 请<a href="/register" class="auth-hint__link">注 册</a></div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue"
import { useRoute } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuth } from "@/composables/useAuth"
import { ElMessageBox } from "element-plus"

const route = useRoute()
const { login: authLogin } = useAuth()

function getDefaultPath(role) {
  if (role === 'USER') return '/front/home'
  if (role === 'CINEMA') return '/back/home'
  if (role === 'ADMIN') return '/manage/home'
  return '/front/home'
}

const data = reactive({
  form: {
    username: "",
    password: "",
    role: "USER"
  },
  rules: {
    username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
    role: [{ required: true, message: '请选择角色', trigger: 'change' }]
  }
})

const formRef = ref()

const login = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      const user = await authLogin(data.form)
      const redirectParam = route.query.redirect
      const redirect = (redirectParam && redirectParam.startsWith('/')) ? redirectParam : getDefaultPath(user.role)
      window.location.href = redirect
    } catch (e) {
      ElMessageBox({
        message: e.message || '登录失败，请检查账号或密码是否正确',
        title: '登录失败',
        type: 'error',
        showCancelButton: false,
        confirmButtonText: '确定'
      })
    }
  })
}
</script>

<style scoped lang="scss">
/* 几何、配色与背景图统一在 auth-layout.scss，与 Register.vue 共用同一套外壳 */
@use '@/assets/css/auth-layout' as *;
</style>
