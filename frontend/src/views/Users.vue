<template>
  <div class="users-container">
    <div class="page-header">
      <h1>{{ $t('users.title') }}</h1>
      <p>{{ $t('users.description') }}</p>
    </div>

    <div class="users-layout">
      <!-- 左侧用户认证方法配置 -->
      <div class="auth-methods-panel">
        <div class="panel-header">
          <h3>{{ $t('users.userAuthenticationMethods') }}</h3>
          <span class="restart-note">({{ $t('users.changesAppliedAfterRestart') }})</span>
        </div>

        <div class="auth-methods">
          <div class="auth-method">
            <el-checkbox v-model="authMethods.anonymous" @change="onAuthMethodChange">
              {{ $t('users.anonymous') }}
            </el-checkbox>
          </div>
          <div class="auth-method">
            <el-checkbox v-model="authMethods.usernamePassword" @change="onAuthMethodChange">
              {{ $t('users.usernamePassword') }}
            </el-checkbox>
          </div>
          <div class="auth-method">
            <el-checkbox v-model="authMethods.certificate" @change="onAuthMethodChange">
              {{ $t('users.certificate') }}
            </el-checkbox>
          </div>
          <div class="auth-method">
            <el-checkbox v-model="authMethods.issuedToken" @change="onAuthMethodChange">
              {{ $t('users.issuedTokenExternalSystem') }}
            </el-checkbox>
          </div>
        </div>

        <div class="auth-methods-actions">
          <el-button
            type="primary"
            @click="saveAuthMethods"
            :loading="savingAuthMethods"
            :disabled="!authMethodsChanged"
            size="small"
          >
            {{ $t('common.save') }}
          </el-button>
          <el-button
            @click="revertAuthMethods"
            :disabled="!authMethodsChanged"
            size="small"
          >
            {{ $t('common.revert') }}
          </el-button>
        </div>
      </div>

      <!-- 右侧用户管理 -->
      <div class="users-panel">
        <div class="panel-header">
          <h3>{{ $t('users.users') }}</h3>
          <el-button type="primary" @click="showAddUserDialog = true" size="small">
            {{ $t('users.addNewUser') }}
          </el-button>
        </div>

        <div class="users-list">
          <div
            v-for="user in users"
            :key="user.id"
            :class="['user-item', { 'selected': selectedUser?.id === user.id, 'disabled': !user.enabled }]"
            @click="selectUser(user)"
          >
            <div class="user-info">
              <div class="username">{{ user.username }}</div>
              <div class="user-role">{{ getUserRoleText(user.role) }}</div>
            </div>
            <div class="user-status">
              <el-tag
                :type="user.enabled ? 'success' : 'danger'"
                size="small"
              >
                {{ user.enabled ? $t('users.enabled') : $t('users.disabled') }}
              </el-tag>
            </div>
            <div class="user-actions" @click.stop>
              <el-button
                type="primary"
                size="small"
                @click="openChangePasswordDialog(user)"
              >
                {{ $t('users.changePassword') }}
              </el-button>
              <el-button
                :type="user.enabled ? 'warning' : 'success'"
                size="small"
                @click="toggleUserStatus(user)"
              >
                {{ user.enabled ? $t('users.disable') : $t('users.enable') }}
              </el-button>
              <el-button
                type="danger"
                size="small"
                @click="confirmDeleteUser(user)"
                :disabled="user.username === 'admin'"
              >
                {{ $t('users.delete') }}
              </el-button>
            </div>
          </div>
        </div>

        <!-- 添加用户表单 -->
        <div v-if="showAddUserDialog" class="add-user-form">
          <div class="form-header">
            <h4>{{ $t('users.addNewUser') }}</h4>
          </div>

          <el-form :model="newUser" :rules="userRules" ref="userFormRef" label-position="top">
            <el-form-item :label="$t('users.username')" prop="username">
              <el-input
                v-model="newUser.username"
                :placeholder="$t('users.username')"
                maxlength="20"
              />
            </el-form-item>

            <el-form-item :label="$t('users.password')" prop="password">
              <el-input
                v-model="newUser.password"
                type="password"
                :placeholder="$t('users.password')"
                show-password
              />
            </el-form-item>

            <el-form-item :label="$t('users.confirmPassword')" prop="confirmPassword">
              <el-input
                v-model="newUser.confirmPassword"
                type="password"
                :placeholder="$t('users.confirmPassword')"
                show-password
              />
            </el-form-item>

            <div class="form-actions">
              <el-button @click="cancelAddUser">
                {{ $t('common.cancel') }}
              </el-button>
              <el-button type="primary" @click="addUser">
                {{ $t('users.addUser') }}
              </el-button>
            </div>
          </el-form>
        </div>
      </div>
    </div>

    <!-- 修改密码对话框 -->
    <el-dialog
      v-model="showChangePasswordDialog"
      :title="$t('users.changePassword')"
      width="400px"
      @close="cancelChangePassword"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <div class="dialog-content">
        <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="120px">
          <el-form-item :label="$t('users.username')">
            <el-input v-model="currentUser.username" disabled />
          </el-form-item>

          <el-form-item :label="$t('users.newPassword')" prop="newPassword">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              :placeholder="$t('users.enterNewPassword')"
              show-password
            />
          </el-form-item>

          <el-form-item :label="$t('users.confirmPassword')" prop="confirmPassword">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              :placeholder="$t('users.confirmNewPassword')"
              show-password
            />
          </el-form-item>

          <div class="form-actions">
            <el-button @click="cancelChangePassword">
              {{ $t('common.cancel') }}
            </el-button>
            <el-button type="primary" @click="changePassword">
              {{ $t('users.changePassword') }}
            </el-button>
          </div>
        </el-form>
      </div>
    </el-dialog>

    <!-- 删除用户确认对话框 -->
    <el-dialog
      v-model="showDeleteConfirmDialog"
      :title="$t('users.deleteUser')"
      width="400px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <div class="dialog-content">
        <p>{{ $t('users.deleteUserConfirm', { username: userToDelete?.username }) }}</p>
        <div class="form-actions">
          <el-button @click="showDeleteConfirmDialog = false">
            {{ $t('common.cancel') }}
          </el-button>
          <el-button type="danger" @click="deleteUser">
            {{ $t('users.delete') }}
          </el-button>
        </div>
      </div>
    </el-dialog>

  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/stores/user'

export default {
  name: 'UsersView',
  setup() {
    const loading = ref(false)
    const selectedUser = ref(null)
    const showAddUserDialog = ref(false)
    const showChangePasswordDialog = ref(false)
    const showDeleteConfirmDialog = ref(false)
    const users = ref([])
    const currentUser = ref({})
    const userToDelete = ref(null)

    // 认证方法配置
    const authMethods = reactive({
      anonymous: false,
      usernamePassword: false,
      certificate: false,
      issuedToken: false
    })

    // 认证方法相关状态
    const originalAuthMethods = ref({})
    const authMethodsChanged = ref(false)
    const savingAuthMethods = ref(false)

    // 新用户表单
    const newUser = reactive({
      username: '',
      password: '',
      confirmPassword: '',
      role: 'user'
    })

    // 修改密码表单
    const passwordForm = reactive({
      newPassword: '',
      confirmPassword: ''
    })

    // 表单验证规则
    const userRules = {
      username: [
        { required: true, message: 'Please enter username', trigger: 'blur' },
        { min: 3, max: 20, message: 'Username must be 3-20 characters', trigger: 'blur' }
      ],
      password: [
        { required: true, message: 'Please enter password', trigger: 'blur' },
        { min: 6, message: 'Password must be at least 6 characters', trigger: 'blur' }
      ],
      confirmPassword: [
        { required: true, message: 'Please confirm password', trigger: 'blur' },
        {
          validator: (rule, value, callback) => {
            if (value !== newUser.password) {
              callback(new Error('Passwords do not match'))
            } else {
              callback()
            }
          },
          trigger: 'blur'
        }
      ]
    }

    // 密码修改验证规则
    const passwordRules = {
      newPassword: [
        { required: true, message: 'Please input new password', trigger: 'blur' },
        { min: 6, message: 'Password should be at least 6 characters', trigger: 'blur' }
      ],
      confirmPassword: [
        { required: true, message: 'Please confirm new password', trigger: 'blur' },
        {
          validator: (rule, value, callback) => {
            if (value !== passwordForm.newPassword) {
              callback(new Error('Passwords do not match'))
            } else {
              callback()
            }
          },
          trigger: 'blur'
        }
      ]
    }

    // 认证方法相关函数
    const fetchAuthMethods = async () => {
      try {
        const response = await api.get('/auth-methods')
        console.log('Fetched auth methods:', response.data)

        // 明确更新每个属性以确保响应式更新
        authMethods.anonymous = response.data.anonymous || false
        authMethods.usernamePassword = response.data.usernamePassword || false
        authMethods.certificate = response.data.certificate || false
        authMethods.issuedToken = response.data.issuedToken || false

        originalAuthMethods.value = { ...response.data }
        authMethodsChanged.value = false

        console.log('Updated auth methods:', authMethods)
      } catch (error) {
        console.error('Failed to fetch auth methods:', error)
        ElMessage.error('Failed to load authentication methods')
      }
    }

    const onAuthMethodChange = () => {
      authMethodsChanged.value = JSON.stringify(authMethods) !== JSON.stringify(originalAuthMethods.value)
    }

    const saveAuthMethods = async () => {
      savingAuthMethods.value = true
      try {
        await api.put('/auth-methods', authMethods)
        originalAuthMethods.value = { ...authMethods }
        authMethodsChanged.value = false
        ElMessage.success('Authentication methods saved successfully')
      } catch (error) {
        console.error('Failed to save auth methods:', error)
        ElMessage.error('Failed to save authentication methods')
      } finally {
        savingAuthMethods.value = false
      }
    }

    const revertAuthMethods = () => {
      Object.assign(authMethods, originalAuthMethods.value)
      authMethodsChanged.value = false
    }

    // 获取用户列表
    const fetchUsers = async () => {
      loading.value = true
      try {
        const response = await api.get('/users')
        users.value = response.data
      } catch (error) {
        console.error('Failed to fetch users:', error)
        ElMessage.error('Failed to load users')
      } finally {
        loading.value = false
      }
    }

    // 选择用户
    const selectUser = (user) => {
      selectedUser.value = user
    }

    // 获取用户角色文本
    const getUserRoleText = (role) => {
      const roleMap = {
        'admin': 'Administrator',
        'user': 'User',
        'readonly': 'Read Only'
      }
      return roleMap[role] || role
    }

    // 添加用户
    const addUser = async () => {
      try {
        const userData = {
          username: newUser.username,
          password: newUser.password,
          role: newUser.role,
          enabled: true,
          authenticationMethods: {
            anonymous: false,
            usernamePassword: true,
            certificate: false,
            issuedToken: false
          }
        }

        await api.post('/users', userData)
        ElMessage.success('User added successfully')

        // 重置表单
        Object.assign(newUser, {
          username: '',
          password: '',
          confirmPassword: '',
          role: 'user'
        })

        showAddUserDialog.value = false
        await fetchUsers()
      } catch (error) {
        ElMessage.error(error.response?.data?.message || 'Failed to add user')
      }
    }

    // 取消添加用户
    const cancelAddUser = () => {
      Object.assign(newUser, {
        username: '',
        password: '',
        confirmPassword: '',
        role: 'user'
      })
      showAddUserDialog.value = false
    }

    // 显示修改密码对话框
    const openChangePasswordDialog = (user) => {
      currentUser.value = { ...user }
      Object.assign(passwordForm, {
        newPassword: '',
        confirmPassword: ''
      })
      showChangePasswordDialog.value = true
    }

    // 修改密码
    const changePassword = async () => {
      try {
        const userData = {
          password: passwordForm.newPassword
        }

        await api.put(`/users/${currentUser.value.id}`, userData)
        ElMessage.success('Password changed successfully')

        // 重置表单
        Object.assign(passwordForm, {
          newPassword: '',
          confirmPassword: ''
        })

        showChangePasswordDialog.value = false
      } catch (error) {
        ElMessage.error(error.response?.data?.message || 'Failed to change password')
      }
    }

    // 取消修改密码
    const cancelChangePassword = () => {
      Object.assign(passwordForm, {
        newPassword: '',
        confirmPassword: ''
      })
      showChangePasswordDialog.value = false
    }

    // 确认删除用户
    const confirmDeleteUser = (user) => {
      if (user.username === 'admin') {
        ElMessage.warning('Cannot delete admin user')
        return
      }
      userToDelete.value = user
      showDeleteConfirmDialog.value = true
    }

    // 删除用户
    const deleteUser = async () => {
      try {
        await api.delete(`/users/${userToDelete.value.id}`)
        ElMessage.success('User deleted successfully')

        showDeleteConfirmDialog.value = false
        userToDelete.value = null

        // 如果删除的是当前选中的用户，清除选择
        if (selectedUser.value?.id === userToDelete.value?.id) {
          selectedUser.value = null
        }

        await fetchUsers()
      } catch (error) {
        ElMessage.error(error.response?.data?.message || 'Failed to delete user')
      }
    }

    // 切换用户启用/禁用状态
    const toggleUserStatus = async (user) => {
      try {
        const newStatus = !user.enabled
        await api.post(`/users/${user.id}/toggle-status`)

        ElMessage.success(`User ${newStatus ? 'enabled' : 'disabled'} successfully`)
        await fetchUsers()
      } catch (error) {
        ElMessage.error(error.response?.data?.message || 'Failed to toggle user status')
      }
    }

    // 生命周期钩子
    onMounted(() => {
      fetchUsers()
      fetchAuthMethods()
    })

    return {
      loading,
      selectedUser,
      showAddUserDialog,
      showChangePasswordDialog,
      showDeleteConfirmDialog,
      users,
      currentUser,
      userToDelete,
      authMethods,
      authMethodsChanged,
      savingAuthMethods,
      newUser,
      passwordForm,
      userRules,
      passwordRules,
      selectUser,
      getUserRoleText,
      addUser,
      cancelAddUser,
      openChangePasswordDialog,
      changePassword,
      cancelChangePassword,
      confirmDeleteUser,
      deleteUser,
      toggleUserStatus,
      onAuthMethodChange,
      saveAuthMethods,
      revertAuthMethods
    }
  }
}
</script>

<style scoped>
.users-container {
  padding: 20px;
  height: 100%;
  overflow: hidden;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.page-header p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.users-layout {
  display: flex;
  height: calc(100vh - 140px);
  gap: 20px;
}

.auth-methods-panel {
  width: 300px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  background: #fff;
}

.panel-header {
  padding: 12px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

.panel-header h3 {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.restart-note {
  font-size: 12px;
  color: #909399;
  font-style: italic;
}

.auth-methods {
  padding: 16px;
}

.auth-method {
  margin-bottom: 12px;
}

.auth-methods-actions {
  padding: 0 16px 16px 16px;
  display: flex;
  gap: 8px;
}

.users-panel {
  flex: 1;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.users-list {
  flex: 1;
  overflow-y: auto;
  border-bottom: 1px solid #e4e7ed;
}

.user-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s;
  position: relative;
}

.user-item:hover {
  background-color: #f5f7fa;
}

.user-item:hover .user-actions {
  opacity: 1;
}

.user-item.selected {
  background-color: #e6f7ff;
  border-left: 3px solid #409eff;
}

.user-item.disabled {
  opacity: 0.6;
}

.user-actions {
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.2s;
  margin-left: 8px;
}

.user-actions .el-button {
  padding: 4px 8px;
  font-size: 12px;
}

.user-info {
  flex: 1;
}

.username {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 2px;
}

.user-role {
  font-size: 11px;
  color: #909399;
}

.user-status {
  margin-left: 8px;
}

.add-user-form {
  padding: 16px;
  border-top: 1px solid #e4e7ed;
  background: #fafafa;
}

.form-header {
  margin-bottom: 16px;
}

.form-header h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.form-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 16px;
}

.dialog-content {
  padding: 0;
}

.dialog-content p {
  margin: 0 0 20px 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
}
</style>
