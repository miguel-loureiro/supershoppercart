package com.supershoppercart.security;

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
import static org.mockito.Mockito.*;

/**
 * Unit tests for the GoogleTokenVerifier class.
 * This class uses JUnit 5 and Mockito to test the functionality of
 * token verification by mocking the external GoogleIdTokenVerifier dependency.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GoogleTokenVerifier Unit Tests")
class GoogleTokenVerifierTest {

    @Mock
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    @InjectMocks
    private GoogleTokenVerifier googleTokenVerifier;

    private final String MOCK_ID_TOKEN = "mockIdTokenString";
    private final String MOCK_EMAIL = "testuser@example.com";

    @BeforeEach
    void setUp() {
        // Injetar o verifier mockado na nossa classe. Isso é feito automaticamente pelo @InjectMocks.
        // A sua abordagem original com ReflectionTestUtils era uma boa forma de contornar a inicialização, mas
        // o Mockito trata disso. Vamos apenas garantir que o clientId e o ambiente são configurados.
        ReflectionTestUtils.setField(googleTokenVerifier, "googleClientId", "test-client-id");
        ReflectionTestUtils.setField(googleTokenVerifier, "appEnv", "prod");

        // Simular a inicialização do verifier. No seu código, o @PostConstruct é chamado
        // para inicializar o verifier. No teste, já o temos mockado, mas para simular o comportamento
        // real, vamos apenas garantir que a referência está definida.
        ReflectionTestUtils.setField(googleTokenVerifier, "verifier", googleIdTokenVerifier);
    }

    // ---
    // Teste do cenário de bypass de desenvolvimento
    // ---
    @Test
    @DisplayName("Should return fake payload for TEST_TOKEN in dev environment")
    void testVerify_DevBypass_Success() throws GeneralSecurityException, IOException {
        // Arrange
        // Mudar o ambiente da app para 'dev' para ativar o bypass
        ReflectionTestUtils.setField(googleTokenVerifier, "appEnv", "dev");

        // Act
        GoogleIdToken.Payload resultPayload = googleTokenVerifier.verify("TEST_TOKEN");

        // Assert
        assertNotNull(resultPayload);
        assertEquals("test@example.com", resultPayload.getEmail());
        assertEquals("Test User", resultPayload.get("name"));

        // Assert that the real verifier was never called
        verify(googleIdTokenVerifier, never()).verify(anyString());
    }

    // ---
    // Testes de caminho feliz (Happy Path)
    // ---
    @Test
    @DisplayName("Should successfully verify a valid Google ID token")
    void testVerify_Success() throws GeneralSecurityException, IOException {
        // Arrange
        GoogleIdToken.Payload mockPayload = new GoogleIdToken.Payload();
        mockPayload.setEmail(MOCK_EMAIL);

        GoogleIdToken mockIdToken = mock(GoogleIdToken.class);
        when(mockIdToken.getPayload()).thenReturn(mockPayload);

        when(googleIdTokenVerifier.verify(MOCK_ID_TOKEN)).thenReturn(mockIdToken);

        // Act
        GoogleIdToken.Payload resultPayload = googleTokenVerifier.verify(MOCK_ID_TOKEN);

        // Assert
        assertNotNull(resultPayload);
        assertEquals(MOCK_EMAIL, resultPayload.getEmail());
        verify(googleIdTokenVerifier, times(1)).verify(MOCK_ID_TOKEN);
    }

    // ---
    // Testes para cenários de falha de verificação
    // ---
    @Test
    @DisplayName("Should return null for an invalid Google ID token")
    void testVerify_InvalidToken() throws GeneralSecurityException, IOException {
        // Arrange
        when(googleIdTokenVerifier.verify(MOCK_ID_TOKEN)).thenReturn(null);

        // Act
        GoogleIdToken.Payload resultPayload = googleTokenVerifier.verify(MOCK_ID_TOKEN);

        // Assert
        assertNull(resultPayload);
        verify(googleIdTokenVerifier, times(1)).verify(MOCK_ID_TOKEN);
    }

    // ---
    // Testes para cenários de exceção
    // ---
    @Test
    @DisplayName("Should return null when a GeneralSecurityException is thrown")
    void testVerify_GeneralSecurityException() throws GeneralSecurityException, IOException {
        // Arrange
        when(googleIdTokenVerifier.verify(MOCK_ID_TOKEN)).thenThrow(new GeneralSecurityException("Test security exception"));

        // Act
        GoogleIdToken.Payload resultPayload = googleTokenVerifier.verify(MOCK_ID_TOKEN);

        // Assert
        assertNull(resultPayload);
        verify(googleIdTokenVerifier, times(1)).verify(MOCK_ID_TOKEN);
    }

    @Test
    @DisplayName("Should return null when an IOException is thrown")
    void testVerify_IOException() throws GeneralSecurityException, IOException {
        // Arrange
        when(googleIdTokenVerifier.verify(MOCK_ID_TOKEN)).thenThrow(new IOException("Test I/O exception"));

        // Act
        GoogleIdToken.Payload resultPayload = googleTokenVerifier.verify(MOCK_ID_TOKEN);

        // Assert
        assertNull(resultPayload);
        verify(googleIdTokenVerifier, times(1)).verify(MOCK_ID_TOKEN);
    }
}