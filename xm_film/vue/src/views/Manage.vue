<template>
  <AdminShell :menu="menu" />
</template>

<script setup>
import AdminShell from '@/views/AdminShell.vue'
import { ADMIN_MENU } from '@/constants/adminMenu'

// 管理员后台：布局与行为在 AdminShell，这里只给菜单数据（FE-64）
const menu = ADMIN_MENU
</script>
