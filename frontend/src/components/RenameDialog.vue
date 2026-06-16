<template>
  <el-dialog
    v-model="dialogVisible"
    title="重命名节点"
    width="400px"
    :close-on-click-modal="false"
    :close-on-press-escape="true"
  >
    <div class="dialog-content">
      <div class="dialog-description">
        重命名 {{ nodeData?.type === 'folder' ? '文件夹' : '变量' }}: {{ nodeData?.label }}
      </div>
      
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="100px"
        label-position="left"
      >
        <el-form-item label="新名称" prop="newName">
          <el-input
            v-model="formData.newName"
            placeholder="输入新的名称"
            style="width: 100%"
            @keyup.enter="handleConfirm"
          />
        </el-form-item>

        <el-form-item label="显示名称" prop="newDisplayName">
          <el-input
            v-model="formData.newDisplayName"
            placeholder="输入新的显示名称"
            style="width: 100%"
            @keyup.enter="handleConfirm"
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
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'RenameDialog',
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
      newName: '',
      newDisplayName: ''
    })

    const rules = {
      newName: [
        { required: true, message: '请输入新名称', trigger: 'blur' },
        {
          pattern: /^[a-zA-Z0-9_\-/]+$/,
          message: '名称只能包含字母、数字、下划线、连字符和斜杠',
          trigger: 'blur'
        }
      ],
      newDisplayName: [
        { required: true, message: '请输入新显示名称', trigger: 'blur' }
      ]
    }

    const dialogVisible = computed({
      get: () => props.visible,
      set: (value) => emit('update:visible', value)
    })

    // 监听对话框打开，初始化表单数据
    watch(() => props.visible, (visible) => {
      if (visible && props.nodeData) {
        // 从路径中提取当前名称
        const path = props.nodeData.path || props.nodeData.id
        const pathParts = path.split('/')
        const currentName = pathParts[pathParts.length - 1]
        
        formData.value = {
          newName: currentName,
          newDisplayName: props.nodeData.label || props.nodeData.displayName || currentName
        }
      }
    })

    // 监听名称变化，自动填充显示名称
    watch(() => formData.value.newName, (newName) => {
      if (newName && !formData.value.newDisplayName) {
        formData.value.newDisplayName = newName
      }
    })

    const handleClose = () => {
      dialogVisible.value = false
      resetForm()
    }

    const handleConfirm = async () => {
      try {
        await formRef.value.validate()
        
        const renameData = {
          oldPath: props.nodeData.path || props.nodeData.id,
          newName: formData.value.newName,
          newDisplayName: formData.value.newDisplayName,
          nodeType: props.nodeData.type
        }

        emit('confirm', renameData)
        dialogVisible.value = false
        resetForm()
      } catch (error) {
        ElMessage.error('请检查表单输入')
      }
    }

    const resetForm = () => {
      formData.value = {
        newName: '',
        newDisplayName: ''
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
