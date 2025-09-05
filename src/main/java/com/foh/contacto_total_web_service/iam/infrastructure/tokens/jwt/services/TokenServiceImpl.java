package com.foh.contacto_total_web_service.iam.infrastructure.tokens.jwt.services;

import com.foh.contacto_total_web_service.iam.domain.model.entities.Token;
import com.foh.contacto_total_web_service.iam.infrastructure.persistence.jpa.repositories.TokenRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

@Service
public class TokenServiceImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenServiceImpl.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_TOKEN_BEGIN_INDEX = 7;

    @Value("${authorization.jwt.secret}")
    private String secret;

    @Value("${authorization.jwt.expiration.days}")
    private int expirationDays;

    @Value("${authorization.jwt.refresh.days}")
    private int refreshExpirationDays;

    @Autowired
    private TokenRepository tokenRepository;

    // ========================
    // 🔐 Token Generation
    // ========================
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        String token = createToken(claims, userDetails.getUsername(), expirationDays, "access");

        Token tokenEntity = new Token(
                token,
                userDetails.getUsername(),
                Instant.now(),
                Instant.now().plusSeconds(expirationDays * 24L * 60L * 60L)
        );
        tokenRepository.save(tokenEntity);

        return token;
    }

    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return createToken(claims, username, refreshExpirationDays, "refresh");
    }

    private String createToken(Map<String, Object> claims, String subject, int days, String type) {
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + days * 24L * 60L * 60L * 1000L);

        if (!claims.containsKey("type")) {
            claims.put("type", type);
        }

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    // ========================
    // ✅ Validation
    // ========================
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);

            return tokenRepository.findByToken(token)
                    .map(t -> !t.isRevoked())
                    .orElse(false);
        } catch (JwtException e) {
            LOGGER.error("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername())
                && !isTokenExpired(token)
                && tokenRepository.findByToken(token)
                .map(t -> !t.isRevoked())
                .orElse(false);
    }

    public boolean isRefreshToken(String token) {
        try {
            String type = extractClaim(token, claims -> claims.get("type", String.class));
            return "refresh".equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        try {
            String type = extractClaim(token, claims -> claims.get("type", String.class));
            return "access".equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    // ========================
    // 🧠 Extract Info
    // ========================
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        final Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Map<String, Object> getTokenInfo(String token) {
        Claims claims = extractAllClaims(token);
        Map<String, Object> info = new HashMap<>();
        info.put("username", claims.getSubject());
        info.put("issuedAt", claims.getIssuedAt());
        info.put("expiration", claims.getExpiration());
        info.put("type", Optional.ofNullable(claims.get("type", String.class)).orElse("access"));
        return info;
    }

    // ========================
    // 🔄 Refresh
    // ========================
    public String refreshAccessToken(String refreshToken) {
        if (!validateToken(refreshToken) || !isRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String username = extractUsername(refreshToken);
        UserDetails userDetails = new User(username, "", Collections.emptyList());

        return generateAccessToken(userDetails);
    }

    // ========================
    // 🚪 HTTP Integration
    // ========================
    public String getTokenFromRequest(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_TOKEN_BEGIN_INDEX);
        }
        return null;
    }

    // ========================
    // 🔕 Token revocation
    // ========================
    public void revokeToken(String token) {
        tokenRepository.findByToken(token).ifPresent(tokenToRevoke -> {
            tokenToRevoke.setRevoked(true);
            tokenRepository.save(tokenToRevoke);
        });
    }

    // ========================
    // 🔑 Signing Key
    // ========================
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
