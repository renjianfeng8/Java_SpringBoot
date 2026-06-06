<template>

  <div style="display: flex; justify-content: center; min-height: 50vh; padding: 20px;">
    <div class="card" style="width: 50%; max-width: 500px; padding: 40px 20px">

      <el-form ref="formRef" :rules="data.rules" :model="data.form" style="padding-right: 50px;padding-top: 20px" label-width="80px">

        <el-form-item label="用户名" prop="username">
          <el-input disabled v-model="data.form.username" autocomplete="off" placeholder="请输入用户名"/>
        </el-form-item>

        <el-form-item label="名称" prop="name">
          <el-input v-model="data.form.name" autocomplete="off" placeholder="请输入名称"/>
        </el-form-item>

        <el-form-item label="电话" >
          <el-input v-model="data.form.phone" autocomplete="off" placeholder="请输入电话"/>
        </el-form-item>
        <el-form-item label="邮箱" >
          <el-input v-model="data.form.email" autocomplete="off" placeholder="请输入邮箱"/>
        </el-form-item>

        <el-form-item label="个人介绍">
          <el-input type="textarea" :rows="3" v-model="data.form.description" autocomplete="off" placeholder="请输入个人介绍"/>
        </el-form-item>

        <div style="text-align: center">
          <el-button @click="updateUser" type="primary" style="padding: 20px 30px" >更新个人信息</el-button>
        </div>
      </el-form>

    </div>
  </div>
</template>

<script setup>

import {reactive , ref} from "vue";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";
import { API_PATHS, apiById } from '@/constants';


const formRef = ref()
const data = reactive ({
  form: {
    sex: '男'
  },
  user: (() => { try { return JSON.parse(localStorage.getItem('xm-pro-user')); } catch { return {}; } })(),
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

const emit = defineEmits(['updateUser'])

if (data.user.role === 'USER') {
  request.get(apiById(API_PATHS.USERS, data.user.id)).then(res => {
    data.form = res.data
  })
} else {
  data.form = data.user
}

const updateUser = () => {
  if (data.user.role === 'USER') {
    request.put(API_PATHS.USERS,data.form).then(res =>{
      if (res.code === '200') {
        ElMessage.success('更新成功')
        //更新缓存数据
        localStorage.setItem('xm-pro-user',JSON.stringify(data.form))
        //触发父级从缓存里面取到最新的数据
        emit('updateUser', data.form)
      } else {
        ElMessage.error(res.msg)
      }
    })
  } else {
    request.put(API_PATHS.ADMINS,data.form).then(res =>{
      if (res.code === '200') {
        ElMessage.success('更新成功')
        //更新缓存数据
        localStorage.setItem('xm-pro-user',JSON.stringify(data.form))
        //触发父级从缓存里面取到最新的数据
        emit('updateUser', data.form)
      } else {
        ElMessage.error(res.msg)
      }
    })
  }
}

</script>
