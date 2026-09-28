<template>
  <div class="register-container">
    <div class="register-box">
      <div class="register-form-wrapper">
        <div class="register-title">欢迎注册账号</div>
        <el-form ref="formRef" :rules="data.rules" :model="data.form" class="register-form">
          <el-form-item prop="username">
            <el-input
                v-model="data.form.username"
                placeholder="请输入账号"
                prefix-icon="User"
            ></el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input
                show-password
                v-model="data.form.password"
                placeholder="请输入密码"
                prefix-icon="Lock"
            ></el-input>
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input
                show-password
                v-model="data.form.confirmPassword"
                placeholder="请确认密码"
                prefix-icon="Lock"
            ></el-input>
          </el-form-item>
          <el-form-item prop="role">
            <el-select v-model="data.form.role" class="register-field">
              <el-option value="CINEMA" label="电影院"></el-option>
              <el-option value="USER" label="用户"></el-option>
            </el-select>
          </el-form-item>
          <div class="register-actions">
            <el-button
                @click="register"
                type="primary"
                class="register-submit"
            >注 册</el-button>
          </div>
          <div class="register-hint">
            已有账号? 请<a href="/login" class="register-hint__link">登 录</a>
          </div>
        </el-form>
      </div>
    </div>
    <div class="register-footer">
      <p>本系统为 <strong>个人学习项目</strong>，所有展示数据均为 <strong>模拟数据</strong>，不反映真实市场情况，严禁用于商业用途。</p>
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

/* 注册框居中：绝对定位 50% + 回移自身 50% */
.register-box {
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
.register-form-wrapper {
  width: 100%;
  max-width: 380px;
  padding: var(--space-40);
  border-radius: var(--el-border-radius-base);
  background-color: var(--surface-glass);
  box-shadow: var(--el-box-shadow-light);
}

.register-title {
  margin-bottom: var(--space-20);
  font-size: var(--fs-xl);
  font-weight: var(--fw-bold);
  text-align: center;
  color: var(--el-color-primary);
}

.register-form {
  width: 380px;
}

.register-field {
  width: 100%;
}

.register-actions {
  margin-bottom: var(--space-16);
}

.register-submit {
  width: 100%;
  height: 44px;
}

.register-hint {
  font-size: var(--fs-base);
  text-align: right;
}

.register-hint__link {
  color: var(--el-color-primary);
  text-decoration: none;
}

.register-footer {
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
