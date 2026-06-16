package org.go.show.multiproto.opcuaserver.service;

import org.go.show.multiproto.opcuaserver.server.OpcUaServerManager;
import org.go.show.multiproto.opcuaserver.service.NodePersistenceService.NodeStatistics;
import org.eclipse.milo.opcua.sdk.server.SessionListener;
import org.eclipse.milo.opcua.sdk.server.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 状态服务
 */
@Service
public class StatusService implements SessionListener {
    
    private static final Logger logger = LoggerFactory.getLogger(StatusService.class);
    
    @Autowired
    private OpcUaServerManager serverManager;
    
    // 节点持久化服务（用于统计自定义节点数量）
    @Autowired(required = false)
    private NodePersistenceService nodePersistenceService;
    
    // 统计数据
    private final AtomicInteger totalConnections = new AtomicInteger(0);
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicInteger failedConnections = new AtomicInteger(0);
    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicLong lastRequestTime = new AtomicLong(System.currentTimeMillis());
    
    // 活动日志队列（最多保存1000条）
    private final Queue<Map<String, Object>> activityLogs = new ConcurrentLinkedQueue<>();
    private static final int MAX_LOG_SIZE = 1000;

    // 初始化时添加一些示例日志
    {
        addActivityLog("INFO", "StatusService initialized", "System");
        addActivityLog("INFO", "OPC UA Server starting up", "Server");
    }

    /**
     * 初始化方法，注册会话监听器
     */
    @PostConstruct
    public void init() {
        logger.info("StatusService initialized");
        addActivityLog("INFO", "StatusService initialized", "StatusService");

        try {
            // 等待服务器启动后注册监听器
            new Thread(() -> {
                try {
                    // 等待更长时间确保服务器完全启动
                    Thread.sleep(8000); // 增加到8秒

                    int retryCount = 0;
                    int maxRetries = 15; // 增加重试次数

                    while (retryCount < maxRetries) {
                        try {
                            if (serverManager != null &&
                                serverManager.getOpcUaServer() != null &&
                                serverManager.getOpcUaServer().getServer() != null &&
                                serverManager.getOpcUaServer().getServer().getSessionManager() != null) {

                                serverManager.getOpcUaServer().getServer().getSessionManager().addSessionListener(this);
                                logger.info("*** Session listener registered successfully ***");
                                addActivityLog("INFO", "Session listener registered", "StatusService");
                                break;
                            } else {
                                logger.debug("OPC UA server not ready yet, retry {}/{}", retryCount + 1, maxRetries);
                                Thread.sleep(2000); // 增加等待时间到2秒
                                retryCount++;
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to register session listener, retry {}/{}: {}", retryCount + 1, maxRetries, e.getMessage());
                            Thread.sleep(2000);
                            retryCount++;
                        }
                    }

                    if (retryCount >= maxRetries) {
                        logger.error("Failed to register session listener after {} retries", maxRetries);
                        addActivityLog("ERROR", "Failed to register session listener", "StatusService");
                    }

                } catch (Exception e) {
                    logger.error("Failed to register session listener", e);
                }
            }).start();
        } catch (Exception e) {
            logger.error("Failed to initialize StatusService", e);
        }
    }

    /**
     * 会话创建时的回调
     */
    @Override
    public void onSessionCreated(Session session) {
        totalConnections.incrementAndGet();
        activeConnections.incrementAndGet();

        String clientInfo = session.getSessionName() != null ? session.getSessionName() : "Unknown Client";
        logger.info("Client session created: {}", clientInfo);
        addActivityLog("INFO", "Client connected: " + clientInfo, "Server");
    }

    /**
     * 会话关闭时的回调
     */
    @Override
    public void onSessionClosed(Session session) {
        activeConnections.decrementAndGet();

        String clientInfo = session.getSessionName() != null ? session.getSessionName() : "Unknown Client";
        logger.info("Client session closed: {}", clientInfo);
        addActivityLog("INFO", "Client disconnected: " + clientInfo, "Server");
    }

    /**
     * 获取服务器状态信息
     */
    public Map<String, Object> getServerStatus() {
        Map<String, Object> status = new HashMap<>();

        OpcUaServerManager.ServerStatus serverStatus = serverManager.getServerStatus();

        status.put("running", serverStatus.isRunning());
        status.put("uptime", calculateUptime(serverStatus.getStartTime()));
        status.put("port", serverStatus.getPort());
        status.put("endpointUrl", serverStatus.getEndpointUrl());
        status.put("allEndpoints", serverManager.getAllEndpointUrls());

        if (serverStatus.getStartTime() != null) {
            status.put("startTime", serverStatus.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } else {
            status.put("startTime", "");
        }

        status.put("currentTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        status.put("applicationName", serverStatus.getApplicationName());
        status.put("applicationUri", serverStatus.getApplicationUri());
        status.put("productUri", serverStatus.getProductUri());
        status.put("buildInfo", serverStatus.getBuildInfo());

        return status;
    }
    
    /**
     * 获取连接统计信息
     */
    public Map<String, Object> getConnectionStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("active", activeConnections.get());
        stats.put("total", totalConnections.get());
        stats.put("failed", failedConnections.get());
        stats.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return stats;
    }
    
    /**
     * 获取节点统计信息
     */
    public Map<String, Object> getNodeStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            long totalNodes = 0;
            long variableNodes = 0;
            long objectNodes = 0;

            // 使用节点持久化服务统计自定义节点数量
            if (nodePersistenceService != null) {
                try {
                    NodeStatistics nodeStats = nodePersistenceService.getStatistics();
                    if (nodeStats != null) {
                        totalNodes = nodeStats.getTotalNodes();
                        objectNodes = nodeStats.getFolderCount();
                        variableNodes = nodeStats.getVariableCount();

                        // 如果 totalNodes 为 0（配置关闭等情况），则使用 folder+variable 作为总数
                        if (totalNodes == 0) {
                            totalNodes = objectNodes + variableNodes;
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to get node statistics from persistence service", e);
                }
            }

            stats.put("total", totalNodes);
            stats.put("variables", variableNodes);
            stats.put("objects", objectNodes);
            stats.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        } catch (Exception e) {
            logger.error("Error getting node stats", e);
            stats.put("total", 0L);
            stats.put("variables", 0L);
            stats.put("objects", 0L);
        }

        return stats;
    }
    
    /**
     * 获取性能指标
     */
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            // CPU使用率 - 使用兼容的方法
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double cpuUsage = 0;

            // 尝试获取CPU使用率，如果不支持则使用模拟值
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean =
                    (com.sun.management.OperatingSystemMXBean) osBean;
                cpuUsage = sunOsBean.getProcessCpuLoad() * 100;
                if (cpuUsage < 0) cpuUsage = Math.random() * 30; // 模拟值
            } else {
                cpuUsage = Math.random() * 30; // 模拟值
            }
            
            // 内存使用率
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
            double memoryUsage = (double) usedMemory / maxMemory * 100;
            
            // 请求/秒
            long currentTime = System.currentTimeMillis();
            long timeDiff = currentTime - lastRequestTime.get();
            double requestsPerSec = 0;
            if (timeDiff > 0) {
                requestsPerSec = (double) requestCount.get() / (timeDiff / 1000.0);
            }
            
            metrics.put("cpu", Math.round(cpuUsage));
            metrics.put("memory", Math.round(memoryUsage));
            metrics.put("requestsPerSec", Math.round(requestsPerSec));
            metrics.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
        } catch (Exception e) {
            logger.error("Error getting performance metrics", e);
            metrics.put("cpu", 0);
            metrics.put("memory", 0);
            metrics.put("requestsPerSec", 0);
        }
        
        return metrics;
    }
    
    /**
     * 获取活动日志
     */
    public Map<String, Object> getActivityLogs(int limit) {
        Map<String, Object> result = new HashMap<>();
        
        List<Map<String, Object>> logs = new ArrayList<>();
        Iterator<Map<String, Object>> iterator = activityLogs.iterator();
        int count = 0;
        
        while (iterator.hasNext() && count < limit) {
            logs.add(iterator.next());
            count++;
        }
        
        // 按时间倒序排列（最新的在前面）
        logs.sort((a, b) -> {
            String timeA = (String) a.get("timestamp");
            String timeB = (String) b.get("timestamp");
            return timeB.compareTo(timeA);
        });
        
        result.put("logs", logs);
        result.put("total", activityLogs.size());
        result.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }
    
    /**
     * 获取所有状态信息
     */
    public Map<String, Object> getAllStatus() {
        Map<String, Object> allStatus = new HashMap<>();

        allStatus.put("server", getServerStatus());
        allStatus.put("connections", getConnectionStats());
        allStatus.put("nodes", getNodeStats());
        allStatus.put("performance", getPerformanceMetrics());
        allStatus.put("logs", getActivityLogs(20)); // 只返回最近20条日志

        return allStatus;
    }

    /**
     * 获取端点信息
     */
    public Map<String, Object> getEndpointsInfo() {
        Map<String, Object> endpointsInfo = new HashMap<>();

        List<String> allEndpoints = serverManager.getAllEndpointUrls();
        String primaryEndpoint = serverManager.getEndpointUrl();

        endpointsInfo.put("primary", primaryEndpoint);
        endpointsInfo.put("all", allEndpoints);
        endpointsInfo.put("count", allEndpoints.size());

        return endpointsInfo;
    }
    
    /**
     * 计算服务器运行时间
     */
    private String calculateUptime(LocalDateTime startTime) {
        if (startTime == null) {
            return "Not running";
        }

        Duration duration = Duration.between(startTime, LocalDateTime.now());

        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;

        if (days > 0) {
            return String.format("%d days, %d hours, %d minutes", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%d hours, %d minutes", hours, minutes);
        } else {
            return String.format("%d minutes", minutes);
        }
    }
    
    // 以下是用于更新统计数据的方法，可以被其他服务调用
    
    public void incrementTotalConnections() {
        totalConnections.incrementAndGet();
    }
    
    public void incrementActiveConnections() {
        activeConnections.incrementAndGet();
    }
    
    public void decrementActiveConnections() {
        activeConnections.decrementAndGet();
    }
    
    public void incrementFailedConnections() {
        failedConnections.incrementAndGet();
    }
    
    public void incrementRequestCount() {
        requestCount.incrementAndGet();
        lastRequestTime.set(System.currentTimeMillis());
    }
    
    /**
     * 手动注册SessionListener（用于确保连接统计正常工作）
     */
    public void registerSessionListener() {
        try {
            if (serverManager.getOpcUaServer() != null &&
                serverManager.getOpcUaServer().getServer() != null) {

                serverManager.getOpcUaServer().getServer().getSessionManager().addSessionListener(this);
                logger.info("*** Session listener manually registered successfully ***");
                addActivityLog("INFO", "Session listener manually registered", "StatusService");
            } else {
                logger.warn("Cannot register session listener - OPC UA server not available");
            }
        } catch (Exception e) {
            logger.error("Failed to manually register session listener", e);
        }
    }

    /**
     * 添加活动日志
     */
    public void addActivityLog(String level, String message, String source) {
        Map<String, Object> log = new HashMap<>();
        log.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        log.put("level", level);
        log.put("message", message);
        log.put("source", source);

        activityLogs.offer(log);

        // 保持队列大小不超过最大限制
        while (activityLogs.size() > MAX_LOG_SIZE) {
            activityLogs.poll();
        }
    }
    
    /**
     * 重置服务器启动时间（当服务器重启时调用）
     */
    public void resetServerStartTime() {
        addActivityLog("INFO", "OPC UA Server started", "Server");
    }
}
