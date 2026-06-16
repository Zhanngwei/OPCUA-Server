package org.go.show.multiproto.opcuaserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * OPC UA 端点配置模型
 */
public class EndpointConfig {
    
    @JsonProperty("tcpEndpoint")
    private TcpEndpointConfig tcpEndpoint;
    
    @JsonProperty("httpsEndpoint")
    private HttpsEndpointConfig httpsEndpoint;
    
    @JsonProperty("securityPolicies")
    private SecurityPoliciesConfig securityPolicies;
    
    @JsonProperty("bindAddressMode")
    private String bindAddressMode;
    
    @JsonProperty("customBindAddress")
    private String customBindAddress;
    
    @JsonProperty("enableIPv6")
    private boolean enableIPv6;
    
    @JsonProperty("registerToLDS")
    private boolean registerToLDS;
    
    @JsonProperty("ldsUrl")
    private String ldsUrl;
    
    // TCP端点配置
    public static class TcpEndpointConfig {
        private boolean enabled;
        private int port;
        private String serverName;
        private SecurityModesConfig securityModes;
        
        // getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public String getServerName() { return serverName; }
        public void setServerName(String serverName) { this.serverName = serverName; }
        
        public SecurityModesConfig getSecurityModes() { return securityModes; }
        public void setSecurityModes(SecurityModesConfig securityModes) { this.securityModes = securityModes; }
    }
    
    // HTTPS端点配置
    public static class HttpsEndpointConfig {
        private boolean enabled;
        private int port;
        private String serverName;
        private SecurityModesConfig securityModes;
        private TlsPoliciesConfig tlsPolicies;
        
        // getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public String getServerName() { return serverName; }
        public void setServerName(String serverName) { this.serverName = serverName; }
        
        public SecurityModesConfig getSecurityModes() { return securityModes; }
        public void setSecurityModes(SecurityModesConfig securityModes) { this.securityModes = securityModes; }
        
        public TlsPoliciesConfig getTlsPolicies() { return tlsPolicies; }
        public void setTlsPolicies(TlsPoliciesConfig tlsPolicies) { this.tlsPolicies = tlsPolicies; }
    }
    
    // 安全模式配置
    public static class SecurityModesConfig {
        private boolean none;
        private boolean sign;
        private boolean signAndEncrypt;
        
        // getters and setters
        public boolean isNone() { return none; }
        public void setNone(boolean none) { this.none = none; }
        
        public boolean isSign() { return sign; }
        public void setSign(boolean sign) { this.sign = sign; }
        
        public boolean isSignAndEncrypt() { return signAndEncrypt; }
        public void setSignAndEncrypt(boolean signAndEncrypt) { this.signAndEncrypt = signAndEncrypt; }
    }
    
    // TLS策略配置
    public static class TlsPoliciesConfig {
        private boolean tls10;
        private boolean tls11;
        private boolean tls12;
        
        // getters and setters
        public boolean isTls10() { return tls10; }
        public void setTls10(boolean tls10) { this.tls10 = tls10; }
        
        public boolean isTls11() { return tls11; }
        public void setTls11(boolean tls11) { this.tls11 = tls11; }
        
        public boolean isTls12() { return tls12; }
        public void setTls12(boolean tls12) { this.tls12 = tls12; }
    }
    
    // 安全策略配置
    public static class SecurityPoliciesConfig {
        private boolean basic128Rsa15;
        private boolean basic256;
        private boolean basic256Sha256;
        private boolean aes128Sha256RsaOaep;
        private boolean aes256Sha256RsaPss;
        
        // getters and setters
        public boolean isBasic128Rsa15() { return basic128Rsa15; }
        public void setBasic128Rsa15(boolean basic128Rsa15) { this.basic128Rsa15 = basic128Rsa15; }
        
        public boolean isBasic256() { return basic256; }
        public void setBasic256(boolean basic256) { this.basic256 = basic256; }
        
        public boolean isBasic256Sha256() { return basic256Sha256; }
        public void setBasic256Sha256(boolean basic256Sha256) { this.basic256Sha256 = basic256Sha256; }
        
        public boolean isAes128Sha256RsaOaep() { return aes128Sha256RsaOaep; }
        public void setAes128Sha256RsaOaep(boolean aes128Sha256RsaOaep) { this.aes128Sha256RsaOaep = aes128Sha256RsaOaep; }
        
        public boolean isAes256Sha256RsaPss() { return aes256Sha256RsaPss; }
        public void setAes256Sha256RsaPss(boolean aes256Sha256RsaPss) { this.aes256Sha256RsaPss = aes256Sha256RsaPss; }
    }
    
    // 主类的getters and setters
    public TcpEndpointConfig getTcpEndpoint() { return tcpEndpoint; }
    public void setTcpEndpoint(TcpEndpointConfig tcpEndpoint) { this.tcpEndpoint = tcpEndpoint; }
    
    public HttpsEndpointConfig getHttpsEndpoint() { return httpsEndpoint; }
    public void setHttpsEndpoint(HttpsEndpointConfig httpsEndpoint) { this.httpsEndpoint = httpsEndpoint; }
    
    public SecurityPoliciesConfig getSecurityPolicies() { return securityPolicies; }
    public void setSecurityPolicies(SecurityPoliciesConfig securityPolicies) { this.securityPolicies = securityPolicies; }
    
    public String getBindAddressMode() { return bindAddressMode; }
    public void setBindAddressMode(String bindAddressMode) { this.bindAddressMode = bindAddressMode; }
    
    public String getCustomBindAddress() { return customBindAddress; }
    public void setCustomBindAddress(String customBindAddress) { this.customBindAddress = customBindAddress; }
    
    public boolean isEnableIPv6() { return enableIPv6; }
    public void setEnableIPv6(boolean enableIPv6) { this.enableIPv6 = enableIPv6; }
    
    public boolean isRegisterToLDS() { return registerToLDS; }
    public void setRegisterToLDS(boolean registerToLDS) { this.registerToLDS = registerToLDS; }
    
    public String getLdsUrl() { return ldsUrl; }
    public void setLdsUrl(String ldsUrl) { this.ldsUrl = ldsUrl; }
}
