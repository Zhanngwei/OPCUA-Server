package org.go.show.multiproto.opcuaserver.controller;

import org.go.show.multiproto.opcuaserver.model.EndpointConfig;
import org.go.show.multiproto.opcuaserver.model.EndpointInfo;
import org.go.show.multiproto.opcuaserver.service.EndpointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;
import java.util.Map;

/**
 * 端点管理控制器
 */
@RestController
@RequestMapping("/api/endpoints")
@CrossOrigin(origins = "*")
public class EndpointController {
    
    @Autowired
    private EndpointService endpointService;
    
    /**
     * 获取当前活跃的端点列表
     */
    @GetMapping("/active")
    public ResponseEntity<List<EndpointInfo>> getActiveEndpoints() {
        try {
            List<EndpointInfo> endpoints = endpointService.getActiveEndpoints();
            return ResponseEntity.ok(endpoints);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取端点配置
     */
    @GetMapping("/config")
    public ResponseEntity<EndpointConfig> getEndpointConfig() {
        try {
            EndpointConfig config = endpointService.getEndpointConfig();
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 更新端点配置
     */
    @RequestMapping(value = "/config", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<Map<String, Object>> updateEndpointConfig(@RequestBody EndpointConfig config) {
        try {
            boolean success = endpointService.updateEndpointConfig(config);
            
            Map<String, Object> response = Map.of(
                "success", success,
                "message", success ? "Endpoint configuration updated successfully" : "Failed to update endpoint configuration"
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Error updating endpoint configuration: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 应用端点配置（重启服务器）
     */
    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> applyEndpointConfig() {
        try {
            boolean success = endpointService.applyEndpointConfig();
            
            Map<String, Object> response = Map.of(
                "success", success,
                "message", success ? "Endpoint configuration applied successfully. Server will restart." : "Failed to apply endpoint configuration"
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Error applying endpoint configuration: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取端点统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getEndpointStats() {
        try {
            Map<String, Object> stats = endpointService.getEndpointStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 测试端点连接
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testEndpoint(@RequestBody Map<String, String> request) {
        try {
            String endpointUrl = request.get("url");
            boolean success = endpointService.testEndpointConnection(endpointUrl);
            
            Map<String, Object> response = Map.of(
                "success", success,
                "message", success ? "Endpoint connection test successful" : "Endpoint connection test failed"
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Error testing endpoint: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
