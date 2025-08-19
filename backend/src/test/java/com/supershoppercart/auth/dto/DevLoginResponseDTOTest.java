package com.supershoppercart.auth.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DevLoginResponseDTO Unit Tests")
class DevLoginResponseDTOTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create success response with valid access and refresh tokens")
        void constructor_validTokens_shouldCreateSuccessResponse() {
            // Arrange
            String accessToken = "valid-access-token";
            String refreshToken = "valid-refresh-token";

            // Act
            DevLoginResponseDTO response = new DevLoginResponseDTO(accessToken, refreshToken);

            // Assert
            assertEquals(accessToken, response.getAccessToken());
            assertEquals(refreshToken, response.getRefreshToken());
            assertNull(response.getError());
        }

        @Test
        @DisplayName("Should create success response with null access token")
        void constructor_nullAccessToken_shouldCreateResponse() {
            // Arrange
            String refreshToken = "valid-refresh-token";

            // Act
            DevLoginResponseDTO response = new DevLoginResponseDTO(null, refreshToken);

            // Assert
            assertNull(response.getAccessToken());
            assertEquals(refreshToken, response.getRefreshToken());
            assertNull(response.getError());
        }

        @Test
        @DisplayName("Should create success response with null refresh token")
        void constructor_nullRefreshToken_shouldCreateResponse() {
            // Arrange
            String accessToken = "valid-access-token";

            // Act
            DevLoginResponseDTO response = new DevLoginResponseDTO(accessToken, null);

            // Assert
            assertEquals(accessToken, response.getAccessToken());
            assertNull(response.getRefreshToken());
            assertNull(response.getError());
        }

        @Test
        @DisplayName("Should create success response with both tokens null")
        void constructor_bothTokensNull_shouldCreateResponse() {
            // Act
            DevLoginResponseDTO response = new DevLoginResponseDTO(null, null);

            // Assert
            assertNull(response.getAccessToken());
            assertNull(response.getRefreshToken());
            assertNull(response.getError());
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "  ", "\t", "\n"})
        @DisplayName("Should create success response with empty or whitespace tokens")
        void constructor_emptyOrWhitespaceTokens_shouldCreateResponse(String token) {
            // Act
            DevLoginResponseDTO response = new DevLoginResponseDTO(token, token);

            // Assert
            assertEquals(token, response.getAccessToken());
            assertEquals(token, response.getRefreshToken());
            assertNull(response.getError());
        }

        @Test
        @DisplayName("Should create error response with valid error message")
        void constructor_validError_shouldCreateErrorResponse() {
            // Arrange
            String errorMessage = "Authentication failed";

            // Act
            DevLoginResponseDTO response = new DevLoginResponseDTO(errorMessage);

            // Assert
            assertNull(response.getAccessToken());
            assertNull(response.getRefreshToken());
            assertEquals(errorMessage, response.getError());
        }

        @Test
        @DisplayName("Should create error response with null error message")
        void constructor_nullError_shouldCreateErrorResponse() {
            // Act
            DevLoginResponseDTO response = new DevLoginResponseDTO((String) null);

            // Assert
            assertNull(response.getAccessToken());
            assertNull(response.getRefreshToken());
            assertNull(response.getError());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t", "\n"})
        @DisplayName("Should create error response with null, empty, or whitespace error message")
        void constructor_nullEmptyOrWhitespaceError_shouldCreateErrorResponse(String error) {
            // Act
            DevLoginResponseDTO response = new DevLoginResponseDTO(error);

            // Assert
            assertNull(response.getAccessToken());
            assertNull(response.getRefreshToken());
            assertEquals(error, response.getError());
        }
    }

    @Nested
    @DisplayName("Getter and Setter Tests")
    class GetterSetterTests {

        @Test
        @DisplayName("Should get and set access token correctly")
        void accessToken_getterSetter_shouldWork() {
            // Arrange
            DevLoginResponseDTO response = new DevLoginResponseDTO("error");
            String accessToken = "new-access-token";

            // Act
            response.setAccessToken(accessToken);

            // Assert
            assertEquals(accessToken, response.getAccessToken());
        }

        @Test
        @DisplayName("Should get and set refresh token correctly")
        void refreshToken_getterSetter_shouldWork() {
            // Arrange
            DevLoginResponseDTO response = new DevLoginResponseDTO("error");
            String refreshToken = "new-refresh-token";

            // Act
            response.setRefreshToken(refreshToken);

            // Assert
            assertEquals(refreshToken, response.getRefreshToken());
        }

        @Test
        @DisplayName("Should get and set error message correctly")
        void error_getterSetter_shouldWork() {
            // Arrange
            DevLoginResponseDTO response = new DevLoginResponseDTO("access", "refresh");
            String error = "new-error-message";

            // Act
            response.setError(error);

            // Assert
            assertEquals(error, response.getError());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t", "\n", "valid-token"})
        @DisplayName("Should handle various access token values")
        void accessToken_variousValues_shouldBeHandled(String token) {
            // Arrange
            DevLoginResponseDTO response = new DevLoginResponseDTO("error");

            // Act
            response.setAccessToken(token);

            // Assert
            assertEquals(token, response.getAccessToken());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t", "\n", "valid-refresh-token"})
        @DisplayName("Should handle various refresh token values")
        void refreshToken_variousValues_shouldBeHandled(String token) {
            // Arrange
            DevLoginResponseDTO response = new DevLoginResponseDTO("error");

            // Act
            response.setRefreshToken(token);

            // Assert
            assertEquals(token, response.getRefreshToken());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t", "\n", "Authentication failed", "Network error"})
        @DisplayName("Should handle various error message values")
        void error_variousValues_shouldBeHandled(String error) {
            // Arrange
            DevLoginResponseDTO response = new DevLoginResponseDTO("access", "refresh");

            // Act
            response.setError(error);

            // Assert
            assertEquals(error, response.getError());
        }
    }

    @Nested
    @DisplayName("State Transition Tests")
    class StateTransitionTests {

        @Test
        @DisplayName("Should transition from success to error response")
        void transition_successToError_shouldWork() {
            // Arrange
            DevLoginResponseDTO response = new DevLoginResponseDTO("access-token", "refresh-token");
            String errorMessage = "Token expired";

            // Act
            response.setError(errorMessage);
            response.setAccessToken(null);
            response.setRefreshToken(null);

            // Assert
            assertNull(response.getAccessToken());
            assertNull(response.getRefreshToken());
            assertEquals(errorMessage, response.getError());
        }

        @Test
        @DisplayName("Should transition from error to success response")
        void transition_errorToSuccess_shouldWork() {
            // Arrange
            DevLoginResponseDTO response = new DevLoginResponseDTO("Authentication failed");
            String accessToken = "new-access-token";
            String refreshToken = "new-refresh-token";

            // Act
            response.setAccessToken(accessToken);
            response.setRefreshToken(refreshToken);
            response.setError(null);

            // Assert
            assertEquals(accessToken, response.getAccessToken());
            assertEquals(refreshToken, response.getRefreshToken());
            assertNull(response.getError());
        }

        @Test
        @DisplayName("Should allow mixed state with both tokens and error")
        void mixedState_tokensAndError_shouldBeAllowed() {
            // Arrange
            DevLoginResponseDTO response = new DevLoginResponseDTO("access", "refresh");
            String error = "Warning: Token expires soon";

            // Act
            response.setError(error);

            // Assert
            assertEquals("access", response.getAccessToken());
            assertEquals("refresh", response.getRefreshToken());
            assertEquals(error, response.getError());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very long token values")
        void longTokenValues_shouldBeHandled() {
            // Arrange
            String longToken = "a".repeat(1000);
            DevLoginResponseDTO response = new DevLoginResponseDTO("error");

            // Act
            response.setAccessToken(longToken);
            response.setRefreshToken(longToken);

            // Assert
            assertEquals(longToken, response.getAccessToken());
            assertEquals(longToken, response.getRefreshToken());
        }

        @Test
        @DisplayName("Should handle very long error message")
        void longErrorMessage_shouldBeHandled() {
            // Arrange
            String longError = "Error: " + "x".repeat(1000);
            DevLoginResponseDTO response = new DevLoginResponseDTO("access", "refresh");

            // Act
            response.setError(longError);

            // Assert
            assertEquals(longError, response.getError());
        }

        @Test
        @DisplayName("Should handle special characters in tokens")
        void specialCharactersInTokens_shouldBeHandled() {
            // Arrange
            String specialToken = "token-with-special-chars-!@#$%^&*()_+-=[]{}|;':\",./<>?";
            DevLoginResponseDTO response = new DevLoginResponseDTO("error");

            // Act
            response.setAccessToken(specialToken);
            response.setRefreshToken(specialToken);

            // Assert
            assertEquals(specialToken, response.getAccessToken());
            assertEquals(specialToken, response.getRefreshToken());
        }

        @Test
        @DisplayName("Should handle special characters in error message")
        void specialCharactersInError_shouldBeHandled() {
            // Arrange
            String specialError = "Error: Special chars !@#$%^&*()_+-=[]{}|;':\",./<>?";
            DevLoginResponseDTO response = new DevLoginResponseDTO("access", "refresh");

            // Act
            response.setError(specialError);

            // Assert
            assertEquals(specialError, response.getError());
        }

        @Test
        @DisplayName("Should handle Unicode characters")
        void unicodeCharacters_shouldBeHandled() {
            // Arrange
            String unicodeToken = "token-with-unicode-测试-🔒-αβγ";
            String unicodeError = "Error with unicode: 错误-🚫-αλφα";
            DevLoginResponseDTO response = new DevLoginResponseDTO("initial", "initial");

            // Act
            response.setAccessToken(unicodeToken);
            response.setRefreshToken(unicodeToken);
            response.setError(unicodeError);

            // Assert
            assertEquals(unicodeToken, response.getAccessToken());
            assertEquals(unicodeToken, response.getRefreshToken());
            assertEquals(unicodeError, response.getError());
        }
    }

    @Nested
    @DisplayName("Object State Consistency Tests")
    class ObjectStateConsistencyTests {

        @Test
        @DisplayName("Should maintain independent field states")
        void independentFieldStates_shouldBeMaintained() {
            // Arrange
            DevLoginResponseDTO response = new DevLoginResponseDTO("initial-access", "initial-refresh");

            // Act & Assert - Modify one field at a time
            response.setAccessToken("new-access");
            assertEquals("new-access", response.getAccessToken());
            assertEquals("initial-refresh", response.getRefreshToken());
            assertNull(response.getError());

            response.setRefreshToken("new-refresh");
            assertEquals("new-access", response.getAccessToken());
            assertEquals("new-refresh", response.getRefreshToken());
            assertNull(response.getError());

            response.setError("new-error");
            assertEquals("new-access", response.getAccessToken());
            assertEquals("new-refresh", response.getRefreshToken());
            assertEquals("new-error", response.getError());
        }

        @Test
        @DisplayName("Should allow multiple updates to same field")
        void multipleUpdates_samefield_shouldWork() {
            // Arrange
            DevLoginResponseDTO response = new DevLoginResponseDTO("error");

            // Act & Assert
            response.setAccessToken("token1");
            assertEquals("token1", response.getAccessToken());

            response.setAccessToken("token2");
            assertEquals("token2", response.getAccessToken());

            response.setAccessToken("token3");
            assertEquals("token3", response.getAccessToken());

            response.setAccessToken(null);
            assertNull(response.getAccessToken());
        }
    }
}