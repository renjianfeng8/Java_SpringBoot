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
        <el-form-item label="邮箱" >
          <el-input v-model="data.form.email" autocomplete="off" placeholder="请输入邮箱"/>
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

import { reactive, ref } from "vue";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";
import { API_PATHS, apiById } from "@/constants";
import { getStoredUser } from "@/utils/authStorage";
import { useAuth } from "@/composables/useAuth";


const { user, setUser } = useAuth()
const formRef = ref()
const data = reactive({
  form: {
    sex: '男'
  },
  user: getStoredUser() || {},
  rules: {
    username: [
      { required: true, message: '请输入账号', trigger: 'blur' }
    ],
    name: [
      { required: true, message: '请输入名称', trigger: 'blur' }
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
  const endpoint = { USER: API_PATHS.USERS, CINEMA: API_PATHS.CINEMAS }[data.user.role] ?? API_PATHS.ADMINS
  request.put(endpoint, data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('更新成功')
      // 登录态只能经 useAuth 变更（规则 76）：只写 storage 副本不会更新内存里的 user，
      // 顶栏的用户名与头像要等整页刷新才变。
      setUser({ ...user.value, ...data.form })
      //触发父级从缓存里面取到最新的数据
      emit('updateUser', data.form)
    } else {
      ElMessage.error(res.msg)
    }
  })
}

</script>
