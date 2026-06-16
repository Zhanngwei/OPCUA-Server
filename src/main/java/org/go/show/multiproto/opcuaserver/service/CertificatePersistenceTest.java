package org.go.show.multiproto.opcuaserver.service;

import org.go.show.multiproto.opcuaserver.entity.CertificateEntity;
import org.go.show.multiproto.opcuaserver.model.Certificate;
import org.go.show.multiproto.opcuaserver.repository.CertificateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 证书持久化测试组件
 */
@Component
public class CertificatePersistenceTest {

    private static final Logger logger = LoggerFactory.getLogger(CertificatePersistenceTest.class);

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private DatabaseHealthService databaseHealthService;

    /**
     * 执行完整的证书持久化测试
     */
    public boolean runPersistenceTest() {
        logger.info("Starting certificate persistence test...");

        try {
            // 1. 检查数据库健康状态
            if (!databaseHealthService.isDatabaseHealthy()) {
                logger.error("Database health check failed");
                return false;
            }

            // 2. 测试创建测试证书
            Long testCertId = createTestCertificate();
            if (testCertId == null) {
                logger.error("Failed to create test certificate");
                return false;
            }

            // 3. 测试信任证书
            if (!testTrustCertificate(testCertId)) {
                logger.error("Failed to trust certificate");
                return false;
            }

            // 4. 测试拒绝证书
            if (!testRejectCertificate(testCertId)) {
                logger.error("Failed to reject certificate");
                return false;
            }

            // 5. 清理测试数据
            cleanupTestCertificate(testCertId);

            logger.info("Certificate persistence test completed successfully");
            return true;

        } catch (Exception e) {
            logger.error("Certificate persistence test failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 创建测试证书
     */
    @Transactional
    private Long createTestCertificate() {
        try {
            CertificateEntity testCert = new CertificateEntity();
            testCert.setName("Test Certificate - " + System.currentTimeMillis());
            testCert.setStatus("Rejected");
            testCert.setType("Test Certificate");
            testCert.setTrusted(false);
            testCert.setThumbprint("test-thumbprint-" + System.currentTimeMillis());
            testCert.setSerialNumber("test-serial-" + System.currentTimeMillis());
            testCert.setValidFrom(LocalDateTime.now());
            testCert.setValidTo(LocalDateTime.now().plusYears(1));
            testCert.setCreatedAt(LocalDateTime.now());
            testCert.setLastModified(LocalDateTime.now());

            CertificateEntity saved = certificateRepository.save(testCert);
            logger.info("Created test certificate with ID: {}", saved.getId());
            return saved.getId();

        } catch (Exception e) {
            logger.error("Failed to create test certificate: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 测试信任证书功能
     */
    private boolean testTrustCertificate(Long certId) {
        try {
            // 信任证书
            boolean trustResult = certificateService.trustCertificate(certId);
            if (!trustResult) {
                logger.error("Trust certificate operation returned false");
                return false;
            }

            // 验证状态
            boolean verifyResult = certificateService.verifyCertificateStatus(certId, true);
            if (!verifyResult) {
                logger.error("Certificate trust status verification failed");
                return false;
            }

            logger.info("Trust certificate test passed for ID: {}", certId);
            return true;

        } catch (Exception e) {
            logger.error("Trust certificate test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 测试拒绝证书功能
     */
    private boolean testRejectCertificate(Long certId) {
        try {
            // 拒绝证书
            boolean rejectResult = certificateService.rejectCertificate(certId);
            if (!rejectResult) {
                logger.error("Reject certificate operation returned false");
                return false;
            }

            // 验证状态
            boolean verifyResult = certificateService.verifyCertificateStatus(certId, false);
            if (!verifyResult) {
                logger.error("Certificate reject status verification failed");
                return false;
            }

            logger.info("Reject certificate test passed for ID: {}", certId);
            return true;

        } catch (Exception e) {
            logger.error("Reject certificate test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 清理测试证书
     */
    @Transactional
    private void cleanupTestCertificate(Long certId) {
        try {
            certificateRepository.deleteById(certId);
            logger.info("Cleaned up test certificate with ID: {}", certId);
        } catch (Exception e) {
            logger.error("Failed to cleanup test certificate: {}", e.getMessage());
        }
    }

    /**
     * 获取当前证书统计信息
     */
    public void printCertificateStats() {
        try {
            List<Certificate> allCerts = certificateService.getAllCertificates();
            long trustedCount = allCerts.stream().filter(Certificate::isTrusted).count();
            long rejectedCount = allCerts.stream().filter(cert -> !cert.isTrusted()).count();

            logger.info("Certificate Statistics - Total: {}, Trusted: {}, Rejected: {}", 
                allCerts.size(), trustedCount, rejectedCount);

            // 打印每个证书的详细信息
            for (Certificate cert : allCerts) {
                logger.debug("Certificate: ID={}, Name={}, Status={}, Trusted={}", 
                    cert.getId(), cert.getName(), cert.getStatus(), cert.isTrusted());
            }

        } catch (Exception e) {
            logger.error("Failed to get certificate statistics: {}", e.getMessage());
        }
    }
}
