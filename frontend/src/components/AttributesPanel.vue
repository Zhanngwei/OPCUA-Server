<template>
  <div class="attributes-panel">
    <div class="panel-header">
      <el-select v-model="selectedView" placeholder="选择视图" style="width: 120px;">
        <el-option label="Basic" value="basic" />
        <el-option label="Advanced" value="advanced" />
      </el-select>
      <el-button :icon="Refresh" circle size="small" @click="refreshAttributes" />
    </div>

    <div class="attributes-content">
      <el-table
        :data="attributeData"
        class="property-table"
        :show-header="true"
        stripe
        size="small"
      >
        <el-table-column prop="attribute" label="Attribute" width="200">
          <template #default="{ row }">
            <div class="attribute-cell" :style="{ paddingLeft: row.parent ? '20px' : '0px' }">
              <el-icon
                v-if="row.expandable"
                class="expand-icon"
                :class="{ 'expanded': expandedRows.includes(row.attribute) }"
                @click="toggleExpand(row.attribute)"
              >
                <ArrowRight />
              </el-icon>
              <span>{{ row.attribute }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="value" label="Value">
          <template #default="{ row }">
            <div class="value-cell">
              <span v-if="!row.editable">{{ row.value }}</span>
              <el-input
                v-else-if="row.type === 'string'"
                v-model="row.value"
                size="small"
                @blur="updateValue(row)"
                @keyup.enter="updateValue(row)"
              />
              <el-input-number
                v-else-if="row.type === 'number'"
                v-model="row.value"
                size="small"
                @blur="updateValue(row)"
                @keyup.enter="updateValue(row)"
              />
              <el-switch
                v-else-if="row.type === 'boolean'"
                v-model="row.value"
                @change="updateValue(row)"
              />
              <el-select
                v-else-if="row.type === 'datatype'"
                v-model="row.value"
                size="small"
                @change="updateDataType(row)"
              >
                <el-option label="Double (浮点数)" value="double" />
                <el-option label="Integer (整数)" value="int" />
                <el-option label="Long (长整数)" value="long" />
                <el-option label="Boolean (布尔值)" value="boolean" />
                <el-option label="String (字符串)" value="string" />
              </el-select>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, ArrowRight } from '@element-plus/icons-vue'
import { useOpcuaStore } from '../stores/opcua'

export default {
  name: 'AttributesPanel',
  props: {
    nodeData: {
      type: Object,
      default: () => null
    }
  },
  setup(props) {
    const selectedView = ref('basic')
    const expandedRows = ref(['NodeId', 'DisplayName', 'Description']) // 默认展开这些行
    const opcuaStore = useOpcuaStore()

    // 解析NodeId字符串
    const parseNodeId = (nodeIdString) => {
      const result = {
        namespaceIndex: 0,
        identifier: '',
        identifierType: 0,
        identifierTypeName: 'Numeric'
      }

      if (!nodeIdString) return result

      // 解析命名空间索引
      const nsMatch = nodeIdString.match(/ns=(\d+)/)
      if (nsMatch) {
        result.namespaceIndex = parseInt(nsMatch[1])
      }

      // 解析标识符类型和值
      if (nodeIdString.includes(';i=')) {
        // Numeric
        const match = nodeIdString.match(/;i=(\d+)/)
        result.identifier = match ? match[1] : ''
        result.identifierType = 0
        result.identifierTypeName = 'Numeric'
      } else if (nodeIdString.includes(';s=')) {
        // String
        const match = nodeIdString.match(/;s=(.+)/)
        result.identifier = match ? match[1] : ''
        result.identifierType = 1
        result.identifierTypeName = 'String'
      } else if (nodeIdString.includes(';g=')) {
        // GUID
        const match = nodeIdString.match(/;g=(.+)/)
        result.identifier = match ? match[1] : ''
        result.identifierType = 2
        result.identifierTypeName = 'Guid'
      } else if (nodeIdString.includes(';b=')) {
        // Opaque
        const match = nodeIdString.match(/;b=(.+)/)
        result.identifier = match ? match[1] : ''
        result.identifierType = 3
        result.identifierTypeName = 'Opaque'
      }

      return result
    }

    // 获取命名空间URI
    const getNamespaceURI = (namespaceIndex) => {
      const namespaceMap = {
        0: 'http://opcfoundation.org/UA/',
        1: 'urn:eclipse:milo:hello-world',
        2: 'http://localhost:8080/opcua',
        3: 'http://www.prosysopc.com/OPCUA/SimulationNodes/'
      }
      return namespaceMap[namespaceIndex] || 'Unknown'
    }

    const attributeData = computed(() => {
      if (!props.nodeData) {
        return [
          { attribute: 'NodeId', value: '', editable: false },
          { attribute: 'NodeClass', value: '', editable: false },
          { attribute: 'DisplayName', value: '', editable: false },
          { attribute: 'Description', value: 'The browse entry point when looking for objects in the server ...', editable: false }
        ]
      }

      // 解析NodeId信息
      const nodeIdInfo = parseNodeId(props.nodeData.nodeId || `ns=2;s=${props.nodeData.path || 'unknown'}`)

      const baseAttributes = [
        {
          attribute: 'NodeId',
          value: props.nodeData.nodeId || `ns=2;s=${props.nodeData.path || 'unknown'}`,
          editable: false,
          expandable: true
        },
        {
          attribute: 'NamespaceIndex',
          value: nodeIdInfo.namespaceIndex,
          editable: false,
          parent: 'NodeId'
        },
        {
          attribute: 'NamespaceURI',
          value: getNamespaceURI(nodeIdInfo.namespaceIndex),
          editable: false,
          parent: 'NodeId'
        },
        {
          attribute: 'IdentifierType',
          value: `${nodeIdInfo.identifierType} (${nodeIdInfo.identifierTypeName})`,
          editable: false,
          parent: 'NodeId'
        },
        {
          attribute: 'Identifier',
          value: nodeIdInfo.identifier,
          editable: false,
          parent: 'NodeId'
        },
        {
          attribute: 'NodeClass',
          value: props.nodeData.type === 'folder' ? 'Object' : 'Variable',
          editable: false
        },
        {
          attribute: 'DisplayName',
          value: props.nodeData.displayName || props.nodeData.label || 'Unknown',
          editable: true,
          type: 'string',
          expandable: true
        },
        {
          attribute: 'Locale',
          value: '',
          editable: false,
          parent: 'DisplayName'
        },
        {
          attribute: 'Text',
          value: props.nodeData.displayName || props.nodeData.label || 'Unknown',
          editable: false,
          parent: 'DisplayName'
        },
        {
          attribute: 'Description',
          value: props.nodeData.description || `${props.nodeData.type || 'unknown'} node: ${props.nodeData.label || 'unknown'}`,
          editable: true,
          type: 'string',
          expandable: true
        },
        {
          attribute: 'Locale',
          value: '',
          editable: false,
          parent: 'Description'
        },
        {
          attribute: 'Text',
          value: props.nodeData.description || `${props.nodeData.type || 'unknown'} node: ${props.nodeData.label || 'unknown'}`,
          editable: false,
          parent: 'Description'
        },
        {
          attribute: 'EventNotifier',
          value: 'None',
          editable: false
        }
      ]

      // 如果是变量，添加变量特有属性
      if (props.nodeData.type === 'variable') {
        baseAttributes.push(
          {
            attribute: 'Value',
            value: props.nodeData.value !== undefined ? props.nodeData.value : 0,
            editable: true,
            type: getValueType(props.nodeData.dataType)
          },
          {
            attribute: 'DataType',
            value: getEditableDataTypeValue(props.nodeData),
            editable: isDataTypeEditable(props.nodeData),
            type: 'datatype'
          },
          {
            attribute: 'AccessLevel',
            value: props.nodeData.accessLevel || 'READ_WRITE',
            editable: false
          },
          {
            attribute: 'UserAccessLevel',
            value: props.nodeData.userAccessLevel || 'READ_WRITE',
            editable: false
          }
        )
      }

      // 过滤掉未展开的子属性
      return baseAttributes.filter(attr => {
        if (!attr.parent) return true
        return expandedRows.value.includes(attr.parent)
      })
    })



    const isDataTypeEditable = (nodeData) => {
      if (!nodeData || nodeData.type !== 'variable') return false
      if (!nodeData.path) return false
      return true
    }

    const mapNodeIdToSimpleType = (dataType) => {
      if (!dataType) return 'double'
      const normalizedType = dataType.startsWith('ns=0;') ? dataType : `ns=0;${dataType}`

      switch (normalizedType) {
        case 'ns=0;i=11':
          return 'double'
        case 'ns=0;i=6':
          return 'int'
        case 'ns=0;i=8':
          return 'long'
        case 'ns=0;i=1':
          return 'boolean'
        case 'ns=0;i=12':
          return 'string'
        default:
          return 'string'
      }
    }

    const getEditableDataTypeValue = (nodeData) => {
      if (!nodeData) return 'double'
      return mapNodeIdToSimpleType(nodeData.dataType || 'ns=0;i=11')
    }

    const getValueType = (dataType) => {
      if (!dataType) return 'string'

      const typeMap = {
        'ns=0;i=11': 'number', // Double
        'ns=0;i=6': 'number',  // Int32
        'ns=0;i=1': 'boolean', // Boolean
        'ns=0;i=12': 'string'  // String
      }

      return typeMap[dataType] || 'string'
    }

    // 将数据类型ID转换为可读格式
    const formatDataType = (dataType) => {
      if (!dataType) return 'Unknown'

      // 标准化数据类型格式
      let normalizedType = dataType
      if (dataType.startsWith('i=')) {
        normalizedType = `ns=0;${dataType}`
      }

      const typeMap = {
        'ns=0;i=1': '1 [Boolean]',
        'ns=0;i=2': '2 [SByte]',
        'ns=0;i=3': '3 [Byte]',
        'ns=0;i=4': '4 [Int16]',
        'ns=0;i=5': '5 [UInt16]',
        'ns=0;i=6': '6 [Int32]',
        'ns=0;i=7': '7 [UInt32]',
        'ns=0;i=8': '8 [Int64]',
        'ns=0;i=9': '9 [UInt64]',
        'ns=0;i=10': '10 [Float]',
        'ns=0;i=11': '11 [Double]',
        'ns=0;i=12': '12 [String]',
        'ns=0;i=13': '13 [DateTime]',
        'ns=0;i=14': '14 [Guid]',
        'ns=0;i=15': '15 [ByteString]',
        'ns=0;i=16': '16 [XmlElement]',
        'ns=0;i=17': '17 [NodeId]',
        'ns=0;i=18': '18 [ExpandedNodeId]',
        'ns=0;i=19': '19 [StatusCode]',
        'ns=0;i=20': '20 [QualifiedName]',
        'ns=0;i=21': '21 [LocalizedText]',
        'ns=0;i=22': '22 [ExtensionObject]',
        'ns=0;i=23': '23 [DataValue]',
        'ns=0;i=24': '24 [Variant]',
        'ns=0;i=25': '25 [DiagnosticInfo]'
      }

      return typeMap[normalizedType] || dataType
    }

    const updateDataType = async (row) => {
      if (!props.nodeData || props.nodeData.type !== 'variable') return
      if (!isDataTypeEditable(props.nodeData)) return

      try {
        const path = props.nodeData.path
        const dataType = row.value
        const value = props.nodeData.value

        await opcuaStore.updateVariableDataType(path, dataType, value)
        ElMessage.success('数据类型更新成功')
      } catch (error) {
        ElMessage.error('更新数据类型失败: ' + (error.message || '未知错误'))
      }
    }

    const updateValue = async (row) => {
      if (!props.nodeData || props.nodeData.type !== 'variable') return

      try {
        if (row.attribute === 'Value') {
          // TODO: 实现实际的更新逻辑
          console.log('更新值:', props.nodeData.path, row.value)
          ElMessage.success('值更新成功')
        }
        // 这里可以添加其他属性的更新逻辑
      } catch (error) {
        ElMessage.error('更新失败: ' + error.message)
      }
    }

    const refreshAttributes = () => {
      ElMessage.success('属性已刷新')
    }

    const toggleExpand = (attribute) => {
      const index = expandedRows.value.indexOf(attribute)
      if (index > -1) {
        expandedRows.value.splice(index, 1)
      } else {
        expandedRows.value.push(attribute)
      }
    }

    return {
      selectedView,
      attributeData,
      expandedRows,
      updateValue,
      refreshAttributes,
      toggleExpand,
      formatDataType,
      updateDataType,
      Refresh,
      ArrowRight
    }
  }
}
</script>

<style scoped>
.attributes-panel {
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

.attributes-content {
  flex: 1;
  overflow: auto;
}

.attribute-cell {
  display: flex;
  align-items: center;
}

.expand-icon {
  margin-right: 4px;
  font-size: 12px;
  color: #909399;
  cursor: pointer;
  transition: transform 0.2s;
}

.expand-icon:hover {
  color: #409eff;
}

.expand-icon.expanded {
  transform: rotate(90deg);
}

.value-cell {
  width: 100%;
}

.property-table :deep(.el-table__cell) {
  padding: 4px 0;
}

.property-table :deep(.el-input) {
  --el-input-height: 24px;
}

.property-table :deep(.el-input-number) {
  --el-input-height: 24px;
  width: 100%;
}
</style>
