import { defineStore } from 'pinia'
import axios from 'axios'

// 配置 axios 基础 URL
const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器 - 添加token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('authToken')
    console.log('Request interceptor - token:', token) // 调试日志
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
      console.log('Added Authorization header:', config.headers.Authorization) // 调试日志
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器 - 处理401错误
api.interceptors.response.use(
  (response) => {
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      // 检查是否是登录请求失败，如果是登录请求失败，不要自动跳转
      const isLoginRequest = error.config?.url?.includes('/auth/login')

      if (!isLoginRequest) {
        // 只有非登录请求的401错误才自动跳转（Token过期或无效）
        localStorage.removeItem('authToken')
        localStorage.removeItem('username')
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export const useUserStore = defineStore('user', {
  state: () => ({
    username: localStorage.getItem('username') || '',
    token: localStorage.getItem('authToken') || '',
    isLoggedIn: !!localStorage.getItem('authToken')
  }),

  actions: {
    // 设置用户信息
    setUser(userInfo) {
      this.username = userInfo.username
      this.token = userInfo.token
      this.isLoggedIn = true
      
      // 保存到localStorage
      localStorage.setItem('username', userInfo.username)
      localStorage.setItem('authToken', userInfo.token)
    },

    // 清除用户信息
    clearUser() {
      this.username = ''
      this.token = ''
      this.isLoggedIn = false
      
      // 清除localStorage
      localStorage.removeItem('username')
      localStorage.removeItem('authToken')
    },

    // 登录
    async login(credentials) {
      try {
        const response = await api.post('/auth/login', credentials)
        
        if (response.data.success) {
          this.setUser({
            username: credentials.username,
            token: response.data.token
          })
          return response.data
        } else {
          throw new Error(response.data.message || '登录失败')
        }
      } catch (error) {
        console.error('Login error:', error)
        throw error
      }
    },

    // 登出
    async logout() {
      try {
        if (this.token) {
          await api.post('/auth/logout')
        }
      } catch (error) {
        console.error('Logout error:', error)
      } finally {
        this.clearUser()
      }
    },

    // 验证token
    async validateToken() {
      try {
        const response = await api.get('/auth/validate')
        if (response.data.valid) {
          this.isLoggedIn = true
          return true
        } else {
          this.clearUser()
          return false
        }
      } catch (error) {
        console.error('Token validation error:', error)
        this.clearUser()
        return false
      }
    }
  },

  getters: {
    // 获取用户信息
    userInfo: (state) => ({
      username: state.username,
      isLoggedIn: state.isLoggedIn
    })
  }
})

// 导出配置好的axios实例供其他地方使用
export { api }
