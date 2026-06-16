package org.go.show.multiproto.opcuaserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OPC UA 证书模型
 */
public class Certificate {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("status")
    private String status; // Trusted, Rejected, Unknown
    
    @JsonProperty("signedBy")
    private String signedBy;
    
    @JsonProperty("validFrom")
    private String validFrom;
    
    @JsonProperty("validTo")
    private String validTo;
    
    @JsonProperty("applicationUri")
    private String applicationUri;
    
    @JsonProperty("keySize")
    private Integer keySize;
    
    @JsonProperty("filename")
    private String filename;
    
    @JsonProperty("version")
    private Integer version;
    
    @JsonProperty("serialNumber")
    private String serialNumber;
    
    @JsonProperty("signatureAlgorithm")
    private String signatureAlgorithm;
    
    @JsonProperty("issuer")
    private String issuer;
    
    @JsonProperty("subject")
    private String subject;
    
    @JsonProperty("subjectAlternativeName")
    private String subjectAlternativeName;
    
    @JsonProperty("thumbprint")
    private String thumbprint;
    
    @JsonProperty("type")
    private String type; // Own Certificate, Client Certificate, etc.
    
    @JsonProperty("trusted")
    private boolean trusted;
    
    @JsonProperty("created")
    private String created;
    
    @JsonProperty("lastModified")
    private String lastModified;
    
    // 构造函数
    public Certificate() {}
    
    public Certificate(Long id, String name, String status, String signedBy, 
                      String validFrom, String validTo, String type) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.signedBy = signedBy;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.type = type;
        this.trusted = "Trusted".equals(status);
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
    }
    
    public String getSignedBy() { return signedBy; }
    public void setSignedBy(String signedBy) { this.signedBy = signedBy; }
    
    public String getValidFrom() { return validFrom; }
    public void setValidFrom(String validFrom) { this.validFrom = validFrom; }
    
    public String getValidTo() { return validTo; }
    public void setValidTo(String validTo) { this.validTo = validTo; }
    
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
    }
    
    public String getCreated() { return created; }
    public void setCreated(String created) { this.created = created; }
    
    public String getLastModified() { return lastModified; }
    public void setLastModified(String lastModified) { this.lastModified = lastModified; }
}
