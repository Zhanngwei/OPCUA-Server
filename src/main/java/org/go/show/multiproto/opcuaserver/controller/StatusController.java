package org.go.show.multiproto.opcuaserver.controller;

import org.go.show.multiproto.opcuaserver.service.StatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 服务器状态控制器
 */
@RestController
@RequestMapping("/api/status")
@CrossOrigin(origins = "*")
public class StatusController {
    
    @Autowired
    private StatusService statusService;
    
    /**
     * 获取服务器状态信息
     */
    @GetMapping("/server")
    public ResponseEntity<Map<String, Object>> getServerStatus() {
        try {
            Map<String, Object> status = statusService.getServerStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取连接统计信息
     */
    @GetMapping("/connections")
    public ResponseEntity<Map<String, Object>> getConnectionStats() {
        try {
            Map<String, Object> stats = statusService.getConnectionStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取节点统计信息
     */
    @GetMapping("/nodes")
    public ResponseEntity<Map<String, Object>> getNodeStats() {
        try {
            Map<String, Object> stats = statusService.getNodeStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取性能指标
     */
    @GetMapping("/performance")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        try {
            Map<String, Object> metrics = statusService.getPerformanceMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取活动日志
     */
    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getActivityLogs(
            @RequestParam(defaultValue = "50") int limit) {
        try {
            Map<String, Object> logs = statusService.getActivityLogs(limit);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取完整状态信息（包含所有统计数据）
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllStatus() {
        try {
            Map<String, Object> allStatus = statusService.getAllStatus();
            return ResponseEntity.ok(allStatus);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取所有端点URL
     */
    @GetMapping("/endpoints")
    public ResponseEntity<Map<String, Object>> getEndpoints() {
        try {
            Map<String, Object> endpoints = statusService.getEndpointsInfo();
            return ResponseEntity.ok(endpoints);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
