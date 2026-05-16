<template>
  <div class="error-container">
    <div class="error-content">
      <div class="error-code">404</div>
      <h1 class="error-title">页面不存在</h1>
      <p class="error-message">抱歉，你访问的页面好像迷路了</p>
      <el-button type="primary" @click="goHome" class="home-button">
        返回主页
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';

// 获取路由实例
const router = useRouter();

// 跳转到对应角色的主页
const goHome = () => {
  // 从本地存储获取用户信息（JSON字符串）并解析为对象
  const userStr = localStorage.getItem('xm-pro-user');
  let userRole = 'USER';
  try {
    const userInfo = userStr ? JSON.parse(userStr) : null;
    userRole = userInfo?.role || 'USER';
  } catch (e) {
    // 默认USER角色
  }

  // 根据用户角色跳转到对应的主页
  if (userRole === 'USER') {
    router.push('/front/home');
  }
  if (userRole === 'CINEMA') {
    router.push('/back/home'); // 影院后台首页路由
  }
  if (userRole === 'ADMIN') {
    router.push('/manage/home'); // 管理员首页路由
  }
};
</script>

<style scoped>
.error-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f7fa;
  margin: 0;
  padding: 0;
}

.error-content {
  text-align: center;
  padding: 40px;
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  max-width: 500px;
  width: 100%;
  margin: 20px;
}

.error-code {
  font-size: 120px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 20px;
  transition: transform 0.5s ease;
  position: relative;
}

/* 增加数字装饰效果 */
.error-code::after {
  content: '';
  position: absolute;
  width: 80px;
  height: 8px;
  background-color: #e6f7ff;
  bottom: 10px;
  left: 50%;
  transform: translateX(-50%);
  border-radius: 4px;
}

.error-code:hover {
  transform: scale(1.1);
}

.error-title {
  font-size: 32px;
  color: #303133;
  margin-bottom: 10px;
}

.error-message {
  font-size: 16px;
  color: #606266;
  margin-bottom: 30px;
  line-height: 1.6;
}

.home-button {
  padding: 12px 24px;
  font-size: 16px;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.home-button:hover {
  transform: translateY(-3px);
  box-shadow: 0 5px 15px rgba(64, 158, 255, 0.3);
}
</style>