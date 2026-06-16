<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-form-wrapper">
        <div style="margin-bottom: 20px; font-size: 20px; color: #4A90E2; font-weight: bold; text-align: center;">欢迎登录电影购票系统</div>
        <el-form ref="formRef" :rules="data.rules" :model="data.form" style="width: 380px">
          <el-form-item prop="username">
            <el-input v-model="data.form.username" placeholder="请输入账号" prefix-icon="User"></el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input show-password v-model="data.form.password" placeholder="请输入密码" prefix-icon="Lock"></el-input>
          </el-form-item>
          <el-form-item prop="role">
            <el-select v-model="data.form.role" style="width: 100%">
              <el-option value="ADMIN" label="管理员"></el-option>
              <el-option value="CINEMA" label="电影院"></el-option>
              <el-option value="USER" label="用户"></el-option>
            </el-select>
          </el-form-item>
          <div style="margin-bottom: 15px">
            <el-button @click="login" type="primary" style="width: 100%" size="large">登 录</el-button>
          </div>
          <div style="text-align: right">还没有账号? 请<a href="/register" style="color: red; text-decoration: none">注 册</a></div>
        </el-form>
      </div>
    </div>
    <div class="login-footer">
      <p>本系统为 <strong>个人学习项目</strong>，所有展示数据均为 <strong>模拟数据</strong>，不反映真实市场情况，严禁用于商业用途。</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue"
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from "@/composables/useAuth"
import { ElMessage, ElMessageBox } from "element-plus"

const router = useRouter()
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
    role: "ADMIN"
  },
  rules: {
    username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
    role: [{ required: true, message: '请选择角色', trigger: 'blur' }]
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

<style scoped>
.login-container {
  position: relative;
  height: 100vh;
  overflow: hidden;
  background-image: url('@/assets/imgs/bg_login.jpg');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
}

/* 核心修改：将登录框居中 */
.login-box {
  position: absolute;
  /* 定位到父容器50%位置 */
  top: 50%;
  left: 50%;
  /* 回移自身50%实现居中 */
  transform: translate(-50%, -50%);
  /* 保留原有宽度和内边距，确保表单显示正常 */
  width: 40%;
  max-width: 400px; /* 限制最大宽度，避免在大屏幕上过宽 */
  display: flex;
  justify-content: center;
  padding: 60px;
}

.login-form-wrapper {
  width: 100%;
  max-width: 380px;
  background-color: rgba(255, 255, 255, 0.8);
  border-radius: 10px;
  padding: 40px;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.2);
}

.login-footer {
  position: absolute;
  bottom: 20px;
  left: 0;
  right: 0;
  text-align: center;
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
  padding: 10px;
}
</style>