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
          <router-link
              to="/front/account"
              class="nav-item"
              :class="{ 'active': activePath === '/front/account' }"
          >
            我的账户
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

        <!-- 未登录：显示登录/注册（包一层 flex 容器，用 gap 统一「·」两侧留白，替代原来的 margin） -->
        <div v-if="!isLoggedIn" class="auth-links">
          <router-link to="/login" class="header-link">登录</router-link>
          <span class="header-divider">·</span>
          <router-link to="/register" class="header-link">注册</router-link>
        </div>

        <!-- 已登录：显示用户信息 -->
        <template v-else>
          <!-- 影院/管理员登录后显示后台入口 -->
          <el-button v-if="isCinema || isAdmin" type="primary" size="small" class="admin-btn" @click="goAdmin">
            管理后台
          </el-button>
          <el-dropdown trigger="click">
            <div class="user-info">
              <img :src="userAvatar" alt="用户头像" class="avatar">
              <span class="username">{{ userName }}</span>
              <el-icon><CaretBottom /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/front/person')">个人中心</el-dropdown-item>
                <el-dropdown-item @click="router.push('/front/account')">我的账户</el-dropdown-item>
                <el-dropdown-item @click="router.push('/front/password')">修改密码</el-dropdown-item>
                <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
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
              <li>余额充值为模拟数据</li>
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
const { user, logout: authLogout, isLoggedIn, isAdmin, isCinema } = useAuth()

const searchKeyword = ref('')
const activePath = ref('')

const goAdmin = () => {
  if (isAdmin.value) {
    router.push('/manage/home')
  } else if (isCinema.value) {
    router.push('/back/home')
  }
}

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
  } else if (path.startsWith('/front/account')) {
    activePath.value = '/front/account'
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
/* ============================================================
 * 顶部导航栏（Header）—— Flex 弹性自适应
 * 收缩优先级：左组(不缩) > 导航项(不缩、不换行) > 右组(可缩，搜索框是唯一泄压阀)
 * ============================================================ */
.front-header {
  /* 字号令牌已上移为全局令牌（tokens.scss 的 :root），组件不再重复声明。
   * 组件级只保留本组件专有的尺寸令牌，命名不得与全局令牌冲突。 */
  --header-height: 60px;
  --header-padding-x: 20px;
  --group-gap: 16px;
  --nav-gap: 24px;
  --search-width: 200px;
  --header-min-width: 1120px;     /* 临界阈值：低于此宽度停止压缩，改为整页横向滚动 */

  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--group-gap);          /* 三组之间弹性留白 */
  height: var(--header-height);
  padding: 0 var(--header-padding-x);
  background: white;
  box-shadow: var(--shadow-edge);
  /* 最小宽度保护：低于该阈值不再继续压缩，改为整页横向滚动。
     刻意不用 overflow: hidden —— 裁切会让按钮/输入框不可见不可点 */
  min-width: var(--header-min-width);
  box-sizing: border-box;
}

/* 三组容器公共布局（原 `.front-header > div` 改为显式类名，避免误伤新增子元素） */
.front-header-left,
.front-header-center,
.front-header-right {
  display: flex;
  align-items: center;
  min-width: 0;
}

/* 左组：Logo + 标题，永不压缩 */
.front-header-left {
  flex: 0 0 auto;
  gap: 10px;
}

/* 中组：导航，吸收富余空间；min-width:0 允许自身收缩而不撑破父容器 */
.front-header-center {
  flex: 1 1 auto;
  min-width: 0;
  justify-content: center;
}

.main-nav {
  display: flex;
  align-items: center;
  gap: var(--nav-gap);
  padding: 0;                     /* 去掉原 15px 纵向内边距，垂直居中交给 align-items */
}

.nav-item {
  flex: 0 0 auto;                 /* 导航项不参与压缩 */
  white-space: nowrap;            /* 菜单文字禁止换行，杜绝折行堆叠 */
  color: #1b191a;
  text-decoration: none;
  font-size: var(--fs-md);        /* 16px，与 1920 原观感一致 */
  line-height: 1.2;
  padding: 8px 12px;
  transition: color 0.3s, transform 0.3s;
  position: relative;
}

.nav-item:hover {
  color: var(--el-color-primary);
  transform: translateY(-2px);
}

/* 激活状态的样式 */
.active {
  color: var(--el-color-primary) !important;
  font-weight: var(--fw-bold);
}

.active::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  width: 100%;
  height: 2px;
  background-color: var(--el-color-primary);
  border-radius: 1px;
}

/* 右组：搜索 + 用户区；唯一的泄压阀（flex-shrink: 1） */
.front-header-right {
  flex: 0 1 auto;
  min-width: 0;
  gap: 15px;
}

.logo {
  height: 30px;
  flex: 0 0 auto;
  display: block;
}

.title {
  font-size: var(--fs-md);
  font-weight: bold;
  line-height: 1.2;
  margin: 0;                      /* 抵消 h1 默认外边距，避免在 60px 栏内撑高错位 */
  white-space: nowrap;            /* 标题禁止折行 */
}

.search-input {
  /* 定宽改为弹性基准：空间充足时 200px（与原来一致），不足时优先收窄搜索框 */
  flex-grow: 0;
  flex-shrink: 1;
  flex-basis: var(--search-width);
  min-width: 150px;
  height: 34px;
}

/* Element Plus 输入框内部同样要放开收缩限制，否则外层缩了内层仍撑破 */
.search-input :deep(.el-input__wrapper) {
  min-width: 0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 0 1 auto;
  min-width: 0;
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
  flex: 0 0 auto;
  border-radius: 50%;
  object-fit: cover;
}

.username {
  font-size: var(--fs-base);
  /* 超长用户名截断，避免撑爆右组引发元素互挤 */
  max-width: 6em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  font-weight: var(--fw-bold);
  margin-bottom: 10px;
  position: relative;
  padding-bottom: 10px;
  color: var(--el-color-primary);
}

.footer-title::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  width: 30px;
  height: 2px;
  background-color: var(--el-color-primary);
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
  color: var(--el-color-primary);
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
/* 未登录：登录 / 注册（间距由 gap 统一，替代原来的 margin） */
.auth-links {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 0 0 auto;
}
.header-link {
  color: #333;
  text-decoration: none;
  font-size: var(--fs-base);
  white-space: nowrap;
  transition: color 0.3s;
}
.header-link:hover {
  color: var(--el-color-primary);
}
.header-divider {
  color: #ccc;
}
.admin-btn {
  flex: 0 0 auto;
}

/* ---------- 窄屏逐级收窄间距与搜索框基准宽，给导航让出空间 ----------
   媒体查询按视口宽度匹配：窗口 <1120px 时头部元素仍保持 1120px 宽，
   但内部走最紧的一档规则，因此不会溢出也不会互相挤压 */
@media (max-width: 1400px) {
  .front-header {
    --nav-gap: 14px;
    --search-width: 185px;   /* 留够「请输入电影名称」占位符完整显示 */
  }
}

@media (max-width: 1200px) {
  .front-header {
    --nav-gap: 8px;
    --group-gap: 12px;
    --search-width: 185px;   /* 同上：保住占位符可读，压缩空间从导航间距里出 */
  }

  .nav-item {
    padding: 8px 8px;
  }
}
</style>
