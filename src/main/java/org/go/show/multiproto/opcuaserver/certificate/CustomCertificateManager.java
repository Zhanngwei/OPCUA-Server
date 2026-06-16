package org.go.show.multiproto.opcuaserver.certificate;

import org.eclipse.milo.opcua.stack.core.security.CertificateManager;
import org.eclipse.milo.opcua.stack.core.security.CertificateGroup;
import org.eclipse.milo.opcua.stack.core.security.CertificateQuarantine;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.go.show.multiproto.opcuaserver.service.CertificateService;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

public class CustomCertificateManager implements CertificateManager {
    
    private final Logger logger = LoggerFactory.getLogger(CustomCertificateManager.class);
    private final CertificateManager delegate;
    private final CertificateService certificateService;
    private final X509Certificate[] serverCertificateChain;

    public CustomCertificateManager(CertificateManager delegate, CertificateService certificateService, X509Certificate[] serverCertificateChain) {
        this.delegate = delegate;
        this.certificateService = certificateService;
        this.serverCertificateChain = serverCertificateChain;
        logger.info("*** CustomCertificateManager created ***");
    }
    
    @Override
    public Optional<KeyPair> getKeyPair(ByteString thumbprint) {
        logger.info("*** getKeyPair called for thumbprint: {} ***", thumbprint);
        return delegate.getKeyPair(thumbprint);
    }
    
    @Override
    public Optional<X509Certificate> getCertificate(ByteString thumbprint) {
        logger.info("*** getCertificate called for thumbprint: {} ***", thumbprint);
        
        Optional<X509Certificate> cert = delegate.getCertificate(thumbprint);
        if (cert.isPresent()) {
            // 验证证书
            validateCertificate(cert.get());
        }
        
        return cert;
    }
    
    @Override
    public Optional<X509Certificate[]> getCertificateChain(ByteString thumbprint) {
        logger.info("*** getCertificateChain called for thumbprint: {} ***", thumbprint);
        
        Optional<X509Certificate[]> chain = delegate.getCertificateChain(thumbprint);
        if (chain.isPresent() && chain.get().length > 0) {
            // 验证证书链中的第一个证书
            validateCertificate(chain.get()[0]);
        }
        
        return chain;
    }
    
    @Override
    public Optional<CertificateGroup> getCertificateGroup(ByteString thumbprint) {
        logger.info("*** getCertificateGroup called for thumbprint: {} ***", thumbprint);
        return delegate.getCertificateGroup(thumbprint);
    }
    
    @Override
    public Optional<CertificateGroup> getCertificateGroup(NodeId certificateGroupId) {
        logger.info("*** getCertificateGroup called for groupId: {} ***", certificateGroupId);
        return delegate.getCertificateGroup(certificateGroupId);
    }
    
    @Override
    public List<CertificateGroup> getCertificateGroups() {
        logger.info("*** getCertificateGroups called ***");
        return delegate.getCertificateGroups();
    }
    
    @Override
    public CertificateQuarantine getCertificateQuarantine() {
        logger.info("*** getCertificateQuarantine called ***");
        return delegate.getCertificateQuarantine();
    }
    
    private void validateCertificate(X509Certificate certificate) {
        try {
            logger.info("*** Validating certificate: {} ***", certificate.getSubjectDN().getName());

            // 计算证书指纹
            String thumbprint = certificateService.calculateThumbprint(certificate);
            logger.info("*** Certificate thumbprint: {} ***", thumbprint);

            // 检查证书状态
            var existingCert = certificateService.findByThumbprint(thumbprint);

            if (existingCert.isPresent()) {
                var cert = existingCert.get();
                logger.info("*** Found existing certificate: {} with status: {} ***", cert.getName(), cert.getStatus());

                if (cert.isDeleted()) {
                    logger.warn("*** Certificate is deleted: {} ***", cert.getName());
                    // 对于已删除的证书，我们也允许连接，只是记录警告
                    logger.warn("*** Allowing connection despite deleted certificate ***");
                }

                if ("Rejected".equals(cert.getStatus())) {
                    logger.warn("*** Certificate is rejected: {} ***", cert.getName());
                    // 对于被拒绝的证书，我们也允许连接，只是记录警告
                    logger.warn("*** Allowing connection despite rejected certificate ***");
                }

                logger.info("*** Certificate validation completed ***");
            } else {
                // 新证书处理 - 对所有新证书都自动接受
                logger.info("*** New certificate detected, auto-accepting ***");
                try {
                    certificateService.saveNewClientCertificate(certificate);
                    logger.info("*** New certificate saved and accepted ***");
                } catch (Exception e) {
                    logger.warn("*** Failed to save new certificate, but allowing connection: {} ***", e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.error("*** Certificate validation failed: {} ***", e.getMessage());
            // 对于所有验证错误，我们都允许连接，只是记录日志
            logger.warn("*** Certificate validation error, but allowing connection: {} ***", e.getMessage());
        }
    }
}
