import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../components/MainLayout.vue'
import LoginView from '../views/LoginView.vue'
import Status from '../views/Status.vue'
import OpcuaManager from '../views/OpcuaManager.vue'
import Endpoints from '../views/Endpoints.vue'
import Certificates from '../views/Certificates.vue'
import Users from '../views/Users.vue'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: MainLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/objects'
      },
      {
        path: 'status',
        name: 'Status',
        component: Status
      },
      {
        path: 'objects',
        name: 'Objects',
        component: OpcuaManager
      },
      {
        path: 'endpoints',
        name: 'Endpoints',
        component: Endpoints
      },
      {
        path: 'certificates',
        name: 'Certificates',
        component: Certificates
      },
      {
        path: 'users',
        name: 'Users',
        component: Users
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory('/'),
  routes
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()

  // 如果路由需要认证
  if (to.meta.requiresAuth !== false) {
    // 检查是否已登录
    if (!userStore.isLoggedIn) {
      // 未登录，重定向到登录页
      // 过滤掉静态文件路径，避免重定向循环
      const redirectPath = to.fullPath.endsWith('.html') || to.fullPath.startsWith('/static/') ? '/' : to.fullPath
      next({
        path: '/login',
        query: { redirect: redirectPath }
      })
      return
    }

    // 验证token是否有效
    const isValid = await userStore.validateToken()
    if (!isValid) {
      // Token无效，重定向到登录页
      // 过滤掉静态文件路径，避免重定向循环
      const redirectPath = to.fullPath.endsWith('.html') || to.fullPath.startsWith('/static/') ? '/' : to.fullPath
      next({
        path: '/login',
        query: { redirect: redirectPath }
      })
      return
    }
  }

  // 如果已登录且访问登录页，重定向到首页
  if (to.path === '/login' && userStore.isLoggedIn) {
    next('/')
    return
  }

  next()
})

export default router
