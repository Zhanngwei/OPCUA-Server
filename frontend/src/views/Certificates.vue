<template>
  <div class="certificates-container">
    <div class="page-header">
      <h1>{{ $t('certificates.title') }}</h1>
      <p>{{ $t('certificates.description') }}</p>
    </div>

    <div class="certificates-layout">
      <!-- 左侧证书列表 -->
      <div class="certificates-list">
        <div class="list-header">
          <h3>{{ $t('certificates.certificateList') }}</h3>
          <el-button @click="refreshCertificates" size="small">
            <el-icon><Refresh /></el-icon>
            {{ $t('common.refresh') }}
          </el-button>
        </div>

        <div class="certificate-tree">
          <div
            v-for="cert in certificates"
            :key="cert.id"
            :class="['certificate-item', { 'selected': selectedCertificate?.id === cert.id }]"
            @click="selectCertificate(cert)"
            @contextmenu.prevent="showContextMenu($event, cert)"
          >
            <div class="cert-icon">
              <el-icon v-if="cert.type.includes('Own Certificate')" color="#409eff">
                <Key />
              </el-icon>
              <el-icon v-else color="#67c23a">
                <Document />
              </el-icon>
            </div>
            <div class="cert-info">
              <div class="cert-name">{{ cert.name }}</div>
              <div class="cert-type">{{ cert.type }}</div>
            </div>
            <div class="cert-status">
              <el-tag
                :type="getCertificateStatusType(cert.status)"
                size="small"
              >
                {{ cert.status }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧证书详情 -->
      <div class="certificate-details">
        <div v-if="selectedCertificate" class="details-content">
          <div class="details-header">
            <!-- 移除了两个按钮：在操作系统查看器中打开证书 和 在文件浏览器中打开 -->
          </div>

          <div class="details-grid">
            <div class="detail-row">
              <label>{{ $t('certificates.status') }}:</label>
              <el-tag :type="getCertificateStatusType(selectedCertificate.status)">
                {{ selectedCertificate.status }}
              </el-tag>
            </div>

            <div class="detail-row">
              <label>{{ $t('certificates.name') }}:</label>
              <span>{{ selectedCertificate.name }}</span>
            </div>

            <div class="detail-row">
              <label>{{ $t('certificates.signedBy') }}:</label>
              <span>{{ selectedCertificate.signedBy }}</span>
            </div>

            <div class="detail-row">
              <label>{{ $t('certificates.validFrom') }}:</label>
              <span>{{ selectedCertificate.validFrom }}</span>
            </div>

            <div class="detail-row">
              <label>{{ $t('certificates.validTo') }}:</label>
              <span>{{ selectedCertificate.validTo }}</span>
            </div>

            <div class="detail-row">
              <label>{{ $t('certificates.applicationUri') }}:</label>
              <span class="uri-text">{{ selectedCertificate.applicationUri }}</span>
            </div>

            <div class="detail-row">
              <label>{{ $t('certificates.keySize') }}:</label>
              <span>{{ selectedCertificate.keySize }}</span>
            </div>

            <div class="detail-row">
              <label>{{ $t('certificates.filename') }}:</label>
              <span class="filename-text">{{ selectedCertificate.filename }}</span>
            </div>

            <div class="detail-row">
              <label>{{ $t('certificates.version') }}:</label>
              <span>{{ selectedCertificate.version }}</span>
            </div>

            <div class="detail-row">
              <label>{{ $t('certificates.serialNumber') }}:</label>
              <span>{{ selectedCertificate.serialNumber }}</span>
            </div>

            <div class="detail-row">
              <label>{{ $t('certificates.signatureAlgorithm') }}:</label>
              <span>{{ selectedCertificate.signatureAlgorithm }}</span>
            </div>

            <div class="detail-row">
              <label>{{ $t('certificates.issuer') }}:</label>
              <span class="issuer-text">{{ selectedCertificate.issuer }}</span>
            </div>

            <div class="detail-row">
              <label>{{ $t('certificates.subject') }}:</label>
              <span class="subject-text">{{ selectedCertificate.subject }}</span>
            </div>

            <div class="detail-row">
              <label>{{ $t('certificates.subjectAlternativeName') }}:</label>
              <span class="san-text">{{ selectedCertificate.subjectAlternativeName }}</span>
            </div>

            <div class="detail-row">
              <label>{{ $t('certificates.thumbprint') }}:</label>
              <span class="thumbprint-text">{{ selectedCertificate.thumbprint }}</span>
            </div>
          </div>
        </div>

        <div v-else class="no-selection">
          <el-icon><Document /></el-icon>
          <p>{{ $t('certificates.selectCertificate') }}</p>
        </div>
      </div>
    </div>

    <!-- 右键菜单 -->
    <div
      v-if="contextMenu.visible"
      :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
      class="context-menu"
      @click.stop
    >
      <div class="menu-item" @click="trustCertificate">
        <el-icon><Check /></el-icon>
        {{ $t('certificates.trust') }}
      </div>
      <div class="menu-item" @click="rejectCertificate">
        <el-icon><Close /></el-icon>
        {{ $t('certificates.reject') }}
      </div>
      <div
        v-if="contextMenu.certificate?.type.includes('Own Certificate')"
        class="menu-item"
        @click="recreateOwnCertificate"
      >
        <el-icon><Refresh /></el-icon>
        {{ $t('certificates.recreateOwnCertificate') }}
      </div>
      <div class="menu-divider"></div>
      <div class="menu-item danger" @click="deleteCertificate">
        <el-icon><Delete /></el-icon>
        {{ $t('common.delete') }}
      </div>
    </div>

    <!-- 遮罩层用于关闭右键菜单 -->
    <div
      v-if="contextMenu.visible"
      class="context-menu-overlay"
      @click="hideContextMenu"
    ></div>

  </div>
</template>

<script>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Key,
  Refresh,
  Document,
  Check,
  Close,
  Delete
} from '@element-plus/icons-vue'
import { api } from '@/stores/user'

export default {
  name: 'CertificatesView',
  components: {
    Key,
    Refresh,
    Document,
    Check,
    Close,
    Delete
  },
  setup() {
    const loading = ref(false)
    const selectedCertificate = ref(null)
    const certificates = ref([])

    // 右键菜单状态
    const contextMenu = reactive({
      visible: false,
      x: 0,
      y: 0,
      certificate: null
    })

    // 获取证书列表
    const fetchCertificates = async () => {
      loading.value = true
      try {
        const response = await api.get('/certificates')
        certificates.value = response.data
      } catch (error) {
        console.error('Failed to fetch certificates:', error)
        ElMessage.error('Failed to load certificates')
      } finally {
        loading.value = false
      }
    }

    // 选择证书
    const selectCertificate = (cert) => {
      selectedCertificate.value = cert
    }

    // 显示右键菜单
    const showContextMenu = (event, cert) => {
      contextMenu.certificate = cert
      contextMenu.x = event.clientX
      contextMenu.y = event.clientY
      contextMenu.visible = true
    }

    // 隐藏右键菜单
    const hideContextMenu = () => {
      contextMenu.visible = false
      contextMenu.certificate = null
    }

    // 信任证书
    const trustCertificate = async () => {
      if (!contextMenu.certificate) return

      try {
        await api.post(`/certificates/${contextMenu.certificate.id}/trust`)
        ElMessage.success('Certificate trusted successfully')
        await fetchCertificates()
      } catch (error) {
        ElMessage.error('Failed to trust certificate')
      } finally {
        hideContextMenu()
      }
    }

    // 拒绝证书
    const rejectCertificate = async () => {
      if (!contextMenu.certificate) return

      try {
        await api.post(`/certificates/${contextMenu.certificate.id}/reject`)
        ElMessage.success('Certificate rejected successfully')
        await fetchCertificates()
      } catch (error) {
        ElMessage.error('Failed to reject certificate')
      } finally {
        hideContextMenu()
      }
    }

    // 重新创建自有证书
    const recreateOwnCertificate = async () => {
      if (!contextMenu.certificate) return

      try {
        await ElMessageBox.confirm(
          'Are you sure you want to recreate this certificate? This action cannot be undone.',
          'Confirm Recreate',
          {
            confirmButtonText: 'Recreate',
            cancelButtonText: 'Cancel',
            type: 'warning'
          }
        )

        await api.post(`/certificates/${contextMenu.certificate.id}/recreate`)
        ElMessage.success('Certificate recreated successfully')
        await fetchCertificates()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('Failed to recreate certificate')
        }
      } finally {
        hideContextMenu()
      }
    }

    // 删除证书
    const deleteCertificate = async () => {
      const cert = contextMenu.certificate
      if (!cert) return

      try {
        await ElMessageBox.confirm(
          `Are you sure you want to delete certificate "${cert.name}"?`,
          'Confirm Delete',
          {
            confirmButtonText: 'Delete',
            cancelButtonText: 'Cancel',
            type: 'warning'
          }
        )

        await api.delete(`/certificates/${cert.id}`)
        ElMessage.success('Certificate deleted successfully')

        // 如果删除的是当前选中的证书，清空选择
        if (selectedCertificate.value?.id === cert.id) {
          selectedCertificate.value = null
        }

        await fetchCertificates()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('Failed to delete certificate')
        }
      } finally {
        hideContextMenu()
      }
    }

    const getCertificateStatusType = (status) => {
      switch (status) {
        case 'Trusted': return 'success'
        case 'Rejected': return 'danger'
        case 'Unknown': return 'warning'
        default: return 'info'
      }
    }

    // 移除了 openCertificateInViewer 和 openInFileExplorer 方法

    // 刷新证书列表
    const refreshCertificates = async () => {
      try {
        await api.post('/certificates/refresh')
        await fetchCertificates()
        ElMessage.success('Certificates refreshed')
      } catch (error) {
        ElMessage.error('Failed to refresh certificates')
      }
    }

    // 监听点击事件以关闭右键菜单
    const handleDocumentClick = () => {
      if (contextMenu.visible) {
        hideContextMenu()
      }
    }

    // 生命周期钩子
    onMounted(() => {
      fetchCertificates()
      document.addEventListener('click', handleDocumentClick)
    })

    onUnmounted(() => {
      document.removeEventListener('click', handleDocumentClick)
    })

    return {
      loading,
      selectedCertificate,
      certificates,
      contextMenu,
      selectCertificate,
      showContextMenu,
      hideContextMenu,
      trustCertificate,
      rejectCertificate,
      recreateOwnCertificate,
      deleteCertificate,
      getCertificateStatusType,
      refreshCertificates
    }
  }
}
</script>

<style scoped>
.certificates-container {
  padding: 20px;
  height: 100%;
  overflow: hidden;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.page-header p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.certificates-layout {
  display: flex;
  height: calc(100vh - 140px);
  gap: 20px;
}

.certificates-list {
  width: 300px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  background: #fff;
}

.list-header {
  padding: 12px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.list-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.certificate-tree {
  height: calc(100% - 50px);
  overflow-y: auto;
}

.certificate-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s;
}

.certificate-item:hover {
  background-color: #f5f7fa;
}

.certificate-item.selected {
  background-color: #e6f7ff;
  border-left: 3px solid #409eff;
}

.cert-icon {
  margin-right: 8px;
  font-size: 16px;
}

.cert-info {
  flex: 1;
  min-width: 0;
}

.cert-name {
  font-size: 12px;
  font-weight: 500;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cert-type {
  font-size: 11px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cert-status {
  margin-left: 8px;
}

.certificate-details {
  flex: 1;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
  overflow: hidden;
}

.details-content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.details-header {
  padding: 12px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.cert-actions {
  display: flex;
  gap: 8px;
}

.details-grid {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.detail-row {
  display: flex;
  margin-bottom: 12px;
  align-items: flex-start;
}

.detail-row label {
  width: 180px;
  font-weight: 500;
  color: #606266;
  font-size: 13px;
  flex-shrink: 0;
}

.detail-row span {
  flex: 1;
  color: #303133;
  font-size: 13px;
  word-break: break-all;
}

.uri-text,
.filename-text,
.issuer-text,
.subject-text,
.san-text,
.thumbprint-text {
  font-family: monospace;
  font-size: 12px;
  background: #f5f7fa;
  padding: 2px 4px;
  border-radius: 2px;
}

.no-selection {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #909399;
}

.no-selection .el-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.context-menu {
  position: fixed;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: 2000;
  min-width: 160px;
}

.menu-item {
  padding: 8px 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #303133;
  transition: background-color 0.2s;
}

.menu-item:hover {
  background-color: #f5f7fa;
}

.menu-item.danger {
  color: #f56c6c;
}

.menu-item.danger:hover {
  background-color: #fef0f0;
}

.menu-divider {
  height: 1px;
  background: #e4e7ed;
  margin: 4px 0;
}

.context-menu-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1999;
}
</style>
