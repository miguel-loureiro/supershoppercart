package com.supershoppercart.services;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.supershoppercart.models.RefreshToken;
import com.supershoppercart.models.Shopper;
import com.supershoppercart.repositories.ShopperRepository;
import com.supershoppercart.security.GoogleTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    GoogleTokenVerifier googleTokenVerifier;
    @Mock
    ShopperRepository shopperRepository;
    @Mock JwtTokenService jwtTokenService;

    @InjectMocks
    AuthService authService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    final String validIdToken = "valid-google-id-token";
    final String authHeader = "Bearer " + validIdToken;
    final String deviceId = "device-123";
    final String email = "test@example.com";
    final String name = "Test User";
    final String shopperId = "shopper-123";

    @Test
    void testLoginWithGoogleAsync_MissingAuthHeader() throws Exception {
        CompletableFuture<ResponseEntity<?>> responseFuture = authService.loginWithGoogleAsync(null, deviceId);
        ResponseEntity<?> response = responseFuture.get();
        assertEquals(400, response.getStatusCode().value());
        assertEquals(Map.of("error", "Missing or invalid Authorization header"), response.getBody());
    }

    @Test
    void testLoginWithGoogleAsync_InvalidAuthHeader() throws Exception {
        CompletableFuture<ResponseEntity<?>> responseFuture = authService.loginWithGoogleAsync("InvalidFormat " + validIdToken, deviceId);
        ResponseEntity<?> response = responseFuture.get();
        assertEquals(400, response.getStatusCode().value());
        assertEquals(Map.of("error", "Missing or invalid Authorization header"), response.getBody());
    }

    @Test
    void testLoginWithGoogleAsync_MissingDeviceId() throws Exception {
        CompletableFuture<ResponseEntity<?>> responseFuture = authService.loginWithGoogleAsync(authHeader, null);
        ResponseEntity<?> response = responseFuture.get();
        assertEquals(400, response.getStatusCode().value());
        assertEquals(Map.of("error", "Missing X-Device-Id header"), response.getBody());
    }

    @Test
    void testLoginWithGoogleAsync_BlankDeviceId() throws Exception {
        CompletableFuture<ResponseEntity<?>> responseFuture = authService.loginWithGoogleAsync(authHeader, "");
        ResponseEntity<?> response = responseFuture.get();
        assertEquals(400, response.getStatusCode().value());
        assertEquals(Map.of("error", "Missing X-Device-Id header"), response.getBody());
    }

    @Test
    void testLoginWithGoogleAsync_InvalidGoogleToken() throws Exception {
        when(googleTokenVerifier.verifyAsync(validIdToken)).thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<ResponseEntity<?>> responseFuture = authService.loginWithGoogleAsync(authHeader, deviceId);
        ResponseEntity<?> response = responseFuture.get();

        assertEquals(401, response.getStatusCode().value());
        assertEquals(Map.of("error", "Invalid Google token"), response.getBody());
    }

    @Test
    void testLoginWithGoogleAsync_ExistingShopper_Success() throws Exception {
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        when(payload.getEmail()).thenReturn(email);
        when(payload.get("name")).thenReturn(name);

        Shopper existingShopper = new Shopper(email, name);
        existingShopper.setId(shopperId);

        when(googleTokenVerifier.verifyAsync(validIdToken)).thenReturn(CompletableFuture.completedFuture(payload));
        when(shopperRepository.findByEmailAsync(email)).thenReturn(CompletableFuture.completedFuture(Optional.of(existingShopper)));
        when(jwtTokenService.generateAccessToken(shopperId, deviceId)).thenReturn("access-token");
        when(jwtTokenService.generateRefreshToken(shopperId)).thenReturn("refresh-token");
        when(jwtTokenService.getRefreshTokenExpiration()).thenReturn(1000000L);
        when(jwtTokenService.saveRefreshTokenAsync(eq("refresh-token"), any(RefreshToken.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<ResponseEntity<?>> responseFuture = authService.loginWithGoogleAsync(authHeader, deviceId);
        ResponseEntity<?> response = responseFuture.get();

        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("access-token", body.get("accessToken"));
        assertEquals("refresh-token", body.get("refreshToken"));
        verify(shopperRepository, never()).save(any());
    }

    @Test
    void testLoginWithGoogleAsync_NewShopper_Success() throws Exception {
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        when(payload.getEmail()).thenReturn(email);
        when(payload.get("name")).thenReturn(name);

        when(googleTokenVerifier.verifyAsync(validIdToken)).thenReturn(CompletableFuture.completedFuture(payload));
        when(shopperRepository.findByEmailAsync(email)).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        Shopper newShopper = new Shopper(email, name);
        newShopper.setId("new-shopper-id");
        when(shopperRepository.save(any(Shopper.class))).thenReturn(newShopper);

        when(jwtTokenService.generateAccessToken(newShopper.getId(), deviceId)).thenReturn("access-token");
        when(jwtTokenService.generateRefreshToken(newShopper.getId())).thenReturn("refresh-token");
        when(jwtTokenService.getRefreshTokenExpiration()).thenReturn(1000000L);
        when(jwtTokenService.saveRefreshTokenAsync(eq("refresh-token"), any(RefreshToken.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<ResponseEntity<?>> responseFuture = authService.loginWithGoogleAsync(authHeader, deviceId);
        ResponseEntity<?> response = responseFuture.get();

        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("access-token", body.get("accessToken"));
        assertEquals("refresh-token", body.get("refreshToken"));
        verify(shopperRepository).save(any(Shopper.class));
        verify(shopperRepository, never()).deleteById(any());
    }

    @Test
    void testLoginWithGoogleAsync_NewShopper_ShopperSaveFails() throws Exception {
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        when(payload.getEmail()).thenReturn(email);
        when(payload.get("name")).thenReturn(name);

        when(googleTokenVerifier.verifyAsync(validIdToken)).thenReturn(CompletableFuture.completedFuture(payload));
        when(shopperRepository.findByEmailAsync(email)).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(shopperRepository.save(any(Shopper.class))).thenThrow(new RuntimeException("Database save failed"));

        CompletableFuture<ResponseEntity<?>> responseFuture = authService.loginWithGoogleAsync(authHeader, deviceId);
        ResponseEntity<?> response = responseFuture.get();

        assertEquals(500, response.getStatusCode().value());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertTrue(body.get("error").startsWith("Failed to create new shopper:"));
        verify(jwtTokenService, never()).generateAccessToken(any(), any());
        verify(jwtTokenService, never()).generateRefreshToken(anyString());
    }

    @Test
    void testLoginWithGoogleAsync_SaveRefreshTokenFailsAndRollback() throws Exception {
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        when(payload.getEmail()).thenReturn(email);
        when(payload.get("name")).thenReturn(name);

        when(googleTokenVerifier.verifyAsync(validIdToken)).thenReturn(CompletableFuture.completedFuture(payload));
        when(shopperRepository.findByEmailAsync(email)).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        Shopper newShopper = new Shopper(email, name);
        newShopper.setId("new-shopper-id");
        when(shopperRepository.save(any(Shopper.class))).thenReturn(newShopper);

        when(jwtTokenService.generateAccessToken(newShopper.getId(), deviceId)).thenReturn("access-token");
        when(jwtTokenService.generateRefreshToken(newShopper.getId())).thenReturn("refresh-token");
        when(jwtTokenService.getRefreshTokenExpiration()).thenReturn(1000000L);
        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Firestore write failed"));
        when(jwtTokenService.saveRefreshTokenAsync(eq("refresh-token"), any(RefreshToken.class)))
                .thenReturn(failedFuture);

        doNothing().when(shopperRepository).deleteById(newShopper.getId());

        CompletableFuture<ResponseEntity<?>> responseFuture = authService.loginWithGoogleAsync(authHeader, deviceId);
        ResponseEntity<?> response = responseFuture.get();

        assertEquals(500, response.getStatusCode().value());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertTrue(body.get("error").startsWith("Failed to complete login:"));
        verify(shopperRepository).deleteById(newShopper.getId());
    }

    @Test
    void testLoginWithGoogleAsync_SaveRefreshTokenFailsNoRollbackIfExisting() throws Exception {
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        when(payload.getEmail()).thenReturn(email);
        when(payload.get("name")).thenReturn(name);

        Shopper existingShopper = new Shopper(email, name);
        existingShopper.setId(shopperId);

        when(googleTokenVerifier.verifyAsync(validIdToken)).thenReturn(CompletableFuture.completedFuture(payload));
        when(shopperRepository.findByEmailAsync(email)).thenReturn(CompletableFuture.completedFuture(Optional.of(existingShopper)));
        when(jwtTokenService.generateAccessToken(shopperId, deviceId)).thenReturn("access-token");
        when(jwtTokenService.generateRefreshToken(shopperId)).thenReturn("refresh-token");
        when(jwtTokenService.getRefreshTokenExpiration()).thenReturn(1000000L);
        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Firestore write failed"));
        when(jwtTokenService.saveRefreshTokenAsync(eq("refresh-token"), any(RefreshToken.class)))
                .thenReturn(failedFuture);

        CompletableFuture<ResponseEntity<?>> responseFuture = authService.loginWithGoogleAsync(authHeader, deviceId);
        ResponseEntity<?> response = responseFuture.get();

        assertEquals(500, response.getStatusCode().value());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertTrue(body.get("error").startsWith("Failed to complete login:"));
        verify(shopperRepository, never()).deleteById(any());
    }

    @Test
    void testRefreshTokenAsync_DelegatesToJwtTokenService() throws Exception {
        Map<String, String> request = Map.of("refreshToken", "token", "deviceId", deviceId);
        ResponseEntity<?> expected = ResponseEntity.ok(Map.of("accessToken", "a", "refreshToken", "r"));
        when(jwtTokenService.refreshTokenAsync(request)).thenReturn(CompletableFuture.completedFuture(expected));

        CompletableFuture<ResponseEntity<?>> responseFuture = authService.refreshTokenAsync(request);
        ResponseEntity<?> response = responseFuture.get();
        assertEquals(expected, response);
    }

    @Test
    void testLogoutAsync_DelegatesToJwtTokenService() throws Exception {
        Map<String, String> request = Map.of("refreshToken", "token", "deviceId", deviceId);
        ResponseEntity<?> expected = ResponseEntity.ok(Map.of("message", "Logged out from device"));
        when(jwtTokenService.logoutAsync(request)).thenReturn(CompletableFuture.completedFuture(expected));

        CompletableFuture<ResponseEntity<?>> responseFuture = authService.logoutAsync(request);
        ResponseEntity<?> response = responseFuture.get();
        assertEquals(expected, response);
    }

    @Test
    void testLogoutAllDevicesAsync_DelegatesToJwtTokenService() throws Exception {
        Map<String, String> request = Map.of("shopperId", shopperId);
        ResponseEntity<?> expected = ResponseEntity.ok(Map.of("message", "Logged out from all devices"));
        when(jwtTokenService.logoutAllDevicesAsync(request)).thenReturn(CompletableFuture.completedFuture(expected));

        CompletableFuture<ResponseEntity<?>> responseFuture = authService.logoutAllDevicesAsync(request);
        ResponseEntity<?> response = responseFuture.get();
        assertEquals(expected, response);
    }
}