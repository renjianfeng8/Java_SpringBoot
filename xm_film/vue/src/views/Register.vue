<template>
  <div class="auth-container">
    <div class="auth-card">
      <div class="auth-title">欢迎注册账号</div>
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
            @keyup.enter="register"
          ></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            show-password
            v-model="data.form.password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            @keyup.enter="register"
          ></el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            show-password
            v-model="data.form.confirmPassword"
            placeholder="请确认密码"
            :prefix-icon="Lock"
            @keyup.enter="register"
          ></el-input>
        </el-form-item>
        <el-form-item label="注册身份" prop="role">
          <el-select v-model="data.form.role" class="auth-field">
            <el-option value="USER" label="用户"></el-option>
            <el-option value="CINEMA" label="电影院"></el-option>
          </el-select>
        </el-form-item>
        <div class="auth-actions">
          <el-button
              @click="register"
              type="primary"
              class="auth-submit"
              size="large"
          >注 册</el-button>
        </div>
        <div class="auth-hint">
          已有账号? 请<a href="/login" class="auth-hint__link">登 录</a>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import {reactive, ref} from "vue";
import { User, Lock } from '@element-plus/icons-vue';
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
    role: [{required: true, message: '请选择角色', trigger: 'change'}],
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

<style scoped lang="scss">
/* 几何、配色与背景图统一在 auth-layout.scss，与 Login.vue 共用同一套外壳 */
@use '@/assets/css/auth-layout' as *;
</style>
