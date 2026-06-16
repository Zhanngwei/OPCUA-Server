package org.go.show.multiproto.opcuaserver.controller;

import org.go.show.multiproto.opcuaserver.DynamicNodeManager;
import org.go.show.multiproto.opcuaserver.dto.BatchCreateRequest;
import org.go.show.multiproto.opcuaserver.dto.BatchCreateResponse;
import org.go.show.multiproto.opcuaserver.service.BatchNodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 公共API控制器 - 完全无需认证
 * 使用/public路径前缀，完全绕过认证系统
 */
@RestController
@RequestMapping("/public")
@CrossOrigin(origins = "*")
public class PublicApiController {

    private static final Logger logger = LoggerFactory.getLogger(PublicApiController.class);
    
    @Autowired
    private BatchNodeService batchNodeService;
    
    private static DynamicNodeManager dynamicNodeManager;

    public static void setDynamicNodeManager(DynamicNodeManager manager) {
        dynamicNodeManager = manager;
    }

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public ResponseEntity<Object> health() {
        logger.info("*** 公共健康检查API被调用 ***");
        return ResponseEntity.ok(java.util.Map.of(
            "status", "ok",
            "message", "Public API is working",
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 批量创建节点 - 完全无认证
     */
    @PostMapping("/batch-create-nodes")
    public ResponseEntity<BatchCreateResponse> batchCreateNodes(@RequestBody BatchCreateRequest request) {
        try {
            logger.info("*** 公共批量创建API被调用 ***");
            logger.info("收到批量创建请求，节点数量: {}",
                request.getItems() != null ? request.getItems().size() : 0);

            if (dynamicNodeManager == null) {
                logger.error("节点管理器未初始化");
                BatchCreateResponse errorResponse = new BatchCreateResponse();
                errorResponse.setSuccess(false);
                errorResponse.setMessage("节点管理器未初始化");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 设置BatchNodeService的DynamicNodeManager
            batchNodeService.setDynamicNodeManager(dynamicNodeManager);

            // 执行批量创建
            BatchCreateResponse response = batchNodeService.batchCreateNodes(request);

            logger.info("公共批量创建完成，成功: {}, 消息: {}", response.isSuccess(), response.getMessage());

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            logger.error("公共批量创建节点失败", e);

            BatchCreateResponse errorResponse = new BatchCreateResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("批量创建失败: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * API信息端点
     */
    @GetMapping("/info")
    public ResponseEntity<Object> info() {
        logger.info("*** 公共API信息被调用 ***");
        return ResponseEntity.ok(java.util.Map.of(
            "name", "OPC UA Public API",
            "version", "1.0.0",
            "description", "Public API for OPC UA node creation without authentication",
            "endpoints", java.util.Map.of(
                "health", "/public/health",
                "batch-create", "/public/batch-create-nodes",
                "info", "/public/info"
            )
        ));
    }
}
