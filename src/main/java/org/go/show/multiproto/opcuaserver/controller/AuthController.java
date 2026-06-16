package org.go.show.multiproto.opcuaserver.controller;

import org.go.show.multiproto.opcuaserver.service.UserService;
import org.go.show.multiproto.opcuaserver.config.WebSecurityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("Login attempt for user: {}", request.getUsername());
            
            // 验证用户名和密码
            boolean isValid = userService.validateUser(request.getUsername(), request.getPassword());
            
            if (isValid) {
                // 生成token
                String token = UUID.randomUUID().toString();
                WebSecurityConfig.putToken(token, request.getUsername());
                
                response.put("success", true);
                response.put("token", token);
                response.put("message", "登录成功");
                
                logger.info("User {} logged in successfully", request.getUsername());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "用户名或密码错误");
                
                logger.warn("Failed login attempt for user: {}", request.getUsername());
                return ResponseEntity.status(401).body(response);
            }
        } catch (Exception e) {
            logger.error("Login error for user {}: {}", request.getUsername(), e.getMessage());
            response.put("success", false);
            response.put("message", "登录过程中发生错误");
            return ResponseEntity.status(500).body(response);
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String username = WebSecurityConfig.removeToken(token);
            
            if (username != null) {
                logger.info("User {} logged out", username);
            }
        }
        
        response.put("success", true);
        response.put("message", "退出成功");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String username = WebSecurityConfig.getUsername(token);
            
            if (username != null) {
                response.put("valid", true);
                response.put("username", username);
                return ResponseEntity.ok(response);
            }
        }
        
        response.put("valid", false);
        return ResponseEntity.status(401).body(response);
    }
    
    // 内部类用于接收登录请求
    public static class LoginRequest {
        private String username;
        private String password;
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
    }
}
