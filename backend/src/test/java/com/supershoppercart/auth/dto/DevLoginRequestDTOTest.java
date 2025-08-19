package com.supershoppercart.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive JUnit 5 unit tests for the DevLoginRequestDTO class.
 * This class tests the constructors, getters, setters, and the
 * Jakarta Bean Validation constraints (@Email and @NotBlank).
 */
public class DevLoginRequestDTOTest {

    // A static validator instance to be used across all tests.
    private static Validator validator;

    /**
     * Sets up the Jakarta Bean Validation validator before all test methods are run.
     * This is an efficient way to initialize the validator once.
     */
    @BeforeAll
    public static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /**
     * Tests the no-argument constructor to ensure the object is created
     * without any issues.
     */
    @Test
    void testNoArgsConstructor() {
        // Arrange & Act
        DevLoginRequestDTO dto = new DevLoginRequestDTO();

        // Assert
        assertNull(dto.getEmail());
        assertNull(dto.getDeviceId());
    }

    /**
     * Tests the parameterized constructor to ensure that fields are
     * initialized correctly upon object creation.
     */
    @Test
    void testAllArgsConstructor() {
        // Arrange
        String email = "test@example.com";
        String deviceId = "device-123";

        // Act
        DevLoginRequestDTO dto = new DevLoginRequestDTO(email, deviceId);

        // Assert
        assertEquals(email, dto.getEmail());
        assertEquals(deviceId, dto.getDeviceId());
    }

    /**
     * Tests the setters to confirm that they correctly update the fields.
     * This also implicitly tests the getters as they are used for assertion.
     */
    @Test
    void testSettersAndGetters() {
        // Arrange
        DevLoginRequestDTO dto = new DevLoginRequestDTO();
        String email = "setter.test@example.com";
        String deviceId = "new-device-456";

        // Act
        dto.setEmail(email);
        dto.setDeviceId(deviceId);

        // Assert
        assertEquals(email, dto.getEmail());
        assertEquals(deviceId, dto.getDeviceId());
    }

    /**
     * Tests a valid DTO instance to ensure that it passes all validation
     * constraints and has no constraint violations.
     */
    @Test
    void testValidDto() {
        // Arrange
        DevLoginRequestDTO dto = new DevLoginRequestDTO("valid.email@example.com", "dev-id");

        // Act
        Set<ConstraintViolation<DevLoginRequestDTO>> violations = validator.validate(dto);

        // Assert
        assertTrue(violations.isEmpty(), "Valid DTO should have no constraint violations");
    }

    /**
     * Tests an invalid email format to ensure the @Email annotation correctly
     * flags it as a violation.
     */
    @Test
    void testInvalidEmailFormat() {
        // Arrange
        DevLoginRequestDTO dto = new DevLoginRequestDTO("invalid-email", "dev-id");

        // Act
        Set<ConstraintViolation<DevLoginRequestDTO>> violations = validator.validate(dto);

        // Assert
        assertEquals(1, violations.size(), "Should have exactly one violation for invalid email format");
        ConstraintViolation<DevLoginRequestDTO> violation = violations.iterator().next();
        assertEquals("email", violation.getPropertyPath().toString(), "Violation should be on the 'email' field");
    }

    /**
     * Tests a null email to ensure the @NotBlank annotation correctly
     * flags it as a violation.
     */
    @Test
    void testNullEmail() {
        // Arrange
        DevLoginRequestDTO dto = new DevLoginRequestDTO(null, "dev-id");

        // Act
        Set<ConstraintViolation<DevLoginRequestDTO>> violations = validator.validate(dto);

        // Assert
        // Expecting two violations: one for @Email and one for @NotBlank
        assertEquals(1, violations.size(), "Should have exactly one violation for a null email");
        ConstraintViolation<DevLoginRequestDTO> violation = violations.iterator().next();
        // The message will depend on the implementation, but it should indicate not blank.
        assertTrue(violation.getMessage().contains("must not be blank"), "Violation message should indicate 'not blank'");
    }

    /**
     * Tests an empty string email to ensure the @NotBlank annotation correctly
     * flags it as a violation.
     */
    @Test
    void testEmptyEmail() {
        // Arrange
        DevLoginRequestDTO dto = new DevLoginRequestDTO("", "dev-id");

        // Act
        Set<ConstraintViolation<DevLoginRequestDTO>> violations = validator.validate(dto);

        // Assert
        // The Bean Validation spec states @NotBlank includes @NotEmpty, so we expect one violation.
        assertEquals(1, violations.size(), "Should have exactly one violation for an empty email");
    }

    /**
     * Tests a blank string (containing only whitespace) email to ensure the @NotBlank
     * annotation correctly flags it as a violation.
     */
    @Test
    void testBlankEmail() {
        // Arrange
        DevLoginRequestDTO dto = new DevLoginRequestDTO("   ", "dev-id");

        // Act
        Set<ConstraintViolation<DevLoginRequestDTO>> violations = validator.validate(dto);

        // Assert
        assertEquals(2, violations.size(), "Should have exactly one violation for a blank email");
    }
}