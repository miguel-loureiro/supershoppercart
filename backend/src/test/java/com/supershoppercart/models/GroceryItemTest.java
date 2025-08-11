package com.supershoppercart.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GroceryItemTest {

    @Test
    void testConstructor_setsFieldsCorrectly() {
        GroceryItem item = new GroceryItem("Milk", "1L");

        assertEquals("Milk", item.getDesignation());
        assertEquals("1L", item.getQuantity());
        assertFalse(item.isPurchased()); // default is false
    }

    @Test
    void testSettersAndGetters() {
        GroceryItem item = new GroceryItem();

        item.setDesignation("Eggs");
        item.setQuantity("12");
        item.setPurchased(true);

        assertEquals("Eggs", item.getDesignation());
        assertEquals("12", item.getQuantity());
        assertTrue(item.isPurchased());
    }

    @Test
    void testEquals_sameObject() {
        GroceryItem item = new GroceryItem("Bread", "1 loaf");
        assertEquals(item, item);
    }

    @Test
    void testEquals_equalObjects() {
        GroceryItem item1 = new GroceryItem("Apples", "2kg");
        GroceryItem item2 = new GroceryItem("Apples", "2kg");

        assertEquals(item1, item2);
        assertEquals(item1.hashCode(), item2.hashCode());
    }

    @Test
    void testEquals_differentObjects() {
        GroceryItem item1 = new GroceryItem("Bananas", "1kg");
        GroceryItem item2 = new GroceryItem("Oranges", "2kg");

        assertNotEquals(item1, item2);
    }

    @Test
    void testEquals_withDifferentPurchasedFlag() {
        GroceryItem item1 = new GroceryItem("Juice", "1L");
        GroceryItem item2 = new GroceryItem("Juice", "1L");
        item2.setPurchased(true);

        assertNotEquals(item1, item2);
    }

    @Test
    void testEquals_nullAndDifferentClass() {
        GroceryItem item = new GroceryItem("Milk", "1L");

        assertNotEquals(null, item);
        assertNotEquals("Not a GroceryItem", item);
    }

    @Test
    void testHashCode_consistency() {
        GroceryItem item = new GroceryItem("Coffee", "250g");
        int initialHash = item.hashCode();
        int laterHash = item.hashCode();

        assertEquals(initialHash, laterHash);
    }

    @Test
    void testToString_containsFields() {
        GroceryItem item = new GroceryItem("Tea", "100g");
        item.setPurchased(true);

        String str = item.toString();

        assertTrue(str.contains("Tea"));
        assertTrue(str.contains("100g"));
        assertTrue(str.contains("true"));
    }
}