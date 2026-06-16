<template>
  <teleport to="body">
    <div
      v-if="visible"
      ref="menuRef"
      class="context-menu"
      :style="menuStyle"
      @click.stop
    >
      <div
        v-for="item in menuItems"
        :key="item.key"
        class="context-menu-item"
        :class="{ 'disabled': item.disabled, 'separator': item.separator }"
        @click="handleItemClick(item)"
      >
        <el-icon v-if="item.icon" class="menu-icon">
          <component :is="item.icon" />
        </el-icon>
        <span v-if="!item.separator" class="menu-text">{{ item.label }}</span>
      </div>
    </div>
  </teleport>
</template>

<script>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import {
  Edit,
  Delete,
  Setting,
  Document,
  Folder,
  CopyDocument
} from '@element-plus/icons-vue'

export default {
  name: 'ContextMenu',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    x: {
      type: Number,
      default: 0
    },
    y: {
      type: Number,
      default: 0
    },
    nodeData: {
      type: Object,
      default: () => ({})
    }
  },
  emits: ['update:visible', 'menu-click'],
  setup(props, { emit }) {
    const menuRef = ref(null)

    const menuStyle = computed(() => ({
      position: 'fixed',
      left: `${props.x}px`,
      top: `${props.y}px`,
      zIndex: 9999
    }))

    const isHelloWorldNode = (node) => {
      if (!node) {
        return false
      }
      const path = node.path || node.id
      if (!path) {
        return false
      }
      return path === 'HelloWorld' || path.startsWith('HelloWorld/')
    }

    const menuItems = computed(() => {
      const nodeType = props.nodeData?.type
      const isHelloWorld = isHelloWorldNode(props.nodeData)
      
      if (nodeType === 'folder' || nodeType === 'object') {
        return [
          {
            key: 'add-folder',
            label: '添加文件夹',
            icon: Folder,
            disabled: isHelloWorld
          },
          {
            key: 'add-variable',
            label: '添加变量',
            icon: Document,
            disabled: isHelloWorld
          },
          {
            key: 'separator-1',
            separator: true
          },
          {
            key: 'rename',
            label: '重命名',
            icon: Edit,
            disabled: nodeType === 'object' // Objects folder cannot be renamed
          },
          {
            key: 'delete',
            label: '删除',
            icon: Delete,
            disabled: nodeType === 'object' || isHelloWorld // Objects folder and HelloWorld cannot be deleted
          },
          {
            key: 'separator-2',
            separator: true
          },
          {
            key: 'properties',
            label: '属性',
            icon: Setting,
            disabled: false
          }
        ]
      } else if (nodeType === 'variable') {
        return [
          {
            key: 'edit-value',
            label: '编辑值',
            icon: Edit,
            disabled: false
          },
          {
            key: 'copy-path',
            label: '复制路径',
            icon: CopyDocument,
            disabled: false
          },
          {
            key: 'separator-1',
            separator: true
          },
          {
            key: 'rename',
            label: '重命名',
            icon: Edit,
            disabled: false
          },
          {
            key: 'delete',
            label: '删除',
            icon: Delete,
            disabled: isHelloWorld
          },
          {
            key: 'separator-2',
            separator: true
          },
          {
            key: 'properties',
            label: '属性',
            icon: Setting,
            disabled: false
          }
        ]
      }
      
      return []
    })

    const handleItemClick = (item) => {
      if (item.disabled || item.separator) return
      
      emit('menu-click', {
        action: item.key,
        nodeData: props.nodeData
      })
      emit('update:visible', false)
    }

    const handleClickOutside = (event) => {
      if (menuRef.value && !menuRef.value.contains(event.target)) {
        emit('update:visible', false)
      }
    }

    onMounted(() => {
      document.addEventListener('click', handleClickOutside)
      document.addEventListener('contextmenu', handleClickOutside)
    })

    onUnmounted(() => {
      document.removeEventListener('click', handleClickOutside)
      document.removeEventListener('contextmenu', handleClickOutside)
    })

    return {
      menuRef,
      menuStyle,
      menuItems,
      handleItemClick
    }
  }
}
</script>

<style scoped>
.context-menu {
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 4px 0;
  min-width: 120px;
  font-size: 14px;
}

.context-menu-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.context-menu-item:hover:not(.disabled):not(.separator) {
  background-color: #f5f7fa;
}

.context-menu-item.disabled {
  color: #c0c4cc;
  cursor: not-allowed;
}

.context-menu-item.separator {
  height: 1px;
  background-color: #e4e7ed;
  margin: 4px 0;
  padding: 0;
  cursor: default;
}

.menu-icon {
  margin-right: 8px;
  font-size: 16px;
}

.menu-text {
  flex: 1;
}
</style>
