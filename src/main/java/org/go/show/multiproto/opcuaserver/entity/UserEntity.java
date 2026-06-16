package org.go.show.multiproto.opcuaserver.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_username", columnList = "username", unique = true)
})
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户名，唯一标识符
     */
    @Column(name = "username", length = 50, nullable = false, unique = true)
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    private String username;

    /**
     * 密码
     */
    @Column(name = "password", length = 255, nullable = false)
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 角色：admin, user, readonly
     */
    @Column(name = "role", length = 20, nullable = false)
    @NotBlank(message = "角色不能为空")
    private String role;

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    @NotNull
    private Boolean enabled = true;

    /**
     * 认证方法 - 匿名访问
     */
    @Column(name = "auth_anonymous", nullable = false)
    private Boolean authAnonymous = false;

    /**
     * 认证方法 - 用户名密码
     */
    @Column(name = "auth_username_password", nullable = false)
    private Boolean authUsernamePassword = true;

    /**
     * 认证方法 - 证书
     */
    @Column(name = "auth_certificate", nullable = false)
    private Boolean authCertificate = false;

    /**
     * 认证方法 - 颁发的令牌
     */
    @Column(name = "auth_issued_token", nullable = false)
    private Boolean authIssuedToken = false;

    /**
     * 证书内容
     */
    @Column(name = "certificate", columnDefinition = "TEXT")
    private String certificate;

    /**
     * 颁发的令牌
     */
    @Column(name = "issued_token", columnDefinition = "TEXT")
    private String issuedToken;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    @NotNull
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    @NotNull
    private LocalDateTime updatedAt;

    /**
     * 最后登录时间
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // 构造函数
    public UserEntity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public UserEntity(String username, String password, String role) {
        this();
        this.username = username;
        this.password = password;
        this.role = role;
    }

    // JPA生命周期回调
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getter和Setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getAuthAnonymous() {
        return authAnonymous;
    }

    public void setAuthAnonymous(Boolean authAnonymous) {
        this.authAnonymous = authAnonymous;
    }

    public Boolean getAuthUsernamePassword() {
        return authUsernamePassword;
    }

    public void setAuthUsernamePassword(Boolean authUsernamePassword) {
        this.authUsernamePassword = authUsernamePassword;
    }

    public Boolean getAuthCertificate() {
        return authCertificate;
    }

    public void setAuthCertificate(Boolean authCertificate) {
        this.authCertificate = authCertificate;
    }

    public Boolean getAuthIssuedToken() {
        return authIssuedToken;
    }

    public void setAuthIssuedToken(Boolean authIssuedToken) {
        this.authIssuedToken = authIssuedToken;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getIssuedToken() {
        return issuedToken;
    }

    public void setIssuedToken(String issuedToken) {
        this.issuedToken = issuedToken;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    @Override
    public String toString() {
        return "UserEntity{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", enabled=" + enabled +
                ", createdAt=" + createdAt +
                '}';
    }
}
