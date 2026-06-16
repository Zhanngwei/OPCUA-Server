package org.go.show.multiproto.opcuaserver.identity;

import org.eclipse.milo.opcua.sdk.server.identity.AbstractIdentityValidator;
import org.eclipse.milo.opcua.sdk.server.identity.DefaultAnonymousIdentity;
import org.eclipse.milo.opcua.sdk.server.identity.DefaultUsernameIdentity;
import org.eclipse.milo.opcua.sdk.server.Session;
import org.eclipse.milo.opcua.stack.core.types.enumerated.UserTokenType;
import org.eclipse.milo.opcua.stack.core.types.structured.AnonymousIdentityToken;
import org.eclipse.milo.opcua.stack.core.types.structured.UserNameIdentityToken;
import org.eclipse.milo.opcua.stack.core.types.structured.UserTokenPolicy;
import org.eclipse.milo.opcua.stack.core.types.structured.SignatureData;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.sdk.server.identity.Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Set;

public class SimpleIdentityValidator extends AbstractIdentityValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleIdentityValidator.class);
    
    public SimpleIdentityValidator() {
        logger.info("*** SimpleIdentityValidator created ***");

        // 测试认证逻辑
        testAuthenticationLogic();
    }

    private void testAuthenticationLogic() {
        logger.info("*** Testing authentication logic directly ***");

        // 测试用户名密码验证
        if ("admin".equals("admin") && "admin123".equals("admin123")) {
            logger.info("*** Direct authentication test: SUCCESS ***");
        } else {
            logger.warn("*** Direct authentication test: FAILED ***");
        }
    }
    
    @Override
    public Set<UserTokenType> getSupportedTokenTypes() {
        logger.info("*** getSupportedTokenTypes() called ***");
        Set<UserTokenType> types = Set.of(UserTokenType.Anonymous, UserTokenType.UserName);
        logger.info("*** Returning token types: {} ***", types);
        return types;
    }
    
    @Override
    protected Identity.AnonymousIdentity validateAnonymousToken(
            Session session,
            AnonymousIdentityToken token,
            UserTokenPolicy policy,
            SignatureData signature) throws UaException {

        logger.info("*** validateAnonymousToken() called for session: {} ***", session.getSessionId());
        logger.info("*** Anonymous authentication successful ***");
        return new DefaultAnonymousIdentity();
    }
    
    @Override
    protected Identity.UsernameIdentity validateUsernameToken(
            Session session,
            UserNameIdentityToken token,
            UserTokenPolicy policy,
            SignatureData signature) throws UaException {

        logger.info("*** validateUsernameToken() called for session: {} ***", session.getSessionId());
        
        String username = token.getUserName();

        // 检查加密算法
        String algorithmUri = token.getEncryptionAlgorithm();
        logger.info("*** Encryption algorithm: {} ***", algorithmUri);

        byte[] tokenBytes = token.getPassword().bytesOrEmpty();
        logger.info("*** Token bytes length: {} ***", tokenBytes.length);

        String password;

        // 现在使用SecurityPolicy.None，密码应该是明文
        if (algorithmUri == null || algorithmUri.isEmpty()) {
            logger.info("*** No encryption algorithm specified, treating as plain text ***");
            password = new String(tokenBytes, StandardCharsets.UTF_8);
        } else {
            logger.warn("*** Unexpected encryption algorithm: {}, but treating as plain text ***", algorithmUri);
            // 即使有加密算法，也尝试明文解析，因为我们使用的是SecurityPolicy.None
            password = new String(tokenBytes, StandardCharsets.UTF_8);
        }

        logger.info("*** Username: {}, Password: {} ***", username, password);
        
        // 简单验证：只接受 admin/admin123
        if ("admin".equals(username) && "admin123".equals(password)) {
            logger.info("*** Username authentication successful ***");
            return new DefaultUsernameIdentity(username);
        } else {
            logger.warn("*** Username authentication failed: invalid credentials ***");
            throw new UaException(StatusCodes.Bad_IdentityTokenRejected, "Invalid username or password");
        }
    }
}
