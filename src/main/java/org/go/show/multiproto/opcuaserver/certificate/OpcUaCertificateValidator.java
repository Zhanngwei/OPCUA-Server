package org.go.show.multiproto.opcuaserver.certificate;

import org.go.show.multiproto.opcuaserver.entity.CertificateEntity;
import org.go.show.multiproto.opcuaserver.service.CertificateService;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.CertificateValidator;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

/**
 * OPC UA 证书验证器
 * 负责验证客户端证书并管理证书状态
 */
@Component
public class OpcUaCertificateValidator implements CertificateValidator {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final CertificateService certificateService;
    
    public OpcUaCertificateValidator(CertificateService certificateService) {
        this.certificateService = certificateService;
    }
    
    @Override
    public void validateCertificateChain(List<X509Certificate> certificateChain, String applicationUri, String[] validHostnames) throws UaException {
        logger.info("*** Certificate validation called ***");
        logger.info("Certificate chain size: {}", certificateChain != null ? certificateChain.size() : 0);
        logger.info("Application URI: {}", applicationUri);
        
        if (certificateChain == null || certificateChain.isEmpty()) {
            logger.warn("Certificate chain is empty");
            throw new UaException(StatusCodes.Bad_CertificateInvalid, "Certificate chain is empty");
        }
        
        // 验证证书链中的第一个证书（客户端证书）
        X509Certificate clientCertificate = certificateChain.get(0);
        logger.info("Validating client certificate: {}", clientCertificate.getSubjectDN().getName());
        
        try {
            // 计算证书指纹
            String thumbprint = certificateService.calculateThumbprint(clientCertificate);
            logger.info("Certificate thumbprint: {}", thumbprint);
            
            // 检查证书是否已存在
            Optional<CertificateEntity> existingCert = certificateService.findByThumbprint(thumbprint);
            
            if (existingCert.isPresent()) {
                CertificateEntity cert = existingCert.get();
                logger.info("Found existing certificate: {} with status: {}", cert.getName(), cert.getStatus());
                
                if (cert.isDeleted()) {
                    logger.warn("Certificate is deleted: {}", cert.getName());
                    throw new UaException(StatusCodes.Bad_CertificateInvalid, "Certificate has been deleted");
                }
                
                if ("Rejected".equals(cert.getStatus())) {
                    logger.warn("Certificate is rejected: {}", cert.getName());
                    throw new UaException(StatusCodes.Bad_CertificateUntrusted, "Certificate is rejected");
                }
                
                if ("Trusted".equals(cert.getStatus())) {
                    logger.info("✅ Certificate is trusted: {}", cert.getName());
                    return; // 允许连接
                }
                
                // 其他状态也拒绝
                logger.warn("Certificate has unknown status: {}", cert.getStatus());
                throw new UaException(StatusCodes.Bad_CertificateInvalid, "Certificate status unknown");
                
            } else {
                // 新证书，自动保存为Rejected状态
                logger.info("New client certificate detected, saving as rejected: {}", clientCertificate.getSubjectDN().getName());
                certificateService.saveNewClientCertificate(clientCertificate);
                throw new UaException(StatusCodes.Bad_CertificateUntrusted, "New certificate must be trusted before use");
            }
            
        } catch (UaException e) {
            logger.error("Certificate validation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error validating certificate: {}", e.getMessage(), e);
            throw new UaException(StatusCodes.Bad_CertificateInvalid, "Certificate validation error: " + e.getMessage());
        }
    }
}
