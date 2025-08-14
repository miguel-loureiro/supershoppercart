package com.supershoppercart.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
@DisplayName("SharePermission toLowerCase() Tests")
class SharePermissionToLowerCaseTest {

    @Test
    @DisplayName("Should return 'view' for VIEW enum value")
    void shouldReturnViewForViewEnumValue() {
        // Given
        SharePermission permission = SharePermission.VIEW;

        // When
        String result = permission.toLowerCase();

        // Then
        assertEquals("view", result);
        assertNotNull(result);
        assertEquals(result, result.toLowerCase()); // Verify it's actually lowercase
    }

    @Test
    @DisplayName("Should return 'edit' for EDIT enum value")
    void shouldReturnEditForEditEnumValue() {
        // Given
        SharePermission permission = SharePermission.EDIT;

        // When
        String result = permission.toLowerCase();

        // Then
        assertEquals("edit", result);
        assertNotNull(result);
        assertEquals(result, result.toLowerCase()); // Verify it's actually lowercase
    }

    @Test
    @DisplayName("Should return 'admin' for ADMIN enum value")
    void shouldReturnAdminForAdminEnumValue() {
        // Given
        SharePermission permission = SharePermission.ADMIN;

        // When
        String result = permission.toLowerCase();

        // Then
        assertEquals("admin", result);
        assertNotNull(result);
        assertEquals(result, result.toLowerCase()); // Verify it's actually lowercase
    }

    @ParameterizedTest
    @EnumSource(SharePermission.class)
    @DisplayName("Should return lowercase string for all enum values")
    void shouldReturnLowerCaseStringForAllEnumValues(SharePermission permission) {
        // When
        String result = permission.toLowerCase();

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(result.toLowerCase(), result); // Verify result is in lowercase
        assertEquals(permission.name().toLowerCase(), result); // Verify matches name().toLowerCase()
    }

    @Test
    @DisplayName("Should return consistent results for multiple invocations")
    void shouldReturnConsistentResultsForMultipleInvocations() {
        // Given
        SharePermission permission = SharePermission.VIEW;

        // When
        String result1 = permission.toLowerCase();
        String result2 = permission.toLowerCase();
        String result3 = permission.toLowerCase();

        // Then
        assertEquals(result1, result2);
        assertEquals(result2, result3);
        assertEquals(result1, result3);
        assertEquals("view", result1);
    }

    @Test
    @DisplayName("Should handle all enum values without throwing exceptions")
    void shouldHandleAllEnumValuesWithoutThrowingExceptions() {
        // Given
        SharePermission[] allPermissions = SharePermission.values();

        // When/Then
        for (SharePermission permission : allPermissions) {
            assertDoesNotThrow(() -> permission.toLowerCase());
            String result = permission.toLowerCase();
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }
    }

    @Test
    @DisplayName("Should return different strings for different enum values")
    void shouldReturnDifferentStringsForDifferentEnumValues() {
        // Given
        String viewResult = SharePermission.VIEW.toLowerCase();
        String editResult = SharePermission.EDIT.toLowerCase();
        String adminResult = SharePermission.ADMIN.toLowerCase();

        // Then
        assertNotEquals(viewResult, editResult);
        assertNotEquals(editResult, adminResult);
        assertNotEquals(viewResult, adminResult);
    }

    @Test
    @DisplayName("Should return strings that match expected lengths")
    void shouldReturnStringsThatMatchExpectedLengths() {
        // When/Then
        assertEquals(4, SharePermission.VIEW.toLowerCase().length());  // "view"
        assertEquals(4, SharePermission.EDIT.toLowerCase().length());  // "edit"
        assertEquals(5, SharePermission.ADMIN.toLowerCase().length()); // "admin"
    }

    @Test
    @DisplayName("Should verify returned strings contain only lowercase letters")
    void shouldVerifyReturnedStringsContainOnlyLowercaseLetters() {
        // Given
        SharePermission[] allPermissions = SharePermission.values();

        // When/Then
        for (SharePermission permission : allPermissions) {
            String result = permission.toLowerCase();
            assertTrue(result.chars().allMatch(Character::isLowerCase),
                    "Result should contain only lowercase letters: " + result);
            assertTrue(result.matches("^[a-z]+$"),
                    "Result should match lowercase letters pattern: " + result);
        }
    }

    @Test
    @DisplayName("Should return immutable strings")
    void shouldReturnImmutableStrings() {
        // Given
        SharePermission permission = SharePermission.VIEW;

        // When
        String result1 = permission.toLowerCase();
        String result2 = permission.toLowerCase();

        // Then
        assertNotSame(result1, result2); // Different string instances (due to method call)
        assertEquals(result1, result2);   // But same content
    }

    @Test
    @DisplayName("Should match Java String toLowerCase behavior")
    void shouldMatchJavaStringToLowerCaseBehavior() {
        // Given/When/Then
        for (SharePermission permission : SharePermission.values()) {
            String methodResult = permission.toLowerCase();
            String expectedResult = permission.name().toLowerCase();
            assertEquals(expectedResult, methodResult);
        }
    }

    @Test
    @DisplayName("Should work correctly with string comparison operations")
    void shouldWorkCorrectlyWithStringComparisonOperations() {
        // Given
        String viewLowercase = SharePermission.VIEW.toLowerCase();
        String editLowercase = SharePermission.EDIT.toLowerCase();

        // When/Then
        assertEquals("view", viewLowercase);
        assertEquals("edit", editLowercase);
        assertNotEquals("VIEW", viewLowercase);
        assertNotEquals("EDIT", editLowercase);

        // Test with equalsIgnoreCase
        assertTrue("VIEW".equalsIgnoreCase(viewLowercase));
        assertTrue("EDIT".equalsIgnoreCase(editLowercase));
    }

    @Test
    @DisplayName("Should return expected values for all enum constants")
    void shouldReturnExpectedValuesForAllEnumConstants() {
        // Given
        List<String> expectedResults = Arrays.asList("view", "edit", "admin");
        List<SharePermission> permissions = Arrays.asList(
                SharePermission.VIEW,
                SharePermission.EDIT,
                SharePermission.ADMIN
        );

        // When/Then
        for (int i = 0; i < permissions.size(); i++) {
            String actualResult = permissions.get(i).toLowerCase();
            String expectedResult = expectedResults.get(i);
            assertEquals(expectedResult, actualResult,
                    "Expected " + expectedResult + " but got " + actualResult +
                            " for " + permissions.get(i).name());
        }
    }

    @Test
    @DisplayName("Should be locale-independent")
    void shouldBeLocaleIndependent() {
        // Given
        Locale originalLocale = Locale.getDefault();
        SharePermission permission = SharePermission.VIEW;

        try {
            // When - Test with different locales
            Locale.setDefault(Locale.ENGLISH);
            String resultEnglish = permission.toLowerCase();

            Locale.setDefault(Locale.FRENCH);
            String resultFrench = permission.toLowerCase();

            // Then
            assertEquals("view", resultEnglish);
            assertEquals("view", resultFrench);
            assertEquals(resultEnglish, resultFrench);
        } finally {
            // Restore original locale
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    @DisplayName("Should handle concurrent access correctly")
    void shouldHandleConcurrentAccessCorrectly() throws InterruptedException {
        // Given
        SharePermission permission = SharePermission.ADMIN;
        int numberOfThreads = 10;
        String[] results = new String[numberOfThreads];
        Thread[] threads = new Thread[numberOfThreads];

        // When - Create multiple threads calling toLowerCase()
        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            threads[i] = new Thread(() -> results[index] = permission.toLowerCase());
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then - All results should be the same
        String expectedResult = "admin";
        for (int i = 0; i < numberOfThreads; i++) {
            assertEquals(expectedResult, results[i]);
            assertNotNull(results[i]);
        }
    }

    @Test
    @DisplayName("Should verify method does not modify enum state")
    void shouldVerifyMethodDoesNotModifyEnumState() {
        // Given
        SharePermission permission = SharePermission.EDIT;
        String originalName = permission.name();

        // When
        String lowercaseResult = permission.toLowerCase();

        // Then
        assertEquals("EDIT", permission.name()); // Original name unchanged
        assertEquals(originalName, permission.name()); // Still the same
        assertEquals("edit", lowercaseResult); // Method result is lowercase
        assertNotEquals(permission.name(), lowercaseResult); // Original != result
    }

    @Test
    @DisplayName("Should verify all enum values have expected string representations")
    void shouldVerifyAllEnumValuesHaveExpectedStringRepresentations() {
        // Given/When/Then
        assertEquals("view", SharePermission.VIEW.toLowerCase());
        assertEquals("edit", SharePermission.EDIT.toLowerCase());
        assertEquals("admin", SharePermission.ADMIN.toLowerCase());

        // Verify count
        assertEquals(3, SharePermission.values().length);

        // Verify no null values
        for (SharePermission permission : SharePermission.values()) {
            assertNotNull(permission.toLowerCase());
        }
    }
}