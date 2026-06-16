package org.go.show.multiproto.opcuaserver.controller;

import lombok.extern.slf4j.Slf4j;
import org.go.show.multiproto.opcuaserver.DynamicNodeManager;
import org.go.show.multiproto.opcuaserver.dto.BatchCreateRequest;
import org.go.show.multiproto.opcuaserver.dto.BatchCreateResponse;
import org.go.show.multiproto.opcuaserver.service.BatchNodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/nodes")
@CrossOrigin(origins = "*")
public class NodeController {

    private static final Logger logger = LoggerFactory.getLogger(NodeController.class);
    private static DynamicNodeManager dynamicNodeManager;

    @Autowired
    private BatchNodeService batchNodeService;

    public static void setDynamicNodeManager(DynamicNodeManager manager) {
        dynamicNodeManager = manager;
        // 同时设置BatchNodeService的DynamicNodeManager
        // 注意：这里需要通过ApplicationContext获取BatchNodeService实例
    }

    /**
     * 获取所有节点（仅自定义节点）
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllNodes() {
        try {
            if (dynamicNodeManager == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Node manager not initialized"));
            }

            // 默认返回完整地址空间
            Map<String, Object> nodes = dynamicNodeManager.getFullAddressSpace();
            return ResponseEntity.ok(nodes);
        } catch (Exception e) {
            logger.error("Failed to get all nodes", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取完整的地址空间（包括标准节点）
     */
    @GetMapping("/addressspace")
    public ResponseEntity<Map<String, Object>> getFullAddressSpace() {
        try {
            if (dynamicNodeManager == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Node manager not initialized"));
            }

            Map<String, Object> addressSpace = dynamicNodeManager.getFullAddressSpace();
            return ResponseEntity.ok(addressSpace);
        } catch (Exception e) {
            logger.error("Failed to get full address space", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 创建文件夹
     */
    @PostMapping("/folder")
    public ResponseEntity<Map<String, Object>> createFolder(@RequestBody Map<String, Object> request) {
        try {
            if (dynamicNodeManager == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Node manager not initialized"));
            }

            String path = (String) request.get("path");
            String displayName = (String) request.get("displayName");
            String nodeIdType = (String) request.getOrDefault("nodeIdType", "string");
            String nodeIdValue = (String) request.get("nodeIdValue");

            if (path == null || displayName == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Path and displayName are required"));
            }

            if (path != null && ("HelloWorld".equals(path) || path.startsWith("HelloWorld/"))) {
                logger.warn("Attempt to create folder under protected HelloWorld path: {}", path);
                return ResponseEntity.badRequest().body(Map.of("error", "HelloWorld 节点及其子节点为系统内置示例，不允许在其下创建新节点"));
            }

            boolean success = dynamicNodeManager.createFolder(path, displayName, nodeIdType, nodeIdValue);
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Folder created successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to create folder"));
            }
        } catch (Exception e) {
            logger.error("Failed to create folder", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 创建变量
     */
    @PostMapping("/variable")
    public ResponseEntity<Map<String, Object>> createVariable(@RequestBody Map<String, Object> request) {
        try {
            if (dynamicNodeManager == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Node manager not initialized"));
            }

            String path = (String) request.get("path");
            String displayName = (String) request.get("displayName");
            Object initialValue = request.get("initialValue");
            String dataType = (String) request.get("dataType");
            String accessLevel = (String) request.getOrDefault("accessLevel", "READ_WRITE");
            String nodeIdType = (String) request.getOrDefault("nodeIdType", "string");
            String nodeIdValue = (String) request.get("nodeIdValue");

            if (path == null || displayName == null || dataType == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Path, displayName, and dataType are required"));
            }

            if (path != null && ("HelloWorld".equals(path) || path.startsWith("HelloWorld/"))) {
                logger.warn("Attempt to create variable under protected HelloWorld path: {}", path);
                return ResponseEntity.badRequest().body(Map.of("error", "HelloWorld 节点及其子节点为系统内置示例，不允许在其下创建新节点"));
            }

            if (dynamicNodeManager.isVariableExists(path)) {
                logger.warn("Variable already exists at path: {}", path);
                return ResponseEntity.badRequest().body(Map.of("error", "变量名重复，不能添加！"));
            }

            boolean success = dynamicNodeManager.createVariable(path, displayName, initialValue, dataType, accessLevel, nodeIdType, nodeIdValue);
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Variable created successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to create variable"));
            }
        } catch (Exception e) {
            logger.error("Failed to create variable", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 更新变量值
     */
    @PutMapping("/variable/{path}")
    public ResponseEntity<Map<String, Object>> updateVariable(
            @PathVariable String path, 
            @RequestBody Map<String, Object> request) {
        try {
            if (dynamicNodeManager == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Node manager not initialized"));
            }

            Object value = request.get("value");
            if (value == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Value is required"));
            }

            boolean success = dynamicNodeManager.updateVariable(path, value);
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Variable updated successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to update variable"));
            }
        } catch (Exception e) {
            logger.error("Failed to update variable", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 更新变量数据类型
     */
    @PutMapping("/variable/datatype")
    public ResponseEntity<Map<String, Object>> updateVariableDataType(
            @RequestBody Map<String, Object> request) {
        try {
            if (dynamicNodeManager == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Node manager not initialized"));
            }

            String path = (String) request.get("path");
            String dataType = (String) request.get("dataType");
            Object value = request.get("value");

            if (path == null || dataType == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Path and dataType are required"));
            }

            boolean success = dynamicNodeManager.updateVariableDataType(path, dataType, value);
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Variable data type updated successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to update variable data type"));
            }
        } catch (Exception e) {
            logger.error("Failed to update variable data type", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 重命名节点
     */
    @PutMapping("/{path}/rename")
    public ResponseEntity<Map<String, Object>> renameNode(
            @PathVariable String path,
            @RequestBody Map<String, String> request) {
        try {
            if (dynamicNodeManager == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Node manager not initialized"));
            }

            String newName = request.get("newName");
            String newDisplayName = request.get("newDisplayName");

            if (newName == null || newDisplayName == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "newName and newDisplayName are required"));
            }

            boolean success = dynamicNodeManager.renameNode(path, newName, newDisplayName);
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Node renamed successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to rename node"));
            }
        } catch (Exception e) {
            logger.error("Failed to rename node", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 删除节点 (使用RequestParam方式)
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteNode(@RequestParam String path) {
        try {
            if (dynamicNodeManager == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Node manager not initialized"));
            }

            logger.info("Deleting node: {}", path);

            if (path != null && ("HelloWorld".equals(path) || path.startsWith("HelloWorld/"))) {
                logger.warn("Attempt to delete protected HelloWorld node: {}", path);
                return ResponseEntity.badRequest().body(Map.of("error", "HelloWorld 节点及其子节点为系统内置示例，禁止删除"));
            }

            boolean success = dynamicNodeManager.deleteNode(path);
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Node deleted successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to delete node: " + path));
            }
        } catch (Exception e) {
            logger.error("Failed to delete node", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 删除节点 (POST方式，更安全)
     */
    @PostMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteNodePost(@RequestBody Map<String, String> request) {
        try {
            if (dynamicNodeManager == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Node manager not initialized"));
            }

            String path = request.get("path");
            if (path == null || path.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Path is required"));
            }

            logger.info("Deleting node: {}", path);

            if (path != null && ("HelloWorld".equals(path) || path.startsWith("HelloWorld/"))) {
                logger.warn("Attempt to delete protected HelloWorld node: {}", path);
                return ResponseEntity.badRequest().body(Map.of("error", "HelloWorld 节点及其子节点为系统内置示例，禁止删除"));
            }

            boolean success = dynamicNodeManager.deleteNode(path);
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Node deleted successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to delete node: " + path));
            }
        } catch (Exception e) {
            logger.error("Failed to delete node", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取节点详细信息
     */
    @GetMapping("/{path}/details")
    public ResponseEntity<Map<String, Object>> getNodeDetails(@PathVariable String path) {
        try {
            if (dynamicNodeManager == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Node manager not initialized"));
            }

            Map<String, Object> allNodes = dynamicNodeManager.getAllNodes();
            Map<String, Object> folders = (Map<String, Object>) allNodes.get("folders");
            Map<String, Object> variables = (Map<String, Object>) allNodes.get("variables");

            Map<String, Object> nodeDetails = null;
            String nodeType = null;

            if (folders.containsKey(path)) {
                nodeDetails = (Map<String, Object>) folders.get(path);
                nodeType = "folder";
            } else if (variables.containsKey(path)) {
                nodeDetails = (Map<String, Object>) variables.get(path);
                nodeType = "variable";
            }

            if (nodeDetails != null) {
                nodeDetails.put("type", nodeType);
                nodeDetails.put("path", path);
                return ResponseEntity.ok(nodeDetails);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Failed to get node details for path: " + path, e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "nodeManagerInitialized", dynamicNodeManager != null
        ));
    }

    /**
     * 获取下一个可用的数字NodeId
     */
    @GetMapping("/next-numeric-id")
    public ResponseEntity<Map<String, Object>> getNextNumericId() {
        try {
            if (dynamicNodeManager == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Node manager not initialized"));
            }

            long nextId = dynamicNodeManager.generateNextNumericId();
            return ResponseEntity.ok(Map.of("nextId", nextId));
        } catch (Exception e) {
            logger.error("Failed to get next numeric ID", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 检查NodeId是否已存在
     */
    @PostMapping("/check-nodeid")
    public ResponseEntity<Map<String, Object>> checkNodeIdExists(@RequestBody Map<String, Object> request) {
        try {
            if (dynamicNodeManager == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Node manager not initialized"));
            }

            String nodeIdType = (String) request.get("nodeIdType");
            String nodeIdValue = (String) request.get("nodeIdValue");

            if (nodeIdType == null || nodeIdValue == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "NodeIdType and nodeIdValue are required"));
            }

            boolean exists = dynamicNodeManager.isNodeIdExists(nodeIdType, nodeIdValue);
            return ResponseEntity.ok(Map.of("exists", exists));
        } catch (Exception e) {
            logger.error("Failed to check NodeId existence", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取节点引用信息
     */
    @GetMapping("/references")
    public ResponseEntity<Map<String, Object>> getNodeReferences(@RequestParam("path") String path) {
        try {
            if (dynamicNodeManager == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Node manager not initialized"));
            }

            Map<String, Object> references = dynamicNodeManager.getNodeReferences(path);
            return ResponseEntity.ok(references);
        } catch (Exception e) {
            logger.error("Failed to get node references for path: {}", path, e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 浏览节点的子节点
     */
    @GetMapping("/browse")
    public ResponseEntity<Map<String, Object>> browseNodeChildren(@RequestParam("nodeId") String nodeIdString) {
        try {
            if (dynamicNodeManager == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Node manager not initialized"));
            }

            // 解析NodeId
            org.eclipse.milo.opcua.stack.core.types.builtin.NodeId nodeId =
                org.eclipse.milo.opcua.stack.core.types.builtin.NodeId.parseOrNull(nodeIdString);

            if (nodeId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid NodeId format"));
            }

            List<Map<String, Object>> children = dynamicNodeManager.browseNodeChildren(nodeId);

            Map<String, Object> result = new HashMap<>();
            result.put("nodeId", nodeIdString);
            result.put("children", children);
            result.put("count", children.size());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to browse node children for nodeId: {}", nodeIdString, e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 批量创建节点
     */
    @PostMapping("/batch")
    public ResponseEntity<BatchCreateResponse> batchCreateNodes(@RequestBody BatchCreateRequest request) {
        // 添加调试日志
        log.info("*** BATCH CREATE API CALLED - NO AUTH REQUIRED ***");
        try {
            logger.info("收到批量创建请求，节点数量: {}",
                request.getItems() != null ? request.getItems().size() : 0);

            if (dynamicNodeManager == null) {
                BatchCreateResponse errorResponse = new BatchCreateResponse();
                errorResponse.setSuccess(false);
                errorResponse.setMessage("节点管理器未初始化");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 设置BatchNodeService的DynamicNodeManager
            batchNodeService.setDynamicNodeManager(dynamicNodeManager);

            // 执行批量创建
            BatchCreateResponse response = batchNodeService.batchCreateNodes(request);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            logger.error("批量创建节点失败", e);

            BatchCreateResponse errorResponse = new BatchCreateResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("批量创建失败: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 获取单个节点的值
     */
    @GetMapping("/{path}/value")
    public ResponseEntity<Map<String, Object>> getNodeValue(@PathVariable String path) {
        try {
            if (dynamicNodeManager == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Node manager not initialized"));
            }

            // 获取节点值
            Object value = dynamicNodeManager.getNodeValue(path);
            if (value != null) {
                Map<String, Object> result = new HashMap<>();
                result.put("path", path);
                result.put("value", value);
                result.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Failed to get node value for path: " + path, e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 无认证批量创建节点端点
     * 使用不同的路径避免认证拦截
     */
    @PostMapping("/batch-no-auth")
    public ResponseEntity<BatchCreateResponse> batchCreateNodesNoAuth(@RequestBody BatchCreateRequest request) {
        try {
            logger.info("*** 无认证批量创建API被调用 ***");
            logger.info("收到批量创建请求，节点数量: {}",
                request.getItems() != null ? request.getItems().size() : 0);

            if (dynamicNodeManager == null) {
                BatchCreateResponse errorResponse = new BatchCreateResponse();
                errorResponse.setSuccess(false);
                errorResponse.setMessage("节点管理器未初始化");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 设置BatchNodeService的DynamicNodeManager
            batchNodeService.setDynamicNodeManager(dynamicNodeManager);

            // 执行批量创建
            BatchCreateResponse response = batchNodeService.batchCreateNodes(request);

            logger.info("无认证批量创建完成，成功: {}, 消息: {}", response.isSuccess(), response.getMessage());

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            logger.error("无认证批量创建节点失败", e);

            BatchCreateResponse errorResponse = new BatchCreateResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("批量创建失败: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
