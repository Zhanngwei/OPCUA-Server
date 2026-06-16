<template>
  <div class="main-layout">
    <!-- 顶部导航菜单 -->
    <el-menu
      :default-active="activeIndex"
      class="top-menu"
      mode="horizontal"
      @select="handleSelect"
      background-color="#545c64"
      text-color="#fff"
      active-text-color="#ffd04b"
    >
      <el-menu-item index="status">
        <el-icon><Monitor /></el-icon>
        <span>{{ $t('nav.status') }}</span>
      </el-menu-item>
      <el-menu-item index="objects">
        <el-icon><FolderOpened /></el-icon>
        <span>{{ $t('nav.objects') }}</span>
      </el-menu-item>
      <el-menu-item index="endpoints">
        <el-icon><Connection /></el-icon>
        <span>{{ $t('nav.endpoints') }}</span>
      </el-menu-item>
      <el-menu-item index="certificates">
        <el-icon><Document /></el-icon>
        <span>{{ $t('nav.certificates') }}</span>
      </el-menu-item>
      <el-menu-item index="users">
        <el-icon><User /></el-icon>
        <span>{{ $t('nav.users') }}</span>
      </el-menu-item>

      <!-- 右侧菜单 -->
      <div class="menu-right">
        <LanguageSwitcher />

        <!-- 用户信息和退出 -->
        <el-dropdown class="user-dropdown">
          <span class="user-info">
            <el-icon><User /></el-icon>
            {{ userStore.username }}
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleLogout">
                <el-icon><SwitchButton /></el-icon>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-menu>

    <!-- 主内容区域 -->
    <div class="main-content">
      <router-view />
    </div>
  </div>
</template>

<script>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Monitor,
  FolderOpened,
  Connection,
  Document,
  User,
  ArrowDown,
  SwitchButton
} from '@element-plus/icons-vue'
import LanguageSwitcher from './LanguageSwitcher.vue'
import { useUserStore } from '@/stores/user'

export default {
  name: 'MainLayout',
  components: {
    Monitor,
    FolderOpened,
    Connection,
    Document,
    User,
    ArrowDown,
    SwitchButton,
    LanguageSwitcher
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const userStore = useUserStore()

    // 根据当前路由计算活动菜单项
    const activeIndex = computed(() => {
      const path = route.path
      if (path === '/' || path === '/objects') return 'objects'
      if (path === '/status') return 'status'
      if (path === '/endpoints') return 'endpoints'
      if (path === '/certificates') return 'certificates'
      if (path === '/users') return 'users'
      return 'objects' // 默认
    })

    const handleSelect = (key) => {
      // 根据菜单项导航到对应路由
      switch (key) {
        case 'status':
          router.push('/status')
          break
        case 'objects':
          router.push('/objects')
          break
        case 'endpoints':
          router.push('/endpoints')
          break
        case 'certificates':
          router.push('/certificates')
          break
        case 'users':
          router.push('/users')
          break
      }
    }

    // 退出登录处理
    const handleLogout = async () => {
      try {
        await ElMessageBox.confirm(
          '确定要退出登录吗？',
          '提示',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )

        await userStore.logout()
        ElMessage.success('已退出登录')
        router.push('/login')
      } catch (error) {
        if (error !== 'cancel') {
          console.error('Logout error:', error)
          ElMessage.error('退出登录失败')
        }
      }
    }

    return {
      activeIndex,
      handleSelect,
      handleLogout,
      userStore
    }
  }
}
</script>

<style scoped>
.main-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.top-menu {
  border-bottom: solid 1px #e6e6e6;
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.main-content {
  flex: 1;
  overflow: hidden;
}

/* 确保菜单项图标和文字对齐 */
.top-menu .el-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.top-menu .el-menu-item .el-icon {
  margin-right: 0;
}

/* 右侧菜单区域 */
.menu-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 16px;
  padding-right: 16px;
}

/* 用户下拉菜单样式 */
.user-dropdown {
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #fff;
  font-size: 14px;
  padding: 8px 12px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: rgba(255, 255, 255, 0.1);
}
</style>
