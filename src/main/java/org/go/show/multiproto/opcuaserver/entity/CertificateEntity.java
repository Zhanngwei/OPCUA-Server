package org.go.show.multiproto.opcuaserver.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 证书实体类
 */
@Entity
@Table(name = "certificates")
public class CertificateEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "status", nullable = false)
    private String status; // Trusted, Rejected, Unknown
    
    @Column(name = "signed_by")
    private String signedBy;
    
    @Column(name = "valid_from")
    private LocalDateTime validFrom;
    
    @Column(name = "valid_to")
    private LocalDateTime validTo;
    
    @Column(name = "application_uri")
    private String applicationUri;
    
    @Column(name = "key_size")
    private Integer keySize;
    
    @Column(name = "filename")
    private String filename;
    
    @Column(name = "version")
    private Integer version;
    
    @Column(name = "serial_number")
    private String serialNumber;
    
    @Column(name = "signature_algorithm")
    private String signatureAlgorithm;
    
    @Column(name = "issuer", length = 1000)
    private String issuer;
    
    @Column(name = "subject", length = 1000)
    private String subject;
    
    @Column(name = "subject_alternative_name", length = 1000)
    private String subjectAlternativeName;
    
    @Column(name = "thumbprint")
    private String thumbprint;
    
    @Column(name = "type")
    private String type; // Own Certificate, Client Certificate, etc.
    
    @Column(name = "trusted")
    private boolean trusted;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "last_modified")
    private LocalDateTime lastModified;
    
    @Column(name = "certificate_data", columnDefinition = "LONGBLOB")
    private byte[] certificateData; // 存储实际的证书数据
    
    @Column(name = "file_path")
    private String filePath; // 证书文件路径
    
    @Column(name = "auto_trust")
    private boolean autoTrust = false; // 是否自动信任

    @Column(name = "deleted", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean deleted = false; // 是否已删除（软删除）
    
    // 构造函数
    public CertificateEntity() {
        this.createdAt = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { 
        this.status = status;
        this.trusted = "Trusted".equals(status);
        this.lastModified = LocalDateTime.now();
    }
    
    public String getSignedBy() { return signedBy; }
    public void setSignedBy(String signedBy) { this.signedBy = signedBy; }
    
    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }
    
    public LocalDateTime getValidTo() { return validTo; }
    public void setValidTo(LocalDateTime validTo) { this.validTo = validTo; }
    
    public String getApplicationUri() { return applicationUri; }
    public void setApplicationUri(String applicationUri) { this.applicationUri = applicationUri; }
    
    public Integer getKeySize() { return keySize; }
    public void setKeySize(Integer keySize) { this.keySize = keySize; }
    
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    
    public String getSignatureAlgorithm() { return signatureAlgorithm; }
    public void setSignatureAlgorithm(String signatureAlgorithm) { this.signatureAlgorithm = signatureAlgorithm; }
    
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getSubjectAlternativeName() { return subjectAlternativeName; }
    public void setSubjectAlternativeName(String subjectAlternativeName) { this.subjectAlternativeName = subjectAlternativeName; }
    
    public String getThumbprint() { return thumbprint; }
    public void setThumbprint(String thumbprint) { this.thumbprint = thumbprint; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public boolean isTrusted() { return trusted; }
    public void setTrusted(boolean trusted) { 
        this.trusted = trusted;
        this.status = trusted ? "Trusted" : "Rejected";
        this.lastModified = LocalDateTime.now();
    }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
    
    public byte[] getCertificateData() { return certificateData; }
    public void setCertificateData(byte[] certificateData) { this.certificateData = certificateData; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public boolean isAutoTrust() { return autoTrust; }
    public void setAutoTrust(boolean autoTrust) { this.autoTrust = autoTrust; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
        this.lastModified = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.lastModified = LocalDateTime.now();
    }
}
