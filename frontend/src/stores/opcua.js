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
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

export const useOpcuaStore = defineStore('opcua', {
  state: () => ({
    nodes: {
      folders: {},
      variables: {}
    },
    loading: false,
    error: null
  }),

  actions: {
    // 获取所有节点
    async fetchNodes() {
      this.loading = true
      this.error = null

      try {
        const response = await api.get('/nodes')
        console.log('API response:', response.data)

        // 确保数据结构正确
        const data = response.data || {}
        this.nodes = {
          folders: data.folders || {},
          variables: data.variables || {}
        }

        console.log('Processed nodes:', this.nodes)
        return this.nodes
      } catch (error) {
        console.error('Error fetching nodes:', error)
        this.error = error.message
        throw error
      } finally {
        this.loading = false
      }
    },

    // 创建文件夹
    async createFolder(folderData) {
      const response = await api.post('/nodes/folder', folderData)
      if (response.data.success) {
        return response.data
      } else {
        throw new Error(response.data.error || '创建文件夹失败')
      }
    },

    // 创建变量
    async createVariable(variableData) {
      try {
        const response = await api.post('/nodes/variable', variableData)
        if (response.data.success) {
          return response.data
        } else {
          throw new Error(response.data.error || '创建变量失败')
        }
      } catch (error) {
        const message =
          error.response?.data?.error ||
          (typeof error.response?.data === 'string' ? error.response.data : '') ||
          error.message ||
          '创建变量失败'
        throw new Error(message)
      }
    },

    // 更新变量值
    async updateVariable(path, value) {
      const response = await api.put(`/nodes/variable/${encodeURIComponent(path)}`, {
        value: value
      })
      if (response.data.success) {
        return response.data
      } else {
        throw new Error(response.data.error || '更新变量失败')
      }
    },

    // 更新变量数据类型
    async updateVariableDataType(path, dataType, value) {
      const payload = {
        path: path,
        dataType: dataType
      }

      if (value !== undefined) {
        payload.value = value
      }

      const response = await api.put('/nodes/variable/datatype', payload)
      if (response.data.success) {
        return response.data
      } else {
        throw new Error(response.data.error || '更新变量数据类型失败')
      }
    },

    // 重命名节点
    async renameNode(path, newName, newDisplayName) {
      const response = await api.put(`/nodes/${encodeURIComponent(path)}/rename`, {
        newName: newName,
        newDisplayName: newDisplayName
      })
      if (response.data.success) {
        return response.data
      } else {
        throw new Error(response.data.error || '重命名节点失败')
      }
    },

    // 删除节点
    async deleteNode(path) {
      const response = await api.delete(`/nodes/delete?path=${encodeURIComponent(path)}`)
      if (response.data.success) {
        return response.data
      } else {
        throw new Error(response.data.error || '删除节点失败')
      }
    },

    // 获取节点详细信息
    async getNodeDetails(path) {
      const response = await api.get(`/nodes/${encodeURIComponent(path)}/details`)
      return response.data
    },

    // 获取单个节点的值
    async getNodeValue(path) {
      try {
        const response = await api.get(`/nodes/${encodeURIComponent(path)}/value`)
        return response.data
      } catch (error) {
        console.error('Error getting node value:', error)
        throw error
      }
    }
  },

  getters: {
    // 获取所有文件夹路径
    folderPaths: (state) => {
      return Object.keys(state.nodes.folders || {})
    },

    // 获取所有变量路径
    variablePaths: (state) => {
      return Object.keys(state.nodes.variables || {})
    },

    // 根据路径获取节点
    getNodeByPath: (state) => {
      return (path) => {
        if (state.nodes.folders[path]) {
          return { ...state.nodes.folders[path], type: 'folder', path }
        }
        if (state.nodes.variables[path]) {
          return { ...state.nodes.variables[path], type: 'variable', path }
        }
        return null
      }
    }
  }
})
