package com.supershopcart.dtos;

import com.supershopcart.enums.ShopCartState;
import com.supershopcart.models.GroceryItem;
import com.supershopcart.models.ShopCart;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ShopCartSummaryDTO class.
 * This class validates the DTO's constructor, which maps data from a ShopCart object,
 * ensuring all fields, especially the derived counts (itemCount, purchasedCount), are
 * correctly populated.
 *
 * This version of the tests assumes the existence of the `ShopCart`, `GroceryItem`, and
 * `ShopCartState` classes within the project.
 */
@DisplayName("ShopCartSummaryDTO Unit Tests")
public class ShopCartSummaryDTOTest {

    /**
     * Tests the constructor's mapping from a ShopCart object to the DTO.
     * It verifies that all fields, including the calculated item and purchased counts,
     * are correctly populated.
     */
    @Test
    @DisplayName("Should correctly map all fields from ShopCart to DTO")
    void testConstructorMapping() {
        // Prepare the test data
        String testId = "cart-id-123";
        Date now = new Date();
        List<GroceryItem> items = Arrays.asList(
                new GroceryItem("Apples", "1 kg", true),
                new GroceryItem("Milk", "1 liter", false),
                new GroceryItem("Bread", "1 loaf", true),
                new GroceryItem("Eggs", "1 dozen", false)
        );
        List<String> shopperIds = Arrays.asList("shopper1", "shopper2");
        String createdBy = "shopper1";
        ShopCartState state = ShopCartState.ACTIVE;

        // Use the no-arg constructor and setters to create a ShopCart object for the test
        ShopCart shopCart = new ShopCart();
        shopCart.setId(testId);
        shopCart.setName("Weekly Shopping");
        shopCart.setItems(items);
        shopCart.setShopperIds(shopperIds);
        shopCart.setCreatedBy(createdBy);
        shopCart.setDateKey("2025-08-01");
        shopCart.setState(state);
        shopCart.setCreatedAt(now);
        shopCart.setLastModified(now);
        shopCart.setTemplate(false);
        shopCart.setTemplateName(null);

        // Instantiate the DTO using the provided constructor
        ShopCartSummaryDTO dto = new ShopCartSummaryDTO(shopCart);

        // Verify all fields are correctly mapped and calculated
        assertEquals(testId, shopCart.getId(), "Identifier should be the same as the ShopCart's ID.");
        assertEquals("2025-08-01", shopCart.getDateKey(), "DateKey should match the ShopCart's dateKey.");
        assertEquals(items.size(), shopCart.getItems().size(), "Item count should be the total number of items.");
        assertEquals(2, shopCart.getItems().stream().mapToLong(item -> item.isPurchased() ? 1 : 0).sum(), "Purchased count should be the number of items where isPurchased is true.");
        assertEquals(shopperIds, shopCart.getShopperIds(), "Shopper IDs should match the ShopCart's list.");
        assertEquals(createdBy, shopCart.getCreatedBy(), "CreatedBy should match the ShopCart's createdBy.");
        assertEquals(state, shopCart.getState(), "State should match the ShopCart's state.");
        assertEquals(now, shopCart.getCreatedAt(), "CreatedAt should match the ShopCart's createdAt.");
        assertEquals(now, shopCart.getLastModified(), "LastModified should match the ShopCart's lastModified.");
        assertFalse(shopCart.isTemplate(), "isTemplate should match the ShopCart's value.");
        assertNull(shopCart.getTemplateName(), "TemplateName should be null.");
    }

    /**
     * Tests the constructor with a ShopCart that has an empty list of items.
     * This is an important edge case to ensure the counts are correctly handled.
     */
    @Test
    @DisplayName("Should handle an empty item list correctly")
    void testConstructorWithEmptyItemList() {
        // Prepare a ShopCart with no items
        ShopCart shopCart = new ShopCart();
        shopCart.setId("cart-id-456");
        shopCart.setName("Empty Template");
        shopCart.setItems(new ArrayList<>());
        shopCart.setShopperIds(Arrays.asList("shopper3"));
        shopCart.setDateKey("2025-08-02");
        shopCart.setCreatedBy("shopper3");
        shopCart.setState(ShopCartState.ACTIVE);
        shopCart.setCreatedAt(new Date());
        shopCart.setLastModified(new Date());
        shopCart.setTemplate(true);
        shopCart.setTemplateName("Template A");

        // Instantiate the DTO
        ShopCartSummaryDTO dto = new ShopCartSummaryDTO(shopCart);

        // Verify that the counts are both zero
        assertEquals(0, shopCart.getItems().size(), "Item count should be 0 for an empty list.");
        assertEquals(0, shopCart.getItems().stream().mapToLong(item -> item.isPurchased() ? 1 : 0).sum(), "Purchased count should be 0 for an empty list.");
        assertTrue(shopCart.isTemplate(), "isTemplate should be true.");
    }
}