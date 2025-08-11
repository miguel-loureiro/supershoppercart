package com.supershoppercart.security.filter;

import com.supershoppercart.models.Shopper;
import com.supershoppercart.repositories.ShopperRepository;
import com.supershoppercart.services.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the JwtAuthFilter class using JUnit 5 and Mockito.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthFilter Unit Tests")
class JwtAuthFilterTest {

    @Mock
    private ShopperRepository shopperRepository;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    private final String VALID_TOKEN = "valid.jwt.token";
    private final String SHOPPER_ID = "shopper-123";
    private Shopper shopper;

    @BeforeEach
    void setUp() {
        shopper = new Shopper();
        shopper.setId(SHOPPER_ID);
        // Clear security context before each test
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        // Ensure security context is cleared after each test
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should authenticate and set security context for a valid token")
    void doFilterInternal_validToken_shouldAuthenticate() throws ServletException, IOException, ExecutionException, InterruptedException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(jwtTokenService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtTokenService.extractShopperId(VALID_TOKEN)).thenReturn(SHOPPER_ID);
        when(shopperRepository.findById(SHOPPER_ID)).thenReturn(Optional.of(shopper));

        // Act
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate if there is no Authorization header")
    void doFilterInternal_noAuthorizationHeader_shouldNotAuthenticate() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenService, never()).isTokenValid(any());
    }

    @Test
    @DisplayName("Should not authenticate if header is not in 'Bearer' format")
    void doFilterInternal_invalidAuthorizationHeader_shouldNotAuthenticate() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Token " + VALID_TOKEN);

        // Act
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenService, never()).isTokenValid(any());
    }

    @Test
    @DisplayName("Should not authenticate if the token is invalid")
    void doFilterInternal_invalidToken_shouldNotAuthenticate() throws ServletException, IOException, ExecutionException, InterruptedException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(jwtTokenService.isTokenValid(VALID_TOKEN)).thenReturn(false);

        // Act
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate if shopper ID cannot be extracted")
    void doFilterInternal_noShopperId_shouldNotAuthenticate() throws ServletException, IOException, ExecutionException, InterruptedException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(jwtTokenService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtTokenService.extractShopperId(VALID_TOKEN)).thenReturn(null);

        // Act
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(shopperRepository, never()).findById(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate if shopper is not found in the repository")
    void doFilterInternal_shopperNotFound_shouldNotAuthenticate() throws ServletException, IOException, ExecutionException, InterruptedException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(jwtTokenService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtTokenService.extractShopperId(VALID_TOKEN)).thenReturn(SHOPPER_ID);
        when(shopperRepository.findById(SHOPPER_ID)).thenReturn(Optional.empty());

        // Act
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should throw ServletException if token service throws an exception")
    void doFilterInternal_tokenServiceThrowsException_shouldThrowServletException() throws ServletException, IOException, ExecutionException, InterruptedException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(jwtTokenService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtTokenService.extractShopperId(VALID_TOKEN)).thenThrow(new RuntimeException("Token error"));

        // Act & Assert
        assertThrows(ServletException.class, () -> {
            jwtAuthFilter.doFilterInternal(request, response, filterChain);
        });

        // Verify that the filter chain was not called
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Should not re-authenticate if the SecurityContext is already populated")
    void doFilterInternal_alreadyAuthenticated_shouldNotReauthenticate() throws ServletException, IOException, ExecutionException, InterruptedException {
        // Arrange
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("existingUser", null, Collections.emptyList()));
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);

        // Act
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Assert
        // The authentication should remain unchanged
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenService, never()).isTokenValid(any());
    }
}