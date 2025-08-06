package com.supershopcart.dtos;

import com.supershopcart.enums.SharePermission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for the ShareCartRequestDTO class.
 * This class validates the basic functionality of the DTO, including constructors, getters, and setters.
 */
@DisplayName("ShareCartRequestDTO Unit Tests")
public class ShareCartRequestDTOTest {

    // Assuming the SharePermission enum is defined elsewhere, e.g.:
    // public enum SharePermission { READ, EDIT }

    /**
     * Tests the no-argument constructor and setter methods.
     * It creates an empty DTO, sets values using the setters, and then verifies
     * that the values are correctly retrieved using the getters.
     */
    @Test
    @DisplayName("Should create a DTO using no-arg constructor and set values correctly")
    void testNoArgsConstructorAndSetters() {
        // 1. Create a new DTO using the default constructor
        ShareCartRequestDTO dto = new ShareCartRequestDTO();

        // 2. Define test data
        String cartId = "cart123";
        String targetShopperEmail = "shopper@example.com";
        SharePermission permission = SharePermission.EDIT;

        // 3. Set values using setters
        dto.setCartId(cartId);
        dto.setTargetShopperEmail(targetShopperEmail);
        dto.setPermission(permission);

        // 4. Verify values using getters
        assertEquals(cartId, dto.getCartId(), "Cart ID should match the set value.");
        assertEquals(targetShopperEmail, dto.getTargetShopperEmail(), "Target shopper email should match the set value.");
        assertEquals(permission, dto.getPermission(), "Permission should match the set value.");
    }

    /**
     * Tests the all-argument constructor.
     * It creates a DTO with initial values and verifies that the values are
     * correctly stored and can be retrieved using the getters.
     */
    @Test
    @DisplayName("Should create a DTO using all-arg constructor and retrieve values correctly")
    void testAllArgsConstructorAndGetters() {
        // 1. Define test data
        String cartId = "cart456";
        String targetShopperEmail = "another.shopper@example.com";
        SharePermission permission = SharePermission.VIEW;

        // 2. Create a new DTO using the all-args constructor
        ShareCartRequestDTO dto = new ShareCartRequestDTO(cartId, targetShopperEmail, permission);

        // 3. Verify values using getters
        assertEquals(cartId, dto.getCartId(), "Cart ID should match the constructor value.");
        assertEquals(targetShopperEmail, dto.getTargetShopperEmail(), "Target shopper email should match the constructor value.");
        assertEquals(permission, dto.getPermission(), "Permission should match the constructor value.");
    }

    /**
     * Tests the setters with null values.
     * This is an important edge case to ensure the DTO can handle nulls without
     * throwing a NullPointerException and that the state is correctly updated.
     */
    @Test
    @DisplayName("Should handle null values correctly via setters")
    void testSettersWithNullValues() {
        // 1. Create a new DTO with some initial values
        ShareCartRequestDTO dto = new ShareCartRequestDTO("initial-id", "initial@email.com", SharePermission.VIEW);

        // 2. Set all fields to null
        dto.setCartId(null);
        dto.setTargetShopperEmail(null);
        dto.setPermission(null);

        // 3. Verify that all fields are now null
        assertNull(dto.getCartId(), "Cart ID should be null.");
        assertNull(dto.getTargetShopperEmail(), "Target shopper email should be null.");
        assertNull(dto.getPermission(), "Permission should be null.");
    }
}