package org.go.show.multiproto.opcuaserver.service;

import org.go.show.multiproto.opcuaserver.entity.CertificateEntity;
import org.go.show.multiproto.opcuaserver.model.Certificate;
import org.go.show.multiproto.opcuaserver.repository.CertificateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 真实的证书管理服务
 */
@Service
@Transactional
public class CertificateService {
    
    private static final Logger logger = LoggerFactory.getLogger(CertificateService.class);
    
    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private DatabaseHealthService databaseHealthService;
    
    // 证书存储目录
    private static final String TRUSTED_CERTS_DIR = "src/main/resources/trusted";
    private static final String REJECTED_CERTS_DIR = "src/main/resources/rejected";
    private static final String OWN_CERTS_DIR = "src/main/resources/own";
    
    public CertificateService() {
        // 初始化证书目录
        initializeCertificateDirectories();
    }

    @PostConstruct
    public void initializeService() {
        logger.info("Initializing Certificate Service...");

        // 检查数据库健康状态
        if (!databaseHealthService.isDatabaseHealthy()) {
            logger.error("Database is not healthy, certificate service initialization failed");
            return;
        }

        if (!databaseHealthService.isCertificateTableHealthy()) {
            logger.error("Certificate table is not accessible, certificate service initialization failed");
            return;
        }

        logger.info("Database health check passed: {}", databaseHealthService.getDatabaseInfo());

        try {
            // 清理重复的证书记录
            cleanupDuplicateCertificates();

            // 扫描并加载证书文件
            scanAndLoadCertificates();

            logger.info("Certificate Service initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Certificate Service: {}", e.getMessage(), e);
        }
    }


    
    /**
     * 获取所有证书（未删除的）
     */
    public List<Certificate> getAllCertificates() {
        List<CertificateEntity> entities = certificateRepository.findByDeletedFalse();
        return entities.stream().map(this::convertToModel).collect(Collectors.toList());
    }
    
    /**
     * 根据类型获取证书（未删除的）
     */
    public List<Certificate> getCertificatesByType(String type) {
        List<CertificateEntity> entities;
        if (type == null || type.isEmpty()) {
            entities = certificateRepository.findByDeletedFalse();
        } else {
            entities = certificateRepository.findByTypeAndDeletedFalse(type);
        }
        return entities.stream().map(this::convertToModel).collect(Collectors.toList());
    }
    
    /**
     * 根据ID获取证书
     */
    public Certificate getCertificateById(Long id) {
        Optional<CertificateEntity> entity = certificateRepository.findById(id);
        return entity.map(this::convertToModel).orElse(null);
    }
    
    /**
     * 信任证书
     */
    @Transactional
    public boolean trustCertificate(Long id) {
        try {
            Optional<CertificateEntity> entityOpt = certificateRepository.findById(id);
            if (entityOpt.isPresent()) {
                CertificateEntity entity = entityOpt.get();
                entity.setTrusted(true);
                entity.setStatus("Trusted");
                entity.setLastModified(LocalDateTime.now());

                CertificateEntity savedEntity = certificateRepository.save(entity);
                logger.info("Certificate trusted successfully: {} (ID: {})", savedEntity.getName(), savedEntity.getId());

                // 验证保存是否成功
                Optional<CertificateEntity> verifyEntity = certificateRepository.findById(id);
                if (verifyEntity.isPresent() && verifyEntity.get().isTrusted()) {
                    logger.debug("Certificate trust status verified in database");
                    return true;
                } else {
                    logger.error("Failed to verify certificate trust status in database");
                    return false;
                }
            } else {
                logger.warn("Certificate not found with ID: {}", id);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error trusting certificate with ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to trust certificate", e);
        }
    }
    
    /**
     * 拒绝证书
     */
    @Transactional
    public boolean rejectCertificate(Long id) {
        try {
            Optional<CertificateEntity> entityOpt = certificateRepository.findById(id);
            if (entityOpt.isPresent()) {
                CertificateEntity entity = entityOpt.get();
                entity.setTrusted(false);
                entity.setStatus("Rejected");
                entity.setLastModified(LocalDateTime.now());

                CertificateEntity savedEntity = certificateRepository.save(entity);
                logger.info("Certificate rejected successfully: {} (ID: {})", savedEntity.getName(), savedEntity.getId());

                // 验证保存是否成功
                Optional<CertificateEntity> verifyEntity = certificateRepository.findById(id);
                if (verifyEntity.isPresent() && !verifyEntity.get().isTrusted()) {
                    logger.debug("Certificate reject status verified in database");
                    return true;
                } else {
                    logger.error("Failed to verify certificate reject status in database");
                    return false;
                }
            } else {
                logger.warn("Certificate not found with ID: {}", id);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error rejecting certificate with ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to reject certificate", e);
        }
    }
    
    /**
     * 删除证书（软删除）
     */
    public boolean deleteCertificate(Long id) {
        Optional<CertificateEntity> entityOpt = certificateRepository.findById(id);
        if (entityOpt.isPresent()) {
            CertificateEntity entity = entityOpt.get();
            entity.setDeleted(true);
            certificateRepository.save(entity);
            logger.info("Certificate soft deleted: {}", entity.getName());
            return true;
        }
        return false;
    }
    
    /**
     * 更新证书状态
     */
    public boolean updateCertificateStatus(Long id, String status) {
        Optional<CertificateEntity> entityOpt = certificateRepository.findById(id);
        if (entityOpt.isPresent()) {
            CertificateEntity entity = entityOpt.get();
            entity.setStatus(status);
            certificateRepository.save(entity);
            logger.info("Updated certificate {} status to {}", entity.getName(), status);
            return true;
        }
        return false;
    }
    
    /**
     * 扫描并加载证书文件
     */
    public void scanAndLoadCertificates() {
        logger.info("Scanning and loading certificates from file system...");
        
        // 扫描受信任证书目录（新发现的证书默认为Rejected，需要手动信任）
        scanCertificatesInDirectory(TRUSTED_CERTS_DIR, "Rejected", "Client Certificate");
        
        // 扫描被拒绝证书目录
        scanCertificatesInDirectory(REJECTED_CERTS_DIR, "Rejected", "Client Certificate");
        
        // 扫描自有证书目录
        scanCertificatesInDirectory(OWN_CERTS_DIR, "Trusted", "Own Certificate");
        
        logger.info("Certificate scanning completed.");
    }
    
    /**
     * 扫描指定目录中的证书
     */
    private void scanCertificatesInDirectory(String directory, String status, String type) {
        try {
            Path dirPath = Paths.get(directory);
            if (!Files.exists(dirPath)) {
                logger.warn("Certificate directory does not exist: {}", directory);
                return;
            }
            
            Files.walk(dirPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().toLowerCase().endsWith(".der") || 
                              path.toString().toLowerCase().endsWith(".crt") ||
                              path.toString().toLowerCase().endsWith(".cer"))
                .forEach(path -> loadCertificateFromFile(path, status, type));
                
        } catch (IOException e) {
            logger.error("Error scanning certificate directory {}: {}", directory, e.getMessage());
        }
    }
    
    /**
     * 从文件加载证书
     */
    private void loadCertificateFromFile(Path filePath, String status, String type) {
        try {
            // 检查是否已经存在且未删除
            Optional<CertificateEntity> existing = certificateRepository.findByFilePathAndDeletedFalse(filePath.toString());
            if (existing.isPresent()) {
                return; // 已存在且未删除，跳过
            }

            // 检查是否存在已删除的证书，如果存在则跳过（不自动恢复）
            Optional<CertificateEntity> deleted = certificateRepository.findByFilePath(filePath.toString());
            if (deleted.isPresent() && deleted.get().isDeleted()) {
                logger.info("Skipping deleted certificate: {} from {}", deleted.get().getName(), filePath);
                return; // 跳过已删除的证书，不自动恢复
            }
            
            // 读取证书文件
            byte[] certData = Files.readAllBytes(filePath);
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509Certificate x509Cert = (X509Certificate) factory.generateCertificate(
                new FileInputStream(filePath.toFile()));
            
            // 创建证书实体
            CertificateEntity entity = new CertificateEntity();
            entity.setName(extractCommonName(x509Cert.getSubjectDN().getName()));
            entity.setStatus(status);
            entity.setType(type);
            entity.setFilename(filePath.getFileName().toString());
            entity.setFilePath(filePath.toString());
            entity.setCertificateData(certData);
            
            // 设置证书详细信息
            entity.setSubject(x509Cert.getSubjectDN().getName());
            entity.setIssuer(x509Cert.getIssuerDN().getName());
            entity.setSerialNumber(x509Cert.getSerialNumber().toString());
            entity.setSignatureAlgorithm(x509Cert.getSigAlgName());
            entity.setVersion(x509Cert.getVersion());
            entity.setValidFrom(x509Cert.getNotBefore().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
            entity.setValidTo(x509Cert.getNotAfter().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
            
            // 计算指纹
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(x509Cert.getEncoded());
            entity.setThumbprint(bytesToHex(digest));
            
            // 获取密钥大小
            entity.setKeySize(x509Cert.getPublicKey().getAlgorithm().contains("RSA") ? 
                ((java.security.interfaces.RSAPublicKey) x509Cert.getPublicKey()).getModulus().bitLength() : null);
            
            // 保存到数据库
            certificateRepository.save(entity);
            logger.info("Loaded certificate: {} from {}", entity.getName(), filePath);
            
        } catch (Exception e) {
            logger.error("Error loading certificate from {}: {}", filePath, e.getMessage());
        }
    }
    
    /**
     * 初始化证书目录
     */
    private void initializeCertificateDirectories() {
        try {
            Files.createDirectories(Paths.get(TRUSTED_CERTS_DIR));
            Files.createDirectories(Paths.get(REJECTED_CERTS_DIR));
            Files.createDirectories(Paths.get(OWN_CERTS_DIR));
            logger.info("Certificate directories initialized.");
        } catch (IOException e) {
            logger.error("Error creating certificate directories: {}", e.getMessage());
        }
    }
    
    /**
     * 计算证书指纹
     */
    public String calculateThumbprint(X509Certificate certificate) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(certificate.getEncoded());
        return bytesToHex(digest);
    }

    /**
     * 根据指纹查找证书
     */
    public Optional<CertificateEntity> findByThumbprint(String thumbprint) {
        // 处理可能的重复记录，返回第一个未删除的证书
        List<CertificateEntity> certs = certificateRepository.findAll().stream()
            .filter(cert -> thumbprint.equals(cert.getThumbprint()) && !cert.isDeleted())
            .collect(Collectors.toList());

        return certs.isEmpty() ? Optional.empty() : Optional.of(certs.get(0));
    }

    /**
     * 保存新的客户端证书（默认为Rejected状态）
     */
    public void saveNewClientCertificate(X509Certificate certificate) {
        try {
            // 计算指纹
            String thumbprint = calculateThumbprint(certificate);

            // 检查证书是否已经存在 - 使用findAll来处理重复记录
            List<CertificateEntity> existingCerts = certificateRepository.findAll().stream()
                .filter(cert -> thumbprint.equals(cert.getThumbprint()) && !cert.isDeleted())
                .collect(Collectors.toList());

            if (!existingCerts.isEmpty()) {
                logger.info("Client certificate already exists: {}", existingCerts.get(0).getName());
                return; // 证书已存在，不重复保存
            }

            // 创建证书实体
            CertificateEntity entity = new CertificateEntity();
            entity.setName(extractCommonName(certificate.getSubjectDN().getName()));
            entity.setStatus("Rejected"); // 新证书默认为拒绝状态
            entity.setType("Client Certificate");
            entity.setFilename("client-" + System.currentTimeMillis() + ".der");
            entity.setFilePath(""); // 客户端证书不保存到文件
            entity.setCertificateData(certificate.getEncoded());

            // 设置证书详细信息
            entity.setSubject(certificate.getSubjectDN().getName());
            entity.setIssuer(certificate.getIssuerDN().getName());
            entity.setSerialNumber(certificate.getSerialNumber().toString());
            entity.setSignatureAlgorithm(certificate.getSigAlgName());
            entity.setVersion(certificate.getVersion());
            entity.setValidFrom(certificate.getNotBefore().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
            entity.setValidTo(certificate.getNotAfter().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());

            // 设置指纹
            entity.setThumbprint(thumbprint);

            // 获取密钥大小
            if (certificate.getPublicKey().getAlgorithm().contains("RSA")) {
                entity.setKeySize(((java.security.interfaces.RSAPublicKey) certificate.getPublicKey()).getModulus().bitLength());
            }

            // 保存到数据库
            certificateRepository.save(entity);
            logger.info("Saved new client certificate as rejected: {}", entity.getName());

        } catch (Exception e) {
            logger.error("Error saving new client certificate: {}", e.getMessage());
        }
    }

    /**
     * 将实体转换为模型
     */
    private Certificate convertToModel(CertificateEntity entity) {
        Certificate cert = new Certificate();
        cert.setId(entity.getId());
        cert.setName(entity.getName());
        cert.setStatus(entity.getStatus());
        cert.setSignedBy(entity.getSignedBy());
        cert.setType(entity.getType());
        cert.setTrusted(entity.isTrusted());

        // 设置详细信息
        cert.setValidFrom(entity.getValidFrom() != null ?
            entity.getValidFrom().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        cert.setValidTo(entity.getValidTo() != null ?
            entity.getValidTo().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        cert.setApplicationUri(entity.getApplicationUri());
        cert.setKeySize(entity.getKeySize());
        cert.setFilename(entity.getFilename());
        cert.setVersion(entity.getVersion());
        cert.setSerialNumber(entity.getSerialNumber());
        cert.setSignatureAlgorithm(entity.getSignatureAlgorithm());
        cert.setIssuer(entity.getIssuer());
        cert.setSubject(entity.getSubject());
        cert.setSubjectAlternativeName(entity.getSubjectAlternativeName());
        cert.setThumbprint(entity.getThumbprint());

        cert.setCreated(entity.getCreatedAt() != null ?
            entity.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        cert.setLastModified(entity.getLastModified() != null ?
            entity.getLastModified().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);

        return cert;
    }
    
    /**
     * 从DN中提取通用名称
     */
    private String extractCommonName(String dn) {
        if (dn == null) return "Unknown";
        String[] parts = dn.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("CN=")) {
                return part.substring(3);
            }
        }
        return "Unknown";
    }
    
    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }
    
    /**
     * 获取证书统计信息
     */
    public Map<String, Object> getCertificateStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        List<Object[]> statusCounts = certificateRepository.countByStatus();
        Map<String, Long> statusMap = new HashMap<>();
        for (Object[] row : statusCounts) {
            statusMap.put((String) row[0], (Long) row[1]);
        }
        stats.put("statusCounts", statusMap);
        
        List<Object[]> typeCounts = certificateRepository.countByType();
        Map<String, Long> typeMap = new HashMap<>();
        for (Object[] row : typeCounts) {
            typeMap.put((String) row[0], (Long) row[1]);
        }
        stats.put("typeCounts", typeMap);
        
        // 即将过期的证书
        LocalDateTime thirtyDaysLater = LocalDateTime.now().plusDays(30);
        List<CertificateEntity> expiring = certificateRepository.findExpiringCertificates(thirtyDaysLater, LocalDateTime.now());
        stats.put("expiringCount", expiring.size());
        
        // 已过期的证书
        List<CertificateEntity> expired = certificateRepository.findExpiredCertificates(LocalDateTime.now());
        stats.put("expiredCount", expired.size());
        
        return stats;
    }

    /**
     * 重新创建自有证书
     */
    public boolean recreateOwnCertificate(Long id) {
        Optional<CertificateEntity> entityOpt = certificateRepository.findById(id);
        if (entityOpt.isPresent()) {
            CertificateEntity entity = entityOpt.get();
            if (entity.getType().contains("Own Certificate")) {
                // 模拟重新创建证书
                entity.setValidFrom(LocalDateTime.now());
                entity.setValidTo(LocalDateTime.now().plusYears(5));
                certificateRepository.save(entity);
                logger.info("Certificate recreated: {}", entity.getName());
                return true;
            }
        }
        return false;
    }

    /**
     * 刷新证书列表
     */
    public void refreshCertificates() {
        logger.info("Refreshing certificate list...");
        scanAndLoadCertificates();
    }

    /**
     * 获取证书统计信息（兼容旧方法名）
     */
    public Map<String, Object> getCertificateStats() {
        return getCertificateStatistics();
    }

    /**
     * 验证证书是否受信任
     */
    public boolean isCertificateTrusted(String thumbprint) {
        Optional<CertificateEntity> entity = findByThumbprint(thumbprint);
        return entity.map(CertificateEntity::isTrusted).orElse(false);
    }

    /**
     * 验证证书状态是否正确保存
     */
    @Transactional(readOnly = true)
    public boolean verifyCertificateStatus(Long id, boolean expectedTrusted) {
        try {
            Optional<CertificateEntity> entity = certificateRepository.findById(id);
            if (entity.isPresent()) {
                boolean actualTrusted = entity.get().isTrusted();
                String actualStatus = entity.get().getStatus();
                logger.debug("Certificate ID {}: Expected trusted={}, Actual trusted={}, Status={}",
                    id, expectedTrusted, actualTrusted, actualStatus);
                return actualTrusted == expectedTrusted;
            } else {
                logger.warn("Certificate not found for verification: ID {}", id);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error verifying certificate status for ID {}: {}", id, e.getMessage());
            return false;
        }
    }

    /**
     * 清理重复的证书记录
     */
    public void cleanupDuplicateCertificates() {
        logger.info("Cleaning up duplicate certificates...");

        Map<String, List<CertificateEntity>> groupedByThumbprint = certificateRepository.findAll().stream()
            .collect(Collectors.groupingBy(CertificateEntity::getThumbprint));

        for (Map.Entry<String, List<CertificateEntity>> entry : groupedByThumbprint.entrySet()) {
            List<CertificateEntity> duplicates = entry.getValue();
            if (duplicates.size() > 1) {
                logger.info("Found {} duplicate certificates with thumbprint: {}", duplicates.size(), entry.getKey());

                // 保留第一个，删除其他的
                for (int i = 1; i < duplicates.size(); i++) {
                    CertificateEntity duplicate = duplicates.get(i);
                    logger.info("Deleting duplicate certificate: {}", duplicate.getName());
                    certificateRepository.delete(duplicate);
                }
            }
        }

        logger.info("Certificate cleanup completed.");
    }

    /**
     * 根据应用程序URI查找证书
     */
    public Certificate getCertificateByApplicationUri(String applicationUri) {
        Optional<CertificateEntity> entity = certificateRepository.findByApplicationUri(applicationUri);
        return entity.map(this::convertToModel).orElse(null);
    }

    /**
     * 获取受信任的证书列表（未删除的）
     */
    public List<Certificate> getTrustedCertificates() {
        List<CertificateEntity> entities = certificateRepository.findByTrustedTrueAndDeletedFalse();
        return entities.stream().map(this::convertToModel).collect(Collectors.toList());
    }

    /**
     * 获取即将过期的证书
     */
    public List<Certificate> getExpiringCertificates(int days) {
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(days);
        List<CertificateEntity> entities = certificateRepository.findExpiringCertificates(expiryDate, LocalDateTime.now());
        return entities.stream().map(this::convertToModel).collect(Collectors.toList());
    }

    /**
     * 获取已过期的证书
     */
    public List<Certificate> getExpiredCertificates() {
        List<CertificateEntity> entities = certificateRepository.findExpiredCertificates(LocalDateTime.now());
        return entities.stream().map(this::convertToModel).collect(Collectors.toList());
    }
}
