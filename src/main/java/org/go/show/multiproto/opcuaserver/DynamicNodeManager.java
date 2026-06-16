package org.go.show.multiproto.opcuaserver;

import org.go.show.multiproto.opcuaserver.entity.OpcNode;
import org.go.show.multiproto.opcuaserver.service.NodePersistenceService;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;

import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicNodeManager {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private SimpleNamespace namespace;
    private final Map<String, UaVariableNode> dynamicNodes = new ConcurrentHashMap<>();
    private final Map<String, UaFolderNode> dynamicFolders = new ConcurrentHashMap<>();

    // NodeId管理
    private final Set<String> usedNumericIds = ConcurrentHashMap.newKeySet();
    private final Set<String> usedStringIds = ConcurrentHashMap.newKeySet();
    private final Set<String> usedGuidIds = ConcurrentHashMap.newKeySet();
    private final Set<String> usedOpaqueIds = ConcurrentHashMap.newKeySet();
    private volatile long nextNumericId = 1000; // 从1000开始，避免与系统预定义的NodeId冲突

    private NodePersistenceService persistenceService;

    public DynamicNodeManager(SimpleNamespace namespace) {
        this.namespace = namespace;
    }

    public void setPersistenceService(NodePersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    /**
     * 初始化时从数据库加载已保存的节点
     */
    public void loadPersistedNodes() {
        if (persistenceService == null) {
            logger.warn("持久化服务未初始化，跳过节点加载");
            return;
        }

        try {
            logger.info("开始从数据库加载已保存的节点...");

            // 先加载所有文件夹节点
            List<OpcNode> folders = persistenceService.findAllFolders();
            for (OpcNode folder : folders) {
                try {
                    createFolderFromEntity(folder);
                    logger.debug("已加载文件夹节点: {}", folder.getPath());
                } catch (Exception e) {
                    logger.error("加载文件夹节点失败: {}", folder.getPath(), e);
                }
            }

            // 再加载所有变量节点
            List<OpcNode> variables = persistenceService.findAllVariables();
            for (OpcNode variable : variables) {
                try {
                    createVariableFromEntity(variable);
                    logger.debug("已加载变量节点: {}", variable.getPath());
                } catch (Exception e) {
                    logger.error("加载变量节点失败: {}", variable.getPath(), e);
                }
            }

            logger.info("节点加载完成，文件夹: {}, 变量: {}", folders.size(), variables.size());

            // 清理 HelloWorld 子树下的动态节点（只保留内置示例变量）
            cleanupHelloWorldDynamicNodes();
        } catch (Exception e) {
            logger.error("从数据库加载节点失败", e);
        }
    }

    /**
     * 清理 HelloWorld 子树下的所有动态节点
     * 仅删除动态创建并持久化的节点，不影响内置的 HelloWorld 文件夹和示例变量
     */
    public void cleanupHelloWorldDynamicNodes() {
        try {
            String prefix = "HelloWorld/";

            // 删除 HelloWorld 下的动态变量节点
            List<String> variableKeysToRemove = new ArrayList<>();
            for (String key : dynamicNodes.keySet()) {
                if (key != null && key.startsWith(prefix)) {
                    variableKeysToRemove.add(key);
                }
            }

            for (String key : variableKeysToRemove) {
                UaVariableNode variableNode = dynamicNodes.remove(key);
                if (variableNode != null) {
                    namespace.getNodeManager().removeNode(variableNode.getNodeId());
                    logger.info("Cleaned dynamic HelloWorld variable node: {}", key);
                }
            }

            // 删除 HelloWorld 下的动态文件夹节点
            List<String> folderKeysToRemove = new ArrayList<>();
            for (String key : dynamicFolders.keySet()) {
                if (key != null && key.startsWith(prefix)) {
                    folderKeysToRemove.add(key);
                }
            }

            // 先删子目录，再删父目录（按路径长度从长到短）
            folderKeysToRemove.sort((a, b) -> Integer.compare(b.length(), a.length()));

            for (String key : folderKeysToRemove) {
                UaFolderNode folderNode = dynamicFolders.remove(key);
                if (folderNode != null) {
                    namespace.getNodeManager().removeNode(folderNode.getNodeId());
                    logger.info("Cleaned dynamic HelloWorld folder node: {}", key);
                }
            }

            // 清理数据库中 HelloWorld 下的动态节点
            if (persistenceService != null) {
                try {
                    persistenceService.deleteNodeWithChildren("HelloWorld");
                    logger.info("Cleaned persisted HelloWorld dynamic nodes from database");
                } catch (Exception e) {
                    logger.error("Failed to cleanup HelloWorld dynamic nodes from database", e);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to cleanup dynamic HelloWorld nodes", e);
        }
    }

    /**
     * 动态创建文件夹节点
     */
    public boolean createFolder(String path, String displayName) {
        return createFolder(path, displayName, "string", null);
    }

    /**
     * 动态创建文件夹节点（带NodeId类型控制）
     */
    public boolean createFolder(String path, String displayName, String nodeIdType, String nodeIdValue) {
        try {
            if (path != null && ("HelloWorld".equals(path) || path.startsWith("HelloWorld/"))) {
                logger.warn("Attempt to create folder under protected HelloWorld path: {}", path);
                return false;
            }

            if (dynamicFolders.containsKey(path)) {
                logger.warn("Folder already exists: {}", path);
                return false;
            }

            // 检查NodeId是否重复
            if (nodeIdValue != null && !nodeIdValue.trim().isEmpty() && isNodeIdExists(nodeIdType, nodeIdValue)) {
                logger.warn("NodeId already exists: type={}, value={}", nodeIdType, nodeIdValue);
                return false;
            }

            NodeId folderId = createNodeId(nodeIdType, nodeIdValue, path);
            QualifiedName folderName = new QualifiedName(namespace.getNamespaceIndex(), displayName);

            UaFolderNode folderNode = new UaFolderNode(
                namespace.getNodeContext(),
                folderId,
                folderName,
                LocalizedText.english(displayName)
            );

            namespace.getNodeManager().addNode(folderNode);

            // 添加到Objects文件夹下
            folderNode.addReference(new Reference(
                folderNode.getNodeId(),
                NodeIds.Organizes,
                NodeIds.ObjectsFolder.expanded(),
                false
            ));

            dynamicFolders.put(path, folderNode);

            // 持久化到数据库
            if (persistenceService != null) {
                try {
                    String parentPath = getParentPath(path);
                    OpcNode entity = new OpcNode(path, "folder", displayName);
                    entity.setNodeId(folderId.toParseableString());
                    entity.setParentPath(parentPath);
                    entity.setDescription("动态创建的文件夹节点");
                    persistenceService.saveNode(entity);
                    logger.debug("文件夹节点已保存到数据库: {}", path);
                } catch (Exception e) {
                    logger.error("保存文件夹节点到数据库失败: {}", path, e);
                    // 不影响节点创建，只记录错误
                }
            }

            logger.info("Created folder: {} -> {} with NodeId: {}", path, displayName, folderId.toParseableString());
            return true;

        } catch (Exception e) {
            logger.error("Failed to create folder: {}", path, e);
            return false;
        }
    }

    /**
     * 动态创建变量节点
     */
    public boolean createVariable(String path, String displayName, Object initialValue, String dataType) {
        return createVariable(path, displayName, initialValue, dataType, "READ_WRITE");
    }

    /**
     * 动态创建变量节点（带权限控制）
     */
    public boolean createVariable(String path, String displayName, Object initialValue, String dataType, String accessLevel) {
        return createVariable(path, displayName, initialValue, dataType, accessLevel, "string", null);
    }

    /**
     * 动态创建变量节点（带权限控制和NodeId类型控制）
     */
    public boolean createVariable(String path, String displayName, Object initialValue, String dataType, String accessLevel, String nodeIdType, String nodeIdValue) {
        try {
            if (path != null && ("HelloWorld".equals(path) || path.startsWith("HelloWorld/"))) {
                logger.warn("Attempt to create variable under protected HelloWorld path: {}", path);
                return false;
            }

            if (dynamicNodes.containsKey(path)) {
                logger.warn("Variable already exists: {}", path);
                return false;
            }

            // 检查NodeId是否重复
            if (nodeIdValue != null && !nodeIdValue.trim().isEmpty() && isNodeIdExists(nodeIdType, nodeIdValue)) {
                logger.warn("NodeId already exists: type={}, value={}", nodeIdType, nodeIdValue);
                return false;
            }

            NodeId variableId = createNodeId(nodeIdType, nodeIdValue, path);
            QualifiedName variableName = new QualifiedName(namespace.getNamespaceIndex(), displayName);

            // 确定数据类型
            NodeId dataTypeId = getDataTypeId(dataType);
            if (dataTypeId == null) {
                logger.error("Unsupported data type: {}", dataType);
                return false;
            }

            // 确定访问级别
            Set<AccessLevel> accessLevelSet = getAccessLevelSet(accessLevel);

            UaVariableNode variableNode = new UaVariableNode.UaVariableNodeBuilder(namespace.getNodeContext())
                .setNodeId(variableId)
                .setAccessLevel(accessLevelSet)
                .setUserAccessLevel(accessLevelSet)  // 设置用户访问级别
                .setBrowseName(variableName)
                .setDisplayName(LocalizedText.english(displayName))
                .setDataType(dataTypeId)
                .setTypeDefinition(NodeIds.BaseDataVariableType)
                .build();

            // 设置初始值
            Object convertedValue = convertValue(initialValue, dataType);
            variableNode.setValue(new DataValue(new Variant(convertedValue)));

            namespace.getNodeManager().addNode(variableNode);

            // 确定父节点
            UaFolderNode parentFolder = findParentFolder(path);
            if (parentFolder != null) {
                parentFolder.addOrganizes(variableNode);
            } else {
                // 添加到Objects文件夹下
                variableNode.addReference(new Reference(
                    variableNode.getNodeId(),
                    NodeIds.Organizes,
                    NodeIds.ObjectsFolder.expanded(),
                    false
                ));
            }

            dynamicNodes.put(path, variableNode);

            // 持久化到数据库
            if (persistenceService != null) {
                try {
                    String parentPath = getParentPath(path);
                    OpcNode entity = new OpcNode(path, "variable", displayName);
                    entity.setNodeId(variableId.toParseableString());
                    entity.setParentPath(parentPath);
                    entity.setDataType(dataType);
                    entity.setAccessLevel(accessLevel);
                    entity.setCurrentValue(persistenceService.serializeValue(convertedValue));
                    entity.setValueType(convertedValue != null ? convertedValue.getClass().getSimpleName() : null);
                    entity.setWritable(accessLevelSet.contains(AccessLevel.CurrentWrite));
                    entity.setDescription("动态创建的变量节点");
                    persistenceService.saveNode(entity);
                    logger.debug("变量节点已保存到数据库: {}", path);
                } catch (Exception e) {
                    logger.error("保存变量节点到数据库失败: {}", path, e);
                    // 不影响节点创建，只记录错误
                }
            }

            logger.info("Created variable: {} -> {} = {} with NodeId: {}", path, displayName, initialValue, variableId.toParseableString());
            return true;

        } catch (Exception e) {
            logger.error("Failed to create variable: {}", path, e);
            return false;
        }
    }

    /**
     * 判断指定路径的变量是否已存在
     */
    public boolean isVariableExists(String path) {
        return path != null && dynamicNodes.containsKey(path);
    }

    /**
     * 更新变量值
     */
    public boolean updateVariable(String path, Object value) {
        try {
            UaVariableNode node = dynamicNodes.get(path);
            if (node == null) {
                logger.warn("Variable not found: {}", path);
                return false;
            }

            node.setValue(new DataValue(new Variant(value)));

            // 更新数据库中的值
            if (persistenceService != null) {
                persistenceService.updateNodeValue(path, value);
            }

            logger.debug("Updated variable: {} = {}", path, value);
            return true;

        } catch (Exception e) {
            logger.error("Failed to update variable: {}", path, e);
            return false;
        }
    }

    /**
     * 更新变量数据类型
     */
    public boolean updateVariableDataType(String path, String newDataType, Object newValue) {
        try {
            UaVariableNode node = dynamicNodes.get(path);
            boolean isDynamicNode = true;

            if (node == null) {
                isDynamicNode = false;

                if (path != null && path.startsWith("HelloWorld/")) {
                    node = namespace.getHelloWorldVariableNode(path);
                }
            }

            if (node == null) {
                logger.warn("Variable not found: {}", path);
                return false;
            }

            NodeId dataTypeId = getDataTypeId(newDataType);
            if (dataTypeId == null) {
                logger.warn("Unsupported data type: {}", newDataType);
                return false;
            }

            Object valueToUse = newValue;
            if (valueToUse == null) {
                DataValue dataValue = node.getValue();
                if (dataValue != null && dataValue.getValue() != null) {
                    valueToUse = dataValue.getValue().getValue();
                }
            }

            Object convertedValue = convertValue(valueToUse, newDataType);

            node.setDataType(dataTypeId);
            node.setValue(new DataValue(new Variant(convertedValue)));

            if (isDynamicNode && persistenceService != null) {
                persistenceService.updateNodeDataType(path, newDataType, convertedValue);
            }

            logger.debug("Updated variable data type: {} -> {}", path, newDataType);
            return true;

        } catch (Exception e) {
            logger.error("Failed to update variable data type: {}", path, e);
            return false;
        }
    }

    /**
     * 重命名节点
     */
    public boolean renameNode(String oldPath, String newName, String newDisplayName) {
        try {
            // 尝试重命名变量
            UaVariableNode variableNode = dynamicNodes.get(oldPath);
            if (variableNode != null) {
                // 计算新路径
                String[] pathParts = oldPath.split("/");
                pathParts[pathParts.length - 1] = newName;
                String newPath = String.join("/", pathParts);

                // 检查新路径是否已存在
                if (dynamicNodes.containsKey(newPath)) {
                    logger.warn("Variable with new path already exists: {}", newPath);
                    return false;
                }

                // 更新节点属性
                variableNode.setDisplayName(LocalizedText.english(newDisplayName));
                variableNode.setBrowseName(new QualifiedName(namespace.getNamespaceIndex(), newName));

                // 更新映射
                dynamicNodes.remove(oldPath);
                dynamicNodes.put(newPath, variableNode);

                logger.info("Renamed variable: {} -> {} ({})", oldPath, newPath, newDisplayName);
                return true;
            }

            // 尝试重命名文件夹
            UaFolderNode folderNode = dynamicFolders.get(oldPath);
            if (folderNode != null) {
                // 计算新路径
                String[] pathParts = oldPath.split("/");
                pathParts[pathParts.length - 1] = newName;
                String newPath = String.join("/", pathParts);

                // 检查新路径是否已存在
                if (dynamicFolders.containsKey(newPath)) {
                    logger.warn("Folder with new path already exists: {}", newPath);
                    return false;
                }

                // 更新节点属性
                folderNode.setDisplayName(LocalizedText.english(newDisplayName));
                folderNode.setBrowseName(new QualifiedName(namespace.getNamespaceIndex(), newName));

                // 更新映射
                dynamicFolders.remove(oldPath);
                dynamicFolders.put(newPath, folderNode);

                logger.info("Renamed folder: {} -> {} ({})", oldPath, newPath, newDisplayName);
                return true;
            }

            logger.warn("Node not found: {}", oldPath);
            return false;

        } catch (Exception e) {
            logger.error("Failed to rename node: {}", oldPath, e);
            return false;
        }
    }

    /**
     * 删除节点
     */
    public boolean deleteNode(String path) {
        try {
            if (path != null && ("HelloWorld".equals(path) || path.startsWith("HelloWorld/"))) {
                logger.warn("Attempt to delete protected HelloWorld node: {}", path);
                return false;
            }

            boolean deleted = false;

            String prefix = path;
            if (prefix != null && !prefix.endsWith("/")) {
                prefix = prefix + "/";
            }

            // 收集并删除变量节点（包含子路径）
            java.util.List<String> variableKeysToRemove = new java.util.ArrayList<>();
            for (String key : dynamicNodes.keySet()) {
                if (key.equals(path) || (prefix != null && key.startsWith(prefix))) {
                    variableKeysToRemove.add(key);
                }
            }

            for (String key : variableKeysToRemove) {
                UaVariableNode variableNode = dynamicNodes.remove(key);
                if (variableNode != null) {
                    namespace.getNodeManager().removeNode(variableNode.getNodeId());
                    logger.info("Deleted variable: {}", key);
                    deleted = true;
                }
            }

            // 收集并删除文件夹节点（包含子路径）
            java.util.List<String> folderKeysToRemove = new java.util.ArrayList<>();
            for (String key : dynamicFolders.keySet()) {
                if (key.equals(path) || (prefix != null && key.startsWith(prefix))) {
                    folderKeysToRemove.add(key);
                }
            }

            // 先删除子文件夹，再删自身（按路径长度从长到短排序）
            folderKeysToRemove.sort((a, b) -> Integer.compare(b.length(), a.length()));

            for (String key : folderKeysToRemove) {
                UaFolderNode folderNode = dynamicFolders.remove(key);
                if (folderNode != null) {
                    namespace.getNodeManager().removeNode(folderNode.getNodeId());
                    logger.info("Deleted folder: {}", key);
                    deleted = true;
                }
            }

            if (deleted) {
                // 从数据库删除自身及其子节点
                if (persistenceService != null) {
                    try {
                        persistenceService.deleteNodeWithChildren(path);
                        logger.debug("节点及其子节点已从数据库删除: {}", path);
                    } catch (Exception e) {
                        logger.error("从数据库删除节点及其子节点失败: {}", path, e);
                        // 不影响内存中的删除操作
                    }
                }
                return true;
            }

            logger.warn("Node not found: {}", path);
            return false;

        } catch (Exception e) {
            logger.error("Failed to delete node: {}", path, e);
            return false;
        }
    }

    /**
     * 获取完整的地址空间（包括标准节点）
     */
    public Map<String, Object> getFullAddressSpace() {
        Map<String, Object> result = new ConcurrentHashMap<>();
        Map<String, Object> folders = new ConcurrentHashMap<>();
        Map<String, Object> variables = new ConcurrentHashMap<>();

        try {
            // 浏览Objects节点的子节点
            browseObjectsChildren(folders, variables);

            // 添加自定义节点
            Map<String, Object> customNodes = getAllNodes();
            Map<String, Object> customFolders = (Map<String, Object>) customNodes.get("folders");
            Map<String, Object> customVariables = (Map<String, Object>) customNodes.get("variables");

            if (customFolders != null) {
                folders.putAll(customFolders);
            }
            if (customVariables != null) {
                variables.putAll(customVariables);
            }

        } catch (Exception e) {
            logger.error("Failed to get full address space", e);
        }

        result.put("folders", folders);
        result.put("variables", variables);
        return result;
    }

    /**
     * 浏览Objects节点的子节点（真正的动态浏览）
     */
    private void browseObjectsChildren(Map<String, Object> folders, Map<String, Object> variables) {
        try {
            // Objects节点的NodeId
            NodeId objectsNodeId = NodeIds.ObjectsFolder; // ns=0;i=85

            // 获取Objects节点的真实子节点
            List<Map<String, Object>> children = browseNodeChildrenInternal(objectsNodeId);

            for (Map<String, Object> child : children) {
                String browseName = (String) child.get("browseName");
                String nodeClass = (String) child.get("nodeClass");
                String nodeId = (String) child.get("nodeId");
                String displayName = (String) child.get("displayName");

                if ("Object".equals(nodeClass)) {
                    Map<String, Object> folderInfo = new ConcurrentHashMap<>();
                    folderInfo.put("displayName", displayName);
                    folderInfo.put("nodeId", nodeId);
                    folderInfo.put("type", "folder");
                    folders.put(browseName, folderInfo);
                } else if ("Variable".equals(nodeClass)) {
                    Map<String, Object> varInfo = new ConcurrentHashMap<>();
                    varInfo.put("displayName", displayName);
                    varInfo.put("nodeId", nodeId);
                    varInfo.put("type", "variable");
                    variables.put(browseName, varInfo);
                }
            }

            logger.debug("Browsed {} children from Objects node", children.size());
        } catch (Exception e) {
            logger.warn("Failed to browse Objects children, falling back to static list", e);
            // 如果动态浏览失败，回退到静态列表
            addStandardObjectsChildren(folders);
        }
    }

    /**
     * 添加标准的Objects节点下的子节点
     */
    private void addStandardObjectsChildren(Map<String, Object> folders) {
        try {
            // 添加Server节点
            Map<String, Object> serverNode = new ConcurrentHashMap<>();
            serverNode.put("displayName", "Server");
            serverNode.put("nodeId", "ns=0;i=2253"); // Server object NodeId
            serverNode.put("type", "folder");
            folders.put("Server", serverNode);

            // 添加Aliases节点（如果存在）
            Map<String, Object> aliasesNode = new ConcurrentHashMap<>();
            aliasesNode.put("displayName", "Aliases");
            aliasesNode.put("nodeId", "ns=0;i=23470"); // Aliases object NodeId
            aliasesNode.put("type", "folder");
            folders.put("Aliases", aliasesNode);

            // 添加Locations节点（如果存在）
            Map<String, Object> locationsNode = new ConcurrentHashMap<>();
            locationsNode.put("displayName", "Locations");
            locationsNode.put("nodeId", "ns=0;i=23471"); // 假设的Locations NodeId
            locationsNode.put("type", "folder");
            folders.put("Locations", locationsNode);

            logger.debug("Added standard Objects children: Server, Aliases, Locations");
        } catch (Exception e) {
            logger.warn("Failed to add standard Objects children", e);
        }
    }

    /**
     * 浏览指定节点的子节点（公共API）
     */
    public List<Map<String, Object>> browseNodeChildren(NodeId nodeId) {
        return browseNodeChildrenInternal(nodeId);
    }

    /**
     * 浏览指定节点的子节点（内部实现）
     */
    private List<Map<String, Object>> browseNodeChildrenInternal(NodeId nodeId) {
        List<Map<String, Object>> children = new ArrayList<>();

        try {
            String nodeIdStr = nodeId.toParseableString();

            // 对于Objects节点，返回标准子节点
            if ("ns=0;i=85".equals(nodeIdStr)) { // Objects folder
                children.addAll(getObjectsChildren());
            }
            // 对于HelloWorld节点，添加我们的自定义变量
            else if ("ns=1;s=HelloWorld".equals(nodeIdStr)) {
                addHelloWorldVariables(children);
            }
            // 对于Server节点，添加Server的子节点
            else if ("ns=0;i=2253".equals(nodeIdStr)) { // Server object
                children.addAll(getServerChildren());
            }

            logger.debug("Browsed {} children for node: {}", children.size(), nodeId);
        } catch (Exception e) {
            logger.warn("Failed to browse children for node: {}", nodeId, e);
        }

        return children;
    }

    /**
     * 获取Objects节点的子节点
     */
    private List<Map<String, Object>> getObjectsChildren() {
        List<Map<String, Object>> children = new ArrayList<>();

        // Server节点
        Map<String, Object> serverNode = new ConcurrentHashMap<>();
        serverNode.put("browseName", "Server");
        serverNode.put("displayName", "Server");
        serverNode.put("nodeId", "ns=0;i=2253");
        serverNode.put("nodeClass", "Object");
        children.add(serverNode);

        // 其他标准节点可以在这里添加
        // 注意：实际的NodeId需要根据OPC UA规范确定

        return children;
    }

    /**
     * 获取Server节点的子节点
     */
    private List<Map<String, Object>> getServerChildren() {
        List<Map<String, Object>> children = new ArrayList<>();

        // ServerStatus节点
        Map<String, Object> serverStatusNode = new ConcurrentHashMap<>();
        serverStatusNode.put("browseName", "ServerStatus");
        serverStatusNode.put("displayName", "ServerStatus");
        serverStatusNode.put("nodeId", "ns=0;i=2256");
        serverStatusNode.put("nodeClass", "Variable");
        children.add(serverStatusNode);

        // ServiceLevel节点
        Map<String, Object> serviceLevelNode = new ConcurrentHashMap<>();
        serviceLevelNode.put("browseName", "ServiceLevel");
        serviceLevelNode.put("displayName", "ServiceLevel");
        serviceLevelNode.put("nodeId", "ns=0;i=2267");
        serviceLevelNode.put("nodeClass", "Variable");
        children.add(serviceLevelNode);

        // Auditing节点
        Map<String, Object> auditingNode = new ConcurrentHashMap<>();
        auditingNode.put("browseName", "Auditing");
        auditingNode.put("displayName", "Auditing");
        auditingNode.put("nodeId", "ns=0;i=2994");
        auditingNode.put("nodeClass", "Variable");
        children.add(auditingNode);

        return children;
    }

    /**
     * 添加HelloWorld节点的变量子节点
     */
    private void addHelloWorldVariables(List<Map<String, Object>> children) {
        String[] variableNames = {"Temperature", "Pressure", "Humidity"};
        for (String varName : variableNames) {
            Map<String, Object> childInfo = new ConcurrentHashMap<>();
            childInfo.put("browseName", varName);
            childInfo.put("displayName", varName);
            childInfo.put("nodeId", "ns=1;s=HelloWorld/" + varName);
            childInfo.put("nodeClass", "Variable");
            children.add(childInfo);
        }
    }

    /**
     * 从UaNode创建节点信息
     */
    private Map<String, Object> createNodeInfo(org.eclipse.milo.opcua.sdk.server.nodes.UaNode node) {
        try {
            Map<String, Object> nodeInfo = new ConcurrentHashMap<>();

            // NodeClass
            nodeInfo.put("nodeClass", node.getNodeClass().name());

            // BrowseName
            nodeInfo.put("browseName", node.getBrowseName().getName());

            // DisplayName
            nodeInfo.put("displayName", node.getDisplayName().getText());

            // NodeId
            nodeInfo.put("nodeId", node.getNodeId().toParseableString());

            return nodeInfo;
        } catch (Exception e) {
            logger.warn("Failed to create node info for: {}", node.getNodeId(), e);
            return null;
        }
    }

    /**
     * 判断是否为层次化引用
     */
    private boolean isHierarchicalReference(NodeId referenceTypeId) {
        // HasComponent: ns=0;i=47
        // Organizes: ns=0;i=35
        // HasChild: ns=0;i=34 (父类型)
        return referenceTypeId.equals(new NodeId(0, 47)) ||  // HasComponent
               referenceTypeId.equals(new NodeId(0, 35)) ||  // Organizes
               referenceTypeId.equals(new NodeId(0, 34));    // HasChild
    }

    /**
     * 获取所有动态节点信息
     */
    public Map<String, Object> getAllNodes() {
        Map<String, Object> result = new ConcurrentHashMap<>();
        
        Map<String, Object> folders = new ConcurrentHashMap<>();
        dynamicFolders.forEach((path, node) -> {
            Map<String, Object> info = new ConcurrentHashMap<>();
            info.put("type", "folder");
            info.put("displayName", node.getDisplayName().getText());
            info.put("nodeId", node.getNodeId().toParseableString());
            folders.put(path, info);
        });
        
        Map<String, Object> variables = new ConcurrentHashMap<>();
        dynamicNodes.forEach((path, node) -> {
            Map<String, Object> info = new ConcurrentHashMap<>();
            info.put("type", "variable");
            info.put("displayName", node.getDisplayName().getText());
            info.put("nodeId", node.getNodeId().toParseableString());
            info.put("value", node.getValue().getValue().getValue());
            info.put("dataType", node.getDataType().toParseableString());
            info.put("accessLevel", getAccessLevelString(node.getAccessLevel()));
            info.put("userAccessLevel", getAccessLevelString(node.getUserAccessLevel()));
            variables.put(path, info);
        });
        
        // 添加 HelloWorld 文件夹和变量（它们是在 SimpleNamespace 中创建的）
        try {
            // 添加 HelloWorld 文件夹
            Map<String, Object> helloWorldFolder = new ConcurrentHashMap<>();
            helloWorldFolder.put("displayName", "HelloWorld");
            helloWorldFolder.put("nodeId", "ns=1;s=HelloWorld");
            helloWorldFolder.put("type", "folder");
            folders.put("HelloWorld", helloWorldFolder);

            // 添加 HelloWorld 变量
            String[] variableNames = {"Temperature", "Pressure", "Humidity"};
            for (String varName : variableNames) {
                Map<String, Object> varInfo = new ConcurrentHashMap<>();
                String path = "HelloWorld/" + varName;

                UaVariableNode helloNode = namespace.getHelloWorldVariableNode(path);
                if (helloNode != null) {
                    varInfo.put("displayName", varName);
                    varInfo.put("nodeId", helloNode.getNodeId().toParseableString());
                    varInfo.put("type", "variable");
                    varInfo.put("dataType", helloNode.getDataType().toParseableString());
                    varInfo.put("accessLevel", getAccessLevelString(helloNode.getAccessLevel()));
                    varInfo.put("userAccessLevel", getAccessLevelString(helloNode.getUserAccessLevel()));

                    Object value = null;
                    try {
                        DataValue dv = helloNode.getValue();
                        if (dv != null && dv.getValue() != null) {
                            value = dv.getValue().getValue();
                        }
                    } catch (Exception ignored) {
                    }

                    varInfo.put("value", value != null ? value : 0.0);
                } else {
                    // 回退到静态信息
                    varInfo.put("displayName", varName);
                    varInfo.put("nodeId", "ns=1;s=HelloWorld/" + varName);
                    varInfo.put("type", "variable");
                    varInfo.put("dataType", "ns=0;i=11");
                    varInfo.put("accessLevel", "READ_WRITE");
                    varInfo.put("userAccessLevel", "READ_WRITE");
                    varInfo.put("value", 0.0);
                }

                variables.put(path, varInfo);
            }
        } catch (Exception e) {
            logger.warn("Failed to add HelloWorld nodes to result", e);
        }

        result.put("folders", folders);
        result.put("variables", variables);
        return result;
    }

    private NodeId getDataTypeId(String dataType) {
        switch (dataType.toLowerCase()) {
            case "double":
            case "float":
                return NodeIds.Double;
            case "int":
            case "integer":
                return NodeIds.Int32;
            case "long":
                return NodeIds.Int64;
            case "string":
                return NodeIds.String;
            case "boolean":
            case "bool":
                return NodeIds.Boolean;
            default:
                return null;
        }
    }

    private Object convertValue(Object value, String dataType) {
        if (value == null) return null;
        
        try {
            switch (dataType.toLowerCase()) {
                case "double":
                case "float":
                    return Double.valueOf(value.toString());
                case "int":
                case "integer":
                    return Integer.valueOf(value.toString());
                case "long":
                    return Long.valueOf(value.toString());
                case "string":
                    return value.toString();
                case "boolean":
                case "bool":
                    return Boolean.valueOf(value.toString());
                default:
                    return value;
            }
        } catch (Exception e) {
            logger.warn("Failed to convert value {} to {}, using original value", value, dataType);
            return value;
        }
    }

    private Set<AccessLevel> getAccessLevelSet(String accessLevel) {
        if (accessLevel == null) {
            return AccessLevel.READ_WRITE;
        }

        switch (accessLevel.toUpperCase()) {
            case "READ_ONLY":
            case "READONLY":
                return AccessLevel.READ_ONLY;
            case "WRITE_ONLY":
            case "WRITEONLY":
                return AccessLevel.WRITE_ONLY;
            case "READ_WRITE":
            case "READWRITE":
            default:
                return AccessLevel.READ_WRITE;
            case "NONE":
                return AccessLevel.NONE;
        }
    }

    private String getAccessLevelString(org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte accessLevel) {
        Set<AccessLevel> levels = AccessLevel.fromValue(accessLevel);

        if (levels.equals(AccessLevel.READ_WRITE)) {
            return "READ_WRITE";
        } else if (levels.equals(AccessLevel.READ_ONLY)) {
            return "READ_ONLY";
        } else if (levels.equals(AccessLevel.WRITE_ONLY)) {
            return "WRITE_ONLY";
        } else if (levels.equals(AccessLevel.NONE)) {
            return "NONE";
        } else {
            return levels.toString();
        }
    }

    private UaFolderNode findParentFolder(String path) {
        // 查找最匹配的父文件夹
        String longestMatch = "";
        UaFolderNode parentFolder = null;

        for (Map.Entry<String, UaFolderNode> entry : dynamicFolders.entrySet()) {
            String folderPath = entry.getKey();
            if (path.startsWith(folderPath + "/") && folderPath.length() > longestMatch.length()) {
                longestMatch = folderPath;
                parentFolder = entry.getValue();
            }
        }

        return parentFolder;
    }

    /**
     * 生成下一个可用的数字NodeId
     */
    public synchronized long generateNextNumericId() {
        while (isNodeIdExists("numeric", String.valueOf(nextNumericId))) {
            nextNumericId++;
        }
        long result = nextNumericId;
        nextNumericId++;
        return result;
    }

    /**
     * 检查NodeId是否已存在
     */
    public boolean isNodeIdExists(String nodeIdType, String nodeIdValue) {
        if (nodeIdValue == null || nodeIdValue.trim().isEmpty()) {
            return false;
        }

        String identifier = nodeIdValue.trim();

        switch (nodeIdType.toLowerCase()) {
            case "numeric":
                return usedNumericIds.contains(identifier);
            case "string":
                return usedStringIds.contains(identifier);
            case "guid":
                return usedGuidIds.contains(identifier);
            case "opaque":
                return usedOpaqueIds.contains(identifier);
            default:
                return usedStringIds.contains(identifier);
        }
    }

    /**
     * 记录已使用的NodeId
     */
    private void recordUsedNodeId(String nodeIdType, String nodeIdValue) {
        if (nodeIdValue == null || nodeIdValue.trim().isEmpty()) {
            return;
        }

        String identifier = nodeIdValue.trim();

        switch (nodeIdType.toLowerCase()) {
            case "numeric":
                usedNumericIds.add(identifier);
                break;
            case "string":
                usedStringIds.add(identifier);
                break;
            case "guid":
                usedGuidIds.add(identifier);
                break;
            case "opaque":
                usedOpaqueIds.add(identifier);
                break;
            default:
                usedStringIds.add(identifier);
                break;
        }
    }

    /**
     * 根据NodeId类型和值创建NodeId
     */
    private NodeId createNodeId(String nodeIdType, String nodeIdValue, String fallbackPath) {
        try {
            // 如果没有指定nodeIdValue，使用fallbackPath
            String identifier = (nodeIdValue != null && !nodeIdValue.trim().isEmpty()) ? nodeIdValue.trim() : fallbackPath;

            NodeId result;
            String actualType = nodeIdType.toLowerCase();
            String actualIdentifier = identifier;

            switch (actualType) {
                case "numeric":
                    try {
                        // 尝试将identifier转换为数字
                        long numericId = Long.parseLong(identifier);
                        result = new NodeId(namespace.getNamespaceIndex(), org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger.valueOf(numericId));
                        actualIdentifier = String.valueOf(numericId);
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid numeric NodeId value: {}, falling back to string type", identifier);
                        result = new NodeId(namespace.getNamespaceIndex(), identifier);
                        actualType = "string";
                        actualIdentifier = identifier;
                    }
                    break;
                case "guid":
                    try {
                        java.util.UUID uuid = java.util.UUID.fromString(identifier);
                        result = new NodeId(namespace.getNamespaceIndex(), uuid);
                        actualIdentifier = uuid.toString();
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid GUID NodeId value: {}, falling back to string type", identifier);
                        result = new NodeId(namespace.getNamespaceIndex(), identifier);
                        actualType = "string";
                        actualIdentifier = identifier;
                    }
                    break;
                case "opaque":
                    try {
                        // 假设opaque是Base64编码的字符串
                        byte[] bytes = java.util.Base64.getDecoder().decode(identifier);
                        result = new NodeId(namespace.getNamespaceIndex(), org.eclipse.milo.opcua.stack.core.types.builtin.ByteString.of(bytes));
                        actualIdentifier = identifier; // 保持Base64字符串作为标识符
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid Opaque NodeId value: {}, falling back to string type", identifier);
                        result = new NodeId(namespace.getNamespaceIndex(), identifier);
                        actualType = "string";
                        actualIdentifier = identifier;
                    }
                    break;
                case "string":
                default:
                    result = new NodeId(namespace.getNamespaceIndex(), identifier);
                    actualType = "string";
                    actualIdentifier = identifier;
                    break;
            }

            // 记录已使用的NodeId
            recordUsedNodeId(actualType, actualIdentifier);

            return result;
        } catch (Exception e) {
            logger.error("Error creating NodeId with type: {}, value: {}, falling back to string type", nodeIdType, nodeIdValue, e);
            NodeId fallbackResult = new NodeId(namespace.getNamespaceIndex(), fallbackPath);
            recordUsedNodeId("string", fallbackPath);
            return fallbackResult;
        }
    }

    /**
     * 获取节点的引用信息
     */
    public Map<String, Object> getNodeReferences(String path) {
        Map<String, Object> result = new ConcurrentHashMap<>();
        List<Map<String, Object>> references = new ArrayList<>();

        try {
            UaNode node = null;

            // 查找节点
            if (dynamicNodes.containsKey(path)) {
                node = dynamicNodes.get(path);
            } else if (dynamicFolders.containsKey(path)) {
                node = dynamicFolders.get(path);
            }

            if (node != null) {
                // 获取节点的所有引用
                List<Reference> nodeReferences = node.getReferences();

                for (Reference ref : nodeReferences) {
                    Map<String, Object> refInfo = new ConcurrentHashMap<>();

                    // 获取引用类型名称
                    String referenceTypeName = getReferenceTypeName(ref.getReferenceTypeId());
                    refInfo.put("referenceType", referenceTypeName);

                    // 确定方向
                    String direction = ref.isInverse() ? "Inverse" : "Forward";
                    refInfo.put("direction", direction);

                    // 获取目标节点信息
                    ExpandedNodeId expandedTargetNodeId = ref.getTargetNodeId();
                    NodeId targetNodeId = expandedTargetNodeId.toNodeId(namespace.getNamespaceTable()).orElse(null);

                    if (targetNodeId != null) {
                        String targetNodeName = getNodeDisplayName(targetNodeId);
                        String targetBrowseName = getNodeBrowseName(targetNodeId);

                        refInfo.put("targetNode", targetNodeName);
                        refInfo.put("browseName", targetBrowseName);
                        refInfo.put("targetNodeId", targetNodeId.toParseableString());
                    } else {
                        refInfo.put("targetNode", expandedTargetNodeId.toParseableString());
                        refInfo.put("browseName", expandedTargetNodeId.toParseableString());
                        refInfo.put("targetNodeId", expandedTargetNodeId.toParseableString());
                    }

                    references.add(refInfo);
                }
            }

            result.put("references", references);
            result.put("path", path);

        } catch (Exception e) {
            logger.error("Failed to get references for node: {}", path, e);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 获取引用类型名称
     */
    private String getReferenceTypeName(NodeId referenceTypeId) {
        try {
            if (referenceTypeId.equals(NodeIds.Organizes)) {
                return "Organizes";
            } else if (referenceTypeId.equals(NodeIds.HasTypeDefinition)) {
                return "HasTypeDefinition";
            } else if (referenceTypeId.equals(NodeIds.HasComponent)) {
                return "HasComponent";
            } else if (referenceTypeId.equals(NodeIds.HasProperty)) {
                return "HasProperty";
            } else if (referenceTypeId.equals(NodeIds.HasSubtype)) {
                return "HasSubtype";
            } else {
                // 尝试从节点管理器获取引用类型节点的显示名称
                return referenceTypeId.toParseableString();
            }
        } catch (Exception e) {
            return referenceTypeId.toParseableString();
        }
    }

    /**
     * 获取节点显示名称
     */
    private String getNodeDisplayName(NodeId nodeId) {
        try {
            // 检查是否是我们管理的节点
            for (Map.Entry<String, UaVariableNode> entry : dynamicNodes.entrySet()) {
                if (entry.getValue().getNodeId().equals(nodeId)) {
                    return entry.getValue().getDisplayName().getText();
                }
            }

            for (Map.Entry<String, UaFolderNode> entry : dynamicFolders.entrySet()) {
                if (entry.getValue().getNodeId().equals(nodeId)) {
                    return entry.getValue().getDisplayName().getText();
                }
            }

            // 检查标准节点
            if (nodeId.equals(NodeIds.ObjectsFolder)) {
                return "Objects";
            } else if (nodeId.equals(Identifiers.BaseDataVariableType)) {
                return "BaseDataVariableType";
            } else if (nodeId.equals(Identifiers.FolderType)) {
                return "FolderType";
            }

            return nodeId.toParseableString();
        } catch (Exception e) {
            return nodeId.toParseableString();
        }
    }

    /**
     * 获取节点浏览名称
     */
    private String getNodeBrowseName(NodeId nodeId) {
        try {
            // 检查是否是我们管理的节点
            for (Map.Entry<String, UaVariableNode> entry : dynamicNodes.entrySet()) {
                if (entry.getValue().getNodeId().equals(nodeId)) {
                    return entry.getValue().getBrowseName().getName();
                }
            }

            for (Map.Entry<String, UaFolderNode> entry : dynamicFolders.entrySet()) {
                if (entry.getValue().getNodeId().equals(nodeId)) {
                    return entry.getValue().getBrowseName().getName();
                }
            }

            // 检查标准节点
            if (nodeId.equals(NodeIds.ObjectsFolder)) {
                return "Objects";
            } else if (nodeId.equals(Identifiers.BaseDataVariableType)) {
                return "BaseDataVariableType";
            } else if (nodeId.equals(Identifiers.FolderType)) {
                return "FolderType";
            }

            return getNodeDisplayName(nodeId);
        } catch (Exception e) {
            return nodeId.toParseableString();
        }
    }

    /**
     * 从数据库实体创建文件夹节点
     */
    private void createFolderFromEntity(OpcNode entity) {
        try {
            String nodeIdStr = entity.getNodeId();
            if (nodeIdStr == null || nodeIdStr.isEmpty()) {
                nodeIdStr = persistenceService.generateNextNodeId();
                entity.setNodeId(nodeIdStr);
                persistenceService.saveNode(entity);
            }

            NodeId nodeId = parseNodeId(nodeIdStr);
            if (nodeId == null) {
                logger.error("无效的NodeId: {}", nodeIdStr);
                return;
            }

            // 创建文件夹节点
            UaFolderNode folderNode = new UaFolderNode(
                namespace.getNodeContext(),
                nodeId,
                new QualifiedName(namespace.getNamespaceIndex(), entity.getDisplayName()),
                LocalizedText.english(entity.getDisplayName())
            );

            // 添加到命名空间
            namespace.getNodeManager().addNode(folderNode);

            // 建立父子关系
            if (entity.getParentPath() != null && !entity.getParentPath().isEmpty()) {
                UaFolderNode parentFolder = dynamicFolders.get(entity.getParentPath());
                if (parentFolder != null) {
                    parentFolder.addOrganizes(folderNode);
                }
            } else {
                // 添加到Objects文件夹下
                folderNode.addReference(new Reference(
                    folderNode.getNodeId(),
                    NodeIds.Organizes,
                    NodeIds.ObjectsFolder.expanded(),
                    false
                ));
            }

            // 保存到内存映射
            dynamicFolders.put(entity.getPath(), folderNode);
            markNodeIdAsUsed(nodeIdStr);

        } catch (Exception e) {
            logger.error("从实体创建文件夹节点失败: {}", entity.getPath(), e);
            throw e;
        }
    }

    /**
     * 从数据库实体创建变量节点
     */
    private void createVariableFromEntity(OpcNode entity) {
        try {
            String nodeIdStr = entity.getNodeId();
            if (nodeIdStr == null || nodeIdStr.isEmpty()) {
                nodeIdStr = persistenceService.generateNextNodeId();
                entity.setNodeId(nodeIdStr);
                persistenceService.saveNode(entity);
            }

            NodeId nodeId = parseNodeId(nodeIdStr);
            if (nodeId == null) {
                logger.error("无效的NodeId: {}", nodeIdStr);
                return;
            }

            // 解析数据类型
            NodeId dataTypeId = getDataTypeNodeId(entity.getDataType());

            // 反序列化值
            Object value = persistenceService.deserializeValue(entity.getCurrentValue(), entity.getValueType());
            if (value == null) {
                value = getDefaultValue(entity.getDataType());
            }

            // 创建变量节点
            Set<AccessLevel> accessLevelSet = getAccessLevelSet(entity.getAccessLevel());
            UaVariableNode variableNode = new UaVariableNode.UaVariableNodeBuilder(namespace.getNodeContext())
                .setNodeId(nodeId)
                .setAccessLevel(accessLevelSet)
                .setUserAccessLevel(accessLevelSet)
                .setBrowseName(new QualifiedName(namespace.getNamespaceIndex(), entity.getDisplayName()))
                .setDisplayName(LocalizedText.english(entity.getDisplayName()))
                .setDataType(dataTypeId)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

            // 设置初始值
            variableNode.setValue(new DataValue(new Variant(value)));

            // 添加到命名空间
            namespace.getNodeManager().addNode(variableNode);

            // 建立父子关系
            if (entity.getParentPath() != null && !entity.getParentPath().isEmpty()) {
                UaFolderNode parentFolder = dynamicFolders.get(entity.getParentPath());
                if (parentFolder != null) {
                    parentFolder.addOrganizes(variableNode);
                }
            } else {
                // 添加到Objects文件夹下
                variableNode.addReference(new Reference(
                    variableNode.getNodeId(),
                    NodeIds.Organizes,
                    NodeIds.ObjectsFolder.expanded(),
                    false
                ));
            }

            // 保存到内存映射
            dynamicNodes.put(entity.getPath(), variableNode);
            markNodeIdAsUsed(nodeIdStr);

        } catch (Exception e) {
            logger.error("从实体创建变量节点失败: {}", entity.getPath(), e);
            throw e;
        }
    }

    /**
     * 解析NodeId字符串
     */
    private NodeId parseNodeId(String nodeIdStr) {
        try {
            return NodeId.parse(nodeIdStr);
        } catch (Exception e) {
            logger.error("解析NodeId失败: {}", nodeIdStr, e);
            return null;
        }
    }

    /**
     * 标记NodeId为已使用
     */
    private void markNodeIdAsUsed(String nodeIdStr) {
        if (nodeIdStr.contains("i=")) {
            usedNumericIds.add(nodeIdStr);
        } else if (nodeIdStr.contains("s=")) {
            usedStringIds.add(nodeIdStr);
        } else if (nodeIdStr.contains("g=")) {
            usedGuidIds.add(nodeIdStr);
        } else if (nodeIdStr.contains("b=")) {
            usedOpaqueIds.add(nodeIdStr);
        }
    }

    /**
     * 获取数据类型的默认值
     */
    private Object getDefaultValue(String dataType) {
        if (dataType == null) {
            return 0;
        }

        switch (dataType.toLowerCase()) {
            case "string":
                return "";
            case "boolean":
                return false;
            case "int32":
            case "integer":
                return 0;
            case "double":
                return 0.0;
            case "float":
                return 0.0f;
            case "int64":
            case "long":
                return 0L;
            default:
                return 0;
        }
    }

    /**
     * 获取数据类型对应的NodeId
     */
    private NodeId getDataTypeNodeId(String dataType) {
        if (dataType == null) {
            return Identifiers.Int32;
        }

        switch (dataType.toLowerCase()) {
            case "string":
                return Identifiers.String;
            case "boolean":
                return Identifiers.Boolean;
            case "int32":
            case "integer":
                return Identifiers.Int32;
            case "double":
                return Identifiers.Double;
            case "float":
                return Identifiers.Float;
            case "int64":
            case "long":
                return Identifiers.Int64;
            default:
                return Identifiers.Int32;
        }
    }

    /**
     * 获取父路径
     */
    private String getParentPath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) {
            return null; // 根节点没有父路径
        }

        return path.substring(0, lastSlash);
    }

    /**
     * 检查节点是否存在
     */
    public boolean nodeExists(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        // 检查是否存在于内存映射中
        boolean existsInMemory = dynamicNodes.containsKey(path) || dynamicFolders.containsKey(path);

        // 如果内存中不存在，检查数据库
        if (!existsInMemory && persistenceService != null) {
            return persistenceService.existsByPath(path);
        }

        return existsInMemory;
    }

    /**
     * 获取指定路径节点的当前值
     */
    public Object getNodeValue(String path) {
        try {
            // 首先检查动态变量节点
            UaVariableNode variableNode = dynamicNodes.get(path);
            if (variableNode != null) {
                DataValue dataValue = variableNode.getValue();
                if (dataValue != null && dataValue.getValue() != null) {
                    return dataValue.getValue().getValue();
                }
            }

            // 如果不是动态节点，可能是HelloWorld示例节点
            if (path.startsWith("HelloWorld/")) {
                // 对于HelloWorld节点，返回模拟值
                String variableName = path.substring("HelloWorld/".length());
                switch (variableName) {
                    case "Temperature":
                        return 25.0 + Math.random() * 10; // 25-35度
                    case "Pressure":
                        return 1013.25 + Math.random() * 50; // 大气压变化
                    case "Humidity":
                        return 40.0 + Math.random() * 40; // 40-80%湿度
                    default:
                        return 0.0;
                }
            }

            logger.warn("Node not found or has no value: " + path);
            return null;
        } catch (Exception e) {
            logger.error("Error getting node value for path: " + path, e);
            return null;
        }
    }
}
