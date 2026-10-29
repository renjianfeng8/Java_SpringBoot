<template>
  <div class="manage-container">
    <!-- 头部导航栏 -->
    <div class="manage-header">
      <div class="manage-header-left">
        <img src="@/assets/imgs/xm_logo.jpg" :alt="SITE_NAME" class="logo">
        <h1 class="title">{{ menu.title }}</h1>
      </div>

      <div class="manage-header-center">
        <!-- 面包屑导航 -->
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: menu.home }">首页</el-breadcrumb-item>
          <el-breadcrumb-item>{{ route.meta.name || '未知页面' }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <div class="manage-header-right">
        <el-button size="small" @click="router.push('/front/home')">前台首页</el-button>

        <el-dropdown trigger="click">
          <div class="user-info">
            <img :src="userAvatar" alt="用户头像" class="avatar" @error="onAvatarError">
            <span class="username">{{ userName }}</span>
            <el-icon><CaretBottom /></el-icon>
          </div>

          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push(personPath)">
                <el-icon><User /></el-icon>
                <span>个人资料</span>
              </el-dropdown-item>
              <el-dropdown-item @click="router.push(passwordPath)">
                <el-icon><Lock /></el-icon>
                <span>修改密码</span>
              </el-dropdown-item>
              <el-dropdown-item divided @click="confirmLogout">
                <el-icon><SwitchButton /></el-icon>
                <span>退出登录</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 主体内容区 -->
    <div class="manage-main">
      <!-- 侧边栏导航：分组与条目由 constants/adminMenu.js 提供 -->
      <div class="manage-main-left">
        <el-menu
            :default-active="route.path"
            :default-openeds="openedMenuKeys"
            router
            unique-opened
            class="manage-menu"
        >
          <el-menu-item :index="menu.homeItem.path">
            <el-icon><component :is="menu.homeItem.icon" /></el-icon>
            <span>{{ menu.homeItem.label }}</span>
          </el-menu-item>

          <el-sub-menu v-for="group in menu.groups" :key="group.key" :index="group.key">
            <template #title>
              <el-icon><component :is="group.icon" /></el-icon>
              <span>{{ group.label }}</span>
            </template>
            <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
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
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CaretBottom, Lock, SwitchButton, User } from '@element-plus/icons-vue'
import { useAuth } from '@/composables/useAuth'
import { useLogout } from '@/composables/useLogout'
import { avatarSrc, onAvatarError } from '@/utils/avatar'
import { SITE_NAME } from '@/utils/navigation'

const props = defineProps({
  // ADMIN_MENU / CINEMA_MENU：两份外壳只差菜单数据，布局与行为共用这一个组件
  menu: { type: Object, required: true },
})

const route = useRoute()
const router = useRouter()
const { user } = useAuth()
const { confirmLogout } = useLogout()

const userName = computed(() => user.value?.username || '')
const userAvatar = computed(() => avatarSrc(user.value?.avatar))

// 第一组默认展开（与原来 openedMenuKeys = ['1','2'] 的观感一致）
const openedMenuKeys = ref(props.menu.groups.slice(0, 2).map((g) => g.key))

// 下拉里的两个个人页：由首页路径派生（/manage/home -> /manage），避免在外壳里写死前缀
const basePath = computed(() => props.menu.home.replace(/\/home$/, ''))
const personPath = computed(() => `${basePath.value}/person`)
const passwordPath = computed(() => `${basePath.value}/password`)
</script>

<style scoped lang="scss">
@use '@/assets/css/admin-layout' as *;
</style>
