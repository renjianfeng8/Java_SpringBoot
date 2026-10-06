<template>
  <div class="password-wrapper">

    <div class="card password-card">
      <h1 class="password-title">修改密码</h1>

      <!-- status-icon 打开「错误反馈三件套」的图标那一件；@submit.prevent 兜住原生提交，
           回车由输入框上的 @keyup.enter 触发（只挂一处，避免一次回车发两次请求，规范 §9.3） -->
      <el-form ref="formRef" :rules="data.rules" :model="data.form" class="password-form" label-width="100px"
               status-icon @submit.prevent>
        <el-form-item label="原密码" prop="password">
          <el-input show-password v-model="data.form.password" autocomplete="off" placeholder="请输入原密码"
                    @keyup.enter="updatePassword"/>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input show-password v-model="data.form.newPassword" autocomplete="off" placeholder="请输入新密码"
                    @keyup.enter="updatePassword"/>
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword" required>
          <el-input show-password v-model="data.form.confirmPassword" autocomplete="off" placeholder="请再次确认新密码"
                    @keyup.enter="updatePassword"/>
        </el-form-item>
        <div class="password-actions">
          <el-button @click="updatePassword" type="primary" class="password-submit">立即修改</el-button>
        </div>
      </el-form>

    </div>
  </div>
</template>

<script setup>

import {reactive, ref} from "vue";
import { useRouter } from "vue-router";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";
import { API_PATHS } from '@/constants';
import { getStoredUser } from "@/utils/authStorage";
import { useAuth } from "@/composables/useAuth";

const router = useRouter();
const { logout: authLogout } = useAuth();

const formRef = ref()

const validatePass = (rule,value,callback) => {
  if (!value) {
    callback(new Error('请再次确认新密码'))
  } else if (value !== data.form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const data = reactive({
  user: getStoredUser() || {},
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
  data.form.id = data.user.id
  data.form.role = data.user.role
  formRef.value.validate((valid) =>{
    if (valid) {
      request.put(`${API_PATHS.AUTH}/password`,data.form).then(res => {
        if (res.code === '200') {
          ElMessage.success('修改成功')
          // 必须走 useAuth 的 logout 而不是只清 storage：登录态的唯一来源是 useAuth 里
          // 那个模块级 ref，只清 storage 的话内存里仍留着 token，界面会继续当作已登录
          // （那正是这里原先要整页重载的原因）。清干净后就是一次普通的站内跳转。
          authLogout()
          setTimeout(() => {
            router.push('/login')
          }, 500)
        } else {
          ElMessage.error(res.msg)
        }
      })
    }
  })

}

</script>

<style scoped>
.password-wrapper {
  display: flex;
  justify-content: center;
  min-height: 50vh;
  padding: var(--space-40);
}

.password-card {
  width: 50%;
  max-width: 500px;
  padding: var(--space-40) var(--space-24);
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
}

.password-title {
  /* 标题现在是 h1，要显式清掉浏览器默认外边距 */
  margin: 0 0 var(--space-4);
  font-size: var(--fs-lg);
  font-weight: var(--fw-bold);
}

.password-form {
  padding-top: var(--space-32);
  padding-right: var(--space-48);
}

.password-actions {
  text-align: center;
}

.password-submit {
  padding: var(--space-20) var(--space-32);
}
</style>
