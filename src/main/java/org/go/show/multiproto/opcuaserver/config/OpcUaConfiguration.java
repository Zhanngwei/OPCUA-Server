package org.go.show.multiproto.opcuaserver.config;

import org.go.show.multiproto.opcuaserver.service.NodePersistenceService;
import org.go.show.multiproto.opcuaserver.identity.SimpleIdentityValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OPC UA配置类
 * 负责在Spring容器启动后设置持久化服务
 */
@Configuration
public class OpcUaConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(OpcUaConfiguration.class);

    @Autowired
    private NodePersistenceService persistenceService;

    @Bean
    public SimpleIdentityValidator simpleIdentityValidator() {
        return new SimpleIdentityValidator();
    }

    /**
     * 在Spring容器完全启动后，设置持久化服务
     */
    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            // 获取SimpleNamespace实例（通过OpcUaServerManager）
            // 这里我们需要一个静态引用或者通过其他方式获取
            logger.info("Spring容器启动完成，准备设置持久化服务");
            
            // 由于SimpleNamespace不是Spring Bean，我们需要通过其他方式设置持久化服务
            // 这将在OpcUaServerManager中处理
            
        } catch (Exception e) {
            logger.error("设置持久化服务失败", e);
        }
    }
}
