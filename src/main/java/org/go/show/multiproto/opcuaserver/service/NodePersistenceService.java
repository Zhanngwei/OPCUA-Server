package org.go.show.multiproto.opcuaserver.service;

import org.go.show.multiproto.opcuaserver.entity.OpcNode;
import org.go.show.multiproto.opcuaserver.repository.OpcNodeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 节点持久化服务
 * 负责OPC UA节点的数据库操作
 */
@Service
@Transactional
public class NodePersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(NodePersistenceService.class);

    @Autowired
    private OpcNodeRepository nodeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${opcua.persistence.enabled:true}")
    private boolean persistenceEnabled;

    @Value("${opcua.persistence.auto-save:true}")
    private boolean autoSave;

    /**
     * 保存节点
     */
    public OpcNode saveNode(OpcNode node) {
        if (!persistenceEnabled) {
            logger.debug("节点持久化已禁用，跳过保存操作");
            return node;
        }

        try {
            OpcNode savedNode = nodeRepository.save(node);
            logger.debug("节点已保存到数据库: {}", savedNode.getPath());
            return savedNode;
        } catch (Exception e) {
            logger.error("保存节点失败: {}", node.getPath(), e);
            throw new RuntimeException("保存节点失败", e);
        }
    }

    /**
     * 根据路径查找节点
     */
    @Transactional(readOnly = true)
    public Optional<OpcNode> findByPath(String path) {
        if (!persistenceEnabled) {
            return Optional.empty();
        }

        try {
            return nodeRepository.findByPath(path);
        } catch (Exception e) {
            logger.error("查找节点失败: {}", path, e);
            return Optional.empty();
        }
    }

    /**
     * 根据NodeId查找节点
     */
    @Transactional(readOnly = true)
    public Optional<OpcNode> findByNodeId(String nodeId) {
        if (!persistenceEnabled) {
            return Optional.empty();
        }

        try {
            return nodeRepository.findByNodeId(nodeId);
        } catch (Exception e) {
            logger.error("根据NodeId查找节点失败: {}", nodeId, e);
            return Optional.empty();
        }
    }

    /**
     * 获取所有启用的节点
     */
    @Transactional(readOnly = true)
    public List<OpcNode> findAllEnabledNodes() {
        if (!persistenceEnabled) {
            return List.of();
        }

        try {
            return nodeRepository.findByEnabledTrueOrderByPathAsc();
        } catch (Exception e) {
            logger.error("获取所有启用节点失败", e);
            return List.of();
        }
    }

    /**
     * 获取所有文件夹节点
     */
    @Transactional(readOnly = true)
    public List<OpcNode> findAllFolders() {
        if (!persistenceEnabled) {
            return List.of();
        }

        try {
            return nodeRepository.findAllFolders();
        } catch (Exception e) {
            logger.error("获取所有文件夹节点失败", e);
            return List.of();
        }
    }

    /**
     * 获取所有变量节点
     */
    @Transactional(readOnly = true)
    public List<OpcNode> findAllVariables() {
        if (!persistenceEnabled) {
            return List.of();
        }

        try {
            return nodeRepository.findAllVariables();
        } catch (Exception e) {
            logger.error("获取所有变量节点失败", e);
            return List.of();
        }
    }

    /**
     * 获取指定父路径的直接子节点
     */
    @Transactional(readOnly = true)
    public List<OpcNode> findDirectChildren(String parentPath) {
        if (!persistenceEnabled) {
            return List.of();
        }

        try {
            return nodeRepository.findDirectChildren(parentPath);
        } catch (Exception e) {
            logger.error("获取子节点失败: {}", parentPath, e);
            return List.of();
        }
    }

    /**
     * 删除节点
     */
    public boolean deleteNode(String path) {
        if (!persistenceEnabled) {
            logger.debug("节点持久化已禁用，跳过删除操作");
            return true;
        }

        try {
            if (nodeRepository.existsByPath(path)) {
                nodeRepository.deleteByPath(path);
                logger.debug("节点已从数据库删除: {}", path);
                return true;
            } else {
                logger.warn("要删除的节点不存在: {}", path);
                return false;
            }
        } catch (Exception e) {
            logger.error("删除节点失败: {}", path, e);
            return false;
        }
    }

    /**
     * 删除节点及其所有子节点
     */
    public boolean deleteNodeWithChildren(String path) {
        if (!persistenceEnabled) {
            logger.debug("节点持久化已禁用，跳过删除操作");
            return true;
        }

        try {
            String prefix = path;
            if (!prefix.endsWith("/")) {
                prefix = prefix + "/";
            }

            // 删除所有子节点
            java.util.List<OpcNode> children = nodeRepository.findByPathStartingWith(prefix + "%");
            if (!children.isEmpty()) {
                nodeRepository.deleteAll(children);
                logger.debug("已从数据库删除子节点 {} 个, 父路径: {}", children.size(), path);
            }

            // 删除自身节点
            if (nodeRepository.existsByPath(path)) {
                nodeRepository.deleteByPath(path);
                logger.debug("节点已从数据库删除: {}", path);
            }

            return true;
        } catch (Exception e) {
            logger.error("删除节点及其子节点失败: {}", path, e);
            return false;
        }
    }

    /**
     * 检查路径是否存在
     */
    @Transactional(readOnly = true)
    public boolean existsByPath(String path) {
        if (!persistenceEnabled) {
            return false;
        }

        try {
            return nodeRepository.existsByPath(path);
        } catch (Exception e) {
            logger.error("检查路径是否存在失败: {}", path, e);
            return false;
        }
    }

    /**
     * 检查NodeId是否存在
     */
    @Transactional(readOnly = true)
    public boolean existsByNodeId(String nodeId) {
        if (!persistenceEnabled) {
            return false;
        }

        try {
            return nodeRepository.existsByNodeId(nodeId);
        } catch (Exception e) {
            logger.error("检查NodeId是否存在失败: {}", nodeId, e);
            return false;
        }
    }

    /**
     * 生成下一个可用的数字型NodeId
     */
    @Transactional(readOnly = true)
    public String generateNextNodeId() {
        if (!persistenceEnabled) {
            return "ns=2;i=1000"; // 默认起始值
        }

        try {
            Integer maxId = nodeRepository.findMaxNumericNodeId();
            int nextId = (maxId != null) ? maxId + 1 : 1000;
            return "ns=2;i=" + nextId;
        } catch (Exception e) {
            logger.error("生成NodeId失败", e);
            return "ns=2;i=1000";
        }
    }

    /**
     * 更新节点值
     */
    public boolean updateNodeValue(String path, Object value) {
        if (!persistenceEnabled || !autoSave) {
            return true;
        }

        try {
            Optional<OpcNode> nodeOpt = nodeRepository.findByPath(path);
            if (nodeOpt.isPresent()) {
                OpcNode node = nodeOpt.get();
                
                // 序列化值
                String valueStr = serializeValue(value);
                node.setCurrentValue(valueStr);
                node.setValueType(value != null ? value.getClass().getSimpleName() : null);
                
                nodeRepository.save(node);
                logger.debug("节点值已更新: {} = {}", path, valueStr);
                return true;
            } else {
                logger.warn("要更新的节点不存在: {}", path);
                return false;
            }
        } catch (Exception e) {
            logger.error("更新节点值失败: {}", path, e);
            return false;
        }
    }

    /**
     * 更新节点数据类型
     */
    public boolean updateNodeDataType(String path, String dataType, Object value) {
        if (!persistenceEnabled || !autoSave) {
            return true;
        }

        try {
            Optional<OpcNode> nodeOpt = nodeRepository.findByPath(path);
            if (nodeOpt.isPresent()) {
                OpcNode node = nodeOpt.get();

                node.setDataType(dataType);

                String valueStr = serializeValue(value);
                node.setCurrentValue(valueStr);
                node.setValueType(value != null ? value.getClass().getSimpleName() : null);

                nodeRepository.save(node);
                logger.debug("节点数据类型已更新: {} -> {}", path, dataType);
                return true;
            } else {
                logger.warn("要更新数据类型的节点不存在: {}", path);
                return false;
            }
        } catch (Exception e) {
            logger.error("更新节点数据类型失败: {}", path, e);
            return false;
        }
    }

    /**
     * 序列化值为字符串
     */
    public String serializeValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }

        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            logger.warn("序列化值失败，使用toString: {}", value, e);
            return value.toString();
        }
    }

    /**
     * 反序列化字符串为值
     */
    public Object deserializeValue(String valueStr, String valueType) {
        if (valueStr == null || valueType == null) {
            return null;
        }

        try {
            switch (valueType) {
                case "String":
                    return valueStr;
                case "Integer":
                    return Integer.valueOf(valueStr);
                case "Long":
                    return Long.valueOf(valueStr);
                case "Double":
                    return Double.valueOf(valueStr);
                case "Float":
                    return Float.valueOf(valueStr);
                case "Boolean":
                    return Boolean.valueOf(valueStr);
                default:
                    // 尝试JSON反序列化
                    return objectMapper.readValue(valueStr, Object.class);
            }
        } catch (Exception e) {
            logger.warn("反序列化值失败，返回原始字符串: {}", valueStr, e);
            return valueStr;
        }
    }

    /**
     * 获取节点统计信息
     */
    @Transactional(readOnly = true)
    public NodeStatistics getStatistics() {
        if (!persistenceEnabled) {
            return new NodeStatistics(0, 0, 0);
        }

        try {
            long totalNodes = nodeRepository.countEnabledNodes();
            long folderCount = nodeRepository.countByNodeType("folder");
            long variableCount = nodeRepository.countByNodeType("variable");
            
            return new NodeStatistics(totalNodes, folderCount, variableCount);
        } catch (Exception e) {
            logger.error("获取节点统计信息失败", e);
            return new NodeStatistics(0, 0, 0);
        }
    }

    /**
     * 节点统计信息类
     */
    public static class NodeStatistics {
        private final long totalNodes;
        private final long folderCount;
        private final long variableCount;

        public NodeStatistics(long totalNodes, long folderCount, long variableCount) {
            this.totalNodes = totalNodes;
            this.folderCount = folderCount;
            this.variableCount = variableCount;
        }

        public long getTotalNodes() { return totalNodes; }
        public long getFolderCount() { return folderCount; }
        public long getVariableCount() { return variableCount; }
    }
}
