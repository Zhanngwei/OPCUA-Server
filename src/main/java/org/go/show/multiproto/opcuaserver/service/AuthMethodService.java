package org.go.show.multiproto.opcuaserver.service;

import org.go.show.multiproto.opcuaserver.entity.AuthMethodEntity;
import org.go.show.multiproto.opcuaserver.repository.AuthMethodRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证方法管理服务
 */
@Service
public class AuthMethodService {

    private static final Logger logger = LoggerFactory.getLogger(AuthMethodService.class);

    @Autowired
    private AuthMethodRepository authMethodRepository;

    // 全局认证方法配置缓存
    private Map<String, Object> globalAuthMethods = new HashMap<>();
    
    /**
     * 初始化认证方法配置
     */
    @PostConstruct
    public void initializeAuthMethods() {
        try {
            // 从数据库加载配置
            loadAuthMethodsFromDatabase();

            // 如果数据库中没有认证方法配置，则创建默认配置
            List<AuthMethodEntity> entities = authMethodRepository.findAll();
            if (entities.isEmpty()) {
                createDefaultAuthMethods();
            }

            logger.info("Initialized authentication methods from database");
        } catch (Exception e) {
            logger.error("Failed to initialize authentication methods", e);
            // 如果数据库操作失败，使用默认配置
            createDefaultAuthMethods();
        }
    }

    /**
     * 从数据库加载认证方法配置
     */
    private void loadAuthMethodsFromDatabase() {
        List<AuthMethodEntity> entities = authMethodRepository.findAll();
        globalAuthMethods.clear();

        for (AuthMethodEntity entity : entities) {
            globalAuthMethods.put(entity.getMethodName(), entity.getEnabled());
        }

        // 不要将lastUpdated作为认证方法存储
        logger.info("Loaded {} authentication methods from database", entities.size());
    }

    /**
     * 创建默认认证方法配置
     */
    private void createDefaultAuthMethods() {
        String[] methods = {"anonymous", "usernamePassword", "certificate", "issuedToken"};
        boolean[] defaultValues = {false, true, false, false}; // 默认只启用用户名密码认证

        for (int i = 0; i < methods.length; i++) {
            String methodName = methods[i];
            boolean enabled = defaultValues[i];

            if (!authMethodRepository.existsByMethodName(methodName)) {
                AuthMethodEntity entity = new AuthMethodEntity(methodName, enabled);
                authMethodRepository.save(entity);
                logger.info("Created default auth method: {} = {}", methodName, enabled);
            }

            globalAuthMethods.put(methodName, enabled);
        }

        // 不要将lastUpdated作为认证方法存储
        logger.info("Created default authentication methods");
    }
    
    /**
     * 获取认证方法配置
     */
    public Map<String, Object> getAuthMethods() {
        Map<String, Object> result = new HashMap<>();

        // 只返回真正的认证方法，不包括lastUpdated等元数据
        String[] methods = {"anonymous", "usernamePassword", "certificate", "issuedToken"};
        for (String method : methods) {
            result.put(method, globalAuthMethods.getOrDefault(method, false));
        }

        // 添加最后更新时间
        result.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return result;
    }

    /**
     * 检查特定认证方法是否启用
     */
    public boolean isAuthMethodEnabled(String methodName) {
        Boolean enabled = (Boolean) globalAuthMethods.get(methodName);
        return enabled != null && enabled;
    }
    
    /**
     * 更新认证方法配置
     */
    public Map<String, Object> updateAuthMethods(Map<String, Object> newAuthMethods) {
        try {
            // 验证输入参数
            if (newAuthMethods == null) {
                throw new IllegalArgumentException("Authentication methods cannot be null");
            }

            // 更新数据库和缓存
            String[] methods = {"anonymous", "usernamePassword", "certificate", "issuedToken"};

            for (String methodName : methods) {
                if (newAuthMethods.containsKey(methodName)) {
                    Boolean enabled = (Boolean) newAuthMethods.get(methodName);
                    updateAuthMethodInDatabase(methodName, enabled);
                    globalAuthMethods.put(methodName, enabled);
                }
            }

            // 更新时间戳
            globalAuthMethods.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            logger.info("Authentication methods updated: {}", globalAuthMethods);

            return new HashMap<>(globalAuthMethods);

        } catch (Exception e) {
            logger.error("Error updating authentication methods", e);
            throw new RuntimeException("Failed to update authentication methods: " + e.getMessage());
        }
    }

    /**
     * 更新数据库中的认证方法配置
     */
    private void updateAuthMethodInDatabase(String methodName, Boolean enabled) {
        try {
            AuthMethodEntity entity = authMethodRepository.findByMethodName(methodName)
                    .orElse(new AuthMethodEntity(methodName, enabled));

            entity.setEnabled(enabled);
            authMethodRepository.save(entity);

            logger.debug("Updated auth method in database: {} = {}", methodName, enabled);
        } catch (Exception e) {
            logger.error("Failed to update auth method in database: {}", methodName, e);
            throw e;
        }
    }
    
    /**
     * 获取认证方法统计信息
     */
    public Map<String, Object> getAuthMethodStats() {
        Map<String, Object> stats = new HashMap<>();
        
        int enabledCount = 0;
        if (isAuthMethodEnabled("anonymous")) enabledCount++;
        if (isAuthMethodEnabled("usernamePassword")) enabledCount++;
        if (isAuthMethodEnabled("certificate")) enabledCount++;
        if (isAuthMethodEnabled("issuedToken")) enabledCount++;
        
        stats.put("totalMethods", 4);
        stats.put("enabledMethods", enabledCount);
        stats.put("disabledMethods", 4 - enabledCount);
        stats.put("lastUpdated", globalAuthMethods.get("lastUpdated"));
        
        return stats;
    }
}
