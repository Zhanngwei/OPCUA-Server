package org.go.show.multiproto.opcuaserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OPC UA 用户模型
 */
public class User {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("username")
    private String username;
    
    @JsonProperty("password")
    private String password;
    
    @JsonProperty("role")
    private String role; // admin, user, readonly
    
    @JsonProperty("enabled")
    private boolean enabled;
    
    @JsonProperty("authenticationMethods")
    private AuthenticationMethods authenticationMethods;
    
    @JsonProperty("certificate")
    private String certificate;
    
    @JsonProperty("issuedToken")
    private String issuedToken;
    
    @JsonProperty("created")
    private String created;
    
    @JsonProperty("lastModified")
    private String lastModified;
    
    @JsonProperty("lastLogin")
    private String lastLogin;
    
    // 认证方法配置
    public static class AuthenticationMethods {
        private boolean anonymous;
        private boolean usernamePassword;
        private boolean certificate;
        private boolean issuedToken;
        
        public AuthenticationMethods() {}
        
        public AuthenticationMethods(boolean anonymous, boolean usernamePassword, 
                                   boolean certificate, boolean issuedToken) {
            this.anonymous = anonymous;
            this.usernamePassword = usernamePassword;
            this.certificate = certificate;
            this.issuedToken = issuedToken;
        }
        
        // getters and setters
        public boolean isAnonymous() { return anonymous; }
        public void setAnonymous(boolean anonymous) { this.anonymous = anonymous; }
        
        public boolean isUsernamePassword() { return usernamePassword; }
        public void setUsernamePassword(boolean usernamePassword) { this.usernamePassword = usernamePassword; }
        
        public boolean isCertificate() { return certificate; }
        public void setCertificate(boolean certificate) { this.certificate = certificate; }
        
        public boolean isIssuedToken() { return issuedToken; }
        public void setIssuedToken(boolean issuedToken) { this.issuedToken = issuedToken; }
    }
    
    // 构造函数
    public User() {}
    
    public User(Long id, String username, String role, boolean enabled) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.enabled = enabled;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public AuthenticationMethods getAuthenticationMethods() { return authenticationMethods; }
    public void setAuthenticationMethods(AuthenticationMethods authenticationMethods) { 
        this.authenticationMethods = authenticationMethods; 
    }
    
    public String getCertificate() { return certificate; }
    public void setCertificate(String certificate) { this.certificate = certificate; }
    
    public String getIssuedToken() { return issuedToken; }
    public void setIssuedToken(String issuedToken) { this.issuedToken = issuedToken; }
    
    public String getCreated() { return created; }
    public void setCreated(String created) { this.created = created; }
    
    public String getLastModified() { return lastModified; }
    public void setLastModified(String lastModified) { this.lastModified = lastModified; }
    
    public String getLastLogin() { return lastLogin; }
    public void setLastLogin(String lastLogin) { this.lastLogin = lastLogin; }
}
