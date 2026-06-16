
<template>
  <div class="opcua-manager">
    <!-- 工具栏 -->
    <div class="toolbar">
      <!-- 新增下拉菜单 -->
      <el-dropdown @command="handleAddCommand" trigger="click">
        <div class="toolbar-button dropdown-button" :title="$t('common.add')">
          <el-icon><Plus /></el-icon>
          <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="folder">
              <el-icon><Folder /></el-icon>
              {{ $t('objects.addFolder') }}
            </el-dropdown-item>
            <el-dropdown-item command="variable">
              <el-icon><Document /></el-icon>
              {{ $t('objects.addVariable') }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <div
        class="toolbar-button"
        :class="{ 'toolbar-button-disabled': isDeleteDisabled }"
        @click="removeNode"
        :title="$t('objects.removeNode')"
      >
        <el-icon><Minus /></el-icon>
      </div>
      <div class="toolbar-button" @click="refreshTree" :title="$t('common.refresh')">
        <el-icon><Refresh /></el-icon>
      </div>
      <div class="toolbar-separator"></div>
      <span class="toolbar-text">{{ $t('nav.objects') }}</span>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧树形结构 -->
      <div class="left-panel">
        <el-input
          v-model="searchText"
          :placeholder="$t('objects.searchNodes')"
          class="search-input"
          clearable
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <div class="tree-container">
          <div v-if="isLoading" class="loading-container">
            <el-loading-directive v-loading="true" text="加载中..." />
          </div>
          <el-tree
            v-else
            ref="treeRef"
            :data="treeData"
            :props="treeProps"
            :filter-node-method="filterNode"
            node-key="id"
            :expand-on-click-node="false"
            @node-click="handleNodeClick"
            :highlight-current="true"
          >
            <template #default="{ node, data }">
              <span
                class="custom-tree-node"
                @contextmenu.prevent="handleRightClick($event, data)"
              >
                <span>
                  <el-icon class="node-icon" :class="getNodeIconClass(data.type)">
                    <component :is="getNodeIcon(data.type)" />
                  </el-icon>
                  {{ node.label }}
                </span>
              </span>
            </template>
          </el-tree>
        </div>
      </div>

      <!-- 右侧属性面板 -->
      <div class="right-panel">
        <div class="property-panel">
          <!-- 自定义标签页 -->
          <div class="custom-tabs">
            <!-- 标签头部 -->
            <div class="tab-header">
              <div
                class="tab-item"
                :class="{ active: activeTab === 'attributes' }"
                @click="activeTab = 'attributes'"
              >
                属性
              </div>
              <div
                class="tab-item"
                :class="{ active: activeTab === 'references' }"
                @click="activeTab = 'references'"
              >
                引用
              </div>
              <div
                class="tab-item"
                :class="{ active: activeTab === 'events' }"
                @click="activeTab = 'events'"
              >
                事件
              </div>
            </div>

            <!-- 标签内容 -->
            <div class="tab-content">
              <div v-if="activeTab === 'attributes'" class="tab-pane">
                <div class="property-content">
                  <AttributesPanel v-if="selectedNode" :node-data="selectedNode" />
                  <div v-else class="no-selection">
                    <el-empty description="请选择一个节点查看属性" />
                  </div>
                </div>
              </div>
              <div v-else-if="activeTab === 'references'" class="tab-pane">
                <div class="property-content">
                  <ReferencesPanel v-if="selectedNode" :node-data="selectedNode" />
                  <div v-else class="no-selection">
                    <el-empty description="请选择一个节点查看引用" />
                  </div>
                </div>
              </div>
              <div v-else-if="activeTab === 'events'" class="tab-pane">
                <div class="property-content">
                  <EventsPanel v-if="selectedNode" :node-data="selectedNode" />
                  <div v-else class="no-selection">
                    <el-empty description="请选择一个节点查看事件" />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加文件夹对话框 -->
    <AddFolderDialog
      v-model:visible="showAddFolderDialog"
      :selected-node="selectedNode"
      @confirm="handleAddFolder"
    />

    <!-- 添加变量对话框 -->
    <AddVariableDialog
      v-model:visible="showAddVariableDialog"
      :selected-node="selectedNode"
      @confirm="handleAddVariable"
    />

    <!-- 上下文菜单 -->
    <ContextMenu
      v-model:visible="showContextMenu"
      :x="contextMenuX"
      :y="contextMenuY"
      :node-data="contextMenuNode"
      @menu-click="handleContextMenuClick"
    />

    <!-- 重命名对话框 -->
    <RenameDialog
      v-if="selectedNode"
      v-model:visible="showRenameDialog"
      :node-data="selectedNode"
      @confirm="handleRename"
    />

    <!-- 编辑值对话框 -->
    <EditValueDialog
      v-if="selectedNode"
      v-model:visible="showEditValueDialog"
      :node-data="selectedNode"
      @confirm="handleEditValue"
    />

    <!-- 属性对话框 -->
    <PropertiesDialog
      v-if="selectedNode"
      v-model:visible="showPropertiesDialog"
      :node-data="selectedNode"
    />
  </div>
</template>

<script>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Folder,
  Document,
  Setting,
  Plus,
  Minus,
  Refresh,
  ArrowDown,
  Search
} from '@element-plus/icons-vue'
import AttributesPanel from '../components/AttributesPanel.vue'
import ReferencesPanel from '../components/ReferencesPanel.vue'
import EventsPanel from '../components/EventsPanel.vue'
import AddFolderDialog from '../components/AddFolderDialog.vue'
import AddVariableDialog from '../components/AddVariableDialog.vue'
import ContextMenu from '../components/ContextMenu.vue'
import RenameDialog from '../components/RenameDialog.vue'
import EditValueDialog from '../components/EditValueDialog.vue'
import PropertiesDialog from '../components/PropertiesDialog.vue'
import { useOpcuaStore } from '../stores/opcua'

export default {
  name: 'OpcuaManager',
  components: {
    AttributesPanel,
    ReferencesPanel,
    EventsPanel,
    AddFolderDialog,
    AddVariableDialog,
    ContextMenu,
    RenameDialog,
    EditValueDialog,
    PropertiesDialog
  },
  setup() {
    const opcuaStore = useOpcuaStore()

    const searchText = ref('')
    const activeTab = ref('attributes')
    const selectedNode = ref(null)
    const showAddFolderDialog = ref(false)
    const showAddVariableDialog = ref(false)
    const showContextMenu = ref(false)
    const showRenameDialog = ref(false)
    const showEditValueDialog = ref(false)
    const showPropertiesDialog = ref(false)
    const contextMenuX = ref(0)
    const contextMenuY = ref(0)
    const contextMenuNode = ref(null)
    const treeRef = ref(null)
    const isLoading = ref(true)

    const treeData = ref([])
    const treeProps = {
      children: 'children',
      label: 'label'
    }

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

    const isDeleteDisabled = computed(() => {
      if (!selectedNode.value) {
        return true
      }
      if (selectedNode.value.id === 'Objects') {
        return true
      }
      return isHelloWorldNode(selectedNode.value)
    })

    // 搜索过滤
    const filterNode = (value, data) => {
      if (!value) return true
      return data.label.toLowerCase().includes(value.toLowerCase())
    }

    // 监听搜索文本变化
    watch(searchText, (val) => {
      treeRef.value?.filter(val)
    })

    // 获取节点图标
    const getNodeIcon = (type) => {
      switch (type) {
        case 'folder':
          return Folder
        case 'variable':
          return Document
        case 'object':
          return Setting
        default:
          return Folder
      }
    }

    const getNodeIconClass = (type) => {
      switch (type) {
        case 'folder':
          return 'folder-icon'
        case 'variable':
          return 'variable-icon'
        case 'object':
          return 'object-icon'
        default:
          return 'folder-icon'
      }
    }

    // 处理节点点击
    const handleNodeClick = (data) => {
      selectedNode.value = data || null
    }

    // 处理右键点击
    const handleRightClick = (event, data) => {
      event.preventDefault()
      contextMenuX.value = event.clientX
      contextMenuY.value = event.clientY
      contextMenuNode.value = data || null
      selectedNode.value = data || null
      showContextMenu.value = true
    }

    // 保存树的展开状态
    const expandedKeys = ref([])

    const saveExpandedState = () => {
      if (treeRef.value) {
        try {
          // 获取当前展开的节点keys
          const keys = []
          const getAllExpandedKeys = (node) => {
            if (node.expanded) {
              keys.push(node.key)
            }
            if (node.childNodes && node.childNodes.length > 0) {
              node.childNodes.forEach(getAllExpandedKeys)
            }
          }

          if (treeRef.value.store && treeRef.value.store.root) {
            treeRef.value.store.root.childNodes.forEach(getAllExpandedKeys)
          }
          expandedKeys.value = keys
          console.log('Saved expanded keys:', keys)
          return keys
        } catch (error) {
          console.warn('Error saving expanded state:', error)
          return []
        }
      }
      return []
    }

    // 恢复树的展开状态
    const restoreExpandedState = (keys) => {
      if (treeRef.value && keys && keys.length > 0) {
        nextTick(() => {
          try {
            keys.forEach(key => {
              const node = treeRef.value.getNode(key)
              if (node) {
                node.expanded = true
              }
            })
            console.log('Restored expanded keys:', keys)
          } catch (error) {
            console.warn('Error restoring expanded state:', error)
          }
        })
      }
    }

    // 根据ID查找节点
    const findNodeById = (nodes, id) => {
      for (const node of nodes) {
        if (node.id === id) {
          return node
        }
        if (node.children && node.children.length > 0) {
          const found = findNodeById(node.children, id)
          if (found) {
            return found
          }
        }
      }
      return null
    }

    // 智能刷新功能
    const refreshTree = async () => {
      try {
        isLoading.value = true

        if (!selectedNode.value) {
          // 没有选中节点，执行全量刷新
          console.log('No node selected, performing full refresh')
          await fullRefresh()
        } else if (selectedNode.value.type === 'folder' || selectedNode.value.type === 'object') {
          // 选中的是文件夹，刷新该文件夹下的内容
          console.log('Folder selected, refreshing folder:', selectedNode.value.label)
          await refreshFolder(selectedNode.value)
        } else {
          // 选中的是叶子节点（变量），刷新右侧属性信息
          console.log('Variable selected, refreshing attributes:', selectedNode.value.label)
          await refreshNodeAttributes(selectedNode.value)
        }

        ElMessage.success('刷新成功')
      } catch (error) {
        console.error('Refresh error:', error)
        ElMessage.error('刷新失败: ' + error.message)
      } finally {
        isLoading.value = false
      }
    }

    // 全量刷新（保持展开状态）
    const fullRefresh = async () => {
      console.log('Starting full refresh...')

      // 保存当前状态
      const savedExpandedKeys = saveExpandedState()
      const currentSelectedKey = selectedNode.value ? selectedNode.value.id : null

      console.log('Current selected key:', currentSelectedKey)
      console.log('Saved expanded keys:', savedExpandedKeys)

      // 重新获取数据
      await opcuaStore.fetchNodes()
      buildTreeData()

      // 等待DOM更新后恢复状态
      await nextTick()

      // 恢复展开状态
      if (savedExpandedKeys.length > 0) {
        restoreExpandedState(savedExpandedKeys)
      }

      // 恢复选中状态
      if (currentSelectedKey && treeRef.value) {
        setTimeout(() => {
          try {
            treeRef.value.setCurrentKey(currentSelectedKey)
            const nodeData = findNodeById(treeData.value, currentSelectedKey)
            if (nodeData) {
              selectedNode.value = nodeData
              console.log('Restored selected node:', nodeData.label)
            }
          } catch (error) {
            console.warn('Error restoring selection:', error)
          }
        }, 100)
      }

      console.log('Full refresh completed')
    }

    // 刷新指定文件夹
    const refreshFolder = async (folderNode) => {
      // 保存当前展开状态
      const expandedKeys = saveExpandedState()
      const currentSelectedKey = selectedNode.value ? selectedNode.value.id : null

      // 重新获取数据
      await opcuaStore.fetchNodes()
      buildTreeData()

      // 恢复展开状态
      restoreExpandedState(expandedKeys)

      // 确保刷新的文件夹保持展开
      if (treeRef.value) {
        nextTick(() => {
          const node = treeRef.value.getNode(folderNode.id)
          if (node) {
            node.expanded = true
          }

          // 恢复选中状态
          if (currentSelectedKey) {
            treeRef.value.setCurrentKey(currentSelectedKey)
            const nodeData = findNodeById(treeData.value, currentSelectedKey)
            if (nodeData) {
              selectedNode.value = nodeData
            }
          }
        })
      }
    }

    // 刷新节点属性（仅更新右侧面板）
    const refreshNodeAttributes = async (node) => {
      console.log('Refreshing node attributes for:', node.label, node.type)

      if (node.type === 'variable') {
        // 获取最新的变量值
        try {
          console.log('Getting node value for path:', node.path)
          const response = await opcuaStore.getNodeValue(node.path)
          console.log('Node value response:', response)

          if (response && response.value !== undefined) {
            console.log('Updating node value from', selectedNode.value.value, 'to', response.value)

            // 更新选中节点的值
            selectedNode.value = {
              ...selectedNode.value,
              value: response.value
            }

            // 同时更新树中对应节点的值
            const treeNode = findNodeById(treeData.value, node.id)
            if (treeNode) {
              treeNode.value = response.value
              console.log('Updated tree node value')
            }

            console.log('Node attributes refreshed successfully')
          } else {
            console.warn('No value returned from server')
          }
        } catch (error) {
          console.warn('Failed to refresh node value:', error)
          // 如果获取单个节点值失败，回退到全量刷新
          console.log('Falling back to full refresh')
          await fullRefresh()
        }
      } else {
        // 对于非变量节点，执行全量刷新
        console.log('Non-variable node, performing full refresh')
        await fullRefresh()
      }
    }

    // 构建树形数据
    const buildTreeData = () => {
      const nodes = opcuaStore.nodes
      console.log('Building tree with nodes:', nodes)

      // 添加根节点
      const objectsNode = {
        id: 'Objects',
        label: 'Objects',
        type: 'object',
        children: []
      }

      // 创建路径到节点的映射，用于快速查找
      const pathToNodeMap = new Map()
      pathToNodeMap.set('', objectsNode)

      // 处理文件夹 - 先创建所有文件夹节点
      Object.entries(nodes.folders || {}).forEach(([path, info]) => {
        console.log('Processing folder:', path, info)

        // 分割路径并创建层次结构
        const pathParts = path.split('/').filter(part => part.trim() !== '')
        let currentPath = ''
        let currentParent = objectsNode

        pathParts.forEach((part, index) => {
          currentPath = currentPath ? `${currentPath}/${part}` : part

          // 检查是否已经存在这个节点
          let existingNode = pathToNodeMap.get(currentPath)

          if (!existingNode) {
            // 创建新节点
            const isLeafFolder = index === pathParts.length - 1
            existingNode = {
              id: currentPath,
              path: currentPath,
              label: part,
              type: 'folder',
              displayName: isLeafFolder ? (info.displayName || part) : part,
              nodeId: isLeafFolder ? info.nodeId : null,
              children: []
            }

            // 添加到父节点
            currentParent.children.push(existingNode)
            pathToNodeMap.set(currentPath, existingNode)

            console.log('Created folder node:', currentPath, existingNode)
          }

          currentParent = existingNode
        })
      })

      // 处理变量
      Object.entries(nodes.variables || {}).forEach(([path, info]) => {
        console.log('Processing variable:', path, info)

        const pathParts = path.split('/').filter(part => part.trim() !== '')
        const variableName = pathParts.pop()
        const parentPath = pathParts.join('/')

        // 找到父节点
        let parentNode = pathToNodeMap.get(parentPath) || objectsNode

        // 如果父路径不存在，创建必要的文件夹结构
        if (!pathToNodeMap.has(parentPath) && parentPath) {
          const missingParts = parentPath.split('/').filter(part => part.trim() !== '')
          let currentPath = ''
          let currentParent = objectsNode

          missingParts.forEach(part => {
            currentPath = currentPath ? `${currentPath}/${part}` : part

            let existingNode = pathToNodeMap.get(currentPath)
            if (!existingNode) {
              existingNode = {
                id: currentPath,
                path: currentPath,
                label: part,
                type: 'folder',
                displayName: part,
                nodeId: null,
                children: []
              }
              currentParent.children.push(existingNode)
              pathToNodeMap.set(currentPath, existingNode)
            }
            currentParent = existingNode
          })

          parentNode = pathToNodeMap.get(parentPath)
        }

        // 创建变量节点
        const variableNode = {
          id: path,
          path: path,
          label: variableName,
          type: 'variable',
          displayName: info.displayName || variableName,
          nodeId: info.nodeId,
          value: info.value,
          dataType: info.dataType,
          accessLevel: info.accessLevel,
          userAccessLevel: info.userAccessLevel
        }

        parentNode.children.push(variableNode)
        console.log('Created variable node:', path, variableNode)
      })

      // 递归排序所有节点（文件夹在前，变量在后，按名称排序）
      const sortNodes = (node) => {
        if (node.children && node.children.length > 0) {
          node.children.sort((a, b) => {
            // 文件夹优先
            if (a.type === 'folder' && b.type !== 'folder') return -1
            if (a.type !== 'folder' && b.type === 'folder') return 1
            // 同类型按名称排序
            return a.label.localeCompare(b.label)
          })

          // 递归排序子节点
          node.children.forEach(sortNodes)
        }
      }

      sortNodes(objectsNode)

      console.log('Final tree structure:', objectsNode)
      treeData.value = [objectsNode]
    }

    // 处理新增下拉菜单命令
    const handleAddCommand = (command) => {
      switch (command) {
        case 'folder':
          addFolder()
          break
        case 'variable':
          addVariable()
          break
      }
    }

    // 添加文件夹
    const addFolder = () => {
      showAddFolderDialog.value = true
    }

    const handleAddFolder = async (folderData) => {
      try {
        // 如果选中了文件夹节点，将新文件夹创建在该文件夹下
        if (selectedNode.value && selectedNode.value.type === 'folder') {
          if (isHelloWorldNode(selectedNode.value)) {
            ElMessage.warning('HelloWorld 节点及其子节点为系统内置示例，不允许在其下创建新节点')
            return
          }

          const parentPath = selectedNode.value.path
          // 构建完整路径：父路径/文件夹名
          folderData.path = parentPath ? `${parentPath}/${folderData.path}` : folderData.path
        }

        const targetPath = folderData.path
        if (targetPath === 'HelloWorld' || targetPath.startsWith('HelloWorld/')) {
          ElMessage.warning('HelloWorld 节点及其子节点为系统内置示例，不允许在其下创建新节点')
          return
        }

        await opcuaStore.createFolder(folderData)
        await refreshTree()
        ElMessage.success('文件夹创建成功')
      } catch (error) {
        ElMessage.error('创建文件夹失败: ' + error.message)
      }
    }

    // 添加变量
    const addVariable = () => {
      showAddVariableDialog.value = true
    }

    const handleAddVariable = async (variableData) => {
      try {
        // 如果选中了文件夹节点，将变量创建在该文件夹下
        if (selectedNode.value && selectedNode.value.type === 'folder') {
          if (isHelloWorldNode(selectedNode.value)) {
            ElMessage.warning('HelloWorld 节点及其子节点为系统内置示例，不允许在其下创建新节点')
            return
          }

          const parentPath = selectedNode.value.path
          // 构建完整路径：父路径/变量名
          variableData.path = parentPath ? `${parentPath}/${variableData.path}` : variableData.path
        }

        const targetPath = variableData.path
        if (targetPath === 'HelloWorld' || targetPath.startsWith('HelloWorld/')) {
          ElMessage.warning('HelloWorld 节点及其子节点为系统内置示例，不允许在其下创建新节点')
          return
        }

        await opcuaStore.createVariable(variableData)
        await refreshTree()
        ElMessage.success('变量创建成功')
      } catch (error) {
        ElMessage.error(error.message || '创建变量失败')
      }
    }

    // 处理上下文菜单点击
    const handleContextMenuClick = async ({ action, nodeData }) => {
      switch (action) {
        case 'add-folder':
          showAddFolderDialog.value = true
          break
        case 'add-variable':
          showAddVariableDialog.value = true
          break
        case 'rename':
          showRenameDialog.value = true
          break
        case 'delete':
          await deleteNode(nodeData)
          break
        case 'edit-value':
          showEditValueDialog.value = true
          break
        case 'copy-path':
          await copyPathToClipboard(nodeData)
          break
        case 'properties':
          showPropertiesDialog.value = true
          break
      }
    }

    // 删除节点
    const deleteNode = async (nodeData) => {
      if (!nodeData) {
        ElMessage.warning('无法删除此节点')
        return
      }

      if (isHelloWorldNode(nodeData)) {
        ElMessage.warning('HelloWorld 节点及其子节点为系统内置示例，禁止删除')
        return
      }

      if (nodeData.id === 'Objects') {
        ElMessage.warning('无法删除此节点')
        return
      }

      try {
        await ElMessageBox.confirm(
          `确定要删除节点 "${nodeData.label}" 吗？`,
          '确认删除',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )

        await opcuaStore.deleteNode(nodeData.path)
        await refreshTree()
        selectedNode.value = null
        ElMessage.success('节点删除成功')
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除节点失败: ' + error.message)
        }
      }
    }

    // 删除节点（工具栏按钮）
    const removeNode = async () => {
      if (!selectedNode.value) {
        ElMessage.warning('请选择要删除的节点')
        return
      }
      await deleteNode(selectedNode.value)
    }

    // 复制路径到剪贴板
    const copyPathToClipboard = async (nodeData) => {
      try {
        const path = nodeData.path || nodeData.id
        await navigator.clipboard.writeText(path)
        ElMessage.success('路径已复制到剪贴板')
      } catch (error) {
        ElMessage.error('复制失败')
      }
    }

    // 处理重命名
    const handleRename = async (renameData) => {
      try {
        await opcuaStore.renameNode(renameData.oldPath, renameData.newName, renameData.newDisplayName)
        await refreshTree()
        ElMessage.success('节点重命名成功')
      } catch (error) {
        ElMessage.error('重命名失败: ' + error.message)
      }
    }

    // 处理编辑值
    const handleEditValue = async (updateData) => {
      try {
        await opcuaStore.updateVariable(updateData.path, updateData.value)
        await refreshTree()
        ElMessage.success('变量值更新成功')
      } catch (error) {
        ElMessage.error('更新变量值失败: ' + error.message)
      }
    }

    // 初始化
    onMounted(async () => {
      await refreshTree()
    })

    return {
      searchText,
      activeTab,
      selectedNode,
      isDeleteDisabled,
      showAddFolderDialog,
      showAddVariableDialog,
      showContextMenu,
      showRenameDialog,
      showEditValueDialog,
      showPropertiesDialog,
      contextMenuX,
      contextMenuY,
      contextMenuNode,
      treeRef,
      treeData,
      treeProps,
      isLoading,
      filterNode,
      getNodeIcon,
      getNodeIconClass,
      handleNodeClick,
      handleRightClick,
      handleContextMenuClick,
      handleAddCommand,
      refreshTree,
      addFolder,
      handleAddFolder,
      addVariable,
      handleAddVariable,
      removeNode,
      handleRename,
      handleEditValue,
      // 图标组件
      Plus,
      Minus,
      Refresh,
      ArrowDown,
      Search,
      Folder,
      Document
    }
  }
}
</script>

<style scoped>
.opcua-manager {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.left-panel {
  width: 300px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.right-panel {
  flex: 1;
  overflow: hidden;
}

.toolbar-separator {
  width: 1px;
  height: 24px;
  background: #e4e7ed;
  margin: 0 8px;
}

.toolbar-text {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 8px;
  user-select: none;
}

.node-icon {
  margin-right: 6px;
}

.folder-icon {
  color: #409eff;
}

.variable-icon {
  color: #67c23a;
}

.object-icon {
  color: #e6a23c;
}

/* 工具栏样式 */
.toolbar {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid #e4e7ed;
  background-color: #fafafa;
}

.toolbar-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
  margin-right: 4px;
}

.toolbar-button:hover {
  background-color: #e6f7ff;
}

.toolbar-button-disabled {
  cursor: not-allowed;
  color: #c0c4cc;
}

.toolbar-button-disabled:hover {
  background-color: transparent;
}

.dropdown-button {
  width: auto !important;
  padding: 0 8px;
  gap: 4px;
}

.dropdown-arrow {
  font-size: 12px;
}

/* 搜索框样式 */
.search-input {
  margin: 12px;
  margin-bottom: 8px;
}

/* 树容器样式 */
.tree-container {
  flex: 1;
  overflow: auto;
  padding: 0 12px 12px;
}

/* 属性面板样式 */
.property-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 自定义选项卡样式 */
.custom-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
}

.tab-header {
  display: flex;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
  flex-shrink: 0;
}

.tab-item {
  padding: 12px 20px;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  color: #606266;
  font-size: 14px;
  transition: all 0.3s;
  user-select: none;
}

.tab-item:hover {
  color: #409eff;
}

.tab-item.active {
  color: #409eff;
  border-bottom-color: #409eff;
  font-weight: 500;
}

.tab-content {
  flex: 1;
  overflow: hidden;
  background: #fff;
}

.tab-pane {
  height: 100%;
  overflow: auto;
}

.property-content {
  height: 100%;
  overflow: auto;
  padding: 16px;
}

.no-selection {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.loading-container {
  height: 200px;
  position: relative;
}
</style>
