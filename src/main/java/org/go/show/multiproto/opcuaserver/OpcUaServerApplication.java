package org.go.show.multiproto.opcuaserver;

import org.go.show.multiproto.opcuaserver.config.WebSecurityConfig;
import org.go.show.multiproto.opcuaserver.server.OpcUaServerManager;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.concurrent.CompletableFuture;

@SpringBootApplication(scanBasePackages = "org.go.show.multiproto.opcuaserver")
@EnableJpaRepositories(basePackages = "org.go.show.multiproto.opcuaserver.repository")
@EntityScan(basePackages = "org.go.show.multiproto.opcuaserver.entity")
public class OpcUaServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(OpcUaServerApplication.class);

    /**
     * 初始化固定Token用于mqtt2opcua服务调用
     * 这样mqtt2opcua可以使用固定token调用受保护的API
     */
    @PostConstruct
    public void initFixedToken() {
        String fixedToken = "mqtt2opcua-fixed-token-12345";
        WebSecurityConfig.putToken(fixedToken, "mqtt2opcua-service");
        logger.info("✅ 固定Token已初始化用于mqtt2opcua服务: {}", fixedToken);
    }

    public static void main(String[] args) throws Exception {
        logger.debug("Starting OPC UA Server with Web Interface...");

        // 启动Spring Boot Web应用
        ConfigurableApplicationContext context = SpringApplication.run(OpcUaServerApplication.class, args);

        // 获取OpcUaServerManager并启动服务器
        OpcUaServerManager serverManager = context.getBean(OpcUaServerManager.class);
        serverManager.init();

        Environment env = context.getEnvironment();
        String port = env.getProperty("server.port", "8080");

        logger.info("==================== OPC UA Server启动完成 ====================");
        logger.info("应用程序已启动，访问地址: http://localhost:{}", port);

        final CompletableFuture<Void> future = new CompletableFuture<>();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.debug("Shutting down...");
            try {
                serverManager.cleanup();
                context.close();
            } catch (Exception e) {
                logger.error("Error during shutdown", e);
            }
            future.complete(null);
        }));

        try {
            future.get();
        } catch (Exception e) {
            logger.error("Error waiting for shutdown", e);
        }

        logger.debug("Application stopped.");
    }
}
