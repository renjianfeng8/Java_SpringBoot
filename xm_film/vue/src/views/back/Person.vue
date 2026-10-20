<template>

  <div class="profile-wrapper">
    <div class="card profile-card">

      <el-form ref="formRef" :rules="data.rules" :model="data.form" class="dialog-form" label-width="80px">

        <el-form-item label="用户名" prop="username">
          <el-input disabled v-model="data.form.username" autocomplete="off" placeholder="请输入用户名"/>
        </el-form-item>

        <el-form-item label="名称" prop="name">
          <el-input v-model="data.form.name" autocomplete="off" placeholder="请输入名称"/>
        </el-form-item>

        <el-form-item label="电话" >
          <el-input v-model="data.form.phone" autocomplete="off" placeholder="请输入电话"/>
        </el-form-item>

        <el-form-item label="影院介绍">
          <el-input type="textarea" :rows="3" v-model="data.form.description" autocomplete="off" placeholder="请输入影院介绍"/>
        </el-form-item>

        <div class="form-actions">
          <el-button @click="updateUser" type="primary" class="submit-button" >更新影院信息</el-button>
        </div>
      </el-form>

    </div>
  </div>
</template>

<script setup>
import { isSuccess, API_PATHS, apiById } from '@/constants';

import { reactive, ref } from "vue";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";
import { useAuth } from "@/composables/useAuth";

const { user, setUser } = useAuth()
const formRef = ref()
const data = reactive({
  form: {
    sex: '男'
  },
  user: user.value || {},
  rules: {
    username: [
      { required: true, message: '请输入账号', trigger: 'blur' }
    ],
    name: [
      { required: true, message: '请输入名称', trigger: 'blur' }
    ]
  }
})

if (data.user.role === 'USER') {
  request.get(apiById(API_PATHS.USERS, data.user.id))
      .then(res => {
        data.form = res.data
      })
      .catch(() => {})
} else {
  // 存储副本带 token（useAuth 落库时会并进去），直接当表单会把会话令牌塞进 PUT body
  const { token: _token, ...profile } = data.user
  data.form = profile
}

const updateUser = () => {
  const endpoint = { USER: API_PATHS.USERS, CINEMA: API_PATHS.CINEMAS }[data.user.role] ?? API_PATHS.ADMINS
  request.put(endpoint, data.form).then(res => {
    if (isSuccess(res)) {
      ElMessage.success('更新成功')
      // 登录态只能经 useAuth 变更：只写 storage 副本不会更新内存里的 user，
      // 顶栏的用户名与头像要等整页刷新才变。
      setUser({ ...user.value, ...data.form })
    } else {
      ElMessage.error(res.msg)
    }
  }).catch(() => {})
}

</script>
