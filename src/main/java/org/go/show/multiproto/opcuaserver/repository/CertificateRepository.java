package org.go.show.multiproto.opcuaserver.repository;

import org.go.show.multiproto.opcuaserver.entity.CertificateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 证书数据访问层
 */
@Repository
public interface CertificateRepository extends JpaRepository<CertificateEntity, Long> {
    
    /**
     * 根据状态查找证书
     */
    List<CertificateEntity> findByStatus(String status);
    
    /**
     * 根据类型查找证书
     */
    List<CertificateEntity> findByType(String type);
    
    /**
     * 根据应用程序URI查找证书
     */
    Optional<CertificateEntity> findByApplicationUri(String applicationUri);
    
    /**
     * 根据序列号查找证书
     */
    Optional<CertificateEntity> findBySerialNumber(String serialNumber);
    
    /**
     * 根据指纹查找证书
     */
    Optional<CertificateEntity> findByThumbprint(String thumbprint);
    
    /**
     * 查找受信任的证书
     */
    List<CertificateEntity> findByTrustedTrue();
    
    /**
     * 查找被拒绝的证书
     */
    List<CertificateEntity> findByTrustedFalse();
    
    /**
     * 根据文件路径查找证书
     */
    Optional<CertificateEntity> findByFilePath(String filePath);
    
    /**
     * 查找即将过期的证书（30天内）
     */
    @Query("SELECT c FROM CertificateEntity c WHERE c.validTo <= :expiryDate AND c.validTo > :now")
    List<CertificateEntity> findExpiringCertificates(@Param("expiryDate") LocalDateTime expiryDate, @Param("now") LocalDateTime now);
    
    /**
     * 查找已过期的证书
     */
    @Query("SELECT c FROM CertificateEntity c WHERE c.validTo < :now")
    List<CertificateEntity> findExpiredCertificates(@Param("now") LocalDateTime now);
    
    /**
     * 根据主题查找证书
     */
    List<CertificateEntity> findBySubjectContaining(String subject);
    
    /**
     * 根据颁发者查找证书
     */
    List<CertificateEntity> findByIssuerContaining(String issuer);
    
    /**
     * 查找自动信任的证书
     */
    List<CertificateEntity> findByAutoTrustTrue();
    
    /**
     * 统计各状态的证书数量
     */
    @Query("SELECT c.status, COUNT(c) FROM CertificateEntity c GROUP BY c.status")
    List<Object[]> countByStatus();
    
    /**
     * 统计各类型的证书数量
     */
    @Query("SELECT c.type, COUNT(c) FROM CertificateEntity c GROUP BY c.type")
    List<Object[]> countByType();
    
    /**
     * 查找最近创建的证书
     */
    List<CertificateEntity> findTop10ByOrderByCreatedAtDesc();
    
    /**
     * 查找最近修改的证书
     */
    List<CertificateEntity> findTop10ByOrderByLastModifiedDesc();

    /**
     * 查找未删除的证书
     */
    List<CertificateEntity> findByDeletedFalse();

    /**
     * 根据状态查找未删除的证书
     */
    List<CertificateEntity> findByStatusAndDeletedFalse(String status);

    /**
     * 根据类型查找未删除的证书
     */
    List<CertificateEntity> findByTypeAndDeletedFalse(String type);

    /**
     * 查找受信任且未删除的证书
     */
    List<CertificateEntity> findByTrustedTrueAndDeletedFalse();

    /**
     * 根据文件路径查找未删除的证书
     */
    Optional<CertificateEntity> findByFilePathAndDeletedFalse(String filePath);

    /**
     * 根据指纹查找未删除的证书
     */
    Optional<CertificateEntity> findByThumbprintAndDeletedFalse(String thumbprint);
}
