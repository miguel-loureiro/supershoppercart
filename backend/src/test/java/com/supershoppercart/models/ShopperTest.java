package com.supershoppercart.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Shopper class.
 * This class validates the behavior of the data model itself,
 * including its constructors, getters, setters, and the overridden
 * equals() and hashCode() methods.
 */
@DisplayName("Shopper Class Unit Tests")
public class ShopperTest {

    private Shopper shopper1;
    private Shopper shopper2;

    @BeforeEach
    void setUp() {
        // Initialize two Shopper objects with the same data for equality tests
        shopper1 = new Shopper();
        shopper1.setId("shopperId1");
        shopper1.setEmail("shopper1@example.com");
        shopper1.setName("John Doe");
        shopper1.setPassword("hashedPassword123");
        shopper1.setShopCartIds(Arrays.asList("cart1", "cart2"));

        shopper2 = new Shopper();
        shopper2.setId("shopperId1");
        shopper2.setEmail("shopper1@example.com");
        shopper2.setName("John Doe");
        shopper2.setPassword("hashedPassword123");
        shopper2.setShopCartIds(Arrays.asList("cart1", "cart2"));
    }

    @Test
    @DisplayName("equals() should return true for identical objects")
    void testEquals_IdenticalObjects() {
        assertEquals(shopper1, shopper1);
    }

    @Test
    @DisplayName("equals() should return true for objects with the same field values")
    void testEquals_EqualObjects() {
        assertEquals(shopper1, shopper2);
    }

    @Test
    @DisplayName("equals() should return false when IDs are different")
    void testEquals_DifferentId() {
        shopper2.setId("differentId");
        assertNotEquals(shopper1, shopper2);
    }

    @Test
    @DisplayName("equals() should return false when emails are different")
    void testEquals_DifferentEmail() {
        shopper2.setEmail("different@example.com");
        assertNotEquals(shopper1, shopper2);
    }

    @Test
    @DisplayName("equals() should return false when names are different")
    void testEquals_DifferentName() {
        shopper2.setName("Jane Smith");
        assertNotEquals(shopper1, shopper2);
    }

    @Test
    @DisplayName("equals() should return false when shopCartIds are different")
    void testEquals_DifferentShopCartIds() {
        shopper2.setShopCartIds(Collections.singletonList("cart3"));
        assertNotEquals(shopper1, shopper2);
    }

    @Test
    @DisplayName("equals() should return false when compared to null")
    void testEquals_NullObject() {
        assertNotEquals(null, shopper1);
    }

    @Test
    @DisplayName("equals() should return false when compared to a different class")
    void testEquals_DifferentClass() {
        Object differentObject = new Object();
        assertNotEquals(shopper1, differentObject);
    }

    @Test
    @DisplayName("toString() should return the correct string representation with protected password")
    void testToString_CorrectFormat() {
        // Expected string representation based on the toString() method logic
        String expected = "Shopper{" +
                "id='shopperId1', " +
                "email='shopper1@example.com', " +
                "name='John Doe', " +
                "provider='google', " +
                "shopCartIds=[cart1, cart2]}";

        assertEquals(expected, shopper1.toString());
    }

    @Test
    @DisplayName("Should return same hashCode for identical shoppers")
    void shouldReturnSameHashCodeForIdenticalShoppers() {
        // Given
        shopper1.setId("shopper-123");
        shopper1.setEmail("test@example.com");
        shopper1.setName("John Doe");
        shopper1.setProvider("google");
        shopper1.setShopCartIds(Arrays.asList("cart-1", "cart-2"));

        shopper2.setId("shopper-123");
        shopper2.setEmail("test@example.com");
        shopper2.setName("John Doe");
        shopper2.setProvider("google");
        shopper2.setShopCartIds(Arrays.asList("cart-1", "cart-2"));

        // When
        int hashCode1 = shopper1.hashCode();
        int hashCode2 = shopper2.hashCode();

        // Then
        assertEquals(hashCode1, hashCode2);
    }

    @Test
    @DisplayName("Should return different hashCode when IDs are different")
    void shouldReturnDifferentHashCodeWhenIdsAreDifferent() {
        // Given
        shopper1.setId("shopper-123");
        shopper1.setEmail("test@example.com");
        shopper1.setName("John Doe");
        shopper1.setProvider("google");

        shopper2.setId("shopper-456");
        shopper2.setEmail("test@example.com");
        shopper2.setName("John Doe");
        shopper2.setProvider("google");

        // When
        int hashCode1 = shopper1.hashCode();
        int hashCode2 = shopper2.hashCode();

        // Then
        assertNotEquals(hashCode1, hashCode2);
    }

    @Test
    @DisplayName("Should return different hashCode when emails are different")
    void shouldReturnDifferentHashCodeWhenEmailsAreDifferent() {
        // Given
        shopper1.setId("shopper-123");
        shopper1.setEmail("test1@example.com");
        shopper1.setName("John Doe");
        shopper1.setProvider("google");

        shopper2.setId("shopper-123");
        shopper2.setEmail("test2@example.com");
        shopper2.setName("John Doe");
        shopper2.setProvider("google");

        // When
        int hashCode1 = shopper1.hashCode();
        int hashCode2 = shopper2.hashCode();

        // Then
        assertNotEquals(hashCode1, hashCode2);
    }

    @Test
    @DisplayName("Should return different hashCode when names are different")
    void shouldReturnDifferentHashCodeWhenNamesAreDifferent() {
        // Given
        shopper1.setId("shopper-123");
        shopper1.setEmail("test@example.com");
        shopper1.setName("John Doe");
        shopper1.setProvider("google");

        shopper2.setId("shopper-123");
        shopper2.setEmail("test@example.com");
        shopper2.setName("Jane Doe");
        shopper2.setProvider("google");

        // When
        int hashCode1 = shopper1.hashCode();
        int hashCode2 = shopper2.hashCode();

        // Then
        assertNotEquals(hashCode1, hashCode2);
    }

    @Test
    @DisplayName("Should return different hashCode when providers are different")
    void shouldReturnDifferentHashCodeWhenProvidersAreDifferent() {
        // Given
        shopper1.setId("shopper-123");
        shopper1.setEmail("test@example.com");
        shopper1.setName("John Doe");
        shopper1.setProvider("google");

        shopper2.setId("shopper-123");
        shopper2.setEmail("test@example.com");
        shopper2.setName("John Doe");
        shopper2.setProvider("manual");

        // When
        int hashCode1 = shopper1.hashCode();
        int hashCode2 = shopper2.hashCode();

        // Then
        assertNotEquals(hashCode1, hashCode2);
    }

    @Test
    @DisplayName("Should return different hashCode when shopCartIds are different")
    void shouldReturnDifferentHashCodeWhenShopCartIdsAreDifferent() {
        // Given
        shopper1.setId("shopper-123");
        shopper1.setEmail("test@example.com");
        shopper1.setName("John Doe");
        shopper1.setProvider("google");
        shopper1.setShopCartIds(Arrays.asList("cart-1", "cart-2"));

        shopper2.setId("shopper-123");
        shopper2.setEmail("test@example.com");
        shopper2.setName("John Doe");
        shopper2.setProvider("google");
        shopper2.setShopCartIds(Arrays.asList("cart-1", "cart-3"));

        // When
        int hashCode1 = shopper1.hashCode();
        int hashCode2 = shopper2.hashCode();

        // Then
        assertNotEquals(hashCode1, hashCode2);
    }

    @Test
    @DisplayName("Should handle null values in hashCode calculation")
    void shouldHandleNullValuesInHashCodeCalculation() {
        // Given
        shopper1.setId(null);
        shopper1.setEmail(null);
        shopper1.setName(null);
        shopper1.setProvider(null);
        shopper1.setShopCartIds(null);

        shopper2.setId(null);
        shopper2.setEmail(null);
        shopper2.setName(null);
        shopper2.setProvider(null);
        shopper2.setShopCartIds(null);

        // When
        int hashCode1 = shopper1.hashCode();
        int hashCode2 = shopper2.hashCode();

        // Then
        assertEquals(hashCode1, hashCode2);
        assertDoesNotThrow(() -> shopper1.hashCode());
    }

    @Test
    @DisplayName("Should handle mixed null and non-null values")
    void shouldHandleMixedNullAndNonNullValues() {
        // Given
        shopper1.setId("shopper-123");
        shopper1.setEmail(null);
        shopper1.setName("John Doe");
        shopper1.setProvider(null);
        shopper1.setShopCartIds(Arrays.asList("cart-1"));

        shopper2.setId("shopper-123");
        shopper2.setEmail(null);
        shopper2.setName("John Doe");
        shopper2.setProvider(null);
        shopper2.setShopCartIds(Arrays.asList("cart-1"));

        // When
        int hashCode1 = shopper1.hashCode();
        int hashCode2 = shopper2.hashCode();

        // Then
        assertEquals(hashCode1, hashCode2);
    }

    @Test
    @DisplayName("Should return different hashCode when one field is null and other is not")
    void shouldReturnDifferentHashCodeWhenOneFieldIsNullAndOtherIsNot() {
        // Given
        shopper1.setId("shopper-123");
        shopper1.setEmail("test@example.com");
        shopper1.setName("John Doe");
        shopper1.setProvider("google");

        shopper2.setId(null);
        shopper2.setEmail("test@example.com");
        shopper2.setName("John Doe");
        shopper2.setProvider("google");

        // When
        int hashCode1 = shopper1.hashCode();
        int hashCode2 = shopper2.hashCode();

        // Then
        assertNotEquals(hashCode1, hashCode2);
    }

    @Test
    @DisplayName("Should return consistent hashCode for multiple invocations")
    void shouldReturnConsistentHashCodeForMultipleInvocations() {
        // Given
        shopper1.setId("shopper-123");
        shopper1.setEmail("test@example.com");
        shopper1.setName("John Doe");
        shopper1.setProvider("google");
        shopper1.setShopCartIds(Arrays.asList("cart-1", "cart-2"));

        // When
        int hashCode1 = shopper1.hashCode();
        int hashCode2 = shopper1.hashCode();
        int hashCode3 = shopper1.hashCode();

        // Then
        assertEquals(hashCode1, hashCode2);
        assertEquals(hashCode2, hashCode3);
        assertEquals(hashCode1, hashCode3);
    }

    @Test
    @DisplayName("Should maintain hashCode consistency after field modifications")
    void shouldMaintainHashCodeConsistencyAfterFieldModifications() {
        // Given
        shopper1.setId("shopper-123");
        shopper1.setEmail("test@example.com");
        shopper1.setName("John Doe");
        shopper1.setProvider("google");
        shopper1.setShopCartIds(Arrays.asList("cart-1"));

        int initialHashCode = shopper1.hashCode();

        // When - modify a field
        shopper1.setName("Jane Doe");
        int modifiedHashCode = shopper1.hashCode();

        // Then
        assertNotEquals(initialHashCode, modifiedHashCode);
    }

    @Test
    @DisplayName("Should handle empty shopCartIds list")
    void shouldHandleEmptyShopCartIdsList() {
        // Given
        shopper1.setId("shopper-123");
        shopper1.setEmail("test@example.com");
        shopper1.setName("John Doe");
        shopper1.setProvider("google");
        shopper1.setShopCartIds(new ArrayList<>());

        shopper2.setId("shopper-123");
        shopper2.setEmail("test@example.com");
        shopper2.setName("John Doe");
        shopper2.setProvider("google");
        shopper2.setShopCartIds(new ArrayList<>());

        // When
        int hashCode1 = shopper1.hashCode();
        int hashCode2 = shopper2.hashCode();

        // Then
        assertEquals(hashCode1, hashCode2);
    }

    @Test
    @DisplayName("Should return different hashCode for different list orders")
    void shouldReturnDifferentHashCodeForDifferentListOrders() {
        // Given
        shopper1.setId("shopper-123");
        shopper1.setEmail("test@example.com");
        shopper1.setName("John Doe");
        shopper1.setProvider("google");
        shopper1.setShopCartIds(Arrays.asList("cart-1", "cart-2"));

        shopper2.setId("shopper-123");
        shopper2.setEmail("test@example.com");
        shopper2.setName("John Doe");
        shopper2.setProvider("google");
        shopper2.setShopCartIds(Arrays.asList("cart-2", "cart-1"));

        // When
        int hashCode1 = shopper1.hashCode();
        int hashCode2 = shopper2.hashCode();

        // Then
        assertNotEquals(hashCode1, hashCode2);
    }

    @Test
    @DisplayName("Should verify hashCode implementation matches Objects.hash implementation")
    void shouldVerifyHashCodeImplementationMatchesObjectsHashImplementation() {
        // Given
        shopper1.setId("shopper-123");
        shopper1.setEmail("test@example.com");
        shopper1.setName("John Doe");
        shopper1.setProvider("google");
        shopper1.setShopCartIds(Arrays.asList("cart-1", "cart-2"));

        // When
        int actualHashCode = shopper1.hashCode();
        int expectedHashCode = Objects.hash(
                shopper1.getId(),
                shopper1.getEmail(),
                shopper1.getName(),
                shopper1.getProvider(),
                shopper1.getShopCartIds()
        );

        // Then
        assertEquals(expectedHashCode, actualHashCode);
    }

    @Test
    @DisplayName("Should handle special characters in string fields")
    void shouldHandleSpecialCharactersInStringFields() {
        // Given
        shopper1.setId("shopper-123-éñ@#$%");
        shopper1.setEmail("tést+ñoñó@example.com");
        shopper1.setName("José María");
        shopper1.setProvider("google-oauth2");
        shopper1.setShopCartIds(Arrays.asList("cart-1-éñ", "cart-2-🛒"));

        shopper2.setId("shopper-123-éñ@#$%");
        shopper2.setEmail("tést+ñoñó@example.com");
        shopper2.setName("José María");
        shopper2.setProvider("google-oauth2");
        shopper2.setShopCartIds(Arrays.asList("cart-1-éñ", "cart-2-🛒"));

        // When
        int hashCode1 = shopper1.hashCode();
        int hashCode2 = shopper2.hashCode();

        // Then
        assertEquals(hashCode1, hashCode2);
        assertDoesNotThrow(() -> shopper1.hashCode());
    }

    @Test
    @DisplayName("Should handle very long string values")
    void shouldHandleVeryLongStringValues() {
        // Given
        String longString = "a".repeat(1000);
        shopper1.setId(longString);
        shopper1.setEmail(longString + "@example.com");
        shopper1.setName(longString);
        shopper1.setProvider(longString);

        // When/Then
        assertDoesNotThrow(() -> shopper1.hashCode());
        assertTrue(shopper1.hashCode() != 0); // Should produce some hash value
    }

    @Test
    @DisplayName("Should validate hashCode contract with equals method")
    void shouldValidateHashCodeContractWithEqualsMethod() {
        // Given
        shopper1.setId("shopper-123");
        shopper1.setEmail("test@example.com");
        shopper1.setName("John Doe");
        shopper1.setProvider("google");
        shopper1.setShopCartIds(Arrays.asList("cart-1"));

        shopper2.setId("shopper-123");
        shopper2.setEmail("test@example.com");
        shopper2.setName("John Doe");
        shopper2.setProvider("google");
        shopper2.setShopCartIds(Arrays.asList("cart-1"));

        // When
        boolean areEqual = shopper1.equals(shopper2);
        int hashCode1 = shopper1.hashCode();
        int hashCode2 = shopper2.hashCode();

        // Then - If objects are equal, their hash codes must be equal
        assertTrue(areEqual);
        assertEquals(hashCode1, hashCode2);
    }

}