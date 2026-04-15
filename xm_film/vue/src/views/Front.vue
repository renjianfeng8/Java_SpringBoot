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
              to="home"
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
      <RouterView @updateUser="updateUser" />
    </div>

    <!-- 页脚 -->
    <footer class="front-footer">
      <div style="border-bottom:1px solid #333;">
        <div class="footer-wrapper">
          <div class="footer-column">
            <h3 class="footer-title">关于我们</h3>
            <ul class="footer-links">
              <li>公司简介</li>
              <li>联系我们</li>
              <li>加入我们</li>
            </ul>
          </div>
          <div class="footer-column">
            <h3 class="footer-title">购票服务</h3>
            <ul class="footer-links">
              <li>购票指南</li>
              <li>退改签规则</li>
              <li>会员权益</li>
            </ul>
          </div>
          <div class="footer-column">
            <h3 class="footer-title">帮助中心</h3>
            <ul class="footer-links">
              <li>常见问题</li>
              <li>影院设施</li>
              <li>儿童观影政策</li>
            </ul>
          </div>
          <div class="footer-column contact-column">
            <h3 class="footer-title">联系我们</h3>
            <div style="margin-right: 8px;color: #e53935;">
              <p class="contact-item"><el-icon><Phone /></el-icon><span>400-123-4567</span></p>
              <p class="contact-item"><el-icon><Message /></el-icon><span>service@movie-ticket.com</span></p>
            </div>
          </div>
        </div>
      </div>
      <div class="footer-bottom">
        <div style="margin: 5px 0;">
          <p>©电影购票网站 版权所有 | 营业执照 | 增值业务许可证</p>
          <p>网络文化经营许可证 | 广播电视节目制作经营许可证 | 违法和不良信息举报中心</p>
        </div>
        <div style="color: red; font-style: italic; font-size: 18px">
          <p>让每一次观影都成为美好回忆</p>
        </div>
      </div>
    </footer>

  </div>
</template>

<script setup>
import {ref, computed, onMounted, watch, reactive} from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Phone, Message, CaretBottom } from '@element-plus/icons-vue'
import request from "@/utils/request.js";


const router = useRouter()
const route = useRoute()
const searchKeyword = ref('') // 存储用户输入的电影名称
const activePath = ref('') // 用于存储当前激活的路由路径

const data = reactive({
  filmList: [],    // 电影列表（用于存储搜索结果）
});

// 用户数据状态
const userData = ref({
  username: null,
  avatar: null,
  role: null
})

const userName = computed(() => userData.value.username)

const userAvatar = computed(() => {
  const avatar = userData.value.avatar
  if (!avatar) return null
  return avatar.startsWith('http') ? avatar : `https://your-domain.com${avatar}`
})

// 登出方法
const logout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    localStorage.removeItem('xm-pro-user')
    router.push('/login')
    ElMessage.success('退出成功')
  }).catch(() => {
    ElMessage.info('已取消退出')
  })
}

// 更新用户信息
const updateUser = (newUserInfo) => {
  if (newUserInfo) {
    userData.value = {...userData.value, ...newUserInfo}
    localStorage.setItem('xm-pro-user', JSON.stringify(userData.value))
  }
}


// 搜索处理函数
const handleSearch = () => {
  // 使用searchKeyword进行验证，与输入框绑定保持一致
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入电影名称');
    return;
  }

  request.get('/film/selectByTitle', {
    params: {
      title: searchKeyword.value.trim() // 使用searchKeyword作为查询参数
    }
  }).then(res => {
    if (res.code === '200') {
      data.filmList = res.data;
      // 跳转到搜索结果页，将搜索结果传递过去
      router.push({
        path: '/front/search',
        query: {title: searchKeyword.value.trim()}
      })

      if (data.filmList.length === 0) {
        ElMessage.info('未找到匹配的电影');
      }
    } else {
      ElMessage.error(res.msg || '查询电影失败');
    }
  }).catch(err => {
    ElMessage.error('网络错误，查询失败');
    console.error(err);
  });
}

// 生命周期钩子：组件挂载后初始化
onMounted(() => {
  // 从本地存储获取用户信息
  const storedUser = localStorage.getItem('xm-pro-user')
  if (storedUser) {
    try {
      const user = JSON.parse(storedUser)
      userData.value = {...userData.value, ...user}
    } catch (error) {
      console.error('解析用户数据失败', error)
      ElMessage.error('获取用户信息失败')
    }
  }

  // 初始化激活路径
  updateActivePath(route.path)
})

// 监听路由变化，更新菜单激活状态
watch(() => route.path, (newPath) => {
  updateActivePath(newPath)
})

// 更新激活路径的方法
const updateActivePath = (path) => {
  // 处理嵌套路由的情况，获取一级路径
  if (path.startsWith('/front/movie')) {
    activePath.value = '/front/movie'
  } else if (path.startsWith('/front/cinema')) {
    activePath.value = '/front/cinema'
  } else if (path.startsWith('/front/rank')) {
    activePath.value = '/front/rank'
  } else if (path.startsWith('/front/orders')) {
    activePath.value = '/front/orders'
  } else if (path.startsWith('/front/search')) {
    // 搜索页路由激活“电影”菜单
    activePath.value = '/front/movie'
  } else if (path.startsWith('/front')) {
    // 其他/front开头的路径，默认激活首页
    activePath.value = '/home'
  } else {
    // 直接匹配根路径
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
  padding: 10px 0 20px;
  margin-top: 60px;
}

.footer-wrapper {
  color: #aaa;
  max-width: 850px;
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
}

.footer-links {
  list-style: none;
  padding: 0;
}

.footer-links li {
  margin-bottom: 10px;
  font-size: 14px;
  cursor: pointer;
  transition: color 0.3s;
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
  margin-bottom: 10px;
  font-size: 14px;
  color: #aaa;
}

.contact-item i {
  margin-right: 8px;
  color: #e53935;
}

.footer-bottom {
  max-width: 1200px;
  margin: 0 auto;
  padding: 15px 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #666;
}
</style>
