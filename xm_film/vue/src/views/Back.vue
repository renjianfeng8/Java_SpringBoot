<template>
  <div class="manage-container">
    <!-- 头部导航栏 -->
    <div class="manage-header">
      <div class="manage-header-left">
        <img src="@/assets/imgs/xm_logo.jpg" alt="电影购票系统" class="logo">
        <h1 class="title">电影购票网站 - 影院后台</h1>
      </div>

      <div class="manage-header-center">
        <!-- 面包屑导航 -->
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/back/home' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item>{{ router.currentRoute.value.meta.name || '未知页面' }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <div class="manage-header-right">
        <el-button size="small" @click="navigateTo('/front/home')">前台首页</el-button>

        <el-dropdown trigger="click">
          <div class="user-info">
            <img :src="userAvatar" alt="用户头像" class="avatar">
            <span class="username">{{ userName }}</span>
            <el-icon><CaretBottom /></el-icon>
          </div>

          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="navigateTo('/back/person')">
                <el-icon><User /></el-icon>
                <span>个人资料</span>
              </el-dropdown-item>
              <el-dropdown-item @click="navigateTo('/back/password')">
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
            class="manage-menu"
        >
          <el-menu-item index="/back/home">
            <el-icon><House /></el-icon>
            <span>系统首页</span>
          </el-menu-item>

          <el-sub-menu index="1">
            <template #title>
              <el-icon><Document /></el-icon>
              <span>信息管理</span>
            </template>
            <el-menu-item index="/back/film">
              <el-icon><VideoCamera /></el-icon>
              <span>电影信息</span>
            </el-menu-item>
            <el-menu-item index="/back/room">
              <el-icon><Tickets /></el-icon>
              <span>影厅房间</span>
            </el-menu-item>
            <el-menu-item index="/back/record">
              <el-icon><Calendar /></el-icon>
              <span>放映记录</span>
            </el-menu-item>
            <el-menu-item index="/back/ordered">
              <el-icon><CreditCard /></el-icon>
              <span>购票订单</span>
            </el-menu-item>
          </el-sub-menu>

          <!-- 新增：个人中心板块 -->
          <el-sub-menu index="2">
            <template #title>
              <el-icon><User /></el-icon>
              <span>个人中心</span>
            </template>
            <el-menu-item index="/back/person">
              <el-icon><Avatar /></el-icon>
              <span>个人资料</span>
            </el-menu-item>
            <el-menu-item index="/back/password">
              <el-icon><Lock /></el-icon>
              <span>修改密码</span>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </div>

      <!-- 主内容区 -->
      <div class="manage-content">
        <RouterView />
      </div>
    </div>
    <div class="admin-footer">
      <p>个人学习项目 · 所有数据均为模拟数据 · 严禁商业用途</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Avatar,
  Calendar,
  CaretBottom,
  CreditCard,
  Document,
  House,
  Lock,
  SwitchButton as Logout,
  Tickets,
  User,
  VideoCamera,
} from '@element-plus/icons-vue'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const { user, logout: authLogout } = useAuth()

const userName = computed(() => user.value?.username || '')

const userAvatar = computed(() => {
  return user.value?.avatar || null
})

const openedMenuKeys = ref(['1', '2'])

const navigateTo = (path) => {
  router.push(path)
}

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
</script>

<style scoped lang="scss">
@use '@/assets/css/admin-layout' as *;
</style>
