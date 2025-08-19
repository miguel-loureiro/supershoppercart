package com.supershoppercart.controllers;

import com.supershoppercart.auth.dto.DevLoginRequestDTO;
import com.supershoppercart.auth.dto.DevLoginResponseDTO;
import com.supershoppercart.models.Shopper;
import com.supershoppercart.repositories.ShopperRepository;
import com.supershoppercart.services.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
public class DevAuthControllerTest {
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private ShopperRepository shopperRepository;
    @InjectMocks
    private DevAuthController devAuthController;

    private DevLoginRequestDTO validRequest;
    private DevLoginRequestDTO validRequestWithDeviceId;
    private Shopper mockShopper;
    private final String mockAccessToken = "mock-dev-access-token";
    private final String mockRefreshToken = "mock-dev-refresh-token";

    @BeforeEach
    void setUp() {
        // Initialize request and mock shopper
        validRequest = new DevLoginRequestDTO();
        validRequest.setEmail("dev@example.com");
        // Remove password field as it's not used in the controller

        validRequestWithDeviceId = new DevLoginRequestDTO();
        validRequestWithDeviceId.setEmail("dev@example.com");
        validRequestWithDeviceId.setDeviceId("custom-device-id");

        mockShopper = new Shopper("dev@example.com", "Dev User");
        mockShopper.setId("dev-shopper-123");
    }

    @Test
    void devLogin_ExistingUser_ReturnsSuccess() throws Exception {
        // Scenario: Development user already exists
        when(shopperRepository.findByEmail(validRequest.getEmail()))
                .thenReturn(Optional.of(mockShopper));
        when(jwtTokenService.generateAccessToken(anyString(), anyString()))
                .thenReturn(mockAccessToken);
        when(jwtTokenService.generateRefreshToken(anyString()))
                .thenReturn(mockRefreshToken);

        ResponseEntity<DevLoginResponseDTO> response = devAuthController.devLogin(validRequest);

        // Verification:
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(mockAccessToken, response.getBody().getAccessToken());
        assertEquals(mockRefreshToken, response.getBody().getRefreshToken());
        assertNull(response.getBody().getError());

        // Mock verifications: ensure correct methods were called
        verify(shopperRepository, times(1)).findByEmail(validRequest.getEmail());
        verify(shopperRepository, never()).save(any(Shopper.class)); // Save should not be called
        verify(jwtTokenService, times(1)).generateAccessToken(mockShopper.getId(), "dev-device-id");
        verify(jwtTokenService, times(1)).generateRefreshToken(mockShopper.getId());
    }

    @Test
    void devLogin_NewUser_ReturnsSuccess() throws Exception {
        // Scenario: New development user
        when(shopperRepository.findByEmail(validRequest.getEmail()))
                .thenReturn(Optional.empty()); // findByEmail returns empty
        when(shopperRepository.save(any(Shopper.class)))
                .thenReturn(mockShopper); // Returns the saved shopper
        when(jwtTokenService.generateAccessToken(anyString(), anyString()))
                .thenReturn(mockAccessToken);
        when(jwtTokenService.generateRefreshToken(anyString()))
                .thenReturn(mockRefreshToken);

        ResponseEntity<DevLoginResponseDTO> response = devAuthController.devLogin(validRequest);

        // Verification:
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(mockAccessToken, response.getBody().getAccessToken());
        assertEquals(mockRefreshToken, response.getBody().getRefreshToken());
        assertNull(response.getBody().getError());

        // Mock verifications: ensure correct methods were called
        verify(shopperRepository, times(1)).findByEmail(validRequest.getEmail());
        verify(shopperRepository, times(1)).save(any(Shopper.class)); // Save should be called
        verify(jwtTokenService, times(1)).generateAccessToken(mockShopper.getId(), "dev-device-id");
        verify(jwtTokenService, times(1)).generateRefreshToken(mockShopper.getId());
    }

    @Test
    void devLogin_WithCustomDeviceId_UsesProvidedDeviceId() throws Exception {
        // Scenario: Request includes custom device ID
        when(shopperRepository.findByEmail(validRequestWithDeviceId.getEmail()))
                .thenReturn(Optional.of(mockShopper));
        when(jwtTokenService.generateAccessToken(anyString(), anyString()))
                .thenReturn(mockAccessToken);
        when(jwtTokenService.generateRefreshToken(anyString()))
                .thenReturn(mockRefreshToken);

        ResponseEntity<DevLoginResponseDTO> response = devAuthController.devLogin(validRequestWithDeviceId);

        // Verification:
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(mockAccessToken, response.getBody().getAccessToken());
        assertEquals(mockRefreshToken, response.getBody().getRefreshToken());

        // Verify that the custom device ID was used
        verify(jwtTokenService, times(1)).generateAccessToken(mockShopper.getId(), "custom-device-id");
        verify(jwtTokenService, times(1)).generateRefreshToken(mockShopper.getId());
    }

    @Test
    void devLogin_AccessTokenGenerationError_ReturnsInternalServerError() throws Exception {
        // Scenario: Exception occurs during access token generation
        when(shopperRepository.findByEmail(validRequest.getEmail()))
                .thenReturn(Optional.of(mockShopper));
        when(jwtTokenService.generateAccessToken(anyString(), anyString()))
                .thenThrow(new RuntimeException("Access token generation failed"));

        ResponseEntity<DevLoginResponseDTO> response = devAuthController.devLogin(validRequest);

        // Verification:
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().getAccessToken());
        assertNull(response.getBody().getRefreshToken());
        assertEquals("Error: Access token generation failed", response.getBody().getError());

        verify(shopperRepository, times(1)).findByEmail(validRequest.getEmail());
        verify(jwtTokenService, times(1)).generateAccessToken(anyString(), anyString());
        verify(jwtTokenService, never()).generateRefreshToken(anyString());
    }

    @Test
    void devLogin_RefreshTokenGenerationError_ReturnsInternalServerError() throws Exception {
        // Scenario: Exception occurs during refresh token generation
        when(shopperRepository.findByEmail(validRequest.getEmail()))
                .thenReturn(Optional.of(mockShopper));
        when(jwtTokenService.generateAccessToken(anyString(), anyString()))
                .thenReturn(mockAccessToken);
        when(jwtTokenService.generateRefreshToken(anyString()))
                .thenThrow(new RuntimeException("Refresh token generation failed"));

        ResponseEntity<DevLoginResponseDTO> response = devAuthController.devLogin(validRequest);

        // Verification:
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().getAccessToken());
        assertNull(response.getBody().getRefreshToken());
        assertEquals("Error: Refresh token generation failed", response.getBody().getError());

        verify(shopperRepository, times(1)).findByEmail(validRequest.getEmail());
        verify(jwtTokenService, times(1)).generateAccessToken(anyString(), anyString());
        verify(jwtTokenService, times(1)).generateRefreshToken(anyString());
    }

    @Test
    void devLogin_DatabaseError_ReturnsInternalServerError() throws Exception {
        // Scenario: Database error when finding user
        when(shopperRepository.findByEmail(validRequest.getEmail()))
                .thenThrow(new RuntimeException("Database connection failed"));

        ResponseEntity<DevLoginResponseDTO> response = devAuthController.devLogin(validRequest);

        // Verification:
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().getAccessToken());
        assertNull(response.getBody().getRefreshToken());
        assertEquals("Error: Database connection failed", response.getBody().getError());

        verify(shopperRepository, times(1)).findByEmail(validRequest.getEmail());
        verify(shopperRepository, never()).save(any(Shopper.class));
        verify(jwtTokenService, never()).generateAccessToken(anyString(), anyString());
        verify(jwtTokenService, never()).generateRefreshToken(anyString());
    }

    @Test
    void devLogin_SaveNewUserError_ReturnsInternalServerError() throws Exception {
        // Scenario: Error when saving new user
        when(shopperRepository.findByEmail(validRequest.getEmail()))
                .thenReturn(Optional.empty());
        when(shopperRepository.save(any(Shopper.class)))
                .thenThrow(new RuntimeException("Failed to save user"));

        ResponseEntity<DevLoginResponseDTO> response = devAuthController.devLogin(validRequest);

        // Verification:
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().getAccessToken());
        assertNull(response.getBody().getRefreshToken());
        assertEquals("Error: Failed to save user", response.getBody().getError());

        verify(shopperRepository, times(1)).findByEmail(validRequest.getEmail());
        verify(shopperRepository, times(1)).save(any(Shopper.class));
        verify(jwtTokenService, never()).generateAccessToken(anyString(), anyString());
        verify(jwtTokenService, never()).generateRefreshToken(anyString());
    }
}