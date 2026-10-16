package org.example.aispringboot.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.example.aispringboot.config.JwtConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;

/**
 * JWT 工具类
 * 提供 token 的生成、提取、校验、解析能力。由于部分方法被设计为静态方法以便在任意地方调用，
 * 这里通过实现 ApplicationContextAware 来获取 Spring 容器中的 JwtConfig。
 */
@Component
public class JwtTokenUtil implements ApplicationContextAware {
    private static final String ISSUER = "mental-health-assistant";

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        JwtTokenUtil.applicationContext = applicationContext;
    }

    private static JwtConfig getJwtConfig() {
        return applicationContext.getBean(JwtConfig.class);
    }

    // 生成 token
    public static String generateToken(Long userId, String username, Integer roleType) {
        JwtConfig jwtConfig = getJwtConfig();
        Algorithm algorithm = Algorithm.HMAC256(jwtConfig.getSecret());
        Date expiration = new Date(System.currentTimeMillis() + jwtConfig.getExpiration());

        return JWT.create()
                .withClaim("userId", userId)
                .withClaim("username", username)
                .withClaim("roleType", roleType)
                .withExpiresAt(expiration)
                .withIssuedAt(new Date())
                .withIssuer(ISSUER)
                .sign(algorithm);
    }

    // 从请求头提取 token（自定义请求头 "token"，而非标准的 Authorization）
    public static String extractTokenFromRequest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String tokenHeader = request.getHeader("token");
        return StringUtils.hasText(tokenHeader) ? tokenHeader : null;
    }

    // 获取当前请求携带的 token（优先从请求属性读取，其次从请求头读取）
    public static String getCurrentToken() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String token = (String) request.getAttribute("jwtToken");
        if (token != null) {
            return token;
        }
        return extractTokenFromRequest(request);
    }

    /**
     * 获取当前登录用户 ID
     */
    public static Long getCurrentUserId() {
        DecodedJWT jwt = verifyToken(getCurrentToken());
        return jwt.getClaim("userId").asLong();
    }

    // 校验 token 并提取用户信息；无效或过期返回 null
    public static TokenVerificationResult validateToken(String token) {
        try {
            DecodedJWT jwt = verifyToken(token);
            Long userId = jwt.getClaim("userId").asLong();
            String username = jwt.getClaim("username").asString();
            Integer roleType = readRoleType(jwt);

            if (userId != null && StringUtils.hasText(username) && roleType != null) {
                return new TokenVerificationResult(userId, username, roleType, true);
            }
        } catch (JWTVerificationException | IllegalArgumentException e) {
            // token 非法或过期，返回 null 交由调用方处理
        }
        return null;
    }

    private static Integer readRoleType(DecodedJWT jwt) {
        try {
            return jwt.getClaim("roleType").asInt();
        } catch (Exception e) {
            String roleTypeStr = jwt.getClaim("roleType").asString();
            return StringUtils.hasText(roleTypeStr) ? Integer.valueOf(roleTypeStr) : null;
        }
    }

    // 验证 token 签名与有效期，失败抛出 JWTVerificationException
    public static DecodedJWT verifyToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new JWTVerificationException("Token不能为空");
        }
        JwtConfig jwtConfig = getJwtConfig();
        Algorithm algorithm = Algorithm.HMAC256(jwtConfig.getSecret());
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build();
        return verifier.verify(token);
    }

    // Token 验证结果封装类
    @Getter
    public static class TokenVerificationResult {
        private final Long userId;
        private final String username;
        private final Integer roleType;
        private final boolean valid;

        public TokenVerificationResult(Long userId, String username, Integer roleType, boolean valid) {
            this.userId = userId;
            this.username = username;
            this.roleType = roleType;
            this.valid = valid;
        }
    }
}
