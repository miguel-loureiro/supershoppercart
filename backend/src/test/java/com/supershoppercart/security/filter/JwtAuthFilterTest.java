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
import org.springframework.security.core.Authentication;
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(shopper, auth.getPrincipal());
        assertInstanceOf(UsernamePasswordAuthenticationToken.class, auth);
        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
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
        verify(response, never()).sendError(anyInt(), anyString());
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
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    @DisplayName("Should send unauthorized error if the token is invalid")
    void doFilterInternal_invalidToken_shouldSendUnauthorizedError() throws ServletException, IOException, ExecutionException, InterruptedException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(jwtTokenService.isTokenValid(VALID_TOKEN)).thenReturn(false);

        // Act
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token.");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Should send unauthorized error if shopper ID cannot be extracted")
    void doFilterInternal_noShopperId_shouldSendUnauthorizedError() throws ServletException, IOException, ExecutionException, InterruptedException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(jwtTokenService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtTokenService.extractShopperId(VALID_TOKEN)).thenReturn(null);

        // Act
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token payload.");
        verify(shopperRepository, never()).findById(any());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Should send unauthorized error if shopper is not found in the repository")
    void doFilterInternal_shopperNotFound_shouldSendUnauthorizedError() throws ServletException, IOException, ExecutionException, InterruptedException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(jwtTokenService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtTokenService.extractShopperId(VALID_TOKEN)).thenReturn(SHOPPER_ID);
        when(shopperRepository.findById(SHOPPER_ID)).thenReturn(Optional.empty());

        // Act
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Shopper not found for token.");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Should send unauthorized error if token service throws RuntimeException")
    void doFilterInternal_tokenServiceThrowsRuntimeException_shouldSendUnauthorizedError() throws ServletException, IOException, ExecutionException, InterruptedException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(jwtTokenService.isTokenValid(VALID_TOKEN)).thenThrow(new RuntimeException("Token processing error"));

        // Act
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token processing error.");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not process token if user is already authenticated")
    void doFilterInternal_alreadyAuthenticated_shouldSkipTokenProcessing() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);

        // Set up existing authentication
        Authentication existingAuth = new UsernamePasswordAuthenticationToken("existingUser", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        // Act
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertEquals(existingAuth, SecurityContextHolder.getContext().getAuthentication());
        verify(jwtTokenService, never()).isTokenValid(any());
        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    @DisplayName("Should handle empty Authorization header")
    void doFilterInternal_emptyAuthorizationHeader_shouldNotAuthenticate() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("");

        // Act
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenService, never()).isTokenValid(any());
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    @DisplayName("Should handle Bearer token with only 'Bearer' keyword")
    void doFilterInternal_bearerOnlyHeader_shouldNotAuthenticate() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer");

        // Act
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenService, never()).isTokenValid(any());
        verify(response, never()).sendError(anyInt(), anyString());
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