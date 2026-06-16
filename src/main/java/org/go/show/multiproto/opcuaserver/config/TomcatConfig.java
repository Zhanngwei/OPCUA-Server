package org.go.show.multiproto.opcuaserver.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tomcat 配置类
 * 用于禁用 APR 以避免在麒麟系统上的版本兼容性问题
 * 解决错误: An incompatible version [1.2.23] of the Apache Tomcat Native library is installed
 */
@Configuration
public class TomcatConfig {

    /**
     * 自定义 Tomcat 配置，强制使用 NIO 协议而不是 APR
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> {
            // 强制使用 NIO 协议
            factory.setProtocol("org.apache.coyote.http11.Http11NioProtocol");
            
            // 添加额外的连接器配置
            factory.addConnectorCustomizers(connector -> {
                // 确保使用 NIO 协议
                connector.setProperty("protocol", "HTTP/1.1");
                // 禁用 APR
                connector.setProperty("useAprConnector", "false");
            });
            
            System.out.println("=== Tomcat 配置已应用: 强制使用 NIO 协议，APR 已禁用 ===");
        };
    }
}
