package com.supershoppercart.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.supershoppercart.models.RefreshToken;
import com.supershoppercart.models.Shopper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class JwtTokenService {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenService.class);

    @Value("${jwt.secret}")
    private String secretKeyString;

    @Value("${jwt.access-token.expiration}")
    private long accessTokenExpiration;

    @Getter
    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;

    private SecretKey secretKey;
    private Firestore firestore;

    public JwtTokenService(Firestore firestore) {
        this.firestore = firestore;
    }

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

            // Ensure the key is at least 256 bits (32 bytes) for HS256
            if (keyBytes.length < 32) {
                logger.warn("JWT secret key is shorter than 32 bytes (256 bits). Padding it to meet HS256 requirements.");
                byte[] paddedKey = new byte[32];
                System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
                for (int i = keyBytes.length; i < 32; i++) {
                    paddedKey[i] = 0;
                }
                keyBytes = paddedKey;
            }

            this.secretKey = Keys.hmacShaKeyFor(keyBytes);
            logger.info("JWT secret key initialized successfully!");

        } catch (Exception e) {
            logger.error("FAILED to initialize JWT secret key", e);
            throw e;
        }
    }

    public String generateAccessToken(Shopper shopper) {
        return generateAccessToken(shopper.getId(), null);
    }

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

    public String generateRefreshToken(Shopper shopper) {
        return generateRefreshToken(shopper.getId());
    }

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
            return shopperId;
        } catch (JwtException e) {
            logger.debug("Invalid JWT token: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error parsing JWT token", e);
            return null;
        }
    }

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
            logger.error("Unexpected error extracting claims from JWT token", e);
            return null;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = extractAllClaims(token);
            if (claims != null) {
                Date expiration = claims.getExpiration();
                return expiration != null && expiration.before(new Date());
            }
            return true;
        } catch (Exception e) {
            logger.debug("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

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

    public Date getExpirationDate(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims != null ? claims.getExpiration() : null;
        } catch (Exception e) {
            logger.debug("Error getting expiration date from token: {}", e.getMessage());
            return null;
        }
    }

    public Date getIssuedDate(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims != null ? claims.getIssuedAt() : null;
        } catch (Exception e) {
            logger.debug("Error getting issued date from token: {}", e.getMessage());
            return null;
        }
    }

    public TokenPair generateTokenPair(Shopper shopper) {
        String accessToken = generateAccessToken(shopper);
        String refreshToken = generateRefreshToken(shopper);
        return new TokenPair(accessToken, refreshToken);
    }

    @Getter
    public static class TokenPair {
        private final String accessToken;
        private final String refreshToken;

        public TokenPair(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

    }

    // ==== ASYNC FIRESTORE OPERATIONS ====

    @Async
    public CompletableFuture<Void> saveRefreshTokenAsync(String refreshToken, RefreshToken tokenRecord) {
        return CompletableFuture.runAsync(() -> {
            try {
                firestore.collection("refresh_tokens").document(refreshToken).set(tokenRecord).get();
            } catch (Exception e) {
                throw new RuntimeException("Failed to save refresh token", e);
            }
        });
    }

    @Async
    public CompletableFuture<ResponseEntity<?>> refreshTokenAsync(Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        String deviceId = body.get("deviceId");

        if (refreshToken == null || deviceId == null) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().body(Map.of("error", "Missing refreshToken or deviceId"))
            );
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                DocumentSnapshot doc = firestore.collection("refresh_tokens").document(refreshToken).get().get();
                if (!doc.exists()) {
                    logger.warn("Failed refresh attempt: Invalid refresh token [{}] for device [{}]", refreshToken, deviceId);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid refresh token"));
                }

                RefreshToken stored = doc.toObject(RefreshToken.class);
                if (stored == null || !deviceId.equals(stored.getDeviceId()) || stored.getExpiry() < System.currentTimeMillis()) {
                    logger.warn("Failed refresh attempt: Refresh token expired or device mismatch [{}] [{}]", refreshToken, deviceId);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Refresh token expired or device mismatch"));
                }

                firestore.collection("refresh_tokens").document(refreshToken).delete().get();

                String newAccessToken = generateAccessToken(stored.getShopperId(), deviceId);
                String newRefreshToken = generateRefreshToken(stored.getShopperId());

                RefreshToken rotated = new RefreshToken(
                        newRefreshToken,
                        stored.getShopperId(),
                        deviceId,
                        System.currentTimeMillis() + getRefreshTokenExpiration()
                );

                firestore.collection("refresh_tokens").document(newRefreshToken).set(rotated).get();

                logger.info("Refresh token rotated for shopperId [{}] and device [{}]", stored.getShopperId(), deviceId);

                return ResponseEntity.ok(Map.of(
                        "accessToken", newAccessToken,
                        "refreshToken", newRefreshToken
                ));
            } catch (Exception e) {
                logger.error("Exception during refresh token: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to refresh token: " + e.getMessage()));
            }
        });
    }

    @Async
    public CompletableFuture<ResponseEntity<?>> logoutAsync(Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        String deviceId = body.get("deviceId");

        if (refreshToken == null || deviceId == null) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().body(Map.of("error", "Missing refreshToken or deviceId"))
            );
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                DocumentReference ref = firestore.collection("refresh_tokens").document(refreshToken);
                DocumentSnapshot doc = ref.get().get();

                if (doc.exists()) {
                    RefreshToken stored = doc.toObject(RefreshToken.class);
                    if (stored != null && deviceId.equals(stored.getDeviceId())) {
                        ref.delete().get();
                        logger.info("Logout successful for device [{}] and shopperId [{}]", deviceId, stored.getShopperId());
                        return ResponseEntity.ok(Map.of("message", "Logged out from device"));
                    }
                }

                logger.warn("Invalid logout request for token [{}] and device [{}]", refreshToken, deviceId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid logout request"));
            } catch (Exception e) {
                logger.error("Exception during logout: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to logout: " + e.getMessage()));
            }
        });
    }

    @Async
    public CompletableFuture<ResponseEntity<?>> logoutAllDevicesAsync(Map<String, String> body) {
        String shopperId = body.get("shopperId");

        if (shopperId == null || shopperId.isBlank()) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().body(Map.of("error", "Missing shopperId"))
            );
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                ApiFuture<QuerySnapshot> snapshot = firestore.collection("refresh_tokens")
                        .whereEqualTo("shopperId", shopperId)
                        .get();

                List<QueryDocumentSnapshot> docs = snapshot.get().getDocuments();
                for (DocumentSnapshot doc : docs) {
                    doc.getReference().delete().get();
                }

                logger.info("Logout from all devices for shopperId [{}]", shopperId);
                return ResponseEntity.ok(Map.of("message", "Logged out from all devices"));
            } catch (Exception e) {
                logger.error("Exception during logout-all: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to logout from all devices: " + e.getMessage()));
            }
        });
    }
}