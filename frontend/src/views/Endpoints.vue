<template>
  <div class="endpoints-container">
    <div class="page-header">
      <h1>OPC UA Endpoints</h1>
      <p>Manage OPC UA server endpoints and security configurations</p>
    </div>

    <!-- 端点配置表单 -->
    <el-form :model="endpointConfig" label-width="140px" class="endpoint-form">
      <!-- UA TCP 端点配置 -->
      <div class="endpoint-group">
        <div class="endpoint-header">
          <el-checkbox v-model="endpointConfig.tcpEndpoint.enabled" @change="onConfigChange" />
          <span class="endpoint-title">UA TCP</span>
          <span class="endpoint-url">({{ getTcpEndpointUrl() }})</span>
        </div>

        <div class="endpoint-config" v-if="endpointConfig.tcpEndpoint.enabled">
          <el-row :gutter="20">
            <el-col :span="6">
              <el-form-item label="Port">
                <el-input-number
                  v-model="endpointConfig.tcpEndpoint.port"
                  :min="1"
                  :max="65535"
                  @change="onConfigChange"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="Server Name">
                <el-input
                  v-model="endpointConfig.tcpEndpoint.serverName"
                  @input="onConfigChange"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="Security Modes">
            <div class="security-modes">
              <el-checkbox
                v-model="endpointConfig.tcpEndpoint.securityModes.none"
                @change="onConfigChange"
              >
                {{ $t('endpoints.securityModes.none') }}
              </el-checkbox>
              <el-checkbox
                v-model="endpointConfig.tcpEndpoint.securityModes.sign"
                @change="onConfigChange"
              >
                {{ $t('endpoints.securityModes.sign') }}
              </el-checkbox>
              <el-checkbox
                v-model="endpointConfig.tcpEndpoint.securityModes.signAndEncrypt"
                @change="onConfigChange"
              >
                {{ $t('endpoints.securityModes.signAndEncrypt') }}
              </el-checkbox>
              <span class="security-note">(select at least one)</span>
            </div>
          </el-form-item>
        </div>
      </div>

      <!-- UA HTTPS 端点配置 -->
      <div class="endpoint-group">
        <div class="endpoint-header">
          <el-checkbox v-model="endpointConfig.httpsEndpoint.enabled" @change="onConfigChange" />
          <span class="endpoint-title">UA HTTPS</span>
          <span class="endpoint-url">({{ getHttpsEndpointUrl() }})</span>
        </div>

        <div class="endpoint-config" v-if="endpointConfig.httpsEndpoint.enabled">
          <el-row :gutter="20">
            <el-col :span="6">
              <el-form-item label="Port">
                <el-input-number
                  v-model="endpointConfig.httpsEndpoint.port"
                  :min="1"
                  :max="65535"
                  @change="onConfigChange"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="Server Name">
                <el-input
                  v-model="endpointConfig.httpsEndpoint.serverName"
                  @input="onConfigChange"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="Security Modes">
            <div class="security-modes">
              <el-checkbox
                v-model="endpointConfig.httpsEndpoint.securityModes.none"
                @change="onConfigChange"
              >
                {{ $t('endpoints.securityModes.none') }}
              </el-checkbox>
              <el-checkbox
                v-model="endpointConfig.httpsEndpoint.securityModes.sign"
                @change="onConfigChange"
              >
                {{ $t('endpoints.securityModes.sign') }}
              </el-checkbox>
            </div>
          </el-form-item>

          <el-form-item label="TLS Policies">
            <div class="tls-policies">
              <el-checkbox
                v-model="endpointConfig.httpsEndpoint.tlsPolicies.tls10"
                @change="onConfigChange"
              >
                TLS 1.0
              </el-checkbox>
              <el-checkbox
                v-model="endpointConfig.httpsEndpoint.tlsPolicies.tls11"
                @change="onConfigChange"
              >
                TLS 1.1
              </el-checkbox>
              <el-checkbox
                v-model="endpointConfig.httpsEndpoint.tlsPolicies.tls12"
                @change="onConfigChange"
              >
                TLS 1.2 PFS
              </el-checkbox>
            </div>
          </el-form-item>
        </div>
      </div>

      <!-- 安全策略配置 -->
      <div class="security-policies-section">
        <h3>{{ $t('endpoints.securityPolicies') }}</h3>
        <div class="security-policies">
          <el-checkbox
            v-model="endpointConfig.securityPolicies.basic128Rsa15"
            @change="onConfigChange"
          >
            Basic128Rsa15
          </el-checkbox>
          <el-checkbox
            v-model="endpointConfig.securityPolicies.basic256"
            @change="onConfigChange"
          >
            Basic256
          </el-checkbox>
          <el-checkbox
            v-model="endpointConfig.securityPolicies.basic256Sha256"
            @change="onConfigChange"
          >
            Basic256Sha256
          </el-checkbox>
          <el-checkbox
            v-model="endpointConfig.securityPolicies.aes128Sha256RsaOaep"
            @change="onConfigChange"
          >
            Aes128Sha256RsaOaep
          </el-checkbox>
          <el-checkbox
            v-model="endpointConfig.securityPolicies.aes256Sha256RsaPss"
            @change="onConfigChange"
          >
            Aes256Sha256RsaPss
          </el-checkbox>
        </div>
      </div>



      <!-- 操作按钮 -->
      <div class="action-buttons">
        <el-button @click="revertChanges">{{ $t('common.revert') }}</el-button>
        <el-button
          type="primary"
          @click="applyChanges"
          :disabled="!hasChanges"
          :loading="applying"
        >
          {{ $t('common.apply') }}
        </el-button>
        <span class="next-startup-note">(From next startup)</span>
      </div>
    </el-form>


  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

import { api } from '@/stores/user'

export default {
  name: 'EndpointsView',
  setup() {
    const loading = ref(false)
    const applying = ref(false)
    const hasChanges = ref(false)
    const originalConfig = ref(null)

    // 端点配置
    const endpointConfig = reactive({
      tcpEndpoint: {
        enabled: true,
        port: 4840,
        serverName: 'milo',
        securityModes: {
          none: true,
          sign: true,
          signAndEncrypt: true
        }
      },
      httpsEndpoint: {
        enabled: false,
        port: 53443,
        serverName: 'OPCUA/SimulationServer',
        securityModes: {
          none: true,
          sign: true
        },
        tlsPolicies: {
          tls10: true,
          tls11: true,
          tls12: true
        }
      },
      securityPolicies: {
        basic128Rsa15: true,
        basic256: true,
        basic256Sha256: true,
        aes128Sha256RsaOaep: true,
        aes256Sha256RsaPss: true
      }
    })



    // 计算属性
    const getTcpEndpointUrl = () => {
      return `opc.tcp://localhost:${endpointConfig.tcpEndpoint.port}/${endpointConfig.tcpEndpoint.serverName}`
    }

    const getHttpsEndpointUrl = () => {
      return `opc.https://localhost:${endpointConfig.httpsEndpoint.port}/${endpointConfig.httpsEndpoint.serverName}`
    }

    // 方法
    const onConfigChange = () => {
      hasChanges.value = true
    }

    const applyChanges = async () => {
      applying.value = true
      try {
        // 先保存配置到后端
        const response = await api.put('/endpoints/config', endpointConfig)

        if (response.data) {
          // 然后应用配置（重启服务器）
          const applyResponse = await api.post('/endpoints/apply')

          if (applyResponse.data.success) {
            // 保存原始配置
            originalConfig.value = JSON.parse(JSON.stringify(endpointConfig))
            hasChanges.value = false

            ElMessage.success('Endpoint configuration applied successfully. Changes will take effect from next startup.')
          } else {
            ElMessage.error(applyResponse.data.message || 'Failed to apply endpoint configuration')
          }
        }
      } catch (error) {
        console.error('Error applying endpoint configuration:', error)
        ElMessage.error(error.response?.data?.message || 'Failed to apply endpoint configuration')
      } finally {
        applying.value = false
      }
    }

    const revertChanges = () => {
      if (originalConfig.value) {
        Object.assign(endpointConfig, JSON.parse(JSON.stringify(originalConfig.value)))
        hasChanges.value = false
        ElMessage.info('Changes reverted')
      }
    }







    // 从后端加载配置
    const loadConfiguration = async () => {
      try {
        const response = await api.get('/endpoints/config')
        if (response.data) {
          Object.assign(endpointConfig, response.data)
          // 保存原始配置
          originalConfig.value = JSON.parse(JSON.stringify(response.data))
          hasChanges.value = false
        }
      } catch (error) {
        console.error('Error loading endpoint configuration:', error)
        // 如果加载失败，使用默认配置
        originalConfig.value = JSON.parse(JSON.stringify(endpointConfig))
      }
    }

    // 组件挂载时加载配置
    onMounted(() => {
      loadConfiguration()
    })

    onMounted(() => {
      // 保存初始配置
      originalConfig.value = JSON.parse(JSON.stringify(endpointConfig))
    })

    return {
      loading,
      applying,
      hasChanges,
      endpointConfig,
      getTcpEndpointUrl,
      getHttpsEndpointUrl,
      onConfigChange,
      applyChanges,
      revertChanges,
      loadConfiguration
    }
  }
}
</script>

<style scoped>
.endpoints-container {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
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

.endpoint-form {
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 20px;
  margin-bottom: 20px;
}

.endpoint-group {
  margin-bottom: 30px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
}

.endpoint-header {
  background: #f5f7fa;
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  gap: 12px;
}

.endpoint-title {
  font-weight: 600;
  color: #303133;
}

.endpoint-url {
  color: #909399;
  font-family: monospace;
  font-size: 12px;
}

.endpoint-config {
  padding: 20px;
}

.security-modes,
.tls-policies,
.security-policies {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
}

.security-note {
  color: #909399;
  font-size: 12px;
  font-style: italic;
}

.security-policies-section,
.bind-addresses-section,
.discovery-section,
.reverse-connections-section {
  margin-bottom: 30px;
  padding: 20px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.security-policies-section h3,
.bind-addresses-section h3,
.discovery-section h3,
.reverse-connections-section h3 {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.custom-addresses {
  margin-top: 12px;
  display: flex;
  gap: 12px;
  align-items: center;
}

.action-buttons {
  display: flex;
  gap: 12px;
  align-items: center;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
}

.next-startup-note {
  color: #909399;
  font-size: 12px;
  font-style: italic;
}


</style>
