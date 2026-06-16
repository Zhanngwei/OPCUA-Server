package org.go.show.multiproto.opcuaserver.server;

import org.go.show.multiproto.opcuaserver.SimpleOpcUaServer;
import org.go.show.multiproto.opcuaserver.controller.NodeController;
import org.go.show.multiproto.opcuaserver.controller.PublicApiController;
import org.go.show.multiproto.opcuaserver.SimpleNamespace;
import org.go.show.multiproto.opcuaserver.identity.DynamicIdentityValidator;
import org.go.show.multiproto.opcuaserver.service.AuthMethodService;
import org.go.show.multiproto.opcuaserver.service.NodePersistenceService;
import org.go.show.multiproto.opcuaserver.service.CertificateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.ArrayList;

/**
 * OPC UA服务器管理器
 */
@Component
public class OpcUaServerManager {
    
    private static final Logger logger = LoggerFactory.getLogger(OpcUaServerManager.class);

    private SimpleOpcUaServer opcUaServer;
    private boolean serverRunning = false;
    private LocalDateTime startTime;

    @Autowired
    private NodePersistenceService persistenceService;

    @Autowired
    private DynamicIdentityValidator identityValidator;

    @Autowired
    private AuthMethodService authMethodService;

    @Autowired
    private CertificateService certificateService;

    // 默认配置
    private static final int DEFAULT_PORT = 4840;
    private static final String DEFAULT_HOSTNAME = "localhost";
    private static final String DEFAULT_PATH = "";
    
    public void init() {
        try {
            startServer();
        } catch (Exception e) {
            logger.error("Failed to start OPC UA server during initialization", e);
        }
    }

    public void cleanup() {
        try {
            stopServer();
        } catch (Exception e) {
            logger.error("Failed to stop OPC UA server during cleanup", e);
        }
    }
    
    /**
     * 启动服务器
     */
    public synchronized CompletableFuture<Boolean> startServer() {
        if (serverRunning) {
            logger.warn("OPC UA server is already running");
            return CompletableFuture.completedFuture(true);
        }
        
        try {
            logger.info("Starting OPC UA server...");
            opcUaServer = new SimpleOpcUaServer(identityValidator, authMethodService, certificateService);
            
            return opcUaServer.startup().thenApply(server -> {
                serverRunning = true;
                startTime = LocalDateTime.now();
                logger.info("OPC UA server started successfully");

                // 记录端点信息
                server.getConfig().getEndpoints().forEach(endpoint -> {
                    logger.info("Server endpoint: {}", endpoint.getEndpointUrl());
                });

                // 设置动态节点管理器
                SimpleNamespace namespace = opcUaServer.getSimpleNamespace();
                if (namespace != null) {
                    // 设置持久化服务
                    namespace.getDynamicNodeManager().setPersistenceService(persistenceService);
                    logger.info("Persistence service set for dynamic node manager");

                    NodeController.setDynamicNodeManager(namespace.getDynamicNodeManager());

                    // 设置PublicApiController的DynamicNodeManager
                    PublicApiController.setDynamicNodeManager(namespace.getDynamicNodeManager());

                    logger.info("Dynamic node manager initialized");
                }

                // StatusService会通过自己的延迟机制注册SessionListener

                return true;
            }).exceptionally(throwable -> {
                logger.error("Failed to start OPC UA server", throwable);
                serverRunning = false;
                return false;
            });
            
        } catch (Exception e) {
            logger.error("Error creating OPC UA server", e);
            serverRunning = false;
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * 停止服务器
     */
    public synchronized CompletableFuture<Boolean> stopServer() {
        if (!serverRunning || opcUaServer == null) {
            logger.warn("OPC UA server is not running");
            return CompletableFuture.completedFuture(true);
        }
        
        try {
            logger.info("Stopping OPC UA server...");
            
            return opcUaServer.shutdown().thenApply(server -> {
                serverRunning = false;
                startTime = null;
                logger.info("OPC UA server stopped successfully");
                return true;
            }).exceptionally(throwable -> {
                logger.error("Failed to stop OPC UA server", throwable);
                return false;
            });
            
        } catch (Exception e) {
            logger.error("Error stopping OPC UA server", e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * 重启服务器
     */
    public CompletableFuture<Boolean> restartServer() {
        logger.info("Restarting OPC UA server...");
        
        return stopServer().thenCompose(stopped -> {
            if (stopped) {
                // 等待一小段时间确保完全停止
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return startServer();
            } else {
                return CompletableFuture.completedFuture(false);
            }
        });
    }
    
    /**
     * 检查服务器是否运行
     */
    public boolean isServerRunning() {
        return serverRunning && opcUaServer != null;
    }
    
    /**
     * 获取服务器端口
     */
    public int getServerPort() {
        return DEFAULT_PORT;
    }
    
    /**
     * 获取端点URL（返回主要端点）
     */
    public String getEndpointUrl() {
        if (opcUaServer != null && opcUaServer.getServer() != null) {
            var endpoints = opcUaServer.getServer().getConfig().getEndpoints();
            if (!endpoints.isEmpty()) {
                // 返回第一个非discovery端点
                for (var endpoint : endpoints) {
                    String url = endpoint.getEndpointUrl();
                    if (!url.contains("/discovery")) {
                        return url;
                    }
                }
                // 如果没有找到非discovery端点，返回第一个
                return endpoints.iterator().next().getEndpointUrl();
            }
        }

        // 回退到默认值
        if (DEFAULT_PATH.isEmpty()) {
            return String.format("opc.tcp://%s:%d", DEFAULT_HOSTNAME, DEFAULT_PORT);
        } else {
            return String.format("opc.tcp://%s:%d%s", DEFAULT_HOSTNAME, DEFAULT_PORT, DEFAULT_PATH);
        }
    }

    /**
     * 获取所有端点URL
     */
    public List<String> getAllEndpointUrls() {
        List<String> endpointUrls = new ArrayList<>();

        if (opcUaServer != null && opcUaServer.getServer() != null) {
            var endpoints = opcUaServer.getServer().getConfig().getEndpoints();
            for (var endpoint : endpoints) {
                endpointUrls.add(endpoint.getEndpointUrl());
            }
        }

        return endpointUrls;
    }
    
    /**
     * 获取服务器启动时间
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    /**
     * 获取服务器实例
     */
    public SimpleOpcUaServer getOpcUaServer() {
        return opcUaServer;
    }
    
    /**
     * 获取服务器状态信息
     */
    public ServerStatus getServerStatus() {
        ServerStatus status = new ServerStatus();
        status.setRunning(isServerRunning());
        status.setPort(getServerPort());
        status.setEndpointUrl(getEndpointUrl());
        status.setStartTime(getStartTime());
        
        if (opcUaServer != null && opcUaServer.getServer() != null) {
            // 获取更多详细信息
            var config = opcUaServer.getServer().getConfig();
            status.setApplicationName(config.getApplicationName().getText());
            status.setApplicationUri(config.getApplicationUri());
            status.setProductUri(config.getProductUri());
            
            if (config.getBuildInfo() != null) {
                var buildInfo = config.getBuildInfo();
                status.setBuildInfo(String.format("%s %s",
                    buildInfo.getProductName(), buildInfo.getSoftwareVersion()));
            }
        }
        
        return status;
    }
    
    /**
     * 服务器状态信息类
     */
    public static class ServerStatus {
        private boolean running;
        private int port;
        private String endpointUrl;
        private LocalDateTime startTime;
        private String applicationName;
        private String applicationUri;
        private String productUri;
        private String buildInfo;
        
        // Getters and Setters
        public boolean isRunning() { return running; }
        public void setRunning(boolean running) { this.running = running; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public String getEndpointUrl() { return endpointUrl; }
        public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public String getApplicationName() { return applicationName; }
        public void setApplicationName(String applicationName) { this.applicationName = applicationName; }
        
        public String getApplicationUri() { return applicationUri; }
        public void setApplicationUri(String applicationUri) { this.applicationUri = applicationUri; }
        
        public String getProductUri() { return productUri; }
        public void setProductUri(String productUri) { this.productUri = productUri; }
        
        public String getBuildInfo() { return buildInfo; }
        public void setBuildInfo(String buildInfo) { this.buildInfo = buildInfo; }
    }
}
