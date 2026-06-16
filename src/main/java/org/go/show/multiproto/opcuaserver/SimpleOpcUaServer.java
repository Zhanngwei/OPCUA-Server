package org.go.show.multiproto.opcuaserver;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.milo.opcua.sdk.server.EndpointConfig;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.identity.AbstractIdentityValidator;

import org.go.show.multiproto.opcuaserver.service.AuthMethodService;
import org.go.show.multiproto.opcuaserver.service.CertificateService;
import org.eclipse.milo.opcua.stack.core.types.structured.UserTokenPolicy;
import org.eclipse.milo.opcua.stack.core.types.enumerated.UserTokenType;
import org.eclipse.milo.opcua.sdk.server.util.HostnameUtil;
import org.eclipse.milo.opcua.stack.core.security.DefaultApplicationGroup;
import org.eclipse.milo.opcua.stack.core.security.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.security.DefaultServerCertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.FileBasedCertificateQuarantine;
import org.eclipse.milo.opcua.stack.core.security.FileBasedTrustListManager;
import org.eclipse.milo.opcua.stack.core.security.KeyStoreCertificateStore;
import org.eclipse.milo.opcua.stack.core.security.RsaSha256CertificateFactory;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.transport.TransportProfile;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;
import org.eclipse.milo.opcua.stack.core.util.CertificateUtil;
import org.eclipse.milo.opcua.stack.core.util.NonceUtil;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import java.util.List;
import org.eclipse.milo.opcua.stack.transport.server.tcp.OpcTcpServerTransport;
import org.eclipse.milo.opcua.stack.transport.server.tcp.OpcTcpServerTransportConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.Arrays;

import static org.eclipse.milo.opcua.sdk.server.OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS;

public class SimpleOpcUaServer {

    private static final int TCP_BIND_PORT = 4840;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final OpcUaServer server;
    private final SimpleNamespace simpleNamespace;
    private final AbstractIdentityValidator identityValidator;
    private final AuthMethodService authMethodService;
    private final CertificateService certificateService;

    static {
        // Required for SecurityPolicy.Aes256_Sha256_RsaPss
        Security.addProvider(new BouncyCastleProvider());

        try {
            NonceUtil.blockUntilSecureRandomSeeded(10, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public SimpleOpcUaServer(AbstractIdentityValidator identityValidator, AuthMethodService authMethodService,
                             CertificateService certificateService) throws Exception {
        this.identityValidator = identityValidator;
        this.authMethodService = authMethodService;
        this.certificateService = certificateService;
        Path securityTempDir = Paths.get(System.getProperty("java.io.tmpdir"), "opcua-server", "security");
        Files.createDirectories(securityTempDir);
        if (!Files.exists(securityTempDir)) {
            throw new Exception("unable to create security temp dir: " + securityTempDir);
        }

        File pkiDir = securityTempDir.resolve("pki").toFile();

        logger.info("security dir: {}", securityTempDir.toAbsolutePath());
        logger.info("security pki dir: {}", pkiDir.getAbsolutePath());

        KeyStoreLoader loader = new KeyStoreLoader().load(securityTempDir);

        var certificateStore = KeyStoreCertificateStore.createAndInitialize(
            new KeyStoreCertificateStore.Settings(
                securityTempDir.resolve("example-server.pfx"),
                "password"::toCharArray,
                alias -> "password".toCharArray()));

        var trustListManager = FileBasedTrustListManager.createAndInitialize(pkiDir.toPath());

        var certificateQuarantine = FileBasedCertificateQuarantine.create(
            pkiDir.toPath().resolve("rejected").resolve("certs"));

        var certificateFactory = new RsaSha256CertificateFactory() {
            @Override
            protected KeyPair createRsaSha256KeyPair() {
                return loader.getServerKeyPair();
            }

            @Override
            protected X509Certificate[] createRsaSha256CertificateChain(KeyPair keyPair) {
                return loader.getServerCertificateChain();
            }
        };

        // 创建一个动态证书验证器，实时检查数据库中的证书状态
        var certificateValidator = new DefaultServerCertificateValidator(trustListManager, certificateQuarantine) {
            @Override
            public void validateCertificateChain(
                    List<X509Certificate> certificateChain,
                    String applicationUri,
                    String[] validHostnames) throws UaException {
                logger.debug("*** Dynamic certificate validator: validating client certificate chain with {} certificates ***", certificateChain.size());

                if (!certificateChain.isEmpty()) {
                    X509Certificate clientCert = certificateChain.get(0);
                    logger.debug("*** Validating client certificate: {} ***", clientCert.getSubjectDN().getName());

                    try {
                        // 计算证书指纹
                        String thumbprint = certificateService.calculateThumbprint(clientCert);
                        logger.debug("*** Client certificate thumbprint: {} ***", thumbprint);

                        // 检查数据库中的证书状态
                        var existingCert = certificateService.findByThumbprint(thumbprint);

                        if (existingCert.isPresent()) {
                            var cert = existingCert.get();
                            logger.debug("*** Found existing client certificate: {} with status: {} ***", cert.getName(), cert.getStatus());

                            // 检查证书是否被删除
                            if (cert.isDeleted()) {
                                logger.warn("*** Client certificate is deleted: {} ***", cert.getName());
                                throw new UaException(StatusCodes.Bad_CertificateInvalid, "Certificate has been deleted");
                            }

                            // 检查证书状态
                            if ("Rejected".equals(cert.getStatus())) {
                                logger.warn("*** Client certificate is rejected: {} ***", cert.getName());
                                throw new UaException(StatusCodes.Bad_CertificateInvalid, "Certificate is rejected");
                            }

                            if ("Trusted".equals(cert.getStatus())) {
                                logger.debug("*** Client certificate is trusted: {} ***", cert.getName());
                                // 证书被信任，允许连接
                            } else {
                                logger.warn("*** Client certificate has unknown status: {} ***", cert.getStatus());
                                throw new UaException(StatusCodes.Bad_CertificateInvalid, "Certificate status is unknown");
                            }

                        } else {
                            // 新证书，保存为Rejected状态
                            logger.debug("*** New client certificate detected, saving as rejected ***");
                            certificateService.saveNewClientCertificate(clientCert);
                            logger.warn("*** New client certificate saved as rejected, connection denied ***");
                            throw new UaException(StatusCodes.Bad_CertificateInvalid, "New certificate must be trusted before use");
                        }

                    } catch (UaException e) {
                        // 重新抛出UaException
                        throw e;
                    } catch (Exception e) {
                        logger.error("*** Client certificate validation failed: {} ***", e.getMessage());
                        throw new UaException(StatusCodes.Bad_CertificateInvalid, "Certificate validation error: " + e.getMessage());
                    }
                }

                logger.debug("*** Client certificate validation completed - certificate accepted ***");
            }
        };

        var defaultGroup = DefaultApplicationGroup.createAndInitialize(
            trustListManager, certificateStore, certificateFactory, certificateValidator);

        var certificateManager = new DefaultCertificateManager(certificateQuarantine, defaultGroup);

        X509Certificate certificate = loader.getServerCertificate();

        // The configured application URI must match the one in the certificate(s)
        String applicationUri = CertificateUtil.getSanUri(certificate)
            .orElse("urn:example:simple-opcua-server");

        Set<EndpointConfig> endpointConfigurations = createEndpointConfigs(certificate);

        OpcUaServerConfig serverConfig = OpcUaServerConfig.builder()
            .setApplicationUri(applicationUri)
            .setApplicationName(LocalizedText.english("Simple OPC UA Server"))
            .setEndpoints(endpointConfigurations)
            .setBuildInfo(new BuildInfo(
                "urn:example:simple-opcua-server",
                "Example",
                "Simple OPC UA Server",
                "1.0.0",
                "",
                DateTime.now()))
            .setCertificateManager(certificateManager)
            .setIdentityValidator(identityValidator)
            .setProductUri("urn:example:simple-opcua-server")
            .build();

        server = new OpcUaServer(serverConfig, transportProfile -> {
            assert transportProfile == TransportProfile.TCP_UASC_UABINARY;

            OpcTcpServerTransportConfig transportConfig = OpcTcpServerTransportConfig.newBuilder().build();

            return new OpcTcpServerTransport(transportConfig);
        });

        simpleNamespace = new SimpleNamespace(server);
        simpleNamespace.startup();

        logger.info("Simple OPC UA Server created successfully");
    }

    private Set<EndpointConfig> createEndpointConfigs(X509Certificate certificate) {
        var endpointConfigs = new LinkedHashSet<EndpointConfig>();

        List<String> bindAddresses = List.of("0.0.0.0");

        var hostnames = new LinkedHashSet<String>();
        hostnames.add(HostnameUtil.getHostname());
        hostnames.addAll(HostnameUtil.getHostnames("0.0.0.0", true, false));

        for (String bindAddress : bindAddresses) {
            for (String hostname : hostnames) {
                EndpointConfig.Builder builder = EndpointConfig.newBuilder()
                    .setBindAddress(bindAddress)
                    .setHostname(hostname)
                    .setPath("/milo")
                    .setCertificate(certificate)
                    .addTokenPolicies(createTokenPolicies().toArray(new UserTokenPolicy[0]));

                // 获取认证配置
                var authMethods = authMethodService.getAuthMethods();
                boolean certificateAuth = (Boolean) authMethods.get("certificate");

                // 创建多种安全策略的端点
                createSecurityPolicyEndpoints(builder, endpointConfigs);

                // Discovery endpoint - 总是无安全性
                EndpointConfig.Builder discoveryBuilder = builder.copy()
                    .setPath("/milo/discovery")
                    .setSecurityPolicy(SecurityPolicy.None)
                    .setSecurityMode(MessageSecurityMode.None);

                endpointConfigs.add(buildTcpEndpoint(discoveryBuilder));
            }
        }

        return endpointConfigs;
    }

    private List<UserTokenPolicy> createTokenPolicies() {
        List<UserTokenPolicy> tokenPolicies = new java.util.ArrayList<>();

        logger.debug("*** Creating token policies based on auth configuration ***");

        // 获取认证配置
        var authMethods = authMethodService.getAuthMethods();

        boolean anonymous = (Boolean) authMethods.get("anonymous");
        boolean usernamePassword = (Boolean) authMethods.get("usernamePassword");
        boolean certificate = (Boolean) authMethods.get("certificate");

        logger.debug("*** Auth methods: anonymous={}, usernamePassword={}, certificate={} ***",
                   anonymous, usernamePassword, certificate);

        if (anonymous) {
            logger.debug("*** Adding anonymous token policy ***");
            tokenPolicies.add(USER_TOKEN_POLICY_ANONYMOUS);
        }

        if (usernamePassword) {
            logger.debug("*** Adding username/password token policy ***");
            // 创建自定义的用户名密码策略，使用SecurityPolicy.None（无加密）
            UserTokenPolicy customUsernamePolicy = new UserTokenPolicy(
                "username",
                UserTokenType.UserName,
                null,
                null,
                SecurityPolicy.None.getUri()  // 使用无安全策略，避免密码加密
            );
            tokenPolicies.add(customUsernamePolicy);
            logger.debug("*** Added custom username/password token policy with SecurityPolicy.None ***");
        }

        if (certificate) {
            logger.debug("*** Adding certificate token policy ***");
            tokenPolicies.add(new UserTokenPolicy(
                "certificate",
                UserTokenType.Certificate,
                null,
                null,
                SecurityPolicy.None.getUri()
            ));
        }

        logger.debug("*** Created {} token policies ***", tokenPolicies.size());
        return tokenPolicies;
    }

    /**
     * 创建多种安全策略的端点
     */
    private void createSecurityPolicyEndpoints(EndpointConfig.Builder baseBuilder, Set<EndpointConfig> endpointConfigs) {
        logger.debug("*** Creating endpoints with multiple security policies ***");

        // 定义支持的安全策略和消息安全模式组合
        List<SecurityPolicyConfig> securityConfigs = Arrays.asList(
            // None - 无安全性
            new SecurityPolicyConfig(SecurityPolicy.None, MessageSecurityMode.None),

            // Basic128Rsa15 - 签名
            new SecurityPolicyConfig(SecurityPolicy.Basic128Rsa15, MessageSecurityMode.Sign),

            // Basic256 - 签名
            new SecurityPolicyConfig(SecurityPolicy.Basic256, MessageSecurityMode.Sign),

            // Basic256Sha256 - 签名
            new SecurityPolicyConfig(SecurityPolicy.Basic256Sha256, MessageSecurityMode.Sign),

            // Basic128Rsa15 - 签名和加密
            new SecurityPolicyConfig(SecurityPolicy.Basic128Rsa15, MessageSecurityMode.SignAndEncrypt),

            // Basic256 - 签名和加密
            new SecurityPolicyConfig(SecurityPolicy.Basic256, MessageSecurityMode.SignAndEncrypt),

            // Basic256Sha256 - 签名和加密
            new SecurityPolicyConfig(SecurityPolicy.Basic256Sha256, MessageSecurityMode.SignAndEncrypt),

            // Aes128_Sha256_RsaOaep - 签名和加密
            new SecurityPolicyConfig(SecurityPolicy.Aes128_Sha256_RsaOaep, MessageSecurityMode.SignAndEncrypt),

            // Aes256_Sha256_RsaPss - 签名和加密
            new SecurityPolicyConfig(SecurityPolicy.Aes256_Sha256_RsaPss, MessageSecurityMode.SignAndEncrypt),

            // Aes256_Sha256_RsaPss - 签名
            new SecurityPolicyConfig(SecurityPolicy.Aes256_Sha256_RsaPss, MessageSecurityMode.Sign),

            // Aes128_Sha256_RsaOaep - 签名
            new SecurityPolicyConfig(SecurityPolicy.Aes128_Sha256_RsaOaep, MessageSecurityMode.Sign)
        );

        // 为每种安全策略创建端点
        for (SecurityPolicyConfig config : securityConfigs) {
            try {
                EndpointConfig.Builder endpointBuilder = baseBuilder.copy()
                    .setSecurityPolicy(config.securityPolicy)
                    .setSecurityMode(config.messageSecurityMode);

                endpointConfigs.add(buildTcpEndpoint(endpointBuilder));

                logger.debug("*** Added endpoint: {} with {} ***",
                    config.securityPolicy.getUri(), config.messageSecurityMode);

            } catch (Exception e) {
                logger.warn("*** Failed to create endpoint for {} with {}: {} ***",
                    config.securityPolicy.getUri(), config.messageSecurityMode, e.getMessage());
            }
        }

        logger.debug("*** Created {} security policy endpoints ***", securityConfigs.size());
    }



    /**
     * 安全策略配置类
     */
    private static class SecurityPolicyConfig {
        final SecurityPolicy securityPolicy;
        final MessageSecurityMode messageSecurityMode;

        SecurityPolicyConfig(SecurityPolicy securityPolicy, MessageSecurityMode messageSecurityMode) {
            this.securityPolicy = securityPolicy;
            this.messageSecurityMode = messageSecurityMode;
        }
    }

    public SimpleNamespace getSimpleNamespace() {
        return simpleNamespace;
    }

    private static EndpointConfig buildTcpEndpoint(EndpointConfig.Builder base) {
        return base.copy()
            .setTransportProfile(TransportProfile.TCP_UASC_UABINARY)
            .setBindPort(TCP_BIND_PORT)
            .build();
    }

    public CompletableFuture<OpcUaServer> startup() {
        logger.info("Starting OPC UA Server...");
        return server.startup();
    }

    public CompletableFuture<OpcUaServer> shutdown() {
        logger.info("Shutting down OPC UA Server...");
        simpleNamespace.shutdown();
        return server.shutdown();
    }

    public OpcUaServer getServer() {
        return server;
    }
}
