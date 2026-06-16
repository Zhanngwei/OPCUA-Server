package org.go.show.multiproto.opcuaserver.service;

import org.go.show.multiproto.opcuaserver.model.EndpointConfig;
import org.go.show.multiproto.opcuaserver.model.EndpointInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 端点管理服务
 */
@Service
public class EndpointService {
    
    private static final Logger logger = LoggerFactory.getLogger(EndpointService.class);
    private static final String CONFIG_FILE = "endpoint-config.json";
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 当前端点配置
    private EndpointConfig currentConfig;
    
    /**
     * 获取当前活跃的端点列表
     */
    public List<EndpointInfo> getActiveEndpoints() {
        List<EndpointInfo> endpoints = new ArrayList<>();
        
        // 模拟当前活跃的端点
        endpoints.add(new EndpointInfo(1L, "opc.tcp://localhost:12686/milo", "TCP", 
                                     "None", "None", "Active", 2));
        endpoints.add(new EndpointInfo(2L, "opc.tcp://localhost:12686", "TCP", 
                                     "None", "None", "Active", 1));
        
        return endpoints;
    }
    
    /**
     * 获取端点配置
     */
    public EndpointConfig getEndpointConfig() {
        if (currentConfig == null) {
            currentConfig = loadConfigFromFile();
        }
        return currentConfig;
    }
    
    /**
     * 更新端点配置
     */
    public boolean updateEndpointConfig(EndpointConfig config) {
        try {
            this.currentConfig = config;
            saveConfigToFile(config);
            logger.info("Endpoint configuration updated successfully");
            return true;
        } catch (Exception e) {
            logger.error("Failed to update endpoint configuration", e);
            return false;
        }
    }
    
    /**
     * 应用端点配置
     */
    public boolean applyEndpointConfig() {
        try {
            // 这里应该重启OPC UA服务器以应用新配置
            logger.info("Applying endpoint configuration...");
            
            // 模拟应用配置的过程
            Thread.sleep(1000);
            
            logger.info("Endpoint configuration applied successfully");
            return true;
        } catch (Exception e) {
            logger.error("Failed to apply endpoint configuration", e);
            return false;
        }
    }
    
    /**
     * 获取端点统计信息
     */
    public Map<String, Object> getEndpointStats() {
        Map<String, Object> stats = new HashMap<>();
        
        List<EndpointInfo> activeEndpoints = getActiveEndpoints();
        int totalConnections = activeEndpoints.stream()
                .mapToInt(EndpointInfo::getConnections)
                .sum();
        
        stats.put("totalEndpoints", activeEndpoints.size());
        stats.put("activeEndpoints", activeEndpoints.size());
        stats.put("totalConnections", totalConnections);
        stats.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return stats;
    }
    
    /**
     * 测试端点连接
     */
    public boolean testEndpointConnection(String endpointUrl) {
        try {
            logger.info("Testing endpoint connection: {}", endpointUrl);
            
            // 模拟连接测试
            Thread.sleep(500);
            
            // 简单的URL验证
            if (endpointUrl != null && endpointUrl.startsWith("opc.tcp://")) {
                logger.info("Endpoint connection test successful: {}", endpointUrl);
                return true;
            } else {
                logger.warn("Invalid endpoint URL: {}", endpointUrl);
                return false;
            }
        } catch (Exception e) {
            logger.error("Endpoint connection test failed: {}", endpointUrl, e);
            return false;
        }
    }
    
    /**
     * 从文件加载配置
     */
    private EndpointConfig loadConfigFromFile() {
        try {
            File configFile = new File(CONFIG_FILE);
            if (configFile.exists()) {
                return objectMapper.readValue(configFile, EndpointConfig.class);
            }
        } catch (IOException e) {
            logger.error("Failed to load endpoint configuration from file", e);
        }
        
        // 返回默认配置
        return createDefaultConfig();
    }
    
    /**
     * 保存配置到文件
     */
    private void saveConfigToFile(EndpointConfig config) throws IOException {
        File configFile = new File(CONFIG_FILE);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(configFile, config);
    }
    
    /**
     * 创建默认配置
     */
    private EndpointConfig createDefaultConfig() {
        EndpointConfig config = new EndpointConfig();
        
        // TCP端点配置
        EndpointConfig.TcpEndpointConfig tcpConfig = new EndpointConfig.TcpEndpointConfig();
        tcpConfig.setEnabled(true);
        tcpConfig.setPort(4840);
        tcpConfig.setServerName("milo");
        
        EndpointConfig.SecurityModesConfig tcpSecurityModes = new EndpointConfig.SecurityModesConfig();
        tcpSecurityModes.setNone(true);
        tcpSecurityModes.setSign(false);
        tcpSecurityModes.setSignAndEncrypt(false);
        tcpConfig.setSecurityModes(tcpSecurityModes);
        
        config.setTcpEndpoint(tcpConfig);
        
        // HTTPS端点配置
        EndpointConfig.HttpsEndpointConfig httpsConfig = new EndpointConfig.HttpsEndpointConfig();
        httpsConfig.setEnabled(false);
        httpsConfig.setPort(8443);
        httpsConfig.setServerName("milo");
        
        EndpointConfig.SecurityModesConfig httpsSecurityModes = new EndpointConfig.SecurityModesConfig();
        httpsSecurityModes.setNone(true);
        httpsSecurityModes.setSign(false);
        httpsConfig.setSecurityModes(httpsSecurityModes);
        
        EndpointConfig.TlsPoliciesConfig tlsPolicies = new EndpointConfig.TlsPoliciesConfig();
        tlsPolicies.setTls10(false);
        tlsPolicies.setTls11(false);
        tlsPolicies.setTls12(true);
        httpsConfig.setTlsPolicies(tlsPolicies);
        
        config.setHttpsEndpoint(httpsConfig);
        
        // 安全策略配置
        EndpointConfig.SecurityPoliciesConfig securityPolicies = new EndpointConfig.SecurityPoliciesConfig();
        securityPolicies.setBasic128Rsa15(false);
        securityPolicies.setBasic256(false);
        securityPolicies.setBasic256Sha256(true);
        securityPolicies.setAes128Sha256RsaOaep(false);
        securityPolicies.setAes256Sha256RsaPss(false);
        config.setSecurityPolicies(securityPolicies);
        
        // 其他配置
        config.setBindAddressMode("all");
        config.setCustomBindAddress("");
        config.setEnableIPv6(false);
        config.setRegisterToLDS(false);
        config.setLdsUrl("opc.tcp://localhost:4840");
        
        return config;
    }
}
