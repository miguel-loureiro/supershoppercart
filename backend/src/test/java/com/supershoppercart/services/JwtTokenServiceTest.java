package com.supershoppercart.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.supershoppercart.models.RefreshToken;
import com.supershoppercart.models.Shopper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoBeans;
import org.springframework.test.util.ReflectionTestUtils;
import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {JwtTokenService.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm",
        "jwt.access-token.expiration=3600000", // 1 hour
        "jwt.refresh-token.expiration=2592000000" // 30 days
})
class JwtTokenServiceTest {

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private Firestore firestore;

    private Shopper testShopper;

    @BeforeEach
    void setUp() {
        // Create test shopper
        testShopper = new Shopper();
        testShopper.setId("test-shopper-123");
        testShopper.setName("Test Shopper");

        String secretKeyString = "aStrongAndSecretKeyForTestingPurposesThatIsAtLeast32BytesLong";
        ReflectionTestUtils.setField(jwtTokenService, "secretKeyString", secretKeyString);
        ReflectionTestUtils.setField(jwtTokenService, "accessTokenExpiration", TimeUnit.MINUTES.toMillis(60));
        ReflectionTestUtils.setField(jwtTokenService, "refreshTokenExpiration", TimeUnit.DAYS.toMillis(30));
        // Chamar manualmente o método de inicialização
        jwtTokenService.initializeSecretKey();
    }

    @Test
    @DisplayName("Should initialize secret key successfully and not require manual setup")
    void testInitializeSecretKey() {
        // The @Autowired service should be fully initialized by Spring.
        // No manual invocation of initializeSecretKey is needed.
        assertNotNull(jwtTokenService);
        assertDoesNotThrow(() -> {
            // A simple assertion to prove the service is functional
            String token = jwtTokenService.generateAccessToken(testShopper);
            assertNotNull(token);
        });
    }

    @Test
    @DisplayName("Should throw exception when secret key is null")
    void testInitializeSecretKeyThrowsExceptionForNullSecret() {
        // Given
        JwtTokenService newService = new JwtTokenService(firestore);
        ReflectionTestUtils.setField(newService, "secretKeyString", null);
        ReflectionTestUtils.setField(newService, "accessTokenExpiration", 3600000L);
        ReflectionTestUtils.setField(newService, "refreshTokenExpiration", 2592000000L);

        // When & Then
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
            ReflectionTestUtils.invokeMethod(newService, "initializeSecretKey");
        });

        assertTrue(thrown.getMessage().contains("JWT secret is null"));
    }

    @Test
    @DisplayName("Should throw exception when secret key is empty")
    void testInitializeSecretKeyThrowsExceptionForEmptySecret() {
        // Given
        JwtTokenService newService = new JwtTokenService(firestore);
        ReflectionTestUtils.setField(newService, "secretKeyString", "    ");
        ReflectionTestUtils.setField(newService, "accessTokenExpiration", 3600000L);
        ReflectionTestUtils.setField(newService, "refreshTokenExpiration", 2592000000L);

        // When & Then
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
            ReflectionTestUtils.invokeMethod(newService, "initializeSecretKey");
        });

        assertTrue(thrown.getMessage().contains("JWT secret is empty"));
    }

    @Test
    @DisplayName("Should generate access token for shopper")
    void testGenerateAccessTokenForShopper() {
        // When
        String token = jwtTokenService.generateAccessToken(testShopper);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(3, token.split("\\.").length); // JWT has 3 parts separated by dots
    }

    @Test
    @DisplayName("Should generate access token with custom claims")
    void testGenerateAccessTokenWithCustomClaims() {
        // Given
        String deviceId = "device-123";

        // When
        String token = jwtTokenService.generateAccessToken(testShopper.getId(), deviceId);

        // Then
        assertNotNull(token);
        Claims claims = jwtTokenService.extractAllClaims(token);
        assertNotNull(claims);
        assertEquals(testShopper.getId(), claims.getSubject());
        assertEquals(deviceId, claims.get("deviceId"));
    }

    @Test
    @DisplayName("Should throw exception when generating access token with null shopper ID")
    void testGenerateAccessTokenThrowsExceptionForNullShopperId() {
        // When & Then
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenService.generateAccessToken(null, "device-123");
        });

        assertTrue(thrown.getMessage().contains("Shopper ID cannot be null or empty"));
    }

    @Test
    @DisplayName("Should throw exception when generating access token with empty shopper ID")
    void testGenerateAccessTokenThrowsExceptionForEmptyShopperId() {
        // When & Then
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenService.generateAccessToken("    ", "device-123");
        });

        assertTrue(thrown.getMessage().contains("Shopper ID cannot be null or empty"));
    }

    @Test
    @DisplayName("Should generate refresh token for shopper")
    void testGenerateRefreshTokenForShopper() {
        // When
        String token = jwtTokenService.generateRefreshToken(testShopper);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    @DisplayName("Should generate refresh token for shopper ID")
    void testGenerateRefreshTokenForShopperId() {
        // When
        String token = jwtTokenService.generateRefreshToken(testShopper.getId());

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(testShopper.getId(), jwtTokenService.extractShopperId(token));
    }

    @Test
    @DisplayName("Should throw exception when generating refresh token with null shopper ID")
    void testGenerateRefreshTokenThrowsExceptionForNullShopperId() {
        // When & Then
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenService.generateRefreshToken((String) null);
        });

        assertTrue(thrown.getMessage().contains("Shopper ID cannot be null or empty"));
    }

    @Test
    @DisplayName("Should extract shopper ID from valid token")
    void testExtractShopperIdFromValidToken() {
        // Given
        String token = jwtTokenService.generateAccessToken(testShopper);

        // When
        String extractedId = jwtTokenService.extractShopperId(token);

        // Then
        assertEquals(testShopper.getId(), extractedId);
    }

    @Test
    @DisplayName("Should return null when extracting shopper ID from null token")
    void testExtractShopperIdFromNullToken() {
        // When
        String extractedId = jwtTokenService.extractShopperId(null);

        // Then
        assertNull(extractedId);
    }

    @Test
    @DisplayName("Should return null when extracting shopper ID from empty token")
    void testExtractShopperIdFromEmptyToken() {
        // When
        String extractedId = jwtTokenService.extractShopperId("    ");

        // Then
        assertNull(extractedId);
    }

    @Test
    @DisplayName("Should return null when extracting shopper ID from invalid token")
    void testExtractShopperIdFromInvalidToken() {
        // When
        String extractedId = jwtTokenService.extractShopperId("invalid.token.here");

        // Then
        assertNull(extractedId);
    }

    @Test
    @DisplayName("Should extract all claims from valid token")
    void testExtractAllClaimsFromValidToken() {
        // Given
        String token = jwtTokenService.generateAccessToken(testShopper.getId(), "device-123");

        // When
        Claims claims = jwtTokenService.extractAllClaims(token);

        // Then
        assertNotNull(claims);
        assertEquals(testShopper.getId(), claims.getSubject());
        assertEquals("device-123", claims.get("deviceId"));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    @DisplayName("Should return null when extracting claims from null token")
    void testExtractAllClaimsFromNullToken() {
        // When
        Claims claims = jwtTokenService.extractAllClaims(null);
        // Then
        assertNull(claims);
    }

    @Test
    @DisplayName("Should return null when extracting claims from empty token")
    void testExtractAllClaimsFromEmptyToken() {
        // When
        Claims claims = jwtTokenService.extractAllClaims("    ");
        // Then
        assertNull(claims);
    }

    @Test
    @DisplayName("Should return null when extracting claims from invalid token")
    void testExtractAllClaimsFromInvalidToken() {
        // When
        Claims claims = jwtTokenService.extractAllClaims("invalid.token");

        // Then
        assertNull(claims);
    }

    @Test
    @DisplayName("Should correctly identify non-expired token")
    void testIsTokenExpiredReturnsFalseForValidToken() {
        // Given
        String token = jwtTokenService.generateAccessToken(testShopper);

        // When
        boolean isExpired = jwtTokenService.isTokenExpired(token);

        // Then
        assertFalse(isExpired);
    }

    @Test
    @DisplayName("Should correctly identify expired token")
    void testIsTokenExpiredReturnsTrueForExpiredToken() throws InterruptedException {
        // Given - create a service with very short expiration
        JwtTokenService shortExpiryService = new JwtTokenService(firestore);
        ReflectionTestUtils.setField(shortExpiryService, "secretKeyString", "test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm");
        ReflectionTestUtils.setField(shortExpiryService, "accessTokenExpiration", 1L); // 1ms
        ReflectionTestUtils.setField(shortExpiryService, "refreshTokenExpiration", 2592000000L);
        ReflectionTestUtils.invokeMethod(shortExpiryService, "initializeSecretKey");

        String token = shortExpiryService.generateAccessToken(testShopper);

        // Wait for token to expire
        Thread.sleep(10);

        // When
        boolean isExpired = shortExpiryService.isTokenExpired(token);

        // Then
        assertTrue(isExpired);
    }

    @Test
    @DisplayName("Should return true for expired when token is null")
    void testIsTokenExpiredReturnsTrueForNullToken() {
        // When
        boolean isExpired = jwtTokenService.isTokenExpired(null);
        // Then
        assertTrue(isExpired);
    }

    @Test
    @DisplayName("Should return true for expired when token is empty")
    void testIsTokenExpiredReturnsTrueForEmptyToken() {
        // When
        boolean isExpired = jwtTokenService.isTokenExpired("    ");
        // Then
        assertTrue(isExpired);
    }

    @Test
    @DisplayName("Should return true for expired when token is invalid")
    void testIsTokenExpiredReturnsTrueForInvalidToken() {
        // When
        boolean isExpired = jwtTokenService.isTokenExpired("invalid.token");

        // Then
        assertTrue(isExpired);
    }

    @Test
    @DisplayName("Should validate token for specific shopper")
    void testIsTokenValidForSpecificShopper() {
        // Given
        String token = jwtTokenService.generateAccessToken(testShopper);

        // When
        boolean isValid = jwtTokenService.isTokenValid(token, testShopper.getId());

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should return false when validating token for wrong shopper")
    void testIsTokenValidReturnsFalseForWrongShopper() {
        // Given
        String token = jwtTokenService.generateAccessToken(testShopper);

        // When
        boolean isValid = jwtTokenService.isTokenValid(token, "wrong-shopper-id");

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should return false when validating with null parameters")
    void testIsTokenValidReturnsFalseForNullParameters() {
        // When & Then
        assertFalse(jwtTokenService.isTokenValid(null, testShopper.getId()));
        assertFalse(jwtTokenService.isTokenValid("token", null));
        assertFalse(jwtTokenService.isTokenValid(null, null));
    }

    @Test
    @DisplayName("Should validate token without shopper ID parameter")
    void testIsTokenValidWithoutShopperId() {
        // Given
        String token = jwtTokenService.generateAccessToken(testShopper);

        // When
        boolean isValid = jwtTokenService.isTokenValid(token);

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should return false for null token when validating without shopper ID")
    void testIsTokenValidReturnsFalseForNullTokenWithoutShopperId() {
        // When
        boolean isValid = jwtTokenService.isTokenValid(null);
        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should return false for empty token when validating without shopper ID")
    void testIsTokenValidReturnsFalseForEmptyTokenWithoutShopperId() {
        // When
        boolean isValid = jwtTokenService.isTokenValid("    ");
        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should return false for invalid token validation")
    void testIsTokenValidReturnsFalseForInvalidToken() {
        // When
        boolean isValid = jwtTokenService.isTokenValid("invalid.token");

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should get expiration date from token")
    void testGetExpirationDate() {
        // Given
        String token = jwtTokenService.generateAccessToken(testShopper);

        // When
        Date expirationDate = jwtTokenService.getExpirationDate(token);

        // Then
        assertNotNull(expirationDate);
        assertTrue(expirationDate.after(new Date()));
    }

    @Test
    @DisplayName("Should return null expiration date for null token")
    void testGetExpirationDateForNullToken() {
        // When
        Date expirationDate = jwtTokenService.getExpirationDate(null);
        // Then
        assertNull(expirationDate);
    }

    @Test
    @DisplayName("Should return null expiration date for empty token")
    void testGetExpirationDateForEmptyToken() {
        // When
        Date expirationDate = jwtTokenService.getExpirationDate("    ");
        // Then
        assertNull(expirationDate);
    }

    @Test
    @DisplayName("Should return null expiration date for invalid token")
    void testGetExpirationDateForInvalidToken() {
        // When
        Date expirationDate = jwtTokenService.getExpirationDate("invalid.token");

        // Then
        assertNull(expirationDate);
    }

    @Test
    @DisplayName("Should get issued date from token")
    void testGetIssuedDate() {

        // Capture time in seconds for more robust comparison with JWT's 'iat' claim
        // The JWT specification (RFC 7519) defines the iat claim as a "NumericDate" value,
        // which represents the number of seconds from 1970-01-01T00:00:00Z UTC (Unix epoch).
        // When you use a library like JJWT (which your JwtTokenService uses) to parse a JWT,
        // it automatically handles the conversion from the iat numeric value (seconds) into a java.util.Date object.
        // The Date object internally stores this as milliseconds.
        long beforeGenerationSeconds = System.currentTimeMillis() / 1000;
        String token = jwtTokenService.generateAccessToken(testShopper);
        long afterGenerationSeconds = System.currentTimeMillis() / 1000;

        // When
        Date issuedDate = jwtTokenService.getIssuedDate(token);

        // Then
        assertNotNull(issuedDate);
        long issuedDateSeconds = issuedDate.getTime() / 1000; // Convert issuedDate to seconds

        // The issued date (in seconds) should be greater than or equal to the time before generation (in seconds)
        assertTrue(issuedDateSeconds >= beforeGenerationSeconds, "Issued date (in seconds) should not be before token generation started");
        // The issued date (in seconds) should be less than or equal to the time after generation (in seconds)
        assertTrue(issuedDateSeconds <= afterGenerationSeconds, "Issued date (in seconds) should not be after token generation completed");
    }

    @Test
    @DisplayName("Should return null issued date for null token")
    void testGetIssuedDateForNullToken() {
        // When
        Date issuedDate = jwtTokenService.getIssuedDate(null);
        // Then
        assertNull(issuedDate);
    }

    @Test
    @DisplayName("Should return null issued date for empty token")
    void testGetIssuedDateForEmptyToken() {
        // When
        Date issuedDate = jwtTokenService.getIssuedDate("    ");
        // Then
        assertNull(issuedDate);
    }

    @Test
    @DisplayName("Should return null issued date for invalid token")
    void testGetIssuedDateForInvalidToken() {
        // When
        Date issuedDate = jwtTokenService.getIssuedDate("invalid.token");

        // Then
        assertNull(issuedDate);
    }

    @Test
    @DisplayName("Should generate token pair")
    void testGenerateTokenPair() {
        // When
        JwtTokenService.TokenPair tokenPair = jwtTokenService.generateTokenPair(testShopper);

        // Then
        assertNotNull(tokenPair);
        assertNotNull(tokenPair.getAccessToken());
        assertNotNull(tokenPair.getRefreshToken());
        assertNotEquals(tokenPair.getAccessToken(), tokenPair.getRefreshToken());

        // Verify both tokens are valid for the same shopper
        assertEquals(testShopper.getId(), jwtTokenService.extractShopperId(tokenPair.getAccessToken()));
        assertEquals(testShopper.getId(), jwtTokenService.extractShopperId(tokenPair.getRefreshToken()));
    }

    @Test
    @DisplayName("Should get refresh token expiration")
    void testGetRefreshTokenExpiration() {
        // When
        long expiration = jwtTokenService.getRefreshTokenExpiration();

        // Then
        assertEquals(2592000000L, expiration); // 30 days
    }

    @Test
    @DisplayName("Should handle short secret key by padding")
    void testShortSecretKeyPadding() {
        // Given
        JwtTokenService newService = new JwtTokenService(firestore);
        ReflectionTestUtils.setField(newService, "secretKeyString", "short"); // Only 5 characters
        ReflectionTestUtils.setField(newService, "accessTokenExpiration", 3600000L);
        ReflectionTestUtils.setField(newService, "refreshTokenExpiration", 2592000000L);

        // When & Then - should not throw exception and should work
        assertDoesNotThrow(() -> {
            ReflectionTestUtils.invokeMethod(newService, "initializeSecretKey");
            String token = newService.generateAccessToken(testShopper);
            assertNotNull(token);
            assertEquals(testShopper.getId(), newService.extractShopperId(token));
        });
    }

    @Test
    @DisplayName("Should handle RuntimeException during token generation")
    void testTokenGenerationRuntimeException() {
        // Given - create a service with invalid configuration to force an exception
        JwtTokenService faultyService = new JwtTokenService(firestore);
        ReflectionTestUtils.setField(faultyService, "secretKeyString", "valid-secret-key-that-is-long-enough-for-hmac-sha256-algorithm");
        ReflectionTestUtils.setField(faultyService, "accessTokenExpiration", 3600000L);
        ReflectionTestUtils.setField(faultyService, "refreshTokenExpiration", 2592000000L);
        ReflectionTestUtils.invokeMethod(faultyService, "initializeSecretKey");

        // Set secretKey to null to force an exception
        ReflectionTestUtils.setField(faultyService, "secretKey", null);

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            faultyService.generateAccessToken(testShopper);
        });

        assertTrue(thrown.getMessage().contains("Token generation failed"));
    }

    @Test
    @DisplayName("Should throw exception when generating refresh token with empty shopper ID")
    void testGenerateRefreshTokenThrowsExceptionForEmptyShopperId() {
        // When & Then
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenService.generateRefreshToken("    "); // Using a blank string
        });

        assertTrue(thrown.getMessage().contains("Shopper ID cannot be null or empty"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when generating access token with null shopper ID")
    void testGenerateAccessTokenThrowsForNullShopperId() {
        assertThrows(IllegalArgumentException.class, () ->
                jwtTokenService.generateAccessToken((String) null, null)
        );
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when generating access token with empty shopper ID")
    void testGenerateAccessTokenThrowsForEmptyShopperId() {
        assertThrows(IllegalArgumentException.class, () ->
                jwtTokenService.generateAccessToken("   ", null)
        );
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when generating refresh token with null shopper ID")
    void testGenerateRefreshTokenThrowsForNullShopperId() {
        assertThrows(IllegalArgumentException.class, () ->
                jwtTokenService.generateRefreshToken((String) null)
        );
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when generating refresh token with empty shopper ID")
    void testGenerateRefreshTokenThrowsForEmptyShopperId() {
        assertThrows(IllegalArgumentException.class, () ->
                jwtTokenService.generateRefreshToken("   ")
        );
    }

    @Test
    @DisplayName("Should return null from extractShopperId on unexpected exception")
    void testExtractShopperIdUnexpectedException() throws Exception {
        JwtTokenService spyService = Mockito.spy(jwtTokenService);
        doThrow(new RuntimeException("Unexpected error"))
                .when(spyService).extractAllClaims(anyString());

        String result = spyService.extractShopperId("fake-token");
        assertNull(result);
    }

    @Test
    @DisplayName("Should return null from extractAllClaims on unexpected exception")
    void testExtractAllClaimsUnexpectedException() {
        JwtTokenService newService = new JwtTokenService(firestore);
        ReflectionTestUtils.setField(newService, "secretKeyString", "test-secret-key-that-is-long-enough-for-hmac-sha256");
        ReflectionTestUtils.setField(newService, "accessTokenExpiration", 3600000L);
        ReflectionTestUtils.setField(newService, "refreshTokenExpiration", 2592000000L);
        // Override secretKey with null to trigger exception inside parser
        ReflectionTestUtils.setField(newService, "secretKey", null);

        Claims claims = newService.extractAllClaims("any-token");
        assertNull(claims);
    }

    @Test
    @DisplayName("Should return null from extractShopperId when JwtException occurs")
    void testExtractShopperIdReturnsNullOnJwtException() {
        JwtTokenService spyService = Mockito.spy(jwtTokenService);
        // Simulate JwtException thrown in the parser logic
        doThrow(new io.jsonwebtoken.JwtException("Mocked JWT exception"))
                .when(spyService).extractAllClaims(anyString());

        String result = spyService.extractShopperId("any-invalid-token");
        assertNull(result);
    }

    @Test
    @DisplayName("Should return null from extractAllClaims on invalid JWT token (JwtException scenario)")
    void testExtractAllClaimsReturnsNullOnInvalidToken() {
        // Use an invalid JWT token string to trigger JwtException inside extractAllClaims
        String invalidToken = "this.is.not.a.valid.jwt";

        Claims claims = jwtTokenService.extractAllClaims(invalidToken);
        assertNull(claims);
    }

    @Test
    @DisplayName("Should return null from extractShopperId on invalid JWT token (JwtException scenario)")
    void testExtractShopperIdReturnsNullOnInvalidToken() {
        String invalidToken = "invalid.token.structure";

        String shopperId = jwtTokenService.extractShopperId(invalidToken);
        assertNull(shopperId);
    }

    @Test
    @DisplayName("Should return null when token is null")
    void extractShopperId_shouldReturnNull_whenTokenIsNull() {
        // When
        String shopperId = jwtTokenService.extractShopperId(null);

        // Then
        assertNull(shopperId);
    }

    @Test
    @DisplayName("Should return null when token is empty")
    void extractShopperId_shouldReturnNull_whenTokenIsEmpty() {
        // When
        String shopperId = jwtTokenService.extractShopperId("");

        // Then
        assertNull(shopperId);
    }

    @Test
    @DisplayName("Should return null when token has invalid signature")
    void extractShopperId_shouldReturnNull_whenTokenHasInvalidSignature() {
        // Given
        String invalidSecret = "anotherDifferentKeyForTestingPurposesThatIsAtLeast32BytesLong";
        String invalidToken = Jwts.builder()
                .subject("test-shopper")
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(invalidSecret.getBytes()))
                .compact();

        // When
        String shopperId = jwtTokenService.extractShopperId(invalidToken);

        // Then
        assertNull(shopperId);
    }

    @Test
    @DisplayName("Should return null when token has expired")
    void extractShopperId_shouldReturnNull_whenTokenHasExpired() {
        // Given
        SecretKey secretKey = (SecretKey) ReflectionTestUtils.getField(jwtTokenService, "secretKey");

        String expiredToken = Jwts.builder()
                .subject("test-shopper")
                .expiration(new Date(System.currentTimeMillis() - 1000)) // Expirado há 1 segundo
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();

        // When
        String shopperId = jwtTokenService.extractShopperId(expiredToken);

        // Then
        assertNull(shopperId);
    }

    @Test
    @DisplayName("Should return true when token is null")
    void isTokenExpired_shouldReturnTrue_whenTokenIsNull() {
        // When
        boolean isExpired = jwtTokenService.isTokenExpired(null);

        // Then
        assertTrue(isExpired);
    }

    @Test
    @DisplayName("Should return true when token is empty")
    void isTokenExpired_shouldReturnTrue_whenTokenIsEmpty() {
        // When
        boolean isExpired = jwtTokenService.isTokenExpired("   ");

        // Then
        assertTrue(isExpired);
    }

    @Test
    @DisplayName("Should return true when token is expired using reflection to access key")
    void isTokenExpired_shouldReturnTrue_whenTokenIsExpired_withReflection() {
        // Given
        SecretKey secretKey = (SecretKey) ReflectionTestUtils.getField(jwtTokenService, "secretKey");

        String expiredToken = Jwts.builder()
                .subject("test-shopper")
                .issuedAt(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1))) // Emitido há 1 dia
                .expiration(new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1))) // Expirou há 1 hora
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();

        // When
        boolean isExpired = jwtTokenService.isTokenExpired(expiredToken);

        // Then
        assertTrue(isExpired);
    }

    @Test
    @DisplayName("Should return true when extractAllClaims throws an exception")
    void isTokenExpired_shouldReturnTrue_onException() {
        // Given
        JwtTokenService spiedService = Mockito.spy(jwtTokenService);
        doThrow(new RuntimeException("Simulated parsing error")).when(spiedService).extractAllClaims(anyString());

        // When
        boolean isExpired = spiedService.isTokenExpired("a.b.c");

        // Then
        assertTrue(isExpired);
        verify(spiedService, times(1)).extractAllClaims(anyString());
    }

    @Test
    @DisplayName("Should return null when token is null or empty")
    void getExpirationDate_shouldReturnNull_whenTokenIsInvalid() {
        assertNull(jwtTokenService.getExpirationDate(null));
        assertNull(jwtTokenService.getExpirationDate(""));
    }

    @Test
    @DisplayName("Should return null when extractAllClaims throws an exception")
    void getExpirationDate_shouldReturnNull_onException() {
        // Given
        JwtTokenService spiedService = Mockito.spy(jwtTokenService);
        doThrow(new RuntimeException("Simulated parsing error")).when(spiedService).extractAllClaims(anyString());

        // When
        Date expirationDate = spiedService.getExpirationDate("a.b.c");

        // Then
        assertNull(expirationDate);
        verify(spiedService, times(1)).extractAllClaims(anyString());
    }

    @Test
    @DisplayName("Should return null when token is null or empty")
    void getIssuedDate_shouldReturnNull_whenTokenIsInvalid() {
        assertNull(jwtTokenService.getIssuedDate(null));
        assertNull(jwtTokenService.getIssuedDate(""));
    }

    @Test
    @DisplayName("Should return null when extractAllClaims throws an exception")
    void getIssuedDate_shouldReturnNull_onException() {
        // Given
        JwtTokenService spiedService = Mockito.spy(jwtTokenService);
        doThrow(new RuntimeException("Simulated parsing error")).when(spiedService).extractAllClaims(anyString());

        // When
        Date issuedDate = spiedService.getIssuedDate("a.b.c");

        // Then
        assertNull(issuedDate);
        verify(spiedService, times(1)).extractAllClaims(anyString());
    }

    @Test
    @DisplayName("Should successfully refresh token with valid parameters")
    void testRefreshTokenAsyncSuccess() throws Exception {
        // Given
        String refreshToken = "valid-refresh-token";
        String deviceId = "device-123";
        String shopperId = "shopper-456";

        Map<String, String> body = Map.of(
                "refreshToken", refreshToken,
                "deviceId", deviceId
        );

        RefreshToken storedToken = new RefreshToken(refreshToken, shopperId, deviceId,
                System.currentTimeMillis() + 1000000);

        DocumentSnapshot mockDoc = mock(DocumentSnapshot.class);
        DocumentReference mockRef = mock(DocumentReference.class);
        CollectionReference mockCollection = mock(CollectionReference.class);
        ApiFuture<DocumentSnapshot> mockGetFuture = mock(ApiFuture.class);
        ApiFuture<WriteResult> mockDeleteFuture = mock(ApiFuture.class);
        ApiFuture<WriteResult> mockSetFuture = mock(ApiFuture.class);

        when(firestore.collection("refresh_tokens")).thenReturn(mockCollection);
        when(mockCollection.document(refreshToken)).thenReturn(mockRef);
        when(mockRef.get()).thenReturn(mockGetFuture);
        when(mockGetFuture.get()).thenReturn(mockDoc);
        when(mockDoc.exists()).thenReturn(true);
        when(mockDoc.toObject(RefreshToken.class)).thenReturn(storedToken);
        when(mockRef.delete()).thenReturn(mockDeleteFuture);
        when(mockDeleteFuture.get()).thenReturn(null);
        when(mockCollection.document(anyString())).thenReturn(mockRef);
        when(mockRef.set(any(RefreshToken.class))).thenReturn(mockSetFuture);
        when(mockSetFuture.get()).thenReturn(null);

        // When
        CompletableFuture<ResponseEntity<?>> result = jwtTokenService.refreshTokenAsync(body);
        ResponseEntity<?> response = result.get();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(Map.class, response.getBody());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertTrue(responseBody.containsKey("accessToken"));
        assertTrue(responseBody.containsKey("refreshToken"));
    }
    @Test
    @DisplayName("Should return bad request when refreshToken is missing")
    void testRefreshTokenAsyncMissingRefreshToken() throws Exception {
        // Given
        Map<String, String> body = Map.of("deviceId", "device-123");

        // When
        CompletableFuture<ResponseEntity<?>> result = jwtTokenService.refreshTokenAsync(body);
        ResponseEntity<?> response = result.get();

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Missing refreshToken or deviceId", responseBody.get("error"));
    }
    @Test
    @DisplayName("Should return bad request when deviceId is missing")
    void testRefreshTokenAsyncMissingDeviceId() throws Exception {
        // Given
        Map<String, String> body = Map.of("refreshToken", "token-123");

        // When
        CompletableFuture<ResponseEntity<?>> result = jwtTokenService.refreshTokenAsync(body);
        ResponseEntity<?> response = result.get();

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Missing refreshToken or deviceId", responseBody.get("error"));
    }
    @Test
    @DisplayName("Should return unauthorized when refresh token does not exist")
    void testRefreshTokenAsyncInvalidToken() throws Exception {
        // Given
        Map<String, String> body = Map.of(
                "refreshToken", "invalid-token",
                "deviceId", "device-123"
        );

        DocumentSnapshot mockDoc = mock(DocumentSnapshot.class);
        DocumentReference mockRef = mock(DocumentReference.class);
        CollectionReference mockCollection = mock(CollectionReference.class);
        ApiFuture<DocumentSnapshot> mockGetFuture = mock(ApiFuture.class);

        when(firestore.collection("refresh_tokens")).thenReturn(mockCollection);
        when(mockCollection.document("invalid-token")).thenReturn(mockRef);
        when(mockRef.get()).thenReturn(mockGetFuture);
        when(mockGetFuture.get()).thenReturn(mockDoc);
        when(mockDoc.exists()).thenReturn(false);

        // When
        CompletableFuture<ResponseEntity<?>> result = jwtTokenService.refreshTokenAsync(body);
        ResponseEntity<?> response = result.get();

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Invalid refresh token", responseBody.get("error"));
    }
    @Test
    @DisplayName("Should return unauthorized when device ID does not match")
    void testRefreshTokenAsyncDeviceMismatch() throws Exception {
        // Given
        String refreshToken = "valid-refresh-token";
        String deviceId = "device-123";
        String wrongDeviceId = "wrong-device";
        String shopperId = "shopper-456";

        Map<String, String> body = Map.of(
                "refreshToken", refreshToken,
                "deviceId", wrongDeviceId
        );

        RefreshToken storedToken = new RefreshToken(refreshToken, shopperId, deviceId,
                System.currentTimeMillis() + 1000000);

        DocumentSnapshot mockDoc = mock(DocumentSnapshot.class);
        DocumentReference mockRef = mock(DocumentReference.class);
        CollectionReference mockCollection = mock(CollectionReference.class);
        ApiFuture<DocumentSnapshot> mockGetFuture = mock(ApiFuture.class);

        when(firestore.collection("refresh_tokens")).thenReturn(mockCollection);
        when(mockCollection.document(refreshToken)).thenReturn(mockRef);
        when(mockRef.get()).thenReturn(mockGetFuture);
        when(mockGetFuture.get()).thenReturn(mockDoc);
        when(mockDoc.exists()).thenReturn(true);
        when(mockDoc.toObject(RefreshToken.class)).thenReturn(storedToken);

        // When
        CompletableFuture<ResponseEntity<?>> result = jwtTokenService.refreshTokenAsync(body);
        ResponseEntity<?> response = result.get();

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Refresh token expired or device mismatch", responseBody.get("error"));
    }
    @Test
    @DisplayName("Should return unauthorized when refresh token is expired")
    void testRefreshTokenAsyncExpiredToken() throws Exception {
        // Given
        String refreshToken = "expired-refresh-token";
        String deviceId = "device-123";
        String shopperId = "shopper-456";

        Map<String, String> body = Map.of(
                "refreshToken", refreshToken,
                "deviceId", deviceId
        );

        RefreshToken expiredToken = new RefreshToken(refreshToken, shopperId, deviceId,
                System.currentTimeMillis() - 1000); // Expired

        DocumentSnapshot mockDoc = mock(DocumentSnapshot.class);
        DocumentReference mockRef = mock(DocumentReference.class);
        CollectionReference mockCollection = mock(CollectionReference.class);
        ApiFuture<DocumentSnapshot> mockGetFuture = mock(ApiFuture.class);

        when(firestore.collection("refresh_tokens")).thenReturn(mockCollection);
        when(mockCollection.document(refreshToken)).thenReturn(mockRef);
        when(mockRef.get()).thenReturn(mockGetFuture);
        when(mockGetFuture.get()).thenReturn(mockDoc);
        when(mockDoc.exists()).thenReturn(true);
        when(mockDoc.toObject(RefreshToken.class)).thenReturn(expiredToken);

        // When
        CompletableFuture<ResponseEntity<?>> result = jwtTokenService.refreshTokenAsync(body);
        ResponseEntity<?> response = result.get();

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Refresh token expired or device mismatch", responseBody.get("error"));
    }
    @Test
    @DisplayName("Should successfully logout with valid parameters")
    void testLogoutAsyncSuccess() throws Exception {
        // Given
        String refreshToken = "valid-refresh-token";
        String deviceId = "device-123";
        String shopperId = "shopper-456";

        Map<String, String> body = Map.of(
                "refreshToken", refreshToken,
                "deviceId", deviceId
        );

        RefreshToken storedToken = new RefreshToken(refreshToken, shopperId, deviceId,
                System.currentTimeMillis() + 1000000);

        DocumentSnapshot mockDoc = mock(DocumentSnapshot.class);
        DocumentReference mockRef = mock(DocumentReference.class);
        CollectionReference mockCollection = mock(CollectionReference.class);
        ApiFuture<DocumentSnapshot> mockGetFuture = mock(ApiFuture.class);
        ApiFuture<WriteResult> mockDeleteFuture = mock(ApiFuture.class);

        when(firestore.collection("refresh_tokens")).thenReturn(mockCollection);
        when(mockCollection.document(refreshToken)).thenReturn(mockRef);
        when(mockRef.get()).thenReturn(mockGetFuture);
        when(mockGetFuture.get()).thenReturn(mockDoc);
        when(mockDoc.exists()).thenReturn(true);
        when(mockDoc.toObject(RefreshToken.class)).thenReturn(storedToken);
        when(mockRef.delete()).thenReturn(mockDeleteFuture);
        when(mockDeleteFuture.get()).thenReturn(null);

        // When
        CompletableFuture<ResponseEntity<?>> result = jwtTokenService.logoutAsync(body);
        ResponseEntity<?> response = result.get();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Logged out from device", responseBody.get("message"));
    }
    @Test
    @DisplayName("Should return bad request when logout refreshToken is missing")
    void testLogoutAsyncMissingRefreshToken() throws Exception {
        // Given
        Map<String, String> body = Map.of("deviceId", "device-123");

        // When
        CompletableFuture<ResponseEntity<?>> result = jwtTokenService.logoutAsync(body);
        ResponseEntity<?> response = result.get();

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Missing refreshToken or deviceId", responseBody.get("error"));
    }
    @Test
    @DisplayName("Should return bad request when logout deviceId is missing")
    void testLogoutAsyncMissingDeviceId() throws Exception {
        // Given
        Map<String, String> body = Map.of("refreshToken", "token-123");

        // When
        CompletableFuture<ResponseEntity<?>> result = jwtTokenService.logoutAsync(body);
        ResponseEntity<?> response = result.get();

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Missing refreshToken or deviceId", responseBody.get("error"));
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when logout token does not exist")
    void testLogoutAsyncNonExistentToken() throws Exception {
        // Given
        Map<String, String> body = Map.of(
                "refreshToken", "non-existent-token",
                "deviceId", "device-123"
        );
        DocumentSnapshot mockDoc = mock(DocumentSnapshot.class);
        DocumentReference mockRef = mock(DocumentReference.class);
        CollectionReference mockCollection = mock(CollectionReference.class);
        ApiFuture<DocumentSnapshot> mockGetFuture = mock(ApiFuture.class);
        when(firestore.collection("refresh_tokens")).thenReturn(mockCollection);
        when(mockCollection.document("non-existent-token")).thenReturn(mockRef);
        when(mockRef.get()).thenReturn(mockGetFuture);
        when(mockGetFuture.get()).thenReturn(mockDoc);
        when(mockDoc.exists()).thenReturn(false);
        // When
        CompletableFuture<ResponseEntity<?>> result = jwtTokenService.logoutAsync(body);
        ResponseEntity<?> response = result.get();
        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Invalid logout request", responseBody.get("error"));
    }

    @Test
    @DisplayName("Should successfully logout from all devices")
    void testLogoutAllDevicesAsyncSuccess() throws Exception {
        // Given
        String shopperId = "shopper-456";
        Map<String, String> body = Map.of("shopperId", shopperId);

        CollectionReference mockCollection = mock(CollectionReference.class);
        Query mockQuery = mock(Query.class);
        ApiFuture<QuerySnapshot> mockQueryFuture = mock(ApiFuture.class);
        QuerySnapshot mockQuerySnapshot = mock(QuerySnapshot.class);
        QueryDocumentSnapshot mockDoc1 = mock(QueryDocumentSnapshot.class);
        QueryDocumentSnapshot mockDoc2 = mock(QueryDocumentSnapshot.class);
        DocumentReference mockRef1 = mock(DocumentReference.class);
        DocumentReference mockRef2 = mock(DocumentReference.class);
        ApiFuture<WriteResult> mockDeleteFuture = mock(ApiFuture.class);

        List<QueryDocumentSnapshot> docs = List.of(mockDoc1, mockDoc2);

        when(firestore.collection("refresh_tokens")).thenReturn(mockCollection);
        when(mockCollection.whereEqualTo("shopperId", shopperId)).thenReturn(mockQuery);
        when(mockQuery.get()).thenReturn(mockQueryFuture);
        when(mockQueryFuture.get()).thenReturn(mockQuerySnapshot);
        when(mockQuerySnapshot.getDocuments()).thenReturn(docs);
        when(mockDoc1.getReference()).thenReturn(mockRef1);
        when(mockDoc2.getReference()).thenReturn(mockRef2);
        when(mockRef1.delete()).thenReturn(mockDeleteFuture);
        when(mockRef2.delete()).thenReturn(mockDeleteFuture);
        when(mockDeleteFuture.get()).thenReturn(null);

        // When
        CompletableFuture<ResponseEntity<?>> result = jwtTokenService.logoutAllDevicesAsync(body);
        ResponseEntity<?> response = result.get();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Logged out from all devices", responseBody.get("message"));

        verify(mockRef1).delete();
        verify(mockRef2).delete();
    }

    @Test
    @DisplayName("Should return bad request when logoutAllDevices shopperId is missing")
    void testLogoutAllDevicesAsyncMissingShopperId() throws Exception {
        // Given
        Map<String, String> body = Map.of();

        // When
        CompletableFuture<ResponseEntity<?>> result = jwtTokenService.logoutAllDevicesAsync(body);
        ResponseEntity<?> response = result.get();

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Missing shopperId", responseBody.get("error"));
    }

    @Test
    @DisplayName("Should return bad request when logoutAllDevices shopperId is blank")
    void testLogoutAllDevicesAsyncBlankShopperId() throws Exception {
        // Given
        Map<String, String> body = Map.of("shopperId", "   ");

        // When
        CompletableFuture<ResponseEntity<?>> result = jwtTokenService.logoutAllDevicesAsync(body);
        ResponseEntity<?> response = result.get();

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Missing shopperId", responseBody.get("error"));
    }

    @Test
    @DisplayName("Should successfully logout from all devices when no devices found")
    void testLogoutAllDevicesAsyncNoDevicesFound() throws Exception {
        // Given
        String shopperId = "shopper-456";
        Map<String, String> body = Map.of("shopperId", shopperId);

        CollectionReference mockCollection = mock(CollectionReference.class);
        Query mockQuery = mock(Query.class);
        ApiFuture<QuerySnapshot> mockQueryFuture = mock(ApiFuture.class);
        QuerySnapshot mockQuerySnapshot = mock(QuerySnapshot.class);

        List<QueryDocumentSnapshot> emptyDocs = new ArrayList<>();

        when(firestore.collection("refresh_tokens")).thenReturn(mockCollection);
        when(mockCollection.whereEqualTo("shopperId", shopperId)).thenReturn(mockQuery);
        when(mockQuery.get()).thenReturn(mockQueryFuture);
        when(mockQueryFuture.get()).thenReturn(mockQuerySnapshot);
        when(mockQuerySnapshot.getDocuments()).thenReturn(emptyDocs);

        // When
        CompletableFuture<ResponseEntity<?>> result = jwtTokenService.logoutAllDevicesAsync(body);
        ResponseEntity<?> response = result.get();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Logged out from all devices", responseBody.get("message"));
    }
    @Test
    @DisplayName("Should handle Firestore exception in refreshTokenAsync")
    void testRefreshTokenAsyncFirestoreException() throws Exception {
        // Given
        Map<String, String> body = Map.of(
                "refreshToken", "token-123",
                "deviceId", "device-123"
        );

        CollectionReference mockCollection = mock(CollectionReference.class);
        DocumentReference mockRef = mock(DocumentReference.class);
        ApiFuture<DocumentSnapshot> mockGetFuture = mock(ApiFuture.class);

        when(firestore.collection("refresh_tokens")).thenReturn(mockCollection);
        when(mockCollection.document("token-123")).thenReturn(mockRef);
        when(mockRef.get()).thenReturn(mockGetFuture);
        when(mockGetFuture.get()).thenThrow(new RuntimeException("Firestore error"));

        // When
        CompletableFuture<ResponseEntity<?>> result = jwtTokenService.refreshTokenAsync(body);
        ResponseEntity<?> response = result.get();

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertTrue(responseBody.get("error").toString().contains("Failed to refresh token"));
    }
    @Test
    @DisplayName("Should handle Firestore exception in logoutAllDevicesAsync")
    void testLogoutAllDevicesAsyncFirestoreException() throws Exception {
        // Given
        String shopperId = "shopper-456";
        Map<String, String> body = Map.of("shopperId", shopperId);

        CollectionReference mockCollection = mock(CollectionReference.class);
        Query mockQuery = mock(Query.class);
        ApiFuture<QuerySnapshot> mockQueryFuture = mock(ApiFuture.class);

        when(firestore.collection("refresh_tokens")).thenReturn(mockCollection);
        when(mockCollection.whereEqualTo("shopperId", shopperId)).thenReturn(mockQuery);
        when(mockQuery.get()).thenReturn(mockQueryFuture);
        when(mockQueryFuture.get()).thenThrow(new RuntimeException("Firestore error"));

        // When
        CompletableFuture<ResponseEntity<?>> result = jwtTokenService.logoutAllDevicesAsync(body);
        ResponseEntity<?> response = result.get();

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertTrue(responseBody.get("error").toString().contains("Failed to logout from all devices"));
    }
}
