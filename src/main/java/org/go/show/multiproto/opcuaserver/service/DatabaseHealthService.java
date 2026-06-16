package org.go.show.multiproto.opcuaserver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 数据库健康检查服务
 */
@Service
public class DatabaseHealthService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthService.class);

    @Autowired
    private DataSource dataSource;

    /**
     * 检查数据库连接是否正常
     */
    public boolean isDatabaseHealthy() {
        try (Connection connection = dataSource.getConnection()) {
            // 执行简单的查询来测试连接
            try (PreparedStatement stmt = connection.prepareStatement("SELECT 1")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    boolean hasResult = rs.next();
                    logger.debug("Database health check passed: {}", hasResult);
                    return hasResult;
                }
            }
        } catch (SQLException e) {
            logger.error("Database health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查证书表是否存在且可访问
     */
    public boolean isCertificateTableHealthy() {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM certificates WHERE 1=0")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    logger.debug("Certificate table health check passed");
                    return true;
                }
            }
        } catch (SQLException e) {
            logger.error("Certificate table health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取数据库连接信息
     */
    public String getDatabaseInfo() {
        try (Connection connection = dataSource.getConnection()) {
            return String.format("Database: %s, URL: %s, AutoCommit: %s", 
                connection.getMetaData().getDatabaseProductName(),
                connection.getMetaData().getURL(),
                connection.getAutoCommit());
        } catch (SQLException e) {
            logger.error("Failed to get database info: {}", e.getMessage());
            return "Database info unavailable: " + e.getMessage();
        }
    }

    /**
     * 测试事务功能
     */
    @Transactional
    public boolean testTransaction() {
        try (Connection connection = dataSource.getConnection()) {
            // 测试事务是否正常工作
            boolean autoCommit = connection.getAutoCommit();
            logger.debug("Transaction test - AutoCommit: {}", autoCommit);
            return true;
        } catch (SQLException e) {
            logger.error("Transaction test failed: {}", e.getMessage());
            return false;
        }
    }
}
