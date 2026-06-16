<template>
  <el-dialog
    v-model="dialogVisible"
    title="Add New Variable"
    width="600px"
    :before-close="handleClose"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <div class="dialog-content">
      <p class="dialog-description">Set the parameters for the new Variable:</p>
      
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="140px"
        label-position="left"
      >
        <el-form-item label="Namespace" prop="namespace">
          <div style="display: flex; gap: 8px; align-items: center;">
            <el-select
              v-model="formData.namespace"
              placeholder="选择命名空间"
              style="flex: 1"
            >
              <el-option
                v-for="ns in namespaces"
                :key="ns.value"
                :label="ns.label"
                :value="ns.value"
              />
              <el-option
                label="&lt;Add New Namespace&gt;"
                value="<Add New Namespace>"
                style="color: #409eff; font-style: italic;"
              />
            </el-select>
          </div>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="NodeId Type" prop="nodeIdType">
              <el-select
                v-model="formData.nodeIdType"
                style="width: 100%"
                @change="handleNodeIdTypeChange"
              >
                <el-option label="Numeric" value="numeric" />
                <el-option label="String" value="string" />
                <el-option label="GUID" value="guid" />
                <el-option label="Opaque" value="opaque" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="NodeId Value" prop="nodeIdValue">
              <div style="display: flex; align-items: center; gap: 8px;">
                <el-input
                  v-model="formData.nodeIdValue"
                  :placeholder="getNodeIdPlaceholder()"
                  @blur="validateNodeId"
                />
                <el-button
                  v-if="formData.nodeIdType === 'numeric'"
                  type="primary"
                  size="small"
                  @click="generateNextNumericId"
                  :loading="generatingId"
                >
                  自动生成
                </el-button>
              </div>
              <div v-if="nodeIdValidation.message" :class="nodeIdValidation.isValid ? 'validation-success' : 'validation-error'">
                {{ nodeIdValidation.message }}
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 显示父文件夹信息 -->
        <el-form-item v-if="selectedNode && selectedNode.type === 'folder'" label="Parent Folder">
          <div class="parent-folder-info">
            <el-icon class="folder-icon"><Folder /></el-icon>
            <span class="folder-path">{{ selectedNode.path || 'Root' }}</span>
            <el-tag size="small" type="info">{{ selectedNode.displayName || selectedNode.label }}</el-tag>
          </div>
        </el-form-item>

        <el-form-item label="Name" prop="name">
          <el-input
            v-model="formData.name"
            placeholder="输入变量名称"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="Type" prop="type">
          <el-select v-model="formData.type" style="width: 100%">
            <el-option label="BaseDataVariableType" value="BaseDataVariableType" />
            <el-option label="PropertyType" value="PropertyType" />
            <el-option label="DataItemType" value="DataItemType" />
          </el-select>
        </el-form-item>

        <el-form-item label="Reference Type" prop="referenceType">
          <el-select v-model="formData.referenceType" style="width: 100%">
            <el-option label="Organizes" value="Organizes" />
            <el-option label="HasComponent" value="HasComponent" />
            <el-option label="HasProperty" value="HasProperty" />
          </el-select>
        </el-form-item>

        <el-form-item label="Data Type" prop="dataType">
          <el-select v-model="formData.dataType" style="width: 100%">
            <el-option label="Double (浮点数)" value="double" />
            <el-option label="Integer (整数)" value="int" />
            <el-option label="String (字符串)" value="string" />
            <el-option label="Boolean (布尔值)" value="boolean" />
            <el-option label="Long (长整数)" value="long" />
          </el-select>
        </el-form-item>

        <el-form-item label="Initial Value" prop="initialValue">
          <el-input
            v-model="formData.initialValue"
            :placeholder="getValuePlaceholder()"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="Access Level" prop="accessLevel">
          <el-select v-model="formData.accessLevel" style="width: 100%">
            <el-option label="Read/Write (读写)" value="READ_WRITE" />
            <el-option label="Read Only (只读)" value="READ_ONLY" />
            <el-option label="Write Only (只写)" value="WRITE_ONLY" />
            <el-option label="None (无权限)" value="NONE" />
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleConfirm">确定</el-button>
      </div>
    </template>
  </el-dialog>

  <!-- 创建命名空间对话框 -->
  <CreateNamespaceDialog
    v-model:visible="showCreateNamespaceDialog"
    @confirm="handleCreateNamespace"
    @cancel="handleCreateNamespaceCancel"
  />
</template>

<script>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Folder } from '@element-plus/icons-vue'
import CreateNamespaceDialog from './CreateNamespaceDialog.vue'
import { api } from '@/stores/user'

export default {
  name: 'AddVariableDialog',
  components: {
    CreateNamespaceDialog
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    selectedNode: {
      type: Object,
      default: null
    }
  },
  emits: ['update:visible', 'confirm'],
  setup(props, { emit }) {
    const formRef = ref(null)
    const showCreateNamespaceDialog = ref(false)
    const previousNamespace = ref('')
    const generatingId = ref(false)
    const nodeIdValidation = ref({ isValid: true, message: '' })

    const namespaces = ref([
      {
        label: 'http://www.prosysopc.com/OPCUA/SimulationNodes/',
        value: 'http://www.prosysopc.com/OPCUA/SimulationNodes/'
      },
      {
        label: 'http://localhost:8080/opcua',
        value: 'http://localhost:8080/opcua'
      }
    ])

    const formData = ref({
      namespace: 'http://localhost:8080/opcua',
      nodeIdType: 'numeric',
      nodeIdValue: '',
      name: '',
      type: 'BaseDataVariableType',
      referenceType: 'Organizes',
      dataType: 'double',
      initialValue: '',
      accessLevel: 'READ_WRITE'
    })

    const rules = {
      name: [
        { required: true, message: '请输入变量名称', trigger: 'blur' }
      ],
      namespace: [
        { required: true, message: '请选择命名空间', trigger: 'change' }
      ],
      nodeIdType: [
        { required: true, message: '请选择NodeId类型', trigger: 'change' }
      ],
      nodeIdValue: [
        { required: true, message: '请输入NodeId值', trigger: 'blur' }
      ],
      dataType: [
        { required: true, message: '请选择数据类型', trigger: 'change' }
      ]
    }

    const dialogVisible = computed({
      get: () => props.visible,
      set: (value) => emit('update:visible', value)
    })

    // 获取NodeId占位符文本
    const getNodeIdPlaceholder = () => {
      switch (formData.value.nodeIdType) {
        case 'numeric':
          return '例如: 1012'
        case 'string':
          return '例如: MyVariable'
        case 'guid':
          return '例如: 550e8400-e29b-41d4-a716-446655440000'
        case 'opaque':
          return '例如: SGVsbG9Xb3JsZA=='
        default:
          return '输入NodeId值'
      }
    }

    // 处理NodeId类型变化
    const handleNodeIdTypeChange = async (newType) => {
      if (newType === 'numeric') {
        await generateNextNumericId()
      } else {
        formData.value.nodeIdValue = ''
        nodeIdValidation.value = { isValid: true, message: '' }
      }
    }

    // 生成下一个可用的数字ID
    const generateNextNumericId = async () => {
      try {
        generatingId.value = true
        const response = await api.get('/nodes/next-numeric-id')
        if (response.data.nextId) {
          formData.value.nodeIdValue = response.data.nextId.toString()
          nodeIdValidation.value = { isValid: true, message: '✓ 可用的数字ID' }
        }
      } catch (error) {
        console.error('Failed to generate numeric ID:', error)
        ElMessage.error('生成数字ID失败')
      } finally {
        generatingId.value = false
      }
    }

    // 验证NodeId是否重复
    const validateNodeId = async () => {
      if (!formData.value.nodeIdValue.trim()) {
        nodeIdValidation.value = { isValid: true, message: '' }
        return
      }

      try {
        const response = await api.post('/nodes/check-nodeid', {
          nodeIdType: formData.value.nodeIdType,
          nodeIdValue: formData.value.nodeIdValue
        })

        if (response.data.exists) {
          nodeIdValidation.value = { isValid: false, message: '✗ NodeId已存在，请选择其他值' }
        } else {
          nodeIdValidation.value = { isValid: true, message: '✓ NodeId可用' }
        }
      } catch (error) {
        console.error('Failed to validate NodeId:', error)
        nodeIdValidation.value = { isValid: true, message: '' }
      }
    }

    // 监听对话框显示状态
    watch(() => props.visible, async (newVal) => {
      if (newVal) {
        // 如果默认是numeric类型，自动生成ID
        if (formData.value.nodeIdType === 'numeric') {
          await generateNextNumericId()
        }
      }
    })

    // 监听命名空间选择变化
    watch(() => formData.value.namespace, (newNamespace, oldNamespace) => {
      if (newNamespace === '<Add New Namespace>') {
        // 保存之前的命名空间值并立即重置
        previousNamespace.value = oldNamespace || 'http://localhost:8080/opcua'
        formData.value.namespace = previousNamespace.value
        showCreateNamespaceDialog.value = true
      }
    })

    const getValuePlaceholder = () => {
      switch (formData.value.dataType) {
        case 'double':
          return '例如: 25.5'
        case 'int':
          return '例如: 100'
        case 'string':
          return '例如: Hello World'
        case 'boolean':
          return '例如: true 或 false'
        case 'long':
          return '例如: 1000000'
        default:
          return '输入初始值'
      }
    }

    const handleClose = () => {
      dialogVisible.value = false
      resetForm()
    }

    const handleConfirm = async () => {
      try {
        await formRef.value.validate()

        // 检查NodeId是否有效
        if (!nodeIdValidation.value.isValid) {
          ElMessage.error('NodeId已存在，请选择其他值')
          return
        }

        // 转换初始值
        let initialValue = formData.value.initialValue
        if (initialValue) {
          switch (formData.value.dataType) {
            case 'double':
              initialValue = parseFloat(initialValue)
              break
            case 'int':
              initialValue = parseInt(initialValue)
              break
            case 'long':
              initialValue = parseInt(initialValue)
              break
            case 'boolean':
              initialValue = initialValue.toLowerCase() === 'true'
              break
            // string 保持原样
          }
        }

        const variableData = {
          path: formData.value.name,
          displayName: formData.value.name, // 使用name作为displayName
          dataType: formData.value.dataType,
          initialValue: initialValue,
          accessLevel: formData.value.accessLevel,
          namespace: formData.value.namespace,
          nodeIdType: formData.value.nodeIdType,
          nodeIdValue: formData.value.nodeIdValue,
          type: formData.value.type,
          referenceType: formData.value.referenceType
        }

        emit('confirm', variableData)
        dialogVisible.value = false
        resetForm()
      } catch (error) {
        ElMessage.error('请检查表单输入')
      }
    }

    const handleCreateNamespace = (namespaceData) => {
      // 添加新的命名空间到列表
      const newNamespace = {
        label: namespaceData.uri,
        value: namespaceData.uri
      }
      namespaces.value.push(newNamespace)

      // 设置为当前选中的命名空间
      formData.value.namespace = namespaceData.uri

      ElMessage.success('命名空间创建成功')
    }

    const handleCreateNamespaceCancel = () => {
      // 如果用户取消创建命名空间，恢复到之前的值
      formData.value.namespace = previousNamespace.value
    }

    const resetForm = () => {
      formData.value = {
        namespace: 'http://localhost:8080/opcua',
        nodeIdType: 'numeric',
        nodeIdValue: '',
        name: '',
        type: 'BaseDataVariableType',
        referenceType: 'Organizes',
        dataType: 'double',
        initialValue: '',
        accessLevel: 'READ_WRITE'
      }
      nodeIdValidation.value = { isValid: true, message: '' }
      formRef.value?.clearValidate()
    }

    return {
      formRef,
      formData,
      rules,
      dialogVisible,
      namespaces,
      showCreateNamespaceDialog,
      generatingId,
      nodeIdValidation,
      getNodeIdPlaceholder,
      handleNodeIdTypeChange,
      generateNextNumericId,
      validateNodeId,
      getValuePlaceholder,
      handleClose,
      handleConfirm,
      handleCreateNamespace,
      handleCreateNamespaceCancel,
      // 图标
      Folder
    }
  }
}
</script>

<style scoped>
.dialog-content {
  padding: 0 8px;
}

.dialog-description {
  margin-bottom: 20px;
  color: #606266;
  font-size: 14px;
}

.dialog-footer {
  text-align: right;
}

:deep(.el-form-item__label) {
  font-weight: normal;
  color: #606266;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

.validation-success {
  color: #67c23a;
  font-size: 12px;
  margin-top: 4px;
}

.validation-error {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 4px;
}

.parent-folder-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.parent-folder-info .folder-icon {
  color: #409eff;
  font-size: 16px;
}

.parent-folder-info .folder-path {
  color: #606266;
  font-family: monospace;
  font-size: 13px;
  flex: 1;
}
</style>
