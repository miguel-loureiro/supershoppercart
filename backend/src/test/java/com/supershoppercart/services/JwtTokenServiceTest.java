package com.supershoppercart.services;

import com.supershoppercart.models.Shopper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
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

    private Shopper testShopper;

    private final String secretKeyString = "aStrongAndSecretKeyForTestingPurposesThatIsAtLeast32BytesLong";

    @BeforeEach
    void setUp() {
        // Create test shopper
        testShopper = new Shopper();
        testShopper.setId("test-shopper-123");
        testShopper.setName("Test Shopper");

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
        JwtTokenService newService = new JwtTokenService();
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
        JwtTokenService newService = new JwtTokenService();
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
        JwtTokenService shortExpiryService = new JwtTokenService();
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
        JwtTokenService newService = new JwtTokenService();
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
        JwtTokenService faultyService = new JwtTokenService();
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
        JwtTokenService newService = new JwtTokenService();
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
}
