package com.supershoppercart.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

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
}