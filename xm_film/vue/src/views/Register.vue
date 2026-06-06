<template>
  <div class="register-container">
    <div class="register-box">
      <div class="register-form-wrapper">
        <div style="margin-bottom: 20px; font-size: 20px; color: #4A90E2; font-weight: bold; text-align: center;">欢迎注册账号</div>
        <el-form ref="formRef" :rules="data.rules" :model="data.form" style="width: 380px">
          <el-form-item prop="username">
            <el-input
                v-model="data.form.username"
                placeholder="请输入账号"
                prefix-icon="User"
                size="medium"
            ></el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input
                show-password
                v-model="data.form.password"
                placeholder="请输入密码"
                prefix-icon="Lock"
                size="medium"
            ></el-input>
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input
                show-password
                v-model="data.form.confirmPassword"
                placeholder="请确认密码"
                prefix-icon="Lock"
                size="medium"
            ></el-input>
          </el-form-item>
          <el-form-item prop="role">
            <el-select v-model="data.form.role" style="width: 100%">
              <el-option value="CINEMA" label="电影院"></el-option>
              <el-option value="USER" label="用户"></el-option>
            </el-select>
          </el-form-item>
          <div style="margin-bottom: 15px">
            <el-button
                @click="register"
                type="primary"
                style="width: 100%; height: 44px;"
                size="medium"
            >注 册</el-button>
          </div>
          <div style="text-align: right; font-size: 14px;">
            已有账号? 请<a href="/login" style="color: #F53F3F; text-decoration: none;">登 录</a>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import {reactive, ref} from "vue";
import request from "@/utils/request.js";
import {ElMessage} from "element-plus";
import { AUTH_API } from "@/constants";

const validatePass = (value, callback) => {
  if (!value) {
    callback(new Error('请再次确认密码'));
  } else if (value !== data.form.password) {
    callback(new Error('两次输入的密码不一致'));
  } else {
    callback();
  }
};

const data = reactive({
  form: {role: "USER"},
  rules: {
    username: [{required: true, message: '请输入账号', trigger: 'blur'}],
    password: [{required: true, message: '请输入密码', trigger: 'blur'}],
    role: [{required: true, message: '请选择角色', trigger: 'blur'}],
    confirmPassword: [{validator: validatePass, trigger: 'blur'}],
  },
});

const formRef = ref();

const register = () => {
  formRef.value.validate((valid) => {
    if (valid) {
      request.post(AUTH_API.REGISTER, data.form).then((res) => {
        if (res.code === '200') {
          ElMessage.success('注册成功');
          setTimeout(() => {
            location.href = '/login';
          }, 1000);
        } else {
          ElMessage.error(res.msg || '注册失败，请检查信息');
        }
      });
    }
  });
};
</script>

<style scoped>
.register-container {
  position: relative;
  height: 100vh;
  overflow: hidden;
  background-image: url('@/assets/imgs/registerbg.jpg');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
}

/* 核心修改：将注册框居中 */
.register-box {
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

.register-form-wrapper {
  width: 100%;
  max-width: 380px;
  background-color: rgba(255, 255, 255, 0.8);
  border-radius: 10px;
  padding: 40px;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.2);
}
</style>
