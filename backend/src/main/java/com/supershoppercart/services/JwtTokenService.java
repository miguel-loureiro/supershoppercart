package com.supershoppercart.services;

import com.supershoppercart.models.Shopper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


@Component
public class JwtTokenService {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenService.class);

    @Value("${jwt.secret}")
    private String secretKeyString;

    @Value("${jwt.access-token.expiration}") // 1 hour default
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration}") // 30 days default
    private long refreshTokenExpiration;

    private SecretKey secretKey;

    @PostConstruct
    void initializeSecretKey() {
        try {
            logger.info("=== JWT Service Initialization Debug ===");
            logger.info("Secret key string: {}", secretKeyString != null ? "[PRESENT]" : "[NULL]");
            logger.info("Secret key length: {}", secretKeyString != null ? secretKeyString.length() : 0);
            logger.info("Access token expiration: {}", accessTokenExpiration);
            logger.info("Refresh token expiration: {}", refreshTokenExpiration);

            if (secretKeyString == null) {
                throw new IllegalStateException("JWT secret is null - check your application.properties");
            }

            if (secretKeyString.trim().isEmpty()) {
                throw new IllegalStateException("JWT secret is empty - check your application.properties");
            }

            byte[] keyBytes = secretKeyString.getBytes(StandardCharsets.UTF_8);
            logger.info("Original key bytes length: {}", keyBytes.length);

            // Ensure the key is at least 256 bits (32 bytes) for HS256
            if (keyBytes.length < 32) {
                logger.warn("JWT secret key is shorter than 32 bytes (256 bits). Padding it to meet HS256 requirements.");
                byte[] paddedKey = new byte[32];
                System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
                // Fill the rest with zeros if needed
                for (int i = keyBytes.length; i < 32; i++) {
                    paddedKey[i] = 0;
                }
                keyBytes = paddedKey; // Use the padded key
                logger.info("Padded key bytes length: {}", keyBytes.length);
            }

            this.secretKey = Keys.hmacShaKeyFor(keyBytes); // Use the potentially padded keyBytes

            logger.info("JWT secret key initialized successfully!");

        } catch (Exception e) {
            logger.error("FAILED to initialize JWT secret key", e);
            logger.error("Exception type: {}", e.getClass().getSimpleName());
            logger.error("Exception message: {}", e.getMessage());
            if (e.getCause() != null) {
                logger.error("Caused by: {}", e.getCause().getMessage());
            }
            throw e;
        }
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    /**
     * Generate access token for shopper
     */
    public String generateAccessToken(Shopper shopper) {
        return generateAccessToken(shopper.getId(), null);
    }

    /**
     * Generate access token with custom claims
     */
    public String generateAccessToken(String shopperId, String deviceId) {

        Map<String, Object> claims = new HashMap<>();
        if (deviceId != null && !deviceId.trim().isEmpty()) {
            claims.put("deviceId", deviceId);
        }

        if (shopperId == null || shopperId.trim().isEmpty()) {
            throw new IllegalArgumentException("Shopper ID cannot be null or empty");
        }

        try {
            return Jwts.builder()
                    .claims(claims)
                    .subject(shopperId)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                    .signWith(secretKey, Jwts.SIG.HS256)
                    .compact();

        } catch (Exception e) {
            logger.error("Failed to generate access token for shopper: {}", shopperId, e);
            throw new RuntimeException("Token generation failed", e);
        }
    }

    /**
     * Generate refresh token for shopper
     */
    public String generateRefreshToken(Shopper shopper) {
        return generateRefreshToken(shopper.getId());
    }

    /**
     * Generate refresh token for shopper ID
     */
    public String generateRefreshToken(String shopperId) {
        if (shopperId == null || shopperId.trim().isEmpty()) {
            throw new IllegalArgumentException("Shopper ID cannot be null or empty");
        }

        try {
            return Jwts.builder()
                    .subject(shopperId)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                    .signWith(secretKey, Jwts.SIG.HS256)
                    .compact();

        } catch (Exception e) {
            logger.error("Failed to generate refresh token for shopper: {}", shopperId, e);
            throw new RuntimeException("Token generation failed", e);
        }
    }

    /**
     * Extract shopper ID from JWT token
     */
    public String extractShopperId(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String shopperId = claims.getSubject();

            if (shopperId == null || shopperId.trim().isEmpty()) {
                logger.warn("JWT subject (shopper ID) is empty or null");
                return null;
            }

            // Validate that it's a valid number format if needed
            // Removed Long.parseLong validation as it's not universally required for shopper IDs
            // and can be handled at a higher layer if specific ID format is enforced.
            return shopperId;

        } catch (JwtException e) {
            logger.debug("Invalid JWT token: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error parsing JWT token", e, e.getCause()); // Log cause
            return null;
        }
    }

    /**
     * Extract all claims from token
     */
    public Claims extractAllClaims(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (JwtException e) {
            logger.debug("Invalid JWT token: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error extracting claims from JWT token", e, e.getCause()); // Log cause
            return null;
        }
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = extractAllClaims(token);
            if (claims != null) {
                Date expiration = claims.getExpiration();
                return expiration != null && expiration.before(new Date());
            }
            return true; // Consider invalid tokens as expired
        } catch (Exception e) {
            logger.debug("Error checking token expiration: {}", e.getMessage());
            return true; // If extraction fails, consider it expired/invalid
        }
    }

    /**
     * Validate token for specific shopper
     */
    public boolean isTokenValid(String token, String shopperId) {
        if (token == null || shopperId == null) {
            return false;
        }

        try {
            String extractedShopperId = extractShopperId(token);
            return extractedShopperId != null &&
                    extractedShopperId.equals(shopperId) &&
                    !isTokenExpired(token);
        } catch (Exception e) {
            logger.debug("Error validating token for shopper {}: {}", shopperId, e.getMessage());
            return false;
        }
    }

    /**
     * Validate if token is valid (not expired)
     */
    public boolean isTokenValid(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        try {
            return extractAllClaims(token) != null && !isTokenExpired(token);
        } catch (Exception e) {
            logger.debug("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get token expiration date
     */
    public Date getExpirationDate(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims != null ? claims.getExpiration() : null;
        } catch (Exception e) {
            logger.debug("Error getting expiration date from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get token issued date
     */
    public Date getIssuedDate(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims != null ? claims.getIssuedAt() : null;
        } catch (Exception e) {
            logger.debug("Error getting issued date from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Generate token pair (access + refresh)
     */
    public TokenPair generateTokenPair(Shopper shopper) {
        String accessToken = generateAccessToken(shopper);
        String refreshToken = generateRefreshToken(shopper);
        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * Token pair container class
     */
    public static class TokenPair {
        private final String accessToken;
        private final String refreshToken;

        public TokenPair(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }
    }
}