<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-form-wrapper">
        <div class="login-title">欢迎登录电影购票系统</div>
        <el-form ref="formRef" :rules="data.rules" :model="data.form" class="login-form">
          <el-form-item prop="username">
            <el-input v-model="data.form.username" placeholder="请输入账号" prefix-icon="User"></el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input show-password v-model="data.form.password" placeholder="请输入密码" prefix-icon="Lock"></el-input>
          </el-form-item>
          <el-form-item prop="role">
            <el-select v-model="data.form.role" class="login-field">
              <el-option value="ADMIN" label="管理员"></el-option>
              <el-option value="CINEMA" label="电影院"></el-option>
              <el-option value="USER" label="用户"></el-option>
            </el-select>
          </el-form-item>
          <div class="login-actions">
            <el-button @click="login" type="primary" class="login-submit" size="large">登 录</el-button>
          </div>
          <div class="login-hint">还没有账号? 请<a href="/register" class="login-hint__link">注 册</a></div>
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
import { ElMessageBox } from "element-plus"

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

/* 登录框居中：绝对定位 50% + 回移自身 50% */
.login-box {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 40%;
  max-width: 400px;
  display: flex;
  justify-content: center;
  padding: var(--space-64);
}

/* 图片上的半透明面板，见 tokens.scss 的 --surface-glass */
.login-form-wrapper {
  width: 100%;
  max-width: 380px;
  padding: var(--space-40);
  border-radius: var(--el-border-radius-base);
  background-color: var(--surface-glass);
  box-shadow: var(--el-box-shadow-light);
}

.login-title {
  margin-bottom: var(--space-20);
  font-size: var(--fs-xl);
  font-weight: var(--fw-bold);
  text-align: center;
  color: var(--el-color-primary);
}

.login-form {
  width: 380px;
}

.login-field {
  width: 100%;
}

.login-actions {
  margin-bottom: var(--space-16);
}

.login-submit {
  width: 100%;
}

.login-hint {
  text-align: right;
}

/* 文字型链接：hover 用主色（规范 §3.3） */
.login-hint__link {
  color: var(--el-color-primary);
  text-decoration: none;
}

.login-footer {
  position: absolute;
  bottom: var(--space-20);
  left: 0;
  right: 0;
  padding: var(--space-12);
  text-align: center;
  font-size: var(--fs-xs);
  color: var(--dark-text-secondary);
}
</style>
