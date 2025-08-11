package com.supershoppercart.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void handleValidationErrors_shouldReturnBadRequestWithErrors() {
        // Arrange
        // Mock a MethodArgumentNotValidException with a BindingResult
        MethodArgumentNotValidException mockException = mock(MethodArgumentNotValidException.class);
        BindingResult mockBindingResult = mock(BindingResult.class);
        when(mockException.getBindingResult()).thenReturn(mockBindingResult);

        // Mock field errors
        FieldError fieldError1 = new FieldError("objectName", "field1", "Error message for field1");
        FieldError fieldError2 = new FieldError("objectName", "field2", "Error message for field2");
        List<FieldError> fieldErrors = List.of(fieldError1, fieldError2);
        when(mockBindingResult.getFieldErrors()).thenReturn(fieldErrors);

        // Act
        ResponseEntity<Map<String, String>> responseEntity = globalExceptionHandler.handleValidationErrors(mockException);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Map<String, String> expectedErrors = new HashMap<>();
        expectedErrors.put("field1", "Error message for field1");
        expectedErrors.put("field2", "Error message for field2");
        assertEquals(expectedErrors, responseEntity.getBody());
    }

    @Test
    void handleOtherExceptions_shouldReturnInternalServerError() {
        // Arrange
        Exception genericException = new RuntimeException("A generic error");

        // Act
        ResponseEntity<String> responseEntity = globalExceptionHandler.handleOtherExceptions(genericException);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals("Unexpected error occurred", responseEntity.getBody());
    }
}