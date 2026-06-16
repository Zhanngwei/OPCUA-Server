package org.go.show.multiproto.opcuaserver.controller;

import org.go.show.multiproto.opcuaserver.service.AuthMethodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证方法管理控制器
 */
@RestController
@RequestMapping("/api/auth-methods")
@CrossOrigin(origins = "*")
public class AuthMethodController {
    
    @Autowired
    private AuthMethodService authMethodService;
    
    /**
     * 获取认证方法配置
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAuthMethods() {
        try {
            Map<String, Object> authMethods = authMethodService.getAuthMethods();
            return ResponseEntity.ok(authMethods);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 更新认证方法配置
     */
    @PutMapping
    public ResponseEntity<Map<String, Object>> updateAuthMethods(@RequestBody Map<String, Object> authMethods) {
        try {
            Map<String, Object> updatedMethods = authMethodService.updateAuthMethods(authMethods);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Authentication methods updated successfully",
                "authMethods", updatedMethods
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Error updating authentication methods: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
