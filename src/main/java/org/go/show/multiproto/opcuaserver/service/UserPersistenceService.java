package org.go.show.multiproto.opcuaserver.service;

import org.go.show.multiproto.opcuaserver.entity.UserEntity;
import org.go.show.multiproto.opcuaserver.model.User;
import org.go.show.multiproto.opcuaserver.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户持久化服务
 */
@Service
@Transactional
public class UserPersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(UserPersistenceService.class);

    @Autowired
    private UserRepository userRepository;

    /**
     * 保存用户
     */
    public UserEntity saveUser(User user) {
        try {
            UserEntity entity = convertToEntity(user);
            UserEntity savedEntity = userRepository.save(entity);
            logger.debug("用户已保存到数据库: {}", savedEntity.getUsername());
            return savedEntity;
        } catch (Exception e) {
            logger.error("保存用户失败: {}", user.getUsername(), e);
            throw new RuntimeException("保存用户失败", e);
        }
    }

    /**
     * 更新用户
     */
    public UserEntity updateUser(Long id, User user) {
        try {
            Optional<UserEntity> existingEntity = userRepository.findById(id);
            if (existingEntity.isPresent()) {
                UserEntity entity = existingEntity.get();
                updateEntityFromUser(entity, user);
                UserEntity savedEntity = userRepository.save(entity);
                logger.debug("用户已更新: {}", savedEntity.getUsername());
                return savedEntity;
            } else {
                throw new RuntimeException("用户不存在: " + id);
            }
        } catch (Exception e) {
            logger.error("更新用户失败: {}", id, e);
            throw new RuntimeException("更新用户失败", e);
        }
    }

    /**
     * 根据ID查找用户
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        try {
            return userRepository.findById(id).map(this::convertToModel);
        } catch (Exception e) {
            logger.error("查找用户失败: {}", id, e);
            return Optional.empty();
        }
    }

    /**
     * 根据用户名查找用户
     */
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        try {
            return userRepository.findByUsername(username).map(this::convertToModel);
        } catch (Exception e) {
            logger.error("根据用户名查找用户失败: {}", username, e);
            return Optional.empty();
        }
    }

    /**
     * 获取所有用户
     */
    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        try {
            return userRepository.findAll().stream()
                    .map(this::convertToModel)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("获取所有用户失败", e);
            return List.of();
        }
    }

    /**
     * 删除用户
     */
    public boolean deleteUser(Long id) {
        try {
            if (userRepository.existsById(id)) {
                userRepository.deleteById(id);
                logger.debug("用户已从数据库删除: {}", id);
                return true;
            } else {
                logger.warn("要删除的用户不存在: {}", id);
                return false;
            }
        } catch (Exception e) {
            logger.error("删除用户失败: {}", id, e);
            return false;
        }
    }

    /**
     * 检查用户名是否存在
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        try {
            return userRepository.existsByUsername(username);
        } catch (Exception e) {
            logger.error("检查用户名是否存在失败: {}", username, e);
            return false;
        }
    }

    /**
     * 更新最后登录时间
     */
    public void updateLastLogin(String username) {
        try {
            Optional<UserEntity> userEntity = userRepository.findByUsername(username);
            if (userEntity.isPresent()) {
                UserEntity entity = userEntity.get();
                entity.setLastLoginAt(LocalDateTime.now());
                userRepository.save(entity);
                logger.debug("更新用户最后登录时间: {}", username);
            }
        } catch (Exception e) {
            logger.error("更新最后登录时间失败: {}", username, e);
        }
    }

    /**
     * 将User模型转换为UserEntity实体
     */
    private UserEntity convertToEntity(User user) {
        UserEntity entity = new UserEntity();
        
        if (user.getId() != null) {
            entity.setId(user.getId());
        }
        
        entity.setUsername(user.getUsername());
        entity.setPassword(user.getPassword());
        entity.setRole(user.getRole());
        entity.setEnabled(user.isEnabled());
        entity.setCertificate(user.getCertificate());
        entity.setIssuedToken(user.getIssuedToken());

        // 设置认证方法
        if (user.getAuthenticationMethods() != null) {
            entity.setAuthAnonymous(user.getAuthenticationMethods().isAnonymous());
            entity.setAuthUsernamePassword(user.getAuthenticationMethods().isUsernamePassword());
            entity.setAuthCertificate(user.getAuthenticationMethods().isCertificate());
            entity.setAuthIssuedToken(user.getAuthenticationMethods().isIssuedToken());
        }

        return entity;
    }

    /**
     * 将UserEntity实体转换为User模型
     */
    private User convertToModel(UserEntity entity) {
        User user = new User();
        
        user.setId(entity.getId());
        user.setUsername(entity.getUsername());
        user.setPassword(entity.getPassword());
        user.setRole(entity.getRole());
        user.setEnabled(entity.getEnabled());
        user.setCertificate(entity.getCertificate());
        user.setIssuedToken(entity.getIssuedToken());

        // 设置认证方法
        User.AuthenticationMethods authMethods = new User.AuthenticationMethods(
            entity.getAuthAnonymous(),
            entity.getAuthUsernamePassword(),
            entity.getAuthCertificate(),
            entity.getAuthIssuedToken()
        );
        user.setAuthenticationMethods(authMethods);

        // 设置时间字段
        if (entity.getCreatedAt() != null) {
            user.setCreated(entity.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (entity.getUpdatedAt() != null) {
            user.setLastModified(entity.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (entity.getLastLoginAt() != null) {
            user.setLastLogin(entity.getLastLoginAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        return user;
    }

    /**
     * 从User模型更新UserEntity实体
     */
    private void updateEntityFromUser(UserEntity entity, User user) {
        // 只有在提供非空值时才更新必填字段
        if (user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
            entity.setUsername(user.getUsername());
        }
        if (user.getRole() != null && !user.getRole().trim().isEmpty()) {
            entity.setRole(user.getRole());
        }

        // 布尔字段直接更新（基本类型不会为null）
        entity.setEnabled(user.isEnabled());

        // 可选字段可以为空
        entity.setCertificate(user.getCertificate());
        entity.setIssuedToken(user.getIssuedToken());

        // 只有在提供新密码时才更新密码
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            entity.setPassword(user.getPassword());
        }

        // 更新认证方法
        if (user.getAuthenticationMethods() != null) {
            entity.setAuthAnonymous(user.getAuthenticationMethods().isAnonymous());
            entity.setAuthUsernamePassword(user.getAuthenticationMethods().isUsernamePassword());
            entity.setAuthCertificate(user.getAuthenticationMethods().isCertificate());
            entity.setAuthIssuedToken(user.getAuthenticationMethods().isIssuedToken());
        }
    }
}
