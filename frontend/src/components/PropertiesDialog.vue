<template>
  <el-dialog
    v-model="dialogVisible"
    title="节点属性"
    width="600px"
    :close-on-click-modal="false"
    :close-on-press-escape="true"
  >
    <div class="dialog-content">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="节点ID">
          {{ nodeData?.nodeId || nodeData?.id || 'N/A' }}
        </el-descriptions-item>
        <el-descriptions-item label="显示名称">
          {{ nodeData?.displayName || nodeData?.label || 'N/A' }}
        </el-descriptions-item>
        <el-descriptions-item label="节点类型">
          <el-tag :type="getNodeTypeColor(nodeData?.type)">
            {{ getNodeTypeText(nodeData?.type) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="路径">
          {{ nodeData?.path || nodeData?.id || 'N/A' }}
        </el-descriptions-item>
        
        <!-- 变量特有属性 -->
        <template v-if="nodeData?.type === 'variable'">
          <el-descriptions-item label="数据类型">
            {{ nodeData?.dataType || 'N/A' }}
          </el-descriptions-item>
          <el-descriptions-item label="当前值">
            <span class="value-display">{{ formatValue(nodeData?.value) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="访问级别">
            <el-tag :type="getAccessLevelType(nodeData?.accessLevel)">
              {{ getAccessLevelText(nodeData?.accessLevel) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="用户访问级别">
            <el-tag :type="getAccessLevelType(nodeData?.userAccessLevel)">
              {{ getAccessLevelText(nodeData?.userAccessLevel) }}
            </el-tag>
          </el-descriptions-item>
        </template>

        <!-- 文件夹特有属性 -->
        <template v-if="nodeData?.type === 'folder'">
          <el-descriptions-item label="子节点数量">
            {{ getChildrenCount() }}
          </el-descriptions-item>
        </template>

        <!-- 通用属性 -->
        <el-descriptions-item label="命名空间">
          {{ getNamespace() }}
        </el-descriptions-item>
        <el-descriptions-item label="浏览名称">
          {{ nodeData?.browseName || nodeData?.label || 'N/A' }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 引用信息 -->
      <div class="references-section" v-if="nodeData?.references">
        <h4>引用关系</h4>
        <el-table :data="formatReferences()" size="small" border>
          <el-table-column prop="referenceType" label="引用类型" width="150" />
          <el-table-column prop="targetNodeId" label="目标节点" />
          <el-table-column prop="isForward" label="方向" width="80">
            <template #default="scope">
              <el-tag :type="scope.row.isForward ? 'success' : 'warning'" size="small">
                {{ scope.row.isForward ? '正向' : '反向' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">关闭</el-button>
        <el-button type="primary" @click="copyToClipboard">复制信息</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'PropertiesDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    nodeData: {
      type: Object,
      default: () => ({})
    }
  },
  emits: ['update:visible'],
  setup(props, { emit }) {
    const dialogVisible = computed({
      get: () => props.visible,
      set: (value) => emit('update:visible', value)
    })

    const getNodeTypeColor = (type) => {
      switch (type) {
        case 'folder': return 'primary'
        case 'variable': return 'success'
        case 'object': return 'warning'
        default: return 'info'
      }
    }

    const getNodeTypeText = (type) => {
      switch (type) {
        case 'folder': return '文件夹'
        case 'variable': return '变量'
        case 'object': return '对象'
        default: return '未知'
      }
    }

    const getAccessLevelType = (level) => {
      switch (level) {
        case 'READ_WRITE': return 'success'
        case 'READ_ONLY': return 'info'
        case 'WRITE_ONLY': return 'warning'
        default: return 'danger'
      }
    }

    const getAccessLevelText = (level) => {
      switch (level) {
        case 'READ_WRITE': return '读写'
        case 'READ_ONLY': return '只读'
        case 'WRITE_ONLY': return '只写'
        default: return '无权限'
      }
    }

    const formatValue = (value) => {
      if (value === null || value === undefined) return 'N/A'
      if (typeof value === 'boolean') return value ? 'True' : 'False'
      if (typeof value === 'number') return value.toString()
      return String(value)
    }

    const getChildrenCount = () => {
      // This would need to be calculated based on the tree structure
      return 'N/A'
    }

    const getNamespace = () => {
      const nodeId = props.nodeData?.nodeId || props.nodeData?.id
      if (nodeId && nodeId.includes('ns=')) {
        const match = nodeId.match(/ns=(\d+)/)
        return match ? `命名空间 ${match[1]}` : 'N/A'
      }
      return 'N/A'
    }

    const formatReferences = () => {
      const refs = props.nodeData?.references
      if (!refs || !Array.isArray(refs)) return []
      
      return refs.map(ref => ({
        referenceType: ref.referenceType || 'Unknown',
        targetNodeId: ref.targetNodeId || 'Unknown',
        isForward: ref.isForward !== false
      }))
    }

    const copyToClipboard = async () => {
      try {
        const info = [
          `节点ID: ${props.nodeData?.nodeId || props.nodeData?.id || 'N/A'}`,
          `显示名称: ${props.nodeData?.displayName || props.nodeData?.label || 'N/A'}`,
          `节点类型: ${getNodeTypeText(props.nodeData?.type)}`,
          `路径: ${props.nodeData?.path || props.nodeData?.id || 'N/A'}`,
        ]

        if (props.nodeData?.type === 'variable') {
          info.push(
            `数据类型: ${props.nodeData?.dataType || 'N/A'}`,
            `当前值: ${formatValue(props.nodeData?.value)}`,
            `访问级别: ${getAccessLevelText(props.nodeData?.accessLevel)}`
          )
        }

        await navigator.clipboard.writeText(info.join('\n'))
        ElMessage.success('节点信息已复制到剪贴板')
      } catch (error) {
        ElMessage.error('复制失败')
      }
    }

    const handleClose = () => {
      dialogVisible.value = false
    }

    return {
      dialogVisible,
      getNodeTypeColor,
      getNodeTypeText,
      getAccessLevelType,
      getAccessLevelText,
      formatValue,
      getChildrenCount,
      getNamespace,
      formatReferences,
      copyToClipboard,
      handleClose
    }
  }
}
</script>

<style scoped>
.dialog-content {
  padding: 0 8px;
}

.references-section {
  margin-top: 20px;
}

.references-section h4 {
  margin-bottom: 10px;
  color: #606266;
}

.value-display {
  font-family: 'Courier New', monospace;
  background-color: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
}

.dialog-footer {
  text-align: right;
}

:deep(.el-descriptions__label) {
  font-weight: 500;
}
</style>
