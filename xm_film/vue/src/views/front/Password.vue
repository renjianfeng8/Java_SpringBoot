<template>
  <div class="password-wrapper">

    <div class="card password-card">
      <div class="password-title">修改密码</div>

      <el-form ref="formRef" :rules="data.rules" :model="data.form" class="password-form" label-width="100px">
        <el-form-item label="原密码" prop="password">
          <el-input show-password v-model="data.form.password" autocomplete="off" placeholder="请输入原密码"/>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input show-password v-model="data.form.newPassword" autocomplete="off" placeholder="请输入新密码"/>
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword" required>
          <el-input show-password v-model="data.form.confirmPassword" autocomplete="off" placeholder="请再次确认新密码"/>
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
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";
import { API_PATHS } from '@/constants';
import { clearStoredUser, getStoredUser } from "@/utils/authStorage";


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
          clearStoredUser()
          setTimeout(() => {
            location.href = '/login'
          },500)
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
  margin: var(--space-4);
  font-size: var(--fs-md);
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
