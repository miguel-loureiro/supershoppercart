package com.supershoppercart.controllers;

import com.google.cloud.firestore.*;
import com.supershoppercart.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/google")
    public CompletableFuture<ResponseEntity<?>> loginWithGoogle(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId
    ) {
        return authService.loginWithGoogleAsync(authHeader, deviceId);
    }

    @PostMapping("/refresh")
    public CompletableFuture<ResponseEntity<?>> refreshToken(@RequestBody Map<String, String> body) {
        return authService.refreshTokenAsync(body);
    }

    @PostMapping("/logout")
    public CompletableFuture<ResponseEntity<?>> logout(@RequestBody Map<String, String> body) {
        return authService.logoutAsync(body);
    }

    @PostMapping("/logout-all")
    public CompletableFuture<ResponseEntity<?>> logoutAllDevices(@RequestBody Map<String, String> body) {
        return authService.logoutAllDevicesAsync(body);
    }
}