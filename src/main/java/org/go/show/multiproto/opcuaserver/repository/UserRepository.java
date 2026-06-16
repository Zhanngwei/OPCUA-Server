package org.go.show.multiproto.opcuaserver.repository;

import org.go.show.multiproto.opcuaserver.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问接口
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    /**
     * 根据用户名查找用户
     */
    Optional<UserEntity> findByUsername(String username);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 根据角色查找用户
     */
    List<UserEntity> findByRoleOrderByUsernameAsc(String role);

    /**
     * 查找所有启用的用户
     */
    List<UserEntity> findByEnabledTrueOrderByUsernameAsc();

    /**
     * 查找所有禁用的用户
     */
    List<UserEntity> findByEnabledFalseOrderByUsernameAsc();

    /**
     * 根据角色和启用状态查找用户
     */
    List<UserEntity> findByRoleAndEnabledOrderByUsernameAsc(String role, Boolean enabled);

    /**
     * 查找最近登录的用户
     */
    @Query("SELECT u FROM UserEntity u WHERE u.lastLoginAt IS NOT NULL ORDER BY u.lastLoginAt DESC")
    List<UserEntity> findRecentlyLoggedInUsers();

    /**
     * 查找在指定时间之后登录的用户
     */
    @Query("SELECT u FROM UserEntity u WHERE u.lastLoginAt > :since ORDER BY u.lastLoginAt DESC")
    List<UserEntity> findUsersLoggedInSince(@Param("since") LocalDateTime since);

    /**
     * 统计各角色的用户数量
     */
    @Query("SELECT u.role, COUNT(u) FROM UserEntity u GROUP BY u.role")
    List<Object[]> countUsersByRole();

    /**
     * 统计启用和禁用的用户数量
     */
    @Query("SELECT u.enabled, COUNT(u) FROM UserEntity u GROUP BY u.enabled")
    List<Object[]> countUsersByEnabledStatus();

    /**
     * 查找支持特定认证方法的用户
     */
    @Query("SELECT u FROM UserEntity u WHERE " +
           "(:anonymous = true AND u.authAnonymous = true) OR " +
           "(:usernamePassword = true AND u.authUsernamePassword = true) OR " +
           "(:certificate = true AND u.authCertificate = true) OR " +
           "(:issuedToken = true AND u.authIssuedToken = true)")
    List<UserEntity> findUsersByAuthMethods(
        @Param("anonymous") Boolean anonymous,
        @Param("usernamePassword") Boolean usernamePassword,
        @Param("certificate") Boolean certificate,
        @Param("issuedToken") Boolean issuedToken
    );

    /**
     * 查找有证书的用户
     */
    @Query("SELECT u FROM UserEntity u WHERE u.certificate IS NOT NULL AND u.certificate != ''")
    List<UserEntity> findUsersWithCertificates();

    /**
     * 查找有颁发令牌的用户
     */
    @Query("SELECT u FROM UserEntity u WHERE u.issuedToken IS NOT NULL AND u.issuedToken != ''")
    List<UserEntity> findUsersWithIssuedTokens();
}
