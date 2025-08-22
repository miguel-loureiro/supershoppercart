package com.supershoppercart.services;

import com.supershoppercart.models.RefreshToken;
import com.supershoppercart.models.Shopper;
import com.supershoppercart.repositories.ShopperRepository;
import com.supershoppercart.security.GoogleTokenVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final GoogleTokenVerifier googleTokenVerifier;
    private final ShopperRepository shopperRepository;
    private final JwtTokenService jwtTokenService;

    public AuthService(
            GoogleTokenVerifier googleTokenVerifier,
            ShopperRepository shopperRepository,
            JwtTokenService jwtTokenService
    ) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.shopperRepository = shopperRepository;
        this.jwtTokenService = jwtTokenService;
    }

    @Async
    public CompletableFuture<ResponseEntity<?>> loginWithGoogleAsync(String authHeader, String deviceId) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().body(Map.of("error", "Missing or invalid Authorization header"))
            );
        }
        if (deviceId == null || deviceId.isBlank()) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().body(Map.of("error", "Missing X-Device-Id header"))
            );
        }

        String idToken = authHeader.substring(7).trim();

        return googleTokenVerifier.verifyAsync(idToken)
                .thenCompose(payload -> {
                    if (payload == null) {
                        logger.warn("Invalid Google token for login attempt");
                        return CompletableFuture.completedFuture(
                                ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid Google token"))
                        );
                    }
                    String email = payload.getEmail();
                    String name = (String) payload.get("name");
                    return shopperRepository.findByEmailAsync(email)
                            .thenCompose(existing -> {
                                Shopper shopper;
                                if (existing.isPresent()) {
                                    shopper = existing.get();
                                } else {
                                    shopper = new Shopper(email, name);
                                    try {
                                        shopper = shopperRepository.save(shopper);
                                    } catch (Exception e) {
                                        logger.error("Failed to create new shopper: {}", e.getMessage());
                                        return CompletableFuture.completedFuture(
                                                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                        .body(Map.of("error", "Failed to create new shopper: " + e.getMessage()))
                                        );
                                    }
                                }
                                String accessToken = jwtTokenService.generateAccessToken(shopper.getId(), deviceId);
                                String refreshToken = jwtTokenService.generateRefreshToken(shopper.getId());
                                RefreshToken tokenRecord = new RefreshToken(
                                        refreshToken,
                                        shopper.getId(),
                                        deviceId,
                                        System.currentTimeMillis() + jwtTokenService.getRefreshTokenExpiration()
                                );
                                try {
                                    jwtTokenService.saveRefreshTokenAsync(refreshToken, tokenRecord).get();
                                } catch (Exception e) {
                                    if (existing.isEmpty()) {
                                        try {
                                            shopperRepository.deleteById(shopper.getId());
                                        } catch (Exception ex) {
                                            logger.error("Failed rollback after Firestore error: {}", ex.getMessage());
                                        }
                                    }
                                    logger.error("Failed to save refresh token: {}", e.getMessage());
                                    return CompletableFuture.completedFuture(
                                            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                    .body(Map.of("error", "Failed to complete login: " + e.getMessage()))
                                    );
                                }
                                return CompletableFuture.completedFuture(
                                        ResponseEntity.ok(Map.of(
                                                "accessToken", accessToken,
                                                "refreshToken", refreshToken
                                        ))
                                );
                            });
                });
    }

    @Async
    public CompletableFuture<ResponseEntity<?>> refreshTokenAsync(Map<String, String> body) {
        return jwtTokenService.refreshTokenAsync(body);
    }

    @Async
    public CompletableFuture<ResponseEntity<?>> logoutAsync(Map<String, String> body) {
        return jwtTokenService.logoutAsync(body);
    }

    @Async
    public CompletableFuture<ResponseEntity<?>> logoutAllDevicesAsync(Map<String, String> body) {
        return jwtTokenService.logoutAllDevicesAsync(body);
    }
}
