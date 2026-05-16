<template>
  <div class="manage-container">
    <!-- 头部导航栏 -->
    <div class="manage-header">
      <div class="manage-header-left">
        <img src="@/assets/imgs/xm_logo.jpg" alt="电影购票系统" class="logo">
        <h1 class="title">电影购票网站 - 管理员后台</h1>
      </div>

      <div class="manage-header-center">
        <!-- 面包屑导航 -->
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/manage/home' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item>{{ router.currentRoute.value.meta.name || '未知页面' }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <div class="manage-header-right">
        <el-dropdown trigger="click">
          <div class="user-info">
            <img :src="userAvatar" alt="用户头像" class="avatar">
            <span class="username">{{ userName }}</span>
            <el-icon><CaretBottom /></el-icon>
          </div>

          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="navigateTo('/manage/person')">
                <el-icon><User /></el-icon>
                <span>个人资料</span>
              </el-dropdown-item>
              <el-dropdown-item @click="navigateTo('/manage/password')">
                <el-icon><Lock /></el-icon>
                <span>修改密码</span>
              </el-dropdown-item>
              <el-dropdown-item divided @click="logout">
                <el-icon><Logout /></el-icon>
                <span>退出登录</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 主体内容区 -->
    <div class="manage-main">
      <!-- 侧边栏导航 -->
      <div class="manage-main-left">
        <el-menu
            :default-active="router.currentRoute.value.path"
            :default-openeds="openedMenuKeys"
            router
            unique-opened
            style="border: none"
        >
          <el-menu-item index="/manage/home">
            <el-icon><HomeFilled /></el-icon>
            <span>系统首页</span>
          </el-menu-item>

          <el-sub-menu index="1">
            <template #title>
              <el-icon><UserFilled /></el-icon>
              <span>用户管理</span>
            </template>
            <el-menu-item index="/manage/admin">
              <el-icon><Setting /></el-icon>
              <span>管理员信息</span>
            </el-menu-item>
            <el-menu-item index="/manage/cinema">
              <el-icon><OfficeBuilding /></el-icon>
              <span>影院信息</span>
            </el-menu-item>
            <el-menu-item index="/manage/user">
              <el-icon><User /></el-icon>
              <span>用户信息</span>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="2">
            <template #title>
              <el-icon><Document /></el-icon>
              <span>信息管理</span>
            </template>
            <el-menu-item index="/manage/notice">
              <el-icon><Bell /></el-icon>
              <span>系统公告</span>
            </el-menu-item>
            <el-menu-item index="/manage/type">
              <el-icon><Grid /></el-icon>
              <span>电影分类</span>
            </el-menu-item>
            <el-menu-item index="/manage/area">
              <el-icon><Location /></el-icon>
              <span>电影区域</span>
            </el-menu-item>
            <el-menu-item index="/manage/film">
              <el-icon><VideoCameraFilled /></el-icon>
              <span>电影信息</span>
            </el-menu-item>
            <el-menu-item index="/manage/video">
              <el-icon><VideoPlay /></el-icon>
              <span>电影预告视频</span>
            </el-menu-item>
            <el-menu-item index="/manage/actor">
              <el-icon><UserFilled /></el-icon>
              <span>演职人员</span>
            </el-menu-item>
            <el-menu-item index="/manage/room">
              <el-icon><Tickets /></el-icon>
              <span>影厅房间</span>
            </el-menu-item>
            <el-menu-item index="/manage/record">
              <el-icon><Calendar /></el-icon>
              <span>放映记录</span>
            </el-menu-item>
            <el-menu-item index="/manage/ordered">
              <el-icon><CreditCard /></el-icon>
              <span>购票订单</span>
            </el-menu-item>
            <el-menu-item index="/manage/mark">
              <el-icon><Star /></el-icon>
              <span>用户评价</span>
            </el-menu-item>
            <el-menu-item index="/manage/person">
              <el-icon><Avatar /></el-icon>
              <span>个人资料</span>
            </el-menu-item>
            <el-menu-item index="/manage/password">
              <el-icon><Lock /></el-icon>
              <span>修改密码</span>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </div>

      <!-- 主内容区 -->
      <div class="manage-content">
        <RouterView @updateUser="updateUser" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'


// 获取路由实例
const router = useRouter()


// 用户数据状态
const userData = ref({
  username: null,
  avatar: null,
  role: null
})

const userName = computed(() => userData.value.username)

const userAvatar = computed(() => {
  return userData.value.avatar || null
})

// 展开的菜单key
const openedMenuKeys = ref(['1', '2'])


// 导航方法
const navigateTo = (path) => {
  router.push(path)
}

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
    userData.value = { ...userData.value, ...newUserInfo }
    localStorage.setItem('xm-pro-user', JSON.stringify(userData.value))
  }
}

// 生命周期钩子：组件挂载后初始化
onMounted(() => {
  // 从本地存储获取用户信息
  const storedUser = localStorage.getItem('xm-pro-user')
  if (storedUser) {
    try {
      const user = JSON.parse(storedUser)
      userData.value = {
        ...userData.value,
        ...user, // 合并本地存储的用户数据
        // 处理头像路径（如果是相对路径需要拼接域名）
        avatar: user.avatar && !user.avatar.startsWith('http')
            ? `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:9090'}${user.avatar}`
            : user.avatar || userData.value.avatar
      }
    } catch (error) {
      console.error('解析用户数据失败', error)
      ElMessage.error('获取用户信息失败')
    }
  }
})
</script>

<style scoped>
/* 样式部分保持不变 */
.manage-container {
  width: 100%;
  min-height: 100vh;
  background-color: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.manage-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 50px;
  padding: 0 5px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  z-index: 10;
}

.manage-header-left {
  display: flex;
  align-items: center;
  gap: 5px;
}

.logo {
  height: 25px;
  width: auto;
}

.title {
  font-size: 15px;
  margin: 0;
  font-weight: 700;
}

.manage-header-center {
  flex: 1;
  padding: 0 20px;
  text-align: center;
}

.manage-header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid gray;
  transition: all 0.3s;
}

.avatar:hover {
  transform: scale(1.2);
}

.username {
  font-size: 15px;
  color: gray;
  font-weight: 500;
}

.manage-main {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.manage-main-left {
  width: 175px;
  transition: width 0.3s;
  border-right: 1px solid #ebeef5;
}

.manage-content {
  flex: 1;
  overflow-y: auto;
  background-color: #ffffff;
  padding: 15px;
  min-height: calc(100vh - 120px);
}
</style>