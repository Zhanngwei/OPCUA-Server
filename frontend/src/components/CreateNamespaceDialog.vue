<template>
  <el-dialog
    v-model="dialogVisible"
    title="Create Namespace"
    width="500px"
    :before-close="handleClose"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <div class="dialog-content">
      <p class="dialog-description">What is the URI of the new empty namespace?</p>
      
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="80px"
        label-position="left"
      >
        <el-form-item label="URI:" prop="uri">
          <el-input
            v-model="formData.uri"
            placeholder="urn:LAPTOP-279267VD:OPCUA:SimulationServer"
            style="width: 100%"
          />
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
</template>

<script>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'CreateNamespaceDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:visible', 'confirm', 'cancel'],
  setup(props, { emit }) {
    const formRef = ref(null)
    
    const formData = ref({
      uri: ''
    })

    const rules = {
      uri: [
        { required: true, message: '请输入命名空间URI', trigger: 'blur' },
        { 
          pattern: /^[a-zA-Z][a-zA-Z0-9+.-]*:/, 
          message: '请输入有效的URI格式', 
          trigger: 'blur' 
        }
      ]
    }

    const dialogVisible = computed({
      get: () => props.visible,
      set: (value) => emit('update:visible', value)
    })

    const handleClose = () => {
      emit('cancel')
      dialogVisible.value = false
      resetForm()
    }

    const handleConfirm = async () => {
      try {
        await formRef.value.validate()
        
        const namespaceData = {
          uri: formData.value.uri
        }

        emit('confirm', namespaceData)
        dialogVisible.value = false
        resetForm()
      } catch (error) {
        ElMessage.error('请检查表单输入')
      }
    }

    const resetForm = () => {
      formData.value = {
        uri: ''
      }
      formRef.value?.clearValidate()
    }

    return {
      formRef,
      formData,
      rules,
      dialogVisible,
      handleClose,
      handleConfirm
    }
  }
}
</script>

<style scoped>
.dialog-content {
  padding: 0 20px;
}

.dialog-description {
  margin-bottom: 20px;
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
}

.dialog-footer {
  text-align: right;
}

.el-button + .el-button {
  margin-left: 10px;
}
</style>
