<template>
  <div class="front-container">

    <div class="front-header">
      <div class="front-header-left">
        <img src="@/assets/imgs/xm_logo.jpg" alt="电影购票系统" class="logo">
        <h1 class="title">电影购票网站</h1>
      </div>

      <div class="front-header-center">
        <nav class="main-nav">
          <router-link
              to="/front/home"
              class="nav-item"
              :class="{ 'active': activePath === '/home' }"
          >
            首页
          </router-link>
          <router-link
              to="/front/movie"
              class="nav-item"
              :class="{ 'active': activePath === '/front/movie' }"
          >
            电影
          </router-link>
          <router-link to="/front/cinema" class="nav-item" :class="{ 'active': activePath === '/front/cinema' }">影院</router-link>
          <router-link to="/front/rank" class="nav-item" :class="{ 'active': activePath === '/front/rank' }">排行榜</router-link>
          <router-link
              to="/front/orders"
              class="nav-item"
              :class="{ 'active': activePath === '/front/orders' }"
          >
            购票记录
          </router-link>
        </nav>
      </div>

      <div class="front-header-right">
        <!-- 搜索框提示文案明确为“电影名称”，引导用户输入 -->
        <el-input v-model="searchKeyword" placeholder="请输入电影名称" class="search-input" @keyup.enter="handleSearch">
          <template #append>
            <el-button type="info" @click="handleSearch">搜 索</el-button>
          </template>
        </el-input>

        <el-dropdown trigger="click">
          <div class="user-info">
            <img :src="userAvatar" alt="用户头像" class="avatar">
            <span class="username">{{ userName }}</span>
            <el-icon><CaretBottom /></el-icon>
          </div>

          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push('/front/person')">个人中心</el-dropdown-item>
              <el-dropdown-item @click="router.push('/front/password')">修改密码</el-dropdown-item>
              <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <div class="front-content">
      <RouterView />
    </div>

    <!-- 页脚 -->
    <footer class="front-footer">
      <div class="footer-main">
        <div class="footer-wrapper">
          <div class="footer-column">
            <h3 class="footer-title">项目声明</h3>
            <ul class="footer-links">
              <li>本系统为个人学习项目</li>
              <li>所有数据均为模拟数据</li>
              <li>严禁用于任何商业用途</li>
              <li>部分素材来源网络，侵删</li>
            </ul>
          </div>
          <div class="footer-column">
            <h3 class="footer-title">功能与风险</h3>
            <ul class="footer-links">
              <li>本系统仅展示功能演示</li>
              <li>不提供真实购票服务</li>
              <li>不涉及任何资金交易</li>
              <li>使用风险由用户自行承担</li>
            </ul>
          </div>
          <div class="footer-column">
            <h3 class="footer-title">用户信息与隐私</h3>
            <ul class="footer-links">
              <li>本系统不收集真实个人信息</li>
              <li>注册信息仅用于功能演示</li>
              <li>密码采用加密存储</li>
              <li>请勿使用真实密码注册</li>
            </ul>
          </div>
          <div class="footer-column contact-column">
            <h3 class="footer-title">版权信息</h3>
            <div style="margin-right: 8px;color: #aaa; font-size: 14px; line-height: 1.8;">
              <p class="contact-item">© 2024-2026 电影购票网站</p>
              <p class="contact-item">个人学习项目 保留所有权利</p>
              <p class="contact-item">本系统仅用于技术学习与交流</p>
              <p class="contact-item">联系方式: 2145345678@qq.com</p>
            </div>
          </div>
        </div>
      </div>
      <div class="footer-bottom">
        <div style="color: #888; font-size: 13px; text-align: center; width: 100%;">
          <p>本系统为 <strong>个人学习项目</strong>，所有展示数据（包括但不限于电影信息、票房数据、影院信息、订单记录）均为 <strong>模拟数据</strong>，不反映真实市场情况。</p>
          <p style="margin-top: 4px;">严禁将本系统用于任何商业用途。使用本系统即表示您已了解并同意上述条款。</p>
        </div>
      </div>
    </footer>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CaretBottom } from '@element-plus/icons-vue'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const route = useRoute()
const { user, logout: authLogout } = useAuth()

const searchKeyword = ref('')
const activePath = ref('')

const userName = computed(() => user.value?.username || '')

const userAvatar = computed(() => {
  const avatar = user.value?.avatar
  if (!avatar) return null
  return avatar.startsWith('http') ? avatar : `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:9090'}${avatar}`
})

const logout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    authLogout()
    router.push('/login')
    ElMessage.success('退出成功')
  }).catch(() => {
    ElMessage.info('已取消退出')
  })
}

const handleSearch = () => {
  const keyword = searchKeyword.value.trim()
  if (!keyword) {
    ElMessage.warning('请输入电影名称')
    return
  }
  window.location.href = '/front/search?title=' + encodeURIComponent(keyword)
}

onMounted(() => {
  updateActivePath(route.path)
})

watch(() => route.path, (newPath) => {
  updateActivePath(newPath)
})

const updateActivePath = (path) => {
  if (path.startsWith('/front/movie')) {
    activePath.value = '/front/movie'
  } else if (path.startsWith('/front/cinema')) {
    activePath.value = '/front/cinema'
  } else if (path.startsWith('/front/rank')) {
    activePath.value = '/front/rank'
  } else if (path.startsWith('/front/orders')) {
    activePath.value = '/front/orders'
  } else if (path.startsWith('/front/search')) {
    activePath.value = '/front/movie'
  } else if (path.startsWith('/front')) {
    activePath.value = '/home'
  } else {
    activePath.value = path === '/' ? '/home' : path
  }
}
</script>

<style scoped>
.front-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 20px;
  background: white;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}

.front-header > div {
  display: flex;
  align-items: center;
}

.front-header-center {
  flex: 1;
  max-width: 500px;
  justify-content: center;
}

.main-nav {
  display: flex;
  gap: 25px;
  padding: 15px 20px;
}

.nav-item {
  color: #1b191a;
  text-decoration: none;
  font-size: 16px;
  padding: 8px 12px;
  transition: all 0.3s;
  position: relative;
}

.nav-item:hover {
  color: #409eff;
  transform: translateY(-2px);
}

/* 激活状态的样式 */
.active {
  color: #e53935 !important;
  font-weight: bold;
}

.active::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  width: 100%;
  height: 2px;
  background-color: #e53935;
  border-radius: 1px;
}

.front-header-right {
  gap: 15px;
  max-width: 500px;
}

.logo {
  height: 30px;
  margin-right: 10px;
}

.title {
  font-size: 16px;
}

.search-input {
  width: 200px;
  height: 34px;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 4px;
}

.user-info:hover {
  background-color: #f5f7fa;
}

.avatar {
  height: 32px;
  width: 32px;
  border-radius: 50%;
  margin-right: 8px;
  object-fit: cover;
}

.username {
  font-size: 14px;
  margin-right: 4px;
}

.front-footer {
  background-color: #1a1a1a;
  color: #fff;
  padding: 0;
  margin-top: 60px;
}

.footer-main {
  border-bottom: 1px solid #333;
  padding: 20px 0 10px;
  background-color: #1a1a1a;
}

.footer-wrapper {
  color: #aaa;
  max-width: 950px;
  margin: 0 auto;
  display: flex;
}

.footer-column {
  flex: 1;
  margin-bottom: 15px;
}

.footer-title {
  font-size: 15px;
  font-weight: bold;
  margin-bottom: 10px;
  position: relative;
  padding-bottom: 10px;
  color: #e53935;
}

.footer-title::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  width: 30px;
  height: 2px;
  background-color: #e53935;
}

.footer-links {
  list-style: none;
  padding: 0;
}

.footer-links li {
  margin-bottom: 8px;
  font-size: 13px;
  color: #999;
  cursor: default;
  line-height: 1.6;
}

.footer-links li:hover {
  color: #e53935;
}

.contact-column {
  display: flex;
  flex-direction: column;
}

.contact-item {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
  font-size: 13px;
  color: #999;
}

.footer-bottom {
  max-width: 1000px;
  margin: 0 auto;
  padding: 15px;
  font-size: 12px;
  color: #666;
}
</style>
