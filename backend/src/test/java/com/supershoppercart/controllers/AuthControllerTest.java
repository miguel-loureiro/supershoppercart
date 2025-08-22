package com.supershoppercart.controllers;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.*;
import com.supershoppercart.models.RefreshToken;
import com.supershoppercart.models.Shopper;
import com.supershoppercart.repositories.ShopperRepository;
import com.supershoppercart.security.GoogleTokenVerifier;
import com.supershoppercart.services.AuthService;
import com.supershoppercart.services.JwtTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private final String validIdToken = "valid-google-id-token";
    private final String deviceId = "device-123";

    @Test
    @DisplayName("Login with Google - Missing Authorization header")
    void testLoginWithGoogle_MissingAuthHeader() throws Exception {
        // Given
        CompletableFuture<ResponseEntity<?>> expectedResponse = CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(Map.of("error", "Missing or invalid Authorization header"))
        );
        when(authService.loginWithGoogleAsync(null, deviceId)).thenReturn(expectedResponse);

        // When
        CompletableFuture<ResponseEntity<?>> responseF = authController.loginWithGoogle(null, deviceId);
        ResponseEntity<?> response = responseF.get();

        // Then
        assertEquals(400, response.getStatusCode().value());
        assertEquals(Map.of("error", "Missing or invalid Authorization header"), response.getBody());
        verify(authService).loginWithGoogleAsync(null, deviceId);
    }

    @Test
    @DisplayName("Login with Google - Invalid Authorization header format")
    void testLoginWithGoogle_InvalidAuthHeader() throws Exception {
        // Given
        String invalidAuthHeader = "InvalidFormat " + validIdToken;
        CompletableFuture<ResponseEntity<?>> expectedResponse = CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(Map.of("error", "Missing or invalid Authorization header"))
        );
        when(authService.loginWithGoogleAsync(invalidAuthHeader, deviceId)).thenReturn(expectedResponse);

        // When
        CompletableFuture<ResponseEntity<?>> responseF = authController.loginWithGoogle(invalidAuthHeader, deviceId);
        ResponseEntity<?> response = responseF.get();

        // Then
        assertEquals(400, response.getStatusCode().value());
        assertEquals(Map.of("error", "Missing or invalid Authorization header"), response.getBody());
        verify(authService).loginWithGoogleAsync(invalidAuthHeader, deviceId);
    }

    @Test
    @DisplayName("Login with Google - Missing Device ID")
    void testLoginWithGoogle_MissingDeviceId() throws Exception {
        // Given
        String authHeader = "Bearer " + validIdToken;
        CompletableFuture<ResponseEntity<?>> expectedResponse = CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(Map.of("error", "Missing X-Device-Id header"))
        );
        when(authService.loginWithGoogleAsync(authHeader, null)).thenReturn(expectedResponse);

        // When
        CompletableFuture<ResponseEntity<?>> responseF = authController.loginWithGoogle(authHeader, null);
        ResponseEntity<?> response = responseF.get();

        // Then
        assertEquals(400, response.getStatusCode().value());
        assertEquals(Map.of("error", "Missing X-Device-Id header"), response.getBody());
        verify(authService).loginWithGoogleAsync(authHeader, null);
    }

    @Test
    @DisplayName("Login with Google - Blank Device ID")
    void testLoginWithGoogle_BlankDeviceId() throws Exception {
        // Given
        String authHeader = "Bearer " + validIdToken;
        String blankDeviceId = "";
        CompletableFuture<ResponseEntity<?>> expectedResponse = CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(Map.of("error", "Missing X-Device-Id header"))
        );
        when(authService.loginWithGoogleAsync(authHeader, blankDeviceId)).thenReturn(expectedResponse);

        // When
        CompletableFuture<ResponseEntity<?>> responseF = authController.loginWithGoogle(authHeader, blankDeviceId);
        ResponseEntity<?> response = responseF.get();

        // Then
        assertEquals(400, response.getStatusCode().value());
        assertEquals(Map.of("error", "Missing X-Device-Id header"), response.getBody());
        verify(authService).loginWithGoogleAsync(authHeader, blankDeviceId);
    }

    @Test
    @DisplayName("Login with Google - Invalid Google token")
    void testLoginWithGoogle_InvalidGoogleToken() throws Exception {
        // Given
        String authHeader = "Bearer " + validIdToken;
        CompletableFuture<ResponseEntity<?>> expectedResponse = CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid Google token"))
        );
        when(authService.loginWithGoogleAsync(authHeader, deviceId)).thenReturn(expectedResponse);

        // When
        CompletableFuture<ResponseEntity<?>> responseF = authController.loginWithGoogle(authHeader, deviceId);
        ResponseEntity<?> response = responseF.get();

        // Then
        assertEquals(401, response.getStatusCode().value());
        assertEquals(Map.of("error", "Invalid Google token"), response.getBody());
        verify(authService).loginWithGoogleAsync(authHeader, deviceId);
    }

    @Test
    @DisplayName("Login with Google - Existing shopper success")
    void testLoginWithGoogle_ExistingShopper_Success() throws Exception {
        // Given
        String authHeader = "Bearer " + validIdToken;
        CompletableFuture<ResponseEntity<?>> expectedResponse = CompletableFuture.completedFuture(
                ResponseEntity.ok(Map.of(
                        "accessToken", "access-token",
                        "refreshToken", "refresh-token"
                ))
        );
        when(authService.loginWithGoogleAsync(authHeader, deviceId)).thenReturn(expectedResponse);

        // When
        CompletableFuture<ResponseEntity<?>> responseF = authController.loginWithGoogle(authHeader, deviceId);
        ResponseEntity<?> response = responseF.get();

        // Then
        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertEquals("access-token", body.get("accessToken"));
        assertEquals("refresh-token", body.get("refreshToken"));
        verify(authService).loginWithGoogleAsync(authHeader, deviceId);
    }

    @Test
    @DisplayName("Login with Google - New shopper success")
    void testLoginWithGoogle_NewShopper_Success() throws Exception {
        // Given
        String authHeader = "Bearer " + validIdToken;
        CompletableFuture<ResponseEntity<?>> expectedResponse = CompletableFuture.completedFuture(
                ResponseEntity.ok(Map.of(
                        "accessToken", "access-token",
                        "refreshToken", "refresh-token"
                ))
        );
        when(authService.loginWithGoogleAsync(authHeader, deviceId)).thenReturn(expectedResponse);

        // When
        CompletableFuture<ResponseEntity<?>> responseF = authController.loginWithGoogle(authHeader, deviceId);
        ResponseEntity<?> response = responseF.get();

        // Then
        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertEquals("access-token", body.get("accessToken"));
        assertEquals("refresh-token", body.get("refreshToken"));
        verify(authService).loginWithGoogleAsync(authHeader, deviceId);
    }

    @Test
    @DisplayName("Login with Google - Shopper save fails")
    void testLoginWithGoogle_ShopperSaveFails() throws Exception {
        // Given
        String authHeader = "Bearer " + validIdToken;
        CompletableFuture<ResponseEntity<?>> expectedResponse = CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to create new shopper: Database save failed"))
        );
        when(authService.loginWithGoogleAsync(authHeader, deviceId)).thenReturn(expectedResponse);

        // When
        CompletableFuture<ResponseEntity<?>> responseF = authController.loginWithGoogle(authHeader, deviceId);
        ResponseEntity<?> response = responseF.get();

        // Then
        assertEquals(500, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertTrue(body.get("error").startsWith("Failed to create new shopper:"));
        verify(authService).loginWithGoogleAsync(authHeader, deviceId);
    }

    @Test
    @DisplayName("Login with Google - Firestore operation fails")
    void testLoginWithGoogle_FirestoreFails() throws Exception {
        // Given
        String authHeader = "Bearer " + validIdToken;
        CompletableFuture<ResponseEntity<?>> expectedResponse = CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to complete login: Firestore write failed"))
        );
        when(authService.loginWithGoogleAsync(authHeader, deviceId)).thenReturn(expectedResponse);

        // When
        CompletableFuture<ResponseEntity<?>> responseF = authController.loginWithGoogle(authHeader, deviceId);
        ResponseEntity<?> response = responseF.get();

        // Then
        assertEquals(500, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertTrue(body.get("error").startsWith("Failed to complete login:"));
        verify(authService).loginWithGoogleAsync(authHeader, deviceId);
    }

    @Test
    @DisplayName("Refresh token - Success")
    void testRefreshToken_Success() throws Exception {
        // Given
        Map<String, String> requestBody = Map.of(
                "refreshToken", "refresh-token",
                "deviceId", deviceId
        );
        CompletableFuture<ResponseEntity<?>> expectedResponse = CompletableFuture.completedFuture(
                ResponseEntity.ok(Map.of(
                        "accessToken", "new-access-token",
                        "refreshToken", "new-refresh-token"
                ))
        );
        when(authService.refreshTokenAsync(requestBody)).thenReturn(expectedResponse);

        // When
        CompletableFuture<ResponseEntity<?>> responseF = authController.refreshToken(requestBody);
        ResponseEntity<?> response = responseF.get();

        // Then
        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertEquals("new-access-token", body.get("accessToken"));
        assertEquals("new-refresh-token", body.get("refreshToken"));
        verify(authService).refreshTokenAsync(requestBody);
    }

    @Test
    @DisplayName("Refresh token - Missing parameters")
    void testRefreshToken_MissingParameters() throws Exception {
        // Given
        Map<String, String> requestBody = Map.of("refreshToken", "token");
        CompletableFuture<ResponseEntity<?>> expectedResponse = CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(Map.of("error", "Missing refreshToken or deviceId"))
        );
        when(authService.refreshTokenAsync(requestBody)).thenReturn(expectedResponse);

        // When
        CompletableFuture<ResponseEntity<?>> responseF = authController.refreshToken(requestBody);
        ResponseEntity<?> response = responseF.get();

        // Then
        assertEquals(400, response.getStatusCode().value());
        assertEquals(Map.of("error", "Missing refreshToken or deviceId"), response.getBody());
        verify(authService).refreshTokenAsync(requestBody);
    }

    @Test
    @DisplayName("Refresh token - Token not found")
    void testRefreshToken_TokenNotFound() throws Exception {
        // Given
        Map<String, String> requestBody = Map.of(
                "refreshToken", "invalid-token",
                "deviceId", deviceId
        );
        CompletableFuture<ResponseEntity<?>> expectedResponse = CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid refresh token"))
        );
        when(authService.refreshTokenAsync(requestBody)).thenReturn(expectedResponse);

        // When
        CompletableFuture<ResponseEntity<?>> responseF = authController.refreshToken(requestBody);
        ResponseEntity<?> response = responseF.get();

        // Then
        assertEquals(401, response.getStatusCode().value());
        assertEquals(Map.of("error", "Invalid refresh token"), response.getBody());
        verify(authService).refreshTokenAsync(requestBody);
    }

    @Test
    @DisplayName("Refresh token - Expired token")
    void testRefreshToken_ExpiredToken() throws Exception {
        // Given
        Map<String, String> requestBody = Map.of(
                "refreshToken", "expired-token",
                "deviceId", deviceId
        );
        CompletableFuture<ResponseEntity<?>> expectedResponse = CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Refresh token expired or device mismatch"))
        );
        when(authService.refreshTokenAsync(requestBody)).thenReturn(expectedResponse);

        // When
        CompletableFuture<ResponseEntity<?>> responseF = authController.refreshToken(requestBody);
        ResponseEntity<?> response = responseF.get();

        // Then
        assertEquals(401, response.getStatusCode().value());
        assertEquals(Map.of("error", "Refresh token expired or device mismatch"), response.getBody());
        verify(authService).refreshTokenAsync(requestBody);
    }

    @Test
    @DisplayName("Logout - Success")
    void testLogout_Success() throws Exception {
        // Given
        Map<String, String> requestBody = Map.of(
                "refreshToken", "refresh-token",
                "deviceId", deviceId
        );
        CompletableFuture<ResponseEntity<?>> expectedResponse = CompletableFuture.completedFuture(
                ResponseEntity.ok(Map.of("message", "Logged out from device"))
        );
        when(authService.logoutAsync(requestBody)).thenReturn(expectedResponse);

        // When
        CompletableFuture<ResponseEntity<?>> responseF = authController.logout(requestBody);
        ResponseEntity<?> response = responseF.get();

        // Then
        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("Logged out from device", body.get("message"));
        verify(authService).logoutAsync(requestBody);
    }

    @Test
    @DisplayName("Logout - Invalid request")
    void testLogout_InvalidRequest() throws Exception {
        // Given
        Map<String, String> requestBody = Map.of(
                "refreshToken", "token",
                "deviceId", "wrong-device"
        );
        CompletableFuture<ResponseEntity<?>> expectedResponse = CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid logout request"))
        );
        when(authService.logoutAsync(requestBody)).thenReturn(expectedResponse);

        // When
        CompletableFuture<ResponseEntity<?>> responseF = authController.logout(requestBody);
        ResponseEntity<?> response = responseF.get();

        // Then
        assertEquals(401, response.getStatusCode().value());
        assertEquals(Map.of("error", "Invalid logout request"), response.getBody());
        verify(authService).logoutAsync(requestBody);
    }

    @Test
    @DisplayName("Logout all devices - Success")
    void testLogoutAllDevices_Success() throws Exception {
        // Given
        Map<String, String> requestBody = Map.of("shopperId", "shopper-123");
        CompletableFuture<ResponseEntity<?>> expectedResponse = CompletableFuture.completedFuture(
                ResponseEntity.ok(Map.of("message", "Logged out from all devices"))
        );
        when(authService.logoutAllDevicesAsync(requestBody)).thenReturn(expectedResponse);

        // When
        CompletableFuture<ResponseEntity<?>> responseF = authController.logoutAllDevices(requestBody);
        ResponseEntity<?> response = responseF.get();

        // Then
        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("Logged out from all devices", body.get("message"));
        verify(authService).logoutAllDevicesAsync(requestBody);
    }

    @Test
    @DisplayName("Logout all devices - Missing shopper ID")
    void testLogoutAllDevices_MissingShopperId() throws Exception {
        // Given
        Map<String, String> requestBody = Map.of();
        CompletableFuture<ResponseEntity<?>> expectedResponse = CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(Map.of("error", "Missing shopperId"))
        );
        when(authService.logoutAllDevicesAsync(requestBody)).thenReturn(expectedResponse);

        // When
        CompletableFuture<ResponseEntity<?>> responseF = authController.logoutAllDevices(requestBody);
        ResponseEntity<?> response = responseF.get();

        // Then
        assertEquals(400, response.getStatusCode().value());
        assertEquals(Map.of("error", "Missing shopperId"), response.getBody());
        verify(authService).logoutAllDevicesAsync(requestBody);
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Service throws unexpected exception")
        void testServiceThrowsException() {
            // Given
            String authHeader = "Bearer " + validIdToken;
            when(authService.loginWithGoogleAsync(authHeader, deviceId))
                    .thenThrow(new RuntimeException("Unexpected service error"));

            // When & Then
            assertThrows(RuntimeException.class, () -> {
                authController.loginWithGoogle(authHeader, deviceId);
            });
            verify(authService).loginWithGoogleAsync(authHeader, deviceId);
        }

        @Test
        @DisplayName("Service returns null CompletableFuture")
        void testServiceReturnsNull() throws Exception {
            // Given
            String authHeader = "Bearer " + validIdToken;
            when(authService.loginWithGoogleAsync(authHeader, deviceId)).thenReturn(null);

            // When
            CompletableFuture<ResponseEntity<?>> result = authController.loginWithGoogle(authHeader, deviceId);

            // Then
            assertNull(result);
            verify(authService).loginWithGoogleAsync(authHeader, deviceId);
        }
    }

    @Nested
    @DisplayName("Parameter Validation")
    class ParameterValidation {

        @ParameterizedTest
        @DisplayName("Various invalid auth headers")
        @ValueSource(strings = {"", "Bearer", "Basic token", "Bearer "})
        void testInvalidAuthHeaders(String invalidAuthHeader) throws Exception {
            // Given
            CompletableFuture<ResponseEntity<?>> expectedResponse = CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().body(Map.of("error", "Missing or invalid Authorization header"))
            );
            when(authService.loginWithGoogleAsync(invalidAuthHeader, deviceId)).thenReturn(expectedResponse);

            // When
            CompletableFuture<ResponseEntity<?>> responseF = authController.loginWithGoogle(invalidAuthHeader, deviceId);
            ResponseEntity<?> response = responseF.get();

            // Then
            assertEquals(400, response.getStatusCode().value());
            verify(authService).loginWithGoogleAsync(invalidAuthHeader, deviceId);
        }

        @ParameterizedTest
        @DisplayName("Various invalid device IDs")
        @ValueSource(strings = {"", "   "})
        @NullSource
        void testInvalidDeviceIds(String invalidDeviceId) throws Exception {
            // Given
            String authHeader = "Bearer " + validIdToken;
            CompletableFuture<ResponseEntity<?>> expectedResponse = CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().body(Map.of("error", "Missing X-Device-Id header"))
            );
            when(authService.loginWithGoogleAsync(authHeader, invalidDeviceId)).thenReturn(expectedResponse);

            // When
            CompletableFuture<ResponseEntity<?>> responseF = authController.loginWithGoogle(authHeader, invalidDeviceId);
            ResponseEntity<?> response = responseF.get();

            // Then
            assertEquals(400, response.getStatusCode().value());
            verify(authService).loginWithGoogleAsync(authHeader, invalidDeviceId);
        }
    }
}