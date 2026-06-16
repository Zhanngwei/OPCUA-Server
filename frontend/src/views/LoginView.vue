<template>
  <div class="login-container">
    <div class="login-box">
      <h2>OPC UA Server</h2>
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="rules"
        class="login-form"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="用户名"
            prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="login-button"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { api } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginForm = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const loading = ref(false)
const loginFormRef = ref(null)

const handleLogin = async () => {
  if (!loginFormRef.value) return

  try {
    await loginFormRef.value.validate()
    loading.value = true

    // 直接调用API而不是使用store方法
    const { data } = await api({
      url: '/auth/login',
      method: 'post',
      data: loginForm
    })

    console.log('Login response:', data) // 调试日志

    if (data.success) {
      // 手动保存token和用户信息
      localStorage.setItem('authToken', data.token)
      localStorage.setItem('username', loginForm.username)

      // 更新store状态
      userStore.setUser({
        username: loginForm.username,
        token: data.token
      })

      console.log('Token saved:', localStorage.getItem('authToken')) // 调试日志

      ElMessage.success('登录成功')

      const redirect = route.query.redirect || '/objects'
      router.push(redirect)
    } else {
      ElMessage.error(data.message || '登录失败')
    }
  } catch (error) {
    console.error('登录错误:', error)

    // 显示错误消息
    let errorMessage = '登录失败'
    if (error.response?.data?.message) {
      errorMessage = error.response.data.message
    } else if (error.message) {
      errorMessage = error.message
    }

    ElMessage.error(errorMessage)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  width: 100vw;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #1e88e5 0%, #1565c0 100%);
  position: fixed;
  top: 0;
  left: 0;
  overflow: hidden;
}

.login-box {
  width: 400px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.18);
  animation: fadeIn 0.5s ease-out;
}

.login-box h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #1565c0;
  font-size: 24px;
  font-weight: 600;
}

.login-form {
  margin-top: 20px;
}

.login-button {
  width: 100%;
  height: 40px;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 1px;
  background: linear-gradient(135deg, #1e88e5 0%, #1565c0 100%);
  border: none;
  margin-top: 10px;
}

:deep(.el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover) {
  background-color: rgba(255, 255, 255, 0.95);
}

:deep(.el-input__inner) {
  height: 40px;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style> 