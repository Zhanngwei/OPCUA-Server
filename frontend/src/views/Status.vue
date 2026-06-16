<template>
  <div class="status-container">
    <div class="page-header">
      <h1>{{ $t('status.title') }}</h1>
      <p>{{ $t('status.description') }}</p>
    </div>

    <div class="status-grid">
      <!-- 服务器状态卡片 -->
      <el-card class="status-card">
        <template #header>
          <div class="card-header">
            <el-icon><Monitor /></el-icon>
            <span>{{ $t('status.serverStatus') }}</span>
          </div>
        </template>
        <div class="status-item">
          <span class="label">Status:</span>
          <el-tag :type="serverStatus.running ? 'success' : 'danger'">
            {{ serverStatus.running ? 'Running' : 'Stopped' }}
          </el-tag>
        </div>
        <div class="status-item">
          <span class="label">Uptime:</span>
          <span>{{ serverStatus.uptime }}</span>
        </div>
        <div class="status-item">
          <span class="label">Port:</span>
          <span>{{ serverStatus.port }}</span>
        </div>
        <div class="status-item">
          <span class="label">Endpoint URL:</span>
          <span class="endpoint-url">{{ serverStatus.endpointUrl }}</span>
        </div>
      </el-card>

      <!-- 连接统计卡片 -->
      <el-card class="status-card">
        <template #header>
          <div class="card-header">
            <el-icon><Connection /></el-icon>
            <span>{{ $t('status.connections') }}</span>
          </div>
        </template>
        <div class="status-item">
          <span class="label">{{ $t('status.activeConnections') }}:</span>
          <span class="metric-value">{{ connectionStats.active }}</span>
        </div>
        <div class="status-item">
          <span class="label">{{ $t('status.totalConnections') }}:</span>
          <span class="metric-value">{{ connectionStats.total }}</span>
        </div>
        <div class="status-item">
          <span class="label">{{ $t('status.failedConnections') }}:</span>
          <span class="metric-value error">{{ connectionStats.failed }}</span>
        </div>
      </el-card>

      <!-- 节点统计卡片 -->
      <el-card class="status-card">
        <template #header>
          <div class="card-header">
            <el-icon><FolderOpened /></el-icon>
            <span>{{ $t('status.nodes') }}</span>
          </div>
        </template>
        <div class="status-item">
          <span class="label">{{ $t('status.totalNodes') }}:</span>
          <span class="metric-value">{{ nodeStats.total }}</span>
        </div>
        <div class="status-item">
          <span class="label">{{ $t('status.variableNodes') }}:</span>
          <span class="metric-value">{{ nodeStats.variables }}</span>
        </div>
        <div class="status-item">
          <span class="label">{{ $t('status.objectNodes') }}:</span>
          <span class="metric-value">{{ nodeStats.objects }}</span>
        </div>
      </el-card>

      <!-- 性能指标卡片 -->
      <el-card class="status-card">
        <template #header>
          <div class="card-header">
            <el-icon><TrendCharts /></el-icon>
            <span>{{ $t('status.performance') }}</span>
          </div>
        </template>
        <div class="status-item">
          <span class="label">{{ $t('status.cpuUsage') }}:</span>
          <el-progress :percentage="performance.cpu" :color="getProgressColor(performance.cpu)" />
        </div>
        <div class="status-item">
          <span class="label">{{ $t('status.memoryUsage') }}:</span>
          <el-progress :percentage="performance.memory" :color="getProgressColor(performance.memory)" />
        </div>
        <div class="status-item">
          <span class="label">{{ $t('status.requestsPerSec') }}:</span>
          <span class="metric-value">{{ performance.requestsPerSec }}</span>
        </div>
      </el-card>
    </div>

    <!-- 最近活动日志 -->
    <el-card class="activity-log">
      <template #header>
        <div class="card-header">
          <el-icon><Document /></el-icon>
          <span>{{ $t('status.recentActivity') }}</span>
          <el-button size="small" @click="refreshLogs">
            <el-icon><Refresh /></el-icon>
            {{ $t('common.refresh') }}
          </el-button>
        </div>
      </template>
      <el-table :data="activityLogs" style="width: 100%" max-height="300">
        <el-table-column prop="timestamp" :label="$t('status.time')" width="180" />
        <el-table-column prop="level" :label="$t('status.level')" width="100">
          <template #default="scope">
            <el-tag :type="getLogLevelType(scope.row.level)" size="small">
              {{ scope.row.level }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" :label="$t('status.message')" />
        <el-table-column prop="source" :label="$t('status.source')" width="120" />
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Monitor,
  Connection,
  FolderOpened,
  TrendCharts,
  Document,
  Refresh
} from '@element-plus/icons-vue'
import { api } from '@/stores/user'

export default {
  name: 'StatusView',
  components: {
    Monitor,
    Connection,
    FolderOpened,
    TrendCharts,
    Document,
    Refresh
  },
  setup() {
    const serverStatus = ref({
      running: false,
      uptime: 'Loading...',
      port: 0,
      endpointUrl: 'Loading...'
    })

    const connectionStats = ref({
      active: 0,
      total: 0,
      failed: 0
    })

    const nodeStats = ref({
      total: 0,
      variables: 0,
      objects: 0
    })

    const performance = ref({
      cpu: 0,
      memory: 0,
      requestsPerSec: 0
    })

    const activityLogs = ref([
      {
        timestamp: '2024-01-15 14:32:15',
        level: 'INFO',
        message: 'Client connected from 192.168.1.100',
        source: 'Server'
      },
      {
        timestamp: '2024-01-15 14:31:45',
        level: 'WARN',
        message: 'High memory usage detected',
        source: 'Monitor'
      },
      {
        timestamp: '2024-01-15 14:30:22',
        level: 'INFO',
        message: 'Node value updated: Temperature_Sensor_01',
        source: 'NodeManager'
      },
      {
        timestamp: '2024-01-15 14:29:18',
        level: 'ERROR',
        message: 'Failed to authenticate client',
        source: 'Security'
      },
      {
        timestamp: '2024-01-15 14:28:55',
        level: 'INFO',
        message: 'Subscription created for client session',
        source: 'Subscription'
      }
    ])

    let refreshInterval = null

    const getProgressColor = (percentage) => {
      if (percentage < 50) return '#67c23a'
      if (percentage < 80) return '#e6a23c'
      return '#f56c6c'
    }

    const getLogLevelType = (level) => {
      switch (level) {
        case 'INFO': return 'success'
        case 'WARN': return 'warning'
        case 'ERROR': return 'danger'
        default: return 'info'
      }
    }

    const refreshLogs = async () => {
      try {
        const response = await api.get('/status/logs?limit=50')
        const data = response.data

        if (data.logs) {
          activityLogs.value = data.logs
          ElMessage.success('Logs refreshed')
        }
      } catch (error) {
        console.error('Failed to refresh logs:', error)
        ElMessage.error('Failed to refresh logs')
      }
    }

    const fetchStatus = async () => {
      try {
        // 获取所有状态信息
        const response = await api.get('/status/all')
        const data = response.data

        // 更新服务器状态
        if (data.server) {
          serverStatus.value = {
            running: data.server.running,
            uptime: data.server.uptime || 'Unknown',
            port: data.server.port || 0,
            endpointUrl: data.server.endpointUrl || 'Unknown'
          }
        }

        // 更新连接统计
        if (data.connections) {
          connectionStats.value = {
            active: data.connections.active || 0,
            total: data.connections.total || 0,
            failed: data.connections.failed || 0
          }
        }

        // 更新节点统计
        if (data.nodes) {
          nodeStats.value = {
            total: data.nodes.total || 0,
            variables: data.nodes.variables || 0,
            objects: data.nodes.objects || 0
          }
        }

        // 更新性能指标
        if (data.performance) {
          performance.value = {
            cpu: data.performance.cpu || 0,
            memory: data.performance.memory || 0,
            requestsPerSec: data.performance.requestsPerSec || 0
          }
        }

        // 更新活动日志
        if (data.logs && data.logs.logs) {
          activityLogs.value = data.logs.logs
        }

      } catch (error) {
        console.error('Failed to fetch status:', error)
        ElMessage.error('Failed to fetch server status')
      }
    }

    onMounted(() => {
      fetchStatus()
      // 每30秒刷新一次状态
      refreshInterval = setInterval(fetchStatus, 30000)
    })

    onUnmounted(() => {
      if (refreshInterval) {
        clearInterval(refreshInterval)
      }
    })

    return {
      serverStatus,
      connectionStats,
      nodeStats,
      performance,
      activityLogs,
      getProgressColor,
      getLogLevelType,
      refreshLogs
    }
  }
}
</script>

<style scoped>
.status-container {
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

.status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.status-card {
  height: fit-content;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.status-item:last-child {
  border-bottom: none;
}

.label {
  font-weight: 500;
  color: #606266;
}

.metric-value {
  font-weight: 600;
  color: #303133;
}

.metric-value.error {
  color: #f56c6c;
}

.endpoint-url {
  font-family: monospace;
  background-color: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}

.activity-log {
  margin-top: 24px;
}

.activity-log .card-header {
  justify-content: space-between;
}
</style>
