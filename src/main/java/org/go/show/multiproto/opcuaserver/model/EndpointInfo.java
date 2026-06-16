package org.go.show.multiproto.opcuaserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 端点信息模型
 */
public class EndpointInfo {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("url")
    private String url;
    
    @JsonProperty("type")
    private String type; // TCP, HTTPS
    
    @JsonProperty("securityMode")
    private String securityMode;
    
    @JsonProperty("securityPolicy")
    private String securityPolicy;
    
    @JsonProperty("status")
    private String status; // Active, Inactive
    
    @JsonProperty("connections")
    private int connections;
    
    @JsonProperty("certificate")
    private String certificate;
    
    @JsonProperty("privateKey")
    private String privateKey;
    
    @JsonProperty("created")
    private String created;
    
    @JsonProperty("lastModified")
    private String lastModified;
    
    // 构造函数
    public EndpointInfo() {}
    
    public EndpointInfo(Long id, String url, String type, String securityMode, 
                       String securityPolicy, String status, int connections) {
        this.id = id;
        this.url = url;
        this.type = type;
        this.securityMode = securityMode;
        this.securityPolicy = securityPolicy;
        this.status = status;
        this.connections = connections;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getSecurityMode() { return securityMode; }
    public void setSecurityMode(String securityMode) { this.securityMode = securityMode; }
    
    public String getSecurityPolicy() { return securityPolicy; }
    public void setSecurityPolicy(String securityPolicy) { this.securityPolicy = securityPolicy; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public int getConnections() { return connections; }
    public void setConnections(int connections) { this.connections = connections; }
    
    public String getCertificate() { return certificate; }
    public void setCertificate(String certificate) { this.certificate = certificate; }
    
    public String getPrivateKey() { return privateKey; }
    public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }
    
    public String getCreated() { return created; }
    public void setCreated(String created) { this.created = created; }
    
    public String getLastModified() { return lastModified; }
    public void setLastModified(String lastModified) { this.lastModified = lastModified; }
}
