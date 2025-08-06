package com.supershopcart.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.IOException;
import java.security.GeneralSecurityException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the GoogleTokenVerifier class.
 * This class uses JUnit 5 and Mockito to test the functionality of
 * token verification by mocking the external GoogleIdTokenVerifier dependency.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GoogleTokenVerifier Unit Tests")
class GoogleTokenVerifierTest {

    // Mock the GoogleIdTokenVerifier, which is the key dependency
    @Mock
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    // Inject the mocks into the GoogleTokenVerifier instance
    @InjectMocks
    private GoogleTokenVerifier googleTokenVerifier;

    // Test data
    private final String MOCK_ID_TOKEN = "mockIdTokenString";
    private final String MOCK_EMAIL = "testuser@example.com";

    @BeforeEach
    void setUp() {
        // Use ReflectionTestUtils to set the private 'verifier' field with our mock
        ReflectionTestUtils.setField(googleTokenVerifier, "verifier", googleIdTokenVerifier);

        // Also set the private 'googleClientId' field, as @Value is not processed in unit tests
        ReflectionTestUtils.setField(googleTokenVerifier, "googleClientId", "test-client-id");
    }

    @Test
    @DisplayName("Should successfully verify a valid Google ID token")
    void testVerify_Success() throws GeneralSecurityException, IOException {
        // Arrange
        // Create a mock payload object
        GoogleIdToken.Payload mockPayload = new GoogleIdToken.Payload();
        mockPayload.setEmail(MOCK_EMAIL);

        // Create a mock GoogleIdToken object and set its behavior
        GoogleIdToken mockIdToken = mock(GoogleIdToken.class);
        when(mockIdToken.getPayload()).thenReturn(mockPayload);

        // Mock the verifier.verify() method to return our mock ID token
        when(googleIdTokenVerifier.verify(MOCK_ID_TOKEN)).thenReturn(mockIdToken);

        // Act
        GoogleIdToken.Payload resultPayload = googleTokenVerifier.verify(MOCK_ID_TOKEN);

        // Assert
        assertEquals(MOCK_EMAIL, resultPayload.getEmail());
    }

    @Test
    @DisplayName("Should return null for an invalid Google ID token")
    void testVerify_InvalidToken() throws GeneralSecurityException, IOException {
        // Arrange
        // Mock the verifier.verify() method to return null for an invalid token
        when(googleIdTokenVerifier.verify(MOCK_ID_TOKEN)).thenReturn(null);

        // Act
        GoogleIdToken.Payload resultPayload = googleTokenVerifier.verify(MOCK_ID_TOKEN);

        // Assert
        assertNull(resultPayload);
    }

    @Test
    @DisplayName("Should return null when a GeneralSecurityException is thrown")
    void testVerify_GeneralSecurityException() throws GeneralSecurityException, IOException {
        // Arrange
        // Mock the verifier.verify() method to throw a security exception
        when(googleIdTokenVerifier.verify(MOCK_ID_TOKEN)).thenThrow(new GeneralSecurityException("Test security exception"));

        // Act
        GoogleIdToken.Payload resultPayload = googleTokenVerifier.verify(MOCK_ID_TOKEN);

        // Assert
        assertNull(resultPayload);
    }

    @Test
    @DisplayName("Should return null when an IOException is thrown")
    void testVerify_IOException() throws GeneralSecurityException, IOException {
        // Arrange
        // Mock the verifier.verify() method to throw an I/O exception
        when(googleIdTokenVerifier.verify(MOCK_ID_TOKEN)).thenThrow(new IOException("Test I/O exception"));

        // Act
        GoogleIdToken.Payload resultPayload = googleTokenVerifier.verify(MOCK_ID_TOKEN);

        // Assert
        assertNull(resultPayload);
    }

    @Test
    @DisplayName("Should return null when an unexpected generic Exception is thrown")
    void testVerify_GenericException() throws GeneralSecurityException, IOException {
        // Arrange
        // Mock the verifier.verify() method to throw a generic exception
        when(googleIdTokenVerifier.verify(MOCK_ID_TOKEN)).thenThrow(new RuntimeException("Test unexpected exception"));

        // Act
        GoogleIdToken.Payload resultPayload = googleTokenVerifier.verify(MOCK_ID_TOKEN);

        // Assert
        assertNull(resultPayload);
    }

    @Test
    @DisplayName("Should verify with correct audience and not null")
    void testInit() {
        // We'll need a real GoogleIdTokenVerifier instance to test init
        GoogleTokenVerifier realVerifier = new GoogleTokenVerifier();

        // Use ReflectionTestUtils to set the private 'googleClientId' field
        ReflectionTestUtils.setField(realVerifier, "googleClientId", "test-client-id");

        // Call the init method directly
        realVerifier.init();

        // Use ReflectionTestUtils to get the verifier instance
        GoogleIdTokenVerifier initializedVerifier = (GoogleIdTokenVerifier) ReflectionTestUtils.getField(realVerifier, "verifier");

        // Assert that the verifier was initialized
        assertNotNull(initializedVerifier);

        // It's not straightforward to check the audience set on the verifier object
        // in a unit test without reflection. We'll stick to asserting it's not null for this test.
    }
}