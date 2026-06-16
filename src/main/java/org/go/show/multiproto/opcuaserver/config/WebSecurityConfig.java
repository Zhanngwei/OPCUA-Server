package org.go.show.multiproto.opcuaserver.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
public class WebSecurityConfig implements WebMvcConfigurer {
    
    // 简单的内存token存储（与AuthController共享）
    private static final Map<String, String> tokenStore = new ConcurrentHashMap<>();
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/favicon.ico",
                    "/public/**"
                );
    }
    
    public static class AuthInterceptor implements HandlerInterceptor {
        
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            // 检查是否是静态资源或登录相关请求
            String requestURI = request.getRequestURI();
            String method = request.getMethod();

            log.info("=== AUTH INTERCEPTOR ===");
            log.info("Request URI: " + requestURI);
            log.info("Method: " + method);

            // 允许访问公共API
            if (requestURI.startsWith("/public/")) {
                log.info("*** PUBLIC API DETECTED - ALLOWING ACCESS ***");
                return true;
            }

            // 允许访问认证API
            if (requestURI.startsWith("/api/auth/")) {
                log.info("Allowing access to auth API: " + requestURI);
                return true;
            }
            
            // 检查Authorization header
            String authHeader = request.getHeader("Authorization");
            log.info("Auth header: " + authHeader); // 调试日志
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                log.info("Extracted token: " + token); // 调试日志
                log.info("Token exists in store: " + tokenStore.containsKey(token)); // 调试日志
                if (tokenStore.containsKey(token)) {
                    return true;
                }
            }
            
            // 区分API请求和页面请求的处理
            if (requestURI.startsWith("/api/")) {
                // 对于API请求，返回401
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"未授权访问\"}");
                return false;
            } else {
                // 对于页面请求，允许通过（由前端路由处理）
                return true;
            }
        }
    }
    
    // 提供静态方法供AuthController使用
    public static void putToken(String token, String username) {
        tokenStore.put(token, username);
    }
    
    public static String getUsername(String token) {
        return tokenStore.get(token);
    }
    
    public static String removeToken(String token) {
        return tokenStore.remove(token);
    }
}
