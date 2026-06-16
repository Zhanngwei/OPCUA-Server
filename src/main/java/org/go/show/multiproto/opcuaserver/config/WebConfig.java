package org.go.show.multiproto.opcuaserver.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Web配置类，用于处理前端路由和静态资源
 */
@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 处理静态资源
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        
        // 处理CSS和JS文件
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
        
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
        
        // 处理Vue Router的History模式，所有未匹配的路径都返回index.html
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        
                        // 如果请求的资源存在，直接返回
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        
                        // 如果是API请求，不处理
                        log.info("WebConfig - resourcePath: " + resourcePath);
                        if (resourcePath.startsWith("api/") || resourcePath.startsWith("public/")) {
                            log.info("WebConfig - API请求，返回null: " + resourcePath);
                            return null;
                        }

                        // 如果是H2控制台请求，不处理
                        if (resourcePath.startsWith("h2-console")) {
                            return null;
                        }

                        // 其他所有请求都返回index.html（用于Vue Router）
                        log.info("WebConfig - 返回index.html: " + resourcePath);
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}
