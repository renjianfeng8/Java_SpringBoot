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

          <el-form-item label="头像" prop="avatar">
            <el-upload :action="FILE_UPLOAD_URL" :headers="uploadHeaders"
                       :on-success="handleFileUpload" :on-error="handleUploadError"
                       :auto-upload="true" list-type="picture">
              <el-button type="primary">点击上传</el-button>
            </el-upload>
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
import { API_PATHS, FILE_UPLOAD_URL } from '@/constants';
import { uploadHeaders, handleUploadError } from "@/utils/upload";
import { useAuth } from "@/composables/useAuth";

const { user, setUser } = useAuth()

const formRef = ref();
const formData = reactive({
  id: null,
  username: "",
  name: "",
  phone: "",
  email: "",
  avatar: "",
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
  const current = user.value;
  if (current) {
    formData.username = current.username;
    formData.name = current.name || current.username;
    formData.phone = current.phone || "";
    formData.email = current.email || "";
    formData.avatar = current.avatar || "";
    formData.id = current.id || null;
  }
});

const updateUser = () => {
  formRef.value.validate((valid) => {
    if (valid) {
      request.put(API_PATHS.USERS, formData).then((res) => {
        if (res.code === "200") {
          ElMessage.success("更新成功");
          // 登录态只能经 useAuth 变更（规则 76）：只写 storage 副本不更新内存里的
          // user ref，顶栏的头像与用户名要等整页刷新才变。
          setUser({ ...user.value, ...formData });
        } else {
          ElMessage.error(res.msg);
        }
      });
    }
  });
};

function handleFileUpload(res) {
  if (res.code === '200') saveAvatar(res.data)
  else ElMessage.error(res.msg || '头像上传失败')
}

/**
 * 头像上传成功后立即落库，不等用户再点「更新个人信息」。
 * 载荷只带 avatar 与定位用的 id：整体 PUT 会把表单里尚未校验、尚未保存的
 * 其他改动（比如写了一半的邮箱）一起写进去。id 是 WHERE 键，其余字段为 null 时
 * updateById 的动态 set 不会碰它们，所以本次只改 avatar 一列。
 * 失败则把表单回退到服务端的真值 —— 没落库就不该在页面上显示成已生效。
 */
function saveAvatar(url) {
  formData.avatar = url
  request.put(API_PATHS.USERS, { id: formData.id, avatar: url }).then((res) => {
    if (res.code === '200') {
      // 规则 76：登录态只经 useAuth 变更，顶栏头像才会立刻跟着变
      setUser({ ...user.value, avatar: url })
      ElMessage.success('头像已更新')
    } else {
      formData.avatar = user.value?.avatar || ''
      ElMessage.error(res.msg || '头像保存失败')
    }
  }).catch(() => {
    formData.avatar = user.value?.avatar || ''
  })
}
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
