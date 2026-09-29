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
import { getStoredUser } from '@/utils/authStorage';

// 获取路由实例
const router = useRouter();

// 跳转到对应角色的主页
const goHome = () => {
  const userRole = getStoredUser()?.role || 'USER';

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
  margin: 0;
  padding: 0;
  background-color: var(--el-fill-color-light);
}

.error-content {
  width: 100%;
  max-width: 500px;
  margin: var(--space-20);
  padding: var(--space-40);
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-light);
  text-align: center;
}

/* 404 错误码，规范 §4.2 唯一豁免的 120px 特例 */
.error-code {
  position: relative;
  margin-bottom: var(--space-20);
  font-size: 120px;
  font-weight: var(--fw-bold);
  color: var(--el-color-primary);
  transition: transform 200ms ease-in-out;
}

/* 数字下方装饰短线 */
.error-code::after {
  content: '';
  position: absolute;
  bottom: var(--space-12);
  left: 50%;
  width: 80px;
  height: 8px;
  transform: translateX(-50%);
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-color-primary-light-9);
}

.error-code:hover {
  transform: scale(1.1);
}

.error-title {
  margin-bottom: var(--space-12);
  font-size: var(--fs-4xl);
  color: var(--el-text-color-primary);
}

.error-message {
  margin-bottom: var(--space-32);
  font-size: var(--fs-md);
  line-height: var(--lh-base);
  color: var(--el-text-color-regular);
}

.home-button {
  padding: var(--space-12) var(--space-24);
  border-radius: var(--el-border-radius-base);
  font-size: var(--fs-md);
  transition: transform 200ms ease-in-out, box-shadow 200ms ease-in-out;
}

.home-button:hover {
  transform: translateY(-4px);
  box-shadow: var(--el-box-shadow-light);
}
</style>
