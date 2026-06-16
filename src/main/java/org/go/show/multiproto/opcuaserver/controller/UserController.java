package org.go.show.multiproto.opcuaserver.controller;

import org.go.show.multiproto.opcuaserver.model.User;
import org.go.show.multiproto.opcuaserver.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    /**
     * 获取所有用户
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            // 不返回密码信息
            users.forEach(user -> user.setPassword(null));
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        try {
            User user = userService.getUserById(id);
            if (user != null) {
                // 不返回密码信息
                user.setPassword(null);
                return ResponseEntity.ok(user);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 创建新用户
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody User user) {
        try {
            User createdUser = userService.createUser(user);
            createdUser.setPassword(null); // 不返回密码
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "User created successfully",
                "user", createdUser
            );
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Error creating user: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable Long id, @RequestBody User user) {
        try {
            User updatedUser = userService.updateUser(id, user);
            if (updatedUser != null) {
                updatedUser.setPassword(null); // 不返回密码
                
                Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "User updated successfully",
                    "user", updatedUser
                );
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "User not found"
                );
                return ResponseEntity.notFound().build();
            }
        } catch (RuntimeException e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Error updating user: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        try {
            boolean success = userService.deleteUser(id);
            
            Map<String, Object> response = Map.of(
                "success", success,
                "message", success ? "User deleted successfully" : "User not found"
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Error deleting user: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 启用/禁用用户
     */
    @PostMapping("/{id}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleUserStatus(@PathVariable Long id) {
        try {
            boolean success = userService.toggleUserStatus(id);
            
            Map<String, Object> response = Map.of(
                "success", success,
                "message", success ? "User status updated successfully" : "User not found"
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Error updating user status: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 重置用户密码
     */
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String newPassword = request.get("password");
            if (newPassword == null || newPassword.isEmpty()) {
                Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Password is required"
                );
                return ResponseEntity.badRequest().body(response);
            }
            
            boolean success = userService.resetPassword(id, newPassword);
            
            Map<String, Object> response = Map.of(
                "success", success,
                "message", success ? "Password reset successfully" : "User not found"
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Error resetting password: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取用户统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        try {
            Map<String, Object> stats = userService.getUserStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 验证用户凭据
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateCredentials(@RequestBody Map<String, String> credentials) {
        try {
            String username = credentials.get("username");
            String password = credentials.get("password");
            
            boolean valid = userService.validateCredentials(username, password);
            
            Map<String, Object> response = Map.of(
                "success", valid,
                "message", valid ? "Credentials valid" : "Invalid credentials"
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Error validating credentials: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
