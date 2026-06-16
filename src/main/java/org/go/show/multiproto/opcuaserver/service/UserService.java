package org.go.show.multiproto.opcuaserver.service;

import org.go.show.multiproto.opcuaserver.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户管理服务
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserPersistenceService userPersistenceService;

    /**
     * 初始化方法，创建示例用户（如果数据库为空）
     */
    @PostConstruct
    public void init() {
        try {
            List<User> existingUsers = userPersistenceService.findAllUsers();
            if (existingUsers.isEmpty()) {
                logger.info("数据库中没有用户，创建示例用户");
                initializeSampleUsers();
            } else {
                logger.info("数据库中已有 {} 个用户", existingUsers.size());
            }
        } catch (Exception e) {
            logger.error("初始化用户服务失败", e);
        }
    }
    
    /**
     * 验证用户名和密码
     */
    public boolean validateUser(String username, String password) {
        try {
            Optional<User> userOpt = userPersistenceService.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                // 简单的密码验证（生产环境应使用加密）
                return password.equals(user.getPassword());
            }
            return false;
        } catch (Exception e) {
            logger.error("验证用户失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取所有用户
     */
    public List<User> getAllUsers() {
        return userPersistenceService.findAllUsers();
    }

    /**
     * 根据ID获取用户
     */
    public User getUserById(Long id) {
        Optional<User> user = userPersistenceService.findById(id);
        return user.orElse(null);
    }

    /**
     * 根据用户名获取用户
     */
    public User getUserByUsername(String username) {
        Optional<User> user = userPersistenceService.findByUsername(username);
        return user.orElse(null);
    }
    
    /**
     * 创建新用户
     */
    public User createUser(User user) {
        // 检查用户名是否已存在
        if (userPersistenceService.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // 设置默认认证方法
        if (user.getAuthenticationMethods() == null) {
            user.setAuthenticationMethods(new User.AuthenticationMethods(false, true, false, false));
        }

        var savedEntity = userPersistenceService.saveUser(user);
        User savedUser = userPersistenceService.findById(savedEntity.getId()).orElse(user);
        logger.info("User created: {}", savedUser.getUsername());
        return savedUser;
    }
    
    /**
     * 更新用户
     */
    public User updateUser(Long id, User updatedUser) {
        try {
            var savedEntity = userPersistenceService.updateUser(id, updatedUser);
            User savedUser = userPersistenceService.findById(savedEntity.getId()).orElse(null);
            if (savedUser != null) {
                logger.info("User updated: {}", savedUser.getUsername());
            }
            return savedUser;
        } catch (Exception e) {
            logger.error("Failed to update user: {}", id, e);
            return null;
        }
    }
    
    /**
     * 删除用户
     */
    public boolean deleteUser(Long id) {
        User user = getUserById(id);
        if (user != null) {
            boolean deleted = userPersistenceService.deleteUser(id);
            if (deleted) {
                logger.info("User deleted: {}", user.getUsername());
            }
            return deleted;
        }
        return false;
    }
    
    /**
     * 启用/禁用用户
     */
    public boolean toggleUserStatus(Long id) {
        User user = getUserById(id);
        if (user != null) {
            user.setEnabled(!user.isEnabled());
            try {
                userPersistenceService.updateUser(id, user);
                logger.info("User {} status changed to: {}", user.getUsername(), user.isEnabled() ? "enabled" : "disabled");
                return true;
            } catch (Exception e) {
                logger.error("Failed to toggle user status: {}", id, e);
                return false;
            }
        }
        return false;
    }
    
    /**
     * 重置用户密码
     */
    public boolean resetPassword(Long id, String newPassword) {
        User user = getUserById(id);
        if (user != null) {
            user.setPassword(newPassword);
            try {
                userPersistenceService.updateUser(id, user);
                logger.info("Password reset for user: {}", user.getUsername());
                return true;
            } catch (Exception e) {
                logger.error("Failed to reset password for user: {}", id, e);
                return false;
            }
        }
        return false;
    }
    
    /**
     * 获取用户统计信息
     */
    public Map<String, Object> getUserStats() {
        Map<String, Object> stats = new HashMap<>();

        List<User> allUsers = getAllUsers();
        long totalUsers = allUsers.size();
        long enabledUsers = allUsers.stream().filter(User::isEnabled).count();
        long disabledUsers = totalUsers - enabledUsers;
        long adminUsers = allUsers.stream().filter(user -> "admin".equals(user.getRole())).count();

        stats.put("totalUsers", totalUsers);
        stats.put("enabledUsers", enabledUsers);
        stats.put("disabledUsers", disabledUsers);
        stats.put("adminUsers", adminUsers);
        stats.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return stats;
    }
    
    /**
     * 验证用户凭据
     */
    public boolean validateCredentials(String username, String password) {
        User user = getUserByUsername(username);
        if (user != null && user.isEnabled()) {
            boolean valid = password.equals(user.getPassword());
            if (valid) {
                userPersistenceService.updateLastLogin(username);
            }
            return valid;
        }
        return false;
    }
    
    /**
     * 初始化示例用户
     */
    private void initializeSampleUsers() {
        try {
            // 管理员用户
            User admin = new User(null, "admin", "admin", true);
            admin.setPassword("admin123");
            admin.setAuthenticationMethods(new User.AuthenticationMethods(false, true, false, false));
            userPersistenceService.saveUser(admin);

            // 普通用户
            User user1 = new User(null, "operator", "user", true);
            user1.setPassword("operator123");
            user1.setAuthenticationMethods(new User.AuthenticationMethods(false, true, false, false));
            userPersistenceService.saveUser(user1);

            // 只读用户
            User user2 = new User(null, "viewer", "readonly", true);
            user2.setPassword("viewer123");
            user2.setAuthenticationMethods(new User.AuthenticationMethods(false, true, false, false));
            userPersistenceService.saveUser(user2);

            // 禁用的用户
            User user3 = new User(null, "guest", "readonly", false);
            user3.setPassword("guest123");
            user3.setAuthenticationMethods(new User.AuthenticationMethods(true, false, false, false));
            userPersistenceService.saveUser(user3);

            logger.info("Initialized 4 sample users");
        } catch (Exception e) {
            logger.error("Failed to initialize sample users", e);
        }
    }
}
