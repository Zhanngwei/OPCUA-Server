<template>
  <div class="references-panel">
    <div class="panel-header">
      <el-select v-model="selectedDirection" placeholder="Direction" size="small" style="width: 100px; margin-right: 8px;">
        <el-option label="Forward" value="Forward" />
        <el-option label="Inverse" value="Inverse" />
        <el-option label="Both" value="Both" />
      </el-select>
      <el-button :icon="Refresh" circle size="small" @click="refreshReferences" :loading="loading" />
    </div>

    <div class="references-content">
      <el-table
        :data="filteredReferenceData"
        class="property-table"
        :show-header="true"
        stripe
        size="small"
        v-loading="loading"
      >
        <el-table-column prop="referenceType" label="Reference Type" width="200">
          <template #default="{ row }">
            <el-icon class="reference-icon">
              <component :is="getReferenceIcon(row.referenceType)" />
            </el-icon>
            {{ row.referenceType }}
          </template>
        </el-table-column>
        <el-table-column prop="direction" label="Direction" width="100">
          <template #default="{ row }">
            <el-tag :type="row.direction === 'Forward' ? 'success' : 'info'" size="small">
              {{ row.direction }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetNode" label="Target Node" min-width="200">
          <template #default="{ row }">
            <div class="target-node">
              <el-icon class="node-icon">
                <component :is="getTargetIcon(row.targetNode)" />
              </el-icon>
              <span>{{ row.targetNode }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="browseName" label="Browse Name" width="150" />
      </el-table>
    </div>
  </div>
</template>

<script>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Link, Folder, Document, Setting } from '@element-plus/icons-vue'
import { api } from '@/stores/user'

export default {
  name: 'ReferencesPanel',
  props: {
    nodeData: {
      type: Object,
      default: null
    }
  },
  setup(props) {
    const referenceData = ref([])
    const loading = ref(false)
    const selectedDirection = ref('Forward')

    // 过滤后的引用数据
    const filteredReferenceData = computed(() => {
      if (selectedDirection.value === 'Both') {
        return referenceData.value
      }
      return referenceData.value.filter(ref => ref.direction === selectedDirection.value)
    })

    // 获取节点引用信息
    const fetchReferences = async () => {
      if (!props.nodeData || !props.nodeData.path) {
        referenceData.value = []
        return
      }

      try {
        loading.value = true
        const response = await api.get(`/nodes/references?path=${encodeURIComponent(props.nodeData.path)}`)

        if (response.data && response.data.references) {
          referenceData.value = response.data.references
        } else {
          referenceData.value = []
        }
      } catch (error) {
        console.error('Failed to fetch references:', error)
        ElMessage.error('获取引用信息失败')
        referenceData.value = []
      } finally {
        loading.value = false
      }
    }

    const refreshReferences = () => {
      fetchReferences()
    }

    // 监听选中节点变化
    watch(() => props.nodeData, () => {
      fetchReferences()
    }, { immediate: true })

    const getReferenceIcon = (referenceType) => {
      switch (referenceType) {
        case 'Organizes':
          return Link
        case 'HasTypeDefinition':
          return Setting
        case 'HasComponent':
          return Link
        case 'HasProperty':
          return Setting
        default:
          return Link
      }
    }

    const getTargetIcon = (targetNode) => {
      // 根据目标节点名称判断类型
      if (targetNode === 'Objects' || targetNode.includes('Folder')) {
        return Folder
      } else if (targetNode.includes('Type')) {
        return Setting
      } else {
        return Document
      }
    }

    return {
      referenceData,
      filteredReferenceData,
      loading,
      selectedDirection,
      getReferenceIcon,
      getTargetIcon,
      refreshReferences,
      Refresh
    }
  }
}
</script>

<style scoped>
.references-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 16px;
}

.references-content {
  flex: 1;
  overflow: auto;
}

.reference-icon {
  margin-right: 8px;
  color: #409eff;
}

.target-node {
  display: flex;
  align-items: center;
}

.node-icon {
  margin-right: 8px;
  font-size: 14px;
}

.property-table :deep(.el-table__cell) {
  padding: 4px 0;
}
</style>
