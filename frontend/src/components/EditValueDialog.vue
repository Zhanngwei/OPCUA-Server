<template>
  <el-dialog
    v-model="dialogVisible"
    title="编辑变量值"
    width="400px"
    :close-on-click-modal="false"
    :close-on-press-escape="true"
  >
    <div class="dialog-content">
      <div class="dialog-description">
        编辑变量: {{ nodeData?.label || nodeData?.displayName }}
      </div>
      
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="100px"
        label-position="left"
      >
        <el-form-item label="当前值">
          <el-input
            :value="currentValue"
            readonly
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="数据类型">
          <el-input
            :value="dataType"
            readonly
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="新值" prop="newValue">
          <el-input
            v-if="dataType === 'string'"
            v-model="formData.newValue"
            placeholder="输入字符串值"
            style="width: 100%"
            @keyup.enter="handleConfirm"
          />
          <el-input-number
            v-else-if="dataType === 'double' || dataType === 'float'"
            v-model="formData.newValue"
            placeholder="输入数值"
            style="width: 100%"
            :precision="2"
            :step="0.1"
          />
          <el-input-number
            v-else-if="dataType === 'integer' || dataType === 'int32'"
            v-model="formData.newValue"
            placeholder="输入整数"
            style="width: 100%"
            :precision="0"
            :step="1"
          />
          <el-switch
            v-else-if="dataType === 'boolean'"
            v-model="formData.newValue"
            active-text="True"
            inactive-text="False"
          />
          <el-input
            v-else
            v-model="formData.newValue"
            placeholder="输入值"
            style="width: 100%"
            @keyup.enter="handleConfirm"
          />
        </el-form-item>

        <el-form-item label="访问级别">
          <el-tag :type="getAccessLevelType(accessLevel)">
            {{ getAccessLevelText(accessLevel) }}
          </el-tag>
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button 
          type="primary" 
          @click="handleConfirm"
          :disabled="!canWrite"
        >
          更新
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'EditValueDialog',
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
  emits: ['update:visible', 'confirm'],
  setup(props, { emit }) {
    const formRef = ref(null)
    
    const formData = ref({
      newValue: null
    })

    const currentValue = computed(() => {
      return props.nodeData?.value !== undefined ? props.nodeData.value : 'N/A'
    })

    const dataType = computed(() => {
      return props.nodeData?.dataType || 'unknown'
    })

    const accessLevel = computed(() => {
      return props.nodeData?.accessLevel || 'READ_ONLY'
    })

    const canWrite = computed(() => {
      const level = accessLevel.value
      return level === 'READ_WRITE' || level === 'WRITE_ONLY'
    })

    const rules = computed(() => ({
      newValue: [
        { required: true, message: '请输入新值', trigger: 'blur' }
      ]
    }))

    const dialogVisible = computed({
      get: () => props.visible,
      set: (value) => emit('update:visible', value)
    })

    // 监听对话框打开，初始化表单数据
    watch(() => props.visible, (visible) => {
      if (visible && props.nodeData) {
        formData.value = {
          newValue: props.nodeData.value
        }
      }
    })

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

    const handleClose = () => {
      dialogVisible.value = false
      resetForm()
    }

    const handleConfirm = async () => {
      if (!canWrite.value) {
        ElMessage.error('该变量不允许写入')
        return
      }

      try {
        await formRef.value.validate()
        
        const updateData = {
          path: props.nodeData.path || props.nodeData.id,
          value: formData.value.newValue,
          dataType: dataType.value
        }

        emit('confirm', updateData)
        dialogVisible.value = false
        resetForm()
      } catch (error) {
        ElMessage.error('请检查表单输入')
      }
    }

    const resetForm = () => {
      formData.value = {
        newValue: null
      }
      formRef.value?.clearValidate()
    }

    return {
      formRef,
      formData,
      rules,
      dialogVisible,
      currentValue,
      dataType,
      accessLevel,
      canWrite,
      getAccessLevelType,
      getAccessLevelText,
      handleClose,
      handleConfirm
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
</style>
