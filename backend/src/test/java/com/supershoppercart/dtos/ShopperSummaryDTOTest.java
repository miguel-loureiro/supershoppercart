package com.supershoppercart.dtos;

import com.supershoppercart.models.Shopper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ShopperSummaryDTO class.
 * This class validates the basic functionality of the DTO, including its two constructors,
 * as well as the getters and setters.
 *
 * This test file includes a simplified placeholder for the Shopper class to ensure
 * the tests are self-contained and runnable without external dependencies.
 */
@DisplayName("ShopperSummaryDTO Unit Tests")
public class ShopperSummaryDTOTest {

    // --- End of placeholder class ---

    /**
     * Tests the no-argument constructor and setter methods.
     * It verifies that a DTO can be created and its fields can be set and retrieved correctly.
     */
    @Test
    @DisplayName("Should create a DTO with no-arg constructor and set values correctly")
    void testNoArgsConstructorAndSetters() {
        // 1. Create a new DTO using the default constructor
        ShopperSummaryDTO dto = new ShopperSummaryDTO();

        // 2. Define test data
        String id = "shopper-id-123";
        String email = "test@example.com";
        String name = "Test Shopper";

        // 3. Set values using setters
        dto.setId(id);
        dto.setEmail(email);
        dto.setName(name);

        // 4. Verify values using getters
        assertEquals(id, dto.getId(), "The ID should match the set value.");
        assertEquals(email, dto.getEmail(), "The email should match the set value.");
        assertEquals(name, dto.getName(), "The name should match the set value.");
    }

    /**
     * Tests the constructor that takes a Shopper object.
     * It verifies that the DTO correctly maps the properties from the Shopper.
     */
    @Test
    @DisplayName("Should create a DTO with Shopper constructor and retrieve values correctly")
    void testShopperConstructor() {
        // 1. Create a Shopper object with sample data
        Shopper shopper = new Shopper("shopper-id-456", "another.test@example.com", "Another Shopper");

        // 2. Create a new DTO using the Shopper constructor
        ShopperSummaryDTO dto = new ShopperSummaryDTO(shopper);

        // 3. Verify that the DTO fields match the Shopper's fields
        assertEquals(shopper.getId(), dto.getId(), "The ID should be mapped from the Shopper object.");
        assertEquals(shopper.getEmail(), dto.getEmail(), "The email should be mapped from the Shopper object.");
        assertEquals(shopper.getName(), dto.getName(), "The name should be mapped from the Shopper object.");
    }

    /**
     * Tests the setters with null values.
     * This ensures the DTO handles nulls without errors and correctly updates its state.
     */
    @Test
    @DisplayName("Should handle null values correctly via setters")
    void testSettersWithNullValues() {
        // 1. Create a DTO and set some initial values
        ShopperSummaryDTO dto = new ShopperSummaryDTO();
        dto.setId("some-id");
        dto.setEmail("some@email.com");
        dto.setName("Some Name");

        // 2. Set all fields to null
        dto.setId(null);
        dto.setEmail(null);
        dto.setName(null);

        // 3. Verify that all fields are now null
        assertNull(dto.getId(), "The ID should be null.");
        assertNull(dto.getEmail(), "The email should be null.");
        assertNull(dto.getName(), "The name should be null.");
    }
}