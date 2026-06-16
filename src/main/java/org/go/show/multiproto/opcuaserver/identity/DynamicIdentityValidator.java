package org.go.show.multiproto.opcuaserver.identity;

import org.go.show.multiproto.opcuaserver.service.AuthMethodService;
import org.go.show.multiproto.opcuaserver.service.UserService;
import org.eclipse.milo.opcua.sdk.server.Session;
import org.eclipse.milo.opcua.sdk.server.identity.AbstractIdentityValidator;
import org.eclipse.milo.opcua.sdk.server.identity.Identity;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.enumerated.UserTokenType;
import org.eclipse.milo.opcua.stack.core.types.structured.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * 动态身份验证器，根据配置的认证方法进行验证
 */
@Component
public class DynamicIdentityValidator extends AbstractIdentityValidator {

    private static final Logger logger = LoggerFactory.getLogger(DynamicIdentityValidator.class);

    @Autowired
    private AuthMethodService authMethodService;

    @Autowired
    private UserService userService;

    public DynamicIdentityValidator() {
        logger.info("DynamicIdentityValidator created");
    }

    @Override
    public Set<UserTokenType> getSupportedTokenTypes() {
        logger.info("getSupportedTokenTypes() called");
        Set<UserTokenType> supportedTypes = new HashSet<>();

        // 根据配置动态添加支持的认证类型
        boolean anonymousEnabled = authMethodService.isAuthMethodEnabled("anonymous");
        boolean usernameEnabled = authMethodService.isAuthMethodEnabled("usernamePassword");
        boolean certificateEnabled = authMethodService.isAuthMethodEnabled("certificate");
        boolean issuedTokenEnabled = authMethodService.isAuthMethodEnabled("issuedToken");

        logger.info("Auth method status: anonymous={}, username={}, certificate={}, issuedToken={}",
                   anonymousEnabled, usernameEnabled, certificateEnabled, issuedTokenEnabled);

        if (anonymousEnabled) {
            supportedTypes.add(UserTokenType.Anonymous);
            logger.info("Added Anonymous token type");
        }
        if (usernameEnabled) {
            supportedTypes.add(UserTokenType.UserName);
            logger.info("Added UserName token type");
        }
        if (certificateEnabled) {
            supportedTypes.add(UserTokenType.Certificate);
            logger.info("Added Certificate token type");
        }
        if (issuedTokenEnabled) {
            supportedTypes.add(UserTokenType.IssuedToken);
            logger.info("Added IssuedToken token type");
        }

        logger.info("Final supported token types: {}", supportedTypes);
        return supportedTypes;
    }

    @Override
    protected Identity.AnonymousIdentity validateAnonymousToken(
            Session session,
            AnonymousIdentityToken token,
            UserTokenPolicy policy,
            SignatureData signature) throws UaException {

        logger.info("*** validateAnonymousToken() called for session: {} ***", session.getSessionId());
        logger.info("*** This is the ANONYMOUS AUTHENTICATION method ***");

        // 检查是否允许匿名访问
        boolean anonymousEnabled = authMethodService.isAuthMethodEnabled("anonymous");

        if (!anonymousEnabled) {
            logger.warn("Anonymous access is disabled but client attempted anonymous login");
            throw new UaException(StatusCodes.Bad_IdentityTokenRejected, "Anonymous access is not allowed");
        }

        logger.info("*** Anonymous user authenticated successfully ***");
        return new DefaultAnonymousIdentity();
    }

    @Override
    protected Identity.UsernameIdentity validateUsernameToken(
            Session session,
            UserNameIdentityToken token,
            UserTokenPolicy policy,
            SignatureData signature) throws UaException {

        logger.info("*** validateUsernameToken() called for session: {} ***", session.getSessionId());
        logger.info("*** This is the USERNAME AUTHENTICATION method ***");

        // 检查是否允许用户名密码认证
        if (!authMethodService.isAuthMethodEnabled("usernamePassword")) {
            logger.warn("Username/password authentication is disabled but client attempted username login");
            throw new UaException(StatusCodes.Bad_IdentityTokenRejected, "Username/password authentication is not allowed");
        }

        String username = token.getUserName();
        logger.debug("Username from token: {}", username);

        if (username == null || username.isEmpty()) {
            logger.warn("Username is null or empty");
            throw new UaException(StatusCodes.Bad_IdentityTokenInvalid, "Username cannot be empty");
        }

        // 解密密码
        String password;
        try {
            password = decryptPassword(session, token);
            logger.debug("Password decrypted successfully for user: {}", username);
        } catch (Exception e) {
            logger.error("Failed to decrypt password for user: {}", username, e);
            throw new UaException(StatusCodes.Bad_IdentityTokenInvalid, "Failed to decrypt password");
        }

        // 验证用户凭据
        boolean isValid = userService.validateCredentials(username, password);
        logger.debug("Credential validation result for user {}: {}", username, isValid);

        if (!isValid) {
            logger.warn("Authentication failed for user: {}", username);
            throw new UaException(StatusCodes.Bad_UserAccessDenied, "Invalid username or password");
        }

        logger.info("User '{}' authenticated successfully", username);
        return new DefaultUsernameIdentity(username);
    }

    // 暂时注释掉证书和颁发令牌认证，因为这些在当前版本的Milo中可能不可用
    // 如果需要，可以在后续版本中实现

    /**
     * 解密用户名密码令牌中的密码
     */
    private String decryptPassword(Session session, UserNameIdentityToken token) throws UaException {
        byte[] tokenBytes = token.getPassword().bytes();
        logger.debug("Password token bytes length: {}", tokenBytes != null ? tokenBytes.length : 0);

        if (tokenBytes == null || tokenBytes.length == 0) {
            logger.warn("Password token bytes is null or empty");
            throw new UaException(StatusCodes.Bad_IdentityTokenInvalid, "Password cannot be empty");
        }

        try {
            // 对于无安全策略的连接，密码通常是明文传输
            // 首先尝试直接解析为UTF-8字符串
            String directPassword = new String(tokenBytes, StandardCharsets.UTF_8);
            logger.debug("Direct password decode attempt: length={}", directPassword.length());

            // 检查是否是有效的UTF-8字符串
            if (isValidPassword(directPassword)) {
                logger.debug("Using direct password decode");
                return directPassword;
            }

            // 如果直接解析失败，尝试长度前缀格式
            if (tokenBytes.length >= 4) {
                int passwordLength = java.nio.ByteBuffer.wrap(tokenBytes, 0, 4).getInt();
                logger.debug("Password length from header: {}", passwordLength);

                if (passwordLength > 0 && passwordLength <= tokenBytes.length - 4) {
                    byte[] passwordBytes = new byte[passwordLength];
                    System.arraycopy(tokenBytes, 4, passwordBytes, 0, passwordLength);
                    String prefixedPassword = new String(passwordBytes, StandardCharsets.UTF_8);
                    logger.debug("Using length-prefixed password decode");
                    return prefixedPassword;
                }
            }

            // 最后尝试直接使用字节数组
            logger.debug("Falling back to direct byte array decode");
            return directPassword;

        } catch (Exception e) {
            logger.error("Failed to decrypt password", e);
            throw new UaException(StatusCodes.Bad_IdentityTokenInvalid, "Failed to decrypt password");
        }
    }

    /**
     * 检查密码是否有效（不包含控制字符等）
     */
    private boolean isValidPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }

        // 检查是否包含过多的控制字符
        long controlCharCount = password.chars()
                .filter(c -> Character.isISOControl(c) && c != '\t' && c != '\n' && c != '\r')
                .count();

        // 如果控制字符超过密码长度的一半，可能不是有效密码
        return controlCharCount < password.length() / 2;
    }

    /**
     * 默认匿名身份实现
     */
    private static class DefaultAnonymousIdentity implements Identity.AnonymousIdentity {
        private Object userData;

        @Override
        public Object getUserData() {
            return userData;
        }

        @Override
        public void setUserData(Object userData) {
            this.userData = userData;
        }

        @Override
        public boolean equalTo(Identity other) {
            return other instanceof Identity.AnonymousIdentity;
        }

        @Override
        public UserTokenType getUserTokenType() {
            return UserTokenType.Anonymous;
        }

        @Override
        public String toString() {
            return "AnonymousIdentity{}";
        }
    }

    /**
     * 默认用户名身份实现
     */
    private static class DefaultUsernameIdentity implements Identity.UsernameIdentity {
        private final String username;
        private Object userData;

        public DefaultUsernameIdentity(String username) {
            this.username = username;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public Object getUserData() {
            return userData;
        }

        @Override
        public void setUserData(Object userData) {
            this.userData = userData;
        }

        @Override
        public boolean equalTo(Identity other) {
            if (other instanceof Identity.UsernameIdentity) {
                return username.equals(((Identity.UsernameIdentity) other).getUsername());
            }
            return false;
        }

        @Override
        public UserTokenType getUserTokenType() {
            return UserTokenType.UserName;
        }

        @Override
        public String toString() {
            return "UsernameIdentity{username='" + username + "'}";
        }
    }
}
