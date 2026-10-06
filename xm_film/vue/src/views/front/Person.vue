<template>
  <div class="front-person-container">
    <div class="person-wrapper">
      <div class="card person-card">
        <h1 class="person-title">个人中心</h1>
        <!-- status-icon 打开「错误反馈三件套」的图标那一件；@submit.prevent 兜住原生提交，
             回车由输入框上的 @keyup.enter 触发（只挂一处，避免一次回车发两次请求，规范 §9.3） -->
        <el-form ref="formRef" :rules="rules" :model="formData" class="person-form" label-width="80px"
                 status-icon @submit.prevent>
          <el-form-item label="用户名" prop="username">
            <el-input disabled v-model="formData.username" autocomplete="off" placeholder="请输入用户名" />
          </el-form-item>

          <el-form-item label="名称" prop="name">
            <el-input v-model="formData.name" autocomplete="off" placeholder="请输入名称" @keyup.enter="updateUser" />
          </el-form-item>

          <el-form-item label="电话" prop="phone">
            <el-input v-model="formData.phone" autocomplete="off" placeholder="请输入电话" @keyup.enter="updateUser" />
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <el-input v-model="formData.email" autocomplete="off" placeholder="请输入邮箱" @keyup.enter="updateUser" />
          </el-form-item>

          <div class="person-actions">
            <el-button @click="updateUser" type="primary" class="person-submit">更新个人信息</el-button>
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
import { getStoredUser, setStoredUser } from "@/utils/authStorage";

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
  const storedUser = getStoredUser();
  if (storedUser) {
    try {
      const user = storedUser;
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
          setStoredUser({ ...getStoredUser(), ...formData });
        } else {
          ElMessage.error(res.msg);
        }
      });
    }
  });
};
</script>

<style scoped>
/* 底色与最小高度都交给外壳：这里再写一次 min-height: 100vh 会把页脚顶到视口之外 */
.person-wrapper {
  display: flex;
  justify-content: center;
  padding: var(--space-40);
}

.person-card {
  width: 50%;
  max-width: 500px;
  padding: var(--space-40) var(--space-20);
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
}

.person-title {
  /* 标题现在是 h1，要显式清掉浏览器默认外边距 */
  margin: 0 0 var(--space-4);
  font-size: var(--fs-lg);
  font-weight: var(--fw-bold);
}

.person-form {
  padding-top: var(--space-20);
  padding-right: var(--space-48);
}

.person-actions {
  text-align: center;
}

.person-submit {
  padding: var(--space-20) var(--space-32);
}
</style>
