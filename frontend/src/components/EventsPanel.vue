<template>
  <div class="events-panel">
    <div class="panel-header">
      <el-button :icon="Refresh" circle size="small" @click="refreshEvents" />
      <el-button :icon="VideoPlay" size="small" @click="startMonitoring">
        {{ isMonitoring ? '停止监控' : '开始监控' }}
      </el-button>
    </div>

    <div class="events-content">
      <div v-if="!props.nodeData" class="no-selection">
        <el-empty description="请选择一个节点查看事件" />
      </div>
      
      <div v-else>
        <div class="event-info">
          <h4>事件监控 - {{ props.nodeData.label }}</h4>
          <p>节点类型: {{ props.nodeData.type }}</p>
          <p>监控状态: 
            <el-tag :type="isMonitoring ? 'success' : 'info'" size="small">
              {{ isMonitoring ? '监控中' : '未监控' }}
            </el-tag>
          </p>
        </div>

        <el-divider />

        <div class="events-list">
          <h5>事件历史</h5>
          <el-table
            :data="eventData"
            class="property-table"
            :show-header="true"
            stripe
            size="small"
            max-height="300"
          >
            <el-table-column prop="timestamp" label="时间戳" width="180">
              <template #default="{ row }">
                {{ formatTime(row.timestamp) }}
              </template>
            </el-table-column>
            <el-table-column prop="eventType" label="事件类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getEventTypeColor(row.eventType)" size="small">
                  {{ row.eventType }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="消息" />
            <el-table-column prop="severity" label="严重程度" width="100">
              <template #default="{ row }">
                <el-rate
                  v-model="row.severity"
                  :max="5"
                  disabled
                  size="small"
                  show-score
                />
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="eventData.length === 0" class="no-events">
          <el-empty description="暂无事件记录" />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, VideoPlay } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

export default {
  name: 'EventsPanel',
  props: {
    nodeData: {
      type: Object,
      default: null
    }
  },
  setup(props) {
    const isMonitoring = ref(false)
    const eventData = ref([])
    let monitoringInterval = null

    // 模拟事件数据
    const generateMockEvent = () => {
      const eventTypes = ['ValueChange', 'AlarmCondition', 'SystemEvent', 'UserAction']
      const messages = [
        '节点值发生变化',
        '检测到报警条件',
        '系统状态更新',
        '用户执行了操作',
        '连接状态改变',
        '配置已更新'
      ]

      return {
        timestamp: new Date(),
        eventType: eventTypes[Math.floor(Math.random() * eventTypes.length)],
        message: messages[Math.floor(Math.random() * messages.length)],
        severity: Math.floor(Math.random() * 5) + 1,
        nodeId: props.nodeData?.nodeId || 'Unknown'
      }
    }

    const formatTime = (timestamp) => {
      return dayjs(timestamp).format('YYYY-MM-DD HH:mm:ss')
    }

    const getEventTypeColor = (eventType) => {
      const colorMap = {
        'ValueChange': 'primary',
        'AlarmCondition': 'danger',
        'SystemEvent': 'warning',
        'UserAction': 'success'
      }
      return colorMap[eventType] || 'info'
    }

    const startMonitoring = () => {
      if (!props.nodeData) {
        ElMessage.warning('请先选择一个节点')
        return
      }

      if (isMonitoring.value) {
        // 停止监控
        if (monitoringInterval) {
          clearInterval(monitoringInterval)
          monitoringInterval = null
        }
        isMonitoring.value = false
        ElMessage.info('已停止事件监控')
      } else {
        // 开始监控
        isMonitoring.value = true
        ElMessage.success('开始监控事件')
        
        // 模拟事件生成
        monitoringInterval = setInterval(() => {
          if (Math.random() > 0.7) { // 30% 概率生成事件
            const newEvent = generateMockEvent()
            eventData.value.unshift(newEvent)
            
            // 限制事件数量
            if (eventData.value.length > 50) {
              eventData.value = eventData.value.slice(0, 50)
            }
          }
        }, 2000)
      }
    }

    const refreshEvents = () => {
      eventData.value = []
      ElMessage.success('事件列表已清空')
    }

    // 组件卸载时清理定时器
    onUnmounted(() => {
      if (monitoringInterval) {
        clearInterval(monitoringInterval)
      }
    })

    // 初始化一些示例事件
    onMounted(() => {
      if (props.nodeData) {
        eventData.value = [
          {
            timestamp: new Date(Date.now() - 300000),
            eventType: 'SystemEvent',
            message: '节点初始化完成',
            severity: 2,
            nodeId: props.nodeData.nodeId
          },
          {
            timestamp: new Date(Date.now() - 600000),
            eventType: 'UserAction',
            message: '节点被创建',
            severity: 1,
            nodeId: props.nodeData.nodeId
          }
        ]
      }
    })

    return {
      isMonitoring,
      eventData,
      formatTime,
      getEventTypeColor,
      startMonitoring,
      refreshEvents,
      Refresh,
      VideoPlay,
      props
    }
  }
}
</script>

<style scoped>
.events-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.events-content {
  flex: 1;
  overflow: auto;
}

.no-selection {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.event-info {
  margin-bottom: 16px;
}

.event-info h4 {
  margin: 0 0 8px 0;
  color: #303133;
}

.event-info p {
  margin: 4px 0;
  color: #606266;
  font-size: 14px;
}

.events-list h5 {
  margin: 16px 0 8px 0;
  color: #303133;
}

.no-events {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 150px;
}

.property-table :deep(.el-table__cell) {
  padding: 4px 0;
}
</style>
