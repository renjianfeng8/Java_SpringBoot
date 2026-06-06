<template>
  <div class="front-person-container">
    <div style="display: flex; justify-content: center; min-height: 50vh; padding: 40px;">
      <div class="card" style="width: 50%; max-width: 500px; padding: 40px 20px">
        <div style="font-size: 16px;font-weight: 550;margin: 5px">个人中心</div>
        <el-form ref="formRef" :rules="rules" :model="formData" style="padding-right: 50px; padding-top: 20px" label-width="80px">
          <el-form-item label="用户名" prop="username">
            <el-input disabled v-model="formData.username" autocomplete="off" placeholder="请输入用户名" />
          </el-form-item>

          <el-form-item label="名称" prop="name">
            <el-input v-model="formData.name" autocomplete="off" placeholder="请输入名称" />
          </el-form-item>

          <el-form-item label="电话">
            <el-input v-model="formData.phone" autocomplete="off" placeholder="请输入电话" />
          </el-form-item>

          <el-form-item label="邮箱">
            <el-input v-model="formData.email" autocomplete="off" placeholder="请输入邮箱" />
          </el-form-item>

          <div style="text-align: center">
            <el-button @click="updateUser" type="primary" style="padding: 20px 30px">更新个人信息</el-button>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from "vue";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";
import { API_PATHS } from '@/constants';

const formRef = ref();
const formData = reactive({
  id: null,
  username: "",
  name: "",
  phone: "",
  email: "",
});

const rules = {
  id: [{ required: true, message: "用户ID不能为空", trigger: "blur" }],
  username: [{ required: true, message: "请输入账号", trigger: "blur" }],
  name: [{ required: true, message: "请输入名称", trigger: "blur" }],
  email: [
    { type: "email", message: "请输入正确的邮箱地址", trigger: ["blur", "change"] },
  ],
};

onMounted(() => {
  const storedUser = localStorage.getItem("xm-pro-user");
  if (storedUser) {
    try {
      const user = JSON.parse(storedUser);
      formData.username = user.username;
      formData.name = user.name || user.username;
      formData.phone = user.phone || "";
      formData.email = user.email || "";
      formData.id = user.id || null;
    } catch (error) {
      console.error("解析用户数据失败", error);
      ElMessage.error("获取用户信息失败");
    }
  }
});

const updateUser = () => {
  formRef.value.validate((valid) => {
    if (valid) {
      request.put(API_PATHS.USERS, formData).then((res) => {
        if (res.code === "200") {
          ElMessage.success("更新成功");
          // 更新缓存数据
          localStorage.setItem("xm-pro-user", JSON.stringify(formData));
        } else {
          ElMessage.error(res.msg);
        }
      });
    }
  });
};
</script>

<style scoped>
.front-person-container {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.card {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}
</style>
